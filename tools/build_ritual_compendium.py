#!/usr/bin/env python3
"""Build player-facing Circle Magic compendium metadata from the audited source fixture."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


FAMILIES = [
    ("circle_talisman", "Circle Talisman Binding", [1, 45]),
    ("waystone_binding", "Waystone Binding", [2, 41]),
    ("attuned_stone_charging", "Attuned Stone Charging", [3]),
    ("infusion_recharge", "Infusion Recharging", [4]),
    ("waystone_transposition", "Waystone Transposition", [5, 91]),
    ("entity_transposition", "Entity Transposition", [6]),
    ("ore_transposition", "Ore Transposition", [7]),
    ("sanctity", "Rite of Sanctity", [8]),
    ("imprisonment", "Rite of Imprisonment", [9]),
    ("protection", "Rites of Protection", [10, 11, 12]),
    ("earths_wrath", "Rite of Earth's Wrath", [13, 77]),
    ("skys_wrath", "Rite of Sky's Wrath", [14, 15, 16]),
    ("total_eclipse", "Rite of Total Eclipse", [17, 18]),
    ("broken_earth", "Rite of Broken Earth", [19]),
    ("moving_earth", "Rite of Moving Earth", [20, 46, 47]),
    ("banishing", "Rite of Banishing", [21, 22]),
    ("demon_summoning", "Demon Summoning", [23, 24]),
    ("wither_summoning", "Wither Summoning", [25, 26]),
    ("player_infusions", "Player Infusions", [27, 28, 29, 30]),
    ("enchanted_broom", "Enchanted Broom", [31]),
    ("necromantic_stone", "Necromantic Stone", [32]),
    ("familiar_summoning", "Familiar Summoning", [33]),
    ("waystone_copying", "Waystone Copying", [34, 42]),
    ("fertility", "Rite of Fertility", [35, 36]),
    ("blight", "Curse of Blight", [37]),
    ("blindness", "Curse of Blindness", [38]),
    ("hell_on_earth", "Curse of Hell on Earth", [39]),
    ("witch_summoning", "Witch Summoning", [40]),
    ("natures_power", "Rite of Nature's Power", [43]),
    ("prior_incarnation", "Rite of Prior Incarnation", [44]),
    ("misfortune", "Misfortune and its Cure", [48, 49]),
    ("mystic_branch", "Mystic Branch Infusion", [50]),
    ("broiling", "Rite of Broiling", [51, 58]),
    ("insanity", "Insanity and its Cure", [52, 53]),
    ("familiar_binding", "Familiar Binding", [54]),
    ("familiar_call", "Calling the Familiar", [55]),
    ("corrupt_poppet", "Curse of Corrupt Poppet", [56]),
    ("crystal_ball", "Crystal Ball Infusion", [57]),
    ("sinking", "Sinking and its Cure", [59, 60]),
    ("seer_stone", "Seer Stone Infusion", [61]),
    ("overheating", "Overheating and its Cure", [62, 63]),
    ("shifting_seasons", "Rite of Shifting Seasons", [64]),
    ("icy_expansion", "Rite of Icy Expansion", [65]),
    ("raining_toads", "Curse of Raining Toads", [66]),
    ("glyphic_transformation", "Glyphic Transformation", [67]),
    ("beastial_call", "Rite of Beastial Call", [68]),
    ("manifestation", "Rite of Manifestation", [69]),
    ("forest", "Rite of the Forest", [70, 71, 72, 73, 74, 75, 76]),
    ("waking_nightmare", "Waking Nightmare and its Cure", [78, 79]),
    ("infused_brews", "Infused Brews", [80, 81]),
    ("spectral_stone", "Spectral Stone Workings", [82, 83]),
    ("fetishes", "Fetish Binding", [84, 85, 86]),
    ("imp_summoning", "Imp Summoning", [87]),
    ("blooded_waystone", "Blooded Waystone Binding", [88, 89]),
    ("patron_statue", "Patron Statue Binding", [90]),
    ("wolf_curse", "The Wolf Curse and its Cure", [92, 93]),
    ("vampire_cure", "Rite of Remove Vampirism", [94]),
    ("mirror", "Demonic Mirror Workings", [95, 96]),
]


VARIANT_LABELS = {
    1: "Redstone talisman binding", 45: "Portable talisman binding",
    2: "Altar-powered location binding", 41: "Portable location binding",
    5: "Bound Waystone journey", 91: "Blooded Waystone journey",
    10: "Monster ward", 11: "Universal ward", 12: "Portable ward",
    13: "Charged-stone volcano", 77: "Altar-powered volcano",
    14: "Inner storm", 15: "Outer storm", 16: "Portable storm",
    17: "Altar-powered eclipse", 18: "Portable eclipse",
    20: "Small column", 46: "Medium column", 47: "Large column",
    21: "Portable banishing", 22: "Altar-powered banishing",
    23: "Refined Evil path", 24: "Blaze Rod path",
    25: "Diamond Vapour path", 26: "Diamond path",
    27: "Light infusion", 28: "Overworld infusion", 29: "Otherwhere infusion", 30: "Infernal infusion",
    34: "Altar-powered copying", 42: "Portable copying",
    35: "Altar-powered fertility", 36: "Portable fertility",
    48: "Curse of Misfortune", 49: "Cure Misfortune",
    51: "Altar-powered broiling", 58: "Portable broiling",
    52: "Curse of Insanity", 53: "Cure Insanity",
    59: "Curse of Sinking", 60: "Cure Sinking",
    62: "Curse of Overheating", 63: "Cure Overheating",
    70: "Oak forest", 71: "Spruce forest", 72: "Birch forest", 73: "Jungle forest",
    74: "Rowan forest", 75: "Alder forest", 76: "Hawthorn forest",
    78: "Curse of Waking Nightmare", 79: "Cure Waking Nightmare",
    80: "Brew of Soaring", 81: "Brew of the Grave",
    82: "Create Spectral Stone", 83: "Bind spectral creatures",
    84: "Scarecrow", 85: "Trent Effigy", 86: "Witch's Ladder",
    88: "Altar-powered binding", 89: "Portable binding",
    92: "Curse of the Wolf", 93: "Cure Lycanthropy",
    95: "Trap a Demon", 96: "Release the Reflection",
}


ITEMS = {
    "Dye.BONE_MEAL.createStack()": "minecraft:bone_meal",
    "Dye.INK_SAC.createStack()": "minecraft:ink_sac",
    "new ItemStack(Blocks.field_150327_N)": "minecraft:dandelion",
    "new ItemStack(Blocks.field_150328_O)": "minecraft:poppy",
    "new ItemStack(Blocks.field_150337_Q)": "minecraft:red_mushroom",
    "new ItemStack(Blocks.field_150343_Z)": "minecraft:obsidian",
    "new ItemStack(Blocks.field_150345_g, 1, 0)": "minecraft:oak_sapling",
    "new ItemStack(Blocks.field_150345_g, 1, 1)": "minecraft:spruce_sapling",
    "new ItemStack(Blocks.field_150345_g, 1, 2)": "minecraft:birch_sapling",
    "new ItemStack(Blocks.field_150345_g, 1, 3)": "minecraft:jungle_sapling",
    "new ItemStack(Blocks.field_150347_e)": "minecraft:cobblestone",
    "new ItemStack(Blocks.field_150348_b)": "minecraft:stone",
    "new ItemStack(Blocks.field_150407_cf)": "minecraft:hay_block",
    "new ItemStack(Blocks.field_150410_aZ)": "minecraft:glass_pane",
    "new ItemStack(Blocks.field_150434_aF)": "minecraft:cactus",
    "new ItemStack(Items.field_151005_D)": "minecraft:golden_pickaxe",
    "new ItemStack(Items.field_151008_G)": "minecraft:feather",
    "new ItemStack(Items.field_151010_B)": "minecraft:golden_sword",
    "new ItemStack(Items.field_151014_N)": "minecraft:wheat_seeds",
    "new ItemStack(Items.field_151016_H)": "minecraft:gunpowder",
    "new ItemStack(Items.field_151034_e)": "minecraft:apple",
    "new ItemStack(Items.field_151036_c)": "minecraft:iron_axe",
    "new ItemStack(Items.field_151040_l)": "minecraft:iron_sword",
    "new ItemStack(Items.field_151041_m)": "minecraft:wooden_sword",
    "new ItemStack(Items.field_151042_j)": "minecraft:iron_ingot",
    "new ItemStack(Items.field_151043_k)": "minecraft:gold_ingot",
    "new ItemStack(Items.field_151044_h)": "minecraft:coal",
    "new ItemStack(Items.field_151045_i)": "minecraft:diamond",
    "new ItemStack(Items.field_151048_u)": "minecraft:diamond_sword",
    "new ItemStack(Items.field_151049_t)": "minecraft:stone_axe",
    "new ItemStack(Items.field_151052_q)": "minecraft:stone_sword",
    "new ItemStack(Items.field_151060_bw)": "minecraft:glistering_melon_slice",
    "new ItemStack(Items.field_151064_bs)": "minecraft:magma_cream",
    "new ItemStack(Items.field_151065_br)": "minecraft:blaze_powder",
    "new ItemStack(Items.field_151068_bn, 1, 8193)": "minecraft:potion",
    "new ItemStack(Items.field_151070_bp)": "minecraft:spider_eye",
    "new ItemStack(Items.field_151071_bq)": "minecraft:fermented_spider_eye",
    "new ItemStack(Items.field_151072_bj)": "minecraft:blaze_rod",
    "new ItemStack(Items.field_151075_bm)": "minecraft:nether_wart",
    "new ItemStack(Items.field_151078_bh)": "minecraft:rotten_flesh",
    "new ItemStack(Items.field_151079_bi)": "minecraft:ender_pearl",
    "new ItemStack(Items.field_151082_bd)": "minecraft:beef",
    "new ItemStack(Items.field_151102_aT)": "minecraft:sugar",
    "new ItemStack(Items.field_151103_aS)": "minecraft:bone",
    "new ItemStack(Items.field_151114_aO)": "minecraft:glowstone_dust",
    "new ItemStack(Items.field_151115_aP)": "minecraft:cod",
    "new ItemStack(Items.field_151117_aB)": "minecraft:milk_bucket",
    "new ItemStack(Items.field_151123_aH)": "minecraft:slime_ball",
    "new ItemStack(Items.field_151126_ay)": "minecraft:snowball",
    "new ItemStack(Items.field_151131_as)": "minecraft:water_bucket",
    "new ItemStack(Items.field_151137_ax)": "minecraft:redstone",
    "new ItemStack(Items.field_151144_bL, 1, 1)": "minecraft:wither_skeleton_skull",
    "new ItemStack(Items.field_151147_al)": "minecraft:porkchop",
    "new ItemStack(Items.field_151150_bK)": "minecraft:golden_carrot",
    "new ItemStack(Items.field_151156_bN)": "minecraft:nether_star",
    "new ItemStack(Items.field_151170_bI)": "minecraft:poisonous_potato",
    "new ItemStack(Items.field_151172_bF)": "minecraft:carrot",
    "new ItemStack(Items.field_151174_bG)": "minecraft:potato",
    "new ItemStack(Witchery.Blocks.FETISH_SCARECROW)": "witchery:fetish_scarecrow",
    "new ItemStack(Witchery.Blocks.FETISH_TREANT_IDOL)": "witchery:fetish_trent_effigy",
    "new ItemStack(Witchery.Blocks.FETISH_WITCHS_LADDER)": "witchery:fetish_witchs_ladder",
    "new ItemStack(Witchery.Blocks.SAPLING, 1, 0)": "witchery:rowan_sapling",
    "new ItemStack(Witchery.Blocks.SAPLING, 1, 1)": "witchery:alder_sapling",
    "new ItemStack(Witchery.Blocks.SAPLING, 1, 2)": "witchery:hawthorn_sapling",
    "new ItemStack(Witchery.Blocks.STATUE_OF_WORSHIP)": "witchery:statue_hobgoblin_patron",
    "new ItemStack(Witchery.Blocks.WICKER_BUNDLE)": "witchery:wicker_bundle",
    "new ItemStack(Witchery.Blocks.WOLFHEAD, 1, 1)": "witchery:hellhound_head",
    "new ItemStack(Witchery.Items.ARTHANA)": "witchery:arthana",
    "new ItemStack(Witchery.Items.BOLINE)": "witchery:boline",
    "new ItemStack(Witchery.Items.CIRCLE_TALISMAN)": "witchery:circle_talisman",
    "new ItemStack(Witchery.Items.MIRROR)": "witchery:mirror",
    "new ItemStack(Witchery.Items.SEEDS_GARLIC)": "witchery:garlic",
    "new ItemStack(Witchery.Items.SILVER_SWORD)": "witchery:silver_sword",
    "new ItemStack(Witchery.Items.SPECTRAL_STONE)": "witchery:spectral_stone",
    "new ItemStack(Witchery.Items.TAGLOCK_KIT, 1, 1)": "witchery:bound_taglock_kit",
    "Witchery.Items.POPPET.antiVoodooPoppet.createStack()": "witchery:voodoo_protection_poppet",
}


GENERIC_ITEMS = {
    "itemAshWood": "wood_ash", "itemAttunedStone": "attuned_stone",
    "itemAttunedStoneCharged": "attuned_stone_charged", "itemBatWool": "bat_wool",
    "itemBelladonnaFlower": "belladonna", "itemBranchEnt": "ent_twig",
    "itemBreathOfTheGoddess": "breath_of_goddess", "itemBrewGrotesque": "brew_of_the_grotesque",
    "itemBrewOfErosion": "brew_of_erosion", "itemBrewOfHollowTears": "brew_of_hollow_tears",
    "itemBrewOfInk": "brew_of_ink", "itemBrewOfLove": "brew_of_love",
    "itemBrewOfSprouting": "brew_of_sprouting", "itemBrewOfTheDepths": "brew_of_the_depths",
    "itemBroom": "broom", "itemCondensedFear": "condensed_fear",
    "itemCongealedSpirit": "congealed_spirit", "itemDemonHeart": "demon_heart",
    "itemDiamondVapour": "diamond_vapour", "itemDogTongue": "tongue_of_dog",
    "itemDropOfLuck": "drop_of_luck", "itemEnderDew": "ender_dew",
    "itemExhaleOfTheHornedOne": "exhale_of_horned_one", "itemFlyingOintment": "flying_ointment",
    "itemFrozenHeart": "frozen_heart", "itemGhostOfTheLight": "ghost_of_the_light",
    "itemGypsum": "gypsum", "itemHappenstanceOil": "happenstance_oil",
    "itemHintOfRebirth": "hint_of_rebirth", "itemIcyNeedle": "icy_needle",
    "itemInfernalAnimus": "infernal_animus", "itemInfernalBlood": "infernal_blood",
    "itemInfusionBase": "infused_brew_base", "itemMellifluousHunger": "mellifluous_hunger",
    "itemMutandis": "mutandis", "itemMutandisExtremis": "mutandis_extremis",
    "itemMysticUnguent": "mystic_unguent", "itemNecroStone": "necro_stone",
    "itemOdourOfPurity": "odour_of_purity", "itemOwletsWing": "owlets_wing",
    "itemQuartzSphere": "quartz_sphere", "itemQuicklime": "quicklime",
    "itemRedstoneSoup": "redstone_soup", "itemReekOfMisfortune": "reek_of_misfortune",
    "itemRefinedEvil": "refined_evil", "itemSoulOfTheWorld": "soul_of_the_world",
    "itemSpectralDust": "spectral_dust", "itemSpiritOfOtherwhere": "spirit_of_otherwhere",
    "itemTearOfTheGoddess": "tear_of_goddess", "itemToeOfFrog": "toe_of_frog",
    "itemTormentedTwine": "tormented_twine", "itemWaystone": "waystone",
    "itemWaystoneBound": "bound_waystone", "itemWaystonePlayerBound": "blooded_waystone",
    "itemWeb": "dense_web", "itemWhiffOfMagic": "whiff_of_magic",
    "itemWolfsbane": "wolfsbane_flower",
}
for source_name, modern_name in GENERIC_ITEMS.items():
    ITEMS[f"Witchery.Items.GENERIC.{source_name}.createStack()"] = f"witchery:{modern_name}"


LABEL_OVERRIDES = {
    "minecraft:potion": "Potion of Regeneration",
    "witchery:ent_twig": "Ent Twig",
    "witchery:statue_hobgoblin_patron": "Hobgoblin Patron Statue",
    "witchery:fetish_trent_effigy": "Trent Effigy",
}

TRAIT_LABELS = {
    "ONLY_AT_DAY": "Day",
    "ONLY_AT_NIGHT": "Night",
    "ONLY_IN_STROM": "Storm",
    "ONLY_OVERWORLD": "Overworld",
}

COVEN_REQUIREMENTS = {64: 4, 65: 2, 66: 1, 68: 3, 87: 1, 92: 6, 93: 6, 94: 6}
EXTRA_CONDITIONS = {
    92: ["Full moon", "Active cat familiar"],
    93: ["Full moon", "Active cat familiar"],
    94: ["Active cat familiar"],
}


def read_lang(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        if not raw or raw.startswith("#") or "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        values[key] = value
    return values


def item_label(item_id: str, modern_lang: dict[str, str]) -> str:
    if item_id in LABEL_OVERRIDES:
        return LABEL_OVERRIDES[item_id]
    namespace, name = item_id.split(":", 1)
    if namespace == "witchery":
        for key in (f"item.witchery.{name}", f"block.witchery.{name}"):
            if key in modern_lang:
                return modern_lang[key]
    words = name.replace("_", " ").title()
    return words.replace(" Of ", " of ").replace(" The ", " the ")


def offering(expression: str, optional: bool, modern_lang: dict[str, str]) -> dict[str, object]:
    item_id = ITEMS.get(expression)
    if item_id is None:
        raise ValueError(f"unmapped source offering: {expression}")
    return {
        "sourceExpression": expression,
        "item": item_id,
        "label": item_label(item_id, modern_lang),
        "optional": optional,
    }


def circle(expression: str) -> dict[str, str]:
    match = re.fullmatch(r"new Circle\((\d+), (\d+), (\d+)\)", expression)
    if match is None:
        raise ValueError(f"unmapped source circle: {expression}")
    ritual, otherwhere, infernal = (int(value) for value in match.groups())
    radius = max(ritual, otherwhere, infernal)
    size = {16: "small", 28: "medium", 40: "large"}.get(radius)
    if size is None:
        raise ValueError(f"unknown source circle radius: {expression}")
    color = "white" if ritual else "purple" if otherwhere else "red"
    return {"sourceExpression": expression, "color": color, "size": size}


def conditions(traits: str, source_id: int) -> list[str]:
    found = [label for key, label in TRAIT_LABELS.items() if key in traits]
    for condition in EXTRA_CONDITIONS.get(source_id, []):
        if condition not in found:
            found.append(condition)
    return found


def normalize_description(text: str) -> str:
    return " ".join(text.replace("lightnig", "lightning").split())


def build(fixture_path: Path, lang_path: Path, modern_lang_path: Path | None = None) -> dict[str, object]:
    fixture = json.loads(fixture_path.read_text(encoding="utf-8"))
    lang = read_lang(lang_path)
    modern_lang = (json.loads(modern_lang_path.read_text(encoding="utf-8"))
                   if modern_lang_path is not None else {})
    rows = {row["sourceId"]: row for row in fixture["registrations"]}
    assigned = [source_id for _, _, source_ids in FAMILIES for source_id in source_ids]
    if sorted(assigned) != list(range(1, 97)) or len(set(assigned)) != 96:
        raise ValueError("family table must assign source IDs 1..96 exactly once")

    families: list[dict[str, object]] = []
    for family_id, family_title, source_ids in FAMILIES:
        variants: list[dict[str, object]] = []
        for source_id in source_ids:
            row = rows[source_id]
            localized = lang.get(row["translationKey"])
            if localized is None or "{r||" not in localized:
                raise ValueError(f"missing rite book text for {row['translationKey']}")
            source_title, description = localized.split("{r||", 1)
            required = [offering(value, False, modern_lang) for value in row["requiredItems"]]
            optional = [offering(value, True, modern_lang) for value in row["optionalItems"]]
            variants.append({
                "sourceId": source_id,
                "sourceLine": row["sourceLine"],
                "sourceBookIndex": row["bookIndex"],
                "sourceTraits": row["traits"],
                "translationKey": row["translationKey"],
                "title": source_title,
                "variantLabel": VARIANT_LABELS.get(source_id, source_title),
                "description": normalize_description(description),
                "modernHome": row["modernHome"],
                "mode": row["mode"],
                "power": int(row["power"]) if float(row["power"]).is_integer() else row["power"],
                "coven": COVEN_REQUIREMENTS.get(source_id, 0),
                "conditions": conditions(row["traits"], source_id),
                "circles": [circle(value) for value in row["circles"]],
                "required": required,
                "optional": optional,
            })
        families.append({
            "id": family_id,
            "title": family_title,
            "icon": variants[0]["required"][0]["item"],
            "sourceIds": source_ids,
            "variants": variants,
        })

    return {
        "schemaVersion": 1,
        "source": fixture["source"],
        "sourceSha256": fixture["sourceSha256"],
        "registrationCount": fixture["registrationCount"],
        "familyCount": len(families),
        "families": families,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("fixture", type=Path)
    parser.add_argument("lang", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--modern-lang", type=Path)
    args = parser.parse_args()
    payload = build(args.fixture, args.lang, args.modern_lang)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {payload['registrationCount']} registrations in {payload['familyCount']} families")


if __name__ == "__main__":
    main()
