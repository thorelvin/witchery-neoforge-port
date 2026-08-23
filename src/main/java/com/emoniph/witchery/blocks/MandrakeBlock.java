package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.MandrakeEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class MandrakeBlock extends WitchCropBlock {
    public static final IntegerProperty MANDRAKE_AGE = BlockStateProperties.AGE_4;
    static final int SOURCE_MATURE_AGE = 4;
    static final double SOURCE_DAY_SUMMON_CHANCE = 0.9D;
    static final double SOURCE_NIGHT_SUMMON_CHANCE = 0.1D;
    static final int SOURCE_EMERGENCE_PARTICLES = 20;

    public MandrakeBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() { return MANDRAKE_AGE; }

    @Override
    public int getMaxAge() { return SOURCE_MATURE_AGE; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MANDRAKE_AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.MANDRAKE_SEEDS.get(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return WitchCropSoilRules.supports(this, state);
    }

    // Mature mandrake only summon the hostile root creature under the original
    // difficulty and time-based chance rules; otherwise they drop normally.
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.world.item.ItemStack tool) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.playerDestroy(level, player, pos, state, be, tool);
            return;
        }

        if (!shouldSummonMandrake(state.getValue(MANDRAKE_AGE), level.getDifficulty(),
                isSourceDaytime(level.getDayTime()), level.random.nextDouble())) {
            super.playerDestroy(level, player, pos, state, be, tool);
            return;
        }

        MandrakeEntity mob = WitcheryEntities.MANDRAKE_MOB.get().create(
                serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (mob == null) {
            super.playerDestroy(level, player, pos, state, be, tool);
            return;
        }

        mob.setPos(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D);
        if (!serverLevel.addFreshEntity(mob)) {
            super.playerDestroy(level, player, pos, state, be, tool);
            return;
        }

        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        serverLevel.sendParticles(ParticleTypes.POOF,
                mob.getX(), mob.getY(), mob.getZ(),
                SOURCE_EMERGENCE_PARTICLES, 0.5D, 1.0D, 0.5D, 0.0D);
    }

    static boolean shouldSummonMandrake(int age, Difficulty difficulty, boolean daytime, double roll) {
        double chance = daytime ? SOURCE_DAY_SUMMON_CHANCE : SOURCE_NIGHT_SUMMON_CHANCE;
        return age >= SOURCE_MATURE_AGE
                && difficulty != Difficulty.PEACEFUL
                && roll <= chance;
    }

    static boolean isSourceDaytime(long dayTime) {
        return Math.floorMod(dayTime, 24000L) < 13000L;
    }
}
