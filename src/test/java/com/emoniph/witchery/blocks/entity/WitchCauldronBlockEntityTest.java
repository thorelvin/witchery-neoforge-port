package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.item.TaglockKitItem;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchCauldronBlockEntityTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void filledTaglockPayloadPersistsAndLocksTheFinalIngredient() {
        UUID targetId = UUID.randomUUID();
        ItemStack target = TaglockKitItem.filledPlayerTaglock(targetId, "Target");
        WitchCauldronBlockEntity original = cauldron();

        assertTrue(original.tryAddIngredient(Items.NETHER_WART));
        assertTrue(original.tryAddIngredient(Items.SPIDER_EYE));
        assertTrue(original.tryAddIngredient(target));
        assertFalse(original.tryAddIngredient(Items.GUNPOWDER),
                "source refuses every ingredient after the ritual trigger");
        assertEquals(targetId, TaglockKitItem.getUUID(original.getRitualTarget()).orElseThrow());

        WitchCauldronBlockEntity restored = cauldron();
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,
                RegistryAccess.EMPTY,
                original.getUpdateTag(RegistryAccess.EMPTY)));

        assertEquals(original.getIngredients(), restored.getIngredients());
        assertEquals(targetId, TaglockKitItem.getUUID(restored.getRitualTarget()).orElseThrow());

        restored.clear();
        assertTrue(restored.getRitualTarget().isEmpty());
        assertTrue(restored.getIngredients().isEmpty());
    }

    private static WitchCauldronBlockEntity cauldron() {
        return new WitchCauldronBlockEntity(BlockPos.ZERO,
                WitcheryBlocks.WITCH_CAULDRON.get().defaultBlockState());
    }
}
