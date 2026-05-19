package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.LeechChestBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LeechChestBlock extends BaseEntityBlock {
    public static final MapCodec<LeechChestBlock> CODEC = simpleCodec(LeechChestBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public LeechChestBlock(BlockBehaviour.Properties p) { super(p); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LeechChestBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level.getBlockEntity(pos) instanceof LeechChestBlockEntity chest)) {
            return;
        }
        CompoundTag dropData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!dropData.isEmpty()) {
            chest.loadDropData(dropData);
        }
        if (stack.getHoverName() != null
                && !stack.getHoverName().getString().equals(stack.getItem().getName(stack).getString())) {
            chest.setCustomName(stack.getHoverName().getString());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LeechChestBlockEntity chest) {
            chest.storePlayer(player);
            player.openMenu(chest);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LeechChestBlockEntity chest) {
            Containers.dropContents(level, pos, chest);
            if (!player.isCreative()) {
                ItemStack stack = new ItemStack(WitcheryItems.LEECH_CHEST.get());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(chest.createDropData()));
                popResource(level, pos, stack);
            }
            level.updateNeighbourForOutputSignal(pos, this);
        } else if (!level.isClientSide() && !player.isCreative()) {
            ItemStack stack = new ItemStack(WitcheryItems.LEECH_CHEST.get());
            popResource(level, pos, stack);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
