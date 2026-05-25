package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Broom entity with control rules modeled after the original Witchery broom.
 * The rider gains lift by pitching the camera, forward input accelerates the
 * broom, and an unmanned broom drifts down instead of hovering forever.
 */
public class BroomEntity extends Mob {

    private static final double BASE_SPEED_MULTIPLIER = 0.07;
    private static final double MAX_SPEED_MULTIPLIER = 0.35;
    private static final double BASE_SPEED_LIMIT = 0.9;
    private static final double ENCHANTED_SPEED_BONUS = 0.15;
    private static final double SIDEWAYS_CONTROL = 0.25;

    private static final EntityDataAccessor<Boolean> DATA_ENCHANTED =
            SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.BOOLEAN);

    private double speedMultiplier = BASE_SPEED_MULTIPLIER;
    private boolean riderHasOwlFamiliar;
    private boolean riderHasSoaringBrew;

    public BroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    protected void registerGoals() {
        // No AI.
    }

    public boolean isEnchanted() {
        return entityData.get(DATA_ENCHANTED);
    }

    public void setEnchanted(boolean value) {
        entityData.set(DATA_ENCHANTED, value);
    }

    public static BroomEntity create(Level level, double x, double y, double z) {
        BroomEntity broom = new BroomEntity(WitcheryEntities.BROOM.get(), level);
        broom.setPos(x, y, z);
        return broom;
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        // Block vanilla LivingEntity travel() — broom uses its own move() call in tick()
    }

    @Override
    public void tick() {
        super.tick();

        Entity passenger = getFirstPassenger();
        if (passenger instanceof LivingEntity rider) {
            updateRiderBuffFlags(rider);
            handleRiddenMovement(rider);
        } else {
            riderHasOwlFamiliar = false;
            riderHasSoaringBrew = false;
            handleUnriddenMovement();
        }

        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.99));
        alignToMotion();
    }

    private void handleRiddenMovement(LivingEntity rider) {
        double previousHorizontalSpeed = horizontalDistance(getDeltaMovement());

        setYRot(rider.getYRot());
        yRotO = getYRot();
        setXRot(0.0F);

        Vec3 motion = getDeltaMovement();
        if (rider.zza > 0.0F) {
            double yawRadians = Math.toRadians(rider.getYRot());
            double sinYaw = -Math.sin(yawRadians);
            double cosYaw = Math.cos(yawRadians);
            double controlFactor = 0.1 + (riderHasSoaringBrew ? 0.1 : 0.0) + (riderHasOwlFamiliar ? 0.2 : 0.0);

            motion = motion.add(
                    sinYaw * speedMultiplier * controlFactor,
                    0.0,
                    cosYaw * speedMultiplier * controlFactor
            );

            double pitch = -Math.sin(Math.toRadians(rider.getXRot()));
            if (pitch > -0.5 && pitch < 0.2) {
                pitch = 0.0;
            } else if (pitch < 0.0) {
                pitch *= 0.5;
            }
            motion = new Vec3(motion.x, pitch * speedMultiplier * 2.0, motion.z);

            if (riderHasOwlFamiliar) {
                motion = motion.add(
                        rider.xxa * cosYaw * speedMultiplier * SIDEWAYS_CONTROL,
                        0.0,
                        rider.xxa * -sinYaw * speedMultiplier * SIDEWAYS_CONTROL
                );
            }
        } else {
            motion = new Vec3(motion.x * 0.9, motion.y * 0.9, motion.z * 0.9);
        }

        motion = clampSpeed(motion);
        setDeltaMovement(motion);
        updateSpeedMultiplier(previousHorizontalSpeed, horizontalDistance(motion));
    }

    private void handleUnriddenMovement() {
        Vec3 motion = getDeltaMovement();
        double x = motion.x * 0.9;
        double z = motion.z * 0.9;
        if (Math.abs(x) < 0.01) {
            x = 0.0;
        }
        if (Math.abs(z) < 0.01) {
            z = 0.0;
        }

        double y = onGround() ? 0.0 : -0.2;
        setDeltaMovement(x, y, z);
        speedMultiplier = BASE_SPEED_MULTIPLIER;
    }

    private void updateRiderBuffFlags(LivingEntity rider) {
        riderHasOwlFamiliar = false;
        riderHasSoaringBrew = false;

        if (!(rider instanceof Player player)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        riderHasSoaringBrew = data.hasSoaringBuff();
        if (level() instanceof net.minecraft.server.level.ServerLevel) {
            riderHasOwlFamiliar = FamiliarHelper.hasActiveOwlFamiliar(player);
        } else {
            riderHasOwlFamiliar = data.getFamiliarUUID().isPresent() && "owl".equals(data.getFamiliarType());
        }
    }

    private Vec3 clampSpeed(Vec3 motion) {
        double speedLimit = BASE_SPEED_LIMIT;
        if (riderHasOwlFamiliar) {
            speedLimit += 0.3;
        }
        if (riderHasSoaringBrew) {
            speedLimit += 0.3;
        }
        if (isEnchanted()) {
            speedLimit += ENCHANTED_SPEED_BONUS;
        }

        double horizontalSpeed = horizontalDistance(motion);
        if (horizontalSpeed > speedLimit) {
            double scale = speedLimit / horizontalSpeed;
            motion = new Vec3(motion.x * scale, motion.y * scale, motion.z * scale);
        }
        return motion;
    }

    private void updateSpeedMultiplier(double previousHorizontalSpeed, double currentHorizontalSpeed) {
        if (currentHorizontalSpeed > previousHorizontalSpeed && speedMultiplier < MAX_SPEED_MULTIPLIER) {
            speedMultiplier += (MAX_SPEED_MULTIPLIER - speedMultiplier) / (MAX_SPEED_MULTIPLIER * 100.0);
            if (speedMultiplier > MAX_SPEED_MULTIPLIER) {
                speedMultiplier = MAX_SPEED_MULTIPLIER;
            }
        } else {
            speedMultiplier -= (speedMultiplier - BASE_SPEED_MULTIPLIER) / (MAX_SPEED_MULTIPLIER * 100.0);
            if (speedMultiplier < BASE_SPEED_MULTIPLIER) {
                speedMultiplier = BASE_SPEED_MULTIPLIER;
            }
        }
    }

    private void alignToMotion() {
        Vec3 motion = getDeltaMovement();
        if (horizontalDistanceSquared(motion) > 0.001) {
            float yaw = (float) (Mth.atan2(-motion.x, motion.z) * (180.0F / Math.PI));
            setYRot(yaw);
            yRotO = yaw;
        }
        setXRot(0.0F);
    }

    private static double horizontalDistance(Vec3 motion) {
        return Math.sqrt(horizontalDistanceSquared(motion));
    }

    private static double horizontalDistanceSquared(Vec3 motion) {
        return motion.x * motion.x + motion.z * motion.z;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            if (player.isShiftKeyDown() && !hasPassenger(player)) {
                ItemStack broomItem = new ItemStack(isEnchanted()
                        ? WitcheryItems.ENCHANTED_BROOM.get()
                        : WitcheryItems.BROOM.get());
                player.addItem(broomItem);
                this.discard();
                return InteractionResult.SUCCESS;
            }
            if (!hasPassenger(player)) {
                player.startRiding(this, true, true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunc) {
        if (hasPassenger(passenger)) {
            double offsetY = getBbHeight() + 0.1;
            Vec3 pos = new Vec3(getX(), getY() + offsetY, getZ());
            moveFunc.accept(passenger, pos.x, pos.y, pos.z);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ENCHANTED, false);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enchanted", isEnchanted());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setEnchanted(tag.getBooleanOr("Enchanted", false));
    }
}
