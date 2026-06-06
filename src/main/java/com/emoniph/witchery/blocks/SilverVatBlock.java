package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.SilverVatBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SilverVatBlock extends BaseEntityBlock {

    public static final MapCodec<SilverVatBlock> CODEC = simpleCodec(SilverVatBlock::new);
    public static final BooleanProperty HAS_DEPOSITS = BooleanProperty.create("has_deposits");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.24, 16.0);

    public SilverVatBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(HAS_DEPOSITS, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_DEPOSITS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SilverVatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, WitcheryBlockEntities.SILVER_VAT.get(), SilverVatBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SilverVatBlockEntity be)) return InteractionResult.PASS;

        ItemStack collected = be.takeStored();
        if (!collected.isEmpty()) {
            player.drop(collected, false);
            player.displayClientMessage(Component.translatable("witchery.silver_vat.collected", collected.getCount()), false);
        } else {
            player.displayClientMessage(Component.translatable("witchery.silver_vat.empty"), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SilverVatBlockEntity be) {
            be.checkAdjacentGoldOutputs(level, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SilverVatBlockEntity be) {
            ItemStack stored = be.takeStored();
            if (!stored.isEmpty()) {
                Block.popResource(level, pos, stored);
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
