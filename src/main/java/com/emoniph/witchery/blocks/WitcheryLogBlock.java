package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.EntEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WitcheryLogBlock extends RotatedPillarBlock {
    private static final int MIN_SPAWN_DISTANCE = 8;
    private static final int MAX_SPAWN_DISTANCE = 16;
    private static final int MAX_HEIGHT_SCAN = 8;
    private static final int MIN_HEADROOM = 3;

    public WitcheryLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            trySpawnEnt(serverLevel, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void trySpawnEnt(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        double chance = Math.min(0.05, 0.01 + adjacentWitcheryLogs(level, pos) * 0.01);
        if (random.nextDouble() >= chance) {
            return;
        }

        BlockPos spawnPos = findEntSpawnPos(level, pos, random);
        if (spawnPos == null) {
            return;
        }

        EntEntity ent = WitcheryEntities.ENT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ent == null) {
            return;
        }

        ent.snapTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        ent.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);
        level.addFreshEntityWithPassengers(ent);
    }

    private static int adjacentWitcheryLogs(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (isWitcheryLog(level.getBlockState(pos.relative(direction)))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isWitcheryLog(BlockState state) {
        return state.is(WitcheryBlocks.ROWAN_LOG.get())
                || state.is(WitcheryBlocks.ALDER_LOG.get())
                || state.is(WitcheryBlocks.HAWTHORN_LOG.get());
    }

    private static BlockPos findEntSpawnPos(ServerLevel level, BlockPos source, RandomSource random) {
        int x = source.getX() + outerBandOffset(random);
        int z = source.getZ() + outerBandOffset(random);
        int y = source.getY();

        while (!level.isEmptyBlock(new BlockPos(x, y, z)) && y < source.getY() + MAX_HEIGHT_SCAN) {
            y++;
        }

        while (level.isEmptyBlock(new BlockPos(x, y, z)) && y > level.getMinY()) {
            y--;
        }

        BlockPos ground = new BlockPos(x, y, z);
        int headroom = 0;
        while (level.isEmptyBlock(ground.above(headroom + 1)) && headroom < MIN_HEADROOM) {
            headroom++;
        }
        return headroom >= MIN_HEADROOM ? ground.above() : null;
    }

    private static int outerBandOffset(RandomSource random) {
        int activeRadius = MIN_SPAWN_DISTANCE;
        int value = random.nextInt(activeRadius * 2 + 1);
        if (value > activeRadius) {
            value += MAX_SPAWN_DISTANCE;
        }
        return -MAX_SPAWN_DISTANCE + value;
    }
}
