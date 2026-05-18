package com.emoniph.witchery.crafting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;

public class DistilleryRecipeSerializer implements RecipeSerializer<DistilleryRecipe> {
    @Override public MapCodec<DistilleryRecipe> codec() { return DistilleryRecipe.CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, DistilleryRecipe> streamCodec() {
        return DistilleryRecipe.STREAM_CODEC.cast();
    }
}
