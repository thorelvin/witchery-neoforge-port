package com.emoniph.witchery.ritual;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static List<String> rings(RitualCircleDetector.CircleSize size, ItemStack item, int variant) {
        return RitualExecutor.matchingRitualDefinitionCompanionRings(size, item, variant);
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
    void companionGatedRitesAreInTheTableWithTheirRings() {
        // Small infusion bases require a Medium companion ring in the matching chalk family.
        assertEquals("infuse_light", match(SMALL, new ItemStack(WitcheryItems.GHOST_OF_THE_LIGHT.get()), RITUAL));
        assertEquals(List.of("MEDIUM:" + RITUAL), rings(SMALL, new ItemStack(WitcheryItems.GHOST_OF_THE_LIGHT.get()), RITUAL));
        assertEquals("infuse_overworld", match(SMALL, new ItemStack(WitcheryItems.SOUL_OF_THE_WORLD.get()), RITUAL));
        assertEquals("infuse_otherwhere", match(SMALL, new ItemStack(WitcheryItems.SPIRIT_OF_OTHERWHERE.get()), OTHERWHERE));
        assertEquals(List.of("MEDIUM:" + OTHERWHERE), rings(SMALL, new ItemStack(WitcheryItems.SPIRIT_OF_OTHERWHERE.get()), OTHERWHERE));
        assertEquals("infuse_infernal", match(SMALL, new ItemStack(WitcheryItems.INFERNAL_ANIMUS.get()), INFERNAL));

        // Medium rites gated on a companion Small ring.
        assertEquals("enchant_broom", match(MEDIUM, new ItemStack(WitcheryItems.FLYING_OINTMENT.get()), RITUAL));
        assertEquals(List.of("SMALL:" + RITUAL), rings(MEDIUM, new ItemStack(WitcheryItems.FLYING_OINTMENT.get()), RITUAL));
        assertEquals("mystic_branch", match(MEDIUM, new ItemStack(WitcheryItems.ENT_TWIG.get()), RITUAL));
        assertEquals("infuse_brew", match(MEDIUM, new ItemStack(WitcheryItems.INFUSED_BREW_BASE.get()), RITUAL));

        // A rite with no companion requirement reports none.
        assertEquals(List.of(), rings(SMALL, new ItemStack(Items.FEATHER), RITUAL));
    }

    @Test
    void ringSizeAndVariantDisambiguateSharedItems() {
        // Diamond Vapour: small+infernal -> summon_witch, large+otherwhere -> transpose_ores; nothing else.
        assertEquals("summon_witch", match(SMALL, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertEquals("transpose_ores", match(LARGE, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), OTHERWHERE));
        assertNull(match(LARGE, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertNull(match(SMALL, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), OTHERWHERE));
        // Raise Column is data-driven for all three rings.
        assertEquals("raise_column_small", match(SMALL, new ItemStack(Items.COBBLESTONE), RITUAL));
        assertEquals("raise_column_medium", match(MEDIUM, new ItemStack(Items.COBBLESTONE), RITUAL));
        assertEquals("raise_column_large", match(LARGE, new ItemStack(Items.STONE), RITUAL));
    }

    @Test
    void offeringGatedAndRemainingSmallRitesAreInTheTable() {
        // Small stragglers (no offering) now data-driven.
        assertEquals("total_eclipse", match(SMALL, new ItemStack(Items.IRON_AXE), RITUAL));
        assertEquals("total_eclipse", match(SMALL, new ItemStack(Items.STONE_AXE), RITUAL));
        assertEquals("spectral_stone", match(SMALL, new ItemStack(WitcheryItems.SPECTRAL_DUST.get()), RITUAL));
        assertEquals("manifestation_dream", match(SMALL, new ItemStack(WitcheryItems.SPECTRAL_DUST.get()), OTHERWHERE));
        // Blaze Powder disambiguates by ring variant.
        assertEquals("blindness", match(SMALL, new ItemStack(Items.BLAZE_POWDER), INFERNAL));
        assertEquals("banish_demon", match(SMALL, new ItemStack(Items.BLAZE_POWDER), RITUAL));

        // Medium offering-gated rites (trigger mapping; the offering/companion are checked at runtime).
        assertEquals("protection", match(MEDIUM, new ItemStack(Items.OBSIDIAN), RITUAL));
        assertEquals("protection", match(MEDIUM, new ItemStack(Items.CRYING_OBSIDIAN), RITUAL));
        assertEquals("charge_stone_medium", match(MEDIUM, new ItemStack(WitcheryItems.ATTUNED_STONE.get()), RITUAL));
        // charge_stone_medium carries both a companion ring and an offering requirement.
        assertEquals(List.of("SMALL:" + RITUAL), rings(MEDIUM, new ItemStack(WitcheryItems.ATTUNED_STONE.get()), RITUAL));
    }
}
