package com.emoniph.witchery.blocks;

import com.emoniph.witchery.data.BlightHeartSavedData;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlightFibreBlock extends VineBlock {
    public static final BooleanProperty NORTH = VineBlock.NORTH;
    public static final BooleanProperty EAST = VineBlock.EAST;
    public static final BooleanProperty SOUTH = VineBlock.SOUTH;
    public static final BooleanProperty WEST = VineBlock.WEST;
    public static final BooleanProperty UP = VineBlock.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final Direction[] ATTACHMENT_DIRECTIONS = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final VoxelShape DOWN_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    private static final VoxelShape UP_SHAPE = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape EAST_SHAPE = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);

    public BlightFibreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !BlightHelper.isProtected(level, pos)
                && hasAnyAttachment(withAttachments(state, level, pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (BlightHelper.isProtected(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        BlockState state = withAttachments(defaultBlockState(), context.getLevel(), context.getClickedPos());
        return hasAnyAttachment(state) ? state : null;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        BlightHelper.decayIfOrphaned(level, pos, state, random);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        BlightHeartSavedData.get(level.getServer()).forgetMutation(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        BlockState updated = withAttachments(state, level, pos);
        if (!hasAnyAttachment(updated) || BlightHelper.isProtected(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return updated;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && level.random.nextInt(120) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide() && tool.is(Items.SHEARS) && !player.getAbilities().instabuild) {
            popResource(level, pos, new ItemStack(WitcheryItems.BLIGHT_ICHOR.get()));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) != 0) {
            return;
        }
        double y = state.getValue(DOWN) ? 0.06D : 0.25D + random.nextDouble() * 0.5D;
        level.addParticle(ParticleTypes.WITCH,
                pos.getX() + random.nextDouble(),
                pos.getY() + y,
                pos.getZ() + random.nextDouble(),
                0.0D, 0.01D, 0.0D);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST, DOWN);
    }

    public static boolean hasAttachableFace(LevelReader level, BlockPos pos) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            if (canAttachTo(level, pos, direction)) {
                return true;
            }
        }
        return false;
    }

    public static BlockState withAttachments(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            state = state.setValue(propertyForFace(direction), canAttachTo(level, pos, direction));
        }
        return state;
    }

    public static boolean hasAnyAttachment(BlockState state) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            if (state.getValue(propertyForFace(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canAttachTo(LevelReader level, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
            BlockPos supportPos = pos.below();
            BlockState support = level.getBlockState(supportPos);
            return !support.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                    && VineBlock.isAcceptableNeighbour(level, supportPos, Direction.DOWN);
        }

        BlockPos supportPos = pos.relative(direction);
        BlockState support = level.getBlockState(supportPos);
        if (!support.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                && VineBlock.isAcceptableNeighbour(level, supportPos, direction)) {
            return true;
        }

        if (direction.getAxis().isHorizontal()) {
            BlockState above = level.getBlockState(pos.above());
            return above.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                    && above.getValue(propertyForFace(direction));
        }

        return false;
    }

    private static BooleanProperty propertyForFace(Direction direction) {
        return direction == Direction.DOWN ? DOWN : VineBlock.getPropertyForFace(direction);
    }
}
