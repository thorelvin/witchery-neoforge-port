package com.emoniph.witchery.book;

import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookImagePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.book.BookDisplayMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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

        Identifier brewingImage = WitcheryBooks.id("textures/patchouli/brewing.png");

        // One idea per page — Modonomicon does not auto-paginate, so short pages avoid overflow.
        BookEntryModel cauldron = BookEntryModel.create(
                        WitcheryBooks.id("brewing/the_cauldron"), "Brewing in the Cauldron")
                .withCategory(brewing)
                .withIcon(cauldronIcon)
                .withLocation(0, 0)
                .withPages(
                        BookImagePageModel.create()
                                .withImages(brewingImage)
                                .withBorder(true)
                                .withText(WitcheryBooks.ink(
                                        "The heart of every working — a cauldron of water over a steady fire.")),
                        BookTextPageModel.create()
                                .withTitle(Component.literal("Brewing in the Cauldron"))
                                .withText(WitcheryBooks.ink(
                                        "A heated cauldron full of water is a patient thing. It takes whatever you "
                                                + "give it, in the order you give it, and remembers.")),
                        BookTextPageModel.create()
                                .withText(WitcheryBooks.ink(
                                        "Light a fire beneath it and fill the bowl. Drop your reagents in one at a "
                                                + "time. When the brew suits you, dip an empty bottle and draw it off.")),
                        BookTextPageModel.create()
                                .withTitle(Component.literal("A Hand Upon the Brew"))
                                .withText(WitcheryBooks.ink(
                                        "Some things you add are not the brew, but a hand laid upon it. **Glowstone** "
                                                + "makes the next working stronger; **redstone** makes it last.")),
                        BookTextPageModel.create()
                                .withText(WitcheryBooks.ink(
                                        "A **fermented spider eye** turns a kindness into a cruelty, or back again. "
                                                + "Lay these *before* the reagent they shape — the cauldron remembers "
                                                + "only forward.")),
                        BookTextPageModel.create()
                                .withTitle(Component.literal("The Gift of the Reagent"))
                                .withText(WitcheryBooks.ink(
                                        "The reagent decides the gift: a spider's eye for poison, sugar for swiftness, "
                                                + "a ghast's tear for mending, wormwood for unseeing.")),
                        BookTextPageModel.create()
                                .withText(WitcheryBooks.ink(
                                        "**Gunpowder** bursts the bottle where it lands, sharing its work with all "
                                                + "nearby. **Dragon's breath** leaves a slow, drifting fog. With "
                                                + "neither, the draught is yours alone.")),
                        BookTextPageModel.create()
                                .withTitle(Component.literal("Knowing When to Stop"))
                                .withText(WitcheryBooks.ink(
                                        "Eight reagents at most before the water grows too muddled. Pour off what you "
                                                + "have made, rinse with fresh water, and begin the next working.")))
                .withSortNumber(0);

        brewing.withEntries(cauldron);

        BookModel book = BookModel.create(BOOK_ID, "Cauldron Book")
                .withCreativeTab(WitcheryBooks.id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withCategories(brewing);

        consumer.accept(book.getId(), book);
    }
}
