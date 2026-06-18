package com.emoniph.witchery.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawManTemplateTest {
    private static final String TEMPLATE_RESOURCE_DIR = "data/witchery/structure/straw_man/";

    @Test
    void templatesHaveExpectedSizeAndMarkers() throws IOException {
        assertTemplateShape(readTemplate("body_x.nbt"));
        assertTemplateShape(readTemplate("body_z.nbt"));
    }

    @Test
    void templatesPreserveSourceHayAxisVariants() throws IOException {
        CompoundTag bodyX = readTemplate("body_x.nbt");
        CompoundTag bodyZ = readTemplate("body_z.nbt");

        assertEquals("minecraft:hay_block[axis=x]", blockStateAt(bodyX, 2, 1, 2));
        assertEquals("minecraft:hay_block[axis=z]", blockStateAt(bodyX, 1, 3, 2));
        assertEquals("minecraft:hay_block[axis=z]", blockStateAt(bodyZ, 2, 1, 2));
        assertEquals("minecraft:hay_block[axis=x]", blockStateAt(bodyZ, 1, 3, 2));
    }

    @Test
    void templatesKeepGroundAndSpawnerOnBaseLayer() throws IOException {
        assertMarkerAt(readTemplate("body_x.nbt"), "witchery:ground/adaptive", 1, 0, 0);
        assertMarkerAt(readTemplate("body_x.nbt"), "witchery:spawner/straw_man", 2, 0, 2);
        assertMarkerAt(readTemplate("body_z.nbt"), "witchery:ground/adaptive", 1, 0, 0);
        assertMarkerAt(readTemplate("body_z.nbt"), "witchery:spawner/straw_man", 2, 0, 2);
    }

    @Test
    void footprintGroundAdaptsToExistingSurface() {
        assertEquals(Blocks.GRASS_BLOCK.defaultBlockState(), StrawManTemplatePiece.normalizeFootprintGroundState(
                Blocks.DIRT.defaultBlockState(), false));
        assertEquals(Blocks.PODZOL.defaultBlockState(), StrawManTemplatePiece.normalizeFootprintGroundState(
                Blocks.PODZOL.defaultBlockState(), false));
        assertEquals(Blocks.GRAVEL.defaultBlockState(), StrawManTemplatePiece.normalizeFootprintGroundState(
                Blocks.GRAVEL.defaultBlockState(), false));
        assertEquals(Blocks.STONE.defaultBlockState(), StrawManTemplatePiece.normalizeFootprintGroundState(
                Blocks.STONE.defaultBlockState(), false));
        assertEquals(Blocks.SAND.defaultBlockState(), StrawManTemplatePiece.normalizeFootprintGroundState(
                Blocks.GRASS_BLOCK.defaultBlockState(), true));
    }

    @Test
    void plantsMatchFootprintGround() {
        assertEquals(Blocks.DANDELION.defaultBlockState(), StrawManTemplatePiece.plantStateForGround(
                Blocks.GRASS_BLOCK.defaultBlockState(), false));
        assertEquals(Blocks.FERN.defaultBlockState(), StrawManTemplatePiece.plantStateForGround(
                Blocks.PODZOL.defaultBlockState(), false));
        assertEquals(Blocks.AIR.defaultBlockState(), StrawManTemplatePiece.plantStateForGround(
                Blocks.GRAVEL.defaultBlockState(), false));
        assertEquals(Blocks.DEAD_BUSH.defaultBlockState(), StrawManTemplatePiece.plantStateForGround(
                Blocks.SAND.defaultBlockState(), true));
        assertEquals(Blocks.AIR.defaultBlockState(), StrawManTemplatePiece.plantStateForGround(
                Blocks.STONE.defaultBlockState(), true));
    }

    private static void assertTemplateShape(CompoundTag template) {
        ListTag size = list(template, "size");
        assertEquals(6, intAt(size, 0));
        assertEquals(8, intAt(size, 1));
        assertEquals(5, intAt(size, 2));

        ListTag blocks = list(template, "blocks");
        assertEquals(176, blocks.size());
        assertEquals(21, countMarker(blocks, "witchery:ground/adaptive"));
        assertEquals(6, countMarker(blocks, "witchery:plant/straw_man"));
        assertEquals(1, countMarker(blocks, "witchery:spawner/straw_man"));
        assertTrue(allMarkersKnown(blocks));
    }

    private static void assertMarkerAt(CompoundTag template, String marker, int x, int y, int z) {
        ListTag blocks = list(template, "blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            ListTag pos = list(block, "pos");
            if (intAt(pos, 0) == x && intAt(pos, 1) == y && intAt(pos, 2) == z) {
                assertEquals(marker, string(compound(block, "nbt"), "metadata"));
                return;
            }
        }
        throw new AssertionError("No marker at " + x + "," + y + "," + z);
    }

    private static CompoundTag readTemplate(String fileName) throws IOException {
        String resourceName = TEMPLATE_RESOURCE_DIR + fileName;
        InputStream input = StrawManTemplateTest.class.getClassLoader().getResourceAsStream(resourceName);
        if (input == null) {
            throw new IOException("Missing template resource " + resourceName);
        }
        try (InputStream stream = input) {
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        }
    }

    private static int countMarker(ListTag blocks, String marker) {
        int count = 0;
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            if (block.contains("nbt")
                    && marker.equals(string(compound(block, "nbt"), "metadata"))) {
                count++;
            }
        }
        return count;
    }

    private static boolean allMarkersKnown(ListTag blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            if (!block.contains("nbt")) {
                continue;
            }
            String marker = string(compound(block, "nbt"), "metadata");
            if (!Map.of(
                    "witchery:ground/adaptive", true,
                    "witchery:plant/straw_man", true,
                    "witchery:spawner/straw_man", true
            ).containsKey(marker)) {
                return false;
            }
        }
        return true;
    }

    private static String blockStateAt(CompoundTag template, int x, int y, int z) {
        ListTag palette = list(template, "palette");
        ListTag blocks = list(template, "blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            ListTag pos = list(block, "pos");
            if (intAt(pos, 0) == x && intAt(pos, 1) == y && intAt(pos, 2) == z) {
                return stateName(compoundAt(palette, integer(block, "state")));
            }
        }
        throw new AssertionError("No block at " + x + "," + y + "," + z);
    }

    private static String stateName(CompoundTag state) {
        String name = string(state, "Name");
        if (!state.contains("Properties")) {
            return name;
        }
        CompoundTag properties = compound(state, "Properties");
        return name + "[axis=" + string(properties, "axis") + "]";
    }

    private static ListTag list(CompoundTag tag, String key) {
        return tag.getList(key).orElseThrow();
    }

    private static CompoundTag compound(CompoundTag tag, String key) {
        return tag.getCompound(key).orElseThrow();
    }

    private static CompoundTag compoundAt(ListTag tag, int index) {
        return tag.getCompound(index).orElseThrow();
    }

    private static int integer(CompoundTag tag, String key) {
        return tag.getInt(key).orElseThrow();
    }

    private static int intAt(ListTag tag, int index) {
        return tag.getInt(index).orElseThrow();
    }

    private static String string(CompoundTag tag, String key) {
        return tag.getString(key).orElseThrow();
    }
}
