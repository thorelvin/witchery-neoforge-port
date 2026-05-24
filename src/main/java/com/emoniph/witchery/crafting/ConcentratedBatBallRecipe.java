package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The original bat-brewing chain relies on packed bat-filled critter snares,
 * not the temporary spawn-egg placeholder the port used early on.
 */
public class ConcentratedBatBallRecipe extends CustomRecipe {
    private static final String CAUGHT_TAG = "Caught";
    private static final int BAT_CAUGHT = 1;

    public ConcentratedBatBallRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matchesPattern(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return matchesPattern(input) ? new ItemStack(WitcheryItems.CONCENTRATED_BAT_BALL.get()) : ItemStack.EMPTY;
    }

    private static boolean matchesPattern(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return false;
        return isSlimeBall(input, 0, 0)
                && isBatSnare(input, 1, 0)
                && isSlimeBall(input, 2, 0)
                && isBatSnare(input, 0, 1)
                && input.getItem(slot(input, 1, 1)).isEmpty()
                && isBatSnare(input, 2, 1)
                && isSlimeBall(input, 0, 2)
                && isBatSnare(input, 1, 2)
                && isSlimeBall(input, 2, 2);
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<ConcentratedBatBallRecipe> getSerializer() {
        return WitcheryRecipeSerializers.CONCENTRATED_BAT_BALL.get();
    }

    private static boolean isSlimeBall(CraftingInput input, int x, int y) {
        return input.getItem(slot(input, x, y)).is(net.minecraft.world.item.Items.SLIME_BALL);
    }

    private static boolean isBatSnare(CraftingInput input, int x, int y) {
        ItemStack stack = input.getItem(slot(input, x, y));
        if (!stack.is(WitcheryItems.CRITTER_SNARE.get())) {
            return false;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr(CAUGHT_TAG, 0) == BAT_CAUGHT;
    }

    private static int slot(CraftingInput input, int x, int y) {
        return x + y * input.width();
    }
}
