package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of the Grotesque — personal repulsion aura for 1200 ticks (60 seconds).
 *
 * Source: 1.7.10 Infusion.handleBrewGrotesqueEffect — each tick, all living entities
 * within 4 blocks (except bosses, golems, witches) are pushed away from the drinker.
 * Also changes the player's head model to a grotesque demon-head (cosmetic; handled
 * separately as a render-layer pass when ready).
 */
public class BrewOfTheGrotesqueItem extends BrewItem {

    public BrewOfTheGrotesqueItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        // Set the 1200-tick repulsion timer (transient, not serialized)
        WitcheryAttachments.get(player).setGrotesqueTicks(1200);
        player.displayClientMessage(
            Component.translatable("witchery.brew.grotesque.activate"), false);
    }
}
