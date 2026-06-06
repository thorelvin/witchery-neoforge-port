package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A single ritual sacrifice source — either a dropped {@link ItemEntity} resting on the circle, or one
 * item held in a {@link GrassperBlockEntity}. Consuming one decrements whichever backing it wraps.
 *
 * <p>Extracted verbatim from {@code RitualExecutor} as the first slice of the dispatcher split; the
 * item-consumption primitive it calls still lives (package-private) on {@link RitualExecutor}.
 */
record RitualOffering(ItemEntity itemEntity, GrassperBlockEntity grassper) {
    static RitualOffering dropped(ItemEntity itemEntity) {
        return new RitualOffering(itemEntity, null);
    }

    static RitualOffering grassper(GrassperBlockEntity grassper) {
        return new RitualOffering(null, grassper);
    }

    void consumeOne() {
        if (itemEntity != null) {
            RitualExecutor.consumeOfferedItem(itemEntity);
            return;
        }
        ItemStack stack = grassper.getItem().copy();
        stack.shrink(1);
        grassper.setItem(stack);
    }
}
