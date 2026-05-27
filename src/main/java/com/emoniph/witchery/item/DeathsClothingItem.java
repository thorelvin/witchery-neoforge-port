package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public class DeathsClothingItem extends Item {
    private final Piece piece;

    public DeathsClothingItem(Piece piece, Properties properties) {
        super(properties);
        this.piece = piece;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable(piece.tooltipKey()).withStyle(ChatFormatting.BLUE));
    }

    public static boolean isFullSetWorn(LivingEntity entity) {
        int pieces = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.DEATHS_HOOD.get())) pieces++;
        if (entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.DEATHS_ROBE.get())) pieces++;
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.DEATHS_SHOES.get())) pieces++;
        return pieces >= 3;
    }

    public enum Piece {
        HOOD("item.witchery.deaths_hood.tip"),
        ROBE("item.witchery.deaths_robe.tip"),
        SHOES("item.witchery.deaths_shoes.tip");

        private final String tooltipKey;

        Piece(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }

        private String tooltipKey() {
            return tooltipKey;
        }
    }
}
