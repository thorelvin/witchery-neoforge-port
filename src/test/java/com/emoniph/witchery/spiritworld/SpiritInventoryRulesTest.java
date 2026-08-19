package com.emoniph.witchery.spiritworld;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritInventoryRulesTest {

    @Test
    void onlySourceItemsEnterWithTheDreamer() {
        assertTrue(SpiritWorldHelper.isAlwaysCarried(new ItemStack(WitcheryItems.ICY_NEEDLE.get())));
        assertTrue(SpiritWorldHelper.isAlwaysCarried(new ItemStack(WitcheryItems.MUTANDIS.get())));
        assertFalse(SpiritWorldHelper.isAlwaysCarried(new ItemStack(WitcheryItems.MUTANDIS_EXTREMIS.get())));
        assertFalse(SpiritWorldHelper.isAlwaysCarried(new ItemStack(WitcheryItems.WITCHES_PRIMER.get())));
        assertFalse(SpiritWorldHelper.isAlwaysCarried(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void onlySourceSpiritMaterialsReturnToTheWakingInventory() {
        assertTrue(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.WISPY_COTTON.get())));
        assertTrue(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.DISTURBED_COTTON.get())));
        assertTrue(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get())));
        assertTrue(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.SUBDUED_SPIRIT.get())));
        assertTrue(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.SUBDUED_VILLAGE_SPIRIT.get())));
        assertFalse(SpiritWorldHelper.isTransferable(new ItemStack(WitcheryItems.WITCHES_PRIMER.get())));
        assertFalse(SpiritWorldHelper.isTransferable(new ItemStack(Items.ENDER_PEARL)));
    }

    @Test
    void manifestationCarriesOnlyNeedlesOutAndAllowsBoundFetishesBack() {
        ItemStack boundFetish = new ItemStack(WitcheryItems.FETISH_SCARECROW.get());
        CustomData.update(DataComponents.CUSTOM_DATA, boundFetish,
                tag -> tag.putInt("FetishEffect", 1));

        assertTrue(SpiritWorldHelper.isManifestationOutwardItem(
                new ItemStack(WitcheryItems.ICY_NEEDLE.get())));
        assertFalse(SpiritWorldHelper.isManifestationOutwardItem(boundFetish));
        assertFalse(SpiritWorldHelper.isManifestationOutwardItem(
                new ItemStack(WitcheryItems.WITCHES_PRIMER.get())));

        assertTrue(SpiritWorldHelper.isManifestationReturnItem(
                new ItemStack(WitcheryItems.ICY_NEEDLE.get())));
        assertTrue(SpiritWorldHelper.isManifestationReturnItem(boundFetish));
        assertFalse(SpiritWorldHelper.isManifestationReturnItem(
                new ItemStack(WitcheryItems.FETISH_SCARECROW.get())));
        assertFalse(SpiritWorldHelper.isManifestationReturnItem(
                new ItemStack(WitcheryItems.WITCHES_PRIMER.get())));
    }
}
