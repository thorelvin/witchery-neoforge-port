package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Somnian Cotton in old Witchery is a spreading bush, not a growth-stage crop.
 * This ported block keeps the Spirit World spread rules and nightmare-sensitive
 * harvesting instead of acting like bonemealable farmland produce.
 */
public class CottonBlock extends BushBlock {

    public CottonBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isSourcePlacementSupport(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSourceBushLight(level.getRawBrightness(pos, 0), level.canSeeSky(pos))
                && super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        if (!canSourceSpread(level, pos)) {
            return;
        }

        int found = 5;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(scan).is(this)) {
                if (--found <= 0) {
                    return;
                }
            }
        }

        BlockPos center = pos;
        BlockPos candidate = randomSpreadCandidate(center, random);
        for (int i = 0; i < 4; i++) {
            if (canSpreadTo(level, candidate)) {
                center = candidate;
            }
            candidate = randomSpreadCandidate(center, random);
        }

        if (canSpreadTo(level, candidate)) {
            level.setBlock(candidate, defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockPos randomSpreadCandidate(BlockPos center, RandomSource random) {
        return center.offset(random.nextInt(3) - 1,
                random.nextInt(2) - random.nextInt(2),
                random.nextInt(3) - 1);
    }

    private boolean canSourceSpread(ServerLevel level, BlockPos pos) {
        return level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD) && hasFlowingSpiritBelowNeighbor(level, pos);
    }

    private boolean canSpreadTo(LevelReader level, BlockPos pos) {
        return level.isEmptyBlock(pos)
                && isSourceSpreadSupport(level.getBlockState(pos.below()))
                && canSurvive(defaultBlockState(), level, pos);
    }

    static boolean isSourcePlacementSupport(BlockState state) {
        return isSourceSpreadSupport(state) || state.is(Blocks.FARMLAND);
    }

    static boolean isSourceSpreadSupport(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL);
    }

    static boolean hasSourceBushLight(int rawBrightness, boolean canSeeSky) {
        return rawBrightness >= 8 || canSeeSky;
    }

    private boolean hasFlowingSpiritBelowNeighbor(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return isFlowingSpiritSource(level.getBlockState(below.north()))
                || isFlowingSpiritSource(level.getBlockState(below.south()))
                || isFlowingSpiritSource(level.getBlockState(below.east()))
                || isFlowingSpiritSource(level.getBlockState(below.west()));
    }

    static boolean isFlowingSpiritSource(BlockState state) {
        return state.is(WitcheryBlocks.FLOWING_SPIRIT.get()) && state.getValue(LiquidBlock.LEVEL) == 0;
    }
}
