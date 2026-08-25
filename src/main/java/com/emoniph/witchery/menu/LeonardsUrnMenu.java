package com.emoniph.witchery.menu;

import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.item.LeonardsUrnItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public final class LeonardsUrnMenu extends AbstractBrewStorageMenu {
    private final int capacity;

    public LeonardsUrnMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readEnum(InteractionHand.class), data.readVarInt(), true);
    }

    public LeonardsUrnMenu(int containerId, Inventory inventory, InteractionHand hand) {
        this(containerId, inventory, hand,
                LeonardsUrnItem.getCapacity(inventory.player.getItemInHand(hand)), false);
    }

    private LeonardsUrnMenu(int containerId, Inventory inventory, InteractionHand hand,
                            int requestedCapacity, boolean clientSide) {
        super(WitcheryMenuTypes.LEONARDS_URN.get(), containerId, inventory,
                clientSide
                        ? new SimpleContainer(clampCapacity(requestedCapacity))
                        : new HeldItemBrewContainer(inventory.player, hand, clampCapacity(requestedCapacity), 1,
                                LeonardsUrnItem::isStorableBrew),
                hand, clampCapacity(requestedCapacity));
        this.capacity = clampCapacity(requestedCapacity);

        for (int slot = 0; slot < capacity; slot++) {
            addBrewSlot(slot, BrewStorageLayout.Urn.storageX(slot), BrewStorageLayout.Urn.storageY(slot));
        }
        addPlayerInventory(inventory, BrewStorageLayout.Urn.PLAYER_INVENTORY_Y,
                BrewStorageLayout.Urn.HOTBAR_Y);
    }

    public int capacity() {
        return capacity;
    }

    public int brewSlotX(int slot) {
        return BrewStorageLayout.Urn.storageX(slot);
    }

    public int brewSlotY(int slot) {
        return BrewStorageLayout.Urn.storageY(slot);
    }

    private static int clampCapacity(int capacity) {
        return Math.max(1, Math.min(4, capacity));
    }
}
