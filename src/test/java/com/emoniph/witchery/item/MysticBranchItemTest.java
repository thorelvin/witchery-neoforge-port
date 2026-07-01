package com.emoniph.witchery.item;

import com.emoniph.witchery.symbol.EffectRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MysticBranchItemTest {

    @Test
    void cooldownKeyUsesKnowledgeKeyWhenPresent() {
        assertEquals("tormentum", MysticBranchItem.cooldownKey(EffectRegistry.TORMENTUM));
        assertEquals("witchery.pott.episkey", MysticBranchItem.cooldownKey(EffectRegistry.EPISKEY));
    }

    @Test
    void tormentumKeepsSourceThirtyMinuteCooldown() {
        assertEquals(36000, EffectRegistry.TORMENTUM.getCooldownTicks());
    }

    @Test
    void enhancedBranchLevelsRequireWorshipLikeSource() {
        assertEquals(1, MysticBranchItem.effectiveSpellLevel(null, 0));
        assertEquals(1, MysticBranchItem.effectiveSpellLevel(null, 1));
        assertEquals(1, MysticBranchItem.effectiveSpellLevel(null, 2));
        assertEquals(1, MysticBranchItem.effectiveSpellLevel(null, 3));

        assertEquals(2, MysticBranchItem.effectiveSpellLevelForWorshipAmplifier(2, 0));
        assertEquals(1, MysticBranchItem.effectiveSpellLevelForWorshipAmplifier(3, 0));
        assertEquals(3, MysticBranchItem.effectiveSpellLevelForWorshipAmplifier(3, 1));
        assertEquals(1, MysticBranchItem.effectiveSpellLevelForWorshipAmplifier(4, 1));
    }

    @Test
    void formatCooldownRoundsUpToPlayerReadableTime() {
        assertEquals("1s", MysticBranchItem.formatCooldown(1L));
        assertEquals("1s", MysticBranchItem.formatCooldown(20L));
        assertEquals("2s", MysticBranchItem.formatCooldown(21L));
        assertEquals("1m", MysticBranchItem.formatCooldown(1200L));
        assertEquals("1m 1s", MysticBranchItem.formatCooldown(1201L));
        assertEquals("30m", MysticBranchItem.formatCooldown(36000L));
    }
}
