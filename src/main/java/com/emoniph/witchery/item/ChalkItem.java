package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlockTags;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.function.Supplier;

/**
 * Chalk — draws or erases chalk circle marks on floors.
 * Different chalk variants place different circle block types.
 * Right-clicking the same mark erases it; right-clicking a different mark replaces it.
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

        // Only act on the top face of a block
        if (ctx.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        var above  = ctx.getClickedPos().above();
        var current = level.getBlockState(above);
        var target  = targetBlock.get();

        EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        if (current.is(WitcheryBlockTags.CHALK_CIRCLES)) {
            if (current.getBlock() == target) {
                // Same type → erase
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                level.playSound(null, above, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.5f, 1.2f);
            } else {
                // Different type → replace
                level.setBlock(above, target.defaultBlockState(), 3);
                level.playSound(null, above, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            return InteractionResult.CONSUME;
        }

        if (!current.canBeReplaced()) return InteractionResult.PASS;

        level.setBlock(above, target.defaultBlockState(), 3);
        level.playSound(null, above, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
        return InteractionResult.CONSUME;
    }
}
