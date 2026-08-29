package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.BlightHelper;
import com.emoniph.witchery.data.BlightHeartSavedData;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BrewOfBlightItem extends BrewItem {
    public BrewOfBlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !BlightHelper.isHeartEnabled()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.blight_heart.disabled"), false);
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (!BlightHelper.isHeartEnabled()) {
                player.displayClientMessage(Component.translatable("witchery.ritual.blight_heart.disabled"), false);
                return stack;
            }
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                BlockPos heartPos = embeddedHeartPos(level, player.blockPosition());
                if (heartPos == null) {
                    player.displayClientMessage(Component.translatable("witchery.ritual.blight_heart.no_ground"), false);
                    return stack;
                }
                if (!WitcheryTerrainProtection.canTransform(player, serverLevel, heartPos)
                        || !WitcheryTerrainProtection.canTransform(player, serverLevel, heartPos.above())) {
                    player.displayClientMessage(Component.translatable("witchery.blight_heart.protected"), false);
                    return stack;
                }
                BlightHeartSavedData.PlacementResult placement =
                        BlightHelper.canPlaceHeartFor(serverLevel, serverPlayer, heartPos);
                if (placement != BlightHeartSavedData.PlacementResult.ALLOWED) {
                    player.displayClientMessage(
                            Component.translatable(BlightHelper.placementFailureKey(placement)), false);
                    return stack;
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && !BlightHelper.isHeartEnabled()) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("witchery.ritual.blight_heart.disabled"), false);
            }
            return InteractionResult.FAIL;
        }
        BlockPos heartPos = pouredHeartPos(level, context.getClickedPos(), context.getClickedFace());
        if (heartPos == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WitcheryTerrainProtection.canTransform(context.getPlayer(), serverLevel, heartPos)
                || !WitcheryTerrainProtection.canTransform(context.getPlayer(), serverLevel, heartPos.above())) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("witchery.blight_heart.protected"), false);
            }
            return InteractionResult.FAIL;
        }

        BlightHeartSavedData.PlacementResult placement =
                BlightHelper.canPlaceHeartFor(serverLevel, serverPlayer, heartPos);
        if (placement != BlightHeartSavedData.PlacementResult.ALLOWED) {
            serverPlayer.displayClientMessage(
                    Component.translatable(BlightHelper.placementFailureKey(placement)), false);
            return InteractionResult.FAIL;
        }

        if (!BlightHelper.placeHeart(serverLevel, heartPos, true, serverPlayer)) {
            return InteractionResult.FAIL;
        }
        consumeBrewBottle(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)
                || !BlightHelper.isHeartEnabled()) {
            return;
        }
        BlockPos heartPos = embeddedHeartPos(level, player.blockPosition());
        if (heartPos != null
                && WitcheryTerrainProtection.canTransform(player, serverLevel, heartPos)
                && WitcheryTerrainProtection.canTransform(player, serverLevel, heartPos.above())) {
            BlightHelper.placeHeart(serverLevel, heartPos, true, serverPlayer);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private static BlockPos pouredHeartPos(Level level, BlockPos clickedPos, net.minecraft.core.Direction face) {
        BlockState clicked = level.getBlockState(clickedPos);
        BlockPos candidate = clicked.canBeReplaced() ? clickedPos.below() : clickedPos;
        if (BlightHelper.canPlaceEmbeddedHeartAt(level, candidate)) {
            return candidate;
        }

        BlockPos side = clickedPos.relative(face);
        return embeddedHeartPos(level, side);
    }

    private static BlockPos embeddedHeartPos(Level level, BlockPos pos) {
        BlockPos candidate = level.getBlockState(pos).canBeReplaced() ? pos.below() : pos;
        while (candidate.getY() > level.getMinY() && level.getBlockState(candidate).canBeReplaced()) {
            candidate = candidate.below();
        }
        return BlightHelper.canPlaceEmbeddedHeartAt(level, candidate) ? candidate : null;
    }
}
