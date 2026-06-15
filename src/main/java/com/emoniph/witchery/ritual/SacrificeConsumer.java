package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Staged ritual-sacrifice consumption, faithful to source {@code SacrificeItem.StepSacrificeItem}:
 * each required item is removed <b>one at a time, one per 20 ticks</b> (an {@code EXPLODE} particle +
 * a pop sound at the item), rather than every offering vanishing at once when the rite fires.
 *
 * <p>Each port consumption call ({@link RitualExecutor#consumeOfferedItem}, {@link RitualOffering#consumeOne()},
 * and the dropped trigger in {@code RitualEvents}) enqueues here instead of removing immediately; the
 * actual decrement happens at the scheduled pop. While an item is pending it is non-pickable and is
 * skipped by the offering finders, so the still-present items cannot be re-detected to re-fire the rite
 * or be grabbed mid-animation.
 *
 * <p>State is transient and per-dimension — this is a cosmetic stagger that completes within seconds, so
 * (unlike active-rite state) it is not persisted; a mid-animation reload simply leaves the items in world.
 */
final class SacrificeConsumer {
    private SacrificeConsumer() {
    }

    /** Source cadence: one sacrifice consumed every 20 ticks (StepSacrificeItem only acts on ticks % 20 == 0). */
    private static final int POP_INTERVAL = 20;

    private record Pending(RitualOffering offering, long popAt) {}

    private static final Map<ResourceKey<Level>, List<Pending>> QUEUES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Integer>> PENDING_IDS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<Runnable>> DRAIN_CALLBACKS = new HashMap<>();

    /** Schedule one offering to pop after the current queue drains (sequential, one per 20 ticks). */
    static void stage(RitualOffering offering) {
        if (offering == null) {
            return;
        }
        Level level = offering.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            offering.applyConsumption(); // no world to animate in (e.g. headless) — consume immediately
            return;
        }
        ResourceKey<Level> key = serverLevel.dimension();
        List<Pending> queue = QUEUES.computeIfAbsent(key, k -> new ArrayList<>());

        long popAt = serverLevel.getGameTime();
        for (Pending pending : queue) {
            popAt = Math.max(popAt, pending.popAt());
        }
        popAt += POP_INTERVAL;
        queue.add(new Pending(offering, popAt));

        ItemEntity item = offering.itemEntity();
        if (item != null) {
            // Hold the item un-pickable until just past its pop, then it (or its remainder) is grabbable again.
            item.setPickUpDelay((int) (popAt - serverLevel.getGameTime()) + 5);
            PENDING_IDS.computeIfAbsent(key, k -> new HashSet<>()).add(item.getId());
        }
    }

    /** Whether this dropped item is awaiting a staged pop (offering finders skip these). */
    static boolean isPending(Level level, ItemEntity item) {
        Set<Integer> ids = PENDING_IDS.get(level.dimension());
        return ids != null && ids.contains(item.getId());
    }

    /** Whether this dimension still has staged sacrifices waiting to disappear. */
    static boolean hasPending(ServerLevel level) {
        List<Pending> queue = QUEUES.get(level.dimension());
        return queue != null && !queue.isEmpty();
    }

    /**
     * Run after all staged sacrifices in this dimension have popped. The callback is always deferred
     * until a later tick, so activation code can enqueue the trigger item after the rite helper returns.
     */
    static void runWhenDrained(ServerLevel level, Runnable callback) {
        if (callback == null) {
            return;
        }
        DRAIN_CALLBACKS.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(callback);
    }

    /** Process any sacrifices due this tick. Call once per server-level tick. */
    static void tick(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        List<Pending> queue = QUEUES.get(key);
        if (queue == null || queue.isEmpty()) {
            runDrainCallbacksIfReady(level, key);
            return;
        }
        long now = level.getGameTime();
        Set<Integer> ids = PENDING_IDS.get(key);
        Iterator<Pending> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (now < pending.popAt()) {
                continue;
            }
            pop(level, pending.offering(), ids);
            iterator.remove();
        }
        if (queue.isEmpty()) {
            QUEUES.remove(key);
            PENDING_IDS.remove(key);
            runDrainCallbacksIfReady(level, key);
        }
    }

    /** Consume everything still pending immediately (server stop) so no offering is silently spared. */
    static void flush(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        List<Pending> queue = QUEUES.remove(key);
        Set<Integer> ids = PENDING_IDS.remove(key);
        if (queue != null) {
            for (Pending pending : queue) {
                pop(level, pending.offering(), ids);
            }
        }
        runDrainCallbacksIfReady(level, key);
    }

    private static void runDrainCallbacksIfReady(ServerLevel level, ResourceKey<Level> key) {
        if (hasPending(level)) {
            return;
        }
        List<Runnable> callbacks = DRAIN_CALLBACKS.remove(key);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (RuntimeException e) {
                WitcheryMod.LOGGER.error("Delayed ritual effect failed after sacrifices drained in {}", key, e);
            }
        }
    }

    private static void pop(ServerLevel level, RitualOffering offering, Set<Integer> ids) {
        ItemEntity item = offering.itemEntity();
        if (item != null && ids != null) {
            ids.remove(item.getId());
        }
        if (item != null && !item.isAlive()) {
            return; // grabbed/removed already — nothing to pop
        }
        Vec3 pos = offering.position();
        if (pos != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, BlockPos.containing(pos), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 0.7f);
        }
        offering.applyConsumption();
    }
}
