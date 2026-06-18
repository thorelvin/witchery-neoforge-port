package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.PlacedModonomiconBlockEntity;
import com.emoniph.witchery.book.PlacedModonomiconBookResolver;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/** Invisible pedestal block that displays and stores one exact Modonomicon guide item. */
public class PlacedModonomiconBlock extends BaseEntityBlock {
    public static final MapCodec<PlacedModonomiconBlock> CODEC = simpleCodec(PlacedModonomiconBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape INTERACTION_SHAPE = box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);
    private static final VoxelShape COLLISION_SHAPE = box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);
    /**
     * Uniform scale of the whole floating book. Raised from the original 0.8 by 20% (0.8 * 1.2) so the
     * pages read more easily. Lives here (a common class) rather than the client renderer because
     * {@link #pagePixelPose} runs server-side via {@link #getShape}; the renderer reads this same value
     * so the drawn book and its click / page-selection geometry stay aligned.
     */
    public static final float BOOK_BASIS_SCALE = 0.96F;
    private static final int PAGE_INTERACTION_PIXEL_WIDTH = 124;
    private static final int PAGE_INTERACTION_PIXEL_HEIGHT = 155;
    private static final int PAGE_BUTTON_MARGIN_PIXELS = 3;
    private static final int PAGE_TEXT_LEFT_PIXELS = 8;
    private static final int PAGE_TEXT_RIGHT_PIXELS = 118;
    private static final int PAGE_PREVIOUS_ARROW_MIN_X = 4;
    private static final int PAGE_PREVIOUS_ARROW_MAX_X = 28;
    private static final int PAGE_NEXT_ARROW_MIN_X = 83;
    private static final int PAGE_NEXT_ARROW_MAX_X = 119;
    private static final int PAGE_ARROW_MIN_Y = 133;
    private static final int PAGE_ARROW_MAX_Y = 150;
    private static final int PAGE_INDEX_LEFT_MIN_X = 106;
    private static final int PAGE_INDEX_LEFT_MAX_X = 122;
    private static final int PAGE_INDEX_RIGHT_MIN_X = 2;
    private static final int PAGE_INDEX_RIGHT_MAX_X = 18;
    private static final int PAGE_INDEX_MIN_Y = 135;
    private static final int PAGE_INDEX_MAX_Y = 150;
    private static final float HEXEREI_DEGREES_TO_RADIANS_DIVISOR = 57.3F;
    private static final float SURFACE_REST_Y = 0.165F;
    private static final float OPEN_BOOK_LIFT_Y = 0.215F;
    private static final float CLOSED_CENTER_X_SHIFT = -0.28125F;
    private static final float CLOSED_CENTER_Z_SHIFT = 0.125F;
    private static final float MODEL_BASIS_Z_ROTATION = 90.0F;
    private static final float MODEL_BASIS_X_ROTATION = 90.0F;
    private static final float PAGE_MODEL_WIDTH = 5.0F;
    private static final float PAGE_PIXEL_SCALE = PAGE_MODEL_WIDTH / 16.0F / PAGE_INTERACTION_PIXEL_WIDTH;
    private static final float PAGE_CONTENT_LIFT_PIXELS = 0.5F;
    private static final float LEFT_PAGE_CONTENT_INSET_PIXELS = 6.0F;
    private static final float PAGE_TOP_X = 0.26F / 16.0F;
    private static final float PAGE_TOP_Y = -3.72F / 16.0F;
    private static final float LEFT_PAGE_SURFACE_Z = -0.025F / 16.0F;
    private static final float RIGHT_PAGE_SURFACE_Z = 0.025F / 16.0F;
    private static final float PAGE_RAY_EPSILON = 1.0E-5F;
    private static final float PAGE_READY_OPEN_AMOUNT = 0.92F;
    private static final double PAGE_SELECTION_PADDING = 0.035D;

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
        if (level.getBlockEntity(pos) instanceof PlacedModonomiconBlockEntity be && !be.getBookStack().isEmpty()) {
            VoxelShape pageShape = pageSelectionShape(pos, be);
            if (!pageShape.isEmpty()) {
                return pageShape;
            }
        }
        return INTERACTION_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
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
                PageHit pageHit = pageHit(pos, player, be);
                if (pageHit == null) {
                    return InteractionResult.PASS;
                }

                int targetPage = pageJumpTarget(level, be, player, pageHit);
                if (targetPage >= 0) {
                    if (be.jumpPlacedPage(targetPage, player)) {
                        com.emoniph.witchery.client.PlacedModonomiconClientActions.sendPageJump(pos, targetPage);
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    int direction = pageControlDirection(pageHit);
                    if (direction != 0 && be.turnPlacedPage(direction, player)) {
                        com.emoniph.witchery.client.PlacedModonomiconClientActions.sendPageTurn(pos, direction);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    public static int pageControlDirection(BlockState state, BlockPos pos, Player player) {
        return 0;
    }

    public static boolean isLookingAtReadablePage(BlockState state, BlockPos pos, Player player) {
        return true;
    }

    @Nullable
    private static PageHit pageHit(BlockPos pos, Player player, PlacedModonomiconBlockEntity be) {
        float degreesOpened = be.degreesOpened;
        float openAmount = Mth.clamp(1.0F - degreesOpened / 90.0F, 0.0F, 1.0F);
        if (openAmount <= PAGE_READY_OPEN_AMOUNT) {
            return null;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        PageHit left = pageHit(pos, be, true, openAmount, eye, look);
        PageHit right = pageHit(pos, be, false, openAmount, eye, look);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.distance() <= right.distance() ? left : right;
    }

    static int pageControlDirection(@Nullable PageHit hit) {
        if (hit == null) {
            return 0;
        }

        int x = pagePixelX(hit);
        int y = pagePixelY(hit.pageY());
        if (hit.isLeftPage()
                && inPixels(x, PAGE_PREVIOUS_ARROW_MIN_X, PAGE_PREVIOUS_ARROW_MAX_X)
                && inPixels(y, PAGE_ARROW_MIN_Y, PAGE_ARROW_MAX_Y)) {
            return -1;
        }
        if (!hit.isLeftPage()
                && inPixels(x, PAGE_NEXT_ARROW_MIN_X, PAGE_NEXT_ARROW_MAX_X)
                && inPixels(y, PAGE_ARROW_MIN_Y, PAGE_ARROW_MAX_Y)) {
            return 1;
        }
        return 0;
    }

    private static @Nullable PageHit pageHit(BlockPos pos, PlacedModonomiconBlockEntity be, boolean leftPage,
                                             float openAmount, Vec3 eye, Vec3 lookDirection) {
        Matrix4f inversePagePose = new Matrix4f(pagePixelPose(pos, be, leftPage, openAmount)).invert();
        Vector3f localEye = inversePagePose.transformPosition(new Vector3f(
                (float) eye.x(), (float) eye.y(), (float) eye.z()));
        Vector3f localLook = inversePagePose.transformDirection(new Vector3f(
                (float) lookDirection.x(), (float) lookDirection.y(), (float) lookDirection.z()));
        if (Math.abs(localLook.z) < PAGE_RAY_EPSILON) {
            return null;
        }

        float distance = -localEye.z / localLook.z;
        if (distance < 0.0F) {
            return null;
        }

        float pageX = localEye.x + localLook.x * distance;
        float pageY = localEye.y + localLook.y * distance;
        if (pageX < 0.0F || pageX > PAGE_INTERACTION_PIXEL_WIDTH
                || pageY < 0.0F || pageY > PAGE_INTERACTION_PIXEL_HEIGHT) {
            return null;
        }

        return new PageHit(leftPage, pageX, pageY, distance);
    }

    private static VoxelShape pageSelectionShape(BlockPos pos, PlacedModonomiconBlockEntity be) {
        float degreesOpened = be.degreesOpened;
        float openAmount = Mth.clamp(1.0F - degreesOpened / 90.0F, 0.0F, 1.0F);
        if (openAmount <= PAGE_READY_OPEN_AMOUNT) {
            return INTERACTION_SHAPE;
        }

        VoxelShape left = pageSelectionShape(pos, pagePixelPose(pos, be, true, openAmount));
        VoxelShape right = pageSelectionShape(pos, pagePixelPose(pos, be, false, openAmount));
        return Shapes.or(left, right);
    }

    private static VoxelShape pageSelectionShape(BlockPos pos, Matrix4f pagePose) {
        Vector3f[] corners = {
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(PAGE_INTERACTION_PIXEL_WIDTH, 0.0F, 0.0F),
                new Vector3f(PAGE_INTERACTION_PIXEL_WIDTH, PAGE_INTERACTION_PIXEL_HEIGHT, 0.0F),
                new Vector3f(0.0F, PAGE_INTERACTION_PIXEL_HEIGHT, 0.0F)
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vector3f corner : corners) {
            pagePose.transformPosition(corner);
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        return box(
                (minX - pos.getX() - PAGE_SELECTION_PADDING) * 16.0D,
                (minY - pos.getY() - PAGE_SELECTION_PADDING) * 16.0D,
                (minZ - pos.getZ() - PAGE_SELECTION_PADDING) * 16.0D,
                (maxX - pos.getX() + PAGE_SELECTION_PADDING) * 16.0D,
                (maxY - pos.getY() + PAGE_SELECTION_PADDING) * 16.0D,
                (maxZ - pos.getZ() + PAGE_SELECTION_PADDING) * 16.0D);
    }

    private static Matrix4f pagePixelPose(BlockPos pos, PlacedModonomiconBlockEntity be, boolean leftPage,
                                          float openAmount) {
        float degreesOpened = be.degreesOpened;
        float degreesFlopped = be.degreesFlopped;
        float floppedAmount = Mth.clamp(degreesFlopped / 90.0F, 0.0F, 1.0F);
        float openShift = openAmount * (degreesOpened / 5.0F - 12.0F) / 32.0F;
        float openLift = Math.max(openAmount, 1.0F - floppedAmount) * OPEN_BOOK_LIFT_Y;
        float closedCenterAmount = (1.0F - openAmount) * floppedAmount;
        float degreesSpunRadians = be.degreesSpun / HEXEREI_DEGREES_TO_RADIANS_DIVISOR;

        Matrix4f pose = new Matrix4f()
                .translate(pos.getX(), pos.getY(), pos.getZ())
                .translate(0.5F, SURFACE_REST_Y + openLift, 0.5F)
                .translate(
                        (float) Math.sin(degreesSpunRadians) * openShift,
                        (PlacedModonomiconBlockEntity.easeFlop(1.0F - floppedAmount) - 1.0F) / 16.0F,
                        (float) Math.cos(degreesSpunRadians) * openShift)
                .rotateY((float) Math.toRadians(be.degreesSpun))
                .translate(CLOSED_CENTER_X_SHIFT * closedCenterAmount, 0.0F, CLOSED_CENTER_Z_SHIFT * closedCenterAmount)
                .rotateX((float) Math.toRadians(-(degreesOpened / 2.0F + 45.0F)))
                .rotateY((float) Math.toRadians(-degreesFlopped))
                .translate(0.0F, -0.00390625F * (1.0F - degreesOpened / 90.0F), -(degreesFlopped / 10.0F) / 32.0F)
                .rotateZ((float) Math.toRadians(MODEL_BASIS_Z_ROTATION))
                .rotateX((float) Math.toRadians(MODEL_BASIS_X_ROTATION))
                .scale(BOOK_BASIS_SCALE);

        float spread = 1.2F * Mth.clamp(openAmount, 0.0F, 1.0F);
        pose.translate((float) Math.sin(spread) / 16.0F, 0.0F, 0.0F)
                .rotateY(leftPage ? -spread : spread)
                .translate(PAGE_TOP_X, PAGE_TOP_Y, leftPage ? LEFT_PAGE_SURFACE_Z : RIGHT_PAGE_SURFACE_Z);
        if (leftPage) {
            pose.rotateY((float) Math.PI)
                    .translate(-PAGE_MODEL_WIDTH / 16.0F + LEFT_PAGE_CONTENT_INSET_PIXELS * PAGE_PIXEL_SCALE, 0.0F, 0.0F);
        }
        return pose.translate(0.0F, 0.0F, PAGE_CONTENT_LIFT_PIXELS * PAGE_PIXEL_SCALE)
                .translate(0.0F, PAGE_INTERACTION_PIXEL_HEIGHT * PAGE_PIXEL_SCALE, 0.0F)
                .scale(PAGE_PIXEL_SCALE, -PAGE_PIXEL_SCALE, PAGE_PIXEL_SCALE);
    }

    private static int pageJumpTarget(Level level, PlacedModonomiconBlockEntity be, Player player,
                                      @Nullable PageHit hit) {
        if (hit == null) {
            return -1;
        }

        if (hitsIndexButton(hit)) {
            return 0;
        }

        int x = pagePixelX(hit);
        if (!inPixels(x, PAGE_TEXT_LEFT_PIXELS, PAGE_TEXT_RIGHT_PIXELS)) {
            return -1;
        }

        int indexPage = be.getOpenPagesIndex() + (hit.isLeftPage() ? 0 : 1);
        return PlacedModonomiconBookResolver.entryTargetPageIndexAt(
                be.getBookStack(), level, player, indexPage, pagePixelY(hit.pageY()));
    }

    static boolean hitsIndexButton(PageHit hit) {
        int x = pagePixelX(hit);
        int y = pagePixelY(hit.pageY());
        if (hit.isLeftPage()) {
            return inPixels(x, PAGE_INDEX_LEFT_MIN_X, PAGE_INDEX_LEFT_MAX_X)
                    && inPixels(y, PAGE_INDEX_MIN_Y, PAGE_INDEX_MAX_Y);
        }
        return inPixels(x, PAGE_INDEX_RIGHT_MIN_X, PAGE_INDEX_RIGHT_MAX_X)
                && inPixels(y, PAGE_INDEX_MIN_Y, PAGE_INDEX_MAX_Y);
    }

    static int pagePixelX(PageHit hit) {
        return (int) Math.round(hit.pageX());
    }

    static int pagePixelY(double pageY) {
        return (int) Math.round(pageY);
    }

    private static boolean inPixels(int value, int min, int max) {
        return value >= min - PAGE_BUTTON_MARGIN_PIXELS && value <= max + PAGE_BUTTON_MARGIN_PIXELS;
    }

    record PageHit(boolean leftPage, double pageX, double pageY, double distance) {
        boolean isLeftPage() {
            return leftPage;
        }
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
