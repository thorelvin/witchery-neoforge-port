"""
Generate were_villager.png (64x64) for WereVillagerEntity.
Uses WitchHunterModel layout (same as witchhunter1.png):
head texOffs(0,0) 8x8x8; hat texOffs(32,0) overlay; body texOffs(16,16) 8x12x4;
arms texOffs(40,16) 4x12x4; legs texOffs(0,16) 4x12x4.

Visual intent: cursed / sickly villager — pale sallow skin, dark hollow eyes,
dishevelled grey-green clothing suggesting illness and the wolf curse.
"""
from PIL import Image, ImageDraw
import os, random

random.seed(7)
OUT = r"src/main/resources/assets/witchery/textures/entity"
os.makedirs(OUT, exist_ok=True)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
d   = ImageDraw.Draw(img)

SKIN  = (195, 170, 120, 255)  # sallow / sickly skin
SKIN_D= (155, 130,  85, 255)  # shadowed skin
EYE   = ( 30,  20,  10, 255)  # dark sunken eyes
PUPIL = ( 80, 160,  20, 255)  # wolf-green iris
CLOTH = ( 80,  90,  60, 255)  # dirty grey-green robes
CLOTH_D=(55,  65,  40, 255)   # shadow/fold
CLOTH_H=(110, 120,  85, 255)  # highlight
HAIR  = ( 60,  45,  25, 255)  # dark unkempt hair

def fill(x1, y1, x2, y2, c):
    d.rectangle([x1, y1, x2-1, y2-1], fill=c)

def noise(base, rng=12):
    r,g,b,a = base
    v = random.randint(-rng, rng)
    return (max(0,min(255,r+v)), max(0,min(255,g+v//2)), max(0,min(255,b+v//2)), a)

# ── HEAD (texOffs 0,0; 8x8x8) ────────────────────────────────────────────────
# Top: rows 0-7, cols 8-15; Bottom: rows 0-7, cols 16-23
# Right: rows 8-15, cols 0-7; Front: rows 8-15, cols 8-15; Left: rows 8-15, cols 16-23; Back: rows 8-15, cols 24-31
fill(8,0,16,8, HAIR)       # top — hair
fill(16,0,24,8, SKIN_D)    # bottom

fill(0,8,8,16, SKIN)       # right cheek
fill(24,8,32,16, SKIN)     # left cheek
fill(24,8,32,16, SKIN)
fill(8,8,16,16, SKIN)      # back
fill(8,8,16,16, SKIN_D)

# Front face — sickly with sunken eyes
fill(16,8,24,16, SKIN)     # base face
# Hair / forehead shadow
for x in range(16,24): img.putpixel((x,8), HAIR)
# Sunken eyes
img.putpixel((18,10), EYE); img.putpixel((19,10), EYE)
img.putpixel((18,11), PUPIL); img.putpixel((19,11), PUPIL)
img.putpixel((21,10), EYE); img.putpixel((22,10), EYE)
img.putpixel((21,11), PUPIL); img.putpixel((22,11), PUPIL)
# Sunken cheeks / hollows
img.putpixel((17,12), SKIN_D); img.putpixel((23,12), SKIN_D)
# Thin worried mouth
for x in range(18,23): img.putpixel((x,14), SKIN_D)

# Noise on face
for y in range(8,16):
    for x in range(16,24):
        p = img.getpixel((x,y))
        if p[3] > 0: img.putpixel((x,y), noise(p, 8))

# ── HAT OVERLAY (texOffs 32,0; 8x8x8 with inflation) ──────────────────────────
# (hidden in VillageGuardModel but needed by HumanoidModel — keep transparent)
# Leave as transparent

# ── BODY (texOffs 16,16; 8x12x4) ─────────────────────────────────────────────
# top: (20,16)-(28,20); bottom: (28,16)-(36,20)
# right: (16,20)-(20,32); front: (20,20)-(28,32); left: (28,20)-(32,32); back: (32,20)-(40,32)
fill(20,16,28,20, CLOTH_D)
fill(28,16,36,20, CLOTH_D)
fill(16,20,20,32, CLOTH)
fill(20,20,28,32, CLOTH)
fill(28,20,32,32, CLOTH)
fill(32,20,40,32, CLOTH_D)
# Fold lines on front
for y in range(22,30,3):
    for x in range(21,27): img.putpixel((x,y), CLOTH_D)
fill(20,20,28,21, CLOTH_H)

# ── ARMS (texOffs 40,16; 4x12x4) ─────────────────────────────────────────────
fill(44,16,48,20, CLOTH_D)
fill(48,16,52,20, CLOTH_D)
fill(40,20,44,32, CLOTH)
fill(44,20,48,32, CLOTH)
fill(48,20,52,32, CLOTH)
fill(52,20,56,32, CLOTH_D)
fill(44,20,48,21, CLOTH_H)

# ── LEGS (texOffs 0,16; 4x12x4) ──────────────────────────────────────────────
fill(4,16,8,20, CLOTH_D)
fill(8,16,12,20, CLOTH_D)
fill(0,20,4,32, CLOTH)
fill(4,20,8,32, CLOTH)
fill(8,20,12,32, CLOTH)
fill(12,20,16,32, CLOTH_D)
fill(4,20,8,21, CLOTH_H)

path = os.path.join(OUT, "were_villager.png")
img.save(path)
print(f"Saved {path}  {img.size}")
