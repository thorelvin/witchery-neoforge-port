package com.emoniph.witchery.crafting;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
public class KettleBrewingRecipe implements Recipe<KettleRecipeInput> {

    private final List<ItemStack> ingredients;
    private final ItemStack result;
    private final float power;
    private final int color;
    private final int hatBonus;
    private final int familiarType;
    private final String dimension;

    public KettleBrewingRecipe(
            List<ItemStack> ingredients,
            ItemStack result,
            float power,
            int color,
            int hatBonus,
            int familiarType,
            String dimension
    ) {
        this.ingredients = List.copyOf(ingredients.stream().map(ItemStack::copy).toList());
        this.result = result.copy();
        this.power = power;
        this.color = color;
        this.hatBonus = Math.max(0, hatBonus);
        this.familiarType = Math.max(0, familiarType);
        this.dimension = dimension == null ? "" : dimension;
    }

    private boolean matchesIngredients(List<ItemStack> providedIngredients, boolean partial) {
        if (!partial && providedIngredients.size() != ingredients.size()) {
            return false;
        }

        List<ItemStack> needed = new ArrayList<>(ingredients);
        int matched = 0;
        for (ItemStack provided : providedIngredients) {
            if (provided == null || provided.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int index = 0; index < needed.size(); index++) {
                ItemStack required = needed.get(index);
                if (ItemStack.isSameItemSameComponents(provided, required)) {
                    needed.remove(index);
                    matched++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return partial ? matched > 0 : needed.isEmpty();
    }

    public boolean isAllowedInDimension(Level level) {
        return dimension.isEmpty() || level.dimension().identifier().toString().equals(dimension);
    }

    @Override
    public boolean matches(KettleRecipeInput input, Level level) {
        return isAllowedInDimension(level) && matchesIngredients(input.ingredients(), false);
    }

    public boolean matchesPartial(KettleRecipeInput input, Level level) {
        return isAllowedInDimension(level) && matchesIngredients(input.ingredients(), true);
    }

    @Override
    public ItemStack assemble(KettleRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<KettleBrewingRecipe> getSerializer() {
        return WitcheryRecipeSerializers.KETTLE_BREWING.get();
    }

    @Override
    public RecipeType<KettleBrewingRecipe> getType() {
        return WitcheryRecipeTypes.KETTLE_BREWING.get();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public ItemStack createStoredResult() {
        return result.copy();
    }

    public ItemStack createExtractedResult(@Nullable Player player) {
        ItemStack extracted = result.copyWithCount(1);
        if (player != null && hatBonus > 0) {
            double bonusChance = 0.0;
            double babaBonusChance = 0.0;

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.is(WitcheryItems.WITCHES_HAT.get())) {
                bonusChance += 0.35;
            } else if (head.is(WitcheryItems.BABAS_HAT.get())) {
                bonusChance += 0.25;
                babaBonusChance += 0.25;
            }

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!extracted.is(WitcheryItems.BREW_OF_RAISING.get()) && chest.is(WitcheryItems.WITCHES_ROBE.get())) {
                bonusChance += 0.35;
            } else if (extracted.is(WitcheryItems.BREW_OF_RAISING.get()) && chest.is(WitcheryItems.NECROMANCER_ROBES.get())) {
                bonusChance += 0.35;
            }

            if (FamiliarHelper.hasActiveToadFamiliar(player)) {
                bonusChance += 0.05;
                if (head.is(WitcheryItems.BABAS_HAT.get())) {
                    babaBonusChance += 0.05;
                }
            }

            if (bonusChance > 0.0 && player.getRandom().nextDouble() <= bonusChance) {
                extracted.grow(hatBonus);
            }
            if (babaBonusChance > 0.0 && player.getRandom().nextDouble() <= babaBonusChance) {
                extracted.grow(hatBonus);
            }
        }
        return extracted;
    }

    public boolean matchesResult(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, result);
    }

    public boolean isBrewableBy(@Nullable Player player) {
        if (familiarType == 0) {
            return true;
        }
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return FamiliarHelper.getActiveFamiliarType(player) == familiarType;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public List<ItemStack> getInputItems() {
        return ingredients.stream().map(ItemStack::copy).toList();
    }

    public float getPower() {
        return power;
    }

    public int getColor() {
        return color;
    }

    public int getHatBonus() {
        return hatBonus;
    }

    public int getFamiliarType() {
        return familiarType;
    }

    public String getDimension() {
        return dimension;
    }
}
