package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class WitcheryItemTags {

    /** Silver weapons — silver_sword, silver_bolt. Entities check this tag to detect silver damage. */
    public static final TagKey<Item> SILVER_WEAPONS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "silver_weapons"));

    private WitcheryItemTags() {}
}
