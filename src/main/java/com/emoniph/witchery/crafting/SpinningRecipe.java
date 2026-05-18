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

import java.util.ArrayList;
import java.util.List;

public class SpinningRecipe implements Recipe<SpinningRecipeInput> {

    private static final int DEFAULT_COOKING_TIME = 300;

    private final ItemStack fibre;
    private final List<ItemStack> modifiers;
    private final ItemStack result;
    private final int cookingTime;

    public SpinningRecipe(ItemStack fibre, List<ItemStack> modifiers, ItemStack result, int cookingTime) {
        this.fibre = fibre == null ? ItemStack.EMPTY : fibre.copy();
        this.modifiers = List.copyOf(modifiers.stream().map(ItemStack::copy).toList());
        this.result = result.copy();
        this.cookingTime = cookingTime <= 0 ? DEFAULT_COOKING_TIME : cookingTime;
    }

    private static boolean stackMatches(ItemStack provided, ItemStack required) {
        return !provided.isEmpty()
                && !required.isEmpty()
                && ItemStack.isSameItemSameComponents(provided, required)
                && provided.getCount() >= required.getCount();
    }

    @Override
    public boolean matches(SpinningRecipeInput input, Level level) {
        if (!stackMatches(input.fibre(), fibre)) {
            return false;
        }

        List<ItemStack> available = new ArrayList<>();
        for (ItemStack stack : input.modifiers()) {
            if (stack != null && !stack.isEmpty()) {
                available.add(stack);
            }
        }

        for (ItemStack requiredModifier : modifiers) {
            boolean found = false;
            for (int index = 0; index < available.size(); index++) {
                if (stackMatches(available.get(index), requiredModifier)) {
                    available.remove(index);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(SpinningRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<SpinningRecipe> getSerializer() {
        return WitcheryRecipeSerializers.SPINNING.get();
    }

    @Override
    public RecipeType<SpinningRecipe> getType() {
        return WitcheryRecipeTypes.SPINNING.get();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public ItemStack fibre() {
        return fibre.copy();
    }

    public List<ItemStack> modifiers() {
        return modifiers.stream().map(ItemStack::copy).toList();
    }

    public ItemStack getResultItem() {
        return result.copy();
    }

    public int cookingTime() {
        return cookingTime;
    }

    public static final MapCodec<SpinningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("fibre").forGetter(SpinningRecipe::fibre),
            ItemStack.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(SpinningRecipe::modifiers),
            ItemStack.CODEC.fieldOf("result").forGetter(SpinningRecipe::getResultItem),
            Codec.INT.optionalFieldOf("cookingtime", DEFAULT_COOKING_TIME).forGetter(SpinningRecipe::cookingTime)
    ).apply(instance, SpinningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC,
                    SpinningRecipe::fibre,
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SpinningRecipe::modifiers,
                    ItemStack.STREAM_CODEC,
                    SpinningRecipe::getResultItem,
                    ByteBufCodecs.INT,
                    SpinningRecipe::cookingTime,
                    SpinningRecipe::new
            );
}
