package com.emoniph.witchery.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Holds multiple Rowan Door Key UUIDs. Opens any matching door in its list. */
public class RowanKeyringItem extends Item {

    public RowanKeyringItem(Properties props) { super(props); }

    /** Add a key's UUID to the ring. No-op if already present. */
    public static void addKey(ItemStack stack, UUID uuid) {
        if (hasKey(stack, uuid)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag list = tag.getList("Keys").map(l -> l).orElse(new ListTag());
            CompoundTag entry = new CompoundTag();
            entry.putLong("M", uuid.getMostSignificantBits());
            entry.putLong("L", uuid.getLeastSignificantBits());
            list.add(entry);
            tag.put("Keys", list);
        });
    }

    public static boolean hasKey(ItemStack stack, UUID uuid) {
        return getUUIDs(stack).contains(uuid);
    }

    public static List<UUID> getUUIDs(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<UUID> result = new ArrayList<>();
        ListTag keyList = tag.getListOrEmpty("Keys");
        if (keyList.isEmpty()) return result;
        for (Tag t : keyList) {
            CompoundTag entry = (CompoundTag) t;
            result.add(new UUID(entry.getLongOr("M", 0L), entry.getLongOr("L", 0L)));
        }
        return result;
    }

    public static int getKeyCount(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getListOrEmpty("Keys").size();
    }

    /** Build a keyring from two key items. */
    public static ItemStack combine(ItemStack keyA, ItemStack keyB, Item ringItem) {
        ItemStack ring = new ItemStack(ringItem);
        RowanDoorKeyItem.getKeyUUID(keyA).ifPresent(id -> addKey(ring, id));
        RowanDoorKeyItem.getKeyUUID(keyB).ifPresent(id -> addKey(ring, id));
        return ring;
    }

    /** Merge a key into an existing keyring. */
    public static ItemStack mergeInto(ItemStack ring, ItemStack key) {
        ItemStack result = ring.copy();
        RowanDoorKeyItem.getKeyUUID(key).ifPresent(id -> addKey(result, id));
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tip, TooltipFlag flag) {
        tip.accept(Component.translatable("witchery.rowan_keyring.tooltip.count", getKeyCount(stack)));
    }
}
