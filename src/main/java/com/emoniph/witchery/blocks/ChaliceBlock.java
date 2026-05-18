package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.ChaliceBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionResult;

public class ChaliceBlock extends BaseEntityBlock {

    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    public static final MapCodec<ChaliceBlock> CODEC = simpleCodec(ChaliceBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public ChaliceBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FILLED);
    }

    // Right-click with water bucket to fill; right-click with empty bucket to drain
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean filled = state.getValue(FILLED);
            if (!filled && stack.is(Items.WATER_BUCKET)) {
                level.setBlock(pos, state.setValue(FILLED, true), 3);
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.BUCKET));
                return InteractionResult.SUCCESS;
            } else if (filled && stack.is(Items.BUCKET)) {
                level.setBlock(pos, state.setValue(FILLED, false), 3);
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.WATER_BUCKET));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ChaliceBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
}
