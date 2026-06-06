package com.emoniph.witchery.client;

import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import net.minecraft.resources.Identifier;

/**
 * Client-only helper that opens a native Modonomicon book by id. Kept in a separate class so the
 * client-only Modonomicon GUI types are never referenced from a common item on a dedicated server;
 * only ever invoked behind a {@code level.isClientSide()} guard.
 */
public final class WitcheryBookOpener {

    private WitcheryBookOpener() {}

    public static void open(Identifier bookId) {
        BookGuiManager.get().openBook(BookAddress.defaultFor(bookId));
    }

    /** Opens a book straight to a specific category/entry (e.g. a topic guide item). */
    public static void openEntry(Identifier bookId, Identifier categoryId, Identifier entryId) {
        BookGuiManager.get().openBook(BookAddress.of(bookId, categoryId, entryId, 0));
    }
}
