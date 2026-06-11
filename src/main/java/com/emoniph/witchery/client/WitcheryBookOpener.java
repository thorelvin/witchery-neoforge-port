package com.emoniph.witchery.client;

import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.BookCategory;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.data.BookDataManager;
import net.minecraft.resources.Identifier;

/**
 * Client-only helper that opens a native Modonomicon book. Kept in a separate class so the
 * client-only Modonomicon GUI types are never referenced from a common item on a dedicated server;
 * only ever invoked behind a {@code level.isClientSide()} guard.
 */
public final class WitcheryBookOpener {

    private WitcheryBookOpener() {}

    public static void open(Identifier bookId) {
        BookGuiManager.get().openBook(BookAddress.defaultFor(bookId));
    }

    /**
     * Resume the reader's saved page, or open {@code categoryId/entryId} page 0 on first use.
     * {@link BookAddress#of} keeps all three {@code ignoreSaved} flags false, so the saved
     * category/entry/page wins when present and the addressed entry is only the first-time fallback.
     */
    public static void openResumingOrFirst(Identifier bookId, Identifier categoryId, Identifier entryId) {
        BookGuiManager.get().openBook(BookAddress.of(bookId, categoryId, entryId, 0));
    }

    /** Open a book straight to one chapter's entries list (the category screen). */
    public static void openCategoryList(Identifier bookId, Identifier categoryId) {
        Book book = BookDataManager.get().getBook(bookId);
        BookCategory category = book == null ? null : book.getCategory(categoryId);
        if (category == null) {
            open(bookId);
            return;
        }
        BookGuiManager.get().openCategory(category, BookAddress.defaultFor(category));
    }
}
