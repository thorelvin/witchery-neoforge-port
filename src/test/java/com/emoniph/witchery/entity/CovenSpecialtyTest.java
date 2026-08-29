package com.emoniph.witchery.entity;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CovenSpecialtyTest {
    @Test
    void idsRoundTripAndIdentityAssignmentIsStable() {
        UUID identity = UUID.fromString("3aa7cf91-6dd2-4239-9a27-afafaa8a8ec8");
        for (CovenSpecialty specialty : CovenSpecialty.values()) {
            assertSame(specialty, CovenSpecialty.fromId(specialty.id(), identity));
        }
        assertSame(CovenSpecialty.forIdentity(identity), CovenSpecialty.forIdentity(identity));
        assertSame(CovenSpecialty.forIdentity(identity), CovenSpecialty.fromId("unknown", identity));
    }

    @Test
    void bonusesAreModestAndDuplicateSpecialtiesCollapse() {
        CovenContributionCounter.SpecialtyRoster graveRoster = new CovenContributionCounter.SpecialtyRoster(
                List.of(), Set.of(CovenSpecialty.GRAVE_WITCH));
        CovenContributionCounter.SpecialtyRoster hearthRoster = new CovenContributionCounter.SpecialtyRoster(
                List.of(), Set.of(CovenSpecialty.HEARTH_WITCH));

        assertEquals(950, CovenSpecialtyContributions.gravePower(1_000, graveRoster));
        assertEquals(5_700, CovenSpecialtyContributions.gravePower(6_000, graveRoster));
        assertEquals(1.3F, CovenSpecialtyContributions.hearthUpkeep(1.4F, hearthRoster), 0.0001F);
        assertEquals(0.0F, CovenSpecialtyContributions.hearthUpkeep(0.0F, hearthRoster));
        assertTrue(CovenSpecialtyContributions.HEDGE_FERTILITY_RADIUS_BONUS < 2,
                "Hedge craft must add less radius than one ordinary coven member");
        assertTrue(CovenSpecialtyContributions.PALE_MANIFESTATION_DURATION_BONUS < 25,
                "Pale craft must add less duration than one ordinary coven member");
        EnumSet<CovenSpecialty> duplicate = EnumSet.noneOf(CovenSpecialty.class);
        duplicate.add(CovenSpecialty.STORM_WITCH);
        duplicate.add(CovenSpecialty.STORM_WITCH);
        assertEquals(1, duplicate.size());
    }
}
