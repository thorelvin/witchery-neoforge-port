package com.emoniph.witchery.events;

import com.emoniph.witchery.entity.MindrakeEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * Mindrake Bulbs are alive: a bulb left on the ground sprouts into a Mindrake. Tossing one yourself
 * makes it hatch quickly (3s) and tames the hatchling to you, so it fights at your side.
 *
 * Source parity: GenericEvents.onItemToss / onItemExpireEvent (1.7.10-0.24.1).
 */
@EventBusSubscriber(modid = "witchery")
public final class MindrakeEvents {

    private static final int THROWN_LIFESPAN_TICKS = 60; // 3 seconds

    private MindrakeEvents() {
    }

    /** A thrown bulb gets a short fuse, and remembers who threw it (for taming on hatch). */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity item = event.getEntity();
        if (isBulb(item.getItem())) {
            item.lifespan = THROWN_LIFESPAN_TICKS;
            item.setThrower(event.getPlayer());
        }
    }

    /** When the bulb's life runs out, it hatches — one Mindrake per bulb in the stack. */
    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity item = event.getEntity();
        if (!isBulb(item.getItem()) || !(item.level() instanceof ServerLevel level)) {
            return;
        }

        Entity thrower = item.getOwner();
        Player owner = thrower instanceof Player p ? p : null;
        int count = item.getItem().getCount();

        for (int i = 0; i < count; i++) {
            MindrakeEntity mindrake = WitcheryEntities.MINDRAKE.get().create(level, EntitySpawnReason.EVENT);
            if (mindrake == null) {
                continue;
            }
            mindrake.snapTo(item.getX(), item.getY(), item.getZ(), 0.0F, 0.0F);
            mindrake.finalizeSpawn(level, level.getCurrentDifficultyAt(mindrake.blockPosition()),
                    EntitySpawnReason.EVENT, null);
            if (owner != null) {
                mindrake.setPersistenceRequired();
                mindrake.tame(owner);
            }
            level.addFreshEntity(mindrake);
            level.sendParticles(ParticleTypes.POOF, mindrake.getX(), mindrake.getY() + 0.3, mindrake.getZ(),
                    8, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, mindrake.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.NEUTRAL, 0.5F, 1.6F);
        }
    }

    private static boolean isBulb(ItemStack stack) {
        return stack.is(WitcheryItems.MINDRAKE_BULB.get());
    }
}
