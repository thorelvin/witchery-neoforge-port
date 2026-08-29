package com.emoniph.witchery.entity;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/** One restrained craft identity carried by a living Coven Witch. */
public enum CovenSpecialty {
    HEDGE_WITCH("hedge_witch", 0x5C8A3D),
    GRAVE_WITCH("grave_witch", 0x70657A),
    PALE_WITCH("pale_witch", 0xA9D8D8),
    STORM_WITCH("storm_witch", 0x657FBD),
    HEARTH_WITCH("hearth_witch", 0xC56B3F);

    private final String id;
    private final int color;

    CovenSpecialty(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("witchery.coven.specialty." + id);
    }

    public Component seerDescription() {
        return Component.translatable("witchery.coven.specialty." + id + ".seer");
    }

    public Component spokenDescription() {
        return Component.translatable("witchery.coven.specialty." + id + ".speech");
    }

    public static CovenSpecialty fromOrdinal(int ordinal) {
        CovenSpecialty[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : HEDGE_WITCH;
    }

    public static CovenSpecialty fromId(String id, UUID identity) {
        if (id != null && !id.isBlank()) {
            String normalized = id.toLowerCase(Locale.ROOT);
            return Arrays.stream(values())
                    .filter(specialty -> specialty.id.equals(normalized))
                    .findFirst()
                    .orElseGet(() -> forIdentity(identity));
        }
        return forIdentity(identity);
    }

    /** Stable assignment avoids rerolling a witch across save/load or dimension movement. */
    public static CovenSpecialty forIdentity(UUID identity) {
        long mixed = identity.getMostSignificantBits() ^ Long.rotateLeft(identity.getLeastSignificantBits(), 17);
        return values()[Math.floorMod(Long.hashCode(mixed), values().length)];
    }
}
