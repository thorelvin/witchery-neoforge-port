package com.emoniph.witchery.entity;

/**
 * Marker for entities that count toward a coven when a rite checks the number of nearby witches.
 * Implemented by the wandering {@link CovenWitchEntity} merchant and by the conjured
 * {@link SpectralWitchEntity}, so a solo caster who has summoned a spectral coven can still trigger
 * coven-gated rites (Beastial Call, Summon Imp, …).
 *
 * @see com.emoniph.witchery.ritual.RitualExecutor#countNearbyCovenWitches
 */
public interface CovenMember {
}
