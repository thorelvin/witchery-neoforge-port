package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Brew of Hollow Tears — the opposite of Flowing Spirit.
 * Weakens living players, grants Regeneration to undead creatures.
 */
public class BrewOfHollowTearsItem extends BrewItem {
    public BrewOfHollowTearsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        // Weaken the drinker
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));

        // Buff nearby undead with regeneration
        AABB area = new AABB(player.blockPosition()).inflate(10);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player) continue;
            if (e.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0));
            }
        }
    }
}
