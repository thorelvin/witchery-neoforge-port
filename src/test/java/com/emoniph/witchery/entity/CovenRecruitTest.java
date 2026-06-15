package com.emoniph.witchery.entity;

import com.emoniph.witchery.entity.CovenWitchEntity.CovenRecruitOutcome;
import com.emoniph.witchery.entity.CovenWitchEntity.QuestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
