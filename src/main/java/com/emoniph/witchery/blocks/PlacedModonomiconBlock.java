package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.PlacedModonomiconBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Invisible pedestal block that displays and stores one exact Modonomicon guide item. */
public class PlacedModonomiconBlock extends BaseEntityBlock {
    public static final MapCodec<PlacedModonomiconBlock> CODEC = simpleCodec(PlacedModonomiconBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape INTERACTION_SHAPE = box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);
    private static final VoxelShape COLLISION_SHAPE = box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);
    private static final double PAGE_INTERACTION_CENTER_Y = 0.55D;
    private static final double PAGE_INTERACTION_HALF_WIDTH = 0.45D;
    private static final double PAGE_INTERACTION_HALF_HEIGHT = 0.36D;
    private static final double PAGE_NAV_INNER_X = 0.17D;
    private static final double PAGE_NAV_TOP_Y = -0.08D;
    private static final double PAGE_NAV_DENOMINATOR_EPSILON = 1.0E-5D;

    public PlacedModonomiconBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INTERACTION_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INTERACTION_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            dropStoredStack(level, pos);
            level.destroyBlock(pos, false);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof PlacedModonomiconBlockEntity be && !be.getBookStack().isEmpty()) {
            return be.getBookStack().copy();
        }
        return new ItemStack(WitcheryItems.WITCHES_PRIMER.get());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedModonomiconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, WitcheryBlockEntities.PLACED_MODONOMICON.get(), PlacedModonomiconBlockEntity::clientTick)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, hit);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.getAbilities().instabuild) {
            clearStoredStack(level, pos);
        } else {
            dropStoredStack(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level realLevel) {
            dropStoredStack(realLevel, pos);
        }
        super.destroy(level, pos, state);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        dropStoredStack(level, pos);
        super.wasExploded(level, pos, explosion);
    }

    private static InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlacedModonomiconBlockEntity be)) {
            return InteractionResult.PASS;
        }

        ItemStack stored = be.getBookStack();
        if (stored.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ItemStack returned = be.takeBookStack();
                if (!returned.isEmpty()) {
                    if (!player.getInventory().add(returned.copy())) {
                        player.drop(returned.copy(), false);
                    }
                    level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.7F, 1.0F);
                }
                level.removeBlock(pos, false);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        if (level.isClientSide()) {
            if (be.markReaderInteractionAndCanTurnPage()) {
                int direction = pageControlDirection(state, pos, player);
                if (be.turnPlacedPage(direction)) {
                    com.emoniph.witchery.client.PlacedModonomiconClientActions.sendPageTurn(pos, direction);
                }
            }
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    public static int pageControlDirection(BlockState state, BlockPos pos, Player player) {
        double toPlayerX = player.getX() - pos.getX() - 0.5D;
        double toPlayerZ = player.getZ() - pos.getZ() - 0.5D;
        double distanceSqr = toPlayerX * toPlayerX + toPlayerZ * toPlayerZ;
        Vec3 forward;
        if (distanceSqr > 1.0E-4D) {
            double invDistance = 1.0D / Math.sqrt(distanceSqr);
            forward = new Vec3(toPlayerX * invDistance, 0.0D, toPlayerZ * invDistance);
        } else {
            Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.SOUTH;
            forward = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
        }

        Vec3 pageCenter = new Vec3(pos.getX() + 0.5D, pos.getY() + PAGE_INTERACTION_CENTER_Y, pos.getZ() + 0.5D);
        return pageControlDirection(forward, pageCenter, player.getEyePosition(), player.getLookAngle());
    }

    static int pageControlDirection(Vec3 forward, Vec3 pageCenter, Vec3 eye, Vec3 lookDirection) {
        Vec3 look = lookDirection.normalize();
        double denominator = forward.dot(look);
        if (Math.abs(denominator) < PAGE_NAV_DENOMINATOR_EPSILON) {
            return 0;
        }

        double distanceToPlane = forward.dot(pageCenter.subtract(eye)) / denominator;
        if (distanceToPlane < 0.0D) {
            return 0;
        }

        Vec3 hit = eye.add(look.scale(distanceToPlane));
        Vec3 toHit = hit.subtract(pageCenter);
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        double pageX = toHit.dot(right);
        double pageY = toHit.y;
        if (Math.abs(pageX) > PAGE_INTERACTION_HALF_WIDTH
                || Math.abs(pageY) > PAGE_INTERACTION_HALF_HEIGHT
                || pageY > PAGE_NAV_TOP_Y
                || Math.abs(pageX) < PAGE_NAV_INNER_X) {
            return 0;
        }

        return pageX > 0.0D ? 1 : -1;
    }

    private static void dropStoredStack(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedModonomiconBlockEntity be) {
            ItemStack stored = be.takeBookStack();
            if (!stored.isEmpty()) {
                popResource(level, pos, stored.copy());
            }
        }
    }

    private static void clearStoredStack(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedModonomiconBlockEntity be) {
            be.takeBookStack();
        }
    }
}
