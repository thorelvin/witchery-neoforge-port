package com.emoniph.witchery.entity;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlyingMonkeySourceParityTest {
    @Test
    void sourceAttributesAreRestored() {
        var attributes = FlyingMonkeyEntity.createAttributes().build();
        assertEquals(20.0D, attributes.getBaseValue(Attributes.MAX_HEALTH));
        assertEquals(0.3D, attributes.getBaseValue(Attributes.MOVEMENT_SPEED));
        assertEquals(0.6D, attributes.getBaseValue(Attributes.FLYING_SPEED));
        assertEquals(2.0D, attributes.getBaseValue(Attributes.ATTACK_DAMAGE));
        assertEquals(5.0D, attributes.getBaseValue(Attributes.ARMOR));
    }

    @Test
    void sourceFoodAndExplicitCourierStatesRemainStable() {
        assertTrue(FlyingMonkeyEntity.isSourceFood(new ItemStack(Items.BEEF)));
        assertTrue(FlyingMonkeyEntity.isSourceFood(new ItemStack(Items.PORKCHOP)));
        assertFalse(FlyingMonkeyEntity.isSourceFood(new ItemStack(Items.CHICKEN)));
        assertEquals(5, FlyingMonkeyEntity.CourierState.values().length);
        assertEquals(FlyingMonkeyEntity.CourierState.IDLE,
                FlyingMonkeyEntity.CourierState.valueOf("IDLE"));
        assertEquals(FlyingMonkeyEntity.CourierState.FETCHING,
                FlyingMonkeyEntity.CourierState.valueOf("FETCHING"));
        assertEquals(FlyingMonkeyEntity.CourierState.RETURNING,
                FlyingMonkeyEntity.CourierState.valueOf("RETURNING"));
        assertEquals(FlyingMonkeyEntity.CourierState.WAYSTONE,
                FlyingMonkeyEntity.CourierState.valueOf("WAYSTONE"));
        assertEquals(FlyingMonkeyEntity.CourierState.PREPARING_WAYSTONE,
                FlyingMonkeyEntity.CourierState.valueOf("PREPARING_WAYSTONE"));
    }
}
