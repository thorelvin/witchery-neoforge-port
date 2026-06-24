package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.SpectralStoneItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record SpectralStoneTypeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<SpectralStoneTypeProperty> MAP_CODEC =
            MapCodec.unit(new SpectralStoneTypeProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return switch (SpectralStoneItem.getStoredType(stack)) {
            case SPECTRE -> 1.0F;
            case BANSHEE -> 2.0F;
            case POLTERGEIST -> 3.0F;
            case NONE -> 0.0F;
        };
    }

    @Override
    public MapCodec<SpectralStoneTypeProperty> type() {
        return MAP_CODEC;
    }
}
