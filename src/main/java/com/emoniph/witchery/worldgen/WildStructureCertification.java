package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.CrazyCatLadyEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.world.VanishingHutData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Loaded-world mechanical certificate for the complete six-structure wild roster. */
public final class WildStructureCertification {

    private static final TagKey<Biome> IS_FOREST = TagKey.create(Registries.BIOME,
            Identifier.withDefaultNamespace("is_forest"));
    private static final Set<String> SUPPORTED_IDS = Set.of(
            WildStructureArrival.COVEN_CIRCLE,
            WildStructureArrival.STRAW_MAN,
            WildStructureArrival.HOBGOBLIN_HUT,
            WildStructureArrival.WITCH_HUT,
            WildStructureArrival.WARPED_COVEN,
            WildStructureArrival.VANISHING_HUT);

    private WildStructureCertification() {}

    public static Report audit(ServerLevel level, String rawId, BlockPos near) {
        Identifier id;
        try {
            id = Identifier.parse(rawId);
        } catch (RuntimeException ex) {
            return fail(rawId, "invalid_id");
        }
        String key = id.toString();
        if (!SUPPORTED_IDS.contains(key)) {
            return fail(key, "unsupported_id");
        }

        StructureStart start = null;
        BoundingBox box;
        BlockPos anchor = near;
        UUID expectedResident = null;
        boolean naturalPlacement = true;
        if (WildStructureArrival.VANISHING_HUT.equals(key)) {
            var tracked = VanishingHutData.get(level).nearest(level, near);
            if (tracked.isEmpty()) {
                return fail(key, "no_tracked_hut");
            }
            anchor = tracked.get().pos();
            box = vanishingBox(anchor);
            if (level.getBlockEntity(anchor) instanceof VanishingHutAnchorBlockEntity hut) {
                expectedResident = hut.residentUuid();
            }
            naturalPlacement = hasMatchingPiece(level, id, anchor, box);
        } else {
            start = WildStructureArrival.findLoadedStart(level, id, near, 4);
            if (start == null) {
                return fail(key, "no_loaded_start");
            }
            // StructureStart bounds include terrain-adaptation padding. All six certified
            // structures own a single piece, whose box is the actual block contract.
            box = start.getPieces().getFirst().getBoundingBox();
            anchor = new BlockPos((box.minX() + box.maxX()) / 2, box.minY(),
                    (box.minZ() + box.maxZ()) / 2);
        }

        List<String> failures = new ArrayList<>();
        checkDimension(level, key, failures);
        if (naturalPlacement) {
            checkBiome(level, key, anchor, failures);
        }
        checkDimensions(key, box, failures);

        Scan scan = scan(level, box);
        ResidentCounts residents = residents(level, box, expectedResident);
        checkStructure(level, key, box, scan, residents, expectedResident, failures);

        BlockPos landing = WildStructureArrival.VANISHING_HUT.equals(key)
                ? WildStructureArrival.findVanishingHutLanding(level, anchor)
                : WildStructureArrival.findLanding(level, id, start);
        BlockPos traversalTarget = WildStructureArrival.traversalTarget(key, box);
        int traversal = WildStructureArrival.traversalDistance(level, landing, traversalTarget, box);
        if (landing == null || !WildStructureArrival.isSafeLanding(level, landing)) {
            failures.add("unsafe_landing");
        }
        if (traversal < 0) {
            failures.add("no_traversal");
        }

        String status = failures.isEmpty() ? "PASS" : "FAIL";
        String summary = "[WildStructureCertification] " + status
                + " id=" + key
                + " dimension=" + level.dimension().identifier()
                + " placement=" + (naturalPlacement ? "natural" : "relocated")
                + " anchor=" + compact(anchor)
                + " box=" + box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan()
                + " blocks=" + scan.nonAir
                + " fluids=" + scan.fluids
                + " residents=" + residents.summary()
                + " spawners=" + scan.spawnerTypes
                + " loot=" + scan.lootTables
                + " landing=" + compact(landing)
                + " target=" + compact(traversalTarget)
                + " traversal=" + traversal
                + " failures=" + (failures.isEmpty() ? "none" : String.join(",", failures));
        return new Report(failures.isEmpty(), summary, anchor, landing, traversal);
    }

    private static boolean hasMatchingPiece(ServerLevel level, Identifier id, BlockPos near,
                                            BoundingBox expected) {
        StructureStart start = WildStructureArrival.findLoadedStart(level, id, near, 4);
        if (start == null) {
            return false;
        }
        for (var piece : start.getPieces()) {
            BoundingBox actual = piece.getBoundingBox();
            if (actual.minX() == expected.minX() && actual.minY() == expected.minY()
                    && actual.minZ() == expected.minZ() && actual.maxX() == expected.maxX()
                    && actual.maxY() == expected.maxY() && actual.maxZ() == expected.maxZ()) {
                return true;
            }
        }
        return false;
    }

    private static void checkDimension(ServerLevel level, String id, List<String> failures) {
        boolean valid = WildStructureArrival.WARPED_COVEN.equals(id)
                ? level.dimension().equals(Level.NETHER)
                : level.dimension().equals(Level.OVERWORLD);
        if (!valid) {
            failures.add("wrong_dimension");
        }
    }

    private static void checkBiome(ServerLevel level, String id, BlockPos anchor,
                                   List<String> failures) {
        Holder<Biome> biome = level.getBiome(anchor);
        boolean valid = switch (id) {
            case WildStructureArrival.WARPED_COVEN -> biome.is(Biomes.WARPED_FOREST);
            case WildStructureArrival.HOBGOBLIN_HUT, WildStructureArrival.VANISHING_HUT ->
                    biome.is(IS_FOREST) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
            case WildStructureArrival.WITCH_HUT -> biome.is(IS_FOREST);
            case WildStructureArrival.COVEN_CIRCLE, WildStructureArrival.STRAW_MAN ->
                    !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER);
            default -> false;
        };
        if (!valid) {
            failures.add("wrong_biome");
        }
    }

    private static void checkDimensions(String id, BoundingBox box, List<String> failures) {
        int[] expected = switch (id) {
            case WildStructureArrival.COVEN_CIRCLE -> new int[]{11, 4, 11};
            case WildStructureArrival.STRAW_MAN -> new int[]{6, 8, 5};
            case WildStructureArrival.HOBGOBLIN_HUT -> new int[]{14, 14, 14};
            case WildStructureArrival.WITCH_HUT -> new int[]{4, 6, 5};
            case WildStructureArrival.WARPED_COVEN -> new int[]{17, 17, 17};
            case WildStructureArrival.VANISHING_HUT ->
                    new int[]{VanishingHut.SIZE_X, VanishingHut.SIZE_Y, VanishingHut.SIZE_Z};
            default -> new int[]{0, 0, 0};
        };
        if (box.getXSpan() != expected[0] || box.getYSpan() != expected[1]
                || box.getZSpan() != expected[2]) {
            failures.add("wrong_box");
        }
    }

    private static void checkStructure(ServerLevel level, String id, BoundingBox box, Scan scan,
                                       ResidentCounts residents, UUID expectedResident,
                                       List<String> failures) {
        switch (id) {
            case WildStructureArrival.COVEN_CIRCLE -> {
                requireBlock(scan, "minecraft:cobweb", 3, failures, "cobwebs");
                requireBlock(scan, "minecraft:spawner", 2, failures, "witch_spawners");
                requireBlock(scan, "minecraft:dispenser", 1, failures, "loot_dispenser");
                BlockPos altarCenter = local(box, 5, 1, 5);
                BlockState altarBasin = level.getBlockState(altarCenter);
                if (!altarBasin.is(Blocks.WATER) && !altarBasin.is(Blocks.ICE)) {
                    failures.add("altar_basin_" + blockId(level, altarCenter));
                }
                if (!scan.spawnerTypes.equals(Set.of("minecraft:witch"))) {
                    failures.add("wrong_spawner_type");
                }
                if (!scan.lootTables.contains("minecraft:chests/abandoned_mineshaft")) {
                    failures.add("wrong_loot");
                }
                if (residents.covenWitches > 1) {
                    failures.add("duplicate_resident");
                }
                if (scan.fluids + scan.count("minecraft:ice") != 1) {
                    failures.add("wet_footprint");
                }
                checkRectSupport(level, box, 0, 10, 0, 10, 0, failures);
            }
            case WildStructureArrival.STRAW_MAN -> {
                requireBlock(scan, "minecraft:hay_block", 20, failures, "hay_shape");
                requireBlock(scan, "minecraft:spawner", 1, failures, "hidden_spawner");
                if (scan.spawnerTypes.size() != 1 || (!scan.spawnerTypes.contains("minecraft:zombie")
                        && !scan.spawnerTypes.contains("minecraft:skeleton"))) {
                    failures.add("wrong_spawner_type");
                }
                if (scan.fluids != 0) {
                    failures.add("wet_footprint");
                }
                checkStrawSupport(level, box, failures);
            }
            case WildStructureArrival.HOBGOBLIN_HUT -> {
                if (residents.hobgoblins != 1) {
                    failures.add("resident_count");
                }
                if (scan.count("minecraft:chest") != 0 || scan.count("minecraft:barrel") != 0) {
                    failures.add("unexpected_storage");
                }
                if (scan.fluids != 0) {
                    failures.add("wet_footprint");
                }
                checkSupportColumns(level, box,
                        new int[][]{{2, 4}, {2, 8}, {4, 9}, {8, 8}, {11, 4}, {11, 8}},
                        -1, failures);
            }
            case WildStructureArrival.WITCH_HUT -> {
                requireBlock(scan, "minecraft:chest", 1, failures, "loot_chest");
                requireBlock(scan, "minecraft:crafting_table", 1, failures, "crafting_table");
                requireBlock(scan, "witchery:rowan_log", 1, failures, "rowan_table");
                if (residents.covenWitches + residents.catLadies != 1) {
                    failures.add("resident_count");
                }
                if (!scan.lootTables.contains("witchery:chests/witch_hut")) {
                    failures.add("wrong_loot");
                }
                if (scan.fluids != 0) {
                    failures.add("wet_footprint");
                }
                checkRectSupport(level, box, 0, 3, 0, 4, 0, failures);
            }
            case WildStructureArrival.WARPED_COVEN -> {
                requireBlock(scan, "witchery:witches_oven", 1, failures, "oven");
                requireBlock(scan, "witchery:witch_cauldron", 1, failures, "cauldron");
                requireBlock(scan, "witchery:chalice", 1, failures, "chalice");
                requireBlock(scan, "minecraft:chest", 1, failures, "loot_chest");
                if (residents.covenWitches != 1) {
                    failures.add("resident_count");
                }
                if (!scan.lootTables.contains("witchery:chests/warped_coven")) {
                    failures.add("wrong_loot");
                }
                if (scan.containsPath("mirror") || scan.containsPath("snow")) {
                    failures.add("designer_artifact");
                }
                if (scan.fluids != 0) {
                    failures.add("fluid_intersection");
                }
            }
            case WildStructureArrival.VANISHING_HUT -> {
                requireBlock(scan, "witchery:vanishing_hut_anchor", 1, failures, "anchor");
                requireBlock(scan, "minecraft:oxidized_copper_chest", 1, failures, "loot_chest");
                requireBlock(scan, "witchery:witch_cauldron", 1, failures, "cauldron");
                if (residents.covenWitches > 1 || expectedResident == null
                        || residents.expectedResident != 1) {
                    failures.add("resident_identity");
                }
                if (!scan.lootTables.contains("witchery:chests/vanishing_hut")) {
                    failures.add("wrong_loot");
                }
                if (scan.fluids != 0) {
                    failures.add("wet_footprint");
                }
                checkRectSupport(level, box, 0, VanishingHut.SIZE_X - 1,
                        0, VanishingHut.SIZE_Z - 1, 0, failures);
            }
            default -> failures.add("unsupported_id");
        }
    }

    private static Scan scan(ServerLevel level, BoundingBox box) {
        Scan scan = new Scan();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);
                    String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    scan.blocks.merge(blockId, 1, Integer::sum);
                    if (!state.isAir()) {
                        scan.nonAir++;
                    }
                    if (!state.getFluidState().isEmpty()) {
                        scan.fluids++;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity == null) {
                        continue;
                    }
                    CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
                    tag.getString("LootTable").filter(value -> !value.isBlank()).ifPresent(scan.lootTables::add);
                    if (blockEntity instanceof SpawnerBlockEntity) {
                        tag.getCompound("SpawnData")
                                .flatMap(spawnData -> spawnData.getCompound("entity"))
                                .flatMap(entity -> entity.getString("id"))
                                .filter(value -> !value.isBlank())
                                .ifPresent(scan.spawnerTypes::add);
                    }
                }
            }
        }
        return scan;
    }

    private static ResidentCounts residents(ServerLevel level, BoundingBox box, UUID expectedResident) {
        AABB area = new AABB(box.minX() - 2, box.minY() - 2, box.minZ() - 2,
                box.maxX() + 3, box.maxY() + 3, box.maxZ() + 3);
        int coven = level.getEntitiesOfClass(CovenWitchEntity.class, area).size();
        int hobgoblins = level.getEntitiesOfClass(HobgoblinEntity.class, area).size();
        int catLadies = level.getEntitiesOfClass(CrazyCatLadyEntity.class, area).size();
        int matching = 0;
        if (expectedResident != null
                && level.getEntity(expectedResident) instanceof CovenWitchEntity expected
                && expected.isAlive()) {
            AABB identityArea = area.inflate(14.0);
            if (identityArea.contains(expected.position())) {
                matching = 1;
            }
        }
        return new ResidentCounts(coven, hobgoblins, catLadies, matching);
    }

    private static void requireBlock(Scan scan, String block, int expected,
                                     List<String> failures, String failure) {
        if (scan.count(block) != expected) {
            failures.add(failure);
        }
    }

    private static void checkRectSupport(ServerLevel level, BoundingBox box,
                                         int minX, int maxX, int minZ, int maxZ,
                                         int localY, List<String> failures) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = local(box, x, localY, z);
                if (level.getBlockState(pos).isAir() || !level.getBlockState(pos).getFluidState().isEmpty()) {
                    failures.add("unsupported_base");
                    return;
                }
            }
        }
    }

    private static void checkStrawSupport(ServerLevel level, BoundingBox box, List<String> failures) {
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                if (!StrawManTemplatePiece.isGroundFootprintForCertification(x, z)) {
                    continue;
                }
                BlockPos pos = local(box, x, 0, z);
                if (level.getBlockState(pos).isAir() || !level.getBlockState(pos).getFluidState().isEmpty()) {
                    failures.add("unsupported_base");
                    return;
                }
            }
        }
    }

    private static void checkSupportColumns(ServerLevel level, BoundingBox box, int[][] columns,
                                            int localY, List<String> failures) {
        for (int[] column : columns) {
            BlockPos pos = local(box, column[0], localY, column[1]);
            if (level.getBlockState(pos).isAir() || !level.getBlockState(pos).getFluidState().isEmpty()) {
                failures.add("unsupported_base");
                return;
            }
        }
    }

    private static BoundingBox vanishingBox(BlockPos anchor) {
        return new BoundingBox(anchor.getX() + VanishingHut.MIN_DX, anchor.getY(),
                anchor.getZ() + VanishingHut.MIN_DZ,
                anchor.getX() + VanishingHut.MAX_DX, anchor.getY() + VanishingHut.ROOF_DY,
                anchor.getZ() + VanishingHut.MAX_DZ);
    }

    private static BlockPos local(BoundingBox box, int x, int y, int z) {
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    private static String blockId(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                .toString().replace(':', '_');
    }

    private static String compact(BlockPos pos) {
        return pos == null ? "none" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static Report fail(String id, String reason) {
        return new Report(false, "[WildStructureCertification] FAIL id=" + id
                + " failures=" + reason, null, null, -1);
    }

    public record Report(boolean passed, String summary, BlockPos anchor,
                         BlockPos landing, int traversalDistance) {}

    private static final class Scan {
        private final Map<String, Integer> blocks = new HashMap<>();
        private final Set<String> lootTables = new HashSet<>();
        private final Set<String> spawnerTypes = new HashSet<>();
        private int nonAir;
        private int fluids;

        private int count(String id) {
            return blocks.getOrDefault(id, 0);
        }

        private boolean containsPath(String value) {
            return blocks.keySet().stream().anyMatch(id -> id.contains(value));
        }
    }

    private record ResidentCounts(int covenWitches, int hobgoblins, int catLadies,
                                  int expectedResident) {
        private String summary() {
            return "coven:" + covenWitches + ",hobgoblin:" + hobgoblins
                    + ",cat_lady:" + catLadies + ",expected:" + expectedResident;
        }
    }
}
