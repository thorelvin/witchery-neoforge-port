package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Broom — rideable flying entity.
 *
 * Control: W to accelerate (look direction determines climb/dive),
 * S to slow, release W to hover and gradually descend.
 * Shift while riding to dismount.
 */
public class BroomEntity extends Mob {

    private static final double SPEED          = 0.18;
    private static final double SPEED_ENCHANTED = 0.30;
    private static final double DRAG            = 0.80;
    private static final double DRAG_ENCHANTED  = 0.85; // more responsive
    private static final double HOVER_DROP      = 0.008;

    private static final EntityDataAccessor<Boolean> DATA_ENCHANTED =
            SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.BOOLEAN);

    public BroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0); // minimal — broom is invulnerable anyway
    }

    @Override protected void registerGoals() {} // no AI

    public boolean isEnchanted() { return entityData.get(DATA_ENCHANTED); }
    public void setEnchanted(boolean v) { entityData.set(DATA_ENCHANTED, v); }

    /** Convenience constructor used by BroomItem when spawning. */
    public static BroomEntity create(Level level, double x, double y, double z) {
        BroomEntity broom = new BroomEntity(WitcheryEntities.BROOM.get(), level);
        broom.setPos(x, y, z);
        return broom;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        Entity passenger = getFirstPassenger();

        if (passenger instanceof Player rider) {
            // Sync rotation to rider
            setYRot(rider.getYRot());
            yRotO = getYRot();

            Vec3 look    = rider.getLookAngle();
            float forward = rider.zza; // 1.0 = W held, -1.0 = S held
            Vec3 current  = getDeltaMovement();

            double spd  = isEnchanted() ? SPEED_ENCHANTED : SPEED;
            double drag = isEnchanted() ? DRAG_ENCHANTED  : DRAG;

            // Soaring brew boost — stacks with owl familiar
            if (level() instanceof net.minecraft.server.level.ServerLevel) {
                WitcheryPlayerData pd = WitcheryAttachments.get(rider);
                if (pd.hasSoaringBuff()) {
                    boolean hasOwl = pd.getFamiliarUUID().isPresent();
                    spd  *= hasOwl ? 1.5 : 1.3;
                    drag  = Math.min(drag + 0.03, 0.95);
                }
            }

            if (Math.abs(forward) > 0.05f) {
                double fwd = forward > 0 ? spd : spd * 0.3;
                setDeltaMovement(
                        current.x * drag + look.x * fwd * forward,
                        current.y * drag + look.y * spd * 0.4 * Math.signum(forward),
                        current.z * drag + look.z * fwd * forward
                );
            } else {
                // No input — bleed momentum and hover-drop
                setDeltaMovement(
                        current.x * 0.7,
                        Math.max(current.y * 0.7 - HOVER_DROP, -0.15),
                        current.z * 0.7
                );
            }
        } else {
            // Unmanned — fall slowly
            Vec3 current = getDeltaMovement();
            setDeltaMovement(current.x * 0.8, Math.max(current.y - 0.03, -0.5), current.z * 0.8);
        }

        move(MoverType.SELF, getDeltaMovement());
    }

    // ── Mounting ──────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && !hasPassenger(player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunc) {
        if (hasPassenger(passenger)) {
            // Rider sits on top of the broom handle, slightly forward
            double offsetY = getBbHeight() + 0.1;
            Vec3 pos = new Vec3(getX(), getY() + offsetY, getZ());
            moveFunc.accept(passenger, pos.x, pos.y, pos.z);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    // ── Entity properties ─────────────────────────────────────────────────────

    @Override public boolean isPickable()         { return true; }
    @Override public boolean isPushable()         { return false; }
    @Override public boolean isInvulnerable()     { return true; }
    @Override public boolean shouldBeSaved()      { return true; }
    @Override public boolean removeWhenFarAway(double dist) { return false; }

    // ── NBT (no custom data) ──────────────────────────────────────────────────

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
