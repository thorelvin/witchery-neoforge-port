package com.emoniph.witchery.book;

import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookCraftingRecipePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.book.BookDisplayMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Native Modonomicon edition of the source Witches' Brews cauldron guide. */
public class CauldronBookProvider implements BookSubProvider {

    public static final Identifier BOOK_ID = WitcheryBooks.CAULDRON;

    @Override
    public void generate(BiConsumer<Identifier, BookModel> consumer, HolderLookup.Provider registries) {
        BookCategoryModel brewing = category("brewing", "Brewing", WitcheryItems.BREW_BOTTLE.get(), 0);
        BookCategoryModel crafting = category("crafting", "Cauldron Crafting", Items.CAULDRON, 1);
        BookCategoryModel rituals = category("rituals", "Cauldron Rituals", WitcheryItems.TONGUE_OF_DOG.get(), 2);
        BookCategoryModel effects = category("effects", "Effects & Actions", Items.SPIDER_EYE, 3);

        brewing.withEntries(
                entry(brewing, "brewing/the_cauldron", "The Witch's Cauldron",
                        Items.CAULDRON, 0, cauldronPages()),
                entry(brewing, "brewing/making_a_brew", "Making a Brew",
                        WitcheryItems.BREW_BOTTLE.get(), 1, makingBrewPages()),
                entry(brewing, "brewing/bottling_power", "Bottling & Power",
                        Items.GLASS_BOTTLE, 2, bottlingPages()),
                entry(brewing, "brewing/capacity", "Capacity",
                        Items.NETHER_WART, 3, BrewReferencePages.capacity()),
                entry(brewing, "brewing/modifiers", "Modifiers",
                        Items.GLOWSTONE_DUST, 4, modifierPages()),
                entry(brewing, "brewing/dispersal", "Dispersal",
                        Items.GUNPOWDER, 5, dispersalPages()));

        crafting.withEntries(
                entry(crafting, "crafting/cauldron_crafting", "Cauldron Crafting",
                        WitcheryItems.MUTANDIS.get(), 0, cauldronCraftingPages()),
                entry(crafting, "crafting/source_crafting", "The Older Index",
                        WitcheryItems.PLAYER_COMPASS.get(), 1, sourceCraftingPages()));

        rituals.withEntries(
                entry(rituals, "rituals/cauldron_rituals", "Casting from the Cauldron",
                        WitcheryItems.TONGUE_OF_DOG.get(), 0, ritualPages()),
                entry(rituals, "rituals/targets", "Waystones & Taglocks",
                        WitcheryItems.WAYSTONE.get(), 1, targetPages()),
                entry(rituals, "rituals/circles_covens", "Circles & Covens",
                        WitcheryItems.CHALK.get(), 2, circlePages()));

        effects.withEntries(
                entry(effects, "effects/source_index", "Reading the Old Index",
                        WitcheryItems.CAULDRON_BOOK.get(), 0, sourceIndexPages()),
                entry(effects, "effects/potion_reagents", "Potion Reagents",
                        Items.SPIDER_EYE, 1, BrewReferencePages.effects()),
                entry(effects, "effects/craft_actions", "World Actions",
                        WitcheryItems.WITCHES_SALT.get(), 2, BrewReferencePages.actions()));

        BookModel book = BookModel.create(BOOK_ID, "Witches' Brews")
                .withCreativeTab(WitcheryBooks.id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withBookTextOffsetX(WitcheryBooks.TEXT_OFFSET_X)
                .withBookTextOffsetWidth(WitcheryBooks.TEXT_OFFSET_WIDTH)
                .withCategories(brewing, crafting, rituals, effects);
        consumer.accept(book.getId(), book);
    }

    private static BookCategoryModel category(String id, String name, ItemLike icon, int sort) {
        return BookCategoryModel.create(WitcheryBooks.id(id), name)
                .withIcon(BookIconModel.create(icon))
                .withDisplayMode(BookDisplayMode.INDEX)
                .withSortNumber(sort);
    }

    private static BookEntryModel entry(BookCategoryModel category, String id, String name, ItemLike icon,
                                        int sort, List<BookPageModel<?>> pages) {
        return BookEntryModel.create(WitcheryBooks.id(id), name)
                .withCategory(category)
                .withIcon(BookIconModel.create(icon))
                .withLocation(0, sort)
                .withPages(pages)
                .withSortNumber(sort);
    }

    private static BookCraftingRecipePageModel recipe(String id, String title, String text) {
        return BookCraftingRecipePageModel.create()
                .withRecipeId1(WitcheryBooks.id(id))
                .withTitle1(Component.literal(title))
                .withText(WitcheryBooks.ink(text));
    }

    private static BookSpotlightPageModel spotlight(ItemLike item, String title, String text) {
        return BookSpotlightPageModel.create()
                .withItem(item)
                .withTitle(Component.literal(title))
                .withText(WitcheryBooks.ink(text));
    }

    private static List<BookPageModel<?>> cauldronPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("The Cauldron",
                "Place a normal **Cauldron** and use **Anointing Paste** upon it. The iron wakes into a Witch's "
                        + "Cauldron, fit for old craftings, composed brews, and cauldron rituals."
                        + "\n\nFill it to the brim with a Water Bucket and heat it from below. Fire, lava, a "
                        + "lit campfire, or a lit soul campfire will bring the water to working heat."));
        pages.add(recipe("anointing_paste", "Anointing Paste",
                "Use the paste on a placed vanilla Cauldron; it is not a separate crafting-table block recipe."));
        pages.add(spotlight(Items.CAULDRON, "Ready for the Work",
                "A bucket fills all three water levels. Rain and water bottles may also fill the vessel more "
                        + "slowly. A plain bucket removes full, unseasoned water and clears any unfinished brew."));
        return pages;
    }

    private static List<BookPageModel<?>> makingBrewPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Making a Brew",
                "A brew is an ordered chain of no more than **eight physical ingredients**. Drop ordinary "
                        + "reagents into the hot bowl one at a time. Each accepted item is consumed and the water "
                        + "takes its colour."
                        + "\n\nSome dual-purpose ingredients must be **sneak-stirred** by using them directly on "
                        + "the cauldron. This keeps them from being swallowed while the cauldron is waiting for "
                        + "an old crafting recipe. The reference pages mark every such ingredient."));
        pages.addAll(WitcheryBooks.prose("Order Matters",
                "Capacity must be opened before an effect or world action can fit. Strength, duration, inversion, "
                        + "particle, and target modifiers shape the **next** accepted effect or action, then their "
                        + "temporary state resets."
                        + "\n\nA rejected ingredient remains yours. If the bowl refuses an effect, add enough "
                        + "capacity first; if it refuses everything, the eight-item batch is already full or a "
                        + "final ritual target has closed the recipe."));
        pages.addAll(BrewReferencePages.exampleBrews());
        return pages;
    }

    private static List<BookPageModel<?>> bottlingPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Bottling",
                "Use a **Glass Bottle** on a heated composed brew. Each bottle takes one of the cauldron's three "
                        + "water levels, so a brim-full batch yields three bottles. The ingredient chain remains "
                        + "seasoned in the vessel until the last water level is drawn."
                        + "\n\nUse an empty **Bucket** instead to take the whole batch as one moving Brew Bucket. "
                        + "That empties and clears the cauldron immediately."));
        pages.addAll(WitcheryBooks.prose("Altar Power",
                "Every ingredient may add a power cost. Bottling charges that cost **for each bottle**; taking a "
                        + "Brew Bucket charges it once for the whole batch. A valid altar within eight blocks must "
                        + "hold enough power before either vessel can be filled."
                        + "\n\nA brew finished with a ritual release ingredient or a living Taglock target cannot "
                        + "be bottled. Complete that working with Tongue of Dog instead."));
        return pages;
    }

    private static List<BookPageModel<?>> modifierPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Modifiers",
                "Modifiers are read from left to right through the ingredient chain. Strength and duration climb "
                        + "only to the ceiling shown beside their reagent. Inversion changes the next compatible "
                        + "effect or action; colour and drinking speed shape the finished vessel."));
        pages.addAll(BrewReferencePages.modifiers());
        return pages;
    }

    private static List<BookPageModel<?>> dispersalPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Dispersal",
                "With no dispersal reagent, the result is a draught. **Splash** bursts at impact; **gas** and "
                        + "**liquid** travel through the world and repeatedly touch nearby targets. Extent widens "
                        + "the affected area, while lifetime keeps moving brews active longer."
                        + "\n\nA triggered curse always takes precedence over splash, gas, or liquid ingredients. "
                        + "There is no vanilla Dragon's Breath shortcut: use the Witchery reagents listed here."));
        pages.addAll(BrewReferencePages.reachAndTargets());
        pages.addAll(BrewReferencePages.dispersal());
        return pages;
    }

    private static List<BookPageModel<?>> cauldronCraftingPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Old Craftings",
                "For these workings, drop the loose offerings into a heated cauldron with water, then use the final "
                        + "trigger item on the vessel. The result rises from the bowl. Powered recipes require a "
                        + "valid altar within eight blocks and consume one water level."));
        pages.add(BrewReferencePages.vesselRecipe("Mutandis", "Use Exhale of the Horned One last.",
                WitcheryItems.MUTANDIS.get(), 6,
                List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.EGG,
                        WitcheryItems.EXHALE_OF_HORNED_ONE.get())));
        pages.add(BrewReferencePages.vesselRecipe("Mutandis Extremis", "Use Mutandis last.",
                WitcheryItems.MUTANDIS_EXTREMIS.get(), 1,
                List.of(Items.NETHER_WART, WitcheryItems.MUTANDIS.get())));
        pages.add(BrewReferencePages.vesselRecipe("Drop of Luck", "Use Mutandis Extremis last; costs 6,930 power.",
                WitcheryItems.DROP_OF_LUCK.get(), 1,
                List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.NETHER_WART,
                        WitcheryItems.TEAR_OF_GODDESS.get(), WitcheryItems.REFINED_EVIL.get(),
                        WitcheryItems.MUTANDIS_EXTREMIS.get())));
        pages.add(BrewReferencePages.vesselRecipe("Mutating Sprig",
                "Use Mutandis Extremis last; costs 3,080 power.", WitcheryItems.MUTATOR.get(), 1,
                List.of(Items.NETHER_WART, WitcheryItems.ENT_TWIG.get(),
                        WitcheryItems.MUTANDIS_EXTREMIS.get())));
        pages.add(BrewReferencePages.vesselRecipe("Otherwhere Chalk", "Use white Chalk last; costs 4,410 power.",
                WitcheryItems.CHALK_OTHERWHERE.get(), 1,
                List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(), Items.ENDER_PEARL,
                        WitcheryItems.CHALK.get())));
        pages.add(BrewReferencePages.vesselRecipe("Infernal Chalk", "Use white Chalk last; costs 3,150 power.",
                WitcheryItems.CHALK_INFERNAL.get(), 1,
                List.of(Items.NETHER_WART, Items.BLAZE_POWDER, WitcheryItems.CHALK.get())));
        pages.add(BrewReferencePages.vesselRecipe("Golden Chalk", "Use white Chalk last; costs 2,870 power.",
                WitcheryItems.CHALK_GOLDEN.get(), 1,
                List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.GOLD_NUGGET, WitcheryItems.CHALK.get())));
        pages.add(BrewReferencePages.vesselRecipe("Dirt Pit Trap",
                "Use Tormented Twine last; costs 4,000 power and yields four traps.",
                WitcheryItems.PIT_DIRT.get(), 4,
                List.of(Items.DIRT, WitcheryItems.MANDRAKE_ROOT.get(), WitcheryItems.TORMENTED_TWINE.get())));
        pages.add(BrewReferencePages.vesselRecipe("Grass Pit Trap",
                "Use Tormented Twine last; costs 4,000 power and yields four traps.",
                WitcheryItems.PIT_GRASS.get(), 4,
                List.of(Items.DIRT, Items.WHEAT_SEEDS, Items.DANDELION,
                        WitcheryItems.TORMENTED_TWINE.get())));
        return pages;
    }

    private static List<BookPageModel<?>> sourceCraftingPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("The Older Index",
                "The old book also indexed cauldron cooking, Nether Wart, End Stone, Rotten Flesh, and a Player "
                        + "Compass. Those old transmutations are **not accepted by the present cauldron**. They "
                        + "remain part of the source record, but this guide will not pretend an unavailable "
                        + "working can be performed."
                        + "\n\nThe Player Compass has a modern crafting recipe and can also be bound with a filled "
                        + "player Taglock Kit. Chalk, Mutandis, Mutandis Extremis, Drop of Luck, and the Mutating "
                        + "Sprig also keep ordinary recipe bridges for accessibility."));
        pages.add(spotlight(WitcheryItems.PLAYER_COMPASS.get(), "Player Compass",
                "Craft the blank compass, then bind it directly to a player or combine it with that player's "
                        + "filled Taglock Kit."));
        return pages;
    }

    private static List<BookPageModel<?>> ritualPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Casting from the Cauldron",
                "Begin with a **brim-full**, heated cauldron and a finished effect or world action. Sneak-use "
                        + "**Tongue of Dog** on the vessel to release the brew without bottling it. A valid altar "
                        + "within eight blocks must pay the full ritual power cost."
                        + "\n\nDelivery is checked before power is spent. A ritual that lacks a valid target, "
                        + "cannot place its gas or liquid, or conflicts with its dispersal remains in the bowl so "
                        + "you can correct the working."));
        pages.addAll(WitcheryBooks.prose("Risk",
                "Misfortune is tested only after a powered ritual succeeds. Casting without a complete small or "
                        + "medium ritual circle adds risk; Infernal circles and the Shade of Leonard add further "
                        + "risk. Backfire may bring Slowness, Paralysis, Insanity, or lightning."
                        + "\n\nA successful release empties the cauldron. Protective Hunter clothing, Area "
                        + "Markers, and Voodoo Protection Poppets may still intercept hostile target workings."));
        return pages;
    }

    private static List<BookPageModel<?>> targetPages() {
        List<BookPageModel<?>> pages = new ArrayList<>(BrewReferencePages.ritualTargets());
        pages.addAll(WitcheryBooks.prose("Places and People",
                "A **filled Taglock Kit** placed last names a living target and closes the ingredient chain. "
                        + "Location workings use the appropriate bound Waystone offering near the cauldron. Keep "
                        + "target items out of the recipe until the brew itself is finished."
                        + "\n\nCoven reach is finite at first. A full six-witch coven removes the same-world "
                        + "distance limit; crossing dimensions additionally requires the Shade of Leonard."));
        return pages;
    }

    private static List<BookPageModel<?>> circlePages() {
        return WitcheryBooks.prose("Circles & Covens",
                "A complete small or medium ritual circle removes the risk carried by casting with no circle. "
                        + "Infernal circles are more dangerous. Keep the altar structure valid before release; a "
                        + "failed delivery validation leaves the brew in the cauldron."
                        + "\n\nNearby coven witches raise the ritual's permitted strength and extend target reach. The source "
                        + "examples of raised land and extended blindness are now compositions: choose the world "
                        + "action or effect reagent, open its capacity, then add ritual delivery and release it "
                        + "from the cauldron.");
    }

    private static List<BookPageModel<?>> sourceIndexPages() {
        return WitcheryBooks.prose("Reading the Old Index",
                "The source book grouped effects under capacity levels I, II, IV, V, VI, VIII, and XII. The "
                        + "modern edition separates **Potion Reagents** from **World Actions** and prints each "
                        + "accepted ingredient directly from the live brew registry."
                        + "\n\nA row's **level** is the capacity it consumes; **power** is charged by the altar. "
                        + "Inverted names show what Fermented Spider Eye can turn that reagent into. If an old "
                        + "effect has no live row, it is not an accepted recipe in this build.");
    }

    /** Maps every one of the 196 source markup keys to its modern Modonomicon home. */
    static Map<String, String> legacyCoverage() {
        LinkedHashMap<String, String> coverage = new LinkedHashMap<>();
        cover(coverage, "crafting/cauldron_crafting", "tbench tcraft tritual");
        cover(coverage, "brewing/the_cauldron", "toc introduction cauldron cauldron2");
        cover(coverage, "crafting/cauldron_crafting",
                "crafting toc/crafting2 crafting3 boiledmeat mutandis mutandisextremis mutatingsprig dropofluck "
                        + "otherwherechalk infernalchalk goldenchalk netherwart endstone rottenflesh pittraps compass");
        cover(coverage, "brewing/making_a_brew", "brewing brewing2 brewing3 brewing4 brewing5 brewing6");
        cover(coverage, "brewing/bottling_power", "bottling bottling2 power");
        cover(coverage, "rituals/cauldron_rituals", "rituals rituals4 rituals5 rituals9 otherrituals");
        cover(coverage, "rituals/targets", "rituals2 ritualwaystone ritualwaystoneblooded ritualtranspose ritualfind");
        cover(coverage, "rituals/circles_covens", "rituals3 rituals6 rituals7 rituals8 rituals10 rituals11");
        cover(coverage, "brewing/capacity", "capacity");
        cover(coverage, "brewing/modifiers", "duration modifiers modifiers2 modifiers3 modifiers4 modifiers5");
        cover(coverage, "brewing/dispersal", "dispersal dispersaltrigger dispersaltrigger2 extent linger");
        cover(coverage, "effects/source_index",
                "toc/effects level1 level1_2 level1_3 level1_4 level2 level2_2 level2_3 level2_4 level2_5 "
                        + "level2_6 level2_7 level2_8 level2_9 level4 level4_2 level4_3 level4_4 level4_5 level4_6 "
                        + "level5 level5_2 level6 level6_2 level8 level12 endlesswater growtree removebuffs "
                        + "insectbane knockback undeadbane thorns sprouting freeze erosion levelland webs airhike "
                        + "pull batburst poisonweapon attractarrows reflectarrows slowfall jump floating damageboost "
                        + "weakness heal harm nightvision invisible fastmove slowmove waterbreathing resistfire "
                        + "partlava repel poison regeneration harmwerewolves weakenvampires animalattraction "
                        + "animalrepulsion flames blindness disease insanity sinking overheating nightmare "
                        + "absorbtion healthboost wasting fullness revealing blight raisedead raiseland gruesprey "
                        + "wither demonbane inferno blast poisontoad reflectdamage iceshell fortune undeadscurse "
                        + "snowburst swimspeed enderinhibition partwater extinguish dissipategas growflowers "
                        + "fertilize harvest tilling pruning felling growlily tinting transpose transpose2 transpose3 "
                        + "transposeore frogsleg fear removedebuffs paralysis combustion illfitting leonard "
                        + "durationboost absorbmagic reincarnate moonshine stoutbelly brewgasimmunity drainmagic "
                        + "keepinventory shiftingseasons keepeffects spreaddebuffs brewbottling planting pulverize "
                        + "vines love volatility iceworld resizing stealbuffs wolfsbane");
        return Collections.unmodifiableMap(coverage);
    }

    private static void cover(Map<String, String> coverage, String entry, String keys) {
        for (String key : keys.split("\\s+")) {
            String previous = coverage.putIfAbsent(key, entry);
            if (previous != null) {
                throw new IllegalStateException("Duplicate legacy Cauldron Book key: " + key);
            }
        }
    }
}
