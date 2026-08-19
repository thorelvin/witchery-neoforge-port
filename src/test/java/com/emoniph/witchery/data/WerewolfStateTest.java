package com.emoniph.witchery.data;

import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import net.minecraft.nbt.NbtOps;
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
        d.setTransformationLevel(1.0F);
        d.setWolfForm(WolfForm.WOLFMAN);
        assertEquals(WolfForm.WOLFMAN, d.getWolfForm());

        d.setWerewolfLevel(4);
        assertEquals(WolfForm.WOLF, d.getWolfForm());
        assertEquals(1.0F, d.getTransformationLevel());

        d.setWerewolfLevel(0);
        assertEquals(WolfForm.NONE, d.getWolfForm());
        assertEquals(0.0F, d.getTransformationLevel());
    }

    @Test
    void clearingTheCurseClearsEveryTransformationMarker() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        d.setWerewolfLevel(5);
        d.setTransformationLevel(1.0F);
        d.setWolfForm(WolfForm.WOLFMAN);

        d.setWerewolf(false);

        assertFalse(d.isWerewolf());
        assertEquals(0, d.getWerewolfLevel());
        assertEquals(WolfForm.NONE, d.getWolfForm());
        assertEquals(0.0F, d.getTransformationLevel());
    }

    @Test
    void howledChunksDeDuplicate() {
        WitcheryPlayerData d = new WitcheryPlayerData();
        assertTrue(d.addHowledChunk(42L));
        assertFalse(d.addHowledChunk(42L)); // already counted
        assertTrue(d.addHowledChunk(43L));
        assertEquals(2, d.getHowledChunkCount());
    }

    @Test
    void lycanthropyStateSurvivesCodecRoundTrip() {
        WitcheryPlayerData original = new WitcheryPlayerData();
        original.setWerewolfLevel(9);
        original.setTransformationLevel(1.0F);
        original.setWolfForm(WolfForm.WOLFMAN);
        original.setWolfQuestState(WolfQuestState.STARTED);
        original.incrementWolfQuestCounter();
        original.addHowledChunk(42L);
        original.addHowledChunk(43L);
        original.setLastHowl(12000L);
        original.setLastBoneFind(8000L);

        var tag = WitcheryPlayerData.CODEC.codec().encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        WitcheryPlayerData restored = WitcheryPlayerData.CODEC.codec()
                .parse(NbtOps.INSTANCE, tag).getOrThrow();

        assertTrue(restored.isWerewolf());
        assertEquals(9, restored.getWerewolfLevel());
        assertEquals(1.0F, restored.getTransformationLevel());
        assertEquals(WolfForm.WOLFMAN, restored.getWolfForm());
        assertEquals(WolfQuestState.STARTED, restored.getWolfQuestState());
        assertEquals(1, restored.getWolfQuestCounter());
        assertEquals(2, restored.getHowledChunkCount());
        assertEquals(12000L, restored.getLastHowl());
        assertEquals(8000L, restored.getLastBoneFind());
    }
}
