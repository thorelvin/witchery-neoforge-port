package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ArtichokeSeedItem extends Item {
    public ArtichokeSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos waterPos = hit.getBlockPos();
        BlockPos placePos = waterPos.above();
        BlockState waterState = level.getBlockState(waterPos);
        BlockState cropState = WitcheryBlocks.ARTICHOKE.get().defaultBlockState();

        if (!waterState.is(Blocks.WATER) || !waterState.getFluidState().isSource()) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, waterPos) || !player.mayUseItemAt(placePos, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(placePos).canBeReplaced() || !cropState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.setBlock(placePos, cropState, 11);
            SoundType soundType = cropState.getSoundType(level, placePos, player);
            level.playSound(null, placePos, soundType.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
