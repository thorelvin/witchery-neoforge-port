# Ritual QA Progress

Last updated: 2026-06-13

## Source Rules

- Original 1.7.10 Witchery remains the behavior source: `C:\claude\witchery\decompiled\witchery-1.7.10-0.24.1`.
- Any behavior or visual that differs from source must be approved before it becomes intended behavior.
- Approved deviation: active Sanctity/Imprisonment may keep the green ring pulse when a mob is pushed or pulled. Source protection rites do not show that pulse, but it is intentionally retained as playtest feedback.
- Source `SacrificeItem` accepts both dropped item entities and Grassper-held items within the ritual area. The port now supports that path for the tested Charging and Sanctity/Imprisonment rites, plus the corrected Protection/Banish support items listed below. The remaining rites still need a broader transaction refactor before universal Grassper parity is safe.
- Source circle recipes match required rings and ignore harmless extra outer rings. A small rite should still work inside a complete small+medium or small+medium+large setup unless a larger matching recipe with all required sacrifices is present first.

## Confirmed In Game

- Golden center glyph right-click is the activation point.
- Rite of Charging works with dropped offerings and Grassper-held offerings.
- Required Charging offerings: Attuned Stone as the activation item, plus Glowstone Dust, Redstone, Wood Ash, Quicklime, and 2000 altar power.
- Charging requires a small ritual ring plus a medium ritual ring. A large extra ring is allowed because source matching ignores unrelated extra rings.
- Rite of Sanctity works and repels a Banshee.
- Sustained rite inspection reports active state, pulse timer, upkeep, radius, and nearby altar power/range.
- Small Rite of Protection works from a small ritual ring with Obsidian activation, Redstone support, and 500 altar power. Source parity confirmed: it is sustained, blocks arrows/projectiles, keeps players able to walk in and out, and shows the approved green floor pulse.
- Portable Rite of Protection works from a small ritual ring with Obsidian plus a Charged Attuned Stone and no altar power. Source parity confirmed: it creates the larger short-lived barrier, blocks players, and blocks arrows/projectiles. Source duration is 60 one-second pulses, with barrier blocks lingering up to about 30 ticks after the last pulse.
- Medium Rite of Protection activated and rendered a barrier from the medium ritual ring with Obsidian plus Glowstone Dust and 1000 altar power, but pre-fix QA found players could still pass through it. This should be retested after the source-geometry fix below.

## Completed Port Fixes From This QA Pass

- Circle detection now uses source-scale rings:
  - Small: 7x7 footprint, 16 glyphs.
  - Medium: 11x11 footprint, 28 glyphs.
  - Large: 15x15 footprint, 40 glyphs.
- Ritual chalk variant detection now treats white ritual chalk as variant `1` and ignores visual glyph art selection.
- Glyph blocks now use a flat 1/64-block floor shape, so the glyph no longer blocks targeting the floor block.
- Debug Inspector right-click resolves the floor center from a clicked glyph/center, reports detected rings, reports nearby altar power using the altar's upgraded range, and reports sustained active rituals.
- Creative tab includes the colored chalk families.
- Missing circle/source visual assets and flat circle block model parent were restored for current circle rendering.
- Rite of Charging now checks the source small+medium setup and consumes support offerings from dropped items or Grasspers.
- Rite of Sanctity and Rite of Imprisonment now select their representative item correctly and accept Redstone from dropped items or Grasspers.
- Unknown rite feedback changed from plain debug wording to a Witchery-flavored line.
- Iron Golems no longer target Witchery Village Guards.

## Additional Adjustments Found And Fixed

- Portable Rite of Protection was wired to the wrong ring in the port. Source uses a small ritual circle with Obsidian plus a Charged Attuned Stone. The port now routes the portable barrier through the small ring, not the medium ring.
- Medium Rite of Protection now only claims an Obsidian activation when Glowstone Dust support is present, so an extra medium ring does not block a valid small Obsidian rite.
- Medium Rite of Charging now only claims an Attuned Stone activation when the full Charging support set is present, so missing support items do not block smaller valid rites.
- Rite of Banish Demon was missing its source support sacrifices. The port now requires Blaze Powder plus a blank Waystone, with 2000 altar power for the non-portable branch and a Charged Attuned Stone activation item for the portable branch.
- Protection and Banish support checks now accept Grassper-held support items in the corrected branches.
- Protection barrier blocks now render through the old translucent barrier texture instead of being forced invisible by the port. The source texture is very faint, so the intended visual is a subtle shimmer rather than a bright wall.
- Protection barrier geometry now follows source `RiteProtectionCircleBarrier`: filled floor disk below the circle plane, one-block-thick Bresenham cylinder at the circle plane, and filled roof disk above it. This fixes the medium player-blocking path being interrupted by modern chalk glyph blocks.
- 2026-06-13 Waystone/Transposition parity update: blank/bound/blooded Waystone source IDs, small bind/copy support, Blooded Waystone support sets, dropped blank Otherwhere auto-binding, dropped bound/Blooded Waystone transposition, small transposition to a bound Waystone, and medium taglock transposition are restored. Crystal Ball scrying now consumes the same bound/blooded/taglock data for source-style held scrying, and Player Compass has its source-style 32-frame dynamic needle model.
- 2026-06-16 Sky's Wrath activation no longer relies on a single representative trigger item. Small Ritual-circle activation now pre-scans dropped offerings for the full source Storm recipes (sword tier + Wood Ash, plus Charged Attuned Stone for portable) using `RitualRecipe.assign`, then feeds the exact matched item entities into the staged sacrifice path. This prevents Wood Ash or another nearby support item from making a valid Storm setup miss before the rite body runs.
- 2026-06-16 full-offering dispatch slice 2: Storm's pre-scan is now a reusable ordered recipe table, and the source protection-circle/barrier cluster plus Banish Demon routes are converted. Dropped Feather/Slimeball/Obsidian/Blaze Powder no longer need to be the representative trigger when the complete source offering set is present near the center. Converted recipes return no-trigger-consume because the matched dropped items are already queued into staged sacrifice consumption.

## Remaining Ritual Risks

- The port still uses a representative activation item instead of the old recipe registry's full unordered sacrifice match for many rites. Sky's Wrath/Storm, Sanctity/Imprisonment, Protection barriers, and Banish Demon now use the dropped full-offering dispatch path first, but other multi-sacrifice rites can still misroute if the center click chooses a support item that is not one of the manually prioritized representative items.
- Many offering records still store `ItemEntity` directly, so universal Grassper support is incomplete outside the corrected Charging, Sanctity, Imprisonment, Protection, and Banish paths.
- Active ritual state is stored in memory and cancelled/refunded on server stop, not persisted through reload like a full modern SavedData implementation would require.
- Optional Bound Waystone relocation is still mostly dropped-item based. Source optional sacrifices can also come from Grasspers.
- Sustained visuals need per-rite source review before more effects are added. The green protection ring is approved; other non-source visuals still need approval before becoming intentional.
- Source BarrierBlock also had owner and creative-sneak pass-through exceptions for some non-rite barrier uses. Creative-sneak pass-through is restored for current player-blocking barriers. Current protection rites pass no owner, and the port has no other live `BarrierBlock.setBarrier` caller yet, so that broader owner path remains a future parity check if Light Infusion barrier placement is restored.

## Suggested Next Rite Tests

1. Medium Rite of Protection retest: medium ritual ring, Obsidian plus Glowstone Dust, 1000 altar power. It should create a sustained player-blocking barrier after the source-geometry fix.
2. Rite of Banish Demon: small ritual ring, Blaze Powder plus blank Waystone, 2000 altar power; then portable branch with Charged Attuned Stone plus Blaze Powder and blank Waystone.
3. Waystone route retest: bind a blank Waystone, copy it with another blank, create a Blooded Waystone with each support set, use small transposition to the bound location, drop a bound/Blooded Waystone on a small Otherwhere ring with a nearby mob/item stack, use medium taglock transposition on a loaded target, confirm Crystal Ball scrying accepts the same complete Waystone/Taglock data, and confirm Player Compass points at a visible bound player.
