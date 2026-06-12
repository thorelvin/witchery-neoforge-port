package com.emoniph.witchery.book;

import com.emoniph.witchery.item.WitchesPrimerItem;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.BookCategory;
import com.klikli_dev.modonomicon.book.entries.BookContentEntry;
import com.klikli_dev.modonomicon.book.entries.BookEntry;
import com.klikli_dev.modonomicon.book.page.BookPage;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PlacedModonomiconBookResolver {
    public static final Identifier FALLBACK_CATEGORY = WitcheryBooks.id("getting_started");
    public static final Identifier FALLBACK_ENTRY = WitcheryBooks.id("getting_started/first_steps");

    private PlacedModonomiconBookResolver() {}

    public static List<PlacedPage> resolvePages(ItemStack bookStack, @Nullable Level level) {
        List<PlacedPage> pages = new ArrayList<>();
        List<BookContentEntry> entries = resolveEntries(bookStack, level);
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            BookContentEntry entry = entries.get(entryIndex);
            int entryPageCount = 0;
            for (BookPage page : entry.getPages()) {
                pages.add(new PlacedPage(entry, page));
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
        List<BookContentEntry> entries = resolveEntries(bookStack, level);
        return entries.isEmpty() ? null : entries.getFirst();
    }

    private static List<BookContentEntry> resolveEntries(ItemStack bookStack, @Nullable Level level) {
        if (!(bookStack.getItem() instanceof WitchesPrimerItem guide)) {
            return List.of();
        }

        ensureBooksBuilt(level);
        Book book = BookDataManager.get().getBook(guide.getBookId());
        if (book == null) {
            return List.of();
        }

        Identifier categoryId = guide.getCategoryId();
        Identifier entryId = guide.getEntryId();
        if (entryId != null) {
            BookContentEntry entry = findContentEntry(book, categoryId == null ? null : book.getCategory(categoryId), entryId);
            return entry == null ? List.of() : List.of(entry);
        }

        if (categoryId != null) {
            BookCategory category = book.getCategory(categoryId);
            return category == null ? List.of() : resolveCategoryEntries(category);
        }

        List<BookContentEntry> entries = new ArrayList<>();
        for (BookCategory category : book.getCategoriesSorted()) {
            if (category.showCategoryButton()) {
                entries.addAll(resolveCategoryEntries(category));
            }
        }
        if (!entries.isEmpty()) {
            return entries;
        }

        for (BookCategory category : book.getCategoriesSorted()) {
            entries.addAll(resolveCategoryEntries(category));
        }
        if (!entries.isEmpty()) {
            return entries;
        }

        if (book.getId().equals(WitcheryBooks.PRIMER)) {
            BookCategory fallbackCategory = book.getCategory(FALLBACK_CATEGORY);
            BookContentEntry entry = findContentEntry(book, fallbackCategory, FALLBACK_ENTRY);
            if (entry != null) {
                return List.of(entry);
            }
        }

        return List.of();
    }

    private static List<BookContentEntry> resolveCategoryEntries(BookCategory category) {
        List<BookContentEntry> entries = new ArrayList<>();
        List<BookEntry> sortedEntries = new ArrayList<>(category.getEntries().values());
        sortedEntries.sort(Comparator.comparingInt(BookEntry::getSortNumber)
                .thenComparing(entry -> entry.getId().toString()));
        for (BookEntry entry : sortedEntries) {
            if (entry instanceof BookContentEntry contentEntry) {
                entries.add(contentEntry);
            }
        }
        return entries;
    }

    public static int getPageCount(ItemStack bookStack, @Nullable Level level) {
        return resolvePages(bookStack, level).size();
    }

    public static int clampOpenPagesIndex(ItemStack bookStack, @Nullable Level level, int openPagesIndex) {
        int pageCount = getPageCount(bookStack, level);
        if (pageCount <= 0) {
            return 0;
        }

        return clampOpenPagesIndex(openPagesIndex, pageCount);
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

    public record PlacedPage(@Nullable BookContentEntry entry, @Nullable BookPage page) {
        public static PlacedPage blank() {
            return new PlacedPage(null, null);
        }

        public boolean isBlank() {
            return page == null;
        }
    }
}
