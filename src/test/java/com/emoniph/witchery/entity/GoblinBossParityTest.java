package com.emoniph.witchery.entity;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoblinBossParityTest {
    @Test
    void partnerDamageUsesSourceSquaredDistanceThresholdsAndCap() {
        assertEquals(0.0D, GoblinBossContracts.damageScale(9.0D));
        assertEquals(0.2D, GoblinBossContracts.damageScale(9.0001D));
        assertEquals(0.2D, GoblinBossContracts.damageScale(36.0D));
        assertEquals(0.5D, GoblinBossContracts.damageScale(36.0001D));
        assertEquals(0.5D, GoblinBossContracts.damageScale(81.0D));
        assertEquals(0.8D, GoblinBossContracts.damageScale(81.0001D));
        assertEquals(0.8D, GoblinBossContracts.damageScale(256.0D));
        assertEquals(1.0D, GoblinBossContracts.damageScale(256.0001D));
        assertEquals(15.0F, GoblinBossContracts.scaledAndCappedDamage(100.0F, 300.0D));
    }

    @Test
    void gulgForceAndLaunchMatchEverySourceBand() {
        assertBand(9.0D, 20, 1.5D);
        assertBand(9.0001D, 15, 1.3D);
        assertBand(36.0D, 15, 1.3D);
        assertBand(36.0001D, 10, 1.0D);
        assertBand(81.0D, 10, 1.0D);
        assertBand(81.0001D, 6, 0.7D);
        assertBand(256.0D, 6, 0.7D);
        assertBand(256.0001D, 4, 0.5D);
    }

    @Test
    void mogArrowContractUsesAirborneSpeedAndRangedDamageFormula() {
        assertEquals(1.5D, GoblinBossContracts.mogArrowSpeedFactor(false));
        assertEquals(2.5D, GoblinBossContracts.mogArrowSpeedFactor(true));
        assertEquals(8.0D, GoblinBossContracts.mogArrowDamage(1.0F, 0.0D, 0), 0.0001D);
        assertEquals(8.72D, GoblinBossContracts.mogArrowDamage(1.0F, 2.0D, 2), 0.0001D);
    }

    @Test
    void bossArmorDropsUseTheSourceChainmailSet() {
        assertEquals(Items.CHAINMAIL_HELMET, GoblinBossContracts.armorDrop(0));
        assertEquals(Items.CHAINMAIL_CHESTPLATE, GoblinBossContracts.armorDrop(1));
        assertEquals(Items.CHAINMAIL_LEGGINGS, GoblinBossContracts.armorDrop(2));
        assertEquals(Items.CHAINMAIL_BOOTS, GoblinBossContracts.armorDrop(3));
    }

    private static void assertBand(double distanceSqr, int force, double launch) {
        assertEquals(force, GoblinBossContracts.gulgDamageForce(distanceSqr));
        assertEquals(launch, GoblinBossContracts.gulgLaunch(distanceSqr), 0.0001D);
    }
}
