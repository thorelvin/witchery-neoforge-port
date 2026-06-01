package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.BiomeBookItem;
import com.emoniph.witchery.item.BiomeNoteItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeNoteCopyRecipe extends CustomRecipe {
    public BiomeNoteCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findBookSlot(input) >= 0 && findPaperSlot(input) >= 0 && countNonEmpty(input) == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int bookSlot = findBookSlot(input);
        if (bookSlot < 0 || findPaperSlot(input) < 0 || countNonEmpty(input) != 2) {
            return ItemStack.EMPTY;
        }
        ResourceKey<Biome> biome = BiomeBookItem.getSelectedBiomeKey(input.getItem(bookSlot));
        return BiomeNoteItem.create(biome);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int bookSlot = findBookSlot(input);
        if (bookSlot >= 0) {
            ItemStack copy = input.getItem(bookSlot).copy();
            copy.setCount(1);
            remaining.set(bookSlot, copy);
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<BiomeNoteCopyRecipe> getSerializer() {
        return WitcheryRecipeSerializers.BIOME_NOTE_COPY.get();
    }

    private static int findBookSlot(CraftingInput input) {
        int slot = -1;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(WitcheryItems.EXTENDED_BIOMES_BOOK.get()) || stack.is(WitcheryItems.BIOMES_BOOK.get())) {
                if (slot >= 0) {
                    return -1;
                }
                slot = i;
            }
        }
        return slot;
    }

    private static int findPaperSlot(CraftingInput input) {
        int slot = -1;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(Items.PAPER)) {
                if (slot >= 0) {
                    return -1;
                }
                slot = i;
            }
        }
        return slot;
    }

    private static int countNonEmpty(CraftingInput input) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
