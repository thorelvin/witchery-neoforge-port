package com.emoniph.witchery.data;

import com.emoniph.witchery.blocks.BlightHelper;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Global owner and restoration journal for Blight Hearts.
 *
 * <p>Only this data owns active Heart spread. Blighted blocks never rediscover Hearts with world
 * scans; they use the exact mutation index and legacy, unindexed blocks decay independently.
 */
public final class BlightHeartSavedData extends SavedData {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_SPREAD_MUTATIONS = 512;
    public static final int MAX_LEDGER_ENTRIES = MAX_SPREAD_MUTATIONS + 1;
    public static final int MIN_HEART_SEPARATION = BlightHelper.HEART_RADIUS * 2;
    public static final long SPREAD_INTERVAL_TICKS = 20L;

    static final int MAX_PERSISTED_HEARTS = 4096;
    static final int CLEANUP_HEARTS_PER_LEVEL_TICK = 4;
    static final int CLEANUP_INSPECTIONS_PER_HEART = 12;
    static final int CLEANUP_RESTORES_PER_HEART = 4;

    public static final Codec<BlightHeartSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    HeartRecord.CODEC.listOf().optionalFieldOf("hearts", List.of())
                            .forGetter(BlightHeartSavedData::recordsForCodec)
            ).apply(instance, BlightHeartSavedData::fromCodec));

    public static final SavedDataType<BlightHeartSavedData> TYPE = new SavedDataType<>(
            "witchery_blight_hearts",
            BlightHeartSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, HeartRecord> heartsByOwner = new LinkedHashMap<>();
    private final Map<AnchorKey, UUID> anchors = new HashMap<>();
    private final Map<String, Map<Long, Set<UUID>>> anchorsByChunk = new HashMap<>();
    private final Map<AnchorKey, UUID> mutationOwners = new HashMap<>();
    private final Map<String, ArrayDeque<UUID>> cleanupQueues = new HashMap<>();
    private final Set<UUID> queuedForCleanup = new HashSet<>();

    public BlightHeartSavedData() {
    }

    private static BlightHeartSavedData fromCodec(List<HeartRecord> records) {
        BlightHeartSavedData data = new BlightHeartSavedData();
        int accepted = 0;
        for (HeartRecord record : records) {
            if (accepted >= MAX_PERSISTED_HEARTS
                    || record.version() > CURRENT_VERSION
                    || !record.hasAnchorSnapshot()
                    || data.heartsByOwner.putIfAbsent(record.ownerId(), record) != null) {
                continue;
            }
            accepted++;
        }
        data.rebuildIndexes();
        return data;
    }

    public static BlightHeartSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public PlacementResult canRegister(ServerLevel level, ServerPlayer owner, BlockPos anchor) {
        if (level == null || owner == null || anchor == null) {
            return PlacementResult.INVALID;
        }
        if (heartsByOwner.containsKey(owner.getUUID())) {
            return PlacementResult.OWNER_LIMIT;
        }

        String dimension = dimensionId(level);
        int chunkRadius = (MIN_HEART_SEPARATION + 15) / 16;
        ChunkPos centerChunk = new ChunkPos(anchor);
        Map<Long, Set<UUID>> dimensionIndex = anchorsByChunk.get(dimension);
        if (dimensionIndex == null) {
            return PlacementResult.ALLOWED;
        }
        int separationSq = MIN_HEART_SEPARATION * MIN_HEART_SEPARATION;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                Set<UUID> candidates = dimensionIndex.get(ChunkPos.asLong(centerChunk.x + dx, centerChunk.z + dz));
                if (candidates == null) {
                    continue;
                }
                for (UUID candidateId : candidates) {
                    HeartRecord candidate = heartsByOwner.get(candidateId);
                    if (candidate != null && candidate.anchor().distSqr(anchor) < separationSq) {
                        return PlacementResult.NEARBY_HEART;
                    }
                }
            }
        }
        return PlacementResult.ALLOWED;
    }

    public PlacementResult register(ServerLevel level, ServerPlayer owner, BlockPos anchor,
                                    BlockState originalState) {
        PlacementResult result = canRegister(level, owner, anchor);
        if (result != PlacementResult.ALLOWED || originalState == null) {
            return result == PlacementResult.ALLOWED ? PlacementResult.INVALID : result;
        }

        HeartRecord record = HeartRecord.create(owner, dimensionId(level), anchor, originalState);
        heartsByOwner.put(record.ownerId(), record);
        indexHeart(record);
        setDirty();
        return PlacementResult.ALLOWED;
    }

    public void unregisterFailedPlacement(ServerLevel level, UUID ownerId) {
        HeartRecord record = heartsByOwner.get(ownerId);
        if (record != null && record.dimension().equals(dimensionId(level)) && record.mutations().size() == 1) {
            removeHeart(record);
        }
    }

    @Nullable
    public HeartRecord heartAt(ServerLevel level, BlockPos anchor) {
        UUID ownerId = anchors.get(new AnchorKey(dimensionId(level), anchor));
        return ownerId == null ? null : heartsByOwner.get(ownerId);
    }

    @Nullable
    public HeartRecord heartOwnedBy(UUID ownerId) {
        return heartsByOwner.get(ownerId);
    }

    @Nullable
    public HeartRecord heartOwningMutation(ServerLevel level, BlockPos pos) {
        UUID ownerId = mutationOwners.get(new AnchorKey(dimensionId(level), pos));
        return ownerId == null ? null : heartsByOwner.get(ownerId);
    }

    public boolean isSustained(ServerLevel level, BlockPos pos) {
        HeartRecord record = heartOwningMutation(level, pos);
        return record != null && record.state() != HeartState.CLEANSING;
    }

    public boolean hasSustainingHeartNear(ServerLevel level, BlockPos pos) {
        String dimension = dimensionId(level);
        Map<Long, Set<UUID>> dimensionIndex = anchorsByChunk.get(dimension);
        if (dimensionIndex == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(pos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Set<UUID> candidates = dimensionIndex.get(ChunkPos.asLong(chunk.x + dx, chunk.z + dz));
                if (candidates == null) {
                    continue;
                }
                for (UUID ownerId : candidates) {
                    HeartRecord record = heartsByOwner.get(ownerId);
                    if (record != null && record.state() != HeartState.CLEANSING
                            && record.anchor().distSqr(pos) <= BlightHelper.HEART_RADIUS_SQ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean claimSpreadWindow(HeartRecord record, long gameTime) {
        if (record == null || record.state() != HeartState.ACTIVE || record.remainingBudget() <= 0
                || gameTime < record.nextSpreadTick()) {
            return false;
        }
        record.setNextSpreadTick(gameTime + SPREAD_INTERVAL_TICKS);
        setDirty();
        return true;
    }

    public Optional<BlockPos> randomSpreadOrigin(HeartRecord record, RandomSource random) {
        if (record == null || record.mutations().isEmpty()) {
            return Optional.empty();
        }
        List<BlockPos> positions = new ArrayList<>(record.mutations().keySet());
        return Optional.of(positions.get(random.nextInt(positions.size())));
    }

    public boolean reserveMutation(HeartRecord record, BlockPos pos, BlockState originalState,
                                   BlockState appliedState) {
        if (record == null || record.state() != HeartState.ACTIVE || record.remainingBudget() <= 0
                || record.mutations().size() >= MAX_LEDGER_ENTRIES
                || originalState == null || appliedState == null) {
            return false;
        }
        AnchorKey key = new AnchorKey(record.dimension(), pos);
        if (mutationOwners.containsKey(key) || record.anchor().distSqr(pos) > BlightHelper.HEART_RADIUS_SQ) {
            return false;
        }

        record.addMutation(new Mutation(pos, originalState, appliedState));
        record.consumeBudget();
        mutationOwners.put(key, record.ownerId());
        setDirty();
        return true;
    }

    public void rollbackFailedMutation(HeartRecord record, BlockPos pos) {
        if (record == null || pos.equals(record.anchor())) {
            return;
        }
        Mutation removed = record.removeMutation(pos);
        if (removed != null) {
            mutationOwners.remove(new AnchorKey(record.dimension(), pos), record.ownerId());
            record.refundBudget();
            setDirty();
        }
    }

    public void forgetMutation(ServerLevel level, BlockPos pos) {
        HeartRecord record = heartOwningMutation(level, pos);
        if (record == null || pos.equals(record.anchor())) {
            return;
        }
        detachMutation(record, pos);
        finishCleanupIfEmpty(level, record, pos);
    }

    public void beginCleansing(ServerLevel level, BlockPos anchor, @Nullable ServerPlayer actor) {
        HeartRecord record = heartAt(level, anchor);
        if (record == null) {
            return;
        }
        if (actor != null) {
            record.setCleanupActor(actor.getUUID(), actor.getGameProfile().name());
        }
        if (record.state() != HeartState.CLEANSING) {
            record.setState(HeartState.CLEANSING);
        }
        queueCleanup(record);
        setDirty();
    }

    public boolean restoreMutation(ServerLevel level, BlockPos pos, @Nullable ServerPlayer actor,
                                   boolean beginHeartCleanup) {
        HeartRecord record = heartOwningMutation(level, pos);
        if (record == null) {
            return false;
        }
        if (beginHeartCleanup && pos.equals(record.anchor())) {
            beginCleansing(level, pos, actor);
        }
        ServerPlayer protectionActor = actor != null ? actor : cleanupActor(level, record);
        return restoreMutation(level, record, pos, protectionActor);
    }

    public void tickLevel(ServerLevel level) {
        String dimension = dimensionId(level);
        ArrayDeque<UUID> queue = cleanupQueues.get(dimension);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        int hearts = Math.min(CLEANUP_HEARTS_PER_LEVEL_TICK, queue.size());
        for (int i = 0; i < hearts; i++) {
            UUID ownerId = queue.pollFirst();
            queuedForCleanup.remove(ownerId);
            HeartRecord record = heartsByOwner.get(ownerId);
            if (record == null || record.state() != HeartState.CLEANSING
                    || !record.dimension().equals(dimension)) {
                continue;
            }
            processCleanup(level, record);
            if (heartsByOwner.containsKey(ownerId) && record.state() == HeartState.CLEANSING) {
                queueCleanup(record);
            }
        }
    }

    private void processCleanup(ServerLevel level, HeartRecord record) {
        ServerPlayer actor = cleanupActor(level, record);
        int inspected = 0;
        int restored = 0;
        while (inspected < CLEANUP_INSPECTIONS_PER_HEART
                && restored < CLEANUP_RESTORES_PER_HEART && !record.mutations().isEmpty()) {
            BlockPos pos = record.firstMutationPos();
            inspected++;
            if (!level.isLoaded(pos)) {
                record.rotateMutation(pos);
                continue;
            }
            if (restoreMutation(level, record, pos, actor)) {
                restored++;
            } else {
                record.rotateMutation(pos);
            }
        }
    }

    private boolean restoreMutation(ServerLevel level, HeartRecord record, BlockPos pos,
                                    @Nullable ServerPlayer actor) {
        Mutation mutation = record.mutations().get(pos);
        if (mutation == null || !record.dimension().equals(dimensionId(level)) || !level.isLoaded(pos)) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        boolean restorable = current.equals(mutation.appliedState())
                || BlightHelper.isBlight(mutation.appliedState()) && BlightHelper.isBlight(current)
                || pos.equals(record.anchor()) && record.state() == HeartState.CLEANSING && current.isAir();
        if (!restorable) {
            detachMutation(record, pos);
            finishCleanupIfEmpty(level, record, pos);
            return true;
        }
        if (!WitcheryProtectionService.canModifyBlock(actor, level, pos,
                WitcheryProtectionAction.BLOCK_TRANSFORM)) {
            return false;
        }

        Mutation removed = detachMutation(record, pos);
        if (!level.setBlock(pos, mutation.originalState(), Block.UPDATE_ALL)) {
            record.addMutation(removed);
            mutationOwners.put(new AnchorKey(record.dimension(), pos), record.ownerId());
            setDirty();
            return false;
        }
        BlightHelper.cleanseFeedback(level, pos, 4);
        finishCleanupIfEmpty(level, record, pos);
        return true;
    }

    @Nullable
    private Mutation detachMutation(HeartRecord record, BlockPos pos) {
        Mutation removed = record.removeMutation(pos);
        if (removed == null) {
            return null;
        }
        mutationOwners.remove(new AnchorKey(record.dimension(), pos), record.ownerId());
        setDirty();
        return removed;
    }

    private void finishCleanupIfEmpty(ServerLevel level, HeartRecord record, BlockPos lastPos) {
        if (record.state() != HeartState.CLEANSING || !record.mutations().isEmpty()) {
            return;
        }
        removeHeart(record);
        if (level.isLoaded(lastPos)) {
            level.playSound(null, lastPos, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.BLOCKS, 0.9F, 1.35F);
            level.sendParticles(ParticleTypes.POOF,
                    lastPos.getX() + 0.5D, lastPos.getY() + 0.5D, lastPos.getZ() + 0.5D,
                    18, 0.6D, 0.4D, 0.6D, 0.03D);
        }
    }

    @Nullable
    private ServerPlayer cleanupActor(ServerLevel level, HeartRecord record) {
        UUID actorId = record.cleanupActorId().orElse(record.ownerId());
        String actorName = record.cleanupActorName().isBlank()
                ? record.ownerName() : record.cleanupActorName();
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(actorId);
        if (online != null && online.level() == level && online.isAlive()) {
            return online;
        }
        return FakePlayerFactory.get(level, new GameProfile(actorId,
                actorName.isBlank() ? "WitcheryBlight" : actorName));
    }

    private void removeHeart(HeartRecord record) {
        heartsByOwner.remove(record.ownerId(), record);
        anchors.remove(new AnchorKey(record.dimension(), record.anchor()), record.ownerId());
        Map<Long, Set<UUID>> dimensionIndex = anchorsByChunk.get(record.dimension());
        if (dimensionIndex != null) {
            long chunk = new ChunkPos(record.anchor()).toLong();
            Set<UUID> owners = dimensionIndex.get(chunk);
            if (owners != null) {
                owners.remove(record.ownerId());
                if (owners.isEmpty()) {
                    dimensionIndex.remove(chunk);
                }
            }
            if (dimensionIndex.isEmpty()) {
                anchorsByChunk.remove(record.dimension());
            }
        }
        for (BlockPos pos : record.mutations().keySet()) {
            mutationOwners.remove(new AnchorKey(record.dimension(), pos), record.ownerId());
        }
        queuedForCleanup.remove(record.ownerId());
        setDirty();
    }

    private void rebuildIndexes() {
        anchors.clear();
        anchorsByChunk.clear();
        mutationOwners.clear();
        cleanupQueues.clear();
        queuedForCleanup.clear();
        for (HeartRecord record : heartsByOwner.values()) {
            indexHeart(record);
            if (record.state() == HeartState.CLEANSING) {
                queueCleanup(record);
            }
        }
    }

    private void indexHeart(HeartRecord record) {
        anchors.put(new AnchorKey(record.dimension(), record.anchor()), record.ownerId());
        anchorsByChunk.computeIfAbsent(record.dimension(), ignored -> new HashMap<>())
                .computeIfAbsent(new ChunkPos(record.anchor()).toLong(), ignored -> new HashSet<>())
                .add(record.ownerId());
        for (BlockPos pos : record.mutations().keySet()) {
            mutationOwners.putIfAbsent(new AnchorKey(record.dimension(), pos), record.ownerId());
        }
    }

    private void queueCleanup(HeartRecord record) {
        if (queuedForCleanup.add(record.ownerId())) {
            cleanupQueues.computeIfAbsent(record.dimension(), ignored -> new ArrayDeque<>())
                    .addLast(record.ownerId());
        }
    }

    private List<HeartRecord> recordsForCodec() {
        return List.copyOf(heartsByOwner.values());
    }

    static List<HeartRecord> recordsForTest(BlightHeartSavedData data) {
        return data.recordsForCodec();
    }

    static BlightHeartSavedData fromRecordsForTest(List<HeartRecord> records) {
        return fromCodec(records);
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public enum PlacementResult {
        ALLOWED,
        OWNER_LIMIT,
        NEARBY_HEART,
        INVALID
    }

    public enum HeartState {
        ACTIVE,
        DISABLED,
        CLEANSING;

        private static HeartState parse(String value) {
            try {
                return HeartState.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return DISABLED;
            }
        }
    }

    public static final class HeartRecord {
        static final Codec<HeartState> STATE_CODEC = Codec.STRING.xmap(HeartState::parse, HeartState::name);
        public static final Codec<HeartRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("version", 0).forGetter(HeartRecord::version),
                UUIDUtil.CODEC.fieldOf("owner_id").forGetter(HeartRecord::ownerId),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(HeartRecord::ownerName),
                Codec.STRING.fieldOf("dimension").forGetter(HeartRecord::dimension),
                BlockPos.CODEC.fieldOf("anchor").forGetter(HeartRecord::anchor),
                STATE_CODEC.optionalFieldOf("state", HeartState.DISABLED).forGetter(HeartRecord::state),
                Codec.INT.optionalFieldOf("remaining_budget", 0).forGetter(HeartRecord::remainingBudget),
                Codec.LONG.optionalFieldOf("next_spread_tick", 0L).forGetter(HeartRecord::nextSpreadTick),
                UUIDUtil.CODEC.optionalFieldOf("cleanup_actor_id").forGetter(HeartRecord::cleanupActorId),
                Codec.STRING.optionalFieldOf("cleanup_actor_name", "").forGetter(HeartRecord::cleanupActorName),
                Mutation.CODEC.listOf().optionalFieldOf("mutations", List.of()).forGetter(HeartRecord::mutationList)
        ).apply(instance, HeartRecord::new));

        private final int version;
        private final UUID ownerId;
        private final String ownerName;
        private final String dimension;
        private final BlockPos anchor;
        private HeartState state;
        private int remainingBudget;
        private long nextSpreadTick;
        private Optional<UUID> cleanupActorId;
        private String cleanupActorName;
        private final LinkedHashMap<BlockPos, Mutation> mutations = new LinkedHashMap<>();

        public HeartRecord(int version, UUID ownerId, String ownerName, String dimension, BlockPos anchor,
                           HeartState state, int remainingBudget, long nextSpreadTick,
                           Optional<UUID> cleanupActorId, String cleanupActorName, List<Mutation> mutations) {
            this.version = Math.max(0, version);
            this.ownerId = ownerId;
            this.ownerName = ownerName == null ? "" : ownerName;
            this.dimension = dimension == null ? "" : dimension;
            this.anchor = anchor.immutable();
            this.state = state == null ? HeartState.DISABLED : state;
            this.remainingBudget = Math.max(0, Math.min(MAX_SPREAD_MUTATIONS, remainingBudget));
            this.nextSpreadTick = Math.max(0L, nextSpreadTick);
            this.cleanupActorId = cleanupActorId == null ? Optional.empty() : cleanupActorId;
            this.cleanupActorName = cleanupActorName == null ? "" : cleanupActorName;
            for (Mutation mutation : mutations) {
                if (this.mutations.size() >= MAX_LEDGER_ENTRIES) {
                    break;
                }
                this.mutations.putIfAbsent(mutation.pos(), mutation);
            }
            if (this.remainingBudget == 0 && this.state == HeartState.ACTIVE) {
                this.state = HeartState.DISABLED;
            }
        }

        private static HeartRecord create(ServerPlayer owner, String dimension, BlockPos anchor,
                                          BlockState originalState) {
            return new HeartRecord(CURRENT_VERSION, owner.getUUID(), owner.getGameProfile().name(),
                    dimension, anchor, HeartState.ACTIVE, MAX_SPREAD_MUTATIONS, 0L,
                    Optional.empty(), "", List.of(new Mutation(anchor, originalState,
                            WitcheryBlocks.BLIGHT_HEART.get().defaultBlockState())));
        }

        public int version() {
            return version;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public String ownerName() {
            return ownerName;
        }

        public String dimension() {
            return dimension;
        }

        public BlockPos anchor() {
            return anchor;
        }

        public HeartState state() {
            return state;
        }

        public int remainingBudget() {
            return remainingBudget;
        }

        public long nextSpreadTick() {
            return nextSpreadTick;
        }

        public Optional<UUID> cleanupActorId() {
            return cleanupActorId;
        }

        public String cleanupActorName() {
            return cleanupActorName;
        }

        public Map<BlockPos, Mutation> mutations() {
            return Map.copyOf(mutations);
        }

        public boolean hasAnchorSnapshot() {
            return mutations.containsKey(anchor);
        }

        private List<Mutation> mutationList() {
            return List.copyOf(mutations.values());
        }

        private void setState(HeartState state) {
            this.state = state;
        }

        private void setNextSpreadTick(long nextSpreadTick) {
            this.nextSpreadTick = nextSpreadTick;
        }

        private void setCleanupActor(UUID actorId, String actorName) {
            this.cleanupActorId = Optional.of(actorId);
            this.cleanupActorName = actorName == null ? "" : actorName;
        }

        private void consumeBudget() {
            remainingBudget = Math.max(0, remainingBudget - 1);
            if (remainingBudget == 0) {
                state = HeartState.DISABLED;
            }
        }

        private void refundBudget() {
            if (state == HeartState.DISABLED && remainingBudget == 0) {
                state = HeartState.ACTIVE;
            }
            remainingBudget = Math.min(MAX_SPREAD_MUTATIONS, remainingBudget + 1);
        }

        private void addMutation(Mutation mutation) {
            mutations.put(mutation.pos(), mutation);
        }

        @Nullable
        private Mutation removeMutation(BlockPos pos) {
            return mutations.remove(pos);
        }

        private BlockPos firstMutationPos() {
            return mutations.keySet().iterator().next();
        }

        private void rotateMutation(BlockPos pos) {
            Mutation mutation = mutations.remove(pos);
            if (mutation != null) {
                mutations.put(pos, mutation);
            }
        }
    }

    public record Mutation(BlockPos pos, BlockState originalState, BlockState appliedState) {
        public static final Codec<Mutation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Mutation::pos),
                BlockState.CODEC.fieldOf("original_state").forGetter(Mutation::originalState),
                BlockState.CODEC.optionalFieldOf("applied_state", Blocks.AIR.defaultBlockState())
                        .forGetter(Mutation::appliedState)
        ).apply(instance, Mutation::new));

        public Mutation {
            pos = pos.immutable();
        }
    }

    private record AnchorKey(String dimension, BlockPos pos) {
        private AnchorKey {
            pos = pos.immutable();
        }
    }
}
