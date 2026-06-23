from __future__ import annotations

import math
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "src/main/resources/assets/witchery/textures/block"
OUTPUT_DIR = ROOT / "src/main/resources/assets/witchery/textures/gui/book"

CANVAS_W = 96
CANVAS_H = 64
CENTER_X = CANVAS_W // 2
CENTER_Y = CANVAS_H // 2
GLYPH_SIZE = 6
CENTER_GLYPH_SIZE = 8

# The book view squashes the in-world footprint into a page diagram. These values
# preserve the old 7x7, 11x11, and 15x15 ring footprints while fitting a book card.
X_STEP = 5.0
Y_STEP = 3.0

SMALL_RING = [
    (-1, -3), (0, -3), (1, -3),
    (-2, -2), (2, -2),
    (-3, -1), (3, -1),
    (-3, 0), (3, 0),
    (-3, 1), (3, 1),
    (-2, 2), (2, 2),
    (-1, 3), (0, 3), (1, 3),
]

MEDIUM_RING = [
    (-2, -5), (-1, -5), (0, -5), (1, -5), (2, -5),
    (-3, -4), (3, -4),
    (-4, -3), (4, -3),
    (-5, -2), (5, -2),
    (-5, -1), (5, -1),
    (-5, 0), (5, 0),
    (-5, 1), (5, 1),
    (-5, 2), (5, 2),
    (-4, 3), (4, 3),
    (-3, 4), (3, 4),
    (-2, 5), (-1, 5), (0, 5), (1, 5), (2, 5),
]

LARGE_RING = [
    (-3, -7), (-2, -7), (-1, -7), (0, -7), (1, -7), (2, -7), (3, -7),
    (-4, -6), (4, -6),
    (-5, -5), (5, -5),
    (-6, -4), (6, -4),
    (-7, -3), (7, -3),
    (-7, -2), (7, -2),
    (-7, -1), (7, -1),
    (-7, 0), (7, 0),
    (-7, 1), (7, 1),
    (-7, 2), (7, 2),
    (-7, 3), (7, 3),
    (-6, 4), (6, 4),
    (-5, 5), (5, 5),
    (-4, 6), (4, 6),
    (-3, 7), (-2, 7), (-1, 7), (0, 7), (1, 7), (2, 7), (3, 7),
]

RINGS = {
    "small": SMALL_RING,
    "medium": MEDIUM_RING,
    "large": LARGE_RING,
}

FAMILIES = {
    "white": "circleglyph1",
    "purple": "circleglyph2",
    "red": "circleglyph3",
}


def fit_texture(path: Path, size: int) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    bbox = image.getbbox()
    if bbox is not None:
        image = image.crop(bbox)
    resampling = getattr(Image, "Resampling", Image).NEAREST
    return image.resize((size, size), resampling)


def sorted_offsets(offsets: list[tuple[int, int]]) -> list[tuple[int, int]]:
    return sorted(offsets, key=lambda offset: math.atan2(offset[1], offset[0]))


def paste_centered(canvas: Image.Image, glyph: Image.Image, x: float, y: float) -> None:
    left = round(x - glyph.width / 2)
    top = round(y - glyph.height / 2)
    canvas.alpha_composite(glyph, (left, top))


def bake_ring(color: str, prefix: str, size_name: str, offsets: list[tuple[int, int]]) -> None:
    glyphs = [
        fit_texture(SOURCE_DIR / f"{prefix}.{index}.png", GLYPH_SIZE)
        for index in range(1, 13)
    ]
    canvas = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    for index, (dx, dz) in enumerate(sorted_offsets(offsets)):
        glyph = glyphs[index % len(glyphs)]
        paste_centered(canvas, glyph, CENTER_X + dx * X_STEP, CENTER_Y + dz * Y_STEP)

    canvas.save(OUTPUT_DIR / f"ritual_ring_{color}_{size_name}.png")


def bake_center() -> None:
    canvas = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    glyph = fit_texture(SOURCE_DIR / "heart_glyph.png", CENTER_GLYPH_SIZE)
    paste_centered(canvas, glyph, CENTER_X, CENTER_Y)
    canvas.save(OUTPUT_DIR / "ritual_ring_center.png")


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for color, prefix in FAMILIES.items():
        for size_name, offsets in RINGS.items():
            bake_ring(color, prefix, size_name, offsets)
    bake_center()


if __name__ == "__main__":
    main()
