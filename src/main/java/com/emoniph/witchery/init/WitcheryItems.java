package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.item.*;
import com.emoniph.witchery.item.BrewOfErosionItem;
import com.emoniph.witchery.item.BrewOfFoamingItem;
import com.emoniph.witchery.item.BrewOfFoulnessItem;
import com.emoniph.witchery.item.BrewOfFlowingSpiritItem;
import com.emoniph.witchery.item.BrewOfInfectionItem;
import com.emoniph.witchery.item.BrewOfLoveItem;
import com.emoniph.witchery.item.BrewOfRaisingItem;
import com.emoniph.witchery.item.BrewOfRevealingItem;
import com.emoniph.witchery.item.BrewOfSleepingItem;
import com.emoniph.witchery.item.BrewOfTheGrotesqueItem;
import com.emoniph.witchery.item.BrewOfWastingItem;
import com.emoniph.witchery.item.BroomItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WitcheryItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WitcheryMod.MODID);

    private static Item.Properties itemProps(net.minecraft.resources.Identifier id) {
        return new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ITEM, id));
    }

    private static Item vampireArmor(net.minecraft.resources.Identifier id,
                                     net.minecraft.world.item.equipment.ArmorMaterial material,
                                     net.minecraft.world.item.equipment.ArmorType armorType,
                                     VampireClothingItem.SetType setType) {
        return new VampireClothingItem(setType, itemProps(id).humanoidArmor(material, armorType));
    }


    // ── M3/M4 crop + ingredient items ──────────────────────────────────────────
    // Witchery crops now split closer to source between planting stock and the
    // harvested ingredient item they produce.
    public static final DeferredItem<Item> BELLADONNA = ITEMS.registerSimpleItem("belladonna");
    public static final DeferredItem<Item> BELLADONNA_SEEDS = ITEMS.register("belladonna_seeds", id -> new BlockItem(WitcheryBlocks.BELLADONNA.get(), itemProps(id)));
    public static final DeferredItem<Item> MANDRAKE_ROOT = ITEMS.registerSimpleItem("mandrake_root");
    public static final DeferredItem<Item> MANDRAKE_SEEDS = ITEMS.register("mandrake_seeds", id -> new BlockItem(WitcheryBlocks.MANDRAKE.get(), itemProps(id)));
    public static final DeferredItem<Item> ARTICHOKE = ITEMS.registerSimpleItem("artichoke");
    public static final DeferredItem<Item> ARTICHOKE_SEEDS = ITEMS.register("artichoke_seeds", id -> new ArtichokeSeedItem(itemProps(id)));
    public static final DeferredItem<Item> SNOWBELL_SEEDS = ITEMS.register("snowbell_seeds", id -> new BlockItem(WitcheryBlocks.SNOWBELL.get(), itemProps(id)));

    // ── M14/M15 crafting outputs ───────────────────────────────────────────────
    public static final DeferredItem<Item> FANCIFUL_THREAD     = ITEMS.registerSimpleItem("fanciful_thread");
    public static final DeferredItem<Item> REDSTONE_SOUP       = ITEMS.registerSimpleItem("redstone_soup");
    public static final DeferredItem<Item> DIAMOND_VAPOUR      = ITEMS.registerSimpleItem("diamond_vapour");
    public static final DeferredItem<Item> BAT_WOOL            = ITEMS.registerSimpleItem("bat_wool");
    public static final DeferredItem<Item> CONCENTRATED_BAT_BALL = ITEMS.registerSimpleItem("concentrated_bat_ball");

    // ── M4 ingredient-only items (not plantable) ───────────────────────────────
    public static final DeferredItem<Item> WOOD_ASH            = ITEMS.registerSimpleItem("wood_ash");
    public static final DeferredItem<Item> ROWAN_BERRIES       = ITEMS.registerSimpleItem("rowan_berries");
    public static final DeferredItem<Item> WORMWOOD            = ITEMS.registerSimpleItem("wormwood");
    public static final DeferredItem<Item> SNOWBELL            = ITEMS.registerSimpleItem("snowbell");
    public static final DeferredItem<Item> EXHALE_OF_HORNED_ONE = ITEMS.registerSimpleItem("exhale_of_horned_one");
    public static final DeferredItem<Item> HINT_OF_REBIRTH     = ITEMS.registerSimpleItem("hint_of_rebirth");

    // Block items
    public static final DeferredItem<BlockItem> ROWAN_LOG = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_LOG);
    public static final DeferredItem<BlockItem> ROWAN_WOOD = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_WOOD);
    public static final DeferredItem<BlockItem> ROWAN_LEAVES = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_LEAVES);
    public static final DeferredItem<BlockItem> ROWAN_SAPLING = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_SAPLING);
    public static final DeferredItem<BlockItem> ALDER_LOG = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_LOG);
    public static final DeferredItem<BlockItem> ALDER_LEAVES = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_LEAVES);
    public static final DeferredItem<BlockItem> ALDER_SAPLING = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_SAPLING);
    public static final DeferredItem<BlockItem> HAWTHORN_LOG = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_LOG);
    public static final DeferredItem<BlockItem> HAWTHORN_LEAVES = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_LEAVES);
    public static final DeferredItem<BlockItem> HAWTHORN_SAPLING = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_SAPLING);
    public static final DeferredItem<BlockItem> WISPY_COTTON     = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WISPY_COTTON);
    public static final DeferredItem<BlockItem> DISTURBED_COTTON = ITEMS.register("disturbed_cotton",
            id -> new com.emoniph.witchery.item.DisturbedCottonItem(WitcheryBlocks.DISTURBED_COTTON.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> BLOOD_ROSE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOOD_ROSE);
    public static final DeferredItem<BlockItem> EMBER_MOSS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.EMBER_MOSS);
    public static final DeferredItem<BlockItem> GLINT_WEED = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GLINT_WEED);
    public static final DeferredItem<BlockItem> SPANISH_MOSS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SPANISH_MOSS);
    public static final DeferredItem<BlockItem> BRAMBLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BRAMBLE);
    public static final DeferredItem<BlockItem> VOID_BRAMBLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.VOID_BRAMBLE);
    public static final DeferredItem<BlockItem> LEAPING_LILY = ITEMS.registerSimpleBlockItem(WitcheryBlocks.LEAPING_LILY);
    public static final DeferredItem<BlockItem> GRASSPER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GRASSPER);
    public static final DeferredItem<BlockItem> GLOWGLOBE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GLOWGLOBE);
    public static final DeferredItem<BlockItem> KETTLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.KETTLE);
    public static final DeferredItem<BlockItem> DISTILLERY = ITEMS.registerSimpleBlockItem(WitcheryBlocks.DISTILLERY);
    public static final DeferredItem<BlockItem> SPINNING_WHEEL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SPINNING_WHEEL);
    public static final DeferredItem<BlockItem> WITCHES_OVEN = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WITCHES_OVEN);
    public static final DeferredItem<BlockItem> ALTAR = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALTAR);
    public static final DeferredItem<BlockItem> FUME_FUNNEL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FUME_FUNNEL);
    public static final DeferredItem<BlockItem> BRAZIER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BRAZIER);
    public static final DeferredItem<BlockItem> POPPET_SHELF = ITEMS.registerSimpleBlockItem(WitcheryBlocks.POPPET_SHELF);
    public static final DeferredItem<BlockItem> LEECH_CHEST = ITEMS.registerSimpleBlockItem(WitcheryBlocks.LEECH_CHEST);
    public static final DeferredItem<BlockItem> REFILLING_CHEST = ITEMS.registerSimpleBlockItem(WitcheryBlocks.REFILLING_CHEST);
    public static final DeferredItem<BlockItem> CANDELABRA = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CANDELABRA);
    public static final DeferredItem<BlockItem> CHALICE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CHALICE);
    public static final DeferredItem<BlockItem> CRYSTAL_BALL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CRYSTAL_BALL);
    public static final DeferredItem<BlockItem> DREAM_CATCHER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.DREAM_CATCHER);

    // ── Dream Weave items ──────────────────────────────────────────────────────
    public static final DeferredItem<com.emoniph.witchery.item.DreamWeaveItem> WEAVE_FLEET_FOOT =
        ITEMS.register("weave_fleet_foot", id -> new com.emoniph.witchery.item.DreamWeaveItem(
            com.emoniph.witchery.blocks.DreamWeaveType.FLEET_FOOT, itemProps(id)));
    public static final DeferredItem<com.emoniph.witchery.item.DreamWeaveItem> WEAVE_IRON_ARM =
        ITEMS.register("weave_iron_arm", id -> new com.emoniph.witchery.item.DreamWeaveItem(
            com.emoniph.witchery.blocks.DreamWeaveType.IRON_ARM, itemProps(id)));
    public static final DeferredItem<com.emoniph.witchery.item.DreamWeaveItem> WEAVE_FASTING =
        ITEMS.register("weave_fasting", id -> new com.emoniph.witchery.item.DreamWeaveItem(
            com.emoniph.witchery.blocks.DreamWeaveType.FASTING, itemProps(id)));
    public static final DeferredItem<com.emoniph.witchery.item.DreamWeaveItem> WEAVE_NIGHTMARES =
        ITEMS.register("weave_nightmares", id -> new com.emoniph.witchery.item.DreamWeaveItem(
            com.emoniph.witchery.blocks.DreamWeaveType.NIGHTMARES, itemProps(id)));
    public static final DeferredItem<com.emoniph.witchery.item.DreamWeaveItem> WEAVE_INTENSITY =
        ITEMS.register("weave_intensity", id -> new com.emoniph.witchery.item.DreamWeaveItem(
            com.emoniph.witchery.blocks.DreamWeaveType.INTENSITY, itemProps(id)));

    public static Item getDreamWeaveItem(com.emoniph.witchery.blocks.DreamWeaveType type) {
        return switch (type) {
            case FLEET_FOOT  -> WEAVE_FLEET_FOOT.get();
            case IRON_ARM    -> WEAVE_IRON_ARM.get();
            case FASTING     -> WEAVE_FASTING.get();
            case NIGHTMARES  -> WEAVE_NIGHTMARES.get();
            case INTENSITY   -> WEAVE_INTENSITY.get();
        };
    }
    // ── Death boss drops (outfit items already registered above as DEATHS_HOOD/ROBE/SHOES/HAND)
    public static final DeferredItem<Item> BINKYS_SKULL =
            ITEMS.register("binkys_skull", id -> new com.emoniph.witchery.item.BinkysSkullItem(itemProps(id)));

    // ── Flame Imp items ────────────────────────────────────────────────────────
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> DEMONIC_CONTRACT =
            ITEMS.register("demonic_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));

    // ── Lord of Torment drops ──────────────────────────────────────────────────
    public static final DeferredItem<Item> SOUL_OF_TORMENT  = ITEMS.registerSimpleItem("soul_of_torment");
    public static final DeferredItem<Item> TORMENT_SCROLL   = ITEMS.registerSimpleItem("torment_scroll");

    public static final DeferredItem<BlockItem> ALLURING_SKULL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALLURING_SKULL);
    public static final DeferredItem<BlockItem> DEMON_HEART = ITEMS.registerSimpleBlockItem(WitcheryBlocks.DEMON_HEART);
    public static final DeferredItem<BlockItem> COFFIN               = ITEMS.registerSimpleBlockItem(WitcheryBlocks.COFFIN);
    public static final DeferredItem<BlockItem> ROWAN_PLANKS         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_PLANKS);
    public static final DeferredItem<BlockItem> BLOOD_STAINED_WOOL   = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOOD_STAINED_WOOL);
    public static final DeferredItem<BlockItem> ROWAN_STAIRS         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_STAIRS);
    public static final DeferredItem<BlockItem> ROWAN_SLAB           = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_SLAB);
    public static final DeferredItem<Item>      WITCHES_PRIMER       = ITEMS.register("witches_primer", id -> new com.emoniph.witchery.item.WitchesPrimerItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item>      ROWAN_DOOR_KEY       = ITEMS.register("rowan_door_key", id -> new com.emoniph.witchery.item.RowanDoorKeyItem(itemProps(id)));
    public static final DeferredItem<Item>      ROWAN_KEYRING        = ITEMS.register("rowan_keyring", id -> new com.emoniph.witchery.item.RowanKeyringItem(itemProps(id)));
    public static final DeferredItem<Item>      ROWAN_DOOR           = ITEMS.register("rowan_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.ROWAN_DOOR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> ICE_DOOR             = ITEMS.register("ice_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.ICE_DOOR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> ICE_FENCE            = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_FENCE);
    public static final DeferredItem<BlockItem> ICE_GATE             = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_GATE);
    public static final DeferredItem<BlockItem> ICE_PRESSURE_PLATE   = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> FETISH_SCARECROW    = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_SCARECROW);
    public static final DeferredItem<BlockItem> FETISH_TRENT_EFFIGY  = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_TRENT_EFFIGY);
    public static final DeferredItem<BlockItem> FETISH_WITCHS_LADDER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_WITCHS_LADDER);
    public static final DeferredItem<BlockItem> STATUE_GODDESS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.STATUE_GODDESS);
    public static final DeferredItem<BlockItem> WICKER_BUNDLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WICKER_BUNDLE);
    public static final DeferredItem<BlockItem> BLOODIED_WICKER_BUNDLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOODIED_WICKER_BUNDLE);
    public static final DeferredItem<BlockItem> CRITTER_SNARE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CRITTER_SNARE);
    public static final DeferredItem<BlockItem> BEARTRAP = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BEARTRAP);
    public static final DeferredItem<BlockItem> WOLFTRAP = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WOLFTRAP);
    public static final DeferredItem<BlockItem> WITCH_DOOR = ITEMS.register("witch_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.WITCH_DOOR.get(), itemProps(id)));
    public static final DeferredItem<Item> COTTON = ITEMS.register("cotton", id -> new BlockItem(WitcheryBlocks.COTTON_CROP.get(), itemProps(id)));

    // Pure items
    public static final DeferredItem<Item> ARTHANA = ITEMS.register("arthana", id -> new com.emoniph.witchery.item.ArthanaItem(itemProps(id).durability(250)));
    public static final DeferredItem<Item> KOBOLDITE_PENTACLE = ITEMS.register("koboldite_pentacle", id -> new com.emoniph.witchery.item.PentacleItem(itemProps(id)));
    public static final DeferredItem<BlockItem> STATUE_HOBGOBLIN_PATRON = ITEMS.registerSimpleBlockItem(WitcheryBlocks.STATUE_HOBGOBLIN_PATRON);
    public static final DeferredItem<Item> WAYSTONE = ITEMS.register("waystone", id -> new com.emoniph.witchery.item.WaystoneItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<BlockItem> WOLF_ALTAR = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WOLF_ALTAR);
    public static final DeferredItem<Item> BOLINE = ITEMS.registerSimpleItem("boline");
    public static final DeferredItem<Item> CHALK = ITEMS.register("chalk", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_GLYPH.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_GOLDEN = ITEMS.register("chalk_golden", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_OTHERWHERE = ITEMS.register("chalk_otherwhere", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_OTHERWHERE.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_INFERNAL = ITEMS.register("chalk_infernal", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_INFERNAL.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> MYSTIC_BRANCH = ITEMS.register("mystic_branch", id -> new MysticBranchItem(itemProps(id)));
    public static final DeferredItem<Item> DEBUG_DEATH_WARD =
            ITEMS.register("debug_death_ward", id -> new DebugDeathWardItem(itemProps(id)));
    public static final DeferredItem<Item> TAGLOCK_KIT = ITEMS.register("taglock_kit", id -> new com.emoniph.witchery.item.TaglockKitItem(itemProps(id).stacksTo(16)));

    // ── Poppets ────────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> POPPET = ITEMS.register("poppet", id -> new com.emoniph.witchery.item.PoppetItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> DEATH_PROTECTION_POPPET = ITEMS.register("death_protection_poppet", id -> new com.emoniph.witchery.item.PoppetItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> EARTH_PROTECTION_POPPET = ITEMS.register("earth_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.EARTH, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> WATER_PROTECTION_POPPET = ITEMS.register("water_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.WATER, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> FIRE_PROTECTION_POPPET = ITEMS.register("fire_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.FIRE, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> HUNGER_PROTECTION_POPPET = ITEMS.register("hunger_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.HUNGER, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> VOODOO_PROTECTION_POPPET = ITEMS.register("voodoo_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.VOODOO, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> POPPET_PROTECTION_POPPET = ITEMS.register("poppet_protection_poppet", id -> new com.emoniph.witchery.item.ProtectionPoppetItem(com.emoniph.witchery.item.ProtectionPoppetItem.ProtectionType.POPPET_SHIELD, itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> TOOL_PROTECTION_POPPET = ITEMS.register("tool_protection_poppet", id -> new com.emoniph.witchery.item.PoppetItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> ARMOR_PROTECTION_POPPET = ITEMS.register("armor_protection_poppet", id -> new com.emoniph.witchery.item.PoppetItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> VOODOO_POPPET = ITEMS.register("voodoo_poppet", id -> new com.emoniph.witchery.item.VoodooPoppetItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> VAMPIRIC_POPPET = ITEMS.register("vampiric_poppet", id -> new com.emoniph.witchery.item.VampiricPoppetItem(itemProps(id).stacksTo(1)));
    // ── End poppets ────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> BREW_BAG = ITEMS.registerSimpleItem("brew_bag");
    public static final DeferredItem<Item> DIVINER = ITEMS.registerSimpleItem("diviner");
    public static final DeferredItem<Item> CIRCLE_TALISMAN = ITEMS.register("circle_talisman", id -> new com.emoniph.witchery.item.CircleTalismanItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> WITCH_HAND = ITEMS.registerSimpleItem("witch_hand");
    public static final DeferredItem<Item> WORMWOOD_SEEDS = ITEMS.register("wormwood_seeds", id -> new BlockItem(WitcheryBlocks.WORMWOOD_PLANT.get(), itemProps(id)));
    public static final DeferredItem<Item> MUTATOR = ITEMS.register("mutator", id -> new MutatingSprigItem(itemProps(id).durability(16)));
    public static final DeferredItem<Item> SPECTRAL_STONE = ITEMS.register("spectral_stone", id -> new SpectralStoneItem(itemProps(id)));
    public static final DeferredItem<Item> POPPET_SHELF_COMPASS = ITEMS.registerSimpleItem("poppet_shelf_compass");
    public static final DeferredItem<Item> POLYNESIA_CHARM = ITEMS.registerSimpleItem("polynesia_charm");
    public static final DeferredItem<Item> CHARM_OF_DISRUPTED_DREAMS = ITEMS.registerSimpleItem("charm_of_disrupted_dreams");
    public static final DeferredItem<Item> PARASYTIC_LOUSE = ITEMS.register("parasytic_louse", id -> new ParasyticLouseItem(itemProps(id)));
    public static final DeferredItem<Item> LEAPING_LILY_ITEM = ITEMS.registerSimpleItem("leaping_lily_item");
    public static final DeferredItem<Item> BUCKET_SPIRIT = ITEMS.registerSimpleItem("bucket_spirit");
    public static final DeferredItem<Item> HAND_BOW = ITEMS.registerSimpleItem("hand_bow");
    public static final DeferredItem<Item> HUNTSMAN_SPEAR = ITEMS.registerSimpleItem("huntsman_spear");

    // Armour
    public static final DeferredItem<Item> WITCHES_HAT = ITEMS.registerSimpleItem("witches_hat");
    public static final DeferredItem<Item> WITCHES_ROBE = ITEMS.registerSimpleItem("witches_robe");
    public static final DeferredItem<Item> WITCHES_SHOES = ITEMS.registerSimpleItem("witches_shoes");
    public static final DeferredItem<Item> EARMUFFS = ITEMS.register("earmuffs", id ->
            new EarmuffsItem(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.HELMET)));
    public static final DeferredItem<Item> BITING_BELT = ITEMS.register("biting_belt", id ->
            new BitingBeltItem(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> BARK_BELT = ITEMS.register("bark_belt", id ->
            new BarkBeltItem(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> WOVEN_CRUOR = ITEMS.registerSimpleItem("woven_cruor");
    public static final DeferredItem<Item> VAMPIRE_TOP_HAT = ITEMS.register("vampire_top_hat", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_VEIL_HAT = ITEMS.register("vampire_veil_hat", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_DRESS_COAT = ITEMS.register("vampire_dress_coat", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_DRESS_JACKET = ITEMS.register("vampire_dress_jacket", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_COAT = ITEMS.register("vampire_coat", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_CORSET = ITEMS.register("vampire_corset", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_TROUSERS = ITEMS.register("vampire_trousers", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SKIRTED_TROUSERS = ITEMS.register("vampire_skirted_trousers", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SKIRT = ITEMS.register("vampire_skirt", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_OXFORD_BOOTS = ITEMS.register("vampire_oxford_boots", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SHOES = ITEMS.register("vampire_shoes", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_HEELS = ITEMS.register("vampire_heels", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_HELMET = ITEMS.register("vampire_helmet", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHAIN_COAT = ITEMS.register("vampire_chain_coat", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHAIN_COAT_LADIES = ITEMS.register("vampire_chain_coat_ladies", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHESTPLATE = ITEMS.register("vampire_chestplate", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_LEGGINGS = ITEMS.register("vampire_leggings", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_BOOTS = ITEMS.register("vampire_boots", id ->
            vampireArmor(id, net.minecraft.world.item.equipment.ArmorMaterials.IRON,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> HUNTER_HELMET = ITEMS.registerSimpleItem("hunter_helmet");
    public static final DeferredItem<Item> HUNTER_CHESTPLATE = ITEMS.registerSimpleItem("hunter_chestplate");
    public static final DeferredItem<Item> HUNTER_LEGGINGS = ITEMS.registerSimpleItem("hunter_leggings");
    public static final DeferredItem<Item> HUNTER_BOOTS = ITEMS.registerSimpleItem("hunter_boots");
    public static final DeferredItem<Item> DEATHS_HOOD = ITEMS.registerSimpleItem("deaths_hood");
    public static final DeferredItem<Item> DEATHS_ROBE = ITEMS.registerSimpleItem("deaths_robe");
    public static final DeferredItem<Item> DEATHS_SHOES = ITEMS.registerSimpleItem("deaths_shoes");
    public static final DeferredItem<Item> DEATHS_HAND = ITEMS.registerSimpleItem("deaths_hand");

    // Brew items — all now have implemented effects
    public static final DeferredItem<Item> BREW_OF_RAISING        = ITEMS.register("brew_of_raising", id -> new BrewOfRaisingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SLEEPING       = ITEMS.register("brew_of_sleeping", id -> new BrewOfSleepingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FLOWING_SPIRIT = ITEMS.register("brew_of_flowing_spirit", id -> new BrewOfFlowingSpiritItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_WASTING        = ITEMS.register("brew_of_wasting", id -> new BrewOfWastingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_EROSION        = ITEMS.register("brew_of_erosion", id -> new BrewOfErosionItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FOULNESS       = ITEMS.register("brew_of_foulness", id -> new BrewOfFoulnessItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_INFECTION      = ITEMS.register("brew_of_infection", id -> new BrewOfInfectionItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_LOVE           = ITEMS.register("brew_of_love", id -> new BrewOfLoveItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_THE_GROTESQUE  = ITEMS.register("brew_of_the_grotesque", id -> new BrewOfTheGrotesqueItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_REVEALING      = ITEMS.register("brew_of_revealing", id -> new BrewOfRevealingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_POISON         = ITEMS.register("brew_of_poison", id -> new BrewOfPoisonItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FOAMING        = ITEMS.register("brew_of_foaming", id -> new BrewOfFoamingItem(itemProps(id)));

    // ── Brews — implemented ───────────────────────────────────────────────────
    public static final DeferredItem<Item> BREW_OF_VINES          = ITEMS.register("brew_of_vines", id -> new com.emoniph.witchery.item.BrewOfVinesItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_WEBS           = ITEMS.register("brew_of_webs", id -> new com.emoniph.witchery.item.BrewOfWebsItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_THORNS         = ITEMS.register("brew_of_thorns", id -> new com.emoniph.witchery.item.BrewOfThornsItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_INK            = ITEMS.register("brew_of_ink", id -> new com.emoniph.witchery.item.BrewOfInkItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SPROUTING      = ITEMS.register("brew_of_sprouting", id -> new com.emoniph.witchery.item.BrewOfSproutingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_ICE            = ITEMS.register("brew_of_ice", id -> new com.emoniph.witchery.item.BrewOfIceItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_THE_DEPTHS     = ITEMS.register("brew_of_the_depths", id -> new com.emoniph.witchery.item.BrewOfTheDepthsItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FROGS_TONGUE   = ITEMS.register("brew_of_frogs_tongue", id -> new com.emoniph.witchery.item.BrewOfFrogsTongueItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_CURSED_LEAPING = ITEMS.register("brew_of_cursed_leaping", id -> new com.emoniph.witchery.item.BrewOfCursedLeapingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_HITCHCOCK      = ITEMS.register("brew_of_hitchcock", id -> new com.emoniph.witchery.item.BrewOfHitchcockItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_BATS           = ITEMS.register("brew_of_bats", id -> new com.emoniph.witchery.item.BrewOfBatsItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_HOLLOW_TEARS   = ITEMS.register("brew_of_hollow_tears", id -> new com.emoniph.witchery.item.BrewOfHollowTearsItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOLID_STONE    = ITEMS.register("brew_of_solid_stone", id -> new com.emoniph.witchery.item.BrewOfSolidifyingItem.Stone(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOLID_DIRT     = ITEMS.register("brew_of_solid_dirt", id -> new com.emoniph.witchery.item.BrewOfSolidifyingItem.Dirt(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOLID_SAND     = ITEMS.register("brew_of_solid_sand", id -> new com.emoniph.witchery.item.BrewOfSolidifyingItem.Sand(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOLID_SANDSTONE = ITEMS.register("brew_of_solid_sandstone", id -> new com.emoniph.witchery.item.BrewOfSolidifyingItem.Sandstone(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOLID_EROSION  = ITEMS.register("brew_of_solid_erosion", id -> new com.emoniph.witchery.item.GenericBrewItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOARING        = ITEMS.register("brew_of_soaring", id -> new com.emoniph.witchery.item.BrewOfSoaringItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_GRAVE          = ITEMS.register("brew_of_grave", id -> new com.emoniph.witchery.item.GenericBrewItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SUBSTITUTION   = ITEMS.register("brew_of_substitution", id -> new com.emoniph.witchery.item.BrewOfSubstitutionItem(itemProps(id)));

    // ── Infusion brew bases (M19) ─────────────────────────────────────────────
    public static final DeferredItem<Item> GHOST_OF_THE_LIGHT   = ITEMS.registerSimpleItem("ghost_of_the_light");
    public static final DeferredItem<Item> SOUL_OF_THE_WORLD    = ITEMS.registerSimpleItem("soul_of_the_world");
    public static final DeferredItem<Item> SPIRIT_OF_OTHERWHERE = ITEMS.registerSimpleItem("spirit_of_otherwhere");
    public static final DeferredItem<Item> INFERNAL_ANIMUS      = ITEMS.registerSimpleItem("infernal_animus");
    public static final DeferredItem<Item> MYSTIC_UNGUENT       = ITEMS.registerSimpleItem("mystic_unguent");
    public static final DeferredItem<Item> FLYING_OINTMENT      = ITEMS.registerSimpleItem("flying_ointment");
    public static final DeferredItem<Item> HAPPENSTANCE_OIL     = ITEMS.registerSimpleItem("happenstance_oil");
    public static final DeferredItem<Item> INFUSED_BREW_BASE    = ITEMS.registerSimpleItem("infused_brew_base");
    public static final DeferredItem<Item> INFUSED_BREW_OF_SOARING   = ITEMS.registerSimpleItem("infused_brew_of_soaring");
    public static final DeferredItem<Item> INFUSED_BREW_OF_THE_GRAVE = ITEMS.registerSimpleItem("infused_brew_of_the_grave");

    // ── Ingredient items (Task 2 batch) ───────────────────────────────────────
    public static final DeferredItem<Item> BONE_NEEDLE        = ITEMS.register("bone_needle", id -> new BoneNeedleItem(itemProps(id)));
    public static final DeferredItem<Item> BREATH_OF_GODDESS  = ITEMS.registerSimpleItem("breath_of_goddess");
    public static final DeferredItem<Item> CONDENSED_FEAR     = ITEMS.registerSimpleItem("condensed_fear");
    public static final DeferredItem<Item> TONGUE_OF_DOG      = ITEMS.registerSimpleItem("tongue_of_dog");
    public static final DeferredItem<Item> DROP_OF_LUCK       = ITEMS.registerSimpleItem("drop_of_luck");
    public static final DeferredItem<Item> ENDER_DEW          = ITEMS.registerSimpleItem("ender_dew");
    public static final DeferredItem<Item> ENT_TWIG           = ITEMS.registerSimpleItem("ent_twig");
    public static final DeferredItem<Item> FOCUSED_WILL       = ITEMS.registerSimpleItem("focused_will");
    public static final DeferredItem<Item> FOUL_FUME          = ITEMS.registerSimpleItem("foul_fume");
    public static final DeferredItem<Item> FROZEN_HEART       = ITEMS.registerSimpleItem("frozen_heart");
    public static final DeferredItem<Item> GOLDEN_THREAD      = ITEMS.registerSimpleItem("golden_thread");
    public static final DeferredItem<Item> GRAVEYARD_DUST     = ITEMS.registerSimpleItem("graveyard_dust");
    public static final DeferredItem<Item> GYPSUM             = ITEMS.registerSimpleItem("gypsum");
    public static final DeferredItem<Item> ICY_NEEDLE         = ITEMS.register("icy_needle", id -> new com.emoniph.witchery.item.IcyNeedleItem(itemProps(id)));
    public static final DeferredItem<Item> IMPREGNATED_LEATHER= ITEMS.registerSimpleItem("impregnated_leather");
    public static final DeferredItem<Item> MELLIFLUOUS_HUNGER = ITEMS.registerSimpleItem("mellifluous_hunger");
    public static final DeferredItem<com.emoniph.witchery.item.MutandisItem> MUTANDIS =
            ITEMS.register("mutandis", id -> new com.emoniph.witchery.item.MutandisItem(false, itemProps(id).stacksTo(16)));
    public static final DeferredItem<com.emoniph.witchery.item.MutandisItem> MUTANDIS_EXTREMIS =
            ITEMS.register("mutandis_extremis", id -> new com.emoniph.witchery.item.MutandisItem(true, itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> NULL_CATALYST      = ITEMS.registerSimpleItem("null_catalyst");
    public static final DeferredItem<Item> ODOUR_OF_PURITY    = ITEMS.registerSimpleItem("odour_of_purity");
    public static final DeferredItem<Item> OIL_OF_VITRIOL     = ITEMS.registerSimpleItem("oil_of_vitriol");
    public static final DeferredItem<Item> OWLETS_WING        = ITEMS.registerSimpleItem("owlets_wing");
    public static final DeferredItem<Item> PURIFIED_MILK      = ITEMS.registerSimpleItem("purified_milk");
    public static final DeferredItem<Item> QUICKLIME          = ITEMS.registerSimpleItem("quicklime");
    public static final DeferredItem<Item> REEK_OF_MISFORTUNE = ITEMS.registerSimpleItem("reek_of_misfortune");
    public static final DeferredItem<Item> REFINED_EVIL       = ITEMS.registerSimpleItem("refined_evil");
    public static final DeferredItem<Item> SPECTRAL_DUST      = ITEMS.registerSimpleItem("spectral_dust");
    public static final DeferredItem<Item> SUBDUED_SPIRIT     = ITEMS.registerSimpleItem("subdued_spirit");
    public static final DeferredItem<Item> TEAR_OF_GODDESS    = ITEMS.registerSimpleItem("tear_of_goddess");
    public static final DeferredItem<Item> TOE_OF_FROG        = ITEMS.registerSimpleItem("toe_of_frog");
    public static final DeferredItem<Item> TORMENTED_TWINE    = ITEMS.registerSimpleItem("tormented_twine");
    public static final DeferredItem<Item> WHIFF_OF_MAGIC     = ITEMS.registerSimpleItem("whiff_of_magic");
    public static final DeferredItem<Item> ATTUNED_STONE      = ITEMS.registerSimpleItem("attuned_stone");
    public static final DeferredItem<Item> ATTUNED_STONE_CHARGED = ITEMS.register("attuned_stone_charged",
            id -> new Item(itemProps(id)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });
    public static final DeferredItem<Item> NECRO_STONE        = ITEMS.registerSimpleItem("necro_stone");
    public static final DeferredItem<Item> QUARTZ_SPHERE      = ITEMS.registerSimpleItem("quartz_sphere");
    public static final DeferredItem<Item> SEER_STONE         = ITEMS.register("seer_stone",
            id -> new com.emoniph.witchery.item.SeerStoneItem(itemProps(id).stacksTo(1)) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });
    public static final DeferredItem<Item> CREEPER_HEART      = ITEMS.registerSimpleItem("creeper_heart");
    public static final DeferredItem<Item> CLAY_JAR           = ITEMS.registerSimpleItem("clay_jar");
    public static final DeferredItem<Item> CLAY_JAR_SOFT      = ITEMS.registerSimpleItem("clay_jar_soft");
    public static final DeferredItem<Item> ROCK               = ITEMS.registerSimpleItem("rock");
    public static final DeferredItem<Item> WOLFSBANE_FLOWER   = ITEMS.registerSimpleItem("wolfsbane_flower");
    public static final DeferredItem<Item> SILVER_INGOT        = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> SILVER_SWORD        = ITEMS.register("silver_sword", id -> new Item(itemProps(id).sword(net.minecraft.world.item.ToolMaterial.GOLD, 3.0f, -2.4f)));
    public static final DeferredItem<Item> SILVER_BOLT         = ITEMS.registerSimpleItem("silver_bolt");
    public static final DeferredItem<BlockItem> SILVER_VAT     = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SILVER_VAT);
    public static final DeferredItem<Item> HUNTER_HELMET_SILVERED     = ITEMS.register("hunter_helmet_silvered", id -> new Item(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.IRON, net.minecraft.world.item.equipment.ArmorType.HELMET)));
    public static final DeferredItem<Item> HUNTER_CHESTPLATE_SILVERED = ITEMS.register("hunter_chestplate_silvered", id -> new Item(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.IRON, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> HUNTER_LEGGINGS_SILVERED   = ITEMS.register("hunter_leggings_silvered", id -> new Item(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.IRON, net.minecraft.world.item.equipment.ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> HUNTER_BOOTS_SILVERED      = ITEMS.register("hunter_boots_silvered", id -> new Item(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.IRON, net.minecraft.world.item.equipment.ArmorType.BOOTS)));
    public static final DeferredItem<Item> DENSE_WEB          = ITEMS.registerSimpleItem("dense_web");
    public static final DeferredItem<Item> DEMONIC_BLOOD      = ITEMS.register("demonic_blood", id -> new DemonicBloodItem(itemProps(id)));
    public static final DeferredItem<Item> CONGEALED_SPIRIT   = ITEMS.registerSimpleItem("congealed_spirit");
    // Koboldite material chain
    public static final DeferredItem<Item> KOBOLDITE_DUST     = ITEMS.registerSimpleItem("koboldite_dust");
    public static final DeferredItem<Item> KOBOLDITE_NUGGET   = ITEMS.registerSimpleItem("koboldite_nugget");
    public static final DeferredItem<Item> KOBOLDITE_INGOT    = ITEMS.registerSimpleItem("koboldite_ingot");
    public static final DeferredItem<Item> KOBOLDITE_PICKAXE  = ITEMS.register("koboldite_pickaxe", id -> new Item(net.minecraft.world.item.ToolMaterial.IRON.applyToolProperties(
                    itemProps(id),
                    net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL,
                    6.0f, 1.0f, -2.8f)));
    // ── Broom ──────────────────────────────────────────────────────────────────
    public static final DeferredItem<BroomItem> BROOM =
            ITEMS.register("broom", id -> new BroomItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<BroomItem> ENCHANTED_BROOM =
            ITEMS.register("enchanted_broom", id -> new BroomItem(itemProps(id).stacksTo(1)));

    public static final DeferredItem<Item> ANOINTING_PASTE    = ITEMS.registerSimpleItem("anointing_paste");
    public static final DeferredItem<Item> WORMY_APPLE        = ITEMS.registerSimpleItem("wormy_apple");
    public static final DeferredItem<Item> APPLE_OF_SLEEPING  = ITEMS.register("apple_of_sleeping", id -> new com.emoniph.witchery.item.AppleOfSleepingItem(itemProps(id)));
    public static final DeferredItem<Item> TREEFYD_SEEDS      = ITEMS.register("treefyd_seeds", id -> new com.emoniph.witchery.item.TreefydSeedsItem(itemProps(id)));

    // Source/wikilist placeholders: registered so the remaining item roster is visible in-game and review docs.
    public static final Map<String, DeferredItem<Item>> LEGACY_PLACEHOLDER_ITEMS = registerLegacyPlaceholderItems(
            "babas_hat",
            "necromancer_robes",
            "ice_slippers",
            "ruby_slippers",
            "seeping_shoes",
            "devils_tongue_charm",
            "water_diviner",
            "lava_diviner",
            "bucket_hollow_tears",
            "hollow_tears",
            "alder_door",
            "alder_planks",
            "alder_stairs",
            "alder_slab",
            "fume_filter",
            "infernal_blood",
            "odd_porkchop_raw",
            "odd_porkchop_cooked",
            "book_oven",
            "book_distilling",
            "book_circle_magic",
            "book_infusions",
            "book_herbology",
            "book_biomes",
            "book_wands",
            "book_burning",
            "nullified_leather",
            "bolt_stake",
            "bolt_anti_magic",
            "bolt_holy",
            "bolt_splitting",
            "brew_of_soul_hunger",
            "brew_of_soul_anguish",
            "brew_of_soul_fear",
            "brew_of_soul_torment",
            "contract_ownership",
            "contract_torment",
            "contract_blaze",
            "contract_resist_fire",
            "contract_evaporate",
            "contract_fiery_touch",
            "contract_smelting",
            "silver_dust",
            "alder_boat",
            "hawthorn_boat",
            "hawthorn_door",
            "hawthorn_planks",
            "hawthorn_stairs",
            "hawthorn_slab",
            "biome_note",
            "blooded_waystone",
            "bound_waystone",
            "bone_bolt",
            "book_biomes_extended",
            "brews_and_infusions_book",
            "collecting_fumes_book",
            "conjuration_and_fetishes_book",
            "extended_biomes_book",
            "immortals_observations_book",
            "symbology_book",
            "witches_brews_book",
            "cane_sword",
            "charged_crossbow",
            "cooked_sheep_liver",
            "creative_bat_wolf_token",
            "creative_medallion",
            "dandelion_of_ink",
            "dandelion_of_sprouting",
            "dandelion_of_thorns",
            "dandelion_of_webs",
            "duplication_grenade",
            "filtered_fume_funnel",
            "garlic",
            "garlic_garland",
            "glass_goblet",
            "glass_goblet_full",
            "gulgs_girdle",
            "hellhound_head",
            "horn_of_the_hunt",
            "ice_slab",
            "ice_stairs",
            "ice_stockade",
            "mogs_quiver",
            "moon_charm",
            "nullifying_bolt",
            "perpetual_ice",
            "player_compass",
            "poppy_of_ink",
            "poppy_of_sprouting",
            "poppy_of_thorns",
            "poppy_of_webs",
            "trapped_plant",
            "sheep_liver",
            "shrub_of_ink",
            "shrub_of_sprouting",
            "shrub_of_thorns",
            "shrub_of_webs",
            "snow_pressure_plate",
            "snow_slab",
            "snow_stairs",
            "staff_of_duplication",
            "sun_collector",
            "sun_grenade",
            "twisting_band",
            "universal_antidote",
            "witch_hunter_dawn_boots",
            "witch_hunter_dawn_coat",
            "witch_hunter_dawn_hat",
            "witch_hunter_dawn_trousers",
            "wolf_head",
            "wooden_bolt",
            "wolfsbane",
            "wolfsbane_seeds",
            "brew_of_endless_water",
            "warm_blood",
            "liliths_blood",
            "leonards_urn",

            // Exact 1.12 Resurrected registry IDs that differ from the current/source-backed names.
            "bound_taglock_kit",
            "meaty_stew",
            "raw_meaty_stew",
            "huntsmans_spear",
            "crossbow_pistol",
            "deployed_deaths_hand",
            "drawn_cane_sword",
            "witches_hand",
            "mutating_sprig",
            "brew_bottle",
            "splash_brew_bottle",
            "lingering_brew_bottle",
            "combustion_brew",
            "endless_water_brew",
            "hunts_horn",
            "full_glass_goblet",
            "light_ghost",
            "world_soul",
            "otherwhere_spirit",
            "vines_brew",
            "webs_brew",
            "thorns_brew",
            "ink_brew",
            "sprouting_brew",
            "erosion_brew",
            "raising_brew",
            "love_brew",
            "ice_brew",
            "frogs_tongue_brew",
            "cursed_leaping_brew",
            "hitchcock_brew",
            "infection_brew",
            "flowing_spirit_brew",
            "hollow_tears_brew",
            "wasting_brew",
            "bats_brew",
            "revealing_brew",
            "substitution_brew",
            "grotesque_brew",
            "depths_brew",
            "sleeping_brew",
            "infused_soaring_brew",
            "infused_grave_brew",
            "solidifying_stone_brew",
            "solidifying_dirt_brew",
            "solidifying_sand_brew",
            "solidifying_sandstone_brew",
            "solidifying_erosion_brew",
            "hunger_demon_soul",
            "anguish_demon_soul",
            "fear_demon_soul",
            "torment_demon_soul",
            "distilling_book",
            "circle_magic_book",
            "herbology_book",
            "biomes_book",
            "splitting_bolt",
            "torment_contract",
            "living_flame_contract",
            "fiery_tolerance_contract",
            "evaporation_contract",
            "fiery_touch_contract",
            "melting_touch_contract",
            "witches_robes",
            "icy_slippers",
            "hunter_hat",
            "hunter_coat",
            "silvered_hunter_hat",
            "silvered_hunter_coat",
            "silvered_hunter_leggings",
            "silvered_hunter_boots",
            "dawn_hunter_hat",
            "dawn_hunter_coat",
            "dawn_hunter_leggings",
            "dawn_hunter_boots",
            "gulgs_grudle",
            "soft_clay_jar",
            "diamond_vapor",
            "vitriol_oil",
            "horned_one_exhale",
            "goddess_breath",
            "goddess_tear",
            "rebirth_hint",
            "magic_whiff",
            "misfortune_reek",
            "purity_odor",
            "luck_drop",
            "charged_attuned_stone",
            "belladonna_flower",
            "snowbell_flower",
            "dog_tongue",
            "odd_porkchop",
            "cooked_odd_porkchop",
            "necromantic_stone",
            "artichoke_globe",
            "frogs_toe",
            "sleeping_apple",
            "fanciful_thinking_charm",
            "subdued_village_spirit",
            "pentacle",
            "silver_deposits",
            "torn_page",
            "stake",
            "golden_heart"
    );

    private static Map<String, DeferredItem<Item>> registerLegacyPlaceholderItems(String... ids) {
        LinkedHashMap<String, DeferredItem<Item>> placeholders = new LinkedHashMap<>();
        for (String id : ids) {
            placeholders.put(id, ITEMS.registerSimpleItem(id));
        }
        return Collections.unmodifiableMap(placeholders);
    }

    // ── Spawn eggs (creative only) ─────────────────────────────────────────────
    // 0x2E0047 = dark purple shell, 0xAAFF55 = bright witch-green spots
    public static final DeferredItem<SpawnEggItem> COVEN_WITCH_SPAWN_EGG =
            ITEMS.register("coven_witch_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.COVEN_WITCH.get())));

    // dark brown shell + gold spots (hunter leather + crossbow bolts)
    public static final DeferredItem<SpawnEggItem> WITCH_HUNTER_SPAWN_EGG =
            ITEMS.register("witch_hunter_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.WITCH_HUNTER.get())));

    // ── M27 entity spawn eggs ──────────────────────────────────────────────────
    public static final DeferredItem<SpawnEggItem> BANSHEE_SPAWN_EGG =
            ITEMS.register("banshee_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.BANSHEE.get())));
    public static final DeferredItem<SpawnEggItem> SPECTRE_SPAWN_EGG =
            ITEMS.register("spectre_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.SPECTRE.get())));
    public static final DeferredItem<SpawnEggItem> POLTERGEIST_SPAWN_EGG =
            ITEMS.register("poltergeist_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.POLTERGEIST.get())));
    public static final DeferredItem<SpawnEggItem> TREEFYD_SPAWN_EGG =
            ITEMS.register("treefyd_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.TREEFYD.get())));
    public static final DeferredItem<SpawnEggItem> ENT_SPAWN_EGG =
            ITEMS.register("ent_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.ENT.get())));
    public static final DeferredItem<SpawnEggItem> MANDRAKE_MOB_SPAWN_EGG =
            ITEMS.register("mandrake_mob_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.MANDRAKE_MOB.get())));
    public static final DeferredItem<SpawnEggItem> DEMON_SPAWN_EGG =
            ITEMS.register("demon_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.DEMON.get())));

    public static final DeferredItem<SpawnEggItem> NIGHTMARE_SPAWN_EGG =
            ITEMS.register("nightmare_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.NIGHTMARE.get())));
    public static final DeferredItem<SpawnEggItem> SPIRIT_SPAWN_EGG =
            ITEMS.register("spirit_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.SPIRIT.get())));

    // ── Death spawn egg ───────────────────────────────────────────────────────
    public static final DeferredItem<SpawnEggItem> DEATH_SPAWN_EGG =
            ITEMS.register("death_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.DEATH.get())));

    // ── Torment entities spawn eggs ───────────────────────────────────────────
    // Flame Imp: orange body (fire), dark red spots
    public static final DeferredItem<SpawnEggItem> FLAME_IMP_SPAWN_EGG =
            ITEMS.register("flame_imp_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.FLAME_IMP.get())));
    // Lord of Torment: void-black body, sickly purple-gold boss colours
    public static final DeferredItem<SpawnEggItem> LORD_OF_TORMENT_SPAWN_EGG =
            ITEMS.register("lord_of_torment_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.LORD_OF_TORMENT.get())));
    public static final DeferredItem<SpawnEggItem> HORNED_HUNTSMAN_SPAWN_EGG =
            ITEMS.register("horned_huntsman_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.HORNED_HUNTSMAN.get())));

    // ── Familiar spawn eggs ────────────────────────────────────────────────────
    public static final DeferredItem<SpawnEggItem> RAVEN_SPAWN_EGG =
            ITEMS.register("raven_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.RAVEN.get())));
    public static final DeferredItem<SpawnEggItem> OWL_SPAWN_EGG =
            ITEMS.register("owl_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.OWL.get())));
    public static final DeferredItem<SpawnEggItem> FAMILIAR_CAT_SPAWN_EGG =
            ITEMS.register("familiar_cat_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.FAMILIAR_CAT.get())));
    public static final DeferredItem<SpawnEggItem> SPECTRAL_FAMILIAR_SPAWN_EGG =
            ITEMS.register("spectral_familiar_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.SPECTRAL_FAMILIAR.get())));
    public static final DeferredItem<SpawnEggItem> TOAD_SPAWN_EGG =
            ITEMS.register("toad_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.TOAD.get())));
    public static final DeferredItem<SpawnEggItem> HOBGOBLIN_SPAWN_EGG =
            ITEMS.register("hobgoblin_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.HOBGOBLIN.get())));
    public static final DeferredItem<SpawnEggItem> FLYING_MONKEY_SPAWN_EGG =
            ITEMS.register("flying_monkey_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.FLYING_MONKEY.get())));
    public static final DeferredItem<SpawnEggItem> BINKY_SPAWN_EGG =
            ITEMS.register("binky_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.BINKY.get())));
}
