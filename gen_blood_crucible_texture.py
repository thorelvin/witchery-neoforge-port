"""
Regenerate blood_crucible.png (32x64) so the UV regions for each model part
render correctly:

  rows  0-6   : front wall face (dark iron/stone)
  rows  7-13  : back wall face
  rows 14-30  : left/right wall faces
  rows 31-43  : bottom plate
  rows 44-56  : BLOOD surface (bright crimson)   ← was wrong, now extended
  rows 57-63  : spare / edges

The blood top face samples (6,44)-(12,50) so rows 44+ must be solidly red.
"""
from PIL import Image, ImageDraw
import os

OUT = r"src/main/resources/assets/witchery/textures/block"
os.makedirs(OUT, exist_ok=True)

STONE   = (55,  45,  45, 255)   # dark iron-stone for bowl material
STONE_H = (75,  60,  58, 255)   # slightly lighter for top/bottom caps
EDGE    = (35,  28,  28, 255)   # darker edge for shadow
BLOOD   = (170,  10,  10, 255)  # bright crimson for blood surface
BLOOD_D = (120,   5,   5, 255)  # darker blood for sides/bottom (mostly invisible)

img = Image.new("RGBA", (32, 64), (0, 0, 0, 0))
d   = ImageDraw.Draw(img)

def fill(x1, y1, x2, y2, color):
    d.rectangle([x1, y1, x2-1, y2-1], fill=color)

# ── Bowl material rows 0-43 ──────────────────────────────────────────────────
# Simple stone texture: base colour with 1-pixel dark edge and subtle noise-like
# variation across each row.

import random
random.seed(42)

for y in range(44):
    for x in range(32):
        # Subtle randomised variation so it reads as stone, not flat colour
        v = random.randint(-8, 8)
        base = STONE if (y < 31) else STONE_H
        r = max(0, min(255, base[0] + v))
        g = max(0, min(255, base[1] + v // 2))
        b = max(0, min(255, base[2] + v // 2))
        img.putpixel((x, y), (r, g, b, 255))

# Darken edges of each UV face for a slight shadow
# Front/back wall faces: rows 5-10 and 12-17 (front face column 1-8)
for y in range(5, 10):
    img.putpixel((1, y), EDGE)
    img.putpixel((8, y), EDGE)
for y in range(12, 17):
    img.putpixel((1, y), EDGE)
    img.putpixel((8, y), EDGE)

# ── Blood rows 44-63 ─────────────────────────────────────────────────────────
# Blood top face UV: texOffs(0,44) with box W=6,D=6 → top face at (6,44)-(12,50)
# Make the entire rows 44-63 blood-red so it's impossible to sample wrong area.

for y in range(44, 64):
    for x in range(32):
        # Slight shimmer effect on the blood surface
        v = random.randint(-10, 10)
        r = max(100, min(255, BLOOD[0] + v))
        g = max(0,   min(30,  BLOOD[1] + v // 4))
        b = max(0,   min(20,  BLOOD[2]))
        img.putpixel((x, y), (r, g, b, 220))

# Brighter core of blood surface (center of top face at cols 7-11, rows 44-49)
for y in range(44, 50):
    for x in range(7, 12):
        img.putpixel((x, y), (200, 20, 20, 240))

path = os.path.join(OUT, "blood_crucible.png")
img.save(path)
print(f"Saved {path}  ({img.size})")
