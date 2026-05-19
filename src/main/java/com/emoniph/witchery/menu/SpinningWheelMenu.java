package com.emoniph.witchery.menu;

import com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpinningWheelMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public SpinningWheelMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new SimpleContainer(SpinningWheelBlockEntity.SLOT_COUNT), new SimpleContainerData(3));
    }

    public SpinningWheelMenu(int windowId, Inventory playerInv, Container container, ContainerData data) {
        super(WitcheryMenuTypes.SPINNING_WHEEL.get(), windowId);
        this.container = container;
        this.data = data;

        checkContainerSize(container, SpinningWheelBlockEntity.SLOT_COUNT);

        this.addSlot(new Slot(container, SpinningWheelBlockEntity.SLOT_FIBRE, 56, 26));
        this.addSlot(new Slot(container, SpinningWheelBlockEntity.SLOT_MODIFIER_1, 38, 48));
        this.addSlot(new Slot(container, SpinningWheelBlockEntity.SLOT_OUTPUT, 116, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, SpinningWheelBlockEntity.SLOT_MODIFIER_2, 56, 48));
        this.addSlot(new Slot(container, SpinningWheelBlockEntity.SLOT_MODIFIER_3, 74, 48));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInv, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInv, column, 8 + column * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getSpinProgress() {
        int progress = data.get(0);
        int total = data.get(1);
        return total > 0 ? progress * 24 / total : 0;
    }

    public boolean isSpinning() {
        return data.get(0) > 0;
    }

    public boolean hasPower() {
        return data.get(2) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == SpinningWheelBlockEntity.SLOT_OUTPUT) {
            if (!this.moveItemStackTo(stack, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index >= 5) {
            if (!this.moveItemStackTo(stack, SpinningWheelBlockEntity.SLOT_FIBRE, SpinningWheelBlockEntity.SLOT_FIBRE + 1, false)
                    && !this.moveItemStackTo(stack, SpinningWheelBlockEntity.SLOT_MODIFIER_1, SpinningWheelBlockEntity.SLOT_MODIFIER_1 + 1, false)
                    && !this.moveItemStackTo(stack, SpinningWheelBlockEntity.SLOT_MODIFIER_2, SpinningWheelBlockEntity.SLOT_MODIFIER_2 + 1, false)
                    && !this.moveItemStackTo(stack, SpinningWheelBlockEntity.SLOT_MODIFIER_3, SpinningWheelBlockEntity.SLOT_MODIFIER_3 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 5, 41, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
