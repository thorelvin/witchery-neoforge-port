package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.entity.CovenMember;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Additive endgame home transposition centred on a permanently bound Kettle.
 *
 * <p>Both same-coordinate volumes are captured before the transaction is journalled. Applying a
 * transaction always writes those original snapshots to their opposite levels, making retries
 * idempotent even after a process crash between individual chunk saves.
 */
public final class OtherwhereHearthTransposition {
    public static final int WIDTH = 25;
    public static final int DEPTH = 25;
    public static final int HEIGHT = 25;
    public static final int HORIZONTAL_RADIUS = 12;
    public static final int BLOCKS_BELOW = 8;
    public static final int BLOCKS_ABOVE = 16;
    public static final int REQUIRED_COVEN_SIZE = 6;
    public static final int BASE_CROSSING_POWER_COST = 10_000;
    public static final int SNAPSHOT_BLOCK_COUNT = WIDTH * HEIGHT * DEPTH;

    private static boolean recoveryRunning;

    private OtherwhereHearthTransposition() {
    }

    public record Volume(BlockPos min, BlockPos max) {
        public int blockCount() {
            return (max.getX() - min.getX() + 1)
                    * (max.getY() - min.getY() + 1)
                    * (max.getZ() - min.getZ() + 1);
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        public AABB entityBounds() {
            return new AABB(min.getX(), min.getY(), min.getZ(),
                    max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D);
        }
    }

    public static Volume volumeFor(BlockPos kettlePos) {
        return new Volume(
                kettlePos.offset(-HORIZONTAL_RADIUS, -BLOCKS_BELOW, -HORIZONTAL_RADIUS),
                kettlePos.offset(HORIZONTAL_RADIUS, BLOCKS_ABOVE, HORIZONTAL_RADIUS));
    }

    public static boolean isSupportedDimension(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD)
                || level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD);
    }

    public static boolean destinationIsSpirit(boolean sourceSpirit) {
        return !sourceSpirit;
    }

    static boolean shouldReplayEntityPosition(boolean preparedPhase, boolean alreadyAtDestination) {
        return preparedPhase || !alreadyAtDestination;
    }

    /**
     * Runs all preflight checks, persists the two-volume journal, then attempts the crossing.
     * Returns {@code true} only when the transaction reached its final durable state this tick.
     */
    public static boolean activate(ServerPlayer player, ServerLevel source, BlockPos kettlePos,
                                   KettleBlockEntity kettle) {
        if (!isSupportedDimension(source)) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.wrong_dimension");
        }
        if (!kettle.isOtherwhereHearthBound() || !kettle.isOtherwhereHearthOwner(player)) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.not_owner");
        }

        MinecraftServer server = source.getServer();
        OtherwhereHearthSavedData data = OtherwhereHearthSavedData.get(server);
        if (data.hasPending()) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.pending");
        }

        ServerLevel overworld = server.overworld();
        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit == null) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.no_spirit_world");
        }
        boolean sourceSpirit = source.dimension().equals(WitcheryDimensions.SPIRIT_WORLD);
        ServerLevel destination = destinationIsSpirit(sourceSpirit) ? spirit : overworld;
        Volume volume = volumeFor(kettlePos);

        if (!hasRequiredCircles(source, kettlePos)) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.circles");
        }
        if (!withinWorld(source, volume) || !withinWorld(destination, volume)) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.out_of_bounds");
        }

        loadChunks(overworld, volume);
        loadChunks(spirit, volume);

        List<ServerPlayer> destinationPlayers = playersIn(destination, volume);
        if (!destinationPlayers.isEmpty()) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.destination_player",
                    destinationPlayers.getFirst().getName());
        }

        List<ServerPlayer> sourcePlayers = playersIn(source, volume);
        for (ServerPlayer sourcePlayer : sourcePlayers) {
            if (WitcheryAttachments.get(sourcePlayer).isInSpiritWorld()
                    && !WitcheryAttachments.get(sourcePlayer).isOtherwhereHearthTraveler()) {
                return fail(player, source, kettlePos, "witchery.otherwhere_hearth.dreaming_player",
                        sourcePlayer.getName());
            }
        }

        Entity unmovableEntity = firstUnmovableEntity(overworld, spirit, volume);
        if (unmovableEntity != null) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.unmovable_entity",
                    unmovableEntity.getDisplayName());
        }

        int covenSize = source.getEntitiesOfClass(
                Entity.class, volume.entityBounds(),
                entity -> entity instanceof CovenMember && volume.entityBounds().contains(entity.position())
        ).size();
        if (covenSize < REQUIRED_COVEN_SIZE) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.coven",
                    REQUIRED_COVEN_SIZE, covenSize);
        }

        AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(source, kettlePos);
        int powerCost = RitualExecutor.scaleRitualPowerCost(BASE_CROSSING_POWER_COST);
        if (altar == null || !altar.hasValidStructure()) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.no_altar");
        }
        if (!volume.contains(altar.getBlockPos())) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.altar_outside");
        }
        if (altar.getEffectiveMaxPower() < powerCost) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.altar_too_weak",
                    powerCost, altar.getEffectiveMaxPower());
        }
        if (altar.getPower() < powerCost) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.no_power",
                    powerCost, altar.getPower());
        }

        if (!preflightBlocks(player, source, destination, volume, kettlePos)) {
            return false;
        }

        CompoundTag overworldSnapshot;
        CompoundTag spiritSnapshot;
        List<OtherwhereHearthSavedData.EntityTransfer> transfers;
        try {
            overworldSnapshot = capture(overworld, volume);
            spiritSnapshot = capture(spirit, volume);
            transfers = captureEntityTransfers(overworld, spirit, volume);
        } catch (RuntimeException exception) {
            WitcheryMod.LOGGER.error("Could not capture Otherwhere Hearth volume at {}", kettlePos, exception);
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.capture_failed");
        }

        OtherwhereHearthSavedData.PendingSwap pending = new OtherwhereHearthSavedData.PendingSwap(
                UUID.randomUUID(),
                player.getUUID(),
                player.getScoreboardName(),
                kettlePos.immutable(),
                volume.min().immutable(),
                altar.getBlockPos().immutable(),
                sourceSpirit,
                powerCost,
                false,
                OtherwhereHearthSavedData.Phase.PREPARED,
                overworldSnapshot,
                spiritSnapshot,
                transfers);

        data.setPending(pending);
        flushJournal(server);

        try {
            boolean complete = completePrepared(server, data);
            if (complete) {
                announceSuccess(server, pending);
            } else {
                player.displayClientMessage(
                        Component.translatable("witchery.otherwhere_hearth.waiting_for_players"), false);
            }
            return complete;
        } catch (RuntimeException exception) {
            WitcheryMod.LOGGER.error(
                    "Otherwhere Hearth transaction {} was interrupted; the persisted journal will retry",
                    pending.transactionId(), exception);
            player.displayClientMessage(
                    Component.translatable("witchery.otherwhere_hearth.recovery_queued"), false);
            return false;
        }
    }

    /** Called from the Overworld level tick to finish an interrupted journal. */
    public static void recover(MinecraftServer server) {
        if (recoveryRunning) {
            return;
        }
        OtherwhereHearthSavedData data = OtherwhereHearthSavedData.get(server);
        OtherwhereHearthSavedData.PendingSwap pending = data.pending();
        if (pending == null) {
            return;
        }

        recoveryRunning = true;
        try {
            boolean complete = pending.phase() == OtherwhereHearthSavedData.Phase.PREPARED
                    ? completePrepared(server, data)
                    : completeWaitingForPlayers(server, data);
            if (complete) {
                announceSuccess(server, pending);
            }
        } catch (RuntimeException exception) {
            WitcheryMod.LOGGER.error(
                    "Otherwhere Hearth recovery for transaction {} will retry",
                    pending.transactionId(), exception);
        } finally {
            recoveryRunning = false;
        }
    }

    private static boolean completePrepared(MinecraftServer server, OtherwhereHearthSavedData data) {
        OtherwhereHearthSavedData.PendingSwap pending = data.pending();
        if (pending == null) {
            return true;
        }

        ServerLevel overworld = server.overworld();
        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit == null) {
            throw new IllegalStateException("Spirit World is unavailable");
        }
        Volume volume = volumeFor(pending.kettlePos());
        loadChunks(overworld, volume);
        loadChunks(spirit, volume);

        // Write the original Spirit volume into the Overworld and vice versa. Repeating these exact
        // writes after a crash is idempotent; no state is recaptured from a half-finished world.
        placeSnapshot(overworld, pending.origin(), pending.spiritSnapshot());
        placeSnapshot(spirit, pending.origin(), pending.overworldSnapshot());

        ServerLevel destination = pending.sourceSpirit() ? overworld : spirit;
        if (!(destination.getBlockEntity(pending.kettlePos()) instanceof KettleBlockEntity movedKettle)
                || !movedKettle.isOtherwhereHearthBound()
                || !movedKettle.getOtherwhereHearthOwner().filter(pending.ownerId()::equals).isPresent()) {
            throw new IllegalStateException("Bound Kettle did not survive snapshot placement");
        }

        boolean allPlayersResolved = moveEntities(server, pending, true);
        // A PREPARED replay always reapplies the original snapshot, including the altar's original
        // power. Charge after every such replay so both the first attempt and any crash recovery
        // converge on the same final power instead of accidentally refunding it.
        if (!(destination.getBlockEntity(pending.altarPos()) instanceof AltarBlockEntity movedAltar)
                || !movedAltar.consumePower(pending.powerCost())) {
            throw new IllegalStateException("Travelling altar could not pay the committed crossing");
        }
        OtherwhereHearthSavedData.PendingSwap durablePending = pending.withPowerPaid();
        data.setPending(durablePending);

        // Flush blocks, block entities, entities, players, and the still-present journal before
        // clearing it. A crash anywhere before this save leaves PREPARED on disk and is retryable.
        server.saveEverything(true, true, false);

        if (!allPlayersResolved) {
            data.setPending(durablePending.waitingForPlayers());
            flushJournal(server);
            return false;
        }

        data.clearPending();
        flushJournal(server);
        return true;
    }

    private static boolean completeWaitingForPlayers(MinecraftServer server,
                                                     OtherwhereHearthSavedData data) {
        OtherwhereHearthSavedData.PendingSwap pending = data.pending();
        if (pending == null) {
            return true;
        }
        if (!moveEntities(server, pending, false)) {
            return false;
        }
        server.saveEverything(true, true, false);
        data.clearPending();
        flushJournal(server);
        return true;
    }

    private static boolean hasRequiredCircles(ServerLevel level, BlockPos kettlePos) {
        BlockPos floorCenter = kettlePos.below();
        return RitualCircleDetector.detectVariant(
                level, floorCenter, RitualCircleDetector.CircleSize.LARGE) == 2
                && RitualCircleDetector.detectVariant(
                level, floorCenter, RitualCircleDetector.CircleSize.MEDIUM) == 1;
    }

    private static boolean withinWorld(ServerLevel level, Volume volume) {
        if (!level.isInWorldBounds(volume.min()) || !level.isInWorldBounds(volume.max())) {
            return false;
        }
        int y = volume.min().getY();
        return level.getWorldBorder().isWithinBounds(new BlockPos(volume.min().getX(), y, volume.min().getZ()))
                && level.getWorldBorder().isWithinBounds(new BlockPos(volume.min().getX(), y, volume.max().getZ()))
                && level.getWorldBorder().isWithinBounds(new BlockPos(volume.max().getX(), y, volume.min().getZ()))
                && level.getWorldBorder().isWithinBounds(new BlockPos(volume.max().getX(), y, volume.max().getZ()));
    }

    private static List<ServerPlayer> playersIn(ServerLevel level, Volume volume) {
        AABB bounds = volume.entityBounds();
        return level.players().stream()
                .filter(player -> bounds.intersects(player.getBoundingBox())
                        || bounds.contains(player.position()))
                .toList();
    }

    private static boolean preflightBlocks(ServerPlayer player, ServerLevel source,
                                           ServerLevel destination, Volume volume,
                                           BlockPos activeKettlePos) {
        for (BlockPos cursor : BlockPos.betweenClosed(volume.min(), volume.max())) {
            BlockPos pos = cursor.immutable();
            BlockState sourceState = source.getBlockState(pos);
            BlockState destinationState = destination.getBlockState(pos);

            if (isForbidden(source, pos, sourceState) || isForbidden(destination, pos, destinationState)) {
                return fail(player, source, activeKettlePos,
                        "witchery.otherwhere_hearth.forbidden_block",
                        pos.getX(), pos.getY(), pos.getZ());
            }

            if (isForeignBoundKettle(source, pos, activeKettlePos)
                    || isForeignBoundKettle(destination, pos, null)) {
                return fail(player, source, activeKettlePos,
                        "witchery.otherwhere_hearth.other_bound_kettle",
                        pos.getX(), pos.getY(), pos.getZ());
            }

            if (sourceState.isAir() && destinationState.isAir()
                    && source.getBlockEntity(pos) == null && destination.getBlockEntity(pos) == null) {
                continue;
            }
            if (!WitcheryProtectionService.canModifyRemoteBlock(
                    player, source, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                return fail(player, source, activeKettlePos,
                        "witchery.otherwhere_hearth.protected_source",
                        pos.getX(), pos.getY(), pos.getZ());
            }
            if (!WitcheryProtectionService.canModifyRemoteBlock(
                    player, destination, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                return fail(player, source, activeKettlePos,
                        "witchery.otherwhere_hearth.protected_destination",
                        pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    private static boolean isForbidden(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getDestroySpeed(level, pos) < 0.0F;
    }

    private static boolean isForeignBoundKettle(ServerLevel level, BlockPos pos,
                                                @Nullable BlockPos permittedKettlePos) {
        return level.getBlockEntity(pos) instanceof KettleBlockEntity kettle
                && kettle.isOtherwhereHearthBound()
                && (permittedKettlePos == null || !pos.equals(permittedKettlePos));
    }

    private static CompoundTag capture(ServerLevel level, Volume volume) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, volume.min(), new Vec3i(WIDTH, HEIGHT, DEPTH), false, null);
        CompoundTag snapshot = template.save(new CompoundTag());
        if (snapshot.getListOrEmpty("blocks").size() != SNAPSHOT_BLOCK_COUNT) {
            throw new IllegalStateException("Incomplete structure snapshot");
        }
        return snapshot;
    }

    private static void placeSnapshot(ServerLevel level, BlockPos origin, CompoundTag snapshot) {
        StructureTemplate template = new StructureTemplate();
        template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), snapshot.copy());
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        if (!template.placeInWorld(level, origin, origin, settings,
                RandomSource.create(origin.asLong()), flags)) {
            throw new IllegalStateException("Structure snapshot placement failed in "
                    + level.dimension().identifier());
        }
        restoreBlockEntityData(level, origin, snapshot);
    }

    private static void restoreBlockEntityData(ServerLevel level, BlockPos origin,
                                               CompoundTag snapshot) {
        ListTag blocks = snapshot.getListOrEmpty("blocks");
        for (int index = 0; index < blocks.size(); index++) {
            CompoundTag entry = blocks.getCompoundOrEmpty(index);
            if (!entry.contains("nbt")) {
                continue;
            }
            ListTag relative = entry.getListOrEmpty("pos");
            if (relative.size() != 3) {
                throw new IllegalStateException("Malformed block entity position in hearth snapshot");
            }
            BlockPos target = origin.offset(
                    relative.getIntOr(0, 0),
                    relative.getIntOr(1, 0),
                    relative.getIntOr(2, 0));
            BlockEntity blockEntity = level.getBlockEntity(target);
            if (blockEntity == null) {
                throw new IllegalStateException("Missing block entity after hearth placement at " + target);
            }
            blockEntity.loadWithComponents(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    entry.getCompoundOrEmpty("nbt").copy()));
            blockEntity.setChanged();
            level.sendBlockUpdated(target, level.getBlockState(target), level.getBlockState(target),
                    Block.UPDATE_CLIENTS);
        }
    }

    private static List<OtherwhereHearthSavedData.EntityTransfer> captureEntityTransfers(
            ServerLevel overworld, ServerLevel spirit, Volume volume) {
        List<OtherwhereHearthSavedData.EntityTransfer> result = new ArrayList<>();
        appendEntityTransfers(result, entitiesIn(overworld, volume), true);
        appendEntityTransfers(result, entitiesIn(spirit, volume), false);
        return List.copyOf(result);
    }

    private static List<Entity> entitiesIn(ServerLevel level, Volume volume) {
        AABB bounds = volume.entityBounds();
        return level.getEntitiesOfClass(Entity.class, bounds,
                entity -> !entity.isRemoved()
                        && (bounds.intersects(entity.getBoundingBox())
                        || bounds.contains(entity.position())));
    }

    @Nullable
    private static Entity firstUnmovableEntity(ServerLevel overworld, ServerLevel spirit,
                                               Volume volume) {
        for (Entity entity : entitiesIn(overworld, volume)) {
            if (!canJournalAndTeleport(entity, overworld, spirit)) {
                return entity;
            }
        }
        for (Entity entity : entitiesIn(spirit, volume)) {
            if (!canJournalAndTeleport(entity, spirit, overworld)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean canJournalAndTeleport(Entity entity, ServerLevel source,
                                                 ServerLevel destination) {
        return entity.canTeleport(source, destination)
                && (entity instanceof ServerPlayer || entity.shouldBeSaved());
    }

    private static void appendEntityTransfers(
            List<OtherwhereHearthSavedData.EntityTransfer> output,
            List<Entity> entities,
            boolean toSpirit) {
        Set<UUID> selected = new HashSet<>();
        for (Entity entity : entities) {
            selected.add(entity.getUUID());
        }
        for (Entity entity : entities) {
            Entity vehicle = entity.getVehicle();
            Optional<UUID> vehicleId = vehicle != null && selected.contains(vehicle.getUUID())
                    ? Optional.of(vehicle.getUUID())
                    : Optional.empty();
            output.add(new OtherwhereHearthSavedData.EntityTransfer(
                    entity.getUUID(),
                    entity instanceof ServerPlayer,
                    toSpirit,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot(),
                    vehicleId,
                    entity instanceof ServerPlayer ? new CompoundTag() : captureEntity(entity)));
        }
    }

    private static CompoundTag captureEntity(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, entity.level().registryAccess());
        if (!entity.saveAsPassenger(output)) {
            throw new IllegalStateException("Entity cannot be journalled: " + entity.getStringUUID());
        }
        CompoundTag snapshot = output.buildResult();
        // Every selected passenger has its own manifest entry and vehicle UUID. Keeping nested
        // passenger NBT here would recreate the same entity twice if the vehicle needed recovery.
        snapshot.remove("Passengers");
        return snapshot;
    }

    private static boolean moveEntities(MinecraftServer server,
                                        OtherwhereHearthSavedData.PendingSwap pending,
                                        boolean replayPreparedPositions) {
        ServerLevel overworld = server.overworld();
        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit == null) {
            throw new IllegalStateException("Spirit World is unavailable");
        }

        Map<UUID, Entity> current = new HashMap<>();
        for (OtherwhereHearthSavedData.EntityTransfer transfer : pending.entities()) {
            Entity entity = findEntity(server, transfer);
            if (entity == null && replayPreparedPositions && !transfer.player()) {
                ServerLevel destination = transfer.toSpirit() ? spirit : overworld;
                entity = restoreMissingEntity(destination, transfer);
            }
            if (entity != null) {
                current.put(transfer.entityId(), entity);
                ServerLevel destination = transfer.toSpirit() ? spirit : overworld;
                if (shouldReplayEntityPosition(
                        replayPreparedPositions, entity.level() == destination)) {
                    entity.stopRiding();
                    entity.ejectPassengers();
                }
            }
        }

        boolean allPlayersResolved = true;
        Map<UUID, Entity> moved = new HashMap<>();
        Set<UUID> transferredThisPass = new HashSet<>();
        List<OtherwhereHearthSavedData.EntityTransfer> ordered =
                new ArrayList<>(pending.entities());
        ordered.sort(Comparator.comparing(transfer -> transfer.vehicleId().isPresent()));

        for (OtherwhereHearthSavedData.EntityTransfer transfer : ordered) {
            ServerLevel destination = transfer.toSpirit() ? spirit : overworld;
            Entity entity = current.get(transfer.entityId());
            if (entity == null) {
                if (transfer.player()) {
                    allPlayersResolved = false;
                }
                continue;
            }

            if (!shouldReplayEntityPosition(
                    replayPreparedPositions, entity.level() == destination)) {
                moved.put(transfer.entityId(), entity);
                updateHearthTravelerState(entity, transfer.toSpirit());
                continue;
            }

            if (!teleportEntity(entity, destination, transfer)) {
                throw new IllegalStateException("Could not move entity " + transfer.entityId());
            }
            Entity resolved = transfer.player()
                    ? server.getPlayerList().getPlayer(transfer.entityId())
                    : destination.getEntity(transfer.entityId());
            if (resolved == null) {
                if (transfer.player()) {
                    allPlayersResolved = false;
                    continue;
                }
                throw new IllegalStateException("Moved entity was not present in destination: "
                        + transfer.entityId());
            }
            discardDuplicateNonPlayers(server, destination, resolved);
            moved.put(transfer.entityId(), resolved);
            transferredThisPass.add(transfer.entityId());
            updateHearthTravelerState(resolved, transfer.toSpirit());
        }

        for (OtherwhereHearthSavedData.EntityTransfer transfer : pending.entities()) {
            if (transfer.vehicleId().isEmpty()) {
                continue;
            }
            Entity passenger = moved.get(transfer.entityId());
            Entity vehicle = moved.get(transfer.vehicleId().get());
            if (passenger != null && vehicle != null && passenger.level() == vehicle.level()
                    && (replayPreparedPositions
                    || transferredThisPass.contains(transfer.entityId())
                    || transferredThisPass.contains(transfer.vehicleId().get()))) {
                passenger.startRiding(vehicle, true, true);
            }
        }
        return allPlayersResolved;
    }

    private static Entity restoreMissingEntity(
            ServerLevel destination,
            OtherwhereHearthSavedData.EntityTransfer transfer) {
        if (transfer.entitySnapshot().isEmpty()) {
            throw new IllegalStateException(
                    "Missing entity has no recovery snapshot: " + transfer.entityId());
        }
        Entity restored = EntityType.loadEntityRecursive(
                transfer.entitySnapshot().copy(),
                destination,
                EntitySpawnReason.DIMENSION_TRAVEL,
                EntityProcessor.NOP);
        if (restored == null) {
            throw new IllegalStateException(
                    "Could not recreate missing entity " + transfer.entityId());
        }
        restored.setUUID(transfer.entityId());
        restored.setPos(transfer.x(), transfer.y(), transfer.z());
        restored.setYRot(transfer.yaw());
        restored.setXRot(transfer.pitch());
        restored.stopRiding();
        restored.ejectPassengers();
        if (!destination.addWithUUID(restored)) {
            throw new IllegalStateException(
                    "Could not add recreated entity " + transfer.entityId());
        }
        return restored;
    }

    private static void updateHearthTravelerState(Entity entity, boolean inSpiritWorld) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        var data = WitcheryAttachments.get(player);
        if (data.isOtherwhereHearthTraveler() == inSpiritWorld) {
            return;
        }
        data.setOtherwhereHearthTraveler(inSpiritWorld);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
    }

    @Nullable
    private static Entity findEntity(MinecraftServer server,
                                     OtherwhereHearthSavedData.EntityTransfer transfer) {
        if (transfer.player()) {
            return server.getPlayerList().getPlayer(transfer.entityId());
        }
        ServerLevel destination = transfer.toSpirit()
                ? server.getLevel(WitcheryDimensions.SPIRIT_WORLD)
                : server.overworld();
        if (destination != null) {
            Entity alreadyMoved = destination.getEntity(transfer.entityId());
            if (alreadyMoved != null) {
                return alreadyMoved;
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(transfer.entityId());
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static boolean teleportEntity(Entity entity, ServerLevel destination,
                                          OtherwhereHearthSavedData.EntityTransfer transfer) {
        if (entity.level() == destination) {
            entity.teleportTo(transfer.x(), transfer.y(), transfer.z());
            entity.setYRot(transfer.yaw());
            entity.setXRot(transfer.pitch());
            return true;
        }
        return entity.teleportTo(destination,
                transfer.x(), transfer.y(), transfer.z(),
                Set.<Relative>of(), transfer.yaw(), transfer.pitch(), false);
    }

    private static void discardDuplicateNonPlayers(MinecraftServer server, ServerLevel destination,
                                                   Entity canonical) {
        if (canonical instanceof ServerPlayer) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level == destination) {
                continue;
            }
            Entity duplicate = level.getEntity(canonical.getUUID());
            if (duplicate != null) {
                duplicate.discard();
            }
        }
    }

    private static void loadChunks(ServerLevel level, Volume volume) {
        int minChunkX = volume.min().getX() >> 4;
        int maxChunkX = volume.max().getX() >> 4;
        int minChunkZ = volume.min().getZ() >> 4;
        int maxChunkZ = volume.max().getZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            }
        }
    }

    private static void flushJournal(MinecraftServer server) {
        server.overworld().getDataStorage().saveAndJoin();
    }

    private static void announceSuccess(MinecraftServer server,
                                        OtherwhereHearthSavedData.PendingSwap pending) {
        ServerLevel destination = pending.sourceSpirit()
                ? server.overworld()
                : server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (destination == null) {
            return;
        }
        destination.playSound(null, pending.kettlePos(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.5F, 0.7F);
        ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerId());
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(
                    pending.sourceSpirit()
                            ? "witchery.otherwhere_hearth.returned"
                            : "witchery.otherwhere_hearth.crossed"), false);
        }
        WitcheryMod.LOGGER.info("Otherwhere Hearth transaction {} completed at {}",
                pending.transactionId(), pending.kettlePos());
    }

    private static boolean fail(ServerPlayer player, ServerLevel level, BlockPos pos,
                                String key, Object... arguments) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.BLOCKS, 0.6F, 0.6F);
        player.displayClientMessage(Component.translatable(key, arguments), false);
        return false;
    }
}
