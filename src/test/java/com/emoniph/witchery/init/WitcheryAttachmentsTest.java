package com.emoniph.witchery.init;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WitcheryAttachmentsTest {

    @Test
    void learnedSymbolsTrackAndExpireCooldowns() {
        WitcheryAttachments.LearnedSymbols learned = new WitcheryAttachments.LearnedSymbols();

        assertEquals(0L, learned.cooldownRemaining("tormentum", 100L));

        learned.setCooldownUntil("tormentum", 200L);

        assertEquals(100L, learned.cooldownRemaining("tormentum", 100L));
        assertEquals(1L, learned.cooldownRemaining("tormentum", 199L));
        assertEquals(0L, learned.cooldownRemaining("tormentum", 200L));
        assertFalse(learned.cooldownUntil.containsKey("tormentum"));
    }

    @Test
    void learnedSymbolsCodecPreservesKeysAndCooldowns() {
        WitcheryAttachments.LearnedSymbols learned = new WitcheryAttachments.LearnedSymbols();
        learned.keys.add("tormentum");
        learned.setCooldownUntil("tormentum", 36000L);

        Tag nbt = WitcheryAttachments.LearnedSymbols.CODEC.codec()
                .encodeStart(NbtOps.INSTANCE, learned)
                .getOrThrow();
        WitcheryAttachments.LearnedSymbols decoded = WitcheryAttachments.LearnedSymbols.CODEC.codec()
                .parse(NbtOps.INSTANCE, nbt)
                .getOrThrow();

        assertEquals(1, decoded.keys.size());
        assertEquals(36000L, decoded.cooldownRemaining("tormentum", 0L));
    }
}
