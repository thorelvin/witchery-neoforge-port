package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredFeatureBlockStateValidationTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final List<Path> CONFIGURED_FEATURE_ROOTS = List.of(
            PROJECT_ROOT.resolve(Path.of("src", "main", "resources", "data", "witchery",
                    "worldgen", "configured_feature")),
            PROJECT_ROOT.resolve(Path.of("src", "generated", "resources", "data", "witchery",
                    "worldgen", "configured_feature")));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void allConfiguredFeatureBlockStatesUseRegisteredPropertiesAndValues() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path root : CONFIGURED_FEATURE_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            for (Path resource : jsonFiles(root)) {
                validateBlockStates(readJson(resource), resource.toString(), "$", failures);
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void invalidNestedStateReportsResourceBlockPropertyAndValue() {
        JsonObject fixture = JsonParser.parseString("""
                {
                  "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                      "to_place": {
                        "type": "minecraft:simple_state_provider",
                        "state": {
                          "Name": "witchery:wormwood",
                          "Properties": { "age": "7" }
                        }
                      }
                    }
                  }
                }
                """).getAsJsonObject();
        List<String> failures = new ArrayList<>();

        validateBlockStates(fixture, "fixtures/invalid_wormwood.json", "$", failures);

        assertTrue(failures.stream().anyMatch(failure -> failure.contains("fixtures/invalid_wormwood.json")
                        && failure.contains("witchery:wormwood")
                        && failure.contains("property 'age'")
                        && failure.contains("value '7'")),
                () -> "Expected an actionable invalid-state failure, got: " + failures);
    }

    private static void validateBlockStates(JsonElement element, String resource, String path,
                                            List<String> failures) {
        if (element.isJsonArray()) {
            for (int index = 0; index < element.getAsJsonArray().size(); index++) {
                validateBlockStates(element.getAsJsonArray().get(index), resource,
                        path + "[" + index + "]", failures);
            }
            return;
        }
        if (!element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();
        if (object.has("Name") && object.get("Name").isJsonPrimitive()) {
            validateBlockState(object, resource, path, failures);
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            validateBlockStates(entry.getValue(), resource, path + "." + entry.getKey(), failures);
        }
    }

    private static void validateBlockState(JsonObject state, String resource, String path,
                                           List<String> failures) {
        String blockIdText = state.get("Name").getAsString();
        Identifier blockId = Identifier.tryParse(blockIdText);
        if (blockId == null) {
            failures.add(resource + " at " + path + " references invalid block ID '" + blockIdText + "'");
            return;
        }

        Block block = findBlock(blockId);
        if (block == null) {
            failures.add(resource + " at " + path + " references missing block '" + blockId + "'");
            return;
        }
        if (!state.has("Properties")) return;
        if (!state.get("Properties").isJsonObject()) {
            failures.add(resource + " at " + path + " block '" + blockId
                    + "' has a non-object Properties value");
            return;
        }

        for (Map.Entry<String, JsonElement> entry : state.getAsJsonObject("Properties").entrySet()) {
            String propertyName = entry.getKey();
            String propertyValue = entry.getValue().getAsString();
            Property<?> property = block.getStateDefinition().getProperty(propertyName);
            if (property == null) {
                failures.add(resource + " at " + path + " block '" + blockId
                        + "' has unknown property '" + propertyName + "'");
            } else if (property.getValue(propertyValue).isEmpty()) {
                failures.add(resource + " at " + path + " block '" + blockId + "' property '"
                        + propertyName + "' rejects value '" + propertyValue + "'; allowed="
                        + property.getPossibleValues());
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

    private static List<Path> jsonFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
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
