package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SoulfireProjectile extends AbstractHurtingProjectile {

    public SoulfireProjectile(EntityType<? extends SoulfireProjectile> type, Level level) {
        super(type, level);
    }

    public SoulfireProjectile(Level level, LivingEntity owner, double dx, double dy, double dz) {
        super(WitcheryEntities.SOULFIRE.get(), owner, new Vec3(dx, dy, dz), level);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity shooter = this.getOwner();
        DamageSource src = shooter != null
                ? this.damageSources().mobProjectile(this, (LivingEntity) shooter)
                : this.damageSources().magic();
        hit.hurt(src, 5.0f); if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) this.discard();
    }

    // Intentional: soulfire is cold spectral fire — the projectile itself does not burn
    @Override
    public boolean isOnFire() { return false; }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    public net.minecraft.core.particles.ParticleOptions getTrailParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }
}
