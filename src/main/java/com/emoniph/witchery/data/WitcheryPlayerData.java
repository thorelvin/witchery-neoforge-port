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

        public FamiliarState() {}

        private FamiliarState(String uuid, String type, String name) {
            this.uuid = uuid == null ? "" : uuid;
            this.type = type == null ? "" : type;
            this.name = name == null ? "" : name;
        }

        public static final Codec<FamiliarState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("uuid", "").forGetter(s -> s.uuid),
            Codec.STRING.optionalFieldOf("type", "").forGetter(s -> s.type),
            Codec.STRING.optionalFieldOf("name", "").forGetter(s -> s.name)
        ).apply(i, FamiliarState::new));
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

        public SpiritWorldState() {}

        private SpiritWorldState(boolean isDream, String inventorySnbt,
                                 float savedHealth, int savedFood, float savedSat,
                                 String corpseUUID, boolean manifestedGhost,
                                 int manifestationTime, boolean manifestationSkipTick,
                                 long manifestationReturnPos) {
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
            Codec.LONG.optionalFieldOf("manifest_return_pos", Long.MIN_VALUE).forGetter(s -> s.manifestationReturnPos)
        ).apply(i, SpiritWorldState::new));
    }

    private boolean vampire = false;
    private boolean werewolf = false;
    private int bloodLevel = 0;
    private float transformationLevel = 0f;
    private FamiliarState familiar = new FamiliarState();
    private String infusionType = "";
    private long lastDeathPos = Long.MIN_VALUE;
    private String lastDeathDimension = "";
    private long returnPos = Long.MIN_VALUE;
    private String returnDimension = "";
    private boolean inSpiritWorld = false;
    private SpiritWorldState sw = new SpiritWorldState();
    private String creaturePowerType = "";
    private int creaturePowerCharges = 0;
    private int wolfMastery = 0;
    private int howlCooldown = 0;
    private int soaringTicks = 0;
    private boolean ghostWalkerActive = false;
    private int tormentPortalCooldown = 0;
    private boolean demonicNightmare = false;
    private long lastNightmareKillGameTime = Long.MIN_VALUE;

    public WitcheryPlayerData() {}

    private WitcheryPlayerData(boolean vampire, boolean werewolf, int bloodLevel,
                               float transformationLevel, FamiliarState familiar,
                               String infusionType, long lastDeathPos, String lastDeathDimension,
                               long returnPos, String returnDimension,
                               boolean inSpiritWorld, SpiritWorldState sw,
                               String creaturePowerType, int creaturePowerCharges,
                               int wolfMastery) {
        this.vampire = vampire;
        this.werewolf = werewolf;
        this.bloodLevel = bloodLevel;
        this.transformationLevel = transformationLevel;
        this.familiar = familiar != null ? familiar : new FamiliarState();
        this.infusionType = infusionType;
        this.lastDeathPos = lastDeathPos;
        this.lastDeathDimension = lastDeathDimension;
        this.returnPos = returnPos;
        this.returnDimension = returnDimension;
        this.inSpiritWorld = inSpiritWorld;
        this.sw = sw != null ? sw : new SpiritWorldState();
        this.creaturePowerType = creaturePowerType;
        this.creaturePowerCharges = creaturePowerCharges;
        this.wolfMastery = wolfMastery;
    }

    private static WitcheryPlayerData fromCodec(boolean vampire, boolean werewolf, int bloodLevel,
                                                float transformationLevel, FamiliarState familiar,
                                                String infusionType, long lastDeathPos, String lastDeathDimension,
                                                long returnPos, String returnDimension,
                                                boolean inSpiritWorld, SpiritWorldState sw,
                                                String creaturePowerType, int creaturePowerCharges,
                                                int wolfMastery) {
        return new WitcheryPlayerData(vampire, werewolf, bloodLevel, transformationLevel,
                familiar, infusionType, lastDeathPos, lastDeathDimension, returnPos, returnDimension,
                inSpiritWorld, sw, creaturePowerType, creaturePowerCharges, wolfMastery);
    }

    public static final com.mojang.serialization.MapCodec<WitcheryPlayerData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.BOOL.optionalFieldOf("vampire", false).forGetter(d -> d.vampire),
        Codec.BOOL.optionalFieldOf("werewolf", false).forGetter(d -> d.werewolf),
        Codec.INT.optionalFieldOf("blood_level", 0).forGetter(d -> d.bloodLevel),
        Codec.FLOAT.optionalFieldOf("transformation_level", 0f).forGetter(d -> d.transformationLevel),
        FamiliarState.CODEC.optionalFieldOf("familiar", new FamiliarState()).forGetter(d -> d.familiar),
        Codec.STRING.optionalFieldOf("infusion_type", "").forGetter(d -> d.infusionType),
        Codec.LONG.optionalFieldOf("last_death_pos", Long.MIN_VALUE).forGetter(d -> d.lastDeathPos),
        Codec.STRING.optionalFieldOf("last_death_dim", "").forGetter(d -> d.lastDeathDimension),
        Codec.LONG.optionalFieldOf("return_pos", Long.MIN_VALUE).forGetter(d -> d.returnPos),
        Codec.STRING.optionalFieldOf("return_dim", "").forGetter(d -> d.returnDimension),
        Codec.BOOL.optionalFieldOf("in_spirit_world", false).forGetter(d -> d.inSpiritWorld),
        SpiritWorldState.CODEC.optionalFieldOf("spirit_world", new SpiritWorldState()).forGetter(d -> d.sw),
        Codec.STRING.optionalFieldOf("creature_power_type", "").forGetter(d -> d.creaturePowerType),
        Codec.INT.optionalFieldOf("creature_power_charges", 0).forGetter(d -> d.creaturePowerCharges),
        Codec.INT.optionalFieldOf("wolf_mastery", 0).forGetter(d -> d.wolfMastery)
    ).apply(i, WitcheryPlayerData::fromCodec));

    public boolean isVampire() { return vampire; }
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

    public InfusionType getInfusionType() { return InfusionType.fromString(infusionType); }

    public void setVampire(boolean v) { this.vampire = v; }
    public void setWerewolf(boolean w) { this.werewolf = w; }
    public void setBloodLevel(int b) { this.bloodLevel = Math.clamp(b, 0, 500); }
    public void addBlood(int amount) { setBloodLevel(bloodLevel + amount); }
    public void setTransformationLevel(float t) { this.transformationLevel = Math.clamp(t, 0f, 1f); }
    public void setFamiliarUUID(UUID uuid) { this.familiar.uuid = uuid == null ? "" : uuid.toString(); }
    public void setFamiliarType(String type) { this.familiar.type = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT); }
    public void setFamiliarName(String name) { this.familiar.name = name == null ? "" : name; }
    public void clearFamiliar() { this.familiar = new FamiliarState(); }
    public void setInfusionType(InfusionType type) { this.infusionType = type == null ? "" : type.name(); }
    public void clearInfusion() { this.infusionType = ""; }

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
    public int getWolfMastery() { return wolfMastery; }
    public void setWolfMastery(int v) { wolfMastery = Math.clamp(v, 0, 3); }
    public int getSoaringTicks() { return soaringTicks; }
    public boolean hasSoaringBuff() { return soaringTicks > 0; }
    public void setSoaringTicks(int v) { soaringTicks = Math.max(0, v); }
    public void tickSoaring() { if (soaringTicks > 0) soaringTicks--; }
    public boolean isGhostWalkerActive() { return ghostWalkerActive; }
    public void setGhostWalkerActive(boolean v) { ghostWalkerActive = v; }
    public int getTormentPortalCooldown() { return tormentPortalCooldown; }
    public void setTormentPortalCooldown(int v) { tormentPortalCooldown = Math.max(0, v); }
    public void tickTormentPortal() { if (tormentPortalCooldown > 0) tormentPortalCooldown--; }
    public boolean isDemonicNightmare() { return demonicNightmare; }
    public void setDemonicNightmare(boolean v) { this.demonicNightmare = v; }
    public boolean isNightmare() { return !sw.isDream; }
    public long getLastNightmareKillGameTime() { return lastNightmareKillGameTime; }
    public void setLastNightmareKillGameTime(long t) { this.lastNightmareKillGameTime = t; }

    public void copyFrom(WitcheryPlayerData other, boolean keepOnDeath) {
        this.vampire = other.vampire;
        this.werewolf = other.werewolf;
        this.transformationLevel = other.transformationLevel;
        this.familiar = new FamiliarState(other.familiar.uuid, other.familiar.type, other.familiar.name);
        this.infusionType = other.infusionType;
        this.lastDeathPos = other.lastDeathPos;
        this.lastDeathDimension = other.lastDeathDimension;
        this.returnPos = other.returnPos;
        this.returnDimension = other.returnDimension;
        this.inSpiritWorld = other.inSpiritWorld;
        this.sw = other.sw;
        this.creaturePowerType = other.creaturePowerType;
        this.creaturePowerCharges = other.creaturePowerCharges;
        this.wolfMastery = other.wolfMastery;
        this.soaringTicks = other.soaringTicks;
        if (keepOnDeath) {
            this.bloodLevel = other.bloodLevel;
        }
    }

    @Override
    public String toString() {
        return "WitcheryPlayerData{vampire=" + vampire + ", werewolf=" + werewolf
                + ", blood=" + bloodLevel + ", transform=" + transformationLevel
                + ", familiarType=" + familiar.type + ", infusion=" + infusionType + "}";
    }
}
