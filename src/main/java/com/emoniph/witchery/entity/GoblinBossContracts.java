package com.emoniph.witchery.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

final class GoblinBossContracts {
    private GoblinBossContracts() {
    }

    static double damageScale(double partnerDistanceSqr) {
        if (partnerDistanceSqr <= 9.0D) return 0.0D;
        if (partnerDistanceSqr <= 36.0D) return 0.2D;
        if (partnerDistanceSqr <= 81.0D) return 0.5D;
        if (partnerDistanceSqr <= 256.0D) return 0.8D;
        return 1.0D;
    }

    static float scaledAndCappedDamage(float amount, double partnerDistanceSqr) {
        return (float) Math.min(amount * damageScale(partnerDistanceSqr), 15.0D);
    }

    static int gulgDamageForce(double partnerDistanceSqr) {
        if (partnerDistanceSqr <= 9.0D) return 20;
        if (partnerDistanceSqr <= 36.0D) return 15;
        if (partnerDistanceSqr <= 81.0D) return 10;
        if (partnerDistanceSqr <= 256.0D) return 6;
        return 4;
    }

    static double gulgLaunch(double partnerDistanceSqr) {
        if (partnerDistanceSqr <= 9.0D) return 1.5D;
        if (partnerDistanceSqr <= 36.0D) return 1.3D;
        if (partnerDistanceSqr <= 81.0D) return 1.0D;
        if (partnerDistanceSqr <= 256.0D) return 0.7D;
        return 0.5D;
    }

    static double mogArrowSpeedFactor(boolean airborne) {
        return airborne ? 2.5D : 1.5D;
    }

    static double mogArrowDamage(float attackPower, double gaussian, int difficultyId) {
        return attackPower * 8.0D + gaussian * 0.25D + difficultyId * 0.11D;
    }

    static Item armorDrop(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 0 -> Items.CHAINMAIL_HELMET;
            case 1 -> Items.CHAINMAIL_CHESTPLATE;
            case 2 -> Items.CHAINMAIL_LEGGINGS;
            default -> Items.CHAINMAIL_BOOTS;
        };
    }

    static boolean isGoblinKin(LivingEntity entity) {
        return entity instanceof HobgoblinEntity
                || entity instanceof GoblinMogEntity
                || entity instanceof GoblinGulgEntity;
    }
}
