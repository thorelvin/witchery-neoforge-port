package com.emoniph.witchery.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class FlameImpSourceParityTest {
    @Test
    void shinyAffectionTableMatchesEntityImp() {
        assertEquals(21, FlameImpEntity.sourceShinyCount());
        assertEquals(8, FlameImpEntity.sourceShinyValue(Items.DIAMOND));
        assertEquals(24, FlameImpEntity.sourceShinyValue(Items.DIAMOND_AXE));
        assertEquals(16, FlameImpEntity.sourceShinyValue(Items.DIAMOND_HOE));
        assertEquals(16, FlameImpEntity.sourceShinyValue(Items.DIAMOND_SWORD));
        assertEquals(8, FlameImpEntity.sourceShinyValue(Items.DIAMOND_SHOVEL));
        assertEquals(24, FlameImpEntity.sourceShinyValue(Items.DIAMOND_PICKAXE));
        assertEquals(3, FlameImpEntity.sourceShinyValue(Items.EMERALD));
        assertEquals(1, FlameImpEntity.sourceShinyValue(Items.GOLD_INGOT));
        assertEquals(16, FlameImpEntity.sourceShinyValue(Items.NETHER_STAR));
        assertEquals(1, FlameImpEntity.sourceShinyValue(Items.BLAZE_ROD));
        assertEquals(4, FlameImpEntity.sourceShinyValue(Items.GHAST_TEAR));
        assertEquals(3, FlameImpEntity.sourceShinyValue(Items.GOLDEN_AXE));
        assertEquals(2, FlameImpEntity.sourceShinyValue(Items.GOLDEN_SWORD));
        assertEquals(2, FlameImpEntity.sourceShinyValue(Items.GOLDEN_HOE));
        assertEquals(1, FlameImpEntity.sourceShinyValue(Items.GOLDEN_SHOVEL));
        assertEquals(3, FlameImpEntity.sourceShinyValue(Items.GOLDEN_PICKAXE));
        assertEquals(9, FlameImpEntity.sourceShinyValue(Items.GOLD_BLOCK));
        assertEquals(27, FlameImpEntity.sourceShinyValue(Items.EMERALD_BLOCK));
        assertEquals(72, FlameImpEntity.sourceShinyValue(Items.DIAMOND_BLOCK));
        assertEquals(7, FlameImpEntity.sourceShinyValue(Items.LAPIS_BLOCK));
        assertEquals(5, FlameImpEntity.sourceShinyValue(Items.REDSTONE_BLOCK));
    }

    @Test
    void nonSourceModernShiniesAreRejected() {
        assertEquals(0, FlameImpEntity.sourceShinyValue(Items.AMETHYST_SHARD));
        assertEquals(0, FlameImpEntity.sourceShinyValue(Items.PRISMARINE_CRYSTALS));
        assertEquals(0, FlameImpEntity.sourceShinyValue(Items.HEART_OF_THE_SEA));
        assertEquals(0, FlameImpEntity.sourceShinyValue(Items.GOLD_NUGGET));
    }

    @Test
    void accumulatedAffectionImprovesTheSourceGiftRoll() {
        assertEquals(10, FlameImpEntity.sourceGiftRollBound(20));
        assertEquals(9, FlameImpEntity.sourceGiftRollBound(21));
        assertEquals(2, FlameImpEntity.sourceGiftRollBound(28));
        assertEquals(1, FlameImpEntity.sourceGiftRollBound(29));
        assertEquals(1, FlameImpEntity.sourceGiftRollBound(100));
    }

    @Test
    void lifecycleAndCombatConstantsMatchEntityImp() {
        assertEquals(20, FlameImpEntity.REWARD_AFFECTION_LEVEL);
        assertEquals(3_600, FlameImpEntity.GIFT_DELAY_TICKS);
        assertEquals(6_000, FlameImpEntity.AFFECTION_DECAY_INTERVAL_TICKS);
        assertEquals(72_000, FlameImpEntity.CONTRACT_ABANDON_AGE_TICKS);
        assertEquals(400, FlameImpEntity.PASSIVE_HEAL_INTERVAL_TICKS);
        assertEquals(20, FlameImpEntity.POWERED_HEAL_INTERVAL_TICKS);
        assertEquals(72_000L, FlameImpEntity.POWER_DURATION_TICKS);
        assertEquals(16.0D, FlameImpEntity.HOME_RANGE);
        assertEquals(15.0F, FlameImpEntity.NORMAL_DAMAGE_CAP);
        assertEquals(5.0F, FlameImpEntity.POWERED_DAMAGE_CAP);
        assertEquals(4.0F, FlameImpEntity.NORMAL_ATTACK_DAMAGE);
        assertEquals(8.0F, FlameImpEntity.POWERED_ATTACK_DAMAGE);
        assertEquals(100, FlameImpEntity.sourceDemonNameCount());
    }
}
