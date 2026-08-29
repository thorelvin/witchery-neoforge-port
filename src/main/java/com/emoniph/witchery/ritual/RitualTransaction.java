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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Collects a rite's startup altar-power cost together with dropped, Grassper-held, and inventory
 * sacrifices. Exact source units are removed into persisted escrow before power is charged; failed
 * effects refund that paid batch exactly once, while successful effects arm it permanently.
 */
final class RitualTransaction {
    private record InventoryConsumption(ServerPlayer player, int slot) {}

    private final ServerLevel level;
    private final BlockPos center;
    private final int altarRadius;
    private final int altarPowerCost;
    private final String noPowerMessageKey;
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
            ritualOfferings.add(RitualOffering.dropped(offering));
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

    /** Validate exact sources, remove them into durable escrow, then charge altar power. */
    private UUID pay() {
        Map<Object, Integer> sourceCounts = new IdentityHashMap<>();
        List<ItemStack> escrow = new ArrayList<>();
        List<BlockPos> visuals = new ArrayList<>();
        for (RitualOffering offering : ritualOfferings) {
            if (offering == null || offering.level() != level || offering.stack().isEmpty()
                    || offering.itemEntity() != null && !offering.itemEntity().isAlive()) {
                return null;
            }
            Object source = offering.itemEntity() != null ? offering.itemEntity() : offering.grassper();
            if (source == null) return null;
            int required = sourceCounts.merge(source, 1, Integer::sum);
            if (offering.stack().getCount() < required) return null;
            escrow.add(offering.stack().copyWithCount(1));
            visuals.add(offering.blockPosition().immutable());
        }
        Map<InventoryConsumption, Integer> inventoryCounts = new java.util.HashMap<>();
        for (InventoryConsumption consumption : inventoryConsumptions) {
            if (consumption.player() == null || consumption.player().level() != level
                    || consumption.slot() < 0
                    || consumption.slot() >= consumption.player().getInventory().getContainerSize()) {
                return null;
            }
            ItemStack stack = consumption.player().getInventory().getItem(consumption.slot());
            int required = inventoryCounts.merge(consumption, 1, Integer::sum);
            if (stack.isEmpty() || stack.getCount() < required) return null;
            escrow.add(stack.copyWithCount(1));
            visuals.add(consumption.player().blockPosition().immutable());
        }

        AltarBlockEntity altar = altarPowerCost <= 0
                ? null : RitualExecutor.findNearbyRitualAltar(level, center, altarRadius);
        int scaledPower = RitualExecutor.scaleRitualPowerCost(altarPowerCost);
        if (scaledPower > 0 && (altar == null || !altar.hasValidStructure() || altar.getPower() < scaledPower)) {
            return null;
        }

        UUID batchId = SacrificeConsumer.prepareBatch(level, center, escrow, visuals);
        for (RitualOffering offering : ritualOfferings) {
            offering.applyConsumption();
        }
        for (InventoryConsumption consumption : inventoryConsumptions) {
            RitualExecutor.consumeInventoryItem(consumption.player(), consumption.slot());
        }
        SacrificeConsumer.markPaid(level, batchId, null, 0);
        if (scaledPower > 0) {
            if (!altar.consumePower(scaledPower)) {
                SacrificeConsumer.refund(level, batchId);
                return null;
            }
            SacrificeConsumer.setChargedPower(level, batchId, altar.getBlockPos(), scaledPower);
        }
        return batchId;
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
        UUID batchId = pay();
        if (batchId == null) return false;
        SacrificeConsumer.arm(level, batchId);
        return true;
    }

    /** Validate and pay atomically, apply the effect, then retain source-paced pops as presentation. */
    boolean commitDeferred(Player player, Runnable effect) {
        if (!hasSufficientPower()) {
            return reportNoPower(player);
        }
        UUID batchId = pay();
        if (batchId == null) return false;
        try {
            effect.run();
        } catch (RuntimeException e) {
            SacrificeConsumer.refund(level, batchId);
            throw e;
        }
        SacrificeConsumer.arm(level, batchId);
        return true;
    }

    /**
     * Rollback-safe commit: validate, escrow sources, charge power, apply the effect, then arm or
     * refund. A false result or exception restores the exact paid batch and altar charge.
     */
    boolean commit(Player player, BooleanSupplier effect) {
        if (!hasSufficientPower()) {
            return reportNoPower(player);
        }
        UUID batchId = pay();
        if (batchId == null) return false;
        boolean applied;
        try {
            applied = effect.getAsBoolean();
        } catch (RuntimeException e) {
            SacrificeConsumer.refund(level, batchId);
            throw e;
        }
        if (!applied) {
            SacrificeConsumer.refund(level, batchId);
            return false;
        }
        SacrificeConsumer.arm(level, batchId);
        return true;
    }
}
