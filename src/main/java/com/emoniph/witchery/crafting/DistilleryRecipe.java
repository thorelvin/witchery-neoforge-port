package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class DistilleryRecipe implements Recipe<DistilleryRecipeInput> {

    private static final int DEFAULT_COOKING_TIME = 800;

    private final ItemStack ingredient1;
    private final ItemStack ingredient2;
    private final int jars;
    private final List<ItemStack> outputs;
    private final int cookingTime;

    public DistilleryRecipe(ItemStack ingredient1, ItemStack ingredient2, int jars, List<ItemStack> outputs, int cookingTime) {
        this.ingredient1 = ingredient1 == null ? ItemStack.EMPTY : ingredient1.copy();
        this.ingredient2 = ingredient2 == null ? ItemStack.EMPTY : ingredient2.copy();
        this.jars = Math.max(0, jars);
        this.outputs = List.copyOf(outputs.stream().map(ItemStack::copy).toList());
        this.cookingTime = cookingTime <= 0 ? DEFAULT_COOKING_TIME : cookingTime;
    }

    private static DistilleryRecipe fromCodec(
            ItemStack ingredient1,
            ItemStack ingredient2,
            int jars,
            ItemStack result,
            List<ItemStack> outputs,
            int cookingTime
    ) {
        List<ItemStack> normalizedOutputs = outputs.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .limit(4)
                .toList();
        if (normalizedOutputs.isEmpty() && result != null && !result.isEmpty()) {
            normalizedOutputs = List.of(result.copy());
        }
        return new DistilleryRecipe(ingredient1, ingredient2, jars, normalizedOutputs, cookingTime);
    }

    private static boolean stackMatches(ItemStack provided, ItemStack required) {
        if (required == null || required.isEmpty()) {
            return provided == null || provided.isEmpty();
        }
        if (provided == null || provided.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(provided, required) && provided.getCount() >= required.getCount();
    }

    @Override
    public boolean matches(DistilleryRecipeInput input, Level level) {
        if (input == null) {
            return false;
        }

        if (jars > 0 && input.jarCount() < jars) {
            return false;
        }

        ItemStack a = input.ingredient1();
        ItemStack b = input.ingredient2();
        return (stackMatches(a, ingredient1) && stackMatches(b, ingredient2))
                || (stackMatches(a, ingredient2) && stackMatches(b, ingredient1));
    }

    @Override
    public ItemStack assemble(DistilleryRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return result();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<DistilleryRecipe> getSerializer() {
        return WitcheryRecipeSerializers.DISTILLERY.get();
    }

    @Override
    public RecipeType<DistilleryRecipe> getType() {
        return WitcheryRecipeTypes.DISTILLERY.get();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public ItemStack result() {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    public ItemStack getIngredient1() {
        return ingredient1.copy();
    }

    public ItemStack getIngredient2() {
        return ingredient2.copy();
    }

    public int jars() {
        return jars;
    }

    public List<ItemStack> outputs() {
        return outputs.stream().map(ItemStack::copy).toList();
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public static final MapCodec<DistilleryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("ingredient1").forGetter(recipe -> recipe.ingredient1),
            ItemStack.CODEC.optionalFieldOf("ingredient2", ItemStack.EMPTY).forGetter(recipe -> recipe.ingredient2),
            Codec.INT.optionalFieldOf("jars", 0).forGetter(DistilleryRecipe::jars),
            ItemStack.CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(DistilleryRecipe::result),
            ItemStack.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(DistilleryRecipe::outputs),
            Codec.INT.optionalFieldOf("cookingtime", DEFAULT_COOKING_TIME).forGetter(DistilleryRecipe::getCookingTime)
    ).apply(instance, DistilleryRecipe::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, DistilleryRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC,
                    recipe -> recipe.ingredient1,
                    ItemStack.STREAM_CODEC,
                    recipe -> recipe.ingredient2,
                    ByteBufCodecs.INT,
                    DistilleryRecipe::jars,
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    DistilleryRecipe::outputs,
                    ByteBufCodecs.INT,
                    DistilleryRecipe::getCookingTime,
                    DistilleryRecipe::new
            );
}
