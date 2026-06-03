package com.emoniph.witchery.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the Mystic Branch HUD's pending-stroke direction math — the sign-convention part most
 * likely to point an arrow the wrong way. Must agree with {@link SymbolDrawState}'s commit
 * convention (0=up, 1=down, 2=right, 3=left). Pure logic; runs headlessly (the actual drawing is
 * visual and verified in-game).
 */
class SymbolHudTest {

    @Test
    void pendingArrowFollowsLookDirection() {
        // dxDeg = yaw − anchor (turn right = +); dyDeg = pitch − anchor (look up = −).
        assertEquals(0, SymbolHud.pendingDir(0f, -10f), "looking up → up arrow");
        assertEquals(1, SymbolHud.pendingDir(0f, 10f), "looking down → down arrow");
        assertEquals(2, SymbolHud.pendingDir(10f, 0f), "turning right → right arrow");
        assertEquals(3, SymbolHud.pendingDir(-10f, 0f), "turning left → left arrow");
    }

    @Test
    void dominantAxisWins() {
        assertEquals(0, SymbolHud.pendingDir(4f, -12f), "mostly up → up");
        assertEquals(2, SymbolHud.pendingDir(12f, -4f), "mostly right → right");
        assertEquals(1, SymbolHud.pendingDir(-5f, 9f), "mostly down → down");
    }
}
