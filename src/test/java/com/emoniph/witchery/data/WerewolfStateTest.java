package com.emoniph.witchery.data;

import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the werewolf progression data model that replaced the flat wolfMastery 0-3 slot: level clamp,
 * level-up resetting the quest, the isWerewolf/level coupling, form gating, and the distinct howl-chunk
 * dedupe used by the level 6→7 quest. Pure data — no registries needed.
 */
class WerewolfStateTest {

    @Test
    void levelClampsAndIncreaseStopsAtTen() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        d.setWerewolfLevel(15);
        assertEquals(10, d.getWerewolfLevel());
        d.setWerewolfLevel(-3);
        assertEquals(0, d.getWerewolfLevel());
        d.setWerewolfLevel(9);
        d.increaseWerewolfLevel();
        d.increaseWerewolfLevel(); // already at 10
        assertEquals(10, d.getWerewolfLevel());
    }

    @Test
    void isWerewolfTracksLevel() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        assertFalse(d.isWerewolf());
        d.setWerewolf(true);
        assertTrue(d.isWerewolf());
        assertEquals(1, d.getWerewolfLevel()); // infection sets level 1
        d.setWerewolf(false);
        assertFalse(d.isWerewolf());
        assertEquals(0, d.getWerewolfLevel());
    }

    @Test
    void levelUpResetsTheQuest() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        d.setWerewolfLevel(5);
        d.setWolfQuestState(WolfQuestState.STARTED);
        d.incrementWolfQuestCounter();
        d.incrementWolfQuestCounter();
        assertEquals(2, d.getWolfQuestCounter());
        d.increaseWerewolfLevel();
        assertEquals(6, d.getWerewolfLevel());
        assertEquals(WolfQuestState.NOT_STARTED, d.getWolfQuestState());
        assertEquals(0, d.getWolfQuestCounter());
    }

    @Test
    void formRoundTripsAndClearsAtLevelZero() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        d.setWerewolfLevel(5);
        d.setWolfForm(WolfForm.WOLFMAN);
        assertEquals(WolfForm.WOLFMAN, d.getWolfForm());
        d.setWerewolfLevel(0);
        assertEquals(WolfForm.NONE, d.getWolfForm());
    }

    @Test
    void howledChunksDeDuplicate() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        assertTrue(d.addHowledChunk(42L));
        assertFalse(d.addHowledChunk(42L)); // already counted
        assertTrue(d.addHowledChunk(43L));
        assertEquals(2, d.getHowledChunkCount());
    }
}
