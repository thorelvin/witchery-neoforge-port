package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Flying Monkey — tameable flying animal created by mutating a wolf + owl with Mutandis Extremis.
 * Tamed with Raw Beef or Raw Porkchop (ocelot-style patience required).
 * Give it a Bound Waystone to be carried to those coordinates.
 * Dyeable with 16 dye colors.
 */
public class FlyingMonkeyEntity extends TamableAnimal {

    private static final EntityDataAccessor<Integer> DATA_DYE_COLOR =
            SynchedEntityData.defineId(FlyingMonkeyEntity.class, EntityDataSerializers.INT);

    // Waystone coordinates (0 = none)
    private long waystonePos = 0;
    private String waystoneDim = "";
    private boolean isCarrying = false;

    public FlyingMonkeyEntity(EntityType<? extends FlyingMonkeyEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 10f, 2f));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DYE_COLOR, DyeColor.WHITE.getId());
    }

    public DyeColor getDyeColor() { return DyeColor.byId(entityData.get(DATA_DYE_COLOR)); }
    public void setDyeColor(DyeColor color) { entityData.set(DATA_DYE_COLOR, color.getId()); }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Dye the monkey
        if (isTame() && held.getItem() instanceof DyeItem dye) {
            setDyeColor(dye.getDyeColor());
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.SUCCESS;
        }

        // Give a Bound Waystone → store coordinates
        if (isTame() && isOwnedBy(player) && held.is(net.minecraft.world.item.Items.AIR) == false
                && WaystoneItem.isBoundWaystone(held)) {
            if (!level().isClientSide()) {
                WaystoneItem.getBoundPos(held).ifPresent(pos -> {
                    waystonePos = pos.asLong();
                    WaystoneItem.getBoundDim(held).ifPresent(dim -> waystoneDim = dim.identifier().toString());
                    isCarrying = true;
                    sendSystemMessage(player, "witchery.flying_monkey.waystone_set");
                });
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        // Taming attempt: Raw Beef or Raw Porkchop (ocelot-style)
        if (!isTame() && (held.is(Items.BEEF) || held.is(Items.PORKCHOP))) {
            if (!level().isClientSide()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                if (getRandom().nextInt(3) == 0) {
                    tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    level().broadcastEntityEvent(this, (byte) 7); // heart particles
                } else {
                    level().broadcastEntityEvent(this, (byte) 6); // smoke particles
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Sit/stand toggle for tamed monkey
        if (isTame() && isOwnedBy(player) && !held.is(Items.BEEF) && !held.is(Items.PORKCHOP)) {
            if (!level().isClientSide()) setOrderedToSit(!isOrderedToSit());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void sendSystemMessage(Player player, String key) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), false);
    }

    // ── Carry to waystone ─────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isCarrying || waystonePos == 0) return;
        if (!isTame()) return;

        LivingEntity owner = getOwner();
        if (owner == null) return;

        net.minecraft.core.BlockPos dest = net.minecraft.core.BlockPos.of(waystonePos);

        // If owner is not already a passenger, add them
        if (!hasPassenger(owner)) {
            owner.startRiding(this);
        }

        // Fly toward destination
        Vec3 targetVec = Vec3.atCenterOf(dest);
        Vec3 dir = targetVec.subtract(position()).normalize();
        double spd = 0.4;
        setDeltaMovement(dir.x * spd, dir.y * spd, dir.z * spd);

        // Arrived?
        if (distanceToSqr(targetVec) < 4.0) {
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp && !waystoneDim.isEmpty()) {
                var dimKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.Identifier.parse(waystoneDim));
                ServerLevel target = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getLevel(dimKey);
                owner.stopRiding();
                if (target != null && sp.level() != target) {
                    sp.teleportTo(target, dest.getX()+0.5, (double)(dest.getY()), dest.getZ()+0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), sp.getYRot(), sp.getXRot(), false);
                } else {
                    sp.teleportTo(dest.getX()+0.5, dest.getY(), dest.getZ()+0.5);
                }
            }
            isCarrying = false;
            waystonePos = 0;
            waystoneDim = "";
        }
    }

    @Override
    public boolean isFood(ItemStack stack) { return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP); }

    @Override
    public boolean causeFallDamage(double dist, float mult, net.minecraft.world.damagesource.DamageSource src) { return false; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }

    // ── Breeding / offspring ──────────────────────────────────────────────────

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        FlyingMonkeyEntity child = WitcheryEntities.FLYING_MONKEY.get().create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
        if (child != null) {
            child.setTame(true, true);
        }
        return child;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DyeColor", getDyeColor().getId());
        tag.putLong("WaystonePos", waystonePos);
        tag.putString("WaystoneDim", waystoneDim);
        tag.putBoolean("IsCarrying", isCarrying);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getInt("DyeColor").isPresent()) setDyeColor(DyeColor.byId(tag.getIntOr("DyeColor", 0)));
        waystonePos = tag.getLongOr("WaystonePos", 0L);
        waystoneDim = tag.getStringOr("WaystoneDim", "");
        isCarrying = tag.getBooleanOr("IsCarrying", false);
    }
}
