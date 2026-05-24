package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class WitcheryEntityTags {
    public static final TagKey<EntityType<?>> VAMPIRES = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampires"));
}
