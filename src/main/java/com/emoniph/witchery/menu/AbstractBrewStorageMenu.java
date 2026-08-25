package com.emoniph.witchery.menu;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

abstract class AbstractBrewStorageMenu extends AbstractContainerMenu {
    protected final Container storage;
    protected final int storageSlots;
    private final InteractionHand hand;
    private final int anchoredHotbarSlot;

    protected AbstractBrewStorageMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                      Container storage, InteractionHand hand, int storageSlots) {
        super(type, containerId);
        this.storage = storage;
        this.hand = hand;
        this.storageSlots = storageSlots;
        this.anchoredHotbarSlot = hand == InteractionHand.MAIN_HAND
                ? playerInventory.getSelectedSlot() : -1;
    }

    protected void addBrewSlot(int storageIndex, int x, int y) {
        addSlot(new Slot(storage, storageIndex, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return storage.canPlaceItem(storageIndex, stack);
            }

            @Override
            public int getMaxStackSize() {
                return storage.getMaxStackSize();
            }
        });
    }

    protected void addPlayerInventory(Inventory inventory, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            boolean locked = column == anchoredHotbarSlot;
            addSlot(new Slot(inventory, column, 8 + column * 18, hotbarY) {
                @Override
                public boolean mayPickup(Player player) {
                    return !locked && super.mayPickup(player);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !locked && super.mayPlace(stack);
                }
            });
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && button == anchoredHotbarSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < storageSlots) {
            if (!moveItemStackTo(stack, storageSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!storage.canPlaceItem(0, stack)
                || !moveItemStackTo(stack, 0, storageSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    public InteractionHand hand() {
        return hand;
    }
}
