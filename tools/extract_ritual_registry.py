#!/usr/bin/env python3
"""Extract the authoritative 1.7.10 RiteRegistry table into a stable JSON fixture."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


MODERN_HOMES = {
    1: ("bind_circle_talisman", "direct"), 2: ("bind_waystone", "full"),
    3: ("charge_stone_small", "full"), 4: ("infusion_recharge", "full"),
    5: ("transposition_to_waystone", "full"), 6: ("transposition_entity", "full"),
    7: ("transpose_ores", "full"), 8: ("sanctity", "full"),
    9: ("imprisonment", "full"), 10: ("obsidian_barrier_monsters", "full"),
    11: ("protection", "full"), 12: ("obsidian_barrier_portable", "full"),
    13: ("volcano_charged", "full"), 14: ("storm_base", "full"),
    15: ("storm_large", "full"), 16: ("storm_portable", "full"),
    17: ("total_eclipse", "full"), 18: ("total_eclipse_portable", "full"),
    19: ("broken_earth", "full"), 20: ("raise_column_small", "full"),
    21: ("banish_demon_charged", "full"), 22: ("banish_demon", "full"),
    23: ("summon_demon", "full"), 24: ("summon_demon_expensive", "full"),
    25: ("summon_wither", "full"), 26: ("summon_wither_expensive", "full"),
    27: ("infuse_light", "full"), 28: ("infuse_overworld", "full"),
    29: ("infuse_otherwhere", "full"), 30: ("infuse_infernal", "full"),
    31: ("enchant_broom", "full"), 32: ("necromantic_stone", "full"),
    33: ("summon_familiar", "full"), 34: ("bind_waystone_copy", "full"),
    35: ("fertility", "full"), 36: ("portable_fertility", "full"),
    37: ("blight", "full"), 38: ("blindness", "full"),
    39: ("hell_on_earth", "full"), 40: ("summon_witch", "full"),
    41: ("bind_waystone_portable", "full"), 42: ("bind_waystone_copy_portable", "full"),
    43: ("natures_power", "full"), 44: ("prior_incarnation", "full"),
    45: ("bind_circle_talisman", "direct"), 46: ("raise_column_medium", "full"),
    47: ("raise_column_large", "full"), 48: ("curse_creature", "full"),
    49: ("cure_creature", "full"), 50: ("mystic_branch", "full"),
    51: ("cook_item", "full"), 52: ("curse_insanity", "full"),
    53: ("cure_insanity", "full"), 54: ("bind_familiar", "full"),
    55: ("call_familiar", "full"), 56: ("corrupt_poppet", "full"),
    57: ("crystal_ball", "full"), 58: ("cook_item", "full"),
    59: ("curse_sinking", "full"), 60: ("cure_sinking", "full"),
    61: ("infuse_seer_stone", "full"), 62: ("curse_overheating", "full"),
    63: ("cure_overheating", "full"), 64: ("climate_change", "full"),
    65: ("ice_shell", "full"), 66: ("rain_of_toads", "full"),
    67: ("glyphic_transformation", "direct"), 68: ("beastial_call", "full"),
    69: ("manifestation_dream", "full"), 70: ("forestation_oak", "full"),
    71: ("forestation_spruce", "full"), 72: ("forestation_birch", "full"),
    73: ("forestation_jungle", "full"), 74: ("forestation_rowan", "full"),
    75: ("forestation_alder", "full"), 76: ("forestation_hawthorn", "full"),
    77: ("volcano_powered", "full"), 78: ("curse_waking_nightmare", "full"),
    79: ("cure_waking_nightmare", "full"), 80: ("infuse_brew", "full"),
    81: ("infuse_brew", "full"), 82: ("spectral_stone", "full"),
    83: ("bind_spectral_stone", "full"), 84: ("bind_fetish", "full"),
    85: ("bind_fetish", "full"), 86: ("bind_fetish", "full"),
    87: ("summon_imp", "full"), 88: ("bind_blooded_waystone", "full"),
    89: ("bind_blooded_waystone_portable", "full"), 90: ("bind_statue_to_player", "full"),
    91: ("transposition_to_waystone", "full"), 92: ("curse_of_the_wolf", "full"),
    93: ("cure_wolf", "full"), 94: ("remove_vampirism", "full"),
    95: ("infusion_mirror", "full"), 96: ("summon_reflection", "full"),
}


def balanced(text: str, open_paren: int) -> tuple[str, int]:
    depth = 0
    in_string = False
    escaped = False
    for index in range(open_paren, len(text)):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[open_paren + 1:index], index + 1
    raise ValueError(f"unclosed call at character {open_paren}")


def split_top_level(text: str) -> list[str]:
    parts: list[str] = []
    start = 0
    parens = brackets = braces = 0
    in_string = False
    escaped = False
    for index, char in enumerate(text):
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "(":
            parens += 1
        elif char == ")":
            parens -= 1
        elif char == "[":
            brackets += 1
        elif char == "]":
            brackets -= 1
        elif char == "{":
            braces += 1
        elif char == "}":
            braces -= 1
        elif char == "," and parens == brackets == braces == 0:
            parts.append(text[start:index].strip())
            start = index + 1
    parts.append(text[start:].strip())
    return parts


def normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def constructor_arguments(text: str, class_name: str) -> list[str]:
    result: list[str] = []
    pattern = re.compile(rf"new\s+{re.escape(class_name)}\s*\(")
    for match in pattern.finditer(text):
        args, _ = balanced(text, match.end() - 1)
        result.extend(normalize(arg) for arg in split_top_level(args))
    return result


def extract(source: Path) -> dict[str, object]:
    text = source.read_text(encoding="utf-8")
    rows: list[dict[str, object]] = []
    marker = "RiteRegistry.addRecipe("
    cursor = 0
    while True:
        start = text.find(marker, cursor)
        if start < 0:
            break
        args_text, end = balanced(text, start + len(marker) - 1)
        args = split_top_level(args_text)
        suffix = text[end:text.find(marker, end) if text.find(marker, end) >= 0 else len(text)]
        name_match = re.search(r'\.setUnlocalizedName\("([^"]+)"\)', suffix)
        if len(args) < 5 or name_match is None:
            raise ValueError(f"malformed registration near line {text.count(chr(10), 0, start) + 1}")
        source_id = int(args[0])
        sacrifice = args[3]
        power = constructor_arguments(sacrifice, "SacrificePower")
        circles = [normalize(arg) for arg in args[5:]]
        home, mode = MODERN_HOMES[source_id]
        rows.append({
            "sourceId": source_id,
            "bookIndex": int(args[1]),
            "translationKey": name_match.group(1),
            "sourceLine": text.count("\n", 0, start) + 1,
            "effect": normalize(args[2]),
            "sacrifice": normalize(sacrifice),
            "requiredItems": constructor_arguments(sacrifice, "SacrificeItem"),
            "optionalItems": constructor_arguments(sacrifice, "SacrificeOptionalItem"),
            "power": float(power[0].removesuffix("F")) if power else 0.0,
            "traits": normalize(args[4]),
            "circles": circles,
            "modernHome": home,
            "mode": mode,
        })
        cursor = end
    return {
        "source": source.name,
        "sourceSha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
        "registrationCount": len(rows),
        "registrations": rows,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    payload = extract(args.source)
    if payload["registrationCount"] != 96:
        raise SystemExit(f"expected 96 registrations, found {payload['registrationCount']}")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
