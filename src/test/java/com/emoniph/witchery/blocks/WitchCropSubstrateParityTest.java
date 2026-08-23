package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchCropSubstrateParityTest {
    private static final Path CONFIGURED_FEATURES = projectRoot().resolve(Path.of(
            "src", "main", "resources", "data", "witchery", "worldgen", "configured_feature"));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void nonWaterCropsKeepTheSourceCultivatedAndNaturalSoils() {
        List<Block> crops = List.of(
                WitcheryBlocks.BELLADONNA.get(),
                WitcheryBlocks.MANDRAKE.get(),
                WitcheryBlocks.MINDRAKE.get(),
                WitcheryBlocks.SNOWBELL.get(),
                WitcheryBlocks.WORMWOOD_PLANT.get(),
                WitcheryBlocks.WOLFSBANE_CROP.get(),
                WitcheryBlocks.GARLIC_CROP.get());

        for (Block crop : crops) {
            assertTrue(WitchCropSoilRules.supports(crop, Blocks.FARMLAND.defaultBlockState()));
            assertTrue(WitchCropSoilRules.supports(crop, Blocks.GRASS_BLOCK.defaultBlockState()));
            assertTrue(WitchCropSoilRules.supports(crop, Blocks.DIRT.defaultBlockState()));
            assertTrue(WitchCropSoilRules.supports(crop, crop.defaultBlockState()));
            assertTrue(WitchCropSoilRules.supports(crop, WitcheryBlocks.WORMWOOD_PLANT.get().defaultBlockState()));
            assertFalse(WitchCropSoilRules.supports(crop, Blocks.STONE.defaultBlockState()));
            assertFalse(WitchCropSoilRules.supports(crop, Blocks.WATER.defaultBlockState()));
        }
    }

    @Test
    void snowbellKeepsItsConfiguredColdSoilsWithoutBroadeningOtherCrops() {
        Block snowbell = WitcheryBlocks.SNOWBELL.get();
        Block belladonna = WitcheryBlocks.BELLADONNA.get();

        assertTrue(WitchCropSoilRules.supports(snowbell, Blocks.PODZOL.defaultBlockState()));
        assertTrue(WitchCropSoilRules.supports(snowbell, Blocks.SNOW_BLOCK.defaultBlockState()));
        assertFalse(WitchCropSoilRules.supports(belladonna, Blocks.PODZOL.defaultBlockState()));
        assertFalse(WitchCropSoilRules.supports(belladonna, Blocks.SNOW_BLOCK.defaultBlockState()));
    }

    @Test
    void everyWildCropFeatureSelectsASubstrateItsBlockAccepts() throws IOException {
        List<String> cropFeatures = List.of(
                "wild_artichoke.json",
                "wild_belladonna.json",
                "wild_garlic.json",
                "wild_mandrake.json",
                "wild_snowbell.json",
                "wild_wolfsbane.json",
                "wild_wormwood.json");
        for (String fileName : cropFeatures) {
                Path feature = CONFIGURED_FEATURES.resolve(fileName);
                JsonObject root = readJson(feature);
                JsonObject nestedFeature = root.getAsJsonObject("config")
                        .getAsJsonObject("feature").getAsJsonObject("feature");
                JsonObject state = nestedFeature.getAsJsonObject("config")
                        .getAsJsonObject("to_place").getAsJsonObject("state");
                Block crop = findBlock(Identifier.parse(state.get("Name").getAsString()));
                assertNotNull(crop, () -> feature + " references an unknown crop block");

                JsonObject predicate = root.getAsJsonObject("config").getAsJsonObject("feature")
                        .getAsJsonArray("placement").get(0).getAsJsonObject()
                        .getAsJsonObject("predicate");
                for (var substrateElement : predicate.getAsJsonArray("blocks")) {
                    Block substrate = findBlock(Identifier.parse(substrateElement.getAsString()));
                    assertNotNull(substrate, () -> feature + " references an unknown substrate");
                    boolean supported = crop instanceof ArtichokeBlock
                            ? substrate == Blocks.WATER
                            : WitchCropSoilRules.supports(crop, substrate.defaultBlockState());
                    assertTrue(supported, () -> feature + " places "
                            + BuiltInRegistries.BLOCK.getKey(crop) + " above unsupported "
                            + BuiltInRegistries.BLOCK.getKey(substrate));
                }
        }
    }

    private static Block findBlock(Identifier blockId) {
        for (DeferredHolder<Block, ? extends Block> holder : WitcheryBlocks.BLOCKS.getEntries()) {
            if (holder.getId().equals(blockId)) return holder.get();
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(block).equals(blockId)) return block;
        }
        return null;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
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
