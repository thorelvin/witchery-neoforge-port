package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class VampireClothingUtil {
    public static final int DRINK_BOOST_LIGHT_PIECES = 2;
    public static final int BASIC_FLAME_LIGHT_PIECES = 3;
    public static final int BASIC_FLAME_MEDIUM_PIECES = 2;
    public static final int MESMERISE_BOOST_LIGHT_PIECES = 3;
    public static final int EXTENDED_FLAME_LIGHT_PIECES = 4;

    private VampireClothingUtil() {
    }

    public static int lightPiecesWorn(LivingEntity entity) {
        int pieces = 0;
        if (isLightHead(entity.getItemBySlot(EquipmentSlot.HEAD))) pieces++;
        if (isLightChest(entity.getItemBySlot(EquipmentSlot.CHEST))) pieces++;
        if (isLightLegs(entity.getItemBySlot(EquipmentSlot.LEGS))) pieces++;
        if (isLightFeet(entity.getItemBySlot(EquipmentSlot.FEET))) pieces++;
        return pieces;
    }

    public static int mediumPiecesWorn(LivingEntity entity) {
        int pieces = 0;
        if (isMediumHead(entity.getItemBySlot(EquipmentSlot.HEAD))) pieces++;
        if (isMediumChest(entity.getItemBySlot(EquipmentSlot.CHEST))) pieces++;
        return pieces;
    }

    public static boolean isDrinkBoostActive(LivingEntity entity) {
        return lightPiecesWorn(entity) >= DRINK_BOOST_LIGHT_PIECES;
    }

    public static boolean isBasicFlameProtectionActive(LivingEntity entity) {
        return lightPiecesWorn(entity) >= BASIC_FLAME_LIGHT_PIECES
                || mediumPiecesWorn(entity) >= BASIC_FLAME_MEDIUM_PIECES;
    }

    public static boolean isExtendedFlameProtectionActive(LivingEntity entity) {
        return lightPiecesWorn(entity) >= EXTENDED_FLAME_LIGHT_PIECES;
    }

    public static boolean isMesmeriseBoostActive(LivingEntity entity) {
        return lightPiecesWorn(entity) >= MESMERISE_BOOST_LIGHT_PIECES;
    }

    private static boolean isLightHead(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_TOP_HAT.get());
    }

    private static boolean isLightChest(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_DRESS_COAT.get())
                || stack.is(WitcheryItems.VAMPIRE_DRESS_JACKET.get());
    }

    private static boolean isLightLegs(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_TROUSERS.get())
                || stack.is(WitcheryItems.VAMPIRE_SKIRTED_TROUSERS.get());
    }

    private static boolean isLightFeet(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_OXFORD_BOOTS.get());
    }

    private static boolean isMediumHead(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_HELMET.get());
    }

    private static boolean isMediumChest(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_CHAIN_COAT.get())
                || stack.is(WitcheryItems.VAMPIRE_CHAIN_COAT_LADIES.get());
    }
}
