package com.emoniph.witchery.effect;

import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Grotesque: a non-debuff aura that repels nearby mobs every five ticks.
 */
public class GrotesqueMobEffect extends MobEffect {
    public static final int BREW_DURATION_TICKS = 3600;
    public static final double SOURCE_RADIUS = 4.0D;
    public static final double SOURCE_RADIUS_SQ = SOURCE_RADIUS * SOURCE_RADIUS;
    public static final double HORIZONTAL_PUSH = 0.22D;
    public static final double MIN_VERTICAL_PUSH = -0.10D;
    public static final double MAX_VERTICAL_PUSH = 0.12D;

    public GrotesqueMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!isSourceTickPhase(level.getGameTime())) {
            return true;
        }

        AABB bounds = AABB.ofSize(entity.position(), SOURCE_RADIUS * 2.0D, SOURCE_RADIUS * 2.0D,
                SOURCE_RADIUS * 2.0D);
        for (Mob victim : level.getEntitiesOfClass(Mob.class, bounds)) {
            if (shouldRepulseMob(entity == victim, isSourceExempt(victim), entity.distanceToSqr(victim))) {
                Vec3 push = repulsionPush(entity.position(), victim.position());
                if (push != Vec3.ZERO) {
                    victim.push(push.x, push.y, push.z);
                    victim.hurtMarked = true;
                }
            }
        }
        return true;
    }

    public static boolean isSourceTickPhase(long gameTime) {
        return gameTime % 5L == 3L;
    }

    public static boolean shouldRepulseMob(boolean sameEntity, boolean sourceExempt, double distanceSq) {
        return !sameEntity && !sourceExempt && distanceSq < SOURCE_RADIUS_SQ;
    }

    public static Vec3 repulsionPush(Vec3 source, Vec3 victim) {
        double dx = source.x - victim.x;
        double dy = source.y - victim.y;
        double dz = source.z - victim.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.001D || distance > SOURCE_RADIUS) {
            return Vec3.ZERO;
        }
        double scale = HORIZONTAL_PUSH / distance;
        return new Vec3(
                Mth.clamp(-dx * scale, -HORIZONTAL_PUSH, HORIZONTAL_PUSH),
                Mth.clamp(-dy * scale, MIN_VERTICAL_PUSH, MAX_VERTICAL_PUSH),
                Mth.clamp(-dz * scale, -HORIZONTAL_PUSH, HORIZONTAL_PUSH));
    }

    private static boolean isSourceExempt(LivingEntity entity) {
        return entity instanceof DemonEntity
                || entity instanceof IronGolem
                || entity instanceof SnowGolem
                || entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof BabaYagaEntity
                || entity instanceof DeathEntity
                || entity instanceof GoblinGulgEntity
                || entity instanceof GoblinMogEntity
                || entity instanceof HornedHuntsmanEntity
                || entity instanceof LeonardEntity
                || entity instanceof LordOfTormentEntity;
    }
}
