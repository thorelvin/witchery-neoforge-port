# Witchery AI Card Expansion Draft v3

This companion bundle contains copy-paste-ready Markdown cards for the local NPC knowledge system.

## What v3 adds

- Expands the v2 set from 400 targeted facts to **786 runtime targeted facts** across **43 runtime card files**.
- Adds a persona knowledge model so NPCs are knowledgeable without becoming omniscient.
- Adds `persona_card_access_matrix.md` and `persona_card_access_matrix.json` to answer which persona gets which cards.
- Adds deeper vanilla Minecraft knowledge: maps, coordinates, commands, structures, Nether, End, elytra, resources, tools, farms, animals, redstone, storage, mobs, biomes, potions, enchantments, and first-hour survival.
- Adds explicit rumor/scoped/exact boundaries for villagers, guards, hobgoblins, flame imps, demons, bosses, reflections, apothecaries, and Coven Witches.
- Moves the old runtime TODO card into root-level `future_card_slices.md` so "todo" facts do not leak into NPC speech.

## Runtime path

Copy card files from:

```text
src/main/resources/data/witchery/ai/cards/
```

into the same path in the repo, then add the filenames listed in `index_append.txt` to `cards/index.txt`.

## Persona access rule

Use the companion matrix before retrieval or prompt assembly:

```text
persona_card_access_matrix.md
persona_card_access_matrix.json
```

Access codes:

- `E` means exact mechanics are allowed.
- `S` means scoped to the persona's profession or scene.
- `R` means rumor/referral only.
- `N` means do not inject that card for that persona.

The important design decision is that **Coven Witches can be broadly knowledgeable**, while ordinary villagers should know local life, trades, maps, and rumors. Villagers should not casually explain Nether fortress routing, End progression, elytra acquisition, Witchery rites, or demon work unless a profession-specific exception applies.

## Mythic dial

Use **mythic veneer, mechanical spine**.

A witch can say, "The void remembers falling," but still needs to tell the player that elytra are found in End ship treasure-room item frames. A villager can say, "My cousin heard of gray wings past the dragon," but should not give the full End-city route unless hard persona routing says this specific villager can.

## Suggested review slices

1. Add persona routing files and matrix; test that villagers answer End/Nether questions as rumor and witches answer as exact guidance.
2. Add `general_worldcraft_knowledge.md`, maps/coordinates, and vanilla survival cards; test beginner questions.
3. Add Nether and End deep packs; test Coven Witch vs villager answers for portals, strongholds, elytra, and shulkers.
4. Add structures deep pack; test mansion, ancient city, trial chamber, monument, and archaeology queries.
5. Add resources, farming, redstone, potions, and enchantment packs; tune retrieval caps if answers become too broad.
6. Keep `future_card_slices.md` outside `cards/index.txt`; convert future items only after exact verification.

## Included support files

| File | Purpose |
| --- | --- |
| `index_append.txt` | Proposed runtime index additions. |
| `card_manifest.md` | Counts per runtime card file. |
| `persona_card_access_matrix.md` | Human-readable persona/card assignment. |
| `persona_card_access_matrix.json` | Machine-readable persona/card assignment. |
| `source_notes.md` | Research anchors and version caveats. |
| `future_card_slices.md` | Planning-only future expansion notes, not runtime cards. |
| `VALIDATION_OK.txt` | Validation report for card-line shape and topic metadata. |
