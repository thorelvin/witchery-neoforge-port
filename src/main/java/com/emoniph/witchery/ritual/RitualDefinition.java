package com.emoniph.witchery.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * The effect a matched rite runs; returns whether it succeeded (as the old {@code riteOf*}
     * booleans). The triggering dropped {@code item} is passed through for the rites that consume it
     * (e.g. the sapling for Rite of the Forest, the taglock for entity transposition); rites that do
     * not need it simply ignore the argument.
     */
    @FunctionalInterface
    public interface Effect {
        boolean run(Player player, Level level, BlockPos center, ItemStack item);
    }

    /** Sentinel for "any circle variant" — the rite does not gate on Ritual/Infernal/Otherwhere. */
    public static final int ANY_VARIANT = -1;

    /**
     * A companion ring that must also surround the same centre for the rite to fire: a ring of
     * {@code size} drawn in the {@code variant} chalk family. Checked by the executor against the
     * world (it needs the level + centre), not by {@link #matches}.
     */
    public record CompanionRing(RitualCircleDetector.CircleSize size, int variant) {
        public static CompanionRing of(RitualCircleDetector.CircleSize size, int variant) {
            return new CompanionRing(size, variant);
        }
    }

    /**
     * A presence check the offered sacrifices around the centre must satisfy for the rite to fire —
     * e.g. "a Glowstone Dust is offered" or "any of the cure offering sets is complete". Needs the
     * world, so it is checked by the executor (not by {@link #matches}). Consuming the offerings still
     * happens inside the effect via {@link RitualTransaction}; this only gates dispatch.
     */
    @FunctionalInterface
    public interface OfferingRequirement {
        boolean present(Level level, BlockPos center);
    }

    /**
     * A world condition that, when true, vetoes the rite — dispatch skips this definition and keeps
     * looking (so the cast falls through to another rite, or to NO_MATCH). Used for the rites that the
     * old chain handled with an inner "return NO_MATCH" guard, e.g. Crystal Ball deferring to the
     * small Seer Stone rite, or large Climate Change deferring to the medium Charge Stone.
     */
    @FunctionalInterface
    public interface Veto {
        boolean vetoes(Level level, BlockPos center);
    }

    private final String name;
    private final RitualCircleDetector.CircleSize ringSize;
    private final Predicate<ItemStack> trigger;
    private final int requiredVariant;
    private final Effect effect;
    private final List<CompanionRing> companionRings;
    private final List<OfferingRequirement> offeringRequirements;
    private final List<Veto> vetoes;

    private RitualDefinition(String name, RitualCircleDetector.CircleSize ringSize,
                             Predicate<ItemStack> trigger, int requiredVariant, Effect effect,
                             List<CompanionRing> companionRings, List<OfferingRequirement> offeringRequirements,
                             List<Veto> vetoes) {
        this.name = name;
        this.ringSize = ringSize;
        this.trigger = trigger;
        this.requiredVariant = requiredVariant;
        this.effect = effect;
        this.companionRings = companionRings;
        this.offeringRequirements = offeringRequirements;
        this.vetoes = vetoes;
    }

    public static RitualDefinition of(String name, RitualCircleDetector.CircleSize ringSize,
                                      Predicate<ItemStack> trigger, int requiredVariant, Effect effect,
                                      CompanionRing... companionRings) {
        return new RitualDefinition(name, ringSize, trigger, requiredVariant, effect,
                List.of(companionRings), List.of(), List.of());
    }

    /** Returns a copy of this definition that also requires the given offering to be present. */
    public RitualDefinition requiringOffering(OfferingRequirement requirement) {
        List<OfferingRequirement> updated = new ArrayList<>(offeringRequirements);
        updated.add(requirement);
        return new RitualDefinition(name, ringSize, trigger, requiredVariant, effect,
                companionRings, List.copyOf(updated), vetoes);
    }

    /** Returns a copy of this definition that does <em>not</em> match while {@code veto} holds. */
    public RitualDefinition vetoedWhen(Veto veto) {
        List<Veto> updated = new ArrayList<>(vetoes);
        updated.add(veto);
        return new RitualDefinition(name, ringSize, trigger, requiredVariant, effect,
                companionRings, offeringRequirements, List.copyOf(updated));
    }

    public String name() {
        return name;
    }

    public RitualCircleDetector.CircleSize ringSize() {
        return ringSize;
    }

    /** Companion rings that must also be present; the executor checks these against the world. */
    public List<CompanionRing> companionRings() {
        return companionRings;
    }

    /** Offering presence checks that must all pass; the executor checks these against the world. */
    public List<OfferingRequirement> offeringRequirements() {
        return offeringRequirements;
    }

    /** Veto conditions; if any holds, the definition does not match. */
    public List<Veto> vetoes() {
        return vetoes;
    }

    /** Does a {@code item} dropped on a {@code size} ring of this {@code circleVariant} trigger this rite? */
    public boolean matches(RitualCircleDetector.CircleSize size, ItemStack item, int circleVariant) {
        return size == ringSize
                && (requiredVariant == ANY_VARIANT || circleVariant == requiredVariant)
                && trigger.test(item);
    }

    public boolean run(Player player, Level level, BlockPos center, ItemStack item) {
        return effect.run(player, level, center, item);
    }
}
