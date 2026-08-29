package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VampireFlightOwnershipTest {
    @Test
    void nonBatStateDoesNotRevokeExternalFlight() {
        VampireFlightOwnership.Decision state = VampireFlightOwnership.reconcile(
                false, false, true, true, false, false, false);

        assertTrue(state.mayfly());
        assertTrue(state.flying());
        assertFalse(state.ownsGrant());
    }

    @Test
    void batFormRestoresCapturedStateOnExit() {
        VampireFlightOwnership.Decision entered = VampireFlightOwnership.reconcile(
                true, false, false, false, false, false, false);
        VampireFlightOwnership.Decision exited = VampireFlightOwnership.reconcile(
                false, false, entered.mayfly(), entered.flying(), entered.ownsGrant(),
                entered.mayflyBefore(), entered.flyingBefore());

        assertTrue(entered.mayfly());
        assertTrue(entered.flying());
        assertFalse(exited.mayfly());
        assertFalse(exited.flying());
        assertFalse(exited.ownsGrant());
    }

    @Test
    void preexistingFlightSurvivesBatForm() {
        VampireFlightOwnership.Decision entered = VampireFlightOwnership.reconcile(
                true, false, true, true, false, false, false);
        VampireFlightOwnership.Decision exited = VampireFlightOwnership.reconcile(
                false, false, entered.mayfly(), entered.flying(), entered.ownsGrant(),
                entered.mayflyBefore(), entered.flyingBefore());

        assertTrue(exited.mayfly());
        assertTrue(exited.flying());
    }

    @Test
    void creativeTransitionClearsWitcheryOwnershipWithoutWritingAbilities() {
        VampireFlightOwnership.Decision state = VampireFlightOwnership.reconcile(
                true, true, true, true, true, false, false);

        assertTrue(state.mayfly());
        assertTrue(state.flying());
        assertFalse(state.ownsGrant());
    }
}
