package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Resurrected/source-family combustion brew: a glowing stackable fuel item. */
public class CombustionBrewItem extends Item implements IItemExtension {
    private static final String TAG_LEVEL = "Level";
    private static final int[] BURN_TIMES = {2400, 5000, 10000, 50000};

    public CombustionBrewItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return BURN_TIMES[getLevel(itemStack)];
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        Component line = Component.translatable("item.witchery.combustion_brew.tip");
        int level = getLevel(stack);
        if (level > 0) {
            line = line.copy().append(" ").append(Component.translatable("enchantment.level." + (level + 1)));
        }
        tooltip.accept(line.copy().withStyle(ChatFormatting.GRAY));
    }

    public static int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(BURN_TIMES.length - 1, tag.getIntOr(TAG_LEVEL, 0)));
    }

    public static ItemStack create(int level) {
        ItemStack stack = new ItemStack(com.emoniph.witchery.init.WitcheryItems.COMBUSTION_BREW.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(TAG_LEVEL, Math.clamp(level, 0, BURN_TIMES.length - 1)));
        return stack;
    }
}
