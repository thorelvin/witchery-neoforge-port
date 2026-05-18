package com.emoniph.witchery.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SpinningRecipeSerializer implements RecipeSerializer<SpinningRecipe> {
    @Override
    public MapCodec<SpinningRecipe> codec() {
        return SpinningRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> streamCodec() {
        return SpinningRecipe.STREAM_CODEC;
    }
}
