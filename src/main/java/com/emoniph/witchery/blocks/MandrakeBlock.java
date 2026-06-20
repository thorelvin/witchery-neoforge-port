package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.MandrakeEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class MandrakeBlock extends CropBlock {
    public static final IntegerProperty MANDRAKE_AGE = BlockStateProperties.AGE_4;

    public MandrakeBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() { return MANDRAKE_AGE; }

    @Override
    public int getMaxAge() { return 4; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MANDRAKE_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.MANDRAKE_SEEDS.get(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Source BlockWitchCrop (waterPlant=false): grass, dirt, or farmland.
        return state.is(Blocks.FARMLAND) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    // Mature mandrake only summon the hostile root creature under the original
    // difficulty and time-based chance rules; otherwise they drop normally.
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.world.item.ItemStack tool) {
        if (!shouldSummonMandrake(level, state)) {
            super.playerDestroy(level, player, pos, state, be, tool);
            return;
        }

        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);

        MandrakeEntity mob = WitcheryEntities.MANDRAKE_MOB.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (mob == null) return;
        mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sl.addFreshEntity(mob);
    }

    private boolean shouldSummonMandrake(Level level, BlockState state) {
        if (state.getValue(MANDRAKE_AGE) < getMaxAge()) {
            return false;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }

        double chance = (level.getDayTime() % 24000L) < 13000L ? 0.9 : 0.1;
        return level.random.nextDouble() <= chance;
    }
}
