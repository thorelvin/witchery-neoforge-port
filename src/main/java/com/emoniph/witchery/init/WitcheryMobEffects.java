package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.effect.AttractProjectilesMobEffect;
import com.emoniph.witchery.effect.ChilledMobEffect;
import com.emoniph.witchery.effect.DiseasedMobEffect;
import com.emoniph.witchery.effect.EnderInhibitionMobEffect;
import com.emoniph.witchery.effect.FeatherFallMobEffect;
import com.emoniph.witchery.effect.FloatingMobEffect;
import com.emoniph.witchery.effect.FortuneMobEffect;
import com.emoniph.witchery.effect.GrotesqueMobEffect;
import com.emoniph.witchery.effect.InsanityMobEffect;
import com.emoniph.witchery.effect.MoonshineMobEffect;
import com.emoniph.witchery.effect.MortalCoilMobEffect;
import com.emoniph.witchery.effect.OverheatingMobEffect;
import com.emoniph.witchery.effect.SinkingMobEffect;
import com.emoniph.witchery.effect.ParalyzedMobEffect;
import com.emoniph.witchery.effect.PoisonWeaponsMobEffect;
import com.emoniph.witchery.effect.QueasyMobEffect;
import com.emoniph.witchery.effect.ReflectProjectilesMobEffect;
import com.emoniph.witchery.effect.RepellAttackerMobEffect;
import com.emoniph.witchery.effect.SnowTrailMobEffect;
import com.emoniph.witchery.effect.StoutBellyMobEffect;
import com.emoniph.witchery.effect.SunAllergyMobEffect;
import com.emoniph.witchery.effect.SwimmingMobEffect;
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

    public static final DeferredHolder<MobEffect, MobEffect> ENDER_INHIBITION =
            MOB_EFFECTS.register("ender_inhibition", () -> new EnderInhibitionMobEffect(0x5B1983));

    public static final DeferredHolder<MobEffect, MobEffect> POISON_WEAPONS =
            MOB_EFFECTS.register("poison_weapons", () -> new PoisonWeaponsMobEffect(0x2814C9));

    public static final DeferredHolder<MobEffect, MobEffect> REFLECT_PROJECTILES =
            MOB_EFFECTS.register("reflect_projectiles", () -> new ReflectProjectilesMobEffect(0x664613));

    public static final DeferredHolder<MobEffect, MobEffect> ATTRACT_PROJECTILES =
            MOB_EFFECTS.register("attract_projectiles", () -> new AttractProjectilesMobEffect(0x51BD4F));

    public static final DeferredHolder<MobEffect, MobEffect> MOONSHINE =
            MOB_EFFECTS.register("moonshine", () -> new MoonshineMobEffect(0x7D3083));

    public static final DeferredHolder<MobEffect, MobEffect> STOUT_BELLY =
            MOB_EFFECTS.register("stout_belly", () -> new StoutBellyMobEffect(0x9F51EF));

    public static final DeferredHolder<MobEffect, MobEffect> WOLFSBANE =
            MOB_EFFECTS.register("wolfsbane", () -> new SimpleMobEffect(MobEffectCategory.BENEFICIAL, 0x67FA11));

    public static final DeferredHolder<MobEffect, MobEffect> GAS_MASK =
            MOB_EFFECTS.register("gas_mask", () -> new SimpleMobEffect(MobEffectCategory.BENEFICIAL, 0x693FBF));

    public static final DeferredHolder<MobEffect, MobEffect> SNOW_TRAIL =
            MOB_EFFECTS.register("snow_trail", () -> new SnowTrailMobEffect(0x3F825D));

    public static final DeferredHolder<MobEffect, MobEffect> SWIMMING =
            MOB_EFFECTS.register("swimming", () -> new SwimmingMobEffect(0x0AC3A3));

    public static final DeferredHolder<MobEffect, MobEffect> FORTUNE =
            MOB_EFFECTS.register("fortune", () -> new FortuneMobEffect(0x5C3EBB));

    public static final DeferredHolder<MobEffect, MobEffect> WORSHIP =
            MOB_EFFECTS.register("worship", () -> new SimpleMobEffect(MobEffectCategory.BENEFICIAL, 0x7D3083));

    public static final DeferredHolder<MobEffect, MobEffect> SUN_ALLERGY =
            MOB_EFFECTS.register("sun_allergy", () -> new SunAllergyMobEffect(0x30A212));

    public static final DeferredHolder<MobEffect, MobEffect> PARALYSED =
            MOB_EFFECTS.register("paralysed", () -> new ParalyzedMobEffect(0x35BDB7BF));

    public static final DeferredHolder<MobEffect, MobEffect> QUEASY =
            MOB_EFFECTS.register("queasy", () -> new QueasyMobEffect(0x85AB4A6C));

    public static final DeferredHolder<MobEffect, MobEffect> CHILLED =
            MOB_EFFECTS.register("chilled", () -> new ChilledMobEffect(0x3535073D));

    public static final DeferredHolder<MobEffect, MobEffect> GROTESQUE =
            MOB_EFFECTS.register("grotesque", () -> new GrotesqueMobEffect(0x669DC93D));

    public static final DeferredHolder<MobEffect, MobEffect> DISEASED =
            MOB_EFFECTS.register("diseased", () -> new DiseasedMobEffect(0xB9AD7D));

    /** Source ownership marker; removing it suppresses enslaver-revenge AI but not the ownership tag. */
    public static final DeferredHolder<MobEffect, MobEffect> ENSLAVED =
            MOB_EFFECTS.register("enslaved", () -> new SimpleMobEffect(MobEffectCategory.HARMFUL, 0x9F541C));

    // ── Shade of Leonard hex kit ──────────────────────────────────────────────
    /** Death Coil — incurable, kills the victim when it expires (cleared if the Shade dies). */
    public static final DeferredHolder<MobEffect, MobEffect> MORTAL_COIL =
            MOB_EFFECTS.register("mortal_coil", () -> new MortalCoilMobEffect(0x009900));

    public static final DeferredHolder<MobEffect, MobEffect> SINKING =
            MOB_EFFECTS.register("sinking", () -> new SinkingMobEffect(0x990000));

    public static final DeferredHolder<MobEffect, MobEffect> INSANITY =
            MOB_EFFECTS.register("insanity", () -> new InsanityMobEffect(0x000099));

    public static final DeferredHolder<MobEffect, MobEffect> OVERHEATING =
            MOB_EFFECTS.register("overheating", () -> new OverheatingMobEffect(0x009999));

    /**
     * Resizing — registered here as a marker so Leonard's giant phase reads as the source
     * {@code RESIZING} effect. The full source resizing system (player shrink/grow tiers +
     * size-based damage multiplier) is a deferred follow-up; Leonard drives his own visible
     * scale through the {@code minecraft:generic.scale} attribute.
     */
    public static final DeferredHolder<MobEffect, MobEffect> RESIZING =
            MOB_EFFECTS.register("resizing", () -> new SimpleMobEffect(MobEffectCategory.NEUTRAL, 0xC050C0));

    private static final class SimpleMobEffect extends MobEffect {
        private SimpleMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private WitcheryMobEffects() {}
}
