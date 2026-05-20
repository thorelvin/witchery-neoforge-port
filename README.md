# witchery-neoforge-port

An unofficial modern NeoForge port and continuation of the classic Witchery mod for Minecraft `1.21.11`.

## Status

The port is already playable and well beyond the early proof-of-concept stage. Current work prioritizes original Witchery `1.7.10` mechanics first, then Witchery: Resurrected visuals and models where they improve parity.

Implemented major systems include:

- altar power
- kettle brewing
- witches oven
- distillery
- spinning wheel
- poppets and taglocks
- ritual circles and a broad rite roster
- vampirism, lycanthropy, and familiars
- Spirit World gameplay loop
- Torment dimension with the restored legacy-style maze flow
- core traders, structures, and worldgen content

Project status and current bug tracking live in:

- `docs/status/FEATURE_MATRIX.md`
- `docs/status/BUGS.md`
- `docs/status/ITEM_REVIEW.md`
- `docs/status/NEXT_TASKS.md`

## Debug / QA Helpers

The mod includes a growing set of in-game debug commands under `/witchery`.

Useful current helpers:

- `/witchery setup stations` clears a `20x20` pad in front of the player and rebuilds a fully stocked `11x11` Rowan QA hut 5 blocks ahead, with the shell sunk one block lower, ceiling Glowglobes, cobblestone kettle supports, and the altar/machines/chest/traders kept at player height for faster testing
- `/witchery verify torment` runs a server-side Torment audit against the restored legacy maze footprint and atmosphere settings
- `/witchery spawn wickerman` places a summon-valid bloodied wickerman for Horned Huntsman testing

## Build

```bash
./gradlew compileJava processResources
```

## Notes

- This is a fan port inspired by the original Witchery mod by Emoniph.
- Original Witchery behavior and decompiled reference material are used as porting reference where needed.
