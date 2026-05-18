package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Infused Brew of Soaring — grants improved broom control for 120 min (server time).
 * Stacks with owl familiar for an additional speed bonus.
 * Drinking again resets the duration; does not stack with itself.
 */
public class BrewOfSoaringItem extends BrewItem {

    // 120 minutes * 60 sec * 20 ticks
    private static final int DURATION = 120 * 60 * 20;

    public BrewOfSoaringItem(Properties props) { super(props); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setSoaringTicks(DURATION);
        sp.displayClientMessage(Component.translatable("witchery.brew.soaring.applied"), false);
    }
}
