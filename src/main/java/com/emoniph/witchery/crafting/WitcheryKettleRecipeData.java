package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Registers all kettle brew recipes.
 * Ported from WitcheryRecipes.java kettle section.
 * Ingredients are order-independent (matched as a set).
 */
public class WitcheryKettleRecipeData {

    public static void register() {
        WitcheryKettleRecipes k = WitcheryKettleRecipes.instance();

        // Color values are ARGB ints from the original (used for liquid tint in the kettle renderer)

        // Brew of Raising — 3 bottles, 500 power, dark greenish
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_RAISING.get(), 3), 1, 500f, 0xFF4B3D57,
                new ItemStack(Items.BONE),
                new ItemStack(Items.WHEAT),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.SUGAR));

        // Brew of Sleeping — 3 bottles, 0 power, dark blue
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_SLEEPING.get(), 3), 1, 0f, 0xFF1A2B6D,
                new ItemStack(Items.WHEAT),
                new ItemStack(Items.SUGAR),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.POPPY),
                new ItemStack(Items.BLUE_DYE));

        // Brew of Flowing Spirit — 3 bottles, 0 power, blue-purple
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_FLOWING_SPIRIT.get(), 3), 1, 0f, 0xFF5B3080,
                new ItemStack(Items.SLIME_BALL),
                new ItemStack(Items.GHAST_TEAR),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.ENDER_PEARL));

        // Brew of Wasting — 3 bottles, 0 power, dark yellow-brown
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_WASTING.get(), 3), 1, 0f, 0xFFBC9924,
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.BONE),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.GUNPOWDER));

        // Brew of Erosion — 3 bottles, 0 power, grey-brown
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_EROSION.get(), 3), 1, 0f, 0xFFBBB8B0,
                new ItemStack(Items.SAND),
                new ItemStack(Items.GRAVEL),
                new ItemStack(Items.FLINT),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.FERMENTED_SPIDER_EYE));

        // Brew of Infection — 3 bottles, 0 power, sickly green
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_INFECTION.get(), 3), 1, 0f, 0xFF557741,
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.SLIME_BALL),
                new ItemStack(Items.GUNPOWDER));

        // Brew of Love — 3 bottles, 0 power, pink
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_LOVE.get(), 3), 1, 0f, 0xFFF580A0,
                new ItemStack(Items.DIRT),
                new ItemStack(Items.COCOA_BEANS),
                new ItemStack(Items.SUGAR),
                new ItemStack(Items.APPLE),
                new ItemStack(Items.LILY_PAD),
                new ItemStack(Items.POPPY));

        // Brew of the Grotesque — 3 bottles, 500 power, dark grey
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get(), 3), 1, 500f, 0xFF3D3D56,
                new ItemStack(Items.BOOK),
                new ItemStack(Items.SUGAR),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.SLIME_BALL),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.ROTTEN_FLESH));

        // Brew of Revealing — 3 bottles, 0 power, gold
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_REVEALING.get(), 3), 1, 0f, 0xFFFFCC00,
                new ItemStack(Items.GLOWSTONE_DUST),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.SUGAR),
                new ItemStack(Items.SPIDER_EYE));

        // Brew of Poison (Depths) — 3 bottles, 0 power, dark teal
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_POISON.get(), 3), 1, 0f, 0xFF1A5050,
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.PUFFERFISH),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.GUNPOWDER));

        // Brew of Frost (Ice) — 3 bottles, 1000 power, icy blue
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_FROST.get(), 3), 1, 1000f, 0xFFADD8E6,
                new ItemStack(Items.SNOWBALL),
                new ItemStack(Items.WHEAT_SEEDS),
                new ItemStack(Items.SUGAR),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.ICE),
                new ItemStack(Items.GUNPOWDER));

        // Brew of Foaming (Hitchcock/bats) — 3 bottles, 0 power, dark
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_FOAMING.get(), 3), 1, 0f, 0xFF222222,
                new ItemStack(Items.INK_SAC),
                new ItemStack(Items.FEATHER),
                new ItemStack(Items.BONE),
                new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.SLIME_BALL),
                new ItemStack(Items.GUNPOWDER));

        // Brew of Foulness — 3 bottles, 0 power, dark ink
        k.addRecipe(new ItemStack(WitcheryItems.BREW_OF_FOULNESS.get(), 3), 1, 0f, 0xFF111111,
                new ItemStack(Items.INK_SAC),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.GUNPOWDER));
    }
}
