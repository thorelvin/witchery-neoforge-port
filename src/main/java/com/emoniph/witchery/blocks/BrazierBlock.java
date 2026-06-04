package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BrazierBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class BrazierBlock extends BaseEntityBlock {

    public static final MapCodec<BrazierBlock> CODEC = simpleCodec(BrazierBlock::new);
    private static final VoxelShape SHAPE = box(3.2, 0.0, 3.2, 12.8, 15.2, 12.8);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public BrazierBlock(BlockBehaviour.Properties p) { super(p); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrazierBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, WitcheryBlockEntities.BRAZIER.get(),
                                     BrazierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BrazierBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // Water bucket → reset
        if (stack.is(Items.WATER_BUCKET)) {
            if (!be.isEmpty() || be.isBurning()) {
                be.reset();
                if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // Flint and steel → ignite
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (!be.isEmpty() && !be.isBurning()) {
                be.begin();
                if (!player.isCreative()) stack.hurtAndBreak(1, player,
                        net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // Any other item → add as ingredient (if not already burning)
        if (!be.isBurning() && !stack.isEmpty()) {
            if (be.addIngredient(stack)) {
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3f, 1.5f);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Redstone signal → auto-ignite. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block,
                                net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            if (level.getBlockEntity(pos) instanceof BrazierBlockEntity be
                    && !be.isBurning() && !be.isEmpty()) {
                be.begin();
            }
        }
    }

    /** Source player-break behavior: idle braziers return ingredients; burning braziers leave Wood Ash. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BrazierBlockEntity be) {
            if (be.isBurning()) {
                popResource(level, pos, new ItemStack(WitcheryItems.WOOD_ASH.get()));
            } else {
                for (ItemStack drop : be.getDrops()) {
                    if (!drop.isEmpty()) {
                        popResource(level, pos, drop.copy());
                    }
                }
            }
            be.clearContent();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
