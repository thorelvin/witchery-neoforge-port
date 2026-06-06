package com.emoniph.witchery.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Floating: keep entities hovering just above nearby blocks.
 */
public class FloatingMobEffect extends MobEffect {
    public static final double RAISED_Y_VELOCITY = 0.25D;
    public static final double UNSUPPORTED_DRIFT_Y_VELOCITY = -0.05D;

    public FloatingMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.fallDistance = 0.0F;
        Vec3 motion = entity.getDeltaMovement();
        double yVelocity = hasBlockBelow(level, entity, hoverHeight(amplifier))
                ? RAISED_Y_VELOCITY
                : unsupportedYVelocity(level);
        entity.setDeltaMovement(motion.x, yVelocity, motion.z);
        entity.hurtMarked = true;
        return true;
    }

    public static int hoverHeight(int amplifier) {
        return 3 + amplifier;
    }

    public static double unsupportedYVelocity(ServerLevel level) {
        return level.random.nextInt(5) == 0 ? UNSUPPORTED_DRIFT_Y_VELOCITY : 0.0D;
    }

    private static boolean hasBlockBelow(ServerLevel level, LivingEntity entity, int height) {
        BlockPos origin = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        for (int i = 1; i <= height; i++) {
            if (!level.isEmptyBlock(origin.below(i))) {
                return true;
            }
        }
        return false;
    }
}
