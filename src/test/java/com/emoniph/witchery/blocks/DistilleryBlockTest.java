package com.emoniph.witchery.blocks;

import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.core.particles.ParticleTypes;
import org.junit.jupiter.api.Test;

class DistilleryBlockTest {
    @Test
    void activePresentationUsesTheSourceInstantSpellParticle() {
        assertSame(ParticleTypes.INSTANT_EFFECT, DistilleryBlock.sourceActiveParticle().getType());
    }
}
