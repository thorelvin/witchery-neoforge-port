package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatueHobgoblinPatronBlockEntityTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void ownerUuidAndPrayerStateSurviveSaveReload() {
        UUID ownerId = UUID.fromString("a4ea4124-9a18-4674-bb1c-d70f3fce070a");
        StatueHobgoblinPatronBlockEntity original = statue();
        original.setOwner("PatronOwner", ownerId);
        assertEquals(64, original.addPrayer(64));
        assertEquals(0, original.addPrayer(1));

        StatueHobgoblinPatronBlockEntity restored = statue();
        restored.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING, RegistryAccess.EMPTY,
                original.getUpdateTag(RegistryAccess.EMPTY)));

        assertTrue(restored.hasOwner());
        assertEquals("PatronOwner", restored.ownerName());
        assertEquals(ownerId, restored.ownerUuid());
        assertEquals(64, restored.storedPrayers());
        assertEquals(64, restored.lifetimePrayers());
    }

    private static StatueHobgoblinPatronBlockEntity statue() {
        return new StatueHobgoblinPatronBlockEntity(BlockPos.ZERO,
                WitcheryBlocks.STATUE_HOBGOBLIN_PATRON.get().defaultBlockState());
    }
}
