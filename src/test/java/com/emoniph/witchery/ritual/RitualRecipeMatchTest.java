package com.emoniph.witchery.ritual;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the multi-sacrifice matching rule ({@link RitualRecipe#matches}) and a few canonical rite
 * recipes transcribed from the source {@code RiteRegistry.addRecipe} sacrifice lists
 * (1.7.10-0.24.1). Guards against a rite silently accepting an incomplete offering set. Runs
 * headlessly via ModDevGradle {@code unitTest}.
 */
class RitualRecipeMatchTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // Source rite 9 — "witchery.rite.imprisonment": Slimeball + Redstone (+ power).
    private static final RitualRecipe IMPRISONMENT = RitualRecipe.named("imprisonment")
            .require("Slimeball", s -> s.is(Items.SLIME_BALL))
            .require("Redstone", s -> s.is(Items.REDSTONE))
            .build();

    // Source rite 10 — "witchery.rite.barrier": Obsidian + Redstone (+ power).
    private static final RitualRecipe BARRIER = RitualRecipe.named("barrier")
            .require("Obsidian", s -> s.is(Items.OBSIDIAN))
            .require("Redstone", s -> s.is(Items.REDSTONE))
            .build();

    private static ItemStack boundWaystone() {
        ItemStack stack = new ItemStack(WitcheryItems.WAYSTONE.get());
        WaystoneItem.bindTo(stack, BlockPos.ZERO, Level.OVERWORLD);
        return stack;
    }

    private static ItemStack filledTaglock() {
        ItemStack stack = new ItemStack(WitcheryItems.TAGLOCK_KIT.get());
        TaglockKitItem.bind(stack, UUID.fromString("12345678-1234-1234-1234-123456789abc"), "Victim");
        return stack;
    }

    @Test
    void fullOfferingSetMatches() {
        assertTrue(IMPRISONMENT.matches(List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.REDSTONE))));
        // Extra, irrelevant offerings are ignored.
        assertTrue(IMPRISONMENT.matches(List.of(
                new ItemStack(Items.DIAMOND), new ItemStack(Items.SLIME_BALL), new ItemStack(Items.REDSTONE))));
    }

    @Test
    void missingAnyRequiredItemFails() {
        assertFalse(IMPRISONMENT.matches(List.of(new ItemStack(Items.SLIME_BALL))), "no redstone → no rite");
        assertFalse(IMPRISONMENT.matches(List.of(new ItemStack(Items.REDSTONE))), "no slimeball → no rite");
        assertFalse(IMPRISONMENT.matches(List.of()), "empty altar → no rite");
        // The wrong rite's trigger must not satisfy this recipe.
        assertFalse(BARRIER.matches(List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.REDSTONE))));
    }

    @Test
    void duplicateRequirementsNeedDistinctStacks() {
        RitualRecipe twoRedstone = RitualRecipe.named("two_redstone")
                .require("Redstone A", s -> s.is(Items.REDSTONE))
                .require("Redstone B", s -> s.is(Items.REDSTONE))
                .build();
        assertFalse(twoRedstone.matches(List.of(new ItemStack(Items.REDSTONE))),
                "one stack cannot satisfy two requirements");
        assertTrue(twoRedstone.matches(List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE))));
    }

    @Test
    void stormRecipeVariantMatchesFullOfferingsRegardlessSupportOrder() {
        ItemStack woodAsh = new ItemStack(WitcheryItems.WOOD_ASH.get());
        assertEquals(Optional.of(StormRiteVariant.BASE), RitualOfferings.matchStormRecipeVariant(List.of(
                woodAsh, new ItemStack(Items.WOODEN_SWORD))));
        assertEquals(Optional.of(StormRiteVariant.LARGE), RitualOfferings.matchStormRecipeVariant(List.of(
                woodAsh, new ItemStack(Items.STONE_SWORD))));
        assertEquals(Optional.of(StormRiteVariant.PORTABLE), RitualOfferings.matchStormRecipeVariant(List.of(
                woodAsh, new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()), new ItemStack(Items.IRON_SWORD))));
        assertEquals(Optional.empty(), RitualOfferings.matchStormRecipeVariant(List.of(
                woodAsh, new ItemStack(Items.IRON_SWORD))), "portable Storm needs the charged stone too");
    }

    @Test
    void stormRecipeVariantUsesSourceRegistrationOrderWhenSeveralSetsArePresent() {
        assertEquals(Optional.of(StormRiteVariant.BASE), RitualOfferings.matchStormRecipeVariant(List.of(
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                new ItemStack(Items.WOODEN_SWORD),
                new ItemStack(WitcheryItems.WOOD_ASH.get()))));
    }

    @Test
    void fullRecipeDispatchMatchesProtectionClusterRegardlessSupportOrder() {
        assertEquals("sanctity", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.FEATHER)), 1));
        assertEquals("imprisonment", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.SLIME_BALL)), 1));
        assertEquals("obsidian_barrier_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()), new ItemStack(Items.OBSIDIAN)), 1));
        assertEquals("protection", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.OBSIDIAN)), 1));
    }

    @Test
    void fullRecipeDispatchKeepsSourceOrderForOverlappingProtectionAndBanishSets() {
        ItemStack blankWaystone = new ItemStack(WitcheryItems.WAYSTONE.get());
        assertEquals("obsidian_barrier_monsters", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.OBSIDIAN),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(Items.REDSTONE)), 1),
                "source registers the redstone barrier before portable barrier");
        assertEquals("banish_demon_charged", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BLAZE_POWDER),
                        blankWaystone,
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "source registers portable Banish before the altar-powered Banish recipe");
        assertEquals("obsidian_barrier_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.OBSIDIAN),
                        new ItemStack(Items.BLAZE_POWDER),
                        blankWaystone,
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "source registers portable barrier before the Banish recipes");
    }

    @Test
    void fullRecipeDispatchRequiresChargeStoneCompanionRing() {
        List<ItemStack> offerings = List.of(
                new ItemStack(WitcheryItems.QUICKLIME.get()),
                new ItemStack(Items.REDSTONE),
                new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                new ItemStack(WitcheryItems.WOOD_ASH.get()),
                new ItemStack(Items.GLOWSTONE_DUST));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL, offerings, 1),
                "Charge Stone needs the paired medium Ritual ring");
        assertEquals("charge_stone_small", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL, offerings, 1,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, 1))));
        assertEquals("charge_stone_medium", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM, offerings, 1,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, 1))));
    }

    @Test
    void fullRecipeDispatchKeepsChargeStoneBeforeNecromanticStoneWhenBothSetsArePresent() {
        assertEquals("charge_stone_small", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                        new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(WitcheryItems.WOOD_ASH.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(Items.BONE),
                        new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.SPECTRAL_DUST.get())),
                1,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, 1))),
                "source registers Charge Stone before Necromantic Stone");
    }

    @Test
    void fullRecipeDispatchMatchesStoneAndScryingRitesRegardlessSupportOrder() {
        assertEquals("necromantic_stone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.SPECTRAL_DUST.get()),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.WOOD_ASH.get()),
                        new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(Items.BONE),
                        new ItemStack(WitcheryItems.ATTUNED_STONE.get())), 1));
        assertEquals("spectral_stone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.BOLINE.get()),
                        new ItemStack(WitcheryItems.SPECTRAL_DUST.get()),
                        new ItemStack(WitcheryItems.CONDENSED_FEAR.get()),
                        new ItemStack(WitcheryItems.CONGEALED_SPIRIT.get()),
                        new ItemStack(WitcheryItems.NECRO_STONE.get())), 1));
        assertEquals("crystal_ball", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.HAPPENSTANCE_OIL.get()),
                        new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(WitcheryItems.QUARTZ_SPHERE.get())), 1));
        assertEquals("infuse_seer_stone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.HAPPENSTANCE_OIL.get()),
                        new ItemStack(Items.OBSIDIAN),
                        new ItemStack(WitcheryItems.QUARTZ_SPHERE.get())), 1));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.HAPPENSTANCE_OIL.get()),
                        new ItemStack(Items.MOSSY_COBBLESTONE),
                        new ItemStack(WitcheryItems.QUARTZ_SPHERE.get())), 1),
                "source Seer Stone uses Obsidian, not Mossy Cobblestone");
    }

    @Test
    void fullRecipeDispatchMatchesWaystoneRoutesRegardlessSupportOrder() {
        assertEquals("bind_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get())), 1));
        assertEquals("bind_waystone_copy", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get()),
                        boundWaystone()), 1));
        assertEquals("bind_waystone_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.WOOD_ASH.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get())), 1));
        assertEquals("bind_waystone_copy_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get()),
                        boundWaystone()), 1));
        assertEquals("bind_blooded_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.SNOWBALL),
                        new ItemStack(Items.SLIME_BALL),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get())), 1));
        assertEquals("bind_blooded_waystone_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.ICY_NEEDLE.get()),
                        new ItemStack(Items.SLIME_BALL),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get())), 1));
    }

    @Test
    void fullRecipeDispatchKeepsSourceOrderForOverlappingWaystoneSets() {
        assertEquals("bind_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.WAYSTONE.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(WitcheryItems.WOOD_ASH.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "source registers powered bind before portable bind");
        assertEquals("bind_waystone_copy", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(boundWaystone(),
                        new ItemStack(WitcheryItems.WAYSTONE.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "source registers powered copy before portable copy");
        assertEquals("bind_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.WAYSTONE.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(Items.SLIME_BALL),
                        new ItemStack(Items.SNOWBALL)), 1),
                "source registers location bind before Blooded Waystone binding");
        assertEquals("bind_blooded_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.WAYSTONE.get()),
                        new ItemStack(WitcheryItems.ENDER_DEW.get()),
                        new ItemStack(Items.SLIME_BALL),
                        new ItemStack(Items.SNOWBALL),
                        new ItemStack(WitcheryItems.ICY_NEEDLE.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "source registers powered Blooded Waystone before its portable no-power variant");
    }

    @Test
    void fullRecipeDispatchMatchesWaystoneTranspositionOnOtherwhereRing() {
        assertEquals("transposition_to_waystone", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.DIRT), boundWaystone()), 2));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.WAYSTONE.get())), 2),
                "blank Waystones do not satisfy the source transposition sacrifice");
    }

    @Test
    void fullRecipeDispatchMatchesSummonRoutesRegardlessSupportOrder() {
        assertEquals("summon_familiar", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(WitcheryItems.ARTHANA.get()),
                        new ItemStack(WitcheryItems.DROP_OF_LUCK.get()),
                        new ItemStack(Items.EGG)), 1));
        assertEquals("summon_witch", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(WitcheryItems.ARTHANA.get()),
                        new ItemStack(Items.MAGMA_CREAM),
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get())), 3));
        assertEquals("summon_imp", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                        new ItemStack(WitcheryItems.INFERNAL_BLOOD.get()),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(WitcheryItems.REFINED_EVIL.get())), 3));
        assertEquals("summon_demon_expensive", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(Items.BLAZE_ROD),
                        new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(WitcheryItems.REFINED_EVIL.get())), 3));
    }

    @Test
    void fullRecipeDispatchRequiresWitherCompanionRing() {
        List<ItemStack> witherOfferings = List.of(
                new ItemStack(Items.WITHER_SKELETON_SKULL),
                new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                new ItemStack(Items.ENDER_PEARL));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE, witherOfferings, 3),
                "Summon Wither needs the paired medium Infernal ring");
        assertEquals("summon_wither", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE, witherOfferings, 3,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, 3))));
    }

    @Test
    void fullRecipeDispatchKeepsSourceOrderForOverlappingSummonSets() {
        assertEquals("summon_demon", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.REFINED_EVIL.get()),
                        new ItemStack(Items.BLAZE_POWDER),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.BLAZE_ROD),
                        new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 3),
                "source registers villager Summon Demon before the expensive item-only recipe");
        assertEquals("summon_wither", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(Items.WITHER_SKELETON_SKULL),
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 3,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, 3))),
                "source registers villager Summon Wither before the expensive item-only recipe");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteSummonSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.EGG),
                        new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(WitcheryItems.ARTHANA.get())), 1),
                "Summon Familiar needs Drop of Luck too");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.MAGMA_CREAM),
                        new ItemStack(WitcheryItems.ARTHANA.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE)), 1),
                "Summon Witch is Infernal, not Ritual");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.REFINED_EVIL.get()),
                        new ItemStack(WitcheryItems.INFERNAL_BLOOD.get()),
                        new ItemStack(Items.ENDER_PEARL)), 3),
                "Summon Imp needs the Attuned Stone");
    }

    @Test
    void fullRecipeDispatchMatchesRedstoneSoupRoutesRegardlessSupportOrder() {
        assertEquals("blight", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(Items.POISONOUS_POTATO)), 3));
        assertEquals("blindness", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.BREW_OF_INK.get()),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(Items.POISONOUS_POTATO),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 3));
        assertEquals("rain_of_toads", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.BELLADONNA.get()),
                        new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(WitcheryItems.TOE_OF_FROG.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 3));
    }

    @Test
    void fullRecipeDispatchKeepsSourceOrderForOverlappingRedstoneSoupSets() {
        assertEquals("blight", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.POISONOUS_POTATO),
                        new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(WitcheryItems.TOE_OF_FROG.get()),
                        new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(WitcheryItems.BELLADONNA.get())), 3),
                "source registers Blight before Rain of Toads when both medium Infernal sets are present");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteRedstoneSoupSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.POISONOUS_POTATO),
                        new ItemStack(Items.ROTTEN_FLESH)), 3),
                "Blight needs the Diamond too");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(WitcheryItems.BREW_OF_INK.get()),
                        new ItemStack(Items.POISONOUS_POTATO),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.DIAMOND)), 3),
                "Blindness uses Spider Eye, not Fermented Spider Eye");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(WitcheryItems.TOE_OF_FROG.get()),
                        new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(WitcheryItems.BELLADONNA.get())), 1),
                "Rain of Toads is Infernal, not Ritual");
    }

    @Test
    void fullRecipeDispatchMatchesTaglockCurseRoutesRegardlessSupportOrder() {
        assertEquals("curse_creature", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get())), 3));
        assertEquals("curse_sinking", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Items.NETHER_WART),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.INK_SAC),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get()),
                        filledTaglock()), 3));
        assertEquals("curse_overheating", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.INFERNAL_BLOOD.get()),
                        new ItemStack(Items.BLAZE_ROD),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get())), 3));
        assertEquals("curse_waking_nightmare", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.TORMENTED_TWINE.get()),
                        new ItemStack(Items.EMERALD),
                        new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get()),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get()),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get())), 3));
    }

    @Test
    void fullRecipeDispatchMatchesTaglockCureRoutesRegardlessSupportOrder() {
        assertEquals("cure_misfortune", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BREAD),
                        new ItemStack(WitcheryItems.BREW_OF_LOVE.get()),
                        filledTaglock(),
                        new ItemStack(Items.GOLD_NUGGET),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get())), 1));
        assertEquals("cure_creature", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.BREW_OF_LOVE.get())), 1));
        assertEquals("cure_insanity", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.BREW_OF_LOVE.get()),
                        new ItemStack(Items.SUGAR),
                        new ItemStack(Items.POTATO),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get())), 1));
        assertEquals("cure_sinking", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BONE_MEAL),
                        new ItemStack(WitcheryItems.BREW_OF_THE_DEPTHS.get()),
                        new ItemStack(Items.NETHER_WART),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get())), 1));
        assertEquals("cure_overheating", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BLAZE_ROD),
                        new ItemStack(WitcheryItems.ICY_NEEDLE.get()),
                        new ItemStack(WitcheryItems.BREW_OF_THE_DEPTHS.get()),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                        filledTaglock()), 1));
        assertEquals("cure_waking_nightmare", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.TORMENTED_TWINE.get()),
                        new ItemStack(Items.GOLDEN_CARROT),
                        new ItemStack(WitcheryItems.BREW_OF_LOVE.get()),
                        filledTaglock(),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get())), 1));
    }

    @Test
    void fullRecipeDispatchKeepsTaglockCureOrderAndSourceIngredientCorrections() {
        assertEquals("cure_misfortune", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(filledTaglock(),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                        new ItemStack(Items.GOLD_NUGGET),
                        new ItemStack(Items.BREAD),
                        new ItemStack(WitcheryItems.BREW_OF_LOVE.get()),
                        new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(Items.GUNPOWDER)), 1),
                "current Breath cure selector checks Misfortune before generic Creature cure");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(filledTaglock(),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get())), 3),
                "source Curse Creature uses Fermented Spider Eye, not Spider Eye");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteTaglockSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get())), 3),
                "taglock curses require a filled Taglock Kit");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.TAGLOCK_KIT.get()),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get())), 3),
                "an empty Taglock Kit does not satisfy source filled-taglock sacrifices");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(filledTaglock(),
                        new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(WitcheryItems.BREW_OF_THE_GROTESQUE.get())), 1),
                "curse routes are Infernal, not Ritual");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(filledTaglock(),
                        new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                        new ItemStack(Items.SPIDER_EYE),
                        new ItemStack(WitcheryItems.BREW_OF_LOVE.get())), 1),
                "cure creature needs Gunpowder too");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteProtectionAndBanishSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.REDSTONE)), 1));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BLAZE_POWDER), new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.OBSIDIAN), new ItemStack(Items.REDSTONE)), 3));
    }

    @Test
    void assignmentIsDistinctAndComplete() {
        int[] picks = BARRIER.assign(List.of(
                new ItemStack(Items.OBSIDIAN), new ItemStack(Items.REDSTONE)));
        assertNotNull(picks);
        assertTrue(picks.length == 2 && picks[0] != picks[1], "each ingredient uses a distinct stack");
        assertNull(BARRIER.assign(List.of(new ItemStack(Items.OBSIDIAN))), "incomplete set → no assignment");
    }
}
