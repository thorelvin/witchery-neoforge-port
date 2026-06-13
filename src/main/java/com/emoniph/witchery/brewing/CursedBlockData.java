package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-level store of triggered-dispersal block curses, keyed by position. The port does <b>not</b>
 * replace the vanilla button/lever/pressure-plate/door (the 1.7.10 approach) — that would force every
 * wood type to a single cursed look and a single revert target. Instead the curse lives here as data on
 * the block's position: the block stays exactly what it was (right wood, right state), and "reverting"
 * is simply dropping the record once the charges are spent. Mirrors {@code TileEntityCursedBlock}'s
 * stored brew/charges/thrower, persisted so curses survive a restart.
 */
public final class CursedBlockData extends SavedData {

    public record Curse(BrewContents brew, int charges, Optional<UUID> thrower) {
        static final Codec<Curse> CODEC = RecordCodecBuilder.create(i -> i.group(
                BrewContents.CODEC.fieldOf("brew").forGetter(Curse::brew),
                Codec.INT.fieldOf("charges").forGetter(Curse::charges),
                UUIDUtil.CODEC.optionalFieldOf("thrower").forGetter(Curse::thrower)
        ).apply(i, Curse::new));
    }

    private record Entry(BlockPos pos, Curse curse) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Curse.CODEC.fieldOf("curse").forGetter(Entry::curse)
        ).apply(i, Entry::new));
    }

    private final Map<BlockPos, Curse> curses = new HashMap<>();

    public static final Codec<CursedBlockData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("curses", List.of()).forGetter(d ->
                    d.curses.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList())
    ).apply(i, CursedBlockData::fromCodec));

    public static final SavedDataType<CursedBlockData> TYPE = new SavedDataType<>(
            "witchery_cursed_blocks", CursedBlockData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public CursedBlockData() {}

    private static CursedBlockData fromCodec(List<Entry> entries) {
        CursedBlockData d = new CursedBlockData();
        for (Entry e : entries) {
            d.curses.put(e.pos(), e.curse());
        }
        return d;
    }

    public static CursedBlockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isEmpty() {
        return curses.isEmpty();
    }

    /** Source {@code initalise}/{@code updateCurse}: a fresh curse starts at one charge; re-cursing the
     * same brew stacks a charge, a different brew resets. */
    public void curse(BlockPos pos, BrewContents brew, @Nullable UUID thrower) {
        BlockPos key = pos.immutable();
        Curse existing = curses.get(key);
        Optional<UUID> t = Optional.ofNullable(thrower);
        if (existing != null && existing.brew().items().equals(brew.items())) {
            curses.put(key, new Curse(brew, existing.charges() + 1, t));
        } else {
            curses.put(key, new Curse(brew, 1, t));
        }
        setDirty();
    }

    @Nullable
    public Curse get(BlockPos pos) {
        return curses.get(pos.immutable());
    }

    public void remove(BlockPos pos) {
        if (curses.remove(pos.immutable()) != null) {
            setDirty();
        }
    }

    /** Source {@code applyToEntityAndDestroy}'s {@code --count}: returns the active curse to apply, and
     * decrements it (removing the record when the last charge is spent). Null if nothing is cursed. */
    @Nullable
    public Curse takeCharge(BlockPos pos) {
        BlockPos key = pos.immutable();
        Curse c = curses.get(key);
        if (c == null) {
            return null;
        }
        int remaining = c.charges() - 1;
        if (remaining > 0) {
            curses.put(key, new Curse(c.brew(), remaining, c.thrower()));
        } else {
            curses.remove(key);
        }
        setDirty();
        return c;
    }

    public List<BlockPos> positions() {
        return new ArrayList<>(curses.keySet());
    }
}
