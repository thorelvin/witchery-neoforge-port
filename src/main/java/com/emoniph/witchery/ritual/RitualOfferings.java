package com.emoniph.witchery.ritual;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.SpectralStoneItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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

    static final RitualRecipe BIND_WAYSTONE_RECIPE = RitualRecipe.named("bind_waystone")
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Glowstone Dust", stack -> stack.is(Items.GLOWSTONE_DUST))
            .build();
    static final RitualRecipe CHARGE_STONE_RECIPE = RitualRecipe.named("charge_stone")
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .require("Glowstone Dust", stack -> stack.is(Items.GLOWSTONE_DUST))
            .require("Redstone", stack -> stack.is(Items.REDSTONE))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .build();
    static final RitualRecipe TRANSPOSITION_TO_WAYSTONE_RECIPE = RitualRecipe.named("transposition_to_waystone")
            .require("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe SANCTITY_RECIPE = RitualRecipe.named("sanctity")
            .require("Feather", stack -> stack.is(Items.FEATHER))
            .require("Redstone", stack -> stack.is(Items.REDSTONE))
            .build();
    static final RitualRecipe IMPRISONMENT_RECIPE = RitualRecipe.named("imprisonment")
            .require("Slimeball", stack -> stack.is(Items.SLIME_BALL))
            .require("Redstone", stack -> stack.is(Items.REDSTONE))
            .build();
    static final RitualRecipe PROTECTION_BARRIER_MONSTERS_RECIPE = RitualRecipe.named("obsidian_barrier_monsters")
            .require("Obsidian", RitualOfferings::isObsidianStack)
            .require("Redstone", stack -> stack.is(Items.REDSTONE))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe PROTECTION_BARRIER_LARGE_RECIPE = RitualRecipe.named("protection")
            .require("Obsidian", RitualOfferings::isObsidianStack)
            .require("Glowstone Dust", stack -> stack.is(Items.GLOWSTONE_DUST))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe PROTECTION_BARRIER_PORTABLE_RECIPE = RitualRecipe.named("obsidian_barrier_portable")
            .require("Obsidian", RitualOfferings::isObsidianStack)
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe VOLCANO_CHARGED_RECIPE = RitualRecipe.named("volcano_charged")
            .require("Stone", stack -> stack.is(Blocks.STONE.asItem()))
            .require("Magma Cream", stack -> stack.is(Items.MAGMA_CREAM))
            .require("Golden Sword", stack -> stack.is(Items.GOLDEN_SWORD))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe STORM_BASE_RECIPE = RitualRecipe.named("storm_base")
            .require("Wooden Sword", stack -> stack.is(Items.WOODEN_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe STORM_LARGE_RECIPE = RitualRecipe.named("storm_large")
            .require("Stone Sword", stack -> stack.is(Items.STONE_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe STORM_PORTABLE_RECIPE = RitualRecipe.named("storm_portable")
            .require("Iron Sword", stack -> stack.is(Items.IRON_SWORD))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe TOTAL_ECLIPSE_RECIPE = RitualRecipe.named("total_eclipse")
            .require("Stone Axe", stack -> stack.is(Items.STONE_AXE))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .build();
    static final RitualRecipe TOTAL_ECLIPSE_PORTABLE_RECIPE = RitualRecipe.named("total_eclipse_portable")
            .require("Iron Axe", stack -> stack.is(Items.IRON_AXE))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BANISH_DEMON_PORTABLE_RECIPE = RitualRecipe.named("banish_demon_charged")
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BANISH_DEMON_RECIPE = RitualRecipe.named("banish_demon")
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .build();
    static final RitualRecipe SUMMON_DEMON_RECIPE = RitualRecipe.named("summon_demon")
            .require("Refined Evil", stack -> stack.is(WitcheryItems.REFINED_EVIL.get()))
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .build();
    static final RitualRecipe SUMMON_DEMON_EXPENSIVE_RECIPE = RitualRecipe.named("summon_demon_expensive")
            .require("Refined Evil", stack -> stack.is(WitcheryItems.REFINED_EVIL.get()))
            .require("Blaze Rod", stack -> stack.is(Items.BLAZE_ROD))
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe SUMMON_WITHER_RECIPE = RitualRecipe.named("summon_wither")
            .require("Wither Skeleton Skull", stack -> stack.is(Items.WITHER_SKELETON_SKULL))
            .require("Diamond Vapour", stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()))
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .build();
    static final RitualRecipe SUMMON_WITHER_EXPENSIVE_RECIPE = RitualRecipe.named("summon_wither_expensive")
            .require("Wither Skeleton Skull", stack -> stack.is(Items.WITHER_SKELETON_SKULL))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe NECROMANTIC_STONE_RECIPE = RitualRecipe.named("necromantic_stone")
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .require("Bone", stack -> stack.is(Items.BONE))
            .require("Rotten Flesh", stack -> stack.is(Items.ROTTEN_FLESH))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Gunpowder", stack -> stack.is(Items.GUNPOWDER))
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .build();
    static final RitualRecipe SUMMON_FAMILIAR_RECIPE = RitualRecipe.named("summon_familiar")
            .require("Drop of Luck", stack -> stack.is(WitcheryItems.DROP_OF_LUCK.get()))
            .require("Egg", stack -> stack.is(Items.EGG))
            .require("Gold Ingot", stack -> stack.is(Items.GOLD_INGOT))
            .require("Arthana", stack -> stack.is(WitcheryItems.ARTHANA.get()))
            .build();
    static final RitualRecipe BIND_WAYSTONE_COPY_RECIPE = RitualRecipe.named("bind_waystone_copy")
            .require("Bound Waystone", WaystoneItem::isBoundWaystone)
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .build();
    static final RitualRecipe PORTABLE_FERTILITY_RECIPE = RitualRecipe.named("portable_fertility")
            .require("Bone Meal", stack -> stack.is(Items.BONE_MEAL))
            .require("Hint of Rebirth", stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()))
            .require("Diamond Vapour", stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .require("Gypsum", stack -> stack.is(WitcheryItems.GYPSUM.get()))
            .require("Mutandis Extremis", stack -> stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe FERTILITY_RECIPE = RitualRecipe.named("fertility")
            .require("Bone Meal", stack -> stack.is(Items.BONE_MEAL))
            .require("Hint of Rebirth", stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()))
            .require("Diamond Vapour", stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .require("Gypsum", stack -> stack.is(WitcheryItems.GYPSUM.get()))
            .require("Mutandis", stack -> stack.is(WitcheryItems.MUTANDIS.get()))
            .build();
    static final RitualRecipe BLIGHT_RECIPE = RitualRecipe.named("blight")
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .require("Redstone Soup", stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()))
            .require("Reek of Misfortune", stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()))
            .require("Fermented Spider Eye", stack -> stack.is(Items.FERMENTED_SPIDER_EYE))
            .require("Poisonous Potato", stack -> stack.is(Items.POISONOUS_POTATO))
            .require("Rotten Flesh", stack -> stack.is(Items.ROTTEN_FLESH))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .build();
    static final RitualRecipe BLINDNESS_RECIPE = RitualRecipe.named("blindness")
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .require("Redstone Soup", stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()))
            .require("Reek of Misfortune", stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()))
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Brew of Ink", WitcheryItems::isBrewOfInk)
            .require("Poisonous Potato", stack -> stack.is(Items.POISONOUS_POTATO))
            .require("Spider Eye", stack -> stack.is(Items.SPIDER_EYE))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .build();
    static final RitualRecipe SUMMON_WITCH_RECIPE = RitualRecipe.named("summon_witch")
            .require("Diamond Vapour", stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()))
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Magma Cream", stack -> stack.is(Items.MAGMA_CREAM))
            .require("Arthana", stack -> stack.is(WitcheryItems.ARTHANA.get()))
            .require("Fermented Spider Eye", stack -> stack.is(Items.FERMENTED_SPIDER_EYE))
            .build();
    static final RitualRecipe CURSE_CREATURE_RECIPE = RitualRecipe.named("curse_creature")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Fermented Spider Eye", stack -> stack.is(Items.FERMENTED_SPIDER_EYE))
            .require("Gunpowder", stack -> stack.is(Items.GUNPOWDER))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe CURE_MISFORTUNE_RECIPE = RitualRecipe.named("cure_misfortune")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Gold Nugget", stack -> stack.is(Items.GOLD_NUGGET))
            .require("Bread", stack -> stack.is(Items.BREAD))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();
    static final RitualRecipe CURE_CREATURE_RECIPE = RitualRecipe.named("cure_creature")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Spider Eye", stack -> stack.is(Items.SPIDER_EYE))
            .require("Gunpowder", stack -> stack.is(Items.GUNPOWDER))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();
    static final RitualRecipe CURSE_INSANITY_RECIPE = RitualRecipe.named("curse_insanity")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Poisonous Potato", stack -> stack.is(Items.POISONOUS_POTATO))
            .require("Sugar", stack -> stack.is(Items.SUGAR))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe CURE_INSANITY_RECIPE = RitualRecipe.named("cure_insanity")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Potato", stack -> stack.is(Items.POTATO))
            .require("Sugar", stack -> stack.is(Items.SUGAR))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();
    static final RitualRecipe CURSE_SINKING_RECIPE = RitualRecipe.named("curse_sinking")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Ink Sac", stack -> stack.is(Items.INK_SAC))
            .require("Nether Wart", stack -> stack.is(Items.NETHER_WART))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe CURE_SINKING_RECIPE = RitualRecipe.named("cure_sinking")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Bone Meal", stack -> stack.is(Items.BONE_MEAL))
            .require("Nether Wart", stack -> stack.is(Items.NETHER_WART))
            .require("Brew of the Depths", WitcheryItems::isBrewOfTheDepths)
            .build();
    static final RitualRecipe CURSE_OVERHEATING_RECIPE = RitualRecipe.named("curse_overheating")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Infernal Blood", RitualOfferings::isInfernalBloodStack)
            .require("Blaze Rod", stack -> stack.is(Items.BLAZE_ROD))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe CURE_OVERHEATING_RECIPE = RitualRecipe.named("cure_overheating")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Icy Needle", stack -> stack.is(WitcheryItems.ICY_NEEDLE.get()))
            .require("Blaze Rod", stack -> stack.is(Items.BLAZE_ROD))
            .require("Brew of the Depths", WitcheryItems::isBrewOfTheDepths)
            .build();
    static final RitualRecipe RAIN_OF_TOADS_RECIPE = RitualRecipe.named("rain_of_toads")
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .require("Redstone Soup", stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()))
            .require("Reek of Misfortune", stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()))
            .require("Toe of Frog", stack -> stack.is(WitcheryItems.TOE_OF_FROG.get()))
            .require("Water Bucket", stack -> stack.is(Items.WATER_BUCKET))
            .require("Belladonna", stack -> stack.is(WitcheryItems.BELLADONNA.get()))
            .build();
    static final RitualRecipe NATURES_POWER_RECIPE = RitualRecipe.named("natures_power")
            .require("Brew of Sprouting", WitcheryItems::isBrewOfSprouting)
            .require("Rowan Sapling", stack -> stack.is(WitcheryItems.ROWAN_SAPLING.get()))
            .require("Alder Sapling", stack -> stack.is(WitcheryItems.ALDER_SAPLING.get()))
            .require("Hawthorn Sapling", stack -> stack.is(WitcheryItems.HAWTHORN_SAPLING.get()))
            .require("Oak Sapling", stack -> stack.is(Blocks.OAK_SAPLING.asItem()))
            .require("Spruce Sapling", stack -> stack.is(Blocks.SPRUCE_SAPLING.asItem()))
            .require("Birch Sapling", stack -> stack.is(Blocks.BIRCH_SAPLING.asItem()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BIND_WAYSTONE_PORTABLE_RECIPE = RitualRecipe.named("bind_waystone_portable")
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BIND_WAYSTONE_COPY_PORTABLE_RECIPE = RitualRecipe.named("bind_waystone_copy_portable")
            .require("Bound Waystone", WaystoneItem::isBoundWaystone)
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Quicklime", stack -> stack.is(WitcheryItems.QUICKLIME.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe CRYSTAL_BALL_RECIPE = RitualRecipe.named("crystal_ball")
            .require("Quartz Sphere", stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()))
            .require("Gold Ingot", stack -> stack.is(Items.GOLD_INGOT))
            .require("Happenstance Oil", stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()))
            .build();
    static final RitualRecipe SEER_STONE_RECIPE = RitualRecipe.named("infuse_seer_stone")
            .require("Quartz Sphere", stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()))
            .require("Obsidian", stack -> stack.is(Items.OBSIDIAN))
            .require("Happenstance Oil", stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()))
            .build();
    static final RitualRecipe SPECTRAL_STONE_RECIPE = RitualRecipe.named("spectral_stone")
            .require("Necromantic Stone", stack -> stack.is(WitcheryItems.NECRO_STONE.get()))
            .require("Congealed Spirit", stack -> stack.is(WitcheryItems.CONGEALED_SPIRIT.get()))
            .require("Condensed Fear", stack -> stack.is(WitcheryItems.CONDENSED_FEAR.get()))
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .require("Boline", stack -> stack.is(WitcheryItems.BOLINE.get()))
            .build();
    static final RitualRecipe SUMMON_IMP_RECIPE = RitualRecipe.named("summon_imp")
            .require("Refined Evil", stack -> stack.is(WitcheryItems.REFINED_EVIL.get()))
            .require("Infernal Blood", RitualOfferings::isInfernalBloodStack)
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .build();
    static final RitualRecipe CURSE_WAKING_NIGHTMARE_RECIPE = RitualRecipe.named("curse_waking_nightmare")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Mellifluous Hunger", stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()))
            .require("Tormented Twine", stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()))
            .require("Emerald", stack -> stack.is(Items.EMERALD))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe FORESTATION_OAK_RECIPE = forestationRecipe("forestation_oak",
            "Oak Sapling", stack -> stack.is(Blocks.OAK_SAPLING.asItem()));
    static final RitualRecipe FORESTATION_SPRUCE_RECIPE = forestationRecipe("forestation_spruce",
            "Spruce Sapling", stack -> stack.is(Blocks.SPRUCE_SAPLING.asItem()));
    static final RitualRecipe FORESTATION_BIRCH_RECIPE = forestationRecipe("forestation_birch",
            "Birch Sapling", stack -> stack.is(Blocks.BIRCH_SAPLING.asItem()));
    static final RitualRecipe FORESTATION_JUNGLE_RECIPE = forestationRecipe("forestation_jungle",
            "Jungle Sapling", stack -> stack.is(Blocks.JUNGLE_SAPLING.asItem()));
    static final RitualRecipe FORESTATION_ROWAN_RECIPE = forestationRecipe("forestation_rowan",
            "Rowan Sapling", stack -> stack.is(WitcheryItems.ROWAN_SAPLING.get()));
    static final RitualRecipe FORESTATION_ALDER_RECIPE = forestationRecipe("forestation_alder",
            "Alder Sapling", stack -> stack.is(WitcheryItems.ALDER_SAPLING.get()));
    static final RitualRecipe FORESTATION_HAWTHORN_RECIPE = forestationRecipe("forestation_hawthorn",
            "Hawthorn Sapling", stack -> stack.is(WitcheryItems.HAWTHORN_SAPLING.get()));
    static final RitualRecipe VOLCANO_POWERED_RECIPE = RitualRecipe.named("volcano_powered")
            .require("Cobblestone", stack -> stack.is(Blocks.COBBLESTONE.asItem()))
            .require("Magma Cream", stack -> stack.is(Items.MAGMA_CREAM))
            .require("Golden Sword", stack -> stack.is(Items.GOLDEN_SWORD))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe CURE_WAKING_NIGHTMARE_RECIPE = RitualRecipe.named("cure_waking_nightmare")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Golden Carrot", stack -> stack.is(Items.GOLDEN_CARROT))
            .require("Tormented Twine", stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();
    static final RitualRecipe BIND_BLOODED_WAYSTONE_RECIPE = RitualRecipe.named("bind_blooded_waystone")
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Slimeball", stack -> stack.is(Items.SLIME_BALL))
            .require("Snowball", stack -> stack.is(Items.SNOWBALL))
            .build();
    static final RitualRecipe BIND_BLOODED_WAYSTONE_PORTABLE_RECIPE =
            RitualRecipe.named("bind_blooded_waystone_portable")
                    .require("Blank Waystone", WaystoneItem::isBlankWaystone)
                    .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
                    .require("Slimeball", stack -> stack.is(Items.SLIME_BALL))
                    .require("Icy Needle", stack -> stack.is(WitcheryItems.ICY_NEEDLE.get()))
                    .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
                    .build();
    static final RitualRecipe BIND_STATUE_TO_PLAYER_RECIPE = RitualRecipe.named("bind_statue_to_player")
            .require("Statue of Hobgoblin Patron", stack -> stack.is(WitcheryItems.STATUE_HOBGOBLIN_PATRON.get()))
            .require("Belladonna", stack -> stack.is(WitcheryItems.BELLADONNA.get()))
            .require("Poppy", stack -> stack.is(Items.POPPY))
            .require("Dandelion", stack -> stack.is(Items.DANDELION))
            .build();
    static final RitualRecipe CURSE_OF_THE_WOLF_RECIPE = RitualRecipe.named("curse_of_the_wolf")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Hellhound Head", stack -> stack.is(WitcheryItems.HELLHOUND_HEAD.get()))
            .require("Wolfsbane", stack -> stack.is(WitcheryItems.WOLFSBANE_FLOWER.get()))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .require("Brew of the Grotesque", WitcheryItems::isBrewOfTheGrotesque)
            .build();
    static final RitualRecipe CURE_WOLF_RECIPE = RitualRecipe.named("cure_wolf")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Silver Sword", stack -> stack.is(WitcheryItems.SILVER_SWORD.get()))
            .require("Wolfsbane", stack -> stack.is(WitcheryItems.WOLFSBANE_FLOWER.get()))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();
    static final RitualRecipe INFUSION_MIRROR_RECIPE = RitualRecipe.named("infusion_mirror")
            .require("Brew of Hollow Tears", stack -> stack.is(WitcheryItems.BREW_OF_HOLLOW_TEARS.get()))
            .require("Gold Ingot", stack -> stack.is(Items.GOLD_INGOT))
            .require("Glass Pane", stack -> stack.is(Items.GLASS_PANE))
            .build();
    static final RitualRecipe SUMMON_REFLECTION_RECIPE = RitualRecipe.named("summon_reflection")
            .require("Mirror", stack -> stack.is(WitcheryItems.MIRROR.get()))
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Quartz Sphere", stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()))
            .build();
    static final RitualRecipe INFUSION_RECHARGE_RECIPE = RitualRecipe.named("infusion_recharge")
            .require("Potion of Regeneration", RitualOfferings::isRegenerationPotion)
            .build();
    static final RitualRecipe TRANSPOSITION_ENTITY_RECIPE = RitualRecipe.named("transposition_entity")
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Ender Dew", stack -> stack.is(WitcheryItems.ENDER_DEW.get()))
            .require("Iron Axe", stack -> stack.is(Items.IRON_AXE))
            .build();
    static final RitualRecipe TRANSPOSE_ORES_RECIPE = RitualRecipe.named("transpose_ores")
            .require("Ender Pearl", stack -> stack.is(Items.ENDER_PEARL))
            .require("Iron Ingot", stack -> stack.is(Items.IRON_INGOT))
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Diamond Vapour", stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BROKEN_EARTH_RECIPE = RitualRecipe.named("broken_earth")
            .require("Brew of Erosion", stack -> stack.is(WitcheryItems.BREW_OF_EROSION.get()))
            .build();
    static final RitualRecipe RAISE_COLUMN_SMALL_RECIPE = raiseColumnRecipe(
            "raise_column_small", "Gunpowder", stack -> stack.is(Items.GUNPOWDER), true);
    static final RitualRecipe RAISE_COLUMN_MEDIUM_RECIPE = raiseColumnRecipe(
            "raise_column_medium", "Redstone", stack -> stack.is(Items.REDSTONE), false);
    static final RitualRecipe RAISE_COLUMN_LARGE_RECIPE = raiseColumnRecipe(
            "raise_column_large", "Glowstone Dust", stack -> stack.is(Items.GLOWSTONE_DUST), false);
    static final RitualRecipe INFUSE_LIGHT_RECIPE = RitualRecipe.named("infuse_light")
            .require("Ghost of the Light", stack -> stack.is(WitcheryItems.GHOST_OF_THE_LIGHT.get()))
            .build();
    static final RitualRecipe INFUSE_OVERWORLD_RECIPE = RitualRecipe.named("infuse_overworld")
            .require("Soul of the World", stack -> stack.is(WitcheryItems.SOUL_OF_THE_WORLD.get()))
            .build();
    static final RitualRecipe INFUSE_OTHERWHERE_RECIPE = RitualRecipe.named("infuse_otherwhere")
            .require("Spirit of Otherwhere", stack -> stack.is(WitcheryItems.SPIRIT_OF_OTHERWHERE.get()))
            .build();
    static final RitualRecipe INFUSE_INFERNAL_RECIPE = RitualRecipe.named("infuse_infernal")
            .require("Infernal Animus", stack -> stack.is(WitcheryItems.INFERNAL_ANIMUS.get()))
            .build();
    static final RitualRecipe ENCHANT_BROOM_RECIPE = RitualRecipe.named("enchant_broom")
            .require("Broom", stack -> stack.is(WitcheryItems.BROOM.get()))
            .require("Flying Ointment", stack -> stack.is(WitcheryItems.FLYING_OINTMENT.get()))
            .build();
    static final RitualRecipe HELL_ON_EARTH_RECIPE = RitualRecipe.named("hell_on_earth")
            .require("Redstone Soup", stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()))
            .require("Demon Heart", stack -> stack.is(WitcheryItems.DEMON_HEART.get()))
            .require("Blank Waystone", WaystoneItem::isBlankWaystone)
            .require("Nether Star", stack -> stack.is(Items.NETHER_STAR))
            .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
            .build();
    static final RitualRecipe PRIOR_INCARNATION_RECIPE = RitualRecipe.named("prior_incarnation")
            .require("Necromantic Stone", stack -> stack.is(WitcheryItems.NECRO_STONE.get()))
            .require("Tongue of Dog", stack -> stack.is(WitcheryItems.TONGUE_OF_DOG.get()))
            .require("Bone", stack -> stack.is(Items.BONE))
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe MYSTIC_BRANCH_RECIPE = RitualRecipe.named("mystic_branch")
            .require("Ent Twig", stack -> stack.is(WitcheryItems.ENT_TWIG.get()))
            .require("Mystic Unguent", stack -> stack.is(WitcheryItems.MYSTIC_UNGUENT.get()))
            .build();
    static final RitualRecipe COOK_ITEM_RECIPE = RitualRecipe.named("cook_item")
            .require("Sugar", stack -> stack.is(Items.SUGAR))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Coal", stack -> stack.is(Items.COAL))
            .build();
    static final RitualRecipe COOK_ITEM_CHARGED_RECIPE = RitualRecipe.named("cook_item_charged")
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .require("Sugar", stack -> stack.is(Items.SUGAR))
            .require("Wood Ash", stack -> stack.is(WitcheryItems.WOOD_ASH.get()))
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .build();
    static final RitualRecipe BIND_FAMILIAR_RECIPE = RitualRecipe.named("bind_familiar")
            .require("Tear of the Goddess", stack -> stack.is(WitcheryItems.TEAR_OF_GODDESS.get()))
            .require("Odour of Purity", stack -> stack.is(WitcheryItems.ODOUR_OF_PURITY.get()))
            .require("Whiff of Magic", stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .require("Infernal Blood", RitualOfferings::isInfernalBloodStack)
            .build();
    static final RitualRecipe CALL_FAMILIAR_RECIPE = RitualRecipe.named("call_familiar")
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Hint of Rebirth", stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()))
            .require("Whiff of Magic", stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()))
            .build();
    static final RitualRecipe CORRUPT_POPPET_RECIPE = RitualRecipe.named("corrupt_poppet")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Exhale of the Horned One", stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()))
            .require("Voodoo Protection Poppet", stack -> stack.is(WitcheryItems.VOODOO_PROTECTION_POPPET.get()))
            .require("Blaze Powder", stack -> stack.is(Items.BLAZE_POWDER))
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .build();
    static final RitualRecipe CLIMATE_CHANGE_RECIPE = RitualRecipe.named("climate_change")
            .require("Spider Eye", stack -> stack.is(Items.SPIDER_EYE))
            .require("Toe of Frog", stack -> stack.is(WitcheryItems.TOE_OF_FROG.get()))
            .require("Bat Wool", stack -> stack.is(WitcheryItems.BAT_WOOL.get()))
            .require("Tongue of Dog", stack -> stack.is(WitcheryItems.TONGUE_OF_DOG.get()))
            .require("Owlet's Wing", stack -> stack.is(WitcheryItems.OWLETS_WING.get()))
            .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe ICE_SHELL_RECIPE = RitualRecipe.named("ice_shell")
            .require("Diamond Sword", stack -> stack.is(Items.DIAMOND_SWORD))
            .require("Frozen Heart", stack -> stack.is(WitcheryItems.FROZEN_HEART.get()))
            .require("Charged Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
            .build();
    static final RitualRecipe BEASTIAL_CALL_RECIPE = RitualRecipe.named("beastial_call")
            .require("Milk Bucket", stack -> stack.is(Items.MILK_BUCKET))
            .require("Hay Bale", stack -> stack.is(Blocks.HAY_BLOCK.asItem()))
            .require("Apple", stack -> stack.is(Items.APPLE))
            .require("Raw Beef", stack -> stack.is(Items.BEEF))
            .require("Raw Cod", stack -> stack.is(Items.COD))
            .require("Red Mushroom", stack -> stack.is(Blocks.RED_MUSHROOM.asItem()))
            .require("Carrot", stack -> stack.is(Items.CARROT))
            .require("Wheat Seeds", stack -> stack.is(Items.WHEAT_SEEDS))
            .build();
    static final RitualRecipe MANIFESTATION_DREAM_RECIPE = RitualRecipe.named("manifestation_dream")
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .require("Mellifluous Hunger", stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()))
            .require("Necromantic Stone", stack -> stack.is(WitcheryItems.NECRO_STONE.get()))
            .require("Golden Pickaxe", stack -> stack.is(Items.GOLDEN_PICKAXE))
            .require("Arthana", stack -> stack.is(WitcheryItems.ARTHANA.get()))
            .require("Gunpowder", stack -> stack.is(Items.GUNPOWDER))
            .build();
    static final RitualRecipe INFUSE_BREW_SOARING_RECIPE = RitualRecipe.named("infuse_brew_soaring")
            .require("Infused Brew Base", stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()))
            .require("Broom", stack -> stack.is(WitcheryItems.BROOM.get()))
            .require("Feather", stack -> stack.is(Items.FEATHER))
            .require("Arthana", stack -> stack.is(WitcheryItems.ARTHANA.get()))
            .build();
    static final RitualRecipe INFUSE_BREW_GRAVE_RECIPE = RitualRecipe.named("infuse_brew_grave")
            .require("Infused Brew Base", stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()))
            .require("Bone", stack -> stack.is(Items.BONE))
            .require("Dense Web", stack -> stack.is(WitcheryItems.DENSE_WEB.get()))
            .require("Necromantic Stone", stack -> stack.is(WitcheryItems.NECRO_STONE.get()))
            .build();
    static final RitualRecipe BIND_SPECTRAL_STONE_RECIPE = RitualRecipe.named("bind_spectral_stone")
            .require("Unbound Spectral Stone", stack -> stack.is(WitcheryItems.SPECTRAL_STONE.get())
                    && !SpectralStoneItem.isBound(stack))
            .require("Spectral Dust", stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()))
            .require("Boline", stack -> stack.is(WitcheryItems.BOLINE.get()))
            .build();
    static final RitualRecipe BIND_SCARECROW_RECIPE = bindFetishRecipe(
            "bind_scarecrow", stack -> stack.is(WitcheryItems.FETISH_SCARECROW.get()));
    static final RitualRecipe BIND_TRENT_EFFIGY_RECIPE = bindFetishRecipe(
            "bind_trent_effigy", stack -> stack.is(WitcheryItems.FETISH_TRENT_EFFIGY.get()));
    static final RitualRecipe BIND_WITCHS_LADDER_RECIPE = bindFetishRecipe(
            "bind_witchs_ladder", stack -> stack.is(WitcheryItems.FETISH_WITCHS_LADDER.get()));
    static final RitualRecipe REMOVE_VAMPIRISM_RECIPE = RitualRecipe.named("remove_vampirism")
            .require("Filled Taglock Kit", RitualOfferings::isFilledTaglockStack)
            .require("Breath of the Goddess", stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()))
            .require("Silver Sword", stack -> stack.is(WitcheryItems.SILVER_SWORD.get()))
            .require("Garlic", stack -> stack.is(WitcheryItems.GARLIC.get()))
            .require("Diamond", stack -> stack.is(Items.DIAMOND))
            .require("Brew of Love", WitcheryItems::isBrewOfLove)
            .build();

    private static RitualRecipe forestationRecipe(String name, String saplingName,
                                                   Predicate<ItemStack> saplingMatcher) {
        return RitualRecipe.named(name)
                .require(saplingName, saplingMatcher)
                .require("Wicker Bundle", stack -> stack.is(WitcheryItems.WICKER_BUNDLE.get()))
                .require("Brew of Sprouting", WitcheryItems::isBrewOfSprouting)
                .require("Ent Twig", stack -> stack.is(WitcheryItems.ENT_TWIG.get()))
                .optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone)
                .build();
    }

    private static RitualRecipe raiseColumnRecipe(String name, String focusName,
                                                  Predicate<ItemStack> focusMatcher, boolean portable) {
        RitualRecipe.Builder builder = RitualRecipe.named(name)
                .require("Brew of Sprouting", WitcheryItems::isBrewOfSprouting)
                .require("Cactus", stack -> stack.is(Blocks.CACTUS.asItem()))
                .require(focusName, focusMatcher);
        if (portable) {
            builder.optional("Bound Waystone", WaystoneItem::isAnyBoundWaystone);
        }
        return builder.build();
    }

    private static RitualRecipe bindFetishRecipe(String name, Predicate<ItemStack> fetishMatcher) {
        return RitualRecipe.named(name)
                .require("Fetish", fetishMatcher)
                .require("Attuned Stone", stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()))
                .require("Necromantic Stone", stack -> stack.is(WitcheryItems.NECRO_STONE.get()))
                .require("Boline", stack -> stack.is(WitcheryItems.BOLINE.get()))
                .build();
    }

    private static boolean isRegenerationPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.REGENERATION);
    }

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

    static Optional<List<RitualOffering>> matchOfferings(Level level, BlockPos center, RitualRecipe recipe) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radiusSq = DEFAULT_DROPPED_OFFERING_RADIUS * DEFAULT_DROPPED_OFFERING_RADIUS;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(DEFAULT_DROPPED_OFFERING_RADIUS);
        List<RitualOffering> candidates = new ArrayList<>();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || RitualExecutor.isPendingSacrifice(level, itemEntity)) {
                continue;
            }
            double distance = itemEntity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq) {
                continue;
            }
            candidates.add(RitualOffering.dropped(itemEntity));
        }
        candidates.sort(Comparator
                .comparingDouble((RitualOffering offering) -> offering.position().distanceToSqr(centerPoint))
                .thenComparingInt(offering -> offering.itemEntity().getId()));

        BlockPos grassperLevel = center.above();
        List<RitualOffering> grassperCandidates = new ArrayList<>();
        for (int dx = -GRASSPER_OFFERING_RADIUS; dx <= GRASSPER_OFFERING_RADIUS; dx++) {
            for (int dz = -GRASSPER_OFFERING_RADIUS; dz <= GRASSPER_OFFERING_RADIUS; dz++) {
                BlockPos pos = grassperLevel.offset(dx, 0, dz);
                if (!(level.getBlockEntity(pos) instanceof GrassperBlockEntity grassper)
                        || grassper.getItem().isEmpty()
                        || SacrificeConsumer.isPending(level, pos)) {
                    continue;
                }
                grassperCandidates.add(RitualOffering.grassper(grassper));
            }
        }
        grassperCandidates.sort(Comparator
                .comparingDouble((RitualOffering offering) -> offering.position().distanceToSqr(centerPoint))
                .thenComparingInt(offering -> offering.blockPosition().getX())
                .thenComparingInt(offering -> offering.blockPosition().getZ()));
        candidates.addAll(grassperCandidates);

        int[] assignment = recipe.assign(candidates.stream().map(RitualOffering::stack).toList());
        if (assignment == null) {
            return Optional.empty();
        }
        List<RitualOffering> matched = new ArrayList<>(assignment.length);
        for (int candidateIndex : assignment) {
            if (candidateIndex >= 0) {
                matched.add(candidates.get(candidateIndex));
            }
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
                if (stack.isEmpty() || !matcher.test(stack) || SacrificeConsumer.isPending(level, pos)) {
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
        return findNearbyOffering(level, center, RitualOfferings::isFilledTaglockStack);
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

    static boolean isInfernalBloodStack(ItemStack stack) {
        return stack.is(WitcheryItems.INFERNAL_BLOOD.get());
    }

    static boolean isFilledTaglockStack(ItemStack stack) {
        return TaglockKitItem.isTaglockKit(stack) && TaglockKitItem.isFilled(stack);
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
        Optional<ItemEntity> fermentedSpiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, WitcheryItems::isBrewOfTheGrotesque);
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || fermentedSpiderEye.isEmpty()
                || gunpowder.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseCreatureOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                fermentedSpiderEye.get(),
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
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, RitualOfferings::isInfernalBloodStack);
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
        Optional<ItemEntity> obsidian = findNearbyOffering(level, center, stack -> stack.is(Items.OBSIDIAN));
        Optional<ItemEntity> happenstanceOil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()));
        if (quartzSphere.isEmpty() || obsidian.isEmpty() || happenstanceOil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SeerStoneOfferings(quartzSphere.get(), obsidian.get(), happenstanceOil.get()));
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
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFERNAL_BLOOD.get()));
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
        Optional<ItemEntity> infernalBlood = findNearbyOffering(level, center, RitualOfferings::isInfernalBloodStack);
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
        return new OptionalWaystoneRelocation(tooFar ? null : RitualOffering.dropped(boundWaystone.get()),
                boundPos.get(), tooFar);
    }

    static OptionalWaystoneRelocation matchedOptionalWaystoneRelocation(Level level, BlockPos center,
                                                                        int covenSize,
                                                                        List<RitualOffering> offerings) {
        for (RitualOffering offering : offerings) {
            ItemStack stack = offering.stack();
            if (!WaystoneItem.isAnyBoundWaystone(stack)) {
                continue;
            }
            Optional<ResourceKey<Level>> boundDim = WaystoneItem.getBoundDim(stack);
            Optional<BlockPos> boundPos = WaystoneItem.getBoundPos(stack);
            if (boundDim.isEmpty() || boundPos.isEmpty() || !boundDim.get().equals(level.dimension())) {
                return new OptionalWaystoneRelocation(null, center, false);
            }
            int maxDistance = 50 + 50 * covenSize;
            boolean tooFar = boundPos.get().distSqr(center) > (double) maxDistance * maxDistance;
            return new OptionalWaystoneRelocation(tooFar ? null : offering, boundPos.get(), tooFar);
        }
        return new OptionalWaystoneRelocation(null, center, false);
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
