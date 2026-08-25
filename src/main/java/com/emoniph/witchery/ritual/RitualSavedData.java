package com.emoniph.witchery.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-level persistence for staged rituals and pending climate-change plans, so active rites (and the
 * sacrifices already consumed for them) survive a server restart or crash instead of vanishing.
 *
 * <p>Extracted verbatim from {@code RitualExecutor} as the second slice of the dispatcher split. The
 * executor still drives the ticking loop and holds the thin {@code activeRituals(level)} /
 * {@code climatePlans(level)} / {@code markRitualsDirty(level)} accessors; this file owns the state
 * model ({@link ActiveRitual}, {@link ActiveRitualType}, {@link ClimateChangePlan},
 * {@link ClimateWeather}) and the codecs that serialize it.
 */
public final class RitualSavedData extends SavedData {
    final List<ActiveRitual> activeRituals = new ArrayList<>();
    final Map<BlockPos, ClimateChangePlan> climatePlans = new HashMap<>();

    static <E extends Enum<E>> Codec<E> enumCodec(Class<E> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    public static final Codec<RitualSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ActiveRitual.CODEC.listOf().optionalFieldOf("active_rituals", List.of()).forGetter(d -> d.activeRituals),
            ClimatePlanEntry.CODEC.listOf().optionalFieldOf("climate_plans", List.of()).forGetter(d ->
                    d.climatePlans.entrySet().stream()
                            .map(e -> new ClimatePlanEntry(e.getKey(), e.getValue())).toList())
    ).apply(i, RitualSavedData::fromCodec));

    public static final SavedDataType<RitualSavedData> TYPE = new SavedDataType<>(
            "witchery_rituals", RitualSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public RitualSavedData() {}

    private static RitualSavedData fromCodec(List<ActiveRitual> rituals, List<ClimatePlanEntry> plans) {
        RitualSavedData d = new RitualSavedData();
        d.activeRituals.addAll(rituals);
        for (ClimatePlanEntry entry : plans) {
            d.climatePlans.put(entry.pos(), entry.plan());
        }
        return d;
    }

    static RitualSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Test accessor. */
    int activeRitualCount() {
        return activeRituals.size();
    }

    private record ClimatePlanEntry(BlockPos pos, ClimateChangePlan plan) {
        private static final Codec<ClimatePlanEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(ClimatePlanEntry::pos),
                ClimateChangePlan.CODEC.fieldOf("plan").forGetter(ClimatePlanEntry::plan)
        ).apply(i, ClimatePlanEntry::new));
    }
}

enum ActiveRitualType {
    WEATHER_CALL_STORM,
    FERTILITY,
    FORESTATION,
    ICE_SHELL,
    RAIN_OF_TOADS,
    BLINDNESS,
    BLIGHT,
    HELL_ON_EARTH,
    CALL_CREATURES,
    CLIMATE_CHANGE,
    NATURES_POWER,
    TRANSPOSE_ORES,
    MOVING_EARTH,
    RAISE_COLUMN,
    VOLCANO,
    PROTECTION_BARRIER_MONSTERS,
    PROTECTION_BARRIER_PORTABLE,
    PROTECTION_BARRIER,
    INFUSION_RECHARGE,
    SANCTITY_CIRCLE,
    IMPRISONMENT_CIRCLE
}

final class ActiveRitual {
    final ActiveRitualType type;
    final BlockPos center;
    final BlockPos effectCenter;
    final RitualCircleDetector.CircleSize circleSize;
    final int circleVariant;
    final int minRadius;
    final int maxRadius;
    final int targetStage;
    final int pulseInterval;
    final int covenSize;
    final float upkeepCost;
    final boolean enhanced;
    final ItemStack focus;
    final ItemStack sacrificedIngredient;
    final List<ItemStack> supplementalSacrifices;
    final String ownerName;
    final List<BlockPos> path = new ArrayList<>();
    final int heading;
    int stage;
    long nextGameTime;
    float upkeepDebt;
    boolean failedOnStop;
    boolean refundOnStop;

    ActiveRitual(ActiveRitualType type, BlockPos center, RitualCircleDetector.CircleSize circleSize,
                 BlockPos effectCenter, int circleVariant,
                 int minRadius, int maxRadius, int targetStage, int pulseInterval,
                 int covenSize, float upkeepCost, boolean enhanced, int heading, long nextGameTime,
                 ItemStack sacrificedIngredient, List<ItemStack> supplementalSacrifices, String ownerName) {
        this(type, center, circleSize, effectCenter, circleVariant, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, enhanced, heading, nextGameTime,
                sacrificedIngredient, supplementalSacrifices, ownerName, ItemStack.EMPTY);
    }

    ActiveRitual(ActiveRitualType type, BlockPos center, RitualCircleDetector.CircleSize circleSize,
                 BlockPos effectCenter, int circleVariant,
                 int minRadius, int maxRadius, int targetStage, int pulseInterval,
                 int covenSize, float upkeepCost, boolean enhanced, int heading, long nextGameTime,
                 ItemStack sacrificedIngredient, List<ItemStack> supplementalSacrifices, String ownerName,
                 ItemStack focus) {
        this.type = type;
        this.center = center.immutable();
        this.effectCenter = effectCenter.immutable();
        this.circleSize = circleSize;
        this.circleVariant = circleVariant;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.targetStage = targetStage;
        this.pulseInterval = pulseInterval;
        this.covenSize = covenSize;
        this.upkeepCost = upkeepCost;
        this.enhanced = enhanced;
        this.focus = focus == null ? ItemStack.EMPTY : focus.copy();
        this.heading = heading;
        this.nextGameTime = nextGameTime;
        this.sacrificedIngredient = sacrificedIngredient;
        this.supplementalSacrifices = supplementalSacrifices;
        this.ownerName = ownerName == null ? "" : ownerName;
    }

    static final Codec<ActiveRitual> CODEC = RecordCodecBuilder.create(i -> i.group(
            RitualSavedData.enumCodec(ActiveRitualType.class).fieldOf("type").forGetter(r -> r.type),
            BlockPos.CODEC.fieldOf("center").forGetter(r -> r.center),
            BlockPos.CODEC.fieldOf("effect_center").forGetter(r -> r.effectCenter),
            RitualSavedData.enumCodec(RitualCircleDetector.CircleSize.class).fieldOf("circle_size").forGetter(r -> r.circleSize),
            RitualGeometry.CODEC.fieldOf("geometry").forGetter(r -> new RitualGeometry(
                    r.circleVariant, r.minRadius, r.maxRadius, r.targetStage, r.pulseInterval, r.covenSize, r.heading)),
            Codec.FLOAT.fieldOf("upkeep_cost").forGetter(r -> r.upkeepCost),
            Codec.BOOL.fieldOf("enhanced").forGetter(r -> r.enhanced),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("focus", ItemStack.EMPTY).forGetter(r -> r.focus),
            Codec.LONG.fieldOf("next_game_time").forGetter(r -> r.nextGameTime),
            ItemStack.OPTIONAL_CODEC.fieldOf("sacrificed").forGetter(r -> r.sacrificedIngredient),
            ItemStack.CODEC.listOf().fieldOf("supplemental").forGetter(r -> r.supplementalSacrifices),
            Codec.STRING.optionalFieldOf("owner", "").forGetter(r -> r.ownerName),
            BlockPos.CODEC.listOf().fieldOf("path").forGetter(r -> r.path),
            Codec.INT.fieldOf("stage").forGetter(r -> r.stage),
            Codec.FLOAT.fieldOf("upkeep_debt").forGetter(r -> r.upkeepDebt)
    ).apply(i, ActiveRitual::fromCodec));

    private static ActiveRitual fromCodec(ActiveRitualType type, BlockPos center, BlockPos effectCenter,
            RitualCircleDetector.CircleSize circleSize, RitualGeometry geom, float upkeepCost, boolean enhanced,
            ItemStack focus, long nextGameTime, ItemStack sacrificed, List<ItemStack> supplemental, String ownerName,
            List<BlockPos> path, int stage, float upkeepDebt) {
        ActiveRitual r = new ActiveRitual(type, center, circleSize, effectCenter, geom.circleVariant(),
                geom.minRadius(), geom.maxRadius(), geom.targetStage(), geom.pulseInterval(), geom.covenSize(),
                upkeepCost, enhanced, geom.heading(), nextGameTime, sacrificed, List.copyOf(supplemental), ownerName,
                focus);
        r.stage = stage;
        r.upkeepDebt = upkeepDebt;
        r.path.addAll(path);
        return r;
    }

    private record RitualGeometry(int circleVariant, int minRadius, int maxRadius, int targetStage,
                                  int pulseInterval, int covenSize, int heading) {
        private static final Codec<RitualGeometry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("variant").forGetter(RitualGeometry::circleVariant),
                Codec.INT.fieldOf("min_radius").forGetter(RitualGeometry::minRadius),
                Codec.INT.fieldOf("max_radius").forGetter(RitualGeometry::maxRadius),
                Codec.INT.fieldOf("target_stage").forGetter(RitualGeometry::targetStage),
                Codec.INT.fieldOf("pulse_interval").forGetter(RitualGeometry::pulseInterval),
                Codec.INT.fieldOf("coven_size").forGetter(RitualGeometry::covenSize),
                Codec.INT.fieldOf("heading").forGetter(RitualGeometry::heading)
        ).apply(i, RitualGeometry::new));
    }
}

enum ClimateWeather {
    NONE,
    SUN,
    RAIN
}

record ClimateChangePlan(Holder<Biome> biomeHolder, ClimateWeather weather) {
    static final Codec<ClimateChangePlan> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryFixedCodec.create(Registries.BIOME).fieldOf("biome").forGetter(ClimateChangePlan::biomeHolder),
            RitualSavedData.enumCodec(ClimateWeather.class).fieldOf("weather").forGetter(ClimateChangePlan::weather)
    ).apply(i, ClimateChangePlan::new));
}
