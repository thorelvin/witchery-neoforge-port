# Runtime Test Harness

The runtime harness is the deterministic release gate for Witchery behavior that
cannot be proved by ordinary JVM tests. It starts clean NeoForge dedicated
servers with Witchery's custom dimensions loaded, runs exact named GameTests,
and verifies that persisted state survives a real stop and restart.

## Full Local Gate

Run the JVM/build gate and runtime gate before merging a behavior change:

```powershell
.\gradlew.bat test build
python tools/runtime_test_harness.py all
```

On Unix-like systems:

```bash
./gradlew test build
python3 tools/runtime_test_harness.py all
```

Python uses only the standard library. The harness owns isolated directories
under `build/runtime-test/` and refuses to recursively reset paths outside that
root.

## Available Gates

```text
python tools/runtime_test_harness.py validate
python tools/runtime_test_harness.py resource-contracts
python tools/runtime_test_harness.py game-tests
python tools/runtime_test_harness.py save-reload
python tools/runtime_test_harness.py blight-heart-restart
python tools/runtime_test_harness.py village-certification
python tools/runtime_test_harness.py wild-structure-certification
python tools/runtime_test_harness.py witch-well-certification
python tools/runtime_test_harness.py all
```

- `validate` checks manifest schema, duplicate entries, resource-test source
  classes, and exact equality between registered GameTest IDs and the manifest.
- `resource-contracts` runs the 22 explicitly listed structural/resource JVM
  tests. A broad Gradle summary cannot silently substitute another test set.
- `game-tests` starts one clean normal-difficulty dedicated world and runs all 48
  CI-lane GameTests sequentially by exact ID. Each command must report exactly
  one completion and the required-tests-passed marker. Isolated test servers
  disable empty-server pausing so long scheduled tests keep ticking without a
  connected player.
- `save-reload` starts a clean server, places and mutates a Kettle, Witches'
  Oven, Distillery, Spinning Wheel, Brazier, and Silver Vat, records a command-storage
  marker, queues active Forestation, Blight, and Broken Earth probes, and writes
  PREPARED, PAID, and ARMED ritual-escrow states plus held flowing water. It
  flushes the save and forcibly kills the process rather than granting a graceful
  shutdown. The same world is then restarted and checked for every block,
  inventory payload, block-entity value, marker, active-rite stage/focus/path,
  exact one-time PAID refund, PREPARED discard, ARMED retention, and the original
  fluid block state.
- `blight-heart-restart` runs the two stages of the manual-lane
  `witchery:blight_heart_restart_resume` test around a real clean stop/restart of
  the same world. It is explicit rather than part of the per-commit `all` gate.
- `village-certification` locates and audits plains, desert, savanna, taiga, and
  snowy villages in a fixed-seed world, then restarts and recertifies the same
  claims, palettes, residents, walls, gates, paths, and safe arrivals.
- `wild-structure-certification` creates a fixed-seed world, locates Coven
  Circle, Straw Man, Hobgoblin Hut, wild Witch Hut, Warped Coven, and Vanishing
  Hut through normal generation, audits each actual piece box, computes a
  collision-safe landing and walkable internal route, relocates the Vanishing
  Hut while preserving its expected resident UUID, restarts the world, and
  recertifies all six anchors. This is a focused release-certification lane.
- `witch-well-certification` creates a fixed-seed world and naturally generates
  the regular, small, and cursed-cave designer-NBT variants. It verifies exact
  authored blocks, block entities and authored fluids, adaptive surfaces, loot,
  cave warning/curse ladders, safe arrival, segmented traversal, then restarts
  the same save and recertifies all three stable anchors.
- `all` runs resource contracts, CI GameTests, forced save/reload, village
  certification, wild-structure certification, and Witch Well certification in
  order. The explicit Blight Heart and Otherwhere Hearth manual restart lanes
  remain outside `all`.
  Dedicated-server startup and clean all-dimensions shutdown are required by the
  server-backed stages.

For a focused local rerun, repeat `--game-test` after `game-tests`:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:custom_brew_environmental_actions `
  --game-test witchery:otherwhere_hearth_round_trip `
  --game-test witchery:torment_multiplayer_boss_reconnect
```

The owner-safe loaded-only Hearth inspection can be isolated with:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:otherwhere_hearth_seer_preflight
```

That test starts with an entirely unloaded paired Spirit World footprint and
asserts the count remains zero after both direct preflight and Seer Stone use. It
also covers non-owner privacy, no item/power spend, cooldown, and the shared
broken-circle activation predicate.

The Spirit World fresh-chunk and lifecycle certificates can be isolated with:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:spirit_world_fresh_chunks `
  --game-test witchery:spirit_world_lifecycle
```

The fresh test deterministically selects a plains region from the fixed
`witchery-runtime-test` seed, generates paired 9 x 9 Overworld/Spirit regions,
and compares 5,184 biome samples plus 1,296 pre-decoration terrain heights while
requiring natural cotton, ordinary ecology, eligible source spawn floors, and the
exact Spirit spawn entry. The lifecycle test uses two Survival players to cover
peaceful/nightmare isolation, Body and inventory state, reconnect, Icy Needle and
lethal-damage wake, blocked-return fallback, Manifestation transfer, victim-bound
Nightmare pressure, and cleanup.

The property-gated two-process client certificate is separate from `all`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass `
  -File tools/run_spirit_world_lifecycle_certification.ps1 `
  -WorldName "New World (23)" -WaitSecondsPerPhase 240
```

It clones the selected save, prepares a real dream in one client process, restarts
the JVM, verifies exact Body/escrow/return/inventory/vitals persistence, sends live
Icy Needle use through the game mode, verifies wake/cleanup, and removes the clone.

The Red Thread anchor lifecycle can be isolated with:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:red_thread_anchored_sinking
```

It prepares a bound Poppet through the real shelf interaction, activates the
full Curse of Sinking recipe, proves loaded-anchor renewal, breaks the shelf,
restores the victim's exact prior effects, and verifies the short same-pair ward.

The two final feature-integration certificates can be isolated with:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:coven_specialty_contributions `
  --game-test witchery:vanishing_hut_bargain_cycle
```

They prove restrained living-coven participation/Concord and the hut's one-use
cycle transaction, salt refusal, relocation reset, persisted resident identity,
and unloaded coordinate-free Taglock scrying.

Only exact IDs in the CI lane are accepted. Add or remove tests through
`tools/runtime_test_manifest.json`; manifest drift fails before a server starts.

Production registration is disabled unless the dedicated runtime launches with
`-Dwitchery.enableGameTests=true`. The release workflow exposes five stable check
names: `Build`, `Resource Contracts`, `Hardening Unit Tests`, `Dedicated GameTests`,
and `Dedicated Save Reload`. Repository owners should require those checks through
branch protection after the workflow has run successfully; the repository does
not claim that GitHub branch protection is already configured.

## Results And Failures

Each stage writes its process log and `summary.json` below
`build/runtime-test/<stage>/`. Server stages also preserve `minecraft-*.log`,
the generated world, and any crash reports. GitHub Actions uploads these paths
for 14 days when a build or runtime job fails.

Read `summary.json` first, then the matching process and Minecraft logs. The
harness stops at the first named GameTest failure so its location and stack are
not buried under later output.

## Manual Lanes

`witchery:otherwhere_hearth_recovery_resume` remains a manual restart lane. It
requires deliberately killing a prepared Hearth transaction after its journal
reaches disk, restarting that exact world, reconnecting the owner, and proving
idempotent recovery without a second altar charge. A clean standalone invocation
would not exercise that contract.

`witchery:blight_heart_restart_resume` is the second restart lane. Run
`python tools/runtime_test_harness.py blight-heart-restart`; the harness creates
an owned Heart and one exact terrain mutation, flushes and cleanly stops the
server, restarts the same world, verifies owner/state/budget/cadence/ledger, then
resumes cleansing and checks exact Dirt/Cobblestone restoration. Its two-process
gate passed on 2026-08-27.

Real-client multiplayer feel, claim/protection adapters, rendering, animation,
particles, fog, and audio also remain manual. These checks need clients or
optional server mods and must not be represented as headless CI coverage.
