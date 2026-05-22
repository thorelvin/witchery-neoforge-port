package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitcheryMobDropEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        var rng = entity.getRandom();
        var pos = entity.blockPosition();
        var level = entity.level();
        Player killer = event.getSource().getEntity() instanceof Player player ? player : null;
        boolean arthana = killer != null && killer.getMainHandItem().is(WitcheryItems.ARTHANA.get());

        if (entity instanceof Bat && rng.nextFloat() < (arthana ? 0.75f : 0.33f)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.BAT_WOOL.get())));
        }

        if (entity instanceof Wolf && rng.nextFloat() < (arthana ? 0.75f : 0.33f)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.TONGUE_OF_DOG.get())));
        }

        if (entity instanceof Creeper) {
            float chance = Mth.clamp(arthana ? 0.08f : 0.02f, 0.0f, 1.0f);
            if (rng.nextFloat() < chance) {
                event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(WitcheryItems.CREEPER_HEART.get())));
            }
        }

        if (entity instanceof OwlFamiliarEntity
                && arthana
                && !hasDrop(event, WitcheryItems.OWLETS_WING.get())
                && rng.nextFloat() < 0.375f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.OWLETS_WING.get())));
        }

        if (entity instanceof ToadFamiliarEntity
                && arthana
                && !hasDrop(event, WitcheryItems.TOE_OF_FROG.get())
                && rng.nextFloat() < 0.375f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.TOE_OF_FROG.get())));
        }

        if (entity instanceof Witch && rng.nextFloat() < 0.4f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get())));
        }
    }

    private static boolean hasDrop(LivingDropsEvent event, Item item) {
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(item)) {
                return true;
            }
        }
        return false;
    }
}
