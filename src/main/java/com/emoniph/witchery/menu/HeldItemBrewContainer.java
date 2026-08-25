package com.emoniph.witchery.menu;

import com.emoniph.witchery.item.StoredBrewInventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** A stable inventory view that flushes every slot mutation into the exact held stack that opened it. */
final class HeldItemBrewContainer extends SimpleContainer {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack anchor;
    private final Item expectedItem;
    private final int maxStackSize;
    private final Predicate<ItemStack> mayStore;
    private boolean loading;

    HeldItemBrewContainer(Player owner, InteractionHand hand, int size, int maxStackSize,
                          Predicate<ItemStack> mayStore) {
        super(size);
        this.owner = owner;
        this.hand = hand;
        this.anchor = owner.getItemInHand(hand);
        this.expectedItem = anchor.getItem();
        this.maxStackSize = maxStackSize;
        this.mayStore = mayStore;

        loading = true;
        for (int slot = 0; slot < size; slot++) {
            super.setItem(slot, StoredBrewInventory.get(anchor, slot));
        }
        loading = false;
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < getContainerSize() && mayStore.test(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner
                && player.isAlive()
                && owner.getItemInHand(hand) == anchor
                && anchor.is(expectedItem);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading || !stillValid(owner)) {
            return;
        }
        for (int slot = 0; slot < getContainerSize(); slot++) {
            StoredBrewInventory.set(anchor, slot, getItem(slot));
        }
        owner.getInventory().setChanged();
    }
}
