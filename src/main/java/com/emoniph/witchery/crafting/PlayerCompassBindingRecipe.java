package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.PlayerCompassItem;
import com.emoniph.witchery.item.TaglockKitItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PlayerCompassBindingRecipe extends CustomRecipe {
    public PlayerCompassBindingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(WitcheryItems.PLAYER_COMPASS.get());
        return PlayerCompassItem.bindFromTaglock(result, inputs.taglock()) ? result : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<PlayerCompassBindingRecipe> getSerializer() {
        return WitcheryRecipeSerializers.PLAYER_COMPASS_BINDING.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack compass = ItemStack.EMPTY;
        ItemStack taglock = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.COMPASS)) {
                if (!compass.isEmpty()) {
                    return null;
                }
                compass = stack;
                continue;
            }
            if (stack.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isPlayerTaglock(stack)) {
                if (!taglock.isEmpty()) {
                    return null;
                }
                taglock = stack;
                continue;
            }
            return null;
        }

        return !compass.isEmpty() && !taglock.isEmpty() ? new Inputs(taglock) : null;
    }

    private record Inputs(ItemStack taglock) {
    }
}
