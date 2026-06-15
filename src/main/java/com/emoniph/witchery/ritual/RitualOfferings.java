package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Offering scanning for ritual dispatch: the primitive nearby-item / grassper scanners and the
 * {@code find*Offerings} matchers that bundle a rite's sacrifices into the records in
 * {@link RitualOfferingSets}. Extracted from {@code RitualExecutor} (which reaches these via a
 * static import) as the slice that finally sheds the dispatcher's ~1k-line finder block.
 */
final class RitualOfferings {
    private RitualOfferings() {}

    static final double CHARGE_STONE_ITEM_RADIUS = 6.0D;
    static final int GRASSPER_OFFERING_RADIUS = 5;
    private static final double DEFAULT_DROPPED_OFFERING_RADIUS = 1.5D;

    private static final RitualRecipe STORM_BASE_RECIPE = RitualRecipe.named("storm_base")
            .require("Wooden Sword", stack -> stack.is(Items.WOODEN_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .build();
    private static final RitualRecipe STORM_LARGE_RECIPE = RitualRecipe.named("storm_large")
            .require("Stone Sword", stack -> stack.is(Items.STONE_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .build();
    private static final RitualRecipe STORM_PORTABLE_RECIPE = RitualRecipe.named("storm_portable")
            .require("Iron Sword", stack -> stack.is(Items.IRON_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();

    static Optional<ItemEntity> findNearbyOffering(Level level, BlockPos center, Predicate<ItemStack> matcher) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radius = DEFAULT_DROPPED_OFFERING_RADIUS;
        double radiusSq = radius * radius;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(radius);
        ItemEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (!matcher.test(stack) || RitualExecutor.isPendingSacrifice(level, itemEntity)) {
                continue;
            }
            double distance = itemEntity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq || distance > closestDistance) {
                continue;
            }
            closest = itemEntity;
            closestDistance = distance;
        }
        return Optional.ofNullable(closest);
    }

    static Optional<List<ItemEntity>> matchDroppedOfferings(Level level, BlockPos center, RitualRecipe recipe) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radiusSq = DEFAULT_DROPPED_OFFERING_RADIUS * DEFAULT_DROPPED_OFFERING_RADIUS;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(DEFAULT_DROPPED_OFFERING_RADIUS);
        List<ItemEntity> candidates = new ArrayList<>();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || RitualExecutor.isPendingSacrifice(level, itemEntity)) {
                continue;
            }
            double distance = itemEntity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq) {
                continue;
            }
            candidates.add(itemEntity);
        }
        candidates.sort(Comparator
                .comparingDouble((ItemEntity item) -> item.position().distanceToSqr(centerPoint))
                .thenComparingInt(ItemEntity::getId));
        int[] assignment = recipe.assign(candidates.stream().map(ItemEntity::getItem).toList());
        if (assignment == null) {
            return Optional.empty();
        }
        List<ItemEntity> matched = new ArrayList<>(assignment.length);
        for (int candidateIndex : assignment) {
            matched.add(candidates.get(candidateIndex));
        }
        return Optional.of(List.copyOf(matched));
    }

    static Optional<RitualOffering> findNearbyRitualOffering(Level level, BlockPos center,
                                                                     Predicate<ItemStack> matcher) {
        Optional<ItemEntity> dropped = findNearbyOffering(level, center, CHARGE_STONE_ITEM_RADIUS, matcher);
        if (dropped.isPresent()) {
            return Optional.of(RitualOffering.dropped(dropped.get()));
        }
        return findNearbyGrassperOffering(level, center, matcher);
    }

    static Optional<ItemEntity> findNearbyOffering(Level level, BlockPos center, double radius,
                                                           Predicate<ItemStack> matcher) {
        Vec3 sourcePoint = new Vec3(center.getX(), center.getY(), center.getZ());
        double radiusSq = radius * radius;
        AABB bounds = new AABB(sourcePoint, sourcePoint).inflate(radius);
        ItemEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (!matcher.test(stack) || RitualExecutor.isPendingSacrifice(level, itemEntity)) {
                continue;
            }
            double distance = itemEntity.position().distanceToSqr(sourcePoint);
            if (distance > radiusSq || distance > closestDistance) {
                continue;
            }
            closest = itemEntity;
            closestDistance = distance;
        }
        return Optional.ofNullable(closest);
    }

    static Optional<RitualOffering> findNearbyGrassperOffering(Level level, BlockPos center,
                                                                       Predicate<ItemStack> matcher) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        BlockPos grassperLevel = center.above();
        GrassperBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (int dx = -GRASSPER_OFFERING_RADIUS; dx <= GRASSPER_OFFERING_RADIUS; dx++) {
            for (int dz = -GRASSPER_OFFERING_RADIUS; dz <= GRASSPER_OFFERING_RADIUS; dz++) {
                BlockPos pos = grassperLevel.offset(dx, 0, dz);
                if (!(level.getBlockEntity(pos) instanceof GrassperBlockEntity grassper)) {
                    continue;
                }
                ItemStack stack = grassper.getItem();
                if (stack.isEmpty() || !matcher.test(stack)) {
                    continue;
                }
                double distance = Vec3.atCenterOf(pos).distanceToSqr(centerPoint);
                if (distance < closestDistance) {
                    closest = grassper;
                    closestDistance = distance;
                }
            }
        }
        return closest == null ? Optional.empty() : Optional.of(RitualOffering.grassper(closest));
    }

    static Optional<ItemEntity> findNearbyWaystoneOffering(Level level, BlockPos center, boolean bound) {
        return findNearbyOffering(level, center,
                stack -> bound ? WaystoneItem.isBoundWaystone(stack) : WaystoneItem.isBlankWaystone(stack));
    }

    static Optional<BindBloodedWaystoneOfferings> findBindBloodedWaystoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> slimeBall = findNearbyOffering(level, center, stack -> stack.is(Items.SLIME_BALL));
        Optional<ItemEntity> icyNeedle = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ICY_NEEDLE.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (enderDew.isPresent() && slimeBall.isPresent() && icyNeedle.isPresent() && chargedStone.isPresent()) {
            return Optional.of(new BindBloodedWaystoneOfferings(
                    enderDew.get(), slimeBall.get(), null, icyNeedle.get(), chargedStone.get(), false));
        }

        Optional<ItemEntity> snowball = findNearbyOffering(level, center, stack -> stack.is(Items.SNOWBALL));
        if (enderDew.isEmpty() || slimeBall.isEmpty() || snowball.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindBloodedWaystoneOfferings(
                enderDew.get(), slimeBall.get(), snowball.get(), null, null, true));
    }

    static Optional<BindWaystonePortableOfferings> findBindWaystonePortableOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (enderDew.isEmpty() || woodAsh.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindWaystonePortableOfferings(enderDew.get(), woodAsh.get(), chargedStone.get()));
    }

    static Optional<BindWaystoneCopyPortableOfferings> findBindWaystoneCopyPortableOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> blankWaystone = findNearbyWaystoneOffering(level, center, false);
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (blankWaystone.isEmpty() || enderDew.isEmpty() || quicklime.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindWaystoneCopyPortableOfferings(
                blankWaystone.get(), enderDew.get(), quicklime.get(), chargedStone.get()));
    }

    static Optional<LivingEntity> findBloodedWaystoneTarget(Level level, BlockPos center) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(4.0, 2.0, 4.0);
        Optional<? extends LivingEntity> playerTarget = level.getEntitiesOfClass(Player.class, bounds,
                        entity -> entity.position().distanceToSqr(centerPoint) <= 16.0D)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(centerPoint), b.distanceToSqr(centerPoint)));
        if (playerTarget.isPresent()) {
            return Optional.of(playerTarget.get());
        }
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> !(entity instanceof Player) && entity.position().distanceToSqr(centerPoint) <= 16.0D)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(centerPoint), b.distanceToSqr(centerPoint)));
    }

    static Optional<ItemEntity> findNearbyFilledTaglockOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center,
                stack -> TaglockKitItem.isTaglockKit(stack) && TaglockKitItem.isFilled(stack));
    }

    static Optional<ItemEntity> findNearbyRedstoneOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, stack -> stack.is(Items.REDSTONE));
    }

    static Optional<ItemEntity> findNearbyGlowstoneOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
    }

    static Optional<ItemEntity> findNearbyObsidianOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, RitualOfferings::isObsidianStack);
    }

    static boolean isObsidianStack(ItemStack stack) {
        return stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN);
    }

    static Optional<BanishDemonOfferings> findBanishDemonOfferings(Level level, BlockPos center) {
        Optional<RitualOffering> blazePowder = findNearbyRitualOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<RitualOffering> waystone = findNearbyRitualOffering(level, center, WaystoneItem::isBlankWaystone);
        if (blazePowder.isEmpty() || waystone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BanishDemonOfferings(blazePowder.get(), waystone.get()));
    }

    static Optional<ForestationOfferings> findForestationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> wickerBundle = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WICKER_BUNDLE.get()));
        Optional<ItemEntity> brewOfSprouting = findNearbyOffering(level, center, WitcheryItems::isBrewOfSprouting);
        Optional<ItemEntity> entTwig = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENT_TWIG.get()));
        if (wickerBundle.isEmpty() || brewOfSprouting.isEmpty() || entTwig.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ForestationOfferings(
                wickerBundle.get(),
                brewOfSprouting.get(),
                entTwig.get()));
    }

    static Optional<CookFoodOfferings> findCookFoodOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> coal = findNearbyOffering(level, center, stack -> stack.is(Items.COAL));
        if (woodAsh.isEmpty() || coal.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CookFoodOfferings(woodAsh.get(), coal.get()));
    }

    static Optional<ChargedCookFoodOfferings> findChargedCookFoodOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        if (chargedStone.isEmpty() || woodAsh.isEmpty() || blazePowder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ChargedCookFoodOfferings(
                chargedStone.get(),
                woodAsh.get(),
                blazePowder.get()));
    }

    static Optional<Villager> findNearbyVillagerSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, Villager.class);
    }

    static Optional<OwlFamiliarEntity> findNearbyOwlSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, OwlFamiliarEntity.class);
    }

    static Optional<Zombie> findNearbyZombieSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, Zombie.class);
    }

    static <T extends LivingEntity> Optional<T> findNearbyLivingSacrifice(Level level, BlockPos center, Class<T> type) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radius = 1.5;
        double radiusSq = radius * radius;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(radius);
        T closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (T entity : level.getEntitiesOfClass(type, bounds)) {
            if (!entity.isAlive()) {
                continue;
            }
            double distance = entity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq || distance > closestDistance) {
                continue;
            }
            closest = entity;
            closestDistance = distance;
        }
        return Optional.ofNullable(closest);
    }

    static Optional<CurseCreatureOfferings> findCurseCreatureOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, WitcheryItems::isBrewOfTheGrotesque);
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || spiderEye.isEmpty()
                || gunpowder.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseCreatureOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                spiderEye.get(),
                gunpowder.get(),
                brewGrotesque.get()));
    }

    static Optional<CureCreatureOfferings> findCureCreatureOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, WitcheryItems::isBrewOfLove);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || spiderEye.isEmpty()
                || gunpowder.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureCreatureOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                spiderEye.get(),
                gunpowder.get(),
                brewOfLove.get()));
    }

    static Optional<CureVampirismOfferings> findCureVampirismOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> silverSword = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SILVER_SWORD.get()));
        Optional<ItemEntity> garlic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.GARLIC.get()));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, WitcheryItems::isBrewOfLove);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || silverSword.isEmpty()
                || garlic.isEmpty() || diamond.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureVampirismOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                silverSword.get(),
                garlic.get(),
                diamond.get(),
                brewOfLove.get()));
    }

    static Optional<CureMisfortuneOfferings> findCureMisfortuneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> goldNugget = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_NUGGET));
        Optional<ItemEntity> bread = findNearbyOffering(level, center, stack -> stack.is(Items.BREAD));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, WitcheryItems::isBrewOfLove);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || goldNugget.isEmpty()
                || bread.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureMisfortuneOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                goldNugget.get(),
                bread.get(),
                brewOfLove.get()));
    }

    static Optional<CurseSinkingOfferings> findCurseSinkingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> inkSac = findNearbyOffering(level, center, stack -> stack.is(Items.INK_SAC));
        Optional<ItemEntity> netherWart = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_WART));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || inkSac.isEmpty()
                || netherWart.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseSinkingOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                inkSac.get(),
                netherWart.get(),
                brewGrotesque.get()));
    }

    static Optional<CurseOverheatingOfferings> findCurseOverheatingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || demonicBlood.isEmpty()
                || blazeRod.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseOverheatingOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                demonicBlood.get(),
                blazeRod.get(),
                brewGrotesque.get()));
    }

    static Optional<CurseInsanityOfferings> findCurseInsanityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> sugar = findNearbyOffering(level, center, stack -> stack.is(Items.SUGAR));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || poisonousPotato.isEmpty()
                || sugar.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseInsanityOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                poisonousPotato.get(),
                sugar.get(),
                brewGrotesque.get()));
    }

    static Optional<CurseWakingNightmareOfferings> findCurseWakingNightmareOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> mellifluousHunger = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()));
        Optional<ItemEntity> tormentedTwine = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()));
        Optional<ItemEntity> emerald = findNearbyOffering(level, center, stack -> stack.is(Items.EMERALD));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || mellifluousHunger.isEmpty()
                || tormentedTwine.isEmpty() || emerald.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseWakingNightmareOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                mellifluousHunger.get(),
                tormentedTwine.get(),
                emerald.get(),
                brewGrotesque.get()));
    }

    static Optional<CureInsanityOfferings> findCureInsanityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> potato = findNearbyOffering(level, center, stack -> stack.is(Items.POTATO));
        Optional<ItemEntity> sugar = findNearbyOffering(level, center, stack -> stack.is(Items.SUGAR));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, WitcheryItems::isBrewOfLove);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || potato.isEmpty()
                || sugar.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureInsanityOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                potato.get(),
                sugar.get(),
                brewOfLove.get()));
    }

    static Optional<CureSinkingOfferings> findCureSinkingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> boneMeal = findNearbyOffering(level, center, stack -> stack.is(Items.BONE_MEAL));
        Optional<ItemEntity> netherWart = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_WART));
        Optional<ItemEntity> brewOfTheDepths = findNearbyOffering(level, center, WitcheryItems::isBrewOfTheDepths);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || boneMeal.isEmpty()
                || netherWart.isEmpty() || brewOfTheDepths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureSinkingOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                boneMeal.get(),
                netherWart.get(),
                brewOfTheDepths.get()));
    }

    static Optional<CureOverheatingOfferings> findCureOverheatingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> icyNeedle = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ICY_NEEDLE.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> brewOfTheDepths = findNearbyOffering(level, center, WitcheryItems::isBrewOfTheDepths);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || icyNeedle.isEmpty()
                || blazeRod.isEmpty() || brewOfTheDepths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureOverheatingOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                icyNeedle.get(),
                blazeRod.get(),
                brewOfTheDepths.get()));
    }

    static Optional<CureWakingNightmareOfferings> findCureWakingNightmareOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> goldenCarrot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLDEN_CARROT));
        Optional<ItemEntity> tormentedTwine = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, WitcheryItems::isBrewOfLove);
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || goldenCarrot.isEmpty()
                || tormentedTwine.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureWakingNightmareOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                goldenCarrot.get(),
                tormentedTwine.get(),
                brewOfLove.get()));
    }

    static Optional<CorruptPoppetOfferings> findCorruptPoppetOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        if (exhaleOfHornedOne.isEmpty() || blazePowder.isEmpty() || spectralDust.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CorruptPoppetOfferings(
                exhaleOfHornedOne.get(),
                blazePowder.get(),
                spectralDust.get()));
    }

    static Optional<NecromanticStoneOfferings> findNecromanticStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> bone = findNearbyOffering(level, center, stack -> stack.is(Items.BONE));
        Optional<ItemEntity> rottenFlesh = findNearbyOffering(level, center, stack -> stack.is(Items.ROTTEN_FLESH));
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        if (attunedStone.isEmpty() || bone.isEmpty() || rottenFlesh.isEmpty()
                || woodAsh.isEmpty() || gunpowder.isEmpty() || spectralDust.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NecromanticStoneOfferings(
                attunedStone.get(),
                bone.get(),
                rottenFlesh.get(),
                woodAsh.get(),
                gunpowder.get(),
                spectralDust.get()));
    }

    static Optional<SpectralStoneOfferings> findSpectralStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> congealedSpirit = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.CONGEALED_SPIRIT.get()));
        Optional<ItemEntity> condensedFear = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.CONDENSED_FEAR.get()));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (necroStone.isEmpty() || congealedSpirit.isEmpty() || condensedFear.isEmpty()
                || spectralDust.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SpectralStoneOfferings(
                necroStone.get(),
                congealedSpirit.get(),
                condensedFear.get(),
                spectralDust.get(),
                boline.get()));
    }

    static Optional<BindSpectralOfferings> findBindSpectralOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (spectralDust.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindSpectralOfferings(spectralDust.get(), boline.get()));
    }

    static Optional<BindFetishOfferings> findBindFetishOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (attunedStone.isEmpty() || necroStone.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindFetishOfferings(attunedStone.get(), necroStone.get(), boline.get()));
    }

    static Optional<MysticBranchOfferings> findMysticBranchOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> entTwig = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENT_TWIG.get()));
        Optional<ItemEntity> mysticUnguent = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MYSTIC_UNGUENT.get()));
        if (entTwig.isEmpty() || mysticUnguent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MysticBranchOfferings(entTwig.get(), mysticUnguent.get()));
    }

    static Optional<CrystalBallOfferings> findCrystalBallOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> quartzSphere = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()));
        Optional<ItemEntity> goldIngot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_INGOT));
        Optional<ItemEntity> happenstanceOil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()));
        if (quartzSphere.isEmpty() || goldIngot.isEmpty() || happenstanceOil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CrystalBallOfferings(quartzSphere.get(), goldIngot.get(), happenstanceOil.get()));
    }

    static Optional<SeerStoneOfferings> findSeerStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> quartzSphere = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()));
        Optional<ItemEntity> mossyCobblestone = findNearbyOffering(level, center, stack -> stack.is(Items.MOSSY_COBBLESTONE));
        Optional<ItemEntity> happenstanceOil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()));
        if (quartzSphere.isEmpty() || mossyCobblestone.isEmpty() || happenstanceOil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SeerStoneOfferings(quartzSphere.get(), mossyCobblestone.get(), happenstanceOil.get()));
    }

    static Optional<PriorIncarnationOfferings> findPriorIncarnationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> dogTongue = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TONGUE_OF_DOG.get()));
        Optional<ItemEntity> bone = findNearbyOffering(level, center, stack -> stack.is(Items.BONE));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (necroStone.isEmpty() || dogTongue.isEmpty() || bone.isEmpty()
                || spectralDust.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PriorIncarnationOfferings(necroStone.get(), dogTongue.get(), bone.get(),
                spectralDust.get(), chargedStone.get()));
    }

    static Optional<ManifestationOfferings> findManifestationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> mellifluousHunger = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> goldenPickaxe = findNearbyOffering(level, center, stack -> stack.is(Items.GOLDEN_PICKAXE));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        if (spectralDust.isEmpty() || mellifluousHunger.isEmpty() || necroStone.isEmpty() || goldenPickaxe.isEmpty()
                || arthana.isEmpty() || gunpowder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ManifestationOfferings(spectralDust.get(), mellifluousHunger.get(), necroStone.get(),
                goldenPickaxe.get(), arthana.get(), gunpowder.get()));
    }

    static Optional<TotalEclipseOfferings> findTotalEclipseOfferings(Level level, BlockPos center, boolean portable) {
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> chargedStone = portable
                ? findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
                : Optional.empty();
        if (quicklime.isEmpty() || (portable && chargedStone.isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(new TotalEclipseOfferings(quicklime.get(), chargedStone.orElse(null)));
    }

    static Optional<EntityTranspositionOfferings> findEntityTranspositionOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> waystone = findNearbyOffering(level, center,
                WaystoneItem::isBlankWaystone);
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> ironAxe = findNearbyOffering(level, center, stack -> stack.is(Items.IRON_AXE));
        if (waystone.isEmpty() || enderDew.isEmpty() || ironAxe.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EntityTranspositionOfferings(waystone.get(), enderDew.get(), ironAxe.get()));
    }

    static Optional<InfuseBrewSoaringOfferings> findInfuseBrewSoaringOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> infusedBrewBase = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()));
        Optional<ItemEntity> broom = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BROOM.get()));
        Optional<ItemEntity> feather = findNearbyOffering(level, center, stack -> stack.is(Items.FEATHER));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<OwlFamiliarEntity> owl = findNearbyOwlSacrifice(level, center);
        if (infusedBrewBase.isEmpty() || broom.isEmpty() || feather.isEmpty() || arthana.isEmpty() || owl.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InfuseBrewSoaringOfferings(
                infusedBrewBase.get(),
                broom.get(),
                feather.get(),
                arthana.get(),
                owl.get()));
    }

    static Optional<InfuseBrewGraveOfferings> findInfuseBrewGraveOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> infusedBrewBase = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()));
        Optional<ItemEntity> bone = findNearbyOffering(level, center, stack -> stack.is(Items.BONE));
        Optional<ItemEntity> denseWeb = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DENSE_WEB.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<Zombie> zombie = findNearbyZombieSacrifice(level, center);
        if (infusedBrewBase.isEmpty() || bone.isEmpty() || denseWeb.isEmpty() || necroStone.isEmpty() || zombie.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InfuseBrewGraveOfferings(
                infusedBrewBase.get(),
                bone.get(),
                denseWeb.get(),
                necroStone.get(),
                zombie.get()));
    }

    static Optional<SummonFamiliarOfferings> findSummonFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> egg = findNearbyOffering(level, center, stack -> stack.is(Items.EGG));
        Optional<ItemEntity> goldIngot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_INGOT));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        if (egg.isEmpty() || goldIngot.isEmpty() || arthana.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonFamiliarOfferings(egg.get(), goldIngot.get(), arthana.get()));
    }

    static Optional<BindFamiliarOfferings> findBindFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> tearOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TEAR_OF_GODDESS.get()));
        Optional<ItemEntity> odourOfPurity = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ODOUR_OF_PURITY.get()));
        Optional<ItemEntity> whiffOfMagic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        if (tearOfTheGoddess.isEmpty() || odourOfPurity.isEmpty() || whiffOfMagic.isEmpty()
                || diamond.isEmpty() || demonicBlood.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindFamiliarOfferings(
                tearOfTheGoddess.get(),
                odourOfPurity.get(),
                whiffOfMagic.get(),
                diamond.get(),
                demonicBlood.get()));
    }

    static Optional<BindCreakingFamiliarOfferings> findBindCreakingFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> resinClump = findNearbyOffering(level, center, stack -> stack.is(Items.RESIN_CLUMP));
        Optional<ItemEntity> paleOakSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.PALE_OAK_SAPLING.asItem()));
        Optional<ItemEntity> whiffOfMagic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()));
        Optional<ItemEntity> tearOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TEAR_OF_GODDESS.get()));
        Optional<ItemEntity> mutandisExtremis = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get()));
        if (resinClump.isEmpty() || paleOakSapling.isEmpty() || whiffOfMagic.isEmpty()
                || tearOfTheGoddess.isEmpty() || mutandisExtremis.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindCreakingFamiliarOfferings(
                resinClump.get(),
                paleOakSapling.get(),
                whiffOfMagic.get(),
                tearOfTheGoddess.get(),
                mutandisExtremis.get()));
    }

    static Optional<CallFamiliarOfferings> findCallFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> whiffOfMagic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()));
        if (breathOfTheGoddess.isEmpty() || hintOfRebirth.isEmpty() || whiffOfMagic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CallFamiliarOfferings(
                breathOfTheGoddess.get(),
                hintOfRebirth.get(),
                whiffOfMagic.get()));
    }

    static Optional<SummonImpOfferings> findSummonImpOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> infernalBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFERNAL_BLOOD.get())
                || stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        if (refinedEvil.isEmpty() || infernalBlood.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonImpOfferings(
                refinedEvil.get(),
                infernalBlood.get(),
                enderPearl.get(),
                attunedStone.get()));
    }

    static Optional<SummonWitchOfferings> findSummonWitchOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> magmaCream = findNearbyOffering(level, center, stack -> stack.is(Items.MAGMA_CREAM));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<ItemEntity> fermentedSpiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        if (diamondVapour.isEmpty() || exhaleOfHornedOne.isEmpty() || magmaCream.isEmpty()
                || arthana.isEmpty() || fermentedSpiderEye.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonWitchOfferings(
                diamondVapour.get(),
                exhaleOfHornedOne.get(),
                magmaCream.get(),
                arthana.get(),
                fermentedSpiderEye.get()));
    }

    static Optional<SummonWitherOfferings> findSummonWitherOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (diamondVapour.isEmpty() || enderPearl.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonWitherOfferings(diamondVapour.get(), enderPearl.get(), villager.get()));
    }

    static Optional<ExpensiveSummonWitherOfferings> findExpensiveSummonWitherOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (diamond.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExpensiveSummonWitherOfferings(
                diamond.get(),
                enderPearl.get(),
                attunedStone.get(),
                chargedStone.get()));
    }

    static Optional<SummonDemonOfferings> findSummonDemonOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (refinedEvil.isEmpty() || blazePowder.isEmpty() || enderPearl.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonDemonOfferings(refinedEvil.get(), blazePowder.get(), enderPearl.get(), villager.get()));
    }

    static Optional<ExpensiveSummonDemonOfferings> findExpensiveSummonDemonOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (refinedEvil.isEmpty() || blazeRod.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExpensiveSummonDemonOfferings(
                refinedEvil.get(),
                blazeRod.get(),
                enderPearl.get(),
                attunedStone.get(),
                chargedStone.get()));
    }

    static Optional<HellOnEarthOfferings> findHellOnEarthOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> demonHeart = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMON_HEART.get()));
        Optional<ItemEntity> waystone = findNearbyOffering(level, center,
                WaystoneItem::isBlankWaystone);
        Optional<ItemEntity> netherStar = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_STAR));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (redstoneSoup.isEmpty() || demonHeart.isEmpty() || waystone.isEmpty()
                || netherStar.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        int covenSize = RitualExecutor.countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        return Optional.of(new HellOnEarthOfferings(
                redstoneSoup.get(),
                demonHeart.get(),
                waystone.get(),
                netherStar.get(),
                villager.get(),
                relocation.boundWaystone(),
                relocation.effectCenter(),
                relocation.tooFar()));
    }

    static Optional<FertilityOfferings> findFertilityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> gypsum = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.GYPSUM.get()));
        Optional<ItemEntity> mutandis = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MUTANDIS.get()));
        if (hintOfRebirth.isEmpty() || diamondVapour.isEmpty() || quicklime.isEmpty() || gypsum.isEmpty() || mutandis.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FertilityOfferings(
                hintOfRebirth.get(),
                diamondVapour.get(),
                quicklime.get(),
                gypsum.get(),
                mutandis.get()));
    }

    static Optional<PortableFertilityOfferings> findPortableFertilityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> gypsum = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.GYPSUM.get()));
        Optional<ItemEntity> mutandisExtremis = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (hintOfRebirth.isEmpty() || diamondVapour.isEmpty() || quicklime.isEmpty() || gypsum.isEmpty()
                || mutandisExtremis.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PortableFertilityOfferings(
                hintOfRebirth.get(),
                diamondVapour.get(),
                quicklime.get(),
                gypsum.get(),
                mutandisExtremis.get(),
                chargedStone.get()));
    }

    static Optional<BlindnessOfferings> findBlindnessOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> brewOfInk = findNearbyOffering(level, center, WitcheryItems::isBrewOfInk);
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty() || exhaleOfHornedOne.isEmpty()
                || brewOfInk.isEmpty() || poisonousPotato.isEmpty() || spiderEye.isEmpty() || diamond.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlindnessOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                exhaleOfHornedOne.get(),
                brewOfInk.get(),
                poisonousPotato.get(),
                spiderEye.get(),
                diamond.get()));
    }

    static Optional<BlightOfferings> findBlightOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> fermentedSpiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> rottenFlesh = findNearbyOffering(level, center, stack -> stack.is(Items.ROTTEN_FLESH));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty() || fermentedSpiderEye.isEmpty()
                || poisonousPotato.isEmpty() || rottenFlesh.isEmpty() || diamond.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlightOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                fermentedSpiderEye.get(),
                poisonousPotato.get(),
                rottenFlesh.get(),
                diamond.get()));
    }

    static Optional<RainOfToadsOfferings> findRainOfToadsOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> toeOfFrog = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TOE_OF_FROG.get()));
        Optional<ItemEntity> waterBucket = findNearbyOffering(level, center, stack -> stack.is(Items.WATER_BUCKET));
        Optional<ItemEntity> belladonna = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BELLADONNA.get()));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty()
                || toeOfFrog.isEmpty() || waterBucket.isEmpty() || belladonna.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RainOfToadsOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                toeOfFrog.get(),
                waterBucket.get(),
                belladonna.get()));
    }

    static Optional<NaturesPowerOfferings> findNaturesPowerOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> rowanSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ROWAN_SAPLING.get()));
        Optional<ItemEntity> alderSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ALDER_SAPLING.get()));
        Optional<ItemEntity> hawthornSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAWTHORN_SAPLING.get()));
        Optional<ItemEntity> oakSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.OAK_SAPLING.asItem()));
        Optional<ItemEntity> spruceSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.SPRUCE_SAPLING.asItem()));
        Optional<ItemEntity> birchSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.BIRCH_SAPLING.asItem()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (rowanSapling.isEmpty() || alderSapling.isEmpty() || hawthornSapling.isEmpty()
                || oakSapling.isEmpty() || spruceSapling.isEmpty() || birchSapling.isEmpty()
                || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NaturesPowerOfferings(
                rowanSapling.get(),
                alderSapling.get(),
                hawthornSapling.get(),
                oakSapling.get(),
                spruceSapling.get(),
                birchSapling.get(),
                chargedStone.get()));
    }

    static Optional<TransposeOresOfferings> findTransposeOresOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> ironIngot = findNearbyOffering(level, center, stack -> stack.is(Items.IRON_INGOT));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (enderPearl.isEmpty() || ironIngot.isEmpty() || blazePowder.isEmpty()
                || diamondVapour.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TransposeOresOfferings(
                enderPearl.get(),
                ironIngot.get(),
                blazePowder.get(),
                diamondVapour.get(),
                chargedStone.get()));
    }

    static Optional<RaiseColumnOfferings> findRaiseColumnOfferings(Level level, BlockPos center,
                                                                           RitualCircleDetector.CircleSize circleSize) {
        Optional<ItemEntity> brewOfSprouting = findNearbyOffering(level, center, WitcheryItems::isBrewOfSprouting);
        Optional<ItemEntity> cactus = findNearbyOffering(level, center, stack -> stack.is(Blocks.CACTUS.asItem()));
        Optional<ItemEntity> focus = findRaiseColumnFocus(level, center, circleSize);
        if (brewOfSprouting.isEmpty() || cactus.isEmpty() || focus.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RaiseColumnOfferings(brewOfSprouting.get(), cactus.get(), focus.get()));
    }

    static Optional<ItemEntity> findRaiseColumnFocus(Level level, BlockPos center,
                                                             RitualCircleDetector.CircleSize circleSize) {
        if (circleSize == RitualCircleDetector.CircleSize.SMALL) {
            return findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        }
        return circleSize == RitualCircleDetector.CircleSize.MEDIUM
                ? findNearbyOffering(level, center, stack -> stack.is(Items.REDSTONE))
                : findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
    }

    static OptionalWaystoneRelocation findOptionalWaystoneRelocation(Level level, BlockPos center, int covenSize) {
        Optional<ItemEntity> boundWaystone = findNearbyWaystoneOffering(level, center, true);
        if (boundWaystone.isEmpty()) {
            return new OptionalWaystoneRelocation(null, center, false);
        }

        ItemStack stack = boundWaystone.get().getItem();
        Optional<ResourceKey<Level>> boundDim = WaystoneItem.getBoundDim(stack);
        Optional<BlockPos> boundPos = WaystoneItem.getBoundPos(stack);
        if (boundDim.isEmpty() || boundPos.isEmpty() || !boundDim.get().equals(level.dimension())) {
            return new OptionalWaystoneRelocation(null, center, false);
        }

        int maxDistance = 50 + 50 * covenSize;
        boolean tooFar = boundPos.get().distSqr(center) > (double) (maxDistance * maxDistance);
        return new OptionalWaystoneRelocation(tooFar ? null : boundWaystone.get(), boundPos.get(), tooFar);
    }

    static Optional<StormOfferings> findStormOfferings(Level level, BlockPos center, StormRiteVariant variant, int covenSize) {
        Optional<ItemEntity> weatherFocus = findStormWeatherFocus(level, center, variant);
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> chargedStone = variant == StormRiteVariant.PORTABLE
                ? findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
                : Optional.empty();
        if (weatherFocus.isEmpty() || woodAsh.isEmpty()
                || (variant == StormRiteVariant.PORTABLE && chargedStone.isEmpty())) {
            return Optional.empty();
        }
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        return Optional.of(new StormOfferings(weatherFocus.get(), woodAsh.get(), chargedStone.orElse(null),
                relocation.boundWaystone(), relocation.effectCenter(), relocation.tooFar()));
    }

    static Optional<StormOfferings> findStormOfferingsByRecipe(Level level, BlockPos center,
                                                               StormRiteVariant variant, int covenSize) {
        Optional<List<ItemEntity>> matched = matchDroppedOfferings(level, center, stormRecipe(variant));
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        List<ItemEntity> items = matched.get();
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        return Optional.of(new StormOfferings(items.get(0), items.get(1),
                variant == StormRiteVariant.PORTABLE ? items.get(2) : null,
                relocation.boundWaystone(), relocation.effectCenter(), relocation.tooFar()));
    }

    static Optional<StormRiteVariant> matchStormRecipeVariant(List<ItemStack> offeredStacks) {
        for (StormRiteVariant variant : StormRiteVariant.values()) {
            if (stormRecipe(variant).matches(offeredStacks)) {
                return Optional.of(variant);
            }
        }
        return Optional.empty();
    }

    private static RitualRecipe stormRecipe(StormRiteVariant variant) {
        if (variant == StormRiteVariant.BASE) {
            return STORM_BASE_RECIPE;
        }
        return variant == StormRiteVariant.LARGE ? STORM_LARGE_RECIPE : STORM_PORTABLE_RECIPE;
    }

    static Optional<ItemEntity> findStormWeatherFocus(Level level, BlockPos center, StormRiteVariant variant) {
        // Source/Resurrected Sky's Wrath focus is the sword tier (any damage value): wooden -> base,
        // stone -> large, iron -> portable.
        if (variant == StormRiteVariant.BASE) {
            return findNearbyOffering(level, center, stack -> stack.is(Items.WOODEN_SWORD));
        }
        return variant == StormRiteVariant.LARGE
                ? findNearbyOffering(level, center, stack -> stack.is(Items.STONE_SWORD))
                : findNearbyOffering(level, center, stack -> stack.is(Items.IRON_SWORD));
    }

    static Optional<ChargeStoneOfferings> findChargeStoneOfferings(Level level, BlockPos center) {
        Optional<RitualOffering> glowstone = findNearbyRitualOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
        Optional<RitualOffering> redstone = findNearbyRitualOffering(level, center, stack -> stack.is(Items.REDSTONE));
        Optional<RitualOffering> woodAsh = findNearbyRitualOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<RitualOffering> quicklime = findNearbyRitualOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        if (glowstone.isEmpty() || redstone.isEmpty() || woodAsh.isEmpty() || quicklime.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ChargeStoneOfferings(glowstone.get(), redstone.get(), woodAsh.get(), quicklime.get()));
    }
}
