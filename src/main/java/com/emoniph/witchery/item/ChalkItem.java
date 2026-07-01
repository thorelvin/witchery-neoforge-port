package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Chalk draws floor glyphs. Same-type use rerolls the glyph art, like source metadata.
 */
public class ChalkItem extends Item {

    private final Supplier<Block> targetBlock;

    public ChalkItem(Supplier<Block> targetBlock, Properties props) {
        super(props);
        this.targetBlock = targetBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Block target = targetBlock.get();
        BlockPosAndState mark = targetMark(level, ctx);
        if (mark == null) return InteractionResult.PASS;

        EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        BlockState next = placementState(level, target, mark.state);

        if (mark.state.is(WitcheryBlockTags.CHALK_CIRCLES)) {
            if (!WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, mark.pos)) {
                return InteractionResult.FAIL;
            }
            level.setBlock(mark.pos, next, 3);
            level.playSound(null, mark.pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
            if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            return InteractionResult.CONSUME;
        }

        if (ctx.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!mark.state.canBeReplaced()) return InteractionResult.PASS;
        if (!next.canSurvive(level, mark.pos)) return InteractionResult.PASS;
        if (!(mark.state.isAir()
                ? WitcheryTerrainProtection.canPlace(ctx.getPlayer(), level, mark.pos)
                : WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, mark.pos))) {
            return InteractionResult.FAIL;
        }

        level.setBlock(mark.pos, next, 3);
        level.playSound(null, mark.pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
        return InteractionResult.CONSUME;
    }

    private static BlockPosAndState targetMark(Level level, UseOnContext ctx) {
        var clicked = ctx.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.is(WitcheryBlockTags.CHALK_CIRCLES)) {
            return new BlockPosAndState(clicked, clickedState);
        }

        var above = clicked.above();
        return new BlockPosAndState(above, level.getBlockState(above));
    }

    private static BlockState placementState(Level level, Block target, BlockState current) {
        BlockState state = target.defaultBlockState();
        if (!state.hasProperty(ChalkCircleBlock.GLYPH)) {
            return state;
        }

        int glyph = level.getRandom().nextInt(ChalkCircleBlock.GLYPH_VARIANTS);
        if (current.is(target) && current.hasProperty(ChalkCircleBlock.GLYPH)
                && ChalkCircleBlock.GLYPH_VARIANTS > 1) {
            int previous = current.getValue(ChalkCircleBlock.GLYPH);
            glyph = level.getRandom().nextInt(ChalkCircleBlock.GLYPH_VARIANTS - 1);
            if (glyph >= previous) {
                glyph++;
            }
        }
        return ChalkCircleBlock.withGlyph(state, glyph);
    }

    private record BlockPosAndState(net.minecraft.core.BlockPos pos, BlockState state) {}
}
