package com.emoniph.witchery.menu;

import com.emoniph.witchery.blocks.entity.DistilleryBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
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

public class DistilleryMenu extends AbstractContainerMenu {

    private static final int PLAYER_INV_START = DistilleryBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container container;
    private final ContainerData data;

    public DistilleryMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new SimpleContainer(DistilleryBlockEntity.SLOT_COUNT), new SimpleContainerData(7));
    }

    public DistilleryMenu(int windowId, Inventory playerInv, Container container, ContainerData data) {
        super(WitcheryMenuTypes.DISTILLERY.get(), windowId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, DistilleryBlockEntity.SLOT_COUNT);

        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_INPUT_1,
                MachinePresentationLayout.Distillery.INPUT_X, MachinePresentationLayout.Distillery.INPUT_1_Y));
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_INPUT_2,
                MachinePresentationLayout.Distillery.INPUT_X, MachinePresentationLayout.Distillery.INPUT_2_Y));
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_JARS,
                MachinePresentationLayout.Distillery.JARS_X, MachinePresentationLayout.Distillery.JARS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(WitcheryItems.CLAY_JAR.get());
            }
        });
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_OUTPUT_1,
                MachinePresentationLayout.Distillery.OUTPUT_1_X,
                MachinePresentationLayout.Distillery.OUTPUT_1_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_OUTPUT_2,
                MachinePresentationLayout.Distillery.OUTPUT_2_X,
                MachinePresentationLayout.Distillery.OUTPUT_1_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_OUTPUT_3,
                MachinePresentationLayout.Distillery.OUTPUT_1_X,
                MachinePresentationLayout.Distillery.OUTPUT_2_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, DistilleryBlockEntity.SLOT_OUTPUT_4,
                MachinePresentationLayout.Distillery.OUTPUT_2_X,
                MachinePresentationLayout.Distillery.OUTPUT_2_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

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

    public int getBurnProgress() {
        int lit = data.get(0);
        int max = data.get(1);
        return max > 0 ? lit * 13 / max : 0;
    }

    public int getCookProgress() {
        int progress = data.get(2);
        int total = data.get(3);
        return total > 0 ? progress * MachinePresentationLayout.Distillery.PROGRESS_WIDTH / total : 0;
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    public boolean hasPower() {
        return data.get(0) > 0;
    }

    /** True when a powering altar exists in range (even if it is currently drained). */
    public boolean hasAltar() {
        return data.get(6) > 0;
    }

    /** Altar power fill as a 0..1000 permille of its effective max (overflow-safe). */
    public int getAltarPowerFill() {
        return data.get(4);
    }

    /** Current altar power (clamped for sync; 0 when no altar). */
    public int getAltarPower() {
        return data.get(5);
    }

    /** Altar effective max power (clamped for sync; 0 when no altar). */
    public int getAltarMaxPower() {
        return data.get(6);
    }

    public int getBubbleFrameHeight() {
        int total = data.get(3);
        int progress = data.get(2);
        if (total <= 0 || progress <= 0) {
            return 0;
        }

        return switch ((Math.max(total - progress, 0) / 2) % 7) {
            case 0 -> 29;
            case 1 -> 24;
            case 2 -> 20;
            case 3 -> 16;
            case 4 -> 11;
            case 5 -> 6;
            default -> 0;
        };
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

        if (index >= DistilleryBlockEntity.SLOT_OUTPUT_1 && index <= DistilleryBlockEntity.SLOT_OUTPUT_4) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index >= PLAYER_INV_START) {
            if (stack.is(WitcheryItems.CLAY_JAR.get())) {
                if (!this.moveItemStackTo(stack, DistilleryBlockEntity.SLOT_JARS, DistilleryBlockEntity.SLOT_JARS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, DistilleryBlockEntity.SLOT_INPUT_1, DistilleryBlockEntity.SLOT_INPUT_1 + 1, false)
                    && !this.moveItemStackTo(stack, DistilleryBlockEntity.SLOT_INPUT_2, DistilleryBlockEntity.SLOT_INPUT_2 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
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
