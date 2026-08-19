package com.emoniph.witchery.brewing;

import com.emoniph.witchery.blocks.StockadeBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IceWorldConversionTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void areaRadiusMatchesOriginalFormula() {
        assertEquals(1, IceWorldConversion.sourceAreaRadius(0));
        assertEquals(2, IceWorldConversion.sourceAreaRadius(1));
        assertEquals(5, IceWorldConversion.sourceAreaRadius(3));
        assertEquals(6, IceWorldConversion.sourceAreaRadius(4));
        assertEquals(9, IceWorldConversion.sourceAreaRadius(6));
    }

    @Test
    void filledSphereUsesOriginalStrictBoundary() {
        assertTrue(IceWorldConversion.sourceSphereContains(0, 0, 0, 1));
        assertFalse(IceWorldConversion.sourceSphereContains(1, 0, 0, 1));
        assertTrue(IceWorldConversion.sourceSphereContains(4, 2, 1, 5));
        assertFalse(IceWorldConversion.sourceSphereContains(4, 2, 2, 5));
        assertFalse(IceWorldConversion.sourceSphereContains(5, 0, 0, 5));
    }

    @Test
    void sourceBreakGateRejectsUnbreakableBlocks() {
        assertFalse(IceWorldConversion.sourceCanBreak(-1.0F));
        assertTrue(IceWorldConversion.sourceCanBreak(0.0F));
        assertTrue(IceWorldConversion.sourceCanBreak(50.0F));
    }

    @Test
    void terrainMappingsMatchOriginalTable() {
        assertSame(Blocks.SNOW_BLOCK,
                IceWorldConversion.replacementFor(Blocks.DIRT.defaultBlockState()).getBlock());
        assertSame(Blocks.PACKED_ICE,
                IceWorldConversion.replacementFor(Blocks.COBBLESTONE.defaultBlockState()).getBlock());
        assertSame(WitcheryBlocks.PERPETUAL_ICE.get(),
                IceWorldConversion.replacementFor(Blocks.STONE.defaultBlockState()).getBlock());
    }

    @Test
    void fixtureMappingsPreserveSupportedState() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.TOP);
        BlockState iceStairs = IceWorldConversion.replacementFor(stairs);
        assertSame(WitcheryBlocks.ICE_STAIRS.get(), iceStairs.getBlock());
        assertEquals(Direction.WEST, iceStairs.getValue(StairBlock.FACING));
        assertEquals(Half.TOP, iceStairs.getValue(StairBlock.HALF));

        BlockState slab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState iceSlab = IceWorldConversion.replacementFor(slab);
        assertSame(WitcheryBlocks.ICE_SLAB.get(), iceSlab.getBlock());
        assertEquals(SlabType.TOP, iceSlab.getValue(SlabBlock.TYPE));

        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(FenceGateBlock.FACING, Direction.SOUTH)
                .setValue(FenceGateBlock.OPEN, true)
                .setValue(FenceGateBlock.IN_WALL, true);
        BlockState iceGate = IceWorldConversion.replacementFor(gate);
        assertSame(WitcheryBlocks.ICE_GATE.get(), iceGate.getBlock());
        assertEquals(Direction.SOUTH, iceGate.getValue(FenceGateBlock.FACING));
        assertTrue(iceGate.getValue(FenceGateBlock.OPEN));
        assertTrue(iceGate.getValue(FenceGateBlock.IN_WALL));

        BlockState stockade = WitcheryBlocks.STOCKADE.get().defaultBlockState()
                .setValue(StockadeBlock.UP, true)
                .setValue(StockadeBlock.NORTH, true);
        BlockState iceStockade = IceWorldConversion.replacementFor(stockade);
        assertSame(WitcheryBlocks.ICE_STOCKADE.get(), iceStockade.getBlock());
        assertTrue(iceStockade.getValue(StockadeBlock.UP));
        assertTrue(iceStockade.getValue(StockadeBlock.NORTH));
    }
}
