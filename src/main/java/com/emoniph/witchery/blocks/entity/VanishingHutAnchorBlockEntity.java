package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.events.VanishingHutEvents;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.world.VanishingHutData;
import com.emoniph.witchery.worldgen.VanishingHut;
import com.emoniph.witchery.worldgen.VanishingHutHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The "brain" of a Vanishing Witch Hut. Tracks which players have been inside, watches for the
 * Weeping-Angel relocation trigger (nobody inside, a primed player has left, nobody looking), and
 * hands a relocation job to {@link VanishingHutEvents}. See {@link VanishingHutHelper} for the
 * pure decision logic.
 */
public class VanishingHutAnchorBlockEntity extends BlockEntity {

    private static final String KEY_HUT_MOST = "HutMost";
    private static final String KEY_HUT_LEAST = "HutLeast";
    private static final String KEY_LAST_MOVE = "LastMove";
    private static final String KEY_HELD = "HeldInPlace";
    private static final String KEY_RESIDENT_MOST = "ResidentMost";
    private static final String KEY_RESIDENT_LEAST = "ResidentLeast";

    private static final int CHECK_INTERVAL = 10;

    private UUID hutId = UUID.randomUUID();
    private long lastMoveGameTime;
    private boolean heldInPlace;
    @org.jetbrains.annotations.Nullable
    private UUID residentUuid;

    /** Players who have stepped inside since the hut last settled. Transient by design. */
    private final Set<UUID> primed = new HashSet<>();
    private boolean registered;
    private boolean pendingRelocation;

    public VanishingHutAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.VANISHING_HUT_ANCHOR.get(), pos, state);
    }

    public UUID hutId() {
        return hutId;
    }

    public void setHutId(UUID hutId) {
        this.hutId = hutId;
        setChanged();
    }

    public void markSettled(long gameTime) {
        this.lastMoveGameTime = gameTime;
        this.primed.clear();
        this.pendingRelocation = false;
        setChanged();
    }

    public boolean isHeldInPlace() {
        return heldInPlace;
    }

    public void setHeldInPlace(boolean held) {
        this.heldInPlace = held;
        setChanged();
    }

    @org.jetbrains.annotations.Nullable
    public UUID residentUuid() {
        return residentUuid;
    }

    public void setResident(@org.jetbrains.annotations.Nullable UUID uuid) {
        this.residentUuid = uuid;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VanishingHutAnchorBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        VanishingHutData data = VanishingHutData.get(serverLevel);
        if (!be.registered) {
            data.upsert(be.hutId, serverLevel, pos, be.residentUuid);
            be.registered = true;
        }
        if (be.pendingRelocation || serverLevel.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }

        AABB interior = VanishingHut.interiorAabb(pos);
        List<ServerPlayer> inside = serverLevel.getEntitiesOfClass(ServerPlayer.class, interior,
                p -> !p.isSpectator());
        boolean anyInside = !inside.isEmpty();
        for (ServerPlayer player : inside) {
            be.primed.add(player.getUUID());
        }

        boolean watched = VanishingHut.isWatched(serverLevel, pos);
        boolean salted = VanishingHut.isHeldBySalt(serverLevel, pos);
        VanishingHutData.Entry entry = updateRestraint(serverLevel, pos, be, data, salted);
        boolean escapeReady = salted && entry.saltHeldSince() >= 0L
                && serverLevel.getGameTime() - entry.saltHeldSince() >= VanishingHutHelper.SALT_ESCAPE_TICKS;
        boolean held = be.heldInPlace || (salted && !escapeReady);
        boolean departureReady = !be.primed.isEmpty() || entry.bargainCompleted() || escapeReady;
        long cooldown = entry.bargainCompleted()
                ? VanishingHutHelper.COMPLETED_BARGAIN_COOLDOWN_TICKS
                : VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS;
        long since = serverLevel.getGameTime() - be.lastMoveGameTime;
        if (VanishingHutHelper.shouldRelocate(anyInside, departureReady, watched, held, since, cooldown)) {
            be.pendingRelocation = true;
            VanishingHutEvents.enqueueRelocation(serverLevel, pos);
        }
    }

    private static VanishingHutData.Entry updateRestraint(ServerLevel level, BlockPos pos,
                                                           VanishingHutAnchorBlockEntity be,
                                                           VanishingHutData data, boolean salted) {
        VanishingHutData.Entry entry = data.get(be.hutId).orElseGet(() -> {
            data.upsert(be.hutId, level, pos, be.residentUuid);
            return data.get(be.hutId).orElseThrow();
        });
        if (!salted) {
            return data.clearContinuousSalt(be.hutId).orElse(entry);
        }

        entry = data.observeSalt(be.hutId, level.getGameTime()).orElse(entry);
        long heldFor = level.getGameTime() - entry.saltHeldSince();
        if (heldFor >= VanishingHutHelper.SALT_WARNING_TICKS && entry.restraintStage() < 2) {
            entry = data.setRestraintStage(be.hutId, 2).orElse(entry);
            announceSaltWarning(level, pos, be.residentUuid);
        } else if (entry.restraintStage() >= 2 && level.getGameTime() % 40L == 0L) {
            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, VanishingHut.groundY(pos) + 1.0D, pos.getZ() - 2.5D,
                    3, 0.7D, 0.3D, 0.2D, 0.01D);
        }
        return entry;
    }

    private static void announceSaltWarning(ServerLevel level, BlockPos pos, UUID residentUuid) {
        if (residentUuid != null && level.getEntity(residentUuid) instanceof com.emoniph.witchery.entity.CovenWitchEntity witch) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    witch.getX(), witch.getY() + witch.getBbHeight(), witch.getZ(),
                    10, 0.35D, 0.35D, 0.35D, 0.0D);
        }
        Component warning = Component.translatable("witchery.vanishing_hut.salt.warning");
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 24.0D * 24.0D) {
                player.displayClientMessage(warning, false);
            }
        }
        level.playSound(null, pos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 0.9F, 0.7F);
    }

    /** QA: force the hut to relocate on the next tick, bypassing the observation/cooldown gate. */
    public void forceRelocate(ServerLevel level) {
        if (!pendingRelocation) {
            pendingRelocation = true;
            VanishingHutEvents.enqueueRelocation(level, worldPosition);
        }
    }

    /** QA readout of the current trigger state. */
    public String debugState(ServerLevel level) {
        AABB interior = VanishingHut.interiorAabb(worldPosition);
        int inside = level.getEntitiesOfClass(ServerPlayer.class, interior, p -> !p.isSpectator()).size();
        boolean watched = VanishingHut.isWatched(level, worldPosition);
        boolean salted = VanishingHut.isHeldBySalt(level, worldPosition);
        long since = level.getGameTime() - lastMoveGameTime;
        VanishingHutData.Entry entry = VanishingHutData.get(level).get(hutId).orElse(null);
        int cycle = entry == null ? -1 : entry.bargainCycle();
        String bargain = entry == null ? "unknown"
                : entry.bargainCompleted() ? "completed"
                : entry.restraintStage() > 0 ? "forfeited" : "open";
        int restraint = entry == null ? 0 : entry.restraintStage();
        return String.format("hut %s: inside=%d primed=%d watched=%b held=%b salted=%b "
                        + "cycle=%d bargain=%s restraint=%d sinceMove=%d/%d pending=%b",
                hutId, inside, primed.size(), watched, heldInPlace || salted, salted,
                cycle, bargain, restraint, since, VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS,
                pendingRelocation);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putLong(KEY_HUT_MOST, hutId.getMostSignificantBits());
        tag.putLong(KEY_HUT_LEAST, hutId.getLeastSignificantBits());
        tag.putLong(KEY_LAST_MOVE, lastMoveGameTime);
        tag.putBoolean(KEY_HELD, heldInPlace);
        if (residentUuid != null) {
            tag.putLong(KEY_RESIDENT_MOST, residentUuid.getMostSignificantBits());
            tag.putLong(KEY_RESIDENT_LEAST, residentUuid.getLeastSignificantBits());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        long most = tag.getLongOr(KEY_HUT_MOST, 0L);
        long least = tag.getLongOr(KEY_HUT_LEAST, 0L);
        if (most != 0L || least != 0L) {
            hutId = new UUID(most, least);
        }
        lastMoveGameTime = tag.getLongOr(KEY_LAST_MOVE, 0L);
        heldInPlace = tag.getBooleanOr(KEY_HELD, false);
        long rMost = tag.getLongOr(KEY_RESIDENT_MOST, 0L);
        long rLeast = tag.getLongOr(KEY_RESIDENT_LEAST, 0L);
        residentUuid = (rMost != 0L || rLeast != 0L) ? new UUID(rMost, rLeast) : null;
    }
}
