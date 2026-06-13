# Porting Backlog — Milestone Roadmap

Last reconciled: 2026-06-13
Source: feature matrix audit + FTB wiki scraped-file gap analysis.

2026-06-13 reconciliation note: treat this file as the long-range milestone map; live status remains in `docs/status/FEATURE_MATRIX.md` and `docs/status/NEXT_TASKS.md`. Since the previous reconciliation, placed Modonomicon live pages/navigation, generic brew buckets, vanilla-shaped brew liquid flow, triggered cursed-block dispersal, source-ID Waystone/Blooded Waystone binding/copy/transposition, dropped bound/Blooded Waystone transposition, Player Compass needle/model parity, Crystal Ball Waystone/Taglock scrying, Ender Dew creative access, and Apothecary cauldron-jobsite/trade repair have landed. Remaining high-value follow-ups from this slice are Gas Mask/gas duration/source projectile transport and final scrying camera/apothecary reload QA.

---

## Milestone 6 — Sprint 3 polish (in progress)

Open items from the current sprint. All are small-to-medium code changes.

1. **Dream Weaver items** — 5 wall-hanging blocks (Fleet Foot/Iron Arm/Fasting/Intensity/Nightmares); scan nearby on `PlayerBedExitEvent` and apply buff.
2. **Kettle recipe roster** — ~30 original recipes still missing; JSON-only, no code changes.
3. **Tool / Armor protection poppets** — 2 more poppet types in `PoppetEvents`.
4. **Werewolf howling** — sound + Strength/Speed burst + mob-fear on right-click while transformed.
5. **Circle Talisman** — right-click circle center to store pattern; right-click elsewhere to replay.

---

## Milestone 7 — Curse System + Waystone Teleportation

Two tightly related systems (curses target players via taglocks; waystones provide coordinates for those same taglock-based rites).

### Waystone system
- `Waystone` item (craftable) → used in **Rite of Binding** → produces `Bound Waystone` (coordinate-bound) or `Blooded Waystone` (mob/player-bound via taglock).
- **Rite of Transposition** — 3 variants, all dispatched via `RitualExecutor`:
  - 7×7 small: teleport casting witch to Bound Waystone coordinates (0 altar power).
  - 11×11 large + taglock: pull stored mob/player to witch's location (3000 altar power); 90% blocked by full Witch Hunter armor.
  - Large: transpose iron ore in surrounding area (ore-conversion variant).

2026-06-13 status: blank/bound/blooded Waystone IDs are live; bind/copy rites output distinct `bound_waystone`; Blooded Waystone support routes output distinct `blooded_waystone`; small transposition to a bound waystone and medium taglock transposition are restored; dropped blank Otherwhere auto-binding is live; dropped bound/Blooded Waystones now consume one stone on a small Otherwhere ring and transpose nearby living entities plus loose item stacks; Crystal Ball scrying works with complete Bound Waystones, Blooded Waystones, and filled Taglock Kits; Player Compass binding uses player/taglock data and now has source-style 32-frame needle models.

### Curse system (12 curses)
All curses are circle rites using taglock kits. Wire into `RitualExecutor` dispatch.

| Curse | Circle | Power | Special condition |
|---|---|---|---|
| Blindness | 7×7 | 0 | Area blindness |
| Blight | 7×7 | — | Wither effect |
| Misfortune | 7×7 | — | Bad luck |
| Overheating | 7×7 | — | Fire damage loop |
| Sinking | 7×7 | — | Gravity/slowness |
| Insanity | 7×7 | — | Nausea/confusion |
| Raining Toads | 7×7 | — | Entity spawn rain |
| Waking Nightmare | 7×7 | — | Force nightmare state |
| Corrupt Poppet | 7×7 | — | Disable victim's poppet |
| Hell on Earth | 11×11 | high | Fire + demon spawn |
| Creatures of the Night | 11×11 | — | Hostile mob spawn wave |
| Curse of the Wolf | 11×11 | 10000 | Full moon + cat familiar + taglock; permanently werewolves victim; survives death |

Counter: all curses negated by **Voodoo Protection Poppet**; cat familiar counters one layer; second Voodoo poppet counters cat.

### Wolf Altar progression (ties in here)
- `WolfAltarBlock` — werewolf player right-clicks to pray → receives quest objectives from wolf god.
- Quests grant progression milestones (stronger howl, pack behavior, etc.).
- Wire into existing `WitcheryPlayerData` werewolf flag.

---

## Milestone 8 — Broom Flight + Infused Brews

### Enchanted Broom
- **Flying Ointment** — new kettle brew: Swiftness Potion (8-min) + ingredients, 3000 altar power; brew is poisonous (do not drink).
- **Rite of Infusion** — use Flying Ointment as ingredient → converts `BroomItem` into `EnchantedBroomItem`; check if current broom entity already supports riding (wire if missing).
- **Enchanted Broom** gives full free-flight movement (not just gliding).

### Infused Brew system
- `InfusedBrewBaseItem` — base potion item.
- **Infused Brew of Soaring** — 7×7 circle, Owl familiar required, 1000 altar power; effect: improved broom top speed + turning + deceleration for 120 min server-time; stacks only with owl familiar, not self.
- **Infused Brew of the Grave** — details in decompiled source; check `WitcheryRecipes.java`.

---

## Milestone 9 — Hobgoblin Ecosystem

Self-contained new feature block with its own mob, structure, material chain, and boss events.

### Hobgoblin mob
- Spawns in forest biomes; 20 HP, 3 attack; hostile to villagers; neutral to players.
- Falls do no damage (immune to fall damage).
- Trades: scrounged items (see wiki trades table); open trading screen on right-click.

### Koboldite material chain
- `KobolditeOre` / `KobolditeChunk` (rare underground or as hobgoblin drop).
- `KobolditeDust` → `KobolditeIngot` / `KobolditeNugget` — only Hobgoblins can smelt dust (special smelting event or custom recipe gated behind hobgoblin AI).
- `KobolditPickaxe` — end-product tool.

### Lead + Hobgoblin behavior
- Right-click Hobgoblin with lead → tethers it; it now picks up loose items on ground and deposits in nearby chests.
- Right-click tethered Hobgoblin with pickaxe → it mines, occasionally finding Koboldite Dust.

### Statue of Hobgoblin Patron
- `StatueHobgoblinBlock` (BESR rendered block) — within detection range of a Hobgoblin, it may pray there instead of natural location.
- Stealing worship: may trigger **Gulg** or **Mog** avatar spawn (boss-tier hostile event).

### Hobgoblin Hut structure
- Small forest structure; 1–2 Hobgoblins resident; chest with koboldite/junk loot.

---

## Milestone 10 — Flying Monkey + Silver Chain + Armor Sets

### Flying Monkey mob
- Does not spawn naturally — created by mutating a wolf + owl (via Mutandis or ritual).
- 5 HP, 1 damage; neutral.
- Tamed with Raw Beef or Raw Porkchop; skittish like ocelot (approach slowly).
- Healed and bred the same way when tamed.
- Give tamed Flying Monkey a **Bound Waystone** → it picks up and carries the player to those coordinates.
- Dyeable (16 colors, stored in NBT).
- Drops: Feather.

### Silver material chain
- **Silver Vat** — block that processes Silver Deposits (ore equivalent) into silver items.
- `SilverIngot`, `SilverNugget` outputs.
- **Silver Sword** — bonus damage to werewolves.
- **Silver Bolt** — crossbow ammo for Witch Hunter, effective vs werewolves.
- **Nullifying Bolt** — variant; check decompiled for effect.

### Armor sets
- **Vampire armor** — Chain Coat (male + ladies), Dress Coat (male + ladies), Oxford Boots, Skirted Trousers, Trousers, Top Hat, Helmet; craft from silver/leather/Witchery materials.
- **Witch Hunter Dawn set** — Hat/Coat/Trousers/Boots; upgraded Witch Hunter variant.
- **Witch Hunter Silvered set** — standard WH armor re-forged with silver; extra werewolf resistance.
- **Necromancer Robes** — cloth set; buffs for conjuration/undead rites.
- **Death's outfit** — Hood, Robe, Footwear; already dropped by Death boss, check if armor items exist as wearable.

---

## Milestone 11 — Fetishes + Conjuration + Coffin

### Fetishes (FetishBlock already exists — extend dispatch)
- **Disorientation** — disorients nearby hostile mobs.
- **Ghost Walking** — player phases through blocks while near it.
- **Sentinel** — alerts owner when triggered.
- **Shrieking** — screams when mobs approach (alarm).
- **Voodoo Protection** — nullifies curses targeting the owner (pairs with Voodoo Protection Poppet).

### Conjuration spells
All are placeable/castable witchcraft actions, not rites.

| Spell | Effect |
|---|---|
| Anguish of the Dead | Undead mobs in area writhe/take damage |
| Deathly Veil | Caster becomes temporarily invisible to undead |
| Drain Growth | Siphons growth energy from crops/plants |
| Fortification of the Corpse | Buffs undead allies with resistance |
| Graveyard Mist | Spawns lingering fog; hides players in area |
| Summon Banshee | Summon a Banshee to fight for caster (ritual) |
| Summon Spectre | Summon a Spectre to fight for caster (ritual) |

### Coffin block
- `CoffinBlock` — 2-block-tall horizontal block; vampire can sleep during day to avoid sunburn.
- Right-click to open/enter sleep state; exits automatically at night.
- Non-vampires can also sleep in it (acts as bed in Spirit World?).

---

## Milestone 12 — Decorative + Ice Blocks + Door System

Low-code-complexity fills that complete the block registry.

### Ice blocks set
Perpetual Ice, Ice Fence, Ice Gate, Ice Pressure Plate, Ice Slab, Ice Stairs, Ice Stockade, Icy Slippers item.  
All crafted from Perpetual Ice; standard Minecraft block family (fence/slab/stair patterns).

### Rowan door + key system
- `RowanWoodDoor` block (already has texture).
- `RowanDoorKeyItem` + `RowanKeyringItem` — right-click with matching key to open/lock rowan door; keyring holds multiple keys.
- Anti-intrusion mechanic for witch huts / player builds.

### Alder Wood Door
- Standard door, Alder wood family (already has log/leaves textures).

### Misc decorative
- `ScarecrowBlock`, `TrentEffigyBlock` — static decorative blocks with minor mob-repulsion auras.
- `SpecterStoneBlock`, `NecroStone` — altar-type decor blocks.

---

## Milestone 13 — Guidebook + Polish

- **Patchouli/Modonomicon guidebook** — `Witches' Primer`; one entry per major branch with recipe images; auto-unlock entries as player progresses. 2026-06-13 status: the placed Primer block now projects live generated Primer pages directly onto the Hexerei-style open book, including entry index, gating, entry-click navigation, back-to-entry button, and Primer-matching page item renders.
- **Sound pass** — replace placeholder sounds with proper Witchery sounds from original jar.
- **Particle pass** — altar glow, kettle bubbles, ritual activation, curse cast effects.
- **More rites** — push toward 80+ total; remaining missing: Nature's Power, Earth's Wrath, Sky's Wrath, Necromancy, Banishing, Sanctity fill, etc.
- **Wild plant polish** — herb shop restocking loop in CovenWitch.
- **JEI/EMI integration** — custom recipe categories for Kettle, Distillery, Spinning Wheel, Brazier.

---

## Post-port expansion (Milestone 14+)

See `docs/roadmap/FUTURE_FEATURES.md` — do not implement during faithful-port work unless explicitly requested.

- Mirror World / reflection system
- Soul essence + storage jar
- Spirit walking / possession
- Soulless shells
- Deity powers (Gulg/Mog foundations laid in M9)
- Modern ritual grammar (data-driven ritual registry)
