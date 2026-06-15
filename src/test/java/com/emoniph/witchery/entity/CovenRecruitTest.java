package com.emoniph.witchery.entity;

import com.emoniph.witchery.entity.CovenWitchEntity.CovenRecruitOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the coven recruit-decision state machine (source EntityCovenWitch join flow): a witch you
 * already own is dismissed, one owned by another is refused, a full coven (6) is refused, otherwise
 * she joins. Pure logic — no world needed.
 */
class CovenRecruitTest {

    @Test
    void recruitDecisionMatchesSourceRules() {
        // Already in your coven -> dismiss (ownedByPlayer wins regardless of other flags).
        assertEquals(CovenRecruitOutcome.DISMISS, CovenWitchEntity.recruitOutcome(true, true, false));
        assertEquals(CovenRecruitOutcome.DISMISS, CovenWitchEntity.recruitOutcome(true, true, true));
        // Owned by someone else -> refused.
        assertEquals(CovenRecruitOutcome.SERVES_ANOTHER, CovenWitchEntity.recruitOutcome(false, true, false));
        // Free witch but your coven is full -> refused.
        assertEquals(CovenRecruitOutcome.FULL, CovenWitchEntity.recruitOutcome(false, false, true));
        // Free witch, room in the coven -> joins.
        assertEquals(CovenRecruitOutcome.RECRUIT, CovenWitchEntity.recruitOutcome(false, false, false));
    }

    @Test
    void covenCapIsSix() {
        assertEquals(6, CovenWitchEntity.MAX_COVEN);
    }
}
