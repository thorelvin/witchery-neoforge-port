package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.emoniph.witchery.init.WitcheryBlockEntities;

/**
 * Refilling Chest block — opens a 3-row chest GUI.
 * The BlockEntity handles auto-refill from the Torment loot table.
 * Unbreakable: strength(-1.0F, 3_600_000.0F) set via block properties.
 */
public class RefillingChestBlock extends BaseEntityBlock {

    public static final MapCodec<RefillingChestBlock> CODEC =
            simpleCodec(RefillingChestBlock::new);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public RefillingChestBlock(Properties props) { super(props); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefillingChestBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, WitcheryBlockEntities.REFILLING_CHEST.get(),
                RefillingChestBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
                                               BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        if (level.getBlockEntity(pos) instanceof RefillingChestBlockEntity be) {
            sp.openMenu(be);
        }
        return InteractionResult.CONSUME;
    }
}
