package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.InsideBlockEffectApplier;

public class KettleBlock extends BaseEntityBlock {
    public static final MapCodec<KettleBlock> CODEC = simpleCodec(KettleBlock::new);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public KettleBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KettleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                com.emoniph.witchery.init.WitcheryBlockEntities.KETTLE.get(),
                KettleBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof KettleBlockEntity kettle)) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();

        // Glass bottle when brew is ready → extract brew
        if (held.is(Items.GLASS_BOTTLE) && kettle.isReady()) {
            ItemStack brew = kettle.extractBrew(player);
            if (!brew.isEmpty()) {
                held.shrink(1);
                if (held.isEmpty()) player.setItemInHand(player.getUsedItemHand(), brew);
                else if (!player.getInventory().add(brew)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, brew));
                }
                if (!level.isClientSide()) player.displayClientMessage(Component.translatable("witchery.kettle.extracted"), true);
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("witchery.kettle.status.brewing"), true);
            }
            return InteractionResult.SUCCESS;
        }

        // Water bucket → fill kettle
        if (held.is(Items.WATER_BUCKET)) {
            if (kettle.fillWater()) {
                if (!player.isCreative()) player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.BUCKET));
                kettle.reset(false);
                if (!level.isClientSide()) player.displayClientMessage(Component.translatable("witchery.kettle.filled"), true);
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("witchery.kettle.already_full"), true);
            return InteractionResult.SUCCESS;
        }

        // Empty bucket when filled → drain kettle
        if (held.is(Items.BUCKET) && kettle.isFilled()) {
            kettle.drainWater();
            if (!player.isCreative()) player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.WATER_BUCKET));
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("witchery.kettle.drained"), true);
            return InteractionResult.SUCCESS;
        }

        // Any other item when kettle is filled → add as ingredient
        if (!held.isEmpty() && kettle.isFilled() && !kettle.isRuined()) {
            if (!level.isClientSide()) {
                boolean added = kettle.addIngredient(held);
                if (added) {
                    if (!player.isCreative()) held.shrink(1);
                    player.displayClientMessage(Component.translatable("witchery.kettle.ingredient_added"), true);
                } else {
                    player.displayClientMessage(Component.translatable(
                            kettle.isRuined() ? "witchery.kettle.ruined" : "witchery.kettle.no_space"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Show status when right-clicking empty hand or ruined
        if (!level.isClientSide()) {
            Component status = kettle.isRuined()   ? Component.translatable("witchery.kettle.status.ruined") :
                    !kettle.isFilled()              ? Component.translatable("witchery.kettle.status.empty") :
                    kettle.isReady()                ? Component.translatable("witchery.kettle.status.ready") :
                    kettle.isBrewing()              ? Component.translatable("witchery.kettle.status.brewing") :
                                                      Component.translatable("witchery.kettle.status.filled");
            player.displayClientMessage(Component.translatable("witchery.kettle.status", status), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Items thrown into the kettle (EntityItem collision) */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity, InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof ItemEntity itemEntity)) return;
        if (!(level.getBlockEntity(pos) instanceof KettleBlockEntity kettle)) return;

        ItemStack stack = itemEntity.getItem();

        // Glass bottle → goes to bottle slot
        if (stack.is(Items.GLASS_BOTTLE)) {
            ItemStack current = kettle.getItem(KettleBlockEntity.BOTTLE_SLOT);
            if (current.isEmpty()) {
                kettle.setItem(KettleBlockEntity.BOTTLE_SLOT, stack.copy());
                itemEntity.discard();
            } else if (current.is(Items.GLASS_BOTTLE) && current.getCount() + stack.getCount() <= 64) {
                current.grow(stack.getCount());
                kettle.setItem(KettleBlockEntity.BOTTLE_SLOT, current);
                itemEntity.discard();
            }
            return;
        }

        // Other items → ingredient slot (if water present)
        if (kettle.isFilled() && !kettle.isRuined()) {
            boolean added = kettle.addIngredient(stack);
            if (added) itemEntity.discard();
        }
    }
}
