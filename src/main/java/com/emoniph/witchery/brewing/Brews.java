package com.emoniph.witchery.brewing;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.VampireEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    /** One brew ingredient: an effect/action, or a modifier that augments the next effect / the dispersal. */
    public record Ingredient(Kind kind, Holder<MobEffect> effect, Holder<MobEffect> inverted,
                             BrewAction action, int baseDuration, int invertedDuration, int strengthCeiling, int color,
                             int modifierCeiling, int drinkSpeedModifier, int capacityIncrement,
                             int capacityCeiling, int capacityRequired, int powerCost,
                             String nameKey, String invertedNameKey) {
        public enum Kind {
            EFFECT, ACTION, CAPACITY, STRENGTH, DURATION, INVERT, SPLASH, GAS, COLOR, DRINK_SPEED,
            EXTENT, NO_PARTICLES, DISABLE_STRENGTH_CEILING
        }

        public enum BrewAction { ANIMAL, REMOVE_BUFFS, REMOVE_DEBUFFS, BANE_UNDEAD, BANE_ARTHROPODS, WEAKEN_VAMPIRES }

        static Ingredient effect(Holder<MobEffect> effect, int baseDuration, int ceiling, int color) {
            return effect(effect, null, baseDuration, baseDuration, ceiling, color, 0, 0);
        }

        static Ingredient effect(Holder<MobEffect> effect, int baseDuration, int ceiling, int color, int powerCost) {
            return effect(effect, null, baseDuration, baseDuration, ceiling, color, powerCost, 0);
        }

        static Ingredient effect(Holder<MobEffect> effect, int baseDuration, int ceiling, int color,
                                 int powerCost, int capacityRequired) {
            return effect(effect, null, baseDuration, baseDuration, ceiling, color, powerCost, capacityRequired);
        }

        static Ingredient effect(Holder<MobEffect> effect, Holder<MobEffect> inverted, int baseDuration, int ceiling, int color) {
            return effect(effect, inverted, baseDuration, baseDuration, ceiling, color, 0, 0);
        }

        static Ingredient effect(Holder<MobEffect> effect, Holder<MobEffect> inverted, int baseDuration,
                                 int invertedDuration, int ceiling, int color, int powerCost) {
            return effect(effect, inverted, baseDuration, invertedDuration, ceiling, color, powerCost, 0);
        }

        static Ingredient effect(Holder<MobEffect> effect, Holder<MobEffect> inverted, int baseDuration,
                                 int invertedDuration, int ceiling, int color, int powerCost, int capacityRequired) {
            return new Ingredient(Kind.EFFECT, effect, inverted, null, baseDuration, invertedDuration, ceiling, color,
                    0, 0, 0, 0, capacityRequired, powerCost, null, null);
        }

        static Ingredient action(BrewAction action, int capacityRequired, int powerCost, int color,
                                 String nameKey, String invertedNameKey) {
            return new Ingredient(Kind.ACTION, null, null, action, 0, 0, 0, color,
                    0, 0, 0, 0, capacityRequired, powerCost, nameKey, invertedNameKey);
        }

        static Ingredient capacity(int increment, int ceiling, int powerCost) {
            return new Ingredient(Kind.CAPACITY, null, null, null, 0, 0, 0, 0,
                    0, 0, increment, ceiling, 0, powerCost, null, null);
        }

        static Ingredient color(int color) {
            return new Ingredient(Kind.COLOR, null, null, null, 0, 0, 0, color,
                    0, 0, 0, 0, 0, 0, null, null);
        }

        static Ingredient drinkSpeed(int modifier, int powerCost) {
            return new Ingredient(Kind.DRINK_SPEED, null, null, null, 0, 0, 0, 0,
                    0, modifier, 0, 0, 0, powerCost, null, null);
        }

        static Ingredient modifier(Kind kind) {
            return new Ingredient(kind, null, null, null, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, null, null);
        }

        static Ingredient modifier(Kind kind, int ceiling, int powerCost) {
            return new Ingredient(kind, null, null, null, 0, 0, 0, 0,
                    ceiling, 0, 0, 0, 0, powerCost, null, null);
        }
    }

    /** Running modifier accumulator (mirrors ModifiersEffect strength/duration/inverted). */
    private static final class Modifiers {
        int strength;
        int duration;
        boolean noParticles;
        boolean inverted;
        boolean strengthCeilingDisabled;

        void reset() {
            strength = 0;
            duration = 0;
            noParticles = false;
            inverted = false;
        }
    }

    public record ActionApplication(Ingredient.BrewAction action, boolean inverted, int strength, int duration,
                                    boolean noParticles, boolean strengthCeilingDisabled, String nameKey) {}

    private static final class Resolution {
        final List<MobEffectInstance> effects = new ArrayList<>();
        final List<ActionApplication> actions = new ArrayList<>();
        int capacity;
        int usedCapacity;
        int extent;
        int powerCost;
        boolean blockedByCapacity;
        int blockedRequired;
        int blockedAvailable;
    }

    private static boolean hasAvailableCapacity(Resolution resolved, Ingredient ing) {
        return ing.capacityRequired() <= resolved.capacity - resolved.usedCapacity;
    }

    private static void blockByCapacity(Resolution resolved, Ingredient ing) {
        resolved.blockedByCapacity = true;
        resolved.blockedRequired = Math.max(resolved.blockedRequired, ing.capacityRequired());
        resolved.blockedAvailable = Math.max(resolved.blockedAvailable, resolved.capacity - resolved.usedCapacity);
    }

    private static final Map<Item, Ingredient> REGISTRY = new LinkedHashMap<>();
    private static final Set<Item> EXPLICIT_STIR_ONLY = new HashSet<>();
    private static final Method TAMABLE_SET_OWNER_UUID = findTamableSetOwnerUuid();
    private static final Method MOB_EFFECT_INSTANCE_GET_CURES = findMobEffectInstanceGetCures();
    private static final int SOURCE_DEFAULT_STRENGTH_CEILING = 10;
    private static final int SOURCE_WEAKEN_VAMPIRES_WEAKNESS_TICKS = 90 * 20;
    private static final double SOURCE_INSTANT_RADIUS = 3.0;
    private static final double SOURCE_SPLASH_DURATION_SCALE = 0.5;

    private Brews() {
    }

    public static void bootstrap() {
        if (!REGISTRY.isEmpty()) {
            return;
        }
        // ── Effect ingredients (potion, base duration ticks, strength ceiling, name/tint color) ──
        // NOTE: ingredients consumed by the cauldron's hardcoded recipes are not auto-stirred from
        // dropped item entities. The old dual-purpose capacity trio below is registered for composed
        // brews, but requires the explicit player stir path so source offerings remain intact.
        register(Items.SPIDER_EYE, Ingredient.effect(MobEffects.POISON, 900, SOURCE_DEFAULT_STRENGTH_CEILING, 0x4E9331, 0, 2));
        register(Items.MAGMA_CREAM, Ingredient.effect(MobEffects.FIRE_RESISTANCE, 3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0xE49A3A, 100, 2));
        register(Items.SUGAR, Ingredient.effect(MobEffects.SPEED, MobEffects.SLOWNESS, 3600, 1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x7CAFC6, 100, 2));
        register(Items.GHAST_TEAR, Ingredient.effect(MobEffects.REGENERATION, MobEffects.POISON, 900, 900, SOURCE_DEFAULT_STRENGTH_CEILING, 0xCD5CAB, 200, 2));
        register(Items.LEATHER, Ingredient.effect(MobEffects.JUMP_BOOST, 3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x22FF4C, 200, 2));
        register(Items.SUGAR_CANE, Ingredient.effect(WitcheryMobEffects.FLOATING, 1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x4E18CC, 250, 2));
        register(Items.PUFFERFISH, Ingredient.effect(MobEffects.WATER_BREATHING, 3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x2E5299, 100, 2));
        register(Items.GOLDEN_CARROT, Ingredient.effect(MobEffects.NIGHT_VISION, MobEffects.INVISIBILITY, 3600, 3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x1F1FA1, 200, 2));
        register(Items.GLISTERING_MELON_SLICE, Ingredient.effect(MobEffects.INSTANT_HEALTH, MobEffects.INSTANT_DAMAGE,
                0, 0, 1, 0xBC1A1A, 200, 2));
        register(Items.WITHER_SKELETON_SKULL, Ingredient.effect(MobEffects.WITHER,
                300, SOURCE_DEFAULT_STRENGTH_CEILING, 0x736156, 200, 4));
        register(Items.GOLDEN_APPLE, Ingredient.effect(MobEffects.ABSORPTION,
                600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x2552A5, 1000, 4));
        register(Items.ENCHANTED_GOLDEN_APPLE, Ingredient.effect(MobEffects.HEALTH_BOOST,
                2400, SOURCE_DEFAULT_STRENGTH_CEILING, 0xF87D23, 1000, 4));
        register(Items.INK_SAC, Ingredient.effect(MobEffects.BLINDNESS,
                300, SOURCE_DEFAULT_STRENGTH_CEILING, 0x1F1F23, 1000, 4));
        register(WitcheryItems.INFERNAL_BLOOD.get(), Ingredient.effect(WitcheryMobEffects.GROTESQUE,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x669DC93D, 500, 2));
        register(WitcheryItems.ENDER_DEW.get(), Ingredient.effect(WitcheryMobEffects.ENDER_INHIBITION,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x5B1983, 200, 1));
        register(Items.WHEAT, Ingredient.effect(WitcheryMobEffects.MOONSHINE,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x7D3083, 0, 1));
        register(WitcheryItems.WOLFSBANE_FLOWER.get(), Ingredient.effect(WitcheryMobEffects.WOLFSBANE,
                1200, SOURCE_DEFAULT_STRENGTH_CEILING, 0x67FA11, 0, 1));
        register(Items.SNOWBALL, Ingredient.effect(WitcheryMobEffects.SNOW_TRAIL,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x3F825D, 0, 1));
        register(Items.COD, Ingredient.effect(WitcheryMobEffects.SWIMMING,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x0AC3A3, 0, 1));
        register(Items.CLAY_BALL, Ingredient.effect(WitcheryMobEffects.FORTUNE,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x5C3EBB, 1000, 6));
        register(Items.SALMON, Ingredient.effect(WitcheryMobEffects.SUN_ALLERGY,
                1200, SOURCE_DEFAULT_STRENGTH_CEILING, 0x30A212, 1000, 6));
        register(WitcheryItems.DEMON_HEART.get(), Ingredient.effect(WitcheryMobEffects.PARALYSED,
                200, SOURCE_DEFAULT_STRENGTH_CEILING, 0x35BDB7BF, 750, 4));
        register(WitcheryItems.ICY_NEEDLE.get(), Ingredient.effect(WitcheryMobEffects.CHILLED,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x3535073D, 200, 2));
        registerExplicit(Items.BLAZE_POWDER, Ingredient.effect(MobEffects.STRENGTH, MobEffects.WEAKNESS,
                3600, 1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x932423, 200, 2));
        register(Items.FEATHER, Ingredient.effect(WitcheryMobEffects.FEATHER_FALL, 1200, SOURCE_DEFAULT_STRENGTH_CEILING, 0xFFEBD1, 100, 2));
        register(WitcheryItems.BRAMBLE.get(), Ingredient.effect(WitcheryMobEffects.REPELL_ATTACKER,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x233423, 250, 2));
        register(Items.RED_MUSHROOM, Ingredient.effect(WitcheryMobEffects.POISON_WEAPONS,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x2814C9, 200, 2));
        register(Items.COBWEB, Ingredient.effect(WitcheryMobEffects.REFLECT_PROJECTILES,
                WitcheryMobEffects.ATTRACT_PROJECTILES, 1800, 900,
                SOURCE_DEFAULT_STRENGTH_CEILING, 0x664613, 250, 2));
        register(WitcheryItems.FOUL_FUME.get(), Ingredient.effect(WitcheryMobEffects.STOUT_BELLY,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x9F51EF, 1000, 4));
        register(WitcheryItems.WORMWOOD.get(),
                Ingredient.effect(MobEffects.INVISIBILITY, 1800, 0, 0x7F8392));
        register(WitcheryItems.GOLDEN_HEART.get(), Ingredient.action(Ingredient.BrewAction.ANIMAL,
                4, 500, 0xF2C94C,
                "witchery.brew.animal_attraction", "witchery.brew.animal_repulsion"));
        registerExplicit(WitcheryItems.REEK_OF_MISFORTUNE.get(), Ingredient.action(Ingredient.BrewAction.REMOVE_BUFFS,
                2, 250, 0x210425,
                "witchery.brew.removebuffs", null));
        registerExplicit(WitcheryItems.PURIFIED_MILK.get(), Ingredient.action(Ingredient.BrewAction.REMOVE_DEBUFFS,
                2, 200, 0x3DCB04,
                "witchery.brew.removedebuffs", null));
        register(Items.PUMPKIN, Ingredient.action(Ingredient.BrewAction.BANE_UNDEAD,
                2, 200, 0xD9801F,
                "witchery.brew.harmundead", null));
        register(Items.BLUE_ORCHID, Ingredient.action(Ingredient.BrewAction.BANE_ARTHROPODS,
                2, 200, 0x39A8B8,
                "witchery.brew.harminsects", null));
        register(WitcheryItems.GARLIC.get(), Ingredient.action(Ingredient.BrewAction.WEAKEN_VAMPIRES,
                4, 500, 0xD8D4A5,
                "witchery.brew.weakenvampires", null));

        // Source capacity modifiers. Mandrake Root, Nether Wart, and Tear of the Goddess are also
        // hardcoded cauldron offerings in the modern port, so dropped entities keep that meaning;
        // sneaking with the item explicitly stirs it as custom-brew capacity.
        registerExplicit(WitcheryItems.MANDRAKE_ROOT.get(), Ingredient.capacity(1, 1, 0));
        registerExplicit(Items.NETHER_WART, Ingredient.capacity(2, 2, 50));
        registerExplicit(WitcheryItems.TEAR_OF_GODDESS.get(), Ingredient.capacity(2, 4, 100));
        register(WitcheryItems.DIAMOND_VAPOUR.get(), Ingredient.capacity(2, 6, 150));
        register(Items.DIAMOND, Ingredient.capacity(2, 8, 150));
        register(Items.NETHER_STAR, Ingredient.capacity(4, 10, 150));
        register(WitcheryItems.PENTACLE.get(), Ingredient.capacity(6, 16, 1000));
        register(WitcheryItems.KOBOLDITE_PENTACLE.get(), Ingredient.capacity(6, 16, 1000));

        // Source wool colour modifiers (old Dye.rgb table, registered against wool metadata).
        register(Items.WHITE_WOOL, Ingredient.color(16777215));
        register(Items.ORANGE_WOOL, Ingredient.color(15113780));
        register(Items.MAGENTA_WOOL, Ingredient.color(14383829));
        register(Items.LIGHT_BLUE_WOOL, Ingredient.color(12179199));
        register(Items.YELLOW_WOOL, Ingredient.color(15197994));
        register(Items.LIME_WOOL, Ingredient.color(8639516));
        register(Items.PINK_WOOL, Ingredient.color(15574987));
        register(Items.GRAY_WOOL, Ingredient.color(7763574));
        register(Items.LIGHT_GRAY_WOOL, Ingredient.color(11513789));
        register(Items.CYAN_WOOL, Ingredient.color(3968688));
        register(Items.PURPLE_WOOL, Ingredient.color(8532146));
        register(Items.BLUE_WOOL, Ingredient.color(2247599));
        register(Items.BROWN_WOOL, Ingredient.color(5057301));
        register(Items.GREEN_WOOL, Ingredient.color(5732898));
        register(Items.RED_WOOL, Ingredient.color(12464176));
        register(Items.BLACK_WOOL, Ingredient.color(1118481));

        // ── Modifier ingredients ──
        register(Items.GLOWSTONE_DUST, Ingredient.modifier(Ingredient.Kind.STRENGTH, 1, 50));
        register(Items.BLAZE_ROD, Ingredient.modifier(Ingredient.Kind.STRENGTH, 2, 100));
        register(WitcheryItems.ATTUNED_STONE_CHARGED.get(), Ingredient.modifier(Ingredient.Kind.STRENGTH, 3, 150));
        register(Items.REDSTONE, Ingredient.modifier(Ingredient.Kind.DURATION, 1, 50));
        register(Items.OBSIDIAN, Ingredient.modifier(Ingredient.Kind.DURATION, 2, 100));
        register(WitcheryItems.MINDRAKE_BULB.get(), Ingredient.modifier(Ingredient.Kind.DURATION, 3, 150));
        register(Items.FERMENTED_SPIDER_EYE, Ingredient.modifier(Ingredient.Kind.INVERT, 0, 25));
        register(WitcheryItems.ROWAN_BERRIES.get(), Ingredient.drinkSpeed(-8, 50));
        registerExplicit(WitcheryItems.EXHALE_OF_HORNED_ONE.get(), Ingredient.drinkSpeed(-4, 0));
        register(WitcheryItems.SPANISH_MOSS.get(), Ingredient.drinkSpeed(-4, 50));
        register(WitcheryItems.WOOD_ASH.get(), Ingredient.modifier(Ingredient.Kind.EXTENT, 1, 50));
        register(Items.COCOA_BEANS, Ingredient.modifier(Ingredient.Kind.EXTENT, 2, 100));
        register(WitcheryItems.WISPY_COTTON.get(), Ingredient.modifier(Ingredient.Kind.EXTENT, 3, 150));
        registerExplicit(Items.GOLD_NUGGET, Ingredient.modifier(Ingredient.Kind.NO_PARTICLES, 0, 50));
        register(Items.TROPICAL_FISH, Ingredient.modifier(Ingredient.Kind.DISABLE_STRENGTH_CEILING, 0, 200));
        register(Items.GUNPOWDER, Ingredient.modifier(Ingredient.Kind.SPLASH));
        register(WitcheryItems.ARTICHOKE.get(), Ingredient.modifier(Ingredient.Kind.SPLASH));
        register(WitcheryItems.BAT_WOOL.get(), Ingredient.modifier(Ingredient.Kind.GAS));
        register(Items.DRAGON_BREATH, Ingredient.modifier(Ingredient.Kind.GAS));
    }

    private static void register(Item item, Ingredient ingredient) {
        if (item != null && item != Items.AIR) {
            REGISTRY.put(item, ingredient);
        }
    }

    private static void registerExplicit(Item item, Ingredient ingredient) {
        register(item, ingredient);
        if (item != null && item != Items.AIR) {
            EXPLICIT_STIR_ONLY.add(item);
        }
    }

    public static boolean isIngredient(Item item) {
        bootstrap();
        return REGISTRY.containsKey(item);
    }

    public static boolean requiresExplicitStir(Item item) {
        bootstrap();
        return EXPLICIT_STIR_ONLY.contains(item);
    }

    public static boolean isAutomaticIngredient(Item item) {
        bootstrap();
        return REGISTRY.containsKey(item) && !EXPLICIT_STIR_ONLY.contains(item);
    }

    public static boolean canAddAutomatically(List<Item> currentItems, Item candidate) {
        return isAutomaticIngredient(candidate) && canAdd(currentItems, candidate);
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

    private static Resolution resolveBrew(List<Item> items) {
        Resolution resolved = new Resolution();
        Modifiers mods = new Modifiers();
        for (Ingredient ing : resolve(items)) {
            resolved.powerCost += ing.powerCost();
            switch (ing.kind()) {
                case CAPACITY -> {
                    if (resolved.capacity < ing.capacityCeiling()) {
                        resolved.capacity += ing.capacityIncrement();
                    }
                }
                case STRENGTH -> {
                    if (mods.strength < ing.modifierCeiling()) {
                        mods.strength++;
                    }
                }
                case DURATION -> {
                    if (mods.duration < ing.modifierCeiling()) {
                        mods.duration++;
                    }
                }
                case INVERT -> mods.inverted = true;
                case NO_PARTICLES -> mods.noParticles = true;
                case DISABLE_STRENGTH_CEILING -> mods.strengthCeilingDisabled = true;
                case SPLASH, GAS, COLOR, DRINK_SPEED -> { /* presentation/dispersal/use-time only */ }
                case EXTENT -> {
                    if (resolved.extent < ing.modifierCeiling()) {
                        resolved.extent++;
                    }
                }
                case EFFECT -> {
                    if (hasAvailableCapacity(resolved, ing)) {
                        Holder<MobEffect> effect = mods.inverted && ing.inverted() != null ? ing.inverted() : ing.effect();
                        int baseDuration = mods.inverted && ing.inverted() != null ? ing.invertedDuration() : ing.baseDuration();
                        int strengthCeiling = mods.strengthCeilingDisabled ? SOURCE_DEFAULT_STRENGTH_CEILING : ing.strengthCeiling();
                        int amplifier = Math.min(mods.strength, strengthCeiling);
                        int duration = baseDuration * (mods.duration + 1);
                        resolved.effects.add(new MobEffectInstance(effect, duration, amplifier, mods.noParticles, true, true));
                        resolved.usedCapacity += ing.capacityRequired();
                        mods.reset();
                    } else {
                        blockByCapacity(resolved, ing);
                    }
                }
                case ACTION -> {
                    if (hasAvailableCapacity(resolved, ing)) {
                        boolean inverted = mods.inverted && ing.invertedNameKey() != null;
                        String nameKey = inverted ? ing.invertedNameKey() : ing.nameKey();
                        resolved.actions.add(new ActionApplication(ing.action(), inverted, mods.strength,
                                mods.duration, mods.noParticles, mods.strengthCeilingDisabled, nameKey));
                        resolved.usedCapacity += ing.capacityRequired();
                        mods.reset();
                    } else {
                        blockByCapacity(resolved, ing);
                    }
                }
            }
        }
        return resolved;
    }

    public static boolean canAdd(List<Item> currentItems, Item candidate) {
        Ingredient ing = get(candidate);
        if (ing == null) {
            return false;
        }
        Resolution current = resolveBrew(currentItems);
        return switch (ing.kind()) {
            case CAPACITY -> current.capacity < ing.capacityCeiling();
            case EFFECT, ACTION -> hasAvailableCapacity(current, ing);
            default -> true;
        };
    }

    public static int powerCost(List<Item> items) {
        return resolveBrew(items).powerCost;
    }

    public static boolean blockedByCapacity(List<Item> items) {
        return resolveBrew(items).blockedByCapacity;
    }

    public static int availableCapacity(List<Item> items) {
        return resolveBrew(items).capacity;
    }

    public static int usedCapacity(List<Item> items) {
        return resolveBrew(items).usedCapacity;
    }

    public static int remainingCapacity(List<Item> items) {
        Resolution resolved = resolveBrew(items);
        return resolved.capacity - resolved.usedCapacity;
    }

    public static Dispersal dispersal(List<Item> items) {
        Dispersal result = Dispersal.DRINK;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.SPLASH) {
                result = Dispersal.SPLASH;
            } else if (ing.kind() == Ingredient.Kind.GAS) {
                result = Dispersal.GAS;
            }
        }
        return result;
    }

    /** Whether the brew contains at least one real effect (otherwise it's a mundane brew). */
    public static boolean hasEffect(List<Item> items) {
        Resolution resolved = resolveBrew(items);
        return !resolved.effects.isEmpty() || !resolved.actions.isEmpty();
    }

    /** Build the resolved effect instances the brew will apply (used for drink, splash and gas). */
    public static List<MobEffectInstance> buildEffects(List<Item> items) {
        return List.copyOf(resolveBrew(items).effects);
    }

    public static List<ActionApplication> buildActions(List<Item> items) {
        return List.copyOf(resolveBrew(items).actions);
    }

    public static void applyToEntity(LivingEntity target, List<Item> items) {
        applyToEntity(target, items, target);
    }

    public static void applyToEntity(LivingEntity target, List<Item> items, Entity source) {
        Resolution resolved = resolveBrew(items);
        for (MobEffectInstance effect : resolved.effects) {
            applyEffect(target, effect, source, 1.0, 1.0);
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            for (ActionApplication action : resolved.actions) {
                applyAction(serverLevel, target, action, 1.0, 1.0, source);
            }
        }
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal) {
        applyToArea(level, pos, items, dispersal, null);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal, LivingEntity directTarget) {
        applyToArea(level, pos, items, dispersal, directTarget, directTarget);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal,
                                   LivingEntity directTarget, Entity source) {
        Resolution resolved = resolveBrew(items);
        if (resolved.effects.isEmpty() && resolved.actions.isEmpty()) {
            return;
        }
        if (dispersal == Brews.Dispersal.GAS && !resolved.effects.isEmpty()) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setRadius(3.0F);
            cloud.setDuration(600);
            cloud.setRadiusPerTick(-0.005F);
            cloud.setWaitTime(10);
            for (MobEffectInstance effect : resolved.effects) {
                cloud.addEffect(new MobEffectInstance(effect));
            }
            level.addFreshEntity(cloud);
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 24, 1.0, 0.5, 1.0, 0.05);
        } else {
            double instantRadius = SOURCE_INSTANT_RADIUS + resolved.extent;
            AABB area = new AABB(pos.x - instantRadius, pos.y - instantRadius, pos.z - instantRadius,
                    pos.x + instantRadius, pos.y + instantRadius, pos.z + instantRadius);
            if (!resolved.effects.isEmpty()) {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    double distance = Math.sqrt(entity.distanceToSqr(pos));
                    if (distance <= instantRadius) {
                        boolean directHit = entity == directTarget;
                        double powerFactor = splashPowerFactor(distance, directHit, instantRadius);
                        double durationFactor = SOURCE_SPLASH_DURATION_SCALE * powerFactor;
                        for (MobEffectInstance effect : resolved.effects) {
                            applyEffect(entity, effect, source, powerFactor, durationFactor);
                        }
                    }
                }
            }
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.2, pos.z, 30, 1.0, 0.3, 1.0, 0.1);
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 12, 0.8, 0.3, 0.8, 0.05);
        }
        if (!resolved.actions.isEmpty()) {
            applyAreaActions(level, pos, directTarget, resolved.actions, source, SOURCE_INSTANT_RADIUS + resolved.extent);
        }
    }

    static double splashPowerFactor(double distance, boolean directHit) {
        return splashPowerFactor(distance, directHit, SOURCE_INSTANT_RADIUS);
    }

    static double splashPowerFactor(double distance, boolean directHit, double radius) {
        return directHit ? 1.0 : Math.max(0.0, 1.0 - distance / radius);
    }

    static int scaledSplashDuration(int baseDuration, double powerFactor) {
        return Math.min(Mth.ceil(SOURCE_SPLASH_DURATION_SCALE * powerFactor * (double) baseDuration), Integer.MAX_VALUE);
    }

    private static void applyEffect(LivingEntity target, MobEffectInstance effect, Entity source,
                                    double powerFactor, double durationFactor) {
        MobEffect mobEffect = effect.getEffect().value();
        if (mobEffect.isInstantenous()) {
            if (target.level() instanceof ServerLevel serverLevel) {
                mobEffect.applyInstantenousEffect(serverLevel, source, source, target, effect.getAmplifier(), powerFactor);
            }
        } else {
            int duration = Math.min(Mth.ceil(durationFactor * (double) effect.getDuration()), Integer.MAX_VALUE);
            if (duration > 0) {
                target.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()), source);
            }
        }
    }

    private static Method findTamableSetOwnerUuid() {
        try {
            Method method = TamableAnimal.class.getMethod("setOwnerUUID", UUID.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void setTamableOwnerUuid(TamableAnimal tameable, UUID uuid) {
        if (TAMABLE_SET_OWNER_UUID == null) {
            return;
        }
        try {
            TAMABLE_SET_OWNER_UUID.invoke(tameable, uuid);
        } catch (ReflectiveOperationException ignored) {
            // Modern mappings expose this in dev sources, but keep runtime-safe if a loader remaps it.
        }
    }

    private static Method findMobEffectInstanceGetCures() {
        try {
            Method method = MobEffectInstance.class.getMethod("getCures");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void applyAreaActions(ServerLevel level, Vec3 pos, LivingEntity directTarget,
                                         List<ActionApplication> actions, Entity source, double instantRadius) {
        AABB area = new AABB(pos.x - instantRadius, pos.y - instantRadius, pos.z - instantRadius,
                pos.x + instantRadius, pos.y + instantRadius, pos.z + instantRadius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double distance = Math.sqrt(target.distanceToSqr(pos));
            if (distance > instantRadius) {
                continue;
            }
            boolean directHit = target == directTarget;
            double powerFactor = splashPowerFactor(distance, directHit, instantRadius);
            for (ActionApplication action : actions) {
                applyAction(level, target, action, powerFactor, SOURCE_SPLASH_DURATION_SCALE * powerFactor, source);
            }
        }
    }

    private static void applyAction(ServerLevel level, LivingEntity target, ActionApplication action,
                                    double powerFactor, double durationFactor, Entity source) {
        if (action.action() == Ingredient.BrewAction.ANIMAL) {
            applyAnimalAttraction(level, target, action.inverted(), action.strength());
        } else if (action.action() == Ingredient.BrewAction.REMOVE_BUFFS) {
            removeBuffs(target, action.strength());
        } else if (action.action() == Ingredient.BrewAction.REMOVE_DEBUFFS) {
            removeDebuffs(target, action.strength());
        } else if (action.action() == Ingredient.BrewAction.BANE_UNDEAD) {
            applyBaneDamage(level, target, source, isSourceUndead(target), action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.BANE_ARTHROPODS) {
            applyBaneDamage(level, target, source, isSourceArthropod(target), action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.WEAKEN_VAMPIRES) {
            applyWeakenVampires(target, action, durationFactor, source);
        }
    }

    private static void applyWeakenVampires(LivingEntity target, ActionApplication action,
                                            double durationFactor, Entity source) {
        if (!isSourceVampire(target)) {
            return;
        }
        if (target instanceof Player player && !player.getAbilities().instabuild) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            data.setBloodPower(sourceWeakenVampiresBloodAfter(data.getBloodPower(), action.strength()));
        }
        int duration = sourceModifiedActionDuration(SOURCE_WEAKEN_VAMPIRES_WEAKNESS_TICKS,
                action.duration(), durationFactor);
        int amplifier = sourceWeakenVampiresWeaknessAmplifier(action.strength());
        if (duration > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier,
                    action.noParticles(), true, true), source);
        }
    }

    private static void applyBaneDamage(ServerLevel level, LivingEntity target, Entity source, boolean matchingTarget,
                                        int strength, boolean strengthCeilingDisabled, double powerFactor) {
        int damage = sourceBaneDamage(matchingTarget, strength, strengthCeilingDisabled, powerFactor);
        if (damage > 0) {
            target.hurtServer(level, sourceBaneDamageSource(level, source), damage);
        }
    }

    private static DamageSource sourceBaneDamageSource(ServerLevel level, Entity source) {
        return source == null ? level.damageSources().magic() : level.damageSources().indirectMagic(source, source);
    }

    public static int sourceBaneDamage(boolean matchingTarget, int strength, boolean strengthCeilingDisabled,
                                       double powerFactor) {
        int cappedStrength = Math.min(strength, strengthCeilingDisabled ? 3 : 1);
        int baseDamage = matchingTarget ? 10 : 3;
        return Mth.ceil((double) (baseDamage << cappedStrength) * powerFactor);
    }

    public static boolean isSourceUndead(LivingEntity target) {
        if (target.getType().is(EntityTypeTags.UNDEAD) || target instanceof VampireEntity) {
            return true;
        }
        return target instanceof Player player && WitcheryAttachments.get(player).isVampire();
    }

    public static boolean isSourceArthropod(LivingEntity target) {
        return target.getType().is(EntityTypeTags.ARTHROPOD);
    }

    public static boolean isSourceVampire(LivingEntity target) {
        if (target instanceof VampireEntity) {
            return true;
        }
        return target instanceof Player player && WitcheryAttachments.get(player).isVampire();
    }

    public static int sourceWeakenVampiresBloodLoss(int strength) {
        return 50 + 20 * Math.max(0, strength);
    }

    public static int sourceWeakenVampiresBloodAfter(int currentBlood, int strength) {
        int clampedCurrent = Math.max(0, currentBlood);
        return clampedCurrent >= 1
                ? Math.max(0, clampedCurrent - sourceWeakenVampiresBloodLoss(strength))
                : clampedCurrent;
    }

    public static int sourceWeakenVampiresWeaknessAmplifier(int strength) {
        return Math.min(Math.max(0, strength), SOURCE_DEFAULT_STRENGTH_CEILING);
    }

    public static int sourceModifiedActionDuration(int baseTicks, int durationModifiers, double durationFactor) {
        return Math.min(Mth.ceil(durationFactor * (double) baseTicks * (double) (durationModifiers + 1)),
                Integer.MAX_VALUE);
    }

    private static void removeBuffs(LivingEntity target, int strength) {
        List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (shouldRemoveBuff(effect, strength)) {
                effectsToRemove.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> effect : effectsToRemove) {
            target.removeEffect(effect);
        }
    }

    private static void removeDebuffs(LivingEntity target, int strength) {
        List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (shouldRemoveDebuff(effect, strength)) {
                effectsToRemove.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> effect : effectsToRemove) {
            target.removeEffect(effect);
        }
    }

    public static boolean shouldRemoveBuff(MobEffectInstance effect, int strength) {
        return shouldRemoveBuff(effect.getEffect(), effect.getAmplifier(), isCurableByModernApi(effect), strength);
    }

    public static boolean shouldRemoveBuff(Holder<MobEffect> effect, int amplifier, boolean curable, int strength) {
        return amplifier <= strength
                && effect.value().getCategory() != MobEffectCategory.HARMFUL
                && curable;
    }

    public static boolean shouldRemoveDebuff(MobEffectInstance effect, int strength) {
        return shouldRemoveDebuff(effect.getEffect(), effect.getAmplifier(), isCurableByModernApi(effect), strength);
    }

    public static boolean shouldRemoveDebuff(Holder<MobEffect> effect, int amplifier, boolean curable, int strength) {
        return amplifier <= strength
                && effect.value().getCategory() == MobEffectCategory.HARMFUL
                && curable;
    }

    private static boolean isCurableByModernApi(MobEffectInstance effect) {
        if (MOB_EFFECT_INSTANCE_GET_CURES == null) {
            return true;
        }
        try {
            Object cures = MOB_EFFECT_INSTANCE_GET_CURES.invoke(effect);
            return !(cures instanceof Collection<?> collection) || !collection.isEmpty();
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static void applyAnimalAttraction(ServerLevel level, LivingEntity target, boolean inverted, int strength) {
        double radius = (double) (strength + 1) * 16.0;
        double radiusSq = radius * radius;
        AABB bounds = target.getBoundingBox().inflate(radius, radius, radius);

        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (other == target || other.distanceToSqr(target) >= radiusSq) {
                continue;
            }
            if (other instanceof TamableAnimal tameable) {
                if (tameable instanceof FamiliarEntity) {
                    continue;
                }
                if (!inverted) {
                    if (!tameable.isTame()) {
                        tameable.setTame(true, true);
                        tameable.getNavigation().stop();
                        tameable.setTarget(null);
                        float health = Math.min(20.0F, (float) tameable.getAttributeValue(Attributes.MAX_HEALTH));
                        tameable.setHealth(health);
                        if (target instanceof Player player) {
                            tameable.tame(player);
                        } else {
                            tameable.setTame(true, true);
                            setTamableOwnerUuid(tameable, target.getUUID());
                        }
                        level.broadcastEntityEvent(tameable, (byte) 7);
                    }
                } else if (tameable.isTame() && !tameable.isOwnedBy(target)) {
                    tameable.setTame(false, true);
                    tameable.getNavigation().stop();
                    tameable.setTarget(null);
                    setTamableOwnerUuid(tameable, null);
                    level.broadcastEntityEvent(tameable, (byte) 6);
                }
            }

            if (other instanceof Animal animal) {
                if (inverted) {
                    repelAnimal(level, target, animal);
                } else if (!animal.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0)) {
                    animal.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
                }
            }
        }
    }

    private static void repelAnimal(ServerLevel level, LivingEntity target, Animal animal) {
        int range = 6;
        int newX = Mth.floor(target.getX()) + (level.random.nextBoolean() ? 1 : -1) * (range + level.random.nextInt(range));
        int newZ = Mth.floor(target.getZ()) + (level.random.nextBoolean() ? 1 : -1) * (range + level.random.nextInt(range));
        int newY = Math.max(level.getMinY(), 62);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(newX, newY + 1, newZ);
        while (newY < level.getMaxY() - 2 && !level.isEmptyBlock(cursor.set(newX, newY + 1, newZ))) {
            newY++;
        }

        if (!animal.getNavigation().moveTo(newX, newY, newZ, 1.0)) {
            animal.teleportTo(0.5 + (double) newX, newY, 0.5 + (double) newZ);
        }
    }

    /** RGB tint for the bottle contents (average of effect colors, default witch-purple). */
    public static int color(List<Item> items) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        Integer forcedColor = null;
        int capacity = 0;
        int usedCapacity = 0;
        for (Ingredient ing : resolve(items)) {
            switch (ing.kind()) {
                case CAPACITY -> {
                    if (capacity < ing.capacityCeiling()) {
                        capacity += ing.capacityIncrement();
                    }
                }
                case EFFECT, ACTION -> {
                    if (ing.capacityRequired() <= capacity - usedCapacity) {
                        r += (ing.color() >> 16) & 0xFF;
                        g += (ing.color() >> 8) & 0xFF;
                        b += ing.color() & 0xFF;
                        count++;
                        usedCapacity += ing.capacityRequired();
                    }
                }
                case COLOR -> forcedColor = ing.color();
                default -> { }
            }
        }
        if (forcedColor != null) {
            return forcedColor;
        }
        if (count == 0) {
            return 0x8A2BE2;
        }
        return (int) ((r / count) << 16 | (g / count) << 8 | (b / count));
    }

    /** Display name: "Brew of X" for the primary (last) effect, with strength/long prefixes. */
    public static Component name(List<Item> items) {
        List<Ingredient> resolved = resolve(items);
        Component primary = null;
        boolean strong = false;
        boolean lengthy = false;
        int strength = 0;
        int duration = 0;
        int capacity = 0;
        int usedCapacity = 0;
        boolean pendingInvert = false;
        for (Ingredient ing : resolved) {
            switch (ing.kind()) {
                case CAPACITY -> {
                    if (capacity < ing.capacityCeiling()) {
                        capacity += ing.capacityIncrement();
                    }
                }
                case STRENGTH -> {
                    if (strength < ing.modifierCeiling()) {
                        strength++;
                    }
                }
                case DURATION -> {
                    if (duration < ing.modifierCeiling()) {
                        duration++;
                    }
                }
                case INVERT -> pendingInvert = true;
                case EFFECT -> {
                    if (ing.capacityRequired() <= capacity - usedCapacity) {
                        Holder<MobEffect> effect = pendingInvert && ing.inverted() != null ? ing.inverted() : ing.effect();
                        primary = Component.translatable(effect.value().getDescriptionId());
                        strong = strength > 0;
                        lengthy = duration > 0;
                        usedCapacity += ing.capacityRequired();
                        strength = 0;
                        duration = 0;
                        pendingInvert = false;
                    }
                }
                case ACTION -> {
                    if (usedCapacity + ing.capacityRequired() <= capacity) {
                        boolean inverted = pendingInvert && ing.invertedNameKey() != null;
                        primary = Component.translatable(inverted ? ing.invertedNameKey() : ing.nameKey());
                        strong = strength > 0;
                        lengthy = duration > 0;
                        usedCapacity += ing.capacityRequired();
                        strength = 0;
                        duration = 0;
                        pendingInvert = false;
                    }
                }
                default -> { }
            }
        }
        if (primary == null) {
            return Component.translatable("item.witchery.brew_mundane");
        }
        String prefix = strong && lengthy ? "witchery.brew.potent" : strong ? "witchery.brew.strong" : lengthy ? "witchery.brew.lasting" : "witchery.brew.of";
        return Component.translatable(prefix, primary);
    }

    public static int power(List<Item> items) {
        int appliedStrength = 0;
        int strength = 0;
        int capacity = 0;
        int usedCapacity = 0;
        for (Ingredient ing : resolve(items)) {
            switch (ing.kind()) {
                case CAPACITY -> {
                    if (capacity < ing.capacityCeiling()) {
                        capacity += ing.capacityIncrement();
                    }
                }
                case STRENGTH -> {
                    if (strength < ing.modifierCeiling()) {
                        strength++;
                    }
                }
                case EFFECT, ACTION -> {
                    if (ing.capacityRequired() <= capacity - usedCapacity) {
                        appliedStrength = Math.max(appliedStrength, strength);
                        usedCapacity += ing.capacityRequired();
                        strength = 0;
                    }
                }
                default -> { }
            }
        }
        appliedStrength = Math.max(appliedStrength, strength);
        return Math.clamp(1 + appliedStrength, 1, 5);
    }

    public static int drinkDuration(List<Item> items) {
        int duration = 32;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.DRINK_SPEED) {
                duration += ing.drinkSpeedModifier();
            }
        }
        return Math.max(duration, 2);
    }

    public static int impactExtent(List<Item> items) {
        return resolveBrew(items).extent;
    }

    public static Identifier id() {
        return Identifier.fromNamespaceAndPath("witchery", "brews");
    }
}
