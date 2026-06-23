package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeechChestBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int SLOTS = 27;
    private static final int MAX_RECENT_PLAYERS = 3;
    private static final String KEY_CUSTOM_NAME = "CustomName";
    private static final String KEY_STORED_PLAYER_COUNT = "StoredPlayerCount";
    private static final String KEY_STORED_PLAYER_UUID = "StoredPlayerUuid";
    private static final String KEY_STORED_PLAYER_NAME = "StoredPlayerName";
    private static final String SOURCE_PLAYERS_KEY = "WITCPlayers";
    private static final String SOURCE_PLAYER_KEY = "Player";
    private static final UUID UNKNOWN_PLAYER_UUID = new UUID(0L, 0L);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final List<UUID> recentPlayerUuids = new ArrayList<>();
    private final List<String> recentPlayerNames = new ArrayList<>();
    private String customName = "";
    private int openCount = 0;
    private int openTimer = 0;

    // Client-side animation state — openCount is synced from server via handleUpdateTag
    public float lidAngle = 0f;
    public float oLidAngle = 0f;

    public float getOpenNess(float partialTick) {
        return Mth.lerp(partialTick, oLidAngle, lidAngle);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LeechChestBlockEntity be) {
        float previous = be.lidAngle;
        be.oLidAngle = previous;
        if (be.openCount > 0 && previous == 0.0f) {
            playSourceChestSound(level, pos, SoundEvents.CHEST_OPEN);
        }
        if (be.openCount > 0) {
            be.lidAngle = Math.min(1.0f, be.lidAngle + 0.1f);
        } else {
            be.lidAngle = Math.max(0.0f, be.lidAngle - 0.1f);
            if (be.lidAngle < 0.5f && previous >= 0.5f) {
                playSourceChestSound(level, pos, SoundEvents.CHEST_CLOSE);
            }
        }
    }

    private static void playSourceChestSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound,
                SoundSource.BLOCKS,
                0.5f,
                level.random.nextFloat() * 0.1f + 0.9f,
                false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeechChestBlockEntity be) {
        if (be.openTimer > 0) {
            be.openTimer--;
            if (be.openTimer == 0) {
                be.updateOpenCount(0);
            }
        }
    }

    public LeechChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.LEECH_CHEST.get(), pos, state);
    }

    public void setCustomName(String customName) {
        this.customName = customName == null ? "" : customName;
        sync();
    }

    public boolean storePlayer(Player player) {
        if (level == null || level.isClientSide() || player == null) {
            return false;
        }
        UUID uuid = player.getUUID();
        String name = player.getName().getString();
        if (hasStoredPlayer(uuid, name)) {
            return false;
        }

        addStoredPlayer(uuid, name);
        sync();
        playStoredFeedback();
        return true;
    }

    public LeechChestDrainResult drainStoredPlayerExcept(Player usingPlayer) {
        if (level == null || level.isClientSide() || usingPlayer == null) {
            return LeechChestDrainResult.none();
        }

        List<String> missingPlayers = new ArrayList<>();
        UUID usingUuid = usingPlayer.getUUID();
        String usingName = usingPlayer.getName().getString();
        int storedCount = storedPlayerCount();

        for (int i = storedCount - 1; i >= 0; i--) {
            UUID storedUuid = recentPlayerUuids.get(i);
            String storedName = recentPlayerNames.get(i);
            if (storedUuid.equals(usingUuid) || storedName.equalsIgnoreCase(usingName)) {
                if (storedCount == 1) {
                    return LeechChestDrainResult.onlyOwn();
                }
                continue;
            }

            ServerPlayer onlinePlayer = findOnlinePlayer(storedUuid, storedName);
            if (onlinePlayer != null && onlinePlayer.isAlive()) {
                removeStoredPlayer(i);
                sync();
                playDrainFeedback();
                return LeechChestDrainResult.success(onlinePlayer.getUUID(), onlinePlayer.getName().getString());
            }

            missingPlayers.add(storedName);
        }

        if (!missingPlayers.isEmpty()) {
            return LeechChestDrainResult.missingPlayers(missingPlayers);
        }
        return LeechChestDrainResult.none();
    }

    private boolean hasStoredPlayer(UUID uuid, String name) {
        int storedCount = storedPlayerCount();
        for (int i = 0; i < storedCount; i++) {
            UUID storedUuid = recentPlayerUuids.get(i);
            String storedName = recentPlayerNames.get(i);
            if (!UNKNOWN_PLAYER_UUID.equals(storedUuid) && storedUuid.equals(uuid)) {
                return true;
            }
            if (storedName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ServerPlayer findOnlinePlayer(UUID storedUuid, String storedName) {
        if (level == null || level.getServer() == null) {
            return null;
        }
        ServerPlayer player = UNKNOWN_PLAYER_UUID.equals(storedUuid)
                ? null
                : level.getServer().getPlayerList().getPlayer(storedUuid);
        return player != null ? player : level.getServer().getPlayerList().getPlayerByName(storedName);
    }

    public CompoundTag createDropData() {
        CompoundTag tag = new CompoundTag();
        if (!customName.isEmpty()) {
            tag.putString(KEY_CUSTOM_NAME, customName);
        }
        saveStoredPlayersToTag(tag);
        return tag;
    }

    public void loadDropData(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        customName = tag.getStringOr(KEY_CUSTOM_NAME, customName);
        loadStoredPlayersFromTag(tag);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void playStoredFeedback() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS, 0.25f, 0.55f);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.75D, worldPosition.getZ() + 0.5D,
                4, 0.18D, 0.12D, 0.18D, 0.0D);
    }

    private void playDrainFeedback() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.SILVERFISH_HURT,
                SoundSource.BLOCKS, 0.35f, 0.75f);
        serverLevel.sendParticles(ParticleTypes.POOF,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D, worldPosition.getZ() + 0.5D,
                8, 0.22D, 0.18D, 0.22D, 0.01D);
    }

    public int getOpenCount() {
        return openCount;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.replaceAll(stack -> ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public void startOpen(Player player) {
        if (isRemoved() || player.isSpectator()) return;
        openTimer = 200;
        updateOpenCount(Math.max(1, openCount));
    }

    public void stopOpen(Player player) {
        if (isRemoved() || player.isSpectator()) return;
        openTimer = 0;
        updateOpenCount(Math.max(0, openCount - 1));
    }

    private void updateOpenCount(int newOpenCount) {
        openCount = Math.max(0, newOpenCount);
        if (level == null || level.isClientSide()) return;
        BlockState blockState = getBlockState();
        level.updateNeighborsAt(worldPosition, blockState.getBlock());
        level.updateNeighborsAt(worldPosition.below(), blockState.getBlock());
        sync(); // sends AnimOpen to clients via getUpdatePacket / getUpdateTag
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("AnimOpen", openCount > 0 ? 1 : 0);
        if (!customName.isEmpty()) {
            tag.putString(KEY_CUSTOM_NAME, customName);
        }
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty()
                ? Component.translatable("container.witchery.leech_chest")
                : Component.literal(customName);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (!customName.isEmpty()) {
            tag.putString(KEY_CUSTOM_NAME, customName);
        }
        saveStoredPlayers(tag);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        customName = tag.getStringOr(KEY_CUSTOM_NAME, "");
        loadStoredPlayers(tag);
        openCount = tag.getIntOr("AnimOpen", 0);
    }

    private void saveStoredPlayers(net.minecraft.world.level.storage.ValueOutput tag) {
        int storedCount = storedPlayerCount();
        tag.putInt(KEY_STORED_PLAYER_COUNT, storedCount);
        for (int i = 0; i < storedCount; i++) {
            tag.putString(KEY_STORED_PLAYER_UUID + i, recentPlayerUuids.get(i).toString());
            tag.putString(KEY_STORED_PLAYER_NAME + i, recentPlayerNames.get(i));
        }
    }

    private void loadStoredPlayers(net.minecraft.world.level.storage.ValueInput tag) {
        clearStoredPlayers();

        int storedCount = tag.getIntOr(KEY_STORED_PLAYER_COUNT, 0);
        for (int i = 0; i < storedCount; i++) {
            String uuidString = tag.getStringOr(KEY_STORED_PLAYER_UUID + i, "");
            String name = tag.getStringOr(KEY_STORED_PLAYER_NAME + i, "");
            if (uuidString.isEmpty() || name.isEmpty()) {
                continue;
            }

            try {
                addStoredPlayer(UUID.fromString(uuidString), name);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed historical data rather than bricking the chest.
            }
        }
    }

    private void saveStoredPlayersToTag(CompoundTag tag) {
        int storedCount = storedPlayerCount();
        tag.putInt(KEY_STORED_PLAYER_COUNT, storedCount);
        for (int i = 0; i < storedCount; i++) {
            tag.putString(KEY_STORED_PLAYER_UUID + i, recentPlayerUuids.get(i).toString());
            tag.putString(KEY_STORED_PLAYER_NAME + i, recentPlayerNames.get(i));
        }
        saveSourcePlayersToTag(tag);
    }

    private void loadStoredPlayersFromTag(CompoundTag tag) {
        clearStoredPlayers();

        int storedCount = tag.getIntOr(KEY_STORED_PLAYER_COUNT, 0);
        for (int i = 0; i < storedCount; i++) {
            String uuidString = tag.getStringOr(KEY_STORED_PLAYER_UUID + i, "");
            String name = tag.getStringOr(KEY_STORED_PLAYER_NAME + i, "");
            if (uuidString.isEmpty() || name.isEmpty()) {
                continue;
            }

            try {
                addStoredPlayer(UUID.fromString(uuidString), name);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed historical data rather than bricking the chest.
            }
        }

        if (storedPlayerCount() == 0) {
            loadSourcePlayersFromTag(tag);
        }
    }

    private void saveSourcePlayersToTag(CompoundTag tag) {
        int storedCount = storedPlayerCount();
        if (storedCount == 0) {
            return;
        }
        ListTag players = new ListTag();
        for (int i = 0; i < storedCount; i++) {
            String storedName = recentPlayerNames.get(i);
            if (storedName.isBlank()) {
                continue;
            }
            CompoundTag player = new CompoundTag();
            player.putString(SOURCE_PLAYER_KEY, storedName);
            players.add(player);
        }
        if (!players.isEmpty()) {
            tag.put(SOURCE_PLAYERS_KEY, players);
        }
    }

    private void loadSourcePlayersFromTag(CompoundTag tag) {
        ListTag players = tag.getListOrEmpty(SOURCE_PLAYERS_KEY);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag player = players.getCompoundOrEmpty(i);
            String name = player.getStringOr(SOURCE_PLAYER_KEY, "");
            if (!name.isBlank() && !hasStoredPlayer(UNKNOWN_PLAYER_UUID, name)) {
                addStoredPlayer(UNKNOWN_PLAYER_UUID, name);
            }
        }
    }

    private int storedPlayerCount() {
        int storedCount = Math.min(recentPlayerUuids.size(), recentPlayerNames.size());
        while (recentPlayerUuids.size() > storedCount) {
            recentPlayerUuids.remove(recentPlayerUuids.size() - 1);
        }
        while (recentPlayerNames.size() > storedCount) {
            recentPlayerNames.remove(recentPlayerNames.size() - 1);
        }
        return storedCount;
    }

    private void addStoredPlayer(UUID uuid, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        recentPlayerUuids.add(uuid == null ? UNKNOWN_PLAYER_UUID : uuid);
        recentPlayerNames.add(name);
        trimStoredPlayers();
    }

    private void removeStoredPlayer(int index) {
        if (index < 0 || index >= storedPlayerCount()) {
            return;
        }
        recentPlayerUuids.remove(index);
        recentPlayerNames.remove(index);
    }

    private void clearStoredPlayers() {
        recentPlayerUuids.clear();
        recentPlayerNames.clear();
    }

    private void trimStoredPlayers() {
        while (storedPlayerCount() > MAX_RECENT_PLAYERS) {
            removeStoredPlayer(0);
        }
    }
}
