package com.emoniph.witchery.blocks;

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

public class BlightFibreBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape EAST_SHAPE = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
    private static final VoxelShape UP_SHAPE = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public BlightFibreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return BlightHelper.canBlightSurvive(level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = withAttachments(defaultBlockState(), context.getLevel(), context.getClickedPos());
        return hasAnyAttachment(state) && !BlightHelper.isProtected(context.getLevel(), context.getClickedPos()) ? state : null;
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
        if (level.getBlockState(pos).is(this)) {
            BlightHelper.trySpreadNear(level, pos, random, false);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        BlockState updated = withAttachments(state, level, pos);
        if (!hasAnyAttachment(updated) || BlightHelper.isProtected(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(updated, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
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
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    public static boolean hasAttachableFace(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (canAttachTo(level, pos, direction)) {
                return true;
            }
        }
        return false;
    }

    public static BlockState withAttachments(BlockState state, LevelReader level, BlockPos pos) {
        return state
                .setValue(NORTH, canAttachTo(level, pos, Direction.NORTH))
                .setValue(EAST, canAttachTo(level, pos, Direction.EAST))
                .setValue(SOUTH, canAttachTo(level, pos, Direction.SOUTH))
                .setValue(WEST, canAttachTo(level, pos, Direction.WEST))
                .setValue(UP, canAttachTo(level, pos, Direction.UP))
                .setValue(DOWN, canAttachTo(level, pos, Direction.DOWN));
    }

    public static boolean hasAnyAttachment(BlockState state) {
        return state.getValue(NORTH)
                || state.getValue(EAST)
                || state.getValue(SOUTH)
                || state.getValue(WEST)
                || state.getValue(UP)
                || state.getValue(DOWN);
    }

    private static boolean canAttachTo(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos supportPos = pos.relative(direction);
        BlockState support = level.getBlockState(supportPos);
        return !support.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                && support.isFaceSturdy(level, supportPos, direction.getOpposite());
    }
}
