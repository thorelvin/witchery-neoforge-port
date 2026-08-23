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

@SuppressWarnings({"deprecation", "removal"})
public class BarkBeltItem extends Item {
    private static final String TAG_BARK_PIECES = "witchery_bark_pieces";
    private static final String SOURCE_TAG_BARK_PIECES = "WITCBarkPieces";
    private static final String TAG_BARK_CAPACITY = "witchery_bark_capacity";
    private static final int CHARGE_PER_WITCHERY_CLOTHING_PIECE = 2;

    public BarkBeltItem(Properties properties) {
        super(properties);
    }

    public static boolean isWorn(Player player) {
        return player.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.BARK_BELT.get());
    }

    public static int getChargeLevel(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getIntOr(TAG_BARK_PIECES, tag.getIntOr(SOURCE_TAG_BARK_PIECES, 0)));
    }

    public static void setChargeLevel(ItemStack stack, int level) {
        setChargeLevel(stack, level, getKnownMaxChargeLevel(stack));
    }

    public static void setChargeLevel(ItemStack stack, int level, int maxLevel) {
        int capacity = Math.max(CHARGE_PER_WITCHERY_CLOTHING_PIECE, maxLevel);
        int charge = Math.min(Math.max(0, level), capacity);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_BARK_PIECES, charge);
            tag.putInt(SOURCE_TAG_BARK_PIECES, charge);
            tag.putInt(TAG_BARK_CAPACITY, capacity);
        });
    }

    public static void rememberMaxChargeLevel(ItemStack stack, int maxLevel) {
        int capacity = Math.max(CHARGE_PER_WITCHERY_CLOTHING_PIECE, maxLevel);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_BARK_CAPACITY, capacity));
    }

    public static int getKnownMaxChargeLevel(ItemStack stack) {
        return Math.max(CHARGE_PER_WITCHERY_CLOTHING_PIECE, stack
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getIntOr(TAG_BARK_CAPACITY, CHARGE_PER_WITCHERY_CLOTHING_PIECE));
    }

    public static int getMaxChargeLevel(LivingEntity entity) {
        return getMaxChargeLevel(
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS),
                entity.getItemBySlot(EquipmentSlot.FEET));
    }

    public static int getMaxChargeLevel(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        int pieces = (supportsBarkGrowth(head) ? 1 : 0)
                + (supportsBarkGrowth(chest) ? 1 : 0)
                + (supportsBarkGrowth(legs) ? 1 : 0)
                + (supportsBarkGrowth(feet) ? 1 : 0);
        return Math.max(CHARGE_PER_WITCHERY_CLOTHING_PIECE, pieces * CHARGE_PER_WITCHERY_CLOTHING_PIECE);
    }

    public static boolean supportsBarkGrowth(ItemStack stack) {
        return stack.is(WitcheryItems.WITCHES_HAT.get())
                || WitcheryItems.isWitchesRobe(stack)
                || stack.is(WitcheryItems.BABAS_HAT.get())
                || stack.is(WitcheryItems.NECROMANCER_ROBES.get())
                || stack.is(WitcheryItems.BITING_BELT.get())
                || stack.is(WitcheryItems.BARK_BELT.get())
                || WitcheryItems.isIcySlippers(stack)
                || stack.is(WitcheryItems.RUBY_SLIPPERS.get())
                || stack.is(WitcheryItems.SEEPING_SHOES.get());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getChargeLevel(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = getKnownMaxChargeLevel(stack);
        return Math.round(13.0F * Math.min(getChargeLevel(stack), max) / (float) max);
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
                getChargeLevel(stack), getKnownMaxChargeLevel(stack)).withStyle(ChatFormatting.GRAY));
    }
}
