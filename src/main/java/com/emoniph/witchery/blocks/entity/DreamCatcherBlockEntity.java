package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

    private static final String KEY_WEAVE = "WITCWeaveID";
    private static final String LEGACY_PORT_KEY_WEAVE = "WitchWeaveID";
    private static final String LEGACY_PORT_KEY_PENDING = "BuffPending";

    @Nullable private DreamWeaveType weave;
    private boolean buffIfDay = false;
    private boolean buffIfNight = false;

    public DreamCatcherBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.DREAM_CATCHER.get(), pos, state);
    }

    // ── Weave access ──────────────────────────────────────────────────────────

    public @Nullable DreamWeaveType getWeave() { return weave; }
    public boolean hasPendingBuff() { return buffIfDay || buffIfNight; }

    public void setWeave(@Nullable DreamWeaveType w) {
        this.weave = w;
        setChanged();
        syncWeaveVisualState();
    }

    private void syncWeaveVisualState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int visualWeave = weave == null ? 0 : weave.id + 1;
        BlockState state = getBlockState();
        if (state.hasProperty(DreamCatcherBlock.WEAVE) && state.getValue(DreamCatcherBlock.WEAVE) != visualWeave) {
            level.setBlock(worldPosition, state.setValue(DreamCatcherBlock.WEAVE, visualWeave), 3);
        }
    }

    public void consumePendingBuff(ServerLevel level) {
        if (!hasPendingBuff() || weave == null) return;
        boolean day = isDaytime(level);
        if (!((buffIfDay && day) || (buffIfNight && !day))) {
            return;
        }
        applyEffectsToNearby(level, day);
        buffIfDay = false;
        buffIfNight = false;
        setChanged();
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    /**
     * Mirrors BlockDreamCatcher.TileEntityDreamCatcher.updateEntity from 1.7.10:
     * queue day buffs for night sleep and night buffs for vampire/day sleep.
     */
    public static void serverTick(net.minecraft.world.level.Level levelRaw, BlockPos pos,
                                  BlockState state, DreamCatcherBlockEntity be) {
        if (!(levelRaw instanceof ServerLevel level)) return;
        be.syncWeaveVisualState();
        if (be.weave == null) return;

        if (be.hasPendingBuff()) {
            be.consumePendingBuff(level);
        }

        if (!be.hasPendingBuff() && areEnoughPlayersAsleep(level)) {
            boolean day = isDaytime(level);
            be.buffIfDay = !day;
            be.buffIfNight = day;
            be.setChanged();
        }
    }

    private static boolean isDaytime(ServerLevel level) {
        return (level.getDayTime() % 24000L) < 13000L;
    }

    private static boolean areEnoughPlayersAsleep(ServerLevel level) {
        List<net.minecraft.server.level.ServerPlayer> players = level.players();
        if (players.isEmpty()) return false;

        long sleeping = players.stream().filter(net.minecraft.world.entity.LivingEntity::isSleeping).count();
        int percent = WitcheryConfig.PLAYERS_SLEEPING_PERCENT_FOR_BUFF.get();
        int threshold = (int) Math.floor(players.size() * (percent / 100.0));
        return sleeping > 0 && sleeping >= threshold;
    }

    // ── Effect application ────────────────────────────────────────────────────

    /**
     * Called by tick and by SleepFinishedTimeEvent once the queued day/night target is reached.
     * Scans ±5 blocks for nightmare/intensity catchers, then applies weave effects to matching players.
     */
    public void applyEffectsToNearby(ServerLevel level, boolean day) {
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
            boolean vampire = WitcheryAttachments.get(player).isVampire();
            if ((day && vampire) || (!day && !vampire)) {
                continue;
            }
            weave.applyEffect(player, isDream, isEnhanced);
            player.displayClientMessage(Component.translatable(isDream
                    ? "witchery.dream_weaver.wake"
                    : "witchery.dream_weaver.nightmare",
                    Component.translatable(weave.itemTranslationKey()))
                    .withStyle(isDream ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE), true);
            level.sendParticles(isDream ? ParticleTypes.ENCHANT : ParticleTypes.WITCH,
                    player.getX(), player.getY() + player.getBbHeight() * 0.65D, player.getZ(),
                    isDream ? 18 : 12, 0.35D, 0.35D, 0.35D, isDream ? 0.18D : 0.03D);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (weave != null) tag.putInt(KEY_WEAVE, weave.id);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        int _weaveId = tag.getIntOr(KEY_WEAVE, tag.getIntOr(LEGACY_PORT_KEY_WEAVE, -1));
        weave = _weaveId >= 0 ? DreamWeaveType.fromId(_weaveId) : null;
        buffIfDay = tag.getBooleanOr(LEGACY_PORT_KEY_PENDING, false);
        buffIfNight = false;
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
