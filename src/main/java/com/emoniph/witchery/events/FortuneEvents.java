package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.effect.FortuneMobEffect;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class FortuneEvents {
    private FortuneEvents() {
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }

        MobEffectInstance effect = player.getEffect(WitcheryMobEffects.FORTUNE);
        if (effect == null) {
            return;
        }

        boolean silkTouching = FortuneMobEffect.hasSilkTouch(event.getLevel(), event.getTool());
        boolean hasBlockEntity = event.getBlockEntity() != null || event.getState().hasBlockEntity();
        if (!FortuneMobEffect.shouldReplaceDrops(silkTouching, hasBlockEntity, !event.getDrops().isEmpty())) {
            return;
        }

        FortuneMobEffect.replaceDrops(event, player, effect.getAmplifier());
    }
}
