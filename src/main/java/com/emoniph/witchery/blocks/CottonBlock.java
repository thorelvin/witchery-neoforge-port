package com.emoniph.witchery.blocks;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
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
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) {
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

        BlockPos candidate = pos;
        for (int i = 0; i < 4; i++) {
            BlockPos next = candidate.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            if (canSpreadTo(level, next)) {
                candidate = next;
            }
        }

        if (canSpreadTo(level, candidate)) {
            level.setBlock(candidate, defaultBlockState(), 3);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                              net.minecraft.world.item.ItemStack tool) {
        if (!shouldDropDisturbedCotton(level, player)) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        popResource(serverLevel, pos, WitcheryItems.DISTURBED_COTTON.get().getDefaultInstance());
    }

    private boolean canSourceSpread(ServerLevel level, BlockPos pos) {
        return level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD) || hasFlowingSpiritBelowNeighbor(level, pos);
    }

    private boolean canSpreadTo(LevelReader level, BlockPos pos) {
        return level.isEmptyBlock(pos) && canSurvive(defaultBlockState(), level, pos);
    }

    private boolean hasFlowingSpiritBelowNeighbor(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below.north()).is(WitcheryBlocks.FLOWING_SPIRIT.get())
                || level.getBlockState(below.south()).is(WitcheryBlocks.FLOWING_SPIRIT.get())
                || level.getBlockState(below.east()).is(WitcheryBlocks.FLOWING_SPIRIT.get())
                || level.getBlockState(below.west()).is(WitcheryBlocks.FLOWING_SPIRIT.get());
    }

    private boolean shouldDropDisturbedCotton(Level level, Player player) {
        if (!level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            return false;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        return data.isNightmare();
    }
}
