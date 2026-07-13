package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class AreaMarkerBlockEntity extends BlockEntity {
    private static final String KEY_OWNER = "WITCPlacer";
    private static final String KEY_KILLERS = "Killers";
    private static final String KEY_PLAYER_NAME = "PlayerName";
    private static final Set<AreaMarkerBlockEntity> ACTIVE_MARKERS = ConcurrentHashMap.newKeySet();

    private String owner = "";
    private final Set<String> killers = new HashSet<>();

    public AreaMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.AREA_MARKER.get(), pos, state);
    }

    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return !owner.isEmpty() && owner.equals(player.getScoreboardName());
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        registerIfActiveMarker();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        registerIfActiveMarker();
    }

    @Override
    public void setRemoved() {
        ACTIVE_MARKERS.remove(this);
        super.setRemoved();
    }

    public static boolean isDirectedCurseProtected(LivingEntity target) {
        return isProtected(target, ProtectionKind.DIRECTED_CURSE);
    }

    public static boolean isTeleportPullProtected(LivingEntity target) {
        return isProtected(target, ProtectionKind.TELEPORT_PULL);
    }

    public static boolean isRitualEntityTargetProtected(LivingEntity target) {
        return isProtected(target, ProtectionKind.RITUAL_ENTITY_TARGET);
    }

    public static void markKillerNear(ServerPlayer killer) {
        ACTIVE_MARKERS.removeIf(AreaMarkerBlockEntity::isInactiveMarker);
        for (AreaMarkerBlockEntity marker : ACTIVE_MARKERS) {
            if (marker.level == killer.level() && marker.isWithinConfiguredRadius(killer)) {
                marker.addKiller(killer.getScoreboardName());
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString(KEY_OWNER, owner);
        ValueOutput.ValueOutputList killerList = tag.childrenList(KEY_KILLERS);
        for (String killer : killers) {
            ValueOutput child = killerList.addChild();
            child.putString(KEY_PLAYER_NAME, killer);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        owner = tag.getStringOr(KEY_OWNER, "");
        killers.clear();
        for (ValueInput child : tag.childrenListOrEmpty(KEY_KILLERS)) {
            addKillerName(child.getStringOr(KEY_PLAYER_NAME, ""));
        }
    }

    private static boolean isProtected(LivingEntity target, ProtectionKind kind) {
        if (!(target.level() instanceof Level targetLevel) || targetLevel.isClientSide()) {
            return false;
        }
        ACTIVE_MARKERS.removeIf(AreaMarkerBlockEntity::isInactiveMarker);
        for (AreaMarkerBlockEntity marker : ACTIVE_MARKERS) {
            if (marker.protects(target, kind)) {
                return true;
            }
        }
        return false;
    }

    private boolean protects(LivingEntity target, ProtectionKind kind) {
        if (level == null || level != target.level() || isKiller(target) || !isWithinConfiguredRadius(target)) {
            return false;
        }
        BlockState state = getBlockState();
        boolean teleport = state.is(WitcheryBlocks.DECURSE_TELEPORT.get());
        boolean directed = state.is(WitcheryBlocks.DECURSE_DIRECTED.get());
        return switch (kind) {
            case TELEPORT_PULL -> teleport && WitcheryConfig.ALLOW_DECURSE_TELEPORT.get();
            case DIRECTED_CURSE -> directed && WitcheryConfig.ALLOW_DECURSE_DIRECTED.get();
            case RITUAL_ENTITY_TARGET ->
                    (teleport && WitcheryConfig.ALLOW_DECURSE_TELEPORT.get())
                            || (directed && WitcheryConfig.ALLOW_DECURSE_DIRECTED.get());
        };
    }

    private boolean isWithinConfiguredRadius(LivingEntity entity) {
        double radius = configuredRadius();
        if (radius <= 0.0D) {
            return false;
        }
        double dx = entity.getX() - (worldPosition.getX() + 0.5D);
        double dz = entity.getZ() - (worldPosition.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    private int configuredRadius() {
        BlockState state = getBlockState();
        if (state.is(WitcheryBlocks.DECURSE_TELEPORT.get())) {
            return WitcheryConfig.DECURSE_TELEPORT_RADIUS.get();
        }
        if (state.is(WitcheryBlocks.DECURSE_DIRECTED.get())) {
            return WitcheryConfig.DECURSE_DIRECTED_RADIUS.get();
        }
        return 0;
    }

    private void addKiller(String name) {
        if (addKillerName(name)) {
            setChanged();
        }
    }

    private boolean addKillerName(String name) {
        String normalized = normalizeName(name);
        return !normalized.isEmpty() && killers.add(normalized);
    }

    private boolean isKiller(LivingEntity target) {
        return target instanceof Player player && killers.contains(normalizeName(player.getScoreboardName()));
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void registerIfActiveMarker() {
        if (!isInactiveMarker(this)) {
            ACTIVE_MARKERS.add(this);
        }
    }

    private static boolean isInactiveMarker(AreaMarkerBlockEntity marker) {
        return marker.isRemoved()
                || marker.level == null
                || marker.level.isClientSide()
                || !(marker.level.getBlockEntity(marker.worldPosition) instanceof AreaMarkerBlockEntity live)
                || live != marker;
    }

    private enum ProtectionKind {
        DIRECTED_CURSE,
        TELEPORT_PULL,
        RITUAL_ENTITY_TARGET
    }
}
