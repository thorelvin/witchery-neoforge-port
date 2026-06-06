package com.emoniph.witchery.book;

import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.book.BookDisplayMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;

/**
 * Observations of an Immortal — a native Modonomicon lore book opened by the {@code vampire_book} /
 * {@code immortals_observations_book} items. The vampire level-cap unlock on read is preserved in
 * {@code ImmortalsObservationsBookItem}; this book replaces the old level-gated written-book screen
 * (all entries are now visible). Lore text mirrors the
 * {@code item.witchery.immortals_observations_book.page_*} lang lines, inlined as sepia Markdown.
 *
 * <p>Output: {@code data/witchery/modonomicon/books/observations/...}.
 */
public class ObservationsBookProvider implements BookSubProvider {

    public static final Identifier BOOK_ID = WitcheryBooks.OBSERVATIONS;

    @Override
    public void generate(BiConsumer<Identifier, BookModel> consumer, HolderLookup.Provider registries) {
        BookIconModel icon = BookIconModel.create(WitcheryItems.VAMPIRE_BOOK.get());

        BookCategoryModel record = BookCategoryModel.create(
                        WitcheryBooks.id("record"), "The Immortal's Record")
                .withIcon(icon)
                .withDisplayMode(BookDisplayMode.INDEX)
                .withSortNumber(0);

        BookEntryModel rite = BookEntryModel.create(
                        WitcheryBooks.id("record/blood_rite"), "The Blood Rite")
                .withCategory(record)
                .withIcon(icon)
                .withLocation(0, 0)
                .withPages(WitcheryBooks.prose("A Scholar's Warning",
                        "The first pages warn that the author studied an immortal predator and wrote for "
                                + "those who would survive the lesson."
                                + "\n\nA chicken, a skull, a boline, and a glass goblet begin the old blood rite."
                                + "\n\nA night rite calls string, red dust, torches, and a skull beneath open "
                                + "stars."
                                + "\n\nBlood poured over the skull draws Lilith's attention and sends the seeker "
                                + "toward the Nether."
                                + "\n\nStrength is proven to her in battle; the filled goblet begins the curse."))
                .withSortNumber(0);

        BookEntryModel curse = BookEntryModel.create(
                        WitcheryBooks.id("record/the_curse"), "The Curse Takes Hold")
                .withCategory(record)
                .withIcon(icon)
                .withLocation(0, 1)
                .withParent(rite)
                .withPages(WitcheryBooks.prose("The Curse Takes Hold",
                        "The first hunger fades only after feeding, and the body starts to change."
                                + "\n\nMesmerised victims can be held still, but draining too deeply has "
                                + "consequences."
                                + "\n\nAfter mastery, the vampire begins to sense time, hunger, and sunlight "
                                + "differently."
                                + "\n\nSunlight can be collected and turned back on the curse until the burning "
                                + "dulls."
                                + "\n\nBlazes and fortress fire mark the next trial of endurance."))
                .withSortNumber(1);

        BookEntryModel mastery = BookEntryModel.create(
                        WitcheryBooks.id("record/mastery"), "Mastery Over Blood")
                .withCategory(record)
                .withIcon(icon)
                .withLocation(0, 2)
                .withParent(curse)
                .withPages(WitcheryBooks.prose("Mastery Over Blood",
                        "Greater strength brings stone-breaking power and a second appeal to Lilith."
                                + "\n\nA red flower and another filled goblet open the path to higher mastery."
                                + "\n\nFlight follows; distance and village walls matter less than before."
                                + "\n\nTransfixed victims can be guided into cages for careful feeding."
                                + "\n\nPower over blood becomes power over making new vampires."
                                + "\n\nThe final note describes a coffin, a goblet, and an unwilling rebirth."))
                .withSortNumber(2);

        record.withEntries(rite, curse, mastery);

        BookModel book = BookModel.create(BOOK_ID, "Observations of an Immortal")
                .withCreativeTab(WitcheryBooks.id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withBookTextOffsetX(WitcheryBooks.TEXT_OFFSET_X)
                .withBookTextOffsetWidth(WitcheryBooks.TEXT_OFFSET_WIDTH)
                .withCategories(record);

        consumer.accept(book.getId(), book);
    }
}
