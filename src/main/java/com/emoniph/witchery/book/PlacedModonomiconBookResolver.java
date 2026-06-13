package com.emoniph.witchery.book;

import com.emoniph.witchery.item.WitchesPrimerItem;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.BookCategory;
import com.klikli_dev.modonomicon.book.conditions.context.BookConditionContext;
import com.klikli_dev.modonomicon.book.entries.BookContentEntry;
import com.klikli_dev.modonomicon.book.entries.BookEntry;
import com.klikli_dev.modonomicon.book.page.BookPage;
import com.klikli_dev.modonomicon.bookstate.BookUnlockStateManager;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PlacedModonomiconBookResolver {
    public static final Identifier FALLBACK_CATEGORY = WitcheryBooks.id("getting_started");
    public static final Identifier FALLBACK_ENTRY = WitcheryBooks.id("getting_started/first_steps");
    public static final int INDEX_LINES_PER_PAGE = 12;
    public static final int INDEX_START_Y_PIXELS = 19;
    public static final int INDEX_LINE_HEIGHT_PIXELS = 7;
    public static final int INDEX_CATEGORY_GAP_PIXELS = 2;

    private PlacedModonomiconBookResolver() {}

    public static List<PlacedPage> resolvePages(ItemStack bookStack, @Nullable Level level) {
        return resolvePages(bookStack, level, null);
    }

    public static List<PlacedPage> resolvePages(ItemStack bookStack, @Nullable Level level, @Nullable Player player) {
        List<PlacedPage> pages = new ArrayList<>();
        ResolvedBook resolvedBook = resolveBook(bookStack, level, player);
        List<BookContentEntry> entries = resolvedBook.entries();
        pages.addAll(createIndexPages(resolvedBook.book(), entries));
        if (!pages.isEmpty() && pages.size() % 2 != 0 && !entries.isEmpty()) {
            pages.add(PlacedPage.blank());
        }
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            BookContentEntry entry = entries.get(entryIndex);
            int entryPageCount = 0;
            for (BookPage page : resolveEntryPages(entry, player)) {
                pages.add(PlacedPage.content(entry, page));
                entryPageCount++;
            }
            if (shouldAddEntrySpacer(entryPageCount, entryIndex + 1 < entries.size())) {
                pages.add(PlacedPage.blank());
            }
        }
        return pages;
    }

    @Nullable
    public static BookContentEntry resolveEntry(ItemStack bookStack, @Nullable Level level) {
        List<BookContentEntry> entries = resolveBook(bookStack, level, null).entries();
        return entries.isEmpty() ? null : entries.getFirst();
    }

    private static ResolvedBook resolveBook(ItemStack bookStack, @Nullable Level level, @Nullable Player player) {
        if (!(bookStack.getItem() instanceof WitchesPrimerItem guide)) {
            return ResolvedBook.empty();
        }

        ensureBooksBuilt(level);
        Book book = BookDataManager.get().getBook(guide.getBookId());
        if (book == null) {
            return ResolvedBook.empty();
        }

        Identifier categoryId = guide.getCategoryId();
        Identifier entryId = guide.getEntryId();
        if (entryId != null) {
            BookContentEntry entry = findContentEntry(book, categoryId == null ? null : book.getCategory(categoryId), entryId);
            return entry == null || resolveEntryPages(entry, player).isEmpty()
                    ? ResolvedBook.empty(book)
                    : new ResolvedBook(book, List.of(entry));
        }

        if (categoryId != null) {
            BookCategory category = book.getCategory(categoryId);
            return category == null || !isCategoryVisible(category, player)
                    ? ResolvedBook.empty(book)
                    : new ResolvedBook(book, resolveCategoryEntries(category, player));
        }

        List<BookContentEntry> entries = new ArrayList<>();
        for (BookCategory category : book.getCategoriesSorted()) {
            if (category.showCategoryButton() && isCategoryVisible(category, player)) {
                entries.addAll(resolveCategoryEntries(category, player));
            }
        }
        if (!entries.isEmpty()) {
            return new ResolvedBook(book, entries);
        }

        for (BookCategory category : book.getCategoriesSorted()) {
            if (isCategoryVisible(category, player)) {
                entries.addAll(resolveCategoryEntries(category, player));
            }
        }
        if (!entries.isEmpty()) {
            return new ResolvedBook(book, entries);
        }

        if (book.getId().equals(WitcheryBooks.PRIMER)) {
            BookCategory fallbackCategory = book.getCategory(FALLBACK_CATEGORY);
            BookContentEntry entry = findContentEntry(book, fallbackCategory, FALLBACK_ENTRY);
            if (entry != null) {
                return new ResolvedBook(book, List.of(entry));
            }
        }

        return ResolvedBook.empty(book);
    }

    private static List<BookContentEntry> resolveCategoryEntries(BookCategory category, @Nullable Player player) {
        List<BookContentEntry> entries = new ArrayList<>();
        List<BookEntry> sortedEntries = new ArrayList<>(category.getEntries().values());
        sortedEntries.sort(Comparator.comparingInt(BookEntry::getSortNumber)
                .thenComparing(entry -> entry.getId().toString()));
        for (BookEntry entry : sortedEntries) {
            if (entry instanceof BookContentEntry contentEntry && !resolveEntryPages(contentEntry, player).isEmpty()) {
                entries.add(contentEntry);
            }
        }
        return entries;
    }

    private static List<PlacedPage> createIndexPages(@Nullable Book book, List<BookContentEntry> entries) {
        if (book == null || entries.size() <= 1) {
            return List.of();
        }

        List<IndexLine> lines = new ArrayList<>();
        Identifier currentCategory = null;
        for (BookContentEntry entry : entries) {
            BookCategory category = entry.getCategory();
            Identifier categoryId = category == null ? null : category.getId();
            if (category != null && !categoryId.equals(currentCategory)) {
                currentCategory = categoryId;
                lines.add(IndexLine.category(category.getName()));
            }
            lines.add(IndexLine.entry(entry.getName(), entry.getId()));
        }

        List<PlacedPage> pages = new ArrayList<>();
        for (int offset = 0; offset < lines.size(); offset += INDEX_LINES_PER_PAGE) {
            pages.add(PlacedPage.index(new PlacedIndexPage(book.getName(),
                    List.copyOf(lines.subList(offset, Math.min(lines.size(), offset + INDEX_LINES_PER_PAGE))))));
        }
        return pages;
    }

    private static boolean isCategoryVisible(BookCategory category, @Nullable Player player) {
        if (player == null) {
            return true;
        }

        return BookUnlockStateManager.get().isUnlockedFor(player, category)
                || category.getCondition().test(BookConditionContext.of(category.getBook(), category), player);
    }

    private static List<BookPage> resolveEntryPages(BookContentEntry entry, @Nullable Player player) {
        return player == null ? entry.getPages() : entry.getUnlockedPagesFor(player);
    }

    public static int getPageCount(ItemStack bookStack, @Nullable Level level) {
        return resolvePages(bookStack, level).size();
    }

    public static int getPageCount(ItemStack bookStack, @Nullable Level level, @Nullable Player player) {
        return resolvePages(bookStack, level, player).size();
    }

    public static int clampOpenPagesIndex(ItemStack bookStack, @Nullable Level level, int openPagesIndex) {
        int pageCount = getPageCount(bookStack, level);
        if (pageCount <= 0) {
            return 0;
        }

        return clampOpenPagesIndex(openPagesIndex, pageCount);
    }

    public static int clampOpenPagesIndex(ItemStack bookStack, @Nullable Level level, @Nullable Player player,
                                          int openPagesIndex) {
        int pageCount = getPageCount(bookStack, level, player);
        if (pageCount <= 0) {
            return 0;
        }

        return clampOpenPagesIndex(openPagesIndex, pageCount);
    }

    public static int entryTargetPageIndexAt(ItemStack bookStack, @Nullable Level level, @Nullable Player player,
                                             int indexPageIndex, int pageY) {
        List<PlacedPage> pages = resolvePages(bookStack, level, player);
        if (indexPageIndex < 0 || indexPageIndex >= pages.size()) {
            return -1;
        }

        PlacedIndexPage indexPage = pages.get(indexPageIndex).indexPage();
        if (indexPage == null) {
            return -1;
        }

        int lineIndex = indexLineAtY(indexPage, pageY);
        if (lineIndex < 0) {
            return -1;
        }

        Identifier entryId = indexPage.lines().get(lineIndex).entryId();
        if (entryId == null) {
            return -1;
        }

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            BookContentEntry entry = pages.get(pageIndex).entry();
            if (entry != null && entry.getId().equals(entryId)) {
                return clampOpenPagesIndex(pageIndex, pages.size());
            }
        }
        return -1;
    }

    static int indexLineAtY(PlacedIndexPage indexPage, int pageY) {
        int y = INDEX_START_Y_PIXELS;
        for (int lineIndex = 0; lineIndex < indexPage.lines().size(); lineIndex++) {
            IndexLine line = indexPage.lines().get(lineIndex);
            if (line.category() && y != INDEX_START_Y_PIXELS) {
                y += INDEX_CATEGORY_GAP_PIXELS;
            }

            if (pageY >= y - 1 && pageY < y + INDEX_LINE_HEIGHT_PIXELS) {
                return lineIndex;
            }
            y += INDEX_LINE_HEIGHT_PIXELS;
        }
        return -1;
    }

    static int clampOpenPagesIndex(int openPagesIndex, int pageCount) {
        if (pageCount <= 0) {
            return 0;
        }

        int evenIndex = Math.max(0, openPagesIndex) & -2;
        if (evenIndex >= pageCount) {
            evenIndex = (pageCount - 1) & -2;
        }
        return Math.max(0, evenIndex);
    }

    static boolean shouldAddEntrySpacer(int pageCount, boolean hasFollowingEntry) {
        return hasFollowingEntry && pageCount % 2 != 0;
    }

    @Nullable
    private static BookContentEntry findContentEntry(Book book, @Nullable BookCategory category, Identifier entryId) {
        BookEntry entry = category == null ? null : category.getEntry(entryId);
        if (entry == null) {
            entry = book.getEntry(entryId);
        }
        return entry instanceof BookContentEntry contentEntry ? contentEntry : null;
    }

    private static void ensureBooksBuilt(@Nullable Level level) {
        if (level == null) {
            return;
        }

        BookDataManager manager = BookDataManager.get();
        if (!manager.areBooksBuilt()) {
            manager.registries(level.registryAccess());
            manager.tryBuildBooks(level);
        }
    }

    private record ResolvedBook(@Nullable Book book, List<BookContentEntry> entries) {
        private static ResolvedBook empty() {
            return new ResolvedBook(null, List.of());
        }

        private static ResolvedBook empty(Book book) {
            return new ResolvedBook(book, List.of());
        }
    }

    public record PlacedPage(@Nullable BookContentEntry entry, @Nullable BookPage page,
                             @Nullable PlacedIndexPage indexPage) {
        public static PlacedPage content(BookContentEntry entry, BookPage page) {
            return new PlacedPage(entry, page, null);
        }

        public static PlacedPage index(PlacedIndexPage indexPage) {
            return new PlacedPage(null, null, indexPage);
        }

        public static PlacedPage blank() {
            return new PlacedPage(null, null, null);
        }

        public boolean isBlank() {
            return page == null && indexPage == null;
        }
    }

    public record PlacedIndexPage(String bookName, List<IndexLine> lines) {}

    public record IndexLine(String text, boolean category, @Nullable Identifier entryId) {
        public static IndexLine category(String text) {
            return new IndexLine(text, true, null);
        }

        public static IndexLine entry(String text, Identifier entryId) {
            return new IndexLine(text, false, entryId);
        }
    }
}
