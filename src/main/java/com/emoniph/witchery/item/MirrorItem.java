package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** A source mirror item retains its assigned room and defeated-Reflection state. */
public class MirrorItem extends DoubleHighBlockItem {
    public MirrorItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CompoundTag tag = blockEntityData(stack);
        if (hasRoom(tag)) {
            String key = tag.getBooleanOr("DemonKilled", false)
                    ? "item.witchery.mirror.bridge" : "item.witchery.mirror.inhabited";
            tooltip.accept(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    public static CompoundTag blockEntityData(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTagWithoutId();
    }

    public static boolean hasRoom(CompoundTag tag) {
        return tag.contains("RoomCX") && tag.contains("RoomY") && tag.contains("RoomCZ");
    }

    public static int roomIndex(ItemStack stack) {
        CompoundTag tag = blockEntityData(stack);
        if (!hasRoom(tag)) return -1;
        return com.emoniph.witchery.world.MirrorWorldLayout.indexForRoom(
                tag.getIntOr("RoomCX", Integer.MIN_VALUE),
                tag.getIntOr("RoomY", Integer.MIN_VALUE),
                tag.getIntOr("RoomCZ", Integer.MIN_VALUE));
    }
}
