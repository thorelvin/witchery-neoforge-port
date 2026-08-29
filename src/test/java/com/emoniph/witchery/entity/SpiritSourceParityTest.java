package com.emoniph.witchery.entity;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSourceParityTest {
    @Test
    void naturalSpawnsRequireSourceGrassOrSandFloor() {
        assertTrue(SpiritEntity.isSourceSpawnFloor(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(SpiritEntity.isSourceSpawnFloor(Blocks.SAND.defaultBlockState()));
        assertFalse(SpiritEntity.isSourceSpawnFloor(Blocks.STONE.defaultBlockState()));
        assertFalse(SpiritEntity.isSourceSpawnFloor(Blocks.RED_SAND.defaultBlockState()));
    }

    @Test
    void sourceBaselineRetainsAttackDamageAndBoneFoodPredicate() {
        var attributes = SpiritEntity.createAttributes().build();
        assertTrue(attributes.hasAttribute(Attributes.ATTACK_DAMAGE));
        assertTrue(attributes.hasAttribute(Attributes.TEMPT_RANGE),
                "TemptGoal crashes the server tick when the modern tempt-range attribute is absent");
        assertTrue(SpiritEntity.isSourceFood(new ItemStack(Items.BONE)));
        assertFalse(SpiritEntity.isSourceFood(new ItemStack(Items.WHEAT)));
    }

    @Test
    void sourceParticlesUseTheDefaultGoldenTint() {
        assertEquals(0xFFCC00, SpiritEntity.SOURCE_PARTICLE_COLOR);
        assertEquals(1, SpiritEntity.SOURCE_PARTICLES_PER_CLIENT_TICK);
        assertEquals(0.1D, SpiritEntity.SOURCE_PARTICLE_Y_MIN);
        assertEquals(0.2D, SpiritEntity.SOURCE_PARTICLE_Y_RANGE);
    }
}
