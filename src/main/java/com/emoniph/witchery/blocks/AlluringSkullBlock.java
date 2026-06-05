package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.AlluringSkullBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import org.jetbrains.annotations.Nullable;

public class AlluringSkullBlock extends BaseEntityBlock {

    public static final MapCodec<AlluringSkullBlock> CODEC = simpleCodec(AlluringSkullBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final VoxelShape FLOOR_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape WEST_SHAPE = Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);
    private static final int FEEDBACK_PARTICLES = 20;
    private static final double FEEDBACK_WIDTH = 0.5;
    private static final double FEEDBACK_HEIGHT = 0.5;
    private static final float FEEDBACK_VOLUME = 0.5F;

    public AlluringSkullBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ON, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LevelReader level = context.getLevel();
        BlockState floorState = defaultBlockState().setValue(FACING, Direction.UP);

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction == Direction.UP) {
                continue;
            }

            BlockState state = direction == Direction.DOWN
                    ? floorState
                    : defaultBlockState().setValue(FACING, direction.getOpposite());
            if (state.canSurvive(level, pos) && level.isUnobstructed(state, pos, CollisionContext.empty())) {
                return state;
            }
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || state.getValue(FACING) != Direction.UP || !(level.getBlockEntity(pos) instanceof AlluringSkullBlockEntity skull)) {
            return;
        }
        float rotation = placer != null ? placer.getYRot() : 0.0F;
        skull.setRotationSegment(RotationSegment.convertToSegment(rotation));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = facing == Direction.UP ? pos.below() : pos.relative(facing.getOpposite());
        return Block.canSupportCenter(level, supportPos, facing == Direction.UP ? Direction.UP : facing);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> FLOOR_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(WitcheryItems.NECRO_STONE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!state.getValue(ON)) {
            level.setBlock(pos, state.setValue(ON, true), 3);
            playFeedback(level, pos, ParticleTypes.FLAME, SoundEvents.SKELETON_HORSE_DEATH);
        } else {
            playFeedback(level, pos, ParticleTypes.EXPLOSION, SoundEvents.SKELETON_HORSE_HURT);
            level.removeBlock(pos, false);
            level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    WitcheryItems.ALLURING_SKULL.get().getDefaultInstance()));
        }
        return InteractionResult.SUCCESS;
    }

    private static void playFeedback(Level level, BlockPos pos, ParticleOptions particles, SoundEvent sound) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.5;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particles, x, y, z,
                    FEEDBACK_PARTICLES, FEEDBACK_WIDTH, FEEDBACK_HEIGHT, FEEDBACK_WIDTH, 0.0);
        }
        float pitch = 0.4F / (level.random.nextFloat() * 0.4F + 0.8F);
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, FEEDBACK_VOLUME, pitch);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlluringSkullBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, WitcheryBlockEntities.ALLURING_SKULL.get(), AlluringSkullBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
