package com.emoniph.witchery.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Ritual dagger — can be placed on an altar top face to boost its distribution range.
 * Places a PlacedItemBlock holding this item above the altar block.
 */
public class ArthanaItem extends Item {

    public ArthanaItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return AltarPlacedItemSupport.placeOnAltar(ctx);
    }
}
