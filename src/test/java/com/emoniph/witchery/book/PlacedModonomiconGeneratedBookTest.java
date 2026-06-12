package com.emoniph.witchery.book;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PlacedModonomiconGeneratedBookTest {
    private static final Path PRIMER_RELATIVE_PATH = Path.of(
            "src", "generated", "resources", "data", "witchery", "modonomicon", "books", "witches_primer");
    private static final int PLACED_PAGE_WIDTH = 124;
    private static final int PLACED_PAGE_HEIGHT = 155;
    private static final int STATION_PANEL_TOP = 24;
    private static final int STATION_PANEL_TOP_NO_TITLE = 7;
    private static final int STATION_TEXT_GAP = 6;
    private static final int ITEM_SIZE = 16;
    private static final int CRAFTING_SECOND_RECIPE_Y = 78;
    private static final int CRAFTING_TEXT_Y = 104;
    private static final int CRAFTING_GRID_X = 9;
    private static final int CRAFTING_SLOT_STEP = 13;
    private static final int CRAFTING_RENDERED_ITEM_SIZE = 14;
    private static final int CRAFTING_RENDERED_ITEM_OFFSET = 2;
    private static final int CRAFTING_RESULT_X = 89;
    private static final Set<String> PLACED_PAGE_TYPES = Set.of(
            "modonomicon:crafting_recipe",
            "modonomicon:entity",
            "modonomicon:spotlight",
            "modonomicon:text",
            "witchery:station_recipe");

    @Test
    void witchesPrimerHasMultipleEntriesForPlacedBookBrowsing() throws IOException {
        Set<String> entryPaths = entryPaths();

        assertTrue(entryPaths.size() > 20, "placed primer should browse a full book, not one entry");
        assertTrue(entryPaths.contains("getting_started/first_steps.json"), "first Primer entry exists");
        assertTrue(entryPaths.contains("herbology/rowan_tree.json"), "Primer browsing reaches later categories");
        assertTrue(entryPaths.contains("collecting_fumes/oven_recipes.json"), "Primer browsing reaches recipe pages");
    }

    @Test
    void placedRendererCoversEveryGeneratedPrimerPageType() throws IOException {
        Set<String> pageTypes = new HashSet<>();
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                JsonArray pages = readJson(entry).getAsJsonArray("pages");
                if (pages == null) {
                    continue;
                }
                for (var pageElement : pages) {
                    JsonObject page = pageElement.getAsJsonObject();
                    if (page.has("type")) {
                        pageTypes.add(page.get("type").getAsString());
                    }
                }
            }
        }

        assertFalse(pageTypes.isEmpty(), "Primer entries should have renderable pages");
        assertTrue(PLACED_PAGE_TYPES.containsAll(pageTypes),
                () -> "unsupported placed Primer page types: " + difference(pageTypes, PLACED_PAGE_TYPES));
    }

    @Test
    void placedGettingStartedSpreadsDoNotMixAltarAndBolineEntries() throws IOException {
        List<EntrySummary> entries = generatedGettingStartedEntries();

        Integer altarLastPage = null;
        Integer bolineFirstPage = null;
        int placedPageIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            EntrySummary entry = entries.get(i);
            if (entry.path().equals("altar.json")) {
                altarLastPage = placedPageIndex + entry.pageCount() - 1;
            } else if (entry.path().equals("boline.json")) {
                bolineFirstPage = placedPageIndex;
            }

            placedPageIndex += entry.pageCount();
            if (PlacedModonomiconBookResolver.shouldAddEntrySpacer(entry.pageCount(), i + 1 < entries.size())) {
                placedPageIndex++;
            }
        }

        assertTrue(altarLastPage != null, "generated getting_started/altar.json exists");
        assertTrue(bolineFirstPage != null, "generated getting_started/boline.json exists");
        assertTrue(altarLastPage % 2 == 0, "odd Altar entry should end on a left page");
        assertTrue(bolineFirstPage % 2 == 0, "Boline entry should restart on a left page");
        assertTrue(bolineFirstPage - altarLastPage == 2,
                "a blank right page should separate Altar's final page from Boline's first page");
    }

    @Test
    void generatedCraftingRecipePagesFitPlacedBookPage() throws IOException {
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                JsonArray pages = readJson(entry).getAsJsonArray("pages");
                if (pages == null) {
                    continue;
                }
                for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                    JsonObject page = pages.get(pageIndex).getAsJsonObject();
                    if (!"modonomicon:crafting_recipe".equals(page.get("type").getAsString())) {
                        continue;
                    }

                    assertCraftingRecipeFits(entry, pageIndex, page);
                }
            }
        }
    }

    @Test
    void generatedStationRecipePagesFitPlacedBookPage() throws IOException {
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                JsonArray pages = readJson(entry).getAsJsonArray("pages");
                if (pages == null) {
                    continue;
                }
                for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                    JsonObject page = pages.get(pageIndex).getAsJsonObject();
                    if (!"witchery:station_recipe".equals(page.get("type").getAsString())) {
                        continue;
                    }

                    assertStationRecipeFits(entry, pageIndex, page);
                }
            }
        }
    }

    private static Set<String> entryPaths() throws IOException {
        Set<String> paths = new HashSet<>();
        Path entriesRoot = primerRoot().resolve("entries");
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                paths.add(entriesRoot.relativize(entry).toString().replace('\\', '/'));
            }
        }
        return paths;
    }

    private static Stream<Path> entryFiles() throws IOException {
        Path entriesRoot = primerRoot().resolve("entries");
        assertTrue(Files.isDirectory(entriesRoot), "generated Witches' Primer entries must exist");
        return Files.walk(entriesRoot)
                .filter(path -> path.getFileName().toString().endsWith(".json"));
    }

    private static List<EntrySummary> generatedGettingStartedEntries() throws IOException {
        Path entriesRoot = primerRoot().resolve("entries").resolve("getting_started");
        assertTrue(Files.isDirectory(entriesRoot), "generated Witches' Primer getting_started entries must exist");
        List<EntrySummary> entries = new ArrayList<>();
        try (Stream<Path> entryFiles = Files.walk(entriesRoot)
                .filter(path -> path.getFileName().toString().endsWith(".json"))) {
            for (Path entry : entryFiles.toList()) {
                JsonObject json = readJson(entry);
                JsonArray pages = json.getAsJsonArray("pages");
                entries.add(new EntrySummary(
                        entriesRoot.relativize(entry).toString().replace('\\', '/'),
                        json.get("sort_number").getAsInt(),
                        pages == null ? 0 : pages.size()));
            }
        }
        entries.sort(Comparator.comparingInt(EntrySummary::sortNumber)
                .thenComparing(EntrySummary::path));
        return entries;
    }

    private static Path primerRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(PRIMER_RELATIVE_PATH);
            if (Files.isDirectory(candidate.resolve("entries"))) {
                return candidate;
            }
            current = current.getParent();
        }
        return PRIMER_RELATIVE_PATH;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void assertStationRecipeFits(Path entry, int pageIndex, JsonObject page) {
        int bgWidth = page.get("bg_width").getAsInt();
        int bgHeight = page.get("bg_height").getAsInt();
        float scale = Math.min(1.0F, PLACED_PAGE_WIDTH / (float) bgWidth);
        int drawnW = Math.round(bgWidth * scale);
        int drawnH = Math.round(bgHeight * scale);
        int bgX = Math.round((PLACED_PAGE_WIDTH - drawnW) / 2.0F);
        int panelTop = hasNonEmptyTitle(page) ? STATION_PANEL_TOP : STATION_PANEL_TOP_NO_TITLE;
        int textY = panelTop + drawnH + STATION_TEXT_GAP;

        assertTrue(textY < PLACED_PAGE_HEIGHT,
                () -> stationPageMessage(entry, pageIndex) + " leaves no caption room");

        JsonArray slots = page.getAsJsonArray("slots");
        assertFalse(slots.isEmpty(), () -> stationPageMessage(entry, pageIndex) + " has no item slots");
        for (var slotElement : slots) {
            JsonObject slot = slotElement.getAsJsonObject();
            int itemX = bgX + Math.round(slot.get("x").getAsInt() * scale);
            int itemY = panelTop + Math.round(slot.get("y").getAsInt() * scale);
            assertTrue(itemX >= 0 && itemX + ITEM_SIZE <= PLACED_PAGE_WIDTH,
                    () -> stationPageMessage(entry, pageIndex) + " item x outside placed page: " + itemX);
            assertTrue(itemY >= 0 && itemY + ITEM_SIZE <= PLACED_PAGE_HEIGHT,
                    () -> stationPageMessage(entry, pageIndex) + " item y outside placed page: " + itemY);
        }
    }

    private static void assertCraftingRecipeFits(Path entry, int pageIndex, JsonObject page) {
        assertTrue(hasNonEmptyString(page, "recipe_id_1"),
                () -> craftingPageMessage(entry, pageIndex) + " has no first recipe");
        assertFalse(hasNonEmptyString(page, "recipe_id_3"),
                () -> craftingPageMessage(entry, pageIndex) + " has more recipes than the placed renderer supports");

        boolean hasSecondRecipe = hasNonEmptyString(page, "recipe_id_2");
        assertCraftingPanelFits(entry, pageIndex, page, "title1", 0, !hasSecondRecipe);
        if (hasSecondRecipe) {
            assertCraftingPanelFits(entry, pageIndex, page, "title2", CRAFTING_SECOND_RECIPE_Y, false);
        } else if (hasNonEmptyString(page, "text")) {
            assertTrue(CRAFTING_TEXT_Y < PLACED_PAGE_HEIGHT,
                    () -> craftingPageMessage(entry, pageIndex) + " leaves no caption room");
        }
    }

    private static void assertCraftingPanelFits(Path entry, int pageIndex, JsonObject page, String titleKey,
                                                int yOffset, boolean roomy) {
        int top = yOffset + (hasNonEmptyString(page, titleKey) ? 16 : 6);
        int gridY = top + (roomy ? 10 : 6);
        int leftIngredientX = CRAFTING_GRID_X - CRAFTING_RENDERED_ITEM_OFFSET;
        int rightIngredientX = CRAFTING_GRID_X + 2 * CRAFTING_SLOT_STEP - CRAFTING_RENDERED_ITEM_OFFSET;
        int topIngredientY = gridY - CRAFTING_RENDERED_ITEM_OFFSET;
        int bottomIngredientY = gridY + 2 * CRAFTING_SLOT_STEP - CRAFTING_RENDERED_ITEM_OFFSET;
        int resultY = gridY + CRAFTING_SLOT_STEP - CRAFTING_RENDERED_ITEM_OFFSET;

        assertTrue(leftIngredientX >= 0,
                () -> craftingPageMessage(entry, pageIndex) + " ingredient grid starts outside page");
        assertTrue(rightIngredientX + CRAFTING_RENDERED_ITEM_SIZE <= PLACED_PAGE_WIDTH,
                () -> craftingPageMessage(entry, pageIndex) + " ingredient grid ends outside page");
        assertTrue(topIngredientY >= 0,
                () -> craftingPageMessage(entry, pageIndex) + " ingredient grid starts above page");
        assertTrue(bottomIngredientY + CRAFTING_RENDERED_ITEM_SIZE <= PLACED_PAGE_HEIGHT,
                () -> craftingPageMessage(entry, pageIndex) + " ingredient grid ends below page");
        assertTrue(CRAFTING_RESULT_X + CRAFTING_RENDERED_ITEM_SIZE <= PLACED_PAGE_WIDTH,
                () -> craftingPageMessage(entry, pageIndex) + " result item x outside page");
        assertTrue(resultY >= 0 && resultY + CRAFTING_RENDERED_ITEM_SIZE <= PLACED_PAGE_HEIGHT,
                () -> craftingPageMessage(entry, pageIndex) + " result item y outside page");
    }

    private static boolean hasNonEmptyTitle(JsonObject page) {
        return hasNonEmptyString(page, "title");
    }

    private static boolean hasNonEmptyString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).getAsString().isEmpty();
    }

    private static String stationPageMessage(Path entry, int pageIndex) {
        return entry + " page " + pageIndex;
    }

    private static String craftingPageMessage(Path entry, int pageIndex) {
        return entry + " page " + pageIndex;
    }

    private static Set<String> difference(Set<String> values, Set<String> allowed) {
        Set<String> difference = new HashSet<>(values);
        difference.removeAll(allowed);
        return difference;
    }

    private record EntrySummary(String path, int sortNumber, int pageCount) {}
}
