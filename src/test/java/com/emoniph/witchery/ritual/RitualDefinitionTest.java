package com.emoniph.witchery.ritual;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks the data-driven small-ring {@link RitualDefinition} dispatch table: each migrated trigger
 * item (on the right circle variant) resolves to its rite, and the circle-variant gates are enforced.
 * Runs headlessly via ModDevGradle {@code unitTest} (game registries bootstrapped, no client/server).
 */
class RitualDefinitionTest {

    // Mirror of RitualExecutor's private VARIANT_* constants.
    private static final int RITUAL = 1;
    private static final int OTHERWHERE = 2;
    private static final int INFERNAL = 3;

    private static final RitualCircleDetector.CircleSize SMALL = RitualCircleDetector.CircleSize.SMALL;

    private static String match(ItemStack item, int variant) {
        return RitualExecutor.matchingRitualDefinitionName(SMALL, item, variant);
    }

    @Test
    void migratedTriggersResolveToTheirRite() {
        assertEquals("summon_witch", match(new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertEquals("summon_familiar", match(new ItemStack(WitcheryItems.DROP_OF_LUCK.get()), RITUAL));
        assertEquals("prior_incarnation", match(new ItemStack(Items.ROTTEN_FLESH), INFERNAL));
        assertEquals("ice_shell", match(new ItemStack(WitcheryItems.FROZEN_HEART.get()), RITUAL));
        assertEquals("storm_base", match(new ItemStack(Items.SNOWBALL), RITUAL));
        assertEquals("storm_large", match(new ItemStack(Items.WATER_BUCKET), RITUAL));
        assertEquals("storm_portable", match(new ItemStack(Items.GUNPOWDER), RITUAL));
        assertEquals("sanctity", match(new ItemStack(Items.FEATHER), RITUAL));
        assertEquals("imprisonment", match(new ItemStack(Items.SLIME_BALL), RITUAL));
        assertEquals("cook_item", match(new ItemStack(Items.SUGAR), INFERNAL));
        assertEquals("infuse_seer_stone", match(new ItemStack(WitcheryItems.QUARTZ_SPHERE.get()), RITUAL));
    }

    @Test
    void iceShellIgnoresCircleVariant() {
        // Frozen Heart -> Ice Shell has no variant gate (ANY_VARIANT).
        assertEquals("ice_shell", match(new ItemStack(WitcheryItems.FROZEN_HEART.get()), INFERNAL));
        assertEquals("ice_shell", match(new ItemStack(WitcheryItems.FROZEN_HEART.get()), OTHERWHERE));
    }

    @Test
    void variantGatesAreEnforced() {
        // Storm rites need a Ritual ring; cook needs Infernal.
        assertNull(match(new ItemStack(Items.SNOWBALL), INFERNAL));
        assertNull(match(new ItemStack(Items.FEATHER), INFERNAL));
        assertNull(match(new ItemStack(Items.SUGAR), RITUAL));
        assertNull(match(new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), RITUAL));
    }

    @Test
    void unrelatedTriggersDoNotMatch() {
        assertNull(match(new ItemStack(Items.DIRT), RITUAL));
        assertNull(match(ItemStack.EMPTY, RITUAL));
    }
}
