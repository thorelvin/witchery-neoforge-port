package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.PlantMineBlock;
import com.emoniph.witchery.blocks.ShadedGlassBlock;
import com.emoniph.witchery.item.CircleTalismanItem;
import com.emoniph.witchery.item.SpectralStoneItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashSet;
import java.util.Set;

public class WitcheryCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WitcheryMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WITCHERY_TAB = TABS.register("witchery_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.witchery"))
                    .icon(() -> WitcheryItems.ARTHANA.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        addGuide(output);
                        addTreesAndWood(output);
                        addPlantsAndCrops(output);
                        addMachinesAndWorkstations(output);
                        addRitualBlocksAndWorldObjects(output);
                        addToolsAndMobility(output);
                        addPoppetsTaglocksAndScrying(output);
                        addIngredientsAndAlchemy(output);
                        addVampireAndWerewolfItems(output);
                        addWeaponsAndArmor(output);
                        addBrewsAndInfusions(output);
                        addDreamsDemonsAndContracts(output);
                        addLegacyPlaceholders(output);
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WITCHERY_SPAWN_EGGS_TAB =
            TABS.register("witchery_spawn_eggs_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.witchery.spawn_eggs"))
                            .icon(() -> WitcheryItems.COVEN_WITCH_SPAWN_EGG.get().getDefaultInstance())
                            .displayItems((params, output) -> addSpawnEggs(output))
                            .build());

    private static void addGuide(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.WITCHES_PRIMER.get(),
                WitcheryItems.BOOK_OVEN.get(),
                WitcheryItems.BOOK_DISTILLING.get(),
                WitcheryItems.BOOK_CIRCLE_MAGIC.get(),
                WitcheryItems.BOOK_INFUSIONS.get(),
                WitcheryItems.BOOK_HERBOLOGY.get(),
                WitcheryItems.BOOK_WANDS.get(),
                WitcheryItems.BOOK_BURNING.get(),
                WitcheryItems.BOOK_POPPETS.get(),
                WitcheryItems.BOOK_SPIRIT_WORLD.get(),
                WitcheryItems.BOOK_CREATURES.get(),
                WitcheryItems.BOOK_TRANSFORMATIONS.get(),
                WitcheryItems.CAULDRON_BOOK.get(),
                WitcheryItems.BOOK_BIOMES.get(),
                WitcheryItems.EXTENDED_BIOMES_BOOK.get(),
                WitcheryItems.BIOMES_BOOK.get());
    }

    private static void addTreesAndWood(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.ROWAN_LOG.get(),
                WitcheryItems.ROWAN_WOOD.get(),
                WitcheryItems.ROWAN_PLANKS.get(),
                WitcheryItems.ROWAN_STAIRS.get(),
                WitcheryItems.ROWAN_SLAB.get(),
                WitcheryItems.ROWAN_DOOR.get(),
                WitcheryItems.ROWAN_DOOR_KEY.get(),
                WitcheryItems.ROWAN_KEYRING.get(),
                WitcheryItems.ROWAN_LEAVES.get(),
                WitcheryItems.ROWAN_SAPLING.get(),
                WitcheryItems.ALDER_LOG.get(),
                WitcheryItems.ALDER_PLANKS.get(),
                WitcheryItems.ALDER_STAIRS.get(),
                WitcheryItems.ALDER_SLAB.get(),
                WitcheryItems.ALDER_DOOR.get(),
                WitcheryItems.ALDER_LEAVES.get(),
                WitcheryItems.ALDER_SAPLING.get(),
                WitcheryItems.HAWTHORN_LOG.get(),
                WitcheryItems.HAWTHORN_PLANKS.get(),
                WitcheryItems.HAWTHORN_STAIRS.get(),
                WitcheryItems.HAWTHORN_SLAB.get(),
                WitcheryItems.HAWTHORN_DOOR.get(),
                WitcheryItems.HAWTHORN_LEAVES.get(),
                WitcheryItems.HAWTHORN_SAPLING.get());
    }

    private static void addPlantsAndCrops(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.ROWAN_BERRIES.get(),
                WitcheryItems.BELLADONNA_SEEDS.get(),
                WitcheryItems.STALE_BELLADONNA_SEEDS.get(),
                WitcheryItems.BELLADONNA.get(),
                WitcheryItems.MANDRAKE_SEEDS.get(),
                WitcheryItems.STALE_MANDRAKE_SEEDS.get(),
                WitcheryItems.MINDRAKE_BULB.get(),
                WitcheryItems.MANDRAKE_ROOT.get(),
                WitcheryItems.ARTICHOKE_SEEDS.get(),
                WitcheryItems.STALE_ARTICHOKE_SEEDS.get(),
                WitcheryItems.ARTICHOKE.get(),
                WitcheryItems.COTTON.get(),
                WitcheryItems.WORMWOOD_SEEDS.get(),
                WitcheryItems.WORMWOOD.get(),
                WitcheryItems.SNOWBELL_SEEDS.get(),
                WitcheryItems.SNOWBELL.get(),
                WitcheryItems.GARLIC.get(),
                WitcheryItems.GARLIC_GARLAND.get(),
                WitcheryItems.WOLFSBANE_SEEDS.get(),
                WitcheryItems.WOLFSBANE_FLOWER.get(),
                WitcheryItems.BLOOD_ROSE.get(),
                WitcheryItems.EMBER_MOSS.get(),
                WitcheryItems.GLINT_WEED.get(),
                WitcheryItems.SPANISH_MOSS.get(),
                WitcheryItems.BRAMBLE.get(),
                WitcheryItems.VOID_BRAMBLE.get(),
                WitcheryItems.BLIGHT_HEART.get(),
                WitcheryItems.BLIGHT_FIBRE.get(),
                WitcheryItems.BLIGHTED_SOIL.get(),
                WitcheryItems.BLIGHTED_STONE.get(),
                WitcheryItems.BLIGHTED_LOG.get(),
                WitcheryItems.LEAPING_LILY.get(),
                WitcheryItems.GRASSPER.get(),
                WitcheryItems.GLOWGLOBE.get(),
                WitcheryItems.HOLLOW_TEARS.get(),
                WitcheryItems.MUTANDIS.get(),
                WitcheryItems.MUTANDIS_EXTREMIS.get(),
                WitcheryItems.MUTATOR.get(),
                WitcheryItems.TREEFYD_SEEDS.get());
    }

    private static void addMachinesAndWorkstations(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.ALTAR.get(),
                WitcheryItems.KETTLE.get(),
                WitcheryItems.DISTILLERY.get(),
                WitcheryItems.SPINNING_WHEEL.get(),
                WitcheryItems.WITCHES_OVEN.get(),
                WitcheryItems.FUME_FUNNEL.get(),
                WitcheryItems.FUME_FILTER.get(),
                WitcheryItems.FILTERED_FUME_FUNNEL.get(),
                WitcheryItems.SILVER_VAT.get(),
                WitcheryItems.BRAZIER.get(),
                WitcheryItems.POPPET_SHELF.get(),
                WitcheryItems.LEECH_CHEST.get(),
                WitcheryItems.REFILLING_CHEST.get());
    }

    private static void addRitualBlocksAndWorldObjects(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.CANDELABRA.get(),
                WitcheryItems.CHALICE.get());
        output.accept(filledChaliceStack());
        add(output,
                WitcheryItems.CRYSTAL_BALL.get(),
                WitcheryItems.DREAM_CATCHER.get(),
                WitcheryItems.ALLURING_SKULL.get(),
                WitcheryItems.DEMON_HEART.get(),
                WitcheryItems.COFFIN.get(),
                WitcheryItems.BLOOD_CRUCIBLE.get(),
                WitcheryItems.WOLF_ALTAR.get(),
                WitcheryItems.MIRROR.get(),
                WitcheryItems.MIRROR_WALL.get(),
                WitcheryItems.FETISH_SCARECROW.get(),
                WitcheryItems.FETISH_TRENT_EFFIGY.get(),
                WitcheryItems.FETISH_WITCHS_LADDER.get(),
                WitcheryItems.STATUE_GODDESS.get(),
                WitcheryItems.STATUE_HOBGOBLIN_PATRON.get(),
                WitcheryItems.DECURSE_TELEPORT.get(),
                WitcheryItems.DECURSE_DIRECTED.get(),
                WitcheryItems.WICKER_BUNDLE.get(),
                WitcheryItems.BLOODIED_WICKER_BUNDLE.get(),
                WitcheryItems.BLOODED_WOOL.get());
        for (DyeColor color : DyeColor.values()) {
            output.accept(ShadedGlassBlock.stackForColor(color));
        }
        add(output,
                WitcheryItems.INFINITY_EGG.get(),
                WitcheryItems.CRITTER_SNARE.get(),
                WitcheryItems.BEARTRAP.get(),
                WitcheryItems.WOLFTRAP.get());
        addPlantMineVariants(output);
        add(output,
                WitcheryItems.WITCH_DOOR.get(),
                WitcheryItems.PERPETUAL_ICE.get(),
                WitcheryItems.ICE_STAIRS.get(),
                WitcheryItems.ICE_SLAB.get(),
                WitcheryItems.ICE_DOOR.get(),
                WitcheryItems.ICE_FENCE.get(),
                WitcheryItems.ICE_GATE.get());
        addStockadeVariants(output);
        add(output,
                WitcheryItems.ICE_STOCKADE.get(),
                WitcheryItems.ICE_PRESSURE_PLATE.get(),
                WitcheryItems.SNOW_PRESSURE_PLATE.get(),
                WitcheryItems.SNOW_STAIRS.get(),
                WitcheryItems.SNOW_SLAB.get());
    }

    private static void addToolsAndMobility(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.BROOM.get(),
                WitcheryItems.ENCHANTED_BROOM.get(),
                WitcheryItems.ARTHANA.get(),
                WitcheryItems.PENTACLE.get(),
                WitcheryItems.BOLINE.get(),
                WitcheryItems.KOBOLDITE_PICKAXE.get(),
                WitcheryItems.CHALK.get(),
                WitcheryItems.CHALK_GOLDEN.get(),
                WitcheryItems.CHALK_OTHERWHERE.get(),
                WitcheryItems.CHALK_INFERNAL.get(),
                WitcheryItems.MYSTIC_BRANCH.get(),
                WitcheryItems.HORN_OF_THE_HUNT.get(),
                WitcheryItems.ICY_NEEDLE.get(),
                WitcheryItems.WATER_DIVINER.get(),
                WitcheryItems.LAVA_DIVINER.get(),
                WitcheryItems.WOODEN_SCANNER.get());
        addCircleTalismanVariants(output);
        add(output,
                WitcheryItems.WITCH_HAND.get(),
                WitcheryItems.STAFF_OF_DUPLICATION.get(),
                WitcheryItems.BREW_BAG.get(),
                WitcheryItems.BUCKET_SPIRIT.get(),
                WitcheryItems.BUCKET_HOLLOW_TEARS.get());
    }

    private static void addPoppetsTaglocksAndScrying(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.TAGLOCK_KIT.get(),
                WitcheryItems.BOUND_TAGLOCK_KIT.get(),
                WitcheryItems.POPPET.get(),
                WitcheryItems.DEATH_PROTECTION_POPPET.get(),
                WitcheryItems.EARTH_PROTECTION_POPPET.get(),
                WitcheryItems.WATER_PROTECTION_POPPET.get(),
                WitcheryItems.FIRE_PROTECTION_POPPET.get(),
                WitcheryItems.HUNGER_PROTECTION_POPPET.get(),
                WitcheryItems.VOODOO_PROTECTION_POPPET.get(),
                WitcheryItems.POPPET_PROTECTION_POPPET.get(),
                WitcheryItems.TOOL_PROTECTION_POPPET.get(),
                WitcheryItems.ARMOR_PROTECTION_POPPET.get(),
                WitcheryItems.VOODOO_POPPET.get(),
                WitcheryItems.VAMPIRIC_POPPET.get(),
                WitcheryItems.WAYSTONE.get(),
                WitcheryItems.PLAYER_COMPASS.get(),
                WitcheryItems.SEER_STONE.get(),
                WitcheryItems.POPPET_SHELF_COMPASS.get(),
                WitcheryItems.POLYNESIA_CHARM.get(),
                WitcheryItems.DEVILS_TONGUE_CHARM.get(),
                WitcheryItems.FANCIFUL_THINKING_CHARM.get(),
                WitcheryItems.CHARM_OF_DISRUPTED_DREAMS.get(),
                WitcheryItems.PARASYTIC_LOUSE.get());
    }

    private static void addIngredientsAndAlchemy(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.WITCHES_SALT.get(),
                WitcheryItems.FANCIFUL_THREAD.get(),
                WitcheryItems.GOLDEN_THREAD.get(),
                WitcheryItems.BONE_NEEDLE.get(),
                WitcheryItems.IMPREGNATED_LEATHER.get(),
                WitcheryItems.NULLIFIED_LEATHER.get(),
                WitcheryItems.NULL_CATALYST.get(),
                WitcheryItems.ROCK.get(),
                WitcheryItems.SILVER_DEPOSITS.get(),
                WitcheryItems.SILVER_INGOT.get(),
                WitcheryItems.KOBOLDITE_DUST.get(),
                WitcheryItems.KOBOLDITE_NUGGET.get(),
                WitcheryItems.KOBOLDITE_INGOT.get());

        add(output,
                WitcheryItems.REDSTONE_SOUP.get(),
                WitcheryItems.ANOINTING_PASTE.get(),
                WitcheryItems.BAT_WOOL.get(),
                WitcheryItems.CONCENTRATED_BAT_BALL.get(),
                WitcheryItems.CREEPER_HEART.get(),
                WitcheryItems.CONDENSED_FEAR.get(),
                WitcheryItems.FOCUSED_WILL.get(),
                WitcheryItems.FROZEN_HEART.get(),
                WitcheryItems.GRAVEYARD_DUST.get(),
                WitcheryItems.REFINED_EVIL.get(),
                WitcheryItems.TEAR_OF_GODDESS.get(),
                WitcheryItems.DROP_OF_LUCK.get(),
                WitcheryItems.MELLIFLUOUS_HUNGER.get(),
                WitcheryItems.OWLETS_WING.get(),
                WitcheryItems.TOE_OF_FROG.get(),
                WitcheryItems.SPECTRAL_DUST.get(),
                WitcheryItems.TORMENTED_TWINE.get(),
                WitcheryItems.DENSE_WEB.get(),
                WitcheryItems.CONGEALED_SPIRIT.get(),
                WitcheryItems.WORMY_APPLE.get(),
                WitcheryItems.APPLE_OF_SLEEPING.get(),
                WitcheryItems.INFERNAL_BLOOD.get(),
                WitcheryItems.ENT_TWIG.get(),
                WitcheryItems.WOOD_ASH.get(),
                WitcheryItems.GOLDEN_HEART.get());

        add(output,
                WitcheryItems.DIAMOND_VAPOUR.get(),
                WitcheryItems.EXHALE_OF_HORNED_ONE.get(),
                WitcheryItems.HINT_OF_REBIRTH.get(),
                WitcheryItems.BREATH_OF_GODDESS.get(),
                WitcheryItems.WHIFF_OF_MAGIC.get(),
                WitcheryItems.ENDER_DEW.get(),
                WitcheryItems.FOUL_FUME.get(),
                WitcheryItems.QUICKLIME.get(),
                WitcheryItems.GYPSUM.get(),
                WitcheryItems.ODOUR_OF_PURITY.get(),
                WitcheryItems.BLIGHT_ICHOR.get(),
                WitcheryItems.OIL_OF_VITRIOL.get(),
                WitcheryItems.REEK_OF_MISFORTUNE.get(),
                WitcheryItems.SUBDUED_SPIRIT.get(),
                WitcheryItems.SUBDUED_VILLAGE_SPIRIT.get(),
                WitcheryItems.ATTUNED_STONE.get(),
                WitcheryItems.ATTUNED_STONE_CHARGED.get());

        add(output,
                WitcheryItems.CLAY_JAR_SOFT.get(),
                WitcheryItems.CLAY_JAR.get(),
                WitcheryItems.GLASS_GOBLET.get(),
                WitcheryItems.PURIFIED_MILK.get(),
                WitcheryItems.RAW_MEATY_STEW.get(),
                WitcheryItems.MEATY_STEW.get(),
                WitcheryItems.UNIVERSAL_ANTIDOTE.get(),
                WitcheryItems.ODD_PORKCHOP_RAW.get(),
                WitcheryItems.ODD_PORKCHOP_COOKED.get(),
                WitcheryItems.ODD_PORKCHOP.get(),
                WitcheryItems.COOKED_ODD_PORKCHOP.get(),
                WitcheryItems.SHEEP_LIVER.get(),
                WitcheryItems.COOKED_SHEEP_LIVER.get(),
                WitcheryItems.BIOME_NOTE.get());
        addSpectralStoneVariants(output);
        add(output,
                WitcheryItems.NECRO_STONE.get(),
                WitcheryItems.TORN_PAGE.get(),
                WitcheryItems.VAMPIRE_BOOK.get());
        output.accept(vampireBookStack(9));
    }

    private static void addVampireAndWerewolfItems(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.WARM_BLOOD.get(),
                WitcheryItems.LILITHS_BLOOD.get(),
                WitcheryItems.WOVEN_CRUOR.get(),
                WitcheryItems.QUARTZ_SPHERE.get(),
                WitcheryItems.CHARGED_QUARTZ_SPHERE.get(),
                WitcheryItems.SUN_COLLECTOR.get(),
                WitcheryItems.SUN_GRENADE.get(),
                WitcheryItems.DUPLICATION_GRENADE.get(),
                WitcheryItems.MOON_CHARM.get(),
                WitcheryItems.TONGUE_OF_DOG.get(),
                WitcheryItems.CREATIVE_BAT_WOLF_TOKEN.get(),
                WitcheryItems.CREATIVE_MEDALLION.get(),
                WitcheryItems.STAKE.get(),
                WitcheryItems.VAMPIRE_TOP_HAT.get(),
                WitcheryItems.VAMPIRE_VEIL_HAT.get(),
                WitcheryItems.VAMPIRE_DRESS_COAT.get(),
                WitcheryItems.VAMPIRE_DRESS_JACKET.get(),
                WitcheryItems.VAMPIRE_COAT.get(),
                WitcheryItems.VAMPIRE_CORSET.get(),
                WitcheryItems.VAMPIRE_TROUSERS.get(),
                WitcheryItems.VAMPIRE_SKIRTED_TROUSERS.get(),
                WitcheryItems.VAMPIRE_SKIRT.get(),
                WitcheryItems.VAMPIRE_OXFORD_BOOTS.get(),
                WitcheryItems.VAMPIRE_SHOES.get(),
                WitcheryItems.VAMPIRE_HEELS.get(),
                WitcheryItems.VAMPIRE_HELMET.get(),
                WitcheryItems.VAMPIRE_CHAIN_COAT.get(),
                WitcheryItems.VAMPIRE_CHAIN_COAT_LADIES.get(),
                WitcheryItems.VAMPIRE_CHESTPLATE.get(),
                WitcheryItems.VAMPIRE_LEGGINGS.get(),
                WitcheryItems.VAMPIRE_BOOTS.get());
    }

    private static void addWeaponsAndArmor(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.HAND_BOW.get(),
                WitcheryItems.CROSSBOW_PISTOL.get(),
                WitcheryItems.HUNTSMAN_SPEAR.get(),
                WitcheryItems.WOODEN_BOLT.get(),
                WitcheryItems.BOLT_STAKE.get(),
                WitcheryItems.SILVER_BOLT.get(),
                WitcheryItems.BONE_BOLT.get(),
                WitcheryItems.BOLT_HOLY.get(),
                WitcheryItems.NULLIFYING_BOLT.get(),
                WitcheryItems.BOLT_ANTI_MAGIC.get(),
                WitcheryItems.SPLITTING_BOLT.get(),
                WitcheryItems.CANE_SWORD.get(),
                WitcheryItems.SILVER_SWORD.get(),
                WitcheryItems.WITCHES_HAT.get(),
                WitcheryItems.WITCHES_ROBES.get(),
                WitcheryItems.BABAS_HAT.get(),
                WitcheryItems.NECROMANCER_ROBES.get(),
                WitcheryItems.ICY_SLIPPERS.get(),
                WitcheryItems.SEEPING_SHOES.get(),
                WitcheryItems.RUBY_SLIPPERS.get(),
                WitcheryItems.EARMUFFS.get(),
                WitcheryItems.BITING_BELT.get(),
                WitcheryItems.BARK_BELT.get(),
                WitcheryItems.HUNTER_HAT.get(),
                WitcheryItems.HUNTER_COAT.get(),
                WitcheryItems.HUNTER_LEGGINGS.get(),
                WitcheryItems.HUNTER_BOOTS.get(),
                WitcheryItems.SILVERED_HUNTER_HAT.get(),
                WitcheryItems.SILVERED_HUNTER_COAT.get(),
                WitcheryItems.SILVERED_HUNTER_LEGGINGS.get(),
                WitcheryItems.SILVERED_HUNTER_BOOTS.get(),
                WitcheryItems.DAWN_HUNTER_HAT.get(),
                WitcheryItems.DAWN_HUNTER_COAT.get(),
                WitcheryItems.DAWN_HUNTER_LEGGINGS.get(),
                WitcheryItems.DAWN_HUNTER_BOOTS.get(),
                WitcheryItems.DEATHS_HOOD.get(),
                WitcheryItems.DEATHS_ROBE.get(),
                WitcheryItems.DEATHS_SHOES.get(),
                WitcheryItems.DEATHS_HAND.get());
    }

    private static void addBrewsAndInfusions(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.BREW_BOTTLE.get(),
                WitcheryItems.SPLASH_BREW_BOTTLE.get(),
                WitcheryItems.LINGERING_BREW_BOTTLE.get(),
                WitcheryItems.LIQUID_BREW_BOTTLE.get(),
                WitcheryItems.BREW_BUCKET.get(),
                WitcheryItems.BREW_OF_RAISING.get(),
                WitcheryItems.BREW_OF_SLEEPING.get(),
                WitcheryItems.BREW_OF_FLOWING_SPIRIT.get(),
                WitcheryItems.OTHERWHERE_HEARTH_BREW.get(),
                WitcheryItems.BREW_OF_WASTING.get(),
                WitcheryItems.BREW_OF_EROSION.get(),
                WitcheryItems.BREW_OF_FOULNESS.get(),
                WitcheryItems.BREW_OF_INFECTION.get(),
                WitcheryItems.BREW_OF_BLIGHT.get(),
                WitcheryItems.BREW_OF_CLEANSING.get(),
                WitcheryItems.BREW_OF_LOVE.get(),
                WitcheryItems.BREW_OF_THE_GROTESQUE.get(),
                WitcheryItems.BREW_OF_REVEALING.get(),
                WitcheryItems.BREW_OF_ENDLESS_WATER.get(),
                WitcheryItems.COMBUSTION_BREW.get(),
                WitcheryItems.BREW_OF_POISON.get(),
                WitcheryItems.BREW_OF_ICE.get(),
                WitcheryItems.BREW_OF_FOAMING.get(),
                WitcheryItems.BREW_OF_FOULNESS_SPLASH.get(),
                WitcheryItems.BREW_OF_FOULNESS_GAS.get(),
                WitcheryItems.BREW_OF_FOAMING_SPLASH.get(),
                WitcheryItems.BREW_OF_FOAMING_GAS.get(),
                WitcheryItems.BREW_OF_POISON_SPLASH.get(),
                WitcheryItems.BREW_OF_POISON_GAS.get(),
                WitcheryItems.BREW_OF_VINES.get(),
                WitcheryItems.BREW_OF_WEBS.get(),
                WitcheryItems.BREW_OF_THORNS.get(),
                WitcheryItems.BREW_OF_INK.get(),
                WitcheryItems.BREW_OF_SPROUTING.get(),
                WitcheryItems.BREW_OF_THE_DEPTHS.get(),
                WitcheryItems.BREW_OF_FROGS_TONGUE.get(),
                WitcheryItems.BREW_OF_CURSED_LEAPING.get(),
                WitcheryItems.BREW_OF_HITCHCOCK.get(),
                WitcheryItems.BREW_OF_BATS.get(),
                WitcheryItems.BREW_OF_HOLLOW_TEARS.get(),
                WitcheryItems.BREW_OF_SUBSTITUTION.get(),
                WitcheryItems.BREW_OF_SOLID_STONE.get(),
                WitcheryItems.BREW_OF_SOLID_DIRT.get(),
                WitcheryItems.BREW_OF_SOLID_SAND.get(),
                WitcheryItems.BREW_OF_SOLID_SANDSTONE.get(),
                WitcheryItems.BREW_OF_SOLID_EROSION.get(),
                WitcheryItems.BREW_OF_SOARING.get(),
                WitcheryItems.BREW_OF_GRAVE.get(),
                WitcheryItems.GHOST_OF_THE_LIGHT.get(),
                WitcheryItems.SOUL_OF_THE_WORLD.get(),
                WitcheryItems.SPIRIT_OF_OTHERWHERE.get(),
                WitcheryItems.INFERNAL_ANIMUS.get(),
                WitcheryItems.MYSTIC_UNGUENT.get(),
                WitcheryItems.FLYING_OINTMENT.get(),
                WitcheryItems.HAPPENSTANCE_OIL.get(),
                WitcheryItems.INFUSED_BREW_BASE.get(),
                WitcheryItems.INFUSED_SOARING_BREW.get(),
                WitcheryItems.INFUSED_GRAVE_BREW.get());
    }

    private static void addDreamsDemonsAndContracts(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.WEAVE_FLEET_FOOT.get(),
                WitcheryItems.WEAVE_IRON_ARM.get(),
                WitcheryItems.WEAVE_FASTING.get(),
                WitcheryItems.WEAVE_NIGHTMARES.get(),
                WitcheryItems.WEAVE_INTENSITY.get(),
                WitcheryItems.DEMONIC_CONTRACT.get(),
                WitcheryItems.LIVING_FLAME_CONTRACT.get(),
                WitcheryItems.FIERY_TOLERANCE_CONTRACT.get(),
                WitcheryItems.EVAPORATION_CONTRACT.get(),
                WitcheryItems.FIERY_TOUCH_CONTRACT.get(),
                WitcheryItems.MELTING_TOUCH_CONTRACT.get(),
                WitcheryItems.SOUL_OF_TORMENT.get(),
                WitcheryItems.HUNGER_DEMON_SOUL.get(),
                WitcheryItems.ANGUISH_DEMON_SOUL.get(),
                WitcheryItems.FEAR_DEMON_SOUL.get(),
                WitcheryItems.TORMENT_DEMON_SOUL.get(),
                WitcheryItems.TORMENT_SCROLL.get(),
                WitcheryItems.BINKYS_SKULL.get());
        for (int capacity = 1; capacity <= 4; capacity++) {
            output.accept(com.emoniph.witchery.item.LeonardsUrnItem.createWithCapacity(capacity));
        }
        add(output,
                WitcheryItems.TWISTING_BAND.get(),
                WitcheryItems.GULGS_GRUDLE.get(),
                WitcheryItems.MOGS_QUIVER.get());
    }

    private static void addLegacyPlaceholders(CreativeModeTab.Output output) {
        // The mounted trophy heads are the last former-placeholders to become real blocks.
        add(output,
                WitcheryItems.WOLF_HEAD.get(),
                WitcheryItems.HELLHOUND_HEAD.get());

        // Any future inert placeholders would still surface here.
        Set<String> accepted = new LinkedHashSet<>();
        WitcheryItems.LEGACY_PLACEHOLDER_ITEMS.forEach((id, item) -> {
            if (accepted.add(id)) {
                output.accept(item.get());
            }
        });
    }

    private static void addSpawnEggs(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.COVEN_WITCH_SPAWN_EGG.get(),
                WitcheryItems.CRAZY_CAT_LADY_SPAWN_EGG.get(),
                WitcheryItems.VAMPIRE_SPAWN_EGG.get(),
                WitcheryItems.GOBLIN_GULG_SPAWN_EGG.get(),
                WitcheryItems.GOBLIN_MOG_SPAWN_EGG.get(),
                WitcheryItems.BABA_YAGA_SPAWN_EGG.get(),
                WitcheryItems.GOOD_BABA_YAGA_SPAWN_EGG.get(),
                WitcheryItems.WOLFMAN_SPAWN_EGG.get(),
                WitcheryItems.HELLHOUND_SPAWN_EGG.get(),
                WitcheryItems.WERE_VILLAGER_SPAWN_EGG.get(),
                WitcheryItems.VILLAGE_GUARD_SPAWN_EGG.get(),
                WitcheryItems.WITCH_HUNTER_SPAWN_EGG.get(),
                WitcheryItems.RAVEN_SPAWN_EGG.get(),
                WitcheryItems.OWL_SPAWN_EGG.get(),
                WitcheryItems.FAMILIAR_CAT_SPAWN_EGG.get(),
                WitcheryItems.SPECTRAL_FAMILIAR_SPAWN_EGG.get(),
                WitcheryItems.TOAD_SPAWN_EGG.get(),
                WitcheryItems.HOBGOBLIN_SPAWN_EGG.get(),
                WitcheryItems.TREEFYD_SPAWN_EGG.get(),
                WitcheryItems.ENT_SPAWN_EGG.get(),
                WitcheryItems.MANDRAKE_MOB_SPAWN_EGG.get(),
                WitcheryItems.MINDRAKE_SPAWN_EGG.get(),
                WitcheryItems.DARK_MARK_SPAWN_EGG.get(),
                WitcheryItems.FLYING_MONKEY_SPAWN_EGG.get(),
                WitcheryItems.BANSHEE_SPAWN_EGG.get(),
                WitcheryItems.SPECTRE_SPAWN_EGG.get(),
                WitcheryItems.POLTERGEIST_SPAWN_EGG.get(),
                WitcheryItems.DEMON_SPAWN_EGG.get(),
                WitcheryItems.NIGHTMARE_SPAWN_EGG.get(),
                WitcheryItems.SPIRIT_SPAWN_EGG.get(),
                WitcheryItems.FLAME_IMP_SPAWN_EGG.get(),
                WitcheryItems.LORD_OF_TORMENT_SPAWN_EGG.get(),
                WitcheryItems.HORNED_HUNTSMAN_SPAWN_EGG.get(),
                WitcheryItems.DEATH_SPAWN_EGG.get(),
                WitcheryItems.BINKY_SPAWN_EGG.get(),
                WitcheryItems.REFLECTION_SPAWN_EGG.get());
    }

    private static void add(CreativeModeTab.Output output, ItemLike... items) {
        for (ItemLike item : items) {
            output.accept(item);
        }
    }

    private static void addPlantMineVariants(CreativeModeTab.Output output) {
        PlantMineBlock.PlantMineEffect[] sourceEffectOrder = {
                PlantMineBlock.PlantMineEffect.WEBS,
                PlantMineBlock.PlantMineEffect.INK,
                PlantMineBlock.PlantMineEffect.SPROUTING,
                PlantMineBlock.PlantMineEffect.THORNS
        };
        for (PlantMineBlock.PlantMineDisguise disguise : PlantMineBlock.PlantMineDisguise.values()) {
            for (PlantMineBlock.PlantMineEffect effect : sourceEffectOrder) {
                output.accept(PlantMineBlock.stackFor(disguise, effect));
            }
        }
    }

    private static void addStockadeVariants(CreativeModeTab.Output output) {
        for (com.emoniph.witchery.blocks.StockadeBlock.WoodVariant variant
                : com.emoniph.witchery.blocks.StockadeBlock.WoodVariant.values()) {
            output.accept(com.emoniph.witchery.blocks.StockadeBlock.stackFor(variant));
        }
    }

    private static void addCircleTalismanVariants(CreativeModeTab.Output output) {
        for (int data : CircleTalismanItem.SOURCE_CREATIVE_DATA) {
            output.accept(CircleTalismanItem.createWithCircleData(data));
        }
    }

    private static void addSpectralStoneVariants(CreativeModeTab.Output output) {
        for (int meta : SpectralStoneItem.SOURCE_CREATIVE_META) {
            output.accept(SpectralStoneItem.createFromLegacyMeta(meta));
        }
    }

    private static void addLegacy(CreativeModeTab.Output output, Set<String> accepted, String... ids) {
        for (String id : ids) {
            var item = WitcheryItems.LEGACY_PLACEHOLDER_ITEMS.get(id);
            if (item == null) {
                throw new IllegalStateException("Missing legacy placeholder item: " + id);
            }
            if (accepted.add(id)) {
                output.accept(item.get());
            }
        }
    }

    private static ItemStack filledChaliceStack() {
        ItemStack stack = WitcheryItems.CHALICE.get().getDefaultInstance();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ChaliceBlock.FILLED_TAG, true));
        return stack;
    }

    private static ItemStack vampireBookStack(int level) {
        ItemStack stack = WitcheryItems.VAMPIRE_BOOK.get().getDefaultInstance();
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(com.emoniph.witchery.item.ImmortalsObservationsBookItem.TAG_LEVEL, level));
        return stack;
    }
}
