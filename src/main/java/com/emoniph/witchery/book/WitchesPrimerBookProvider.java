package com.emoniph.witchery.book;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.BookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookAdvancementConditionModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookCraftingRecipePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookEntityPageModel;
import com.emoniph.witchery.book.page.ItemReferencePageModel;
import com.emoniph.witchery.book.page.StationRecipePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.book.BookDisplayMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Datagen for the native Modonomicon "Witches' Primer" — one growing book whose sections unlock as
 * the player crafts the matching topic book.
 *
 * <p>Structure: 12 categories aligned to the guide books. <b>Getting Started</b> is always open;
 * the other 11 are gated behind an advancement granted when the player obtains that category's book
 * (see {@code data/witchery/advancement/guide/}), and hidden until then. Each gated category's last
 * entry, "What Comes Next", shows the recipe for the next book in the chain.
 *
 * <p>Authoring rules: body prose goes through {@link WitcheryBooks#prose} (auto-paginates); recipe /
 * spotlight / entity pages always carry an explicit title (Modonomicon otherwise mutates the item's
 * shared name component, leaking style to its tooltip); no broken legacy banner images.
 */
public class WitchesPrimerBookProvider implements BookSubProvider {

    public static final Identifier BOOK_ID = WitcheryBooks.PRIMER;

    private static Identifier id(String path) {
        return WitcheryBooks.id(path);
    }

    private static final Identifier BG_OVEN = id("textures/gui/book/bg_oven.png");
    private static final Identifier BG_DISTILLERY = id("textures/gui/book/bg_distillery.png");
    private static final Identifier BG_CAULDRON = id("textures/gui/book/bg_cauldron.png");
    private static final Identifier BG_RITUAL = id("textures/gui/book/ritual_circle_magic.png");
    private static final int REFERENCE_FIRST_PAGE_ROWS = 4;
    private static final int REFERENCE_CONTINUATION_ROWS = 6;

    private record RecipeSlot(ItemLike item, int count, int x, int y) {}
    private record RecipeStack(ItemLike item, int count) {}

    private static RecipeSlot slot(ItemLike item, int x, int y) {
        return slot(item, 1, x, y);
    }

    private static RecipeSlot slot(ItemLike item, int count, int x, int y) {
        return new RecipeSlot(item, count, x, y);
    }

    private static RecipeStack stack(ItemLike item) {
        return stack(item, 1);
    }

    private static RecipeStack stack(ItemLike item, int count) {
        return new RecipeStack(item, count);
    }

    private static BookCraftingRecipePageModel recipe(String recipeId, String title, String caption) {
        return BookCraftingRecipePageModel.create()
                .withRecipeId1(id(recipeId))
                .withTitle1(Component.literal(title))
                .withText(WitcheryBooks.ink(caption));
    }

    private static BookSpotlightPageModel spotlight(ItemLike item, String title, String caption) {
        return BookSpotlightPageModel.create()
                .withItem(item)
                .withTitle(Component.literal(title))
                .withText(WitcheryBooks.ink(caption));
    }

    private static List<BookPageModel<?>> customBrewReference() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Brew Reagents",
                "A custom brew remembers each ingredient in the order it enters the Witch's Cauldron. "
                        + "Drop ordinary brew ingredients into heated water; ingredients marked **sneak-stir** "
                        + "must be added by sneaking and right-clicking the cauldron, so they do not feed a "
                        + "different cauldron recipe. Ingredients marked **drop or sneak-stir** accept either "
                        + "gesture."
                        + "\n\nThe notes below are drawn from the live brew registry. **Level** is the capacity "
                        + "an effect consumes. **Power** is altar power charged when the brew is bottled, "
                        + "bucketed, or released by ritual."));
        pages.add(vesselRecipe("Brew: Drought Splash",
                "Stir Nether Wart, Tear of the Goddess, Witches' Salt, then Gunpowder. Bottle it to dry "
                        + "water source blocks near impact.",
                WitcheryItems.SPLASH_BREW_BOTTLE.get(), 1,
                List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(),
                        WitcheryItems.WITCHES_SALT.get(), Items.GUNPOWDER, Items.GLASS_BOTTLE)));
        pages.add(vesselRecipe("Brew: Drought Bucket",
                "Swap Gunpowder for Wormwood, then scoop with a bucket. The moving brew eats water source "
                        + "blocks it touches.",
                WitcheryItems.BREW_BUCKET.get(), 1,
                List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(),
                        WitcheryItems.WITCHES_SALT.get(), WitcheryItems.WORMWOOD.get(), Items.BUCKET)));

        referenceSection(pages, "Capacity",
                "Capacity ingredients widen the brew before an effect or action can fit.",
                Brews.Ingredient.Kind.CAPACITY);
        referenceSection(pages, "Power & Duration",
                "These shape the next effect or action, then reset after that working is recorded.",
                Brews.Ingredient.Kind.STRENGTH, Brews.Ingredient.Kind.DURATION,
                Brews.Ingredient.Kind.INVERT, Brews.Ingredient.Kind.DISABLE_STRENGTH_CEILING,
                Brews.Ingredient.Kind.NO_PARTICLES, Brews.Ingredient.Kind.DRINK_SPEED,
                Brews.Ingredient.Kind.COLOR);
        referenceSection(pages, "Reach & Targets",
                "These alter where the brew may act, how wide it reaches, or how long moving gas/liquid persists.",
                Brews.Ingredient.Kind.EXTENT, Brews.Ingredient.Kind.LIFETIME,
                Brews.Ingredient.Kind.DISABLE_BLOCK_TARGET, Brews.Ingredient.Kind.DISABLE_ENTITY_TARGET);
        referenceSection(pages, "Dispersal",
                "Dispersal ingredients choose how the finished brew leaves its bottle or cauldron.",
                Brews.Ingredient.Kind.SPLASH, Brews.Ingredient.Kind.GAS, Brews.Ingredient.Kind.LIQUID,
                Brews.Ingredient.Kind.TRIGGER, Brews.Ingredient.Kind.RITUAL, Brews.Ingredient.Kind.WARNING);
        referenceSection(pages, "Potion Reagents",
                "These ingredients become the brew's potion effect when enough capacity is available.",
                Brews.Ingredient.Kind.EFFECT);
        referenceSection(pages, "Action Reagents",
                "These ingredients perform an old craft action instead of applying a normal potion effect.",
                Brews.Ingredient.Kind.ACTION);
        return pages;
    }

    private static void referenceSection(List<BookPageModel<?>> pages, String title, String intro,
                                         Brews.Ingredient.Kind... kinds) {
        List<Brews.IngredientReference> refs = new ArrayList<>();
        for (Brews.IngredientReference ref : Brews.ingredientReferences()) {
            if (matchesKind(ref, kinds)) {
                refs.add(ref);
            }
        }
        if (refs.isEmpty()) {
            return;
        }

        int index = 0;
        boolean firstPage = true;
        while (index < refs.size()) {
            int rowsThisPage = firstPage ? REFERENCE_FIRST_PAGE_ROWS : REFERENCE_CONTINUATION_ROWS;
            ItemReferencePageModel page = ItemReferencePageModel.create()
                    .withTitle(Component.literal(firstPage ? title : title + " (cont.)"));
            if (firstPage) {
                page.withText(WitcheryBooks.ink(intro));
            }

            int end = Math.min(refs.size(), index + rowsThisPage);
            for (; index < end; index++) {
                Brews.IngredientReference ref = refs.get(index);
                page.withRow(ref.item(), WitcheryBooks.ink(referenceLine(ref)));
            }
            pages.add(page);
            firstPage = false;
        }
    }

    private static boolean matchesKind(Brews.IngredientReference ref, Brews.Ingredient.Kind... kinds) {
        for (Brews.Ingredient.Kind kind : kinds) {
            if (ref.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private static String referenceLine(Brews.IngredientReference ref) {
        String role = switch (ref.kind()) {
            case EFFECT -> effectRole(ref);
            case ACTION -> actionRole(ref);
            case CAPACITY -> "capacity +" + ref.capacityIncrement() + " up to " + ref.capacityCeiling();
            case STRENGTH -> "strength tier " + ref.modifierCeiling();
            case DURATION -> "duration tier " + ref.modifierCeiling();
            case INVERT -> "inverts the next effect";
            case SPLASH -> ref.item() == WitcheryItems.ARTICHOKE.get()
                    ? "source instant/splash dispersal" : "splash dispersal";
            case GAS -> "moving gas dispersal";
            case LIQUID -> "moving liquid dispersal";
            case TRIGGER -> "triggered curse dispersal";
            case WARNING -> "binds nearby spoken warning text";
            case RITUAL -> "ritual cauldron release trigger";
            case COLOR -> "sets bottle colour";
            case DRINK_SPEED -> ref.drinkSpeedModifier() < 0
                    ? "faster drinking" : "slower drinking";
            case EXTENT -> "impact extent tier " + ref.modifierCeiling();
            case LIFETIME -> "gas/liquid lifetime tier " + ref.modifierCeiling();
            case NO_PARTICLES -> "hides potion particles";
            case DISABLE_BLOCK_TARGET -> "skips block targets";
            case DISABLE_ENTITY_TARGET -> "skips entity targets";
            case DISABLE_STRENGTH_CEILING -> "removes the strength ceiling";
        };
        return "**" + itemName(ref.item()) + "** - " + role + referenceMeta(ref) + ".";
    }

    private static String referenceMeta(Brews.IngredientReference ref) {
        List<String> parts = new ArrayList<>();
        if (ref.capacityRequired() > 0) {
            parts.add("level " + ref.capacityRequired());
        }
        if (ref.powerCost() > 0) {
            parts.add(ref.powerCost() + " power");
        }
        if (ref.explicitStirOnly()) {
            parts.add("sneak-stir");
        } else if (ref.explicitStirAllowed()) {
            parts.add("drop or sneak-stir");
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private static String effectRole(Brews.IngredientReference ref) {
        String effect = nameFromId(ref.effectId());
        if (ref.invertedEffectId() != null) {
            effect += " / inverted: " + nameFromId(ref.invertedEffectId());
        }
        return effect;
    }

    private static String actionRole(Brews.IngredientReference ref) {
        String action = actionName(ref.actionNameKey());
        if (ref.invertedActionNameKey() != null) {
            action += " / inverted: " + actionName(ref.invertedActionNameKey());
        }
        return action;
    }

    private static String itemName(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return "Unknown";
        }
        return switch (id.toString()) {
            case "witchery:attuned_stone_charged" -> "Charged Attuned Stone";
            case "witchery:breath_of_goddess" -> "Breath of the Goddess";
            case "witchery:exhale_of_horned_one" -> "Exhale of the Horned One";
            case "witchery:mindrakebulb" -> "Mindrake Bulb";
            case "witchery:tear_of_goddess" -> "Tear of the Goddess";
            case "witchery:witches_salt" -> "Witches' Salt";
            case "minecraft:dragon_breath" -> "Dragon's Breath";
            default -> titleCase(id.getPath());
        };
    }

    private static String nameFromId(Identifier id) {
        return id == null ? "Unknown" : titleCase(id.getPath());
    }

    private static String actionName(String key) {
        if (key == null) {
            return "Unknown";
        }
        return switch (key) {
            case "witchery.brew.extinguish" -> "Extinguish";
            case "witchery.brew.dissipate" -> "Dissipate";
            case "witchery.brew.drought" -> "Drought";
            case "witchery.brew.flowers" -> "Grow Flowers";
            case "witchery.brew.fertilization" -> "Fertilization";
            case "witchery.brew.felling" -> "Felling";
            case "witchery.brew.lilify" -> "Lilify";
            case "witchery.brew.blight" -> "Blight";
            case "witchery.brew.raiseland" -> "Raise Land";
            case "witchery.brew.sprouting" -> "Sprouting";
            case "witchery.brew.planting" -> "Planting";
            case "witchery.brew.transpose" -> "Transposition";
            case "witchery.brew.raising" -> "Raising";
            case "witchery.brew.spreaddebuffs" -> "Spread Affliction";
            case "witchery.brew.animal_attraction" -> "Animal Attraction";
            case "witchery.brew.animal_repulsion" -> "Animal Repulsion";
            case "witchery.brew.removebuffs" -> "Cure Buffs";
            case "witchery.brew.removedebuffs" -> "Cure Debuffs";
            case "witchery.brew.harmundead" -> "Undeadbane";
            case "witchery.brew.harminsects" -> "Insectbane";
            case "witchery.brew.weakenvampires" -> "Weaken Vampires";
            case "witchery.brew.harmdemons" -> "Demonbane";
            case "witchery.brew.harmwerewolves" -> "Harm Werewolves";
            case "witchery.brew.stealbuffs" -> "Steal Buffs";
            case "witchery.brew.durationboost" -> "Potion Longevity";
            case "witchery.brew.airhike" -> "Air Hike";
            case "witchery.brew.frogtongue" -> "Frog's Tongue";
            default -> titleCase(key.substring(key.lastIndexOf('.') + 1));
        };
    }

    private static String titleCase(String path) {
        String[] words = path.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            String lower = word.toLowerCase(java.util.Locale.ROOT);
            if (out.length() > 0 && (lower.equals("of") || lower.equals("and") || lower.equals("the"))) {
                out.append(lower);
            } else {
                out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return out.toString();
    }

    // ── Station-recipe pages (live item overlays) ──────────────────────────────────────────────────
    // Uses old empty machine backgrounds with live item stacks overlaid in the slots, so every
    // ingredient/result keeps its normal Minecraft tooltip.
    private static StationRecipePageModel stationRecipe(Identifier background, int width, int height,
                                                        String title, String caption, RecipeSlot... slots) {
        StationRecipePageModel page = StationRecipePageModel.create()
                .withBackground(background, width, height)
                .withText(WitcheryBooks.ink(caption));
        if (!title.isBlank()) {
            page.withTitle(Component.literal(title));
        }
        for (RecipeSlot slot : slots) {
            page.withSlot(slot.item(), slot.count(), slot.x(), slot.y());
        }
        return page;
    }

    private static StationRecipePageModel ovenRecipe(String title, String caption, ItemLike input, ItemLike result,
                                                     ItemLike byproduct) {
        return stationRecipe(BG_OVEN, 96, 70, title, caption,
                slot(input, 8, 9),
                slot(Items.COAL, 8, 45),
                slot(WitcheryItems.CLAY_JAR.get(), 35, 45),
                slot(result, 70, 13),
                slot(byproduct, 70, 45));
    }

    private static StationRecipePageModel ovenRecipeNote(String title, String caption, ItemLike input, ItemLike result) {
        return ovenRecipeNote(title, caption, input, result, WitcheryItems.WOOD_ASH.get());
    }

    private static StationRecipePageModel ovenRecipeNote(String title, String caption, ItemLike input, ItemLike result,
                                                         ItemLike byproduct) {
        return ovenRecipe(title, caption, input, result, byproduct)
                .withShowBackground(false)
                .withOvenCues(true);
    }

    private static StationRecipePageModel distilleryRecipe(String title, String caption, ItemLike input1,
                                                           ItemLike input2, int jars, RecipeStack... outputs) {
        StationRecipePageModel page = stationRecipe(BG_DISTILLERY, 114, 70, title, caption,
                slot(input1, 16, 7),
                slot(WitcheryItems.CLAY_JAR.get(), jars, 16, 47));
        if (input2 != null) {
            page.withSlot(input2, 1, 16, 25);
        }
        int[][] outputSlots = {{72, 18}, {90, 18}, {72, 36}, {90, 36}};
        for (int i = 0; i < Math.min(outputs.length, outputSlots.length); i++) {
            page.withSlot(outputs[i].item(), outputs[i].count(), outputSlots[i][0], outputSlots[i][1]);
        }
        return page;
    }

    private static StationRecipePageModel distilleryRecipeNote(String title, String caption, ItemLike input1,
                                                               ItemLike input2, int jars, RecipeStack... outputs) {
        return distilleryRecipe(title, caption, input1, input2, jars, outputs)
                .withShowBackground(false)
                .withDistilleryCues(true);
    }

    private static StationRecipePageModel vesselRecipe(String title, String caption, ItemLike result, int count,
                                                       List<ItemLike> ingredients) {
        StationRecipePageModel page = stationRecipe(BG_CAULDRON, 112, 60, title, caption);
        int perRow = 3;
        int pitch = 16;
        int centerX = 51;
        for (int i = 0; i < ingredients.size(); i++) {
            int row = i / perRow;
            int col = i % perRow;
            int itemsInRow = Math.min(perRow, ingredients.size() - row * perRow);
            int rowWidth = itemsInRow * pitch;
            int x = centerX - rowWidth / 2 + col * pitch;
            int y = 2 + row * pitch;
            page.withSlot(ingredients.get(i), 1, x, y);
        }
        page.withSlot(result, count, 89, 33);
        return page;
    }

    private static StationRecipePageModel stationRecipeCard(String key, String title, String caption) {
        return switch (key) {
            case "cauldron_mutandis" -> vesselRecipe(title, caption, WitcheryItems.MUTANDIS.get(), 6,
                    List.of(WitcheryItems.MANDRAKE_ROOT.get(), WitcheryItems.EXHALE_OF_HORNED_ONE.get(), Items.EGG));
            case "cauldron_mutandis_extremis" -> vesselRecipe(title, caption, WitcheryItems.MUTANDIS_EXTREMIS.get(), 1,
                    List.of(Items.NETHER_WART, WitcheryItems.MUTANDIS.get()));
            case "cauldron_drop_of_luck" -> vesselRecipe(title, caption, WitcheryItems.DROP_OF_LUCK.get(), 1,
                    List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(),
                            WitcheryItems.REFINED_EVIL.get(), WitcheryItems.MUTANDIS_EXTREMIS.get()));
            case "kettle_redstone_soup" -> vesselRecipe(title, caption, WitcheryItems.REDSTONE_SOUP.get(), 1,
                    List.of(Items.REDSTONE, WitcheryItems.DROP_OF_LUCK.get(), WitcheryItems.BAT_WOOL.get(),
                            WitcheryItems.TONGUE_OF_DOG.get(), WitcheryItems.BELLADONNA.get(),
                            WitcheryItems.MANDRAKE_ROOT.get()));
            case "kettle_brew_of_sleeping" -> vesselRecipe(title, caption, WitcheryItems.BREW_OF_SLEEPING.get(), 3,
                    List.of(WitcheryItems.PURIFIED_MILK.get(), Items.COOKIE, WitcheryItems.BREW_OF_LOVE.get(),
                            WitcheryItems.WHIFF_OF_MAGIC.get(), WitcheryItems.ICY_NEEDLE.get(),
                            WitcheryItems.ARTICHOKE.get()));
            case "oven_odour_of_purity" -> ovenRecipeNote(title, caption, WitcheryItems.HAWTHORN_SAPLING.get(),
                    WitcheryItems.ODOUR_OF_PURITY.get());
            case "oven_breath_of_goddess" -> ovenRecipeNote(title, caption, Items.BIRCH_SAPLING,
                    WitcheryItems.BREATH_OF_GODDESS.get());
            case "oven_whiff_of_magic" -> ovenRecipeNote(title, caption, WitcheryItems.ROWAN_SAPLING.get(),
                    WitcheryItems.WHIFF_OF_MAGIC.get());
            case "oven_exhale" -> ovenRecipeNote(title, caption, Items.OAK_SAPLING,
                    WitcheryItems.EXHALE_OF_HORNED_ONE.get());
            case "oven_hint_of_rebirth" -> ovenRecipeNote(title, caption, Items.SPRUCE_SAPLING,
                    WitcheryItems.HINT_OF_REBIRTH.get());
            case "oven_reek_of_misfortune" -> ovenRecipeNote(title, caption, WitcheryItems.ALDER_SAPLING.get(),
                    WitcheryItems.REEK_OF_MISFORTUNE.get());
            case "oven_foul_fume_acacia" -> ovenRecipeNote(title, caption, Items.ACACIA_SAPLING,
                    WitcheryItems.FOUL_FUME.get());
            case "oven_foul_fume_dark_oak" -> ovenRecipeNote(title, caption, Items.DARK_OAK_SAPLING,
                    WitcheryItems.FOUL_FUME.get());
            case "oven_foul_fume_jungle" -> ovenRecipeNote(title, caption, Items.JUNGLE_SAPLING,
                    WitcheryItems.FOUL_FUME.get());
            case "oven_charcoal" -> ovenRecipeNote(title, caption, Items.OAK_LOG, Items.CHARCOAL,
                    WitcheryItems.FOUL_FUME.get());
            case "distillery_oil_of_vitriol" -> distilleryRecipeNote(title, caption, WitcheryItems.FOUL_FUME.get(),
                    WitcheryItems.QUICKLIME.get(), 1, stack(WitcheryItems.GYPSUM.get()),
                    stack(WitcheryItems.OIL_OF_VITRIOL.get()), stack(Items.SLIME_BALL));
            case "distillery_diamond_vapour" -> distilleryRecipeNote(title, caption, Items.DIAMOND,
                    WitcheryItems.OIL_OF_VITRIOL.get(), 3, stack(WitcheryItems.DIAMOND_VAPOUR.get()),
                    stack(WitcheryItems.DIAMOND_VAPOUR.get()), stack(WitcheryItems.ODOUR_OF_PURITY.get()));
            case "distillery_four_essences" -> distilleryRecipeNote(title, caption, WitcheryItems.DIAMOND_VAPOUR.get(),
                    Items.GHAST_TEAR, 3, stack(WitcheryItems.ODOUR_OF_PURITY.get()),
                    stack(WitcheryItems.REEK_OF_MISFORTUNE.get()), stack(WitcheryItems.FOUL_FUME.get()),
                    stack(WitcheryItems.REFINED_EVIL.get()));
            case "distillery_tear_of_goddess" -> distilleryRecipeNote(title, caption,
                    WitcheryItems.BREATH_OF_GODDESS.get(), Items.LAPIS_LAZULI, 3,
                    stack(WitcheryItems.TEAR_OF_GODDESS.get()), stack(WitcheryItems.WHIFF_OF_MAGIC.get()),
                    stack(Items.SLIME_BALL), stack(WitcheryItems.FOUL_FUME.get()));
            case "distillery_glowstone_dust" -> distilleryRecipeNote(title, caption, Items.BLAZE_POWDER,
                    Items.GUNPOWDER, 1, stack(Items.GLOWSTONE_DUST), stack(Items.GLOWSTONE_DUST),
                    stack(WitcheryItems.REEK_OF_MISFORTUNE.get()));
            case "distillery_ender_dew" -> distilleryRecipeNote(title, caption, Items.ENDER_PEARL,
                    null, 6, stack(WitcheryItems.ENDER_DEW.get(), 2), stack(WitcheryItems.ENDER_DEW.get(), 2),
                    stack(WitcheryItems.ENDER_DEW.get()), stack(WitcheryItems.WHIFF_OF_MAGIC.get()));
            case "distillery_condensed_fear" -> distilleryRecipeNote(title, caption,
                    WitcheryItems.BREW_OF_FLOWING_SPIRIT.get(), WitcheryItems.OIL_OF_VITRIOL.get(), 2,
                    stack(WitcheryItems.FOCUSED_WILL.get()), stack(WitcheryItems.CONDENSED_FEAR.get()),
                    stack(WitcheryItems.BREW_OF_HOLLOW_TEARS.get(), 4),
                    stack(WitcheryItems.BREW_OF_HOLLOW_TEARS.get(), 4));
            case "distillery_infernal_blood_netherrack" -> distilleryRecipeNote(title, caption,
                    WitcheryItems.DEMON_HEART.get(), Items.NETHERRACK, 2, stack(Items.SOUL_SAND),
                    stack(WitcheryItems.INFERNAL_BLOOD.get()), stack(WitcheryItems.INFERNAL_BLOOD.get()));
            case "distillery_infernal_blood" -> distilleryRecipeNote(title, caption,
                    WitcheryItems.DEMON_HEART.get(), WitcheryItems.DIAMOND_VAPOUR.get(), 4,
                    stack(WitcheryItems.INFERNAL_BLOOD.get(), 2), stack(WitcheryItems.INFERNAL_BLOOD.get(), 2),
                    stack(WitcheryItems.REFINED_EVIL.get()));
            default -> throw new IllegalArgumentException("Unknown live recipe card: " + key);
        };
    }

    // ── Circle Magic rite cards ─────────────────────────────────────────────────────────────────────
    // A clean rite card draws the ring setup as concentric chalk circles (the visual centerpiece), the
    // offerings as live item slots with tooltips, a power/size caption, and one witch face per coven member.
    private static final int RITUAL_CANVAS_W = 116;
    private static final int RITUAL_CANVAS_H = 120;
    private static final int RITUAL_OFFERING_SINGLE_Y = 76;
    private static final int RITUAL_OFFERING_TOP_Y = 68;
    private static final int RITUAL_OFFERING_BOTTOM_Y = 87;

    /** One chalk circle of a ring setup. {@code color}: white(ritual)/purple(otherwhere)/red(infernal); size small/medium/large. */
    private record Ring(String color, String size) {}

    private static Ring ringWhite(String size) { return new Ring("white", size); }
    private static Ring ringOtherwhere(String size) { return new Ring("purple", size); }
    private static Ring ringInfernal(String size) { return new Ring("red", size); }

    private static String ringFootprint(String size) {
        return switch (size) { case "small" -> "7x7"; case "large" -> "15x15"; default -> "11x11"; };
    }

    private static String chalkName(String color) {
        return switch (color) { case "red" -> "Infernal"; case "purple" -> "Otherwhere"; default -> "Ritual"; };
    }

    /**
     * Build a Circle Magic rite card: the {@code rings} drawn concentrically (largest first), the
     * {@code offerings} as a centred row of live item slots, a caption summarising ring size / chalk /
     * altar power, and {@code coven} witch faces. The ring diagram is the page's centerpiece (no panel art).
     */
    private static StationRecipePageModel ritualCard(String power, int coven, List<Ring> rings, ItemLike... offerings) {
        java.util.LinkedHashSet<String> sizes = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<String> chalks = new java.util.LinkedHashSet<>();
        for (Ring r : rings) {
            sizes.add(ringFootprint(r.size()));
            chalks.add(chalkName(r.color()));
        }
        boolean noPower = power == null || power.isBlank() || power.equalsIgnoreCase("none");
        String caption = "**" + String.join(" + ", sizes) + "** - **" + String.join(" / ", chalks) + "** chalk"
                + (noPower ? " - no altar power" : " - **" + power + "** altar power")
                + (coven > 0 ? " - **Coven: " + coven + "**" : "");

        StationRecipePageModel page = StationRecipePageModel.create()
                .withBackground(BG_RITUAL, RITUAL_CANVAS_W, RITUAL_CANVAS_H)
                .withShowBackground(false)
                .withText(WitcheryBooks.ink(caption));
        for (Ring r : rings) {
            page.withRing(r.color(), r.size());
        }
        int n = Math.min(offerings.length, 10);
        if (n <= 5) {
            addRitualOfferingRow(page, offerings, 0, n, RITUAL_OFFERING_SINGLE_Y);
        } else {
            int firstRow = Math.min(5, (n + 1) / 2);
            addRitualOfferingRow(page, offerings, 0, firstRow, RITUAL_OFFERING_TOP_Y);
            addRitualOfferingRow(page, offerings, firstRow, n - firstRow, RITUAL_OFFERING_BOTTOM_Y);
        }
        if (coven > 0) {
            page.withCovenFaces(coven);
        }
        return page;
    }

    private static void addRitualOfferingRow(StationRecipePageModel page, ItemLike[] offerings,
                                             int offset, int count, int y) {
        int pitch = 18;
        int startX = RITUAL_CANVAS_W / 2 - (count * pitch) / 2 + (pitch - 16) / 2;
        for (int i = 0; i < count; i++) {
            page.withSlot(offerings[offset + i], 1, startX + i * pitch, y);
        }
    }

    /**
     * Fixed facing for static creature portraits (degrees). Modonomicon applies this as a Y rotation
     * with {@code rotate=false}; 180° turns the freshly-built entity (default south-facing) to look at
     * the reader. QA-tunable: use ~135/-45 for a 3/4 isometric angle, 0 if it ends up facing away.
     */
    private static final float BOOK_ENTITY_FACING_DEGREES = 180.0F;

    private static BookEntityPageModel creature(String entityId, String name, String caption) {
        return creature(entityId, name, caption, 1.0F, 0.0F);
    }

    /**
     * Entity page with explicit render tuning. Modonomicon auto-fits the entity to its bounding box
     * ({@code renderScale = 100 / max(1, max(bbW, bbH)) * 0.75 * scale}) and centres it vertically by
     * {@code bbHeight/2 + max(bbHeight, size)*0.5 + offset}. Witchery models whose geometry overruns
     * their hitbox (or sit off-centre) clip in the 106px frame, so {@code scale} (<1 shrinks) and
     * {@code offset} nudge them back into the frame. These are first-pass values — confirm in-client
     * and tune from screenshots.
     *
     * <p>All creature pages render <b>static</b> (no spin): {@code rotate=false} with a fixed
     * {@link #BOOK_ENTITY_FACING_DEGREES}, so every living entity shows a steady flat isometric
     * portrait facing the reader instead of Modonomicon's default slow turntable.
     */
    private static BookEntityPageModel creature(String entityId, String name, String caption,
                                                 float scale, float offset) {
        return BookEntityPageModel.create()
                .withEntityId(entityId)
                .withEntityName(Component.literal(name))
                .withScale(scale)
                .withOffset(offset)
                .withRotate(false)
                .withDefaultRotation(BOOK_ENTITY_FACING_DEGREES)
                .withText(WitcheryBooks.ink(caption));
    }

    private static BookEntryModel entry(BookCategoryModel category, String idPath, String name,
                                        ItemLike iconItem, int sort, List<BookPageModel<?>> pages) {
        return BookEntryModel.create(id(idPath), name)
                .withCategory(category)
                .withIcon(BookIconModel.create(iconItem))
                .withSortNumber(sort)
                .withPages(pages);
    }

    /** A two-section rite entry: description, then "The Working" (offerings + use). */
    private BookEntryModel rite(BookCategoryModel cat, String path, String name, ItemLike icon, int sort,
                                String description, String working) {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose(name, description));
        pages.addAll(WitcheryBooks.prose("The Working", working));
        return entry(cat, path, name, icon, sort, pages);
    }

    /** As {@link #rite}, but with a ritual recipe card (size/chalk/power/coven + offerings) after the description. */
    private BookEntryModel rite(BookCategoryModel cat, String path, String name, ItemLike icon, int sort,
                                String description, BookPageModel<?> recipe, String working) {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose(name, description));
        pages.add(recipe);
        pages.addAll(WitcheryBooks.prose("The Working", working));
        return entry(cat, path, name, icon, sort, pages);
    }

    /**
     * An INDEX category gated behind the {@code guide/<book>} advancement. With the book itself in
     * INDEX display mode, the parent screen filters categories by unlock state, so a category whose
     * condition is unmet is hidden entirely until the player obtains its book.
     */
    private BookCategoryModel gated(String catId, String name, ItemLike icon, int sort, String book) {
        return BookCategoryModel.create(id(catId), name)
                .withIcon(BookIconModel.create(icon))
                .withDisplayMode(BookDisplayMode.INDEX)
                .withSortNumber(sort)
                .withCondition(BookAdvancementConditionModel.create().withAdvancementId(id("guide/" + book)));
    }

    /** A closing "What Comes Next" entry that shows the next book's recipe to continue the chain. */
    private BookEntryModel whatsNext(BookCategoryModel cat, String catId, int sort,
                                     String nextRecipe, String nextName, ItemLike nextIcon, String topic) {
        return whatsNext(cat, catId, sort, nextRecipe, nextName, nextIcon, topic,
                "Craft it to continue your studies.");
    }

    /** As {@link #whatsNext}, but with a custom recipe caption — used to name a reagent the player must brew. */
    private BookEntryModel whatsNext(BookCategoryModel cat, String catId, int sort, String nextRecipe,
                                     String nextName, ItemLike nextIcon, String topic, String recipeHint) {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("What Comes Next",
                "Your studies here are done. The next chapter — **" + topic + "** — opens once you craft "
                        + "and read its book."));
        pages.add(recipe(nextRecipe, nextName, recipeHint));
        return entry(cat, catId + "/whats_next", "What Comes Next", nextIcon, sort, pages);
    }

    /** Hides a "What Comes Next" entry until the player has obtained the chapter's signature item. */
    private BookEntryModel gatedByItem(String advancementPath, BookEntryModel next) {
        return next.withCondition(BookAdvancementConditionModel.create().withAdvancementId(id(advancementPath)))
                .hideWhileLocked(true);
    }

    @Override
    public void generate(BiConsumer<Identifier, BookModel> consumer, HolderLookup.Provider registries) {
        BookModel book = BookModel.create(BOOK_ID, "witchery.book.title")
                .withCreativeTab(id("witchery_tab"))
                .withGenerateBookItem(false)
                .withDisplayMode(BookDisplayMode.INDEX)
                .withDefaultTitleColor(WitcheryBooks.INK_COLOR)
                .withBookTextOffsetX(WitcheryBooks.TEXT_OFFSET_X)
                .withBookTextOffsetWidth(WitcheryBooks.TEXT_OFFSET_WIDTH)
                .withCategories(
                        gettingStarted(), collectingFumes(), herbology(), brewsAndInfusions(), distilling(),
                        circleMagic(), conjuration(), symbology(), poppets(), spiritWorld(), creatures(),
                        transformations());

        consumer.accept(book.getId(), book);
    }

    // ── 0. Getting Started (always open) ───────────────────────────────────────
    private BookCategoryModel gettingStarted() {
        BookIconModel icon = BookIconModel.create(WitcheryItems.WITCHES_PRIMER.get());
        BookCategoryModel cat = BookCategoryModel.create(id("getting_started"), "witchery.book.cat.getting_started")
                .withIcon(icon)
                .withDisplayMode(BookDisplayMode.INDEX)
                .withSortNumber(0);

        List<BookPageModel<?>> firstSteps = WitcheryBooks.prose("First Steps",
                "So you would learn the old craft, pupil. Good. Keep this primer close — it remembers "
                        + "what a tired witch forgets."
                        + "\n\nThe craft grows by chapters. This book opens itself as you earn each one: craft "
                        + "the guide for a subject, and its pages appear here.");
        List<BookPageModel<?>> altar = new ArrayList<>(WitcheryBooks.prose("The Altar",
                "If the power of this craft has a lifeblood, the altar is the heart that circulates it. "
                        + "It draws energy from natural and decorative objects within a generous radius and "
                        + "holds it ready for the kettle, the rites, and whatever else you demand of it."
                        + "\n\nTo wake an altar, lay **six Altar blocks** on one level in a full **2 by 3 "
                        + "rectangle**. Turned sideways, the same shape is **3 by 2**. Every space in that "
                        + "rectangle must be an Altar block: no gaps, no extra attached blocks, no steps up "
                        + "or down."
                        + "\n\nRight-click any block in the joined altar to read its power, maximum storage, "
                        + "recharge, range, and whether the structure is valid. If it says invalid, mend the "
                        + "shape before you dress the space. Rowan wood, candelabras, Redstone Soup-filled "
                        + "chalices, placed pentagrams and athames all feed it. Keep it furnished — a sparse "
                        + "altar is a weak altar."));
        altar.add(recipe("altar", "Altar",
                "Craft the altar, then choose where it will live — it is hard to move, and what surrounds "
                        + "it matters."));
        List<BookPageModel<?>> boline = new ArrayList<>(WitcheryBooks.prose("The Boline",
                "The Boline is the witch's field knife. It behaves much like shears, but it also harvests "
                        + "cobweb intact and safely gathers Blood Poppies and other delicate ingredients that "
                        + "rough hands would ruin."));
        boline.add(recipe("boline", "Boline",
                "Forge it from iron, bone, and an emerald. It fights no better than a wooden sword, but it "
                        + "endures like iron and matters far more as a tool."));
        List<BookPageModel<?>> path = new ArrayList<>(WitcheryBooks.prose("Path of the Craft",
                "Eleven chapters lie ahead of this one, pupil, and they open in their proper order — craft a "
                        + "subject's book and its pages appear here. Beyond this **Getting Started** road of "
                        + "the altar, the boline, and the first roots, the whole path runs:"));
        path.add(WitcheryBooks.orderedListPage(null, List.of(
                "**Collecting Fumes** — the Witches' Oven.",
                "**Herbology** — Mutandis and the Cauldron.",
                "**Brews & Infusions** — the Kettle.",
                "**Distilling** — essences drawn by heat.",
                "**Circle Magic** — chalk and rites.",
                "**Conjuration** — the Brazier.",
                "**Symbology** — the Mystic Branch.",
                "**Poppets** — binding from afar.",
                "**The Spirit World** — dreams and the dead.",
                "**Creatures** — familiars and beasts.",
                "**Transformations** — the wolf and the night.")));
        path.addAll(WitcheryBooks.prose(null,
                "The order is a teacher's order, not a cage. Should a coven witch or a dusty bookshop press "
                        + "a later chapter into your hands early, read it gladly — knowledge found is knowledge "
                        + "still."));

        cat.withEntries(
                entry(cat, "getting_started/first_steps", "witchery.book.entry.first_steps", WitcheryItems.WITCHES_PRIMER.get(), 0, firstSteps),
                entry(cat, "getting_started/altar", "The Altar", WitcheryItems.ALTAR.get(), 1, altar),
                entry(cat, "getting_started/boline", "The Boline", WitcheryItems.BOLINE.get(), 2, boline),
                entry(cat, "getting_started/path_of_the_craft", "Path of the Craft", WitcheryItems.CHALK_GOLDEN.get(), 3, path),
                whatsNext(cat, "getting_started", 4, "book_oven", "Witchcraft: Collecting Fumes", WitcheryItems.BOOK_OVEN.get(), "Collecting Fumes",
                        "The Witches' Oven is the first station a witch builds — and you build it before all else, because Herbology's Mutandis is compounded from the **Wood Ash** the Oven renders. Learn the smoke first, then the green."));
        return cat;
    }

    // ── 1. Herbology ────────────────────────────────────────────────────────────
    private BookCategoryModel herbology() {
        BookCategoryModel cat = gated("herbology", "Herbology", WitcheryItems.MANDRAKE_ROOT.get(), 2, "herbology");

        List<BookPageModel<?>> rowan = new ArrayList<>(WitcheryBooks.prose("The Rowan Tree",
                "The rowan is unremarkable to look at — a slender tree with silver bark, prone to growing "
                        + "in unlikely places. But it carries a sympathy with the unseen world that the other "
                        + "trees do not, and that makes it indispensable."
                        + "\n\nGrow one from a sapling mutated with Mutandis, or find one in the wild. Its logs "
                        + "feed your altar; its planks build your tools; its berries have their own quiet uses."));
        rowan.add(spotlight(WitcheryItems.ROWAN_BERRIES.get(), "Rowan Berries",
                "The berries go into several brews, and burned in a brazier they give a mild healing aura. "
                        + "Do not discard them."));

        List<BookPageModel<?>> mutandis = new ArrayList<>(WitcheryBooks.prose("Mutandis",
                "Mutandis is the old hedge-breaking powder, scattered over living growth to force a plant "
                        + "into some stranger relation — one of the first ways a witch steps outside ordinary "
                        + "farming."
                        + "\n\nIt can be compounded by hand, or brewed in the **Witch's Cauldron** by the "
                        + "older path."));
        mutandis.add(recipe("mutandis", "Mutandis",
                "Compound eight portions from **Wood Ash**, **Hint of Rebirth**, a **Mandrake Root**, an "
                        + "**Egg**, **Bone Meal**, and **Green Dye**."));
        mutandis.addAll(WitcheryBooks.prose("Cauldron Path",
                "The original path also works: heat and fill the **Witch's Cauldron**, drop in a **Mandrake "
                        + "Root** and an **Egg**, then right-click with **Exhale of the Horned One** for six "
                        + "portions."
                        + "\n\nDrop **Nether Wart** in and right-click with Mutandis to condense one "
                        + "**Mutandis Extremis**."));
        mutandis.addAll(WitcheryBooks.prose("Hedge Mutations",
                "Use Mutandis on natural plants or saplings. It can roll Witchery plants and saplings, "
                        + "**Alder** and **Hawthorn Saplings**, vanilla saplings, flowers, fern, grass, dead "
                        + "bush, and the small bush."
                        + "\n\nIn the Spirit World it can also reveal Nether Wart. It is a mutating powder, "
                        + "not a fertilizer — expect transformation, not growth."));
        mutandis.add(recipe("mutandis_extremis", "Mutandis Extremis",
                "Surround a **Nether Wart** with eight ordinary Mutandis for eight portions — or use the "
                        + "cauldron path above."));
        mutandis.addAll(WitcheryBooks.prose("Extremis",
                "Extremis pushes the art further: it mutates crops as well as wild growth, flips grass and "
                        + "dirt, and turns sand under water or ice into clay."
                        + "\n\nIt is also the bridge into heavier progressions like Treefyd Seeds. Treat it "
                        + "as the serious version of hedgecraft."));

        List<BookPageModel<?>> drop = new ArrayList<>(WitcheryBooks.prose("Drop of Luck",
                "Drop of Luck sounds fanciful until a rite or brew refuses to work without it. In the older "
                        + "craft it belonged to the **Witch's Cauldron**, not the kettle — a harsher working "
                        + "that curdles good hedge powders into something stranger."
                        + "\n\nIt sits at the edge between ordinary mutation and the more capricious parts of "
                        + "the craft."));
        drop.addAll(WitcheryBooks.prose(null,
                "Heat and fill a Witch's Cauldron, keep an altar near, drop in a **Mandrake Root**, **Nether "
                        + "Wart**, **Tear of the Goddess**, and **Refined Evil**, then use **Mutandis "
                        + "Extremis** on the cauldron."
                        + "\n\nThe powder is spent, the altar pays, and a single Drop of Luck condenses from "
                        + "the bowl."));

        List<BookPageModel<?>> sprig = new ArrayList<>();
        sprig.add(spotlight(WitcheryItems.MUTATOR.get(), "Mutating Sprig",
                "Hedgecraft made deliberate. Where Mutandis scatters possibilities, the sprig forces a few "
                        + "changes with a steady hand. Each use wears it down."));
        sprig.addAll(WitcheryBooks.prose("Making the Sprig",
                "Surround an **Ent Twig** with **Mutandis Extremis** at a table. Or use the cauldron: heat a "
                        + "filled Witch's Cauldron near an altar, drop in an **Ent Twig** and a **Nether "
                        + "Wart**, then use Mutandis Extremis on it."));
        sprig.addAll(WitcheryBooks.prose("Block & Built Mutations",
                "On simple blocks the sprig flips **Grass Block** and **Mycelium** either way, and turns "
                        + "underwater **Dirt** to **Clay** and back."
                        + "\n\nAn empty **Chest** over water with Short Grass on four sides becomes four "
                        + "**Grasspers**. A **Trapped Chest** ringed with Vines over water becomes a **Leech "
                        + "Chest**. A Leech Chest ringed with Poppies leaves four **Blood Roses**."));
        sprig.addAll(WitcheryBooks.prose("Creature Snares",
                "A **Cobweb** over water, with four planted Alder Saplings and a trapped Zombie, becomes four "
                        + "**Critter Snares** — the bridge into bat, slime, and magma snare chains. Four bat "
                        + "snares packed with Slime Balls make a **Concentrated Bat Ball**."));
        sprig.addAll(WitcheryBooks.prose("Living Mutations",
                "The diagonal Grasspers mostly hold Mutandis Extremis and a Charged Attuned Stone. Bat snares "
                        + "around a lured Wolf become **Owls**. Slime snares around an Ocelot become "
                        + "**Toads**. Cocoa-ringed webs with a Wolf and Owl leave a wild **Flying Monkey**. A "
                        + "silverfish snare becomes a **Parasytic Louse**."));
        sprig.addAll(WitcheryBooks.prose("Brambles & Wormwood",
                "A **Cactus** on sand, ringed with water and Spanish Moss and fed Bone Meal/Blaze Powder "
                        + "Grasspers, becomes **Wild Bramble**. The same with **Sugar Cane** and Ender Pearls "
                        + "makes **Void Bramble**. Ripe **Wheat** ringed with Wispy Cotton leaves a young "
                        + "**Wormwood**."));

        // The Witch's Cauldron is the foundation Herbology rests on — Mutandis and Drop of Luck above
        // already use it — so it is taught here, before any later book asks for a cauldron-brewed reagent.
        List<BookPageModel<?>> anointing = new ArrayList<>(WitcheryBooks.prose("The Witch's Cauldron",
                "Before kettles and distilleries, every witch learns the cauldron. Anointing Paste is what "
                        + "wakes an ordinary cauldron into a Witch's Cauldron — the vessel behind the cauldron "
                        + "paths already mentioned above."
                        + "\n\nIt is the earliest real progression gate in the craft, because so many "
                        + "foundational brews begin in its bowl."));
        anointing.add(recipe("anointing_paste", "Anointing Paste",
                "**Water Artichoke**, **Mandrake**, **Belladonna**, and **Snowbell** seeds make four paste. "
                        + "Grass and ferns drop those seeds again."));
        anointing.add(spotlight(Items.CAULDRON, "Witch's Cauldron",
                "Apply the paste to a placed cauldron, then give it a fire and water. Drop reagents in the "
                        + "bowl and trigger the working with the last item by hand — the cauldron workings "
                        + "named throughout this primer follow."));
        anointing.add(stationRecipeCard("cauldron_mutandis", "Cauldron: Mutandis",
                "Drop a **Mandrake Root** and an **Egg** in a heated, filled cauldron, then right-click with "
                        + "**Exhale of the Horned One** — six Mutandis rise from the bowl."));
        anointing.add(stationRecipeCard("cauldron_mutandis_extremis", "Cauldron: Mutandis Extremis",
                "Drop a **Nether Wart** in and right-click with **Mutandis** to condense one Mutandis "
                        + "Extremis — the stronger powder."));
        anointing.add(stationRecipeCard("cauldron_drop_of_luck", "Cauldron: Drop of Luck",
                "Drop the four reagents with an altar near, then use **Mutandis Extremis** on the "
                        + "cauldron — one Drop of Luck."));

        cat.withEntries(
                entry(cat, "herbology/rowan_tree", "The Rowan Tree", WitcheryItems.ROWAN_LOG.get(), 0, rowan),
                entry(cat, "herbology/mutandis", "Mutandis", WitcheryItems.MUTANDIS.get(), 1, mutandis),
                entry(cat, "herbology/anointing_paste", "The Witch's Cauldron", WitcheryItems.ANOINTING_PASTE.get(), 2, anointing),
                entry(cat, "herbology/drop_of_luck", "Drop of Luck", WitcheryItems.DROP_OF_LUCK.get(), 3, drop),
                entry(cat, "herbology/mutating_sprig", "Mutating Sprig", WitcheryItems.MUTATOR.get(), 4, sprig),
                gatedByItem("guide/obtained/mutandis", whatsNext(cat, "herbology", 5, "book_infusions", "Witchcraft: Brews & Infusions", WitcheryItems.BOOK_INFUSIONS.get(), "Brews & Infusions",
                        "With Mutandis in hand the green craft is yours. Next comes the Kettle — and its lasting infusions lean on **Odour of Purity**, baked from a Hawthorn Sapling your Mutandis can now roll.")));
        return cat;
    }

    // ── 2. Brews & Infusions ────────────────────────────────────────────────────
    private BookCategoryModel brewsAndInfusions() {
        BookCategoryModel cat = gated("brews_infusions", "Brews & Infusions", WitcheryItems.KETTLE.get(), 3, "infusions");

        List<BookPageModel<?>> kettle = new ArrayList<>(WitcheryBooks.prose("The Kettle",
                "A kettle is a simple thing: a vessel, a fire, and whatever you choose to put inside it. "
                        + "That last part is where things get interesting."
                        + "\n\nRight-click to add reagents one by one, then light a fire beneath it. It draws "
                        + "power from the nearest altar to complete the working, and the ingredients decide "
                        + "what emerges. Keep records of what you try — the craft does not always announce "
                        + "itself."));
        kettle.add(recipe("kettle", "Kettle",
                "Iron and a modicum of patience. That is all a kettle needs to be born."));
        kettle.add(stationRecipeCard("kettle_redstone_soup", "Kettle: Redstone Soup",
                "Six reagents in a lit kettle over **1000 altar power**. Redstone Soup is the base of "
                        + "every lasting infusion."));

        List<BookPageModel<?>> infusion = new ArrayList<>(WitcheryBooks.prose("Infusion Sources",
                "The four lasting infusions begin in the kettle, not the circle. Each source uses **Redstone "
                        + "Soup** as a base and asks a nearby altar for **4000 power**, preparing the essence "
                        + "for one branch of permanent transformation."
                        + "\n\nThe Otherwhere and Infernal brews also accept their splash-potion twins, if "
                        + "that is what you have to hand."));
        infusion.add(spotlight(WitcheryItems.GHOST_OF_THE_LIGHT.get(), "Ghost of the Light",
                "The Light infusion: Redstone Soup with Long Invisibility, Long Fire Resistance, a Fire "
                        + "Protection Poppet, a torch, and Tongue of Dog."));
        infusion.add(spotlight(WitcheryItems.SOUL_OF_THE_WORLD.get(), "Soul of the World",
                "The Overworld infusion: Redstone Soup with Long Regeneration, an Enchanted Golden Apple, an "
                        + "Attuned Stone, a Mandrake Root, and a Rowan Sapling."));
        infusion.add(spotlight(WitcheryItems.SPIRIT_OF_OTHERWHERE.get(), "Spirit of Otherwhere",
                "The Otherwhere infusion: Redstone Soup with a Long Swiftness potion, two Ender Eyes, a Drop "
                        + "of Luck, and Wool of Bat."));
        infusion.add(spotlight(WitcheryItems.INFERNAL_ANIMUS.get(), "Infernal Animus",
                "The Infernal infusion: Redstone Soup with a Strong Harming potion, a Voodoo Poppet, a Demon "
                        + "Heart, Refined Evil, and a Blaze Rod."));

        List<BookPageModel<?>> brewReference = customBrewReference();

        cat.withEntries(
                entry(cat, "brews_infusions/kettle", "The Kettle", WitcheryItems.KETTLE.get(), 0, kettle),
                entry(cat, "brews_infusions/infusion_sources", "Infusion Sources", WitcheryItems.GHOST_OF_THE_LIGHT.get(), 1, infusion),
                entry(cat, "brews_infusions/brew_reagents", "Brew Reagents", WitcheryItems.BREW_BOTTLE.get(), 2, brewReference),
                gatedByItem("guide/obtained/redstone_soup", whatsNext(cat, "brews_infusions", 3, "book_distilling", "Witchcraft: Distilling", WitcheryItems.BOOK_DISTILLING.get(), "Distilling",
                        "The bottom reagent, **Breath of the Goddess**, is baked from a Birch Sapling in the Witches' Oven.")));
        return cat;
    }

    // ── 3. Collecting Fumes ─────────────────────────────────────────────────────
    private BookCategoryModel collectingFumes() {
        BookCategoryModel cat = gated("collecting_fumes", "Collecting Fumes", WitcheryItems.WITCHES_OVEN.get(), 1, "oven");
        List<BookPageModel<?>> oven = new ArrayList<>(WitcheryBooks.prose("The Witches' Oven",
                "Not everything useful comes from a kettle. Some ingredients must be rendered down first — "
                        + "reduced, transformed, made into something the craft can use."
                        + "\n\nThe Witches' Oven smolders saplings into wood ash and jarred fumes, while logs "
                        + "cook down into charcoal. Insert a clay jar to catch the rising fumes."));
        oven.add(recipe("witches_oven", "Witches' Oven",
                "Iron ingots and iron bars, shaped with intent. The oven needs only a fire beneath and materials "
                        + "worth its attention."));

        List<BookPageModel<?>> ovenRecipes = new ArrayList<>(WitcheryBooks.prose("What the Oven Makes",
                "Feed it a sapling and it renders the wood down into a fume, caught in a clay jar — each "
                        + "tree gives a different essence. Start with what lies to hand: **Wood Ash** from "
                        + "saplings, **Exhale** from an oak, **Breath** from a birch. The Hawthorn and Rowan "
                        + "come later, once Herbology gives you Mutandis and the Rowan tree."));
        ovenRecipes.add(stationRecipeCard("oven_odour_of_purity", "Odour of Purity",
                "A **Hawthorn Sapling** bakes into Odour of Purity — the **Brews & Infusions** reagent. "
                        + "Hawthorn comes from Herbology's Mutandis, so this fume waits a chapter."));
        ovenRecipes.add(stationRecipeCard("oven_breath_of_goddess", "Breath of the Goddess",
                "A **Birch Sapling** gives Breath of the Goddess — the **Distilling** reagent, and the "
                        + "base of countless brews."));
        ovenRecipes.add(stationRecipeCard("oven_whiff_of_magic", "Whiff of Magic",
                "A **Rowan Sapling** gives Whiff of Magic — the **Circle Magic** reagent. Rowans come "
                        + "with Herbology, so this waits a little too."));
        ovenRecipes.add(stationRecipeCard("oven_exhale", "Exhale of the Horned One",
                "An **Oak Sapling** gives Exhale of the Horned One, used in Mutandis and darker workings."));
        ovenRecipes.add(stationRecipeCard("oven_hint_of_rebirth", "Hint of Rebirth",
                "A **Spruce Sapling** gives Hint of Rebirth, caught in a clay jar beside the wood ash."));
        ovenRecipes.add(stationRecipeCard("oven_reek_of_misfortune", "Reek of Misfortune",
                "An **Alder Sapling** gives Reek of Misfortune, another jarred fume from the witch trees."));
        ovenRecipes.add(stationRecipeCard("oven_foul_fume_acacia", "Foul Fume: Acacia",
                "An **Acacia Sapling** gives Foul Fume."));
        ovenRecipes.add(stationRecipeCard("oven_foul_fume_dark_oak", "Foul Fume: Dark Oak",
                "A **Dark Oak Sapling** gives Foul Fume."));
        ovenRecipes.add(stationRecipeCard("oven_foul_fume_jungle", "Foul Fume: Jungle",
                "A **Jungle Sapling** gives Foul Fume."));
        ovenRecipes.add(stationRecipeCard("oven_charcoal", "Charcoal & Foul Fume",
                "Any **log** smolders down to charcoal. With a clay jar present, it may also catch Foul Fume."));

        cat.withEntries(
                entry(cat, "collecting_fumes/witches_oven", "The Witches' Oven", WitcheryItems.WITCHES_OVEN.get(), 0, oven),
                entry(cat, "collecting_fumes/oven_recipes", "What the Oven Makes", WitcheryItems.ODOUR_OF_PURITY.get(), 1, ovenRecipes),
                gatedByItem("guide/obtained/exhale_of_horned_one", whatsNext(cat, "collecting_fumes", 2, "book_herbology", "Witchcraft: Herbology", WitcheryItems.BOOK_HERBOLOGY.get(), "Herbology",
                        "Now that the Oven smokes, Herbology is yours to learn — its Mutandis is compounded from the **Wood Ash** you just rendered.")));
        return cat;
    }

    // ── 4. Distilling ───────────────────────────────────────────────────────────
    private BookCategoryModel distilling() {
        BookCategoryModel cat = gated("distilling", "Distilling", WitcheryItems.DISTILLERY.get(), 4, "distilling");
        List<BookPageModel<?>> distillery = new ArrayList<>(WitcheryBooks.prose("The Distillery",
                "Some essences cannot be coaxed directly from raw materials. They must be drawn out through "
                        + "heat and pressure — concentrated and refined until only the potent core remains."
                        + "\n\nThe Distillery takes two inputs and fuel, and needs no altar."));
        distillery.add(recipe("distillery", "Distillery",
                "Iron, glass, and a brewing stand. The distillery is not complicated; what you put inside it "
                        + "often is."));
        distillery.add(stationRecipeCard("distillery_oil_of_vitriol", "Oil of Vitriol",
                "**Foul Fume** + **Quicklime**, with one clay jar, yields Gypsum, Oil of Vitriol, and a Slimeball."));
        distillery.add(stationRecipeCard("distillery_diamond_vapour", "Diamond Vapour",
                "**Diamond** + **Oil of Vitriol**, with three clay jars, yields two Diamond Vapours and Odour of Purity."));
        // Page-level reveal: this page stays hidden until the witch has actually produced Diamond
        // Vapour at the distillery (the guide/obtained/diamond_vapour advancement fires on pickup).
        distillery.add(BookTextPageModel.create()
                .withTitle(Component.literal("First Distillation"))
                .withText(WitcheryBooks.ink(
                        "You have drawn your first Diamond Vapour, pupil — the Distillery has taken to your "
                                + "hand. Feed it well, and its essences need never run dry."))
                .withCondition(BookAdvancementConditionModel.create()
                        .withAdvancementId(id("guide/obtained/diamond_vapour"))));
        distillery.add(stationRecipeCard("distillery_four_essences", "Four Essences",
                "**Diamond Vapour** + **Ghast Tear**, with three clay jars, yields Odour of Purity, Reek of "
                        + "Misfortune, Foul Fume, and Refined Evil."));
        distillery.add(stationRecipeCard("distillery_tear_of_goddess", "Tear of the Goddess",
                "**Breath of the Goddess** + **Lapis Lazuli**, with three clay jars, yields Tear of the "
                        + "Goddess, Whiff of Magic, Slimeball, and Foul Fume."));
        distillery.add(stationRecipeCard("distillery_glowstone_dust", "Glowstone Dust",
                "**Blaze Powder** + **Gunpowder**, with one clay jar, yields two Glowstone Dust and Reek of "
                        + "Misfortune."));
        distillery.add(stationRecipeCard("distillery_ender_dew", "Ender Dew",
                "**Ender Pearl** alone, with six clay jars, yields five Ender Dew and Whiff of Magic."));
        distillery.add(stationRecipeCard("distillery_condensed_fear", "Condensed Fear",
                "**Brew of Flowing Spirit** + **Oil of Vitriol**, with two clay jars, yields Focused Will, "
                        + "Condensed Fear, and two batches of Hollow Tears."));
        distillery.add(stationRecipeCard("distillery_infernal_blood_netherrack", "Infernal Blood: Netherrack",
                "**Demon Heart** + **Netherrack**, with two clay jars, yields Soul Sand and two Infernal Blood."));
        distillery.add(stationRecipeCard("distillery_infernal_blood", "Infernal Blood: Vapour",
                "**Demon Heart** + **Diamond Vapour**, with four clay jars, yields four Infernal Blood and "
                        + "Refined Evil."));
        cat.withEntries(
                entry(cat, "distilling/distillery", "The Distillery", WitcheryItems.DISTILLERY.get(), 0, distillery),
                gatedByItem("guide/obtained/diamond_vapour", whatsNext(cat, "distilling", 1, "book_circle_magic", "Witchcraft: Circle Magic", WitcheryItems.BOOK_CIRCLE_MAGIC.get(), "Circle Magic",
                        "The bottom reagent, **Whiff of Magic**, is baked from a Rowan Sapling in the Witches' Oven.")));
        return cat;
    }

    // ── 5. Circle Magic & Rites ─────────────────────────────────────────────────
    private BookCategoryModel circleMagic() {
        BookCategoryModel cat = gated("circle_magic", "Circle Magic & Rites", WitcheryItems.CHALK_GOLDEN.get(), 5, "circle_magic");

        List<BookPageModel<?>> chalk = new ArrayList<>(WitcheryBooks.prose("Chalk",
                "Before a rite, you must prepare the ground. The circle is the structure that makes a "
                        + "working possible — a closed space where intention and power accumulate until "
                        + "released."
                        + "\n\nRight-click chalk on flat stone or earth to lay a glyph. The ring is drawn in "
                        + "ordinary white chalk, but the single mark at the very centre must be drawn with "
                        + "**Golden Chalk**. That golden mark is the heart of the working: drop the ingredients "
                        + "beside it, then right-click the **golden** centre to begin."
                        + "\n\nMind this well, pupil — a white centre will not answer. Only the gold opens a "
                        + "rite. Many a circle has been drawn whole and true and still lay dead, because the "
                        + "witch forgot to gild its heart."));
        chalk.add(recipe("chalk", "Chalk",
                "Drawn from simple stuff. The stronger families are crafted or brewed from it."));
        chalk.add(recipe("chalk_golden", "Golden Chalk",
                "The golden centre mark for every rite. Craft it from ordinary chalk, gold, yellow dye, "
                        + "and redstone."));
        chalk.add(recipe("chalk_otherwhere", "Otherwhere Chalk",
                "For rings that reach across worlds. Craft it from ordinary chalk, artichoke, ender pearl, "
                        + "and lapis."));
        chalk.add(recipe("chalk_infernal", "Infernal Chalk",
                "For rites that lean below. Craft it from ordinary chalk, ember moss, blaze powder, and "
                        + "red dye."));
        chalk.addAll(WitcheryBooks.prose("Stronger Chalks",
                "From ordinary chalk you may draw the stronger families: **Golden** with a gold nugget, yellow "
                        + "dye, and redstone; **Otherwhere** with artichoke, ender pearl, and lapis; "
                        + "**Infernal** with ember moss, blaze powder, and red dye."
                        + "\n\nDo not mistake their use. **Golden** is for no single rite — every working is "
                        + "begun from a golden centre, so keep some always at hand. It is the colour of the "
                        + "**ring** that must suit the rite: plain white for common workings, **Otherwhere** "
                        + "when the circle must reach across the boundary of worlds, **Infernal** for the "
                        + "things best not named lightly. Match the ring to the rite, and set gold at its heart."));

        List<BookPageModel<?>> drawing = new ArrayList<>(WitcheryBooks.prose("Drawing Circles",
                "The circle must be complete. A gap, even a small one, undoes the whole working — power "
                        + "spills, reagents are wasted, and the rite fails."
                        + "\n\nPlace each glyph around the perimeter in the chalk the rite calls for — white "
                        + "for most, **Otherwhere** or **Infernal** where a rite demands it — then set a "
                        + "**Golden** centre in the very middle. Drop the activating ingredient nearby and "
                        + "right-click that golden centre. Whatever the ring, the centre is always gold."));
        drawing.addAll(WitcheryBooks.prose("Three Sizes",
                "**Small** (7x7, 16 glyphs) for infusions, familiar binding, and personal rites. **Medium** "
                        + "(11x11, 28 glyphs) for summoning, fertility, and most buff rites. **Large** "
                        + "(15x15, 40 glyphs) for world-altering rites, curses, and dimensional workings."
                        + "\n\nThe Circle Talisman stores a pattern and redeploys it instantly."));

        List<BookPageModel<?>> rites = new ArrayList<>(WitcheryBooks.prose("Rites of Power",
                "A rite is the intentional manipulation of the world through circle, ingredient, and power. "
                        + "The circle gives structure; the ingredient names the working; the altar provides "
                        + "fuel. Some rites also need creatures present — part of the mechanism, not "
                        + "ceremony."
                        + "\n\nExperimentation is encouraged. The craft does not document all of its own "
                        + "workings."));
        rites.addAll(WitcheryBooks.prose("A Selection",
                "**Infusion** (small) grants a lasting magical nature. **Charging** (small+medium, Attuned "
                        + "Stone) uses glowstone, redstone, wood ash, and quicklime. **Familiar Binding** "
                        + "(small) binds a tamed animal. **Forest** (large, any sapling) grows a grove. "
                        + "**Waystone Binding** and **Transposition** anchor and move you."));

        cat.withEntries(
                entry(cat, "circle_magic/chalk", "Chalk", WitcheryItems.CHALK.get(), 0, chalk),
                entry(cat, "circle_magic/drawing_circles", "Drawing Circles", WitcheryItems.CHALK_GOLDEN.get(), 1, drawing),
                entry(cat, "circle_magic/rites", "Rites of Power", WitcheryItems.CIRCLE_TALISMAN.get(), 2, rites),
                rite(cat, "circle_magic/forest", "Rite of the Forest", WitcheryItems.ROWAN_SAPLING.get(), 3,
                        "A medium working for raising a grove where none stood. Drop the sapling for the "
                                + "woodland you want at the center, then prepare the rite. Oak, spruce, birch, "
                                + "jungle, rowan, alder, and hawthorn each answer to their own sapling.",
                        ritualCard("4000", 0, List.of(ringWhite("medium")),
                                WitcheryItems.ROWAN_SAPLING.get(), WitcheryItems.WICKER_BUNDLE.get(),
                                WitcheryItems.BREW_OF_SPROUTING.get(), WitcheryItems.ENT_TWIG.get()),
                        "Offer a **Wicker Bundle**, a **Brew of Sprouting**, and an **Ent Twig** around the "
                                + "circle, with at least **4000 altar power**. A bound waystone in range can "
                                + "raise the grove there instead.\n\nLearn it early if you mean to explore "
                                + "plantcraft, Ents, or living-wood paths."),
                rite(cat, "circle_magic/manifestation", "Rite of Manifestation", WitcheryItems.MELLIFLUOUS_HUNGER.get(), 4,
                        "Not a way into the Spirit World, but the way briefly back out once you are already "
                                + "there. Draw a small Otherwhere circle in the Spirit World and work the rite "
                                + "from within. The result is not a body — it is permission to become more "
                                + "solid for a while.",
                        ritualCard("5000", 0, List.of(ringOtherwhere("small")),
                                WitcheryItems.MELLIFLUOUS_HUNGER.get(), WitcheryItems.SPECTRAL_DUST.get(),
                                WitcheryItems.NECRO_STONE.get(), Items.GOLDEN_PICKAXE,
                                WitcheryItems.ARTHANA.get(), Items.GUNPOWDER),
                        "It asks for **Spectral Dust**, **Mellifluous Hunger**, a **Necromantic Stone**, a "
                                + "**Golden Pickaxe**, an **Arthana**, and **Gunpowder**, plus **5000 altar "
                                + "power**. The stone and Arthana return.\n\nThen pass through a **Spirit "
                                + "Portal** to emerge in the Overworld as a ghostly traveler. End it early "
                                + "with an Icy Needle."),
                rite(cat, "circle_magic/beastial_call", "Rite of Beastial Call", Items.HAY_BLOCK, 5,
                        "A large Otherwhere working that draws animal life into the circle from far across "
                                + "the countryside — up to a hundred paces and more. Less a summoning than a "
                                + "lure: it coaxes creatures to answer a call they do not understand, and they "
                                + "arrive a few at a time over long minutes, not all at once. Be patient, and "
                                + "have beasts within range to call.",
                        ritualCard("6000", 3, List.of(ringOtherwhere("large")),
                                Items.MILK_BUCKET, Items.HAY_BLOCK, Items.APPLE, Items.BEEF, Items.COD,
                                Items.RED_MUSHROOM, Items.CARROT, Items.WHEAT_SEEDS),
                        "Offer barn and field bait: a milk bucket, hay bale, apple, beef, cod, red mushroom, "
                                + "carrot, and seeds, plus **6000 altar power**. It favors pigs, chickens, "
                                + "cows, sheep, mooshrooms, wolves, and ocelots.\n\n**It demands a coven of "
                                + "three.** Three witches or more must keep the circle, or the call goes "
                                + "unanswered and the rite collapses — this is no solitary working. Do not cast "
                                + "it near a village ranch unless you can explain yourself."),
                rite(cat, "circle_magic/sky_wrath", "Rite of Sky's Wrath", Items.IRON_SWORD, 6,
                        "A small-circle storm working for calling lightning where you choose. The inner "
                                + "branch gathers the storm in the circle, the outer lashes the ground around "
                                + "it, and the portable branch can be cast far from home without altar power. "
                                + "A sword laid in the circle is the lightning's focus — the better the blade, "
                                + "the wider its wrath.",
                        ritualCard("1000 / 2000", 0, List.of(ringWhite("small")),
                                Items.IRON_SWORD, WitcheryItems.WOOD_ASH.get(), WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        "A **Wooden Sword** and **Wood Ash** call the inner storm (1000 power). A **Stone "
                                + "Sword** and **Wood Ash** call the outer storm (2000 power). An **Iron "
                                + "Sword**, **Wood Ash**, and a **Charged Attuned Stone** restore the portable "
                                + "branch. Any worn blade serves — its edge is spent, not its temper.\n\nA "
                                + "bound waystone in range moves the storm to its location — safer than "
                                + "standing under your own lightning."),
                rite(cat, "circle_magic/sanctity", "Rite of Sanctity", Items.FEATHER, 7,
                        "A small-circle ward that keeps unwanted creatures out. It does not harm them — it "
                                + "repels them, making a brief sacred space that beasts and enemies find hard "
                                + "to cross.",
                        ritualCard("500", 0, List.of(ringWhite("small")), Items.FEATHER, Items.REDSTONE),
                        "Offer a **Feather** with **Redstone** on a small circle, with **500 altar power** "
                                + "ready. Do not confuse it with the Rite of Imprisonment, which has the "
                                + "opposite effect.\n\nCast "
                                + "it against panic, interruption, or crowding while you prepare a longer "
                                + "working. Not permanent, not a wall, but enough to keep the world at a "
                                + "distance."),
                rite(cat, "circle_magic/imprisonment", "Rite of Imprisonment", Items.SLIME_BALL, 8,
                        "The sister-working to Sanctity. Where Sanctity presses creatures away, Imprisonment "
                                + "draws them back and pens them within the circle. A snare, not a "
                                + "killing-circle.",
                        ritualCard("500 (+16/s)", 0, List.of(ringWhite("small")), Items.SLIME_BALL, Items.REDSTONE),
                        "Offer a **Slime Ball** with **Redstone** on a small circle, with **500 altar "
                                + "power**. For the safer inverse, study Sanctity.\n\nUseful for holding "
                                + "sacrificial beasts or summoned nuisances in one patch while you prepare the "
                                + "next working. Control before destruction."),
                rite(cat, "circle_magic/broiling", "Rite of Broiling", Items.SUGAR, 9,
                        "An infernal cooking-working that turns dropped raw food into cooked meals all at "
                                + "once. Efficient, dramatic, and never entirely gentle — for when a camp of "
                                + "meat needs preparing faster than any oven.",
                        ritualCard("1000", 0, List.of(ringInfernal("small")),
                                Items.SUGAR, WitcheryItems.WOOD_ASH.get(), Items.COAL, Items.BLAZE_POWDER,
                                WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        "Started from a small infernal circle with **Sugar**. The ordinary branch needs "
                                + "**Wood Ash** and **Coal** (1000 power); the hotter branch needs **Wood "
                                + "Ash**, **Blaze Powder**, and a **Charged Attuned Stone**.\n\nIt cooks every "
                                + "raw food in reach. The hotter branch is harsher — some offerings return as "
                                + "**Charcoal** instead of supper."),
                rite(cat, "circle_magic/broken_earth", "Rite of Broken Earth", WitcheryItems.BREW_OF_EROSION.get(), 10,
                        "A small working that tears open the ground beyond the circle — rough, practical "
                                + "witchcraft for a trench, a hazard, or a sudden gap where there was solid "
                                + "ground.",
                        ritualCard("none", 0, List.of(ringWhite("small")), WitcheryItems.BREW_OF_EROSION.get()),
                        "Offer a **Brew of Erosion** on a small circle; the crevice carves ahead of the "
                                + "casting point, so choose your direction first.\n\nUse it to disrupt "
                                + "pursuers, split a battlefield, or make a sudden barrier. Blunt magic — "
                                + "effective, noisy, and inclined to ruin the ground."),
                rite(cat, "circle_magic/moving_earth", "Rite of Moving Earth", Items.DIRT, 11,
                        "Not one neat pillar but a greater sundering — a large working that parts the land "
                                + "and sends the broken line wandering outward. Where Broken Earth is a "
                                + "trench, this is a sustained rift.",
                        ritualCard("none", 0, List.of(ringWhite("large")), Items.DIRT, Items.GRAVEL, Items.SAND),
                        "Worked from a large circle with **Dirt**, **Gravel**, or **Sand**. It becomes a "
                                + "staged rite that walks a tearing path away over time, and nearby Coven "
                                + "Witches widen it.\n\nUse it to divide a field, break pursuit, or command "
                                + "the earth aside."),
                rite(cat, "circle_magic/glyphic_transformation", "Rite of Glyphic Transformation", WitcheryItems.CHALK.get(), 12,
                        "Some witches redraw whole circles by hand; richer or lazier ones persuade the "
                                + "circle to rewrite itself. This changes an existing ring from one chalk "
                                + "family to another — it retunes marks already there.",
                        ritualCard("1000", 0, List.of(ringWhite("medium")),
                                WitcheryItems.CHALK.get(), WitcheryItems.CHALK_OTHERWHERE.get(),
                                WitcheryItems.CHALK_INFERNAL.get(), WitcheryItems.GYPSUM.get(), WitcheryItems.ARTHANA.get()),
                        "Offer the chalk family you want — **Chalk**, **Otherwhere Chalk**, or **Infernal "
                                + "Chalk** — plus **Gypsum**, an **Arthana**, and **1000 altar power**. The "
                                + "Arthana returns.\n\nThe amount of dropped chalk decides which ring is "
                                + "rewritten: one for small, two for medium, three for large."),
                rite(cat, "circle_magic/natures_power", "Rite of Nature's Power", WitcheryItems.BREW_OF_SPROUTING.get(), 13,
                        "Not a burst of growth but a sustained medium-circle coaxing, urging the land toward "
                                + "green abundance over time — fields, moss, saplings, and wild growth.",
                        ritualCard("none", 0, List.of(ringWhite("medium")),
                                WitcheryItems.BREW_OF_SPROUTING.get(), WitcheryItems.ROWAN_SAPLING.get(),
                                WitcheryItems.ALDER_SAPLING.get(), WitcheryItems.HAWTHORN_SAPLING.get(),
                                Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
                                WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        "Worked from a medium circle with **Brew of Sprouting** and a mixed grove offering — "
                                + "**Rowan**, **Alder**, **Hawthorn**, **Oak**, **Spruce**, and **Birch "
                                + "Saplings** — plus a **Charged Attuned Stone** (returned).\n\nIt pulses "
                                + "outward, greening soil and driving growth. Nearby Coven Witches lengthen "
                                + "its reach."),
                rite(cat, "circle_magic/earths_wrath", "Rite of Earth's Wrath", Items.LAVA_BUCKET, 14,
                        "The circle-working of buried fire made manifest. Where the land below hides a real "
                                + "lava pool, this rite drags that violence upward and forces the ground into "
                                + "a fresh volcano. Less control than a bargain with pressure and ruin.",
                        ritualCard("none", 0, List.of(ringInfernal("small")), Items.LAVA_BUCKET),
                        "A large staged terrain rite through a **Lava Bucket** trigger; the ground below "
                                + "must hold a sizable lava pool or it fails. A nearby **Bound Waystone** can "
                                + "retarget the eruption.\n\nThe cone rises in pulses and opens its vent at "
                                + "the end. Use it to scar the land or make a place no traveler mistakes for "
                                + "safe."),
                rite(cat, "circle_magic/icy_expansion", "Rite of Icy Expansion", WitcheryItems.FROZEN_HEART.get(), 15,
                        "The witch's answer to drowning. Rather than parting the sea, it grows a hollow "
                                + "winter shell around the working, pushing liquid away and leaving a "
                                + "breathing chamber inside.",
                        ritualCard("none", 2, List.of(ringWhite("small")), WitcheryItems.FROZEN_HEART.get()),
                        "Worked from a small circle through a **Frozen Heart** trigger, and it needs a coven "
                                + "of at least two before the shell forms; stronger covens grow it larger."
                                + "\n\nIt grows in pulses, clearing the inside. Cast it under the ocean or in "
                                + "flooded caverns for a pocket of winter-made air."),
                rite(cat, "circle_magic/prior_incarnation", "Rite of Prior Incarnation", WitcheryItems.ATTUNED_STONE_CHARGED.get(), 16,
                        "A desperate infernal recovery-working for calling back what was lost in death. It "
                                + "does not reverse the death — it drags your fallen belongings back before "
                                + "the world scatters them.",
                        ritualCard("none", 0, List.of(ringInfernal("small")),
                                WitcheryItems.NECRO_STONE.get(), WitcheryItems.TONGUE_OF_DOG.get(), Items.BONE,
                                WitcheryItems.SPECTRAL_DUST.get(), WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        "Worked on a small infernal circle with a **Necromantic Stone**, **Tongue of Dog**, "
                                + "**Bone**, **Spectral Dust**, and a **Charged Attuned Stone**. Cast it in "
                                + "the same world, within **16 blocks** of where you fell.\n\nLoose drops are "
                                + "pulled toward you, your inventory first. No answer to Keep Inventory or "
                                + "another world — but the witch's answer to a bad death while the trail is "
                                + "warm."),
                gatheredLivesEntry(cat),
                gatedByItem("guide/obtained/chalk", whatsNext(cat, "circle_magic", 18, "book_burning", "Witchcraft: Conjuration & Fetishes", WitcheryItems.BOOK_BURNING.get(), "Conjuration & Fetishes")));
        return cat;
    }

    /**
     * The Spectral Coven — a hidden Circle Magic rite, revealed only once the player has gathered five of
     * the old village story books (advancement {@code guide/gathered_lives}). The short story is the lore
     * reward; the recipe card teaches the Rite of the Gathered Lives.
     */
    private BookEntryModel gatheredLivesEntry(BookCategoryModel cat) {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("The Gathered Lives",
                "You found them, then — five of the old village tales, copied out in trembling hands and "
                        + "left to yellow in chest and cellar. I felt you reading. A tale is a kind of taglock, "
                        + "pupil: a knot tied around a life, holding it fast against forgetting."
                        + "\n\nMost witches work a coven of the living. Three sisters at a circle, five, "
                        + "seven — voices stacked until the working is loud enough to be heard. But a witch "
                        + "alone, far from any sister, learns a quieter trick. She does not gather the living. "
                        + "She gathers the remembered."));
        pages.addAll(WitcheryBooks.prose("The Telling",
                "I learned it the winter I was the last witch in three valleys. The others were under the "
                        + "rowans by then — Mistress Vale, who taught bells to listen; Selene of the cracked "
                        + "mirror; old Mother Hem, who tied malice backward; the broom-witch Nettie; and the "
                        + "midwife Annis, whose shard grew warm before a child ever turned."
                        + "\n\nI had no coven. I had their stories. So I read them aloud over the circle, one "
                        + "after another, until the air went the second kind of silent — not empty, but "
                        + "listening. And then they came. Not the women. The shape the women left in the world. "
                        + "Pale, footless, drifting a hand's breadth above the chalk."));
        pages.addAll(WitcheryBooks.prose("What They Are",
                "Do not mistake them for the dead returned. The dead have better things to do than stand in "
                        + "your draughty barn. What rises is the coven-shape itself — the weight of five lives "
                        + "well-remembered, lent to your working as if the sisters stood beside you again. "
                        + "They do not speak. They do not walk. They turn slowly to face the altar and wait, "
                        + "and while they wait, the circle counts them as kin."
                        + "\n\nThat is the whole of the gift: a witch alone may now cast what only a coven "
                        + "could. Call a familiar's beast, summon what needs more than one voice — the spectral "
                        + "sisters make up the number. They fade by themselves before long. Tales tire of "
                        + "being told, and so do the told."));
        pages.add(ritualCard("2000", 0, List.of(ringOtherwhere("medium")),
                WitcheryItems.MELLIFLUOUS_HUNGER.get(), WitcheryItems.MANDRAKE_ROOT.get(), WitcheryItems.WORMWOOD.get()));
        pages.addAll(WitcheryBooks.prose("The Working",
                "Draw a **medium Otherwhere circle** and lay an altar near with **2000 power** to spend. "
                        + "Offer a **Mellifluous Hunger** — the token that calls them — together with a "
                        + "**Mandrake Root** and a sprig of **Wormwood**. The hunger draws the shapes; the root "
                        + "and the wormwood give them somewhere to stand."
                        + "\n\nFive spectral witches rise and drift about the circle. They count toward any "
                        + "coven a later rite demands, so cast your coven-working while they linger. They keep "
                        + "no altar long — when they fade, simply gather the lives again."));
        return entry(cat, "circle_magic/gathered_lives", "The Spectral Coven",
                        WitcheryItems.MELLIFLUOUS_HUNGER.get(), 17, pages)
                .withCondition(BookAdvancementConditionModel.create()
                        .withAdvancementId(id("guide/gathered_lives")))
                .hideWhileLocked(true);
    }

    // ── 6. Conjuration & Fetishes ───────────────────────────────────────────────
    private BookCategoryModel conjuration() {
        BookCategoryModel cat = gated("conjuration", "Conjuration & Fetishes", WitcheryItems.BRAZIER.get(), 6, "burning");

        List<BookPageModel<?>> brazier = new ArrayList<>(WitcheryBooks.prose("The Brazier",
                "Fire is one of the oldest working tools in the craft, and the brazier is how you direct it. "
                        + "Load up to three ingredients by right-clicking; the bowl shows each offering as "
                        + "colored contents. Ignite it with **flint and steel**, then step back."
                        + "\n\nMost brazier workings draw power from a valid altar within sixteen blocks. If "
                        + "the flame sputters smoke and fizz without finishing, the altar is missing, invalid, "
                        + "or out of power. Empty-hand use on a burning brazier gathers **Wood Ash** and "
                        + "cancels the burn; water clears it outright."));
        brazier.add(recipe("brazier", "Brazier",
                "Iron ingots and a stick. The brazier need not be complicated; what burns inside it often "
                        + "is."));
        brazier.addAll(WitcheryBooks.prose("Named Workings",
                "**Conjure Spectres** - Wormwood, Bat Wool, and Graveyard Dust. A Spectre emerges after "
                        + "thirty seconds of altar-fed flame, with a rare Poltergeist riding the smoke. "
                        + "**Conjure Banshees** - Wormwood, Condensed Fear, and Graveyard Dust. "
                        + "**Conjure Poltergeist** - Wormwood, Refined Evil, and Focused Will; this one needs "
                        + "no altar. **Anguish of the Dead** - Tear of the Goddess, Blaze Rod, and Golden "
                        + "Apple. Strength radiates outward. **Fortification of the Corpse** - Tear of the "
                        + "Goddess, Diamond, and Golden Apple. Resistance gathers nearby. **Deathly Veil** - "
                        + "Fermented Eye, Ender Pearl, and Diamond. Light passes through. **Drain Growth** - "
                        + "Condensed Fear, Wormy Apple, and Graveyard Dust. Crops regress and "
                        + "undead knit together. **Graveyard Mist** - Nether Wart, Quicklime, and Blaze Powder. "
                        + "You will know it by the smoke. **Quick burnings** - Infernal Animus; Exhale of "
                        + "the Horned One; Rowan Berries; Fanciful Thread; Diamond Vapour with Fanciful "
                        + "Thread; or Gunpowder. These release their nearby player effects after a short "
                        + "burn."));

        List<BookPageModel<?>> fetishes = new ArrayList<>(WitcheryBooks.prose("Fetishes",
                "A spirit bound to a physical object becomes something useful — the scarecrow that watches "
                        + "with intent, the effigy that senses intrusion, the ladder that extends a boundary."
                        + "\n\nTo make a fetish, place the host object in a medium circle with an Attuned "
                        + "Stone, a Necromantic Stone, and a Boline, then sacrifice the required spirits near "
                        + "the circle. The rite binds them to the vessel."));
        fetishes.addAll(WitcheryBooks.prose("What a Fetish Does",
                "**Voodoo Protection** makes carried protection poppets more effective nearby. **Sentinel** "
                        + "sends spectres against uninvited guests. **Shrieking** emits a redstone pulse and a "
                        + "banshee's cry. **Disorientation** leaves armed or armored trespassers with "
                        + "unreliable senses. **Ghost Walking** spares those walking the spirit world nearby "
                        + "from being hurried back."
                        + "\n\nUse a Boline to cycle the trigger mode, and a Taglock Kit to say who counts as "
                        + "an intruder."));

        List<BookPageModel<?>> coffin = new ArrayList<>(WitcheryBooks.prose("The Coffin",
                "A vampire does not sleep in a bed. The sun makes ordinary beds inadvisable, and there is a "
                        + "certain indignity to them regardless. The coffin is better: sealed against "
                        + "daylight, and appropriate to the condition."
                        + "\n\nShift-right-click to open the lid, and right-click to enter and sleep. Any "
                        + "player may use it, but for vampires it resolves the problem of daylight with "
                        + "characteristic directness."));
        coffin.add(recipe("coffin", "Coffin",
                "Planks, logs, and a bed, in the proportions shown. The result is more durable than it "
                        + "looks."));

        List<BookPageModel<?>> necro = new ArrayList<>(WitcheryBooks.prose("Necromantic Stone",
                "A Necromantic Stone is the witch's first true token of authority over death-working. Many "
                        + "later rites ask for one, and an **Alluring Skull** answers only to it."));
        necro.add(spotlight(WitcheryItems.NECRO_STONE.get(), "Necromantic Stone",
                "On a small circle at night, offer an Attuned Stone, Bone, Rotten Flesh, Wood Ash, "
                        + "Gunpowder, and Spectral Dust, with **1000 altar power**."));
        necro.addAll(WitcheryBooks.prose("Current Use",
                "Today the stone has two dependable roles: it is consumed or preserved by later rites such "
                        + "as Prior Incarnation, Manifestation, fetish binding, and Spectral Stone work, and "
                        + "it can wake or silence an Alluring Skull."));

        List<BookPageModel<?>> treefyd = new ArrayList<>(WitcheryBooks.prose("Treefyd",
                "A Treefyd is a wooden guardian raised out of corrupted hedgecraft rather than bred. The "
                        + "witch prepares Treefyd Seeds, then plants them on living grass so the rooted thing "
                        + "may wake and walk."));
        treefyd.add(recipe("treefyd_seeds", "Treefyd Seeds",
                "Two seeds from **Mutandis Extremis**, **Reek of Misfortune**, **Ember Moss**, "
                        + "**Artichoke**, **Mandrake Root**, and a **Tear of the Goddess**."));
        treefyd.addAll(WitcheryBooks.prose("Planting the Seed",
                "Use the seed on the top of a grass block; it rises as your own guardian. Feeding an owned "
                        + "Treefyd another seed splits off a fresh one."
                        + "\n\nA **Creeper Heart** hardens it, a **Demon Heart** more so, and a **Boline** "
                        + "toggles whether it wanders when idle. A filled **Taglock Kit** teaches it to "
                        + "ignore a named player or one creature."));

        List<BookPageModel<?>> mandrake = new ArrayList<>(WitcheryBooks.prose("Mandrake",
                "Mandrake is never a polite harvest. A mature root sometimes comes quietly, and sometimes "
                        + "wakes as a shrieking creature instead — especially at night."
                        + "\n\nWhen the crop awakens, the living root keeps nearby players sickened until it "
                        + "is killed."));
        mandrake.add(recipe("earmuffs", "Earmuffs",
                "Leather and white wool. Worn on the head, they block the nausea scream — and blunt a "
                        + "Banshee's cry — so large harvests are far less miserable."));

        List<BookPageModel<?>> wicker = new ArrayList<>(WitcheryBooks.prose("The Wicker Man",
                "A Wicker Man is an invitation written in dry bundles and bad judgment. Rebuilt from "
                        + "Bloodied Wicker Bundles and burned, it calls the Horned Huntsman. The shape "
                        + "matters — a near miss is only a bonfire."));
        wicker.add(recipe("wicker_bundle", "Wicker Bundle",
                "Nine saplings. Witchery saplings — rowan, alder, hawthorn — count too, once grown."));
        wicker.add(recipe("bloodied_wicker_bundle", "Bloodied Wicker",
                "Five bundles and one **Infernal Blood** make five bloodied bundles. You need twenty for "
                        + "the figure."));
        wicker.addAll(WitcheryBooks.prose("The Figure",
                "Build a flat, human-shaped wall from exactly twenty Bloodied Wicker Bundles: two-wide "
                        + "legs, a four-wide body and arms, then a two-wide head, each row centered above "
                        + "the last."
                        + "\n\nLeave the surrounding cells empty — stray bloodied wicker spoils the match. "
                        + "Then use flint and steel on any bundle to summon the Horned Huntsman at its base."));

        List<BookPageModel<?>> leonard = new ArrayList<>(WitcheryBooks.prose("Shade of Leonard",
                "Leonard is an old, dark name — the black goat of the sabbath, the lord at the crossroads "
                        + "of infernal commerce. His Shade does not wander freely; he must be forced into the "
                        + "world through the Witch's Cauldron."));
        leonard.addAll(WitcheryBooks.prose("Summoning",
                "Heat and fill the Witch's Cauldron. Dissolve **Nether Wart**, a **Tear of the Goddess**, "
                        + "**Diamond Vapour**, a **Diamond**, and a **Nether Star** in that order — by dropping "
                        + "each into the bowl or by sneak-stirring it in by hand. Then hold a **Witches' Hat** "
                        + "and use it on the cauldron. No Gunpowder. The altar must hold at least 10,000 "
                        + "power. The Shade forms half-made and untouchable for a few moments before he draws "
                        + "his full strength together."));
        leonard.addAll(WitcheryBooks.prose("Behaviour",
                "Leonard is neutral until struck, for he reckons no mortal thing a threat. Provoked, no blow "
                        + "but your own hand will mark him — arrows and thrown things simply glance away — and "
                        + "even your strikes he shrugs down to almost nothing. He answers with the witch's own "
                        + "signs flung as fire and force, and lays the **Mortal Coil** on every soul nearby: a "
                        + "quiet curse that kills when it lifts, unless the Shade is slain first, which frees "
                        + "everyone it touched."
                        + "\n\nBelow half strength he calls **Lost Souls** in waves. Green souls take harm from "
                        + "anything; red resist all but fire and explosions; black resist all but magic and "
                        + "potions. While any remain, Leonard cannot be harmed. Wounded near to death he swells "
                        + "to a giant, harder still to fell."));
        leonard.addAll(WitcheryBooks.prose("Reward",
                "A defeated Shade drops **Leonard's Urn**, a vessel of considerable infernal standing. Its "
                        + "precise uses await further investigation — but the standing it confers among "
                        + "creatures of the night is real."));

        cat.withEntries(
                entry(cat, "conjuration/brazier", "The Brazier", WitcheryItems.BRAZIER.get(), 0, brazier),
                entry(cat, "conjuration/fetishes", "Fetishes", WitcheryItems.FETISH_SCARECROW.get(), 1, fetishes),
                entry(cat, "conjuration/coffin", "The Coffin", WitcheryItems.COFFIN.get(), 2, coffin),
                entry(cat, "conjuration/necromantic_stone", "Necromantic Stone", WitcheryItems.NECRO_STONE.get(), 3, necro),
                entry(cat, "conjuration/treefyd", "Treefyd", WitcheryItems.TREEFYD_SEEDS.get(), 4, treefyd),
                entry(cat, "conjuration/mandrake", "Mandrake", WitcheryItems.MANDRAKE_ROOT.get(), 5, mandrake),
                entry(cat, "conjuration/wicker_man", "The Wicker Man", WitcheryItems.BLOODIED_WICKER_BUNDLE.get(), 6, wicker),
                entry(cat, "conjuration/shade_of_leonard", "Shade of Leonard", WitcheryItems.LEONARDS_URN.get(), 7, leonard),
                gatedByItem("guide/obtained/fetish_scarecrow", whatsNext(cat, "conjuration", 8, "book_wands", "Witchcraft: Symbology", WitcheryItems.BOOK_WANDS.get(), "Symbology",
                        "The top reagent, an **Ent Twig**, is shed by Ents — grow a grove with the Rite of the Forest.")));
        return cat;
    }

    // ── 7. Symbology ────────────────────────────────────────────────────────────
    private BookCategoryModel symbology() {
        BookCategoryModel cat = gated("symbology", "Symbology", WitcheryItems.MYSTIC_BRANCH.get(), 7, "wands");
        List<BookPageModel<?>> mystic = new ArrayList<>(WitcheryBooks.prose("Mystic Branch",
                "The Mystic Branch is the witch's focus for traced workings and stolen creature force. "
                        + "Raise it, hold the channel, and sweep your gaze through the old directional "
                        + "signs until the branch knows the shape."));
        mystic.add(spotlight(WitcheryItems.MYSTIC_BRANCH.get(), "Mystic Branch",
                "Work the night-only rite that turns an Ent Twig and Mystic Unguent into a branch on paired "
                        + "circles, for **3000 altar power**."));
        mystic.addAll(WitcheryBooks.prose("Current Use",
                "Hold use to begin tracing. The HUD marks each up, down, left, and right stroke; release "
                        + "when the named sign appears to cast it, spending infused energy. Some signs demand "
                        + "Demon-soul knowledge before they answer."
                        + "\n\nThe branch now gives clear feedback when a sign answers or fails. It still also "
                        + "belongs to the wider infusion path, where creature powers and stored charges matter."));
        cat.withEntries(
                entry(cat, "symbology/mystic_branch", "Mystic Branch", WitcheryItems.MYSTIC_BRANCH.get(), 0, mystic),
                gatedByItem("guide/obtained/mystic_branch", whatsNext(cat, "symbology", 1, "book_poppets", "Witchcraft: Poppets & Taglocks", WitcheryItems.BOOK_POPPETS.get(), "Poppets & Taglocks")));
        return cat;
    }

    // ── 8. Poppets & Taglocks ───────────────────────────────────────────────────
    private BookCategoryModel poppets() {
        BookCategoryModel cat = gated("poppets", "Poppets & Taglocks", WitcheryItems.VOODOO_POPPET.get(), 8, "poppets");

        List<BookPageModel<?>> taglock = new ArrayList<>(WitcheryBooks.prose("Taglock Kit",
                "A taglock is a thread of sympathy connecting a practitioner to their subject. Without one, "
                        + "poppets are merely decorative, curses fall without direction, and certain rites "
                        + "cannot find their target."
                        + "\n\nRight-click a living creature directly, or steal a player's sample by crouching "
                        + "up behind them. Blood Roses, Leech Chests, and a freshly slept-in bed can hold the "
                        + "same sample for later theft — guard your own sleep accordingly."));
        taglock.add(recipe("taglock_kit", "Taglock Kit",
                "A glass bottle and a pin. Simple, and deliberately so — the difficulty is in what you do "
                        + "with it afterward."));

        List<BookPageModel<?>> poppetsList = new ArrayList<>(WitcheryBooks.prose("Poppets",
                "An effigy shaped to resemble a person and bound to them through a taglock carries a thread "
                        + "of the sympathy that connects all things. What happens to the effigy tends to "
                        + "happen to its subject — and it works in both directions."
                        + "\n\nBind a poppet by right-clicking it with a filled taglock. Then place it on a "
                        + "Poppet Shelf, or carry a protection poppet in your own inventory."));
        poppetsList.add(recipe("voodoo_poppet", "Voodoo Poppet",
                "The Voodoo Poppet is the foundation of this practice. From it, other forms follow."));
        poppetsList.addAll(WitcheryBooks.prose("On Protection",
                "Protection poppets are consumed when triggered — they give themselves to prevent something "
                        + "worse happening to you. Death, Earth, Water, Fire, Hunger; each covers a different "
                        + "category of misfortune, and Voodoo Protection counters hostile poppet workings."
                        + "\n\nKeep spares. The shelf holds nine."));

        List<BookPageModel<?>> shelf = new ArrayList<>(WitcheryBooks.prose("The Poppet Shelf",
                "A poppet in a pocket is useful. A poppet on a shelf that activates itself when needed is "
                        + "more useful still."
                        + "\n\nThe Poppet Shelf holds up to nine. When you take damage or are killed, it scans "
                        + "nearby shelves for poppets bound to you and triggers the right protection. You need "
                        + "not be watching — that is the point."));
        shelf.add(recipe("poppet_shelf", "Poppet Shelf",
                "Build it, fill it, and stop thinking about it. It will think for itself when the time "
                        + "comes."));

        cat.withEntries(
                entry(cat, "poppets/taglock_kit", "Taglock Kit", WitcheryItems.TAGLOCK_KIT.get(), 0, taglock),
                entry(cat, "poppets/poppets", "Poppets", WitcheryItems.VOODOO_POPPET.get(), 1, poppetsList),
                entry(cat, "poppets/poppet_shelf", "The Poppet Shelf", WitcheryItems.POPPET_SHELF.get(), 2, shelf),
                gatedByItem("guide/obtained/poppet", whatsNext(cat, "poppets", 3, "book_spirit_world", "Witchcraft: The Spirit World", WitcheryItems.BOOK_SPIRIT_WORLD.get(), "The Spirit World")));
        return cat;
    }

    // ── 9. The Spirit World ─────────────────────────────────────────────────────
    private BookCategoryModel spiritWorld() {
        BookCategoryModel cat = gated("spirit_world", "The Spirit World", WitcheryItems.BREW_OF_SLEEPING.get(), 9, "spirit_world");

        List<BookPageModel<?>> spirit = new ArrayList<>(WitcheryBooks.prose("The Spirit World",
                "The world you walk in is not the only world that exists. The Spirit World lies beside it, "
                        + "separated by a boundary that thins around places of power and at the edges of sleep."
                        + "\n\nTo enter, drink a Brew of Sleeping near an altar, or eat an Apple of Sleeping. "
                        + "You will leave your body behind. It will wait for you — try to come back before "
                        + "something finds it."));
        spirit.add(spotlight(WitcheryItems.BREW_OF_SLEEPING.get(), "Brew of Sleeping",
                "Carries you across, leaving your inventory behind. Only a few things cross with you — "
                        + "your Icy Needles, your Mutandis, and this very Primer — and a little of what you "
                        + "gather there, the cotton and the spirits, will follow you home."));
        spirit.add(spotlight(WitcheryItems.ICY_NEEDLE.get(), "Icy Needle",
                "Brings you back at once, used up. Dying also returns you, less comfortably. In emergencies: "
                        + "**/witchery exit**."));
        spirit.add(stationRecipeCard("kettle_brew_of_sleeping", "Kettle: Brew of Sleeping",
                "Brew it from **Purified Milk**, a **Cookie**, **Brew of Love**, **Whiff of Magic**, an "
                        + "**Icy Needle**, and an **Artichoke** — three bottles per working."));

        List<BookPageModel<?>> dream = new ArrayList<>(WitcheryBooks.prose("Dream Weavers",
                "Sleep is not passive, and the spirit world is not a waiting room. A Dream Catcher placed "
                        + "near the sleeping body shapes what happens when consciousness crosses the boundary "
                        + "and returns."
                        + "\n\nDifferent weavings give different effects on waking. Only one catcher near you "
                        + "takes effect — choose deliberately. A placed weave now chimes when it catches, "
                        + "and waking feedback tells you which dream-thread stirred."));
        dream.add(recipe("weave_fleet_foot", "Fleet Foot",
                "The old quickness weaving: Item Frame, Fanciful Thread, feathers, Diamond Vapour, Tormented "
                        + "Twine, and long swiftness/slowness splash potions."));
        dream.add(recipe("weave_iron_arm", "Iron Arm",
                "The same weave frame, but swaps in long strength and weakness splash potions."));
        dream.add(recipe("weave_fasting", "Fasting",
                "A long poison splash potion and Mellifluous Hunger in the opposing potion slot."));
        dream.add(recipe("weave_nightmares", "Nightmares",
                "The harsher Tormented Twine frame, woven with Diamond Vapour and long poison/night-vision "
                        + "splash potions."));
        dream.add(recipe("weave_intensity", "Intensity",
                "Keeps the old frame but binds Brew of Flowing Spirit against Brew of Sleeping."));

        List<BookPageModel<?>> crystal = new ArrayList<>(WitcheryBooks.prose("The Crystal Ball",
                "The crystal ball does not tell you what you want to hear. It tells you what it perceives — "
                        + "oblique, contextual, and occasionally alarming. Right-click it empty-handed near an "
                        + "altar holding at least 500 power for a reading."
                        + "\n\nThere is a cooldown between readings; the ball will not repeat itself. Whether "
                        + "that is a limitation or a mercy is a matter of interpretation."));
        crystal.add(spotlight(WitcheryItems.QUARTZ_SPHERE.get(), "Quartz Sphere",
                "Infuse it at night on a small circle. Offer a Gold Ingot and Happenstance Oil, and keep "
                        + "2000 altar power ready."));
        crystal.add(spotlight(WitcheryItems.CRYSTAL_BALL.get(), "Crystal Ball",
                "The rite returns a Crystal Ball attuned for fortune work. Use it near a powered altar, or "
                        + "focus it with waystones and taglocks. Bad foci are refused before the altar is "
                        + "charged; a true focus wakes the ball with violet motes."));

        cat.withEntries(
                entry(cat, "spirit_world/spirit_world", "The Spirit World", WitcheryItems.BREW_OF_SLEEPING.get(), 0, spirit),
                entry(cat, "spirit_world/dream_weavers", "Dream Weavers", WitcheryItems.DREAM_CATCHER.get(), 1, dream),
                entry(cat, "spirit_world/crystal_ball", "The Crystal Ball", WitcheryItems.CRYSTAL_BALL.get(), 2, crystal),
                gatedByItem("guide/obtained/brew_of_flowing_spirit", whatsNext(cat, "spirit_world", 3, "book_creatures", "Witchcraft: Creatures", WitcheryItems.BOOK_CREATURES.get(), "Creatures")));
        return cat;
    }

    // ── 10. Creatures ───────────────────────────────────────────────────────────
    private BookCategoryModel creatures() {
        BookCategoryModel cat = gated("creatures", "Creatures", WitcheryItems.OWL_SPAWN_EGG.get(), 10, "creatures");

        List<BookPageModel<?>> familiars = new ArrayList<>(WitcheryBooks.prose("Familiars",
                "A familiar is not merely a tame animal — it is a companion bound to you through ritual, "
                        + "carrying part of your craft onward after the binding is done."
                        + "\n\nYou may have one at a time. Tame the creature, then perform the Rite of Familiar "
                        + "Binding. The binding also leaves it strangely resilient to fire, falls, and "
                        + "drowning."));
        familiars.add(creature("witchery:owl", "The Owl",
                "Reads the air and warns of what approaches. At full speed on a broomstick, an owl grants a "
                        + "significant bonus — they seem to enjoy the height.", 0.7F, -0.3F));
        familiars.add(creature("witchery:familiar_cat", "The Cat",
                "Indifferent to most things, hostile to supernatural intrusion. Curses find less purchase "
                        + "near a cat, and your own circle malice takes deeper root.", 1.0F, -0.2F));
        familiars.add(creature("witchery:toad", "The Toad",
                "Patient, damp, and helpful to a brewer. It gathers what falls, coaxes the odd extra bottle "
                        + "from the kettle, and strengthens brews you drink yourself.", 1.0F, -0.3F));

        List<BookPageModel<?>> hunter = new ArrayList<>(WitcheryBooks.prose("Witch Hunters",
                "They exist because we exist. Witch Hunters are organised, motivated, and unpleasant — they "
                        + "track vampires and werewolves as a profession."
                        + "\n\nThey attack at range with crossbows of silver bolts and close to melee when "
                        + "needed. Silver does extra damage to the transformed; silvered armor reduces but "
                        + "does not erase it."));
        hunter.add(creature("witchery:witch_hunter", "Witch Hunter",
                "Known by the hat, the coat, and the look of someone certain of their cause. They drop "
                        + "silver bolts and the occasional armour piece.", 0.85F, -0.4F));

        List<BookPageModel<?>> hobgoblin = new ArrayList<>(WitcheryBooks.prose("Hobgoblins",
                "Hobgoblins are small, territorial, and largely reasonable as long as you do not disturb "
                        + "their patron. They live in woodland huts and trade in koboldite, a metal found "
                        + "only through their commerce."
                        + "\n\nThe Statue of the Hobgoblin Patron is a fine decoration — but right-clicking "
                        + "it has consequences. You have been told."));
        hobgoblin.add(creature("witchery:hobgoblin", "Hobgoblin",
                "Five trades, each unlocked by the last. Coal into dust; dust and gold into nuggets; work "
                        + "upward. The koboldite at the end is genuinely useful.", 0.85F, -1.0F));

        List<BookPageModel<?>> spectral = new ArrayList<>(WitcheryBooks.prose("Spectral Familiar",
                "The old **Rite of Summoning** did not call an owl, toad, or cat that already lived in the "
                        + "world. It called a Spectral Familiar — a ghostly companion that seeks out shinies "
                        + "and strange things for its witch."));
        spectral.addAll(WitcheryBooks.prose("Searching for Ores",
                "Feed it an Iron or Gold Ingot, Lapis, Redstone, Coal, an Emerald, or a Diamond. The gift "
                        + "is spent, the familiar searches, and if it finds that ore below it will sit and "
                        + "call out the spot."
                        + "\n\nRepeated searches grow risky: after the first success there is a rising chance "
                        + "the familiar simply vanishes."));

        List<BookPageModel<?>> traps = new ArrayList<>(WitcheryBooks.prose("Creature Traps",
                "Some creatures are better studied after they have stopped moving. The traps are low metal "
                        + "plates: place one, give it a moment to arm, and keep your own feet clear."));
        traps.add(recipe("beartrap", "Beartrap",
                "Iron and shears. Once armed it catches living creatures, snaps shut for four damage, and "
                        + "applies strong slowness. Right-click to open or close it."));
        traps.add(recipe("wolftrap", "Wolftrap",
                "Upgrade a Beartrap with Silver, Null Catalysts, and Wolfsbane. Werewolves trigger it; "
                        + "others do not. It drops nothing when broken."));

        cat.withEntries(
                entry(cat, "creatures/familiars", "Familiars", WitcheryItems.OWL_SPAWN_EGG.get(), 0, familiars),
                entry(cat, "creatures/witch_hunter", "Witch Hunters", WitcheryItems.HUNTER_HAT.get(), 1, hunter),
                entry(cat, "creatures/hobgoblin", "Hobgoblins", WitcheryItems.KOBOLDITE_INGOT.get(), 2, hobgoblin),
                entry(cat, "creatures/spectral_familiar", "Spectral Familiar", WitcheryItems.DROP_OF_LUCK.get(), 3, spectral),
                entry(cat, "creatures/creature_traps", "Creature Traps", WitcheryItems.BEARTRAP.get(), 4, traps),
                whatsNext(cat, "creatures", 5, "book_transformations", "Witchcraft: Transformations", WitcheryItems.BOOK_TRANSFORMATIONS.get(), "Transformations"));
        return cat;
    }

    // ── 11. Transformations (end of the chain) ──────────────────────────────────
    private BookCategoryModel transformations() {
        BookCategoryModel cat = gated("transformations", "Transformations", WitcheryItems.BREW_OF_INFECTION.get(), 11, "transformations");

        List<BookPageModel<?>> vampirism = new ArrayList<>(WitcheryBooks.prose("Vampirism",
                "There are those who choose this path and those who have it chosen for them. Either way, the "
                        + "nature of vampirism is not subtle: you are sustained by blood, strengthened by it, "
                        + "and diminished without it."
                        + "\n\nDrain blood from living creatures by right-clicking them. A vampire at full "
                        + "blood is formidable; one running dry is much less so."));
        vampirism.addAll(WitcheryBooks.prose("What the Blood Gives",
                "Speed, strength, and reach all scale with your blood level. Night vision is always yours, "
                        + "and your bite takes blood from the living. You resist most damage, and you can "
                        + "sleep through daylight in a coffin — its own kind of grace."));
        vampirism.addAll(WitcheryBooks.prose("What the Blood Costs",
                "Sunlight burns; direct daylight is a constant danger. Silver wounds heal slowly and hurt "
                        + "more. Witch Hunters treat you as a professional priority. Blood drains constantly — "
                        + "feed regularly. And there is no going back, not easily."));
        vampirism.addAll(WitcheryBooks.prose("Vampire Clothing",
                "Woven Cruor is the base cloth. At level 4+, spend 125 Blood Power with a **Bone Needle** on "
                        + "placed White Wool to make Blood-stained Wool, then smelt it to Woven Cruor and "
                        + "tailor the light set."
                        + "\n\nTwo light pieces make your bite draw more blood; three extend mesmerise and "
                        + "grant a chance to survive lethal fire, which all four make much stronger."));
        vampirism.addAll(WitcheryBooks.prose("The Stake",
                "A Stake is simple and final: craft one from a Stick ringed with Garlic, then use it on a "
                        + "sleeping vampire to destroy them."
                        + "\n\nThe Glass Goblet is the other path — a level-9 vampire can fill it with 125 "
                        + "Blood Power, and drinking a filled goblet begins vampirism unless the drinker is a "
                        + "werewolf. Lilith may also fill an empty goblet after she is bested; drink that "
                        + "blood to accept the curse. Garlic asks her to lift vampirism, a poppy asks for "
                        + "deeper mastery, and an enchantable item asks for her old blessing."));

        List<BookPageModel<?>> lycanthropy = new ArrayList<>(WitcheryBooks.prose("Lycanthropy",
                "The wolf's curse comes in through a rite, or through the attention of one already afflicted, "
                        + "and it does not announce itself politely. The full moon forces the change; the rest "
                        + "of the time you choose — though choosing gets harder as the moon waxes."
                        + "\n\nIn wolf form the body is stronger, faster, and harder to slow, while the mind "
                        + "remains your own, more or less. Find the Wolf Altar and tend it. Mastery matters."));
        lycanthropy.addAll(WitcheryBooks.prose("The Wolf's Gifts",
                "Strength and speed well beyond a human's in wolf form. Regeneration mends the body. With the "
                        + "right talisman, a howl becomes a weapon. The Wolf Altar grants mastery from zero to "
                        + "three — and mastery three is not subtle."));

        cat.withEntries(
                entry(cat, "transformations/vampirism", "Vampirism", WitcheryItems.BREW_OF_INFECTION.get(), 0, vampirism),
                entry(cat, "transformations/lycanthropy", "Lycanthropy", WitcheryItems.WOLFSBANE_FLOWER.get(), 1, lycanthropy));
        return cat;
    }
}
