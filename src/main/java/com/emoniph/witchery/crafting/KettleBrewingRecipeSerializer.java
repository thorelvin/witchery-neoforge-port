package com.emoniph.witchery.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class KettleBrewingRecipeSerializer implements RecipeSerializer<KettleBrewingRecipe> {

    public static final MapCodec<KettleBrewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.listOf().fieldOf("ingredients").forGetter(KettleBrewingRecipe::getInputItems),
            ItemStack.CODEC.fieldOf("result").forGetter(KettleBrewingRecipe::getResult),
            Codec.FLOAT.optionalFieldOf("power", 0f).forGetter(KettleBrewingRecipe::getPower),
            Codec.INT.optionalFieldOf("color", 0).forGetter(KettleBrewingRecipe::getColor),
            Codec.INT.optionalFieldOf("hatBonus", 0).forGetter(KettleBrewingRecipe::getHatBonus),
            Codec.INT.optionalFieldOf("familiarType", 0).forGetter(KettleBrewingRecipe::getFamiliarType),
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(KettleBrewingRecipe::getDimension)
    ).apply(instance, KettleBrewingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KettleBrewingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    KettleBrewingRecipe::getInputItems,
                    ItemStack.STREAM_CODEC,
                    KettleBrewingRecipe::getResult,
                    ByteBufCodecs.FLOAT,
                    KettleBrewingRecipe::getPower,
                    ByteBufCodecs.INT,
                    KettleBrewingRecipe::getColor,
                    ByteBufCodecs.INT,
                    KettleBrewingRecipe::getHatBonus,
                    ByteBufCodecs.INT,
                    KettleBrewingRecipe::getFamiliarType,
                    ByteBufCodecs.STRING_UTF8,
                    KettleBrewingRecipe::getDimension,
                    KettleBrewingRecipe::new
            );

    @Override
    public MapCodec<KettleBrewingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KettleBrewingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
