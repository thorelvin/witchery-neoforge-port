package com.emoniph.witchery.menu;

import com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
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

public class WitchesOvenMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public WitchesOvenMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory,
                new SimpleContainer(WitchesOvenBlockEntity.SLOT_COUNT),
                new SimpleContainerData(4));
    }

    public WitchesOvenMenu(int windowId, Inventory playerInventory, Container container, ContainerData data) {
        super(WitcheryMenuTypes.WITCHES_OVEN.get(), windowId);
        this.container = container;
        this.data = data;

        checkContainerSize(container, WitchesOvenBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 4);

        this.addSlot(new Slot(container, WitchesOvenBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new Slot(container, WitchesOvenBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return playerInventory.player.level().fuelValues().isFuel(stack);
            }
        });
        this.addSlot(new Slot(container, WitchesOvenBlockEntity.SLOT_COOKED, 116, 21) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, WitchesOvenBlockEntity.SLOT_BYPRODUCT, 116, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, WitchesOvenBlockEntity.SLOT_JARS, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(WitcheryItems.CLAY_JAR.get());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getBurnProgress() {
        int burnTime = data.get(0);
        int maxBurnTime = data.get(1);
        return maxBurnTime > 0 ? burnTime * 12 / maxBurnTime : 0;
    }

    public int getCookProgress() {
        int cookTime = data.get(2);
        int cookTimeTotal = data.get(3);
        return cookTimeTotal > 0 ? cookTime * 24 / cookTimeTotal : 0;
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == WitchesOvenBlockEntity.SLOT_COOKED || index == WitchesOvenBlockEntity.SLOT_BYPRODUCT) {
            if (!moveItemStackTo(stack, 5, 41, true)) return ItemStack.EMPTY;
        } else if (index >= 5) {
            if (stack.getBurnTime(WitcheryRecipeTypes.OVEN.get(), player.level().fuelValues()) > 0) {
                if (!moveItemStackTo(stack, WitchesOvenBlockEntity.SLOT_FUEL, WitchesOvenBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(WitcheryItems.CLAY_JAR.get())) {
                if (!moveItemStackTo(stack, WitchesOvenBlockEntity.SLOT_JARS, WitchesOvenBlockEntity.SLOT_JARS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, WitchesOvenBlockEntity.SLOT_INPUT, WitchesOvenBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 5, 41, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
