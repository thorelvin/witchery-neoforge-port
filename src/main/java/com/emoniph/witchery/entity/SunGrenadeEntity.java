package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class SunGrenadeEntity extends ThrowableProjectile {

    private static final float BLAST_RADIUS = 5.0f;
    private static final float VAMPIRE_DAMAGE = 8.0f;

    public SunGrenadeEntity(EntityType<? extends SunGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SunGrenadeEntity(Level level, LivingEntity owner) {
        super(WitcheryEntities.SUN_GRENADE.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // no extra data needed
    }

    @Override
    protected void onHit(HitResult result) {
        if (level() instanceof ServerLevel sl) {
            explode(sl);
        }
        if (!level().isClientSide()) {
            discard();
        }
    }

    private void explode(ServerLevel level) {
        // Sunburst particles
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 4, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 32, BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, 0.1);

        AABB box = new AABB(getX() - BLAST_RADIUS, getY() - BLAST_RADIUS, getZ() - BLAST_RADIUS,
                getX() + BLAST_RADIUS, getY() + BLAST_RADIUS, getZ() + BLAST_RADIUS);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : nearby) {
            if (!isVampire(entity)) continue;
            if (entity == getOwner()) continue;
            entity.hurt(level.damageSources().inFire(), VAMPIRE_DAMAGE);
            entity.igniteForSeconds(5.0f);
        }
    }

    private static boolean isVampire(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire();
        }
        // Non-player vampires would be tagged via entity type — future extension
        return false;
    }
}
