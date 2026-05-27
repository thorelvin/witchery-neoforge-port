package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brew of Flowing Spirit places Flowing Spirit like the old BrewFluid impact path.
 */
public class BrewOfFlowingSpiritItem extends BrewItem {

    public BrewOfFlowingSpiritItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = depositPosition(serverLevel, context.getClickedPos(), context.getClickedFace());
        if (!placeFlowingSpirit(serverLevel, pos)) {
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

        BlockPos pos = new BlockPos(Mth.floor(target.getX()), Mth.floor(target.getY()), Mth.floor(target.getZ()));
        if (!placeFlowingSpirit(serverLevel, pos)) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel && placeFlowingSpirit(serverLevel, player.blockPosition())) {
            serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5D, player.getZ(),
                    8, 0.35D, 0.25D, 0.35D, 0.01D);
        }
    }

    private static BlockPos depositPosition(ServerLevel level, BlockPos clicked, Direction face) {
        BlockPos pos = clicked.relative(face);
        if (face == Direction.UP && !level.getBlockState(clicked).isSolid()) {
            return clicked;
        }
        return pos;
    }

    private static boolean placeFlowingSpirit(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.isSolid() || current.hasBlockEntity()) {
            return false;
        }

        level.setBlock(pos, WitcheryBlocks.FLOWING_SPIRIT.get().defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
        return true;
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
