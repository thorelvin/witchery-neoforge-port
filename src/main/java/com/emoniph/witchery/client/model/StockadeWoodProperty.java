package com.emoniph.witchery.client.model;

import com.emoniph.witchery.blocks.StockadeBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record StockadeWoodProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<StockadeWoodProperty> MAP_CODEC =
            MapCodec.unit(new StockadeWoodProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return StockadeBlock.getWoodVariant(stack).ordinal();
    }

    @Override
    public MapCodec<StockadeWoodProperty> type() {
        return MAP_CODEC;
    }
}
