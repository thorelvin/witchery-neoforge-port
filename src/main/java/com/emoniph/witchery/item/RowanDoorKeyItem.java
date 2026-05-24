package com.emoniph.witchery.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bound to a specific Rowan Wood Door by UUID. Right-click the matching door to unlock it. */
public class RowanDoorKeyItem extends Item {
    private static final String TAG_DOOR_UUID_MOST = "DoorUUIDMost";
    private static final String TAG_DOOR_UUID_LEAST = "DoorUUIDLeast";

    public RowanDoorKeyItem(Properties props) { super(props); }

    public static void bindKey(ItemStack stack, UUID uuid) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(TAG_DOOR_UUID_MOST,  uuid.getMostSignificantBits());
            tag.putLong(TAG_DOOR_UUID_LEAST, uuid.getLeastSignificantBits());
        });
    }

    public static Optional<UUID> getKeyUUID(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TAG_DOOR_UUID_MOST) || !tag.contains(TAG_DOOR_UUID_LEAST)) return Optional.empty();
        return Optional.of(new UUID(tag.getLongOr(TAG_DOOR_UUID_MOST, 0L), tag.getLongOr(TAG_DOOR_UUID_LEAST, 0L)));
    }

    public static boolean isBound(ItemStack stack) {
        return getKeyUUID(stack).isPresent();
    }

    public static boolean matchesDoor(ItemStack stack, UUID doorUUID) {
        return getKeyUUID(stack).map(doorUUID::equals).orElse(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tip, TooltipFlag flag) {
        getKeyUUID(stack).ifPresentOrElse(
            id -> tip.accept(Component.translatable("witchery.rowan_door_key.tooltip.bound",
                          id.toString().substring(0, 8))),
            ()  -> tip.accept(Component.translatable("witchery.rowan_door_key.tooltip.unbound")));
    }
}
