package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.CursedBlockData;
import com.emoniph.witchery.brewing.CursedBlockWarningData;
import com.emoniph.witchery.init.WitcheryStructures;
import com.emoniph.witchery.world.WitcheryStructureData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

public class WitchWellPiece extends TemplateStructurePiece {
    private static final String VARIANT_TAG = "WitcheryWellVariant";
    private static final int SURFACE_SAMPLE_RADIUS = 12;
    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "chests/witch_well"));
    private static final BrewContents CAVE_ENTRY_CURSE = new BrewContents(
            List.of(Items.SPIDER_EYE, Items.ZOMBIE_HEAD));
    private static final String CAVE_ENTRY_WARNING = "You feel uneasy.";
    private static final int CAVE_ENTRY_WARNING_COOLDOWN_TICKS = 100;

    private final Variant variant;

    public WitchWellPiece(StructureTemplateManager structureTemplateManager,
                          Variant variant, int x, int y, int z) {
        super(WitcheryStructures.WITCH_WELL_PIECE.get(), 0, structureTemplateManager,
                variant.template(), variant.template().toString(), makeSettings(), new BlockPos(x, y, z));
        this.variant = variant;
    }

    public WitchWellPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.WITCH_WELL_PIECE.get(), tag,
                ctx.structureTemplateManager(), template -> makeSettings());
        this.variant = Variant.bySerializedName(tag.getStringOr(VARIANT_TAG, Variant.REGULAR.serializedName()));
    }

    public Variant variant() {
        return variant;
    }

    public BlockPos indexedTarget() {
        return templateWorldPos(variant.surfaceAnchorX(), variant.surfaceYOffset() + 1,
                variant.surfaceAnchorZ());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putString(VARIANT_TAG, variant.serializedName());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {
        clearSurfaceClutter(level, box);
        if (variant == Variant.CAVE) {
            clearCaveInterior(level, box);
        }
        placeAdaptiveSurface(level, box);

        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);

        if (variant.adaptsTemplateSurfaceAfterPlacement()) {
            placeAdaptiveSurface(level, box);
        }
        if (variant == Variant.CAVE) {
            fillCaveExteriorBackfill(level, box);
            clearCaveInteriorBackfill(level, box);
            seedCaveEntryLadders(level, box);
        }
        placeLoot(level, box, random);
        recordStructure(level);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        WitcheryMod.LOGGER.warn("[WitchWell] Unknown template marker '{}' at {}", name, pos);
    }

    private void clearSurfaceClutter(WorldGenLevel level, BoundingBox box) {
        for (int localX = variant.surfaceMinX(); localX <= variant.surfaceMaxX(); localX++) {
            for (int localZ = variant.surfaceMinZ(); localZ <= variant.surfaceMaxZ(); localZ++) {
                if (!variant.surfaceColumn(localX, localZ)) {
                    continue;
                }
                for (int localY = variant.surfaceYOffset(); localY <= variant.surfaceYOffset() + 8; localY++) {
                    BlockPos pos = templateWorldPos(localX, localY, localZ);
                    if (box.isInside(pos) && isClutter(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void clearCaveInterior(WorldGenLevel level, BoundingBox box) {
        for (int x = variant.surfaceMinX(); x <= variant.surfaceMaxX(); x++) {
            for (int z = variant.surfaceMinZ(); z <= variant.surfaceMaxZ(); z++) {
                for (int y = 3; y <= 18; y++) {
                    if (!variant.clearsCaveBackfillAt(x, y, z)) {
                        continue;
                    }
                    BlockPos pos = templateWorldPos(x, y, z);
                    if (box.isInside(pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void clearCaveInteriorBackfill(WorldGenLevel level, BoundingBox box) {
        for (int x = variant.surfaceMinX(); x <= variant.surfaceMaxX(); x++) {
            for (int z = variant.surfaceMinZ(); z <= variant.surfaceMaxZ(); z++) {
                for (int y = 3; y <= 18; y++) {
                    if (!variant.clearsCaveBackfillAt(x, y, z)) {
                        continue;
                    }
                    BlockPos pos = templateWorldPos(x, y, z);
                    if (box.isInside(pos) && isCaveTerrainBackfill(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void fillCaveExteriorBackfill(WorldGenLevel level, BoundingBox box) {
        for (int x = variant.surfaceMinX(); x <= variant.surfaceMaxX(); x++) {
            for (int z = variant.surfaceMinZ(); z <= variant.surfaceMaxZ(); z++) {
                BlockPos surfacePos = templateWorldPos(x, variant.surfaceYOffset(), z);
                BlockState support = supportState(adaptiveSurfaceState(level, surfacePos));
                for (int y = variant.surfaceYOffset() - 1;
                     y >= variant.surfaceYOffset() - variant.surfaceFillDepth();
                     y--) {
                    if (variant.clearsCaveBackfillAt(x, y, z)) {
                        continue;
                    }
                    BlockPos pos = templateWorldPos(x, y, z);
                    if (box.isInside(pos) && isReplaceableByStructures(level.getBlockState(pos))) {
                        level.setBlock(pos, support, 2);
                    }
                }
            }
        }
    }

    private void placeAdaptiveSurface(WorldGenLevel level, BoundingBox box) {
        int y = variant.surfaceYOffset();
        for (int localX = variant.surfaceMinX(); localX <= variant.surfaceMaxX(); localX++) {
            for (int localZ = variant.surfaceMinZ(); localZ <= variant.surfaceMaxZ(); localZ++) {
                if (!variant.surfaceColumn(localX, localZ)) {
                    continue;
                }
                BlockPos pos = templateWorldPos(localX, y, localZ);
                if (!box.isInside(pos)) {
                    continue;
                }
                BlockState surface = adaptiveSurfaceState(level, pos);
                level.setBlock(pos, surface, 3);
                fillColumnDownAt(level, box, pos.below(), supportState(surface), variant.surfaceFillDepth());
            }
        }
    }

    private void fillColumnDownAt(WorldGenLevel level, BoundingBox box, BlockPos pos,
                                  BlockState state, int maxDepth) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int filled = 0;
        while (box.isInside(cursor)
                && filled < maxDepth
                && isReplaceableByStructures(level.getBlockState(cursor))
                && cursor.getY() > level.getMinY() + 1) {
            level.setBlock(cursor, state, 2);
            cursor.move(Direction.DOWN);
            filled++;
        }
    }

    private void placeLoot(WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockPos lootPos = templateWorldPos(variant.lootLocalX(), variant.lootLocalY(), variant.lootLocalZ());
        if (!box.isInside(lootPos)) {
            return;
        }
        if (level.getBlockEntity(lootPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(LOOT_TABLE, random.nextLong());
            chest.setChanged();
        } else if (level.getBlockEntity(lootPos) instanceof DispenserBlockEntity dispenser) {
            dispenser.setLootTable(LOOT_TABLE, random.nextLong());
            dispenser.setChanged();
        }
    }

    private void seedCaveEntryLadders(WorldGenLevel level, BoundingBox box) {
        LadderSpan ladders = findCaveEntryLadders(level, box);
        if (ladders == null) {
            return;
        }
        ServerLevel serverLevel = level.getLevel();
        CursedBlockWarningData warnings = CursedBlockWarningData.get(serverLevel);
        if (warnings.get(ladders.top()) == null) {
            warnings.warn(ladders.top(), CAVE_ENTRY_WARNING, CAVE_ENTRY_WARNING_COOLDOWN_TICKS);
        }

        BlockPos bottom = ladders.bottom();
        if (!bottom.equals(ladders.top())) {
            CursedBlockData curses = CursedBlockData.get(serverLevel);
            if (curses.get(bottom) == null) {
                curses.curse(bottom, CAVE_ENTRY_CURSE, null);
            }
        }
    }

    private LadderSpan findCaveEntryLadders(WorldGenLevel level, BoundingBox box) {
        BlockPos top = findTopCaveLadder(level, box);
        if (top == null) {
            return null;
        }
        BlockPos bottom = findLowestLadderInColumn(level, box, top.getX(), top.getZ());
        if (bottom.equals(top)) {
            bottom = findLowestCaveLadder(level, box);
        }
        return new LadderSpan(top, bottom);
    }

    private BlockPos findTopCaveLadder(WorldGenLevel level, BoundingBox box) {
        BlockPos anchor = indexedTarget();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int y = box.maxY(); y >= box.minY(); y--) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!level.getBlockState(candidate).is(Blocks.LADDER)) {
                        continue;
                    }
                    long dx = candidate.getX() - anchor.getX();
                    long dz = candidate.getZ() - anchor.getZ();
                    long distance = dx * dx + dz * dz;
                    if (best == null || distance < bestDistance) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private BlockPos findLowestLadderInColumn(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockPos best = null;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (level.getBlockState(candidate).is(Blocks.LADDER)) {
                best = candidate.immutable();
                break;
            }
        }
        return best == null ? new BlockPos(x, box.minY(), z) : best;
    }

    private BlockPos findLowestCaveLadder(WorldGenLevel level, BoundingBox box) {
        BlockPos anchor = indexedTarget();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!level.getBlockState(candidate).is(Blocks.LADDER)) {
                        continue;
                    }
                    long dx = candidate.getX() - anchor.getX();
                    long dz = candidate.getZ() - anchor.getZ();
                    long distance = dx * dx + dz * dz;
                    if (best == null || distance < bestDistance) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return anchor;
    }

    private record LadderSpan(BlockPos top, BlockPos bottom) {}

    private void recordStructure(WorldGenLevel level) {
        ServerLevel serverLevel = level.getLevel();
        WitcheryStructureData.get(serverLevel).record(serverLevel, WitcheryStructureData.WITCH_WELL,
                variant.serializedName(), indexedTarget());
    }

    private static boolean isClutter(BlockState state) {
        return state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS);
    }

    private static BlockState adaptiveSurfaceState(ServerLevelAccessor level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        BlockState normalized = normalizeSurfaceState(existing);
        if (normalized != null) {
            return normalized;
        }

        normalized = nearbySurfaceState(level, pos, SURFACE_SAMPLE_RADIUS);
        if (normalized != null) {
            return normalized;
        }
        BlockPos ground = findGroundAtOrBelow(level, pos);
        if (ground != null) {
            normalized = normalizeSurfaceState(level.getBlockState(ground));
            if (normalized != null) {
                return normalized;
            }
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static BlockState nearbySurfaceState(ServerLevelAccessor level, BlockPos center, int maxRadius) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int dy = 1; dy >= -1; dy--) {
                        BlockState normalized = normalizeSurfaceState(
                                level.getBlockState(center.offset(dx, dy, dz)));
                        if (normalized != null) {
                            return normalized;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static BlockState normalizeSurfaceState(BlockState state) {
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)) {
            return state;
        }
        return null;
    }

    private static boolean isCaveTerrainBackfill(BlockState state) {
        return normalizeSurfaceState(state) != null
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.DIRT);
    }

    private static BlockState supportState(BlockState surface) {
        if (surface.is(Blocks.MUD)) {
            return Blocks.MUD.defaultBlockState();
        }
        if (surface.is(Blocks.CLAY)) {
            return Blocks.CLAY.defaultBlockState();
        }
        if (surface.is(Blocks.SAND) || surface.is(Blocks.RED_SAND) || surface.is(Blocks.GRAVEL)) {
            return surface;
        }
        return Blocks.DIRT.defaultBlockState();
    }

    private static BlockPos findGroundAtOrBelow(ServerLevelAccessor level, BlockPos start) {
        int y = start.getY();
        while (y > level.getMinY()) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()) {
                return pos;
            }
            y--;
        }
        return null;
    }

    private BlockPos templateWorldPos(int localX, int localY, int localZ) {
        return StructureTemplate.calculateRelativePosition(
                placeSettings, new BlockPos(localX, localY, localZ)).offset(templatePosition);
    }

    public enum Variant {
        REGULAR("regular", "witch_well/well", 8, 6,
                0, 14, 0, 13, 1, 4,
                8, 0, 6),
        SMALL("small", "witch_well/small_well", 4, 5,
                0, 8, 1, 9, 0, 4,
                4, 0, 5),
        CAVE("cave", "witch_well/well_cave", 14, 10,
                7, 26, 3, 17, 12, 10,
                22, 8, 15);

        private final String serializedName;
        private final Identifier template;
        private final int surfaceAnchorX;
        private final int surfaceAnchorZ;
        private final int surfaceMinX;
        private final int surfaceMaxX;
        private final int surfaceMinZ;
        private final int surfaceMaxZ;
        private final int surfaceYOffset;
        private final int surfaceFillDepth;
        private final int lootLocalX;
        private final int lootLocalY;
        private final int lootLocalZ;

        Variant(String serializedName, String templatePath,
                int surfaceAnchorX, int surfaceAnchorZ,
                int surfaceMinX, int surfaceMaxX, int surfaceMinZ, int surfaceMaxZ,
                int surfaceYOffset, int surfaceFillDepth,
                int lootLocalX, int lootLocalY, int lootLocalZ) {
            this.serializedName = serializedName;
            this.template = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, templatePath);
            this.surfaceAnchorX = surfaceAnchorX;
            this.surfaceAnchorZ = surfaceAnchorZ;
            this.surfaceMinX = surfaceMinX;
            this.surfaceMaxX = surfaceMaxX;
            this.surfaceMinZ = surfaceMinZ;
            this.surfaceMaxZ = surfaceMaxZ;
            this.surfaceYOffset = surfaceYOffset;
            this.surfaceFillDepth = surfaceFillDepth;
            this.lootLocalX = lootLocalX;
            this.lootLocalY = lootLocalY;
            this.lootLocalZ = lootLocalZ;
        }

        public static Variant bySerializedName(String name) {
            for (Variant variant : values()) {
                if (variant.serializedName.equals(name)) {
                    return variant;
                }
            }
            return REGULAR;
        }

        Identifier template() {
            return template;
        }

        public String serializedName() {
            return serializedName;
        }

        public int surfaceAnchorX() {
            return surfaceAnchorX;
        }

        public int surfaceAnchorZ() {
            return surfaceAnchorZ;
        }

        public int surfaceMinX() {
            return surfaceMinX;
        }

        public int surfaceMaxX() {
            return surfaceMaxX;
        }

        public int surfaceMinZ() {
            return surfaceMinZ;
        }

        public int surfaceMaxZ() {
            return surfaceMaxZ;
        }

        public int surfaceYOffset() {
            return surfaceYOffset;
        }

        boolean surfaceColumn(int localX, int localZ) {
            if (this == SMALL) {
                return smallWellSurfaceMarker(localX, localZ);
            }
            if (this != CAVE) {
                return true;
            }
            double centerX = (surfaceMinX + surfaceMaxX) / 2.0D;
            double centerZ = (surfaceMinZ + surfaceMaxZ) / 2.0D;
            double radiusX = (surfaceMaxX - surfaceMinX + 1) / 2.0D;
            double radiusZ = (surfaceMaxZ - surfaceMinZ + 1) / 2.0D;
            double dx = (localX - centerX) / radiusX;
            double dz = (localZ - centerZ) / radiusZ;
            return dx * dx + dz * dz <= 1.0D;
        }

        boolean heightSampleColumn(int localX, int localZ) {
            return this != SMALL || smallWellDirtRing(localX, localZ);
        }

        boolean adaptsTemplateSurfaceAfterPlacement() {
            return this == SMALL;
        }

        boolean clearsCaveBackfillAt(int localX, int localY, int localZ) {
            return this == CAVE
                    && (inBox(localX, localY, localZ, 13, 10, 9, 15, 11, 11)
                    || inBox(localX, localY, localZ, 12, 7, 8, 16, 10, 12)
                    || inBox(localX, localY, localZ, 13, 13, 9, 15, 18, 11)
                    || inBox(localX, localY, localZ, 17, 6, 7, 22, 11, 13)
                    || inBox(localX, localY, localZ, 20, 5, 5, 25, 11, 15)
                    || inBox(localX, localY, localZ, 9, 13, 5, 19, 16, 15)
                    || inBox(localX, localY, localZ, 20, 13, 4, 26, 18, 16));
        }

        private static boolean inBox(int x, int y, int z,
                                     int minX, int minY, int minZ,
                                     int maxX, int maxY, int maxZ) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        private static boolean smallWellSurfaceMarker(int x, int z) {
            if ((x == 3 && z == 4) || (x == 4 && z == 5) || (x == 5 && z == 6)) {
                return false;
            }
            int dx = x - 4;
            int dz = z - 5;
            return dx * dx + dz * dz <= 18;
        }

        private static boolean smallWellDirtRing(int x, int z) {
            return (x == 3 && (z == 5 || z == 6))
                    || (x == 4 && (z == 4 || z == 6))
                    || (x == 5 && (z == 4 || z == 5));
        }

        int surfaceFillDepth() {
            return surfaceFillDepth;
        }

        int lootLocalX() {
            return lootLocalX;
        }

        int lootLocalY() {
            return lootLocalY;
        }

        int lootLocalZ() {
            return lootLocalZ;
        }
    }
}
