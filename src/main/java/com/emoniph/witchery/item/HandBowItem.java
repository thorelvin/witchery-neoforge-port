package com.emoniph.witchery.item;

import net.minecraft.world.item.Item;

public class HandBowItem extends Item {
    private static final int SOURCE_MAX_DAMAGE = 768;

    public HandBowItem(Properties properties) {
        super(properties.durability(SOURCE_MAX_DAMAGE));
    }
}
