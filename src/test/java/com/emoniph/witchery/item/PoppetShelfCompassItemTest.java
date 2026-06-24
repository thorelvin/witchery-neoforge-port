package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoppetShelfCompassItemTest {
    @Test
    void signalBandsMatchOriginalThresholds() {
        assertEquals(5, PoppetShelfCompassItem.signalBand(63.99));
        assertEquals(4, PoppetShelfCompassItem.signalBand(64.0));
        assertEquals(3, PoppetShelfCompassItem.signalBand(256.0));
        assertEquals(2, PoppetShelfCompassItem.signalBand(1024.0));
        assertEquals(1, PoppetShelfCompassItem.signalBand(4096.0));
        assertEquals(0, PoppetShelfCompassItem.signalBand(16384.0));
    }

    @Test
    void distanceUsesSourceBlockCoordinateAnchor() {
        assertEquals(64.0, PoppetShelfCompassItem.shelfDistanceSq(
                new Vec3(8.0, 70.0, 0.0), new BlockPos(0, 64, 0)));
        assertEquals(4, PoppetShelfCompassItem.signalBand(PoppetShelfCompassItem.shelfDistanceSq(
                new Vec3(8.0, 70.0, 0.0), new BlockPos(0, 64, 0))));
    }
}
