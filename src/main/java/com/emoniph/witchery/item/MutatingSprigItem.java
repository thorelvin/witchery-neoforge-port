package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mutating Sprig - a controlled mutation tool for a few specific block changes.
 * This is intentionally bounded for now: the core block transformations are live,
 * while the wider advanced mutation-structure family remains follow-up work.
 */
public class MutatingSprigItem extends Item {

    public MutatingSprigItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!tryMutate(level, pos, state)) {
            return InteractionResult.PASS;
        }

        spawnEffects(level, pos);
        if (ctx.getPlayer() != null) {
            EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean tryMutate(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.MYCELIUM)) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            return true;
        }
        if (!hasWaterOrIceAbove(level, pos)) {
            return false;
        }
        if (state.is(Blocks.DIRT)) {
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.CLAY)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static boolean hasWaterOrIceAbove(Level level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        return aboveState.is(Blocks.WATER) || aboveState.is(Blocks.ICE);
    }

    private static void spawnEffects(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS,
                0.6F, 0.5F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    8,
                    0.3,
                    0.3,
                    0.3,
                    0.1);
        }
    }
}
