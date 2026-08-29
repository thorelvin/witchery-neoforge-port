package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * NBT-backed Straw Man / Wicker Man. The template owns the hay silhouette and air clearing,
 * while DATA markers keep biome-sensitive ground, plants, and the hidden spawner in code.
 */
public class StrawManTemplatePiece extends TemplateStructurePiece {
    static final Identifier BODY_X_TEMPLATE = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "straw_man/body_x");
    static final Identifier BODY_Z_TEMPLATE = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "straw_man/body_z");

    private static final String MARKER_GROUND = "witchery:ground/adaptive";
    private static final String MARKER_PLANT = "witchery:plant/straw_man";
    private static final String MARKER_SPAWNER = "witchery:spawner/straw_man";

    private static final int FOOTPRINT_WIDTH = 6;
    private static final int FOOTPRINT_DEPTH = 5;

    public StrawManTemplatePiece(StructureTemplateManager structureTemplateManager,
                                 Identifier template, int x, int y, int z) {
        super(WitcheryStructures.STRAW_MAN_TEMPLATE_PIECE.get(), 0, structureTemplateManager,
                template, template.toString(), makeSettings(), new BlockPos(x, y, z));
    }

    public StrawManTemplatePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.STRAW_MAN_TEMPLATE_PIECE.get(), tag,
                ctx.structureTemplateManager(), template -> makeSettings());
    }

    static Identifier randomTemplate(RandomSource random) {
        return random.nextBoolean() ? BODY_X_TEMPLATE : BODY_Z_TEMPLATE;
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {
        if (!hasDryLandFootprint(level)) {
            return;
        }

        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);

        int centerX = templatePosition.getX() + 2;
        int centerZ = templatePosition.getZ() + 2;
        boolean sandGround = isSandGroundBiome(level, centerX, centerZ);
        BlockState fallbackUnderground = sandGround ? Blocks.SAND.defaultBlockState()
                                                    : Blocks.DIRT.defaultBlockState();
        for (int x = 0; x < FOOTPRINT_WIDTH; x++) {
            for (int z = 0; z < FOOTPRINT_DEPTH; z++) {
                if (isGroundFootprint(x, z)) {
                    BlockPos supportStart = templateWorldPos(x, -1, z);
                    fillColumnDownAt(level, box, supportStart,
                            embeddedGroundState(level, supportStart, sandGround, fallbackUnderground));
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        if (!box.isInside(pos)) {
            return;
        }

        switch (name) {
            case MARKER_GROUND -> level.setBlock(pos,
                    adaptiveFootprintGroundState(level, pos),
                    3);
            case MARKER_PLANT -> level.setBlock(pos,
                    plantStateForGround(level.getBlockState(pos.below()),
                            isDryShrubBiome(level, pos.getX(), pos.getZ())),
                    3);
            case MARKER_SPAWNER -> placeSpawner(level, random, pos);
            default -> WitcheryMod.LOGGER.warn("[StrawMan] Unknown template marker '{}' at {}", name, pos);
        }
    }

    private static void placeSpawner(ServerLevelAccessor level, RandomSource random, BlockPos pos) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(random.nextInt(3) == 0 ? EntityType.SKELETON : EntityType.ZOMBIE, random);
            spawner.setChanged();
        }
    }

    private boolean hasDryLandFootprint(WorldGenLevel level) {
        for (int x = 0; x < FOOTPRINT_WIDTH; x++) {
            for (int z = 0; z < FOOTPRINT_DEPTH; z++) {
                BlockPos column = templateWorldPos(x, 0, z);
                int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, column.getX(), column.getZ());
                int baseY = templatePosition.getY();
                for (int y = baseY - 1; y <= baseY + 1; y++) {
                    if (!level.getFluidState(new BlockPos(column.getX(), y, column.getZ())).isEmpty()) {
                        return false;
                    }
                }
                if (!level.getFluidState(new BlockPos(column.getX(), worldSurface - 1, column.getZ())).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void fillColumnDownAt(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        if (!box.isInside(cursor)) {
            return;
        }
        while (isReplaceableByStructures(level.getBlockState(cursor))
                && cursor.getY() > level.getMinY() + 1) {
            level.setBlock(cursor, state, 2);
            cursor.move(Direction.DOWN);
        }
    }

    private BlockPos templateWorldPos(int localX, int localY, int localZ) {
        return StructureTemplate.calculateRelativePosition(
                placeSettings, new BlockPos(localX, localY, localZ)).offset(templatePosition);
    }

    private static boolean isGroundFootprint(int x, int z) {
        return (x >= 1 && x <= 4) || z == 2;
    }

    static boolean isGroundFootprintForCertification(int x, int z) {
        return isGroundFootprint(x, z);
    }

    private static BlockState adaptiveFootprintGroundState(ServerLevelAccessor level, BlockPos pos) {
        boolean forceSand = isSandGroundBiome(level, pos.getX(), pos.getZ());
        BlockState normalized = normalizeFootprintGroundState(level.getBlockState(pos), forceSand);
        if (normalized != null) {
            return normalized;
        }

        BlockPos groundPos = findGroundAtOrBelow(level, pos);
        if (groundPos != null) {
            normalized = normalizeFootprintGroundState(level.getBlockState(groundPos), forceSand);
            if (normalized != null) {
                return normalized;
            }
        }

        return forceSand ? Blocks.SAND.defaultBlockState()
                         : Blocks.GRASS_BLOCK.defaultBlockState();
    }

    static BlockState normalizeFootprintGroundState(BlockState state, boolean forceSand) {
        if (forceSand) {
            return Blocks.SAND.defaultBlockState();
        }
        if (!isUsableGroundState(state)) {
            return null;
        }
        if (isPlainDirtState(state)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (isNaturalTopState(state)) {
            return state;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    static BlockState plantStateForGround(BlockState ground, boolean dryShrubBiome) {
        if (dryShrubBiome) {
            return canHostDeadBush(ground) ? Blocks.DEAD_BUSH.defaultBlockState()
                                          : Blocks.AIR.defaultBlockState();
        }
        if (ground.is(Blocks.GRASS_BLOCK)) {
            return Blocks.DANDELION.defaultBlockState();
        }
        if (ground.is(Blocks.PODZOL)
                || ground.is(Blocks.COARSE_DIRT)
                || ground.is(Blocks.ROOTED_DIRT)
                || ground.is(Blocks.MOSS_BLOCK)
                || ground.is(Blocks.MYCELIUM)) {
            return Blocks.FERN.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState embeddedGroundState(ServerLevelAccessor level, BlockPos pos,
                                                  boolean forceSand, BlockState fallback) {
        if (forceSand) {
            return Blocks.SAND.defaultBlockState();
        }

        BlockState existing = level.getBlockState(pos);
        if (isUsableGroundState(existing)) {
            return existing;
        }

        BlockPos groundPos = findGroundAtOrBelow(level, pos);
        if (groundPos == null) {
            return fallback;
        }

        BlockState ground = level.getBlockState(groundPos);
        return isUsableGroundState(ground) ? ground : fallback;
    }

    private static BlockPos findGroundAtOrBelow(ServerLevelAccessor level, BlockPos start) {
        int y = start.getY();
        int minY = level.getMinY();
        while (y > minY) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            BlockState state = level.getBlockState(pos);
            if (isUsableGroundState(state)) {
                return pos;
            }
            y--;
        }
        return null;
    }

    private static boolean isUsableGroundState(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion();
    }

    private static boolean isPlainDirtState(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT);
    }

    private static boolean isNaturalTopState(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.TERRACOTTA)
                || state.is(Blocks.WHITE_TERRACOTTA)
                || state.is(Blocks.ORANGE_TERRACOTTA)
                || state.is(Blocks.YELLOW_TERRACOTTA)
                || state.is(Blocks.LIGHT_GRAY_TERRACOTTA)
                || state.is(Blocks.BROWN_TERRACOTTA)
                || state.is(Blocks.RED_TERRACOTTA);
    }

    private static boolean canHostDeadBush(BlockState ground) {
        return ground.is(Blocks.SAND)
                || ground.is(Blocks.RED_SAND)
                || ground.is(Blocks.TERRACOTTA)
                || ground.is(Blocks.WHITE_TERRACOTTA)
                || ground.is(Blocks.ORANGE_TERRACOTTA)
                || ground.is(Blocks.YELLOW_TERRACOTTA)
                || ground.is(Blocks.LIGHT_GRAY_TERRACOTTA)
                || ground.is(Blocks.BROWN_TERRACOTTA)
                || ground.is(Blocks.RED_TERRACOTTA)
                || ground.is(Blocks.DIRT)
                || ground.is(Blocks.COARSE_DIRT)
                || ground.is(Blocks.PODZOL);
    }

    private static boolean isSandGroundBiome(ServerLevelAccessor level, int x, int z) {
        return level.getBiome(new BlockPos(x, 64, z)).is(Biomes.DESERT);
    }

    private static boolean isDryShrubBiome(ServerLevelAccessor level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
        return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
    }
}
