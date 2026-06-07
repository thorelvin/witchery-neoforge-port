package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the altar power-source classification ({@code AltarBlockEntity.classify}) against the
 * Appendix-A values: each source block contributes its documented per-block power and group cap, and
 * non-sources contribute nothing. Pure block-identity logic, so it runs headlessly via ModDevGradle
 * {@code unitTest} (registries bootstrapped, no client/server).
 */
class AltarPowerTest {

    private static int power(Block block) {
        return AltarBlockEntity.powerContribution(block.defaultBlockState());
    }

    private static int cap(Block block) {
        return AltarBlockEntity.powerSourceCap(block.defaultBlockState());
    }

    @Test
    void vanillaSourcesHaveTheirSourcePower() {
        assertEquals(1, power(Blocks.DIRT));
        assertEquals(2, power(Blocks.GRASS_BLOCK));
        assertEquals(2, power(Blocks.OAK_LOG));
        assertEquals(3, power(Blocks.OAK_LEAVES));
        assertEquals(3, power(Blocks.CACTUS));
        assertEquals(4, power(Blocks.WHEAT));      // crops contribute +4
        assertEquals(4, power(Blocks.POPPY));
        assertEquals(4, power(Blocks.DANDELION));
    }

    @Test
    void witcherySourcesHaveTheirSourcePower() {
        assertEquals(2, power(WitcheryBlocks.GLINT_WEED.get()));
        assertEquals(3, power(WitcheryBlocks.ROWAN_LOG.get()));
        assertEquals(4, power(WitcheryBlocks.ROWAN_LEAVES.get()));
        assertEquals(4, power(WitcheryBlocks.BELLADONNA.get()));
    }

    @Test
    void highValueSourcesCarryTheirPowerAndCaps() {
        assertEquals(100, power(WitcheryBlocks.DEMON_HEART.get()));
        assertEquals(2, cap(WitcheryBlocks.DEMON_HEART.get()));
        assertEquals(1000, power(WitcheryBlocks.INFINITY_EGG.get()));
        assertEquals(1, cap(WitcheryBlocks.INFINITY_EGG.get()));
        assertEquals(250, power(Blocks.DRAGON_EGG));
        assertEquals(1, cap(Blocks.DRAGON_EGG));
    }

    @Test
    void nonSourcesContributeNothing() {
        assertEquals(0, power(Blocks.STONE));
        assertEquals(0, power(Blocks.COBBLESTONE));
        assertEquals(0, power(Blocks.AIR));
    }
}
