"""
generate_book_art.py — Creates 256x256 Patchouli book images for The Witches' Primer.

Images are composed from existing Witchery assets.
Patchouli spec: 256x256 PNG, content in upper-left 200x200 area.

Run from the project root:
  python generate_book_art.py
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import os
import random

SRC_GUI    = r"C:\claude\witchery\decompiled\witchery-original\assets\witchery\textures\gui"
SRC_BLOCKS = r"C:\claude\witchery\decompiled\witchery-original\assets\witchery\textures\blocks"
SRC_ENT    = r"C:\claude\witchery\decompiled\witchery-original\assets\witchery\textures\entities"
SRC_ITEMS  = r"C:\claude\witchery\witchery-port\src\main\resources\assets\witchery\textures\item"
OUT_DIR    = r"C:\claude\witchery\witchery-port\src\main\resources\assets\witchery\textures\patchouli"

os.makedirs(OUT_DIR, exist_ok=True)

# ── Palette ───────────────────────────────────────────────────────────────────

PARCHMENT   = (232, 213, 163, 255)
PARCHMENT_D = (195, 172, 120, 255)
DARK        = ( 22,  15,  10, 255)
DARK_TEAL   = ( 15,  30,  35, 255)
DARK_PURPLE = ( 28,  12,  40, 255)
BLOOD_RED   = (100,  12,  12, 255)
SPIRIT_BLUE = ( 60,  90, 160, 255)
MOSS_GREEN  = ( 25,  55,  20, 255)
EMBER       = ( 80,  30,   5, 255)


# ── Helpers ───────────────────────────────────────────────────────────────────

def canvas(bg_color=PARCHMENT):
    """256x256 blank canvas."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 200, 200], fill=bg_color)
    return img


def parchment_texture(base_color=PARCHMENT):
    """Warm parchment with subtle noise grain."""
    rng = random.Random(42)
    img = canvas(base_color)
    px = img.load()
    r0, g0, b0, _ = base_color
    for y in range(200):
        for x in range(200):
            n = rng.randint(-12, 12)
            px[x, y] = (
                max(0, min(255, r0 + n)),
                max(0, min(255, g0 + n // 2)),
                max(0, min(255, b0)),
                255
            )
    return img


def dark_texture(base_color=DARK):
    """Dark background with subtle grain."""
    rng = random.Random(99)
    img = canvas(base_color)
    px = img.load()
    r0, g0, b0, _ = base_color
    for y in range(200):
        for x in range(200):
            n = rng.randint(0, 18)
            px[x, y] = (
                max(0, min(255, r0 + n)),
                max(0, min(255, g0 + n)),
                max(0, min(255, b0 + n)),
                255
            )
    return img


def vignette(img, strength=100):
    """Apply a dark vignette to the 200x200 area."""
    mask = Image.new("L", (200, 200), 0)
    draw = ImageDraw.Draw(mask)
    cx, cy = 100, 100
    for r in range(min(cx, cy), 0, -1):
        val = int(strength * (1 - r / min(cx, cy)) ** 2)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=val)
    dark_layer = Image.new("RGBA", (200, 200), (0, 0, 0, 255))
    region = img.crop((0, 0, 200, 200))
    result = Image.composite(dark_layer, region, mask)
    img.paste(result, (0, 0))
    return img


def paste_centered(base, overlay, scale=None, y_offset=0):
    """Scale (optional) and paste overlay centered in the 200x200 area."""
    if scale:
        ow, oh = overlay.size
        overlay = overlay.resize((int(ow * scale), int(oh * scale)), Image.NEAREST)
    ow, oh = overlay.size
    x = (200 - ow) // 2
    y = (200 - oh) // 2 + y_offset
    if overlay.mode == "RGBA":
        base.paste(overlay, (x, y), overlay)
    else:
        base.paste(overlay.convert("RGBA"), (x, y))
    return base


def frame_border(img, color=(100, 80, 50, 255), width=3):
    """Decorative inner frame."""
    draw = ImageDraw.Draw(img)
    for i in range(width):
        draw.rectangle([i, i, 199 - i, 199 - i], outline=color)
    return img


def load_rgba(path):
    return Image.open(path).convert("RGBA")


def tint(img, color, strength=0.4):
    """Blend a color tint over img (RGBA)."""
    tint_layer = Image.new("RGBA", img.size, color)
    return Image.blend(img, tint_layer, strength)


def save(img, name):
    path = os.path.join(OUT_DIR, name)
    img.save(path)
    print(f"  OK  {name}")


# ── Image generators ──────────────────────────────────────────────────────────

def make_getting_started():
    """bookDouble.png — the original Witchery book open on a parchment ground."""
    base = parchment_texture(PARCHMENT)
    book = load_rgba(os.path.join(SRC_GUI, "bookDouble.png"))
    # The bookDouble is a 256x256 GUI sheet; the actual book graphic is in roughly upper portion
    # Crop the book graphic (approx top 160x120) and center it
    book_crop = book.crop((0, 0, 220, 160))
    book_scaled = book_crop.resize((176, 128), Image.NEAREST)
    paste_centered(base, book_scaled, y_offset=-10)
    frame_border(base, (140, 100, 50, 200), 2)
    save(base, "getting_started.png")


def make_altar():
    """The altar GUI — already 256x256, perfect as-is."""
    altar = load_rgba(os.path.join(SRC_GUI, "altar.png"))
    # Crop to interesting portion (top-left 200x200 already contains the altar art)
    result = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    result.paste(altar.crop((0, 0, 200, 200)), (0, 0))
    save(result, "altar.png")


def make_brewing():
    """Kettle on dark parchment with ember glow."""
    base = dark_texture(EMBER)
    kettle = load_rgba(os.path.join(SRC_BLOCKS, "kettle.png"))
    kettle_big = kettle.resize((96, 96), Image.NEAREST)
    paste_centered(base, kettle_big)
    vignette(base, 120)
    frame_border(base, (80, 40, 10, 200), 2)
    save(base, "brewing.png")


def make_witches_oven():
    oven = load_rgba(os.path.join(SRC_GUI, "witchesOven.png"))
    result = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    result.paste(oven.crop((0, 0, 200, 200)), (0, 0))
    save(result, "witches_oven.png")


def make_distillery():
    dist = load_rgba(os.path.join(SRC_GUI, "distiller.png"))
    result = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    result.paste(dist.crop((0, 0, 200, 200)), (0, 0))
    save(result, "distillery.png")


def make_circle_magic():
    """White circle on dark background — atmospheric."""
    base = dark_texture(DARK)
    circle = load_rgba(os.path.join(SRC_GUI, "circle_white_large.png"))
    circle_tinted = tint(circle, (180, 200, 255, 255), 0.2)
    # Crop to the actual circle area (upper 200x200)
    circle_crop = circle_tinted.crop((0, 0, 200, 200))
    circle_scaled = circle_crop.resize((180, 180), Image.LANCZOS)
    paste_centered(base, circle_scaled)
    vignette(base, 80)
    save(base, "circle_magic.png")


def make_rite_circles():
    """Three circles (small/medium/large) composed side by side."""
    base = dark_texture(DARK_PURPLE)
    sizes = [("circle_white_small.png", 48), ("circle_white_medium.png", 72), ("circle_white_large.png", 96)]
    x_positions = [16, 72, 108 + 8]
    for i, (fname, size) in enumerate(sizes):
        c = load_rgba(os.path.join(SRC_GUI, fname))
        c_scaled = c.crop((0, 0, 200, 200)).resize((size, size), Image.LANCZOS)
        y = (200 - size) // 2
        x = x_positions[i]
        base.paste(c_scaled, (x, y), c_scaled)
    vignette(base, 90)
    frame_border(base, (60, 40, 80, 180), 2)
    save(base, "drawing_circles.png")


def make_poppet_magic():
    """PoppetShelf on parchment — domestic and slightly unsettling."""
    base = parchment_texture((210, 190, 140, 255))
    shelf = load_rgba(os.path.join(SRC_BLOCKS, "poppetShelf.png"))
    shelf_big = shelf.resize((112, 112), Image.NEAREST)
    paste_centered(base, shelf_big)
    frame_border(base, (100, 70, 40, 200), 2)
    save(base, "poppet_magic.png")


def make_transformations():
    """Creatures spritesheet — show the creature variety."""
    base = dark_texture(BLOOD_RED)
    creatures = load_rgba(os.path.join(SRC_GUI, "creatures.png"))
    # Top portion of creatures sheet has the most interesting sprites
    crop = creatures.crop((0, 0, 200, 100))
    scaled = crop.resize((200, 100), Image.NEAREST)
    base.paste(scaled, (0, 50), scaled)
    vignette(base, 100)
    save(base, "transformations.png")


def make_spirit_world():
    """Spelleffect texture tinted blue-white — otherworldly."""
    base = dark_texture(DARK_TEAL)
    spell = load_rgba(os.path.join(SRC_ENT, "spelleffect.png"))
    spell_tinted = tint(spell, (100, 150, 255, 255), 0.3)
    spell_crop = spell_tinted.crop((0, 0, 200, 200))
    spell_scaled = spell_crop.resize((160, 160), Image.LANCZOS)
    paste_centered(base, spell_scaled)
    vignette(base, 100)
    save(base, "spirit_world.png")


def make_conjuration():
    """Brazier on dark ember background."""
    base = dark_texture(DARK)
    # Apply fiery tint to lower portion
    draw = ImageDraw.Draw(base)
    for y in range(200):
        alpha = int(60 * (y / 200) ** 2)
        draw.line([(0, y), (199, y)], fill=(80, 20, 0, alpha))
    brazier = load_rgba(os.path.join(SRC_BLOCKS, "brazier.png"))
    brazier_big = brazier.resize((80, 80), Image.NEAREST)
    paste_centered(base, brazier_big)
    vignette(base, 110)
    frame_border(base, (60, 30, 10, 150), 2)
    save(base, "conjuration.png")


def make_creatures():
    """Full creatures spritesheet."""
    base = dark_texture(MOSS_GREEN)
    creatures = load_rgba(os.path.join(SRC_GUI, "creatures.png"))
    c_crop = creatures.crop((0, 0, 200, 200))
    c_scaled = c_crop.resize((180, 180), Image.NEAREST)
    paste_centered(base, c_scaled)
    vignette(base, 70)
    save(base, "creatures.png")


def make_familiars():
    """Small owl/bat sprites on parchment."""
    base = parchment_texture()
    # Creature sheet has animal sprites - use a crop
    creatures = load_rgba(os.path.join(SRC_GUI, "creatures.png"))
    # Lower portion typically has animal/familiar sprites
    crop = creatures.crop((0, 128, 200, 200))
    scaled = crop.resize((200, 72), Image.NEAREST)
    paste_centered(base, scaled, y_offset=20)
    frame_border(base, (120, 90, 40, 200), 2)
    save(base, "familiars.png")


def make_scarecrow():
    """Scarecrow (fetish) on parchment."""
    base = parchment_texture((200, 185, 140, 255))
    scarecrow = load_rgba(os.path.join(SRC_BLOCKS, "scarecrow.png"))
    s_big = scarecrow.resize((96, 96), Image.NEAREST)
    paste_centered(base, s_big)
    frame_border(base, (100, 70, 30, 200), 2)
    save(base, "fetishes.png")


def make_ent():
    """Ent entity — grand forest creature."""
    base = dark_texture(MOSS_GREEN)
    ent = load_rgba(os.path.join(SRC_ENT, "ent.png"))
    # Ent texture is 256x256, head/torso in upper portion
    crop = ent.crop((0, 0, 128, 128))
    scaled = crop.resize((140, 140), Image.NEAREST)
    paste_centered(base, scaled, y_offset=-10)
    vignette(base, 80)
    save(base, "ent.png")


def make_spinning_wheel():
    sw = load_rgba(os.path.join(SRC_GUI, "spinningwheel.png"))
    result = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    result.paste(sw.crop((0, 0, 200, 200)), (0, 0))
    save(result, "spinning_wheel.png")


# ── Main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"Writing images to: {OUT_DIR}\n")

    make_getting_started()
    make_altar()
    make_brewing()
    make_witches_oven()
    make_distillery()
    make_circle_magic()
    make_rite_circles()
    make_poppet_magic()
    make_transformations()
    make_spirit_world()
    make_conjuration()
    make_creatures()
    make_familiars()
    make_scarecrow()
    make_ent()
    make_spinning_wheel()

    print(f"\nDone — {len(os.listdir(OUT_DIR))} images in {OUT_DIR}")
