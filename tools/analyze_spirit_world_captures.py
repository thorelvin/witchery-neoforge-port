#!/usr/bin/env python3
"""Measure and present matched original/port Spirit World atmosphere captures."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageStat


CASES = ("dream-clear", "dream-rain", "nightmare-clear", "demonic-clear")
REGIONS = {
    "upper_sky": (0.20, 0.05, 0.80, 0.20),
    "horizon_sky": (0.20, 0.47, 0.80, 0.52),
    "foreground": (0.05, 0.78, 0.95, 0.92),
}


def region_stats(image: Image.Image, bounds: tuple[float, float, float, float]) -> dict[str, object]:
    width, height = image.size
    box = (
        round(bounds[0] * width),
        round(bounds[1] * height),
        round(bounds[2] * width),
        round(bounds[3] * height),
    )
    region = image.crop(box).convert("RGB")
    stat = ImageStat.Stat(region)
    mean = [round(value, 2) for value in stat.mean]
    luminance = round(0.2126 * mean[0] + 0.7152 * mean[1] + 0.0722 * mean[2], 2)
    return {"box": list(box), "mean_rgb": mean, "luminance": luminance}


def rgb_distance(left: list[float], right: list[float]) -> float:
    return round(math.sqrt(sum((a - b) ** 2 for a, b in zip(left, right))), 2)


def analyze(capture_root: Path) -> dict[str, object]:
    report: dict[str, object] = {"capture_root": str(capture_root), "cases": {}}
    for case in CASES:
        variants: dict[str, object] = {}
        for variant in ("original", "port"):
            path = capture_root / variant / f"{variant}-{case}.png"
            with Image.open(path) as image:
                variants[variant] = {
                    "path": str(path),
                    "size": list(image.size),
                    "regions": {name: region_stats(image, bounds) for name, bounds in REGIONS.items()},
                }

        deltas = {}
        for region_name in REGIONS:
            original_rgb = variants["original"]["regions"][region_name]["mean_rgb"]
            port_rgb = variants["port"]["regions"][region_name]["mean_rgb"]
            deltas[region_name] = {"mean_rgb_distance": rgb_distance(original_rgb, port_rgb)}
        report["cases"][case] = {"captures": variants, "deltas": deltas}
    return report


def create_contact_sheet(capture_root: Path, output_path: Path) -> None:
    cell_width = 960
    cell_height = 540
    label_height = 28
    sheet = Image.new("RGB", (cell_width * 2, (cell_height + label_height) * len(CASES)), "#171717")
    draw = ImageDraw.Draw(sheet)

    for row, case in enumerate(CASES):
        row_y = row * (cell_height + label_height)
        for column, variant in enumerate(("original", "port")):
            path = capture_root / variant / f"{variant}-{case}.png"
            with Image.open(path) as source:
                frame = source.convert("RGB").resize((cell_width, cell_height), Image.Resampling.LANCZOS)
            x = column * cell_width
            sheet.paste(frame, (x, row_y + label_height))
            draw.text((x + 10, row_y + 8), f"{variant.upper()}  {case}", fill="white")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path)


def main() -> None:
    default_root = Path(__file__).resolve().parents[2] / "captures" / "spirit-world"
    parser = argparse.ArgumentParser()
    parser.add_argument("--capture-root", type=Path, default=default_root)
    args = parser.parse_args()

    output_root = args.capture_root / "comparison"
    output_root.mkdir(parents=True, exist_ok=True)
    report_path = output_root / "atmosphere-comparison.json"
    sheet_path = output_root / "atmosphere-contact-sheet.png"

    report_path.write_text(json.dumps(analyze(args.capture_root), indent=2) + "\n", encoding="utf-8")
    create_contact_sheet(args.capture_root, sheet_path)
    print(f"Spirit World comparison report: {report_path}")
    print(f"Spirit World contact sheet: {sheet_path}")


if __name__ == "__main__":
    main()
