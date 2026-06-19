package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;

/**
 * Overheating — source {@code PotionOverheating} (1.7.10-0.24.1): an incurable curse that, in
 * hot and dry biomes, randomly sets the afflicted alight. Cool/wet biomes (or rain in a rainy
 * biome) and standing in water keep them safe, matching the source temperature gate.
 */
public class OverheatingMobEffect extends MobEffect {

    private static final float HOT_BIOME_TEMPERATURE = 1.5F;

    public OverheatingMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Source gate: world time % 5 == 3 -> use the remaining-duration parity to pulse ~every 5 ticks.
        return duration % 5 == 3;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.isOnFire() || entity.isInWater()) {
            return true;
        }
        int chance = amplifier > 1 ? 20 : (amplifier > 0 ? 25 : 30);
        if (level.random.nextInt(chance) != 0) {
            return true;
        }
        Holder<Biome> biome = level.getBiome(entity.blockPosition());
        float temperature = biome.value().getBaseTemperature();
        boolean rainingHere = biome.value().hasPrecipitation() && level.isRaining();
        if (temperature >= HOT_BIOME_TEMPERATURE && !rainingHere) {
            int seconds = Math.min(level.random.nextInt(amplifier < 3 ? 2 : amplifier) + 1, 4);
            entity.igniteForSeconds(seconds);
        }
        return true;
    }
}
