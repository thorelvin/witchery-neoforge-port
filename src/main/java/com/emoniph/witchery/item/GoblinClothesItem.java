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

public class GoblinClothesItem extends Item {
    private final Piece piece;

    public GoblinClothesItem(Piece piece, Properties properties) {
        super(properties);
        this.piece = piece;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        for (String line : Component.translatable(this.getDescriptionId() + ".tip").getString().split("\n")) {
            if (!line.isEmpty()) {
                tooltip.accept(Component.literal(line).withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public Piece piece() {
        return piece;
    }

    public static boolean isQuiverWorn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.MOGS_QUIVER.get());
    }

    public static boolean isGirdleWorn(LivingEntity entity) {
        return WitcheryItems.isGulgsGirdle(entity.getItemBySlot(EquipmentSlot.LEGS));
    }

    public enum Piece {
        MOGS_QUIVER,
        GULGS_GIRDLE
    }
}
