package com.emoniph.witchery.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampireBloodReserveTest {

    @Test
    void reserveIsVampireVisibleAndClamped() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setBloodReserve(125);
        assertEquals(0, data.getBloodReserve());
        assertFalse(data.isBloodReserveReady());

        data.setVampire(true);
        data.addBloodReserve(300);
        assertEquals(250, data.getBloodReserve());
        assertTrue(data.isBloodReserveReady());
    }

    @Test
    void usingReserveClearsWholeSourceReserveAndClampsPowerGain() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setVampire(true);
        data.setVampireLevel(1);
        data.setBloodPower(data.getMaxBloodPower() - 50);
        data.setBloodReserve(250);

        assertEquals(50, data.useBloodReserve());
        assertEquals(data.getMaxBloodPower(), data.getBloodPower());
        assertEquals(0, data.getBloodReserve());
    }

    @Test
    void reserveStaysReadyWhenPowerAlreadyFull() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setVampire(true);
        data.setBloodPower(data.getMaxBloodPower());
        data.setBloodReserve(100);

        assertEquals(0, data.useBloodReserve());
        assertEquals(100, data.getBloodReserve());
    }

    @Test
    void curingVampirismClearsReserve() {
        WitcheryPlayerData data = new WitcheryPlayerData();
        data.setVampire(true);
        data.setBloodReserve(100);

        data.setVampire(false);

        assertEquals(0, data.getBloodReserve());
        data.setVampire(true);
        assertEquals(0, data.getBloodReserve());
    }
}
