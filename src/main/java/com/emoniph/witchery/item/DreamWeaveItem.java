package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Places the source-style Dream Catcher host block carrying this weave. */
public class DreamWeaveItem extends Item {
    private final DreamWeaveType weaveType;

    public DreamWeaveItem(DreamWeaveType weaveType, Properties props) {
        super(props);
        this.weaveType = weaveType;
    }

    public DreamWeaveType getWeaveType() { return weaveType; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof DreamCatcherBlockEntity be) {
            if (be.getWeave() != null) {
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            be.setWeave(weaveType);
            if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative())
                ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }

        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = ctx.getClickedPos().relative(face);
        BlockState placedState = WitcheryBlocks.DREAM_CATCHER.get()
                .defaultBlockState()
                .setValue(DreamCatcherBlock.FACING, face)
                .setValue(DreamCatcherBlock.WEAVE, weaveType.id + 1);
        if (!level.getBlockState(placePos).canBeReplaced() || !placedState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, placedState, 3)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(placePos) instanceof DreamCatcherBlockEntity be) {
            be.setWeave(weaveType);
        }
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative())
            ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
