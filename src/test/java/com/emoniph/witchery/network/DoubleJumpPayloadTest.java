package com.emoniph.witchery.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleJumpPayloadTest {

    @Test
    void sourceJumpCountAndVelocityAreExact() {
        assertEquals(1, DoubleJumpPayload.sourceExtraJumps(0));
        assertEquals(4, DoubleJumpPayload.sourceExtraJumps(3));
        assertEquals(0.42D, DoubleJumpPayload.sourceJumpVelocity(-1), 1.0E-9D);
        assertEquals(0.52D, DoubleJumpPayload.sourceJumpVelocity(0), 1.0E-9D);
        assertEquals(0.72D, DoubleJumpPayload.sourceJumpVelocity(2), 1.0E-9D);
    }
}
