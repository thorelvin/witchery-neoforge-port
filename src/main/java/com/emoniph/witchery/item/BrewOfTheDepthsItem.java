package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of the Depths - five minutes of source-style water protection and land punishment. */
public class BrewOfTheDepthsItem extends BrewItem {
    private static final int DURATION = 5 * 60 * 20;

    public BrewOfTheDepthsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setDepthsTicks(DURATION);
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 6000, 0)); // 5 min
    }
}
