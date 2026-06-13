package com.emoniph.witchery.blocks;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.CursedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Triggered-dispersal block cursing — the port of source {@code DispersalTriggered.onImpactSplashPotion},
 * but data-driven (see {@link CursedBlockData}): a triggered brew splash that lands on a button / lever /
 * pressure plate / door records a curse on that block's position. The block itself is left untouched, so
 * it keeps its exact wood type and state; activation is intercepted in {@code CursedBlockEvents}.
 */
public final class CursedBlocks {

    private CursedBlocks() {}

    /** Curse the block the splash hit, if it is a curseable type. */
    public static boolean tryCurse(ServerLevel level, BlockHitResult hit, BrewContents brew, @Nullable Entity thrower) {
        UUID id = thrower == null ? null : thrower.getUUID();
        BlockPos hitPos = hit.getBlockPos();
        // A thrown brew clips on COLLIDER shapes, so wall/floor-mounted buttons, levers, and pressure
        // plates (no collision) are passed through — the bottle hits the block they are mounted on and the
        // real target sits one step out along the hit face. Try both positions.
        if (curseAt(level, hitPos, brew, id)) {
            return true;
        }
        return curseAt(level, hitPos.relative(hit.getDirection()), brew, id);
    }

    private static boolean curseAt(ServerLevel level, BlockPos pos, BrewContents brew, @Nullable UUID thrower) {
        BlockPos target = curseTarget(level.getBlockState(pos), pos);
        if (target == null) {
            return false;
        }
        CursedBlockData.get(level).curse(target, brew, thrower);
        level.sendParticles(ParticleTypes.WITCH, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.0);
        return true;
    }

    /** The position a curse should be recorded at for this block (doors normalise to the lower half), or
     * null if the block is not curseable. */
    @Nullable
    private static BlockPos curseTarget(BlockState state, BlockPos pos) {
        if (state.is(Blocks.LEVER) || state.is(BlockTags.BUTTONS) || state.getBlock() instanceof PressurePlateBlock) {
            return pos;
        }
        if (state.is(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock) {
            return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.immutable() : pos.below();
        }
        return null;
    }

    /** Buttons, levers, and doors are activated by a right-click (handled via the interaction event). */
    public static boolean isRightClickActivated(BlockState state) {
        return state.is(BlockTags.BUTTONS) || state.is(Blocks.LEVER)
                || (state.is(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock);
    }

    /** Pressure plates have no interaction event — they are polled on the rising edge of POWERED. */
    public static boolean isPressurePlate(BlockState state) {
        return state.getBlock() instanceof PressurePlateBlock;
    }
}
