package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Foulness — nauseates and poisons all nearby living entities (mobs and players). */
public class BrewOfFoulnessItem extends BrewItem {

    public BrewOfFoulnessItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(8);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0)); // 15s Nausea
            target.addEffect(new MobEffectInstance(MobEffects.POISON,    200, 1)); // 10s Poison II
        }
        // Drinker also gets mild nausea (it smells awful)
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
    }
}
