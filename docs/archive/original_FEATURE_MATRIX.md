# Feature Matrix — Witchery NeoForge 1.21.1 Port

Legend: ✅ Done · 🔄 Partial · 🔲 Not started · ⏸ Deferred

Last updated: 2026-05-15

---

## Branch 1: Brewing

| Feature | Status | Notes |
|---|---|---|
| Kettle block + tick | ✅ | Power from nearby altar |
| Kettle recipe system | 🔄 | 10 of 30+ recipes in JSON |
| Brew items (13 types) | ✅ | Sleeping/Frost/Love/Revealing/Raising/Wasting/Erosion/Foulness/Infection/Grotesque/FlowingSpirit/Foaming/Poison |
| Witches Oven | ✅ | Log→Ash, quartz→quicklime, nether wart→exhale |
| Distillery | ✅ | 2-input fuel-based; 4 recipes (soup, vapour, bat wool) |
| Spinning Wheel | ✅ | Cotton→thread, berries→thread |
| Altar power system | ✅ | Scans 5-block radius; 9 power sources |
| Brazier recipes | 🔲 | Block exists, no recipe system |
| Ingredient items | 🔄 | ~40 of 70+ registered; most need kettle recipes |
| Mutandis / Mutandis Extremis | 🔲 | Items registered, no effect |
| Brew of Ice / Depths / Vines | 🔲 | Distillery outputs |
| Books (5 types) | 🔲 | Registered, no content |

---

## Branch 2: Circle Magic

| Feature | Status | Notes |
|---|---|---|
| Chalk item | ✅ | 64 durability, places circle blocks |
| Circle detection (3 sizes) | ✅ | SMALL 3×3, MEDIUM 5×5, LARGE 7×7 |
| Ritual execution | ✅ | 20+ rites dispatched by size + ingredient |
| **Small circle rites** | ✅ | Infusion (4 types), Bind Familiar (owl/cat/toad), Blessing, Total Eclipse, Beastial Call, Transposition, Prior Incarnation, Cloak, Cleansing |
| **Medium circle rites** | ✅ | Summoning, Fertility, Charging, Creatures of Night (vampirism), Sky's Wrath, Sanctity, Imprisonment, Infernal Power, Silent Step |
| **Large circle rites** | ✅ | Forest, Broiling, Moving Earth, Broken Earth, Protection, Curse of the Wolf (lycanthropy), Dragon's Fury, Rain, Manifestation (Dream World), Hellgate (Torment) |
| Chalk variants (otherwhere/infernal/golden) | 🔲 | — |
| Glyph blocks (otherwhere/infernal) | 🔲 | — |
| Full 87-rite roster | 🔄 | ~22 rites implemented |

---

## Branch 3: Poppet Magic

| Feature | Status | Notes |
|---|---|---|
| Poppet binding (taglock) | ✅ | Right-click player, UUID stored in CustomData |
| TaglockKitItem | ✅ | Collects UUID+name |
| Death Protection Poppet | ✅ | Any death → 2 hearts, totem particles |
| Earth Protection Poppet | ✅ | Fall/fly-into-wall damage |
| Water Protection Poppet | ✅ | Drowning |
| Fire Protection Poppet | ✅ | Fire damage tag |
| Hunger Protection Poppet | ✅ | Starvation |
| Voodoo Protection Poppet | ✅ | Magic/bypass-armor damage |
| Poppet Protection Poppet | ✅ | Shields other poppets from consumption |
| Vampiric Poppet | ✅ | Redirects 50% damage to bound player |
| Voodoo Poppet | ✅ | Right-click → Poison II + Weakness on bound player |
| Poppet Shelf | ✅ | 9-slot vanilla ChestMenu; 8-block radius scan on death |
| Basic Poppet (sympathetic) | 🔲 | Registered, no logic |
| Curse of Corrupt Poppet | 🔲 | Requires cat familiar |

---

## Branch 4: Dream Weaving

| Feature | Status | Notes |
|---|---|---|
| Dream World dimension | ✅ | End-style floating islands; eerie sky; Banshee/Spectre/Poltergeist spawn |
| Dream World entry | ✅ | `/witchery dream` or large ritual + Flying Ointment |
| Dream World exit | ✅ | `/witchery exit` returns to stored position |
| Dream Weaver items (5 types) | 🔲 | Wall-hanging blocks, wake effects |
| Rite of Manifestation (spectral form) | ⏸ | Original was overworld spectral, not dimension portal |
| Crystal Ball scrying | 🔲 | Block+BESR exist; no logic |
| Nightmare entity | 🔲 | Model planned |

---

## Branch 5: Infusions / Transformations

| Feature | Status | Notes |
|---|---|---|
| Player data system | ✅ | AttachmentType + CODEC; synced to client |
| Infusion types (4) | ✅ | Light/Overworld/Otherwhere/Infernal — kettle brews + Rite of Infusion |
| Infusion passive effects | ✅ | InfusionEvents tick every 100t |
| Vampirism | ✅ | Bite, blood drain/thresholds, sunburn, night vision, speed/strength |
| Lycanthropy | ✅ | Speed III/Strength II/Jump always when werewolf; wolfsbane suppresses; silver bonus damage |
| Werewolf 3D model | ✅ | WerewolfLayer on PlayerRenderer; hides player body; 1.1× scale |
| Werewolf — howling sound | 🔲 | — |
| Familiar binding | ✅ | Ritual binds owl/cat/toad; UUID stored in PlayerData |
| Owl familiar | ✅ | Warns of nearby monsters every 100t |
| Cat familiar | ✅ | Avoids phantoms/creepers |
| Toad familiar | ✅ | Collects nearby dropped items |
| Creature powers / symbiosis | 🔲 | 20+ powers; none implemented |

---

## Entities

| Entity | Status | Notes |
|---|---|---|
| Coven Witch | ✅ | AbstractVillager; 5 skins; random name; 10 trades; chat dialogue |
| Witch Hunter | ✅ | Ranged+melee; targets vampires/werewolves/witches |
| Banshee | ✅ | Flying; screams near low-HP players; robed ghost model |
| Spectre | ✅ | Flying; fire-immune; reaching-arms ghost model |
| Poltergeist | ✅ | Flying; hurls eggs; ghost model |
| Treefyd | ✅ | Ground; aggressive; plant model |
| Ent | ✅ | Ground; passive until hit; large tree model |
| Mandrake mob | ✅ | Tiny; fast; poison on hit |
| Demon | ✅ | Large; throws fireballs |
| Owl familiar | ✅ | — |
| Cat familiar | ✅ | — |
| Toad familiar | ✅ | — |
| Broom | ✅ | Rideable; player steers with look direction |
| Nightmare | 🔲 | — |
| Lord of Torment | 🔲 | — |
| Imp | 🔲 | — |
| Death entity | 🔲 | — |

---

## Dimensions

| Dimension | Status | Notes |
|---|---|---|
| Dream World | ✅ | End-style noise; custom biome; ghost mobs spawn |
| Torment | ✅ | Nether-style noise; custom biome; demons spawn |

---

## Worldgen & Structures

| Feature | Status | Notes |
|---|---|---|
| Rowan tree (witch sapling) | ✅ | TreeGrower + placed_feature JSON |
| Rowan berries drop | ✅ | Via loot table |
| Witch hut structure | 🔲 | — |
| Apothecary hut | 🔲 | — |
| Wild plant spawning | 🔲 | — |

---

## Debug / QoL Commands

| Command | Status |
|---|---|
| `/witchery werewolf [player] [bool]` | ✅ |
| `/witchery vampire [player] [bool]` | ✅ |
| `/witchery blood <amount>` | ✅ |
| `/witchery infusion <type>` | ✅ |
| `/witchery fullmoon` | ✅ |
| `/witchery clear` | ✅ |
| `/witchery status` | ✅ |
| `/witchery spawn <entity>` | ✅ All 12 entities |
| `/witchery dream` | ✅ |
| `/witchery torment` | ✅ |
| `/witchery exit` | ✅ |
