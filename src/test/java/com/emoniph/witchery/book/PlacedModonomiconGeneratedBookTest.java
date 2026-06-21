package com.emoniph.witchery.book;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final Path BOOKS_RELATIVE_PATH = Path.of(
            "src", "generated", "resources", "data", "witchery", "modonomicon", "books");
    private static final List<String> PLACEABLE_BOOKS = List.of("witches_primer", "cauldron_book");
    private static final int PLACED_PAGE_WIDTH = 124;
    private static final int PLACED_PAGE_HEIGHT = 155;
    private static final int STATION_PANEL_TOP = 24;
    private static final int STATION_PANEL_TOP_NO_TITLE = 7;
    private static final int STATION_TEXT_GAP = 3;
    private static final int ITEM_SIZE = 16;
    private static final int RITUAL_RING_CENTER_Y = 34;
    private static final float RITUAL_RING_SQUASH = 0.62F;
    private static final int RITUAL_FACE_SIZE = 12;
    private static final int RITUAL_FACE_ROW_Y = 106;
    private static final int CRAFTING_SECOND_RECIPE_Y = 78;
    private static final int CRAFTING_TEXT_Y = 104;
    private static final int CRAFTING_GRID_X = 9;
    private static final int CRAFTING_SLOT_STEP = 13;
    private static final int CRAFTING_RENDERED_ITEM_SIZE = 14;
    private static final int CRAFTING_RENDERED_ITEM_OFFSET = 2;
    private static final int CRAFTING_RESULT_X = 89;
    private static final int PLACED_PAGE_TEXT_BOTTOM = PLACED_PAGE_HEIGHT - 22;
    private static final int ITEM_REFERENCE_ROW_HEIGHT = 18;
    private static final Set<String> PLACED_PAGE_TYPES = Set.of(
            "modonomicon:crafting_recipe",
            "modonomicon:entity",
            "modonomicon:spotlight",
            "modonomicon:text",
            "witchery:item_reference",
            "witchery:station_recipe");
    private static final List<GuideTarget> PLACEABLE_GUIDE_TARGETS = List.of(
            new GuideTarget("witches_primer", "herbology", "herbology/rowan_tree"),
            new GuideTarget("witches_primer", "brews_infusions", "brews_infusions/kettle"),
            new GuideTarget("witches_primer", "collecting_fumes", "collecting_fumes/witches_oven"),
            new GuideTarget("witches_primer", "distilling", "distilling/distillery"),
            new GuideTarget("witches_primer", "circle_magic", "circle_magic/chalk"),
            new GuideTarget("witches_primer", "conjuration", "conjuration/brazier"),
            new GuideTarget("witches_primer", "symbology", "symbology/mystic_branch"),
            new GuideTarget("witches_primer", "poppets", "poppets/taglock_kit"),
            new GuideTarget("witches_primer", "spirit_world", "spirit_world/spirit_world"),
            new GuideTarget("witches_primer", "creatures", "creatures/familiars"),
            new GuideTarget("witches_primer", "transformations", "transformations/vampirism"),
            new GuideTarget("cauldron_book", "brewing", "brewing/the_cauldron"));

    @Test
    void witchesPrimerHasMultipleEntriesForPlacedBookBrowsing() throws IOException {
        Set<String> entryPaths = entryPaths();

        assertTrue(entryPaths.size() > 20, "placed primer should browse a full book, not one entry");
        assertTrue(entryPaths.contains("getting_started/first_steps.json"), "first Primer entry exists");
        assertTrue(entryPaths.contains("herbology/rowan_tree.json"), "Primer browsing reaches later categories");
        assertTrue(entryPaths.contains("collecting_fumes/oven_recipes.json"), "Primer browsing reaches recipe pages");
    }

    @Test
    void placedRendererCoversEveryGeneratedPlaceableBookPageType() throws IOException {
        Set<String> pageTypes = new HashSet<>();
        for (String book : PLACEABLE_BOOKS) {
            try (Stream<Path> entries = entryFiles(book)) {
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
        }

        assertFalse(pageTypes.isEmpty(), "placeable Modonomicon books should have renderable pages");
        assertTrue(PLACED_PAGE_TYPES.containsAll(pageTypes),
                () -> "unsupported placed Modonomicon page types: " + difference(pageTypes, PLACED_PAGE_TYPES));
    }

    @Test
    void placeableGuideTargetsExistInGeneratedBooks() throws IOException {
        for (GuideTarget target : PLACEABLE_GUIDE_TARGETS) {
            Path bookRoot = bookRoot(target.book());
            Path categoryFile = bookRoot.resolve("categories").resolve(target.category() + ".json");
            Path entryFile = bookRoot.resolve("entries").resolve(target.entry() + ".json");

            assertTrue(Files.isRegularFile(categoryFile),
                    () -> "placeable guide category missing: " + target.book() + "/" + target.category());
            assertTrue(Files.isRegularFile(entryFile),
                    () -> "placeable guide entry missing: " + target.book() + "/" + target.entry());

            JsonObject entry = readJson(entryFile);
            JsonArray pages = entry.getAsJsonArray("pages");
            assertEquals(target.category(), entry.get("category").getAsString(),
                    () -> "placeable guide entry category mismatch: " + target.book() + "/" + target.entry());
            assertTrue(pages != null && !pages.isEmpty(),
                    () -> "placeable guide entry has no pages: " + target.book() + "/" + target.entry());
        }
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
    void generatedFirstStepsIntroUsesAvailablePageSpace() throws IOException {
        JsonArray pages = readJson(primerRoot()
                .resolve("entries")
                .resolve("getting_started")
                .resolve("first_steps.json"))
                .getAsJsonArray("pages");

        assertEquals(1, pages.size(), "short First Steps intro should fit on one prose page");
        assertTrue(pages.get(0).getAsJsonObject().get("text").getAsString().contains("The craft grows"),
                "First Steps second paragraph should share the opening page");
    }

    @Test
    void generatedTextPagesDoNotSplitBoldMarkdownAcrossPlacedPages() throws IOException {
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                JsonArray pages = readJson(entry).getAsJsonArray("pages");
                if (pages == null) {
                    continue;
                }
                for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                    JsonObject page = pages.get(pageIndex).getAsJsonObject();
                    if (!"modonomicon:text".equals(page.get("type").getAsString()) || !hasNonEmptyString(page, "text")) {
                        continue;
                    }

                    String text = page.get("text").getAsString();
                    int checkedPageIndex = pageIndex;
                    assertEquals(0, countOccurrences(text, "**") % 2,
                            () -> entry + " page " + checkedPageIndex + " splits bold markdown: " + text);
                }
            }
        }
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

    @Test
    void generatedItemReferencePagesFitPlacedBookPage() throws IOException {
        try (Stream<Path> entries = entryFiles()) {
            for (Path entry : entries.toList()) {
                JsonArray pages = readJson(entry).getAsJsonArray("pages");
                if (pages == null) {
                    continue;
                }
                for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                    JsonObject page = pages.get(pageIndex).getAsJsonObject();
                    if (!"witchery:item_reference".equals(page.get("type").getAsString())) {
                        continue;
                    }

                    assertItemReferencePageFits(entry, pageIndex, page);
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
        return entryFiles("witches_primer");
    }

    private static Stream<Path> entryFiles(String book) throws IOException {
        Path entriesRoot = bookRoot(book).resolve("entries");
        assertTrue(Files.isDirectory(entriesRoot), () -> "generated " + book + " entries must exist");
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
        return bookRoot("witches_primer");
    }

    private static Path bookRoot(String book) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(BOOKS_RELATIVE_PATH).resolve(book);
            if (Files.isDirectory(candidate.resolve("entries"))) {
                return candidate;
            }
            current = current.getParent();
        }
        return BOOKS_RELATIVE_PATH.resolve(book);
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
        int textY = panelTop + stationVisibleContentBottom(page, scale, drawnH) + STATION_TEXT_GAP;

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

        assertRitualDecorationsFit(entry, pageIndex, page, bgX, panelTop, scale, bgWidth);
    }

    private static int stationVisibleContentBottom(JsonObject page, float scale, int drawnH) {
        if (pageShouldShowBackground(page)) {
            return drawnH;
        }

        int bottom = 0;
        JsonArray slots = page.getAsJsonArray("slots");
        for (var slotElement : slots) {
            JsonObject slot = slotElement.getAsJsonObject();
            bottom = Math.max(bottom, Math.round((slot.get("y").getAsInt() + ITEM_SIZE) * scale));
        }
        if (page.has("rings")) {
            bottom = Math.max(bottom, Math.round((RITUAL_RING_CENTER_Y
                    + ritualRingRadius(largestRingSize(page)) * RITUAL_RING_SQUASH + 2) * scale));
        }
        if (page.has("coven_faces")) {
            bottom = Math.max(bottom, Math.round((RITUAL_FACE_ROW_Y + RITUAL_FACE_SIZE) * scale));
        }
        return bottom == 0 ? drawnH : bottom;
    }

    private static void assertRitualDecorationsFit(Path entry, int pageIndex, JsonObject page,
                                                   int bgX, int panelTop, float scale, int bgWidth) {
        if (page.has("rings")) {
            int centerX = bgX + Math.round((bgWidth / 2.0F) * scale);
            int centerY = panelTop + Math.round(RITUAL_RING_CENTER_Y * scale);
            for (var ringElement : page.getAsJsonArray("rings")) {
                JsonObject ring = ringElement.getAsJsonObject();
                int radiusX = Math.max(4, Math.round(ritualRingRadius(ring.get("size").getAsString()) * scale));
                int radiusY = Math.max(2, Math.round(radiusX * RITUAL_RING_SQUASH));
                assertTrue(centerX - radiusX >= 0 && centerX + radiusX <= PLACED_PAGE_WIDTH,
                        () -> stationPageMessage(entry, pageIndex) + " ritual ring x outside placed page");
                assertTrue(centerY - radiusY >= 0 && centerY + radiusY <= PLACED_PAGE_HEIGHT,
                        () -> stationPageMessage(entry, pageIndex) + " ritual ring y outside placed page");
            }
        }
        if (page.has("coven_faces")) {
            int count = page.get("coven_faces").getAsInt();
            int face = Math.max(8, Math.round(RITUAL_FACE_SIZE * scale));
            int totalW = count * face + Math.max(0, count - 1) * Math.max(1, Math.round(2 * scale));
            int startX = bgX + Math.round((bgWidth / 2.0F) * scale) - totalW / 2;
            int y = panelTop + Math.round(RITUAL_FACE_ROW_Y * scale);
            assertTrue(startX >= 0 && startX + totalW <= PLACED_PAGE_WIDTH,
                    () -> stationPageMessage(entry, pageIndex) + " coven faces x outside placed page");
            assertTrue(y >= 0 && y + face <= PLACED_PAGE_HEIGHT,
                    () -> stationPageMessage(entry, pageIndex) + " coven faces y outside placed page");
        }
    }

    private static boolean pageShouldShowBackground(JsonObject page) {
        return !page.has("show_background") || page.get("show_background").getAsBoolean();
    }

    private static String largestRingSize(JsonObject page) {
        String largest = "small";
        if (!page.has("rings")) {
            return largest;
        }
        for (var ringElement : page.getAsJsonArray("rings")) {
            String size = ringElement.getAsJsonObject().get("size").getAsString();
            if (ritualRingRadius(size) > ritualRingRadius(largest)) {
                largest = size;
            }
        }
        return largest;
    }

    private static int ritualRingRadius(String size) {
        return switch (size) {
            case "small" -> 15;
            case "large" -> 35;
            default -> 25;
        };
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

    private static void assertItemReferencePageFits(Path entry, int pageIndex, JsonObject page) {
        JsonArray rows = page.getAsJsonArray("rows");
        assertTrue(rows != null && !rows.isEmpty(),
                () -> itemReferencePageMessage(entry, pageIndex) + " has no icon rows");

        int y = hasNonEmptyTitle(page) ? 18 : 2;
        if (hasNonEmptyString(page, "text")) {
            y += 28;
        }
        int bottom = y + rows.size() * ITEM_REFERENCE_ROW_HEIGHT;
        assertTrue(bottom <= PLACED_PAGE_TEXT_BOTTOM,
                () -> itemReferencePageMessage(entry, pageIndex) + " rows exceed placed page: " + bottom);

        for (var rowElement : rows) {
            JsonObject row = rowElement.getAsJsonObject();
            assertTrue(hasNonEmptyString(row, "item"),
                    () -> itemReferencePageMessage(entry, pageIndex) + " row has no item icon");
            assertTrue(hasNonEmptyString(row, "text"),
                    () -> itemReferencePageMessage(entry, pageIndex) + " row has no text");
        }
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

    private static String itemReferencePageMessage(Path entry, int pageIndex) {
        return entry + " page " + pageIndex;
    }

    private static Set<String> difference(Set<String> values, Set<String> allowed) {
        Set<String> difference = new HashSet<>(values);
        difference.removeAll(allowed);
        return difference;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private record GuideTarget(String book, String category, String entry) {}

    private record EntrySummary(String path, int sortNumber, int pageCount) {}
}
