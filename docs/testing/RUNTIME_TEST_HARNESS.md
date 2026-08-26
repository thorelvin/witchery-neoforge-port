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
python tools/runtime_test_harness.py all
```

- `validate` checks manifest schema, duplicate entries, resource-test source
  classes, and exact equality between registered GameTest IDs and the manifest.
- `resource-contracts` runs the 18 explicitly listed structural/resource JVM
  tests. A broad Gradle summary cannot silently substitute another test set.
- `game-tests` starts one clean normal-difficulty dedicated world and runs all 31
  CI-lane GameTests sequentially by exact ID. Each command must report exactly
  one completion and the required-tests-passed marker. Isolated test servers
  disable empty-server pausing so long scheduled tests keep ticking without a
  connected player.
- `save-reload` starts a clean server, places and mutates a Kettle, Witches'
  Oven, Distillery, Spinning Wheel, Brazier, and Silver Vat, records a command-storage
  marker, and queues a relocated active ritual. It then flushes and stops, starts
  the same world again, and checks every block, inventory payload, block-entity
  value, marker, ritual stage, focus, destination, and refund ledger.
- `all` runs the four gates above in order. Dedicated-server startup and clean
  all-dimensions shutdown are required by the server-backed stages.

For a focused local rerun, repeat `--game-test` after `game-tests`:

```powershell
python tools/runtime_test_harness.py game-tests `
  --game-test witchery:custom_brew_environmental_actions `
  --game-test witchery:otherwhere_hearth_round_trip `
  --game-test witchery:torment_multiplayer_boss_reconnect
```

Only exact IDs in the CI lane are accepted. Add or remove tests through
`tools/runtime_test_manifest.json`; manifest drift fails before a server starts.

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

Real-client multiplayer feel, claim/protection adapters, rendering, animation,
particles, fog, and audio also remain manual. These checks need clients or
optional server mods and must not be represented as headless CI coverage.
