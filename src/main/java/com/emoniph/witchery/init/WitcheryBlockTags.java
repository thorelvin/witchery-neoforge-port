package com.emoniph.witchery.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class WitcheryBlockTags {
    /** All blocks that count as chalk circle marks (accepted by the ritual circle scanner). */
    public static final TagKey<Block> CHALK_CIRCLES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("witchery", "chalk_circles"));
}
