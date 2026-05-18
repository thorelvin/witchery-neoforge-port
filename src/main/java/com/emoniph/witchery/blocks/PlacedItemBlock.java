package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Thin display block that holds one ItemStack on top of an altar surface. */
public class PlacedItemBlock extends BaseEntityBlock {

    public static final MapCodec<PlacedItemBlock> CODEC = simpleCodec(PlacedItemBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 2, 14);

    public PlacedItemBlock(BlockBehaviour.Properties p) { super(p); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedItemBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /** Right-click with empty hand to retrieve the stored item. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlacedItemBlockEntity be) {
            ItemStack stored = be.getStack();
            if (!stored.isEmpty()) {
                player.getInventory().add(stored.copy());
                level.removeBlock(pos, false);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
