package com.emoniph.witchery.menu;

import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.item.BrewBagItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

public final class BrewBagMenu extends AbstractBrewStorageMenu {
    public BrewBagMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readEnum(InteractionHand.class), true);
    }

    public BrewBagMenu(int containerId, Inventory inventory, InteractionHand hand) {
        this(containerId, inventory, hand, false);
    }

    private BrewBagMenu(int containerId, Inventory inventory, InteractionHand hand, boolean clientSide) {
        super(WitcheryMenuTypes.BREW_BAG.get(), containerId, inventory,
                clientSide
                        ? new SimpleContainer(BrewBagItem.SLOT_COUNT)
                        : new HeldItemBrewContainer(inventory.player, hand, BrewBagItem.SLOT_COUNT, 64,
                                BrewBagItem::isStorableBrew),
                hand, BrewBagItem.SLOT_COUNT);

        for (int slot = 0; slot < BrewBagItem.SLOT_COUNT; slot++) {
            addBrewSlot(slot, BrewStorageLayout.Bag.storageX(slot), BrewStorageLayout.Bag.STORAGE_Y);
        }
        addPlayerInventory(inventory, BrewStorageLayout.Bag.PLAYER_INVENTORY_Y,
                BrewStorageLayout.Bag.HOTBAR_Y);
    }
}
