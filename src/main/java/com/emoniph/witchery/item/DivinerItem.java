package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DivinerItem extends Item {
    private static final int MAX_USE_TICKS = 400;
    private final Block blockToDetect;

    public DivinerItem(Block blockToDetect, Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON));
        this.blockToDetect = blockToDetect;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TICKS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof Player player)) return;

        int elapsedTicks = getUseDuration(stack, livingEntity) - remainingUseDuration;
        HitResult hit = player.pick(6.0, 1.0f, false);

        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getDirection() != Direction.UP) {
            player.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.5f, 1.0f);
            player.stopUsingItem();
            return;
        }

        BlockPos hitPos = blockHit.getBlockPos();
        int scanY = hitPos.getY() - elapsedTicks;
        Block block = level.getBlockState(new BlockPos(hitPos.getX(), scanY, hitPos.getZ())).getBlock();

        boolean foundTarget = block == this.blockToDetect;
        boolean done = foundTarget || block == Blocks.BEDROCK || scanY <= 1;

        if (done) {
            ServerLevel sl = (ServerLevel) level;
            double px = 0.5 + hitPos.getX();
            double py = hitPos.getY() + 1.0;
            double pz = 0.5 + hitPos.getZ();
            if (foundTarget) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 8, 0.5, 0.5, 0.5, 0.1);
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                sl.sendParticles(ParticleTypes.SMOKE, px, py, pz, 8, 0.5, 0.5, 0.5, 0.0);
                player.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.5f, 1.0f);
            }
            player.stopUsingItem();
            EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }
    }
}
