package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WitcheryMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, WitcheryMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SCRATCHED =
            MOB_EFFECTS.register("scratched", () -> new SimpleMobEffect(MobEffectCategory.HARMFUL, 0x8F2F32)
                    .addAttributeModifier(Attributes.MAX_HEALTH,
                            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "effect.scratched"),
                            -4.0D, AttributeModifier.Operation.ADD_VALUE));

    private static final class SimpleMobEffect extends MobEffect {
        private SimpleMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private WitcheryMobEffects() {}
}
