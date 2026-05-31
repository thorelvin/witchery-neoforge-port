package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.ChaliceBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                        addSpawnEggs(output);
                    })
                    .build());

    private static void addGuide(CreativeModeTab.Output output) {
        add(output, WitcheryItems.WITCHES_PRIMER.get());
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
                WitcheryItems.BELLADONNA.get(),
                WitcheryItems.MANDRAKE_SEEDS.get(),
                WitcheryItems.MANDRAKE_ROOT.get(),
                WitcheryItems.ARTICHOKE_SEEDS.get(),
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
                WitcheryItems.MIRROR.get(),
                WitcheryItems.MIRROR_WALL.get(),
                WitcheryItems.FETISH_SCARECROW.get(),
                WitcheryItems.FETISH_TRENT_EFFIGY.get(),
                WitcheryItems.FETISH_WITCHS_LADDER.get(),
                WitcheryItems.STATUE_GODDESS.get(),
                WitcheryItems.WICKER_BUNDLE.get(),
                WitcheryItems.BLOODIED_WICKER_BUNDLE.get(),
                WitcheryItems.BLOOD_STAINED_WOOL.get(),
                WitcheryItems.PIT_DIRT.get(),
                WitcheryItems.PIT_GRASS.get(),
                WitcheryItems.CRITTER_SNARE.get(),
                WitcheryItems.BEARTRAP.get(),
                WitcheryItems.WOLFTRAP.get(),
                WitcheryItems.WITCH_DOOR.get(),
                WitcheryItems.PERPETUAL_ICE.get(),
                WitcheryItems.ICE_STAIRS.get(),
                WitcheryItems.ICE_SLAB.get(),
                WitcheryItems.ICE_DOOR.get(),
                WitcheryItems.ICE_FENCE.get(),
                WitcheryItems.ICE_GATE.get(),
                WitcheryItems.STOCKADE.get(),
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
                WitcheryItems.BOLINE.get(),
                WitcheryItems.CHALK.get(),
                WitcheryItems.MYSTIC_BRANCH.get(),
                WitcheryItems.HORN_OF_THE_HUNT.get(),
                WitcheryItems.ICY_NEEDLE.get(),
                WitcheryItems.WATER_DIVINER.get(),
                WitcheryItems.LAVA_DIVINER.get(),
                WitcheryItems.CIRCLE_TALISMAN.get(),
                WitcheryItems.WITCH_HAND.get(),
                WitcheryItems.BUCKET_SPIRIT.get(),
                WitcheryItems.BUCKET_HOLLOW_TEARS.get(),
                WitcheryItems.DEBUG_DEATH_WARD.get(),
                WitcheryItems.DEBUG_INSPECTOR.get());
    }

    private static void addPoppetsTaglocksAndScrying(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.TAGLOCK_KIT.get(),
                WitcheryItems.POPPET.get(),
                WitcheryItems.DEATH_PROTECTION_POPPET.get(),
                WitcheryItems.EARTH_PROTECTION_POPPET.get(),
                WitcheryItems.WATER_PROTECTION_POPPET.get(),
                WitcheryItems.FIRE_PROTECTION_POPPET.get(),
                WitcheryItems.HUNGER_PROTECTION_POPPET.get(),
                WitcheryItems.VOODOO_PROTECTION_POPPET.get(),
                WitcheryItems.POPPET_PROTECTION_POPPET.get(),
                WitcheryItems.VOODOO_POPPET.get(),
                WitcheryItems.VAMPIRIC_POPPET.get(),
                WitcheryItems.PLAYER_COMPASS.get(),
                WitcheryItems.SEER_STONE.get(),
                WitcheryItems.POPPET_SHELF_COMPASS.get(),
                WitcheryItems.POLYNESIA_CHARM.get(),
                WitcheryItems.CHARM_OF_DISRUPTED_DREAMS.get(),
                WitcheryItems.PARASYTIC_LOUSE.get());
    }

    private static void addIngredientsAndAlchemy(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.FANCIFUL_THREAD.get(),
                WitcheryItems.GOLDEN_THREAD.get(),
                WitcheryItems.REDSTONE_SOUP.get(),
                WitcheryItems.ANOINTING_PASTE.get(),
                WitcheryItems.BAT_WOOL.get(),
                WitcheryItems.CONCENTRATED_BAT_BALL.get(),
                WitcheryItems.WOOD_ASH.get(),
                WitcheryItems.SILVER_DEPOSITS.get(),
                WitcheryItems.DIAMOND_VAPOUR.get(),
                WitcheryItems.EXHALE_OF_HORNED_ONE.get(),
                WitcheryItems.HINT_OF_REBIRTH.get(),
                WitcheryItems.BREATH_OF_GODDESS.get(),
                WitcheryItems.WHIFF_OF_MAGIC.get(),
                WitcheryItems.FOUL_FUME.get(),
                WitcheryItems.QUICKLIME.get(),
                WitcheryItems.GYPSUM.get(),
                WitcheryItems.ODOUR_OF_PURITY.get(),
                WitcheryItems.OIL_OF_VITRIOL.get(),
                WitcheryItems.REEK_OF_MISFORTUNE.get(),
                WitcheryItems.ATTUNED_STONE.get(),
                WitcheryItems.ATTUNED_STONE_CHARGED.get(),
                WitcheryItems.CLAY_JAR_SOFT.get(),
                WitcheryItems.CLAY_JAR.get(),
                WitcheryItems.GLASS_GOBLET.get(),
                WitcheryItems.RAW_MEATY_STEW.get(),
                WitcheryItems.MEATY_STEW.get(),
                WitcheryItems.SPECTRAL_STONE.get(),
                WitcheryItems.NECRO_STONE.get(),
                WitcheryItems.TORN_PAGE.get(),
                WitcheryItems.IMMORTALS_OBSERVATIONS_BOOK.get());
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
                WitcheryItems.MOON_CHARM.get(),
                WitcheryItems.CREATIVE_BAT_WOLF_TOKEN.get(),
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
                WitcheryItems.HUNTSMAN_SPEAR.get(),
                WitcheryItems.HUNTSMANS_SPEAR.get(),
                WitcheryItems.WOODEN_BOLT.get(),
                WitcheryItems.SILVER_BOLT.get(),
                WitcheryItems.BONE_BOLT.get(),
                WitcheryItems.NULLIFYING_BOLT.get(),
                WitcheryItems.SPLITTING_BOLT.get(),
                WitcheryItems.CANE_SWORD.get(),
                WitcheryItems.WITCHES_HAT.get(),
                WitcheryItems.WITCHES_ROBE.get(),
                WitcheryItems.WITCHES_SHOES.get(),
                WitcheryItems.EARMUFFS.get(),
                WitcheryItems.BITING_BELT.get(),
                WitcheryItems.BARK_BELT.get(),
                WitcheryItems.HUNTER_HELMET.get(),
                WitcheryItems.HUNTER_CHESTPLATE.get(),
                WitcheryItems.HUNTER_LEGGINGS.get(),
                WitcheryItems.HUNTER_BOOTS.get(),
                WitcheryItems.DEATHS_HOOD.get(),
                WitcheryItems.DEATHS_ROBE.get(),
                WitcheryItems.DEATHS_SHOES.get(),
                WitcheryItems.DEATHS_HAND.get());
    }

    private static void addBrewsAndInfusions(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.BREW_BAG.get(),
                WitcheryItems.BREW_OF_RAISING.get(),
                WitcheryItems.BREW_OF_SLEEPING.get(),
                WitcheryItems.BREW_OF_FLOWING_SPIRIT.get(),
                WitcheryItems.BREW_OF_WASTING.get(),
                WitcheryItems.BREW_OF_EROSION.get(),
                WitcheryItems.BREW_OF_FOULNESS.get(),
                WitcheryItems.BREW_OF_INFECTION.get(),
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
                WitcheryItems.GHOST_OF_THE_LIGHT.get(),
                WitcheryItems.SOUL_OF_THE_WORLD.get(),
                WitcheryItems.SPIRIT_OF_OTHERWHERE.get(),
                WitcheryItems.INFERNAL_ANIMUS.get(),
                WitcheryItems.MYSTIC_UNGUENT.get(),
                WitcheryItems.FLYING_OINTMENT.get(),
                WitcheryItems.HAPPENSTANCE_OIL.get(),
                WitcheryItems.INFUSED_BREW_BASE.get(),
                WitcheryItems.INFUSED_BREW_OF_SOARING.get(),
                WitcheryItems.INFUSED_BREW_OF_THE_GRAVE.get());
    }

    private static void addDreamsDemonsAndContracts(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.WEAVE_FLEET_FOOT.get(),
                WitcheryItems.WEAVE_IRON_ARM.get(),
                WitcheryItems.WEAVE_FASTING.get(),
                WitcheryItems.WEAVE_NIGHTMARES.get(),
                WitcheryItems.WEAVE_INTENSITY.get(),
                WitcheryItems.DEMONIC_CONTRACT.get(),
                WitcheryItems.CONTRACT_BLAZE.get(),
                WitcheryItems.CONTRACT_RESIST_FIRE.get(),
                WitcheryItems.CONTRACT_EVAPORATE.get(),
                WitcheryItems.CONTRACT_FIERY_TOUCH.get(),
                WitcheryItems.CONTRACT_SMELTING.get(),
                WitcheryItems.SOUL_OF_TORMENT.get(),
                WitcheryItems.TORMENT_SCROLL.get(),
                WitcheryItems.LEONARDS_URN.get(),
                WitcheryItems.GULGS_GIRDLE.get(),
                WitcheryItems.MOGS_QUIVER.get());
    }

    private static void addLegacyPlaceholders(CreativeModeTab.Output output) {
        Set<String> accepted = new LinkedHashSet<>();

        addLegacy(output, accepted,
                "devils_tongue_charm", "twisting_band",
                "blooded_waystone", "bound_waystone");
        addLegacy(output, accepted,
                "infernal_blood", "odd_porkchop_raw", "odd_porkchop_cooked", "odd_porkchop", "cooked_odd_porkchop",
                "nullified_leather", "silver_dust", "biome_note", "cooked_sheep_liver", "sheep_liver",
                "universal_antidote", "fanciful_thinking_charm", "subdued_village_spirit", "pentacle", "golden_heart");
        addLegacy(output, accepted,
                "book_oven", "book_distilling", "book_circle_magic", "book_infusions", "book_herbology",
                "book_biomes", "book_wands", "book_burning", "brews_and_infusions_book",
                "collecting_fumes_book", "conjuration_and_fetishes_book", "extended_biomes_book",
                "symbology_book", "witches_brews_book", "distilling_book", "circle_magic_book",
                "herbology_book", "biomes_book");
        addLegacy(output, accepted,
                "bolt_stake", "bolt_anti_magic", "bolt_holy", "bolt_splitting",
                "charged_crossbow", "duplication_grenade", "staff_of_duplication",
                "hellhound_head", "wolf_head", "bound_taglock_kit",
                "crossbow_pistol", "deployed_deaths_hand");
        addLegacy(output, accepted,
                "dandelion_of_ink", "dandelion_of_sprouting", "dandelion_of_thorns", "dandelion_of_webs",
                "poppy_of_ink", "poppy_of_sprouting", "poppy_of_thorns", "poppy_of_webs",
                "shrub_of_ink", "shrub_of_sprouting", "shrub_of_thorns", "shrub_of_webs", "trapped_plant");
        addLegacy(output, accepted,
                "brew_of_soul_hunger", "brew_of_soul_anguish", "brew_of_soul_fear", "brew_of_soul_torment",
                "contract_ownership", "contract_torment",
                "creative_medallion");
        addLegacy(output, accepted,
                "brew_bottle", "splash_brew_bottle", "lingering_brew_bottle",
                "endless_water_brew");
        addLegacy(output, accepted,
                "vines_brew", "webs_brew", "thorns_brew", "ink_brew", "sprouting_brew", "erosion_brew",
                "raising_brew", "love_brew", "ice_brew", "frogs_tongue_brew", "cursed_leaping_brew",
                "hitchcock_brew", "infection_brew", "flowing_spirit_brew", "hollow_tears_brew",
                "wasting_brew", "bats_brew", "revealing_brew", "substitution_brew", "grotesque_brew",
                "depths_brew", "sleeping_brew", "infused_soaring_brew", "infused_grave_brew",
                "solidifying_stone_brew", "solidifying_dirt_brew", "solidifying_sand_brew",
                "solidifying_sandstone_brew", "solidifying_erosion_brew", "hunger_demon_soul",
                "anguish_demon_soul", "fear_demon_soul", "torment_demon_soul");
        addLegacy(output, accepted,
                "witches_robes", "icy_slippers", "hunter_hat", "hunter_coat", "silvered_hunter_hat",
                "silvered_hunter_coat", "silvered_hunter_leggings", "silvered_hunter_boots",
                "dawn_hunter_hat", "dawn_hunter_coat", "dawn_hunter_leggings", "dawn_hunter_boots",
                "gulgs_grudle");

        WitcheryItems.LEGACY_PLACEHOLDER_ITEMS.forEach((id, item) -> {
            if (accepted.add(id)) {
                output.accept(item.get());
            }
        });
    }

    private static void addSpawnEggs(CreativeModeTab.Output output) {
        add(output,
                WitcheryItems.COVEN_WITCH_SPAWN_EGG.get(),
                WitcheryItems.VAMPIRE_SPAWN_EGG.get(),
                WitcheryItems.GOBLIN_GULG_SPAWN_EGG.get(),
                WitcheryItems.GOBLIN_MOG_SPAWN_EGG.get(),
                WitcheryItems.BABA_YAGA_SPAWN_EGG.get(),
                WitcheryItems.WOLFMAN_SPAWN_EGG.get(),
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
}
