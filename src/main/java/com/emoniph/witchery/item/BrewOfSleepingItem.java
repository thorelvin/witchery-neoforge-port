package com.emoniph.witchery.item;

import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Sleeping — blindness + slowness + mining fatigue, simulates deep sleep. */
public class BrewOfSleepingItem extends BrewItem {

    public BrewOfSleepingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            SpiritWorldHelper.enter(sp);
        }
    }
}
