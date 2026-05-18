package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.emoniph.witchery.init.WitcheryBlockEntities.WITCHES_OVEN;

public class WitchesOvenBlock extends BaseEntityBlock {
    public static final MapCodec<WitchesOvenBlock> CODEC = simpleCodec(WitchesOvenBlock::new);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public WitchesOvenBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WitchesOvenBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WitchesOvenBlockEntity be && player instanceof ServerPlayer sp) {
            sp.openMenu(be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, WITCHES_OVEN.get(), WitchesOvenBlockEntity::serverTick);
    }
}
