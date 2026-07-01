package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.blocks.PlacedItemBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

final class AltarPlacedItemSupport {

    private AltarPlacedItemSupport() {
    }

    static InteractionResult placeOnAltar(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();

        if (face != Direction.UP) return InteractionResult.PASS;
        if (!level.getBlockState(clickedPos).is(WitcheryBlocks.ALTAR.get())) return InteractionResult.PASS;

        BlockPos above = clickedPos.above();
        if (!level.getBlockState(above).isAir()) return InteractionResult.PASS;
        if (!WitcheryTerrainProtection.canPlace(ctx.getPlayer(), level, above)) return InteractionResult.FAIL;

        if (!level.isClientSide()) {
            Direction facing = ctx.getPlayer() != null ? ctx.getPlayer().getDirection() : Direction.SOUTH;
            level.setBlock(above, WitcheryBlocks.PLACED_ITEM.get().defaultBlockState()
                    .setValue(PlacedItemBlock.FACING, facing), 3);
            if (level.getBlockEntity(above) instanceof PlacedItemBlockEntity be) {
                ItemStack toPlace = ctx.getItemInHand().copyWithCount(1);
                be.setStack(toPlace);
                level.sendBlockUpdated(above, level.getBlockState(above), level.getBlockState(above), 3);
            }
            if (level.getBlockEntity(clickedPos) instanceof AltarBlockEntity altar) {
                altar.invalidate(level);
            }
            if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
