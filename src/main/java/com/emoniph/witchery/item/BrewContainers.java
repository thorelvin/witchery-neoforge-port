package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Central wiring for the generic composed-brew container family. A composed brew is persisted only as
 * its ordered ingredient list ({@link BrewContents}, a {@code BREW_CONTENTS} data component) and all of
 * its behaviour — name, colour, effects, dispersal — is rebuilt on demand from {@link Brews}. This
 * class maps a brew's dispersal to the right container item and stamps the shared components, so the
 * cauldron (and anything else producing brews) has a single source of truth.
 */
public final class BrewContainers {

    private BrewContainers() {}

    /** The drinkable / throwable container item that matches a brew's effective dispersal. */
    public static Item forDispersal(Brews.Dispersal dispersal) {
        return switch (dispersal) {
            // A triggered brew is laid like a splash; its delayed curse comes from the contents.
            case SPLASH, TRIGGERED -> WitcheryItems.SPLASH_BREW_BOTTLE.get();
            case GAS -> WitcheryItems.LINGERING_BREW_BOTTLE.get();
            case LIQUID -> WitcheryItems.LIQUID_BREW_BOTTLE.get();
            default -> WitcheryItems.BREW_BOTTLE.get(); // DRINK
        };
    }

    /** Build a single filled bottle for {@code contents}, choosing the container by bottled dispersal. */
    public static ItemStack bottle(BrewContents contents) {
        return stamp(new ItemStack(forDispersal(contents.bottledDispersal())), contents);
    }

    /** Build a filled Brew Bucket (one bucket = the whole batch) for {@code contents}. */
    public static ItemStack bucket(BrewContents contents) {
        return stamp(new ItemStack(WitcheryItems.BREW_BUCKET.get()), contents);
    }

    /** Stamp the shared composed-brew components (contents, power, colour tint) onto a stack. */
    public static ItemStack stamp(ItemStack stack, BrewContents contents) {
        stack.set(WitcheryDataComponents.BREW_CONTENTS.get(), contents);
        stack.set(WitcheryDataComponents.BREW_POWER.get(), contents.power());
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(contents.color())));
        return stack;
    }
}
