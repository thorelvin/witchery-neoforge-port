package com.emoniph.witchery.world;

import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loaded-world certificate for Mirror World geometry, allocation links, and guardians. */
public final class MirrorWorldCertification {
    private static final int MAX_FAILURE_DETAILS = 96;

    private MirrorWorldCertification() {}

    public static Report audit(ServerLevel mirrorWorld) {
        MirrorRoomData roomData = MirrorRoomData.get(mirrorWorld.getServer());
        FailureCollector failures = new FailureCollector();
        List<RoomReport> roomReports = new ArrayList<>();
        Set<Integer> indexes = new HashSet<>();
        Set<String> sources = new HashSet<>();

        for (MirrorRoomData.Room room : roomData.rooms()) {
            if (!indexes.add(room.index())) {
                failures.add("Room index " + room.index() + " is allocated more than once.");
            }
            String sourceKey = room.sourceDimension() + "@" + room.sourcePos();
            if (!sources.add(sourceKey)) {
                failures.add("Source mirror " + sourceKey + " owns more than one room.");
            }
            roomReports.add(auditRoom(mirrorWorld, room, failures));
        }

        if (roomData.rooms().isEmpty()) {
            mirrorWorld.getChunk(0, 0);
            int checked = auditGeometry(mirrorWorld, 0, 0, 0, false, failures);
            roomReports.add(new RoomReport(-1, 0, 0, 0, checked, 0, false, true));
        }

        int minimumNext = roomData.rooms().stream().mapToInt(MirrorRoomData.Room::index).max().orElse(-1) + 1;
        if (roomData.nextRoomIndex() < minimumNext) {
            failures.add("NextRoom " + roomData.nextRoomIndex()
                    + " overlaps an allocated room; expected at least " + minimumNext + ".");
        }

        return new Report(roomData.nextRoomIndex(), List.copyOf(roomReports),
                failures.total(), failures.details());
    }

    private static RoomReport auditRoom(ServerLevel mirrorWorld, MirrorRoomData.Room room,
                                        FailureCollector failures) {
        mirrorWorld.getChunk(room.chunkX(), room.chunkZ());
        int checkedBlocks = auditGeometry(mirrorWorld, room.chunkX(), room.sectionY(), room.chunkZ(), true, failures);
        BlockPos innerPos = MirrorWorldLayout.innerMirrorFoot(room.chunkX(), room.sectionY(), room.chunkZ());
        MirrorBlockEntity inner = MirrorHelper.getMirrorBlockEntity(mirrorWorld, innerPos);
        boolean linksValid = inner != null
                && room.sourceBlockPos().equals(inner.getLinkedPos())
                && room.sourceDimension().equals(inner.getLinkedDim());
        if (!linksValid) {
            failures.add("Room " + room.index() + " inner mirror has a missing or incorrect source link.");
        }

        ServerLevel sourceLevel = MirrorHelper.resolveLevelByKey(
                mirrorWorld.getServer(), room.sourceDimension());
        MirrorBlockEntity source = sourceLevel == null ? null
                : MirrorHelper.getMirrorBlockEntity(sourceLevel, room.sourceBlockPos());
        if (source == null) {
            failures.add("Room " + room.index() + " source mirror is missing at "
                    + room.sourceDimension() + " " + room.sourceBlockPos() + ".");
        } else {
            int[] assigned = source.getRoom();
            if (!source.hasRoom() || assigned[0] != room.chunkX()
                    || assigned[1] != room.sectionY() || assigned[2] != room.chunkZ()) {
                failures.add("Room " + room.index() + " source mirror points to a different room.");
            }
            if (inner != null && source.isDemonKilled() != inner.isDemonKilled()) {
                failures.add("Room " + room.index() + " inner/source cleared flags disagree.");
            }
        }

        List<ReflectionEntity> occupants = mirrorWorld.getEntitiesOfClass(
                ReflectionEntity.class, MirrorHelper.roomBounds(room.chunkX(), room.sectionY(), room.chunkZ()),
                ReflectionEntity::isAlive);
        int guardians = 0;
        for (ReflectionEntity reflection : occupants) {
            if (reflection.isRoomGuardianFor(room.index())) {
                guardians++;
            } else {
                failures.add("Room " + room.index() + " contains Reflection "
                        + reflection.getUUID() + " assigned to room "
                        + reflection.getGuardianRoomIndex() + ".");
            }
        }

        boolean cleared = source != null && source.isDemonKilled();
        int expectedGuardians = cleared ? 0 : 1;
        if (guardians != expectedGuardians) {
            failures.add("Room " + room.index() + " is " + (cleared ? "cleared" : "uncleared")
                    + " but has " + guardians + " guardian(s); expected " + expectedGuardians + ".");
        }
        java.util.Optional<java.util.UUID> recordedGuardian = room.guardian();
        if (cleared && recordedGuardian.isPresent()) {
            failures.add("Cleared room " + room.index() + " still records guardian "
                    + recordedGuardian.get() + ".");
        } else if (!cleared && guardians == 1) {
            java.util.UUID liveGuardian = occupants.stream()
                    .filter(reflection -> reflection.isRoomGuardianFor(room.index()))
                    .findFirst().orElseThrow().getUUID();
            if (recordedGuardian.filter(liveGuardian::equals).isEmpty()) {
                failures.add("Room " + room.index() + " guardian record does not match live guardian "
                        + liveGuardian + ".");
            }
        } else if (!cleared && recordedGuardian.isEmpty()) {
            failures.add("Uncleared room " + room.index() + " has no recorded guardian UUID.");
        } else if (!cleared && guardians == 0) {
            failures.add("Uncleared room " + room.index() + " records guardian "
                    + recordedGuardian.orElseThrow() + " but that entity is not live in the room.");
        }
        return new RoomReport(room.index(), room.chunkX(), room.sectionY(), room.chunkZ(), checkedBlocks,
                guardians, cleared, linksValid);
    }

    private static int auditGeometry(ServerLevel world, int chunkX, int sectionY, int chunkZ, boolean allocated,
                                     FailureCollector failures) {
        int checked = 0;
        BlockPos innerFoot = MirrorWorldLayout.innerMirrorFoot(chunkX, sectionY, chunkZ);
        int minY = sectionY * MirrorWorldLayout.ROOM_SECTION_HEIGHT;
        int maxY = minY + MirrorWorldLayout.ROOM_SECTION_HEIGHT;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(chunkX * 16 + localX, y, chunkZ * 16 + localZ);
                    BlockState state = world.getBlockState(pos);
                    checked++;
                    if (!MirrorWorldLayout.isInterior(localX, y, localZ)) {
                        expect(state.is(WitcheryBlocks.MIRROR_WALL.get()), pos,
                                "mirror wall", state, failures);
                    } else if (allocated && pos.equals(innerFoot)) {
                        expectInnerMirror(state, pos, BedPart.FOOT, failures);
                    } else if (allocated && pos.equals(innerFoot.above())) {
                        expectInnerMirror(state, pos, BedPart.HEAD, failures);
                    } else if (!state.isAir() && !isBreakableMirror(state)) {
                        failures.add("Mirror room interior mismatch at " + pos + ": found "
                                + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + ".");
                    }
                }
            }
        }
        return checked;
    }

    private static boolean isBreakableMirror(BlockState state) {
        return state.is(WitcheryBlocks.MIRROR.get())
                && state.hasProperty(MirrorBlock.UNBREAKABLE)
                && !state.getValue(MirrorBlock.UNBREAKABLE);
    }

    private static void expectInnerMirror(BlockState state, BlockPos pos, BedPart part,
                                          FailureCollector failures) {
        boolean matches = state.is(WitcheryBlocks.MIRROR.get())
                && state.getValue(MirrorBlock.UNBREAKABLE)
                && state.getValue(MirrorBlock.PART) == part
                && state.getValue(MirrorBlock.FACING) == Direction.EAST;
        expect(matches, pos, "unbreakable " + part.getSerializedName() + " inner mirror", state, failures);
    }

    private static void expect(boolean matches, BlockPos pos, String expected, BlockState found,
                               FailureCollector failures) {
        if (!matches) {
            failures.add("Expected " + expected + " at " + pos + " but found "
                    + BuiltInRegistries.BLOCK.getKey(found.getBlock()) + ".");
        }
    }

    public record Report(int nextRoom, List<RoomReport> rooms, int mismatchCount,
                         List<String> failures) {
        public boolean passed() {
            return mismatchCount == 0;
        }

        public int allocatedRoomCount() {
            return (int) rooms.stream().filter(room -> room.index() >= 0).count();
        }
    }

    public record RoomReport(int index, int chunkX, int sectionY, int chunkZ, int checkedBlocks,
                             int guardians, boolean cleared, boolean linksValid) {}

    private static final class FailureCollector {
        private final List<String> details = new ArrayList<>();
        private int total;

        void add(String failure) {
            total++;
            if (details.size() < MAX_FAILURE_DETAILS) {
                details.add(failure);
            }
        }

        int total() {
            return total;
        }

        List<String> details() {
            if (total > details.size()) {
                details.add("... " + (total - details.size()) + " additional mismatch(es) omitted.");
            }
            return List.copyOf(details);
        }
    }
}
