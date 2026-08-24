#!/usr/bin/env python3
"""Extract the 1.7.10 composed-brew registrations into a stable JSON fixture."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


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


def normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def first_constructor_argument(text: str, class_name: str) -> str | None:
    match = re.search(rf"new\s+{re.escape(class_name)}\s*\(", text)
    if match is None:
        return None
    arguments, _ = balanced(text, match.end() - 1)
    return normalize(arguments)


def ingredient_expression(registration: str) -> str:
    key = first_constructor_argument(registration, "BrewItemKey")
    if key is not None:
        return key
    match = re.search(r"([A-Za-z0-9_.$]+)\.getBrewItemKey\(\)", registration)
    return f"{match.group(1)}.getBrewItemKey()" if match else "dynamic"


def extract(source: Path) -> dict[str, object]:
    text = source.read_text(encoding="utf-8")
    registrations: list[dict[str, object]] = []
    marker = "this.register("
    cursor = 0
    while True:
        start = text.find(marker, cursor)
        if start < 0:
            break
        body, end = balanced(text, start + len(marker) - 1)
        action_match = re.search(r"new\s+([A-Za-z0-9_]+)\s*\(", body)
        if action_match is None:
            raise ValueError(f"registration without action near line {text.count(chr(10), 0, start) + 1}")
        keys = re.findall(r'"(witchery:(?:brew|potion)\.[^"]+)"', body)
        registrations.append({
            "sourceIndex": len(registrations) + 1,
            "sourceLine": text.count("\n", 0, start) + 1,
            "actionClass": action_match.group(1),
            "ingredientExpression": ingredient_expression(body),
            "translationKeys": list(dict.fromkeys(keys)),
            "altarPowerExpression": first_constructor_argument(body, "AltarPower"),
            "effectLevelExpression": first_constructor_argument(body, "EffectLevel"),
            "probabilityExpression": first_constructor_argument(body, "Probability"),
            "baseDurationExpressions": [normalize(value) for value in re.findall(r"\.setBaseDuration\(([^)]+(?:\([^)]*\))?[^)]*)\)", body)],
            "incurable": ".setIncurable()" in body,
            "good": ".setGood()" in body,
            "nullifierCount": body.count(".addNullifier("),
            "sourceExpression": normalize(body),
        })
        cursor = end
    return {
        "source": source.name,
        "sourceSha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
        "registrationSiteCount": len(registrations),
        "registrations": registrations,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    payload = extract(args.source)
    if payload["registrationSiteCount"] != 158:
        raise SystemExit(f"expected 158 registration sites, found {payload['registrationSiteCount']}")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {payload['registrationSiteCount']} source registration sites")


if __name__ == "__main__":
    main()
