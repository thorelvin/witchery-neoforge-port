package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
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

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final List<StoredPlayer> recentPlayers = new ArrayList<>();
    private String customName = "";

    public LeechChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.LEECH_CHEST.get(), pos, state);
    }

    public void setCustomName(String customName) {
        this.customName = customName == null ? "" : customName;
        sync();
    }

    public void storePlayer(Player player) {
        if (level == null || level.isClientSide() || player == null) {
            return;
        }
        StoredPlayer stored = new StoredPlayer(player.getUUID(), player.getName().getString());
        if (recentPlayers.contains(stored)) {
            return;
        }

        recentPlayers.add(stored);
        while (recentPlayers.size() > MAX_RECENT_PLAYERS) {
            recentPlayers.remove(0);
        }
        sync();
    }

    public DrainResult drainStoredPlayerExcept(Player usingPlayer) {
        if (level == null || level.isClientSide() || usingPlayer == null) {
            return DrainResult.none();
        }

        List<String> missingPlayers = new ArrayList<>();
        UUID usingUuid = usingPlayer.getUUID();
        String usingName = usingPlayer.getName().getString();

        for (int i = recentPlayers.size() - 1; i >= 0; i--) {
            StoredPlayer stored = recentPlayers.get(i);
            if (stored.uuid().equals(usingUuid) || stored.name().equalsIgnoreCase(usingName)) {
                if (recentPlayers.size() == 1) {
                    return DrainResult.onlyOwn();
                }
                continue;
            }

            ServerPlayer onlinePlayer = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(stored.uuid());
            if (onlinePlayer != null && onlinePlayer.isAlive()) {
                recentPlayers.remove(i);
                sync();
                return DrainResult.success(new StoredPlayer(onlinePlayer.getUUID(), onlinePlayer.getName().getString()));
            }

            missingPlayers.add(stored.name());
        }

        if (!missingPlayers.isEmpty()) {
            return DrainResult.missingPlayers(missingPlayers);
        }
        return DrainResult.none();
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
    }

    private void saveStoredPlayers(net.minecraft.world.level.storage.ValueOutput tag) {
        tag.putInt(KEY_STORED_PLAYER_COUNT, recentPlayers.size());
        for (int i = 0; i < recentPlayers.size(); i++) {
            StoredPlayer stored = recentPlayers.get(i);
            tag.putString(KEY_STORED_PLAYER_UUID + i, stored.uuid().toString());
            tag.putString(KEY_STORED_PLAYER_NAME + i, stored.name());
        }
    }

    private void loadStoredPlayers(net.minecraft.world.level.storage.ValueInput tag) {
        recentPlayers.clear();

        int storedCount = tag.getIntOr(KEY_STORED_PLAYER_COUNT, 0);
        for (int i = 0; i < storedCount; i++) {
            String uuidString = tag.getStringOr(KEY_STORED_PLAYER_UUID + i, "");
            String name = tag.getStringOr(KEY_STORED_PLAYER_NAME + i, "");
            if (uuidString.isEmpty() || name.isEmpty()) {
                continue;
            }

            try {
                recentPlayers.add(new StoredPlayer(UUID.fromString(uuidString), name));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed historical data rather than bricking the chest.
            }
        }
    }

    private void saveStoredPlayersToTag(CompoundTag tag) {
        tag.putInt(KEY_STORED_PLAYER_COUNT, recentPlayers.size());
        for (int i = 0; i < recentPlayers.size(); i++) {
            StoredPlayer stored = recentPlayers.get(i);
            tag.putString(KEY_STORED_PLAYER_UUID + i, stored.uuid().toString());
            tag.putString(KEY_STORED_PLAYER_NAME + i, stored.name());
        }
    }

    private void loadStoredPlayersFromTag(CompoundTag tag) {
        recentPlayers.clear();

        int storedCount = tag.getIntOr(KEY_STORED_PLAYER_COUNT, 0);
        for (int i = 0; i < storedCount; i++) {
            String uuidString = tag.getStringOr(KEY_STORED_PLAYER_UUID + i, "");
            String name = tag.getStringOr(KEY_STORED_PLAYER_NAME + i, "");
            if (uuidString.isEmpty() || name.isEmpty()) {
                continue;
            }

            try {
                recentPlayers.add(new StoredPlayer(UUID.fromString(uuidString), name));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed historical data rather than bricking the chest.
            }
        }
    }

    public record StoredPlayer(UUID uuid, String name) {}

    public record DrainResult(@Nullable StoredPlayer player, List<String> missingPlayers, boolean onlyOwnTaglock) {
        public static DrainResult success(StoredPlayer player) {
            return new DrainResult(player, List.of(), false);
        }

        public static DrainResult missingPlayers(List<String> missingPlayers) {
            return new DrainResult(null, List.copyOf(missingPlayers), false);
        }

        public static DrainResult onlyOwn() {
            return new DrainResult(null, List.of(), true);
        }

        public static DrainResult none() {
            return new DrainResult(null, List.of(), false);
        }
    }
}
