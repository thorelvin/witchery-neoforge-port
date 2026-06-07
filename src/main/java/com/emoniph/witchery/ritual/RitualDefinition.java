package com.emoniph.witchery.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * One declaratively-dispatched rite: the trigger item and ring requirements that select it, paired
 * with the effect to run. {@code RitualExecutor} consults a table of these ("find the matching
 * definition, run it") instead of an ordered {@code if}/{@code else if} chain — the first step toward
 * the data-driven ritual registry described in {@code docs/status/NEXT_FIXES_PLAN.md} (architecture #1).
 *
 * <p>This first cut covers the simple rites whose dispatch condition is just <em>trigger item
 * (+ optional ring variant)</em>; their offerings and altar power are still validated inside the
 * effect via {@link RitualTransaction}. Offering and companion-ring requirements will move onto the
 * definition itself in a later slice, at which point more of the dispatch chains can be migrated.
 *
 * <p>Behaviour parity is preserved because every migrated rite's trigger item is unique within its
 * ring's dispatch — so matching definitions first yields exactly the rite the old chain would have.
 */
public final class RitualDefinition {

    /** The effect a matched rite runs; returns whether it succeeded (as the old {@code riteOf*} booleans). */
    @FunctionalInterface
    public interface Effect {
        boolean run(Player player, Level level, BlockPos center);
    }

    /** Sentinel for "any circle variant" — the rite does not gate on Ritual/Infernal/Otherwhere. */
    public static final int ANY_VARIANT = -1;

    private final String name;
    private final RitualCircleDetector.CircleSize ringSize;
    private final Predicate<ItemStack> trigger;
    private final int requiredVariant;
    private final Effect effect;

    private RitualDefinition(String name, RitualCircleDetector.CircleSize ringSize,
                             Predicate<ItemStack> trigger, int requiredVariant, Effect effect) {
        this.name = name;
        this.ringSize = ringSize;
        this.trigger = trigger;
        this.requiredVariant = requiredVariant;
        this.effect = effect;
    }

    public static RitualDefinition of(String name, RitualCircleDetector.CircleSize ringSize,
                                      Predicate<ItemStack> trigger, int requiredVariant, Effect effect) {
        return new RitualDefinition(name, ringSize, trigger, requiredVariant, effect);
    }

    public String name() {
        return name;
    }

    public RitualCircleDetector.CircleSize ringSize() {
        return ringSize;
    }

    /** Does a {@code item} dropped on a {@code size} ring of this {@code circleVariant} trigger this rite? */
    public boolean matches(RitualCircleDetector.CircleSize size, ItemStack item, int circleVariant) {
        return size == ringSize
                && (requiredVariant == ANY_VARIANT || circleVariant == requiredVariant)
                && trigger.test(item);
    }

    public boolean run(Player player, Level level, BlockPos center) {
        return effect.run(player, level, center);
    }
}
