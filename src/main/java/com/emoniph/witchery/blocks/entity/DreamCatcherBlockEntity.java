package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DreamCatcherBlockEntity extends BlockEntity {

    private static final String KEY_WEAVE = "WitchWeaveID";

    @Nullable private DreamWeaveType weave;
    private boolean buffPending = false;

    public DreamCatcherBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.DREAM_CATCHER.get(), pos, state);
    }

    // ── Weave access ──────────────────────────────────────────────────────────

    public @Nullable DreamWeaveType getWeave() { return weave; }
    public boolean hasPendingBuff() { return buffPending; }

    public void setWeave(@Nullable DreamWeaveType w) {
        this.weave = w;
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void consumePendingBuff(ServerLevel level) {
        if (!buffPending || weave == null) return;
        applyEffectsToNearby(level);
        buffPending = false;
        setChanged();
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    /**
     * Mirrors BlockDreamCatcher.TileEntityDreamCatcher.updateEntity from 1.7.10:
     * set buffPending when enough players sleep; apply effects when it turns day.
     */
    public static void serverTick(net.minecraft.world.level.Level levelRaw, BlockPos pos,
                                  BlockState state, DreamCatcherBlockEntity be) {
        if (!(levelRaw instanceof ServerLevel level)) return;
        if (be.weave == null) return;

        if (be.buffPending) {
            if ((level.getDayTime() % 24000 < 13000)) {
                be.consumePendingBuff(level);
            }
            return;
        }

        if (areEnoughPlayersAsleep(level)) {
            be.buffPending = true;
        }
    }

    private static boolean areEnoughPlayersAsleep(ServerLevel level) {
        List<net.minecraft.server.level.ServerPlayer> players = level.players();
        if (players.isEmpty()) return false;
        if ((level.getDayTime() % 24000L) < 13000L) return false;

        long sleeping = players.stream().filter(net.minecraft.world.entity.LivingEntity::isSleeping).count();
        int percent = WitcheryConfig.PLAYERS_SLEEPING_PERCENT_FOR_BUFF.get();
        int threshold = Math.max(1, (int) Math.ceil(players.size() * (percent / 100.0)));
        return sleeping >= threshold;
    }

    // ── Effect application ────────────────────────────────────────────────────

    /**
     * Called by tick (buffPending→day transition) and by SleepFinishedTimeEvent.
     * Scans ±5 blocks for nightmare/intensity catchers, then applies weave effect
     * to all players in ±5 block AABB.
     */
    public void applyEffectsToNearby(ServerLevel level) {
        if (weave == null) return;
        BlockPos pos = worldPosition;

        boolean isDream = true;
        boolean isEnhanced = false;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-5, -5, -5), pos.offset(5, 5, 5))) {
            if (scan.equals(pos)) continue;
            if (!level.getBlockState(scan).is(WitcheryBlocks.DREAM_CATCHER.get())) continue;
            if (!(level.getBlockEntity(scan) instanceof DreamCatcherBlockEntity other)) continue;
            if (other.weave == DreamWeaveType.NIGHTMARES) isDream = false;
            else if (other.weave == DreamWeaveType.INTENSITY) isEnhanced = true;
        }

        AABB bounds = new AABB(pos).inflate(5);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            weave.applyEffect(player, isDream, isEnhanced);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (weave != null) tag.putInt(KEY_WEAVE, weave.id);
        tag.putBoolean("BuffPending", buffPending);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        int _weaveId = tag.getIntOr(KEY_WEAVE, -1); weave = _weaveId >= 0 ? DreamWeaveType.fromId(_weaveId) : null;
        buffPending = tag.getBooleanOr("BuffPending", false);
    }

    // ── Network sync (client needs weave for rendering tint) ─────────────────

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
