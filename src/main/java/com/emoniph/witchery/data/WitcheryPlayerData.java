package com.emoniph.witchery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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
        // Max level the player can reach without quest-gating items; 10 = uncapped during dev.
        public int levelCap = 10;
        public int questCounter = 0;
        // Distinct village cells already visited for the level 7→8 quest (source visitedVampireChunks).
        public final java.util.List<Long> visitedVillages = new java.util.ArrayList<>();

        public VampireState() {}

        private VampireState(boolean vampire, int level, int ultimate, int ultimateCharges,
                             int bloodPower, int bloodReserve, int levelCap, int questCounter,
                             java.util.List<Long> visitedVillages) {
            this.vampire = vampire;
            this.level = Math.clamp(level, 0, 10);
            this.ultimate = Math.clamp(ultimate, 0, 3);
            this.ultimateCharges = Math.max(0, ultimateCharges);
            this.bloodPower = Math.max(0, bloodPower);
            this.bloodReserve = Math.clamp(bloodReserve, 0, 250);
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

    /** Nested record for Spirit World state — keeps the main CODEC under the 16-field DFU limit. */
    public static class SpiritWorldState {
        public boolean isDream = false;
        public String inventorySnbt = "";
        public float savedHealth = 20f;
        public int savedFood = 20;
        public float savedSat = 5f;
        public String corpseUUID = "";
        public boolean manifestedGhost = false;
        public int manifestationTime = 0;
        public boolean manifestationSkipTick = false;
        public long manifestationReturnPos = Long.MIN_VALUE;
        public int wakingNightmareLevel = 0;
        public boolean otherwhereHearthTraveler = false;

        public SpiritWorldState() {}

        private SpiritWorldState(boolean isDream, String inventorySnbt,
                                 float savedHealth, int savedFood, float savedSat,
                                 String corpseUUID, boolean manifestedGhost,
                                 int manifestationTime, boolean manifestationSkipTick,
                                 long manifestationReturnPos, int wakingNightmareLevel,
                                 boolean otherwhereHearthTraveler) {
            this.isDream = isDream;
            this.inventorySnbt = inventorySnbt;
            this.savedHealth = savedHealth;
            this.savedFood = savedFood;
            this.savedSat = savedSat;
            this.corpseUUID = corpseUUID == null ? "" : corpseUUID;
            this.manifestedGhost = manifestedGhost;
            this.manifestationTime = Math.max(0, manifestationTime);
            this.manifestationSkipTick = manifestationSkipTick;
            this.manifestationReturnPos = manifestationReturnPos;
            this.wakingNightmareLevel = Math.max(0, wakingNightmareLevel);
            this.otherwhereHearthTraveler = otherwhereHearthTraveler;
        }

        public SpiritWorldState copy() {
            return new SpiritWorldState(
                    isDream,
                    inventorySnbt,
                    savedHealth,
                    savedFood,
                    savedSat,
                    corpseUUID,
                    manifestedGhost,
                    manifestationTime,
                    manifestationSkipTick,
                    manifestationReturnPos,
                    wakingNightmareLevel,
                    otherwhereHearthTraveler);
        }

        public static final Codec<SpiritWorldState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("is_dream", false).forGetter(s -> s.isDream),
            Codec.STRING.optionalFieldOf("inventory_snbt", "").forGetter(s -> s.inventorySnbt),
            Codec.FLOAT.optionalFieldOf("saved_health", 20f).forGetter(s -> s.savedHealth),
            Codec.INT.optionalFieldOf("saved_food", 20).forGetter(s -> s.savedFood),
            Codec.FLOAT.optionalFieldOf("saved_sat", 5f).forGetter(s -> s.savedSat),
            Codec.STRING.optionalFieldOf("corpse_uuid", "").forGetter(s -> s.corpseUUID),
            Codec.BOOL.optionalFieldOf("manifested_ghost", false).forGetter(s -> s.manifestedGhost),
            Codec.INT.optionalFieldOf("manifest_time", 0).forGetter(s -> s.manifestationTime),
            Codec.BOOL.optionalFieldOf("manifest_skip_tick", false).forGetter(s -> s.manifestationSkipTick),
            Codec.LONG.optionalFieldOf("manifest_return_pos", Long.MIN_VALUE).forGetter(s -> s.manifestationReturnPos),
            Codec.INT.optionalFieldOf("waking_nightmare_level", 0).forGetter(s -> s.wakingNightmareLevel),
            Codec.BOOL.optionalFieldOf("otherwhere_hearth_traveler", false)
                    .forGetter(s -> s.otherwhereHearthTraveler)
        ).apply(i, SpiritWorldState::new));
    }

    private VampireState vs = new VampireState();
    private boolean werewolf = false;
    private int bloodLevel = 0;
    private float transformationLevel = 0f;
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
    private int howlCooldown = 0;
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
    public boolean isWerewolf() { return werewolf; }
    public int getBloodLevel() { return bloodLevel; }
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
        if (v && vs.level == 0) vs.level = 1;
        if (!v) { vs.level = 0; vs.bloodPower = 0; vs.bloodReserve = 0; vs.questCounter = 0; }
    }
    public void setVampireLevel(int level) {
        int clamped = Math.clamp(level, 0, 10);
        if (clamped != vs.level) {
            vs.level = clamped;
            vs.questCounter = 0;
            vs.bloodPower = Math.min(vs.bloodPower, getMaxBloodPower());
        }
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
        if (vs.bloodPower < amount) return false;
        setBloodPower(vs.bloodPower - amount);
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
        if (!w) { wws.level = 0; wws.form = 0; resetWolfQuest(); wws.howledChunks.clear(); }
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
    public String getSavedInventorySnbt() { return sw.inventorySnbt; }

    public void setReturnPos(BlockPos pos, ResourceKey<Level> dimension) {
        this.returnPos = pos.asLong();
        this.returnDimension = dimension.identifier().toString();
    }

    public void clearReturnPos() {
        this.returnPos = Long.MIN_VALUE;
        this.returnDimension = "";
    }

    public void setInSpiritWorld(boolean v) { this.inSpiritWorld = v; }
    public void setSpiritWorldIsDream(boolean v) { sw.isDream = v; }
    public void setOtherwhereHearthTraveler(boolean value) { sw.otherwhereHearthTraveler = value; }
    public void setSavedInventorySnbt(String snbt) { sw.inventorySnbt = snbt == null ? "" : snbt; }
    public float getSavedHealth() { return sw.savedHealth; }
    public int getSavedFoodLevel() { return sw.savedFood; }
    public float getSavedSaturation() { return sw.savedSat; }

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

    public Optional<BlockPos> getManifestationReturnPos() {
        if (sw.manifestationReturnPos == Long.MIN_VALUE) return Optional.empty();
        return Optional.of(BlockPos.of(sw.manifestationReturnPos));
    }

    public void setManifestationReturnPos(BlockPos pos) {
        sw.manifestationReturnPos = pos == null ? Long.MIN_VALUE : pos.asLong();
    }

    public void clearManifestationState() {
        sw.manifestedGhost = false;
        sw.manifestationTime = 0;
        sw.manifestationSkipTick = false;
        sw.manifestationReturnPos = Long.MIN_VALUE;
    }

    public void saveSpiritWorldVitals(float health, int food, float saturation) {
        sw.savedHealth = Math.max(1f, health);
        sw.savedFood = Math.clamp(food, 0, 20);
        sw.savedSat = Math.max(0f, saturation);
    }

    public String getCreaturePowerType() { return creaturePowerType; }
    public int getCreaturePowerCharges() { return creaturePowerCharges; }
    public void setCreaturePower(String type, int charges) { this.creaturePowerType = type; this.creaturePowerCharges = Math.max(0, charges); }
    public void addCreaturePowerCharges(int n) { this.creaturePowerCharges = Math.min(100, creaturePowerCharges + n); }
    public boolean spendCreaturePowerCharge() { if (creaturePowerCharges <= 0) return false; creaturePowerCharges--; return true; }
    public void clearCreaturePower() { creaturePowerType = ""; creaturePowerCharges = 0; }
    public int getHowlCooldown() { return howlCooldown; }
    public void setHowlCooldown(int v) { howlCooldown = Math.max(0, v); }
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
        if (wws.level == 0) wws.form = 0;
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
    public boolean isNightmare() { return !sw.isDream; }
    public long getLastNightmareKillGameTime() { return lastNightmareKillGameTime; }
    public void setLastNightmareKillGameTime(long t) { this.lastNightmareKillGameTime = t; }
    public long getWitchHunterTriggerGameTime() { return witchHunterTriggerGameTime; }
    public void setWitchHunterTriggerGameTime(long t) { this.witchHunterTriggerGameTime = t; }
    public void clearWitchHunterTrigger() { this.witchHunterTriggerGameTime = Long.MIN_VALUE; }

    public void copyFrom(WitcheryPlayerData other, boolean keepOnDeath) {
        this.vs = new VampireState(other.vs.vampire, other.vs.level, other.vs.ultimate, other.vs.ultimateCharges,
                other.vs.bloodPower, other.vs.bloodReserve, other.vs.levelCap, other.vs.questCounter,
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
