package com.emoniph.witchery.ritual;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void assignmentIsDistinctAndComplete() {
        int[] picks = BARRIER.assign(List.of(
                new ItemStack(Items.OBSIDIAN), new ItemStack(Items.REDSTONE)));
        assertNotNull(picks);
        assertTrue(picks.length == 2 && picks[0] != picks[1], "each ingredient uses a distinct stack");
        assertNull(BARRIER.assign(List.of(new ItemStack(Items.OBSIDIAN))), "incomplete set → no assignment");
    }
}
