package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.BlightHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BrewOfBlightItem extends BrewItem {
    public BrewOfBlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos heartPos = pouredHeartPos(level, context.getClickedPos(), context.getClickedFace());
        if (heartPos == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlightHelper.placeHeart(serverLevel, heartPos, true);
        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos heartPos = player.blockPosition();
        if (!serverLevel.getBlockState(heartPos).canBeReplaced()) {
            heartPos = heartPos.above();
        }
        if (serverLevel.getBlockState(heartPos).canBeReplaced()) {
            BlightHelper.placeHeart(serverLevel, heartPos, true);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private static BlockPos pouredHeartPos(Level level, BlockPos clickedPos, net.minecraft.core.Direction face) {
        BlockState clicked = level.getBlockState(clickedPos);
        BlockPos candidate = clicked.canBeReplaced() ? clickedPos : clickedPos.relative(face);
        BlockState target = level.getBlockState(candidate);
        return target.canBeReplaced() && !target.hasBlockEntity() ? candidate : null;
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
