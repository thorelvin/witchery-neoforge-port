package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.events.VillageWallEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

final class VillageBuildingMaterials {
    enum VillageType {
        PLAINS,
        DESERT,
        SAVANNA,
        TAIGA,
        SNOWY
    }

    private final VillageType type;
    private final BlockState foundation;
    private final BlockState floor;
    private final BlockState wall;
    private final BlockState trim;
    private final BlockState roofSolid;
    private final Block roofStairs;
    private final Block stepStairs;
    private final Block roofSlab;
    private final BlockState parapet;
    private final Block door;

    private VillageBuildingMaterials(VillageType type, BlockState foundation, BlockState floor, BlockState wall,
                                     BlockState trim, BlockState roofSolid, Block roofStairs,
                                     Block stepStairs, Block roofSlab, BlockState parapet, Block door) {
        this.type = type;
        this.foundation = foundation;
        this.floor = floor;
        this.wall = wall;
        this.trim = trim;
        this.roofSolid = roofSolid;
        this.roofStairs = roofStairs;
        this.stepStairs = stepStairs;
        this.roofSlab = roofSlab;
        this.parapet = parapet;
        this.door = door;
    }

    static VillageBuildingMaterials forVillage(WorldGenLevel level, StructureManager structureMgr,
                                               BoundingBox pieceBox, BoundingBox generationBox) {
        VillageType type = resolveVillageType(level, structureMgr, pieceBox);
        if (type != null) {
            return forType(type);
        }
        return forVillage(level, pieceBox, generationBox);
    }

    static VillageBuildingMaterials forVillage(WorldGenLevel level, BoundingBox pieceBox, BoundingBox generationBox) {
        int minX = Math.max(pieceBox.minX(), generationBox.minX());
        int maxX = Math.min(pieceBox.maxX(), generationBox.maxX());
        int minZ = Math.max(pieceBox.minZ(), generationBox.minZ());
        int maxZ = Math.min(pieceBox.maxZ(), generationBox.maxZ());
        if (minX <= maxX && minZ <= maxZ) {
            BlockPos sample = new BlockPos((minX + maxX) / 2, 64, (minZ + maxZ) / 2);
            return forType(typeForSample(level, sample));
        }
        return forVillage(level, pieceBox);
    }

    static VillageBuildingMaterials forVillage(WorldGenLevel level, BoundingBox box) {
        BlockPos center = new BlockPos((box.minX() + box.maxX()) / 2, 64, (box.minZ() + box.maxZ()) / 2);
        return forType(typeForSample(level, center));
    }

    static VillageType resolveVillageType(ServerLevel level, StructureStart start) {
        VillageType type = typeForStructureId(level, start);
        if (type != null) {
            return type;
        }
        return typeForPoolPieces(start);
    }

    static VillageType typeForSample(ServerLevel level, int x, int z) {
        return typeForBiome(level.getBiome(new BlockPos(x, 64, z)));
    }

    private static VillageType resolveVillageType(WorldGenLevel level, StructureManager structureMgr, BoundingBox pieceBox) {
        StructureStart start = findVillageStart(level.getLevel(), structureMgr, pieceBox);
        return start != null ? resolveVillageType(level.getLevel(), start) : null;
    }

    private static StructureStart findVillageStart(ServerLevel level, StructureManager structureMgr, BoundingBox pieceBox) {
        StructureStart best = null;
        long bestDist = Long.MAX_VALUE;
        for (int chunkX = Math.floorDiv(pieceBox.minX(), 16); chunkX <= Math.floorDiv(pieceBox.maxX(), 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(pieceBox.minZ(), 16); chunkZ <= Math.floorDiv(pieceBox.maxZ(), 16); chunkZ++) {
                for (StructureStart start : structureMgr.startsForStructure(
                        new ChunkPos(chunkX, chunkZ), structure -> VillageWallEvents.isVillageStructure(structure, level))) {
                    if (start == null || !start.isValid() || !start.getBoundingBox().intersects(pieceBox)) continue;
                    long dist = distToCenterSqr(start.getBoundingBox(), pieceBox);
                    if (dist < bestDist) {
                        best = start;
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }

    private static VillageType typeForStructureId(ServerLevel level, StructureStart start) {
        var structure = start.getStructure();
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .listElements()
                .filter(entry -> entry.value() == structure)
                .findFirst();
        if (holder.isEmpty()) {
            return null;
        }
        var id = holder.get().key().identifier();
        return "minecraft".equals(id.getNamespace()) ? typeForVillagePath(id.getPath()) : null;
    }

    private static VillageType typeForPoolPieces(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                VillageType type = typeForVillagePath(poolPiece.getElement().toString());
                if (type != null) {
                    return type;
                }
            }
        }
        return null;
    }

    static VillageType typeForVillagePath(String path) {
        if (path.contains("village_desert") || path.contains("village/desert/")) return VillageType.DESERT;
        if (path.contains("village_savanna") || path.contains("village/savanna/")) return VillageType.SAVANNA;
        if (path.contains("village_taiga") || path.contains("village/taiga/")) return VillageType.TAIGA;
        if (path.contains("village_snowy") || path.contains("village/snowy/")) return VillageType.SNOWY;
        if (path.contains("village_plains") || path.contains("village/plains/")
                || path.contains("village_meadow") || path.contains("village/meadow/")) {
            return VillageType.PLAINS;
        }
        return null;
    }

    private static VillageType typeForSample(WorldGenLevel level, BlockPos center) {
        return typeForBiome(level.getBiome(center));
    }

    private static VillageType typeForBiome(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
            return VillageType.DESERT;
        }
        if (biome.is(Biomes.SAVANNA) || biome.is(Biomes.SAVANNA_PLATEAU)
                || biome.is(Biomes.WINDSWEPT_SAVANNA)) {
            return VillageType.SAVANNA;
        }
        if (biome.is(Biomes.TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            return VillageType.TAIGA;
        }
        if (biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES)) {
            return VillageType.SNOWY;
        }
        // 1.21.11 routes Meadow villages through the plains village style/tag.
        if (biome.is(Biomes.MEADOW)) {
            return VillageType.PLAINS;
        }
        return VillageType.PLAINS;
    }

    static VillageBuildingMaterials forType(VillageType type) {
        if (type == VillageType.DESERT) {
            return new VillageBuildingMaterials(
                    type,
                    Blocks.SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                    Blocks.CUT_SANDSTONE.defaultBlockState(),
                    Blocks.CHISELED_SANDSTONE.defaultBlockState(),
                    Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                    Blocks.SANDSTONE_STAIRS,
                    Blocks.SANDSTONE_STAIRS,
                    Blocks.SANDSTONE_SLAB,
                    Blocks.SANDSTONE_WALL.defaultBlockState(),
                    Blocks.OAK_DOOR);
        }

        if (type == VillageType.SAVANNA) {
            return new VillageBuildingMaterials(
                    type,
                    Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                    Blocks.ACACIA_PLANKS.defaultBlockState(),
                    Blocks.ACACIA_PLANKS.defaultBlockState(),
                    Blocks.ACACIA_LOG.defaultBlockState(),
                    Blocks.ACACIA_PLANKS.defaultBlockState(),
                    Blocks.ACACIA_STAIRS,
                    Blocks.ACACIA_STAIRS,
                    Blocks.ACACIA_SLAB,
                    Blocks.ACACIA_FENCE.defaultBlockState(),
                    Blocks.ACACIA_DOOR);
        }

        if (type == VillageType.TAIGA || type == VillageType.SNOWY) {
            return new VillageBuildingMaterials(
                    type,
                    Blocks.COBBLESTONE.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_LOG.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_STAIRS,
                    Blocks.COBBLESTONE_STAIRS,
                    Blocks.SPRUCE_SLAB,
                    Blocks.SPRUCE_FENCE.defaultBlockState(),
                    Blocks.SPRUCE_DOOR);
        }

        return new VillageBuildingMaterials(
                type,
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.OAK_STAIRS,
                Blocks.COBBLESTONE_STAIRS,
                Blocks.OAK_SLAB,
                Blocks.OAK_FENCE.defaultBlockState(),
                Blocks.OAK_DOOR);
    }

    private static long distToCenterSqr(BoundingBox startBox, BoundingBox pieceBox) {
        long startX = (startBox.minX() + startBox.maxX()) / 2L;
        long startZ = (startBox.minZ() + startBox.maxZ()) / 2L;
        long pieceX = (pieceBox.minX() + pieceBox.maxX()) / 2L;
        long pieceZ = (pieceBox.minZ() + pieceBox.maxZ()) / 2L;
        long dx = pieceX - startX;
        long dz = pieceZ - startZ;
        return dx * dx + dz * dz;
    }

    VillageType type() {
        return type;
    }

    boolean belongsToFamily(BlockState state) {
        Block block = state.getBlock();
        return block == foundation.getBlock()
                || block == floor.getBlock()
                || block == wall.getBlock()
                || block == trim.getBlock()
                || block == roofSolid.getBlock()
                || block == roofStairs
                || block == stepStairs
                || block == roofSlab
                || block == parapet.getBlock()
                || block == door;
    }

    BlockState foundation() {
        return foundation;
    }

    BlockState floor() {
        return floor;
    }

    BlockState wall() {
        return wall;
    }

    BlockState trim() {
        return trim;
    }

    BlockState trim(Direction.Axis axis) {
        return trim.hasProperty(BlockStateProperties.AXIS)
                ? trim.setValue(BlockStateProperties.AXIS, axis)
                : trim;
    }

    BlockState roofSolid() {
        return roofSolid;
    }

    BlockState roofStairs(Direction facing) {
        return roofStairs.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    BlockState stepStairs(Direction facing) {
        return stepStairs.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    BlockState roofSlabTop() {
        return roofSlab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    BlockState roofSlabBottom() {
        return roofSlab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    BlockState parapet() {
        return parapet;
    }

    BlockState door(Direction facing, DoubleBlockHalf half) {
        return door.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, half);
    }

    static BlockState paneEastWest() {
        return pane(false, true, false, true);
    }

    static BlockState paneNorthSouth() {
        return pane(true, false, true, false);
    }

    private static BlockState pane(boolean north, boolean east, boolean south, boolean west) {
        return Blocks.GLASS_PANE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, north)
                .setValue(BlockStateProperties.EAST, east)
                .setValue(BlockStateProperties.SOUTH, south)
                .setValue(BlockStateProperties.WEST, west);
    }
}
