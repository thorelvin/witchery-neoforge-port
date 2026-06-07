package com.emoniph.witchery.ritual;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests of the {@link RitualDefinition} data model itself — the foundation the whole
 * dispatch table is built on. No executor, no world: triggers/effects are plain lambdas. Verifies
 * matching, the ANY_VARIANT sentinel, copy-on-write for {@code requiringOffering}/{@code vetoedWhen},
 * companion-ring varargs, and {@code run} delegation. Runs headlessly via ModDevGradle {@code unitTest}.
 */
class RitualDefinitionModelTest {

    private static final RitualCircleDetector.CircleSize SMALL = RitualCircleDetector.CircleSize.SMALL;
    private static final RitualCircleDetector.CircleSize MEDIUM = RitualCircleDetector.CircleSize.MEDIUM;

    private static final ItemStack STONE = new ItemStack(Items.STONE);
    private static final ItemStack DIRT = new ItemStack(Items.DIRT);

    private static RitualDefinition stoneRite(int variant) {
        return RitualDefinition.of("test", SMALL, item -> item.is(Items.STONE), variant,
                (player, level, center, item) -> true);
    }

    @Test
    void matchesHonoursSizeVariantAndTrigger() {
        RitualDefinition def = stoneRite(1);
        assertTrue(def.matches(SMALL, STONE, 1), "right size+variant+trigger matches");
        assertFalse(def.matches(MEDIUM, STONE, 1), "wrong ring size does not match");
        assertFalse(def.matches(SMALL, STONE, 2), "wrong circle variant does not match");
        assertFalse(def.matches(SMALL, DIRT, 1), "wrong trigger item does not match");
    }

    @Test
    void anyVariantIgnoresCircleVariant() {
        RitualDefinition def = stoneRite(RitualDefinition.ANY_VARIANT);
        assertTrue(def.matches(SMALL, STONE, 1));
        assertTrue(def.matches(SMALL, STONE, 2));
        assertTrue(def.matches(SMALL, STONE, 3));
        assertFalse(def.matches(SMALL, DIRT, 1), "ANY_VARIANT still respects the trigger");
    }

    @Test
    void requirementsAndVetoesAreImmutableCopies() {
        RitualDefinition base = stoneRite(1);
        assertEquals(0, base.companionRings().size());
        assertEquals(0, base.offeringRequirements().size());
        assertEquals(0, base.vetoes().size());

        RitualDefinition withOffering = base.requiringOffering((level, center) -> true);
        RitualDefinition withVeto = withOffering.vetoedWhen((level, center) -> true);

        // Each step is a copy; earlier instances are unchanged.
        assertEquals(0, base.offeringRequirements().size(), "base unchanged by requiringOffering");
        assertEquals(1, withOffering.offeringRequirements().size());
        assertEquals(0, withOffering.vetoes().size(), "withOffering unchanged by vetoedWhen");
        assertEquals(1, withVeto.offeringRequirements().size(), "offering carried into the veto copy");
        assertEquals(1, withVeto.vetoes().size());
    }

    @Test
    void companionRingsComeFromVarargs() {
        RitualDefinition def = RitualDefinition.of("test", SMALL, item -> true, 1,
                (player, level, center, item) -> true,
                RitualDefinition.CompanionRing.of(MEDIUM, 1),
                RitualDefinition.CompanionRing.of(SMALL, 3));
        assertEquals(2, def.companionRings().size());
        assertEquals(MEDIUM, def.companionRings().get(0).size());
        assertEquals(3, def.companionRings().get(1).variant());
    }

    @Test
    void runDelegatesToTheEffectAndReturnsItsResult() {
        ItemStack[] seen = new ItemStack[1];
        RitualDefinition succeeds = RitualDefinition.of("ok", SMALL, item -> true, 1,
                (player, level, center, item) -> { seen[0] = item; return true; });
        RitualDefinition fails = RitualDefinition.of("no", SMALL, item -> true, 1,
                (player, level, center, item) -> false);
        assertTrue(succeeds.run(null, null, null, STONE));
        assertSame(STONE, seen[0], "the triggering item is threaded into the effect");
        assertFalse(fails.run(null, null, null, STONE));
    }
}
