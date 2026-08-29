package com.emoniph.witchery.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Centralizes which nearby witches may contribute to different kinds of workings. */
public final class CovenContributionCounter {
    private CovenContributionCounter() {
    }

    /**
     * Counts the broad source-rite coven: Coven Witches and conjured Spectral Witches.
     */
    public static int countRitualEligible(Level level, AABB bounds) {
        return level.getEntitiesOfClass(Entity.class, bounds,
                entity -> entity instanceof CovenMember).size();
    }

    /**
     * Counts living Coven Witches physically present and sworn to the specified owner.
     */
    public static int countLivingSworn(Level level, AABB bounds, UUID ownerId) {
        return livingSwornRoster(level, bounds, ownerId).livingCount();
    }

    public static SpecialtyRoster livingSwornRoster(Level level, AABB bounds, UUID ownerId) {
        List<CovenWitchEntity> participants = List.copyOf(level.getEntitiesOfClass(
                CovenWitchEntity.class, bounds,
                witch -> witch.isRecruitedBy(ownerId) && occupies(witch, bounds)));
        EnumSet<CovenSpecialty> specialties = EnumSet.noneOf(CovenSpecialty.class);
        for (CovenWitchEntity witch : participants) {
            specialties.add(witch.getSpecialty());
        }
        return new SpecialtyRoster(participants, specialties);
    }

    private static boolean occupies(Entity entity, AABB bounds) {
        return entity.isAlive() && !entity.isRemoved() && bounds.contains(entity.position());
    }

    public record SpecialtyRoster(List<CovenWitchEntity> participants, Set<CovenSpecialty> specialties) {
        public SpecialtyRoster {
            participants = List.copyOf(participants);
            specialties = Set.copyOf(specialties);
        }

        public int livingCount() {
            return participants.size();
        }

        public boolean has(CovenSpecialty specialty) {
            return specialties.contains(specialty);
        }

        public CovenWitchEntity first(CovenSpecialty specialty) {
            return participants.stream()
                    .filter(witch -> witch.getSpecialty() == specialty)
                    .findFirst()
                    .orElse(null);
        }
    }
}
