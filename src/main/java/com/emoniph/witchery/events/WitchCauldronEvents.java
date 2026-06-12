package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.WitchCauldronBlock;
import com.emoniph.witchery.brewing.Brews;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Game-event listeners for the Witch's Cauldron.
 *
 * <p>The cauldron-anointing behaviour (Anointing Paste on a vanilla Cauldron) lives on
 * {@link com.emoniph.witchery.item.AnointingPasteItem#useOn}.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitchCauldronEvents {

    /**
     * Revives the sneak + right-click "stir" gesture for custom-brew ingredients.
     *
     * <p>Vanilla skips a block's {@code useItemOn} when a player sneaks with a non-empty hand
     * ({@code ServerPlayerGameMode#useItemOn} — {@code flag1}), so the cauldron never saw the stir and
     * capacity ingredients like Mandrake Root could not be added. {@code RightClickBlock} fires before
     * that gate, so we perform the stir here for {@link Brews#requiresExplicitStir} ingredients.
     */
    @SubscribeEvent
    public static void onSneakStirBrew(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive()) {
            return; // a normal right-click already reaches the cauldron's useItemOn
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !Brews.requiresExplicitStir(stack.getItem())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof WitchCauldronBlock)) {
            return;
        }
        InteractionResult result = WitchCauldronBlock.stirExplicitIngredient(
                stack, level.getBlockState(pos), level, pos, player);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
