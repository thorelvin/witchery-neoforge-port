package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.RowanDoorKeyItem;
import com.emoniph.witchery.item.RowanKeyringItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapeless crafting recipe:
 *   2+ Rowan Door Keys → Rowan Keyring (merges all UUIDs)
 *   Rowan Keyring + 1+ Keys → Rowan Keyring (adds UUIDs to ring)
 */
public class RowanKeyringRecipe extends CustomRecipe {

    public RowanKeyringRecipe(CraftingBookCategory cat) { super(cat); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int keys = 0, rings = 0, other = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(WitcheryItems.ROWAN_DOOR_KEY.get()))  { keys++;  continue; }
            if (s.is(WitcheryItems.ROWAN_KEYRING.get()))   { rings++; continue; }
            other++;
        }
        if (other > 0 || rings > 1) return false;
        return (rings == 0 && keys >= 2) || (rings == 1 && keys >= 1);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider reg) {
        List<ItemStack> keys  = new ArrayList<>();
        ItemStack existingRing = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(WitcheryItems.ROWAN_DOOR_KEY.get()))  keys.add(s);
            if (s.is(WitcheryItems.ROWAN_KEYRING.get()))   existingRing = s;
        }

        ItemStack result = existingRing.isEmpty()
                ? new ItemStack(WitcheryItems.ROWAN_KEYRING.get())
                : existingRing.copy();

        // Copy existing keys from ring
        if (existingRing.isEmpty()) {
            // Freshly created ring — no existing keys
        } else {
            // result already has them (it's a copy)
        }

        // Add new keys
        for (ItemStack key : keys) {
            RowanDoorKeyItem.getKeyUUID(key).ifPresent(id -> RowanKeyringItem.addKey(result, id));
        }
        return result;
    }

        @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<RowanKeyringRecipe> getSerializer() {
        return WitcheryRecipeSerializers.ROWAN_KEYRING.get();
    }
}
