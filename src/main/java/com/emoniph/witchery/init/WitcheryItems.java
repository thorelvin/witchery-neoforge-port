package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.brewing.Brews;
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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;

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

    /** A topic guide book that opens the Witches' Primer straight to the given category/entry. */
    private static com.emoniph.witchery.item.WitchesPrimerItem primerGuide(
            net.minecraft.resources.Identifier id, String category, String entry) {
        return new com.emoniph.witchery.item.WitchesPrimerItem(
                itemProps(id).stacksTo(1), null,
                com.emoniph.witchery.book.WitcheryBooks.PRIMER,
                com.emoniph.witchery.book.WitcheryBooks.id(category),
                com.emoniph.witchery.book.WitcheryBooks.id(entry));
    }

    private static Item vampireArmor(net.minecraft.resources.Identifier id,
                                     net.minecraft.world.item.equipment.ArmorMaterial material,
                                     net.minecraft.world.item.equipment.ArmorType armorType,
                                     VampireClothingItem.SetType setType) {
        return new VampireClothingItem(setType, itemProps(id).humanoidArmor(material, armorType));
    }

    // ── Vampire armor materials ───────────────────────────────────────────────
    // Vanilla LEATHER/IRON would render the worn armor as leather/iron. These clone those stats but
    // point at custom equipment assets (assets/witchery/equipment/vampire_*.json), so the worn armor
    // uses the ported vampire coat/dress/chestplate textures (and, with the client model hook, geometry).
    public static final net.minecraft.world.item.equipment.ArmorMaterial VAMPIRE_MASCULINE_MATERIAL =
            vampireMaterial(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER, "vampire_masculine");
    public static final net.minecraft.world.item.equipment.ArmorMaterial VAMPIRE_FEMININE_MATERIAL =
            vampireMaterial(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER, "vampire_feminine");
    public static final net.minecraft.world.item.equipment.ArmorMaterial VAMPIRE_METALLIC_MATERIAL =
            vampireMaterial(net.minecraft.world.item.equipment.ArmorMaterials.IRON, "vampire_metallic");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_PLAIN_MATERIAL =
            hunterMaterial("hunter_plain");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_PLAIN_BOOTS_MATERIAL =
            hunterMaterial("hunter_plain_boots");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_SILVERED_MATERIAL =
            hunterMaterial("hunter_silvered");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_SILVERED_BOOTS_MATERIAL =
            hunterMaterial("hunter_silvered_boots");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_DAWN_MATERIAL =
            hunterMaterial("hunter_dawn");
    public static final net.minecraft.world.item.equipment.ArmorMaterial HUNTER_DAWN_BOOTS_MATERIAL =
            hunterMaterial("hunter_dawn_boots");
    public static final net.minecraft.world.item.equipment.ArmorMaterial DEATHS_CLOTHING_MATERIAL =
            deathsMaterial("deaths_clothes");
    public static final net.minecraft.world.item.equipment.ArmorMaterial WITCHES_ROBES_MATERIAL =
            witchesRobesMaterial("witches_robes");
    public static final net.minecraft.world.item.equipment.ArmorMaterial BABAS_HAT_MATERIAL =
            witchesRobesMaterial("babas_hat");
    public static final net.minecraft.world.item.equipment.ArmorMaterial NECROMANCER_ROBES_MATERIAL =
            witchesRobesMaterial("necromancer_robes");
    public static final net.minecraft.world.item.equipment.ArmorMaterial ICY_SLIPPERS_MATERIAL =
            witchesRobesMaterial("icy_slippers");
    public static final net.minecraft.world.item.equipment.ArmorMaterial RUBY_SLIPPERS_MATERIAL =
            witchesRobesMaterial("ruby_slippers");
    public static final net.minecraft.world.item.equipment.ArmorMaterial SEEPING_SHOES_MATERIAL =
            witchesRobesMaterial("seeping_shoes");
    public static final net.minecraft.world.item.equipment.ArmorMaterial TWISTING_BAND_MATERIAL =
            twistingBandMaterial("twisting_band");
    public static final net.minecraft.world.item.equipment.ArmorMaterial MOGS_QUIVER_MATERIAL =
            goblinClothesMaterial("mogs_quiver");
    public static final net.minecraft.world.item.equipment.ArmorMaterial GULGS_GIRDLE_MATERIAL =
            goblinClothesMaterial("gulgs_girdle");

    private static net.minecraft.world.item.equipment.ArmorMaterial vampireMaterial(
            net.minecraft.world.item.equipment.ArmorMaterial base, String assetPath) {
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial hunterMaterial(String assetPath) {
        net.minecraft.world.item.equipment.ArmorMaterial base = net.minecraft.world.item.equipment.ArmorMaterials.LEATHER;
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial deathsMaterial(String assetPath) {
        net.minecraft.world.item.equipment.ArmorMaterial base = net.minecraft.world.item.equipment.ArmorMaterials.IRON;
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial witchesRobesMaterial(String assetPath) {
        net.minecraft.world.item.equipment.ArmorMaterial base = net.minecraft.world.item.equipment.ArmorMaterials.LEATHER;
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial twistingBandMaterial(String assetPath) {
        net.minecraft.world.item.equipment.ArmorMaterial base = net.minecraft.world.item.equipment.ArmorMaterials.IRON;
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial goblinClothesMaterial(String assetPath) {
        net.minecraft.world.item.equipment.ArmorMaterial base = net.minecraft.world.item.equipment.ArmorMaterials.LEATHER;
        return new net.minecraft.world.item.equipment.ArmorMaterial(
                base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), base.repairIngredient(),
                witcheryEquipmentAsset(assetPath));
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> witcheryEquipmentAsset(String path) {
        return net.minecraft.resources.ResourceKey.create(
                net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path));
    }

    private static Item deathsArmor(net.minecraft.resources.Identifier id,
                                    net.minecraft.world.item.equipment.ArmorType armorType,
                                    DeathsClothingItem.Piece piece) {
        int diamondDurability = armorType.getDurability(net.minecraft.world.item.equipment.ArmorMaterials.DIAMOND.durability());
        return new DeathsClothingItem(piece, itemProps(id)
                .humanoidArmor(DEATHS_CLOTHING_MATERIAL, armorType)
                .durability(diamondDurability)
                .rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    private static Item hunterArmor(net.minecraft.resources.Identifier id,
                                    net.minecraft.world.item.equipment.ArmorType armorType,
                                    HunterClothingItem.Variant variant) {
        int ironDurability = armorType.getDurability(net.minecraft.world.item.equipment.ArmorMaterials.IRON.durability());
        return new HunterClothingItem(variant, itemProps(id)
                .humanoidArmor(hunterMaterialFor(variant, armorType), armorType)
                .durability(ironDurability)
                .rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    private static net.minecraft.world.item.equipment.ArmorMaterial hunterMaterialFor(
            HunterClothingItem.Variant variant, net.minecraft.world.item.equipment.ArmorType armorType) {
        boolean boots = armorType == net.minecraft.world.item.equipment.ArmorType.BOOTS;
        return switch (variant) {
            case PLAIN -> boots ? HUNTER_PLAIN_BOOTS_MATERIAL : HUNTER_PLAIN_MATERIAL;
            case SILVERED -> boots ? HUNTER_SILVERED_BOOTS_MATERIAL : HUNTER_SILVERED_MATERIAL;
            case DAWN -> boots ? HUNTER_DAWN_BOOTS_MATERIAL : HUNTER_DAWN_MATERIAL;
        };
    }

    private static Item earmuffs(net.minecraft.resources.Identifier id) {
        return new EarmuffsItem(itemProps(id)
                .durability(56)
                .enchantable(15)
                .repairable(net.minecraft.world.item.Items.LEATHER)
                .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                        net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
                                .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER)
                                .setDispensable(true)
                                .setSwappable(true)
                                .setDamageOnHurt(true)
                                .build())
                .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "earmuffs_armor"),
                                        1.0D,
                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.HEAD)
                        .build()));
    }

    // ── M3/M4 crop + ingredient items ──────────────────────────────────────────
    // Witchery crops now split closer to source between planting stock and the
    // harvested ingredient item they produce.
    public static final DeferredItem<Item> BELLADONNA = ITEMS.registerSimpleItem("belladonna");
    public static final DeferredItem<Item> BELLADONNA_SEEDS = ITEMS.register("belladonna_seeds", id -> new BlockItem(WitcheryBlocks.BELLADONNA.get(), itemProps(id)));
    public static final DeferredItem<Item> MANDRAKE_ROOT = ITEMS.registerSimpleItem("mandrake_root");
    public static final DeferredItem<Item> MANDRAKE_SEEDS = ITEMS.register("mandrake_seeds", id -> new BlockItem(WitcheryBlocks.MANDRAKE.get(), itemProps(id)));
    public static final DeferredItem<Item> MINDRAKE_BULB = ITEMS.register("mindrakebulb", id -> new BlockItem(WitcheryBlocks.MINDRAKE.get(), itemProps(id)));
    public static final DeferredItem<Item> ARTICHOKE = ITEMS.registerSimpleItem("artichoke");
    public static final DeferredItem<Item> ARTICHOKE_SEEDS = ITEMS.register("artichoke_seeds", id -> new ArtichokeSeedItem(itemProps(id)));
    public static final DeferredItem<Item> SNOWBELL_SEEDS = ITEMS.register("snowbell_seeds", id -> new BlockItem(WitcheryBlocks.SNOWBELL.get(), itemProps(id)));
    public static final DeferredItem<Item>      GARLIC         = ITEMS.register("garlic",         id -> new BlockItem(WitcheryBlocks.GARLIC_CROP.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> GARLIC_GARLAND = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GARLIC_GARLAND);
    // Witches' Salt — placed as a thin ground line to hold a Vanishing Witch Hut in place.
    public static final DeferredItem<Item> WITCHES_SALT = ITEMS.register("witches_salt",
            id -> new BlockItem(WitcheryBlocks.SALT_LINE.get(), itemProps(id)));

    // ── M14/M15 crafting outputs ───────────────────────────────────────────────
    public static final DeferredItem<Item> FANCIFUL_THREAD     = ITEMS.registerSimpleItem("fanciful_thread");
    public static final DeferredItem<Item> REDSTONE_SOUP       = ITEMS.registerSimpleItem("redstone_soup");
    public static final DeferredItem<Item> DIAMOND_VAPOUR      = ITEMS.registerSimpleItem("diamond_vapour");
    public static final DeferredItem<Item> BAT_WOOL            = ITEMS.registerSimpleItem("bat_wool");
    public static final DeferredItem<Item> CONCENTRATED_BAT_BALL = ITEMS.registerSimpleItem("concentrated_bat_ball");

    // ── M4 ingredient-only items (not plantable) ───────────────────────────────
    public static final DeferredItem<Item> WOOD_ASH            = ITEMS.registerSimpleItem("wood_ash");
    public static final DeferredItem<Item> ROWAN_BERRIES       = ITEMS.register("rowan_berries", id -> new Item(itemProps(id)
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(6.0F).build())));
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
    public static final DeferredItem<BlockItem> ALDER_PLANKS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_PLANKS);
    public static final DeferredItem<BlockItem> ALDER_STAIRS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_STAIRS);
    public static final DeferredItem<BlockItem> ALDER_SLAB = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_SLAB);
    public static final DeferredItem<Item> ALDER_DOOR = ITEMS.register("alder_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.ALDER_DOOR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> ALDER_LEAVES = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_LEAVES);
    public static final DeferredItem<BlockItem> ALDER_SAPLING = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALDER_SAPLING);
    public static final DeferredItem<BlockItem> HAWTHORN_LOG = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_LOG);
    public static final DeferredItem<BlockItem> HAWTHORN_PLANKS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_PLANKS);
    public static final DeferredItem<BlockItem> HAWTHORN_STAIRS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_STAIRS);
    public static final DeferredItem<BlockItem> HAWTHORN_SLAB = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HAWTHORN_SLAB);
    public static final DeferredItem<Item> HAWTHORN_DOOR = ITEMS.register("hawthorn_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.HAWTHORN_DOOR.get(), itemProps(id)));
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
    public static final DeferredItem<BlockItem> BLIGHT_HEART = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLIGHT_HEART);
    public static final DeferredItem<BlockItem> BLIGHT_FIBRE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLIGHT_FIBRE);
    public static final DeferredItem<BlockItem> BLIGHTED_SOIL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLIGHTED_SOIL);
    public static final DeferredItem<BlockItem> BLIGHTED_STONE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLIGHTED_STONE);
    public static final DeferredItem<BlockItem> BLIGHTED_LOG = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLIGHTED_LOG);
    public static final DeferredItem<BlockItem> LEAPING_LILY = ITEMS.register("leaping_lily", id ->
            new PlaceOnWaterBlockItem(WitcheryBlocks.LEAPING_LILY.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> GRASSPER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GRASSPER);
    public static final DeferredItem<BlockItem> GLOWGLOBE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.GLOWGLOBE);
    public static final DeferredItem<BlockItem> HOLLOW_TEARS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.HOLLOW_TEARS);
    public static final DeferredItem<BlockItem> KETTLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.KETTLE);
    public static final DeferredItem<BlockItem> DISTILLERY = ITEMS.registerSimpleBlockItem(WitcheryBlocks.DISTILLERY);
    public static final DeferredItem<BlockItem> SPINNING_WHEEL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SPINNING_WHEEL);
    public static final DeferredItem<BlockItem> WITCHES_OVEN = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WITCHES_OVEN);
    public static final DeferredItem<BlockItem> ALTAR = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALTAR);
    public static final DeferredItem<BlockItem> FUME_FUNNEL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FUME_FUNNEL);
    public static final DeferredItem<BlockItem> FILTERED_FUME_FUNNEL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FILTERED_FUME_FUNNEL);
    public static final DeferredItem<BlockItem> BRAZIER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BRAZIER);
    public static final DeferredItem<BlockItem> POPPET_SHELF = ITEMS.registerSimpleBlockItem(WitcheryBlocks.POPPET_SHELF);
    public static final DeferredItem<BlockItem> LEECH_CHEST = ITEMS.registerSimpleBlockItem(WitcheryBlocks.LEECH_CHEST);
    public static final DeferredItem<BlockItem> REFILLING_CHEST = ITEMS.registerSimpleBlockItem(WitcheryBlocks.REFILLING_CHEST);
    public static final DeferredItem<BlockItem> CANDELABRA = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CANDELABRA);
    public static final DeferredItem<BlockItem> CHALICE = ITEMS.register("chalice", id ->
        new BlockItem(WitcheryBlocks.CHALICE.get(), itemProps(id)) {
            @Override
            public net.minecraft.network.chat.Component getName(ItemStack stack) {
                net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                return tag.getBooleanOr("Filled", false)
                    ? net.minecraft.network.chat.Component.translatable("block.witchery.chalice.filled")
                    : super.getName(stack);
            }
        });
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
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> LIVING_FLAME_CONTRACT =
            ITEMS.register("living_flame_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> FIERY_TOLERANCE_CONTRACT =
            ITEMS.register("fiery_tolerance_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> EVAPORATION_CONTRACT =
            ITEMS.register("evaporation_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> FIERY_TOUCH_CONTRACT =
            ITEMS.register("fiery_touch_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<com.emoniph.witchery.item.DemonicContractItem> MELTING_TOUCH_CONTRACT =
            ITEMS.register("melting_touch_contract", id -> new com.emoniph.witchery.item.DemonicContractItem(itemProps(id).stacksTo(16)));

    // ── Lord of Torment drops ──────────────────────────────────────────────────
    public static final DeferredItem<Item> SOUL_OF_TORMENT  = ITEMS.registerSimpleItem("soul_of_torment");
    public static final DeferredItem<Item> HUNGER_DEMON_SOUL = ITEMS.registerSimpleItem("hunger_demon_soul");
    public static final DeferredItem<Item> ANGUISH_DEMON_SOUL = ITEMS.registerSimpleItem("anguish_demon_soul");
    public static final DeferredItem<Item> FEAR_DEMON_SOUL = ITEMS.registerSimpleItem("fear_demon_soul");
    public static final DeferredItem<Item> TORMENT_DEMON_SOUL = ITEMS.registerSimpleItem("torment_demon_soul");
    public static final DeferredItem<Item> TORMENT_SCROLL =
            ITEMS.register("torment_scroll", id -> new TormentContractItem(itemProps(id).stacksTo(16)));

    public static final DeferredItem<BlockItem> ALLURING_SKULL = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ALLURING_SKULL);
    public static final DeferredItem<BlockItem> DEMON_HEART = ITEMS.registerSimpleBlockItem(WitcheryBlocks.DEMON_HEART);
    public static final DeferredItem<BlockItem> COFFIN               = ITEMS.registerSimpleBlockItem(WitcheryBlocks.COFFIN);
    public static final DeferredItem<BlockItem> BLOOD_CRUCIBLE       = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOOD_CRUCIBLE);
    public static final DeferredItem<BlockItem> MIRROR_WALL          = ITEMS.registerSimpleBlockItem(WitcheryBlocks.MIRROR_WALL);
    public static final DeferredItem<BlockItem> MIRROR               = ITEMS.register("mirror", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.MIRROR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> PIT_DIRT             = ITEMS.registerSimpleBlockItem(WitcheryBlocks.PIT_DIRT);
    public static final DeferredItem<BlockItem> PIT_GRASS            = ITEMS.registerSimpleBlockItem(WitcheryBlocks.PIT_GRASS);
    public static final DeferredItem<BlockItem> SHADED_GLASS         = ITEMS.register("shaded_glass",
            id -> new com.emoniph.witchery.item.ShadedGlassBlockItem(WitcheryBlocks.SHADED_GLASS.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> INFINITY_EGG         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.INFINITY_EGG);
    public static final DeferredItem<BlockItem> ROWAN_PLANKS         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_PLANKS);
    public static final DeferredItem<BlockItem> BLOODED_WOOL         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOODED_WOOL);
    public static final DeferredItem<BlockItem> BLOOD_STAINED_WOOL   = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOOD_STAINED_WOOL);
    public static final DeferredItem<BlockItem> ROWAN_STAIRS         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_STAIRS);
    public static final DeferredItem<BlockItem> ROWAN_SLAB           = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ROWAN_SLAB);
    public static final DeferredItem<Item>      WITCHES_PRIMER       = ITEMS.register("witches_primer", id -> new com.emoniph.witchery.item.WitchesPrimerItem(itemProps(id).stacksTo(1)));
    // Resurrected duplicate guide-book ids removed 2026-06-04 (kept the source-faithful book_* ids
    // below; their recipes/bookshop loot were migrated onto the book_* / cauldron_book ids).
    // Each guide book opens the Witches' Primer at its category (and gates that category until crafted).
    public static final DeferredItem<Item>      BOOK_HERBOLOGY       = ITEMS.register("book_herbology", id -> primerGuide(id, "herbology", "herbology/rowan_tree"));
    public static final DeferredItem<Item>      BOOK_INFUSIONS       = ITEMS.register("book_infusions", id -> primerGuide(id, "brews_infusions", "brews_infusions/kettle"));
    public static final DeferredItem<Item>      BOOK_OVEN            = ITEMS.register("book_oven", id -> primerGuide(id, "collecting_fumes", "collecting_fumes/witches_oven"));
    public static final DeferredItem<Item>      BOOK_DISTILLING      = ITEMS.register("book_distilling", id -> primerGuide(id, "distilling", "distilling/distillery"));
    public static final DeferredItem<Item>      BOOK_CIRCLE_MAGIC    = ITEMS.register("book_circle_magic", id -> primerGuide(id, "circle_magic", "circle_magic/chalk"));
    public static final DeferredItem<Item>      BOOK_BURNING         = ITEMS.register("book_burning", id -> primerGuide(id, "conjuration", "conjuration/brazier"));
    public static final DeferredItem<Item>      BOOK_WANDS           = ITEMS.register("book_wands", id -> primerGuide(id, "symbology", "symbology/mystic_branch"));
    // New guide books invented for topics the original mod had no book for.
    public static final DeferredItem<Item>      BOOK_POPPETS         = ITEMS.register("book_poppets", id -> primerGuide(id, "poppets", "poppets/taglock_kit"));
    public static final DeferredItem<Item>      BOOK_SPIRIT_WORLD    = ITEMS.register("book_spirit_world", id -> primerGuide(id, "spirit_world", "spirit_world/spirit_world"));
    public static final DeferredItem<Item>      BOOK_CREATURES       = ITEMS.register("book_creatures", id -> primerGuide(id, "creatures", "creatures/familiars"));
    public static final DeferredItem<Item>      BOOK_TRANSFORMATIONS = ITEMS.register("book_transformations", id -> primerGuide(id, "transformations", "transformations/vampirism"));
    public static final DeferredItem<Item>      CAULDRON_BOOK        = ITEMS.register("cauldron_book", id -> new com.emoniph.witchery.item.WitchesPrimerItem(itemProps(id).stacksTo(1), "item.witchery.cauldron_book.tip", com.emoniph.witchery.book.WitcheryBooks.CAULDRON, com.emoniph.witchery.book.WitcheryBooks.id("brewing"), com.emoniph.witchery.book.WitcheryBooks.id("brewing/the_cauldron")));
    public static final DeferredItem<Item>      BOOK_BIOMES          = ITEMS.register("book_biomes", id -> new com.emoniph.witchery.item.BiomeBookItem(itemProps(id)));
    public static final DeferredItem<Item>      EXTENDED_BIOMES_BOOK = ITEMS.register("extended_biomes_book", id -> new com.emoniph.witchery.item.BiomeBookItem(itemProps(id)));
    public static final DeferredItem<Item>      BIOMES_BOOK          = ITEMS.register("biomes_book", id -> new com.emoniph.witchery.item.BiomeBookItem(itemProps(id)));
    public static final DeferredItem<Item>      ROWAN_DOOR_KEY       = ITEMS.register("rowan_door_key", id -> new com.emoniph.witchery.item.RowanDoorKeyItem(itemProps(id)));
    public static final DeferredItem<Item>      ROWAN_KEYRING        = ITEMS.register("rowan_keyring", id -> new com.emoniph.witchery.item.RowanKeyringItem(itemProps(id)));
    public static final DeferredItem<Item>      ROWAN_DOOR           = ITEMS.register("rowan_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.ROWAN_DOOR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> PERPETUAL_ICE        = ITEMS.registerSimpleBlockItem(WitcheryBlocks.PERPETUAL_ICE);
    public static final DeferredItem<BlockItem> ICE_STAIRS           = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_STAIRS);
    public static final DeferredItem<BlockItem> ICE_SLAB             = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_SLAB);
    public static final DeferredItem<BlockItem> ICE_DOOR             = ITEMS.register("ice_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.ICE_DOOR.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> ICE_FENCE            = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_FENCE);
    public static final DeferredItem<BlockItem> ICE_GATE             = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_GATE);
    public static final DeferredItem<BlockItem> STOCKADE             = ITEMS.registerSimpleBlockItem(WitcheryBlocks.STOCKADE);
    public static final DeferredItem<BlockItem> ICE_STOCKADE         = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_STOCKADE);
    public static final DeferredItem<BlockItem> ICE_PRESSURE_PLATE   = ITEMS.registerSimpleBlockItem(WitcheryBlocks.ICE_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> SNOW_PRESSURE_PLATE  = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SNOW_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> SNOW_STAIRS          = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SNOW_STAIRS);
    public static final DeferredItem<BlockItem> SNOW_SLAB            = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SNOW_SLAB);
    public static final DeferredItem<BlockItem> FETISH_SCARECROW    = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_SCARECROW);
    public static final DeferredItem<BlockItem> FETISH_TRENT_EFFIGY  = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_TRENT_EFFIGY);
    public static final DeferredItem<BlockItem> FETISH_WITCHS_LADDER = ITEMS.registerSimpleBlockItem(WitcheryBlocks.FETISH_WITCHS_LADDER);
    public static final DeferredItem<BlockItem> STATUE_GODDESS = ITEMS.registerSimpleBlockItem(WitcheryBlocks.STATUE_GODDESS);
    public static final DeferredItem<BlockItem> WICKER_BUNDLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WICKER_BUNDLE);
    public static final DeferredItem<BlockItem> BLOODIED_WICKER_BUNDLE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BLOODIED_WICKER_BUNDLE);
    public static final DeferredItem<BlockItem> CRITTER_SNARE = ITEMS.registerSimpleBlockItem(WitcheryBlocks.CRITTER_SNARE);
    public static final DeferredItem<BlockItem> BEARTRAP = ITEMS.registerSimpleBlockItem(WitcheryBlocks.BEARTRAP);
    public static final DeferredItem<BlockItem> WOLFTRAP = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WOLFTRAP);
    public static final DeferredItem<BlockItem> PLANT_MINE = ITEMS.register("plant_mine",
            id -> new PlantMineBlockItem(WitcheryBlocks.PLANT_MINE.get(), itemProps(id)));
    public static final DeferredItem<BlockItem> WITCH_DOOR = ITEMS.register("witch_door", id -> new net.minecraft.world.item.DoubleHighBlockItem(WitcheryBlocks.WITCH_DOOR.get(), itemProps(id)));
    public static final DeferredItem<Item> COTTON = ITEMS.register("cotton", id -> new BlockItem(WitcheryBlocks.COTTON_CROP.get(), itemProps(id)));

    // Pure items
    public static final DeferredItem<Item> ARTHANA = ITEMS.register("arthana", id -> new com.emoniph.witchery.item.ArthanaItem(itemProps(id).durability(250)));
    public static final DeferredItem<Item> PENTACLE = ITEMS.register("pentacle", id -> new com.emoniph.witchery.item.PentacleItem(itemProps(id)));
    public static final DeferredItem<BlockItem> STATUE_HOBGOBLIN_PATRON = ITEMS.register("statue_hobgoblin_patron",
            id -> new StatueHobgoblinPatronItem(WitcheryBlocks.STATUE_HOBGOBLIN_PATRON.get(), itemProps(id)));
    public static final DeferredItem<Item> WAYSTONE = ITEMS.register("waystone", id -> new com.emoniph.witchery.item.WaystoneItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> BOUND_WAYSTONE = ITEMS.register("bound_waystone", id -> new com.emoniph.witchery.item.WaystoneItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<Item> BLOODED_WAYSTONE = ITEMS.register("blooded_waystone", id -> new com.emoniph.witchery.item.WaystoneItem(itemProps(id).stacksTo(1)));
    public static final DeferredItem<BlockItem> WOLF_ALTAR = ITEMS.registerSimpleBlockItem(WitcheryBlocks.WOLF_ALTAR);
    public static final DeferredItem<Item> BOLINE = ITEMS.register("boline", id ->
            new com.emoniph.witchery.item.BolineItem(itemProps(id)
                    .sword(net.minecraft.world.item.ToolMaterial.IRON, 2.0f, -2.4f)));
    public static final DeferredItem<Item> CHALK = ITEMS.register("chalk", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_GLYPH.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_GOLDEN = ITEMS.register("chalk_golden", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_OTHERWHERE = ITEMS.register("chalk_otherwhere", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_OTHERWHERE.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> CHALK_INFERNAL = ITEMS.register("chalk_infernal", id -> new com.emoniph.witchery.item.ChalkItem(() -> WitcheryBlocks.CIRCLE_INFERNAL.get(), itemProps(id).durability(64)));
    public static final DeferredItem<Item> MYSTIC_BRANCH = ITEMS.register("mystic_branch", id -> new MysticBranchItem(itemProps(id)));
    public static final DeferredItem<Item> DEBUG_DEATH_WARD =
            ITEMS.register("debug_death_ward", id -> new DebugDeathWardItem(itemProps(id)));
    public static final DeferredItem<Item> DEBUG_INSPECTOR =
            ITEMS.register("debug_inspector", id -> new com.emoniph.witchery.item.DebugInspectorItem(itemProps(id)));
    public static final DeferredItem<Item> TAGLOCK_KIT = ITEMS.register("taglock_kit", id -> new com.emoniph.witchery.item.TaglockKitItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> BOUND_TAGLOCK_KIT = ITEMS.register("bound_taglock_kit", id -> new com.emoniph.witchery.item.TaglockKitItem(itemProps(id).stacksTo(1)));

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
    public static final DeferredItem<Item> BREW_BAG = ITEMS.register("brew_bag",
            id -> new BrewBagItem(itemProps(id)));
    public static final DeferredItem<Item> WATER_DIVINER = ITEMS.register("water_diviner", id -> new com.emoniph.witchery.item.DivinerItem(net.minecraft.world.level.block.Blocks.WATER, itemProps(id).durability(50)));
    public static final DeferredItem<Item> LAVA_DIVINER = ITEMS.register("lava_diviner", id -> new com.emoniph.witchery.item.DivinerItem(net.minecraft.world.level.block.Blocks.LAVA, itemProps(id).durability(50)));
    public static final DeferredItem<Item> WOODEN_SCANNER = ITEMS.register("wooden_scanner",
            id -> new WoodenScannerItem(itemProps(id).durability(128)));
    public static final DeferredItem<Item> CIRCLE_TALISMAN = ITEMS.register("circle_talisman", id -> new com.emoniph.witchery.item.CircleTalismanItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> WITCH_HAND = ITEMS.register("witch_hand",
            id -> new WitchHandItem(itemProps(id)));
    public static final DeferredItem<Item> WITCHES_HAND = ITEMS.register("witches_hand",
            id -> new WitchHandItem(itemProps(id)));
    public static final DeferredItem<Item> WORMWOOD_SEEDS = ITEMS.register("wormwood_seeds", id -> new BlockItem(WitcheryBlocks.WORMWOOD_PLANT.get(), itemProps(id)));
    public static final DeferredItem<Item> MUTATOR = ITEMS.register("mutator", id -> new MutatingSprigItem(itemProps(id).durability(16)));
    public static final DeferredItem<Item> SPECTRAL_STONE = ITEMS.register("spectral_stone", id -> new SpectralStoneItem(itemProps(id)));
    public static final DeferredItem<Item> POPPET_SHELF_COMPASS = ITEMS.register("poppet_shelf_compass",
            id -> new PoppetShelfCompassItem(itemProps(id)));
    public static final DeferredItem<Item> POLYNESIA_CHARM = ITEMS.register("polynesia_charm",
            id -> new PolynesiaCharmItem(itemProps(id)));
    public static final DeferredItem<Item> DEVILS_TONGUE_CHARM = ITEMS.register("devils_tongue_charm",
            id -> new PolynesiaCharmItem(itemProps(id), true));
    public static final DeferredItem<Item> FANCIFUL_THINKING_CHARM = ITEMS.registerSimpleItem("fanciful_thinking_charm");
    public static final DeferredItem<Item> CHARM_OF_DISRUPTED_DREAMS = ITEMS.registerSimpleItem("charm_of_disrupted_dreams");
    public static final DeferredItem<Item> MOON_CHARM = ITEMS.register("moon_charm",
            id -> new MoonCharmItem(itemProps(id)));
    public static final DeferredItem<Item> PARASYTIC_LOUSE = ITEMS.register("parasytic_louse", id -> new ParasyticLouseItem(itemProps(id)));
    public static final DeferredItem<Item> LEAPING_LILY_ITEM = ITEMS.registerSimpleItem("leaping_lily_item");
    public static final DeferredItem<Item> BUCKET_SPIRIT = ITEMS.register("bucket_spirit",
            id -> new SpiritBucketItem(itemProps(id)));
    public static final DeferredItem<Item> BUCKET_HOLLOW_TEARS = ITEMS.register("bucket_hollow_tears",
            id -> new SpiritBucketItem(itemProps(id), WitcheryBlocks.HOLLOW_TEARS));
    public static final DeferredItem<Item> HAND_BOW = ITEMS.register("hand_bow",
            id -> new HandBowItem(itemProps(id)));
    public static final DeferredItem<Item> CROSSBOW_PISTOL = ITEMS.register("crossbow_pistol",
            id -> new HandBowItem(itemProps(id)));
    public static final DeferredItem<Item> CANE_SWORD = ITEMS.register("cane_sword",
            id -> new CaneSwordItem(itemProps(id), true));
    public static final DeferredItem<Item> DRAWN_CANE_SWORD = ITEMS.register("drawn_cane_sword",
            id -> new CaneSwordItem(itemProps(id), false));
    public static final DeferredItem<Item> HUNTSMAN_SPEAR = ITEMS.register("huntsman_spear",
            id -> new HuntsmanSpearItem(itemProps(id)));
    public static final DeferredItem<Item> HORN_OF_THE_HUNT = ITEMS.register("horn_of_the_hunt",
            id -> new HornOfTheHuntItem(itemProps(id)));

    // Armour
    public static final DeferredItem<Item> WITCHES_HAT   = ITEMS.register("witches_hat",
            id -> new Item(itemProps(id).humanoidArmor(WITCHES_ROBES_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.HELMET)));
    public static final DeferredItem<Item> WITCHES_ROBES = ITEMS.register("witches_robes",
            id -> new WitchesRobeItem(itemProps(id)
                    .humanoidArmor(WITCHES_ROBES_MATERIAL,
                            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> EARMUFFS = ITEMS.register("earmuffs", WitcheryItems::earmuffs);
    public static final DeferredItem<Item> BITING_BELT = ITEMS.register("biting_belt", id ->
            new BitingBeltItem(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> BARK_BELT = ITEMS.register("bark_belt", id ->
            new BarkBeltItem(itemProps(id).humanoidArmor(net.minecraft.world.item.equipment.ArmorMaterials.LEATHER,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> WOVEN_CRUOR = ITEMS.registerSimpleItem("woven_cruor");
    public static final DeferredItem<Item> VAMPIRE_TOP_HAT = ITEMS.register("vampire_top_hat", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_VEIL_HAT = ITEMS.register("vampire_veil_hat", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_DRESS_COAT = ITEMS.register("vampire_dress_coat", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_DRESS_JACKET = ITEMS.register("vampire_dress_jacket", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_COAT = ITEMS.register("vampire_coat", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_CORSET = ITEMS.register("vampire_corset", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_TROUSERS = ITEMS.register("vampire_trousers", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SKIRTED_TROUSERS = ITEMS.register("vampire_skirted_trousers", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SKIRT = ITEMS.register("vampire_skirt", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_OXFORD_BOOTS = ITEMS.register("vampire_oxford_boots", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_SHOES = ITEMS.register("vampire_shoes", id ->
            vampireArmor(id, VAMPIRE_MASCULINE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_HEELS = ITEMS.register("vampire_heels", id ->
            vampireArmor(id, VAMPIRE_FEMININE_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.LIGHT));
    public static final DeferredItem<Item> VAMPIRE_HELMET = ITEMS.register("vampire_helmet", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.HELMET, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHAIN_COAT = ITEMS.register("vampire_chain_coat", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHAIN_COAT_LADIES = ITEMS.register("vampire_chain_coat_ladies", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_CHESTPLATE = ITEMS.register("vampire_chestplate", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_LEGGINGS = ITEMS.register("vampire_leggings", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> VAMPIRE_BOOTS = ITEMS.register("vampire_boots", id ->
            vampireArmor(id, VAMPIRE_METALLIC_MATERIAL,
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, VampireClothingItem.SetType.MEDIUM));
    public static final DeferredItem<Item> HUNTER_HAT        = ITEMS.register("hunter_hat",        id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.HELMET, HunterClothingItem.Variant.PLAIN));
    public static final DeferredItem<Item> HUNTER_COAT       = ITEMS.register("hunter_coat",       id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, HunterClothingItem.Variant.PLAIN));
    public static final DeferredItem<Item> HUNTER_LEGGINGS   = ITEMS.register("hunter_leggings",   id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.LEGGINGS, HunterClothingItem.Variant.PLAIN));
    public static final DeferredItem<Item> HUNTER_BOOTS      = ITEMS.register("hunter_boots",      id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.BOOTS, HunterClothingItem.Variant.PLAIN));
    public static final DeferredItem<Item> DEATHS_HOOD = ITEMS.register("deaths_hood", id ->
            deathsArmor(id, net.minecraft.world.item.equipment.ArmorType.HELMET, DeathsClothingItem.Piece.HOOD));
    public static final DeferredItem<Item> DEATHS_ROBE = ITEMS.register("deaths_robe", id ->
            deathsArmor(id, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, DeathsClothingItem.Piece.ROBE));
    public static final DeferredItem<Item> DEATHS_SHOES = ITEMS.register("deaths_shoes", id ->
            deathsArmor(id, net.minecraft.world.item.equipment.ArmorType.BOOTS, DeathsClothingItem.Piece.SHOES));
    public static final DeferredItem<Item> DEATHS_HAND = ITEMS.register("deaths_hand", id ->
            new DeathsHandItem(itemProps(id).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<Item> DEPLOYED_DEATHS_HAND = ITEMS.register("deployed_deaths_hand", id ->
            new DeathsHandItem(itemProps(id).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), true));

    // Brew items — all now have implemented effects
    public static final DeferredItem<Item> BREW_OF_RAISING        = ITEMS.register("brew_of_raising", id -> new BrewOfRaisingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SLEEPING       = ITEMS.register("brew_of_sleeping", id -> new BrewOfSleepingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FLOWING_SPIRIT = ITEMS.register("brew_of_flowing_spirit", id -> new BrewOfFlowingSpiritItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_WASTING        = ITEMS.register("brew_of_wasting", id -> new BrewOfWastingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_EROSION        = ITEMS.register("brew_of_erosion", id -> new BrewOfErosionItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FOULNESS       = ITEMS.register("brew_of_foulness", id -> new BrewOfFoulnessItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_INFECTION      = ITEMS.register("brew_of_infection", id -> new BrewOfInfectionItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_BLIGHT         = ITEMS.register("brew_of_blight", id -> new BrewOfBlightItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_CLEANSING      = ITEMS.register("brew_of_cleansing", id -> new BrewOfCleansingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_LOVE           = ITEMS.register("brew_of_love", id -> new BrewOfLoveItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_THE_GROTESQUE  = ITEMS.register("brew_of_the_grotesque", id -> new BrewOfTheGrotesqueItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_REVEALING      = ITEMS.register("brew_of_revealing", id -> new BrewOfRevealingItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_ENDLESS_WATER  = ITEMS.register("brew_of_endless_water", id -> new BrewOfEndlessWaterItem(itemProps(id)));
    public static final DeferredItem<Item> COMBUSTION_BREW        = ITEMS.register("combustion_brew", id -> new CombustionBrewItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_POISON         = ITEMS.register("brew_of_poison", id -> new BrewOfPoisonItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_FOAMING        = ITEMS.register("brew_of_foaming", id -> new BrewOfFoamingItem(itemProps(id)));

    // ── Custom brews — splash (thrown) dispersal tier ─────────────────────────
    public static final DeferredItem<Item> BREW_OF_FOULNESS_SPLASH = ITEMS.register("brew_of_foulness_splash", id -> new ThrownBrewItem(itemProps(id), BrewType.FOULNESS, false));
    public static final DeferredItem<Item> BREW_OF_FOAMING_SPLASH  = ITEMS.register("brew_of_foaming_splash",  id -> new ThrownBrewItem(itemProps(id), BrewType.FOAMING,  false));
    public static final DeferredItem<Item> BREW_OF_POISON_SPLASH   = ITEMS.register("brew_of_poison_splash",   id -> new ThrownBrewItem(itemProps(id), BrewType.POISON,   false));

    // ── Custom brews — gas (lingering) dispersal tier ─────────────────────────
    public static final DeferredItem<Item> BREW_OF_FOULNESS_GAS    = ITEMS.register("brew_of_foulness_gas",    id -> new ThrownBrewItem(itemProps(id), BrewType.FOULNESS, true));
    public static final DeferredItem<Item> BREW_OF_FOAMING_GAS     = ITEMS.register("brew_of_foaming_gas",     id -> new ThrownBrewItem(itemProps(id), BrewType.FOAMING,  true));
    public static final DeferredItem<Item> BREW_OF_POISON_GAS      = ITEMS.register("brew_of_poison_gas",      id -> new ThrownBrewItem(itemProps(id), BrewType.POISON,   true));

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
    public static final DeferredItem<Item> BREW_OF_SOLID_EROSION  = ITEMS.register("brew_of_solid_erosion", id -> new com.emoniph.witchery.item.BrewOfSolidifyingItem.Erosion(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SOARING        = ITEMS.register("brew_of_soaring", id -> new com.emoniph.witchery.item.BrewOfSoaringItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_GRAVE          = ITEMS.register("brew_of_grave", id -> new com.emoniph.witchery.item.GenericBrewItem(itemProps(id)));
    public static final DeferredItem<Item> BREW_OF_SUBSTITUTION   = ITEMS.register("brew_of_substitution", id -> new com.emoniph.witchery.item.BrewOfSubstitutionItem(itemProps(id)));
    public static final DeferredItem<Item> INFUSED_SOARING_BREW   = ITEMS.register("infused_soaring_brew", id -> new com.emoniph.witchery.item.BrewOfSoaringItem(itemProps(id)));
    public static final DeferredItem<Item> INFUSED_GRAVE_BREW     = ITEMS.register("infused_grave_brew", id -> new com.emoniph.witchery.item.GenericBrewItem(itemProps(id)));

    // ── Infusion brew bases (M19) ─────────────────────────────────────────────
    public static final DeferredItem<Item> GHOST_OF_THE_LIGHT   = ITEMS.register("ghost_of_the_light",
            id -> new InfusionSourceItem(itemProps(id), "witchery.infusion.light.name"));
    public static final DeferredItem<Item> SOUL_OF_THE_WORLD    = ITEMS.register("soul_of_the_world",
            id -> new InfusionSourceItem(itemProps(id), "witchery.infusion.overworld.name"));
    public static final DeferredItem<Item> SPIRIT_OF_OTHERWHERE = ITEMS.register("spirit_of_otherwhere",
            id -> new InfusionSourceItem(itemProps(id), "witchery.infusion.otherwhere.name"));
    public static final DeferredItem<Item> INFERNAL_ANIMUS      = ITEMS.register("infernal_animus",
            id -> new InfusionSourceItem(itemProps(id), "witchery.infusion.infernal.name"));
    public static final DeferredItem<Item> MYSTIC_UNGUENT       = ITEMS.registerSimpleItem("mystic_unguent");
    public static final DeferredItem<Item> FLYING_OINTMENT      = ITEMS.registerSimpleItem("flying_ointment");
    public static final DeferredItem<Item> HAPPENSTANCE_OIL     = ITEMS.registerSimpleItem("happenstance_oil");
    public static final DeferredItem<Item> INFUSED_BREW_BASE    = ITEMS.registerSimpleItem("infused_brew_base");

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
    public static final DeferredItem<Item> FUME_FILTER        = ITEMS.registerSimpleItem("fume_filter");
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
    public static final DeferredItem<Item> NULLIFIED_LEATHER  = ITEMS.registerSimpleItem("nullified_leather");
    public static final DeferredItem<Item> ODOUR_OF_PURITY    = ITEMS.registerSimpleItem("odour_of_purity");
    public static final DeferredItem<Item> BLIGHT_ICHOR       = ITEMS.registerSimpleItem("blight_ichor");
    public static final DeferredItem<Item> OIL_OF_VITRIOL     = ITEMS.registerSimpleItem("oil_of_vitriol");
    public static final DeferredItem<Item> OWLETS_WING        = ITEMS.registerSimpleItem("owlets_wing");
    public static final DeferredItem<Item> PURIFIED_MILK      = ITEMS.registerSimpleItem("purified_milk");
    public static final DeferredItem<Item> QUICKLIME          = ITEMS.registerSimpleItem("quicklime");
    public static final DeferredItem<Item> REEK_OF_MISFORTUNE = ITEMS.registerSimpleItem("reek_of_misfortune");
    public static final DeferredItem<Item> REFINED_EVIL       = ITEMS.registerSimpleItem("refined_evil");
    public static final DeferredItem<Item> SPECTRAL_DUST      = ITEMS.registerSimpleItem("spectral_dust");
    public static final DeferredItem<Item> SUBDUED_SPIRIT     = ITEMS.register("subdued_spirit", id -> new SubduedSpiritItem(itemProps(id)));
    public static final DeferredItem<Item> SUBDUED_VILLAGE_SPIRIT = ITEMS.register("subdued_village_spirit", id -> new SubduedSpiritItem(itemProps(id), true));
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
    public static final DeferredItem<Item> QUARTZ_SPHERE          = ITEMS.registerSimpleItem("quartz_sphere");
    public static final DeferredItem<Item> CHARGED_QUARTZ_SPHERE  = ITEMS.register("charged_quartz_sphere",
            id -> new Item(itemProps(id)) {
                @Override
                public boolean isFoil(ItemStack stack) { return true; }
            });
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SUN_COLLECTOR = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SUN_COLLECTOR);
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
    public static final DeferredItem<Item> WOLFSBANE_SEEDS    = ITEMS.register("wolfsbane_seeds", id -> new BlockItem(WitcheryBlocks.WOLFSBANE_CROP.get(), itemProps(id)));
    public static final DeferredItem<Item> SILVER_INGOT        = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> SILVER_DEPOSITS     = ITEMS.registerSimpleItem("silver_deposits");
    public static final DeferredItem<Item> GOLDEN_HEART        = ITEMS.registerSimpleItem("golden_heart");
    public static final DeferredItem<Item> DANDELION_OF_INK = ITEMS.registerSimpleItem("dandelion_of_ink");
    public static final DeferredItem<Item> DANDELION_OF_SPROUTING = ITEMS.registerSimpleItem("dandelion_of_sprouting");
    public static final DeferredItem<Item> DANDELION_OF_THORNS = ITEMS.registerSimpleItem("dandelion_of_thorns");
    public static final DeferredItem<Item> DANDELION_OF_WEBS = ITEMS.registerSimpleItem("dandelion_of_webs");
    public static final DeferredItem<Item> POPPY_OF_INK = ITEMS.registerSimpleItem("poppy_of_ink");
    public static final DeferredItem<Item> POPPY_OF_SPROUTING = ITEMS.registerSimpleItem("poppy_of_sprouting");
    public static final DeferredItem<Item> POPPY_OF_THORNS = ITEMS.registerSimpleItem("poppy_of_thorns");
    public static final DeferredItem<Item> POPPY_OF_WEBS = ITEMS.registerSimpleItem("poppy_of_webs");
    public static final DeferredItem<Item> SHRUB_OF_INK = ITEMS.registerSimpleItem("shrub_of_ink");
    public static final DeferredItem<Item> SHRUB_OF_SPROUTING = ITEMS.registerSimpleItem("shrub_of_sprouting");
    public static final DeferredItem<Item> SHRUB_OF_THORNS = ITEMS.registerSimpleItem("shrub_of_thorns");
    public static final DeferredItem<Item> SHRUB_OF_WEBS = ITEMS.registerSimpleItem("shrub_of_webs");
    public static final DeferredItem<Item> SILVER_SWORD        = ITEMS.register("silver_sword", id -> new Item(itemProps(id).sword(net.minecraft.world.item.ToolMaterial.GOLD, 3.0f, -2.4f)));
    public static final DeferredItem<Item> SILVER_BOLT         = ITEMS.registerSimpleItem("silver_bolt");
    public static final DeferredItem<Item> WOODEN_BOLT         = ITEMS.registerSimpleItem("wooden_bolt");
    public static final DeferredItem<Item> BONE_BOLT           = ITEMS.registerSimpleItem("bone_bolt");
    public static final DeferredItem<Item> NULLIFYING_BOLT     = ITEMS.registerSimpleItem("nullifying_bolt");
    public static final DeferredItem<Item> SPLITTING_BOLT      = ITEMS.registerSimpleItem("splitting_bolt");
    public static final DeferredItem<Item> BOLT_STAKE          = ITEMS.registerSimpleItem("bolt_stake");
    public static final DeferredItem<Item> BOLT_ANTI_MAGIC     = ITEMS.registerSimpleItem("bolt_anti_magic");
    public static final DeferredItem<Item> BOLT_HOLY           = ITEMS.registerSimpleItem("bolt_holy");
    public static final DeferredItem<Item> STAKE               = ITEMS.register("stake", id -> new com.emoniph.witchery.item.StakeItem(itemProps(id)));
    public static final DeferredItem<BlockItem> SILVER_VAT     = ITEMS.registerSimpleBlockItem(WitcheryBlocks.SILVER_VAT);
    public static final DeferredItem<Item> SILVERED_HUNTER_HAT        = ITEMS.register("silvered_hunter_hat", id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.HELMET, HunterClothingItem.Variant.SILVERED));
    public static final DeferredItem<Item> SILVERED_HUNTER_COAT       = ITEMS.register("silvered_hunter_coat", id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, HunterClothingItem.Variant.SILVERED));
    public static final DeferredItem<Item> SILVERED_HUNTER_LEGGINGS   = ITEMS.register("silvered_hunter_leggings", id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.LEGGINGS, HunterClothingItem.Variant.SILVERED));
    public static final DeferredItem<Item> SILVERED_HUNTER_BOOTS      = ITEMS.register("silvered_hunter_boots", id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.BOOTS, HunterClothingItem.Variant.SILVERED));
    public static final DeferredItem<Item> DENSE_WEB          = ITEMS.registerSimpleItem("dense_web");
    public static final DeferredItem<Item> INFERNAL_BLOOD     = ITEMS.register("infernal_blood", id -> new DemonicBloodItem(itemProps(id)));
    public static final DeferredItem<Item> GLASS_GOBLET       = ITEMS.register("glass_goblet", id -> new com.emoniph.witchery.item.GlassGobletItem(itemProps(id), false));
    public static final DeferredItem<Item> FULL_GLASS_GOBLET  = ITEMS.register("full_glass_goblet", id -> new com.emoniph.witchery.item.GlassGobletItem(itemProps(id), true));
    public static final DeferredItem<Item> TORN_PAGE          = ITEMS.registerSimpleItem("torn_page");
    // immortals_observations_book duplicate removed 2026-06-04; vampire_book (old `vbook`) is the
    // single source-faithful "Observations of an Immortal" item.
    public static final DeferredItem<Item> VAMPIRE_BOOK = ITEMS.register("vampire_book",
            id -> new com.emoniph.witchery.item.ImmortalsObservationsBookItem(itemProps(id).stacksTo(1),
                    "item.witchery.vampire_book.tip"));
    public static final DeferredItem<Item> WARM_BLOOD = ITEMS.register("warm_blood",
            id -> new com.emoniph.witchery.item.VampireBloodItem(com.emoniph.witchery.item.VampireBloodItem.Kind.WARM,
                    itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> LILITHS_BLOOD = ITEMS.register("liliths_blood",
            id -> new com.emoniph.witchery.item.VampireBloodItem(com.emoniph.witchery.item.VampireBloodItem.Kind.LILITH,
                    itemProps(id).stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> CONGEALED_SPIRIT   = ITEMS.registerSimpleItem("congealed_spirit");
    public static final DeferredItem<Item> LEONARDS_URN       = ITEMS.register("leonards_urn",
            id -> new LeonardsUrnItem(itemProps(id).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<Item> GULGS_GIRDLE       = ITEMS.register("gulgs_girdle",
            id -> new GoblinClothesItem(GoblinClothesItem.Piece.GULGS_GIRDLE, itemProps(id)
                    .humanoidArmor(GULGS_GIRDLE_MATERIAL, net.minecraft.world.item.equipment.ArmorType.LEGGINGS)
                    .durability(net.minecraft.world.item.equipment.ArmorType.LEGGINGS.getDurability(
                            net.minecraft.world.item.equipment.ArmorMaterials.IRON.durability()))
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<Item> MOGS_QUIVER        = ITEMS.register("mogs_quiver",
            id -> new GoblinClothesItem(GoblinClothesItem.Piece.MOGS_QUIVER, itemProps(id)
                    .humanoidArmor(MOGS_QUIVER_MATERIAL, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
                    .durability(net.minecraft.world.item.equipment.ArmorType.CHESTPLATE.getDurability(
                            net.minecraft.world.item.equipment.ArmorMaterials.IRON.durability()))
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<Item> TWISTING_BAND      = ITEMS.register("twisting_band",
            id -> new TwistingBandItem(itemProps(id)
                    .humanoidArmor(TWISTING_BAND_MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET)
                    .durability(net.minecraft.world.item.equipment.ArmorType.HELMET.getDurability(
                            net.minecraft.world.item.equipment.ArmorMaterials.DIAMOND.durability()))
                    .rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> CREATIVE_BAT_WOLF_TOKEN = ITEMS.register("creative_bat_wolf_token",
            id -> new CreativeBatWolfTokenItem(itemProps(id)));
    public static final DeferredItem<Item> CREATIVE_MEDALLION = ITEMS.register("creative_medallion",
            id -> new CreativeBatWolfTokenItem(itemProps(id)));
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

    public static final DeferredItem<Item> ANOINTING_PASTE    = ITEMS.register("anointing_paste", id -> new com.emoniph.witchery.item.AnointingPasteItem(itemProps(id)));
    public static final DeferredItem<Item> WORMY_APPLE        = ITEMS.registerSimpleItem("wormy_apple");
    public static final DeferredItem<Item> APPLE_OF_SLEEPING  = ITEMS.register("apple_of_sleeping", id -> new com.emoniph.witchery.item.AppleOfSleepingItem(itemProps(id)));
    public static final DeferredItem<Item> TREEFYD_SEEDS      = ITEMS.register("treefyd_seeds", id -> new com.emoniph.witchery.item.TreefydSeedsItem(itemProps(id)));

    // Witch Hunter Dawn armor set (source garlicked hunter variant)
    public static final DeferredItem<Item> DAWN_HUNTER_HAT            = ITEMS.register("dawn_hunter_hat",             id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.HELMET, HunterClothingItem.Variant.DAWN));
    public static final DeferredItem<Item> DAWN_HUNTER_COAT           = ITEMS.register("dawn_hunter_coat",            id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, HunterClothingItem.Variant.DAWN));
    public static final DeferredItem<Item> DAWN_HUNTER_LEGGINGS       = ITEMS.register("dawn_hunter_leggings",        id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.LEGGINGS, HunterClothingItem.Variant.DAWN));
    public static final DeferredItem<Item> DAWN_HUNTER_BOOTS          = ITEMS.register("dawn_hunter_boots",           id -> hunterArmor(id, net.minecraft.world.item.equipment.ArmorType.BOOTS, HunterClothingItem.Variant.DAWN));

    // Witches outfit / special armors — wearable
    public static final DeferredItem<Item> BABAS_HAT        = ITEMS.register("babas_hat",
            id -> new BabasHatItem(itemProps(id)
                    .humanoidArmor(BABAS_HAT_MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET)
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<Item> NECROMANCER_ROBES = ITEMS.register("necromancer_robes",
            id -> new NecromancerRobesItem(itemProps(id)
                    .humanoidArmor(NECROMANCER_ROBES_MATERIAL,
                            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> ICY_SLIPPERS     = ITEMS.register("icy_slippers",
            id -> new IcySlippersItem(itemProps(id)
                    .humanoidArmor(ICY_SLIPPERS_MATERIAL,
                            net.minecraft.world.item.equipment.ArmorType.BOOTS)));
    public static final DeferredItem<Item> RUBY_SLIPPERS    = ITEMS.register("ruby_slippers",
            id -> new RubySlippersItem(itemProps(id)
                    .humanoidArmor(RUBY_SLIPPERS_MATERIAL,
                            net.minecraft.world.item.equipment.ArmorType.BOOTS)));
    public static final DeferredItem<Item> SEEPING_SHOES    = ITEMS.register("seeping_shoes",
            id -> new SeepingShoesItem(itemProps(id)
                    .humanoidArmor(SEEPING_SHOES_MATERIAL,
                            net.minecraft.world.item.equipment.ArmorType.BOOTS)));

    public static final DeferredItem<Item> PLAYER_COMPASS = ITEMS.register("player_compass",
            id -> new com.emoniph.witchery.item.PlayerCompassItem(itemProps(id)));

    public static final DeferredItem<Item> SUN_GRENADE = ITEMS.register("sun_grenade",
            id -> new com.emoniph.witchery.item.SunGrenadeItem(itemProps(id).stacksTo(16)));
    public static final DeferredItem<Item> DUPLICATION_GRENADE = ITEMS.register("duplication_grenade",
            id -> new com.emoniph.witchery.item.DuplicationGrenadeItem(itemProps(id)));
    public static final DeferredItem<Item> STAFF_OF_DUPLICATION = ITEMS.register("staff_of_duplication",
            id -> new com.emoniph.witchery.item.StaffOfDuplicationItem(itemProps(id)));
    public static final DeferredItem<Item> MEATY_STEW = ITEMS.register("meaty_stew",
            id -> new MeatyStewItem(itemProps(id), 12, 1.0F));
    public static final DeferredItem<Item> RAW_MEATY_STEW = ITEMS.register("raw_meaty_stew",
            id -> new MeatyStewItem(itemProps(id), 1, 0.1F));
    public static final DeferredItem<Item> UNIVERSAL_ANTIDOTE = ITEMS.register("universal_antidote",
            id -> new UniversalAntidoteItem(itemProps(id)));
    public static final DeferredItem<Item> ODD_PORKCHOP_RAW = ITEMS.register("odd_porkchop_raw",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final DeferredItem<Item> ODD_PORKCHOP = ITEMS.register("odd_porkchop",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final DeferredItem<Item> ODD_PORKCHOP_COOKED = ITEMS.register("odd_porkchop_cooked",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build())));
    public static final DeferredItem<Item> COOKED_ODD_PORKCHOP = ITEMS.register("cooked_odd_porkchop",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build())));
    public static final DeferredItem<Item> SHEEP_LIVER = ITEMS.register("sheep_liver",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build())));
    public static final DeferredItem<Item> COOKED_SHEEP_LIVER = ITEMS.register("cooked_sheep_liver",
            id -> new Item(itemProps(id)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build())));
    public static final DeferredItem<Item> BIOME_NOTE = ITEMS.register("biome_note",
            id -> new com.emoniph.witchery.item.BiomeNoteItem(itemProps(id)));

    public static boolean isBrewOfRaising(ItemStack stack) {
        return stack.is(BREW_OF_RAISING.get());
    }

    public static boolean isBrewOfLove(ItemStack stack) {
        return stack.is(BREW_OF_LOVE.get());
    }

    public static boolean isBrewOfFlowingSpirit(ItemStack stack) {
        return stack.is(BREW_OF_FLOWING_SPIRIT.get());
    }

    public static boolean isBrewOfInk(ItemStack stack) {
        return stack.is(BREW_OF_INK.get());
    }

    public static boolean isBrewOfSprouting(ItemStack stack) {
        return stack.is(BREW_OF_SPROUTING.get());
    }

    public static boolean isBrewOfTheGrotesque(ItemStack stack) {
        return stack.is(BREW_OF_THE_GROTESQUE.get());
    }

    public static boolean isBrewOfTheDepths(ItemStack stack) {
        return stack.is(BREW_OF_THE_DEPTHS.get());
    }

    public static boolean isIcySlippers(ItemStack stack) {
        return stack.is(ICY_SLIPPERS.get());
    }

    public static boolean isWitchesRobe(ItemStack stack) {
        return stack.is(WITCHES_ROBES.get());
    }

    public static boolean isGulgsGirdle(ItemStack stack) {
        return stack.is(GULGS_GIRDLE.get());
    }

    public static boolean isDemonicContract(ItemStack stack) {
        return stack.is(DEMONIC_CONTRACT.get());
    }

    public static boolean isTormentContract(ItemStack stack) {
        return stack.is(TORMENT_SCROLL.get());
    }

    // No inert placeholders remain. The last six were resolved:
    //   * wolf_head / hellhound_head -> real mounted-head BLOCKS (see WitcheryBlocks +
    //     WolfHeadBlock / HellhoundHeadBlock), registered as block items just below.
    //   * brew_bottle / splash_brew_bottle / lingering_brew_bottle -> removed; these are the
    //     Resurrected generic NBT custom-brew CONTAINERS, which the port deliberately replaces
    //     with individual brew_of_* items, so they have no role in this architecture.
    //   * trapped_plant -> removed; it was an invented ID with no source item ("trapped plants"
    //     is only a generic Boline-tooltip phrase; the real trap plants are the already-promoted
    //     pit_dirt / pit_grass / plant_mine blocks and Poppy/Dandelion/Shrub derivatives).
    //   * The four brew_of_soul_* were earlier removed as duplicates of *_demon_soul.
    public static final Map<String, DeferredItem<Item>> LEGACY_PLACEHOLDER_ITEMS =
            registerLegacyPlaceholderItems();

    public static final DeferredItem<Item> WOLF_HEAD = ITEMS.register("wolf_head",
            id -> new BlockItem(WitcheryBlocks.WOLF_HEAD.get(), itemProps(id)));
    public static final DeferredItem<Item> HELLHOUND_HEAD = ITEMS.register("hellhound_head",
            id -> new BlockItem(WitcheryBlocks.HELLHOUND_HEAD.get(), itemProps(id)));

    // Custom brew containers — composed in the Witch's Cauldron (see com.emoniph.witchery.brewing).
    public static final DeferredItem<Item> BREW_BOTTLE = ITEMS.register("brew_bottle",
            id -> new com.emoniph.witchery.item.CustomBrewItem(itemProps(id)));
    public static final DeferredItem<Item> SPLASH_BREW_BOTTLE = ITEMS.register("splash_brew_bottle",
            id -> new com.emoniph.witchery.item.CustomThrownBrewItem(itemProps(id), false));
    public static final DeferredItem<Item> LINGERING_BREW_BOTTLE = ITEMS.register("lingering_brew_bottle",
            id -> new com.emoniph.witchery.item.CustomThrownBrewItem(itemProps(id), true));
    public static final DeferredItem<Item> LIQUID_BREW_BOTTLE = ITEMS.register("liquid_brew_bottle",
            id -> new com.emoniph.witchery.item.CustomThrownBrewItem(itemProps(id), Brews.Dispersal.LIQUID));
    public static final DeferredItem<Item> BREW_BUCKET = ITEMS.register("brew_bucket",
            id -> new com.emoniph.witchery.item.BrewBucketItem(itemProps(id)));

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
    public static final DeferredItem<SpawnEggItem> CRAZY_CAT_LADY_SPAWN_EGG =
            ITEMS.register("crazy_cat_lady_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.CRAZY_CAT_LADY.get())));

    public static final DeferredItem<SpawnEggItem> GOBLIN_GULG_SPAWN_EGG =
            ITEMS.register("goblin_gulg_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.GOBLIN_GULG.get())));
    public static final DeferredItem<SpawnEggItem> GOBLIN_MOG_SPAWN_EGG =
            ITEMS.register("goblin_mog_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.GOBLIN_MOG.get())));
    public static final DeferredItem<SpawnEggItem> VAMPIRE_SPAWN_EGG =
            ITEMS.register("vampire_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.VAMPIRE.get())));
    public static final DeferredItem<SpawnEggItem> BABA_YAGA_SPAWN_EGG =
            ITEMS.register("baba_yaga_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.BABA_YAGA.get())));
    public static final DeferredItem<SpawnEggItem> WOLFMAN_SPAWN_EGG =
            ITEMS.register("wolfman_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.WOLFMAN.get())));
    public static final DeferredItem<SpawnEggItem> HELLHOUND_SPAWN_EGG =
            ITEMS.register("hellhound_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.HELLHOUND.get())));
    public static final DeferredItem<SpawnEggItem> WERE_VILLAGER_SPAWN_EGG =
            ITEMS.register("were_villager_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.WERE_VILLAGER.get())));
    public static final DeferredItem<SpawnEggItem> VILLAGE_GUARD_SPAWN_EGG =
            ITEMS.register("village_guard_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.VILLAGE_GUARD.get())));

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
    public static final DeferredItem<SpawnEggItem> MINDRAKE_SPAWN_EGG =
            ITEMS.register("mindrake_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.MINDRAKE.get())));
    public static final DeferredItem<SpawnEggItem> DARK_MARK_SPAWN_EGG =
            ITEMS.register("dark_mark_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.DARK_MARK.get())));
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

    // ── Mirror World entities ─────────────────────────────────────────────────
    // Reflection: silver shell (mirror surface), deep mirror-blue spots
    public static final DeferredItem<SpawnEggItem> REFLECTION_SPAWN_EGG =
            ITEMS.register("reflection_spawn_egg", id ->
                    new SpawnEggItem(itemProps(id).spawnEgg(WitcheryEntities.REFLECTION.get())));
}
