package com.emoniph.witchery.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight per-session bed taglock tracker.
 *
 * This restores the practical old "sleep in a bed, then tag the bed" flow
 * without introducing another large persistence subsystem in this batch.
 */
public final class BedTaglockSavedData {

    private static final Map<ResourceKey<Level>, Map<Long, BedTaglockEntry>> ENTRIES = new HashMap<>();

    private BedTaglockSavedData() {
    }

    public static Optional<BedTaglockEntry> getEntry(Level level, BlockPos pos) {
        return Optional.ofNullable(ENTRIES.getOrDefault(level.dimension(), Map.of()).get(pos.asLong()));
    }

    public static void setEntry(Level level, BlockPos pos, UUID uuid, String name) {
        ENTRIES.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(pos.asLong(), new BedTaglockEntry(uuid, name));
    }

    public static void clearEntry(Level level, BlockPos pos) {
        Map<Long, BedTaglockEntry> entries = ENTRIES.get(level.dimension());
        if (entries == null) {
            return;
        }
        entries.remove(pos.asLong());
        if (entries.isEmpty()) {
            ENTRIES.remove(level.dimension());
        }
    }

    public record BedTaglockEntry(UUID uuid, String name) {
    }
}
