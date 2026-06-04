package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Learning Mystic Branch spells from Demon souls. Right-click a soul to learn the working it
 * carries (consuming the soul); the spell can then be cast on the Branch. Source parity: the old
 * craft taught the demonic glyphs (Ignianima, Carnosa Diem, Tormentum, Morsmordre) only from the
 * souls of Anguish, Hunger, Torment and Fear demons.
 */
@EventBusSubscriber(modid = "witchery")
public final class MysticBranchEvents {

    private MysticBranchEvents() {
    }

    @SubscribeEvent
    public static void onUseSoul(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        String knowledge = knowledgeFor(stack);
        if (knowledge == null) {
            return;
        }

        WitcheryAttachments.LearnedSymbols learned = WitcheryAttachments.learned(player);
        if (learned.keys.add(knowledge)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("witchery.symbol.learned",
                    Component.translatable("witchery.pott." + knowledge)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getEyeY(), player.getZ(),
                    24, 0.4, 0.5, 0.4, 0.1);
            level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable("witchery.symbol.alreadyknown"), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static String knowledgeFor(ItemStack stack) {
        if (stack.is(WitcheryItems.ANGUISH_DEMON_SOUL.get())) {
            return "ignianima";
        }
        if (stack.is(WitcheryItems.HUNGER_DEMON_SOUL.get())) {
            return "carnosadiem";
        }
        if (stack.is(WitcheryItems.FEAR_DEMON_SOUL.get())) {
            return "morsmordre";
        }
        if (stack.is(WitcheryItems.TORMENT_DEMON_SOUL.get())) {
            return "tormentum";
        }
        return null;
    }
}
