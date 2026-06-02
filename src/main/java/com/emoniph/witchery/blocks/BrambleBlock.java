package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wild bramble hurts entities that push through it and can spread into nearby
 * open tiles when broken without shears.
 */
public class BrambleBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.8, 0.0, 0.8, 15.2, 16.0, 15.2);

    public BrambleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
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
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (!level.isClientSide()) {
            entity.hurt(level.damageSources().cactus(), 1.0F);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (level.isClientSide() || tool.is(Items.SHEARS)) {
            return;
        }

        spreadToIfEmpty(level, pos.offset(1, 0, 0));
        spreadToIfEmpty(level, pos.offset(0, 0, 1));
        spreadToIfEmpty(level, pos.offset(-1, 0, 0));
        spreadToIfEmpty(level, pos.offset(0, 0, -1));
        spreadToIfEmpty(level, pos.offset(1, 0, 1));
        spreadToIfEmpty(level, pos.offset(-1, 0, -1));
        spreadToIfEmpty(level, pos.offset(-1, 0, 1));
        spreadToIfEmpty(level, pos.offset(1, 0, -1));
    }

    private void spreadToIfEmpty(Level level, BlockPos origin) {
        for (int y = origin.getY() - 1; y <= origin.getY() + 1; y++) {
            BlockPos candidate = new BlockPos(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(candidate);
            if (!canBrambleReplace(state)) {
                continue;
            }
            if (level.getBlockState(candidate.below()).isAir()) {
                continue;
            }
            if (level.random.nextInt(2) == 0) {
                level.setBlock(candidate, defaultBlockState(), 3);
                if (level.random.nextInt(3) != 0) {
                    break;
                }
            }
        }
    }

    private boolean canBrambleReplace(BlockState state) {
        return state.isAir()
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN);
    }
}
