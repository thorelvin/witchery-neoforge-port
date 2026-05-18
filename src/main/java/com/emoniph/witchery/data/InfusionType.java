package com.emoniph.witchery.data;

public enum InfusionType {
    NONE,
    LIGHT,       // Ghost of the Light — illumination and daytime immunity
    OVERWORLD,   // Soul of the World — nature affinity, plant growth aura
    OTHERWHERE,  // Spirit of Otherwhere — End-like powers, phase/slow fall
    INFERNAL;    // Infernal Animus — fire immunity, darkness, strength

    public static InfusionType fromString(String name) {
        for (InfusionType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return NONE;
    }
}
