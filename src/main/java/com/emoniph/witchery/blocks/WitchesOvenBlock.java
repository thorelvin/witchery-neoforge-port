package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.emoniph.witchery.init.WitcheryBlockEntities.WITCHES_OVEN;

public class WitchesOvenBlock extends BaseEntityBlock {
    public static final MapCodec<WitchesOvenBlock> CODEC = simpleCodec(WitchesOvenBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public WitchesOvenBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WitchesOvenBlockEntity(pos, state);
    }

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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.2 + random.nextDouble() * (6.0 / 16.0);
        double z = pos.getZ() + 0.5;
        double sideOffset = 0.52;
        double randomOffset = random.nextDouble() * 0.6 - 0.3;

        switch (state.getValue(FACING)) {
            case EAST -> {
                level.addParticle(ParticleTypes.SMOKE, x + sideOffset, y, z + randomOffset, 0.0, 0.0, 0.0);
                level.addParticle(ParticleTypes.FLAME, x + sideOffset, y, z + randomOffset, 0.0, 0.0, 0.0);
            }
            case WEST -> {
                level.addParticle(ParticleTypes.SMOKE, x - sideOffset, y, z + randomOffset, 0.0, 0.0, 0.0);
                level.addParticle(ParticleTypes.FLAME, x - sideOffset, y, z + randomOffset, 0.0, 0.0, 0.0);
            }
            case SOUTH -> {
                level.addParticle(ParticleTypes.SMOKE, x + randomOffset, y, z + sideOffset, 0.0, 0.0, 0.0);
                level.addParticle(ParticleTypes.FLAME, x + randomOffset, y, z + sideOffset, 0.0, 0.0, 0.0);
            }
            case NORTH -> {
                level.addParticle(ParticleTypes.SMOKE, x + randomOffset, y, z - sideOffset, 0.0, 0.0, 0.0);
                level.addParticle(ParticleTypes.FLAME, x + randomOffset, y, z - sideOffset, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WitchesOvenBlockEntity be && player instanceof ServerPlayer sp) {
            sp.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WitchesOvenBlockEntity be) {
            Containers.dropContents(level, pos, be);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, WITCHES_OVEN.get(), WitchesOvenBlockEntity::serverTick);
    }
}
