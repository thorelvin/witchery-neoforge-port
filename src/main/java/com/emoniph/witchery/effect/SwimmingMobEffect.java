package com.emoniph.witchery.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Swim Boost: the player-side branch multiplies horizontal water motion
 * while the forward key is held.
 */
public class SwimmingMobEffect extends MobEffect {
    public static final int PLAYER_SOURCE_STRENGTH = 3;
    public static final double BASE_HORIZONTAL_MULTIPLIER = 1.15D;
    public static final double HORIZONTAL_MULTIPLIER_STEP = 0.03D;
    public static final long NON_PLAYER_TICK_PHASE = 3L;
    public static final long NON_PLAYER_TICK_INTERVAL = 10L;

    public SwimmingMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    public static boolean applyPlayerClientTick(Player player, boolean forwardDown, boolean paralyzed) {
        if (!shouldBoostPlayer(player.isInWater(), forwardDown, paralyzed)) {
            return false;
        }

        player.setDeltaMovement(boostHorizontal(player.getDeltaMovement(), PLAYER_SOURCE_STRENGTH));
        return true;
    }

    public static boolean shouldBoostPlayer(boolean inWater, boolean forwardDown, boolean paralyzed) {
        return inWater && forwardDown && !paralyzed;
    }

    public static boolean shouldBoostNonPlayer(long gameTime, boolean inWater, boolean paralyzed) {
        return gameTime % NON_PLAYER_TICK_INTERVAL == NON_PLAYER_TICK_PHASE && inWater && paralyzed;
    }

    public static Vec3 boostHorizontal(Vec3 motion, int amplifier) {
        double multiplier = horizontalMultiplier(amplifier);
        return new Vec3(motion.x * multiplier, motion.y, motion.z * multiplier);
    }

    public static double horizontalMultiplier(int amplifier) {
        return BASE_HORIZONTAL_MULTIPLIER + HORIZONTAL_MULTIPLIER_STEP * amplifier;
    }
}
