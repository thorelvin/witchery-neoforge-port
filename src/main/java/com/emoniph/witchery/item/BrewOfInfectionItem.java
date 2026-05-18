package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Infection — spreads Poison through nearby mobs in a 12-block radius. */
public class BrewOfInfectionItem extends BrewItem {

    public BrewOfInfectionItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(12);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0)); // 15s Poison I
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0)); // 10s Weakness
        }
    }
}
