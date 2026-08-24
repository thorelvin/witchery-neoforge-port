package com.emoniph.witchery.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Circle metadata, exact offerings, and execution action for one source ritual registration. */
record FullRecipeRitualDefinition(String name, RitualCircleDetector.CircleSize size,
                                  int requiredVariant, RitualRecipe recipe,
                                  FullRecipeAction action,
                                  List<RitualDefinition.CompanionRing> companionRings) {

    @FunctionalInterface
    interface FullRecipeAction {
        boolean activate(Player player, Level level, BlockPos center, List<RitualOffering> matchedOfferings);
    }

    static FullRecipeRitualDefinition of(String name, RitualCircleDetector.CircleSize size,
                                         int requiredVariant, RitualRecipe recipe,
                                         FullRecipeAction action,
                                         RitualDefinition.CompanionRing... companionRings) {
        return new FullRecipeRitualDefinition(name, size, requiredVariant, recipe, action,
                List.of(companionRings));
    }

    RitualExecutor.ActivationResult tryActivate(RitualCircleDetector.CircleSize activeSize,
                                                 Player player, Level level, BlockPos center,
                                                 int activeVariant) {
        if (!matchesCircle(activeSize, activeVariant)) return null;
        for (RitualDefinition.CompanionRing ring : companionRings) {
            if (!RitualCircleDetector.hasRing(level, center, ring.size())
                    || RitualCircleDetector.detectVariant(level, center, ring.size()) != ring.variant()) {
                return null;
            }
        }
        Optional<List<RitualOffering>> matchedOfferings = RitualOfferings.matchOfferings(level, center, recipe);
        if (matchedOfferings.isEmpty()) return null;
        return action.activate(player, level, center, matchedOfferings.get())
                ? RitualExecutor.ActivationResult.SUCCESS_NO_CONSUME
                : RitualExecutor.ActivationResult.MATCHED_FAILED;
    }

    boolean matchesStacks(RitualCircleDetector.CircleSize activeSize, List<ItemStack> offeredStacks,
                          int activeVariant, List<RitualDefinition.CompanionRing> presentCompanionRings) {
        return matchesCircle(activeSize, activeVariant)
                && presentCompanionRings.containsAll(companionRings)
                && recipe.matches(offeredStacks);
    }

    private boolean matchesCircle(RitualCircleDetector.CircleSize activeSize, int activeVariant) {
        return size == activeSize
                && (requiredVariant == RitualDefinition.ANY_VARIANT || activeVariant == requiredVariant);
    }
}
