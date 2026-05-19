package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.item.PoppetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PoppetShelfBlockEntity extends BaseContainerBlockEntity {

    public static final int SLOT_COUNT = 9; // one row, matching the original
    private static final Set<PoppetShelfBlockEntity> ACTIVE_SHELVES = ConcurrentHashMap.newKeySet();
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean chunkForced;

    public PoppetShelfBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.POPPET_SHELF.get(), pos, state);
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("container.witchery.poppet_shelf");
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> newItems) {
        for (int i = 0; i < Math.min(newItems.size(), items.size()); i++) items.set(i, newItems.get(i));
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof PoppetItem;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        registerIfServerShelf();
        updateChunkForceState();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        registerIfServerShelf();
        updateChunkForceState();
    }

    @Override
    public void setRemoved() {
        releaseForcedChunk();
        ACTIVE_SHELVES.remove(this);
        super.setRemoved();
    }

    // Use the vanilla single-row chest menu — no custom menu/screen needed
    @Override
    protected AbstractContainerMenu createMenu(int windowId, Inventory playerInv) {
        return new ChestMenu(MenuType.GENERIC_9x1, windowId, playerInv, this, 1);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
        updateChunkForceState();
    }

    public static List<PoppetShelfBlockEntity> getActiveShelves(net.minecraft.server.MinecraftServer server) {
        List<PoppetShelfBlockEntity> result = new ArrayList<>();
        ACTIVE_SHELVES.removeIf(PoppetShelfBlockEntity::isInactiveShelf);
        for (PoppetShelfBlockEntity shelf : ACTIVE_SHELVES) {
            if (shelf.level != null && !shelf.level.isClientSide() && shelf.level.getServer() == server) {
                result.add(shelf);
            }
        }
        return result;
    }

    private void registerIfServerShelf() {
        if (!isInactiveShelf(this)) {
            ACTIVE_SHELVES.add(this);
        }
    }

    private void updateChunkForceState() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        boolean shouldForce = hasBoundPoppet();
        if (shouldForce == chunkForced) {
            return;
        }

        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        serverLevel.setChunkForced(chunkX, chunkZ, shouldForce);
        chunkForced = shouldForce;
    }

    private void releaseForcedChunk() {
        if (!chunkForced || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            chunkForced = false;
            return;
        }

        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        serverLevel.setChunkForced(chunkX, chunkZ, false);
        chunkForced = false;
    }

    private boolean hasBoundPoppet() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.getItem() instanceof PoppetItem && PoppetItem.isBound(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInactiveShelf(PoppetShelfBlockEntity shelf) {
        return shelf == null || shelf.isRemoved() || shelf.level == null || shelf.level.isClientSide();
    }
}
