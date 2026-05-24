package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.BrewType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownBrewEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> BREW_TYPE =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_GAS =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);

    public ThrownBrewEntity(EntityType<? extends ThrownBrewEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewType brewType, boolean gas, int power) {
        super(WitcheryEntities.THROWN_BREW.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
        setBrewType(brewType.ordinal());
        setGas(gas);
        setPower(power);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BREW_TYPE, 0);
        builder.define(IS_GAS, false);
        builder.define(POWER, 1);
    }

    public int getBrewTypeId() { return entityData.get(BREW_TYPE); }
    public void setBrewType(int id) { entityData.set(BREW_TYPE, id); }
    public boolean isGas() { return entityData.get(IS_GAS); }
    public void setGas(boolean gas) { entityData.set(IS_GAS, gas); }
    public int getPower() { return entityData.get(POWER); }
    public void setPower(int power) { entityData.set(POWER, Math.max(1, Math.min(5, power))); }

    public BrewType getBrewType() {
        int id = getBrewTypeId();
        BrewType[] vals = BrewType.values();
        return (id >= 0 && id < vals.length) ? vals[id] : BrewType.FOULNESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(
                    isGas() ? ParticleTypes.WITCH : ParticleTypes.SPLASH,
                    getX(), getY() + 0.1, getZ(),
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1
            );
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            Vec3 pos = result.getLocation();
            BrewType type = getBrewType();
            int tier = getPower();
            if (isGas()) {
                type.applyGas(level(), pos, tier);
            } else {
                type.applySplash(level(), pos, tier);
            }
            discard();
        }
    }
}
