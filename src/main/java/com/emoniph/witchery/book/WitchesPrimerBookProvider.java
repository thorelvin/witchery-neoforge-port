package com.emoniph.witchery.book;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;

/**
 * Datagen for the native Modonomicon "Witches' Primer" book (the Book of Shadows).
 *
 * <p>Phase 1 of the Patchouli &rarr; Modonomicon migration: a minimal, self-contained book with one
 * category and one entry, proving the dependency, datagen wiring, and in-game open path. Text is
 * authored as literal {@link Component}s so this slice does not depend on (or regenerate) the
 * hand-maintained {@code en_us.json}; book/category/entry <em>names</em> reuse the existing
 * {@code witchery.book.*} translation keys. Bulk content migration of the legacy
 * {@code assets/witchery/patchouli_books/witches_primer} entries is a later phase.
 *
 * <p>Output: {@code data/witchery/modonomicon/books/witches_primer/...}.
 */
public class WitchesPrimerBookProvider implements BookSubProvider {

    public static final Identifier BOOK_ID = WitcheryBooks.PRIMER;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    private static String ink(String text) {
        return WitcheryBooks.ink(text);
    }

    @Override
    public void generate(BiConsumer<Identifier, BookModel> consumer, HolderLookup.Provider registries) {
        BookIconModel icon = BookIconModel.create(WitcheryItems.WITCHES_PRIMER.get());

        BookCategoryModel gettingStarted = BookCategoryModel.create(
                        id("getting_started"), "witchery.book.cat.getting_started")
                .withIcon(icon)
                .withSortNumber(0);

        BookEntryModel firstSteps = BookEntryModel.create(
                        id("getting_started/first_steps"), "witchery.book.entry.first_steps")
                .withCategory(gettingStarted)
                .withIcon(icon)
                .withLocation(0, 0)
                .withPages(
                        BookTextPageModel.create()
                                .withTitle(Component.literal("First Steps"))
                                .withText(ink(
                                        "So you would learn the old craft, pupil. Good. "
                                                + "Keep this primer close — it remembers what a tired witch forgets.")),
                        BookTextPageModel.create()
                                .withText(ink(
                                        "Begin small: a rowan tree for its berries and wood, an altar to drink in "
                                                + "the power of growing things, and a kettle set over a fire. "
                                                + "Everything that follows grows from those three.")))
                .withSortNumber(0);

        gettingStarted.withEntries(firstSteps);

        BookModel book = BookModel.create(BOOK_ID, "witchery.book.title")
                .withCreativeTab(id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withCategories(gettingStarted);

        consumer.accept(book.getId(), book);
    }
}
