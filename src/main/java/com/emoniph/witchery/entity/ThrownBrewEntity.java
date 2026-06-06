package com.emoniph.witchery.entity;

import com.emoniph.witchery.blocks.BrewFluidBlock;
import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.BrewType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThrownBrewEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> BREW_TYPE =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_GAS =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);

    /** Non-empty when carrying a composed custom brew (overrides the fixed BrewType path). */
    private BrewContents composed = BrewContents.EMPTY;
    private Brews.Dispersal composedDispersal = Brews.Dispersal.SPLASH;

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

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewContents contents, boolean gas) {
        this(level, owner, contents, gas ? Brews.Dispersal.GAS : Brews.Dispersal.SPLASH);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewContents contents, Brews.Dispersal dispersal) {
        super(WitcheryEntities.THROWN_BREW.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
        this.composed = contents;
        this.composedDispersal = sanitizeComposedDispersal(dispersal);
        setBrewType(0);
        setGas(this.composedDispersal == Brews.Dispersal.GAS);
        setPower(contents.power());
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
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 pos = result.getLocation();
            if (!composed.isEmpty()) {
                LivingEntity directTarget = result instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living ? living : null;
                if (composedDispersal == Brews.Dispersal.GAS || composedDispersal == Brews.Dispersal.LIQUID) {
                    Brews.placeMovingBrewFluid(serverLevel, movingBrewFluidPos(result, serverLevel),
                            composed.items(), composedDispersal, getOwner());
                } else {
                    composed.applyToArea(serverLevel, pos, Brews.Dispersal.SPLASH,
                            directTarget, getOwner(), result.getType() == HitResult.Type.BLOCK);
                }
            } else {
                BrewType type = getBrewType();
                int tier = getPower();
                if (isGas()) {
                    type.applyGas(serverLevel, pos, tier);
                } else {
                    type.applySplash(serverLevel, pos, tier);
                }
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (!composed.isEmpty()) {
            tag.store("Composed", BrewContents.CODEC, composed);
            tag.putString("ComposedDispersal", composedDispersal.name());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        composed = tag.read("Composed", BrewContents.CODEC).orElse(BrewContents.EMPTY);
        composedDispersal = readComposedDispersal(tag.getStringOr("ComposedDispersal",
                isGas() ? Brews.Dispersal.GAS.name() : Brews.Dispersal.SPLASH.name()));
        setGas(composedDispersal == Brews.Dispersal.GAS);
    }

    private static BlockPos movingBrewFluidPos(HitResult result, Level level) {
        if (result instanceof BlockHitResult blockHit) {
            BlockPos hitPos = blockHit.getBlockPos();
            return BrewFluidBlock.canDisplace(level.getBlockState(hitPos))
                    ? hitPos
                    : hitPos.relative(blockHit.getDirection());
        }
        return BlockPos.containing(result.getLocation());
    }

    private static Brews.Dispersal readComposedDispersal(String name) {
        try {
            return sanitizeComposedDispersal(Brews.Dispersal.valueOf(name));
        } catch (IllegalArgumentException ex) {
            return Brews.Dispersal.SPLASH;
        }
    }

    private static Brews.Dispersal sanitizeComposedDispersal(Brews.Dispersal dispersal) {
        return dispersal == Brews.Dispersal.DRINK ? Brews.Dispersal.SPLASH : dispersal;
    }
}
