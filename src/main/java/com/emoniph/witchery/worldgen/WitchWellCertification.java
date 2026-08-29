package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.CursedBlockData;
import com.emoniph.witchery.brewing.CursedBlockWarningData;
import com.emoniph.witchery.world.WitcheryStructureData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed-seed-friendly live certificate for the additive three-variant Witch Well family. */
public final class WitchWellCertification {
    private static final TagKey<Biome> HAS_WITCH_WELL = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "has_witch_well"));
    private static final String LOOT_TABLE = "witchery:chests/witch_well";
    private static final Set<String> ADAPTIVE_SURFACES = Set.of(
            "minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium",
            "minecraft:moss_block", "minecraft:mud", "minecraft:clay",
            "minecraft:gravel", "minecraft:sand", "minecraft:red_sand",
            "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt");

    private WitchWellCertification() {}

    public static Report audit(ServerLevel level, String requestedVariant, BlockPos near) {
        WitchWellPiece.Variant expected = parseVariant(requestedVariant);
        if (expected == null && !"any".equals(requestedVariant)) {
            return fail(requestedVariant, "invalid_variant");
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return fail(requestedVariant, "wrong_dimension");
        }

        StructureStart start = WitchWellArrival.findLoadedStart(level, near);
        WitchWellPiece piece = WitchWellArrival.findPiece(start);
        if (piece == null) {
            return fail(requestedVariant, "no_loaded_start");
        }
        WitchWellPiece.Variant variant = piece.variant();
        if (expected != null && expected != variant) {
            return fail(requestedVariant, "variant_mismatch_actual_" + variant.serializedName());
        }

        BoundingBox box = piece.getBoundingBox();
        BlockPos anchor = piece.indexedTarget();
        WitchWellSourceContract.Contract contract = WitchWellSourceContract.forVariant(variant);
        List<String> failures = new ArrayList<>();
        if (!level.getBiome(anchor).is(HAS_WITCH_WELL)) {
            failures.add("wrong_biome");
        }
        if (box.getXSpan() != contract.sizeX() || box.getYSpan() != contract.sizeY()
                || box.getZSpan() != contract.sizeZ()) {
            failures.add("wrong_box");
        }
        boolean indexed = WitcheryStructureData.get(level)
                .entriesWithin(level, WitcheryStructureData.WITCH_WELL, box, 2).stream()
                .anyMatch(entry -> variant.serializedName().equals(entry.variant())
                        && anchor.equals(entry.pos()));
        if (!indexed) {
            failures.add("missing_index");
        }

        StructureTemplate template = level.getServer().getStructureManager().getOrCreate(variant.template());
        AuthoredSignature authored = checkAuthoredSignature(level, template, box, contract, failures);
        Scan scan = scan(level, box);
        if (scan.blockEntities != contract.blockEntityCount()) {
            failures.add("block_entities_" + scan.blockEntities);
        }
        if (authored.fluidCells != contract.fluidCells()) {
            failures.add("authored_fluid_cells_" + authored.fluidCells);
        }
        if ((variant == WitchWellPiece.Variant.REGULAR || variant == WitchWellPiece.Variant.CAVE)
                && (!scan.spawnerTypes.equals(Set.of("minecraft:witch"))
                || scan.count("minecraft:spawner") != 2)) {
            failures.add("witch_spawners");
        }

        Map<String, Integer> surfacePalette = checkSurface(
                level, piece, authored.positions, failures);
        checkCenter(level, piece, failures);
        String loot = checkLoot(level, piece, scan, failures);
        CurseState curse = checkCaveTriggers(level, piece, scan, failures);

        BlockPos landing = WitchWellArrival.findLanding(level, start);
        BlockPos target = WitchWellArrival.traversalTarget(piece);
        WitchWellArrival.TraversalResult approachRoute = null;
        WitchWellArrival.TraversalResult lowerRoute = null;
        WitchWellArrival.TraversalResult route;
        if (variant == WitchWellPiece.Variant.CAVE) {
            WitchWellArrival.ViaTraversalResult via = WitchWellArrival.traversalViaSegments(
                    level, landing, anchor, target, box);
            approachRoute = via.approach();
            lowerRoute = via.lower();
            route = via.combined();
        } else {
            route = WitchWellArrival.traversal(level, landing, target, box);
        }
        int traversal = route.distance();
        if (landing == null || !WildStructureArrival.isSafeLanding(level, landing)) {
            failures.add("unsafe_landing");
        }
        if (traversal < 0) {
            failures.add("no_traversal");
        }

        String status = failures.isEmpty() ? "PASS" : "FAIL";
        String summary = "[WitchWellCertification] " + status
                + " variant=" + variant.serializedName()
                + " dimension=" + level.dimension().identifier()
                + " placement=natural"
                + " anchor=" + compact(anchor)
                + " box=" + box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan()
                + " authored=" + authored.matched + "/" + contract.cleanedBlockCount()
                + " surfaces=" + compactMap(surfacePalette)
                + " fluids=" + authored.fluidCells + "/" + contract.fluidCells()
                + " total_fluids=" + scan.fluids
                + " loot=" + loot
                + " warning=" + curse.warning
                + " curse=" + curse.curse
                + " ladders=" + curse.ladders
                + " landing=" + compact(landing)
                + " target=" + compact(target)
                + " traversal=" + traversal
                + " route=" + route.inspected() + "/" + route.visited()
                + "/" + compact(route.nearest())
                + (variant == WitchWellPiece.Variant.CAVE
                ? " approach_route=" + compactRoute(approachRoute)
                + " lower_route=" + compactRoute(lowerRoute)
                + (traversal < 0 ? " shaft=" + shaftDiagnostics(level, anchor, target, box) : "")
                : "")
                + " failures=" + (failures.isEmpty() ? "none" : String.join(",", failures));
        return new Report(failures.isEmpty(), summary, variant, anchor, landing, target, traversal);
    }

    private static AuthoredSignature checkAuthoredSignature(
            ServerLevel level, StructureTemplate template, BoundingBox box,
            WitchWellSourceContract.Contract contract, List<String> failures) {
        BlockPos origin = new BlockPos(box.minX(), box.minY(), box.minZ());
        Set<BlockPos> positions = new HashSet<>();
        int matched = 0;
        int frozenWater = 0;
        int fluidCells = 0;
        for (Map.Entry<String, Integer> expected : contract.blockCounts().entrySet()) {
            Identifier id = Identifier.parse(expected.getKey());
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block == null) {
                failures.add("missing_block_" + id.getPath());
                continue;
            }
            List<StructureTemplate.StructureBlockInfo> infos = template.filterBlocks(
                    origin, WitchWellPiece.makeSettings(), block);
            if (infos.size() != expected.getValue()) {
                failures.add("template_" + id.getPath() + "_" + infos.size());
            }
            boolean wrong = false;
            for (StructureTemplate.StructureBlockInfo info : infos) {
                positions.add(info.pos().immutable());
                Block actual = level.getBlockState(info.pos()).getBlock();
                if (actual == block) {
                    matched++;
                    if (!level.getFluidState(info.pos()).isEmpty()) {
                        fluidCells++;
                    }
                } else if (block == Blocks.WATER && actual == Blocks.ICE) {
                    matched++;
                    frozenWater++;
                    fluidCells++;
                } else {
                    wrong = true;
                }
            }
            if (wrong) {
                failures.add("authored_" + id.getPath());
            }
        }
        if (positions.size() != contract.cleanedBlockCount()) {
            failures.add("template_total_" + positions.size());
        }
        return new AuthoredSignature(matched, frozenWater, fluidCells, positions);
    }

    private static Map<String, Integer> checkSurface(ServerLevel level, WitchWellPiece piece,
                                                     Set<BlockPos> authored,
                                                     List<String> failures) {
        WitchWellPiece.Variant variant = piece.variant();
        BoundingBox box = piece.getBoundingBox();
        Map<String, Integer> palette = new HashMap<>();
        boolean badSurface = false;
        boolean badSupport = false;
        for (int x = variant.surfaceMinX(); x <= variant.surfaceMaxX(); x++) {
            for (int z = variant.surfaceMinZ(); z <= variant.surfaceMaxZ(); z++) {
                if (!variant.surfaceColumn(x, z)) {
                    continue;
                }
                BlockPos pos = local(box, x, variant.surfaceYOffset(), z);
                if (authored.contains(pos)) {
                    continue;
                }
                String id = blockId(level, pos);
                palette.merge(id, 1, Integer::sum);
                if (!ADAPTIVE_SURFACES.contains(id) || !level.getFluidState(pos).isEmpty()) {
                    badSurface = true;
                }
                BlockPos below = pos.below();
                if (!variant.clearsCaveBackfillAt(x, variant.surfaceYOffset() - 1, z)
                        && (level.getBlockState(below).isAir() || !level.getFluidState(below).isEmpty())) {
                    badSupport = true;
                }
            }
        }
        if (badSurface) {
            failures.add("adaptive_surface");
        }
        if (badSupport) {
            failures.add("surface_support");
        }
        return palette;
    }

    private static void checkCenter(ServerLevel level, WitchWellPiece piece, List<String> failures) {
        WitchWellPiece.Variant variant = piece.variant();
        BoundingBox box = piece.getBoundingBox();
        BlockPos center = local(box, variant.surfaceAnchorX(), variant.surfaceYOffset(),
                variant.surfaceAnchorZ());
        boolean valid = switch (variant) {
            case REGULAR, CAVE -> level.getBlockState(center).is(Blocks.WATER)
                    || level.getBlockState(center).is(Blocks.ICE);
            case SMALL -> level.getBlockState(center).is(Blocks.CAULDRON);
        };
        if (!valid) {
            failures.add("center_" + blockId(level, center).replace(':', '_'));
        }
    }

    private static String checkLoot(ServerLevel level, WitchWellPiece piece, Scan scan,
                                    List<String> failures) {
        WitchWellPiece.Variant variant = piece.variant();
        if (variant == WitchWellPiece.Variant.SMALL) {
            if (!scan.lootTables.isEmpty() || scan.count("minecraft:chest") != 0
                    || scan.count("minecraft:dispenser") != 0) {
                failures.add("unexpected_loot");
            }
            return "none";
        }
        BoundingBox box = piece.getBoundingBox();
        BlockPos pos = local(box, variant.lootLocalX(), variant.lootLocalY(), variant.lootLocalZ());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        String actual = "none";
        if (blockEntity != null) {
            actual = blockEntity.saveWithoutMetadata(level.registryAccess())
                    .getString("LootTable").orElse("none");
        }
        if (!LOOT_TABLE.equals(actual) || !scan.lootTables.equals(Set.of(LOOT_TABLE))) {
            failures.add("loot_binding");
        }
        return actual;
    }

    private static CurseState checkCaveTriggers(ServerLevel level, WitchWellPiece piece,
                                                Scan scan, List<String> failures) {
        if (piece.variant() != WitchWellPiece.Variant.CAVE) {
            return new CurseState("none", "none", "none");
        }
        List<BlockPos> ladders = scan.ladders.stream()
                .sorted(Comparator.comparingInt(BlockPos::getY)).toList();
        if (ladders.size() != 3 || ladders.get(0).getX() != ladders.get(2).getX()
                || ladders.get(0).getZ() != ladders.get(2).getZ()
                || ladders.get(1).getY() != ladders.get(0).getY() + 1
                || ladders.get(2).getY() != ladders.get(1).getY() + 1) {
            failures.add("ladder_span");
            return new CurseState("missing", "missing", compactList(ladders));
        }
        BlockPos bottom = ladders.getFirst();
        BlockPos top = ladders.getLast();
        CursedBlockWarningData.Warning warning = CursedBlockWarningData.get(level).get(top);
        CursedBlockData.Curse curse = CursedBlockData.get(level).get(bottom);
        boolean warningValid = warning != null
                && WitchWellPiece.CAVE_ENTRY_WARNING.equals(warning.message())
                && warning.cooldownTicks() == WitchWellPiece.CAVE_ENTRY_WARNING_COOLDOWN_TICKS;
        boolean curseValid = curse != null && curse.charges() == 1
                && curse.brew().items().equals(WitchWellPiece.CAVE_ENTRY_CURSE.items());
        if (!warningValid || CursedBlockData.get(level).get(top) != null) {
            failures.add("top_warning");
        }
        if (!curseValid || CursedBlockWarningData.get(level).get(bottom) != null) {
            failures.add("bottom_curse");
        }
        return new CurseState(warningValid ? "ok" : "bad", curseValid ? "ok" : "bad",
                compact(bottom) + ".." + compact(top));
    }

    private static Scan scan(ServerLevel level, BoundingBox box) {
        Scan scan = new Scan();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    String id = blockId(level, pos);
                    scan.blocks.merge(id, 1, Integer::sum);
                    if (!level.getFluidState(pos).isEmpty()) {
                        scan.fluids++;
                    }
                    if (level.getBlockState(pos).is(Blocks.LADDER)) {
                        scan.ladders.add(pos.immutable());
                    }
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity == null) {
                        continue;
                    }
                    scan.blockEntities++;
                    CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
                    tag.getString("LootTable").filter(value -> !value.isBlank())
                            .ifPresent(scan.lootTables::add);
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

    private static WitchWellPiece.Variant parseVariant(String value) {
        if ("any".equals(value)) {
            return null;
        }
        for (WitchWellPiece.Variant variant : WitchWellPiece.Variant.values()) {
            if (variant.serializedName().equals(value)) {
                return variant;
            }
        }
        return null;
    }

    private static BlockPos local(BoundingBox box, int x, int y, int z) {
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    private static String blockId(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private static String compact(BlockPos pos) {
        return pos == null ? "none" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String compactList(List<BlockPos> positions) {
        return positions.isEmpty() ? "none" : positions.stream()
                .map(WitchWellCertification::compact).reduce((a, b) -> a + ";" + b).orElse("none");
    }

    private static String compactMap(Map<String, Integer> values) {
        return values.isEmpty() ? "none" : values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .reduce((a, b) -> a + ";" + b).orElse("none");
    }

    private static String compactRoute(WitchWellArrival.TraversalResult route) {
        return route == null ? "not_run" : route.distance() + "/" + route.inspected()
                + "/" + route.visited() + "/" + compact(route.nearest());
    }

    private static String shaftDiagnostics(ServerLevel level, BlockPos anchor, BlockPos target,
                                           BoundingBox box) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dy = 0; dy >= -6; dy--) {
            positions.add(anchor.offset(0, dy, 0));
        }
        positions.add(target);
        String states = positions.stream().map(pos -> compact(pos) + ":" + blockId(level, pos)
                        + ":" + (WitchWellArrival.canOccupy(level, pos) ? "open" : "blocked"))
                .reduce((a, b) -> a + ";" + b).orElse("none");
        String depths = positions.subList(1, 7).stream()
                .map(pos -> pos.getY() + "=" + WitchWellArrival.traversal(level, anchor, pos, box).distance())
                .reduce((a, b) -> a + "," + b).orElse("none");
        int bottomToTarget = WitchWellArrival.traversal(
                level, positions.get(6), target, box).distance();
        return states + "/depths=" + depths + "/bottom_to_target=" + bottomToTarget;
    }

    private static Report fail(String variant, String reason) {
        return new Report(false, "[WitchWellCertification] FAIL variant=" + variant
                + " failures=" + reason, null, null, null, null, -1);
    }

    public record Report(boolean passed, String summary, WitchWellPiece.Variant variant,
                         BlockPos anchor, BlockPos landing, BlockPos target,
                         int traversalDistance) {}

    private record AuthoredSignature(int matched, int frozenWater, int fluidCells,
                                     Set<BlockPos> positions) {}

    private record CurseState(String warning, String curse, String ladders) {}

    private static final class Scan {
        private final Map<String, Integer> blocks = new HashMap<>();
        private final Set<String> lootTables = new HashSet<>();
        private final Set<String> spawnerTypes = new HashSet<>();
        private final List<BlockPos> ladders = new ArrayList<>();
        private int fluids;
        private int blockEntities;

        private int count(String id) {
            return blocks.getOrDefault(id, 0);
        }
    }
}
