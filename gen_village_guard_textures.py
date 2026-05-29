"""
Generate village_guard1.png and village_guard2.png (64x32) for the Village Guard entity.

UV layout (64x32 biped matching VillageGuardModel):
  Head (8x8x8) at texOffs(0,0):
    top:    (8,0)-(16,8)
    bottom: (16,0)-(24,8)
    right:  (0,8)-(8,16)    ← left cheek
    front:  (8,8)-(16,16)   ← face
    left:   (16,8)-(24,16)  ← right cheek
    back:   (24,8)-(32,16)

  Body (8x12x4) at texOffs(16,16):
    top:    (20,16)-(28,20)
    bottom: (28,16)-(36,20)
    right:  (16,20)-(20,32)
    front:  (20,20)-(28,32)
    left:   (28,20)-(32,32)
    back:   (32,20)-(40,32)

  Arms (4x12x4) at texOffs(40,16):
    top:    (44,16)-(48,20)
    bottom: (48,16)-(52,20)
    right:  (40,20)-(44,32)
    front:  (44,20)-(48,32)
    left:   (48,20)-(52,32)
    back:   (52,20)-(56,32)

  Legs (4x12x4) at texOffs(0,16):
    top:    (4,16)-(8,20)
    bottom: (8,16)-(12,20)
    right:  (0,20)-(4,32)
    front:  (4,20)-(8,32)
    left:   (8,20)-(12,32)
    back:   (12,20)-(16,32)
"""
from PIL import Image, ImageDraw
import os

OUT = r"src/main/resources/assets/witchery/textures/entity"
os.makedirs(OUT, exist_ok=True)

# ── palette ──────────────────────────────────────────────────────────────────
SKIN      = (198, 155, 109, 255)  # face/neck skin
SKIN_D    = (170, 128,  88, 255)  # darker skin (shadow)
EYE       = ( 50,  60,  80, 255)  # dark blue-grey eyes
HAIR      = ( 80,  55,  30, 255)  # dark brown hair
IRON      = (160, 162, 172, 255)  # base iron armour
IRON_H    = (200, 202, 212, 255)  # iron highlight
IRON_D    = (110, 112, 122, 255)  # iron dark/edge
IRON_TRIM = ( 90,  92, 102, 255)  # trim/rivets
CLOTH     = (120,  85,  50, 255)  # brown under-tunic (visible at neck/waist)
LEATHER   = ( 90,  65,  35, 255)  # leather straps

# Variant 2 palette (captain — darker armour, blue accent)
IRON2      = (100, 110, 140, 255)  # deep blue-grey iron
IRON2_H    = (140, 150, 185, 255)
IRON2_D    = ( 60,  70,  95, 255)
IRON2_TRIM = ( 40,  50,  75, 255)
PLUME      = (200, 220, 240, 255)  # light blue helmet plume accent


def fill(img, x1, y1, x2, y2, color):
    ImageDraw.Draw(img).rectangle([x1, y1, x2-1, y2-1], fill=color)


def draw_guard(variant):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    iron     = IRON2     if variant == 1 else IRON
    iron_h   = IRON2_H   if variant == 1 else IRON_H
    iron_d   = IRON2_D   if variant == 1 else IRON_D
    iron_t   = IRON2_TRIM if variant == 1 else IRON_TRIM
    hair_col = (50, 35, 20, 255) if variant == 1 else HAIR

    # ── HEAD ─────────────────────────────────────────────────────────────────
    # Top (hair)
    fill(img, 8, 0, 16, 8, hair_col)
    # Bottom
    fill(img, 16, 0, 24, 8, SKIN_D)

    # Right cheek
    fill(img, 0, 8, 8, 16, SKIN)
    fill(img, 0, 8, 1, 16, SKIN_D)   # edge shadow

    # Front FACE (8,8)-(16,16)
    fill(img, 8, 8, 16, 16, SKIN)
    # Hairline at top of face
    fill(img, 8, 8, 16, 9, hair_col)
    # Eyes (2px each, centred)
    img.putpixel((10, 11), EYE)
    img.putpixel((11, 11), EYE)
    img.putpixel((13, 11), EYE)
    img.putpixel((14, 11), EYE)
    # Nose bridge (single pixel)
    img.putpixel((12, 12), SKIN_D)
    # Mouth
    for x in range(10, 15):
        img.putpixel((x, 14), SKIN_D)

    # Left cheek (mirror of right)
    fill(img, 16, 8, 24, 16, SKIN)
    fill(img, 23, 8, 24, 16, SKIN_D)

    # Back of head (hair)
    fill(img, 24, 8, 32, 16, hair_col)

    # Helmet visor / iron coif at top rows of face and sides
    fill(img, 8, 8, 16, 10, iron_d)    # iron visor over forehead
    fill(img, 0, 8, 8, 10, iron_d)     # side visor
    fill(img, 16, 8, 24, 10, iron_d)   # side visor
    # Iron top of head (helmet crown)
    fill(img, 8, 0, 16, 8, iron)
    fill(img, 8, 0, 16, 1, iron_h)     # highlight
    fill(img, 8, 7, 16, 8, iron_d)
    if variant == 1:
        # Blue plume stripe
        fill(img, 11, 0, 13, 8, plume if False else PLUME)
        fill(img, 11, 0, 13, 8, PLUME)

    # ── BODY ─────────────────────────────────────────────────────────────────
    # Top (shoulder pad)
    fill(img, 20, 16, 28, 20, iron)
    fill(img, 28, 16, 36, 20, iron_d)

    # Right side
    fill(img, 16, 20, 20, 32, iron)
    fill(img, 16, 20, 17, 32, iron_d)

    # Front chest plate (20,20)-(28,32)
    fill(img, 20, 20, 28, 32, iron)
    fill(img, 20, 20, 28, 21, iron_h)   # top highlight
    fill(img, 20, 31, 28, 32, iron_d)   # bottom shadow
    fill(img, 20, 20, 21, 32, iron_d)   # left edge
    fill(img, 27, 20, 28, 32, iron_d)   # right edge
    # Centre line detail (vertical)
    for y in range(22, 30):
        img.putpixel((24, y), iron_t)
    # Horizontal strap
    fill(img, 21, 25, 27, 26, iron_t)
    # Rivets
    img.putpixel((22, 23), iron_h)
    img.putpixel((26, 23), iron_h)
    img.putpixel((22, 27), iron_h)
    img.putpixel((26, 27), iron_h)

    # Left side
    fill(img, 28, 20, 32, 32, iron)
    fill(img, 31, 20, 32, 32, iron_d)

    # Back
    fill(img, 32, 20, 40, 32, iron)
    fill(img, 32, 20, 40, 21, iron_h)
    fill(img, 32, 31, 40, 32, iron_d)

    # Waist leather trim (bottom row of body)
    fill(img, 20, 30, 28, 32, LEATHER)

    # ── ARMS (shared UV, mirrored for left) ───────────────────────────────────
    # Top cap
    fill(img, 44, 16, 48, 20, iron)
    fill(img, 48, 16, 52, 20, iron_d)

    # Right side
    fill(img, 40, 20, 44, 32, iron)
    fill(img, 40, 20, 41, 32, iron_d)

    # Front arm plate
    fill(img, 44, 20, 48, 32, iron)
    fill(img, 44, 20, 48, 21, iron_h)
    fill(img, 44, 31, 48, 32, iron_d)
    fill(img, 44, 20, 45, 32, iron_d)

    # Left side
    fill(img, 48, 20, 52, 32, iron)
    fill(img, 51, 20, 52, 32, iron_d)

    # Back
    fill(img, 52, 20, 56, 32, iron)

    # ── LEGS ─────────────────────────────────────────────────────────────────
    # Top cap
    fill(img, 4, 16, 8, 20, iron)
    fill(img, 8, 16, 12, 20, iron_d)

    # Right side
    fill(img, 0, 20, 4, 32, iron)
    fill(img, 0, 20, 1, 32, iron_d)

    # Front leg plate
    fill(img, 4, 20, 8, 32, iron)
    fill(img, 4, 20, 8, 21, iron_h)
    fill(img, 4, 31, 8, 32, iron_d)
    fill(img, 4, 20, 5, 32, iron_d)
    # Knee detail
    fill(img, 5, 24, 7, 26, iron_h)

    # Left side
    fill(img, 8, 20, 12, 32, iron)
    fill(img, 11, 20, 12, 32, iron_d)

    # Back
    fill(img, 12, 20, 16, 32, iron)

    # Leather boot trim at bottom
    fill(img, 4, 30, 8, 32, LEATHER)

    return img


# ── generate ─────────────────────────────────────────────────────────────────
for variant, name in [(0, "village_guard1.png"), (1, "village_guard2.png")]:
    img = draw_guard(variant)
    path = os.path.join(OUT, name)
    img.save(path)
    print(f"Saved {path}  ({img.size})")
