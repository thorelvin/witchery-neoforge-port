package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Source-style Spirit Bucket: places Flowing Spirit and leaves an empty bucket. */
public class SpiritBucketItem extends Item {
    public SpiritBucketItem(Properties properties) {
        super(properties.stacksTo(1).craftRemainder(Items.BUCKET));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockPos placePos = clickedPos.relative(face);
        if (!level.mayInteract(player, clickedPos) || !player.mayUseItemAt(placePos, face, stack)) {
            return InteractionResult.FAIL;
        }
        if (!canPlaceAt(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            BlockState state = WitcheryBlocks.FLOWING_SPIRIT.get().defaultBlockState();
            BlockState oldState = level.getBlockState(placePos);
            if (!oldState.getFluidState().isEmpty()) {
                level.destroyBlock(placePos, true);
            }
            level.setBlock(placePos, state, Block.UPDATE_ALL);
            level.playSound(null, placePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PLACE, placePos);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canPlaceAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.canBeReplaced()
                || (!state.isSolid() && !state.hasBlockEntity());
    }
}
