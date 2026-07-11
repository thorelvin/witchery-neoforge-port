# Source Notes For Vanilla Card v3

Runtime card facts do not include citations. These notes record research anchors and design caveats used for the v3 vanilla expansion pass.

## Research anchors

- Existing branch docs: local NPC knowledge cards are compact Markdown files under `src/main/resources/data/witchery/ai/cards/`, listed in `cards/index.txt`, retrieved by player wording, and phrased by the local model rather than treated as hard scripts.
- Existing branch docs: the LLM may phrase dialogue and help classify intent, but server code remains authoritative for trades, item grants, quests, commands, combat, terrain changes, and other game-state changes.
- Nether portals: Minecraft Wiki/Fandom Nether portal page. Key facts used: obsidian vertical frame, 4x5 minimum, 23x23 maximum, corners optional for player-built portals, activated by fire, Overworld/Nether only, portal linking, and 8:1 horizontal Nether travel.
- Eye of Ender and End portal: Minecraft Wiki/Fandom Eye of Ender page. Key facts used: Eyes of Ender are crafted from blaze powder and ender pearl, locate strongholds in the Overworld, can shatter, and End portals require all 12 frame blocks filled.
- Elytra: Minecraft Wiki/Fandom Elytra page. Key facts used: elytra are found in End ship treasure-room item frames, guarded by shulkers, and can be repaired with Mending or phantom membranes.
- Woodland mansions: Minecraft Wiki/Fandom Woodland Mansion page. Key facts used: mansions are large generated structures far from spawn in dark forest-like regions, found via woodland explorer maps from cartographers or locate commands if cheats are enabled, and contain evokers/vindicators/allays.
- Trial chambers: Minecraft Wiki/Fandom Trial Chambers page. Key facts used: underground copper/tuff structures, trial and ominous trial spawners, Breezes, vaults, keys, cartographer explorer maps, and locate support when commands are enabled.
- Ancient cities: Minecraft Wiki/Fandom Ancient City page. Key facts used: deep dark generation around Y -51, sculk and shriekers, Warden risk, unique loot, and wool/sound stealth.
- Villagers: Minecraft Wiki/Fandom Villager page. Key facts used: professions are tied to job-site blocks, cartographers use cartography tables, librarians use lecterns, villagers restock at job sites, and trade/profession locking matters.
- General vanilla cards: common Java Edition survival mechanics for current 1.21-era gameplay, with modpack/version caveats where mechanics often differ.

## Version caveats

The mod targets Java/NeoForge, so the cards avoid Bedrock-specific promises unless phrased broadly. Server configs, gamerules, modpack data, and Minecraft point releases can alter recipes, trades, structure locations, and command availability.

## Persona caveats

The card corpus is deliberately broader than any single NPC's knowledge. Use `persona_card_access_matrix.md` or `persona_card_access_matrix.json` to prevent accidental omniscience. Without hard filtering, the runtime boundary cards are only a soft prompt nudge.

## Safety caveats

Herb Latin-name cards are for in-game flavor and botanical naming only. They must not become real-world medical, dosage, ingestion, poison extraction, or preparation advice.
