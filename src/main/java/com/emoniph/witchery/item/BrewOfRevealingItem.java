package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Brew of Revealing - exposes nearby invisible or hidden living entities. */
public class BrewOfRevealingItem extends BrewItem {
    private static final double RADIUS = 5.0;

    public BrewOfRevealingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = player.getBoundingBox().inflate(RADIUS);
        double radiusSq = RADIUS * RADIUS;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.distanceToSqr(player) > radiusSq) continue;
            entity.removeEffect(MobEffects.INVISIBILITY);
            if (entity.isInvisible()) {
                entity.setInvisible(false);
            }
        }
    }
}
