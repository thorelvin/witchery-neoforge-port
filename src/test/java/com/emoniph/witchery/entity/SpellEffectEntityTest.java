package com.emoniph.witchery.entity;

import com.emoniph.witchery.symbol.EffectRegistry;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEffectEntityTest {

    @Test
    void caveInimicumCannotBeDeflected() {
        assertFalse(SpellEffectEntity.isDeflectableEffect(SpellEffectEntity.CAVE_INIMICUM_EFFECT_ID));
        assertTrue(SpellEffectEntity.isDeflectableEffect(EffectRegistry.ACCIO.getEffectId()));
    }

    @Test
    void deflectedVelocityUsesPlayerLookDirection() {
        Vec3 velocity = SpellEffectEntity.deflectedVelocity(new Vec3(0.0D, 0.0D, 2.0D));

        assertEquals(0.0D, velocity.x, 1.0E-6D);
        assertEquals(0.0D, velocity.y, 1.0E-6D);
        assertEquals(1.2D, velocity.z, 1.0E-6D);
    }

    @Test
    void deflectedVelocityHandlesEmptyLookDirection() {
        assertEquals(Vec3.ZERO, SpellEffectEntity.deflectedVelocity(Vec3.ZERO));
    }

    @Test
    void casterCollisionIsDelayedLikeSourceProjectile() {
        assertFalse(SpellEffectEntity.canHitOwnerAtAge(0));
        assertFalse(SpellEffectEntity.canHitOwnerAtAge(24));
        assertTrue(SpellEffectEntity.canHitOwnerAtAge(25));
    }

    @Test
    void trailColorCanBeRestoredFromSavedEffectId() {
        assertEquals(0x513726, SpellEffectEntity.colorForEffect(EffectRegistry.ACCIO.getEffectId()));
        assertEquals(0xB388E8, SpellEffectEntity.colorForEffect(-1));
    }
}
