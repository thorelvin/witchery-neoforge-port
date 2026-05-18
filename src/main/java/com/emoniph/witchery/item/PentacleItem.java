package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Koboldite Pentacle — placed on an altar top to multiply its recharge rate x2.
 * Places a PlacedItemBlock holding this item above the altar block.
 */
public class PentacleItem extends Item {

    public PentacleItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();

        if (face != Direction.UP) return InteractionResult.PASS;
        if (!level.getBlockState(clickedPos).is(WitcheryBlocks.ALTAR.get())) return InteractionResult.PASS;

        BlockPos above = clickedPos.above();
        if (!level.getBlockState(above).isAir()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            level.setBlock(above, WitcheryBlocks.PLACED_ITEM.get().defaultBlockState(), 3);
            if (level.getBlockEntity(above) instanceof PlacedItemBlockEntity be) {
                ItemStack toPlace = ctx.getItemInHand().copyWithCount(1);
                be.setStack(toPlace);
                level.sendBlockUpdated(above, level.getBlockState(above), level.getBlockState(above), 3);
            }
            if (!ctx.getPlayer().getAbilities().instabuild)
                ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
