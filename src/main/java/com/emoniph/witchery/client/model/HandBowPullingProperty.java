package com.emoniph.witchery.client.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record HandBowPullingProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<HandBowPullingProperty> MAP_CODEC =
            MapCodec.unit(new HandBowPullingProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return owner instanceof LivingEntity living && living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F;
    }

    @Override
    public MapCodec<HandBowPullingProperty> type() {
        return MAP_CODEC;
    }
}
