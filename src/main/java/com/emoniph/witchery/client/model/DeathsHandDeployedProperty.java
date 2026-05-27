package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.DeathsHandItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record DeathsHandDeployedProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<DeathsHandDeployedProperty> MAP_CODEC =
            MapCodec.unit(new DeathsHandDeployedProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return DeathsHandItem.isDeployed(stack) ? 1.0F : 0.0F;
    }

    @Override
    public MapCodec<DeathsHandDeployedProperty> type() {
        return MAP_CODEC;
    }
}
