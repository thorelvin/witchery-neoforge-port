package com.emoniph.witchery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Persistent familiar state for a vanilla {@link net.minecraft.world.entity.monster.creaking.Creaking}. */
public record CreakingFamiliarData(
        Optional<UUID> owner,
        Mode mode,
        String guardDimension,
        long guardPos,
        String customName,
        boolean nameVisible,
        boolean watched,
        long lastAlertGameTime,
        int lastAlertCount,
        long nextResinGameTime
) {
    public static final long NO_GUARD_POS = Long.MIN_VALUE;
    public static final long NO_SCHEDULED_TICK = Long.MIN_VALUE;

    public static final CreakingFamiliarData EMPTY = new CreakingFamiliarData(
            Optional.empty(), Mode.FOLLOW, "", NO_GUARD_POS, "", false,
            false, NO_SCHEDULED_TICK, 0, NO_SCHEDULED_TICK);

    /**
     * Every field is optional so pre-specialization attachments remain readable. The legacy
     * {@code sitting} bit is deliberately retained as the migration source for old familiars.
     */
    public static final MapCodec<CreakingFamiliarData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("owner", "").forGetter(data -> data.owner.map(UUID::toString).orElse("")),
            Codec.STRING.optionalFieldOf("mode").forGetter(data -> Optional.of(data.mode.serializedName)),
            Codec.BOOL.optionalFieldOf("sitting", false).forGetter(CreakingFamiliarData::sitting),
            Codec.STRING.optionalFieldOf("guard_dimension", "").forGetter(CreakingFamiliarData::guardDimension),
            Codec.LONG.optionalFieldOf("guard_pos", NO_GUARD_POS).forGetter(CreakingFamiliarData::guardPos),
            Codec.STRING.optionalFieldOf("custom_name", "").forGetter(CreakingFamiliarData::customName),
            Codec.BOOL.optionalFieldOf("name_visible", false).forGetter(CreakingFamiliarData::nameVisible),
            Codec.BOOL.optionalFieldOf("watched", false).forGetter(CreakingFamiliarData::watched),
            Codec.LONG.optionalFieldOf("last_alert_game_time", NO_SCHEDULED_TICK)
                    .forGetter(CreakingFamiliarData::lastAlertGameTime),
            Codec.INT.optionalFieldOf("last_alert_count", 0).forGetter(CreakingFamiliarData::lastAlertCount),
            Codec.LONG.optionalFieldOf("next_resin_game_time", NO_SCHEDULED_TICK)
                    .forGetter(CreakingFamiliarData::nextResinGameTime)
    ).apply(instance, CreakingFamiliarData::decode));

    private static CreakingFamiliarData decode(String owner, Optional<String> encodedMode,
                                                boolean legacySitting, String guardDimension,
                                                long guardPos, String customName, boolean nameVisible,
                                                boolean watched, long lastAlertGameTime,
                                                int lastAlertCount, long nextResinGameTime) {
        Mode mode = encodedMode.map(Mode::fromSerializedName)
                .orElse(legacySitting ? Mode.ROOTED_GUARD : Mode.FOLLOW);
        return new CreakingFamiliarData(parseOwner(owner), mode,
                mode == Mode.ROOTED_GUARD ? guardDimension : "",
                mode == Mode.ROOTED_GUARD ? guardPos : NO_GUARD_POS,
                customName, nameVisible, watched, lastAlertGameTime,
                Math.max(0, lastAlertCount),
                mode == Mode.ROOTED_GUARD ? nextResinGameTime : NO_SCHEDULED_TICK);
    }

    private static Optional<UUID> parseOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(owner));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public boolean isBound() {
        return owner.isPresent();
    }

    public boolean isRootedGuard() {
        return mode == Mode.ROOTED_GUARD;
    }

    /** Compatibility summary used by the existing player familiar record and call rites. */
    public boolean sitting() {
        return isRootedGuard();
    }

    public Optional<BlockPos> guardPosition() {
        return guardPos == NO_GUARD_POS ? Optional.empty() : Optional.of(BlockPos.of(guardPos));
    }

    public boolean hasGuardAnchor(ResourceKey<Level> dimension) {
        return isRootedGuard()
                && guardPosition().isPresent()
                && guardDimension.equals(dimension.identifier().toString());
    }

    public CreakingFamiliarData withOwner(UUID ownerUuid) {
        return new CreakingFamiliarData(Optional.of(ownerUuid), mode, guardDimension, guardPos,
                customName, nameVisible, watched, lastAlertGameTime, lastAlertCount, nextResinGameTime);
    }

    public CreakingFamiliarData withSitting(boolean sitting) {
        return sitting ? withoutGuardAnchor(Mode.ROOTED_GUARD) : following();
    }

    public CreakingFamiliarData rootedAt(ResourceKey<Level> dimension, BlockPos pos, long nextResinTick) {
        return new CreakingFamiliarData(owner, Mode.ROOTED_GUARD,
                dimension.identifier().toString(), pos.asLong(), customName, nameVisible,
                watched, lastAlertGameTime, lastAlertCount, nextResinTick);
    }

    public CreakingFamiliarData following() {
        return new CreakingFamiliarData(owner, Mode.FOLLOW, "", NO_GUARD_POS,
                customName, nameVisible, watched, lastAlertGameTime, 0, NO_SCHEDULED_TICK);
    }

    public CreakingFamiliarData withWatched(boolean watched) {
        return new CreakingFamiliarData(owner, mode, guardDimension, guardPos,
                customName, nameVisible, watched, lastAlertGameTime, lastAlertCount, nextResinGameTime);
    }

    public CreakingFamiliarData withAlertState(long gameTime, int count) {
        return new CreakingFamiliarData(owner, mode, guardDimension, guardPos,
                customName, nameVisible, watched, gameTime, Math.max(0, count), nextResinGameTime);
    }

    public CreakingFamiliarData withAlertCount(int count) {
        return new CreakingFamiliarData(owner, mode, guardDimension, guardPos,
                customName, nameVisible, watched, lastAlertGameTime, Math.max(0, count), nextResinGameTime);
    }

    public CreakingFamiliarData withNextResinGameTime(long gameTime) {
        return new CreakingFamiliarData(owner, mode, guardDimension, guardPos,
                customName, nameVisible, watched, lastAlertGameTime, lastAlertCount, gameTime);
    }

    public CreakingFamiliarData withCustomName(@Nullable String name, boolean visible) {
        return new CreakingFamiliarData(owner, mode, guardDimension, guardPos,
                name == null ? "" : name, visible, watched,
                lastAlertGameTime, lastAlertCount, nextResinGameTime);
    }

    public CreakingFamiliarData cleared() {
        return EMPTY;
    }

    private CreakingFamiliarData withoutGuardAnchor(Mode newMode) {
        return new CreakingFamiliarData(owner, newMode, "", NO_GUARD_POS,
                customName, nameVisible, watched, lastAlertGameTime, 0, NO_SCHEDULED_TICK);
    }

    public enum Mode {
        FOLLOW("follow"),
        ROOTED_GUARD("rooted_guard");

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static Mode fromSerializedName(String value) {
            if (value == null) {
                return FOLLOW;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return ROOTED_GUARD.serializedName.equals(normalized) ? ROOTED_GUARD : FOLLOW;
        }
    }
}
