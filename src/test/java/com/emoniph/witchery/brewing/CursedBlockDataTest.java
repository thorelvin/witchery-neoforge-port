package com.emoniph.witchery.brewing;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the triggered-dispersal curse accounting ({@link CursedBlockData}) — source
 * {@code TileEntityCursedBlock} {@code initalise}/{@code updateCurse}/{@code applyToEntityAndDestroy}
 * charge rules — without a world. Needs the MC bootstrap only so {@code Items.*} resolve for the brews.
 */
class CursedBlockDataTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static BrewContents brew(net.minecraft.world.item.Item... items) {
        return new BrewContents(List.of(items));
    }

    @Test
    void freshCurseStartsAtOneCharge() {
        CursedBlockData data = new CursedBlockData();
        BlockPos pos = new BlockPos(1, 2, 3);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        assertNotNull(data.get(pos));
        assertEquals(1, data.get(pos).charges());
    }

    @Test
    void recursingTheSameBrewStacksCharges() {
        CursedBlockData data = new CursedBlockData();
        BlockPos pos = new BlockPos(0, 0, 0);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        assertEquals(2, data.get(pos).charges());
    }

    @Test
    void recursingADifferentBrewResetsCharges() {
        CursedBlockData data = new CursedBlockData();
        BlockPos pos = new BlockPos(0, 0, 0);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        data.curse(pos, brew(Items.GLOWSTONE_DUST), null);
        assertEquals(1, data.get(pos).charges());
    }

    @Test
    void takeChargeDecrementsThenRemoves() {
        CursedBlockData data = new CursedBlockData();
        BlockPos pos = new BlockPos(5, 6, 7);
        data.curse(pos, brew(Items.SPIDER_EYE), null);
        data.curse(pos, brew(Items.SPIDER_EYE), null); // charges = 2

        assertNotNull(data.takeCharge(pos));            // -> 1, still cursed
        assertEquals(1, data.get(pos).charges());
        assertNotNull(data.takeCharge(pos));            // -> 0, record removed
        assertNull(data.get(pos));
        assertNull(data.takeCharge(pos));               // nothing left
    }
}
