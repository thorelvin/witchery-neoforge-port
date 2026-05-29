package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 *   0 = Wooden  — standard damage
 *   4 = Silver  — detected as silver damage by SilverEvents / WolfmanEntity
 */
public class BoltEntity extends AbstractArrow {

    public static final int WOODEN = 0;
    public static final int SILVER = 4;

    private static final EntityDataAccessor<Integer> BOLT_TYPE =
            SynchedEntityData.defineId(BoltEntity.class, EntityDataSerializers.INT);

    /** Deserialization constructor — required by EntityType. */
    public BoltEntity(EntityType<? extends BoltEntity> type, Level level) {
        super(type, level);
    }

    /** Spawn from a shooter with a specific bolt type. */
    public BoltEntity(Level level, LivingEntity shooter, int boltType) {
        super(WitcheryEntities.BOLT.get(), shooter, level,
                boltType == SILVER
                        ? new ItemStack(WitcheryItems.SILVER_BOLT.get())
                        : new ItemStack(WitcheryItems.WOODEN_BOLT.get()),
                null);
        setBoltType(boltType);
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOLT_TYPE, WOODEN);
    }

    public int getBoltType() { return entityData.get(BOLT_TYPE); }
    public void setBoltType(int t) { entityData.set(BOLT_TYPE, t); }
    public boolean isSilver() { return getBoltType() == SILVER; }

    // ── Pickup item (required abstract) ──────────────────────────────────────

    @Override
    protected ItemStack getDefaultPickupItem() {
        return isSilver()
                ? new ItemStack(WitcheryItems.SILVER_BOLT.get())
                : new ItemStack(WitcheryItems.WOODEN_BOLT.get());
    }

    // ── Hit entity ────────────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner  = getOwner();
        if (target instanceof LivingEntity living && level() instanceof ServerLevel sl) {
            living.hurtServer(sl, level().damageSources().arrow(this, owner), 6.0f);
        }
        super.onHitEntity(result);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

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
}
