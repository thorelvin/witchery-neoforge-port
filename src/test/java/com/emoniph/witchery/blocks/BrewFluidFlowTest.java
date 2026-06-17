package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain-JVM regression lock for the brew-liquid optimal-flow pathfinding
 * ({@link BrewFluidBlock#optimalFlowDirections}, source {@code getOptimalFlowDirections}). The world is
 * modelled by predicates: the flow plane at y=0 is open, and "drops" are flowable cells at y=-1.
 */
class BrewFluidFlowTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    private static final Predicate<BlockPos> NO_SOURCES = pos -> false;

    /** Flowable: the whole y=0 plane is open; below (y=-1) only the given drop holes are flowable. */
    private static Predicate<BlockPos> world(Set<BlockPos> drops) {
        return pos -> pos.getY() == 0 || (pos.getY() == -1 && drops.contains(pos));
    }

    @Test
    void flowsTowardTheNearestReachableDrop() {
        // A hole two blocks east → the liquid should pick only EAST (cost 1) over the others.
        List<Direction> optimal = BrewFluidBlock.optimalFlowDirections(
                ORIGIN, world(Set.of(new BlockPos(2, -1, 0))), NO_SOURCES);
        assertEquals(List.of(Direction.EAST), optimal);
    }

    @Test
    void immediateDropBelowANeighbourIsCostZero() {
        // Hole directly under the east neighbour → EAST cost 0, the unique minimum.
        List<Direction> optimal = BrewFluidBlock.optimalFlowDirections(
                ORIGIN, world(Set.of(new BlockPos(1, -1, 0))), NO_SOURCES);
        assertEquals(List.of(Direction.EAST), optimal);
    }

    @Test
    void noReachableDropSpreadsEvenlyInAllFourDirections() {
        // Flat plane, no holes → every direction ties at max cost → even spread (source fallback).
        List<Direction> optimal = BrewFluidBlock.optimalFlowDirections(ORIGIN, world(Set.of()), NO_SOURCES);
        assertEquals(4, optimal.size());
        assertTrue(optimal.containsAll(
                List.of(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH)));
    }

    @Test
    void aBlockedDirectionIsNeverChosenOverAReachableDrop() {
        // East neighbour is a wall (not flowable); the only drop is to the west → flow WEST.
        Predicate<BlockPos> canFlowInto = pos -> {
            if (pos.equals(new BlockPos(1, 0, 0))) {
                return false; // wall to the east
            }
            return pos.getY() == 0 || (pos.getY() == -1 && pos.equals(new BlockPos(-2, -1, 0)));
        };
        List<Direction> optimal = BrewFluidBlock.optimalFlowDirections(ORIGIN, canFlowInto, NO_SOURCES);
        assertEquals(List.of(Direction.WEST), optimal);
    }

    @Test
    void brewNeverDisplacesRealFluids() {
        assertFalse(BrewFluidBlock.canDisplaceForBrew(false, true, false, false, true));
    }

    @Test
    void brewStillDisplacesAirAndReplaceableNonFluids() {
        assertTrue(BrewFluidBlock.canDisplaceForBrew(true, false, false, false, false));
        assertTrue(BrewFluidBlock.canDisplaceForBrew(false, true, false, false, false));
    }

    @Test
    void gasContactPulseIsDeterministicEveryOtherGasTick() {
        assertFalse(BrewFluidBlock.shouldApplyGasContactOnRunTick(0));
        assertTrue(BrewFluidBlock.shouldApplyGasContactOnRunTick(1));
        assertFalse(BrewFluidBlock.shouldApplyGasContactOnRunTick(2));
        assertTrue(BrewFluidBlock.shouldApplyGasContactOnRunTick(3));
    }
}
