package com.emoniph.witchery.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Shared serializer for the source-style Inventory/Items payload used by both brew containers. */
public final class StoredBrewInventory {
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";

    private StoredBrewInventory() {
    }

    public static ItemStack get(ItemStack carrier, int slot) {
        if (carrier.isEmpty() || slot < 0) {
            return ItemStack.EMPTY;
        }
        CompoundTag root = carrier.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag items = root.getCompoundOrEmpty(TAG_INVENTORY).getListOrEmpty(TAG_ITEMS);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompoundOrEmpty(i);
            if ((entry.getByteOr(TAG_SLOT, (byte) 0) & 255) == slot) {
                return entry.read(TAG_STACK, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    public static void set(ItemStack carrier, int slot, ItemStack stored) {
        if (carrier.isEmpty() || slot < 0) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, carrier, root -> {
            CompoundTag inventory = root.getCompoundOrEmpty(TAG_INVENTORY);
            ListTag existing = inventory.getListOrEmpty(TAG_ITEMS);
            ListTag updated = new ListTag();
            for (int i = 0; i < existing.size(); i++) {
                CompoundTag entry = existing.getCompoundOrEmpty(i);
                if ((entry.getByteOr(TAG_SLOT, (byte) 0) & 255) != slot) {
                    updated.add(entry.copy());
                }
            }
            if (!stored.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte(TAG_SLOT, (byte) slot);
                entry.store(TAG_STACK, ItemStack.CODEC, stored.copy());
                updated.add(entry);
            }
            inventory.put(TAG_ITEMS, updated);
            root.put(TAG_INVENTORY, inventory);
        });
    }
}
