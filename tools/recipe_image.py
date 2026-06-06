#!/usr/bin/env python3
"""
Bakes Witchery station recipes (Witches' Oven, Distillery, ...) into Modonomicon
book-image-page textures: the real station GUI with item sprites placed in their
slots, like the FTB wiki's recipe templates.

Output: 256x256 RGBA PNGs (Modonomicon draws the top-left ~200x200 region of an
image page). The station panel is composited at native slot coordinates, then
scaled 2x and centred in that region.

Sprite resolution:
  witchery:item/X   -> assets/witchery/textures/item/X.png   (via the item model's layer0)
  witchery:block/X  -> assets/witchery/textures/block/X.png
  minecraft:...     -> extracted from the NeoForge client-extra jar (item/ then block/)

Run from the repo root:  python tools/recipe_image.py
"""
import json
import os
import zipfile
import glob
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/witchery")
GUI = os.path.join(ASSETS, "textures/gui")
OUT_DIR = os.path.join(GUI, "book")
CLIENT_JAR = glob.glob(os.path.join(ROOT, "build/moddev/artifacts/*client-extra*.jar"))

_vanilla_zip = zipfile.ZipFile(CLIENT_JAR[0]) if CLIENT_JAR else None
_sprite_cache = {}


def _load_witchery(path):
    # path like "item/ingredient.foo" or "block/sapling_rowan"
    f = os.path.join(ASSETS, "textures", path + ".png")
    return Image.open(f).convert("RGBA")


def _load_vanilla(path):
    for sub in ("item", "block"):
        name = f"assets/minecraft/textures/{sub}/{path}.png"
        try:
            with _vanilla_zip.open(name) as fp:
                return Image.open(fp).convert("RGBA").copy()
        except KeyError:
            continue
    raise FileNotFoundError(f"vanilla sprite not found: {path}")


def sprite(item_id):
    """Resolve an item id ('witchery:foo' / 'minecraft:coal') to a 16x16 RGBA sprite."""
    if item_id in _sprite_cache:
        return _sprite_cache[item_id]
    ns, name = item_id.split(":")
    if ns == "witchery":
        model = os.path.join(ASSETS, "models/item", name + ".json")
        layer = None
        if os.path.exists(model):
            d = json.load(open(model))
            layer = d.get("textures", {}).get("layer0")
        if layer:
            _, p = layer.split(":")
            img = _load_witchery(p)
        else:  # block-item or missing model: search textures
            hits = glob.glob(os.path.join(ASSETS, f"textures/item/*{name}*.png")) \
                or glob.glob(os.path.join(ASSETS, f"textures/block/{name}*.png"))
            img = Image.open(hits[0]).convert("RGBA")
    else:
        img = _load_vanilla(name)
    if img.size != (16, 16):
        img = img.crop((0, 0, 16, 16))  # first frame of any animated sprite
    _sprite_cache[item_id] = img
    return img


# Slot item-render coordinates, lifted from the menu classes (item top-left in GUI space).
OVEN_SLOTS = {"input": (56, 17), "fuel": (56, 53), "jars": (83, 53),
              "cooked": (118, 21), "byproduct": (118, 53)}
OVEN_CROP = (48, 8, 144, 78)
DISTILLERY_SLOTS = {"input1": (56, 15), "input2": (56, 33), "jars": (56, 55),
                    "out1": (112, 26), "out2": (130, 26), "out3": (112, 44), "out4": (130, 44)}
DISTILLERY_CROP = (48, 6, 150, 74)


# Modonomicon's image page renders the FULL 256x256 texture into the page frame (Patchouli-style),
# so content must be centred in the whole 256x256 — not a 200x200 sub-region — or it sits top-left
# with wasted space. Fill most of the frame so the recipe reads large and the box isn't mostly empty.
CANVAS = 256
FILL = 236


def _fit_center(panel):
    s = min(FILL / panel.width, FILL / panel.height)
    w, h = max(1, round(panel.width * s)), max(1, round(panel.height * s))
    p = panel.resize((w, h), Image.NEAREST if s >= 1 else Image.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.alpha_composite(p, ((CANVAS - w) // 2, (CANVAS - h) // 2))
    return canvas


def _compose(gui_name, slots, crop, fills):
    base = Image.open(os.path.join(GUI, gui_name)).convert("RGBA").copy()
    for slot, item in fills.items():
        if item is None:
            continue
        base.alpha_composite(sprite(item), slots[slot])
    return _fit_center(base.crop(crop))


def oven(name, ingredient, result, byproduct="witchery:wood_ash", fuel="minecraft:coal"):
    img = _compose("witches_oven.png", OVEN_SLOTS, OVEN_CROP, {
        "input": ingredient, "fuel": fuel, "jars": "witchery:clay_jar",
        "cooked": result, "byproduct": byproduct})
    _save(name, img)


def distillery(name, in1, in2, outs):
    fills = {"input1": in1, "input2": in2, "jars": "witchery:clay_jar"}
    for i, o in enumerate(outs):
        fills[f"out{i + 1}"] = o
    _save(name, _compose("distillery.png", DISTILLERY_SLOTS, DISTILLERY_CROP, fills))


# Witch's Cauldron / Kettle have no slot GUI — items float in the brew — so we composite onto the
# original cauldron panel: ingredients in a row up top, (tinted) brew in the pot, arrow, result slot.
CAULDRON_PANEL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "recipe_assets/witch_cauldron_panel.png")
CAULDRON_RESULT = (242, 102)          # output-slot centre (panel is 2x scale)
CAULDRON_LIQUID = (113, 89, 142, 98)  # brew-surface bbox to tint
ING_SCALE = 2                          # match the 2x panel


def _draw_count(img, center, count):
    if count <= 1:
        return
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)
    s = str(count)
    x = center[0] + 16 * ING_SCALE - 6 * len(s)
    y = center[1] + 16 * ING_SCALE - 16
    d.text((x + 1, y + 1), s, fill=(0, 0, 0, 255))   # shadow
    d.text((x, y), s, fill=(255, 255, 255, 255))


def _tint_liquid(img, color):
    if color is None:
        return
    r0, g0, b0 = color
    x0, y0, x1, y1 = CAULDRON_LIQUID
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b, a = px[x, y]
            if a > 10 and g > 80 and g > r + 25 and g > b + 25:   # greenish brew pixel
                lum = g / 255.0
                px[x, y] = (int(r0 * lum), int(g0 * lum), int(b0 * lum), a)


def vessel_card(name, ingredients, result, count=1, color=None, per_row=3):
    """Cauldron/Kettle recipe: ingredient sprites in a row up top, result in the output slot."""
    base = Image.open(CAULDRON_PANEL).convert("RGBA").copy()
    _tint_liquid(base, color)
    sz = 16 * ING_SCALE
    # ingredient rows, centred over the cauldron (panel x-centre of the pot ~128)
    pitch = sz + 6
    cx = 122
    for i, item in enumerate(ingredients):
        row, col = divmod(i, per_row)
        n_in_row = min(per_row, len(ingredients) - row * per_row)
        row_w = n_in_row * pitch - 6
        x = cx - row_w // 2 + col * pitch
        y = 6 + row * pitch
        s = sprite(item).resize((sz, sz), Image.NEAREST)
        base.alpha_composite(s, (x, y))
    # result
    rs = sprite(result).resize((sz, sz), Image.NEAREST)
    rx, ry = CAULDRON_RESULT[0] - sz // 2, CAULDRON_RESULT[1] - sz // 2
    base.alpha_composite(rs, (rx, ry))
    _draw_count(base, (rx, ry), count)
    _save(name, _fit_center(base))


def _argb_to_rgb(v):
    v &= 0xFFFFFF
    return (v >> 16 & 0xFF, v >> 8 & 0xFF, v & 0xFF)


def kettle_from_recipe(name, recipe_id):
    """Build a kettle card straight from a witchery:kettle_brewing recipe JSON."""
    d = json.load(open(os.path.join(ROOT, "src/main/resources/data/witchery/recipe", recipe_id + ".json")))
    ings = [i["id"] for i in d["ingredients"]]
    res = d["result"]["id"]
    cnt = d["result"].get("count", 1)
    color = _argb_to_rgb(d["color"]) if "color" in d else None
    vessel_card(name, ings, res, cnt, color)


def _save(name, img):
    os.makedirs(OUT_DIR, exist_ok=True)
    img.save(os.path.join(OUT_DIR, name + ".png"))
    print("wrote", os.path.relpath(os.path.join(OUT_DIR, name + ".png"), ROOT))


def emit_backgrounds():
    """Empty GUI panels (no items) used by the live-rendered StationRecipePage, plus a coord-check preview."""
    from PIL import ImageDraw
    out = {}
    # Oven panel crop (matches OVEN_SLOTS, origin 48,8) -> 96x70
    oven = Image.open(os.path.join(GUI, "witches_oven.png")).convert("RGBA").crop(OVEN_CROP)
    out["bg_oven"] = (oven, {k: (x - OVEN_CROP[0], y - OVEN_CROP[1]) for k, (x, y) in OVEN_SLOTS.items()})
    # Distillery panel crop (origin 48,6) -> 102x68
    dist = Image.open(os.path.join(GUI, "distillery.png")).convert("RGBA").crop(DISTILLERY_CROP)
    out["bg_distillery"] = (dist, {k: (x - DISTILLERY_CROP[0], y - DISTILLERY_CROP[1]) for k, (x, y) in DISTILLERY_SLOTS.items()})
    # Cauldron panel downscaled to fit the book page width
    panel = Image.open(CAULDRON_PANEL).convert("RGBA")
    cw = 112
    ch = round(panel.height * cw / panel.width)
    caul = panel.resize((cw, ch), Image.LANCZOS)
    sx = cw / panel.width
    out["bg_cauldron"] = (caul, {"result": (round(CAULDRON_RESULT[0] * sx) - 8, round(CAULDRON_RESULT[1] * sx) - 8)})

    prev = Image.new("RGBA", (3 * 230, 200), (60, 60, 68, 255))
    for i, (name, (img, slots)) in enumerate(out.items()):
        img.save(os.path.join(OUT_DIR, name + ".png"))
        print("wrote", name + ".png", img.size)
        big = img.resize((img.width * 2, img.height * 2), Image.NEAREST).convert("RGBA")
        d = ImageDraw.Draw(big)
        for k, (x, y) in slots.items():
            d.rectangle([x * 2, y * 2, x * 2 + 31, y * 2 + 31], outline=(255, 60, 60, 255))
            d.text((x * 2, y * 2), k[:3], fill=(255, 255, 0, 255))
        prev.alpha_composite(big, (i * 230 + 8, 8))
    prev.save(os.path.expanduser("~/bg_coords.png"))


def main():
    emit_backgrounds()
    # Witches' Oven: sapling -> brew (+ wood ash, coal fuel, clay jar to catch fumes)
    oven("oven_breath_of_goddess", "minecraft:birch_sapling", "witchery:breath_of_goddess")
    oven("oven_odour_of_purity", "witchery:hawthorn_sapling", "witchery:odour_of_purity")
    oven("oven_whiff_of_magic", "witchery:rowan_sapling", "witchery:whiff_of_magic")
    oven("oven_exhale", "minecraft:oak_sapling", "witchery:exhale_of_horned_one")
    oven("oven_hint_of_rebirth", "minecraft:spruce_sapling", "witchery:hint_of_rebirth")
    oven("oven_reek", "witchery:alder_sapling", "witchery:reek_of_misfortune")
    oven("oven_foul_fume", "minecraft:jungle_sapling", "witchery:foul_fume")
    oven("oven_charcoal", "minecraft:oak_log", "minecraft:charcoal", byproduct="witchery:wood_ash")
    # Distillery: two inputs -> up to four jarred outputs
    distillery("distillery_odour", "witchery:diamond_vapour", "minecraft:ghast_tear",
               ["witchery:odour_of_purity", "witchery:reek_of_misfortune",
                "witchery:foul_fume", "witchery:refined_evil"])
    # Kettle: data-driven brews (liquid tinted to the brew's own colour)
    kettle_from_recipe("kettle_redstone_soup", "redstone_soup")
    kettle_from_recipe("kettle_brew_of_sleeping", "brew_of_sleeping")
    # Witch's Cauldron: hardcoded workings (no recipe JSON — right-click trigger shown in the row)
    GOLD = (232, 198, 64)
    vessel_card("cauldron_mutandis",
                ["witchery:mandrake_root", "witchery:exhale_of_horned_one", "minecraft:egg"],
                "witchery:mutandis", count=6)
    vessel_card("cauldron_mutandis_extremis",
                ["minecraft:nether_wart", "witchery:mutandis"],
                "witchery:mutandis_extremis", count=1)
    vessel_card("cauldron_drop_of_luck",
                ["witchery:mandrake_root", "minecraft:nether_wart", "witchery:tear_of_goddess",
                 "witchery:refined_evil", "witchery:mutandis_extremis"],
                "witchery:drop_of_luck", count=1, color=GOLD)


if __name__ == "__main__":
    main()
