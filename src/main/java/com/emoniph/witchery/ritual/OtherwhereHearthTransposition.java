package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.entity.CovenContributionCounter;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    private static final TicketType HEARTH_LOAD_TICKET = new TicketType(
            2L,
            TicketType.FLAG_LOADING
                    | TicketType.FLAG_SIMULATION
                    | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
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

    public record PendingEntityCounts(int toSpirit, int toOverworld, int players) {
    }

    /** Read-only state returned by the admin verifier. Running an inspection never spends power. */
    public record Inspection(
            Volume volume,
            String sourceDimension,
            String destinationDimension,
            String journal,
            boolean kettlePresent,
            boolean kettleBound,
            String kettleOwner,
            boolean ownerMatches,
            int mediumRitualVariant,
            int largeOtherwhereVariant,
            int covenSize,
            @Nullable BlockPos altarPos,
            boolean altarValid,
            int altarPower,
            int altarCapacity,
            int powerCost,
            List<String> blockers
    ) {
        public Inspection {
            blockers = List.copyOf(blockers);
        }

        public boolean ready() {
            return blockers.isEmpty();
        }
    }

    /** Stable categories shared by the player-facing Hearth preflight and its tests. */
    public enum PreflightIssueKind {
        WRONG_DIMENSION,
        NOT_BOUND,
        NOT_OWNER,
        PENDING_TRANSACTION,
        LOCAL_CHUNKS_UNLOADED,
        REQUIRED_CIRCLES,
        SOURCE_OUT_OF_BOUNDS,
        DREAMING_PLAYER,
        UNMOVABLE_ENTITY,
        COVEN,
        NO_ALTAR,
        ALTAR_OUTSIDE,
        ALTAR_TOO_WEAK,
        NO_POWER,
        FORBIDDEN_BLOCK,
        OTHER_BOUND_KETTLE,
        PROTECTED_SOURCE,
        SPIRIT_WORLD_UNAVAILABLE,
        DESTINATION_OUT_OF_BOUNDS,
        DESTINATION_PLAYER,
        PROTECTED_DESTINATION
    }

    public enum DestinationStatus {
        VEILED,
        READY,
        BLOCKED,
        UNAVAILABLE
    }

    public record PreflightIssue(PreflightIssueKind kind, Component message) {
    }

    /**
     * Owner-safe Seer Stone result. It never names the owner and never reads an unloaded
     * destination chunk.
     */
    public record PlayerInspection(
            Volume volume,
            boolean authorized,
            List<PreflightIssue> localBlockers,
            DestinationStatus destinationStatus,
            List<PreflightIssue> destinationBlockers
    ) {
        public PlayerInspection {
            localBlockers = List.copyOf(localBlockers);
            destinationBlockers = List.copyOf(destinationBlockers);
        }

        public boolean localReady() {
            return authorized && localBlockers.isEmpty();
        }
    }

    private enum BlockProblemKind {
        FORBIDDEN,
        OTHER_BOUND_KETTLE,
        PROTECTED_SOURCE,
        PROTECTED_DESTINATION
    }

    private record BlockProblem(BlockProblemKind kind, BlockPos pos) {
        String describe() {
            return switch (kind) {
                case FORBIDDEN -> "Unbreakable block at " + coordinates(pos) + ".";
                case OTHER_BOUND_KETTLE -> "Another bound Otherwhere Hearth is at "
                        + coordinates(pos) + ".";
                case PROTECTED_SOURCE -> "Source protection blocks changes at "
                        + coordinates(pos) + ".";
                case PROTECTED_DESTINATION -> "Destination protection blocks changes at "
                        + coordinates(pos) + ".";
            };
        }
    }

    public static Volume volumeFor(BlockPos kettlePos) {
        return new Volume(
                kettlePos.offset(-HORIZONTAL_RADIUS, -BLOCKS_BELOW, -HORIZONTAL_RADIUS),
                kettlePos.offset(HORIZONTAL_RADIUS, BLOCKS_ABOVE, HORIZONTAL_RADIUS));
    }

    /** Eight evenly spaced samples per edge, deduplicated at corners. */
    public static List<Vec3> boundaryPoints(Volume volume) {
        double minX = volume.min().getX();
        double minY = volume.min().getY();
        double minZ = volume.min().getZ();
        double maxX = volume.max().getX() + 1.0D;
        double maxY = volume.max().getY() + 1.0D;
        double maxZ = volume.max().getZ() + 1.0D;
        LinkedHashSet<Vec3> points = new LinkedHashSet<>();

        for (double x : edgeSamples(minX, maxX)) {
            points.add(new Vec3(x, minY, minZ));
            points.add(new Vec3(x, minY, maxZ));
            points.add(new Vec3(x, maxY, minZ));
            points.add(new Vec3(x, maxY, maxZ));
        }
        for (double z : edgeSamples(minZ, maxZ)) {
            points.add(new Vec3(minX, minY, z));
            points.add(new Vec3(minX, maxY, z));
            points.add(new Vec3(maxX, minY, z));
            points.add(new Vec3(maxX, maxY, z));
        }
        for (double y : edgeSamples(minY, maxY)) {
            points.add(new Vec3(minX, y, minZ));
            points.add(new Vec3(minX, y, maxZ));
            points.add(new Vec3(maxX, y, minZ));
            points.add(new Vec3(maxX, y, maxZ));
        }
        return List.copyOf(points);
    }

    private static List<Double> edgeSamples(double min, double max) {
        List<Double> samples = new ArrayList<>();
        for (double value = min; value < max; value += 4.0D) {
            samples.add(value);
        }
        samples.add(max);
        return List.copyOf(samples);
    }

    public static boolean isSupportedDimension(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD)
                || level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD);
    }

    public static boolean destinationIsSpirit(boolean sourceSpirit) {
        return !sourceSpirit;
    }

    public static boolean hasPendingTransaction(MinecraftServer server) {
        return OtherwhereHearthSavedData.get(server).hasPending();
    }

    public static PendingEntityCounts pendingEntityCounts(MinecraftServer server) {
        OtherwhereHearthSavedData.PendingSwap pending =
                OtherwhereHearthSavedData.get(server).pending();
        if (pending == null) {
            return new PendingEntityCounts(0, 0, 0);
        }
        int toSpirit = 0;
        int toOverworld = 0;
        int players = 0;
        for (OtherwhereHearthSavedData.EntityTransfer transfer : pending.entities()) {
            if (transfer.toSpirit()) {
                toSpirit++;
            } else {
                toOverworld++;
            }
            if (transfer.player()) {
                players++;
            }
        }
        return new PendingEntityCounts(toSpirit, toOverworld, players);
    }

    public static void loadVolumeForInspection(ServerLevel level, BlockPos kettlePos) {
        loadChunks(level, volumeFor(kettlePos));
    }

    public static boolean volumeFits(ServerLevel level, Volume volume) {
        return withinWorld(level, volume);
    }

    /**
     * Performs the same world-facing readiness checks used by activation and returns every cheap
     * blocker plus the first expensive block/protection blocker. The paired chunks are loaded so
     * this command is intended for deliberate QA use, not a repeating tick path.
     */
    public static Inspection inspect(ServerPlayer player, ServerLevel source, BlockPos kettlePos) {
        MinecraftServer server = source.getServer();
        Volume volume = volumeFor(kettlePos);
        List<String> blockers = new ArrayList<>();
        OtherwhereHearthSavedData.PendingSwap pending = OtherwhereHearthSavedData.get(server).pending();
        String journal = pending == null
                ? "none"
                : pending.phase().name().toLowerCase(java.util.Locale.ROOT)
                        + " transaction=" + pending.transactionId()
                        + " owner=" + pending.ownerName()
                        + " powerPaid=" + pending.powerPaid()
                        + " entities=" + pending.entities().size();

        boolean supported = isSupportedDimension(source);
        if (!supported) {
            blockers.add("Source dimension is not the Overworld or Spirit World.");
        }

        boolean sourceSpirit = source.dimension().equals(WitcheryDimensions.SPIRIT_WORLD);
        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        ServerLevel destination = sourceSpirit ? server.overworld() : spirit;
        if (destination == null) {
            blockers.add("Spirit World destination is unavailable.");
        }
        if (pending != null) {
            blockers.add("A persisted Otherwhere Hearth transaction is still pending.");
        }

        KettleBlockEntity kettle = source.getBlockEntity(kettlePos) instanceof KettleBlockEntity found
                ? found : null;
        boolean kettlePresent = kettle != null;
        boolean kettleBound = kettlePresent && kettle.isOtherwhereHearthBound();
        String kettleOwner = kettlePresent ? kettle.getOtherwhereHearthOwnerName() : "";
        boolean ownerMatches = kettlePresent && kettle.isOtherwhereHearthOwner(player);
        if (!kettlePresent) {
            blockers.add("No Kettle block entity is present at the inspected center.");
        } else if (!kettleBound) {
            blockers.add("The Kettle is not permanently bound as an Otherwhere Hearth.");
        } else if (!ownerMatches) {
            blockers.add("The inspecting player does not own this bound hearth.");
        }

        BlockPos floorCenter = kettlePos.below();
        int mediumVariant = RitualCircleDetector.detectVariant(
                source, floorCenter, RitualCircleDetector.CircleSize.MEDIUM);
        int largeVariant = RitualCircleDetector.detectVariant(
                source, floorCenter, RitualCircleDetector.CircleSize.LARGE);
        if (mediumVariant != 1) {
            blockers.add("Medium ring must be Ritual chalk (variant 1), found "
                    + mediumVariant + ".");
        }
        if (largeVariant != 2) {
            blockers.add("Large ring must be Otherwhere chalk (variant 2), found "
                    + largeVariant + ".");
        }

        boolean sourceFits = withinWorld(source, volume);
        boolean destinationFits = destination != null && withinWorld(destination, volume);
        if (!sourceFits) {
            blockers.add("The 25x25x25 source volume crosses build height or the world border.");
        }
        if (destination != null && !destinationFits) {
            blockers.add("The 25x25x25 destination volume crosses build height or the world border.");
        }

        if (destination != null && sourceFits && destinationFits) {
            // Activation loads both footprints before entity and altar discovery. Match that order
            // so an unloaded edge chunk cannot produce a false coven or altar blocker here.
            loadChunks(source, volume);
            loadChunks(destination, volume);
        }

        int covenSize = CovenContributionCounter.countLivingSworn(
                source, volume.entityBounds(), player.getUUID());
        if (covenSize < REQUIRED_COVEN_SIZE) {
            blockers.add("Sworn living coven missing: " + covenSize + "/"
                    + REQUIRED_COVEN_SIZE + ".");
        }

        AltarBlockEntity altar = findHearthAltar(source, kettlePos, volume);
        int powerCost = RitualExecutor.scaleRitualPowerCost(BASE_CROSSING_POWER_COST);
        BlockPos altarPos = altar == null ? null : altar.getBlockPos();
        boolean altarValid = altar != null && altar.hasValidStructure();
        int altarPower = altar == null ? 0 : altar.getPower();
        int altarCapacity = altar == null ? 0 : altar.getEffectiveMaxPower();
        if (altar == null || !altarValid) {
            blockers.add("No complete altar structure is in range.");
        } else {
            if (!volume.contains(altarPos)) {
                blockers.add("The altar at " + coordinates(altarPos) + " lies outside the moving volume.");
            }
            if (altarCapacity < powerCost) {
                blockers.add("Altar capacity is " + altarCapacity + "/" + powerCost + ".");
            }
            if (altarPower < powerCost) {
                blockers.add("Altar charge is " + altarPower + "/" + powerCost + ".");
            }
        }

        if (destination != null && sourceFits && destinationFits) {
            List<ServerPlayer> destinationPlayers = playersIn(destination, volume);
            if (!destinationPlayers.isEmpty()) {
                blockers.add("Destination contains player "
                        + destinationPlayers.getFirst().getScoreboardName() + ".");
            }
            for (ServerPlayer sourcePlayer : playersIn(source, volume)) {
                if (WitcheryAttachments.get(sourcePlayer).isInSpiritWorld()
                        && !WitcheryAttachments.get(sourcePlayer).isOtherwhereHearthTraveler()) {
                    blockers.add("Source contains sleeping dream-traveller "
                            + sourcePlayer.getScoreboardName() + ".");
                }
            }

            ServerLevel overworld = server.overworld();
            if (spirit != null) {
                Entity unmovable = firstUnmovableEntity(overworld, spirit, volume);
                if (unmovable != null) {
                    blockers.add("Entity cannot be journalled and teleported: "
                            + unmovable.getDisplayName().getString() + ".");
                }
            }

            BlockProblem problem = findBlockProblem(player, source, destination, volume, kettlePos);
            if (problem != null) {
                blockers.add(problem.describe());
            }
        }

        return new Inspection(
                volume,
                source.dimension().identifier().toString(),
                destination == null ? "unavailable" : destination.dimension().identifier().toString(),
                journal,
                kettlePresent,
                kettleBound,
                kettleOwner,
                ownerMatches,
                mediumVariant,
                largeVariant,
                covenSize,
                altarPos,
                altarValid,
                altarPower,
                altarCapacity,
                powerCost,
                blockers);
    }

    /**
     * Performs the owner-facing Seer Stone preflight without adding tickets or synchronously
     * requesting chunks. Destination details remain hidden until local preparation is complete,
     * and are read only when the whole paired footprint is already loaded.
     */
    public static PlayerInspection inspectForPlayer(
            ServerPlayer player, ServerLevel source, BlockPos kettlePos) {
        Volume volume = volumeFor(kettlePos);
        if (!isSupportedDimension(source)) {
            return deniedInspection(volume, PreflightIssueKind.WRONG_DIMENSION,
                    "witchery.otherwhere_hearth.wrong_dimension");
        }

        KettleBlockEntity kettle = source.getBlockEntity(kettlePos) instanceof KettleBlockEntity found
                ? found : null;
        if (kettle == null || !kettle.isOtherwhereHearthBound()) {
            return deniedInspection(volume, PreflightIssueKind.NOT_BOUND,
                    "item.witchery.seer_stone.hearth_not_bound");
        }
        if (!kettle.isOtherwhereHearthOwner(player)) {
            return deniedInspection(volume, PreflightIssueKind.NOT_OWNER,
                    "witchery.otherwhere_hearth.not_owner");
        }

        MinecraftServer server = source.getServer();
        List<PreflightIssue> local = new ArrayList<>();
        if (OtherwhereHearthSavedData.get(server).hasPending()) {
            local.add(issue(PreflightIssueKind.PENDING_TRANSACTION,
                    "witchery.otherwhere_hearth.pending"));
        }

        ServerLevel spirit = server.getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit == null) {
            return new PlayerInspection(volume, true, local, DestinationStatus.UNAVAILABLE,
                    List.of(issue(PreflightIssueKind.SPIRIT_WORLD_UNAVAILABLE,
                            "witchery.otherwhere_hearth.no_spirit_world")));
        }
        ServerLevel destination = source.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                ? server.overworld() : spirit;

        if (!withinWorld(source, volume)) {
            local.add(issue(PreflightIssueKind.SOURCE_OUT_OF_BOUNDS,
                    "witchery.otherwhere_hearth.out_of_bounds"));
        } else if (!allVolumeChunksLoaded(source, volume)) {
            local.add(issue(PreflightIssueKind.LOCAL_CHUNKS_UNLOADED,
                    "item.witchery.seer_stone.hearth_near_veiled"));
        } else {
            inspectLoadedLocalPreparation(player, source, destination, kettlePos, volume, local);
        }

        if (!local.isEmpty()) {
            return new PlayerInspection(volume, true, local, DestinationStatus.VEILED, List.of());
        }

        List<PreflightIssue> destinationBlockers = new ArrayList<>();
        if (!withinWorld(destination, volume)) {
            destinationBlockers.add(issue(PreflightIssueKind.DESTINATION_OUT_OF_BOUNDS,
                    "witchery.otherwhere_hearth.out_of_bounds"));
            return new PlayerInspection(volume, true, local, DestinationStatus.BLOCKED,
                    destinationBlockers);
        }
        if (!allVolumeChunksLoaded(destination, volume)) {
            return new PlayerInspection(volume, true, local, DestinationStatus.VEILED, List.of());
        }

        List<ServerPlayer> destinationPlayers = playersIn(destination, volume);
        if (!destinationPlayers.isEmpty()) {
            destinationBlockers.add(issue(PreflightIssueKind.DESTINATION_PLAYER,
                    "witchery.otherwhere_hearth.destination_player",
                    destinationPlayers.getFirst().getName()));
        }
        Entity unmovable = firstUnmovableEntityOnSide(destination, source, volume);
        if (unmovable != null) {
            destinationBlockers.add(issue(PreflightIssueKind.UNMOVABLE_ENTITY,
                    "witchery.otherwhere_hearth.unmovable_entity", unmovable.getDisplayName()));
        }
        BlockProblem destinationProblem = findSideBlockProblem(
                player, destination, volume, null, false);
        if (destinationProblem != null) {
            destinationBlockers.add(issueForBlockProblem(destinationProblem));
        }
        return new PlayerInspection(volume, true, local,
                destinationBlockers.isEmpty() ? DestinationStatus.READY : DestinationStatus.BLOCKED,
                destinationBlockers);
    }

    public static boolean allVolumeChunksLoaded(ServerLevel level, Volume volume) {
        int minChunkX = volume.min().getX() >> 4;
        int maxChunkX = volume.max().getX() >> 4;
        int minChunkZ = volume.min().getZ() >> 4;
        int maxChunkZ = volume.max().getZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void inspectLoadedLocalPreparation(
            ServerPlayer player,
            ServerLevel source,
            ServerLevel destination,
            BlockPos kettlePos,
            Volume volume,
            List<PreflightIssue> blockers) {
        if (!hasRequiredCircles(source, kettlePos)) {
            blockers.add(issue(PreflightIssueKind.REQUIRED_CIRCLES,
                    "witchery.otherwhere_hearth.circles"));
        }
        for (ServerPlayer sourcePlayer : playersIn(source, volume)) {
            if (WitcheryAttachments.get(sourcePlayer).isInSpiritWorld()
                    && !WitcheryAttachments.get(sourcePlayer).isOtherwhereHearthTraveler()) {
                blockers.add(issue(PreflightIssueKind.DREAMING_PLAYER,
                        "witchery.otherwhere_hearth.dreaming_player", sourcePlayer.getName()));
                break;
            }
        }
        Entity unmovable = firstUnmovableEntityOnSide(source, destination, volume);
        if (unmovable != null) {
            blockers.add(issue(PreflightIssueKind.UNMOVABLE_ENTITY,
                    "witchery.otherwhere_hearth.unmovable_entity", unmovable.getDisplayName()));
        }

        int covenSize = CovenContributionCounter.countLivingSworn(
                source, volume.entityBounds(), player.getUUID());
        if (covenSize < REQUIRED_COVEN_SIZE) {
            blockers.add(issue(PreflightIssueKind.COVEN,
                    "witchery.otherwhere_hearth.coven", REQUIRED_COVEN_SIZE, covenSize));
        }

        AltarBlockEntity altar = findHearthAltar(source, kettlePos, volume);
        int powerCost = RitualExecutor.scaleRitualPowerCost(BASE_CROSSING_POWER_COST);
        if (altar == null || !altar.hasValidStructure()) {
            blockers.add(issue(PreflightIssueKind.NO_ALTAR,
                    "witchery.otherwhere_hearth.no_altar"));
        } else {
            if (!volume.contains(altar.getBlockPos())) {
                blockers.add(issue(PreflightIssueKind.ALTAR_OUTSIDE,
                        "witchery.otherwhere_hearth.altar_outside"));
            }
            if (altar.getEffectiveMaxPower() < powerCost) {
                blockers.add(issue(PreflightIssueKind.ALTAR_TOO_WEAK,
                        "witchery.otherwhere_hearth.altar_too_weak",
                        powerCost, altar.getEffectiveMaxPower()));
            }
            if (altar.getPower() < powerCost) {
                blockers.add(issue(PreflightIssueKind.NO_POWER,
                        "witchery.otherwhere_hearth.no_power", powerCost, altar.getPower()));
            }
        }

        BlockProblem sourceProblem = findSideBlockProblem(
                player, source, volume, kettlePos, true);
        if (sourceProblem != null) {
            blockers.add(issueForBlockProblem(sourceProblem));
        }
    }

    private static PlayerInspection deniedInspection(
            Volume volume, PreflightIssueKind kind, String translationKey) {
        return new PlayerInspection(volume, false,
                List.of(issue(kind, translationKey)), DestinationStatus.VEILED, List.of());
    }

    private static PreflightIssue issue(
            PreflightIssueKind kind, String translationKey, Object... arguments) {
        return new PreflightIssue(kind, Component.translatable(translationKey, arguments));
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

        int covenSize = CovenContributionCounter.countLivingSworn(
                source, volume.entityBounds(), player.getUUID());
        if (covenSize < REQUIRED_COVEN_SIZE) {
            return fail(player, source, kettlePos, "witchery.otherwhere_hearth.coven",
                    REQUIRED_COVEN_SIZE, covenSize);
        }

        AltarBlockEntity altar = findHearthAltar(source, kettlePos, volume);
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

    @Nullable
    private static AltarBlockEntity findHearthAltar(
            ServerLevel level, BlockPos kettlePos, Volume volume) {
        AltarBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos cursor : BlockPos.betweenClosed(volume.min(), volume.max())) {
            if (!(level.getBlockEntity(cursor) instanceof AltarBlockEntity altar)
                    || !altar.hasValidStructure()) {
                continue;
            }
            double distance = AltarPowerSourceLookup.distanceSqr(kettlePos, altar.getBlockPos());
            if (distance <= (double) altar.getRange() * altar.getRange()
                    && distance < closestDistance) {
                closest = altar;
                closestDistance = distance;
            }
        }
        return closest;
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
        BlockProblem problem = findBlockProblem(player, source, destination, volume, activeKettlePos);
        if (problem == null) {
            return true;
        }
        return switch (problem.kind()) {
            case FORBIDDEN -> fail(player, source, activeKettlePos,
                    "witchery.otherwhere_hearth.forbidden_block",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case OTHER_BOUND_KETTLE -> fail(player, source, activeKettlePos,
                    "witchery.otherwhere_hearth.other_bound_kettle",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case PROTECTED_SOURCE -> fail(player, source, activeKettlePos,
                    "witchery.otherwhere_hearth.protected_source",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case PROTECTED_DESTINATION -> fail(player, source, activeKettlePos,
                    "witchery.otherwhere_hearth.protected_destination",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
        };
    }

    @Nullable
    private static BlockProblem findBlockProblem(ServerPlayer player, ServerLevel source,
                                                 ServerLevel destination, Volume volume,
                                                 BlockPos activeKettlePos) {
        for (BlockPos cursor : BlockPos.betweenClosed(volume.min(), volume.max())) {
            BlockPos pos = cursor.immutable();
            BlockState sourceState = source.getBlockState(pos);
            BlockState destinationState = destination.getBlockState(pos);

            if (isForbidden(source, pos, sourceState) || isForbidden(destination, pos, destinationState)) {
                return new BlockProblem(BlockProblemKind.FORBIDDEN, pos);
            }

            if (isForeignBoundKettle(source, pos, activeKettlePos)
                    || isForeignBoundKettle(destination, pos, null)) {
                return new BlockProblem(BlockProblemKind.OTHER_BOUND_KETTLE, pos);
            }

            if (sourceState.isAir() && destinationState.isAir()
                    && source.getBlockEntity(pos) == null && destination.getBlockEntity(pos) == null) {
                continue;
            }
            if (!WitcheryProtectionService.canModifyRemoteBlock(
                    player, source, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                return new BlockProblem(BlockProblemKind.PROTECTED_SOURCE, pos);
            }
            if (!WitcheryProtectionService.canModifyRemoteBlock(
                    player, destination, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                return new BlockProblem(BlockProblemKind.PROTECTED_DESTINATION, pos);
            }
        }
        return null;
    }

    @Nullable
    private static BlockProblem findSideBlockProblem(
            ServerPlayer player,
            ServerLevel level,
            Volume volume,
            @Nullable BlockPos permittedKettlePos,
            boolean sourceSide) {
        for (BlockPos cursor : BlockPos.betweenClosed(volume.min(), volume.max())) {
            BlockPos pos = cursor.immutable();
            BlockState state = level.getBlockState(pos);
            if (isForbidden(level, pos, state)) {
                return new BlockProblem(BlockProblemKind.FORBIDDEN, pos);
            }
            if (isForeignBoundKettle(level, pos, permittedKettlePos)) {
                return new BlockProblem(BlockProblemKind.OTHER_BOUND_KETTLE, pos);
            }
            if (state.isAir() && level.getBlockEntity(pos) == null) {
                continue;
            }
            if (!WitcheryProtectionService.canModifyRemoteBlock(
                    player, level, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                return new BlockProblem(sourceSide
                        ? BlockProblemKind.PROTECTED_SOURCE
                        : BlockProblemKind.PROTECTED_DESTINATION, pos);
            }
        }
        return null;
    }

    private static PreflightIssue issueForBlockProblem(BlockProblem problem) {
        return switch (problem.kind()) {
            case FORBIDDEN -> issue(PreflightIssueKind.FORBIDDEN_BLOCK,
                    "witchery.otherwhere_hearth.forbidden_block",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case OTHER_BOUND_KETTLE -> issue(PreflightIssueKind.OTHER_BOUND_KETTLE,
                    "witchery.otherwhere_hearth.other_bound_kettle",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case PROTECTED_SOURCE -> issue(PreflightIssueKind.PROTECTED_SOURCE,
                    "witchery.otherwhere_hearth.protected_source",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
            case PROTECTED_DESTINATION -> issue(PreflightIssueKind.PROTECTED_DESTINATION,
                    "witchery.otherwhere_hearth.protected_destination",
                    problem.pos().getX(), problem.pos().getY(), problem.pos().getZ());
        };
    }

    private static String coordinates(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
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
        template.fillFromWorld(
                level, volume.min(), new Vec3i(WIDTH, HEIGHT, DEPTH), false, List.of());
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
        List<Entity> entities = new ArrayList<>(level.getEntitiesOfClass(Entity.class, bounds,
                entity -> !entity.isRemoved()
                        && (bounds.intersects(entity.getBoundingBox())
                        || bounds.contains(entity.position()))));
        Set<UUID> selected = entities.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        for (ServerPlayer player : playersIn(level, volume)) {
            if (!player.isRemoved() && selected.add(player.getUUID())) {
                entities.add(player);
            }
        }
        return List.copyOf(entities);
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

    @Nullable
    private static Entity firstUnmovableEntityOnSide(
            ServerLevel source, ServerLevel destination, Volume volume) {
        for (Entity entity : entitiesIn(source, volume)) {
            if (!canJournalAndTeleport(entity, source, destination)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean canJournalAndTeleport(Entity entity, ServerLevel source,
                                                 ServerLevel destination) {
        if (!entity.canTeleport(source, destination)) {
            return false;
        }
        if (entity instanceof ServerPlayer) {
            return true;
        }
        try {
            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING, entity.level().registryAccess());
            // Passengers and vehicles can intentionally return false from shouldBeSaved() to
            // prevent vanilla chunk duplication, while saveAsPassenger remains the correct
            // standalone snapshot contract for this transaction journal.
            return entity.saveAsPassenger(output);
        } catch (RuntimeException exception) {
            return false;
        }
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

            boolean stagedHearthEntry = entity instanceof ServerPlayer player
                    && transfer.toSpirit()
                    && !WitcheryAttachments.get(player).isOtherwhereHearthTraveler();
            if (stagedHearthEntry) {
                updateHearthTravelerState(entity, true);
            }

            Entity resolved;
            try {
                resolved = teleportEntity(entity, destination, transfer);
            } catch (RuntimeException exception) {
                if (stagedHearthEntry) {
                    updateHearthTravelerState(entity, false);
                }
                throw exception;
            }
            if (resolved == null) {
                if (stagedHearthEntry) {
                    updateHearthTravelerState(entity, false);
                }
                throw new IllegalStateException("Could not move entity " + transfer.entityId()
                        + "; removed=" + entity.isRemoved()
                        + ", source=" + entity.level().dimension().identifier()
                        + ", destination=" + destination.dimension().identifier());
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
            ServerPlayer online = server.getPlayerList().getPlayer(transfer.entityId());
            if (online != null) {
                return online;
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    if (player.getUUID().equals(transfer.entityId()) && !player.isRemoved()) {
                        return player;
                    }
                }
            }
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

    @Nullable
    private static Entity teleportEntity(Entity entity, ServerLevel destination,
                                         OtherwhereHearthSavedData.EntityTransfer transfer) {
        if (entity.level() == destination) {
            entity.teleportTo(transfer.x(), transfer.y(), transfer.z());
            entity.setYRot(transfer.yaw());
            entity.setXRot(transfer.pitch());
            return entity;
        }
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(
                    destination,
                    transfer.x(), transfer.y(), transfer.z(),
                    Set.<Relative>of(), transfer.yaw(), transfer.pitch(), false);
            return player.level() == destination ? player : null;
        }
        return entity.teleport(new TeleportTransition(
                destination,
                new Vec3(transfer.x(), transfer.y(), transfer.z()),
                Vec3.ZERO,
                transfer.yaw(),
                transfer.pitch(),
                Set.<Relative>of(),
                TeleportTransition.DO_NOTHING));
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
        BlockPos center = volume.min().offset(HORIZONTAL_RADIUS, 0, HORIZONTAL_RADIUS);
        ChunkPos centerChunk = new ChunkPos(center);
        level.getChunkSource().addTicketWithRadius(HEARTH_LOAD_TICKET, centerChunk, 1);
        try {
            int minChunkX = volume.min().getX() >> 4;
            int maxChunkX = volume.max().getX() >> 4;
            int minChunkZ = volume.min().getZ() >> 4;
            int maxChunkZ = volume.max().getZ() >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                }
            }
            level.waitForEntities(centerChunk, 1);
        } finally {
            level.getChunkSource().removeTicketWithRadius(HEARTH_LOAD_TICKET, centerChunk, 1);
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
