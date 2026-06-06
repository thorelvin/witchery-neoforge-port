package com.emoniph.witchery.book;

import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.book.BookDisplayMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Cauldron Book — a native Modonomicon guide to brewing in the Witch's Cauldron, opened by the
 * {@code witchery:cauldron_book} item. Names are literal English (Modonomicon resolves them through
 * {@code I18n.get}, which returns the literal when no translation key exists), and body text is
 * authored as sepia-inked Markdown so this book needs no hand-maintained lang entries.
 *
 * <p>Output: {@code data/witchery/modonomicon/books/cauldron_book/...}.
 */
public class CauldronBookProvider implements BookSubProvider {

    public static final Identifier BOOK_ID = WitcheryBooks.CAULDRON;

    @Override
    public void generate(BiConsumer<Identifier, BookModel> consumer, HolderLookup.Provider registries) {
        BookIconModel cauldronIcon = BookIconModel.create(WitcheryItems.BREW_BOTTLE.get());

        BookCategoryModel brewing = BookCategoryModel.create(
                        WitcheryBooks.id("brewing"), "Brewing")
                .withIcon(cauldronIcon)
                .withDisplayMode(BookDisplayMode.INDEX)
                .withSortNumber(0);

        // Body prose goes through WitcheryBooks.prose so it auto-paginates (Modonomicon does not),
        // gets the sepia ink, padding, and title-row reservation. No banner image: the legacy
        // patchouli/*.png banners are broken placeholders and the old Companion book used none.
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Brewing in the Cauldron",
                "A heated cauldron full of water is a patient thing. It takes whatever you give it, in the "
                        + "order you give it, and remembers."
                        + "\n\nLight a fire beneath it and fill the bowl. Drop your reagents in one at a time. "
                        + "When the brew suits you, dip an empty bottle and draw it off."));
        pages.addAll(WitcheryBooks.prose("A Hand Upon the Brew",
                "Some things you add are not the brew, but a hand laid upon it. **Glowstone** makes the next "
                        + "working stronger; **redstone** makes it last."
                        + "\n\nA **fermented spider eye** turns a kindness into a cruelty, or back again. Lay "
                        + "these *before* the reagent they shape — the cauldron remembers only forward."));
        pages.addAll(WitcheryBooks.prose("The Gift of the Reagent",
                "The reagent decides the gift: a spider's eye for poison, sugar for swiftness, a ghast's "
                        + "tear for mending, wormwood for unseeing."
                        + "\n\n**Gunpowder** bursts the bottle where it lands, sharing its work with all "
                        + "nearby. **Dragon's breath** leaves a slow, drifting fog. With neither, the draught "
                        + "is yours alone."));
        pages.addAll(WitcheryBooks.prose("Knowing When to Stop",
                "Eight reagents at most before the water grows too muddled. Pour off what you have made, "
                        + "rinse with fresh water, and begin the next working."));

        BookEntryModel cauldron = BookEntryModel.create(
                        WitcheryBooks.id("brewing/the_cauldron"), "Brewing in the Cauldron")
                .withCategory(brewing)
                .withIcon(cauldronIcon)
                .withLocation(0, 0)
                .withPages(pages)
                .withSortNumber(0);

        brewing.withEntries(cauldron);

        BookModel book = BookModel.create(BOOK_ID, "Cauldron Book")
                .withCreativeTab(WitcheryBooks.id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withBookTextOffsetX(WitcheryBooks.TEXT_OFFSET_X)
                .withBookTextOffsetWidth(WitcheryBooks.TEXT_OFFSET_WIDTH)
                .withCategories(brewing);

        consumer.accept(book.getId(), book);
    }
}
