package com.emoniph.witchery.blocks;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursedBlocksTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void laddersAreSupportedCursedBlocksButNotRightClickOrPressureTargets() {
        BlockState ladder = Blocks.LADDER.defaultBlockState();

        assertTrue(CursedBlocks.isLadder(ladder));
        assertTrue(CursedBlocks.isSupportedCursedBlock(ladder));
        assertFalse(CursedBlocks.isRightClickActivated(ladder));
        assertFalse(CursedBlocks.isPressurePlate(ladder));
    }

    @Test
    void ordinaryBlocksAreNotSupportedCursedBlocks() {
        assertFalse(CursedBlocks.isSupportedCursedBlock(Blocks.GRASS_BLOCK.defaultBlockState()));
    }
}
