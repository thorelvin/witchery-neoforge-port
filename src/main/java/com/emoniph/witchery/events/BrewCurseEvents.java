package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.BrewCurse;
import com.emoniph.witchery.brewing.BrewWarningCurse;
import com.emoniph.witchery.brewing.BrewWarningText;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Triggered-dispersal brew "curse" release (source {@code DispersalTriggered.EventHooks}): using an item
 * that carries a {@link BrewCurse} releases one charge of the stored brew onto the user, then the item
 * goes on a short cooldown. When the last charge is spent the curse is removed.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class BrewCurseEvents {

    private static final int CURSE_COOLDOWN_TICKS = 100; // ~5 s (source 5000 ms)

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Only fires on item use (right-click in air / start-use), never on placing a block item, so a
        // cursed door/button is placed normally and only released when the cursed item is actually used.
        releaseCurse(event.getEntity(), event.getItemStack());
    }

    private static void releaseCurse(Player player, ItemStack stack) {
        if (player.level().isClientSide() || stack.isEmpty()) {
            return;
        }
        boolean releasedWarning = releaseWarning(player, stack);
        boolean releasedBrew = releaseBrew(player, stack);
        if (releasedWarning || releasedBrew) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                    SoundSource.PLAYERS, 0.7F, releasedWarning && !releasedBrew ? 1.2F : 0.7F);
        }
    }

    private static boolean releaseBrew(Player player, ItemStack stack) {
        BrewCurse curse = stack.get(WitcheryDataComponents.BREW_CURSE.get());
        long now = player.level().getGameTime();
        if (curse == null || !curse.ready(now)) {
            return false;
        }
        Brews.applyToEntity(player, curse.brew().items());
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                    12, 0.4, 0.6, 0.4, 0.02);
        }

        int remaining = curse.charges() - 1;
        if (remaining > 0) {
            stack.set(WitcheryDataComponents.BREW_CURSE.get(),
                    new BrewCurse(curse.brew(), remaining, now + CURSE_COOLDOWN_TICKS));
        } else {
            stack.remove(WitcheryDataComponents.BREW_CURSE.get());
        }
        return true;
    }

    private static boolean releaseWarning(Player player, ItemStack stack) {
        BrewWarningCurse curse = stack.get(WitcheryDataComponents.BREW_WARNING_CURSE.get());
        long now = player.level().getGameTime();
        if (curse == null || !curse.ready(now)) {
            return false;
        }
        player.displayClientMessage(Component.literal(curse.warning().message()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(BrewWarningText.COLOR, 0.85F),
                    player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.35, 0.45, 0.35, 0.0);
            serverLevel.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                    6, 0.35, 0.45, 0.35, 0.02);
        }

        int remaining = curse.charges() - 1;
        if (remaining > 0) {
            stack.set(WitcheryDataComponents.BREW_WARNING_CURSE.get(),
                    new BrewWarningCurse(curse.warning(), remaining, now + CURSE_COOLDOWN_TICKS));
        } else {
            stack.remove(WitcheryDataComponents.BREW_WARNING_CURSE.get());
        }
        return true;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        BrewCurse curse = event.getItemStack().get(WitcheryDataComponents.BREW_CURSE.get());
        BrewWarningCurse warningCurse = event.getItemStack().get(WitcheryDataComponents.BREW_WARNING_CURSE.get());
        if (warningCurse != null) {
            event.getToolTip().add(Component.translatable("witchery.brew.warning.cursed", warningCurse.charges())
                    .withStyle(ChatFormatting.DARK_PURPLE));
            event.getToolTip().add(Component.literal(warningCurse.warning().message())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (curse == null) {
            return;
        }
        event.getToolTip().add(Component.translatable("witchery.brew.cursed", curse.charges())
                .withStyle(ChatFormatting.DARK_PURPLE));
        for (var effect : Brews.buildEffects(curse.brew().items())) {
            event.getToolTip().add(Component.translatable(effect.getEffect().value().getDescriptionId())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
