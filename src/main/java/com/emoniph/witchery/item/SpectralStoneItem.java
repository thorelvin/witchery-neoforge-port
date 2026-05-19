package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

public class SpectralStoneItem extends Item {

    private static final String TAG_TYPE = "SpectralType";
    private static final String TAG_COUNT = "SpectralCount";

    public enum StoredType {
        NONE(""),
        SPECTRE("entity.witchery.spectre"),
        BANSHEE("entity.witchery.banshee"),
        POLTERGEIST("entity.witchery.poltergeist");

        private final String translationKey;

        StoredType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public SpectralStoneItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        StoredType type = getStoredType(stack);
        int count = getStoredCount(stack);
        if (type == StoredType.NONE || count <= 0) {
            tooltip.accept(Component.translatable("witchery.spectral_stone.tooltip.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        tooltip.accept(Component.translatable(type.translationKey())
                .append(Component.literal(": " + count))
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean isBound(ItemStack stack) {
        return getStoredType(stack) != StoredType.NONE && getStoredCount(stack) > 0;
    }

    public static void clearBinding(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_TYPE);
            tag.remove(TAG_COUNT);
        });
    }

    public static void bind(ItemStack stack, StoredType type, int count) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (type == StoredType.NONE || count <= 0) {
                tag.remove(TAG_TYPE);
                tag.remove(TAG_COUNT);
            } else {
                tag.putString(TAG_TYPE, type.name());
                tag.putInt(TAG_COUNT, Math.min(count, 3));
            }
        });
    }

    public static StoredType getStoredType(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_TYPE)) {
            return StoredType.NONE;
        }

        try {
            return StoredType.valueOf(tag.getStringOr(TAG_TYPE, StoredType.NONE.name()));
        } catch (IllegalArgumentException ignored) {
            return StoredType.NONE;
        }
    }

    public static int getStoredCount(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_COUNT)) {
            return 0;
        }
        return Math.max(0, Math.min(3, tag.getIntOr(TAG_COUNT, 0)));
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
