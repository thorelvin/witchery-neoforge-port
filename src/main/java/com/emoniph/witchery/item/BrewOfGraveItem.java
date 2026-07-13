package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Infused Brew of the Grave - source 120-minute undead-command empowerment. */
public class BrewOfGraveItem extends BrewItem {
    public static final int DURATION_TICKS = 120 * 60 * 20;

    public BrewOfGraveItem(Properties props) {
        super(props);
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setGraveTicks(DURATION_TICKS);
        sp.displayClientMessage(Component.translatable("witchery.brew.grave.applied"), false);
    }
}
