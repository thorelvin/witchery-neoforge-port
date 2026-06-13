# Obscure Mechanics Reference

Use this as inspiration and as a checklist for later faithful-port edge cases. Mark uncertain mechanics clearly in code/docs.

## Confidence labels

- `confirmed`: public changelog/wiki/behavior source supports it.
- `community-reported`: player reports; verify in-game/code before treating as canonical.
- `myth`: do not implement as original behavior unless independently verified.
- `speculative`: design idea inspired by original behavior.

## Mechanics

### Avada Kedavra / Kill Curse

**Confidence:** community-reported / obscure.

Reported as a Symbology spell sequence. If implemented, treat it as a hidden high-cost/late-game curse with strong counterplay.

Implementation notes:
- Do not make it a casual instant-kill button.
- Require rare setup, taglock/line-of-sight/risk, or ritual backlash.
- Add config to disable on servers.

### Lilith vampire leash / overworld trick

**Confidence:** community-reported.

Rumor: vampire progression boss/event could be manipulated outside the intended Nether context. Treat as bug/edge case unless verified.

Implementation notes:
- Prefer robust boss state machine over reproducing a cheese exploit.
- Optional: add a deliberate “ritual relocation” method as a modern feature.

### Doomcat

**Confidence:** myth.

Reported as hidden transformation/ritual, but evidence is weak. Good as an easter egg or future myth quest, not a faithful-port requirement.

### Mirror World / Reflection Demon

**Confidence:** confirmed late Witchery feature.

Core behavior to eventually port:
- Magic mirror bound to a demon.
- Player can enter mirror.
- Reflection/demon encounter inside.
- Claim/control mirror after defeating reflection.
- Escape failsafe/chant.
- Taglock/identity disguise features.
- Mirror teleportation links.

### Cauldron custom brews as traps

**Confidence:** confirmed/strong.

The custom brew system could apply effects to doors, buttons, levers, pressure plates, and items. This is a high-value later system.

2026-06-13 port status: first practical triggered-dispersal parity is live. Triggered brews can curse dropped items and can store invisible saved-data curses on buttons, levers, pressure plates, and wooden doors without replacing the block, preserving modern wood/block variants. Remaining follow-up is the old ritual item-curse entry point and broader dynamic action coverage.

Implementation notes:
- Design brew effects as data-driven handlers.
- Separate effect, dispersal, modifier, and trigger.

### Cauldron altar-power multiplier / ritual-circle cost gotcha

**Confidence:** confirmed by changelog/community reports.

Cauldron recipes could fail when actual power cost differed from what JEI/NEI implied, especially without circles.

Implementation notes:
- Keep power UI/tooltips honest.
- Add debug messages or guidebook hints.

### Minedrake creation

**Confidence:** documented but obscure.

Mutation-style mob/plant creation through spatial setup and special items. Good template for modern mutation puzzles.

Implementation notes:
- Use spatial recipe validator.
- Make failures readable with particles/tooltips.

### Tormentum / sending players to Torment

**Confidence:** semi-documented/late-game.

Useful as a late-game effect unlocked from Lord of Torment progression.

Implementation notes:
- Add strict server config and permissions for player-vs-player use.
- Provide escape/counterplay.

### Waystone chalk-circle behavior

**Confidence:** confirmed/original behavior family.

Waystones, blooded waystones, and chalk circles connect teleportation/identity/location systems.

Implementation notes:
- Use item data components for bound positions/dimensions.
- Keep cross-dimension teleport server-authoritative.

### Modpack-only vampire items

**Confidence:** changelog-supported.

Some vampire items may have existed primarily for map/modpack/server use rather than normal survival crafting.

Implementation notes:
- Put such items behind commands/creative/config until progression is designed.
