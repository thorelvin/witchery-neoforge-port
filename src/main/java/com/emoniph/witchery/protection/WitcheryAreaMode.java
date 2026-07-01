package com.emoniph.witchery.protection;

import java.util.Locale;

public enum WitcheryAreaMode {
    SKIP,
    FAIL;

    public static WitcheryAreaMode parse(String value, WitcheryAreaMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return WitcheryAreaMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
