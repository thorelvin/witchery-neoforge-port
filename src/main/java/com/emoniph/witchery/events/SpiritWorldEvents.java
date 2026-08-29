package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class SpiritWorldEvents {
    private SpiritWorldEvents() {
    }

    static boolean contributesNightmareAtmosphere(WitcheryPlayerData data) {
        return data.isInSpiritWorld() && !data.isSpiritWorldDream();
    }

    public static boolean hasNightmareAtmosphere(net.minecraft.server.level.ServerLevel level) {
        return level.players().stream()
                .map(WitcheryAttachments::get)
                .anyMatch(SpiritWorldEvents::contributesNightmareAtmosphere);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || !(event.getEntity() instanceof LivingEntity living)
                || living instanceof Player) {
            return;
        }

        Class<?> entityClass = living.getClass();
        if (isSourceDisallowedLiving(entityClass.getCanonicalName(), living.getType() == EntityType.ENDERMAN)) {
            event.setCanceled(true);
        }
    }

    static boolean isSourceDisallowedLiving(String canonicalName, boolean enderman) {
        return enderman || canonicalName == null
                || !canonicalName.startsWith("net.minecraft.world.entity")
                    && !canonicalName.startsWith("com.emoniph.witchery");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !event.getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            return;
        }

        event.getDrops().removeIf(drop -> isBannedSpiritDrop(drop.getItem()));
        if (event.getState().is(WitcheryBlocks.WISPY_COTTON.get())
                && hasNightmareAtmosphere(event.getLevel())) {
            event.getDrops().forEach(drop -> {
                ItemStack stack = drop.getItem();
                if (stack.is(WitcheryItems.WISPY_COTTON.get())) {
                    drop.setItem(new ItemStack(WitcheryItems.DISTURBED_COTTON.get(), stack.getCount()));
                }
            });
        }
    }

    static boolean isBannedSpiritDrop(ItemStack stack) {
        return stack.is(Items.ENDER_PEARL) || stack.is(Items.BLAZE_POWDER);
    }
}
