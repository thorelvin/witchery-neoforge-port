package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ThrowableBrewSupport {

    private ThrowableBrewSupport() {
    }

    static boolean isThrowableBrew(ItemStack stack) {
        if (stack.getItem() instanceof ThrownBrewItem) {
            return true;
        }
        return stack.getItem() instanceof CustomThrownBrewItem
                && !stack.getOrDefault(WitcheryDataComponents.BREW_CONTENTS.get(), BrewContents.EMPTY).isEmpty();
    }

    static ThrownBrewEntity createProjectile(Level level, Player player, ItemStack stack) {
        if (stack.getItem() instanceof ThrownBrewItem brewItem) {
            int tier = stack.getOrDefault(WitcheryDataComponents.BREW_POWER.get(), 1);
            return new ThrownBrewEntity(level, player, brewItem.brewType(), brewItem.isGas(), tier);
        }
        if (stack.getItem() instanceof CustomThrownBrewItem brewItem) {
            BrewContents contents = stack.getOrDefault(WitcheryDataComponents.BREW_CONTENTS.get(), BrewContents.EMPTY);
            if (!contents.isEmpty()) {
                return new ThrownBrewEntity(level, player, contents, brewItem.isGas());
            }
        }
        return null;
    }
}
