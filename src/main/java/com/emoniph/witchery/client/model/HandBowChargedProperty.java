package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.HandBowItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record HandBowChargedProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<HandBowChargedProperty> MAP_CODEC =
            MapCodec.unit(new HandBowChargedProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return HandBowItem.hasLoadedBolt(stack) ? 1.0F : 0.0F;
    }

    @Override
    public MapCodec<HandBowChargedProperty> type() {
        return MAP_CODEC;
    }
}
