package com.emoniph.witchery.ritual;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
    void overlappingPredicatesUseExactAssignmentInsteadOfGreedySelection() {
        RitualRecipe overlapping = RitualRecipe.named("overlapping")
                .require("Stone or dirt", stack -> stack.is(Items.STONE) || stack.is(Items.DIRT))
                .require("Stone or sand A", stack -> stack.is(Items.STONE) || stack.is(Items.SAND))
                .require("Stone or sand B", stack -> stack.is(Items.STONE) || stack.is(Items.SAND))
                .build();

        assertTrue(overlapping.matches(List.of(
                new ItemStack(Items.STONE),
                new ItemStack(Items.DIRT),
                new ItemStack(Items.SAND))));
        int[] assignment = overlapping.assign(List.of(
                new ItemStack(Items.STONE),
                new ItemStack(Items.DIRT),
                new ItemStack(Items.SAND)));
        assertNotNull(assignment);
        assertEquals(1, assignment[0], "the flexible first ingredient must move to dirt");
        assertTrue(assignment[1] == 0 || assignment[2] == 0, "one later ingredient must receive stone");
    }

    @Test
    void optionalOfferingDoesNotGateOrDisplaceRequiredOfferings() {
        RitualRecipe recipe = RitualRecipe.named("optional")
                .require("Required redstone", stack -> stack.is(Items.REDSTONE))
                .optional("Optional redstone", stack -> stack.is(Items.REDSTONE))
                .build();

        int[] absent = recipe.assign(List.of(new ItemStack(Items.REDSTONE)));
        assertNotNull(absent);
        assertEquals(0, absent[0]);
        assertEquals(-1, absent[1]);

        int[] present = recipe.assign(List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE)));
        assertNotNull(present);
        assertEquals(0, present[0]);
        assertEquals(1, present[1]);
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
    void fullRecipeDispatchMatchesTotalEclipseRegardlessSupportOrder() {
        assertEquals("total_eclipse", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(Items.STONE_AXE)), 1));
        assertEquals("total_eclipse_portable", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(Items.IRON_AXE)), 1));
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteTotalEclipseSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.STONE_AXE)), 1),
                "ordinary Eclipse needs Quicklime");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.IRON_AXE),
                        new ItemStack(WitcheryItems.QUICKLIME.get())), 1),
                "portable Eclipse needs the Charged Attuned Stone");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.STONE_AXE),
                        new ItemStack(WitcheryItems.QUICKLIME.get())), 3),
                "Eclipse is Ritual chalk, not Infernal");
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
    void fullRecipeDispatchMatchesFertilityRoutesRegardlessSupportOrder() {
        assertEquals("fertility", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.GYPSUM.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(Items.BONE_MEAL),
                        new ItemStack(WitcheryItems.MUTANDIS.get()),
                        new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()),
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get())), 1));
        assertEquals("portable_fertility", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                portableFertilityOfferings(), 1));
    }

    @Test
    void fullRecipeDispatchKeepsPortableFertilityBeforePlainWhenBothSetsArePresent() {
        List<ItemStack> portable = portableFertilityOfferings();
        assertEquals("portable_fertility", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.MUTANDIS.get()),
                        portable.get(0),
                        portable.get(1),
                        portable.get(2),
                        portable.get(3),
                        portable.get(4),
                        portable.get(5),
                        portable.get(6)), 1),
                "the port's existing Bone Meal dispatcher tries the portable no-power branch first");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteFertilitySets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BONE_MEAL),
                        new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()),
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(WitcheryItems.GYPSUM.get())), 1),
                "plain Fertility needs Mutandis");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.BONE_MEAL),
                        new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()),
                        new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                        new ItemStack(WitcheryItems.QUICKLIME.get()),
                        new ItemStack(WitcheryItems.GYPSUM.get()),
                        new ItemStack(WitcheryItems.MUTANDIS_EXTREMIS.get())), 1),
                "portable Fertility needs the Charged Attuned Stone");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                portableFertilityOfferings(), 2),
                "Fertility is Ritual chalk, not Otherwhere");
    }

    private static List<ItemStack> portableFertilityOfferings() {
        return List.of(new ItemStack(Items.BONE_MEAL),
                new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()),
                new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                new ItemStack(WitcheryItems.QUICKLIME.get()),
                new ItemStack(WitcheryItems.GYPSUM.get()),
                new ItemStack(WitcheryItems.MUTANDIS_EXTREMIS.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
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
                RitualCircleDetector.CircleSize.MEDIUM,
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
                        new ItemStack(Items.GLISTERING_MELON_SLICE)), 3));
        assertEquals("blight_heart", RitualExecutor.matchingFullRecipeDefinitionName(
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
    void blightAndBlightHeartUseDistinctMelonOfferings() {
        ItemStack charged = new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get());
        ItemStack soup = new ItemStack(WitcheryItems.REDSTONE_SOUP.get());
        ItemStack reek = new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get());
        ItemStack eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        assertEveryRequiredOfferingMatters(RitualOfferings.BLIGHT_RECIPE,
                charged, soup, reek, eye, new ItemStack(Items.GLISTERING_MELON_SLICE), flesh, diamond);
        assertEveryRequiredOfferingMatters(RitualOfferings.BLIGHT_HEART_RECIPE,
                charged, soup, reek, eye, new ItemStack(Items.POISONOUS_POTATO), flesh, diamond);
        assertFalse(RitualOfferings.BLIGHT_RECIPE.matches(List.of(
                charged, soup, reek, eye, new ItemStack(Items.POISONOUS_POTATO), flesh, diamond)));
        assertFalse(RitualOfferings.BLIGHT_HEART_RECIPE.matches(List.of(
                charged, soup, reek, eye, new ItemStack(Items.GLISTERING_MELON_SLICE), flesh, diamond)));
    }

    @Test
    void fullRecipeDispatchKeepsSourceOrderForOverlappingRedstoneSoupSets() {
        assertEquals("blight", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get()),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.GLISTERING_MELON_SLICE),
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
                        new ItemStack(Items.GLISTERING_MELON_SLICE),
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
    void fullRecipeDispatchMatchesTerrainAndNatureRoutesRegardlessSupportOrder() {
        assertEquals("volcano_charged", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GOLDEN_SWORD),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(Items.MAGMA_CREAM),
                        new ItemStack(Items.STONE)), 3));
        assertEquals("volcano_powered", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GOLDEN_SWORD),
                        boundWaystone(),
                        new ItemStack(Items.COBBLESTONE),
                        new ItemStack(Items.MAGMA_CREAM)), 3),
                "source powered Earth's Wrath allows an optional Bound Waystone relocation");
        assertEquals("natures_power", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Blocks.BIRCH_SAPLING.asItem()),
                        new ItemStack(WitcheryItems.ROWAN_SAPLING.get()),
                        new ItemStack(Blocks.SPRUCE_SAPLING.asItem()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()),
                        new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get()),
                        new ItemStack(WitcheryItems.HAWTHORN_SAPLING.get()),
                        new ItemStack(Blocks.OAK_SAPLING.asItem()),
                        new ItemStack(WitcheryItems.ALDER_SAPLING.get())), 1));
    }

    @Test
    void fullRecipeDispatchMatchesAllForestationSaplingVariants() {
        assertEquals("forestation_oak", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(Blocks.OAK_SAPLING.asItem())), 1));
        assertEquals("forestation_spruce", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(Blocks.SPRUCE_SAPLING.asItem())), 1));
        assertEquals("forestation_birch", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(Blocks.BIRCH_SAPLING.asItem())), 1));
        assertEquals("forestation_jungle", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(Blocks.JUNGLE_SAPLING.asItem())), 1));
        assertEquals("forestation_rowan", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(WitcheryItems.ROWAN_SAPLING.get())), 1));
        assertEquals("forestation_alder", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(WitcheryItems.ALDER_SAPLING.get())), 1));
        assertEquals("forestation_hawthorn", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                forestationOfferings(new ItemStack(WitcheryItems.HAWTHORN_SAPLING.get())), 1));
    }

    @Test
    void fullRecipeDispatchKeepsForestationSourceOrderWhenMultipleSaplingsArePresent() {
        List<ItemStack> offerings = forestationOfferings(new ItemStack(WitcheryItems.ROWAN_SAPLING.get()));
        assertEquals("forestation_oak", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Blocks.OAK_SAPLING.asItem()),
                        offerings.get(0),
                        offerings.get(1),
                        offerings.get(2),
                        offerings.get(3)), 1),
                "source registers vanilla oak Forestation before the Witchery tree variants");
    }

    @Test
    void fullRecipeDispatchRejectsIncompleteTerrainAndNatureSets() {
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.COBBLESTONE),
                        new ItemStack(Items.MAGMA_CREAM)), 3),
                "Earth's Wrath needs the Golden Sword too");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(Items.GOLDEN_SWORD),
                        new ItemStack(Items.STONE),
                        new ItemStack(Items.MAGMA_CREAM),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "Earth's Wrath is Infernal, not Ritual");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get()),
                        new ItemStack(WitcheryItems.ROWAN_SAPLING.get()),
                        new ItemStack(WitcheryItems.ALDER_SAPLING.get()),
                        new ItemStack(WitcheryItems.HAWTHORN_SAPLING.get()),
                        new ItemStack(Blocks.OAK_SAPLING.asItem()),
                        new ItemStack(Blocks.SPRUCE_SAPLING.asItem()),
                        new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())), 1),
                "Nature's Power needs Birch too");
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.MEDIUM,
                List.of(new ItemStack(Blocks.OAK_SAPLING.asItem()),
                        new ItemStack(WitcheryItems.WICKER_BUNDLE.get()),
                        new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get())), 1),
                "Forestation needs the Ent Twig too");
    }

    private static List<ItemStack> forestationOfferings(ItemStack sapling) {
        return List.of(sapling,
                new ItemStack(WitcheryItems.WICKER_BUNDLE.get()),
                new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get()),
                new ItemStack(WitcheryItems.ENT_TWIG.get()));
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
                RitualCircleDetector.CircleSize.MEDIUM,
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
    void convertedSingleAndSmallOfferingRecipesRequireTheirExactSourceSets() {
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSION_RECHARGE_RECIPE,
                PotionContents.createItemStack(Items.POTION, Potions.REGENERATION));
        assertEveryRequiredOfferingMatters(RitualOfferings.BROKEN_EARTH_RECIPE,
                new ItemStack(WitcheryItems.BREW_OF_EROSION.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_LIGHT_RECIPE,
                new ItemStack(WitcheryItems.GHOST_OF_THE_LIGHT.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_OVERWORLD_RECIPE,
                new ItemStack(WitcheryItems.SOUL_OF_THE_WORLD.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_OTHERWHERE_RECIPE,
                new ItemStack(WitcheryItems.SPIRIT_OF_OTHERWHERE.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_INFERNAL_RECIPE,
                new ItemStack(WitcheryItems.INFERNAL_ANIMUS.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.ENCHANT_BROOM_RECIPE,
                new ItemStack(WitcheryItems.BROOM.get()),
                new ItemStack(WitcheryItems.FLYING_OINTMENT.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.MYSTIC_BRANCH_RECIPE,
                new ItemStack(WitcheryItems.ENT_TWIG.get()),
                new ItemStack(WitcheryItems.MYSTIC_UNGUENT.get()));
    }

    @Test
    void convertedTerrainRecipesRequireTheirExactSourceFoci() {
        ItemStack sprouting = new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get());
        ItemStack cactus = new ItemStack(Blocks.CACTUS.asItem());
        assertEveryRequiredOfferingMatters(RitualOfferings.RAISE_COLUMN_SMALL_RECIPE,
                sprouting, cactus, new ItemStack(Items.GUNPOWDER));
        assertEveryRequiredOfferingMatters(RitualOfferings.RAISE_COLUMN_MEDIUM_RECIPE,
                sprouting, cactus, new ItemStack(Items.REDSTONE));
        assertEveryRequiredOfferingMatters(RitualOfferings.RAISE_COLUMN_LARGE_RECIPE,
                sprouting, cactus, new ItemStack(Items.GLOWSTONE_DUST));
        assertEveryRequiredOfferingMatters(RitualOfferings.TRANSPOSE_ORES_RECIPE,
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
    }

    @Test
    void convertedLivingTargetRecipesRequireTheirExactSourceSets() {
        assertEveryRequiredOfferingMatters(RitualOfferings.TRANSPOSITION_ENTITY_RECIPE,
                new ItemStack(WitcheryItems.WAYSTONE.get()), filledTaglock(),
                new ItemStack(WitcheryItems.ENDER_DEW.get()), new ItemStack(Items.IRON_AXE));
        assertEveryRequiredOfferingMatters(RitualOfferings.BIND_FAMILIAR_RECIPE,
                new ItemStack(WitcheryItems.TEAR_OF_GODDESS.get()),
                new ItemStack(WitcheryItems.ODOUR_OF_PURITY.get()),
                new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()),
                new ItemStack(Items.DIAMOND), new ItemStack(WitcheryItems.INFERNAL_BLOOD.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.CALL_FAMILIAR_RECIPE,
                new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                new ItemStack(WitcheryItems.HINT_OF_REBIRTH.get()),
                new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.CORRUPT_POPPET_RECIPE,
                filledTaglock(), new ItemStack(WitcheryItems.EXHALE_OF_HORNED_ONE.get()),
                new ItemStack(WitcheryItems.VOODOO_PROTECTION_POPPET.get()),
                new ItemStack(Items.BLAZE_POWDER), new ItemStack(WitcheryItems.SPECTRAL_DUST.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.REMOVE_VAMPIRISM_RECIPE,
                filledTaglock(), new ItemStack(WitcheryItems.BREATH_OF_GODDESS.get()),
                new ItemStack(WitcheryItems.SILVER_SWORD.get()), new ItemStack(WitcheryItems.GARLIC.get()),
                new ItemStack(Items.DIAMOND), new ItemStack(WitcheryItems.BREW_OF_LOVE.get()));
    }

    @Test
    void convertedLateGameRecipesRequireTheirExactSourceSets() {
        assertEveryRequiredOfferingMatters(RitualOfferings.HELL_ON_EARTH_RECIPE,
                new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                new ItemStack(WitcheryItems.DEMON_HEART.get()),
                new ItemStack(WitcheryItems.WAYSTONE.get()), new ItemStack(Items.NETHER_STAR));
        assertTrue(RitualOfferings.HELL_ON_EARTH_RECIPE.matches(List.of(
                new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                new ItemStack(WitcheryItems.DEMON_HEART.get()),
                new ItemStack(WitcheryItems.WAYSTONE.get()), new ItemStack(Items.NETHER_STAR), boundWaystone())));
        assertEveryRequiredOfferingMatters(RitualOfferings.PRIOR_INCARNATION_RECIPE,
                new ItemStack(WitcheryItems.NECRO_STONE.get()),
                new ItemStack(WitcheryItems.TONGUE_OF_DOG.get()), new ItemStack(Items.BONE),
                new ItemStack(WitcheryItems.SPECTRAL_DUST.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.ICE_SHELL_RECIPE,
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(WitcheryItems.FROZEN_HEART.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.MANIFESTATION_DREAM_RECIPE,
                new ItemStack(WitcheryItems.SPECTRAL_DUST.get()),
                new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get()),
                new ItemStack(WitcheryItems.NECRO_STONE.get()), new ItemStack(Items.GOLDEN_PICKAXE),
                new ItemStack(WitcheryItems.ARTHANA.get()), new ItemStack(Items.GUNPOWDER));
    }

    @Test
    void convertedDuplicateRegistrationsRemainDistinctExactRecipes() {
        assertEveryRequiredOfferingMatters(RitualOfferings.COOK_ITEM_RECIPE,
                new ItemStack(Items.SUGAR), new ItemStack(WitcheryItems.WOOD_ASH.get()),
                new ItemStack(Items.COAL));
        assertEveryRequiredOfferingMatters(RitualOfferings.COOK_ITEM_CHARGED_RECIPE,
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()), new ItemStack(Items.SUGAR),
                new ItemStack(WitcheryItems.WOOD_ASH.get()), new ItemStack(Items.BLAZE_POWDER));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_BREW_SOARING_RECIPE,
                new ItemStack(WitcheryItems.INFUSED_BREW_BASE.get()), new ItemStack(WitcheryItems.BROOM.get()),
                new ItemStack(Items.FEATHER), new ItemStack(WitcheryItems.ARTHANA.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.INFUSE_BREW_GRAVE_RECIPE,
                new ItemStack(WitcheryItems.INFUSED_BREW_BASE.get()), new ItemStack(Items.BONE),
                new ItemStack(WitcheryItems.DENSE_WEB.get()), new ItemStack(WitcheryItems.NECRO_STONE.get()));
        assertFetishRecipe(RitualOfferings.BIND_SCARECROW_RECIPE, WitcheryItems.FETISH_SCARECROW.get());
        assertFetishRecipe(RitualOfferings.BIND_TRENT_EFFIGY_RECIPE, WitcheryItems.FETISH_TRENT_EFFIGY.get());
        assertFetishRecipe(RitualOfferings.BIND_WITCHS_LADDER_RECIPE, WitcheryItems.FETISH_WITCHS_LADDER.get());
    }

    @Test
    void convertedWideAreaRecipesRequireEverySourceOffering() {
        assertEveryRequiredOfferingMatters(RitualOfferings.CLIMATE_CHANGE_RECIPE,
                new ItemStack(Items.SPIDER_EYE), new ItemStack(WitcheryItems.TOE_OF_FROG.get()),
                new ItemStack(WitcheryItems.BAT_WOOL.get()), new ItemStack(WitcheryItems.TONGUE_OF_DOG.get()),
                new ItemStack(WitcheryItems.OWLETS_WING.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        assertEveryRequiredOfferingMatters(RitualOfferings.BEASTIAL_CALL_RECIPE,
                new ItemStack(Items.MILK_BUCKET), new ItemStack(Blocks.HAY_BLOCK.asItem()),
                new ItemStack(Items.APPLE), new ItemStack(Items.BEEF), new ItemStack(Items.COD),
                new ItemStack(Blocks.RED_MUSHROOM.asItem()), new ItemStack(Items.CARROT),
                new ItemStack(Items.WHEAT_SEEDS));
        assertEveryRequiredOfferingMatters(RitualOfferings.BIND_SPECTRAL_STONE_RECIPE,
                new ItemStack(WitcheryItems.SPECTRAL_STONE.get()),
                new ItemStack(WitcheryItems.SPECTRAL_DUST.get()), new ItemStack(WitcheryItems.BOLINE.get()));
    }

    @Test
    void convertedCircleMetadataMatchesSourceRegistrations() {
        assertEquals("broken_earth", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.BREW_OF_EROSION.get())), 1));
        assertNull(RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.BREW_OF_EROSION.get())), 1));
        assertEquals("raise_column_small", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.SMALL,
                List.of(new ItemStack(WitcheryItems.BREW_OF_SPROUTING.get()),
                        new ItemStack(Blocks.CACTUS.asItem()), new ItemStack(Items.GUNPOWDER)), 1));
        assertEquals("hell_on_earth", RitualExecutor.matchingFullRecipeDefinitionName(
                RitualCircleDetector.CircleSize.LARGE,
                List.of(new ItemStack(WitcheryItems.REDSTONE_SOUP.get()),
                        new ItemStack(WitcheryItems.DEMON_HEART.get()),
                        new ItemStack(WitcheryItems.WAYSTONE.get()), new ItemStack(Items.NETHER_STAR)), 3,
                List.of(RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, 2),
                        RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, 3))));
    }

    private static void assertFetishRecipe(RitualRecipe recipe, net.minecraft.world.item.Item fetish) {
        assertEveryRequiredOfferingMatters(recipe,
                new ItemStack(fetish), new ItemStack(WitcheryItems.ATTUNED_STONE.get()),
                new ItemStack(WitcheryItems.NECRO_STONE.get()), new ItemStack(WitcheryItems.BOLINE.get()));
    }

    private static void assertEveryRequiredOfferingMatters(RitualRecipe recipe, ItemStack... stacks) {
        List<ItemStack> offerings = List.of(stacks);
        assertTrue(recipe.matches(offerings), recipe.name() + " must match its complete source offering set");
        for (int omitted = 0; omitted < offerings.size(); omitted++) {
            List<ItemStack> incomplete = new ArrayList<>(offerings);
            incomplete.remove(omitted);
            assertFalse(recipe.matches(incomplete),
                    recipe.name() + " must reject the set without offering " + omitted);
        }
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
