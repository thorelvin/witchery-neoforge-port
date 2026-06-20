package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-level store for warning-only cursed blocks. These reuse cursed-block trigger sensing, but do not
 * carry or consume brew charges; they are for atmospheric tells before a real trap.
 */
public final class CursedBlockWarningData extends SavedData {

    public record Warning(String message, int cooldownTicks) {
        static final Codec<Warning> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("message").forGetter(Warning::message),
                Codec.INT.optionalFieldOf("cooldownTicks", 100).forGetter(Warning::cooldownTicks)
        ).apply(i, Warning::new));
    }

    private record Entry(BlockPos pos, Warning warning) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Warning.CODEC.fieldOf("warning").forGetter(Entry::warning)
        ).apply(i, Entry::new));
    }

    private final Map<BlockPos, Warning> warnings = new HashMap<>();

    public static final Codec<CursedBlockWarningData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("warnings", List.of()).forGetter(d ->
                    d.warnings.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList())
    ).apply(i, CursedBlockWarningData::fromCodec));

    public static final SavedDataType<CursedBlockWarningData> TYPE = new SavedDataType<>(
            "witchery_cursed_block_warnings", CursedBlockWarningData::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public CursedBlockWarningData() {}

    private static CursedBlockWarningData fromCodec(List<Entry> entries) {
        CursedBlockWarningData d = new CursedBlockWarningData();
        for (Entry e : entries) {
            d.warnings.put(e.pos(), e.warning());
        }
        return d;
    }

    public static CursedBlockWarningData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isEmpty() {
        return warnings.isEmpty();
    }

    public void warn(BlockPos pos, String message, int cooldownTicks) {
        BrewWarningText warning = BrewWarningText.sanitize(message);
        if (warning.isEmpty()) {
            return;
        }
        warnings.put(pos.immutable(), new Warning(warning.message(), Math.max(20, cooldownTicks)));
        setDirty();
    }

    @Nullable
    public Warning get(BlockPos pos) {
        return warnings.get(pos.immutable());
    }

    public void remove(BlockPos pos) {
        if (warnings.remove(pos.immutable()) != null) {
            setDirty();
        }
    }

    public List<BlockPos> positions() {
        return new ArrayList<>(warnings.keySet());
    }
}
