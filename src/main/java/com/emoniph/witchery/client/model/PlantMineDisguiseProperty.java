package com.emoniph.witchery.client.model;

import com.emoniph.witchery.blocks.PlantMineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jspecify.annotations.Nullable;

public record PlantMineDisguiseProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<PlantMineDisguiseProperty> MAP_CODEC =
            MapCodec.unit(new PlantMineDisguiseProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        BlockItemStateProperties state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        PlantMineBlock.PlantMineDisguise disguise = state.get(PlantMineBlock.DISGUISE);
        return switch (disguise == null ? PlantMineBlock.PlantMineDisguise.ROSE : disguise) {
            case ROSE -> 0.0F;
            case DANDELION -> 1.0F;
            case SHRUB -> 2.0F;
        };
    }

    @Override
    public MapCodec<PlantMineDisguiseProperty> type() {
        return MAP_CODEC;
    }
}
