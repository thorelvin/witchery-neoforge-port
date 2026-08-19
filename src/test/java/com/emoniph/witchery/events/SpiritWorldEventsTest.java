package com.emoniph.witchery.events;

import com.emoniph.witchery.data.WitcheryPlayerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritWorldEventsTest {
    @Test
    void atmosphereUsesBrightDayForDreamsAndMidnightForNightmares() {
        assertEquals(6_000L, SpiritWorldEvents.targetTimeOfDay(false));
        assertEquals(18_000L, SpiritWorldEvents.targetTimeOfDay(true));
    }

    @Test
    void physicalHearthTravelersDoNotCreateNightmareAtmosphere() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setSpiritWorldIsDream(false);
        assertFalse(SpiritWorldEvents.contributesNightmareAtmosphere(data));

        data.setInSpiritWorld(true);
        assertTrue(SpiritWorldEvents.contributesNightmareAtmosphere(data));

        data.setSpiritWorldIsDream(true);
        assertFalse(SpiritWorldEvents.contributesNightmareAtmosphere(data));
    }

    @Test
    void sourceBansPearlsAndBlazePowderFromSpiritHarvests() {
        assertTrue(SpiritWorldEvents.isBannedSpiritDrop(new ItemStack(Items.ENDER_PEARL)));
        assertTrue(SpiritWorldEvents.isBannedSpiritDrop(new ItemStack(Items.BLAZE_POWDER)));
        assertFalse(SpiritWorldEvents.isBannedSpiritDrop(new ItemStack(Items.BLAZE_ROD)));
    }

    @Test
    void sourceRejectsEndermenAndThirdPartyLivingEntities() {
        assertTrue(SpiritWorldEvents.isSourceDisallowedLiving("net.minecraft.world.entity.monster.EnderMan", true));
        assertTrue(SpiritWorldEvents.isSourceDisallowedLiving("example.mod.entity.DreamMob", false));
        assertTrue(SpiritWorldEvents.isSourceDisallowedLiving(null, false));
        assertFalse(SpiritWorldEvents.isSourceDisallowedLiving("net.minecraft.world.entity.animal.Cow", false));
        assertFalse(SpiritWorldEvents.isSourceDisallowedLiving("com.emoniph.witchery.entity.EntitySpirit", false));
    }
}
