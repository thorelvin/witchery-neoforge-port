package com.emoniph.witchery.data;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.RedThreadBinding;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistent owner for the deliberately narrow Red Thread anchored-curse path. */
public final class AnchoredCurseSavedData extends SavedData {
    public static final int CURRENT_VERSION = 1;
    public static final long WARD_TICKS = 1_200L;
    public static final int CLUE_ONE_TICKS = 600;
    public static final int CLUE_TWO_TICKS = 2_400;
    public static final int CLUE_THREE_TICKS = 6_000;

    static final int MAX_RECORDS = 4_096;
    public static final int TICK_INTERVAL = 20;
    static final int EFFECT_REFRESH_THRESHOLD = 100;
    static final int EFFECT_REFRESH_DURATION = 160;

    public static final Codec<AnchoredCurseSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CurseRecord.CODEC.listOf().optionalFieldOf("curses", List.of())
                    .forGetter(AnchoredCurseSavedData::recordsForCodec),
            WardRecord.CODEC.listOf().optionalFieldOf("wards", List.of())
                    .forGetter(AnchoredCurseSavedData::wardsForCodec)
    ).apply(instance, AnchoredCurseSavedData::fromCodec));

    public static final SavedDataType<AnchoredCurseSavedData> TYPE = new SavedDataType<>(
            "witchery_anchored_curses",
            AnchoredCurseSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<PairKey, CurseRecord> cursesByPair = new LinkedHashMap<>();
    private final Map<UUID, PairKey> pairsByBinding = new HashMap<>();
    private final Map<PairKey, WardRecord> wardsByPair = new HashMap<>();

    public AnchoredCurseSavedData() {
    }

    private static AnchoredCurseSavedData fromCodec(List<CurseRecord> records, List<WardRecord> wards) {
        AnchoredCurseSavedData data = new AnchoredCurseSavedData();
        for (CurseRecord record : records) {
            if (data.cursesByPair.size() >= MAX_RECORDS || record.version() > CURRENT_VERSION
                    || record.state() != CurseState.ACTIVE) {
                continue;
            }
            PairKey pair = new PairKey(record.casterId(), record.targetId());
            if (data.cursesByPair.putIfAbsent(pair, record) == null) {
                data.pairsByBinding.putIfAbsent(record.bindingId(), pair);
            }
        }
        for (WardRecord ward : wards) {
            if (data.wardsByPair.size() >= MAX_RECORDS || ward.version() > CURRENT_VERSION) {
                continue;
            }
            data.wardsByPair.putIfAbsent(new PairKey(ward.casterId(), ward.targetId()), ward);
        }
        return data;
    }

    public static AnchoredCurseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Looks only at currently loaded shelves. No prepared anchor means the source curse remains a
     * normal direct curse; an invalid prepared anchor rejects before the ritual transaction begins.
     */
    public Preparation prepareSinking(ServerPlayer caster, ServerPlayer target) {
        List<AnchorCandidate> candidates = new ArrayList<>();
        for (PoppetShelfBlockEntity shelf : PoppetShelfBlockEntity.getActiveShelves(caster.level().getServer())) {
            if (!(shelf.getLevel() instanceof ServerLevel shelfLevel)) {
                continue;
            }
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
                ItemStack stack = shelf.getItem(slot);
                if (matchesPreparedPoppet(stack, caster.getUUID(), target.getUUID())) {
                    int anchorSlot = slot;
                    RedThreadBinding.bindingId(stack).ifPresent(bindingId -> candidates.add(new AnchorCandidate(
                            shelfLevel, shelf.getBlockPos().immutable(), anchorSlot, bindingId)));
                }
            }
        }

        if (candidates.isEmpty()) {
            return Preparation.direct();
        }
        if (candidates.size() != 1) {
            return Preparation.rejected(PreparationIssue.AMBIGUOUS_ANCHOR);
        }

        PairKey pair = new PairKey(caster.getUUID(), target.getUUID());
        if (cursesByPair.containsKey(pair)) {
            return Preparation.rejected(PreparationIssue.DUPLICATE_PAIR);
        }
        long now = caster.level().getServer().overworld().getGameTime();
        pruneExpiredWards(now);
        if (wardsByPair.containsKey(pair)) {
            return Preparation.rejected(PreparationIssue.WARDED);
        }

        AnchorCandidate candidate = candidates.getFirst();
        if (!isBreakableShelf(candidate.level(), candidate.pos())) {
            return Preparation.rejected(PreparationIssue.INVALID_ANCHOR);
        }
        if (!WitcheryProtectionService.canModifyRemoteBlock(caster, candidate.level(), candidate.pos(),
                WitcheryProtectionAction.ITEM_TRANSFER)
                || !(target.level() instanceof ServerLevel targetLevel)
                || !WitcheryProtectionService.canModifyRemoteBlock(caster, targetLevel, target.blockPosition(),
                WitcheryProtectionAction.RITUAL_AREA_EFFECT)) {
            return Preparation.rejected(PreparationIssue.PROTECTED);
        }
        return Preparation.anchored(candidate);
    }

    /** Called immediately before the already-validated source curse action. */
    public boolean activateSinking(Preparation preparation, ServerPlayer caster, ServerPlayer target,
                                   int potencyBonus) {
        if (!preparation.anchored() || preparation.candidate().isEmpty()) {
            return false;
        }
        AnchorCandidate candidate = preparation.candidate().get();
        PairKey pair = new PairKey(caster.getUUID(), target.getUUID());
        if (cursesByPair.containsKey(pair) || hasActiveWard(pair, caster.level().getServer().overworld().getGameTime())
                || !isCandidateStillPrepared(candidate, caster.getUUID(), target.getUUID())) {
            return false;
        }

        ItemStack poppet = anchorStack(candidate);
        if (poppet.isEmpty() || !RedThreadBinding.activate(poppet, candidate.bindingId(), CurseType.SINKING.id())) {
            return false;
        }
        PoppetShelfBlockEntity shelf = anchorShelf(candidate);
        if (shelf == null) {
            RedThreadBinding.clear(poppet);
            return false;
        }

        int sinkingAmplifier = 3 + Math.min(1, Math.max(0, potencyBonus));
        int fatigueAmplifier = 2 + Math.min(1, Math.max(0, potencyBonus));
        long now = caster.level().getServer().overworld().getGameTime();
        CurseRecord record = new CurseRecord(
                CURRENT_VERSION,
                candidate.bindingId(),
                caster.getUUID(),
                caster.getName().getString(),
                target.getUUID(),
                target.getName().getString(),
                dimensionId(candidate.level()),
                candidate.pos(),
                candidate.slot(),
                CurseType.SINKING,
                CurseState.ACTIVE,
                now,
                0,
                0,
                now + TICK_INTERVAL,
                sinkingAmplifier,
                fatigueAmplifier,
                EffectSnapshot.capture(target.getEffect(MobEffects.SLOWNESS)),
                EffectSnapshot.capture(target.getEffect(MobEffects.MINING_FATIGUE)));
        cursesByPair.put(pair, record);
        pairsByBinding.put(record.bindingId(), pair);
        shelf.setChanged();
        setDirty();
        candidate.level().sendParticles(new DustParticleOptions(0xB51F35, 0.8F),
                candidate.pos().getX() + 0.5D, candidate.pos().getY() + 0.72D, candidate.pos().getZ() + 0.5D,
                18, 0.3D, 0.15D, 0.3D, 0.01D);
        caster.displayClientMessage(Component.translatable("witchery.anchored_curse.sinking.bound",
                target.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }

    public void tickLevel(ServerLevel level) {
        String dimension = dimensionId(level);
        long now = level.getServer().overworld().getGameTime();
        pruneExpiredWards(now);
        for (CurseRecord record : new ArrayList<>(cursesByPair.values())) {
            if (!record.dimension().equals(dimension) || !chunkLoaded(level, record.anchor())) {
                continue;
            }
            if (!isRecordAnchorValid(level, record)) {
                terminate(level.getServer(), record, TerminationCause.BROKEN, true);
                continue;
            }
            if (level.getGameTime() < record.nextPulseTick()) {
                continue;
            }

            record.advance(level.getGameTime() + TICK_INTERVAL);
            ServerPlayer target = level.getServer().getPlayerList().getPlayer(record.targetId());
            if (target != null && target.isAlive() && target.level() instanceof ServerLevel targetLevel) {
                ServerPlayer actor = protectionActor(targetLevel, record);
                if (WitcheryProtectionService.canModifyRemoteBlock(actor, targetLevel, target.blockPosition(),
                        WitcheryProtectionAction.RITUAL_AREA_EFFECT)) {
                    sustainSinking(target, record);
                    advanceVictimClue(target, record);
                }
            }
            if (record.activeTicks() % 100 == 0) {
                level.sendParticles(new DustParticleOptions(0x8F1028, 0.65F),
                        record.anchor().getX() + 0.5D, record.anchor().getY() + 0.72D,
                        record.anchor().getZ() + 0.5D, 3, 0.22D, 0.08D, 0.22D, 0.0D);
            }
            setDirty();
        }
    }

    public int terminateAt(ServerLevel level, BlockPos pos, TerminationCause cause) {
        int terminated = 0;
        for (CurseRecord record : new ArrayList<>(cursesByPair.values())) {
            if (record.dimension().equals(dimensionId(level)) && record.anchor().equals(pos)) {
                terminate(level.getServer(), record, cause, true);
                terminated++;
            }
        }
        return terminated;
    }

    public boolean terminateBinding(MinecraftServer server, UUID bindingId, TerminationCause cause) {
        PairKey pair = pairsByBinding.get(bindingId);
        CurseRecord record = pair == null ? null : cursesByPair.get(pair);
        if (record == null) {
            return false;
        }
        terminate(server, record, cause, true);
        return true;
    }

    public int cleanseTarget(MinecraftServer server, UUID targetId, CurseType type) {
        int terminated = 0;
        for (CurseRecord record : new ArrayList<>(cursesByPair.values())) {
            if (record.targetId().equals(targetId) && record.curseType() == type) {
                terminate(server, record, TerminationCause.CLEANSED, true);
                terminated++;
            }
        }
        return terminated;
    }

    public Optional<AnchorClue> clueFor(UUID targetId) {
        return cursesByPair.values().stream()
                .filter(record -> record.targetId().equals(targetId))
                .findFirst()
                .map(record -> new AnchorClue(record.curseType(), record.clueStage(), record.dimension(),
                        record.anchor(), record.activeTicks()));
    }

    public boolean hasBinding(UUID bindingId) {
        return pairsByBinding.containsKey(bindingId);
    }

    public boolean bindingMatchesAnchor(UUID bindingId, ServerLevel level, BlockPos pos, int slot) {
        PairKey pair = pairsByBinding.get(bindingId);
        CurseRecord record = pair == null ? null : cursesByPair.get(pair);
        return record != null && record.dimension().equals(dimensionId(level))
                && record.anchor().equals(pos) && record.slot() == slot;
    }

    private void sustainSinking(ServerPlayer target, CurseRecord record) {
        MobEffectInstance slowness = target.getEffect(MobEffects.SLOWNESS);
        if (slowness == null || slowness.getAmplifier() <= record.slownessAmplifier()
                && slowness.getDuration() <= EFFECT_REFRESH_THRESHOLD) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_REFRESH_DURATION,
                    record.slownessAmplifier()));
        }
        MobEffectInstance fatigue = target.getEffect(MobEffects.MINING_FATIGUE);
        if (fatigue == null || fatigue.getAmplifier() <= record.fatigueAmplifier()
                && fatigue.getDuration() <= EFFECT_REFRESH_THRESHOLD) {
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, EFFECT_REFRESH_DURATION,
                    record.fatigueAmplifier()));
        }
    }

    private void advanceVictimClue(ServerPlayer target, CurseRecord record) {
        int expected = clueStage(record.activeTicks());
        while (record.clueStage() < expected) {
            record.incrementClueStage();
            target.displayClientMessage(Component.translatable(
                    "witchery.anchored_curse.sinking.clue." + record.clueStage())
                    .withStyle(ChatFormatting.DARK_RED), false);
            target.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE,
                    SoundSource.PLAYERS, 0.45F, 0.65F + record.clueStage() * 0.12F);
        }
    }

    private void terminate(MinecraftServer server, CurseRecord record, TerminationCause cause, boolean grantWard) {
        PairKey pair = new PairKey(record.casterId(), record.targetId());
        if (!cursesByPair.remove(pair, record)) {
            return;
        }
        pairsByBinding.remove(record.bindingId(), pair);
        clearLoadedAnchor(server, record);
        ServerPlayer target = server.getPlayerList().getPlayer(record.targetId());
        if (target != null) {
            restoreSinkingEffects(target, record, server.overworld().getGameTime());
            target.displayClientMessage(Component.translatable("witchery.anchored_curse.sinking.relief")
                    .withStyle(ChatFormatting.GREEN), false);
            if (target.level() instanceof ServerLevel targetLevel) {
                targetLevel.sendParticles(ParticleTypes.POOF,
                        target.getX(), target.getY() + 0.8D, target.getZ(),
                        16, 0.3D, 0.45D, 0.3D, 0.02D);
                targetLevel.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                        SoundSource.PLAYERS, 0.8F, 1.25F);
            }
        }
        if (grantWard) {
            long until = server.overworld().getGameTime() + WARD_TICKS;
            wardsByPair.put(pair, new WardRecord(CURRENT_VERSION, pair.casterId(), pair.targetId(), until));
        }
        setDirty();
    }

    private void restoreSinkingEffects(ServerPlayer target, CurseRecord record, long now) {
        restoreEffect(target, MobEffects.SLOWNESS, record.slownessAmplifier(), record.priorSlowness(),
                elapsedSince(record, now));
        restoreEffect(target, MobEffects.MINING_FATIGUE, record.fatigueAmplifier(), record.priorFatigue(),
                elapsedSince(record, now));
    }

    private static void restoreEffect(ServerPlayer target, net.minecraft.core.Holder<MobEffect> effect,
                                      int anchoredAmplifier, Optional<EffectSnapshot> prior, int elapsed) {
        MobEffectInstance current = target.getEffect(effect);
        if (current == null || current.getAmplifier() != anchoredAmplifier) {
            return;
        }
        target.removeEffect(effect);
        prior.flatMap(snapshot -> snapshot.restore(effect, elapsed)).ifPresent(target::addEffect);
    }

    private void clearLoadedAnchor(MinecraftServer server, CurseRecord record) {
        ServerLevel level = level(server, record.dimension());
        if (level == null || !chunkLoaded(level, record.anchor())
                || !(level.getBlockEntity(record.anchor()) instanceof PoppetShelfBlockEntity shelf)
                || record.slot() < 0 || record.slot() >= shelf.getContainerSize()) {
            return;
        }
        ItemStack stack = shelf.getItem(record.slot());
        if (RedThreadBinding.bindingId(stack).filter(record.bindingId()::equals).isPresent()) {
            RedThreadBinding.clear(stack);
            shelf.setChanged();
        }
        level.sendParticles(ParticleTypes.POOF,
                record.anchor().getX() + 0.5D, record.anchor().getY() + 0.7D,
                record.anchor().getZ() + 0.5D, 10, 0.25D, 0.1D, 0.25D, 0.02D);
    }

    private static boolean matchesPreparedPoppet(ItemStack stack, UUID casterId, UUID targetId) {
        return RedThreadBinding.isPrepared(stack)
                && !RedThreadBinding.isActive(stack)
                && RedThreadBinding.casterId(stack).filter(casterId::equals).isPresent()
                && PoppetItem.getBoundUUID(stack).filter(targetId::equals).isPresent();
    }

    private static boolean isBreakableShelf(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(WitcheryBlocks.POPPET_SHELF.get())
                && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0.0F;
    }

    private static boolean isCandidateStillPrepared(AnchorCandidate candidate, UUID casterId, UUID targetId) {
        return isBreakableShelf(candidate.level(), candidate.pos())
                && matchesPreparedPoppet(anchorStack(candidate), casterId, targetId);
    }

    private static boolean isRecordAnchorValid(ServerLevel level, CurseRecord record) {
        if (!isBreakableShelf(level, record.anchor())
                || !(level.getBlockEntity(record.anchor()) instanceof PoppetShelfBlockEntity shelf)
                || record.slot() < 0 || record.slot() >= shelf.getContainerSize()) {
            return false;
        }
        ItemStack stack = shelf.getItem(record.slot());
        return RedThreadBinding.isActive(stack)
                && RedThreadBinding.bindingId(stack).filter(record.bindingId()::equals).isPresent()
                && RedThreadBinding.casterId(stack).filter(record.casterId()::equals).isPresent()
                && PoppetItem.getBoundUUID(stack).filter(record.targetId()::equals).isPresent()
                && record.curseType().id().equals(RedThreadBinding.activeCurse(stack));
    }

    private static ItemStack anchorStack(AnchorCandidate candidate) {
        PoppetShelfBlockEntity shelf = anchorShelf(candidate);
        return shelf == null || candidate.slot() < 0 || candidate.slot() >= shelf.getContainerSize()
                ? ItemStack.EMPTY : shelf.getItem(candidate.slot());
    }

    @Nullable
    private static PoppetShelfBlockEntity anchorShelf(AnchorCandidate candidate) {
        return candidate.level().getBlockEntity(candidate.pos()) instanceof PoppetShelfBlockEntity shelf
                ? shelf : null;
    }

    private static ServerPlayer protectionActor(ServerLevel level, CurseRecord record) {
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(record.casterId());
        if (online != null && online.isAlive()) {
            return online;
        }
        return FakePlayerFactory.get(level, new GameProfile(record.casterId(),
                record.casterName().isBlank() ? "WitcheryAnchor" : record.casterName()));
    }

    private boolean hasActiveWard(PairKey pair, long now) {
        WardRecord ward = wardsByPair.get(pair);
        if (ward == null) {
            return false;
        }
        if (ward.untilTick() <= now) {
            wardsByPair.remove(pair);
            setDirty();
            return false;
        }
        return true;
    }

    private void pruneExpiredWards(long now) {
        if (wardsByPair.entrySet().removeIf(entry -> entry.getValue().untilTick() <= now)) {
            setDirty();
        }
    }

    private List<CurseRecord> recordsForCodec() {
        return List.copyOf(cursesByPair.values());
    }

    private List<WardRecord> wardsForCodec() {
        return List.copyOf(wardsByPair.values());
    }

    static AnchoredCurseSavedData fromRecordsForTest(List<CurseRecord> records, List<WardRecord> wards) {
        return fromCodec(records, wards);
    }

    static List<CurseRecord> recordsForTest(AnchoredCurseSavedData data) {
        return data.recordsForCodec();
    }

    static List<WardRecord> wardsForTest(AnchoredCurseSavedData data) {
        return data.wardsForCodec();
    }

    private static int clueStage(int activeTicks) {
        if (activeTicks >= CLUE_THREE_TICKS) return 3;
        if (activeTicks >= CLUE_TWO_TICKS) return 2;
        if (activeTicks >= CLUE_ONE_TICKS) return 1;
        return 0;
    }

    private static int elapsedSince(CurseRecord record, long now) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, now - record.startTime()));
    }

    private static boolean chunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    @Nullable
    private static ServerLevel level(MinecraftServer server, String dimension) {
        try {
            var key = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.Identifier.parse(dimension));
            return server.getLevel(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public enum CurseType {
        SINKING("sinking");

        private static final Codec<CurseType> CODEC = Codec.STRING.xmap(CurseType::parse, CurseType::id);
        private final String id;

        CurseType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        private static CurseType parse(String value) {
            return SINKING;
        }
    }

    public enum CurseState {
        ACTIVE,
        ENDED;

        private static final Codec<CurseState> CODEC = Codec.STRING.xmap(CurseState::parse, CurseState::name);

        private static CurseState parse(String value) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return ENDED;
            }
        }
    }

    public enum PreparationIssue {
        NONE,
        AMBIGUOUS_ANCHOR,
        DUPLICATE_PAIR,
        WARDED,
        INVALID_ANCHOR,
        PROTECTED
    }

    public enum TerminationCause {
        BROKEN,
        CLEANSED,
        REMOVED
    }

    public record Preparation(boolean anchored, PreparationIssue issue, Optional<AnchorCandidate> candidate) {
        private static Preparation direct() {
            return new Preparation(false, PreparationIssue.NONE, Optional.empty());
        }

        private static Preparation anchored(AnchorCandidate candidate) {
            return new Preparation(true, PreparationIssue.NONE, Optional.of(candidate));
        }

        private static Preparation rejected(PreparationIssue issue) {
            return new Preparation(false, issue, Optional.empty());
        }

        public boolean allowed() {
            return issue == PreparationIssue.NONE;
        }
    }

    public record AnchorCandidate(ServerLevel level, BlockPos pos, int slot, UUID bindingId) {
    }

    public record AnchorClue(CurseType curseType, int stage, String dimension, BlockPos anchor, int activeTicks) {
    }

    private record PairKey(UUID casterId, UUID targetId) {
    }

    public static final class CurseRecord {
        public static final Codec<CurseRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CurseIdentity.CODEC.fieldOf("identity").forGetter(CurseRecord::identity),
                CurseRuntime.CODEC.optionalFieldOf("runtime", CurseRuntime.defaults())
                        .forGetter(CurseRecord::runtime)
        ).apply(instance, CurseRecord::fromParts));

        private final int version;
        private final UUID bindingId;
        private final UUID casterId;
        private final String casterName;
        private final UUID targetId;
        private final String targetName;
        private final String dimension;
        private final BlockPos anchor;
        private final int slot;
        private final CurseType curseType;
        private final CurseState state;
        private final long startTime;
        private int activeTicks;
        private int clueStage;
        private long nextPulseTick;
        private final int slownessAmplifier;
        private final int fatigueAmplifier;
        private final Optional<EffectSnapshot> priorSlowness;
        private final Optional<EffectSnapshot> priorFatigue;

        public CurseRecord(int version, UUID bindingId, UUID casterId, String casterName,
                           UUID targetId, String targetName, String dimension, BlockPos anchor, int slot,
                           CurseType curseType, CurseState state, long startTime, int activeTicks,
                           int clueStage, long nextPulseTick, int slownessAmplifier, int fatigueAmplifier,
                           Optional<EffectSnapshot> priorSlowness, Optional<EffectSnapshot> priorFatigue) {
            this.version = Math.max(0, version);
            this.bindingId = bindingId;
            this.casterId = casterId;
            this.casterName = casterName == null ? "" : casterName;
            this.targetId = targetId;
            this.targetName = targetName == null ? "" : targetName;
            this.dimension = dimension == null ? "" : dimension;
            this.anchor = anchor.immutable();
            this.slot = Math.max(0, Math.min(PoppetShelfBlockEntity.SLOT_COUNT - 1, slot));
            this.curseType = curseType == null ? CurseType.SINKING : curseType;
            this.state = state == null ? CurseState.ENDED : state;
            this.startTime = Math.max(0L, startTime);
            this.activeTicks = Math.max(0, activeTicks);
            this.clueStage = Math.max(0, Math.min(3, clueStage));
            this.nextPulseTick = Math.max(0L, nextPulseTick);
            this.slownessAmplifier = Math.max(0, slownessAmplifier);
            this.fatigueAmplifier = Math.max(0, fatigueAmplifier);
            this.priorSlowness = priorSlowness == null ? Optional.empty() : priorSlowness;
            this.priorFatigue = priorFatigue == null ? Optional.empty() : priorFatigue;
        }

        public int version() { return version; }
        public UUID bindingId() { return bindingId; }
        public UUID casterId() { return casterId; }
        public String casterName() { return casterName; }
        public UUID targetId() { return targetId; }
        public String targetName() { return targetName; }
        public String dimension() { return dimension; }
        public BlockPos anchor() { return anchor; }
        public int slot() { return slot; }
        public CurseType curseType() { return curseType; }
        public CurseState state() { return state; }
        public long startTime() { return startTime; }
        public int activeTicks() { return activeTicks; }
        public int clueStage() { return clueStage; }
        public long nextPulseTick() { return nextPulseTick; }
        public int slownessAmplifier() { return slownessAmplifier; }
        public int fatigueAmplifier() { return fatigueAmplifier; }
        public Optional<EffectSnapshot> priorSlowness() { return priorSlowness; }
        public Optional<EffectSnapshot> priorFatigue() { return priorFatigue; }

        private CurseIdentity identity() {
            return new CurseIdentity(version, bindingId, casterId, casterName, targetId, targetName,
                    dimension, anchor, slot, curseType);
        }

        private CurseRuntime runtime() {
            return new CurseRuntime(state, startTime, activeTicks, clueStage, nextPulseTick,
                    slownessAmplifier, fatigueAmplifier, priorSlowness, priorFatigue);
        }

        private static CurseRecord fromParts(CurseIdentity identity, CurseRuntime runtime) {
            return new CurseRecord(identity.version(), identity.bindingId(), identity.casterId(),
                    identity.casterName(), identity.targetId(), identity.targetName(), identity.dimension(),
                    identity.anchor(), identity.slot(), identity.curseType(), runtime.state(), runtime.startTime(),
                    runtime.activeTicks(), runtime.clueStage(), runtime.nextPulseTick(), runtime.slownessAmplifier(),
                    runtime.fatigueAmplifier(), runtime.priorSlowness(), runtime.priorFatigue());
        }

        private void advance(long nextPulseTick) {
            activeTicks = Math.min(Integer.MAX_VALUE - TICK_INTERVAL, activeTicks) + TICK_INTERVAL;
            this.nextPulseTick = nextPulseTick;
        }

        private void incrementClueStage() {
            clueStage = Math.min(3, clueStage + 1);
        }
    }

    public record CurseIdentity(int version, UUID bindingId, UUID casterId, String casterName,
                                UUID targetId, String targetName, String dimension, BlockPos anchor,
                                int slot, CurseType curseType) {
        public static final Codec<CurseIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("version", 0).forGetter(CurseIdentity::version),
                UUIDUtil.CODEC.fieldOf("binding_id").forGetter(CurseIdentity::bindingId),
                UUIDUtil.CODEC.fieldOf("caster_id").forGetter(CurseIdentity::casterId),
                Codec.STRING.optionalFieldOf("caster_name", "").forGetter(CurseIdentity::casterName),
                UUIDUtil.CODEC.fieldOf("target_id").forGetter(CurseIdentity::targetId),
                Codec.STRING.optionalFieldOf("target_name", "").forGetter(CurseIdentity::targetName),
                Codec.STRING.fieldOf("dimension").forGetter(CurseIdentity::dimension),
                BlockPos.CODEC.fieldOf("anchor").forGetter(CurseIdentity::anchor),
                Codec.INT.fieldOf("slot").forGetter(CurseIdentity::slot),
                CurseType.CODEC.fieldOf("curse_type").forGetter(CurseIdentity::curseType)
        ).apply(instance, CurseIdentity::new));
    }

    public record CurseRuntime(CurseState state, long startTime, int activeTicks, int clueStage,
                               long nextPulseTick, int slownessAmplifier, int fatigueAmplifier,
                               Optional<EffectSnapshot> priorSlowness, Optional<EffectSnapshot> priorFatigue) {
        public static final Codec<CurseRuntime> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CurseState.CODEC.optionalFieldOf("state", CurseState.ENDED).forGetter(CurseRuntime::state),
                Codec.LONG.optionalFieldOf("start_time", 0L).forGetter(CurseRuntime::startTime),
                Codec.INT.optionalFieldOf("active_ticks", 0).forGetter(CurseRuntime::activeTicks),
                Codec.INT.optionalFieldOf("clue_stage", 0).forGetter(CurseRuntime::clueStage),
                Codec.LONG.optionalFieldOf("next_pulse_tick", 0L).forGetter(CurseRuntime::nextPulseTick),
                Codec.INT.optionalFieldOf("slowness_amplifier", 3).forGetter(CurseRuntime::slownessAmplifier),
                Codec.INT.optionalFieldOf("fatigue_amplifier", 2).forGetter(CurseRuntime::fatigueAmplifier),
                EffectSnapshot.CODEC.optionalFieldOf("prior_slowness").forGetter(CurseRuntime::priorSlowness),
                EffectSnapshot.CODEC.optionalFieldOf("prior_fatigue").forGetter(CurseRuntime::priorFatigue)
        ).apply(instance, CurseRuntime::new));

        private static CurseRuntime defaults() {
            return new CurseRuntime(CurseState.ENDED, 0L, 0, 0, 0L, 3, 2,
                    Optional.empty(), Optional.empty());
        }
    }

    public record WardRecord(int version, UUID casterId, UUID targetId, long untilTick) {
        public static final Codec<WardRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("version", 0).forGetter(WardRecord::version),
                UUIDUtil.CODEC.fieldOf("caster_id").forGetter(WardRecord::casterId),
                UUIDUtil.CODEC.fieldOf("target_id").forGetter(WardRecord::targetId),
                Codec.LONG.fieldOf("until_tick").forGetter(WardRecord::untilTick)
        ).apply(instance, WardRecord::new));
    }

    public record EffectSnapshot(int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        public static final Codec<EffectSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("duration").forGetter(EffectSnapshot::duration),
                Codec.INT.fieldOf("amplifier").forGetter(EffectSnapshot::amplifier),
                Codec.BOOL.optionalFieldOf("ambient", false).forGetter(EffectSnapshot::ambient),
                Codec.BOOL.optionalFieldOf("visible", true).forGetter(EffectSnapshot::visible),
                Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(EffectSnapshot::showIcon)
        ).apply(instance, EffectSnapshot::new));

        private static Optional<EffectSnapshot> capture(@Nullable MobEffectInstance effect) {
            return effect == null ? Optional.empty() : Optional.of(new EffectSnapshot(
                    effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }

        private Optional<MobEffectInstance> restore(net.minecraft.core.Holder<MobEffect> effect, int elapsed) {
            int remaining = duration - Math.max(0, elapsed);
            return remaining <= 0 ? Optional.empty() : Optional.of(new MobEffectInstance(
                    effect, remaining, amplifier, ambient, visible, showIcon));
        }
    }
}
