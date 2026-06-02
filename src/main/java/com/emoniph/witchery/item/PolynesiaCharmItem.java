package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class PolynesiaCharmItem extends Item {
    private static final String STOCK_INVENTORY_TAG = "WitcheryShopStock";

    private final boolean charmsDemons;

    public PolynesiaCharmItem(Properties properties) {
        this(properties, false);
    }

    public PolynesiaCharmItem(Properties properties, boolean charmsDemons) {
        super(properties.durability(50).rarity(Rarity.UNCOMMON));
        this.charmsDemons = charmsDemons;
    }

    public boolean charmsDemons() {
        return charmsDemons;
    }

    public static boolean hasStockInventory(LivingEntity entity) {
        return entity.getPersistentData().contains(STOCK_INVENTORY_TAG);
    }

    public static void setEmptyStockInventory(Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            entity.getPersistentData().put(STOCK_INVENTORY_TAG, new CompoundTag());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String tooltipText = Component.translatable(getDescriptionId() + ".tip").getString();
        for (String line : tooltipText.split("\\n")) {
            if (!line.isEmpty()) {
                tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
