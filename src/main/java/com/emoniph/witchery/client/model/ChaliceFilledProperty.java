package com.emoniph.witchery.client.model;

import com.emoniph.witchery.blocks.ChaliceBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

public record ChaliceFilledProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<ChaliceFilledProperty> MAP_CODEC =
            MapCodec.unit(new ChaliceFilledProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBooleanOr(ChaliceBlock.FILLED_TAG, false) ? 1.0F : 0.0F;
    }

    @Override
    public MapCodec<ChaliceFilledProperty> type() {
        return MAP_CODEC;
    }
}
