package com.emoniph.witchery.brewing;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.effect.DiseasedMobEffect;
import com.emoniph.witchery.effect.SnowTrailMobEffect;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LilithEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.VampireEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.events.SilverEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * The Witchery custom-brew composition engine.
 *
 * Faithful to Resurrected's {@code BrewActionList} / {@code ModifiersEffect}: a brew is an ordered
 * list of ingredient items. Modifier ingredients (Glowstone = +strength, Redstone = +duration,
 * Fermented Spider Eye = invert) accumulate into a running {@link Modifiers}; each effect ingredient
 * then applies its potion at the accumulated strength/duration and resets the accumulator. Gunpowder
 * makes the brew splashable, Bat Wool makes it a lingering gas, and Wormwood selects the old liquid
 * dispersal once moving brew fluids are available.
 *
 * Ingredients are keyed by their item, so a brew persists as just the list of trigger items and the
 * action table is rebuilt on demand — exactly like the source.
 */
public final class Brews {

    /** How a finished brew is delivered. */
    public enum Dispersal { DRINK, SPLASH, GAS, LIQUID, TRIGGERED }

    /** Maximum ingredients a single cauldron brew may hold. */
    public static final int MAX_CAPACITY = 8;

    /** One brew ingredient: an effect/action, or a modifier that augments the next effect / the dispersal. */
    public record Ingredient(Kind kind, Holder<MobEffect> effect, Holder<MobEffect> inverted,
                             BrewAction action, int baseDuration, int invertedDuration, int strengthCeiling, int color,
                             int modifierCeiling, int drinkSpeedModifier, int capacityIncrement,
                             int capacityCeiling, int capacityRequired, int powerCost,
                             String nameKey, String invertedNameKey) {
        public enum Kind {
            EFFECT, ACTION, CAPACITY, STRENGTH, DURATION, INVERT, SPLASH, GAS, LIQUID, TRIGGER, COLOR, DRINK_SPEED,
            EXTENT, LIFETIME, NO_PARTICLES, DISABLE_BLOCK_TARGET, DISABLE_ENTITY_TARGET,
            DISABLE_STRENGTH_CEILING, RITUAL
        }

        public enum BrewAction {
            EXTINGUISH, DISSIPATE, GROW_FLOWERS, FERTILIZE,
            ANIMAL, REMOVE_BUFFS, REMOVE_DEBUFFS, BANE_UNDEAD, BANE_ARTHROPODS, WEAKEN_VAMPIRES, BANE_DEMONS,
            HARM_WEREWOLVES, STEAL_BUFFS, DURATION_BOOST, AIR_HIKE, PULL
        }

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

        static Ingredient ritual(int powerCost) {
            return new Ingredient(Kind.RITUAL, null, null, null, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, powerCost, null, null);
        }
    }

    /** Running modifier accumulator (mirrors ModifiersEffect strength/duration/inverted). */
    private static final class Modifiers {
        int strength;
        int duration;
        boolean noParticles;
        boolean inverted;
        boolean disableBlockTarget;
        boolean disableEntityTarget;
        boolean strengthCeilingDisabled;

        void reset() {
            strength = 0;
            duration = 0;
            noParticles = false;
            inverted = false;
        }
    }

    public record EffectApplication(MobEffectInstance effect, boolean disableBlockTarget,
                                    boolean disableEntityTarget) {}

    public record ActionApplication(Ingredient.BrewAction action, boolean inverted, int strength, int duration,
                                    boolean noParticles, boolean disableBlockTarget, boolean disableEntityTarget,
                                    boolean strengthCeilingDisabled, String nameKey) {}

    private static final class Resolution {
        final List<EffectApplication> effects = new ArrayList<>();
        final List<ActionApplication> actions = new ArrayList<>();
        int capacity;
        int usedCapacity;
        int extent;
        int lifetime;
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
    private static final int SOURCE_POTION_LONGEVITY_BOOST_TICKS = 3 * 60 * 20;
    private static final int SOURCE_POTION_LONGEVITY_HUNGER_TICKS = 2 * 60 * 20;
    private static final int SOURCE_POTION_LONGEVITY_NAUSEA_TICKS = 5 * 60 * 20;
    private static final int SOURCE_PULL_JUMP_TICKS = 20;
    private static final int SOURCE_PULL_JUMP_AMPLIFIER = 1;
    private static final double SOURCE_PULL_HORIZONTAL_SCALE = 0.15D;
    private static final double SOURCE_PULL_VERTICAL_MOTION = 0.4D;
    private static final double SOURCE_INSTANT_RADIUS = 3.0;
    private static final double SOURCE_SPLASH_DURATION_SCALE = 0.5;
    private static final double SOURCE_BREW_FLUID_CONTACT_POWER = 0.25;
    private static final double SOURCE_BREW_FLUID_CONTACT_DURATION_SCALE = 0.5;
    private static final int SOURCE_GAS_CLOUD_BASE_DURATION_TICKS = 600;
    private static final int SOURCE_BREW_FLUID_BASE_DECAY_DENOMINATOR = 5;
    private static final double SOURCE_ADJACENT_BLOCK_PULSE_CHANCE = 0.01;
    private static final double SOURCE_ADJACENT_BLOCK_PULSE_RADIUS = 1.0;
    public static final int SOURCE_RITUAL_TRIGGER_POWER_COST = 250;
    public static final int SOURCE_TRIGGERED_RITUAL_ITEM_CHARGES = 1;
    public static final int SOURCE_TRIGGERED_RITUAL_CAT_ITEM_CHARGES = 2;
    public static final int SOURCE_PURIFIED_MILK_DISEASE_CURE_STRENGTH = DiseasedMobEffect.NORMAL_CURE_STRENGTH;
    public static final int SOURCE_PURIFIED_MILK_DISEASE_BLOCK_CLEANUP_STRENGTH =
            DiseasedMobEffect.BLOCK_CLEANUP_STRENGTH;
    public static final int SOURCE_PURIFIED_MILK_DISEASE_BLOCK_CLEANUP_RADIUS =
            DiseasedMobEffect.BLOCK_CLEANUP_RADIUS;
    private static final String SOURCE_SUMMONED_UNDEAD_OWNER_TAG = "WITCEnslaverName";
    private static final String PORT_SUMMONED_UNDEAD_OWNER_TAG = "witchery_enslaver_uuid";
    private static final String SOURCE_BREW_GAS_TAG = "WITCBrewGas";
    private static final String PORT_BREW_GAS_TAG = "witchery_brew_gas";
    private static final BlockState[] SOURCE_FLOWER_STATES = new BlockState[] {
            Blocks.DANDELION.defaultBlockState(),
            Blocks.POPPY.defaultBlockState(),
            Blocks.BLUE_ORCHID.defaultBlockState(),
            Blocks.ALLIUM.defaultBlockState(),
            Blocks.AZURE_BLUET.defaultBlockState(),
            Blocks.RED_TULIP.defaultBlockState(),
            Blocks.ORANGE_TULIP.defaultBlockState(),
            Blocks.WHITE_TULIP.defaultBlockState(),
            Blocks.PINK_TULIP.defaultBlockState(),
            Blocks.OXEYE_DAISY.defaultBlockState()
    };

    public enum RitualDeliveryResult {
        SUCCESS,
        NO_EFFECT,
        NEEDS_SPLASH_DISPERSAL,
        NO_TRIGGER_ITEM,
        BLOCKED
    }

    public record IngredientReference(Item item, Ingredient.Kind kind, int capacityRequired, int powerCost,
                                      int modifierCeiling, int capacityIncrement, int capacityCeiling,
                                      int drinkSpeedModifier, Identifier effectId, Identifier invertedEffectId,
                                      String actionNameKey, String invertedActionNameKey,
                                      boolean explicitStirOnly) {
    }

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
        registerExplicit(Items.COAL, Ingredient.action(Ingredient.BrewAction.EXTINGUISH,
                1, 0, 0x2E2E2E,
                "witchery.brew.extinguish", null));
        registerExplicit(Blocks.STONE.asItem(), Ingredient.action(Ingredient.BrewAction.DISSIPATE,
                1, 0, 0x8B8B8B,
                "witchery.brew.dissipate", null));
        registerExplicit(Items.DANDELION, Ingredient.action(Ingredient.BrewAction.GROW_FLOWERS,
                1, 200, 0xF2D447,
                "witchery.brew.flowers", null));
        registerExplicit(Items.BONE_MEAL, Ingredient.action(Ingredient.BrewAction.FERTILIZE,
                1, 250, 0xEDE8D0,
                "witchery.brew.fertilization", null));
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
        register(Items.ROTTEN_FLESH, Ingredient.effect(WitcheryMobEffects.DISEASED,
                DiseasedMobEffect.BREW_DURATION_TICKS, SOURCE_DEFAULT_STRENGTH_CEILING, 0xB9AD7D, 2000, 4));
        register(WitcheryItems.DEMON_HEART.get(), Ingredient.effect(WitcheryMobEffects.PARALYSED,
                200, SOURCE_DEFAULT_STRENGTH_CEILING, 0x35BDB7BF, 750, 4));
        register(WitcheryItems.ICY_NEEDLE.get(), Ingredient.effect(WitcheryMobEffects.CHILLED,
                3600, SOURCE_DEFAULT_STRENGTH_CEILING, 0x3535073D, 200, 2));
        registerExplicit(Items.BLAZE_POWDER, Ingredient.effect(MobEffects.STRENGTH, MobEffects.WEAKNESS,
                3600, 1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x932423, 200, 2));
        register(Items.FEATHER, Ingredient.effect(WitcheryMobEffects.FEATHER_FALL, 1200, SOURCE_DEFAULT_STRENGTH_CEILING, 0xFFEBD1, 100, 2));
        register(WitcheryItems.BRAMBLE.get(), Ingredient.effect(WitcheryMobEffects.REPELL_ATTACKER,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x233423, 250, 2));
        register(Items.IRON_ORE, Ingredient.effect(WitcheryMobEffects.GAS_MASK,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x693FBF, 100, 2));
        register(Items.RED_MUSHROOM, Ingredient.effect(WitcheryMobEffects.POISON_WEAPONS,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x2814C9, 200, 2));
        register(Items.COBWEB, Ingredient.effect(WitcheryMobEffects.REFLECT_PROJECTILES,
                WitcheryMobEffects.ATTRACT_PROJECTILES, 1800, 900,
                SOURCE_DEFAULT_STRENGTH_CEILING, 0x664613, 250, 2));
        register(WitcheryItems.FOUL_FUME.get(), Ingredient.effect(WitcheryMobEffects.STOUT_BELLY,
                1800, SOURCE_DEFAULT_STRENGTH_CEILING, 0x9F51EF, 1000, 4));
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
        register(Items.ICE, Ingredient.action(Ingredient.BrewAction.BANE_DEMONS,
                5, 500, 0x8AD7FF,
                "witchery.brew.harmdemons", null));
        register(WitcheryItems.SILVER_DEPOSITS.get(), Ingredient.action(Ingredient.BrewAction.HARM_WEREWOLVES,
                4, 1000, 0xC0C0C0,
                "witchery.brew.harmwerewolves", null));
        register(Items.SKELETON_SKULL, Ingredient.action(Ingredient.BrewAction.STEAL_BUFFS,
                6, 100, 0xD6D6D6,
                "witchery.brew.stealbuffs", null));
        register(WitcheryItems.CREEPER_HEART.get(), Ingredient.action(Ingredient.BrewAction.DURATION_BOOST,
                6, 5000, 0x5A8F3F,
                "witchery.brew.durationboost", null));
        register(WitcheryItems.BREATH_OF_GODDESS.get(), Ingredient.action(Ingredient.BrewAction.AIR_HIKE,
                2, 750, 0xD8F4FF,
                "witchery.brew.airhike", null));
        register(Items.SLIME_BALL, Ingredient.action(Ingredient.BrewAction.PULL,
                2, 150, 0x8CD66A,
                "witchery.brew.frogtongue", null));

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
        register(WitcheryItems.BELLADONNA.get(), Ingredient.modifier(Ingredient.Kind.LIFETIME, 1, 50));
        register(Items.LAPIS_LAZULI, Ingredient.modifier(Ingredient.Kind.LIFETIME, 2, 100));
        register(Items.HAY_BLOCK, Ingredient.modifier(Ingredient.Kind.LIFETIME, 3, 150));
        registerExplicit(Items.GOLD_NUGGET, Ingredient.modifier(Ingredient.Kind.NO_PARTICLES, 0, 50));
        register(Items.NETHER_BRICK, Ingredient.modifier(Ingredient.Kind.DISABLE_BLOCK_TARGET, 0, 50));
        register(Items.BRICK, Ingredient.modifier(Ingredient.Kind.DISABLE_ENTITY_TARGET, 0, 50));
        register(Items.TROPICAL_FISH, Ingredient.modifier(Ingredient.Kind.DISABLE_STRENGTH_CEILING, 0, 200));
        register(Items.GUNPOWDER, Ingredient.modifier(Ingredient.Kind.SPLASH));
        register(WitcheryItems.ARTICHOKE.get(), Ingredient.modifier(Ingredient.Kind.SPLASH));
        register(WitcheryItems.BAT_WOOL.get(), Ingredient.modifier(Ingredient.Kind.GAS));
        register(Items.DRAGON_BREATH, Ingredient.modifier(Ingredient.Kind.GAS));
        register(WitcheryItems.WORMWOOD.get(), Ingredient.modifier(Ingredient.Kind.LIQUID));
        // Zombie Head = source triggered dispersal: the brew is not released on impact but stored as a
        // delayed "curse" on items it splashes, released later when the cursed item is used.
        register(Items.ZOMBIE_HEAD, Ingredient.modifier(Ingredient.Kind.TRIGGER));
        // Tongue of Dog = source BrewActionRitual trigger. It stays explicit so dropped Tongue of Dog
        // still feeds kettle recipes instead of being swallowed by the auto-stir loop.
        registerExplicit(WitcheryItems.TONGUE_OF_DOG.get(), Ingredient.ritual(SOURCE_RITUAL_TRIGGER_POWER_COST));
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

    public static List<IngredientReference> ingredientReferences() {
        bootstrap();
        return REGISTRY.entrySet().stream()
                .map(entry -> {
                    Item item = entry.getKey();
                    Ingredient ing = entry.getValue();
                    Identifier effectId = ing.effect() == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(ing.effect().value());
                    Identifier invertedEffectId = ing.inverted() == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(ing.inverted().value());
                    return new IngredientReference(item, ing.kind(), ing.capacityRequired(), ing.powerCost(),
                            ing.modifierCeiling(), ing.capacityIncrement(), ing.capacityCeiling(),
                            ing.drinkSpeedModifier(), effectId, invertedEffectId, ing.nameKey(),
                            ing.invertedNameKey(), EXPLICIT_STIR_ONLY.contains(item));
                })
                .toList();
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
                case DISABLE_BLOCK_TARGET -> mods.disableBlockTarget = true;
                case DISABLE_ENTITY_TARGET -> mods.disableEntityTarget = true;
                case DISABLE_STRENGTH_CEILING -> mods.strengthCeilingDisabled = true;
                case SPLASH, GAS, LIQUID, TRIGGER, COLOR, DRINK_SPEED, RITUAL -> { /* presentation/dispersal/use-time only */ }
                case EXTENT -> {
                    if (resolved.extent < ing.modifierCeiling()) {
                        resolved.extent++;
                    }
                }
                case LIFETIME -> {
                    if (resolved.lifetime < ing.modifierCeiling()) {
                        resolved.lifetime++;
                    }
                }
                case EFFECT -> {
                    if (hasAvailableCapacity(resolved, ing)) {
                        Holder<MobEffect> effect = mods.inverted && ing.inverted() != null ? ing.inverted() : ing.effect();
                        int baseDuration = mods.inverted && ing.inverted() != null ? ing.invertedDuration() : ing.baseDuration();
                        int strengthCeiling = mods.strengthCeilingDisabled ? SOURCE_DEFAULT_STRENGTH_CEILING : ing.strengthCeiling();
                        int amplifier = Math.min(mods.strength, strengthCeiling);
                        int duration = baseDuration * (mods.duration + 1);
                        MobEffectInstance instance = new MobEffectInstance(effect, duration, amplifier,
                                mods.noParticles, true, true);
                        resolved.effects.add(new EffectApplication(instance, mods.disableBlockTarget,
                                mods.disableEntityTarget));
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
                                mods.duration, mods.noParticles, mods.disableBlockTarget, mods.disableEntityTarget,
                                mods.strengthCeilingDisabled, nameKey));
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

    public static boolean triggersRitual(List<Item> items) {
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.RITUAL) {
                return true;
            }
        }
        return false;
    }

    public static int ritualPowerCost(List<Item> items) {
        return powerCost(items) + (triggersRitual(items) ? 0 : SOURCE_RITUAL_TRIGGER_POWER_COST);
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
        boolean triggered = false;
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.SPLASH) {
                result = Dispersal.SPLASH;
            } else if (ing.kind() == Ingredient.Kind.GAS) {
                result = Dispersal.GAS;
            } else if (ing.kind() == Ingredient.Kind.LIQUID) {
                result = Dispersal.LIQUID;
            } else if (ing.kind() == Ingredient.Kind.TRIGGER) {
                triggered = true;
            }
        }
        // Source: the triggered key nullifies the splash/gas/liquid dispersals — it always wins.
        return triggered ? Dispersal.TRIGGERED : result;
    }

    public static Dispersal bottledDispersal(List<Item> items) {
        return dispersal(items);
    }

    public static boolean placeMovingBrewFluid(ServerLevel level, BlockPos pos, List<Item> items,
                                               Dispersal dispersal, Entity source) {
        return placeMovingBrewFluid(level, pos, items, dispersal, source, false);
    }

    public static boolean placeMovingBrewFluid(ServerLevel level, BlockPos pos, List<Item> items,
                                               Dispersal dispersal, Entity source, boolean persistentLiquidSource) {
        if ((dispersal != Dispersal.GAS && dispersal != Dispersal.LIQUID) || !hasEffect(items)) {
            return false;
        }
        if (!sourceMayModify(level, pos, source)) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.is(WitcheryBlocks.BREW_GAS.get())
                && !existing.is(WitcheryBlocks.BREW_LIQUID.get())
                && !com.emoniph.witchery.blocks.BrewFluidBlock.canDisplace(existing)) {
            return false;
        }
        BlockState state = (dispersal == Dispersal.GAS
                ? WitcheryBlocks.BREW_GAS.get()
                : WitcheryBlocks.BREW_LIQUID.get()).defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_NONE);
        if (!(level.getBlockEntity(pos) instanceof com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity fluid)) {
            level.removeBlock(pos, false);
            return false;
        }
        fluid.initialise(items, source, persistentLiquidSource && dispersal == Dispersal.LIQUID);
        if (dispersal == Dispersal.GAS) {
            level.scheduleTick(pos, state.getBlock(), com.emoniph.witchery.blocks.BrewFluidBlock.SOURCE_GAS_TICK_RATE);
        } else {
            level.scheduleTick(pos, state.getFluidState().getType(), state.getFluidState().getType().getTickDelay(level));
        }
        int brewColor = color(items);
        level.sendParticles(new DustParticleOptions(brewColor, dispersal == Dispersal.GAS ? 1.2F : 0.9F),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                dispersal == Dispersal.GAS ? 24 : 18,
                0.4D, 0.4D, 0.4D, 0.03D);
        level.sendParticles(dispersal == Dispersal.GAS ? ParticleTypes.WITCH : ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                dispersal == Dispersal.GAS ? 24 : 16,
                0.4D, 0.4D, 0.4D, 0.04D);
        return true;
    }

    /** Whether the brew contains at least one real effect (otherwise it's a mundane brew). */
    public static boolean hasEffect(List<Item> items) {
        Resolution resolved = resolveBrew(items);
        return !resolved.effects.isEmpty() || !resolved.actions.isEmpty();
    }

    /** Build the resolved effect instances the brew will apply (used for drink, splash and gas). */
    public static List<MobEffectInstance> buildEffects(List<Item> items) {
        return resolveBrew(items).effects.stream()
                .map(EffectApplication::effect)
                .toList();
    }

    public static List<EffectApplication> buildEffectApplications(List<Item> items) {
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
        for (EffectApplication application : resolved.effects) {
            if (!application.disableEntityTarget()) {
                applyEffect(target, application.effect(), source, 1.0, 1.0);
            }
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            for (ActionApplication action : resolved.actions) {
                if (!action.disableEntityTarget()) {
                    applyAction(serverLevel, target, action, 1.0, 1.0, source, target.position());
                }
            }
        }
    }

    public static void applyFluidContact(ServerLevel level, LivingEntity target, List<Item> items,
                                         Entity source, BlockPos fluidPos) {
        Resolution resolved = resolveBrew(items);
        if (resolved.effects.isEmpty() && resolved.actions.isEmpty()) {
            return;
        }
        boolean gasMaskProtected = target.hasEffect(WitcheryMobEffects.GAS_MASK);
        for (EffectApplication application : resolved.effects) {
            if (!application.disableEntityTarget()
                    && shouldApplyGasContactEffect(application.effect(), gasMaskProtected)) {
                applyEffect(target, application.effect(), source, SOURCE_BREW_FLUID_CONTACT_POWER,
                        SOURCE_BREW_FLUID_CONTACT_DURATION_SCALE);
            }
        }
        Vec3 impactPos = Vec3.atCenterOf(fluidPos);
        for (ActionApplication action : resolved.actions) {
            if (!action.disableEntityTarget()) {
                applyAction(level, target, action, SOURCE_BREW_FLUID_CONTACT_POWER,
                        SOURCE_BREW_FLUID_CONTACT_DURATION_SCALE, source, impactPos);
            }
        }
    }

    public static boolean shouldApplyGasContactEffect(MobEffectInstance effect, boolean gasMaskProtected) {
        return !gasMaskProtected || effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL;
    }

    public static RitualDeliveryResult validateRitualFromCauldron(ServerLevel level, BlockPos center,
                                                                  List<Item> items, Entity source) {
        Resolution resolved = resolveBrew(items);
        Dispersal delivery = dispersal(items);
        RitualDeliveryResult base = validateRitualDelivery(resolved, delivery, true, true);
        if (base != RitualDeliveryResult.SUCCESS) {
            return base;
        }
        boolean triggeredItemPresent = delivery != Dispersal.TRIGGERED || hasTriggeredRitualItemTarget(level, center);
        boolean movingBrewCanPlace = delivery != Dispersal.GAS && delivery != Dispersal.LIQUID
                || canPlaceRitualMovingBrew(level, center.above(), source);
        return validateRitualDelivery(resolved, delivery, triggeredItemPresent, movingBrewCanPlace);
    }

    public static RitualDeliveryResult validateRitualDelivery(List<Item> items, boolean triggeredItemPresent,
                                                              boolean movingBrewCanPlace) {
        return validateRitualDelivery(resolveBrew(items), dispersal(items), triggeredItemPresent, movingBrewCanPlace);
    }

    private static RitualDeliveryResult validateRitualDelivery(Resolution resolved, Dispersal delivery,
                                                               boolean triggeredItemPresent,
                                                               boolean movingBrewCanPlace) {
        if (resolved.effects.isEmpty() && resolved.actions.isEmpty()) {
            return RitualDeliveryResult.NO_EFFECT;
        }
        if (delivery == Dispersal.DRINK) {
            return RitualDeliveryResult.NEEDS_SPLASH_DISPERSAL;
        }
        if (delivery == Dispersal.TRIGGERED && !triggeredItemPresent) {
            return RitualDeliveryResult.NO_TRIGGER_ITEM;
        }
        if ((delivery == Dispersal.GAS || delivery == Dispersal.LIQUID) && !movingBrewCanPlace) {
            return RitualDeliveryResult.BLOCKED;
        }
        return RitualDeliveryResult.SUCCESS;
    }

    public static RitualDeliveryResult applyRitualFromCauldron(ServerLevel level, BlockPos center,
                                                              List<Item> items, Entity source) {
        Resolution resolved = resolveBrew(items);
        RitualDeliveryResult validation = validateRitualFromCauldron(level, center, items, source);
        if (validation != RitualDeliveryResult.SUCCESS) {
            return validation;
        }
        Dispersal delivery = dispersal(items);
        if (delivery == Dispersal.TRIGGERED) {
            return curseTriggeredRitualItem(level, center, items)
                    ? RitualDeliveryResult.SUCCESS
                    : RitualDeliveryResult.NO_TRIGGER_ITEM;
        }
        if (delivery == Dispersal.GAS || delivery == Dispersal.LIQUID) {
            return placeMovingBrewFluid(level, center.above(), items, delivery, source, delivery == Dispersal.LIQUID)
                    ? RitualDeliveryResult.SUCCESS
                    : RitualDeliveryResult.BLOCKED;
        }
        applyRitualBlockInstant(level, center, items, resolved, source);
        return RitualDeliveryResult.SUCCESS;
    }

    private static boolean canPlaceRitualMovingBrew(ServerLevel level, BlockPos pos, Entity source) {
        if (!sourceMayModify(level, pos, source)) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        return existing.is(WitcheryBlocks.BREW_GAS.get())
                || existing.is(WitcheryBlocks.BREW_LIQUID.get())
                || com.emoniph.witchery.blocks.BrewFluidBlock.canDisplace(existing);
    }

    private static void applyRitualBlockInstant(ServerLevel level, BlockPos center, List<Item> items,
                                                Resolution resolved, Entity source) {
        double radius = sourceRitualInstantRadius(resolved.extent);
        Vec3 impactPos = Vec3.atCenterOf(center);
        if (!resolved.effects.isEmpty()) {
            applyBlockImpactEffects(level, center, resolved.effects, source);
        }
        if (!resolved.actions.isEmpty()) {
            applyBlockImpactActions(level, impactPos, center, Direction.UP, resolved.actions, source, radius);
        }
        int brewColor = color(items);
        level.sendParticles(new DustParticleOptions(brewColor, 1.0F),
                center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D,
                24, 0.6D, 0.35D, 0.6D, 0.03D);
        level.sendParticles(ParticleTypes.WITCH,
                center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D,
                16, 0.5D, 0.25D, 0.5D, 0.04D);
    }

    public static int sourceRitualInstantRadius(int extent) {
        return (int) SOURCE_INSTANT_RADIUS + Math.max(0, extent);
    }

    public static boolean sourceRitualDistanceAllowed(double distanceSqr, boolean sameDimension,
                                                      int covenSize, boolean leonardPresent) {
        if (!sameDimension) {
            return covenSize >= 6 && leonardPresent;
        }
        if (covenSize >= 6) {
            return true;
        }
        int rangeScale = (1 + covenSize) * 4;
        int allowedRange = 2 * rangeScale * rangeScale;
        return distanceSqr <= (double) allowedRange * (double) allowedRange;
    }

    private static boolean hasTriggeredRitualItemTarget(ServerLevel level, BlockPos center) {
        return !level.getEntitiesOfClass(ItemEntity.class, triggeredRitualItemBounds(center),
                item -> item.isAlive() && !item.getItem().isEmpty()).isEmpty();
    }

    private static boolean curseTriggeredRitualItem(ServerLevel level, BlockPos center, List<Item> items) {
        AABB bounds = triggeredRitualItemBounds(center);
        BrewContents brew = new BrewContents(List.copyOf(items));
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (!itemEntity.isAlive() || itemEntity.getItem().isEmpty()) {
                continue;
            }
            ItemStack targetStack = splitSingleRitualCurseStack(level, itemEntity);
            int chargesToAdd = sourceTriggeredRitualItemCharges(hasNearbyCatFamiliar(level, bounds));
            BrewCurse existing = targetStack.get(WitcheryDataComponents.BREW_CURSE.get());
            int charges = existing != null && existing.brew().items().equals(brew.items())
                    ? existing.charges() + chargesToAdd
                    : chargesToAdd;
            targetStack.set(WitcheryDataComponents.BREW_CURSE.get(), new BrewCurse(brew, charges, 0L));
            level.sendParticles(new DustParticleOptions(color(items), 1.0F),
                    itemEntity.getX(), itemEntity.getY() + 0.2D, itemEntity.getZ(),
                    20, 0.5D, 0.35D, 0.5D, 0.03D);
            level.sendParticles(ParticleTypes.WITCH,
                    itemEntity.getX(), itemEntity.getY() + 0.2D, itemEntity.getZ(),
                    16, 0.5D, 0.35D, 0.5D, 0.03D);
            return true;
        }
        return false;
    }

    private static AABB triggeredRitualItemBounds(BlockPos center) {
        return new AABB(center.getX(), center.getY() + 1.0D, center.getZ(),
                center.getX() + 1.0D, center.getY() + 2.0D, center.getZ() + 1.0D);
    }

    private static ItemStack splitSingleRitualCurseStack(ServerLevel level, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() <= 1) {
            return stack;
        }
        ItemStack cursed = stack.split(1);
        itemEntity.setItem(stack);
        ItemEntity cursedEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), cursed);
        cursedEntity.setDeltaMovement(itemEntity.getDeltaMovement());
        level.addFreshEntity(cursedEntity);
        return cursed;
    }

    private static boolean hasNearbyCatFamiliar(ServerLevel level, AABB itemBounds) {
        AABB playerBounds = itemBounds.inflate(3.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, playerBounds)) {
            if (FamiliarHelper.hasActiveCatFamiliar(player)) {
                return true;
            }
        }
        return false;
    }

    public static int sourceTriggeredRitualItemCharges(boolean catFamiliarNearby) {
        return catFamiliarNearby ? SOURCE_TRIGGERED_RITUAL_CAT_ITEM_CHARGES : SOURCE_TRIGGERED_RITUAL_ITEM_CHARGES;
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal) {
        applyToArea(level, pos, items, dispersal, null);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal, LivingEntity directTarget) {
        applyToArea(level, pos, items, dispersal, directTarget, directTarget);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal,
                                   LivingEntity directTarget, Entity source) {
        applyToArea(level, pos, items, dispersal, directTarget, source, false);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal,
                                   LivingEntity directTarget, Entity source, boolean blockHit) {
        applyToArea(level, pos, items, dispersal, directTarget, source,
                blockHit ? new BlockHitResult(pos, Direction.UP, BlockPos.containing(pos), false) : null);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal,
                                   LivingEntity directTarget, Entity source, BlockHitResult blockHit) {
        Resolution resolved = resolveBrew(items);
        if (resolved.effects.isEmpty() && resolved.actions.isEmpty()) {
            return;
        }
        if (dispersal == Brews.Dispersal.GAS || dispersal == Brews.Dispersal.LIQUID) {
            placeMovingBrewFluid(level, BlockPos.containing(pos), items, dispersal, source);
            return;
        }
        List<EffectApplication> entityEffects = resolved.effects.stream()
                .filter(application -> !application.disableEntityTarget())
                .toList();
        double instantRadius = SOURCE_INSTANT_RADIUS + resolved.extent;
        AABB area = new AABB(pos.x - instantRadius, pos.y - instantRadius, pos.z - instantRadius,
                pos.x + instantRadius, pos.y + instantRadius, pos.z + instantRadius);
        if (!entityEffects.isEmpty()) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                double distance = Math.sqrt(entity.distanceToSqr(pos));
                if (distance <= instantRadius) {
                    boolean directHit = entity == directTarget;
                    double powerFactor = splashPowerFactor(distance, directHit, instantRadius);
                    double durationFactor = SOURCE_SPLASH_DURATION_SCALE * powerFactor;
                    for (EffectApplication application : entityEffects) {
                        applyEffect(entity, application.effect(), source, powerFactor, durationFactor);
                    }
                }
            }
        }
        level.sendParticles(new DustParticleOptions(color(items), 0.9F),
                pos.x, pos.y + 0.2, pos.z, 30, 1.0, 0.3, 1.0, 0.03);
        level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.2, pos.z, 30, 1.0, 0.3, 1.0, 0.1);
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 12, 0.8, 0.3, 0.8, 0.05);
        if (dispersal == Brews.Dispersal.SPLASH && blockHit != null && !resolved.effects.isEmpty()) {
            applyBlockImpactEffects(level, blockHit.getBlockPos(), resolved.effects, source);
        }
        if (!resolved.actions.isEmpty()) {
            applyAreaActions(level, pos, directTarget, resolved.actions, source, SOURCE_INSTANT_RADIUS + resolved.extent);
            if (dispersal == Brews.Dispersal.SPLASH && blockHit != null) {
                applyBlockImpactActions(level, pos, blockHit.getBlockPos(), blockHit.getDirection(),
                        resolved.actions, source, SOURCE_INSTANT_RADIUS + resolved.extent);
            }
        }
    }

    public static BlockPos sourceMovingBrewFluidPos(HitResult result) {
        if (result instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        if (result instanceof EntityHitResult entityHit) {
            return BlockPos.containing(entityHit.getEntity().position());
        }
        return BlockPos.containing(result.getLocation());
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
        if (mobEffect == WitcheryMobEffects.DISEASED.value() && isSourceDiseaseImmune(target)) {
            return;
        }
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
                if (!action.disableEntityTarget()) {
                    applyAction(level, target, action, powerFactor, SOURCE_SPLASH_DURATION_SCALE * powerFactor, source, pos);
                }
            }
        }
    }

    private static void applyAction(ServerLevel level, LivingEntity target, ActionApplication action,
                                    double powerFactor, double durationFactor, Entity source, Vec3 impactPos) {
        if (action.action() == Ingredient.BrewAction.EXTINGUISH) {
            applyExtinguish(level, target, action.strength(), source);
        } else if (action.action() == Ingredient.BrewAction.DISSIPATE) {
            applyDissipateToEntity(level, target, action.strength(), source);
        } else if (action.action() == Ingredient.BrewAction.ANIMAL) {
            applyAnimalAttraction(level, target, action.inverted(), action.strength());
        } else if (action.action() == Ingredient.BrewAction.REMOVE_BUFFS) {
            removeBuffs(target, action.strength());
        } else if (action.action() == Ingredient.BrewAction.REMOVE_DEBUFFS) {
            removeDebuffs(level, target, action.strength(), source);
        } else if (action.action() == Ingredient.BrewAction.BANE_UNDEAD) {
            applyBaneDamage(level, target, source, isSourceUndead(target), action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.BANE_ARTHROPODS) {
            applyBaneDamage(level, target, source, isSourceArthropod(target), action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.WEAKEN_VAMPIRES) {
            applyWeakenVampires(target, action, durationFactor, source);
        } else if (action.action() == Ingredient.BrewAction.BANE_DEMONS) {
            applyDemonbaneDamage(level, target, source, action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.HARM_WEREWOLVES) {
            applyHarmWerewolvesDamage(level, target, source, action.strength(),
                    action.strengthCeilingDisabled(), powerFactor);
        } else if (action.action() == Ingredient.BrewAction.STEAL_BUFFS) {
            stealBuffs(level, target, action.strength(), powerFactor, source);
        } else if (action.action() == Ingredient.BrewAction.DURATION_BOOST) {
            applyPotionLongevity(target, action.strength(), powerFactor, source);
        } else if (action.action() == Ingredient.BrewAction.AIR_HIKE) {
            applyAirHike(target, action.strength());
        } else if (action.action() == Ingredient.BrewAction.PULL) {
            applyPull(target, action.strength(), source, impactPos);
        }
    }

    private static void applyBlockImpactEffects(ServerLevel level, BlockPos impactPos, List<EffectApplication> effects,
                                                Entity source) {
        for (EffectApplication application : effects) {
            if (application.disableBlockTarget()) {
                continue;
            }
            MobEffectInstance effect = application.effect();
            MobEffect mobEffect = effect.getEffect().value();
            if (mobEffect == WitcheryMobEffects.SNOW_TRAIL.value()) {
                applySnowTrailBlockHit(level, impactPos, effect.getAmplifier(), source);
            } else if (mobEffect == WitcheryMobEffects.CHILLED.value()) {
                applyChilledBlockHit(level, impactPos, effect.getAmplifier(), source);
            }
        }
    }

    private static void applyBlockImpactActions(ServerLevel level, Vec3 impactPos, BlockPos blockImpact,
                                                Direction side, List<ActionApplication> actions, Entity source,
                                                double instantRadius) {
        int blockRadius = Mth.ceil(instantRadius);
        for (ActionApplication action : actions) {
            if (action.disableBlockTarget()) {
                continue;
            }
            if (action.action() == Ingredient.BrewAction.PULL) {
                applyPullToNonLivingEntities(level, impactPos, action.strength(), source, instantRadius);
            } else if (action.action() == Ingredient.BrewAction.EXTINGUISH) {
                extinguishFires(level, blockImpact, blockRadius, action.strength(), source);
            } else if (action.action() == Ingredient.BrewAction.DISSIPATE) {
                dissipateGasClouds(level, blockImpact, blockRadius, action.strength());
            } else if (action.action() == Ingredient.BrewAction.GROW_FLOWERS) {
                growFlowers(level, blockImpact, blockRadius, action.strength(), source);
            } else if (action.action() == Ingredient.BrewAction.FERTILIZE) {
                fertilizeBlocks(level, blockImpact, blockRadius, action.strength(), source);
            }
        }
    }

    /**
     * Source moving-brew-fluid adjacent-block pulse (liquid only, {@code BlockBrewLiquidEffect}): on each
     * scheduled tick, every face of a brew-liquid block has a {@value #SOURCE_ADJACENT_BLOCK_PULSE_CHANCE}
     * chance to apply the brew's BLOCK actions/effects to the neighbouring block at reduced strength
     * (source {@code strengthPenalty++}). So a brew pool slowly grows flowers / fertilises / extinguishes
     * fires / lays snow / freezes water around itself. Most brews carry no block action, so this no-ops.
     */
    public static void tickAdjacentBlockPulse(ServerLevel level, BlockPos fluidPos, List<Item> items, Entity source) {
        Resolution resolved = resolveBrew(items);
        if (resolved.actions.isEmpty() && resolved.effects.isEmpty()) {
            return;
        }
        List<ActionApplication> weakActions = penaliseActionsForPulse(resolved.actions);
        List<EffectApplication> weakEffects = penaliseEffectsForPulse(resolved.effects);
        if (weakActions.isEmpty() && weakEffects.isEmpty()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (level.random.nextDouble() >= SOURCE_ADJACENT_BLOCK_PULSE_CHANCE) {
                continue;
            }
            BlockPos target = fluidPos.relative(direction);
            if (!weakActions.isEmpty()) {
                applyBlockImpactActions(level, Vec3.atCenterOf(target), target, direction.getOpposite(), weakActions, source,
                        SOURCE_ADJACENT_BLOCK_PULSE_RADIUS);
            }
            if (!weakEffects.isEmpty()) {
                applyBlockImpactEffects(level, target, weakEffects, source);
            }
        }
    }

    /** Reduced strength for the per-tick adjacent-block pulse (source {@code strengthPenalty++}). */
    public static int pulsePenalisedStrength(int strength) {
        return Math.max(0, strength - 1);
    }

    private static List<ActionApplication> penaliseActionsForPulse(List<ActionApplication> actions) {
        List<ActionApplication> out = new ArrayList<>(actions.size());
        for (ActionApplication action : actions) {
            if (action.disableBlockTarget()) {
                continue;
            }
            out.add(new ActionApplication(action.action(), action.inverted(),
                    pulsePenalisedStrength(action.strength()), action.duration(), action.noParticles(),
                    action.disableBlockTarget(), action.disableEntityTarget(), action.strengthCeilingDisabled(),
                    action.nameKey()));
        }
        return out;
    }

    private static List<EffectApplication> penaliseEffectsForPulse(List<EffectApplication> effects) {
        List<EffectApplication> out = new ArrayList<>();
        for (EffectApplication application : effects) {
            if (application.disableBlockTarget()) {
                continue;
            }
            MobEffectInstance effect = application.effect();
            MobEffect mob = effect.getEffect().value();
            // Only Snow Trail and Chilled have a block-hit action; other effects act on entities only.
            if (mob != WitcheryMobEffects.SNOW_TRAIL.value() && mob != WitcheryMobEffects.CHILLED.value()) {
                continue;
            }
            MobEffectInstance weak = new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                    pulsePenalisedStrength(effect.getAmplifier()), effect.isAmbient(), effect.isVisible(),
                    effect.showIcon());
            out.add(new EffectApplication(weak, application.disableBlockTarget(), application.disableEntityTarget()));
        }
        return out;
    }

    private static void applySnowTrailBlockHit(ServerLevel level, BlockPos impactPos, int strength, Entity source) {
        if (!sourceMayModify(level, impactPos, source)) {
            return;
        }
        SnowTrailMobEffect.createSnowCovering(level, impactPos, sourceBlockHitRadius(strength));
    }

    private static void applyChilledBlockHit(ServerLevel level, BlockPos impactPos, int strength, Entity source) {
        if (!sourceMayModify(level, impactPos, source)) {
            return;
        }
        replaceSourceConnectedWaterSphere(level, impactPos, sourceBlockHitRadius(strength));
    }

    private static boolean sourceMayModify(ServerLevel level, BlockPos pos, Entity source) {
        return !(source instanceof Player player) || level.mayInteract(player, pos);
    }

    private static void replaceSourceConnectedWaterSphere(ServerLevel level, BlockPos center, int radius) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> processed = new HashSet<>();
        queue.add(center);
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            if (!processed.add(current) || !sourceReplaceSphereCanRecurse(center, current, radius)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (sourceColdBlockHitShouldFreeze(level.getBlockState(next))) {
                    level.setBlockAndUpdate(next, Blocks.ICE.defaultBlockState());
                    queue.add(next);
                }
            }
        }
    }

    private static void applyExtinguish(ServerLevel level, LivingEntity target, int strength, Entity source) {
        if (!sourceExtinguishApplies(level, strength)) {
            return;
        }
        if (strength > 0 && target instanceof Blaze) {
            target.hurtServer(level, sourceBaneDamageSource(level, source), sourceExtinguishBlazeDamage(strength));
        }
        if (target.isOnFire()) {
            target.clearFire();
        }
    }

    private static void applyDissipateToEntity(ServerLevel level, LivingEntity target, int strength, Entity source) {
        if (isSourceSummonedUndead(target)) {
            target.hurtServer(level, sourceBaneDamageSource(level, source), sourceDissipateUndeadDamage(strength));
        }
    }

    private static void extinguishFires(ServerLevel level, BlockPos center, int radius, int strength, Entity source) {
        if (!sourceExtinguishApplies(level, strength)) {
            return;
        }
        int effectiveRadius = radius;
        for (BlockPos column : SnowTrailMobEffect.sourceFilledCircle(center, effectiveRadius)) {
            for (int y = center.getY() - effectiveRadius; y <= center.getY() + effectiveRadius; y++) {
                BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                if (level.getBlockState(pos).is(Blocks.FIRE) && sourceMayModify(level, pos, source)) {
                    level.removeBlock(pos, false);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 2.0F);
                }
            }
        }
    }

    private static void dissipateGasClouds(ServerLevel level, BlockPos center, int radius, int strength) {
        AABB area = new AABB(center).inflate(radius);
        Set<Long> sourceColumns = sourceCircleColumns(center, radius);
        for (BlockPos mutablePos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockPos pos = mutablePos.immutable();
            if (!sourceColumns.contains(BlockPos.asLong(pos.getX(), center.getY(), pos.getZ()))) {
                continue;
            }
            if (sourceDissipateRemovesGas(level.getBlockState(pos).is(WitcheryBlocks.BREW_GAS.get()), false, strength)) {
                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 2.0F);
            }
        }
        for (AreaEffectCloud cloud : level.getEntitiesOfClass(AreaEffectCloud.class, area)) {
            BlockPos pos = cloud.blockPosition();
            if (pos.getY() < center.getY() - radius || pos.getY() > center.getY() + radius) {
                continue;
            }
            if (!sourceColumns.contains(BlockPos.asLong(pos.getX(), center.getY(), pos.getZ()))) {
                continue;
            }
            if (sourceDissipateRemovesGas(isWitcheryBrewGasCloud(cloud), false, strength)) {
                cloud.discard();
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 2.0F);
            }
        }
    }

    private static void growFlowers(ServerLevel level, BlockPos center, int radius, int strength, Entity source) {
        int effectiveRadius = sourceBlockCircleRadius(radius, strength);
        for (BlockPos column : SnowTrailMobEffect.sourceFilledCircle(center, effectiveRadius)) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = column.above(dy);
                if (sourceCanGrowFlowerAt(level, pos, source)
                        && level.random.nextInt(sourceGrowFlowersChanceBound(strength)) == 0) {
                    level.setBlockAndUpdate(pos, SOURCE_FLOWER_STATES[level.random.nextInt(SOURCE_FLOWER_STATES.length)]);
                }
            }
        }
    }

    private static void fertilizeBlocks(ServerLevel level, BlockPos center, int radius, int strength, Entity source) {
        int effectiveRadius = sourceBlockCircleRadius(radius, strength);
        for (BlockPos column : SnowTrailMobEffect.sourceFilledCircle(center, effectiveRadius)) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos pos = column.above(dy);
                BlockState state = level.getBlockState(pos);
                if (sourceCanFertilize(state, level, pos, source)) {
                    for (int i = 0; i < sourceFertilizationApplications(strength); i++) {
                        BlockState current = level.getBlockState(pos);
                        if (current.getBlock() instanceof BonemealableBlock bonemealable
                                && bonemealable.isValidBonemealTarget(level, pos, current)
                                && bonemealable.isBonemealSuccess(level, level.random, pos, current)) {
                            bonemealable.performBonemeal(level, level.random, pos, current);
                        }
                    }
                    break;
                }
            }
        }
    }

    private static Set<Long> sourceCircleColumns(BlockPos center, int radius) {
        Set<Long> columns = new HashSet<>();
        for (BlockPos pos : SnowTrailMobEffect.sourceFilledCircle(center, radius)) {
            columns.add(BlockPos.asLong(pos.getX(), center.getY(), pos.getZ()));
        }
        return columns;
    }

    private static boolean isWitcheryBrewGasCloud(AreaEffectCloud cloud) {
        return cloud.getPersistentData().getBooleanOr(SOURCE_BREW_GAS_TAG, false)
                || cloud.getPersistentData().getBooleanOr(PORT_BREW_GAS_TAG, false);
    }

    public static void tagSourceBrewGasCloud(AreaEffectCloud cloud) {
        cloud.getPersistentData().putBoolean(SOURCE_BREW_GAS_TAG, true);
        cloud.getPersistentData().putBoolean(PORT_BREW_GAS_TAG, true);
    }

    private static boolean sourceCanGrowFlowerAt(ServerLevel level, BlockPos pos, Entity source) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced()
                && !state.liquid()
                && Blocks.DANDELION.defaultBlockState().canSurvive(level, pos)
                && sourceMayModify(level, pos, source);
    }

    private static boolean sourceCanFertilize(BlockState state, ServerLevel level, BlockPos pos, Entity source) {
        return state.getBlock() instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, pos, state)
                && sourceMayModify(level, pos, source);
    }

    private static void applyPullToNonLivingEntities(ServerLevel level, Vec3 impactPos, int strength,
                                                     Entity source, double radius) {
        AABB area = new AABB(impactPos.x - radius, impactPos.y - radius, impactPos.z - radius,
                impactPos.x + radius, impactPos.y + radius, impactPos.z + radius);
        Vec3 pullTarget = sourcePullTarget(impactPos, source, strength);
        for (Entity entity : level.getEntities((Entity) null, area,
                entity -> !(entity instanceof LivingEntity) && entity.isAlive())) {
            if (entity.distanceToSqr(impactPos) <= radius * radius) {
                applySourcePull(entity, pullTarget);
            }
        }
    }

    private static void applyPull(LivingEntity target, int strength, Entity source, Vec3 impactPos) {
        Vec3 pullTarget = sourcePullTarget(impactPos, source, strength);
        if (applySourcePull(target, pullTarget)) {
            target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,
                    SOURCE_PULL_JUMP_TICKS, SOURCE_PULL_JUMP_AMPLIFIER), source);
        }
    }

    private static Vec3 sourcePullTarget(Vec3 impactPos, Entity source, int strength) {
        if (sourcePullUsesCaster(strength, isFakePlayer(source)) && source != null) {
            return source.position();
        }
        return impactPos;
    }

    private static boolean applySourcePull(Entity entity, Vec3 targetPosition) {
        if (entity instanceof EnderDragon || entity instanceof HornedHuntsmanEntity) {
            return false;
        }
        Vec3 motion = sourcePullMotion(entity.position(), targetPosition);
        if (motion == Vec3.ZERO) {
            return false;
        }
        entity.setDeltaMovement(motion);
        entity.hurtMarked = true;
        return true;
    }

    private static boolean isFakePlayer(Entity entity) {
        return entity != null && entity.getClass().getSimpleName().equals("FakePlayer");
    }

    private static void applyAirHike(LivingEntity target, int strength) {
        Vec3 motion = target.getDeltaMovement();
        target.fallDistance = 0.0F;
        target.setDeltaMovement(motion.x, sourceAirHikeMotionY(strength), motion.z);
        target.hurtMarked = true;
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

    private static void applyDemonbaneDamage(ServerLevel level, LivingEntity target, Entity source,
                                             int strength, boolean strengthCeilingDisabled, double powerFactor) {
        if (target instanceof LeonardEntity leonard) {
            int damage = sourceBaneDamage(true, strength, strengthCeilingDisabled, powerFactor);
            if (damage > 0) {
                leonard.hurtFromDemonbaneWeakness(level, damage);
            }
        } else {
            applyBaneDamage(level, target, source, isSourceDemonic(target), strength,
                    strengthCeilingDisabled, powerFactor);
        }
    }

    private static void applyHarmWerewolvesDamage(ServerLevel level, LivingEntity target, Entity source,
                                                  int strength, boolean strengthCeilingDisabled,
                                                  double powerFactor) {
        boolean matchingTarget = isSourceWerewolf(target);
        int damage = sourceHarmWerewolvesDamage(matchingTarget, strength, strengthCeilingDisabled, powerFactor);
        if (damage <= 0) {
            return;
        }
        DamageSource damageSource = sourceBaneDamageSource(level, source);
        if (matchingTarget) {
            SilverEvents.hurtWithBrewSilver(level, target, damageSource, damage);
        } else {
            target.hurtServer(level, damageSource, damage);
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

    public static int sourceHarmWerewolvesDamage(boolean matchingTarget, int strength,
                                                 boolean strengthCeilingDisabled, double powerFactor) {
        int cappedStrength = Math.min(strength, strengthCeilingDisabled ? 3 : 1);
        int baseDamage = matchingTarget ? 3 : 1;
        return Mth.ceil((double) (baseDamage << cappedStrength) * powerFactor);
    }

    public static double sourceAirHikeMotionY(int strength) {
        return 0.6D + 0.2D * (double) strength;
    }

    public static boolean sourcePullUsesCaster(int strength, boolean fakeCaster) {
        return strength > 0 && !fakeCaster;
    }

    public static Vec3 sourcePullMotion(Vec3 entityPosition, Vec3 targetPosition) {
        Vec3 delta = targetPosition.subtract(entityPosition);
        if (delta.lengthSqr() < 0.0001D) {
            return Vec3.ZERO;
        }
        return new Vec3(delta.x * SOURCE_PULL_HORIZONTAL_SCALE,
                SOURCE_PULL_VERTICAL_MOTION,
                delta.z * SOURCE_PULL_HORIZONTAL_SCALE);
    }

    public static int sourceBlockHitRadius(int strength) {
        return 2 + 2 * strength;
    }

    public static boolean sourceExtinguishApplies(ServerLevel level, int strength) {
        return sourceExtinguishAppliesInDimension(level.dimension() == Level.NETHER, strength);
    }

    public static boolean sourceExtinguishAppliesInDimension(boolean nether, int strength) {
        return strength > 1 || !nether;
    }

    public static float sourceExtinguishBlazeDamage(int strength) {
        return strength > 0 ? (float) (strength + 1) * 4.0F : 0.0F;
    }

    public static float sourceDissipateUndeadDamage(int strength) {
        return (float) (strength + 1) * 5.0F;
    }

    public static boolean sourceDissipateRemovesGas(boolean witcheryBrewGas, boolean gaseousFluidBlock, int strength) {
        return witcheryBrewGas || gaseousFluidBlock && strength >= 1;
    }

    public static int sourceBlockCircleRadius(int radius, int strength) {
        return radius + strength;
    }

    public static int sourceGrowFlowersChanceBound(int strength) {
        return 8 - strength;
    }

    public static int sourceFlowerVariantCount() {
        return SOURCE_FLOWER_STATES.length;
    }

    public static int sourceFertilizationApplications(int strength) {
        return strength + 1;
    }

    public static boolean sourceColdBlockHitShouldFreeze(BlockState state) {
        return state.is(Blocks.WATER);
    }

    public static boolean sourceReplaceSphereCanRecurse(BlockPos center, BlockPos current, int radius) {
        int dx = center.getX() - current.getX();
        int dy = center.getY() - current.getY();
        int dz = center.getZ() - current.getZ();
        return dx * dx + dy * dy + dz * dz < radius * radius;
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

    public static boolean isSourceWerewolf(LivingEntity target) {
        if (target instanceof WolfmanEntity) {
            return true;
        }
        if (target instanceof Player player) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            return sourceWerewolfPlayerMatches(data.isWerewolf(), WerewolfShiftUtil.isTransformed(data));
        }
        return false;
    }

    public static boolean sourceWerewolfPlayerMatches(boolean werewolf, boolean transformed) {
        return werewolf && transformed;
    }

    public static boolean isSourceDemonic(LivingEntity target) {
        return target instanceof DemonEntity
                || target instanceof Ghast
                || target instanceof Blaze
                || target instanceof MagmaCube
                || target instanceof LeonardEntity
                || target instanceof LordOfTormentEntity
                || target instanceof FlameImpEntity
                || target instanceof LilithEntity
                || target instanceof WitherBoss;
    }

    public static boolean isSourceDiseaseImmune(LivingEntity target) {
        return isSourceUndead(target)
                || isSourceDemonic(target)
                || isSourceWerewolf(target)
                || target instanceof EnderDragon
                || target instanceof WitherBoss
                || target instanceof IronGolem
                || target instanceof SnowGolem
                || target instanceof BabaYagaEntity
                || target instanceof DeathEntity
                || target instanceof GoblinGulgEntity
                || target instanceof GoblinMogEntity
                || target instanceof HornedHuntsmanEntity
                || target instanceof LeonardEntity
                || target instanceof LordOfTormentEntity;
    }

    public static boolean isSourceSummonedUndead(LivingEntity target) {
        return target.getType().is(EntityTypeTags.UNDEAD)
                && (target.getPersistentData().contains(SOURCE_SUMMONED_UNDEAD_OWNER_TAG)
                || target.getPersistentData().contains(PORT_SUMMONED_UNDEAD_OWNER_TAG));
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

    private static void removeDebuffs(ServerLevel level, LivingEntity target, int strength, Entity source) {
        List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (shouldRemoveDebuff(effect, strength)) {
                effectsToRemove.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> effect : effectsToRemove) {
            target.removeEffect(effect);
        }
        if (sourcePurifiedMilkClearsDiseaseBlocks(strength)) {
            clearDiseaseBlocks(level, target.blockPosition(), source);
        }
    }

    private static void stealBuffs(ServerLevel level, LivingEntity target, int strength, double powerFactor,
                                   Entity source) {
        double radius = sourceStealBuffsRadius(strength);
        double radiusSq = radius * radius;
        AABB bounds = target.getBoundingBox().inflate(radius, radius, radius);
        int maxBuffs = sourceStealBuffsMaxBuffs(strength, powerFactor);
        if (maxBuffs <= 0) {
            return;
        }

        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (other == target || other.distanceToSqr(target) >= radiusSq) {
                continue;
            }
            List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();
            for (MobEffectInstance effect : other.getActiveEffects()) {
                MobEffectInstance current = target.getEffect(effect.getEffect());
                if (shouldStealBuff(effect, current, strength)) {
                    target.addEffect(new MobEffectInstance(effect), source);
                    effectsToRemove.add(effect.getEffect());
                    maxBuffs--;
                    if (maxBuffs <= 0) {
                        break;
                    }
                }
            }
            for (Holder<MobEffect> effect : effectsToRemove) {
                other.removeEffect(effect);
            }
            if (maxBuffs <= 0) {
                break;
            }
        }
    }

    private static void applyPotionLongevity(LivingEntity target, int strength, double powerFactor, Entity source) {
        if (target.hasEffect(WitcheryMobEffects.QUEASY)) {
            if (target.getRandom().nextInt(3) == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER,
                        SOURCE_POTION_LONGEVITY_HUNGER_TICKS, 0, true, true, true), source);
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,
                        SOURCE_POTION_LONGEVITY_NAUSEA_TICKS, 0, true, true, true), source);
            }
            return;
        }

        List<MobEffectInstance> activeEffects = new ArrayList<>(target.getActiveEffects());
        if (activeEffects.isEmpty()) {
            return;
        }

        List<MobEffectInstance> boostedEffects = new ArrayList<>();
        for (MobEffectInstance effect : activeEffects) {
            if (!effect.getEffect().value().isInstantenous()) {
                boostedEffects.add(new MobEffectInstance(effect.getEffect(),
                        sourcePotionLongevityDuration(effect.getDuration(), powerFactor),
                        effect.getAmplifier(), effect.isAmbient(), true, true));
            }
        }

        for (MobEffectInstance effect : activeEffects) {
            target.removeEffect(effect.getEffect());
        }
        for (MobEffectInstance effect : boostedEffects) {
            target.addEffect(effect, source);
        }
        target.addEffect(new MobEffectInstance(WitcheryMobEffects.QUEASY,
                sourcePotionLongevityQueasyDuration(strength), 0, true, true, true), source);
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
        if (isSourceDiseaseEffect(effect)) {
            return sourcePurifiedMilkClearsDisease(strength);
        }
        return amplifier <= strength
                && effect.value().getCategory() == MobEffectCategory.HARMFUL
                && curable;
    }

    public static boolean sourcePurifiedMilkClearsDisease(int strength) {
        return strength >= SOURCE_PURIFIED_MILK_DISEASE_CURE_STRENGTH;
    }

    public static boolean sourcePurifiedMilkClearsDiseaseBlocks(int strength) {
        return strength >= SOURCE_PURIFIED_MILK_DISEASE_BLOCK_CLEANUP_STRENGTH;
    }

    public static double sourceRitualCureDebuffReapplyChance(int effectAmplifier, int modifiedStrength) {
        if (effectAmplifier < modifiedStrength) {
            return 0.01D;
        }
        return effectAmplifier == modifiedStrength ? 0.25D : 0.75D;
    }

    public static int sourceRitualCureDebuffAmplifierAfterBackfire(int effectAmplifier) {
        return Math.max(0, effectAmplifier) + 1;
    }

    public static boolean sourceRitualCureDebuffsRemoves(MobEffectCategory category) {
        return category == MobEffectCategory.HARMFUL;
    }

    public static double sourceStealBuffsRadius(int strength) {
        return (double) (strength + 1) * 8.0;
    }

    public static int sourceStealBuffsMaxBuffs(int strength, double powerFactor) {
        return Mth.ceil(((double) strength + 1.0) * 2.0 * powerFactor);
    }

    public static boolean shouldStealBuff(MobEffectInstance donor, MobEffectInstance current, int strength) {
        return shouldStealBuff(donor.getEffect(), donor.getDuration(), donor.getAmplifier(),
                isCurableByModernApi(donor), current == null ? -1 : current.getDuration(),
                current == null ? -1 : current.getAmplifier(), strength);
    }

    public static boolean shouldStealBuff(Holder<MobEffect> effect, int donorDuration, int donorAmplifier,
                                          boolean curable, int currentDuration, int currentAmplifier,
                                          int strength) {
        if (!shouldRemoveBuff(effect, donorAmplifier, curable, strength)) {
            return false;
        }
        return currentDuration < 0
                || (currentDuration <= donorDuration && currentAmplifier <= donorAmplifier);
    }

    public static int sourcePotionLongevityBoostTicks(double powerFactor) {
        return Mth.ceil(powerFactor * (double) SOURCE_POTION_LONGEVITY_BOOST_TICKS);
    }

    public static int sourcePotionLongevityDuration(int remainingTicks, double powerFactor) {
        int clampedRemaining = Math.max(0, remainingTicks);
        int boost = Math.min(clampedRemaining, sourcePotionLongevityBoostTicks(powerFactor));
        return (int) Math.min((long) clampedRemaining + boost, (long) Integer.MAX_VALUE);
    }

    public static int sourcePotionLongevityQueasyDuration(int strength) {
        return 3 * Math.max(1, 4 - strength) * 60 * 20;
    }

    public static int sourcePotionLongevityBackfireDuration(boolean hungerRoll) {
        return hungerRoll ? SOURCE_POTION_LONGEVITY_HUNGER_TICKS : SOURCE_POTION_LONGEVITY_NAUSEA_TICKS;
    }

    public static boolean isSourceDiseaseEffect(Holder<MobEffect> effect) {
        return effect.value() == WitcheryMobEffects.DISEASED.value();
    }

    public static boolean isSourceDiseaseBlock(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "witchery".equals(id.getNamespace())
                && ("disease".equals(id.getPath())
                || "fluiddisease".equals(id.getPath())
                || "disease_block".equals(id.getPath()));
    }

    private static void clearDiseaseBlocks(ServerLevel level, BlockPos center, Entity source) {
        int radius = SOURCE_PURIFIED_MILK_DISEASE_BLOCK_CLEANUP_RADIUS;
        BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)).forEach(pos -> {
            BlockPos immutable = pos.immutable();
            if (isSourceDiseaseBlock(level.getBlockState(immutable))
                    && sourceMayModify(level, immutable, source)) {
                level.removeBlock(immutable, false);
            }
        });
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

    public static int impactLifetime(List<Item> items) {
        return resolveBrew(items).lifetime;
    }

    public static int sourceBrewFluidDecayDenominator(int lifetime) {
        int clamped = Math.clamp(lifetime, 0, 3);
        return SOURCE_BREW_FLUID_BASE_DECAY_DENOMINATOR + clamped * clamped * 5;
    }

    public static int sourceBrewFluidExpansion(int extent) {
        return Math.min(4 + Math.max(0, extent), 10);
    }

    public static int gasCloudDurationTicks(List<Item> items) {
        return gasCloudDurationTicks(resolveBrew(items).lifetime);
    }

    public static int gasCloudDurationTicks(int lifetime) {
        return SOURCE_GAS_CLOUD_BASE_DURATION_TICKS
                * sourceBrewFluidDecayDenominator(lifetime)
                / SOURCE_BREW_FLUID_BASE_DECAY_DENOMINATOR;
    }

    public static int sourceBrewGasMaxRunTicks(int lifetime) {
        return Math.max(1, gasCloudDurationTicks(lifetime) / 5);
    }

    public static float gasCloudRadiusPerTick(int durationTicks) {
        return durationTicks > 0 ? (float) (-SOURCE_INSTANT_RADIUS / durationTicks) : 0.0F;
    }

    public static Identifier id() {
        return Identifier.fromNamespaceAndPath("witchery", "brews");
    }
}
