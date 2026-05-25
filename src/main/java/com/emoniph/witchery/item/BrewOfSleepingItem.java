package com.emoniph.witchery.item;

import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Sleeping - enters the Spirit World with the source near-guaranteed nightmare chance. */
public class BrewOfSleepingItem extends BrewItem {
    private static final double NIGHTMARE_CHANCE = 0.998;

    public BrewOfSleepingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            SpiritWorldHelper.enter(sp, NIGHTMARE_CHANCE);
        }
    }
}
