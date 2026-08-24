package com.emoniph.witchery.book;

import com.emoniph.witchery.brewing.Brews;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CauldronBookParityTest {

    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path BOOK_ROOT = PROJECT_ROOT.resolve(Path.of("src", "generated", "resources", "data",
            "witchery", "modonomicon", "books", "cauldron_book"));
    private static final Set<String> SOURCE_KEYS = Set.copyOf(List.of(("""
            tbench tcraft tritual toc introduction cauldron cauldron2 crafting toc/crafting2 crafting3
            boiledmeat mutandis mutandisextremis mutatingsprig dropofluck otherwherechalk infernalchalk
            goldenchalk netherwart endstone rottenflesh pittraps compass brewing brewing2 brewing3 brewing4
            brewing5 brewing6 bottling bottling2 rituals rituals2 rituals3 rituals4 rituals5 rituals6 rituals7
            rituals8 rituals9 rituals10 rituals11 otherrituals ritualwaystone ritualwaystoneblooded ritualtranspose
            ritualfind capacity power duration modifiers modifiers2 modifiers3 modifiers4 modifiers5 dispersal
            dispersaltrigger dispersaltrigger2 extent linger toc/effects level1 level1_2 level1_3 level1_4 level2
            level2_2 level2_3 level2_4 level2_5 level2_6 level2_7 level2_8 level2_9 level4 level4_2 level4_3
            level4_4 level4_5 level4_6 level5 level5_2 level6 level6_2 level8 level12 endlesswater growtree
            removebuffs insectbane knockback undeadbane thorns sprouting freeze erosion levelland webs airhike pull
            batburst poisonweapon attractarrows reflectarrows slowfall jump floating damageboost weakness heal harm
            nightvision invisible fastmove slowmove waterbreathing resistfire partlava repel poison regeneration
            harmwerewolves weakenvampires animalattraction animalrepulsion flames blindness disease insanity sinking
            overheating nightmare absorbtion healthboost wasting fullness revealing blight raisedead raiseland
            gruesprey wither demonbane inferno blast poisontoad reflectdamage iceshell fortune undeadscurse snowburst
            swimspeed enderinhibition partwater extinguish dissipategas growflowers fertilize harvest tilling pruning
            felling growlily tinting transpose transpose2 transpose3 transposeore frogsleg fear removedebuffs
            paralysis combustion illfitting leonard durationboost absorbmagic reincarnate moonshine stoutbelly
            brewgasimmunity drainmagic keepinventory shiftingseasons keepeffects spreaddebuffs brewbottling planting
            pulverize vines love volatility iceworld resizing stealbuffs wolfsbane
            """).trim().split("\\s+")));

    @Test
    void all196SourceMarkupKeysHaveExactlyOneModernHome() {
        Map<String, String> coverage = CauldronBookProvider.legacyCoverage();
        assertEquals(196, SOURCE_KEYS.size(), "source fixture must retain all original markup keys");
        assertEquals(SOURCE_KEYS, coverage.keySet(), "every source key must be mapped once, with no inventions");

        Set<String> generatedEntries = generatedEntryIds();
        assertTrue(generatedEntries.containsAll(coverage.values()),
                () -> "coverage targets missing generated entries: " + difference(Set.copyOf(coverage.values()), generatedEntries));
    }

    @Test
    void generatedBookKeepsTheModonomiconStructureAndSourceOpener() throws IOException {
        assertEquals(4, jsonFiles(BOOK_ROOT.resolve("categories")).size());
        assertEquals(14, jsonFiles(BOOK_ROOT.resolve("entries")).size());

        JsonObject book = readJson(BOOK_ROOT.resolve("book.json"));
        assertEquals("Witches' Brews", book.get("name").getAsString());
        assertFalse(book.get("generate_book_item").getAsBoolean());

        JsonObject opener = readJson(BOOK_ROOT.resolve(Path.of("entries", "brewing", "the_cauldron.json")));
        assertEquals("brewing", opener.get("category").getAsString());
        assertEquals("The Witch's Cauldron", opener.get("name").getAsString());
        assertTrue(opener.getAsJsonArray("pages").size() >= 4);
    }

    @Test
    void everyLiveBrewIngredientAppearsOnceInTheGeneratedReference() throws IOException {
        Set<String> expectedItems = Brews.ingredientReferences().stream()
                .map(reference -> BuiltInRegistries.ITEM.getKey(reference.item()).toString())
                .collect(Collectors.toSet());
        List<String> generatedRows = new ArrayList<>();

        for (Path entry : jsonFiles(BOOK_ROOT.resolve("entries"))) {
            JsonArray pages = readJson(entry).getAsJsonArray("pages");
            for (var pageElement : pages) {
                JsonObject page = pageElement.getAsJsonObject();
                if (!"witchery:item_reference".equals(page.get("type").getAsString())) {
                    continue;
                }
                for (var rowElement : page.getAsJsonArray("rows")) {
                    generatedRows.add(rowElement.getAsJsonObject().get("item").getAsString());
                }
            }
        }

        assertEquals(expectedItems.size(), generatedRows.size(), "each live registry item needs one reference row");
        assertEquals(expectedItems, new HashSet<>(generatedRows));
    }

    @Test
    void obsoleteStubInstructionsDoNotReturn() throws IOException {
        String generated = Files.readString(BOOK_ROOT.resolve("book.json"));
        for (Path entry : jsonFiles(BOOK_ROOT.resolve("entries"))) {
            generated += Files.readString(entry);
        }
        assertFalse(generated.contains("Dragon's breath leaves a slow, drifting fog"));
        assertFalse(generated.contains("Eight reagents at most before the water grows too muddled"));
        assertTrue(generated.contains("no vanilla Dragon's Breath shortcut"));
        assertTrue(generated.contains("live brew registry"));
        assertTrue(generated.contains("sneak-stir"));
        assertTrue(generated.contains("Tongue of Dog"));
    }

    private static Set<String> generatedEntryIds() {
        Path entries = BOOK_ROOT.resolve("entries");
        return jsonFiles(entries).stream()
                .map(entries::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/').replaceFirst("\\.json$", ""))
                .collect(Collectors.toSet());
    }

    private static List<Path> jsonFiles(Path root) {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect generated Cauldron Book at " + root, exception);
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> difference(Set<String> values, Set<String> allowed) {
        Set<String> difference = new HashSet<>(values);
        difference.removeAll(allowed);
        return difference;
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "generated", "resources")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate Witchery project root");
    }
}
