package com.emoniph.witchery.brewing;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursedBlockWarningDataTest {

    @Test
    void warningStoresMessageAndMinimumCooldown() {
        CursedBlockWarningData data = new CursedBlockWarningData();
        BlockPos pos = new BlockPos(1, 2, 3);

        data.warn(pos, "You feel uneasy.", 1);

        CursedBlockWarningData.Warning warning = data.get(pos);
        assertNotNull(warning);
        assertEquals("You feel uneasy.", warning.message());
        assertEquals(20, warning.cooldownTicks());
        assertFalse(data.isEmpty());
    }

    @Test
    void warningCanBeRemoved() {
        CursedBlockWarningData data = new CursedBlockWarningData();
        BlockPos pos = new BlockPos(4, 5, 6);

        data.warn(pos, "The stones are listening.", 100);
        data.remove(pos);

        assertNull(data.get(pos));
        assertTrue(data.isEmpty());
    }
}
