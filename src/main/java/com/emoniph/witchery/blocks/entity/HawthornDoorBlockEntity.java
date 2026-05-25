package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HawthornDoorBlockEntity extends BlockEntity {
    private static final int MAX_STORED_PLAYERS = 3;

    private final List<StoredPlayer> players = new ArrayList<>();

    public HawthornDoorBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.HAWTHORN_DOOR.get(), pos, state);
    }

    public void storePlayer(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }
        UUID uuid = player.getUUID();
        for (StoredPlayer stored : players) {
            if (stored.uuid().equals(uuid)) {
                return;
            }
        }
        players.add(new StoredPlayer(uuid, player.getName().getString()));
        while (players.size() > MAX_STORED_PLAYERS) {
            players.removeFirst();
        }
        sync();
    }

    public DrainResult drainStoredPlayerExcept(Player usingPlayer) {
        if (level == null || level.isClientSide() || usingPlayer == null || players.isEmpty()) {
            return DrainResult.none();
        }

        UUID usingUuid = usingPlayer.getUUID();
        List<String> missingPlayers = new ArrayList<>();
        boolean sawOnlySelf = true;
        for (int i = players.size() - 1; i >= 0; i--) {
            StoredPlayer stored = players.get(i);
            if (stored.uuid().equals(usingUuid) || stored.name().equalsIgnoreCase(usingPlayer.getName().getString())) {
                continue;
            }

            sawOnlySelf = false;
            ServerPlayer onlinePlayer = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(stored.uuid());
            if (onlinePlayer != null && onlinePlayer.isAlive()) {
                players.remove(i);
                sync();
                return DrainResult.success(new StoredPlayer(onlinePlayer.getUUID(), onlinePlayer.getName().getString()));
            }
            missingPlayers.add(stored.name());
        }

        if (!missingPlayers.isEmpty()) {
            return DrainResult.missingPlayers(missingPlayers);
        }
        return sawOnlySelf ? DrainResult.onlyOwn() : DrainResult.none();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("PlayerCount", players.size());
        for (int i = 0; i < players.size(); i++) {
            StoredPlayer player = players.get(i);
            tag.putString("Player" + i + "UUID", player.uuid().toString());
            tag.putString("Player" + i + "Name", player.name());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        players.clear();
        int count = Math.min(tag.getIntOr("PlayerCount", 0), MAX_STORED_PLAYERS);
        for (int i = 0; i < count; i++) {
            String uuidString = tag.getStringOr("Player" + i + "UUID", "");
            String name = tag.getStringOr("Player" + i + "Name", "");
            if (uuidString.isEmpty() || name.isEmpty()) {
                continue;
            }
            try {
                players.add(new StoredPlayer(UUID.fromString(uuidString), name));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed legacy/save data.
            }
        }
    }

    public record StoredPlayer(UUID uuid, String name) {}

    public record DrainResult(@Nullable StoredPlayer player, boolean onlyOwnTaglock, List<String> missingPlayers) {
        public static DrainResult success(StoredPlayer player) {
            return new DrainResult(player, false, List.of());
        }

        public static DrainResult onlyOwn() {
            return new DrainResult(null, true, List.of());
        }

        public static DrainResult missingPlayers(List<String> names) {
            return new DrainResult(null, false, List.copyOf(names));
        }

        public static DrainResult none() {
            return new DrainResult(null, false, List.of());
        }
    }
}
