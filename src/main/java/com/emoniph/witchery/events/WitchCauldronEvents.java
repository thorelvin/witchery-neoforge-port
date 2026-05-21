package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.WitchCauldronBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitchCauldronEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !event.getItemStack().is(WitcheryItems.ANOINTING_PASTE.get())) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState clickedState = level.getBlockState(pos);
        int preservedLevel = getVanillaCauldronLevel(clickedState);
        if (preservedLevel < 0) {
            return;
        }

        Player player = event.getEntity();
        BlockState anointedState = WitcheryBlocks.WITCH_CAULDRON.get().defaultBlockState().setValue(WitchCauldronBlock.LEVEL, preservedLevel);
        level.setBlockAndUpdate(pos, anointedState);
        level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(WitcheryItems.ANOINTING_PASTE.get()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static int getVanillaCauldronLevel(BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            return 0;
        }
        if (state.is(Blocks.WATER_CAULDRON)) {
            return state.getValue(LayeredCauldronBlock.LEVEL);
        }
        return -1;
    }
}
