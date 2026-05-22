package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

public class BarkBeltItem extends Item {
    private static final String TAG_BARK_PIECES = "witchery_bark_pieces";
    private static final int CHARGE_PER_WITCHERY_CLOTHING_PIECE = 2;

    public BarkBeltItem(Properties properties) {
        super(properties);
    }

    public static boolean isWorn(Player player) {
        return player.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.BARK_BELT.get());
    }

    public static int getChargeLevel(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getIntOr(TAG_BARK_PIECES, 0);
    }

    public static void setChargeLevel(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_BARK_PIECES, Math.max(0, level)));
    }

    public static int getMaxChargeLevel(LivingEntity entity) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isWitcheryClothing(entity.getItemBySlot(slot))) {
                pieces++;
            }
        }
        return Math.max(CHARGE_PER_WITCHERY_CLOTHING_PIECE, pieces * CHARGE_PER_WITCHERY_CLOTHING_PIECE);
    }

    private static boolean isWitcheryClothing(ItemStack stack) {
        return stack.is(WitcheryItems.WITCHES_HAT.get())
                || stack.is(WitcheryItems.WITCHES_ROBE.get())
                || stack.is(WitcheryItems.WITCHES_SHOES.get())
                || stack.is(WitcheryItems.BITING_BELT.get())
                || stack.is(WitcheryItems.BARK_BELT.get())
                || stack.is(WitcheryItems.EARMUFFS.get());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getChargeLevel(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Math.min(getChargeLevel(stack), CHARGE_PER_WITCHERY_CLOTHING_PIECE)
                / (float) CHARGE_PER_WITCHERY_CLOTHING_PIECE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x8F6B3A;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.bark_belt.tip",
                getChargeLevel(stack), CHARGE_PER_WITCHERY_CLOTHING_PIECE).withStyle(ChatFormatting.GRAY));
    }
}
