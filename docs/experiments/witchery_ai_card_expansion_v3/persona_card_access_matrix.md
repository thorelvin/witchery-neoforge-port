# Persona Card Access Matrix

This matrix answers: **which persona gets which cards?**

Access codes:

- `E` = exact: may answer direct mechanics from these cards.
- `S` = scoped: may answer only within profession, scene, or local experience.
- `R` = rumor: may mention hearsay, danger, or referral, but should avoid step-by-step precision.
- `N` = no: do not inject these cards for that persona except as an explicit refusal/boundary fallback.

Implementation recommendation: apply this matrix before retrieval or before prompt assembly. Prompt-only restrictions will not reliably stop a small local model from making every NPC omniscient.

| Card file | Coven Witch | Apothecary | Villager | Guard | Hobgoblin | Flame Imp | Demon/Boss | Reflection | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `general_worldcraft_knowledge.md` | E | S | E | E | S | S | R | S | Common survival knowledge; persona voice changes certainty. |
| `herb_latin_names.md` | E | E | R | N | R | N | S | S | Botanical names with poison-safety boundaries. |
| `lore_rumor_mythic_style_cards.md` | S | S | S | S | S | S | S | S | Style tuning; exact mechanics still require a matched fact card. |
| `persona_card_access_matrix_runtime_notes.md` | S | S | S | S | S | S | S | S | Style/routing only; all personas may receive their own boundary reminders. |
| `persona_coven_witch_variants.md` | E | N | N | N | N | N | N | R | Coven Witch voice only. |
| `persona_guard_practical_boundaries.md` | S | R | R | E | N | N | N | R | Guard-specific practical safety. |
| `persona_knowledge_routing.md` | S | S | S | S | S | S | S | S | Style/routing only; all personas may receive their own boundary reminders. |
| `persona_memory_relationships.md` | S | S | S | S | S | S | S | S | Relationship/memory style; use only if entity has relevant memory. |
| `persona_specialists_knowledge.md` | E | E | S | S | S | S | S | S | Profession/domain exceptions. |
| `persona_vanilla_teacher.md` | S | S | S | S | S | S | S | S | Style/routing only; all personas may receive their own boundary reminders. |
| `persona_villager_rumor_boundaries.md` | S | E | E | S | N | N | N | R | Villager ignorance and rumor rules. |
| `vanilla_biomes_weather_worldgen.md` | E | S | S | S | S | R | R | S | General world facts; access depends on local/professional context. |
| `vanilla_enchanting_repair_brewing.md` | E | E | S | S | R | S | S | S | Enchanting/potion knowledge; apothecaries know vanilla brewing. |
| `vanilla_end_elytra_deep_pack.md` | E | R | R | R | R | R | S | E | End/elytra facts; witches/reflections exact, most others rumor. |
| `vanilla_end_progression.md` | E | R | R | R | R | R | S | E | End/elytra facts; witches/reflections exact, most others rumor. |
| `vanilla_exploration_navigation.md` | E | S | S | S | S | R | R | E | Navigation/maps; cartographers exact, ordinary villagers scoped. |
| `vanilla_farming_redstone_building.md` | E | E | E | S | R | N | R | S | Village/farm knowledge; villagers and apothecaries can know exactly. |
| `vanilla_farms_animals_villages_deep.md` | E | E | E | S | S | N | R | S | Everyday agriculture, animals, village life. |
| `vanilla_late_game_progression.md` | E | R | R | R | R | R | S | E | End/elytra facts; witches/reflections exact, most others rumor. |
| `vanilla_maps_coordinates_commands_deep.md` | E | S | S | S | S | R | R | E | Navigation/maps; cartographers exact, ordinary villagers scoped. |
| `vanilla_mining_resources.md` | E | S | S | S | E | S | R | S | Mining/resource facts; hobgoblins are strong specialists. |
| `vanilla_mobs_biomes_deep.md` | E | S | S | E | S | S | R | S | Mob/combat knowledge; guards exact for threats. |
| `vanilla_mobs_combat.md` | E | S | S | E | S | S | R | S | Mob/combat knowledge; guards exact for threats. |
| `vanilla_nether_deep_pack.md` | E | R | R | S | S | E | S | S | Nether facts; witches and imps exact, villagers rumor. |
| `vanilla_nether_progression.md` | E | R | R | S | S | E | S | S | Nether facts; witches and imps exact, villagers rumor. |
| `vanilla_potions_enchantments_advanced.md` | E | E | S | S | R | S | S | S | Enchanting/potion knowledge; apothecaries know vanilla brewing. |
| `vanilla_redstone_storage_building_deep.md` | E | R | S | R | S | R | N | S | Technical knowledge; avoid making all NPCs engineers. |
| `vanilla_resources_tools_deep.md` | E | S | S | S | E | S | R | S | Mining/resource facts; hobgoblins are strong specialists. |
| `vanilla_status_effects_items.md` | E | S | S | S | S | R | R | S | General world facts; access depends on local/professional context. |
| `vanilla_structures_deep_pack.md` | E | S | S | S | S | R | R | E | Structure knowledge; cartographer/villager access should be profession-scoped. |
| `vanilla_structures_exploration.md` | E | S | S | S | S | R | R | E | Structure knowledge; cartographer/villager access should be profession-scoped. |
| `vanilla_survival_basics.md` | E | S | E | E | S | S | R | S | Common survival knowledge; persona voice changes certainty. |
| `vanilla_villagers_trading.md` | E | E | E | S | R | N | R | S | Village/farm knowledge; villagers and apothecaries can know exactly. |
| `witchery_vanilla_bridge.md` | E | S | R | S | S | S | S | E | Boundary cards separating vanilla and Witchery systems. |
| `witchery_vanilla_bridge_deep.md` | E | S | R | S | S | S | S | E | Boundary cards separating vanilla and Witchery systems. |
| `world_creatures_hunters_deep.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_glossary_questions.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_mirror_torment_deep.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_omens_biomes_weather.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_orientation.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_spirit_world_deep.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_structures_places_deep.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
| `world_village_social_deep.md` | E | S | R | S | S | S | S | E | Witchery/world lore; non-witches get scoped or rumor access. |
