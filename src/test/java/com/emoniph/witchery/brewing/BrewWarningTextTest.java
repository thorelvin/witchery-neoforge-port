package com.emoniph.witchery.brewing;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BrewWarningTextTest {

    @Test
    void sanitizesFormattingControlsAndLongMessages() {
        BrewWarningText warning = BrewWarningText.sanitize("  \u00a75Beware\r\n  the  well  ");

        assertEquals("Beware the well", warning.message());
        assertFalse(warning.isEmpty());
    }

    @Test
    void codecRoundTripsSanitizedText() {
        BrewWarningText original = BrewWarningText.sanitize("The door remembers.");

        Tag encoded = BrewWarningText.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        BrewWarningText decoded = BrewWarningText.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
    }
}
