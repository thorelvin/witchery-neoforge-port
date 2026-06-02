package com.emoniph.witchery.brewing;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Witchery custom-brew composition engine.
 *
 * Faithful to Resurrected's {@code BrewActionList} / {@code ModifiersEffect}: a brew is an ordered
 * list of ingredient items. Modifier ingredients (Glowstone = +strength, Redstone = +duration,
 * Fermented Spider Eye = invert) accumulate into a running {@link Modifiers}; each effect ingredient
 * then applies its potion at the accumulated strength/duration and resets the accumulator. Gunpowder
 * makes the brew splashable, Dragon's Breath makes it a lingering gas.
 *
 * Ingredients are keyed by their item, so a brew persists as just the list of trigger items and the
 * action table is rebuilt on demand — exactly like the source.
 */
public final class Brews {

    /** How a finished brew is delivered. */
    public enum Dispersal { DRINK, SPLASH, GAS }

    /** Maximum ingredients a single cauldron brew may hold. */
    public static final int MAX_CAPACITY = 8;

    /** One brew ingredient: an effect, or a modifier that augments the next effect / the dispersal. */
    public record Ingredient(Kind kind, Holder<MobEffect> effect, Holder<MobEffect> inverted,
                             int baseDuration, int strengthCeiling, int color) {
        public enum Kind { EFFECT, STRENGTH, DURATION, INVERT, SPLASH, GAS }

        static Ingredient effect(Holder<MobEffect> effect, int baseDuration, int ceiling, int color) {
            return new Ingredient(Kind.EFFECT, effect, null, baseDuration, ceiling, color);
        }

        static Ingredient effect(Holder<MobEffect> effect, Holder<MobEffect> inverted, int baseDuration, int ceiling, int color) {
            return new Ingredient(Kind.EFFECT, effect, inverted, baseDuration, ceiling, color);
        }

        static Ingredient modifier(Kind kind) {
            return new Ingredient(kind, null, null, 0, 0, 0);
        }
    }

    /** Running modifier accumulator (mirrors ModifiersEffect strength/duration/inverted). */
    private static final class Modifiers {
        int strength;
        int duration;
        boolean inverted;

        void reset() {
            strength = 0;
            duration = 0;
            inverted = false;
        }
    }

    private static final Map<Item, Ingredient> REGISTRY = new LinkedHashMap<>();

    private Brews() {
    }

    public static void bootstrap() {
        if (!REGISTRY.isEmpty()) {
            return;
        }
        // ── Effect ingredients (potion, base duration ticks, strength ceiling, name/tint color) ──
        // NOTE: ingredients consumed by the cauldron's hardcoded recipes (Nether Wart, Blaze Powder,
        // Mandrake Root, Gold Nugget, Ender Pearl, Dirt, Egg, Dandelion, Wheat Seeds, Tear of the
        // Goddess) are deliberately NOT brew ingredients, so composition never steals them.
        register(Items.SPIDER_EYE, Ingredient.effect(MobEffects.POISON, MobEffects.REGENERATION, 300, 1, 0x4E9331));
        register(Items.MAGMA_CREAM, Ingredient.effect(MobEffects.FIRE_RESISTANCE, 3600, 0, 0xE49A3A));
        register(Items.SUGAR, Ingredient.effect(MobEffects.SPEED, MobEffects.SLOWNESS, 1800, 2, 0x7CAFC6));
        register(Items.GHAST_TEAR, Ingredient.effect(MobEffects.REGENERATION, MobEffects.POISON, 900, 1, 0xCD5CAB));
        register(Items.RABBIT_FOOT, Ingredient.effect(MobEffects.JUMP_BOOST, 1800, 2, 0x22FF4C));
        register(Items.PUFFERFISH, Ingredient.effect(MobEffects.WATER_BREATHING, 3600, 0, 0x2E5299));
        register(Items.GOLDEN_CARROT, Ingredient.effect(MobEffects.NIGHT_VISION, MobEffects.BLINDNESS, 3600, 0, 0x1F1FA1));
        register(Items.GLISTERING_MELON_SLICE, Ingredient.effect(MobEffects.HEALTH_BOOST, 3600, 1, 0xBC1A1A));
        register(Items.PHANTOM_MEMBRANE, Ingredient.effect(MobEffects.SLOW_FALLING, 1800, 0, 0xCAC5B6));
        register(com.emoniph.witchery.init.WitcheryItems.WORMWOOD.get(),
                Ingredient.effect(MobEffects.INVISIBILITY, 1800, 0, 0x7F8392));

        // ── Modifier ingredients ──
        register(Items.GLOWSTONE_DUST, Ingredient.modifier(Ingredient.Kind.STRENGTH));
        register(Items.REDSTONE, Ingredient.modifier(Ingredient.Kind.DURATION));
        register(Items.FERMENTED_SPIDER_EYE, Ingredient.modifier(Ingredient.Kind.INVERT));
        register(Items.GUNPOWDER, Ingredient.modifier(Ingredient.Kind.SPLASH));
        register(Items.DRAGON_BREATH, Ingredient.modifier(Ingredient.Kind.GAS));
    }

    private static void register(Item item, Ingredient ingredient) {
        if (item != null && item != Items.AIR) {
            REGISTRY.put(item, ingredient);
        }
    }

    public static boolean isIngredient(Item item) {
        bootstrap();
        return REGISTRY.containsKey(item);
    }

    public static Ingredient get(Item item) {
        bootstrap();
        return REGISTRY.get(item);
    }

    private static List<Ingredient> resolve(List<Item> items) {
        bootstrap();
        List<Ingredient> out = new ArrayList<>(items.size());
        for (Item item : items) {
            Ingredient ing = REGISTRY.get(item);
            if (ing != null) {
                out.add(ing);
            }
        }
        return out;
    }

    public static Dispersal dispersal(List<Item> items) {
        Dispersal result = Dispersal.DRINK;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.GAS) {
                return Dispersal.GAS;
            }
            if (ing.kind() == Ingredient.Kind.SPLASH) {
                result = Dispersal.SPLASH;
            }
        }
        return result;
    }

    /** Whether the brew contains at least one real effect (otherwise it's a mundane brew). */
    public static boolean hasEffect(List<Item> items) {
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.EFFECT) {
                return true;
            }
        }
        return false;
    }

    /** Build the resolved effect instances the brew will apply (used for drink, splash and gas). */
    public static List<MobEffectInstance> buildEffects(List<Item> items) {
        List<MobEffectInstance> effects = new ArrayList<>();
        Modifiers mods = new Modifiers();
        for (Ingredient ing : resolve(items)) {
            switch (ing.kind()) {
                case STRENGTH -> mods.strength++;
                case DURATION -> mods.duration++;
                case INVERT -> mods.inverted = !mods.inverted;
                case SPLASH, GAS -> { /* dispersal only */ }
                case EFFECT -> {
                    Holder<MobEffect> effect = mods.inverted && ing.inverted() != null ? ing.inverted() : ing.effect();
                    int amplifier = Math.min(mods.strength, ing.strengthCeiling());
                    int duration = ing.baseDuration() * (mods.duration + 1);
                    effects.add(new MobEffectInstance(effect, duration, amplifier));
                    mods.reset();
                }
            }
        }
        return effects;
    }

    public static void applyToEntity(LivingEntity target, List<Item> items) {
        for (MobEffectInstance effect : buildEffects(items)) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal) {
        List<MobEffectInstance> effects = buildEffects(items);
        if (effects.isEmpty()) {
            return;
        }
        if (dispersal == Brews.Dispersal.GAS) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setRadius(3.0F);
            cloud.setDuration(600);
            cloud.setRadiusPerTick(-0.005F);
            cloud.setWaitTime(10);
            for (MobEffectInstance effect : effects) {
                cloud.addEffect(new MobEffectInstance(effect));
            }
            level.addFreshEntity(cloud);
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 24, 1.0, 0.5, 1.0, 0.05);
        } else {
            AABB area = new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 2, pos.z + 4);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                for (MobEffectInstance effect : effects) {
                    entity.addEffect(new MobEffectInstance(effect));
                }
            }
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.2, pos.z, 30, 1.0, 0.3, 1.0, 0.1);
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 12, 0.8, 0.3, 0.8, 0.05);
        }
    }

    /** RGB tint for the bottle contents (average of effect colors, default witch-purple). */
    public static int color(List<Item> items) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.EFFECT) {
                r += (ing.color() >> 16) & 0xFF;
                g += (ing.color() >> 8) & 0xFF;
                b += ing.color() & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return 0x8A2BE2;
        }
        return (int) ((r / count) << 16 | (g / count) << 8 | (b / count));
    }

    /** Display name: "Brew of X" for the primary (last) effect, with strength/long prefixes. */
    public static Component name(List<Item> items) {
        List<Ingredient> resolved = resolve(items);
        Ingredient primary = null;
        boolean strong = false;
        boolean lengthy = false;
        boolean inverted = false;
        int strength = 0;
        int duration = 0;
        boolean pendingInvert = false;
        for (Ingredient ing : resolved) {
            switch (ing.kind()) {
                case STRENGTH -> strength++;
                case DURATION -> duration++;
                case INVERT -> pendingInvert = !pendingInvert;
                case EFFECT -> {
                    primary = ing;
                    strong = strength > 0;
                    lengthy = duration > 0;
                    inverted = pendingInvert && ing.inverted() != null;
                    strength = 0;
                    duration = 0;
                    pendingInvert = false;
                }
                default -> { }
            }
        }
        if (primary == null) {
            return Component.translatable("item.witchery.brew_mundane");
        }
        Holder<MobEffect> effect = inverted ? primary.inverted() : primary.effect();
        Component effectName = Component.translatable(effect.value().getDescriptionId());
        String prefix = strong && lengthy ? "witchery.brew.potent" : strong ? "witchery.brew.strong" : lengthy ? "witchery.brew.lasting" : "witchery.brew.of";
        return Component.translatable(prefix, effectName);
    }

    public static int power(List<Item> items) {
        int strength = 0;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.STRENGTH) {
                strength++;
            }
        }
        return Math.clamp(1 + strength, 1, 5);
    }

    public static Identifier id() {
        return Identifier.fromNamespaceAndPath("witchery", "brews");
    }
}
