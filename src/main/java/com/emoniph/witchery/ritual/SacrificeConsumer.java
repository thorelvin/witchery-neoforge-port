package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Durable, independent ritual-payment escrows with source-style delayed presentation. */
final class SacrificeConsumer {
    private static final int POP_INTERVAL = 20;

    private SacrificeConsumer() {
    }

    static UUID prepareBatch(ServerLevel level, BlockPos center, List<ItemStack> escrow,
                             List<BlockPos> visualPositions) {
        UUID id = UUID.randomUUID();
        SacrificeBatch batch = new SacrificeBatch(id, center, escrow, visualPositions,
                level.getGameTime() + POP_INTERVAL);
        RitualSavedData data = RitualSavedData.get(level);
        data.sacrificeBatches.add(batch);
        data.setDirty();
        return id;
    }

    static void markPaid(ServerLevel level, UUID batchId, BlockPos altarPos, int altarPower) {
        SacrificeBatch batch = find(level, batchId);
        if (batch == null || batch.state != SacrificeBatchState.PREPARED) return;
        batch.state = SacrificeBatchState.PAID;
        batch.altarPos = altarPos == null ? null : altarPos.immutable();
        batch.altarPower = Math.max(0, altarPower);
        RitualSavedData.get(level).setDirty();
    }

    static void setChargedPower(ServerLevel level, UUID batchId, BlockPos altarPos, int altarPower) {
        SacrificeBatch batch = find(level, batchId);
        if (batch == null || batch.state != SacrificeBatchState.PAID) return;
        batch.altarPos = altarPos == null ? null : altarPos.immutable();
        batch.altarPower = Math.max(0, altarPower);
        RitualSavedData.get(level).setDirty();
    }

    static void arm(ServerLevel level, UUID batchId) {
        SacrificeBatch batch = find(level, batchId);
        if (batch == null || batch.state != SacrificeBatchState.PAID) return;
        batch.state = SacrificeBatchState.ARMED;
        RitualSavedData data = RitualSavedData.get(level);
        if (batch.visualPositions.isEmpty()) {
            data.sacrificeBatches.remove(batch);
        }
        data.setDirty();
    }

    static void refund(ServerLevel level, UUID batchId) {
        SacrificeBatch batch = find(level, batchId);
        if (batch == null || batch.state == SacrificeBatchState.ARMED) return;
        RitualSavedData data = RitualSavedData.get(level);
        if (batch.state == SacrificeBatchState.PREPARED) {
            // PREPARED contains only copies of located sources. Nothing has been removed yet.
            data.sacrificeBatches.remove(batch);
            data.setDirty();
            return;
        }
        if (applyRefund(level, batch)) {
            data.sacrificeBatches.remove(batch);
        } else {
            batch.state = SacrificeBatchState.REFUNDING;
        }
        data.setDirty();
    }

    /** Standalone trigger/direct offering path: remove now, retain only a persisted visual batch. */
    static void stage(RitualOffering offering) {
        if (offering == null || !(offering.level() instanceof ServerLevel level)) return;
        ItemStack stack = offering.stack();
        if (stack.isEmpty() || offering.itemEntity() != null && !offering.itemEntity().isAlive()) return;
        ItemStack paid = stack.copyWithCount(1);
        BlockPos visual = offering.blockPosition().immutable();
        UUID batchId = prepareBatch(level, visual, List.of(paid), List.of(visual));
        offering.applyConsumption();
        markPaid(level, batchId, null, 0);
        arm(level, batchId);
    }

    /** Source units are removed immediately, so no live world entity remains reserved. */
    static boolean isPending(Level level, ItemEntity item) {
        return false;
    }

    static boolean isPending(Level level, BlockPos grassperPos) {
        return false;
    }

    static boolean hasPending(ServerLevel level) {
        return RitualSavedData.get(level).sacrificeBatches.stream().anyMatch(batch ->
                batch.state == SacrificeBatchState.ARMED
                        && batch.visualStep < batch.visualPositions.size());
    }

    /** Deferred effects are armed immediately; delayed pops are presentation, never a payment barrier. */
    static void runWhenDrained(ServerLevel level, Runnable callback) {
        if (callback == null) return;
        try {
            callback.run();
        } catch (RuntimeException e) {
            WitcheryMod.LOGGER.error("Ritual effect failed after authoritative payment in {}",
                    level.dimension().identifier(), e);
        }
    }

    /** Advances every independent batch by at most one visual step. */
    static void tick(ServerLevel level) {
        RitualSavedData data = RitualSavedData.get(level);
        if (data.sacrificeBatches.isEmpty()) return;
        long now = level.getGameTime();
        boolean dirty = false;
        Iterator<SacrificeBatch> iterator = data.sacrificeBatches.iterator();
        while (iterator.hasNext()) {
            SacrificeBatch batch = iterator.next();
            if (batch.state == SacrificeBatchState.PREPARED) {
                if (!batch.loadedFromDisk) continue;
                // PREPARED means authoritative source removal never completed.
                iterator.remove();
                dirty = true;
                continue;
            }
            if (batch.state == SacrificeBatchState.PAID) {
                if (!batch.loadedFromDisk) continue;
                if (applyRefund(level, batch)) {
                    iterator.remove();
                } else {
                    batch.state = SacrificeBatchState.REFUNDING;
                }
                dirty = true;
                continue;
            }
            if (batch.state == SacrificeBatchState.REFUNDING) {
                if (refundPower(level, batch)) {
                    iterator.remove();
                }
                dirty = true;
                continue;
            }
            if (now < batch.nextVisualTick) continue;
            if (batch.visualStep < batch.visualPositions.size()) {
                popVisual(level, batch.visualPositions.get(batch.visualStep));
                batch.visualStep++;
                batch.nextVisualTick = now + POP_INTERVAL;
                dirty = true;
            }
            if (batch.visualStep >= batch.visualPositions.size()) {
                iterator.remove();
                dirty = true;
            }
        }
        if (dirty) data.setDirty();
    }

    /** Preserve ARMED animations for restart; recover any unarmed payment as a refund. */
    static void flush(ServerLevel level) {
        RitualSavedData data = RitualSavedData.get(level);
        for (SacrificeBatch batch : new ArrayList<>(data.sacrificeBatches)) {
            if (batch.state == SacrificeBatchState.PAID) {
                if (applyRefund(level, batch)) {
                    data.sacrificeBatches.remove(batch);
                } else {
                    batch.state = SacrificeBatchState.REFUNDING;
                }
            } else if (batch.state == SacrificeBatchState.PREPARED) {
                data.sacrificeBatches.remove(batch);
            }
        }
        data.setDirty();
    }

    private static boolean applyRefund(ServerLevel level, SacrificeBatch batch) {
        for (ItemStack stack : batch.escrow) {
            if (!stack.isEmpty()) {
                ItemEntity item = new ItemEntity(level, batch.center.getX() + 0.5D,
                        batch.center.getY() + 1.0D, batch.center.getZ() + 0.5D, stack.copy());
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
        }
        batch.escrow.clear();
        return refundPower(level, batch);
    }

    private static boolean refundPower(ServerLevel level, SacrificeBatch batch) {
        if (batch.altarPower <= 0) return true;
        if (batch.altarPos != null
                && level.getBlockEntity(batch.altarPos) instanceof AltarBlockEntity altar) {
            altar.refundPower(batch.altarPower);
            batch.altarPower = 0;
            return true;
        }
        AltarBlockEntity nearby = RitualExecutor.findNearbyRitualAltar(level, batch.center, 16);
        if (nearby == null) return false;
        nearby.refundPower(batch.altarPower);
        batch.altarPower = 0;
        return true;
    }

    private static SacrificeBatch find(ServerLevel level, UUID id) {
        for (SacrificeBatch batch : RitualSavedData.get(level).sacrificeBatches) {
            if (batch.batchId.equals(id)) return batch;
        }
        return null;
    }

    private static void popVisual(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.7F);
    }
}
