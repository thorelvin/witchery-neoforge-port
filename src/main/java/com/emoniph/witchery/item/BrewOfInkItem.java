package com.emoniph.witchery.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Throws an ink explosion — blinds and darkens all entities in a 10-block radius. */
public class BrewOfInkItem extends BrewItem {
    public BrewOfInkItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(10);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS,  100, 0));
            for (int i = 0; i < 8; i++) {
                double ox = (level.getRandom().nextDouble() - 0.5) * 0.6;
                double oz = (level.getRandom().nextDouble() - 0.5) * 0.6;
                level.addParticle(ParticleTypes.SQUID_INK,
                        e.getX() + ox, e.getY() + e.getBbHeight() * 0.5, e.getZ() + oz,
                        ox * 0.3, 0.05, oz * 0.3);
            }
        }
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.GLOW_SQUID_SQUIRT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
    }
}
