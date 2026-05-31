package com.emoniph.witchery.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
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

final class VillageBuildingMaterials {
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

    private VillageBuildingMaterials(BlockState foundation, BlockState floor, BlockState wall,
                                     BlockState trim, BlockState roofSolid, Block roofStairs,
                                     Block stepStairs, Block roofSlab, BlockState parapet, Block door) {
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

    static VillageBuildingMaterials forVillage(WorldGenLevel level, BoundingBox box) {
        BlockPos center = new BlockPos((box.minX() + box.maxX()) / 2, 64, (box.minZ() + box.maxZ()) / 2);
        var biome = level.getBiome(center);

        if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
            return new VillageBuildingMaterials(
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

        if (biome.is(Biomes.SAVANNA) || biome.is(Biomes.SAVANNA_PLATEAU)
                || biome.is(Biomes.WINDSWEPT_SAVANNA)) {
            return new VillageBuildingMaterials(
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

        if (biome.is(Biomes.TAIGA) || biome.is(Biomes.SNOWY_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES)) {
            return new VillageBuildingMaterials(
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
