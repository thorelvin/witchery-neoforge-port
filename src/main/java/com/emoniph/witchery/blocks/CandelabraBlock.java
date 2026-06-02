package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.CandelabraBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CandelabraBlock extends BaseEntityBlock {
    public static final MapCodec<CandelabraBlock> CODEC = simpleCodec(CandelabraBlock::new);
    private static final VoxelShape SHAPE = box(1.6, 0.0, 1.6, 14.4, 16.0, 14.4);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public CandelabraBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                @Nullable Orientation orientation, boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double yMid = pos.getY() + 1.05;
        double yOuter = pos.getY() + 0.9;
        spawnFlame(level, random, pos.getX() + 0.5, yMid, pos.getZ() + 0.5);
        spawnFlame(level, random, pos.getX() + 0.8, yOuter, pos.getZ() + 0.5);
        spawnFlame(level, random, pos.getX() + 0.2, yOuter, pos.getZ() + 0.5);
        spawnFlame(level, random, pos.getX() + 0.5, yOuter, pos.getZ() + 0.8);
        spawnFlame(level, random, pos.getX() + 0.5, yOuter, pos.getZ() + 0.2);
    }

    private static void spawnFlame(Level level, RandomSource random, double x, double y, double z) {
        if (random.nextInt(4) == 0) {
            return;
        }
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandelabraBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
