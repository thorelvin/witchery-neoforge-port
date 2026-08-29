package com.emoniph.witchery.book;

import com.emoniph.witchery.book.page.ItemReferencePageModel;
import com.emoniph.witchery.book.page.StationRecipePageModel;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryItems;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared Modonomicon pages generated from the live composed-brew registry. */
final class BrewReferencePages {

    private static final Identifier CAULDRON_BACKGROUND =
            WitcheryBooks.id("textures/gui/book/bg_cauldron.png");
    private static final int FIRST_PAGE_ROWS = 4;
    private static final int CONTINUATION_ROWS = 6;

    private BrewReferencePages() {}

    static List<BookPageModel<?>> completeReference() {
        List<BookPageModel<?>> pages = new ArrayList<>(WitcheryBooks.prose("Brew Reagents",
                "A custom brew remembers each ingredient in the order it enters the Witch's Cauldron. "
                        + "Drop ordinary brew ingredients into heated water; ingredients marked **sneak-stir** "
                        + "must be added by sneaking and right-clicking the cauldron, so they do not feed a "
                        + "different cauldron recipe. Ingredients marked **drop or sneak-stir** accept either "
                        + "gesture."
                        + "\n\nThe notes below are drawn from the live brew registry. **Level** is the capacity "
                        + "an effect consumes. **Power** is altar power charged when the brew is bottled, "
                        + "bucketed, or released by ritual."));
        pages.addAll(exampleBrews());
        pages.addAll(ritualTargets());
        pages.addAll(capacity());
        pages.addAll(modifiers());
        pages.addAll(reachAndTargets());
        pages.addAll(dispersal());
        pages.addAll(effects());
        pages.addAll(actions());
        return pages;
    }

    static List<BookPageModel<?>> exampleBrews() {
        return List.of(
                vesselRecipe("Brew: Drought Splash",
                        "Stir Nether Wart, Tear of the Goddess, Witches' Salt, then Gunpowder. Bottle it to dry "
                                + "water source blocks near impact.",
                        WitcheryItems.SPLASH_BREW_BOTTLE.get(), 1,
                        List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(),
                                WitcheryItems.WITCHES_SALT.get(), Items.GUNPOWDER, Items.GLASS_BOTTLE)),
                vesselRecipe("Brew: Drought Bucket",
                        "Swap Gunpowder for Wormwood, then scoop with a bucket. The moving brew eats water "
                                + "source blocks it touches.",
                        WitcheryItems.BREW_BUCKET.get(), 1,
                        List.of(Items.NETHER_WART, WitcheryItems.TEAR_OF_GODDESS.get(),
                                WitcheryItems.WITCHES_SALT.get(), WitcheryItems.WORMWOOD.get(), Items.BUCKET)),
                vesselRecipe("Brew: Ice World Splash",
                        "Open level-five capacity, stir an Ender Eye, then add Gunpowder. The impact remakes "
                                + "nearby terrain as snow and ice; Extent ingredients widen the frozen sphere.",
                        WitcheryItems.SPLASH_BREW_BOTTLE.get(), 1,
                        List.of(WitcheryItems.MANDRAKE_ROOT.get(), Items.NETHER_WART,
                                WitcheryItems.TEAR_OF_GODDESS.get(), Items.ENDER_EYE,
                                Items.GUNPOWDER, Items.GLASS_BOTTLE)));
    }

    static List<BookPageModel<?>> ritualTargets() {
        return WitcheryBooks.prose("Ritual Targets",
                "A brim-full heated cauldron may release a finished brew when you sneak-use **Tongue of Dog**. "
                        + "For a living target, add that player or creature's **filled Taglock Kit last**. The "
                        + "Taglock adds 1,000 altar power, works without a splash ingredient, and prevents "
                        + "bottling or further stirring. Tongue of Dog then serves only as the release gesture."
                        + "\n\nDistance grows with a nearby coven; six coven witches remove the same-world limit, "
                        + "while cross-dimensional targeting also requires the Shade of Leonard. Full Hunter "
                        + "clothes, a protecting Area Marker, or a bound Voodoo Protection Poppet may absorb the "
                        + "working after its power is spent.");
    }

    static List<BookPageModel<?>> capacity() {
        return reference("Capacity",
                "Capacity ingredients widen the brew before an effect or action can fit. The cauldron also "
                        + "holds no more than eight physical ingredients in one batch.",
                Brews.Ingredient.Kind.CAPACITY);
    }

    static List<BookPageModel<?>> modifiers() {
        return reference("Power & Duration",
                "These shape the next effect or action, then reset after that working is recorded.",
                Brews.Ingredient.Kind.STRENGTH, Brews.Ingredient.Kind.DURATION,
                Brews.Ingredient.Kind.INVERT, Brews.Ingredient.Kind.DISABLE_STRENGTH_CEILING,
                Brews.Ingredient.Kind.NO_PARTICLES, Brews.Ingredient.Kind.DRINK_SPEED,
                Brews.Ingredient.Kind.COLOR);
    }

    static List<BookPageModel<?>> reachAndTargets() {
        return reference("Reach & Targets",
                "These alter where the brew may act, how wide it reaches, or how long moving gas and liquid "
                        + "persist.",
                Brews.Ingredient.Kind.EXTENT, Brews.Ingredient.Kind.LIFETIME,
                Brews.Ingredient.Kind.DISABLE_BLOCK_TARGET, Brews.Ingredient.Kind.DISABLE_ENTITY_TARGET);
    }

    static List<BookPageModel<?>> dispersal() {
        return reference("Dispersal",
                "Dispersal ingredients choose how the finished brew leaves its bottle or cauldron. A triggered "
                        + "curse overrides splash, gas, and liquid delivery.",
                Brews.Ingredient.Kind.SPLASH, Brews.Ingredient.Kind.GAS, Brews.Ingredient.Kind.LIQUID,
                Brews.Ingredient.Kind.TRIGGER, Brews.Ingredient.Kind.RITUAL, Brews.Ingredient.Kind.WARNING);
    }

    static List<BookPageModel<?>> effects() {
        return reference("Potion Reagents",
                "These ingredients become the brew's potion effect when enough capacity is available.",
                Brews.Ingredient.Kind.EFFECT);
    }

    static List<BookPageModel<?>> actions() {
        return reference("Craft Actions",
                "These ingredients perform an old craft action instead of applying a normal potion effect.",
                Brews.Ingredient.Kind.ACTION);
    }

    static StationRecipePageModel vesselRecipe(String title, String caption, ItemLike result, int count,
                                                List<? extends ItemLike> ingredients) {
        StationRecipePageModel page = StationRecipePageModel.create()
                .withBackground(CAULDRON_BACKGROUND, 112, 60)
                .withTitle(Component.literal(title))
                .withText(WitcheryBooks.ink(caption));
        int perRow = 3;
        int pitch = 16;
        int centerX = 51;
        for (int index = 0; index < ingredients.size(); index++) {
            int row = index / perRow;
            int column = index % perRow;
            int itemsInRow = Math.min(perRow, ingredients.size() - row * perRow);
            int rowWidth = itemsInRow * pitch;
            page.withSlot(ingredients.get(index), 1,
                    centerX - rowWidth / 2 + column * pitch, 2 + row * pitch);
        }
        page.withSlot(result, count, 89, 33);
        return page;
    }

    private static List<BookPageModel<?>> reference(String title, String intro,
                                                     Brews.Ingredient.Kind... kinds) {
        List<Brews.IngredientReference> references = Brews.ingredientReferences().stream()
                .filter(reference -> matchesKind(reference, kinds))
                .toList();
        List<BookPageModel<?>> pages = new ArrayList<>();
        int index = 0;
        boolean firstPage = true;
        while (index < references.size()) {
            int rowsThisPage = firstPage ? FIRST_PAGE_ROWS : CONTINUATION_ROWS;
            ItemReferencePageModel page = ItemReferencePageModel.create()
                    .withTitle(Component.literal(firstPage ? title : title + " (cont.)"));
            if (firstPage) {
                page.withText(WitcheryBooks.ink(intro));
            }
            int end = Math.min(references.size(), index + rowsThisPage);
            for (; index < end; index++) {
                Brews.IngredientReference reference = references.get(index);
                page.withRow(reference.item(), WitcheryBooks.ink(referenceLine(reference)));
            }
            pages.add(page);
            firstPage = false;
        }
        return pages;
    }

    private static boolean matchesKind(Brews.IngredientReference reference, Brews.Ingredient.Kind... kinds) {
        for (Brews.Ingredient.Kind kind : kinds) {
            if (reference.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private static String referenceLine(Brews.IngredientReference reference) {
        String role = switch (reference.kind()) {
            case EFFECT -> effectRole(reference);
            case ACTION -> actionRole(reference);
            case CAPACITY -> "capacity +" + reference.capacityIncrement() + " up to " + reference.capacityCeiling();
            case STRENGTH -> "strength tier " + reference.modifierCeiling();
            case DURATION -> "duration tier " + reference.modifierCeiling();
            case INVERT -> "inverts the next effect or action";
            case SPLASH -> reference.item() == WitcheryItems.ARTICHOKE.get()
                    ? "instant or splash dispersal" : "splash dispersal";
            case GAS -> "moving gas dispersal";
            case LIQUID -> "moving liquid dispersal";
            case TRIGGER -> "triggered curse dispersal";
            case WARNING -> "binds nearby spoken warning text";
            case RITUAL -> reference.item() == WitcheryItems.BOUND_TAGLOCK_KIT.get()
                    ? "direct living-entity ritual target; add last"
                    : "ritual cauldron release trigger";
            case COLOR -> "sets bottle colour";
            case DRINK_SPEED -> reference.drinkSpeedModifier() < 0 ? "faster drinking" : "slower drinking";
            case EXTENT -> "impact extent tier " + reference.modifierCeiling();
            case LIFETIME -> "gas or liquid lifetime tier " + reference.modifierCeiling();
            case NO_PARTICLES -> "hides potion particles";
            case DISABLE_BLOCK_TARGET -> "skips block targets";
            case DISABLE_ENTITY_TARGET -> "skips entity targets";
            case DISABLE_STRENGTH_CEILING -> "removes the strength ceiling";
        };
        return "**" + itemName(reference.item()) + "** - " + role + referenceMeta(reference) + ".";
    }

    private static String referenceMeta(Brews.IngredientReference reference) {
        List<String> parts = new ArrayList<>();
        if (reference.capacityRequired() > 0) {
            parts.add("level " + reference.capacityRequired());
        }
        if (reference.powerCost() > 0) {
            parts.add(reference.powerCost() + " power");
        }
        if (reference.explicitStirOnly()) {
            parts.add("sneak-stir");
        } else if (reference.explicitStirAllowed()) {
            parts.add("drop or sneak-stir");
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private static String effectRole(Brews.IngredientReference reference) {
        String effect = nameFromId(reference.effectId());
        if (reference.invertedEffectId() != null) {
            effect += " / inverted: " + nameFromId(reference.invertedEffectId());
        }
        return effect;
    }

    private static String actionRole(Brews.IngredientReference reference) {
        String action = actionName(reference.actionNameKey());
        if (reference.invertedActionNameKey() != null) {
            action += " / inverted: " + actionName(reference.invertedActionNameKey());
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
            case "witchery:mindrakebulb" -> "Minedrake Bulb";
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
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            String lower = word.toLowerCase(Locale.ROOT);
            if (!result.isEmpty() && (lower.equals("of") || lower.equals("and") || lower.equals("the"))) {
                result.append(lower);
            } else {
                result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return result.toString();
    }
}
