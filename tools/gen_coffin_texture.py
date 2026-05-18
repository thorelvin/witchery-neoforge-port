"""
Generate coffin.png (128×64) to match CoffinModel UV layout.

UV formula for addBox(sX, sY, sZ) at texOffs(u, v):
  Top:    (u+sZ,        v,       u+sZ+sX,       v+sZ)
  Bottom: (u+sZ+sX,    v,       u+2*sZ+sX,     v+sZ)
  Right:  (u,           v+sZ,   u+sZ,            v+sZ+sY)
  Front:  (u+sZ,        v+sZ,   u+sZ+sX,         v+sZ+sY)
  Left:   (u+sZ+sX,    v+sZ,   u+2*sZ+sX,       v+sZ+sY)
  Back:   (u+2*sZ+sX,  v+sZ,   u+2*sZ+2*sX,    v+sZ+sY)
"""

from PIL import Image, ImageDraw
import random, math

W, H = 128, 64
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# ── Color palette — dark gothic coffin with subtle purple tint ───────────────
WOOD_DARK    = (40, 25, 35)   # near-black, slight purple tint
WOOD_MID     = (65, 42, 52)   # main plank face (warm-purple dark wood)
WOOD_LIGHT   = (88, 60, 72)   # highlight grain
WOOD_EDGE    = (28, 17, 24)   # border / depth edge
METAL        = (140, 120, 80) # brass hardware
METAL_DARK   = (90, 76, 50)
INTERIOR     = (55, 30, 38)   # inner walls (slightly redder)
LID_TOP      = (72, 48, 60)   # lid outer face — slightly lighter for visibility

def plank_pattern(draw, x0, y0, x1, y1, base, light, edge, horizontal=False):
    """Fill region with a plank-board texture."""
    draw.rectangle([x0, y0, x1-1, y1-1], fill=base)
    w, h = x1-x0, y1-y0
    if w <= 0 or h <= 0:
        return
    # Grain lines
    if horizontal:
        step = max(2, h // 4)
        for y in range(y0+step, y1, step):
            draw.line([(x0, y), (x1-1, y)], fill=edge, width=1)
    else:
        step = max(2, w // 4)
        for x in range(x0+step, x1, step):
            draw.line([(x, y0), (x, y1-1)], fill=edge, width=1)
    # Edge shadow
    draw.line([(x0, y0), (x1-1, y0)], fill=edge)
    draw.line([(x0, y0), (x0, y1-1)], fill=edge)

def solid(draw, x0, y0, x1, y1, col):
    if x1 > x0 and y1 > y0:
        draw.rectangle([x0, y0, x1-1, y1-1], fill=col)

# ── Helper: paint all 6 faces for a box ─────────────────────────────────────
def box(u, v, sX, sY, sZ):
    sZ, sX, sY = int(sZ), int(sX), int(sY)
    # Top
    plank_pattern(d, u+sZ, v, u+sZ+sX, v+sZ, WOOD_MID, WOOD_LIGHT, WOOD_EDGE)
    # Bottom (same)
    plank_pattern(d, u+sZ+sX, v, u+2*sZ+sX, v+sZ, WOOD_DARK, WOOD_MID, WOOD_EDGE)
    # Right
    plank_pattern(d, u, v+sZ, u+sZ, v+sZ+sY, INTERIOR, WOOD_LIGHT, WOOD_EDGE, horizontal=True)
    # Front
    plank_pattern(d, u+sZ, v+sZ, u+sZ+sX, v+sZ+sY, WOOD_MID, WOOD_LIGHT, WOOD_EDGE)
    # Left
    plank_pattern(d, u+sZ+sX, v+sZ, u+2*sZ+sX, v+sZ+sY, INTERIOR, WOOD_LIGHT, WOOD_EDGE, horizontal=True)
    # Back
    plank_pattern(d, u+2*sZ+sX, v+sZ, u+2*sZ+2*sX, v+sZ+sY, WOOD_MID, WOOD_LIGHT, WOOD_EDGE)

# ── HEAD body parts ──────────────────────────────────────────────────────────
# floor:      texOffs(0,0),  addBox(0,0,0, 16,2,16)
box(0,  0, 16, 2, 16)

# back wall:  texOffs(0,18), addBox(0,2,0, 16,10,2)
box(0, 18, 16, 10, 2)

# left wall:  texOffs(64,0), addBox(0,2,0, 2,10,16)
box(64,  0,  2, 10, 16)

# right wall: texOffs(64,26), addBox(14,2,0, 2,10,16)
box(64, 26,  2, 10, 16)

# lid:        texOffs(0,36), addBox(0,0,0, 16,2,16)
# Paint lid top with a slightly different (raised-panel) style
box(0, 36, 16, 2, 16)

# Decorate lid top face with cross/inset panel
lid_tx, lid_ty = 16, 36
lid_w, lid_h   = 16, 16
# Outer border already from plank_pattern; add inner raised panel
brd = 2
d.rectangle([lid_tx+brd, lid_ty+brd, lid_tx+lid_w-brd-1, lid_ty+lid_h-brd-1], fill=LID_TOP)
# Brass cross decoration on lid
cx = lid_tx + lid_w//2
cy = lid_ty + lid_h//2
d.rectangle([cx-1, lid_ty+brd+1, cx+1, lid_ty+lid_h-brd-2], fill=METAL)
d.rectangle([lid_tx+brd+1, cy-1, lid_tx+lid_w-brd-2, cy+1], fill=METAL)
# Corner dots
for dx, dy in [(-1,-1),(1,-1),(-1,1),(1,1)]:
    px = lid_tx + brd + (lid_w-2*brd-1)//2 * (dx+1)//2
    py = lid_ty + brd + (lid_h-2*brd-1)//2 * (dy+1)//2
    d.rectangle([px, py, px+1, py+1], fill=METAL_DARK)

# ── Clean up ─────────────────────────────────────────────────────────────────
del d
out = r"src\main\resources\assets\witchery\textures\block\coffin.png"
img.save(out)
print(f"Saved {out} ({W}x{H})")
