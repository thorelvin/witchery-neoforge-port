package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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

    /**
     * Queue this offering for staged consumption — source {@code SacrificeItem} removes sacrifices one
     * per 20 ticks with a pop, not all at once. The actual decrement happens at the pop via
     * {@link #applyConsumption()}.
     */
    void consumeOne() {
        SacrificeConsumer.stage(this);
    }

    /** The real decrement of the backing stack — called by {@link SacrificeConsumer} at the staged pop. */
    void applyConsumption() {
        if (itemEntity != null) {
            RitualExecutor.removeOfferedItemNow(itemEntity);
            return;
        }
        if (grassper != null) {
            ItemStack stack = grassper.getItem().copy();
            stack.shrink(1);
            grassper.setItem(stack);
        }
    }

    Level level() {
        if (itemEntity != null) {
            return itemEntity.level();
        }
        return grassper != null ? grassper.getLevel() : null;
    }

    Vec3 position() {
        if (itemEntity != null) {
            return itemEntity.position();
        }
        return grassper != null ? Vec3.atCenterOf(grassper.getBlockPos()) : null;
    }
}
