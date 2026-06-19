package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
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

public class WitchWellPiece extends TemplateStructurePiece {
    private static final String VARIANT_TAG = "WitcheryWellVariant";
    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "chests/witch_well"));

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
        clearLocalBox(level, box, 12, 5, 6, 24, 12, 14);
        clearLocalBox(level, box, 13, 11, 8, 17, 16, 12);
    }

    private void clearLocalBox(WorldGenLevel level, BoundingBox box,
                               int minX, int minY, int minZ,
                               int maxX, int maxY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = templateWorldPos(x, y, z);
                    if (box.isInside(pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void placeAdaptiveSurface(WorldGenLevel level, BoundingBox box) {
        int y = variant.surfaceYOffset();
        for (int localX = variant.surfaceMinX(); localX <= variant.surfaceMaxX(); localX++) {
            for (int localZ = variant.surfaceMinZ(); localZ <= variant.surfaceMaxZ(); localZ++) {
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

        BlockPos ground = findGroundAtOrBelow(level, pos);
        if (ground != null) {
            normalized = normalizeSurfaceState(level.getBlockState(ground));
            if (normalized != null) {
                return normalized;
            }
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
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
        CAVE("cave", "witch_well/well_cave", 14, 10,
                7, 21, 3, 17, 15, 2,
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
