package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.events.VanishingHutEvents;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.world.VanishingHutData;
import com.emoniph.witchery.worldgen.VanishingHut;
import com.emoniph.witchery.worldgen.VanishingHutHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (!be.registered) {
            VanishingHutData.get(serverLevel).upsert(be.hutId, serverLevel, pos);
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
        boolean held = be.heldInPlace || VanishingHut.isHeldBySalt(serverLevel, pos);
        long since = serverLevel.getGameTime() - be.lastMoveGameTime;
        if (VanishingHutHelper.shouldRelocate(anyInside, !be.primed.isEmpty(), watched, held,
                since, VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS)) {
            be.pendingRelocation = true;
            VanishingHutEvents.enqueueRelocation(serverLevel, pos);
        }
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
        return String.format("hut %s: inside=%d primed=%d watched=%b held=%b salted=%b sinceMove=%d/%d pending=%b",
                hutId, inside, primed.size(), watched, heldInPlace || salted, salted, since,
                VanishingHutHelper.DEFAULT_MOVE_COOLDOWN_TICKS, pendingRelocation);
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
