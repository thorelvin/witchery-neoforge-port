package com.emoniph.witchery.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class WitcheryDimensions {

    public static final ResourceKey<Level> SPIRIT_WORLD =
            ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("witchery", "spirit_world"));

    /** @deprecated use SPIRIT_WORLD - kept for command backward compat */
    public static final ResourceKey<Level> DREAM_WORLD = SPIRIT_WORLD;

    public static final ResourceKey<Level> TORMENT =
            ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath("witchery", "torment"));
}
