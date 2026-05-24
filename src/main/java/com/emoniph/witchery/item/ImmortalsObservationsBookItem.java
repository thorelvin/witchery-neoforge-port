package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

public class ImmortalsObservationsBookItem extends Item {
    public static final String TAG_LEVEL = "Level";
    public static final int MAX_LEVEL = 9;
    public static final int BASE_VISIBLE_PAGES = 6;
    public static final int TOTAL_PAGES = 16;

    public ImmortalsObservationsBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.immortals_observations_book.tip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.immortals_observations_book.pages",
                getVisiblePages(stack), TOTAL_PAGES).withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static boolean canAddTornPage(ItemStack stack) {
        return stack.is(WitcheryItems.IMMORTALS_OBSERVATIONS_BOOK.get()) && getResearchLevel(stack) < MAX_LEVEL;
    }

    public static boolean addTornPage(ItemStack stack) {
        if (!canAddTornPage(stack)) {
            return false;
        }
        int nextLevel = Math.min(MAX_LEVEL, getResearchLevel(stack) + 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_LEVEL, nextLevel));
        return true;
    }

    public static int getResearchLevel(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(MAX_LEVEL, tag.getIntOr(TAG_LEVEL, 0)));
    }

    public static int getVisiblePages(ItemStack stack) {
        int level = getResearchLevel(stack);
        int bonusPage = level >= 8 ? 1 : 0;
        return Math.min(TOTAL_PAGES, BASE_VISIBLE_PAGES + level + bonusPage);
    }
}
