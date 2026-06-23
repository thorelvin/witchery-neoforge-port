# Decompiled Source References

Use these folders as reference material while remaking Witchery to NeoForge. The goal is parity first, then careful modernization where the old implementation no longer fits the current runtime.

## Priority Rules

- `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1` is the main source of truth for original Witchery parity. Use it for recipes, block behavior, rituals, entities, timing, failure cases, drops, power costs, models, textures, GUI layouts, and weird edge cases.
- `C:\claude\witchery\decompiled\witchery-1.7.2-0.19.0` is historical context only. It is useful when checking how a mechanic evolved, but it is no longer the baseline for remake parity because 1.7.10/0.24.1 adds large systems missing from 1.7.2/0.19.0.
- `C:\claude\witchery\decompiled\WitcheryResurrected-1.12.2-0.5.2.4-forge-all` is preferred for graphics when it preserves the original Witchery feel better on modern-style assets. Use it for block/item models, GUI layouts, texture wrapping, and renderer clues, but verify mechanics against old Witchery.
- `C:\claude\witchery\decompiled\witcherycompanion-0.36.1-beta` is mainly bugfix and compatibility reference. Use it to understand known fixes, patch behavior, Patchouli docs, mixins, and later community corrections.
- Other Witchery bugfix/companion mods should be treated as correction references, not primary design sources. If they disagree with original Witchery, document why we follow them.
- `C:\claude\witchery\decompiled\bewitchment-1.20-10` is useful for newer Minecraft implementation patterns. Use it for modern-ish ideas around data, UI, registries, entities, and systems that Witchery 1.7.10 could not express cleanly.
- `C:\claude\witchery\decompiled\intangible-1.8.9-0.0.25` is inspiration for development after a stable baseline. Use it for design direction, spirit/magic ideas, and future expansion, not for baseline Witchery parity.

## 2026-05-28 Baseline Correction

Local comparison showed that Witchery `1.7.10-0.24.1` is a major content expansion over `1.7.2-0.19.0`, not just a Minecraft compatibility rebuild:

- `1.7.2-0.19.0`: 933 files, 459 Java files.
- `1.7.10-0.24.1`: 1601 files, 803 Java files.
- Delta: 670 added files, 346 added Java files, 323 added assets, 178 changed shared Java files, and only 2 removed Java files.
- The removed Java files are `PlayerModelEvents.java` and `DumpCommand.java`.

Important systems present or heavily expanded in 1.7.10 include the custom brew and Witch's Cauldron system, vampire and werewolf progression/content, Blood Crucible, Silver Vat, Coffin, Beartrap/Wolftrap, Garlic Garland, Mirror/Mirror Wall, Daylight Collector, Shaded Glass, Perpetual Ice, Stockade, Wolf Head/Altar, Mindrake/Garlic/Wolfsbane crops, village/goblin structure generation, and many new mobs/renderers/assets.

Remaking conclusion: use `1.7.10-0.24.1` as the canonical parity baseline. Use `1.7.2-0.19.0` only to understand older behavior or confirm whether something was introduced later.

## Modern API Reference

For NeoForge API shape, registration, data generation, rendering, sounds, and current best practices, use the official docs:

[NeoForge Documentation](https://docs.neoforged.net/)

## Working Guideline

When implementing a feature, compare in this order: original Witchery `1.7.10-0.24.1` first, Resurrected visuals second when they help interpret old rendering or asset layout, Companion/bugfix behavior third, modern NeoForge API last. If we intentionally choose a newer behavior, document the reason in the status docs so future fixes do not accidentally undo it.
