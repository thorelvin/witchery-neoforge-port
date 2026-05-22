package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BitingBeltPotionRecipe extends CustomRecipe {
    private static final int MAX_EFFECTS = 2;

    public BitingBeltPotionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = inputs.belt.copyWithCount(1);
        result.set(DataComponents.POTION_CONTENTS, potionContentsFromEffects(inputs.effects));
        return result;
    }

    @Override
    public RecipeSerializer<BitingBeltPotionRecipe> getSerializer() {
        return WitcheryRecipeSerializers.BITING_BELT_POTION.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack belt = ItemStack.EMPTY;
        PotionContents potionContents = PotionContents.EMPTY;
        int beltCount = 0;
        int potionCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(WitcheryItems.BITING_BELT.get())) {
                belt = stack;
                beltCount++;
                continue;
            }

            if (stack.is(Items.POTION)) {
                PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                if (!contents.hasEffects()) {
                    return null;
                }
                potionContents = contents;
                potionCount++;
                continue;
            }

            return null;
        }

        if (beltCount != 1 || potionCount != 1) {
            return null;
        }

        List<MobEffectInstance> effects = new ArrayList<>();
        PotionContents beltContents = belt.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : beltContents.getAllEffects()) {
            if (effects.size() < MAX_EFFECTS) {
                effects.add(new MobEffectInstance(effect));
            }
        }
        if (effects.size() >= MAX_EFFECTS) {
            return null;
        }

        for (MobEffectInstance effect : potionContents.getAllEffects()) {
            if (effects.size() < MAX_EFFECTS) {
                effects.add(new MobEffectInstance(effect));
            }
        }
        return effects.isEmpty() ? null : new Inputs(belt, effects);
    }

    private static PotionContents potionContentsFromEffects(List<MobEffectInstance> effects) {
        PotionContents contents = PotionContents.EMPTY;
        for (MobEffectInstance effect : effects) {
            contents = contents.withEffectAdded(new MobEffectInstance(effect));
        }
        return contents;
    }

    private record Inputs(ItemStack belt, List<MobEffectInstance> effects) {}
}
