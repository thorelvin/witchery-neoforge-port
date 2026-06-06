package com.emoniph.witchery.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SunAllergyMobEffect extends MobEffect {
    public static final int TICK_INTERVAL = 20;
    public static final float MIN_BRIGHTNESS = 0.5F;
    public static final float RANDOM_THRESHOLD_OFFSET = 0.45F;
    public static final int BURN_SECONDS = 8;
    public static final float PLAYER_DAMAGE = 1.0F;
    public static final float AMPLIFIED_PLAYER_DAMAGE = 2.0F;

    public SunAllergyMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        float brightness = entity.getLightLevelDependentMagicValue();
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        if (!shouldApplySun(level.getGameTime(), isSourceDaytime(level), brightness, entity.isInWater(),
                level.canSeeSky(pos), level.random.nextFloat())) {
            return true;
        }

        if (entity instanceof Player) {
            entity.hurt(level.damageSources().onFire(), playerDamage(amplifier));
            return true;
        }

        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            damageHelmet(level, entity, helmet);
            return true;
        }

        entity.igniteForSeconds(BURN_SECONDS);
        return true;
    }

    public static boolean shouldApplySun(long gameTime, boolean day, float brightness, boolean inWater,
                                         boolean canSeeSky, float randomRoll) {
        return gameTime % TICK_INTERVAL == 0L
                && day
                && brightness > MIN_BRIGHTNESS
                && !inWater
                && randomRoll < brightness - RANDOM_THRESHOLD_OFFSET
                && canSeeSky;
    }

    public static float playerDamage(int amplifier) {
        return amplifier >= 3 ? AMPLIFIED_PLAYER_DAMAGE : PLAYER_DAMAGE;
    }

    private static boolean isSourceDaytime(ServerLevel level) {
        return level.getDayTime() % 24000L < 12000L;
    }

    private static void damageHelmet(ServerLevel level, LivingEntity entity, ItemStack helmet) {
        if (helmet.isDamageableItem()) {
            helmet.hurtAndBreak(level.random.nextInt(2), entity, EquipmentSlot.HEAD);
        }
    }
}
