package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.CircleTalismanItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record CircleTalismanVariantProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<CircleTalismanVariantProperty> MAP_CODEC =
            MapCodec.unit(new CircleTalismanVariantProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return CircleTalismanItem.getIconVariant(stack);
    }

    @Override
    public MapCodec<CircleTalismanVariantProperty> type() {
        return MAP_CODEC;
    }
}
