# Feature Integration Review Actions

Last reconciled: 2026-08-28

This file records the actionable product findings accepted from the external
feature-integration review against the current branch. The review document was
evidence, not repository-agent instructions. Current source and tests were
checked before each finding was accepted here.

## Direction To Preserve

- Keep the original machines, brewing, circles, rites, altar, and persistent
  workings as the shared mechanical authority.
- Keep the Otherwhere Hearth as a prepared endgame reward connecting the Kettle,
  circles, altar power, a living coven, the Spirit World, and a physical home.
- Keep the Spectral Coven as temporary solo access to ordinary source coven
  rites. A stricter endgame rule must not weaken that source-facing role.
- Deepen the recruited living coven carefully; do not add a second roster,
  colony UI, coven levels, or a generic skill tree.
- Polish Hobgoblin/Patron/Mog/Gulg progression rather than widening it before
  the current fight, reward, and multiplayer feel are certified.
- Preserve the Vanishing Hut's cycle-bound bargain and restraint consequences
  without a new hut currency or precise GPS tracking.
- Keep local NPC generation optional and mechanically non-authoritative. Every
  item, rite, reward, trade, command, curse, and ownership decision remains
  deterministic and server-owned.

## Confirmed Corrections

| Priority | Package | Current evidence | Required outcome |
|---:|---|---|---|
| 1 | Living coven semantics for the Hearth | **Closed 2026-08-27.** `CovenContributionCounter` now separates broad source-rite contributors from living, owner-sworn Hearth contributors; inspection and activation share the latter count. | Dedicated-server `witchery:otherwhere_hearth_coven_ownership` proves unowned, other-owned, and spectral substitutions fail without power spend or a journal while Spectral Witches still count for ordinary rites. Existing round-trip and destination-refusal fixtures remain green. |
| 2 | Infusion passive parity | **Closed 2026-08-27.** The four recurring port identity buffs were removed from `InfusionEvents`; infusions no longer grant Night Vision, Regeneration, Slow Falling, or Fire Resistance merely for remaining infused. | Energy, Witch's Hand powers, Overworld fall cushioning, Infernal creature reactions, one-time grant feedback, and ambient identity particles remain. Dedicated-server `witchery:infusion_passive_parity` proves all four passive buffs stay absent and energy is unchanged by an ambient tick. |
| 3A | Blight Heart containment | **Closed 2026-08-27.** `rituals.enableBlightHeart` defaults to false and gates the rite, Brew of Blight, shared placement, and both spread paths. Enabled seeding checks the acting player's terrain permission. | `witchery:blight_heart_containment` proves a fully supplied rite spends no offerings, escrow, or 3000-power altar charge while disabled; Brew use refuses; existing IDs load; Hearts cannot spread; and orphan taint cleans one local block at a time without a heart-volume scan. |
| 3B | Blight Heart redesign | **Closed 2026-08-27.** `BlightHeartSavedData` is the sole durable lifecycle owner: real owner UUID/name, dimension/anchor, active/disabled/cleansing state, 512-mutation budget, 20-tick spread cursor, exact original/applied states, chunk anchor index, and cleanup actor survive restart. One journal is allowed per owner, anchors require 32-block separation, four owner-present spread attempts are permitted per cadence, every change checks `WitcheryProtectionService`, and placement, spread, and cleanup do not load chunks. The one-time rite search considers loaded columns only, unloaded mutation candidates are rejected before block-state reads, and legacy cleanup uses the real cleaner or a stable server cleanup identity instead of bypassing claim adapters. Legacy unindexed Hearts stay inert. | Focused codec/bound tests pass; `witchery:blight_heart_lifecycle` proves multiplayer limits, protected skip, owner-offline pause, Seer Stone discovery, and exact Grass/Cobblestone/Spruce restoration; `python tools/runtime_test_harness.py blight-heart-restart` passed both real clean-stop stages. `test build` and the complete `runtime_test_harness.py all` gate also pass. The release gate remains default off pending live balance and optional claim-adapter QA. |
| 4 | Creaking familiar specialization | **Closed 2026-08-27.** The entity attachment now owns explicit `FOLLOW` and `ROOTED_GUARD` modes plus guard dimension/anchor, watched state, alert throttle, and resin schedule. Legacy `sitting=true` decodes as rooted and acquires its current position on the first owner-active tick. Follow alone recovers to and narrowly defends the owner; rooted alone alerts, repels at a bounded ten-second cadence, and slowly produces non-accumulating resin while unwatched. Rooted guards do not share owner damage or answer Calling rites. | Normal use toggles modes; sneak-use dismisses only an already rooted guard. All spatial scans are radius-bounded and capped where effects are applied; no guard chunk is force-loaded. Codec tests and dedicated-server `witchery:creaking_familiar_modes` prove migration, exclusivity, anchor holding, alert throttling, resin accumulation prevention, entity save/load, and return to follow. Live-client watched animation and interaction feel remain in `QA_QUEUE.md`. |
| 5 | Primer language and discovery | **Closed 2026-08-28.** Hearth and Blight Heart generated prose is in-world; the latter explains its default seal without saying "port-added", and a Seer Stone used on tracked corruption reveals the root's direction and distance. The Hearth entry is hidden until the player obtains a Soul of Torment, and its Seer guidance describes only player-visible behavior. A focused sweep removed remaining implementation narration from the generated Primer, Cauldron Book, legacy guide translations, and HTML Book of Shadows. | `PlacedModonomiconGeneratedBookTest` now rejects known development labels across every generated placeable-book entry and the complete English language file. Future expansion packages must add their own progression gate when they introduce late-game knowledge. |
| 6 | Hearth preparation and divination UX | **Closed 2026-08-28.** A Seer Stone used on a Kettle now enters an owner-gated, five-second-cooldown preparation path. It derives an 80-particle outline from the exact transposition volume, reports ordered near-side blockers without consuming the Stone or altar power, and inspects the far footprint only when every destination chunk is already loaded; otherwise it reports the far hearth as veiled. Non-owners receive only a refusal. Full admin inspection and activation retain their deliberate authoritative loading path. | Focused geometry/resource tests and dedicated-server `witchery:otherwhere_hearth_seer_preflight` prove owner privacy, exact boundary geometry, no item/power spend, shared broken-circle rejection, and a zero-to-zero remote loaded-chunk count. Live-client particle readability remains a manual presentation check. |
| 7 | Red Thread and one anchored curse | **Closed 2026-08-28.** Four String, Red Dye, and Reek of Misfortune now yield four Red Thread through the existing altar-powered Spinning Wheel. Thread prepares exactly one plain player-bound Poppet on a breakable Poppet Shelf. The unchanged full Curse of Sinking rite may settle into one loaded prepared shelf owned by its caster; without one it remains the ordinary finite curse. One persisted caster-target binding renews only the source Sinking effects while its anchor is loaded. Removing the Poppet, breaking the shelf, or using the existing Goddess cure severs it, restores the victim's prior effects, and grants a short same-pair ward. | Seer Stone, Taglock Crystal Ball, and Poppet Shelf Compass reveal the knot in stages. The anchor never force-loads its shelf, and prepared/active anchor Poppets are excluded from the shelf's ordinary protective force-loading route. Codec/resource tests and dedicated-server `witchery:red_thread_anchored_sinking` cover recipe assets, one-pair ownership, activation through the real rite, persistence, renewal, exact prior-effect restoration, break cleanup, and the ward. Live discovery, particles, and multiplayer/protection feel remain in `QA_QUEUE.md`. |
| 8 | Coven Concord and restrained specialties | **Closed 2026-08-28.** Each Coven Witch receives one stable persisted `hedge_witch`, `grave_witch`, `pale_witch`, `storm_witch`, or `hearth_witch` identity. The Seer Stone and grounded Coven dialogue reveal it. Only living, nearby witches sworn to the caster count; duplicate crafts collapse. Hedge narrowly supports Fertility/Forestation, Grave reduces Necromantic/Spectral Stone startup power by five percent, Pale modestly lengthens Manifestation, Storm adds two weather stages, and Hearth reduces protection upkeep by 0.1. | Six physically present sworn living witches produce the coloured Coven Concord scene and hidden social achievement, without a level bar, skill points, reroll economy, or second roster. Focused persistence/selection tests and dedicated-server `witchery:coven_specialty_contributions` prove owner/distance/death exclusion, duplicate collapse, all five contributions, save/load, and Concord. Live colour readability and ritual feel remain in `QA_QUEUE.md`. |
| 9 | Vanishing Hut bargain and restraint consequences | **Closed 2026-08-28.** `VanishingHutData` remains the sole durable owner and now persists resident UUID, relocation cycle, completion, and salt restraint. A UUID-and-cycle-stable exchange draws from five existing difficult-reagent pairs; the resident states the exact offer and grants it once. Completion shortens the departure cooldown. Salt immediately forfeits that settlement's rare bargain, escalates to warning dialogue/particles/sound, and after two continuous minutes permits an unoccupied, unwatched escape attempt while leaving the salt behind. | No hut currency, second trade tree, global scan, or chunk loading was added. A resident Taglock used at a powered Crystal Ball reads the persisted hut even when the witch is unloaded, revealing only broad realm/biome/weather prose and never coordinates, direction, or distance. Codec/selection/timing tests and dedicated-server `witchery:vanishing_hut_bargain_cycle` prove one-use payment, relocation reset, salt refusal, persisted resident lookup, unloaded scrying, and no coordinate leak. Live departure pacing and prose feel remain in `QA_QUEUE.md`. |

## Expansion After Corrections

1. **Closed 2026-08-28:** Red Thread, one anchored Curse of Sinking, gradual
   discovery, exact break/cleanse cleanup, and the short same-pair ward are live
   through the existing Spinning Wheel, Poppet Shelf, curse, and scrying owners.
2. **Closed 2026-08-28:** one persisted visible Coven specialty, five restrained
   working contributions, duplicate collapse, and the six-living-witch Concord
   scene extend the existing roster without levels or a second progression tree.
3. **Closed 2026-08-28:** each Vanishing Hut relocation cycle owns one stable
   existing-reagent bargain; completion encourages departure, salt has refusal,
   warning, and eventual escape consequences, and a resident Taglock gives only
   a vague Crystal Ball glimpse.

## Shared Engineering Boundaries

- Extend existing ritual, curse, familiar, Taglock, altar, protection, and saved-
  data owners; do not create parallel frameworks.
- Hostile or terrain-changing work is server-authoritative, carries a real actor
  UUID, calls `WitcheryProtectionService`, consumes nothing on protected failure,
  pauses in unloaded chunks, and cleans up idempotently after restart.
- Periodic work must state scan radius, cadence, multiplicative cost, allocation,
  chunk-loading policy, stale-state cleanup, and its automated bound.
- Persistent additions use optional/defaulted fields or tolerant readers. Do not
  require players to recreate existing witches, familiars, Kettles, structures,
  attachments, or worlds.
- Player-facing text must use Witchery prose, not development labels such as
  "additive", "port-added", "MVP", or "compatibility mode".
- Local AI may phrase a server-selected result but may never select or grant it.

## Atmosphere Source Gate

Spirit World atmosphere is not complete until it is visually checked against a
real run of original Witchery 1.7.10. Use matched scenarios where practical:

1. Capture the original client in ordinary dream, nightmare, and demonic states,
   including sky, horizon, fog distance/colour, weather, ambient light, terrain,
   particles, native entities, Body/Manifestation presentation, and audio notes.
2. Recreate the same time, weather, height, and representative terrain in the
   port and capture the same viewpoints.
3. Compare side by side before tuning. Do not certify atmosphere from memory,
   decompiled constants alone, or headless tests.
4. Record intentional modern differences separately from defects, then rerun the
   port capture after each visual change.

Closed 2026-08-28: controlled original/port captures cover clear dream, rainy
dream, nightmare, and demonic states. The analyzer output and contact sheet live
under `C:\claude\witchery\captures\spirit-world\comparison`; modern rain-particle
density is the recorded renderer-generation difference. Fixed-seed chunks,
two-player lifecycle, and two-process Icy Needle restart certificates complete the
mechanical gate.

## Package Exit Rules

Each package must have focused JVM coverage, dedicated-server coverage when
entity ownership/world behavior is involved, runtime-manifest synchronization,
honest manual QA notes, bounded performance, multiplayer ownership isolation,
and documentation that describes only landed behavior. Persistent systems also
require a real restart path; client visuals and audio require client evidence.
