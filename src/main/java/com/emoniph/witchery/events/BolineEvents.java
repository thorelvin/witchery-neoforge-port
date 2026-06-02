package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class BolineEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakBloodRose(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!event.getState().is(WitcheryBlocks.BLOOD_ROSE.get())) {
            return;
        }
        if (!event.getPlayer().getMainHandItem().is(WitcheryItems.BOLINE.get())) {
            return;
        }

        Block.popResource(level, event.getPos(), new ItemStack(WitcheryItems.BLOOD_ROSE.get()));
    }
}
