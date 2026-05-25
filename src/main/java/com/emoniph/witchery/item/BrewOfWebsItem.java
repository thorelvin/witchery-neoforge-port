package com.emoniph.witchery.item;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Webs - source-style compact cobweb burst in practical item form. */
public class BrewOfWebsItem extends BrewItem {
    private static final BlockPos[] WEB_OFFSETS = {
            BlockPos.ZERO,
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(0, 1, 0),
            new BlockPos(0, -1, 0)
    };

    public BrewOfWebsItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos center = blockImpactPosition(serverLevel, context.getClickedPos(), context.getClickedFace());
        explodeWeb(serverLevel, center);
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

        explodeWeb(serverLevel, target.blockPosition());
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            explodeWeb(serverLevel, player.blockPosition());
        }
    }

    private static BlockPos blockImpactPosition(ServerLevel level, BlockPos clickedPos, Direction face) {
        BlockPos pos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            return pos.below();
        }
        return pos;
    }

    private static void explodeWeb(ServerLevel level, BlockPos center) {
        for (BlockPos offset : WEB_OFFSETS) {
            setWebIfNotSolid(level, center.offset(offset));
        }
        level.playSound(null, center, SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    private static boolean setWebIfNotSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isSolid()
                || state.liquid()
                || state.hasBlockEntity()) {
            return false;
        }

        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.02D);
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
