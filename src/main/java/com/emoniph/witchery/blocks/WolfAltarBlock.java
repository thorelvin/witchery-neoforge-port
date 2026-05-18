package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Wolf Altar — werewolf players right-click to receive wolf god boons.
 * Grants a short Strength III + Speed II burst and increments wolf mastery.
 * Non-werewolves receive nothing.
 */
public class WolfAltarBlock extends Block {

    public WolfAltarBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isWerewolf()) {
            player.displayClientMessage(Component.translatable("witchery.wolf_altar.not_werewolf"), false);
            return InteractionResult.FAIL;
        }

        // Grant wolf god boon: burst of power
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,    1200, 2)); // Strength III, 60s
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,  1200, 1)); // Speed II, 60s
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,     200, 1)); // Regen II, 10s

        // Increment mastery level (capped at 3)
        int mastery = data.getWolfMastery();
        if (mastery < 3) {
            data.setWolfMastery(mastery + 1);
            player.displayClientMessage(Component.translatable(
                    "witchery.wolf_altar.mastery." + (mastery + 1)), false);
        } else {
            player.displayClientMessage(Component.translatable("witchery.wolf_altar.max_mastery"), false);
        }

        level.playSound(null, pos, SoundEvents.WOLF_SHAKE, SoundSource.BLOCKS, 1.5f, 0.7f);
        WitcheryMod.LOGGER.debug("Wolf Altar used by {} (mastery {})", player.getName().getString(), data.getWolfMastery());
        return InteractionResult.SUCCESS;
    }
}
