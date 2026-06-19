package com.emoniph.witchery.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbtStructureTemplateTest {
    private static final String HOBGOBLIN_HUT_TEMPLATE =
            "data/witchery/structure/hobgoblin_hut/hobgobhut.nbt";
    private static final String WARPED_COVEN_TEMPLATE =
            "data/witchery/structure/warped_coven/warpedcoven.nbt";
    private static final String WITCH_WELL_TEMPLATE =
            "data/witchery/structure/witch_well/well.nbt";
    private static final String WITCH_WELL_CAVE_TEMPLATE =
            "data/witchery/structure/witch_well/well_cave.nbt";
    private static final String HOBGOBLIN_HUT_STRUCTURE =
            "data/witchery/worldgen/structure/hobgoblin_hut.json";
    private static final String HOBGOBLIN_HUT_BIOME_TAG =
            "data/witchery/tags/worldgen/biome/has_hobgoblin_hut.json";
    private static final String WARPED_COVEN_STRUCTURE =
            "data/witchery/worldgen/structure/warped_coven.json";
    private static final String WITCH_WELL_STRUCTURE =
            "data/witchery/worldgen/structure/witch_well.json";
    private static final String WITCH_WELL_BIOME_TAG =
            "data/witchery/tags/worldgen/biome/has_witch_well.json";
    private static final String WITCH_WELL_LOOT =
            "data/witchery/loot_table/chests/witch_well.json";

    @Test
    void hobgoblinHutTemplateIsPackagedWithoutChest() throws IOException {
        CompoundTag template = readTemplate(HOBGOBLIN_HUT_TEMPLATE);

        assertSize(template, 14, 14, 14);
        assertFalse(paletteContainsBlock(template, "minecraft:chest"));
        assertFalse(paletteContainsBlock(template, "minecraft:barrel"));
        assertFalse(paletteContainsBlockNamePart(template, "snow"));
        assertFalse(blockNbtContainsId(template, "minecraft:chest"));
    }

    @Test
    void warpedCovenTemplateIsPackagedWithWitcheryInteriorButNoMirrorOrSnow() throws IOException {
        CompoundTag template = readTemplate(WARPED_COVEN_TEMPLATE);

        assertSize(template, 17, 17, 17);
        assertTrue(blockNbtContainsId(template, "witchery:witches_oven"));
        assertTrue(blockNbtContainsId(template, "witchery:witch_cauldron"));
        assertTrue(blockNbtContainsId(template, "witchery:chalice"));
        assertFalse(paletteContainsBlockNamePart(template, "mirror"));
        assertFalse(paletteContainsBlockNamePart(template, "snow"));
        assertFalse(blockNbtContainsId(template, "witchery:mirror"));
    }

    @Test
    void warpedCovenGeneratesOnlyInWarpedForestTag() throws IOException {
        String json = readResourceText(WARPED_COVEN_STRUCTURE);
        assertTrue(json.contains("\"type\": \"witchery:warped_coven\""));
        assertTrue(json.contains("\"biomes\": \"#witchery:has_warped_coven\""));
        assertTrue(json.contains("\"step\": \"top_layer_modification\""));
        assertTrue(json.contains("\"terrain_adaptation\": \"none\""));
    }

    @Test
    void nbtHobgoblinHutDoesNotAskMinecraftToBuildATerrainShelf() throws IOException {
        String json = readResourceText(HOBGOBLIN_HUT_STRUCTURE);
        assertTrue(json.contains("\"type\": \"witchery:hobgoblin_hut\""));
        assertTrue(json.contains("\"biomes\": \"#witchery:has_hobgoblin_hut\""));
        assertTrue(json.contains("\"terrain_adaptation\": \"none\""));
        assertFalse(json.contains("beard_thin"));
    }

    @Test
    void nbtHobgoblinHutUsesSourceStyleForestAndSwampBiomes() throws IOException {
        String json = readResourceText(HOBGOBLIN_HUT_BIOME_TAG);
        assertTrue(json.contains("\"#minecraft:is_forest\""));
        assertTrue(json.contains("\"minecraft:swamp\""));
        assertTrue(json.contains("\"minecraft:mangrove_swamp\""));
    }

    @Test
    void witchWellTemplatesArePackagedWithoutCapturedTerrain() throws IOException {
        CompoundTag well = readTemplate(WITCH_WELL_TEMPLATE);
        CompoundTag cave = readTemplate(WITCH_WELL_CAVE_TEMPLATE);

        assertSize(well, 15, 15, 14);
        assertSize(cave, 30, 20, 30);
        assertEquals(138, list(well, "blocks").size());
        assertEquals(496, list(cave, "blocks").size());

        assertFalse(paletteContainsBlock(well, "minecraft:structure_block"));
        assertFalse(paletteContainsBlock(cave, "minecraft:structure_block"));
        assertFalse(paletteContainsBlockNamePart(well, "snow"));
        assertFalse(paletteContainsBlockNamePart(cave, "snow"));
        assertFalse(paletteContainsBlock(well, "minecraft:grass_block"));
        assertFalse(paletteContainsBlock(cave, "minecraft:grass_block"));
        assertFalse(paletteContainsBlock(well, "minecraft:dirt"));
        assertFalse(paletteContainsBlock(cave, "minecraft:dirt"));
        assertFalse(paletteContainsBlock(cave, "minecraft:stone"));

        assertTrue(blockNbtContainsId(well, "minecraft:mob_spawner"));
        assertTrue(blockNbtContainsId(cave, "minecraft:mob_spawner"));
        assertTrue(blockNbtContainsId(cave, "minecraft:chest"));
        assertTrue(blockNbtContainsId(cave, "witchery:brazier"));
        assertFalse(blockNbtContainsKey(cave, "WITCPlacer"));
    }

    @Test
    void witchWellGeneratesInForestAndSwampWithOwnLoot() throws IOException {
        String structure = readResourceText(WITCH_WELL_STRUCTURE);
        String biomes = readResourceText(WITCH_WELL_BIOME_TAG);
        String loot = readResourceText(WITCH_WELL_LOOT);

        assertTrue(structure.contains("\"type\": \"witchery:witch_well\""));
        assertTrue(structure.contains("\"biomes\": \"#witchery:has_witch_well\""));
        assertTrue(structure.contains("\"terrain_adaptation\": \"none\""));
        assertTrue(biomes.contains("\"#minecraft:is_forest\""));
        assertTrue(biomes.contains("\"minecraft:swamp\""));
        assertTrue(biomes.contains("\"minecraft:mangrove_swamp\""));
        assertTrue(loot.contains("\"witchery:graveyard_dust\""));
        assertTrue(loot.contains("\"witchery:refined_evil\""));
        assertTrue(loot.contains("\"witchery:tear_of_goddess\""));
    }

    @Test
    void caveWitchWellPlacesSurfaceOneBlockBelowOpeningWithoutDirtSupportOverCave() {
        WitchWellPiece.Variant cave = WitchWellPiece.Variant.CAVE;
        WitchWellPiece.Variant regular = WitchWellPiece.Variant.REGULAR;

        assertEquals(12, cave.surfaceYOffset());
        assertEquals(26, cave.surfaceMaxX());
        assertEquals(10, cave.surfaceFillDepth());
        assertEquals(4, regular.surfaceFillDepth());
        assertTrue(cave.surfaceColumn(14, 10));
        assertTrue(cave.surfaceColumn(25, 10));
        assertFalse(cave.surfaceColumn(7, 3));
        assertFalse(cave.surfaceColumn(26, 17));
        assertTrue(regular.surfaceColumn(0, 0));
        assertTrue(cave.clearsCaveBackfillAt(14, 10, 10));
        assertTrue(cave.clearsCaveBackfillAt(22, 10, 15));
        assertTrue(cave.clearsCaveBackfillAt(25, 10, 10));
        assertFalse(cave.clearsCaveBackfillAt(7, 10, 10));
        assertFalse(cave.clearsCaveBackfillAt(14, 10, 3));
        assertFalse(regular.clearsCaveBackfillAt(8, 6, 6));
    }

    private static CompoundTag readTemplate(String resourceName) throws IOException {
        InputStream input = NbtStructureTemplateTest.class.getClassLoader().getResourceAsStream(resourceName);
        if (input == null) {
            throw new IOException("Missing template resource " + resourceName);
        }
        try (InputStream stream = input) {
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        }
    }

    private static String readResourceText(String resourceName) throws IOException {
        InputStream input = NbtStructureTemplateTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull(input, "Missing " + resourceName);

        try (InputStream stream = input) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertSize(CompoundTag template, int x, int y, int z) {
        ListTag size = list(template, "size");
        assertEquals(x, intAt(size, 0));
        assertEquals(y, intAt(size, 1));
        assertEquals(z, intAt(size, 2));
    }

    private static boolean paletteContainsBlock(CompoundTag template, String blockName) {
        ListTag palette = list(template, "palette");
        for (int i = 0; i < palette.size(); i++) {
            if (blockName.equals(string(compoundAt(palette, i), "Name"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean paletteContainsBlockNamePart(CompoundTag template, String part) {
        ListTag palette = list(template, "palette");
        for (int i = 0; i < palette.size(); i++) {
            if (string(compoundAt(palette, i), "Name").contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blockNbtContainsId(CompoundTag template, String id) {
        ListTag blocks = list(template, "blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            if (block.contains("nbt")
                    && id.equals(string(compound(block, "nbt"), "id"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean blockNbtContainsKey(CompoundTag template, String key) {
        ListTag blocks = list(template, "blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = compoundAt(blocks, i);
            if (block.contains("nbt")
                    && compound(block, "nbt").contains(key)) {
                return true;
            }
        }
        return false;
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

    private static int intAt(ListTag tag, int index) {
        return tag.getInt(index).orElseThrow();
    }

    private static String string(CompoundTag tag, String key) {
        return tag.getString(key).orElseThrow();
    }
}
