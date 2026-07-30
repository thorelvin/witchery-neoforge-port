package com.emoniph.witchery.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class DemonSourceParityTest {
    @Test
    void onlySourceInfernalCurrenciesTriggerEscapeExplosion() {
        assertTrue(DemonEntity.isSourceVolatileCurrency(new ItemStack(Items.BLAZE_ROD)));
        assertTrue(DemonEntity.isSourceVolatileCurrency(new ItemStack(Items.MAGMA_CREAM)));
        assertFalse(DemonEntity.isSourceVolatileCurrency(new ItemStack(Items.BLAZE_POWDER)));
        assertFalse(DemonEntity.isSourceVolatileCurrency(new ItemStack(Items.EMERALD)));
    }

    @Test
    void sourceCurrencyThresholdsUseBlazeRodRatherThanBlazePowder() {
        assertSame(Items.BLAZE_ROD, DemonEntity.sourceCurrency(0.0D));
        assertSame(Items.MAGMA_CREAM, DemonEntity.sourceCurrency(0.2D));
        assertSame(Items.DIAMOND, DemonEntity.sourceCurrency(0.4D));
        assertSame(Items.EMERALD, DemonEntity.sourceCurrency(0.5D));
        assertSame(Items.GOLD_INGOT, DemonEntity.sourceCurrency(0.75D));
    }
}
