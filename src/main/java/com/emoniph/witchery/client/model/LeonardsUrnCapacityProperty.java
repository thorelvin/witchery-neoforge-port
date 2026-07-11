package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.LeonardsUrnItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record LeonardsUrnCapacityProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<LeonardsUrnCapacityProperty> MAP_CODEC =
            MapCodec.unit(new LeonardsUrnCapacityProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return LeonardsUrnItem.getCapacity(stack);
    }

    @Override
    public MapCodec<LeonardsUrnCapacityProperty> type() {
        return MAP_CODEC;
    }
}
