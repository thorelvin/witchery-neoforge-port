# Future Features — Post-Port Expansion

These ideas are for after the faithful 1.7.10-style port is stable. Do not implement during normal port tasks unless explicitly asked.

**Current scope note:** start with [CLAUDE_HANDOFF_CURRENT_SCOPE.md](./CLAUDE_HANDOFF_CURRENT_SCOPE.md). This file is backlog-only and should not be used to widen current implementation scope.

## Rule

Use these as design inspiration, not as current sprint work.

## Confidence labels

| Label | Meaning |
|---|---|
| `confirmed` | Supported by public project/changelog/author description. |
| `strong inference` | Strongly implied by official descriptions or unfinished systems. |
| `community-reported` | Mentioned by players; useful but not official. |
| `speculative` | Good design direction, not confirmed. |
| `myth` | Treat as folklore unless verified. |

## Expansion backlog

### 1. Deity Powers System

**Confidence:** confirmed for unfinished Intangible deity powers; speculative as Witchery expansion.

Players create, bind, empower, bargain with, or oppose insubstantial godlike entities.

Minimal version:
- one shrine block
- one deity JSON
- favor per player
- three offerings
- two boons
- one wrath event

### 2. Soul Extraction and Essence Storage

**Confidence:** confirmed Intangible theme; strong rewrite fit.

Add typed essences:
- life
- beast
- undead
- infernal
- fey
- echo/identity

Minimal version:
- extraction item
- storage jar
- three essence types
- one recipe per essence

### 3. Spirit Walking / Possession

**Confidence:** confirmed Intangible theme; speculative implementation.

State machine:
```text
NORMAL_BODY -> TRANCE_ENTERING -> SPIRIT_FORM -> POSSESSION_ATTEMPT -> POSSESSING_ENTITY -> RETURNING
```

Minimal version:
- anchor block
- timed spirit form
- one soulless shell possession target

### 4. Soulless Shells / Artificial Bodies

**Confidence:** confirmed Intangible theme.

Shell types:
- clay
- flesh
- bone
- wood
- iron
- mirror

Minimal version:
- one shell entity
- one binding ritual
- follow/protect owner behavior

### 5. Spirit-Powered Devices

**Confidence:** confirmed Intangible theme; speculative implementation.

Examples:
- spirit jar
- bound lantern
- soul bell
- dream loom
- omen engine
- warding nail

Minimal version:
- spirit jar stores charge
- lantern consumes charge to reveal hidden spirits/entities
- bell detects curses/spirits

### 6. Soul-Modified Gear

**Confidence:** confirmed Intangible theme; speculative design.

Examples:
- wolf-bound boots
- bat-bound cloak
- spider-bound gloves
- golem-bound chestpiece
- wraith-bound hood

Implement as a data-driven `InfusionModifier` registry.

### 7. World-Influencing Insubstantial Beings

**Confidence:** confirmed Intangible direction; speculative details.

Persistent spirits that affect chunks/regions:
- forest spirit
- hunger spirit
- mirror echo
- storm wraith
- dream parasite

Minimal version:
- one invisible region entity or level saved-data object
- one chunk-radius effect
- one shrine/ritual to create/remove it

### 8. Modern Ritual Grammar

**Confidence:** speculative modernization.

Move from hardcoded rites to composable ritual definitions:
- circle size/type
- chalk type
- moon/weather/dimension
- offerings
- target rules
- effect handlers

Do this only after the current hardcoded system is stable.

#### Ritual of Tracing

**Confidence:** speculative user-requested future ritual.

A Spirit World investigative rite that lets the caster see traces of all player footsteps as fading spectral footprints or glyph trails. The core fantasy is that the Spirit World becomes a hidden record of movement through the living world.

Design intent:
- Cast from a ritual circle, then enter or view through the Spirit World.
- Reveal recent player footsteps from all players as fading spectral traces.
- Keep traces visible only in the Spirit World or while under a tracing vision effect, not in ordinary Overworld vision.
- Use a configurable retention window and multiplayer privacy/server-safety toggle before any public-server release.

Implementation sketch:
- Record compact per-player footstep samples in world `SavedData` or a chunk-local attachment with age, dimension, position, and player identity display rules.
- Prune samples by age and density so busy servers do not accumulate unbounded traces.
- Render traces client-side as ghostly footprints, motes, or chalk-like floor glyphs while the caster has the rite effect.
- Consider masking exact identity unless the caster has a filled Taglock Kit or stronger offering, so the baseline rite tracks movement while advanced use identifies whose trace it is.

### 9. Mirror / Identity Expansion

**Confidence:** original late Witchery feature + speculative expansion.

Future system:
- mirror pockets
- reflection boss
- identity disguise via taglocks
- mirror network teleport
- reflection decoys

### 10. Secret Knowledge / Symbol Magic

**Confidence:** community-reported/original obscure mechanics.

Use hidden spell sequences, symbol input, or puzzle knowledge:
- secret curse sequences
- forbidden book clues
- hidden chants
- rare ritual variants

### 11. Emoniph NPC — The Witch-Architect

**Confidence:** speculative (original creator tribute); user-requested.

A rare, named NPC who wanders the overworld or inhabits a hand-built tower or cave lair. Clearly knowledgeable about the craft — possibly the author of the Witches' Primer itself. Not hostile unless provoked. Carries unusual trades and can teach rites not found elsewhere.

**Lore concept:**
Emoniph is credited in-world as the scholar who codified the craft. Whether the figure is human, something that was once human, or something that merely wears the shape is left deliberately unclear. The tower smells of old paper and very serious mistakes.

**Design pillars:**
- Rare overworld spawn (~1 per large region, or tied to a specific biome like dark forests or extreme hills)
- Has a **personal structure** — either a Wizard's Tower (tall, multi-floor, bookshelves + brewing apparatus) or a Cliff Cave Lair (carved into a hillside, hidden entrance)
- Dialogue acknowledges the player's witchcraft progression (checks player data for infusion type, familiar, transformations)
- **Trades** (3–5 unique): sells rare spell ingredients, a copy of the Witches' Primer, a one-of-a-kind rite scroll not available elsewhere
- Optional: gives a **quest** — retrieve a specific item → unlock a hidden rite or receive a powerful one-time reward

**Structure ideas:**

*Wizard's Tower* — 5×5 footprint, 4 stories of stone brick + mossy stone, pointed roof, lantern-lit windows; ground floor = entrance + storage; floor 2 = brewing station (kettle + distillery + oven); floor 3 = ritual room (circle drawn on floor + altar); roof = observatory with crystal ball

*Cliff Cave Lair* — Natural cave entrance (hidden behind vines or a fake wall), carved-out chambers, bioluminescent plants (wispy cotton, glint weed), a single candle-lit reading desk with a Witches' Primer, hidden chest with rare ingredients

**Entity design:**
- Extends `AbstractVillager` or custom `NPC` (similar to Coven Witch pattern)
- Name: always "Emoniph" (single unique spawn per world, stored in world SavedData)
- Model: robed humanoid; distinct long-coat texture, slightly unusual proportions
- Attacks if the player has a fully hostile transformation active (e.g. mid-werewolf-change) — otherwise peaceful
- Drops: worn robe (cosmetic), a partially-filled Witches' Primer with extra annotations

**Implementation notes:**
- Use `WitcheryPlayerData` checks in dialogue to personalise: "I see you've bound a familiar. Interesting choice." / "A vampire who studies the craft. Rarer than you'd think."
- Structure uses NBT-based jigsaws or a hand-crafted `StructurePiece` like `HobgoblinHutPiece`
- One spawn per world: flag in a `WitcheryWorldData` (SavedData) to prevent duplicates
- Relevant decompiled ref: `entity/EntityCovenWitch.java` (trading pattern to reuse)

**Minimum viable version:**
1. Entity: named NPC with 3 trades, unique texture, peaceful/hostile toggle
2. Structure: Wizard's Tower (simpler to generate than cave)
3. One-per-world enforcement via SavedData flag
4. Primer dialogue that reads player infusion type

### 12. Crystal Ball — Extended Prediction Roster

**Confidence:** speculative; these are original-feeling expansions, not confirmed original Witchery content.

The original game documents 14 Crystal Ball predictions. The following 11 additional predictions were cut from the Book of Shadows (they were not confirmed by wiki sources) but are reasonable expansions of the system. Each maps to an existing mod mechanic.

| Prediction Text | Intended Consequence |
|---|---|
| "Will find coal" | Coal ore placed nearby (Transposition variant) |
| "Will find diamonds" | Diamond ore placed nearby |
| "Will find emeralds" | Emerald ore placed nearby |
| "Will have visitors" | Villagers spawn near the player |
| "Will be visited by demons" | Demons spawn near the player |
| "Will feel the cold touch of death" | Spectres or similar undead appear |
| "Will be harassed by the small folk" | Hobgoblins spawn and target the player |
| "Will be filled with doubt and uncertainty" | Insanity-like disorientation effect applied |
| "Will be visited by a spectral presence" | Spectres, Banshees, or Poltergeists appear |
| "Will receive a gift from the goddess" | Items or blessings drop at the player's feet |
| "Will be inundated by nature" | Plants and natural growth appear around player |

Implementation note: predictions are dispatched from a single random-pick handler. Adding new entries is a matter of registering the message string and a consequence lambda — low effort once the base system is stable.

### 13. Dried Branch — Altar Power Tell

**Confidence:** speculative; user-requested future altar-room polish, not confirmed original Witchery content.

A wall-attached dried branch that behaves like a quiet visual indicator for the nearest linked altar. It should read as a natural witch-room ornament first and a power gauge second.

Design intent:
- Attaches to walls as a small twig/branch model.
- Searches for the nearest valid altar using the same altar lookup conventions as other altar-aware blocks.
- Points upward when the nearby altar is full or nearly full.
- Hangs level or half-raised at medium power.
- Droops downward when altar power is low or empty.
- Updates slowly or with interpolation so it feels like a living twig responding to power rather than a redstone needle.

Implementation notes:
- Treat as a decorative utility block, not an altar power contributor unless a separate balance pass approves it.
- Prefer a small blockstate set such as `pose=low,mid,high` driven by server-side altar power ratio and synced to clients.
- Keep the model wall-facing, with north/east/south/west variants and a compact outline.
- Consider faint leaf-rustle or creak particles only for transitions; no constant noise.
- If no altar is found, default to drooped.

Minimum viable version:
1. One craftable wall-mounted Dried Branch block.
2. Three poses based on nearest altar current-power / max-power ratio.
3. No altar bonus, no GUI, no redstone output.

## Recommended order after port

1. Stabilize port + replace placeholder assets.
2. Mirror World and reflection system.
3. Soul essence foundation.
4. Spirit devices and shell entity.
5. Possession MVP.
6. Deity alpha.
7. World-spirit regional effects.
8. Altar-room feedback ornaments such as Dried Branch.
