package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Wasting — inflicts Wither + Slowness on all mobs within 8 blocks. */
public class BrewOfWastingItem extends BrewItem {

    public BrewOfWastingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(8);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,           200, 1)); // 10s Wither II
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 2)); // 20s Slowness III
        }
    }
}
