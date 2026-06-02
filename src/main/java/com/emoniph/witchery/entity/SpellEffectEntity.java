package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import com.emoniph.witchery.symbol.SymbolEffectProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;

/**
 * The flying orb launched by a {@link SymbolEffectProjectile} Mystic Branch spell. It resolves
 * the spell on impact via {@link SymbolEffectProjectile#onCollision}.
 *
 * Source parity: {@code com.emoniph.witchery.entity.EntitySpellEffect} (1.7.10-0.24.1),
 * simplified to a {@link ThrowableProjectile} with a no-op renderer plus a client particle trail.
 */
public class SpellEffectEntity extends ThrowableProjectile {

    private int effectId = -1;
    private int effectLevel = 1;
    private int lifeTime = 80;

    public SpellEffectEntity(EntityType<? extends SpellEffectEntity> type, Level level) {
        super(type, level);
    }

    public SpellEffectEntity(Level level, LivingEntity owner, SymbolEffect effect, int effectLevel) {
        super(WitcheryEntities.SPELL_EFFECT.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
        this.effectId = effect.getEffectId();
        this.effectLevel = effectLevel;
        if (effect instanceof SymbolEffectProjectile projectile && projectile.getTimeToLive() > 0) {
            this.lifeTime = projectile.getTimeToLive();
        }
        setNoGravity(!effect.fallsToEarth());
    }

    public int getEffectLevel() {
        return effectLevel;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synched data; the client trail is generic.
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        SymbolEffect effect = EffectRegistry.instance().getById(effectId);
        LivingEntity caster = getOwner() instanceof LivingEntity living ? living : null;
        if (effect instanceof SymbolEffectProjectile projectile) {
            try {
                projectile.onCollision(serverLevel, caster, result, this);
            } catch (RuntimeException ex) {
                // A misbehaving spell effect must never crash the projectile tick.
            }
        }
        serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.0);
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (level().random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.WITCH,
                        getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (tickCount > lifeTime && !isRemoved()) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EffectId", effectId);
        tag.putInt("EffectLevel", effectLevel);
        tag.putInt("LifeTime", lifeTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        effectId = tag.getIntOr("EffectId", -1);
        effectLevel = tag.getIntOr("EffectLevel", 1);
        lifeTime = tag.getIntOr("LifeTime", 80);
    }
}
