package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Hollow Tears - source-style liquid placement in practical item form. */
public class BrewOfHollowTearsItem extends BrewItem {
    public BrewOfHollowTearsItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = placementPos(serverLevel, context.getClickedPos(), context.getClickedFace());
        if (!depositHollowTears(serverLevel, placePos, context.getPlayer())) {
            return InteractionResult.PASS;
        }

        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (!depositHollowTears(serverLevel, target.blockPosition(), player)) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            depositHollowTears(serverLevel, player.blockPosition(), player);
        }
    }

    private static BlockPos placementPos(Level level, BlockPos clickedPos, Direction face) {
        BlockPos placePos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            placePos = clickedPos;
        }
        return placePos;
    }

    private static boolean depositHollowTears(ServerLevel level, BlockPos pos, Player player) {
        BlockState current = level.getBlockState(pos);
        if (current.is(WitcheryBlocks.HOLLOW_TEARS.get())) {
            return false;
        }
        if (!canDisplace(current)) {
            return false;
        }
        if (!(current.isAir()
                ? WitcheryTerrainProtection.canPlace(player, level, pos)
                : WitcheryTerrainProtection.canTransform(player, level, pos))) {
            return false;
        }

        level.setBlock(pos, WitcheryBlocks.HOLLOW_TEARS.get().defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 0.8F);
        level.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                12, 0.25D, 0.25D, 0.25D, 0.05D);
        return true;
    }

    private static boolean canDisplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || (!state.isSolid() && !state.hasBlockEntity());
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
