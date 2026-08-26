package com.emoniph.witchery.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageStructureContractTest {
    private static final Path POOLS = projectRoot().resolve(
            Path.of("src", "main", "resources", "data", "minecraft", "worldgen",
                    "template_pool", "village"));
    private static final Map<String, Integer> EXPECTED_WEIGHTS = Map.of(
            "apothecary", 16,
            "witch_hut", 12,
            "bookshop", 16,
            "village_watch_tower", 8,
            "village_keep", 32);

    @Test
    void everyVillageBiomeUsesTheSameFiveOnePerTypeCandidates() throws IOException {
        for (String biome : List.of("plains", "desert", "savanna", "taiga", "snowy")) {
            JsonObject pool = JsonParser.parseString(
                    Files.readString(POOLS.resolve(biome).resolve("houses.json"))).getAsJsonObject();
            Map<String, Integer> found = new LinkedHashMap<>();
            JsonArray elements = pool.getAsJsonArray("elements");
            for (var value : elements) {
                JsonObject weighted = value.getAsJsonObject();
                JsonObject element = weighted.getAsJsonObject("element");
                if (!"witchery:village_building".equals(element.get("element_type").getAsString())) continue;
                found.put(element.get("building").getAsString(), weighted.get("weight").getAsInt());
                assertEquals("rigid", element.get("projection").getAsString(), biome + " projection");
            }
            assertEquals(EXPECTED_WEIGHTS, found, biome + " Witchery building candidates");
        }
    }

    @Test
    void biomeMaterialFamiliesRemainDistinctAndComplete() {
        VillageBuildingMaterials plains = VillageBuildingMaterials.forType(
                VillageBuildingMaterials.VillageType.PLAINS);
        VillageBuildingMaterials desert = VillageBuildingMaterials.forType(
                VillageBuildingMaterials.VillageType.DESERT);
        VillageBuildingMaterials savanna = VillageBuildingMaterials.forType(
                VillageBuildingMaterials.VillageType.SAVANNA);
        VillageBuildingMaterials taiga = VillageBuildingMaterials.forType(
                VillageBuildingMaterials.VillageType.TAIGA);
        VillageBuildingMaterials snowy = VillageBuildingMaterials.forType(
                VillageBuildingMaterials.VillageType.SNOWY);

        assertTrue(plains.belongsToFamily(Blocks.OAK_PLANKS.defaultBlockState()));
        assertTrue(desert.belongsToFamily(Blocks.CUT_SANDSTONE.defaultBlockState()));
        assertTrue(savanna.belongsToFamily(Blocks.ACACIA_LOG.defaultBlockState()));
        assertTrue(taiga.belongsToFamily(Blocks.SPRUCE_PLANKS.defaultBlockState()));
        assertTrue(snowy.belongsToFamily(Blocks.SPRUCE_DOOR.defaultBlockState()));
        assertFalse(desert.belongsToFamily(Blocks.OAK_PLANKS.defaultBlockState()));
        assertFalse(savanna.belongsToFamily(Blocks.SPRUCE_PLANKS.defaultBlockState()));
        assertEquals(VillageBuildingMaterials.VillageType.PLAINS,
                VillageBuildingMaterials.typeForVillagePath("minecraft:village/meadow/houses"));
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "data", "witchery")))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
