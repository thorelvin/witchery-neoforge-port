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
    private static final RitualCircleDetector.CircleSize MEDIUM = RitualCircleDetector.CircleSize.MEDIUM;
    private static final RitualCircleDetector.CircleSize LARGE = RitualCircleDetector.CircleSize.LARGE;

    private static String match(ItemStack item, int variant) {
        return RitualExecutor.matchingRitualDefinitionName(SMALL, item, variant);
    }

    private static String match(RitualCircleDetector.CircleSize size, ItemStack item, int variant) {
        return RitualExecutor.matchingRitualDefinitionName(size, item, variant);
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

    @Test
    void mediumTriggersResolveToTheirRite() {
        assertEquals("whiff_of_magic_familiar", match(MEDIUM, new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()), RITUAL));
        assertEquals("bind_familiar", match(MEDIUM, new ItemStack(WitcheryItems.TEAR_OF_GODDESS.get()), RITUAL));
        assertEquals("bind_familiar", match(MEDIUM, new ItemStack(WitcheryItems.ODOUR_OF_PURITY.get()), RITUAL));
        assertEquals("call_familiar", match(MEDIUM, new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()), RITUAL));
        assertEquals("call_familiar", match(MEDIUM, new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()), RITUAL));
        assertEquals("rain_of_toads", match(MEDIUM, new ItemStack(Items.LILY_PAD), INFERNAL));
        assertEquals("rain_of_toads", match(MEDIUM, new ItemStack(Items.FROGSPAWN), INFERNAL));
        assertEquals("blight", match(MEDIUM, new ItemStack(Items.WITHER_ROSE), INFERNAL));
        assertEquals("corrupt_poppet", match(MEDIUM, new ItemStack(WitcheryItems.VOODOO_PROTECTION_POPPET.get()), INFERNAL));
        assertEquals("bind_fetish", match(MEDIUM, new ItemStack(WitcheryItems.FETISH_SCARECROW.get()), RITUAL));
    }

    @Test
    void largeTriggersResolveToTheirRite() {
        assertEquals("transpose_ores", match(LARGE, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), OTHERWHERE));
        assertEquals("moving_earth", match(LARGE, new ItemStack(Items.DIRT), RITUAL));
        assertEquals("moving_earth", match(LARGE, new ItemStack(Items.SAND), RITUAL));
        assertEquals("volcano", match(LARGE, new ItemStack(Items.LAVA_BUCKET), INFERNAL));
        assertEquals("broken_earth", match(LARGE, new ItemStack(Items.FLINT), RITUAL));
        assertEquals("summon_imp", match(LARGE, new ItemStack(WitcheryItems.REFINED_EVIL.get()), INFERNAL));
        assertEquals("summon_demon", match(LARGE, new ItemStack(WitcheryItems.TORMENTED_TWINE.get()), INFERNAL));
    }

    @Test
    void ringSizeAndVariantDisambiguateSharedItems() {
        // Diamond Vapour: small+infernal -> summon_witch, large+otherwhere -> transpose_ores; nothing else.
        assertEquals("summon_witch", match(SMALL, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertEquals("transpose_ores", match(LARGE, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), OTHERWHERE));
        assertNull(match(LARGE, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertNull(match(SMALL, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), OTHERWHERE));
        // Raise Column is data-driven for medium and large, but small raise-column is still in the if-chain.
        assertEquals("raise_column_medium", match(MEDIUM, new ItemStack(Items.COBBLESTONE), RITUAL));
        assertEquals("raise_column_large", match(LARGE, new ItemStack(Items.STONE), RITUAL));
        assertNull(match(SMALL, new ItemStack(Items.COBBLESTONE), RITUAL));
    }
}
