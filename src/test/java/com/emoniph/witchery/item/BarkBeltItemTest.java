package com.emoniph.witchery.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class BarkBeltItemTest {
    @Test
    void sourceWitchClothesContributeTwoChargesEachButEarmuffsDoNot() {
        ItemStack belt = new ItemStack(WitcheryItems.BARK_BELT.get());
        ItemStack hat = new ItemStack(WitcheryItems.WITCHES_HAT.get());
        ItemStack robes = new ItemStack(WitcheryItems.WITCHES_ROBES.get());
        ItemStack shoes = new ItemStack(WitcheryItems.RUBY_SLIPPERS.get());
        ItemStack earmuffs = new ItemStack(WitcheryItems.EARMUFFS.get());

        assertTrue(BarkBeltItem.supportsBarkGrowth(belt));
        assertTrue(BarkBeltItem.supportsBarkGrowth(hat));
        assertTrue(BarkBeltItem.supportsBarkGrowth(robes));
        assertTrue(BarkBeltItem.supportsBarkGrowth(shoes));
        assertFalse(BarkBeltItem.supportsBarkGrowth(earmuffs));
        assertEquals(2, BarkBeltItem.getMaxChargeLevel(ItemStack.EMPTY, ItemStack.EMPTY, belt, ItemStack.EMPTY));
        assertEquals(8, BarkBeltItem.getMaxChargeLevel(hat, robes, belt, shoes));
    }
}
