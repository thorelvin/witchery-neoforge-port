package com.emoniph.witchery.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WitcheryAreaModeTest {

    @Test
    void parsesConfiguredModesCaseInsensitively() {
        assertEquals(WitcheryAreaMode.SKIP, WitcheryAreaMode.parse("skip", WitcheryAreaMode.FAIL));
        assertEquals(WitcheryAreaMode.FAIL, WitcheryAreaMode.parse("FAIL", WitcheryAreaMode.SKIP));
        assertEquals(WitcheryAreaMode.SKIP, WitcheryAreaMode.parse(" SkIp ", WitcheryAreaMode.FAIL));
    }

    @Test
    void fallsBackForBlankOrUnknownValues() {
        assertEquals(WitcheryAreaMode.FAIL, WitcheryAreaMode.parse("", WitcheryAreaMode.FAIL));
        assertEquals(WitcheryAreaMode.SKIP, WitcheryAreaMode.parse("ignore", WitcheryAreaMode.SKIP));
    }
}
