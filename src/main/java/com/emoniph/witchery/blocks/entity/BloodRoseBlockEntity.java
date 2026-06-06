package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.BloodRoseBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

import java.util.List;
import java.util.UUID;

public class BloodRoseBlockEntity extends BlockEntity {

    private static final String KEY_UUID = "StoredUUID";
    private static final String KEY_NAME = "StoredName";
    private static final String SOURCE_PLAYERS_KEY = "WITCPlayers";
    private static final String SOURCE_PLAYER_KEY = "Player";

    @Nullable
    private UUID storedUuid;
    private String storedName = "";

    public BloodRoseBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BLOOD_ROSE.get(), pos, state);
    }

    public boolean hasStoredPlayer() {
        return !storedName.isEmpty();
    }

    public void storePlayer(Player player) {
        UUID uuid = player.getUUID();
        String name = player.getName().getString();
        if (uuid.equals(storedUuid) && name.equals(storedName)) {
            return;
        }
        storedUuid = uuid;
        storedName = name;
        sync(true);
    }

    public DrainResult drainStoredPlayerExcept(Player usingPlayer) {
        return drainStoredPlayer(usingPlayer, false);
    }

    public DrainResult drainStoredPlayer(Player usingPlayer, boolean allowOwnTaglock) {
        if (level == null || level.isClientSide() || usingPlayer == null || !hasStoredPlayer()) {
            return DrainResult.none();
        }

        UUID usingUuid = usingPlayer.getUUID();
        String usingName = usingPlayer.getName().getString();
        boolean ownStoredSample = (storedUuid != null && storedUuid.equals(usingUuid))
                || storedName.equalsIgnoreCase(usingName);
        if (ownStoredSample && !allowOwnTaglock) {
            return DrainResult.onlyOwn();
        }
        if (ownStoredSample) {
            StoredPlayer result = new StoredPlayer(usingUuid, usingName);
            storedUuid = null;
            storedName = "";
            sync(false);
            return DrainResult.success(result);
        }

        ServerPlayer onlinePlayer = findOnlineStoredPlayer();
        if (onlinePlayer != null && onlinePlayer.isAlive()) {
            StoredPlayer result = new StoredPlayer(onlinePlayer.getUUID(), onlinePlayer.getName().getString());
            storedUuid = null;
            storedName = "";
            sync(false);
            return DrainResult.success(result);
        }

        return DrainResult.missingPlayers(List.of(storedName));
    }

    @Nullable
    private ServerPlayer findOnlineStoredPlayer() {
        if (level == null || level.getServer() == null || storedName.isEmpty()) {
            return null;
        }
        ServerPlayer player = storedUuid == null
                ? null
                : level.getServer().getPlayerList().getPlayer(storedUuid);
        return player != null ? player : level.getServer().getPlayerList().getPlayerByName(storedName);
    }

    private void sync(boolean filled) {
        setChanged();
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BloodRoseBlock.FULL) && state.getValue(BloodRoseBlock.FULL) != filled) {
            level.setBlock(worldPosition, state.setValue(BloodRoseBlock.FULL, filled), 3);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, 3);
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
        if (storedUuid != null) {
            tag.putString(KEY_UUID, storedUuid.toString());
        }
        if (!storedName.isEmpty()) {
            tag.putString(KEY_NAME, storedName);
        }
        saveSourcePlayer(tag);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        storedUuid = null;
        String uuidString = tag.getStringOr(KEY_UUID, "");
        if (!uuidString.isEmpty()) {
            try {
                storedUuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ignored) {
                storedUuid = null;
            }
        }
        storedName = tag.getStringOr(KEY_NAME, "");
        if (storedName.isEmpty()) {
            loadSourcePlayer(tag);
        }
    }

    private void saveSourcePlayer(ValueOutput tag) {
        if (storedName.isEmpty()) {
            return;
        }
        ValueOutput.ValueOutputList players = tag.childrenList(SOURCE_PLAYERS_KEY);
        ValueOutput player = players.addChild();
        player.putString(SOURCE_PLAYER_KEY, storedName);
    }

    private void loadSourcePlayer(ValueInput tag) {
        for (ValueInput player : tag.childrenListOrEmpty(SOURCE_PLAYERS_KEY)) {
            String name = player.getStringOr(SOURCE_PLAYER_KEY, "");
            if (!name.isBlank()) {
                storedName = name;
                storedUuid = null;
                return;
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
