package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.LeonardsUrnItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class LeonardsUrnUpgradeRecipe extends CustomRecipe {
    public LeonardsUrnUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return scan(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Scan scan = scan(input);
        if (!scan.valid()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = LeonardsUrnItem.createWithCapacity(scan.capacity());
        copyStoredBrews(input.getItem(scan.firstSlot()), result);
        copyStoredBrews(input.getItem(scan.secondSlot()), result);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<LeonardsUrnUpgradeRecipe> getSerializer() {
        return WitcheryRecipeSerializers.LEONARDS_URN_UPGRADE.get();
    }

    private static void copyStoredBrews(ItemStack from, ItemStack to) {
        int capacity = LeonardsUrnItem.getCapacity(from);
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stored = LeonardsUrnItem.getStoredBrew(from, slot);
            if (!stored.isEmpty()) {
                LeonardsUrnItem.tryInsertStoredBrew(to, stored);
            }
        }
    }

    private static Scan scan(CraftingInput input) {
        int first = -1;
        int second = -1;
        int capacity = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(WitcheryItems.LEONARDS_URN.get())) {
                return Scan.invalid();
            }
            if (first < 0) {
                first = i;
            } else if (second < 0) {
                second = i;
            } else {
                return Scan.invalid();
            }
            capacity += LeonardsUrnItem.getCapacity(stack);
        }
        if (first < 0 || second < 0 || capacity < 2 || capacity > 4) {
            return Scan.invalid();
        }
        return new Scan(first, second, capacity);
    }

    private record Scan(int firstSlot, int secondSlot, int capacity) {
        static Scan invalid() {
            return new Scan(-1, -1, 0);
        }

        boolean valid() {
            return firstSlot >= 0 && secondSlot >= 0 && capacity >= 2 && capacity <= 4;
        }
    }
}
