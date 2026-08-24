package com.emoniph.witchery.ritual;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the representative-trigger table that remains for intentional post-1.7.10 rites. */
class RitualDefinitionTest {

    private static final int RITUAL = 1;
    private static final int OTHERWHERE = 2;
    private static final int INFERNAL = 3;

    private static String match(RitualCircleDetector.CircleSize size, ItemStack item, int variant) {
        return RitualExecutor.matchingRitualDefinitionName(size, item, variant);
    }

    @Test
    void modernOnlyRepresentativeRoutesRemainAvailable() {
        assertEquals("whiff_of_magic_familiar", match(RitualCircleDetector.CircleSize.MEDIUM,
                new ItemStack(WitcheryItems.WHIFF_OF_MAGIC.get()), RITUAL));
        assertEquals("bind_creaking_familiar", match(RitualCircleDetector.CircleSize.MEDIUM,
                new ItemStack(Blocks.CREAKING_HEART.asItem()), RITUAL));
        assertEquals("gathered_lives", match(RitualCircleDetector.CircleSize.MEDIUM,
                new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get()), OTHERWHERE));
        assertEquals("moving_earth", match(RitualCircleDetector.CircleSize.LARGE,
                new ItemStack(Items.DIRT), RITUAL));
        assertEquals("volcano", match(RitualCircleDetector.CircleSize.LARGE,
                new ItemStack(Items.LAVA_BUCKET), INFERNAL));
    }

    @Test
    void sourceRecipeNamesCannotFallBackToRepresentativeTriggers() {
        assertNull(match(RitualCircleDetector.CircleSize.SMALL,
                new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), INFERNAL));
        assertNull(match(RitualCircleDetector.CircleSize.SMALL,
                new ItemStack(Items.FEATHER), RITUAL));
        assertNull(match(RitualCircleDetector.CircleSize.MEDIUM,
                new ItemStack(WitcheryItems.TEAR_OF_GODDESS.get()), RITUAL));
        assertNull(match(RitualCircleDetector.CircleSize.LARGE,
                new ItemStack(WitcheryItems.TORMENTED_TWINE.get()), INFERNAL));
    }

    @Test
    void modernOnlyRosterIsLockedAndDisjointFromSourceRecipes() {
        List<String> names = RitualExecutor.allRitualDefinitionNames();
        assertEquals(Set.of(
                "breath_of_goddess_cure",
                "protection_barrier_charged",
                "whiff_of_magic_familiar",
                "bind_creaking_familiar",
                "the_forest",
                "gathered_lives",
                "moving_earth",
                "volcano",
                "summoning_lord_of_torment"), Set.copyOf(names));
        assertEquals(names.size(), Set.copyOf(names).size(), "definition names must be unique");
    }

    @Test
    void infusionRiteUsesSourcePowerAndRadius() {
        assertEquals(2000, RitualExecutor.infusionStartupCost(InfusionType.LIGHT));
        assertEquals(4000, RitualExecutor.infusionStartupCost(InfusionType.OVERWORLD));
        assertEquals(4000, RitualExecutor.infusionStartupCost(InfusionType.OTHERWHERE));
        assertEquals(4000, RitualExecutor.infusionStartupCost(InfusionType.INFERNAL));
        assertEquals(0, RitualExecutor.infusionStartupCost(InfusionType.NONE));

        BlockPos center = new BlockPos(10, 64, -5);
        assertTrue(RitualExecutor.isInsideInfusionRiteRadius(new Vec3(14.0D, 64.0D, -5.0D), center, 4));
        assertTrue(RitualExecutor.isInsideInfusionRiteRadius(new Vec3(10.5D, 64.0D, -4.5D), center, 4));
        assertFalse(RitualExecutor.isInsideInfusionRiteRadius(new Vec3(14.1D, 64.0D, -5.0D), center, 4));

        assertTrue(RitualExecutor.shouldGrantInfusionAfterOrdeal(true, 0.2F));
        assertFalse(RitualExecutor.shouldGrantInfusionAfterOrdeal(true, 0.1F));
        assertFalse(RitualExecutor.shouldGrantInfusionAfterOrdeal(false, 20.0F));
    }
}
