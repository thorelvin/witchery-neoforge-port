package com.emoniph.witchery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WitcheryPlayerData {

    /** Nested familiar state keeps the main player codec under the 16-field DFU limit. */
    public static class FamiliarState {
        public String uuid = "";
        public String type = "";
        public String name = "";
        public boolean sitting = false;
        public boolean nameVisible = false;

        public FamiliarState() {}

        private FamiliarState(String uuid, String type, String name, boolean sitting, boolean nameVisible) {
            this.uuid = uuid == null ? "" : uuid;
            this.type = type == null ? "" : type;
            this.name = name == null ? "" : name;
            this.sitting = sitting;
            this.nameVisible = nameVisible;
        }

        public static final Codec<FamiliarState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("uuid", "").forGetter(s -> s.uuid),
            Codec.STRING.optionalFieldOf("type", "").forGetter(s -> s.type),
            Codec.STRING.optionalFieldOf("name", "").forGetter(s -> s.name),
            Codec.BOOL.optionalFieldOf("sitting", false).forGetter(s -> s.sitting),
            Codec.BOOL.optionalFieldOf("name_visible", false).forGetter(s -> s.nameVisible)
        ).apply(i, FamiliarState::new));
    }

    /** Nested infusion charge state keeps the main player codec within the 16-field DFU limit. */
    public static class InfusionChargeState {
        public int currentEnergy = 0;
        public int maxEnergy = 0;

        public InfusionChargeState() {}

        private InfusionChargeState(int currentEnergy, int maxEnergy) {
            this.maxEnergy = Math.max(0, maxEnergy);
            this.currentEnergy = Math.clamp(currentEnergy, 0, this.maxEnergy);
        }

        public static final Codec<InfusionChargeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("current_energy", 0).forGetter(s -> s.currentEnergy),
            Codec.INT.optionalFieldOf("max_energy", 0).forGetter(s -> s.maxEnergy)
        ).apply(i, InfusionChargeState::new));
    }

    public enum VampirePower {
        NONE(0, 0, 0),
        DRINK(0, 0, 1),
        MESMERIZE(50, 0, 2),
        SPEED(10, 0, 4),
        BAT(50, 1, 7),
        ULTIMATE(50, 0, 10);

        private static final int[] MAX_POWER_BY_LEVEL = {0, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5};

        private final int initialCost;
        private final int upkeepCost;
        private final int unlockLevel;

        VampirePower(int initialCost, int upkeepCost, int unlockLevel) {
            this.initialCost = initialCost;
            this.upkeepCost = upkeepCost;
            this.unlockLevel = unlockLevel;
        }

        public int initialCost() { return initialCost; }
        public int upkeepCost() { return upkeepCost; }
        public int unlockLevel() { return unlockLevel; }

        public static VampirePower fromOrdinal(int ordinal) {
            VampirePower[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }

        public static int maxOrdinalForLevel(int level) {
            return MAX_POWER_BY_LEVEL[Math.clamp(level, 0, MAX_POWER_BY_LEVEL.length - 1)];
        }
    }

    public enum VampireUltimate {
        NONE, STORM, SWARM, FARM;
        public static VampireUltimate fromOrdinal(int i) {
            VampireUltimate[] vals = values();
            return i >= 0 && i < vals.length ? vals[i] : NONE;
        }
    }

    /** Nested vampire progression state — keeps the main CODEC under the 16-field DFU limit. */
    public static class VampireState {
        public boolean vampire = false;
        public int level = 0;
        public int ultimate = 0;
        public int ultimateCharges = 0;
        public int bloodPower = 0;
        public int bloodReserve = 0;
        public int humanBlood = 0;
        public boolean batForm = false;
        // Max level the player can reach without quest-gating items; 10 = uncapped during dev.
        public int levelCap = 10;
        public int questCounter = 0;
        // Distinct village cells already visited for the level 7→8 quest (source visitedVampireChunks).
        public final java.util.List<Long> visitedVillages = new java.util.ArrayList<>();

        public VampireState() {}

        private VampireState(boolean vampire, int level, int ultimate, int ultimateCharges,
                             int bloodPower, int bloodReserve, int humanBlood, boolean batForm,
                             int levelCap, int questCounter,
                             java.util.List<Long> visitedVillages) {
            this.vampire = vampire;
            this.level = Math.clamp(level, 0, 10);
            this.ultimate = Math.clamp(ultimate, 0, 3);
            this.ultimateCharges = Math.max(0, ultimateCharges);
            this.bloodPower = Math.max(0, bloodPower);
            this.bloodReserve = Math.clamp(bloodReserve, 0, 250);
            this.humanBlood = Math.clamp(humanBlood, 0, 500);
            this.batForm = batForm && this.vampire && this.level >= VampirePower.BAT.unlockLevel();
            this.levelCap = Math.clamp(levelCap, 1, 10);
            this.questCounter = Math.max(0, questCounter);
            this.visitedVillages.addAll(visitedVillages);
        }

        public static final Codec<VampireState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("vampire", false).forGetter(s -> s.vampire),
            Codec.INT.optionalFieldOf("level", 0).forGetter(s -> s.level),
            Codec.INT.optionalFieldOf("ultimate", 0).forGetter(s -> s.ultimate),
            Codec.INT.optionalFieldOf("ultimate_charges", 0).forGetter(s -> s.ultimateCharges),
            Codec.INT.optionalFieldOf("blood_power", 0).forGetter(s -> s.bloodPower),
            Codec.INT.optionalFieldOf("blood_reserve", 0).forGetter(s -> s.bloodReserve),
            Codec.INT.optionalFieldOf("human_blood", 0).forGetter(s -> s.humanBlood),
            Codec.BOOL.optionalFieldOf("bat_form", false).forGetter(s -> s.batForm),
            Codec.INT.optionalFieldOf("level_cap", 10).forGetter(s -> s.levelCap),
            Codec.INT.optionalFieldOf("quest_counter", 0).forGetter(s -> s.questCounter),
            Codec.LONG.listOf().optionalFieldOf("visited_villages", java.util.List.of()).forGetter(s -> s.visitedVillages)
        ).apply(i, VampireState::new));
    }

    /** Active werewolf transform shape (source {@code TransformCreature} WOLF/WOLFMAN subset). */
    public enum WolfForm {
        NONE, WOLF, WOLFMAN;
        public static WolfForm fromOrdinal(int i) {
            WolfForm[] vals = values();
            return i >= 0 && i < vals.length ? vals[i] : NONE;
        }
    }

    /** Wolf-Altar quest step state (source {@code ExtendedPlayer.QuestState}). */
    public enum WolfQuestState {
        NOT_STARTED, STARTED, COMPLETE;
        public static WolfQuestState fromOrdinal(int i) {
            WolfQuestState[] vals = values();
            return i >= 0 && i < vals.length ? vals[i] : NOT_STARTED;
        }
    }

    /**
     * Nested lycanthropy progression state — mirrors {@link VampireState} and keeps the main CODEC under
     * the 16-field DFU limit (it replaces the former flat {@code wolf_mastery} 0-3 slot). Holds the source
     * {@code werewolfLevel} (0-10), the active {@link WolfForm}, the Wolf-Altar quest step + counter, the
     * persisted distinct-chunk set for the level 6→7 howl quest, and the howl/bone-find timers.
     */
    public static class WerewolfState {
        public int level = 0;
        public int form = 0;            // WolfForm ordinal
        public int questState = 0;      // WolfQuestState ordinal
        public int questCounter = 0;
        public final java.util.List<Long> howledChunks = new java.util.ArrayList<>();
        public long lastHowl = Long.MIN_VALUE;
        public long lastBoneFind = Long.MIN_VALUE;

        public WerewolfState() {}

        private WerewolfState(int level, int form, int questState, int questCounter,
                              java.util.List<Long> howledChunks, long lastHowl, long lastBoneFind) {
            this.level = Math.clamp(level, 0, 10);
            this.form = Math.clamp(form, 0, 2);
            this.questState = Math.clamp(questState, 0, 2);
            this.questCounter = Math.max(0, questCounter);
            this.howledChunks.addAll(howledChunks);
            this.lastHowl = lastHowl;
            this.lastBoneFind = lastBoneFind;
        }

        public static final Codec<WerewolfState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("level", 0).forGetter(s -> s.level),
            Codec.INT.optionalFieldOf("form", 0).forGetter(s -> s.form),
            Codec.INT.optionalFieldOf("quest_state", 0).forGetter(s -> s.questState),
            Codec.INT.optionalFieldOf("quest_counter", 0).forGetter(s -> s.questCounter),
            Codec.LONG.listOf().optionalFieldOf("howled_chunks", java.util.List.of()).forGetter(s -> s.howledChunks),
            Codec.LONG.optionalFieldOf("last_howl", Long.MIN_VALUE).forGetter(s -> s.lastHowl),
            Codec.LONG.optionalFieldOf("last_bone_find", Long.MIN_VALUE).forGetter(s -> s.lastBoneFind)
        ).apply(i, WerewolfState::new));
    }

    /** One return step for a player travelling through ordinary or nested Mirror World rooms. */
    public record MirrorRoute(String sourceDimension, long sourceMirrorPos, int destinationRoomIndex) {
        public static final Codec<MirrorRoute> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("source_dimension").forGetter(MirrorRoute::sourceDimension),
                Codec.LONG.fieldOf("source_mirror_pos").forGetter(MirrorRoute::sourceMirrorPos),
                Codec.INT.fieldOf("destination_room").forGetter(MirrorRoute::destinationRoomIndex)
        ).apply(i, MirrorRoute::new));

        public BlockPos sourceBlockPos() {
            return BlockPos.of(sourceMirrorPos);
        }
    }

    /** Persisted breadcrumb stack for nested Mirror World travel. */
    public static class MirrorWorldState {
        public final List<MirrorRoute> routes = new ArrayList<>();

        public MirrorWorldState() {}

        private MirrorWorldState(List<MirrorRoute> routes) {
            this.routes.addAll(routes);
        }

        public MirrorWorldState copy() {
            return new MirrorWorldState(routes);
        }

        public static final Codec<MirrorWorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
                MirrorRoute.CODEC.listOf().fieldOf("routes").forGetter(s -> s.routes)
        ).apply(i, MirrorWorldState::new));
    }

    /** Separate mortal and dream-side inventories/vitals used while spirit walking. */
    public static class DreamStorageState {
        public String overworldInventorySnbt = "";
        public String spiritInventorySnbt = "";
        public float overworldHealth = 20f;
        public int overworldFood = 20;
        public float overworldSat = 5f;
        public float spiritHealth = 20f;
        public int spiritFood = 20;
        public float spiritSat = 5f;
        public boolean spiritVitalsInitialized = false;

        public DreamStorageState() {}

        private DreamStorageState(String overworldInventorySnbt, String spiritInventorySnbt,
                                  float overworldHealth, int overworldFood, float overworldSat,
                                  float spiritHealth, int spiritFood, float spiritSat,
                                  boolean spiritVitalsInitialized) {
            this.overworldInventorySnbt = overworldInventorySnbt == null ? "" : overworldInventorySnbt;
            this.spiritInventorySnbt = spiritInventorySnbt == null ? "" : spiritInventorySnbt;
            this.overworldHealth = Math.max(1f, overworldHealth);
            this.overworldFood = Math.clamp(overworldFood, 0, 20);
            this.overworldSat = Math.max(0f, overworldSat);
            this.spiritHealth = Math.max(1f, spiritHealth);
            this.spiritFood = Math.clamp(spiritFood, 0, 20);
            this.spiritSat = Math.max(0f, spiritSat);
            this.spiritVitalsInitialized = spiritVitalsInitialized;
        }

        public DreamStorageState copy() {
            return new DreamStorageState(overworldInventorySnbt, spiritInventorySnbt,
                    overworldHealth, overworldFood, overworldSat,
                    spiritHealth, spiritFood, spiritSat, spiritVitalsInitialized);
        }

        public static final Codec<DreamStorageState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("overworld_inventory").forGetter(s -> s.overworldInventorySnbt),
                Codec.STRING.fieldOf("spirit_inventory").forGetter(s -> s.spiritInventorySnbt),
                Codec.FLOAT.fieldOf("overworld_health").forGetter(s -> s.overworldHealth),
                Codec.INT.fieldOf("overworld_food").forGetter(s -> s.overworldFood),
                Codec.FLOAT.fieldOf("overworld_saturation").forGetter(s -> s.overworldSat),
                Codec.FLOAT.fieldOf("spirit_health").forGetter(s -> s.spiritHealth),
                Codec.INT.fieldOf("spirit_food").forGetter(s -> s.spiritFood),
                Codec.FLOAT.fieldOf("spirit_saturation").forGetter(s -> s.spiritSat),
                Codec.BOOL.fieldOf("spirit_vitals_initialized").forGetter(s -> s.spiritVitalsInitialized)
        ).apply(i, DreamStorageState::new));
    }

    /** Nested record for Spirit World and cross-dimension transit state. */
    public static class SpiritWorldState {
        public boolean isDream = false;
        public DreamStorageState dreamStorage = new DreamStorageState();
        public String corpseUUID = "";
        public boolean manifestedGhost = false;
        public int manifestationTime = 0;
        public boolean manifestationSkipTick = false;
        public int wakingNightmareLevel = 0;
        public boolean otherwhereHearthTraveler = false;
        public long tormentReturnPos = Long.MIN_VALUE;
        public String tormentReturnDimension = "";
        public String tormentSession = "";
        public MirrorWorldState mirrorWorld = new MirrorWorldState();

        public SpiritWorldState() {}

        private SpiritWorldState(boolean isDream, DreamStorageState dreamStorage,
                                 String corpseUUID, boolean manifestedGhost,
                                 int manifestationTime, boolean manifestationSkipTick,
                                  int wakingNightmareLevel,
                                  boolean otherwhereHearthTraveler, long tormentReturnPos,
                                  String tormentReturnDimension, String tormentSession,
                                  MirrorWorldState mirrorWorld) {
            this.isDream = isDream;
            this.dreamStorage = dreamStorage == null ? new DreamStorageState() : dreamStorage.copy();
            this.corpseUUID = corpseUUID == null ? "" : corpseUUID;
            this.manifestedGhost = manifestedGhost;
            this.manifestationTime = Math.max(0, manifestationTime);
            this.manifestationSkipTick = manifestationSkipTick;
            this.wakingNightmareLevel = Math.max(0, wakingNightmareLevel);
            this.otherwhereHearthTraveler = otherwhereHearthTraveler;
            this.tormentReturnPos = tormentReturnPos;
            this.tormentReturnDimension = tormentReturnDimension == null ? "" : tormentReturnDimension;
            this.tormentSession = tormentSession == null ? "" : tormentSession;
            this.mirrorWorld = mirrorWorld == null ? new MirrorWorldState() : mirrorWorld.copy();
        }

        public SpiritWorldState copy() {
            return new SpiritWorldState(
                    isDream,
                    dreamStorage,
                    corpseUUID,
                    manifestedGhost,
                    manifestationTime,
                    manifestationSkipTick,
                    wakingNightmareLevel,
                    otherwhereHearthTraveler,
                    tormentReturnPos,
                    tormentReturnDimension,
                    tormentSession,
                    mirrorWorld);
        }

        public static final Codec<SpiritWorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("is_dream", false).forGetter(s -> s.isDream),
            DreamStorageState.CODEC.fieldOf("dream_storage").forGetter(s -> s.dreamStorage),
            Codec.STRING.optionalFieldOf("corpse_uuid", "").forGetter(s -> s.corpseUUID),
            Codec.BOOL.optionalFieldOf("manifested_ghost", false).forGetter(s -> s.manifestedGhost),
            Codec.INT.optionalFieldOf("manifest_time", 0).forGetter(s -> s.manifestationTime),
            Codec.BOOL.optionalFieldOf("manifest_skip_tick", false).forGetter(s -> s.manifestationSkipTick),
            Codec.INT.optionalFieldOf("waking_nightmare_level", 0).forGetter(s -> s.wakingNightmareLevel),
            Codec.BOOL.optionalFieldOf("otherwhere_hearth_traveler", false)
                    .forGetter(s -> s.otherwhereHearthTraveler),
            Codec.LONG.optionalFieldOf("torment_return_pos", Long.MIN_VALUE)
                    .forGetter(s -> s.tormentReturnPos),
            Codec.STRING.optionalFieldOf("torment_return_dim", "")
                    .forGetter(s -> s.tormentReturnDimension),
            Codec.STRING.optionalFieldOf("torment_session", "")
                    .forGetter(s -> s.tormentSession),
            MirrorWorldState.CODEC.fieldOf("mirror_world").forGetter(s -> s.mirrorWorld)
        ).apply(i, SpiritWorldState::new));
    }

    private VampireState vs = new VampireState();
    private boolean werewolf = false;
    private int bloodLevel = 0;
    private float transformationLevel = 0f;
    private int selectedVampirePower = VampirePower.NONE.ordinal();
    private boolean vampireVisionActive = false;
    private int vampirePowerCooldown = 0;
    private FamiliarState familiar = new FamiliarState();
    private String infusionType = "";
    private InfusionChargeState infusionChargeState = new InfusionChargeState();
    private long lastDeathPos = Long.MIN_VALUE;
    private String lastDeathDimension = "";
    private long returnPos = Long.MIN_VALUE;
    private String returnDimension = "";
    private boolean inSpiritWorld = false;
    private SpiritWorldState sw = new SpiritWorldState();
    private String creaturePowerType = "";
    private int creaturePowerCharges = 0;
    private WerewolfState wws = new WerewolfState();
    private int soaringTicks = 0;
    private int graveTicks = 0;
    private long lastGraveUseGameTime = Long.MIN_VALUE;
    private int depthsTicks = 0;
    private int grotesqueTicks = 0; // transient — not persisted via CODEC
    // Mystic Branch symbol magic — transient server-side scratch for the spell the
    // player has drawn during the current use. -1 = nothing prepared. Not in the CODEC.
    private int preparedSpellId = -1;
    private int preparedSpellLevel = 1;
    private boolean ghostWalkerActive = false;
    private int tormentPortalCooldown = 0;
    private boolean demonicNightmare = false;
    private long lastNightmareKillGameTime = Long.MIN_VALUE;
    private long witchHunterTriggerGameTime = Long.MIN_VALUE;

    public WitcheryPlayerData() {}

    private WitcheryPlayerData(VampireState vs, boolean werewolf, int bloodLevel,
                               float transformationLevel, FamiliarState familiar,
                               String infusionType, InfusionChargeState infusionChargeState,
                               long lastDeathPos, String lastDeathDimension,
                               long returnPos, String returnDimension,
                               boolean inSpiritWorld, SpiritWorldState sw,
                               String creaturePowerType, int creaturePowerCharges,
                               WerewolfState wws) {
        this.vs = vs != null ? vs : new VampireState();
        this.werewolf = werewolf;
        this.bloodLevel = bloodLevel;
        this.transformationLevel = transformationLevel;
        this.familiar = familiar != null ? familiar : new FamiliarState();
        this.infusionType = infusionType;
        this.infusionChargeState = infusionChargeState != null ? infusionChargeState : new InfusionChargeState();
        this.lastDeathPos = lastDeathPos;
        this.lastDeathDimension = lastDeathDimension;
        this.returnPos = returnPos;
        this.returnDimension = returnDimension;
        this.inSpiritWorld = inSpiritWorld;
        this.sw = sw != null ? sw.copy() : new SpiritWorldState();
        this.creaturePowerType = creaturePowerType;
        this.creaturePowerCharges = creaturePowerCharges;
        this.wws = wws != null ? wws : new WerewolfState();
    }

    private static WitcheryPlayerData fromCodec(VampireState vs, boolean werewolf, int bloodLevel,
                                                float transformationLevel, FamiliarState familiar,
                                                String infusionType, InfusionChargeState infusionChargeState,
                                                long lastDeathPos, String lastDeathDimension,
                                                long returnPos, String returnDimension,
                                                boolean inSpiritWorld, SpiritWorldState sw,
                                                String creaturePowerType, int creaturePowerCharges,
                                                WerewolfState wws) {
        return new WitcheryPlayerData(vs, werewolf, bloodLevel, transformationLevel,
                familiar, infusionType, infusionChargeState, lastDeathPos, lastDeathDimension, returnPos, returnDimension,
                inSpiritWorld, sw, creaturePowerType, creaturePowerCharges, wws);
    }

    public static final com.mojang.serialization.MapCodec<WitcheryPlayerData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        VampireState.CODEC.optionalFieldOf("vampire_state", new VampireState()).forGetter(d -> d.vs),
        Codec.BOOL.optionalFieldOf("werewolf", false).forGetter(d -> d.werewolf),
        Codec.INT.optionalFieldOf("blood_level", 0).forGetter(d -> d.bloodLevel),
        Codec.FLOAT.optionalFieldOf("transformation_level", 0f).forGetter(d -> d.transformationLevel),
        FamiliarState.CODEC.optionalFieldOf("familiar", new FamiliarState()).forGetter(d -> d.familiar),
        Codec.STRING.optionalFieldOf("infusion_type", "").forGetter(d -> d.infusionType),
        InfusionChargeState.CODEC.optionalFieldOf("infusion_charge_state", new InfusionChargeState()).forGetter(d -> d.infusionChargeState),
        Codec.LONG.optionalFieldOf("last_death_pos", Long.MIN_VALUE).forGetter(d -> d.lastDeathPos),
        Codec.STRING.optionalFieldOf("last_death_dim", "").forGetter(d -> d.lastDeathDimension),
        Codec.LONG.optionalFieldOf("return_pos", Long.MIN_VALUE).forGetter(d -> d.returnPos),
        Codec.STRING.optionalFieldOf("return_dim", "").forGetter(d -> d.returnDimension),
        Codec.BOOL.optionalFieldOf("in_spirit_world", false).forGetter(d -> d.inSpiritWorld),
        SpiritWorldState.CODEC.optionalFieldOf("spirit_world", new SpiritWorldState()).forGetter(d -> d.sw),
        Codec.STRING.optionalFieldOf("creature_power_type", "").forGetter(d -> d.creaturePowerType),
        Codec.INT.optionalFieldOf("creature_power_charges", 0).forGetter(d -> d.creaturePowerCharges),
        WerewolfState.CODEC.optionalFieldOf("werewolf_state", new WerewolfState()).forGetter(d -> d.wws)
    ).apply(i, WitcheryPlayerData::fromCodec));

    public boolean isVampire() { return vs.vampire; }
    public int getVampireLevel() { return vs.level; }
    public VampireUltimate getVampireUltimate() { return VampireUltimate.fromOrdinal(vs.ultimate); }
    public int getVampireUltimateCharges() { return vs.ultimateCharges; }
    public VampirePower getSelectedVampirePower() { return VampirePower.fromOrdinal(selectedVampirePower); }
    public int getMaxAvailableVampirePowerOrdinal() { return VampirePower.maxOrdinalForLevel(vs.level); }
    public boolean isVampireVisionActive() { return isVampire() && vs.level >= 2 && vampireVisionActive; }
    public boolean isVampireBatForm() { return isVampire() && vs.batForm; }
    public int getVampirePowerCooldown() { return vampirePowerCooldown; }
    public boolean isWerewolf() { return werewolf; }
    public int getBloodLevel() { return bloodLevel; }
    public int getHumanBlood() { return vs.humanBlood; }
    public float getTransformationLevel() { return transformationLevel; }

    public Optional<UUID> getFamiliarUUID() {
        if (familiar.uuid.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(familiar.uuid)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public String getFamiliarType() { return familiar.type; }
    public String getFamiliarName() { return familiar.name; }
    public boolean isFamiliarSitting() { return familiar.sitting; }
    public boolean isFamiliarNameVisible() { return familiar.nameVisible; }

    public InfusionType getInfusionType() { return InfusionType.fromString(infusionType); }
    public int getInfusionEnergy() { return infusionChargeState.currentEnergy; }
    public int getInfusionMaxEnergy() { return infusionChargeState.maxEnergy; }

    public void setVampire(boolean v) {
        vs.vampire = v;
        if (v && vs.level == 0) {
            vs.level = 1;
            vs.bloodPower = 125;
        }
        if (v) vs.humanBlood = 0;
        if (!v) {
            vs.level = 0;
            vs.ultimate = 0;
            vs.ultimateCharges = 0;
            vs.bloodPower = 0;
            vs.bloodReserve = 0;
            vs.humanBlood = 50;
            vs.batForm = false;
            vs.questCounter = 0;
            vs.visitedVillages.clear();
            selectedVampirePower = VampirePower.NONE.ordinal();
            vampireVisionActive = false;
            vampirePowerCooldown = 0;
        }
    }
    public void setVampireLevel(int level) {
        int clamped = Math.clamp(level, 0, 10);
        if (clamped != vs.level) {
            vs.level = clamped;
            vs.questCounter = 0;
            vs.visitedVillages.clear();
            vs.bloodPower = Math.min(vs.bloodPower, getMaxBloodPower());
            selectedVampirePower = VampirePower.NONE.ordinal();
            vampirePowerCooldown = 0;
            if (clamped == 1) vs.bloodPower = 125;
            if (clamped < VampirePower.BAT.unlockLevel()) vs.batForm = false;
            if (clamped < VampirePower.MESMERIZE.unlockLevel()) vampireVisionActive = false;
        }
    }
    public boolean setSelectedVampirePower(VampirePower power) {
        VampirePower requested = power == null ? VampirePower.NONE : power;
        if (!isVampire() || requested.ordinal() > getMaxAvailableVampirePowerOrdinal()) {
            requested = VampirePower.NONE;
        }
        int ordinal = requested.ordinal();
        boolean changed = selectedVampirePower != ordinal;
        selectedVampirePower = ordinal;
        return changed;
    }
    public void setVampireVisionActive(boolean active) {
        vampireVisionActive = active && isVampire() && vs.level >= VampirePower.MESMERIZE.unlockLevel();
    }
    public void setVampireBatForm(boolean active) {
        vs.batForm = active && isVampire() && vs.level >= VampirePower.BAT.unlockLevel();
    }
    public void setVampirePowerCooldown(int ticks) { vampirePowerCooldown = Math.max(0, ticks); }
    public void tickVampirePowerCooldown() {
        if (vampirePowerCooldown > 0) vampirePowerCooldown--;
    }
    public void setVampireUltimate(VampireUltimate ultimate, int charges) {
        vs.ultimate = ultimate == null ? 0 : ultimate.ordinal();
        vs.ultimateCharges = Math.max(0, charges);
    }
    public boolean spendVampireUltimateCharge() {
        if (vs.ultimateCharges <= 0) return false;
        vs.ultimateCharges--;
        return true;
    }
    public void clearVampireUltimate() { vs.ultimate = 0; vs.ultimateCharges = 0; }
    public int getBloodPower() { return vs.bloodPower; }
    // Source ExtendedPlayer.getMaxBloodPower: an established werewolf-hybrid (wolf mastery >= 2, the
    // port's analog of source werewolfLevel >= 2) only gets half the per-level blood-power scaling.
    public int getMaxBloodPower() {
        int scaling = wws.level >= 2 ? (int) Math.floor(vs.level * 0.5) : vs.level;
        return 500 + scaling * 250;
    }
    public int getVampireLevelCap() { return vs.levelCap; }
    public int getVampireQuestCounter() { return vs.questCounter; }
    public boolean canIncreaseVampireLevel() { return vs.level < vs.levelCap && vs.level < 10; }
    public void setBloodPower(int p) { vs.bloodPower = Math.clamp(p, 0, getMaxBloodPower()); }
    public void addBloodPower(int amount) { setBloodPower(vs.bloodPower + amount); }
    public void setHumanBlood(int amount) { vs.humanBlood = Math.clamp(amount, 0, 500); }
    public void giveHumanBlood(int amount) {
        if (!isVampire() && amount > 0) setHumanBlood(vs.humanBlood + amount);
    }
    public int takeHumanBlood(int amount, boolean sleeping) {
        int requested = sleeping ? Math.max(0, amount) : (int) Math.ceil(Math.max(0, amount) * 0.66F);
        int taken = Math.min(vs.humanBlood, requested);
        vs.humanBlood -= taken;
        return taken;
    }
    public int getBloodReserve() { return isVampire() ? vs.bloodReserve : 0; }
    public void setBloodReserve(int blood) { vs.bloodReserve = Math.clamp(blood, 0, 250); }
    public void addBloodReserve(int amount) {
        if (amount > 0) {
            setBloodReserve(vs.bloodReserve + amount);
        }
    }
    public boolean isBloodReserveReady() { return getBloodReserve() > 0; }
    public int useBloodReserve() {
        int reserve = getBloodReserve();
        if (reserve <= 0 || getBloodPower() >= getMaxBloodPower()) {
            return 0;
        }
        int before = getBloodPower();
        vs.bloodReserve = 0;
        addBloodPower(reserve);
        return getBloodPower() - before;
    }
    public boolean spendBloodPower(int amount) {
        return spendBloodPower(amount, true);
    }
    public boolean spendBloodPower(int amount, boolean exact) {
        int cost = Math.max(0, amount);
        if (vs.bloodPower < (exact ? cost : 1)) return false;
        setBloodPower(vs.bloodPower - cost);
        return true;
    }
    public void increaseVampireLevelCap(int cap) {
        if (cap > vs.levelCap) vs.levelCap = Math.clamp(cap, 1, 10);
    }
    public void incrementQuestCounter() { vs.questCounter++; }
    public void resetQuestCounter() { vs.questCounter = 0; }
    /** Record a distinct village cell for the level 7→8 quest; true if it had not been visited before. */
    public boolean addVisitedVillage(long cell) {
        if (vs.visitedVillages.contains(cell)) return false;
        vs.visitedVillages.add(cell);
        return true;
    }
    public int getVisitedVillageCount() { return vs.visitedVillages.size(); }
    public void clearVisitedVillages() { vs.visitedVillages.clear(); }
    public void setWerewolf(boolean w) {
        this.werewolf = w;
        if (w && wws.level == 0) wws.level = 1;       // source: infection sets werewolfLevel = 1
        if (!w) {
            wws.level = 0;
            wws.form = 0;
            transformationLevel = 0.0F;
            resetWolfQuest();
            wws.howledChunks.clear();
        }
    }
    public void setBloodLevel(int b) { this.bloodLevel = Math.clamp(b, 0, 500); }
    public void addBlood(int amount) { setBloodLevel(bloodLevel + amount); }
    public void setTransformationLevel(float t) { this.transformationLevel = Math.clamp(t, 0f, 1f); }
    public void setFamiliarUUID(UUID uuid) { this.familiar.uuid = uuid == null ? "" : uuid.toString(); }
    public void setFamiliarType(String type) { this.familiar.type = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT); }
    public void setFamiliarName(String name) { this.familiar.name = name == null ? "" : name; }
    public void setFamiliarSitting(boolean sitting) { this.familiar.sitting = sitting; }
    public void setFamiliarNameVisible(boolean visible) { this.familiar.nameVisible = visible; }
    public void clearFamiliar() { this.familiar = new FamiliarState(); }
    public void setInfusionType(InfusionType type) {
        if (type == null || type == InfusionType.NONE) {
            clearInfusion();
            return;
        }
        this.infusionType = type.name();
    }
    public void setInfusionEnergy(int currentEnergy, int maxEnergy) {
        this.infusionChargeState.maxEnergy = Math.max(0, maxEnergy);
        this.infusionChargeState.currentEnergy = Math.clamp(currentEnergy, 0, this.infusionChargeState.maxEnergy);
    }
    public void clearInfusion() {
        this.infusionType = "";
        this.infusionChargeState = new InfusionChargeState();
    }

    public Optional<BlockPos> getLastDeathPos() {
        if (lastDeathPos == Long.MIN_VALUE) return Optional.empty();
        return Optional.of(BlockPos.of(lastDeathPos));
    }

    public String getLastDeathDimension() { return lastDeathDimension; }

    public void setLastDeathPos(BlockPos pos, ResourceKey<Level> dimension) {
        this.lastDeathPos = pos.asLong();
        this.lastDeathDimension = dimension.identifier().toString();
    }

    public Optional<BlockPos> getReturnPos() {
        if (returnPos == Long.MIN_VALUE) return Optional.empty();
        return Optional.of(BlockPos.of(returnPos));
    }

    public String getReturnDimension() { return returnDimension; }
    public boolean isInSpiritWorld() { return inSpiritWorld; }
    public boolean isSpiritWorldDream() { return sw.isDream; }
    public boolean isOtherwhereHearthTraveler() { return sw.otherwhereHearthTraveler; }
    public String getSavedInventorySnbt() { return sw.dreamStorage.overworldInventorySnbt; }
    public String getOverworldInventorySnbt() { return sw.dreamStorage.overworldInventorySnbt; }
    public String getSpiritInventorySnbt() { return sw.dreamStorage.spiritInventorySnbt; }

    public void setReturnPos(BlockPos pos, ResourceKey<Level> dimension) {
        this.returnPos = pos.asLong();
        this.returnDimension = dimension.identifier().toString();
    }

    public void clearReturnPos() {
        this.returnPos = Long.MIN_VALUE;
        this.returnDimension = "";
    }

    public Optional<BlockPos> getTormentReturnPos() {
        return sw.tormentReturnPos == Long.MIN_VALUE
                ? Optional.empty() : Optional.of(BlockPos.of(sw.tormentReturnPos));
    }

    public String getTormentReturnDimension() {
        return sw.tormentReturnDimension;
    }

    public Optional<UUID> getTormentSession() {
        if (sw.tormentSession.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(sw.tormentSession));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void setTormentReturn(BlockPos pos, ResourceKey<Level> dimension, UUID session) {
        sw.tormentReturnPos = pos.asLong();
        sw.tormentReturnDimension = dimension.identifier().toString();
        sw.tormentSession = session == null ? "" : session.toString();
    }

    public void setTormentSession(UUID session) {
        sw.tormentSession = session == null ? "" : session.toString();
    }

    public void clearTormentReturn() {
        sw.tormentReturnPos = Long.MIN_VALUE;
        sw.tormentReturnDimension = "";
        sw.tormentSession = "";
    }

    public int getMirrorRouteDepth() {
        return sw.mirrorWorld.routes.size();
    }

    public Optional<MirrorRoute> getCurrentMirrorRoute() {
        List<MirrorRoute> routes = sw.mirrorWorld.routes;
        return routes.isEmpty() ? Optional.empty() : Optional.of(routes.getLast());
    }

    public List<MirrorRoute> getMirrorRoutes() {
        return List.copyOf(sw.mirrorWorld.routes);
    }

    public void pushMirrorRoute(BlockPos sourceMirror, ResourceKey<Level> sourceDimension,
                                int destinationRoomIndex) {
        MirrorRoute route = new MirrorRoute(
                sourceDimension.identifier().toString(), sourceMirror.asLong(), destinationRoomIndex);
        if (getCurrentMirrorRoute().filter(route::equals).isEmpty()) {
            sw.mirrorWorld.routes.add(route);
        }
    }

    public Optional<MirrorRoute> popMirrorRoute() {
        return sw.mirrorWorld.routes.isEmpty()
                ? Optional.empty() : Optional.of(sw.mirrorWorld.routes.removeLast());
    }

    public void clearMirrorRoutes() {
        sw.mirrorWorld.routes.clear();
    }

    public void setInSpiritWorld(boolean v) { this.inSpiritWorld = v; }
    public void setSpiritWorldIsDream(boolean v) { sw.isDream = v; }
    public void setOtherwhereHearthTraveler(boolean value) { sw.otherwhereHearthTraveler = value; }
    public void setSavedInventorySnbt(String snbt) { setOverworldInventorySnbt(snbt); }
    public void setOverworldInventorySnbt(String snbt) {
        sw.dreamStorage.overworldInventorySnbt = snbt == null ? "" : snbt;
    }
    public void setSpiritInventorySnbt(String snbt) {
        sw.dreamStorage.spiritInventorySnbt = snbt == null ? "" : snbt;
    }
    public float getSavedHealth() { return sw.dreamStorage.overworldHealth; }
    public int getSavedFoodLevel() { return sw.dreamStorage.overworldFood; }
    public float getSavedSaturation() { return sw.dreamStorage.overworldSat; }
    public float getSpiritHealth() { return sw.dreamStorage.spiritHealth; }
    public int getSpiritFoodLevel() { return sw.dreamStorage.spiritFood; }
    public float getSpiritSaturation() { return sw.dreamStorage.spiritSat; }
    public boolean hasSpiritVitals() { return sw.dreamStorage.spiritVitalsInitialized; }

    public Optional<UUID> getCorpseUUID() {
        if (sw.corpseUUID.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(sw.corpseUUID)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public void setCorpseUUID(UUID uuid) { sw.corpseUUID = uuid == null ? "" : uuid.toString(); }
    public void clearCorpseUUID() { sw.corpseUUID = ""; }
    public boolean isManifestedGhost() { return sw.manifestedGhost; }
    public void setManifestedGhost(boolean v) { sw.manifestedGhost = v; }
    public int getManifestationTime() { return sw.manifestationTime; }
    public void setManifestationTime(int time) { sw.manifestationTime = Math.max(0, time); }
    public boolean shouldSkipManifestationTick() { return sw.manifestationSkipTick; }
    public void setSkipManifestationTick(boolean v) { sw.manifestationSkipTick = v; }

    public void clearManifestationState() {
        clearManifestedGhostState();
        sw.manifestationTime = 0;
    }

    public void clearManifestedGhostState() {
        sw.manifestedGhost = false;
        sw.manifestationSkipTick = false;
    }

    public void saveSpiritWorldVitals(float health, int food, float saturation) {
        sw.dreamStorage.overworldHealth = Math.max(1f, health);
        sw.dreamStorage.overworldFood = Math.clamp(food, 0, 20);
        sw.dreamStorage.overworldSat = Math.max(0f, saturation);
    }

    public void saveSpiritVitals(float health, int food, float saturation) {
        sw.dreamStorage.spiritHealth = Math.max(1f, health);
        sw.dreamStorage.spiritFood = Math.clamp(food, 0, 20);
        sw.dreamStorage.spiritSat = Math.max(0f, saturation);
        sw.dreamStorage.spiritVitalsInitialized = true;
    }

    public String getCreaturePowerType() { return creaturePowerType; }
    public int getCreaturePowerCharges() { return creaturePowerCharges; }
    public void setCreaturePower(String type, int charges) { this.creaturePowerType = type; this.creaturePowerCharges = Math.max(0, charges); }
    public void addCreaturePowerCharges(int n) { this.creaturePowerCharges = Math.min(100, creaturePowerCharges + n); }
    public boolean spendCreaturePowerCharge() { if (creaturePowerCharges <= 0) return false; creaturePowerCharges--; return true; }
    public void clearCreaturePower() { creaturePowerType = ""; creaturePowerCharges = 0; }
    public int getPreparedSpellId() { return preparedSpellId; }
    public int getPreparedSpellLevel() { return preparedSpellLevel; }
    public void setPreparedSpell(int id, int level) { this.preparedSpellId = id; this.preparedSpellLevel = level; }
    public void clearPreparedSpell() { this.preparedSpellId = -1; this.preparedSpellLevel = 1; }
    // ── Werewolf progression (source werewolfLevel / form / Wolf-Altar quest) ───
    public int getWerewolfLevel() { return wws.level; }
    public void setWerewolfLevel(int level) {
        int clamped = Math.clamp(level, 0, 10);
        if (clamped != wws.level) {
            wws.level = clamped;
            wws.questState = 0;
            wws.questCounter = 0;
            wws.howledChunks.clear();
        }
        this.werewolf = wws.level > 0;
        if (wws.level == 0) {
            wws.form = WolfForm.NONE.ordinal();
            transformationLevel = 0.0F;
        } else if (wws.level < 5 && getWolfForm() == WolfForm.WOLFMAN) {
            wws.form = transformationLevel > 0.01F
                    ? WolfForm.WOLF.ordinal() : WolfForm.NONE.ordinal();
        }
    }
    public void increaseWerewolfLevel() { if (wws.level < 10) setWerewolfLevel(wws.level + 1); }
    public WolfForm getWolfForm() { return WolfForm.fromOrdinal(wws.form); }
    public void setWolfForm(WolfForm form) { wws.form = form == null ? 0 : form.ordinal(); }
    public WolfQuestState getWolfQuestState() { return WolfQuestState.fromOrdinal(wws.questState); }
    public void setWolfQuestState(WolfQuestState s) { wws.questState = s == null ? 0 : s.ordinal(); }
    public int getWolfQuestCounter() { return wws.questCounter; }
    public void incrementWolfQuestCounter() { wws.questCounter++; }
    public void resetWolfQuest() { wws.questState = 0; wws.questCounter = 0; }
    /** Record a distinct howl chunk (level 6→7 quest); true if newly added. */
    public boolean addHowledChunk(long chunk) {
        if (wws.howledChunks.contains(chunk)) return false;
        wws.howledChunks.add(chunk);
        return true;
    }
    public int getHowledChunkCount() { return wws.howledChunks.size(); }
    public long getLastHowl() { return wws.lastHowl; }
    public void setLastHowl(long t) { wws.lastHowl = t; }
    public long getLastBoneFind() { return wws.lastBoneFind; }
    public void setLastBoneFind(long t) { wws.lastBoneFind = t; }
    public int getSoaringTicks() { return soaringTicks; }
    public boolean hasSoaringBuff() { return soaringTicks > 0; }
    public void setSoaringTicks(int v) { soaringTicks = Math.max(0, v); }
    public void tickSoaring() { if (soaringTicks > 0) soaringTicks--; }
    public int getGraveTicks() { return graveTicks; }
    public boolean hasGraveBrew() { return graveTicks > 0; }
    public void setGraveTicks(int v) { graveTicks = Math.max(0, v); }
    public void tickGrave() { if (graveTicks > 0) graveTicks--; }
    public long getLastGraveUseGameTime() { return lastGraveUseGameTime; }
    public void setLastGraveUseGameTime(long v) { lastGraveUseGameTime = v; }
    public int getDepthsTicks() { return depthsTicks; }
    public boolean hasDepthsBuff() { return depthsTicks > 0; }
    public void setDepthsTicks(int v) { depthsTicks = Math.max(0, v); }
    public int getGrotesqueTicks() { return grotesqueTicks; }
    public boolean hasGrotesqueAura() { return grotesqueTicks > 0; }
    public void setGrotesqueTicks(int v) { grotesqueTicks = Math.max(0, v); }
    public void tickGrotesque() { if (grotesqueTicks > 0) grotesqueTicks--; }
    public boolean isGhostWalkerActive() { return ghostWalkerActive; }
    public void setGhostWalkerActive(boolean v) { ghostWalkerActive = v; }
    public int getTormentPortalCooldown() { return tormentPortalCooldown; }
    public void setTormentPortalCooldown(int v) { tormentPortalCooldown = Math.max(0, v); }
    public void tickTormentPortal() { if (tormentPortalCooldown > 0) tormentPortalCooldown--; }
    public boolean isDemonicNightmare() { return demonicNightmare; }
    public void setDemonicNightmare(boolean v) { this.demonicNightmare = v; }
    public int getWakingNightmareLevel() { return sw.wakingNightmareLevel; }
    public boolean hasWakingNightmare() { return sw.wakingNightmareLevel > 0; }
    public void setWakingNightmareLevel(int level) { sw.wakingNightmareLevel = Math.max(0, level); }
    public boolean isNightmare() { return inSpiritWorld && !sw.isDream; }
    public long getLastNightmareKillGameTime() { return lastNightmareKillGameTime; }
    public void setLastNightmareKillGameTime(long t) { this.lastNightmareKillGameTime = t; }
    public long getWitchHunterTriggerGameTime() { return witchHunterTriggerGameTime; }
    public void setWitchHunterTriggerGameTime(long t) { this.witchHunterTriggerGameTime = t; }
    public void clearWitchHunterTrigger() { this.witchHunterTriggerGameTime = Long.MIN_VALUE; }

    public void copyFrom(WitcheryPlayerData other, boolean keepOnDeath) {
        this.vs = new VampireState(other.vs.vampire, other.vs.level, other.vs.ultimate, other.vs.ultimateCharges,
                other.vs.bloodPower, other.vs.bloodReserve, other.vs.humanBlood, other.vs.batForm,
                other.vs.levelCap, other.vs.questCounter,
                other.vs.visitedVillages);
        this.werewolf = other.werewolf;
        this.transformationLevel = other.transformationLevel;
        this.familiar = new FamiliarState(other.familiar.uuid, other.familiar.type, other.familiar.name, other.familiar.sitting, other.familiar.nameVisible);
        this.infusionType = other.infusionType;
        this.infusionChargeState = new InfusionChargeState(other.infusionChargeState.currentEnergy, other.infusionChargeState.maxEnergy);
        this.lastDeathPos = other.lastDeathPos;
        this.lastDeathDimension = other.lastDeathDimension;
        this.returnPos = other.returnPos;
        this.returnDimension = other.returnDimension;
        this.inSpiritWorld = other.inSpiritWorld;
        this.sw = other.sw.copy();
        this.creaturePowerType = other.creaturePowerType;
        this.creaturePowerCharges = other.creaturePowerCharges;
        this.wws = new WerewolfState(other.wws.level, other.wws.form, other.wws.questState,
                other.wws.questCounter, other.wws.howledChunks, other.wws.lastHowl, other.wws.lastBoneFind);
        this.soaringTicks = other.soaringTicks;
        this.graveTicks = other.graveTicks;
        this.lastGraveUseGameTime = other.lastGraveUseGameTime;
        this.depthsTicks = other.depthsTicks;
        this.grotesqueTicks = other.grotesqueTicks;
        this.witchHunterTriggerGameTime = other.witchHunterTriggerGameTime;
        if (keepOnDeath) {
            this.bloodLevel = other.bloodLevel;
        }
    }

    @Override
    public String toString() {
        return "WitcheryPlayerData{vampire=" + vs.vampire + ", vampireLevel=" + vs.level + ", werewolf=" + werewolf
                + ", blood=" + bloodLevel + ", transform=" + transformationLevel
                + ", familiarType=" + familiar.type + ", infusion=" + infusionType
                + ", infusionEnergy=" + infusionChargeState.currentEnergy + "/" + infusionChargeState.maxEnergy + "}";
    }
}
