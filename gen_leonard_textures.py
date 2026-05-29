"""
Generate leonard.png (64x32) and lost_soul.png (32x32) textures for the
Shade of Leonard and Lost Soul entities. Run once; outputs land directly in
the textures/entity folder.
"""
from PIL import Image, ImageDraw
import os

OUT = r"src/main/resources/assets/witchery/textures/entity"
os.makedirs(OUT, exist_ok=True)


# ── helpers ─────────────────────────────────────────────────────────────────

def box_uvs(u, v, W, H, D):
    """Return (x1, y1, x2, y2) rects for (right, front, left, back, top, bot)."""
    return {
        "right":  (u,       v+D,     u+D,       v+D+H),
        "front":  (u+D,     v+D,     u+D+W,     v+D+H),
        "left":   (u+D+W,   v+D,     u+2*D+W,   v+D+H),
        "back":   (u+2*D+W, v+D,     u+2*D+2*W, v+D+H),
        "top":    (u+D,     v,       u+D+W,     v+D),
        "bottom": (u+D+W,   v,       u+2*D+W,   v+D),
    }

def paint(draw, uvs, faces, color, highlight=None):
    for face, rect in uvs.items():
        x1, y1, x2, y2 = rect
        c = highlight if highlight and face in faces else color
        draw.rectangle([x1, y1, x2-1, y2-1], fill=c)


# ── leonard.png — 64×32 dark shade ──────────────────────────────────────────

img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

BODY      = (18,  6,  28, 210)   # dark purple-black, nearly opaque
ROBE      = ( 8,  2,  14, 220)   # deep robe black
HIGHLIGHT = (42, 20,  58, 210)   # slight purple highlight for edges
EYE       = (130, 255, 40, 255)  # spectral green eye glow
EYE_GLOW  = ( 60, 140,  0, 180)  # dim glow ring around eye

# Arms (4×12×4) at texOffs(0, 0)
arm = box_uvs(0, 0, 4, 12, 4)
paint(d, arm, {"top","bottom"}, HIGHLIGHT)
paint(d, arm, {"front","left","right","back"}, BODY)

# Body (8×10×4) at texOffs(16, 0)
body = box_uvs(16, 0, 8, 10, 4)
paint(d, body, {"top","bottom"}, HIGHLIGHT)
paint(d, body, {"front","left","right","back"}, BODY)

# Robe upper (8×6×5) at texOffs(38, 9)
ru = box_uvs(38, 9, 8, 6, 5)
paint(d, ru, {"top","bottom"}, ROBE)
paint(d, ru, {"front","left","right","back"}, ROBE)

# Robe lower (10×6×6) at texOffs(32, 20)  [back wraps past x=64 — skip it]
rl = box_uvs(32, 20, 10, 6, 6)
for face, (x1, y1, x2, y2) in rl.items():
    if face == "back":
        continue   # back would wrap; skip (invisible in practice at this angle)
    c = HIGHLIGHT if face in {"top","bottom"} else ROBE
    d.rectangle([min(x1,63), min(y1,31), min(x2-1,63), min(y2-1,31)], fill=c)

# Head (8×8×8) at texOffs(0, 16)
head = box_uvs(0, 16, 8, 8, 8)
paint(d, head, {"top","bottom"}, HIGHLIGHT)
paint(d, head, {"left","right","back"}, BODY)

# Head front face (pixels 8–15 x, 24–31 y) — draw face detail
hfx1, hfy1 = 8, 24
d.rectangle([hfx1, hfy1, hfx1+7, hfy1+7], fill=BODY)

# Slightly lighter forehead strip
d.rectangle([hfx1, hfy1, hfx1+7, hfy1+2], fill=HIGHLIGHT)

# Eye glow haloes
d.rectangle([hfx1+1, hfy1+3, hfx1+2, hfy1+4], fill=EYE_GLOW)
d.rectangle([hfx1+5, hfy1+3, hfx1+6, hfy1+4], fill=EYE_GLOW)
# Eye pupils (bright green)
d.point((hfx1+2, hfy1+4), fill=EYE)
d.point((hfx1+6, hfy1+4), fill=EYE)

# Subtle mouth slit
for px in range(hfx1+2, hfx1+7):
    img.putpixel((px, hfy1+6), (30, 0, 20, 210))

# Edge highlight: outline top pixel row and left col of each side face
for face in ("right", "left"):
    x1, y1, x2, y2 = head[face]
    d.rectangle([x1, y1, x2-1, y1], fill=HIGHLIGHT)
    d.rectangle([x1, y1, x1, y2-1], fill=HIGHLIGHT)

path = os.path.join(OUT, "leonard.png")
img.save(path)
print(f"Saved {path}  ({img.size})")


# ── lost_soul.png — 32×32 neutral white orb for ARGB tinting ───────────────
# The renderer multiplies by the tint colour. Use bright white so green/red/black
# tints render cleanly. Slightly softer (220 alpha) so translucent shader works.

img2 = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d2   = ImageDraw.Draw(img2)

ORB   = (240, 240, 240, 220)
ORB_C = (200, 200, 200, 200)   # slightly dimmer for side faces
ORB_T = (180, 180, 180, 200)   # top/bottom caps

def orb_box(u, v, W, H, D):
    uvs = box_uvs(u, v, W, H, D)
    for face, (x1, y1, x2, y2) in uvs.items():
        c = ORB_T if face in {"top","bottom"} else (ORB if face == "front" else ORB_C)
        d2.rectangle([x1, y1, x2-1, y2-1], fill=c)

# shape4 top cap (3×1×3) at texOffs(6, 0)
orb_box(6, 0, 3, 1, 3)

# shape1 upper disc (5×1×5) at texOffs(2, 5)
orb_box(2, 5, 5, 1, 5)

# shape3 middle ring (6×2×6) at texOffs(0, 12)
orb_box(0, 12, 6, 2, 6)

# shape2 lower disc (5×1×5) at texOffs(2, 21)
orb_box(2, 21, 5, 1, 5)

# shape5 bottom cap (3×1×3) at texOffs(6, 28)
orb_box(6, 28, 3, 1, 3)

path2 = os.path.join(OUT, "lost_soul.png")
img2.save(path2)
print(f"Saved {path2}  ({img2.size})")
