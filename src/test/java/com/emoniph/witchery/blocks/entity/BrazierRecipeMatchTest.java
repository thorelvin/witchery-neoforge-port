package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrazierRecipeMatchTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keepsSourceRecipesAndSixModernUseCases() {
        assertEquals(14, BrazierBlockEntity.RECIPES.size(),
                "8 source brazier recipes plus the 6 modern completion recipes");
    }

    @Test
    void sourceThreeItemRecipesMatchInAnySlotOrder() {
        assertTrue(matches(
                WitcheryItems.GRAVEYARD_DUST.get(),
                WitcheryItems.WORMWOOD.get(),
                WitcheryItems.BAT_WOOL.get()));
    }

    @Test
    void newSingleAndTwoItemRecipesRejectExtraIngredients() {
        assertTrue(matches(Items.GUNPOWDER));
        assertTrue(matches(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get()));

        assertEquals(0, matchCount(Items.GUNPOWDER, Items.REDSTONE),
                "single ingredient use cases must not trigger when another slot is filled");
        assertEquals(0, matchCount(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get(), Items.REDSTONE),
                "two ingredient use cases must not trigger when another slot is filled");
    }

    @Test
    void modernCompletionUseCasesResolveQuickly() {
        assertEquals(20, matchingRecipe(Items.GUNPOWDER).burnTicks());
        assertEquals(20, matchingRecipe(WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get()).burnTicks());
    }

    private static boolean matches(Item... items) {
        return matchCount(items) == 1;
    }

    private static BrazierBlockEntity.BrazierRecipe matchingRecipe(Item... items) {
        ItemStack first = stack(items, 0);
        ItemStack second = stack(items, 1);
        ItemStack third = stack(items, 2);
        for (BrazierBlockEntity.BrazierRecipe recipe : BrazierBlockEntity.RECIPES) {
            if (recipe.matches(first, second, third)) {
                return recipe;
            }
        }
        throw new AssertionError("No matching brazier recipe");
    }

    private static int matchCount(Item... items) {
        ItemStack first = stack(items, 0);
        ItemStack second = stack(items, 1);
        ItemStack third = stack(items, 2);
        int count = 0;
        for (BrazierBlockEntity.BrazierRecipe recipe : BrazierBlockEntity.RECIPES) {
            if (recipe.matches(first, second, third)) {
                count++;
            }
        }
        return count;
    }

    private static ItemStack stack(Item[] items, int index) {
        return index < items.length ? new ItemStack(items[index]) : ItemStack.EMPTY;
    }
}
