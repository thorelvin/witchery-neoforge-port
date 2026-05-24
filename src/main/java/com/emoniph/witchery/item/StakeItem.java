package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StakeItem extends Item {
    public StakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof ServerPlayer victim) || !(user.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(victim);
        if (!data.isVampire() || !victim.isSleeping()) {
            return InteractionResult.PASS;
        }

        level.playSound(null, victim.getX(), victim.getY() + victim.getBbHeight() * 0.8D, victim.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.8D,
                victim.getZ(), 16, victim.getBbWidth() * 0.5D, victim.getBbHeight() * 0.25D,
                victim.getBbWidth() * 0.5D, 0.02D);
        victim.hurt(user.damageSources().playerAttack(user), Float.MAX_VALUE);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
