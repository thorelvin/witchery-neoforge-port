package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Witch-Hunter crossbow bolt projectile.
 *
 * Bolt types (1.7.10 EntityBolt parity):
 *   0 = Wooden     - standard stake damage
 *   2 = Nullifying - clears active potion effects; powered shots also drain infusion power
 *   3 = Bone       - holy damage bonus against undead/demonic targets
 *   4 = Silver     - detected as silver damage by SilverEvents / WolfmanEntity
 */
public class BoltEntity extends AbstractArrow {

    public static final int WOODEN = 0;
    public static final int NULLIFYING = 2;
    public static final int BONE = 3;
    public static final int SILVER = 4;
    public static final float SHOOTER_POWER_REDUCTION = 1.0F;
    public static final float TARGET_POWER_REDUCTION = 0.5F;

    private static final EntityDataAccessor<Integer> BOLT_TYPE =
            SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> POWERED_DRAINING =
            SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.BOOLEAN);

    private double witcheryBaseDamage = 2.0D;
    private int knockbackStrength;

    /** Deserialization constructor required by EntityType. */
    public BoltEntity(EntityType<? extends BoltEntity> type, Level level) {
        super(type, level);
    }

    /** Spawn from a shooter with a specific bolt type. */
    public BoltEntity(Level level, LivingEntity shooter, int boltType) {
        super(WitcheryEntities.BOLT.get(), shooter, level, pickupForType(boltType), null);
        setBoltType(boltType);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOLT_TYPE, WOODEN);
        builder.define(POWERED_DRAINING, false);
    }

    public int getBoltType() { return entityData.get(BOLT_TYPE); }
    public void setBoltType(int t) { entityData.set(BOLT_TYPE, t); }
    public boolean isSilver() { return getBoltType() == SILVER; }
    public boolean isNullifying() { return getBoltType() == NULLIFYING; }
    public boolean isHolyDamage() { return getBoltType() == BONE; }
    public boolean isPoweredDraining() { return entityData.get(POWERED_DRAINING); }
    public void setPoweredDraining(boolean poweredDraining) { entityData.set(POWERED_DRAINING, poweredDraining); }

    public void setKnockbackStrength(int knockbackStrength) {
        this.knockbackStrength = knockbackStrength;
    }

    @Override
    public void setBaseDamage(double baseDamage) {
        super.setBaseDamage(baseDamage);
        this.witcheryBaseDamage = baseDamage;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return pickupForType(getBoltType());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        double originalDamage = witcheryBaseDamage;

        if (target instanceof LivingEntity living && level() instanceof ServerLevel) {
            if (isNullifying()) {
                stripActiveEffects(living);
                if (isPoweredDraining()) {
                    reducePowerLevels(living, TARGET_POWER_REDUCTION);
                }
            }
            if (isHolyDamage() && isHolyTarget(living)) {
                setBaseDamage(originalDamage * 1.5D);
            }
        }

        super.onHitEntity(result);
        setBaseDamage(originalDamage);
    }

    @Override
    protected void doKnockback(LivingEntity entity, DamageSource damageSource) {
        if (knockbackStrength <= 0) {
            super.doKnockback(entity, damageSource);
            return;
        }

        Vec3 knockback = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D)
                .normalize()
                .scale(knockbackStrength * 0.6D);
        if (knockback.lengthSqr() > 0.0D) {
            entity.push(knockback.x, 0.1D, knockback.z);
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BoltType", getBoltType());
        tag.putBoolean("PoweredDraining", isPoweredDraining());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setBoltType(tag.getIntOr("BoltType", WOODEN));
        setPoweredDraining(tag.getBooleanOr("PoweredDraining", false));
    }

    public static boolean reducePowerLevels(LivingEntity living, float reduction) {
        if (living.level().isClientSide() || !(living instanceof ServerPlayer player)) {
            return false;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int maxEnergy = data.getInfusionMaxEnergy();
        int currentEnergy = data.getInfusionEnergy();
        if (maxEnergy <= 0 || currentEnergy <= 0) {
            return false;
        }

        int reduceBy = Math.max((int) (maxEnergy * reduction), 1);
        int newEnergy = Math.max(currentEnergy - reduceBy, 0);
        if (newEnergy == currentEnergy) {
            return false;
        }

        data.setInfusionEnergy(newEnergy, maxEnergy);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
    }

    private static ItemStack pickupForType(int boltType) {
        return switch (boltType) {
            case NULLIFYING -> new ItemStack(WitcheryItems.NULLIFYING_BOLT.get());
            case BONE -> new ItemStack(WitcheryItems.BONE_BOLT.get());
            case SILVER -> new ItemStack(WitcheryItems.SILVER_BOLT.get());
            default -> new ItemStack(WitcheryItems.WOODEN_BOLT.get());
        };
    }

    private static void stripActiveEffects(LivingEntity living) {
        java.util.List<Holder<MobEffect>> effects = living.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .toList();
        for (Holder<MobEffect> effect : effects) {
            living.removeEffect(effect);
        }
    }

    private static boolean isHolyTarget(LivingEntity living) {
        EntityType<?> type = living.getType();
        return type.is(EntityTypeTags.UNDEAD)
                || living instanceof DemonEntity
                || type == EntityType.GHAST
                || type == EntityType.BLAZE
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.WITHER;
    }
}
