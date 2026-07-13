# Witchery Creative Tab Parity Audit

Date: 2026-07-13

## Sources

- Original screenshots:
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190709.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190742.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190757.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190814.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190830.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190842.png`
  - `C:\claude\witchery\Skjermbilde 2026-07-04 190855.png`
- Original source: `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1`
- Port source: `src/main/java/com/emoniph/witchery/init/WitcheryCreativeTabs.java`
- Port names: `src/main/resources/assets/witchery/lang/en_us.json`

## Summary

The screenshots show roughly 313 visible original Witchery creative/search entries. The port currently exposes 473 entries in the Witchery tab.

The port is not source-order parity. It is a curated modern tab, grouped by guidebooks, wood/trees, plants, machines, ritual/world blocks, tools, poppets/scrying, ingredients/alchemy, vampire/werewolf items, weapons/armor, brews/infusions, dreams/demons/contracts, legacy/debug entries, and spawn eggs.

That makes the port feel fuller and easier to browse in a modern pack, but it also means screenshot-by-screenshot parity checks will report many order differences and some deliberate extras.

## High-Confidence Differences

| Area | Original 1.7.10 behavior | Port behavior | Notes |
|---|---|---|---|
| Overall order | Screenshots begin with the large `ItemGeneral` metadata sequence: Candelabra, Chalice, Dream Weavers, waystones, etc. | Starts with guidebooks and then modern category groups. | This is the biggest visual mismatch. Decide whether exact source order or curated modern order is the target. |
| Total visible entries | About 313 visible entries across the screenshots. | 473 entries in the port tab. | Port includes modern extras and split-out variants. |
| Plant mines | Metadata order is webs, ink, sprouting, thorns for each disguise, with vanilla flower/shrub disguise icons. | Port now emits source order and renders inventory stacks by disguise: poppy, dandelion, or dead bush. | Patched without changing enum serialization. The old standalone alias items remain registered but no longer clutter the creative tab. |
| Stockades | Normal stockade exposes 9 wood variants: Oak, Spruce, Birch, Jungle, Rowan, Alder, Hawthorn, Acacia, Dark Oak. Ice stockade exposes one variant. | Port now exposes the same nine `witchery:stockade` block-state stacks plus `ice_stockade`. | Patched through a `wood` block-state/component design on the real stockade item, with variant-aware names, models, drops, and recipes. |
| Circle Talisman | Creative subitems are metas `0,1,2,3,8,16,24,64,128,192`. | Port now exposes the same source-visible encoded stacks, with names and icons driven by the stored circle data. | Patched through the real talisman item data, not alias items. |
| Spectral Stone | Creative subitems are metas `0,17,18,19`. | Port now exposes the same source-visible blank, Spectre, Banshee, and Poltergeist stacks. | Patched through the real binding data used by the rite/release behavior. |
| Leonard's Urn | Creative subitems are metas `0..3`. | Port now exposes capacity 1-4 urn stacks and chooses the matching source urn icon from capacity data. | The modern storage behavior remains. |
| Stale seeds | 1.7.10 English lang exposes `ingredient` metadata names for Stale Belladonna Seeds, Stale Mandrake Seeds, and Stale Water Artichoke Seeds, distinct from the plantable `seeds...` items. | Port now exposes `stale_belladonna_seeds`, `stale_mandrake_seeds`, and `stale_artichoke_seeds` as non-plantable creative-visible legacy ingredient entries beside the real plantable seeds. | Patched as source-named compatibility items using the old seed sprites; no planting behavior was added because source usage points to the separate plantable seed items. |
| Decurse statues | `Statue of Occluded Summons` (`decurseteleport`) and `Statue of Broken Curses` (`decursedirected`) are source `BlockAreaMarker` variants. | Port now exposes both exact block IDs in the Witchery tab, persists owner/killer data, and dampens taglock entity transposition or directed curse rites in the configured 32-block horizontal radius. | Functional parity slice is live; static stone-marker models stand in for the old mandrake/wolf special renderers pending visual QA. |
| Brambles | Old visible names include `Wild Bramble`, while the stronger owner/null-source plant is `Void Bramble`. | Port now names the damage/spread block `Wild Bramble` and keeps the owner/null-source block `Void Bramble`. | The old metadata `Ender Bramble` remains represented mechanically by the current `Void Bramble` path rather than a second visible stack. |
| Debug items | Not source creative items. | `Debug Death Ward` and `Debug Inspector` are visible in the Witchery tab. | Useful for QA, but they are extras in player-facing parity. |
| Spawn eggs | Not shown in the provided original screenshots. | 36 Witchery spawn eggs are in the tab. | Modern convention, but screenshot parity sees them as extras. |

## Name Mismatches To Review

These look like renamed, corrected, or modernized entries rather than hard missing features:

| Original name | Port name or likely equivalent |
|---|---|
| Chalice (Filled) | Filled Chalice / stack state |
| Dream Weaver of Iron Arm | Dream Weaver of the Iron Arm |
| Attuned Stone (Charged) | Charged Attuned Stone |
| Belladonna Flower | Belladonna |
| Wool of Bat | Bat Wool |
| Diamond Vapor | Diamond Vapour |
| Witches Hand | Witch Hand |
| Icy Slippers | Ice Slippers |
| Demonic Blood | Restored display name on `witchery:infernal_blood` |
| Blood Poppy | Restored display name on `witchery:blood_rose` |
| Apple | Apple of Sleeping |
| Torment | Torment Scroll |
| Minedrake / Minedrake Bulb | Restored display names on `mindrake`, `mindrakebulb`, and spawn egg |
| Gulg's Gurdle | Exact `witchery:gulgs_grudle` restored; `gulgs_girdle` remains a compatibility stack |
| Vampire Dress Jacket (Ladies) | Vampire Dress Jacket |
| Vampire Chain Coat (Ladies) | Vampire Chain Coat Ladies |
| Mirror Surface | Mirror Wall |

These should be decided by style policy. Some are probable old typos, some are source flavor, and some may need compatibility aliases even if the display name stays cleaned up.

## Not Actually Missing From Original Creative

| Entry | Why it should not be counted as missing |
|---|---|
| Bound Waystone / Blooded Waystone | `ItemGeneral` marks these subitems hidden from creative. |
| Captive Critter Snare variants | Old `BlockCritterSnare` only adds the empty snare to creative. Captive names exist in lang but are not creative-visible. |
| Bucket of Brew | Old `ItemBrewBucket` is hidden from creative. The port exposing brew buckets is a modern extra, not an old missing item. |
| Torment Stone / force block | Old force block registration is not creative-visible. |
| Circle glyph blocks | Old glyph block registration path marks them as non-creative; source screenshots should not require them as normal visible entries. |

## Recommended Follow-Ups

1. Decide the creative tab target: exact source-order parity, modern curated tab, or two tabs/modes.
2. Hide debug items from the normal player-facing tab, or gate them behind a config/operator-only debug path.
3. Decide whether spawn eggs belong in the main Witchery tab or a separate/debug/entity section.
4. Add source-visible creative variant stacks for any remaining metadata-backed legacy items that still collapse to one stack.
5. Continue reviewing metadata-backed legacy variants that still collapse to one modern stack.
6. Continue applying source-facing display names when the old English name is clear; keep modern registry IDs stable and add compatibility aliases where needed.

## Patch Made During This Audit

`addPlantMineVariants` now emits plant-mine effects in source creative order:

1. Webs
2. Ink
3. Sprouting
4. Thorns

The enum declaration was left unchanged so serialized block state names remain stable.

## Additional Icon Patch

Follow-up icon parity fixes made after this audit:

- `Torment Scroll` now uses the source parchment contract art instead of the Tormented Twine swirl.
- `Soul of Torment` now uses the source red torment soul art instead of the green Soul of the World jar.
- `Brew Bag` was refreshed from the source `brewbag.png` art while keeping the port's dye tint path.
- `Leonard's Urn` now has four capacity-sensitive source icons.
- `Wolf Head` and `Hellhound Head` were refreshed from the source head sprites.
- Plant mine creative duplicates were removed; the real plant mine block item now renders by disguise.
- `Circle Talisman` now exposes the source creative metadata set and uses the source icon variants for stored circle data.
- `Spectral Stone` now exposes the source creative metadata set through modern bound-stone data.
- `Stockade` now exposes the source Oak, Spruce, Birch, Jungle, Rowan, Alder, Hawthorn, Acacia, and Dark Oak creative stacks through modern block-state data.
- `Statue of Occluded Summons` and `Statue of Broken Curses` are now exposed as exact source block IDs with loaded-marker protection behavior.

## Additional Naming Patch

The first source-facing name policy slice keeps stable modern registry IDs but restores old player-visible names where the 1.7.10 English lang is unambiguous:

- `witchery:mindrake` / `witchery:mindrakebulb` display as Minedrake / Minedrake Bulb.
- `witchery:infernal_blood` displays as Demonic Blood.
- `witchery:blood_rose` displays as Blood Poppy.
- `witchery:bramble` displays as Wild Bramble.
- Exact `witchery:gulgs_grudle` is registered as the source/Resurrected Gulg reward, drops from Gulg, appears in the creative tab, and shares all behavior with the older `witchery:gulgs_girdle` compatibility item.
