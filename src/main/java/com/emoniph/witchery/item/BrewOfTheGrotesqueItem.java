package com.emoniph.witchery.item;

import com.emoniph.witchery.effect.GrotesqueMobEffect;
import com.emoniph.witchery.events.WitcheryPlayerEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of the Grotesque — personal repulsion aura for 1200 ticks (60 seconds).
 *
 * Source: 1.7.10 Infusion.handleBrewGrotesqueEffect — each tick, all living entities
 * within 4 blocks (except bosses, golems, witches) are pushed away from the drinker.
 * Also changes the player's head model to the source grotesque demon head.
 */
public class BrewOfTheGrotesqueItem extends BrewItem {

    public BrewOfTheGrotesqueItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        WitcheryAttachments.get(player).setGrotesqueTicks(GrotesqueMobEffect.FIXED_BREW_DURATION_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            WitcheryPlayerEvents.syncToClient(serverPlayer);
        }
        player.displayClientMessage(
            Component.translatable("witchery.brew.grotesque.activate"), false);
    }
}
