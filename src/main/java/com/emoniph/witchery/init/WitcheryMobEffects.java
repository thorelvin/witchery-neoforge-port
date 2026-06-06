package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.effect.FeatherFallMobEffect;
import com.emoniph.witchery.effect.FloatingMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
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

    public static final DeferredHolder<MobEffect, MobEffect> FEATHER_FALL =
            MOB_EFFECTS.register("feather_fall", () -> new FeatherFallMobEffect(0xFFEBD1));

    public static final DeferredHolder<MobEffect, MobEffect> REPELL_ATTACKER =
            MOB_EFFECTS.register("repell_attacker", () -> new RepellAttackerMobEffect(0x233423));

    public static final DeferredHolder<MobEffect, MobEffect> FLOATING =
            MOB_EFFECTS.register("floating", () -> new FloatingMobEffect(0x4E18CC));

    public static final DeferredHolder<MobEffect, MobEffect> POISON_WEAPONS =
            MOB_EFFECTS.register("poison_weapons", () -> new PoisonWeaponsMobEffect(0x2814C9));

    private static final class SimpleMobEffect extends MobEffect {
        private SimpleMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private WitcheryMobEffects() {}
}
