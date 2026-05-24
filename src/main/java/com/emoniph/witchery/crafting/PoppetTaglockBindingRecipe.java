package com.emoniph.witchery.crafting;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.VampiricPoppetItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PoppetTaglockBindingRecipe extends CustomRecipe {
    public PoppetTaglockBindingRecipe(CraftingBookCategory category) {
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

        ItemStack result = inputs.poppet().copyWithCount(1);
        if (result.is(WitcheryItems.VAMPIRIC_POPPET.get())) {
            if (!PoppetItem.isBound(result)) {
                PoppetItem.bindFromTaglock(result, inputs.taglocks().get(0));
                VampiricPoppetItem.bindVictimFromTaglock(result, inputs.taglocks().get(1));
            } else {
                VampiricPoppetItem.bindVictimFromTaglock(result, inputs.taglocks().get(0));
            }
        } else {
            PoppetItem.bindFromTaglock(result, inputs.taglocks().get(0));
        }
        return result;
    }

    @Override
    public RecipeSerializer<PoppetTaglockBindingRecipe> getSerializer() {
        return WitcheryRecipeSerializers.POPPET_TAGLOCK_BINDING.get();
    }

    private static Inputs findInputs(CraftingInput input) {
        ItemStack poppet = ItemStack.EMPTY;
        List<ItemStack> taglocks = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof PoppetItem) {
                if (!poppet.isEmpty()) {
                    return null;
                }
                poppet = stack;
                continue;
            }
            if (stack.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isFilled(stack)) {
                taglocks.add(stack);
                continue;
            }
            return null;
        }

        if (poppet.isEmpty()) {
            return null;
        }

        if (poppet.is(WitcheryItems.VAMPIRIC_POPPET.get())) {
            boolean sourceBound = PoppetItem.isBound(poppet);
            boolean victimBound = VampiricPoppetItem.getVictimUUID(poppet).isPresent();
            if (victimBound) {
                return null;
            }
            int requiredTaglocks = sourceBound ? 1 : 2;
            return taglocks.size() == requiredTaglocks ? new Inputs(poppet, taglocks) : null;
        }

        if (PoppetItem.isBound(poppet)) {
            return null;
        }
        return taglocks.size() == 1 ? new Inputs(poppet, taglocks) : null;
    }

    private record Inputs(ItemStack poppet, List<ItemStack> taglocks) {
    }
}
