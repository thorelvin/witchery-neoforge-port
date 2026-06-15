package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Collects a rite's startup altar-power cost together with its sacrifices — dropped items,
 * grassper-held items, and inventory slots — then spends them as a unit. Power is peeked before
 * anything is consumed, so a no-power result never leaves a half-consumed offering.
 *
 * <p>Extracted verbatim from {@code RitualExecutor} as the first slice of the dispatcher split. The
 * altar-cost and item-consumption primitives still live (package-private) on {@link RitualExecutor}.
 */
final class RitualTransaction {
    private record InventoryConsumption(ServerPlayer player, int slot) {}

    private final ServerLevel level;
    private final BlockPos center;
    private final int altarRadius;
    private final int altarPowerCost;
    private final String noPowerMessageKey;
    private final List<ItemEntity> offerings = new ArrayList<>();
    private final List<RitualOffering> ritualOfferings = new ArrayList<>();
    private final List<InventoryConsumption> inventoryConsumptions = new ArrayList<>();

    RitualTransaction(ServerLevel level, BlockPos center, int altarRadius, int altarPowerCost,
                      String noPowerMessageKey) {
        this.level = level;
        this.center = center;
        this.altarRadius = altarRadius;
        this.altarPowerCost = altarPowerCost;
        this.noPowerMessageKey = noPowerMessageKey;
    }

    RitualTransaction offer(ItemEntity offering) {
        if (offering != null) {
            offerings.add(offering);
        }
        return this;
    }

    RitualTransaction offer(RitualOffering offering) {
        if (offering != null) {
            ritualOfferings.add(offering);
        }
        return this;
    }

    RitualTransaction offerInventory(ServerPlayer player, int slot) {
        inventoryConsumptions.add(new InventoryConsumption(player, slot));
        return this;
    }

    /** Peek whether the altar can pay this rite's startup power, without consuming anything. */
    boolean hasSufficientPower() {
        if (altarPowerCost <= 0) {
            return true;
        }
        AltarBlockEntity altar = RitualExecutor.findNearbyRitualAltar(level, center, altarRadius);
        return altar != null && altar.hasValidStructure()
                && altar.getPower() >= RitualExecutor.scaleRitualPowerCost(altarPowerCost);
    }

    boolean reportNoPower(Player player) {
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable(noPowerMessageKey), false);
        return false;
    }

    /** Actually spend power + offerings. Only called once a rite is committed to succeeding. */
    void consumeAll() {
        if (altarPowerCost > 0) {
            RitualExecutor.consumeNearbyRitualAltarPower(level, center, altarRadius, altarPowerCost);
        }
        for (ItemEntity offering : offerings) {
            RitualExecutor.consumeOfferedItem(offering);
        }
        for (RitualOffering offering : ritualOfferings) {
            offering.consumeOne();
        }
        for (InventoryConsumption inventoryConsumption : inventoryConsumptions) {
            RitualExecutor.consumeInventoryItem(inventoryConsumption.player(), inventoryConsumption.slot());
        }
    }

    /**
     * Validate, then consume. Use for rites whose effect cannot fail after this point.
     * (Power is now peeked before any offering is consumed, so a no-power result never
     * leaves a half-consumed offering.)
     */
    boolean commit(Player player) {
        if (!hasSufficientPower()) {
            return reportNoPower(player);
        }
        consumeAll();
        return true;
    }

    /**
     * Validate, stage all sacrifices, then run {@code effect} only after the sacrifice queue has fully
     * drained. Use for rites whose visible result should start after every offered item has popped.
     */
    boolean commitDeferred(Player player, Runnable effect) {
        if (!hasSufficientPower()) {
            return reportNoPower(player);
        }
        consumeAll();
        SacrificeConsumer.runWhenDrained(level, effect);
        return true;
    }

    /**
     * Rollback-safe commit: validate -> applyEffect -> consume only on success. If {@code effect}
     * returns false (or the altar cannot pay), NOTHING is consumed, so a post-validation effect
     * failure can no longer silently eat altar power or sacrifices. This is the target pattern
     * for any rite whose effect can fail.
     */
    boolean commit(Player player, BooleanSupplier effect) {
        if (!hasSufficientPower()) {
            return reportNoPower(player);
        }
        if (!effect.getAsBoolean()) {
            return false;
        }
        consumeAll();
        return true;
    }
}
