package com.emoniph.witchery.entity;

import com.emoniph.witchery.entity.CovenWitchEntity.CovenRecruitOutcome;
import com.emoniph.witchery.entity.CovenWitchEntity.QuestKind;
import com.emoniph.witchery.entity.CovenWitchEntity.QuestStep;
import com.emoniph.witchery.world.CovenData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the coven recruit gate (source EntityCovenWitch join flow): a witch you own is dismissed,
 * one owned by another is refused, a full coven (6) is refused, recruiting needs an active familiar,
 * and a free witch offers then runs a fetch quest. Plus the fetch-quest step. Pure logic.
 */
class CovenRecruitTest {

    @Test
    void recruitGateMatchesSourceRules() {
        // args: ownedByPlayer, owned, covenFull, hasFamiliar, questOfferedToPlayer
        assertEquals(CovenRecruitOutcome.DISMISS, CovenWitchEntity.recruitOutcome(true, true, false, true, false));
        assertEquals(CovenRecruitOutcome.SERVES_ANOTHER, CovenWitchEntity.recruitOutcome(false, true, false, true, false));
        assertEquals(CovenRecruitOutcome.FULL, CovenWitchEntity.recruitOutcome(false, false, true, true, false));
        assertEquals(CovenRecruitOutcome.NO_FAMILIAR, CovenWitchEntity.recruitOutcome(false, false, false, false, false));
        assertEquals(CovenRecruitOutcome.OFFER_QUEST, CovenWitchEntity.recruitOutcome(false, false, false, true, false));
        assertEquals(CovenRecruitOutcome.RUN_QUEST, CovenWitchEntity.recruitOutcome(false, false, false, true, true));
    }

    @Test
    void fetchQuestStep() {
        assertEquals(QuestStep.NOT_FINISHED, CovenWitchEntity.questStep(false, 5));
        assertEquals(QuestStep.PROGRESS, CovenWitchEntity.questStep(true, 5));
        assertEquals(QuestStep.PROGRESS, CovenWitchEntity.questStep(true, 2));
        assertEquals(QuestStep.COMPLETE, CovenWitchEntity.questStep(true, 1));
    }

    @Test
    void covenCapIsSix() {
        assertEquals(6, CovenWitchEntity.MAX_COVEN);
    }

    @Test
    void persistedRosterEnforcesCap() {
        CovenData data = new CovenData();
        UUID owner = UUID.randomUUID();
        assertEquals(0, data.size(owner));
        assertFalse(data.isFull(owner));

        for (int i = 0; i < CovenData.MAX_COVEN; i++) {
            assertTrue(data.add(owner, UUID.randomUUID()), "should accept witch " + i);
        }
        assertEquals(6, data.size(owner));
        assertTrue(data.isFull(owner));
        assertFalse(data.add(owner, UUID.randomUUID()), "a full coven rejects more");

        UUID other = UUID.randomUUID();
        UUID dupe = UUID.randomUUID();
        assertTrue(data.add(other, dupe), "another player's coven starts empty and accepts one");
        assertFalse(data.add(other, dupe), "the same witch is not added twice");
        assertEquals(1, data.size(other));
    }

    @Test
    void rosterRemovalFreesSlots() {
        CovenData data = new CovenData();
        UUID owner = UUID.randomUUID();
        UUID witch = UUID.randomUUID();
        data.add(owner, witch);
        assertTrue(data.contains(owner, witch));
        data.remove(owner, witch);
        assertFalse(data.contains(owner, witch));
        assertEquals(0, data.size(owner));
    }

    @Test
    void questKindFromOrdinalIsStable() {
        assertEquals(QuestKind.FETCH, QuestKind.fromOrdinal(0));
        assertEquals(QuestKind.FIGHT_SPIDER, QuestKind.fromOrdinal(1));
        assertEquals(QuestKind.FIGHT_ZOMBIE, QuestKind.fromOrdinal(2));
        assertEquals(QuestKind.FETCH, QuestKind.fromOrdinal(-1));
        assertEquals(QuestKind.FETCH, QuestKind.fromOrdinal(99));
    }
}
