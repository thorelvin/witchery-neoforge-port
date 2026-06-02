package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.CoffinBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ScheduledTickAccess;

/**
 * Coffin — a 2-block-long sleep block for vampires.
 * Vampires can sleep during the day; normal players sleep at night.
 * Shift-right-click opens/closes the lid; right-click while open starts sleep.
 */
public class CoffinBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<CoffinBlock> CODEC = simpleCodec(CoffinBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    public static final EnumProperty<BedPart>  PART     = BlockStateProperties.BED_PART;
    public static final BooleanProperty        OCCUPIED = BlockStateProperties.OCCUPIED;
    public static final BooleanProperty        OPEN     = BooleanProperty.create("open");

    // Shape: box from y=0..12 (a proper coffin depth)
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public CoffinBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING,   Direction.NORTH)
                .setValue(PART,     BedPart.FOOT)
                .setValue(OCCUPIED, false)
                .setValue(OPEN,     false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED, OPEN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace() != Direction.UP) {
            return null;
        }
        Direction facing = ctx.getHorizontalDirection();
        BlockPos foot = ctx.getClickedPos();
        BlockPos head = foot.relative(facing);
        Level level = ctx.getLevel();
        if (level.getBlockState(foot).isAir()
                && level.getBlockState(head).isAir()
                && Block.canSupportCenter(level, foot.below(), Direction.UP)
                && Block.canSupportCenter(level, head.below(), Direction.UP)
                && level.getWorldBorder().isWithinBounds(head)) {
            return defaultBlockState().setValue(FACING, facing);
        }
        return null; // not enough space
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            // Place the HEAD block one step in the facing direction
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
            level.sendBlockUpdated(pos, state, state, 3);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    // ── Neighbour update — break if partner is gone ───────────────────────────

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess _ticks, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, net.minecraft.util.RandomSource _rand) {
        if (facing == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
            return facingState.is(this) && facingState.getValue(PART) != state.getValue(PART)
                    ? state.setValue(OCCUPIED, facingState.getValue(OCCUPIED))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, _ticks, currentPos, facing, facingPos, facingState, _rand);
    }

    // ── Break both halves in creative ─────────────────────────────────────────

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            BedPart part = state.getValue(PART);
            if (part == BedPart.FOOT) {
                BlockPos headPos = pos.relative(getNeighbourDirection(part, state.getValue(FACING)));
                BlockState headState = level.getBlockState(headPos);
                if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
                    level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, headPos, Block.getId(headState));
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.CONSUME;

        // Always operate from HEAD block
        if (state.getValue(PART) != BedPart.HEAD) {
            pos   = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) return InteractionResult.CONSUME;
        }

        // Occupied — just report
        if (state.getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("block.witchery.coffin.occupied"), true);
            return InteractionResult.SUCCESS;
        }

        // Shift-right-click: toggle lid
        if (player.isShiftKeyDown()) {
            boolean nowOpen = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, nowOpen), 3);
            // Also sync foot half's open state for rendering consistency
            BlockPos footPos = pos.relative(state.getValue(FACING).getOpposite());
            BlockState footState = level.getBlockState(footPos);
            if (footState.is(this) && footState.getValue(PART) == BedPart.FOOT) {
                level.setBlock(footPos, footState.setValue(OPEN, nowOpen), 3);
            }
            return InteractionResult.SUCCESS;
        }

        // Right-click: sleep if open
        if (!state.getValue(OPEN)) {
            player.displayClientMessage(Component.translatable("block.witchery.coffin.closed"), true);
            return InteractionResult.SUCCESS;
        }

        player.startSleepInBed(pos).ifLeft(problem -> {
            if (problem.message() != null) {
                player.displayClientMessage(problem.message(), true);
            }
        });
        return InteractionResult.SUCCESS;
    }

    // ── Bed API (enables player.startSleepInBed to work) ─────────────────────

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper) {
        return true;
    }

    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type,
                                                                      LevelReader level, BlockPos pos, float yaw) {
        Direction facing = state.getValue(FACING);
        final BlockPos headPos = state.getValue(PART) != BedPart.HEAD ? pos.relative(facing) : pos;
        return BedBlock.findStandUpPosition(type, level, headPos, facing, yaw)
                .map(vec -> ServerPlayer.RespawnPosAngle.of(vec, headPos, yaw));
    }

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        level.setBlock(pos, state.setValue(OCCUPIED, occupied), 3);
        // Sync to partner half
        BlockPos partnerPos = pos.relative(getNeighbourDirection(state.getValue(PART), state.getValue(FACING)));
        BlockState partnerState = level.getBlockState(partnerPos);
        if (partnerState.is(this)) {
            level.setBlock(partnerPos, partnerState.setValue(OCCUPIED, occupied), 3);
        }
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    // ── Block entity ──────────────────────────────────────────────────────────

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoffinBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null; // No tick needed
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    // ── Seed for consistent model rotation ────────────────────────────────────

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        BlockPos anchor = pos.relative(state.getValue(FACING), state.getValue(PART) == BedPart.HEAD ? 0 : 1);
        return net.minecraft.util.Mth.getSeed(anchor.getX(), pos.getY(), anchor.getZ());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Direction from this part to the other part. */
    public static Direction getNeighbourDirection(BedPart part, Direction facing) {
        return part == BedPart.FOOT ? facing : facing.getOpposite();
    }

    /** Get HEAD block pos from any part. */
    public static BlockPos getHeadPos(BlockState state, BlockPos pos) {
        if (state.getValue(PART) == BedPart.HEAD) return pos;
        return pos.relative(state.getValue(FACING));
    }
}
