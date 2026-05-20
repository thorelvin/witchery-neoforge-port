# Decompiled Source References

Use these folders as reference material while porting Witchery to NeoForge. The goal is parity first, then careful modernization where the old implementation no longer fits the current runtime.

## Priority Rules

- `C:\claude\witchery\decompiled\witchery-1.7.2-0.19.0` is the main source of truth for mechanics. Use it for recipes, block behavior, rituals, entities, timing, failure cases, drops, power costs, and weird edge cases.
- `C:\claude\witchery\decompiled\WitcheryResurrected-1.12.2-0.5.2.4-forge-all` is preferred for graphics when it preserves the original Witchery feel better on modern-style assets. Use it for block/item models, GUI layouts, texture wrapping, and renderer clues, but verify mechanics against old Witchery.
- `C:\claude\witchery\decompiled\witcherycompanion-0.36.1-beta` is mainly bugfix and compatibility reference. Use it to understand known fixes, patch behavior, Patchouli docs, mixins, and later community corrections.
- Other Witchery bugfix/companion mods should be treated as correction references, not primary design sources. If they disagree with original Witchery, document why we follow them.
- `C:\claude\witchery\decompiled\bewitchment-1.20-10` is useful for newer Minecraft implementation patterns. Use it for modern-ish ideas around data, UI, registries, entities, and systems that Witchery 1.7.10 could not express cleanly.
- `C:\claude\witchery\decompiled\intangible-1.8.9-0.0.25` is inspiration for development after a stable baseline. Use it for design direction, spirit/magic ideas, and future expansion, not for baseline Witchery parity.

## Modern API Reference

For NeoForge API shape, registration, data generation, rendering, sounds, and current best practices, use the official docs:

[NeoForge Documentation](https://docs.neoforged.net/)

## Working Guideline

When implementing a feature, compare in this order: original Witchery mechanics first, Resurrected visuals second, Companion/bugfix behavior third, modern NeoForge API last. If we intentionally choose a newer behavior, document the reason in the status docs so future fixes do not accidentally undo it.
