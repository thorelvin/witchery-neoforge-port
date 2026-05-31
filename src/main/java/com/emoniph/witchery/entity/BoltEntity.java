package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Witch-Hunter crossbow bolt projectile.
 *
 * Bolt types (1.7.10 EntityBolt parity):
 *   0 = Wooden     - standard stake damage
 *   2 = Nullifying - clears active potion effects
 *   3 = Bone       - holy damage bonus against undead/demonic targets
 *   4 = Silver     - detected as silver damage by SilverEvents / WolfmanEntity
 */
public class BoltEntity extends AbstractArrow {

    public static final int WOODEN = 0;
    public static final int NULLIFYING = 2;
    public static final int BONE = 3;
    public static final int SILVER = 4;

    private static final EntityDataAccessor<Integer> BOLT_TYPE =
            SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);

    private double witcheryBaseDamage = 2.0D;

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
    }

    public int getBoltType() { return entityData.get(BOLT_TYPE); }
    public void setBoltType(int t) { entityData.set(BOLT_TYPE, t); }
    public boolean isSilver() { return getBoltType() == SILVER; }
    public boolean isNullifying() { return getBoltType() == NULLIFYING; }
    public boolean isHolyDamage() { return getBoltType() == BONE; }

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
            }
            if (isHolyDamage() && isHolyTarget(living)) {
                setBaseDamage(originalDamage * 1.5D);
            }
        }

        super.onHitEntity(result);
        setBaseDamage(originalDamage);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BoltType", getBoltType());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setBoltType(tag.getIntOr("BoltType", WOODEN));
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
