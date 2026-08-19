package com.emoniph.witchery.spiritworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpiritLifecycleAuditTest {
    @Test
    void wakingDreamManifestationAndPhysicalTravelAreDistinctValidStates() {
        assertValid(new SpiritLifecycleAudit.Snapshot(
                false, false, false, false, false, false, false, 0));
        assertValid(new SpiritLifecycleAudit.Snapshot(
                true, true, false, false, true, true, true, 0));
        assertValid(new SpiritLifecycleAudit.Snapshot(
                false, true, false, true, true, true, true, 120));
        assertValid(new SpiritLifecycleAudit.Snapshot(
                true, false, true, false, false, false, false, 0));
    }

    @Test
    void impossibleMixedAndOrphanedStatesReportActionableViolations() {
        List<String> mixed = SpiritLifecycleAudit.violations(new SpiritLifecycleAudit.Snapshot(
                true, true, true, false, false, false, false, 0));
        assertTrue(mixed.contains("Dream travel and physical Hearth travel are both active."));
        assertTrue(mixed.contains("Dream travel has no stored waking position."));
        assertTrue(mixed.contains("Dream travel has no stored waking dimension."));

        List<String> orphanedManifestation = SpiritLifecycleAudit.violations(
                new SpiritLifecycleAudit.Snapshot(
                        false, false, false, true, false, false, false, 0));
        assertEquals(2, orphanedManifestation.size());
        assertTrue(orphanedManifestation.contains(
                "Manifestation is active without an underlying Spirit dream."));
        assertTrue(orphanedManifestation.contains(
                "Manifested ghost has no remaining manifestation time."));
    }

    @Test
    void unexplainedSpiritDimensionPresenceIsRejected() {
        assertEquals(List.of("Player is in the Spirit World without dream or physical-travel state."),
                SpiritLifecycleAudit.violations(new SpiritLifecycleAudit.Snapshot(
                        true, false, false, false, false, false, false, 0)));
    }

    private static void assertValid(SpiritLifecycleAudit.Snapshot snapshot) {
        assertTrue(SpiritLifecycleAudit.violations(snapshot).isEmpty());
    }
}
