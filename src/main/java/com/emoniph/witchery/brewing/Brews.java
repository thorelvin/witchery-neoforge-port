package com.emoniph.witchery.brewing;

import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
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
                             BrewAction action, int baseDuration, int strengthCeiling, int color,
                             int modifierCeiling, int capacityIncrement, int capacityCeiling,
                             int capacityRequired, int powerCost,
                             String nameKey, String invertedNameKey) {
        public enum Kind { EFFECT, ACTION, CAPACITY, STRENGTH, DURATION, INVERT, SPLASH, GAS, COLOR }

        public enum BrewAction { ANIMAL }

        static Ingredient effect(Holder<MobEffect> effect, int baseDuration, int ceiling, int color) {
            return new Ingredient(Kind.EFFECT, effect, null, null, baseDuration, ceiling, color,
                    0, 0, 0, 0, 0, null, null);
        }

        static Ingredient effect(Holder<MobEffect> effect, Holder<MobEffect> inverted, int baseDuration, int ceiling, int color) {
            return new Ingredient(Kind.EFFECT, effect, inverted, null, baseDuration, ceiling, color,
                    0, 0, 0, 0, 0, null, null);
        }

        static Ingredient action(BrewAction action, int capacityRequired, int powerCost, int color,
                                 String nameKey, String invertedNameKey) {
            return new Ingredient(Kind.ACTION, null, null, action, 0, 0, color,
                    0, 0, 0, capacityRequired, powerCost, nameKey, invertedNameKey);
        }

        static Ingredient capacity(int increment, int ceiling, int powerCost) {
            return new Ingredient(Kind.CAPACITY, null, null, null, 0, 0, 0,
                    0, increment, ceiling, 0, powerCost, null, null);
        }

        static Ingredient color(int color) {
            return new Ingredient(Kind.COLOR, null, null, null, 0, 0, color,
                    0, 0, 0, 0, 0, null, null);
        }

        static Ingredient modifier(Kind kind) {
            return new Ingredient(kind, null, null, null, 0, 0, 0,
                    0, 0, 0, 0, 0, null, null);
        }

        static Ingredient modifier(Kind kind, int ceiling, int powerCost) {
            return new Ingredient(kind, null, null, null, 0, 0, 0,
                    ceiling, 0, 0, 0, powerCost, null, null);
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

    public record ActionApplication(Ingredient.BrewAction action, boolean inverted, int strength, String nameKey) {}

    private static final class Resolution {
        final List<MobEffectInstance> effects = new ArrayList<>();
        final List<ActionApplication> actions = new ArrayList<>();
        int capacity;
        int usedCapacity;
        int powerCost;
        boolean blockedByCapacity;
        int blockedRequired;
        int blockedAvailable;
    }

    private static final Map<Item, Ingredient> REGISTRY = new LinkedHashMap<>();
    private static final Set<Item> EXPLICIT_STIR_ONLY = new HashSet<>();
    private static final Method TAMABLE_SET_OWNER_UUID = findTamableSetOwnerUuid();

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
        register(Items.SPIDER_EYE, Ingredient.effect(MobEffects.POISON, MobEffects.REGENERATION, 300, 1, 0x4E9331));
        register(Items.MAGMA_CREAM, Ingredient.effect(MobEffects.FIRE_RESISTANCE, 3600, 0, 0xE49A3A));
        register(Items.SUGAR, Ingredient.effect(MobEffects.SPEED, MobEffects.SLOWNESS, 1800, 2, 0x7CAFC6));
        register(Items.GHAST_TEAR, Ingredient.effect(MobEffects.REGENERATION, MobEffects.POISON, 900, 1, 0xCD5CAB));
        register(Items.RABBIT_FOOT, Ingredient.effect(MobEffects.JUMP_BOOST, 1800, 2, 0x22FF4C));
        register(Items.PUFFERFISH, Ingredient.effect(MobEffects.WATER_BREATHING, 3600, 0, 0x2E5299));
        register(Items.GOLDEN_CARROT, Ingredient.effect(MobEffects.NIGHT_VISION, MobEffects.BLINDNESS, 3600, 0, 0x1F1FA1));
        register(Items.GLISTERING_MELON_SLICE, Ingredient.effect(MobEffects.HEALTH_BOOST, 3600, 1, 0xBC1A1A));
        register(Items.PHANTOM_MEMBRANE, Ingredient.effect(MobEffects.SLOW_FALLING, 1800, 0, 0xCAC5B6));
        register(WitcheryItems.WORMWOOD.get(),
                Ingredient.effect(MobEffects.INVISIBILITY, 1800, 0, 0x7F8392));
        register(WitcheryItems.GOLDEN_HEART.get(), Ingredient.action(Ingredient.BrewAction.ANIMAL,
                4, 500, 0xF2C94C,
                "witchery.brew.animal_attraction", "witchery.brew.animal_repulsion"));

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
        registerExplicit(Items.BLAZE_POWDER, Ingredient.modifier(Ingredient.Kind.STRENGTH, 2, 100));
        register(WitcheryItems.ATTUNED_STONE_CHARGED.get(), Ingredient.modifier(Ingredient.Kind.STRENGTH, 3, 150));
        register(Items.REDSTONE, Ingredient.modifier(Ingredient.Kind.DURATION, 1, 50));
        register(Items.OBSIDIAN, Ingredient.modifier(Ingredient.Kind.DURATION, 2, 100));
        register(WitcheryItems.MINDRAKE_BULB.get(), Ingredient.modifier(Ingredient.Kind.DURATION, 3, 150));
        register(Items.FERMENTED_SPIDER_EYE, Ingredient.modifier(Ingredient.Kind.INVERT, 0, 25));
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
                case SPLASH, GAS, COLOR -> { /* presentation/dispersal only */ }
                case EFFECT -> {
                    Holder<MobEffect> effect = mods.inverted && ing.inverted() != null ? ing.inverted() : ing.effect();
                    int amplifier = Math.min(mods.strength, ing.strengthCeiling());
                    int duration = ing.baseDuration() * (mods.duration + 1);
                    resolved.effects.add(new MobEffectInstance(effect, duration, amplifier));
                    mods.reset();
                }
                case ACTION -> {
                    if (resolved.usedCapacity + ing.capacityRequired() <= resolved.capacity) {
                        boolean inverted = mods.inverted && ing.invertedNameKey() != null;
                        String nameKey = inverted ? ing.invertedNameKey() : ing.nameKey();
                        resolved.actions.add(new ActionApplication(ing.action(), inverted, mods.strength, nameKey));
                        resolved.usedCapacity += ing.capacityRequired();
                        mods.reset();
                    } else {
                        resolved.blockedByCapacity = true;
                        resolved.blockedRequired = Math.max(resolved.blockedRequired, ing.capacityRequired());
                        resolved.blockedAvailable = Math.max(resolved.blockedAvailable, resolved.capacity - resolved.usedCapacity);
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
            case ACTION -> current.usedCapacity + ing.capacityRequired() <= current.capacity;
            default -> true;
        };
    }

    public static int powerCost(List<Item> items) {
        return resolveBrew(items).powerCost;
    }

    public static boolean blockedByCapacity(List<Item> items) {
        return resolveBrew(items).blockedByCapacity;
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
        Resolution resolved = resolveBrew(items);
        for (MobEffectInstance effect : resolved.effects) {
            target.addEffect(new MobEffectInstance(effect));
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            for (ActionApplication action : resolved.actions) {
                applyAction(serverLevel, target, action);
            }
        }
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal) {
        applyToArea(level, pos, items, dispersal, null);
    }

    public static void applyToArea(ServerLevel level, Vec3 pos, List<Item> items, Dispersal dispersal, LivingEntity directTarget) {
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
            AABB area = new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 2, pos.z + 4);
            if (!resolved.effects.isEmpty()) {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    for (MobEffectInstance effect : resolved.effects) {
                        entity.addEffect(new MobEffectInstance(effect));
                    }
                }
            }
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.2, pos.z, 30, 1.0, 0.3, 1.0, 0.1);
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.2, pos.z, 12, 0.8, 0.3, 0.8, 0.05);
        }
        if (!resolved.actions.isEmpty()) {
            applyAreaActions(level, pos, directTarget, resolved.actions);
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

    private static void applyAreaActions(ServerLevel level, Vec3 pos, LivingEntity directTarget, List<ActionApplication> actions) {
        if (directTarget != null && directTarget.isAlive()) {
            for (ActionApplication action : actions) {
                applyAction(level, directTarget, action);
            }
            return;
        }

        AABB area = new AABB(pos.x - 4, pos.y - 2, pos.z - 4, pos.x + 4, pos.y + 2, pos.z + 4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            for (ActionApplication action : actions) {
                applyAction(level, target, action);
            }
        }
    }

    private static void applyAction(ServerLevel level, LivingEntity target, ActionApplication action) {
        if (action.action() == Ingredient.BrewAction.ANIMAL) {
            applyAnimalAttraction(level, target, action.inverted(), action.strength());
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
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.EFFECT || ing.kind() == Ingredient.Kind.ACTION) {
                r += (ing.color() >> 16) & 0xFF;
                g += (ing.color() >> 8) & 0xFF;
                b += ing.color() & 0xFF;
                count++;
            } else if (ing.kind() == Ingredient.Kind.COLOR) {
                forcedColor = ing.color();
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
                    Holder<MobEffect> effect = pendingInvert && ing.inverted() != null ? ing.inverted() : ing.effect();
                    primary = Component.translatable(effect.value().getDescriptionId());
                    strong = strength > 0;
                    lengthy = duration > 0;
                    strength = 0;
                    duration = 0;
                    pendingInvert = false;
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
        for (Ingredient ing : resolve(items)) {
            if (ing.kind() == Ingredient.Kind.STRENGTH) {
                if (strength < ing.modifierCeiling()) {
                    strength++;
                }
            } else if (ing.kind() == Ingredient.Kind.EFFECT || ing.kind() == Ingredient.Kind.ACTION) {
                appliedStrength = Math.max(appliedStrength, strength);
                strength = 0;
            }
        }
        appliedStrength = Math.max(appliedStrength, strength);
        return Math.clamp(1 + appliedStrength, 1, 5);
    }

    public static Identifier id() {
        return Identifier.fromNamespaceAndPath("witchery", "brews");
    }
}
