package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.events.VillageWallEvents;
import com.emoniph.witchery.world.VillageWallData;
import com.emoniph.witchery.world.WitcheryStructureData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Read-only, restart-stable certification of an already generated Witchery village. */
public final class VillageCertification {
    private static final int MIN_MATERIAL_FAMILY_BLOCKS = 16;
    private static final List<String> BUILDINGS = List.of(
            "apothecary", "bookshop", "witch_hut", "village_watch_tower", "village_keep");

    public record Report(boolean passed, String summary) {
    }

    private VillageCertification() {
    }

    public static Report audit(ServerLevel level, BlockPos near) {
        StructureStart start = VillageWallEvents.findVillageStartNear(level, near);
        if (start == null || !start.isValid()) {
            return new Report(false, "[VillageCertification] FAIL reason=no_loaded_village near=" + compact(near));
        }

        BoundingBox villageBox = start.getBoundingBox();
        List<String> failures = new ArrayList<>();
        List<WitcheryStructureData.Entry> entries = WitcheryStructureData.get(level).entriesWithin(
                level, WitcheryStructureData.VILLAGE_BUILDING, villageBox, 8);
        VillageBuildingMaterials.VillageType villageType = VillageBuildingMaterials.resolveVillageType(level, start);
        if (villageType == null) {
            failures.add("unknown_material_family");
            villageType = VillageBuildingMaterials.VillageType.PLAINS;
        }
        VillageBuildingMaterials materials = VillageBuildingMaterials.forType(villageType);

        int planned = 0;
        int certifiedMaterials = 0;
        int expectedResidents = 0;
        int actualResidents = 0;
        AABB residentSearch = new AABB(villageBox.minX() - 64, level.getMinY(), villageBox.minZ() - 64,
                villageBox.maxX() + 65, level.getMaxY(), villageBox.maxZ() + 65);
        List<LivingEntity> residents = level.getEntitiesOfClass(LivingEntity.class, residentSearch);

        for (String building : BUILDINGS) {
            BlockPos plannedOrigin = WitcheryVillageElement.canonicalBuildingOrigin(start, building);
            List<WitcheryStructureData.Entry> claims = entries.stream()
                    .filter(entry -> building.equals(entry.variant()))
                    .toList();
            if (claims.size() > 1) {
                failures.add("duplicate_" + building);
            }
            if (plannedOrigin == null) {
                if (!claims.isEmpty()) failures.add("unplanned_" + building);
                continue;
            }
            planned++;
            if (claims.size() != 1 || !claims.getFirst().pos().equals(plannedOrigin)) {
                failures.add("claim_" + building);
                continue;
            }

            int familyBlocks = countFamilyBlocks(level, building, plannedOrigin, materials);
            if (familyBlocks < MIN_MATERIAL_FAMILY_BLOCKS) {
                failures.add("materials_" + building);
            } else {
                certifiedMaterials++;
            }

            int expected = expectedResidents(building);
            int actual = (int) residents.stream()
                    .filter(resident -> VillageResidentMarker.matches(resident, building, plannedOrigin))
                    .count();
            expectedResidents += expected;
            actualResidents += actual;
            if (actual != expected) {
                failures.add("residents_" + building + "_" + actual + "of" + expected);
            }
        }
        if (planned == 0) {
            failures.add("no_witchery_buildings");
        }

        VillageWallData.WallRecord walls = VillageWallData.get(level).record(villageBox).orElse(null);
        if (walls == null) {
            failures.add("walls_unrecorded");
        } else if (walls.gateCandidates() <= 0 || walls.gates() <= 0) {
            failures.add("gates_" + walls.gateCandidates() + "_" + walls.gates());
        }

        BlockPos landing = VillageWallEvents.certificationLanding(level, start);
        if (!VillageWallEvents.isSafeLanding(level, landing)) {
            failures.add("unsafe_landing");
        }

        boolean passed = failures.isEmpty();
        String status = passed ? "PASS" : "FAIL";
        String summary = "[VillageCertification] " + status
                + " type=" + villageType.name().toLowerCase(java.util.Locale.ROOT)
                + " center=" + compact(center(villageBox))
                + " claims=" + entries.size() + "/" + planned
                + " materials=" + certifiedMaterials + "/" + planned
                + " residents=" + actualResidents + "/" + expectedResidents
                + " gates=" + (walls == null ? "0/0" : walls.gates() + "/" + walls.gateCandidates())
                + " landing=" + compact(landing)
                + (passed ? "" : " failures=" + String.join("|", failures));
        return new Report(passed, summary);
    }

    private static int countFamilyBlocks(ServerLevel level, String building, BlockPos origin,
                                         VillageBuildingMaterials materials) {
        int[] size = WitcheryVillageElement.dimensions(building);
        int count = 0;
        for (int x = 0; x < size[0]; x++) {
            for (int y = 0; y < size[1]; y++) {
                for (int z = 0; z < size[2]; z++) {
                    if (materials.belongsToFamily(level.getBlockState(origin.offset(x, y, z)))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int expectedResidents(String building) {
        return switch (building) {
            case "village_watch_tower" -> 3;
            case "village_keep" -> 12;
            default -> 1;
        };
    }

    private static BlockPos center(BoundingBox box) {
        return new BlockPos((box.minX() + box.maxX()) / 2, 64, (box.minZ() + box.maxZ()) / 2);
    }

    private static String compact(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
