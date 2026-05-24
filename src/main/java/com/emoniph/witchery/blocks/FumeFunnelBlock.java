package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.FumeFunnelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FumeFunnelBlock extends BaseEntityBlock {
    public static final MapCodec<FumeFunnelBlock> CODEC = simpleCodec(FumeFunnelBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean filtered;

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public FumeFunnelBlock(BlockBehaviour.Properties p) {
        this(p, false);
    }

    public FumeFunnelBlock(BlockBehaviour.Properties p, boolean filtered) {
        super(p);
        this.filtered = filtered;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isFiltered() {
        return filtered;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FumeFunnelBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos ovenPos = findLinkedOven(level, pos, state.getValue(FACING));
        if (ovenPos != null && level.getBlockEntity(ovenPos) instanceof com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity oven
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(oven, ovenPos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    private static BlockPos findLinkedOven(Level level, BlockPos pos, Direction facing) {
        BlockPos[] checks = switch (facing) {
            case NORTH, SOUTH -> new BlockPos[]{pos.east(), pos.west(), pos.below()};
            case EAST, WEST -> new BlockPos[]{pos.south(), pos.north(), pos.below()};
            default -> new BlockPos[]{pos.below()};
        };
        for (BlockPos check : checks) {
            if (level.getBlockState(check).getBlock() instanceof WitchesOvenBlock) {
                return check;
            }
        }
        return null;
    }
}
