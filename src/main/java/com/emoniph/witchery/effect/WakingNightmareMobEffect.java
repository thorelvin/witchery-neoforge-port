package com.emoniph.witchery.effect;

import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Source {@code PotionWakingNightmare}: periodically sends one personal Nightmare after its victim. */
public final class WakingNightmareMobEffect extends MobEffect {
    public WakingNightmareMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 3;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)
                || level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || level.random.nextInt(amplifier > 3 ? 30 : amplifier > 1 ? 60 : 180) != 0) {
            return true;
        }
        AABB bounds = entity.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        boolean alreadyHunted = !level.getEntitiesOfClass(NightmareEntity.class, bounds,
                nightmare -> nightmare.matchesVictim(player)).isEmpty();
        if (alreadyHunted) {
            return true;
        }
        NightmareEntity nightmare = WitcheryEntities.NIGHTMARE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (nightmare != null) {
            nightmare.snapTo(entity.getX() + 2.0D, entity.getY(), entity.getZ() + 2.0D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            nightmare.setVictim(player);
            nightmare.setTarget(player);
            level.addFreshEntity(nightmare);
        }
        return true;
    }
}
