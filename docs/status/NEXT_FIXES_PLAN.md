# Next Fixes Plan

This plan is ordered by mechanical parity value, not by how visible a bug is in screenshots.
The goal is to make the NeoForge port behave like 1.7.10 Witchery first, then keep the codebase cleaner than the original where modern structure helps.
Use `docs/status/QA_PARITY_PLAN.md` alongside this file for the subsystem QA checklist covering mechanics, sound, and graphics parity.

## 15-step roadmap

1. Rebuild the ritual activation lifecycle around explicit activation results, queued state, and refund-safe failure handling.
2. Port the remaining staged and upkeep-heavy rites so they tick like the original instead of resolving as one-shot effects.
3. Reintroduce rite preconditions from 1.7.10 consistently: coven size, time-of-day, weather, dimension, and obstructed-circle checks.
4. Break the `RitualExecutor` monolith into data-driven dispatch helpers so parity fixes stop piling into one file.
5. Restore broom behavior closer to the original entity: placement, mounting, acceleration, pitch-based lift, glide, and fall handling.
6. Fix mandrake harvest rules so mature crops only spawn a mandrake under the original difficulty and time-based chances, instead of always doing both.
7. Replace the current cotton placeholder behavior with mechanics that match the Dream World plant flow more closely.
8. Audit Dream Catcher and Dream World nightmare chance rules against the old block scan and sleep-threshold behavior.
9. Restore special plant/block mechanics that are still plain blocks in the port: blood rose, bramble, void bramble, grassper, glowglobe, and critter snare. Spanish Moss is no longer plain behavior-wise, Wild Bramble has its old toughness back, Void Bramble now has its owner/unbreakable column-removal plus 32-block ritual-null-source behavior back, and Critter Snare now catches/releases the old small critter set again; remaining plant work is mostly in-game verification and smaller edge-case cleanup.
10. Revisit altar-adjacent placed-item interactions so Arthana, Mystic Branch, chalice, and similar bonuses match original expectations.
11. Restore storage block behavior parity for Leech Chest, Refilling Chest, Dream Catcher handling, and Poppet Shelf edge cases.
12. Finish Spirit World parity around corpse/state transitions, flowing spirit interactions, and demonic nightmare edge cases.
13. Finish machine screen parity so Distillery, Witch's Oven, and Spinning Wheel GUI rendering matches their textures cleanly.
14. Restore deferred systems that still need real implementation, especially Custom Brews, Mirror World work, and remaining rite coverage.
15. Add a repeatable regression checklist so future parity work can be verified subsystem-by-subsystem instead of rediscovered manually.

## Architecture and maintenance backlog

This backlog is separate from pure feature parity. The port is now far enough along that some of the biggest remaining risks are architectural drift, hidden state bugs, and multiple sources of truth. These items should be pulled in whenever they directly reduce parity risk or unblock safer future work.

1. Split `RitualExecutor` into a registry/definition model.
   The class is still the biggest technical-debt hotspot in the repo because it combines dispatch, preconditions, altar power, offering consumption, active-rite queueing, staged ticking, terrain/entity effects, refunds, climate plans, and utility scans. The long `dispatchSmall` / `dispatchMedium` / `dispatchLarge` chains are especially hard to audit because parity depends on trigger identity, chalk family, companion rings, and fallthrough order. The preferred end state is a `RitualDefinition`-style model with explicit circle requirements, trigger predicates, offering requirements, preconditions, ritual actions, and optional active-rite factories so dispatch becomes "find matching definitions, validate, execute transaction" instead of a giant ordered `if` chain.
2. Introduce ritual transactions and remove mutation-before-validation cases.
   Some rites still spend altar power or mutate state before all failure conditions are known. `riteOfBindFamiliar`, `riteOfCallFamiliar`, `riteOfTheForest`, the first `Breath of the Goddess` cure family, the first active-rite startup-power availability batch (Beastial Call / Infusion Recharge / Sanctity / Imprisonment / Protection / Sky's Wrath / Hell on Earth), the summon-family startup safety batch (Familiar / Witch / Imp / Wither / Demon), the first storm-gated offered-taglock infernal curse batch (`Curse Creature` / `Curse of Overheating` / `Curse of Sinking` / `Curse of Insanity`) plus `Curse of Waking Nightmare`, the first spirit-binding transaction-safety passes on `bindspectral` and fetish binding, the first one-shot item-output batch (`chargestone`, `infusiontree`, `infusionfuture`, `infusionseerstone`, both `infusebrew` branches), and the first waystone/transposition one-shot batch (`bindwaystone`, `bindwaystonecopy`, medium otherwhere entity transposition) were the clearest reviewed examples, and the port now has a small `RitualTransaction` helper plus a shared active-center availability helper covering those paths; `RitualTransaction` also now knows how to consume inventory-slot costs as well as dropped offerings, which removes more bespoke ritual plumbing, but the general risk still applies across the dispatcher. The rule should be: detect the matching rite, gather offerings, validate all preconditions, calculate costs, begin a transaction, consume power/items, apply or queue the rite, then commit or refund. The helper should keep expanding until one-off refund handling stops creeping back in.
3. Persist active rituals, or deliberately cancel/refund them on shutdown.
   `ACTIVE_RITUALS` and `CLIMATE_CHANGE_PLANS` are still static maps keyed by dimension, which is fine inside a single server session but unsafe across restart or reload because staged rites can silently disappear after ingredients and altar power were already consumed. The preferred fix is `SavedData`, a dimension attachment, or another durable ritual anchor. If full persistence is deferred, server stop/unload must at least cancel cleanly or refund stored sacrifices where possible.
4. Keep kettle recipes on one authoritative source and add tests around them.
   The dead `WitcheryKettleRecipes` singleton and `WitcheryKettleRecipeData` startup fallback are now gone, so the live kettle path only uses `WitcheryRecipeTypes.KETTLE_BREWING` JSON recipes. The remaining follow-up is test coverage so JEI, in-game brewing, docs, and QA stay aligned around that single source of truth.
5. Audit the remaining transient player-state fields around Spirit World handling.
   `SpiritWorldState` is now explicitly copyable and `WitcheryPlayerData.copyFrom` / constructor paths no longer share it by reference, so the old clone/respawn shared-mutable-state bug is closed. The remaining follow-up is to decide whether the fields that still live outside the CODEC after `wolfMastery` are intentionally transient, and either document that choice or move them into a persistable nested state object.
6. Wire config values through central helpers.
   `WitcheryConfig.RITUAL_POWER_MULTIPLIER` and `ALLOW_BIOME_CHANGING` should not remain "defined but inconsistently enforced" knobs. Ritual power should route through one shared cost helper, and biome-changing rites should honor the biome-changing flag before final consumption or queueing, not only at effect time.
7. Reduce expensive ritual activation and upkeep scans.
   Some current scans are still acceptable for local QA but too expensive for repeated public-server usage. `isRitualAreaNulled` currently scans a very large cube, altar lookups repeat for startup/upkeep checks, and some active rites still redraw or rescan broad areas frequently. The next performance pass should focus on narrower scan ranges, indexing/cache helpers for Void Brambles and nearby altars, chunk-budgeted active-rite work, and using registries/tags/block entities where they remove repeated brute-force scans.
8. Use helper methods for IDs and registry boilerplate.
   There are still literal `"witchery"` identifiers and repeated registry patterns across blocks, items, and entities. Small helpers for `id(path)`, resource keys, block items, spawn eggs, wood families, chalk variants, and similar patterns would reduce copy-paste drift and make later cleanup work safer.
9. Replace hardcoded altar environmental categories with tags where possible.
   `AltarBlockEntity.classify` still hardcodes large sets of vanilla logs, leaves, flowers, crops, and plants. That gives precise control, but it is brittle on modern Minecraft where vanilla families evolve over time. Prefer broad vanilla tags first (`LOGS`, `LEAVES`, `SAPLINGS`, flower/crop tags where available), then keep Witchery-specific contributors as explicit custom tags or special cases.
10. Clean up release metadata before any public release push.
   `neoforge.mods.toml` still carries development-template comments and placeholders, and the optional JEI/Patchouli metadata should be reconciled with the actual dependency state in `build.gradle`. This is lower priority than parity, but it should be finished before any serious public release packaging.
11. Add tests around the risky systems.
   The next quality jump is not just "more green builds" but focused tests around the systems most likely to regress: circle detection, ring ordering, ritual dispatch, no-consumption-on-failure rules, active-rite refunds, player-data copy/persistence, kettle recipe matching, and altar 2x3 validation plus contributor caps. The existing `gameTestServer` run config makes GameTests the most natural fit.

## Documentation and discovery backlog

Mechanical parity is not enough if the in-game book or player-facing guidance still leaves major progression chains unexplained. This backlog is specifically about closing the "player can craft/use it in the port, but the Witchery book flow still fails to teach it" gap.

1. Boline coverage - closed in first Primer pass.
   The Primer now has a dedicated Boline entry and the missing survival recipe is restored, so the book once again teaches what it is, what it does, and how to obtain it before later necromantic rites call for one.
2. Infusion source item coverage - closed in first Primer pass.
   The Primer now has a dedicated infusion-sources entry explaining how to obtain `Ghost of the Light`, `Soul of the World`, `Spirit of Otherwhere`, and `Infernal Animus`, so the player now has a real starting point for the four infusion paths instead of unexplained item names.
3. Anointing Paste coverage - closed in first Primer pass.
   The Primer now shows the Anointing Paste recipe and explains that it is applied directly to a placed vanilla cauldron to create a Witch's Cauldron, closing the old "how do I even start cauldron brewing?" gap.
4. Missing rite coverage in the compendium.
   Chapter 11 still omits a meaningful set of real rites that exist in the port or are intended parity targets. `Rite of the Forest`, `Rite of Manifestation`, `Rite of Beastial Call`, and `Rite of Sky's Wrath` now have first-pass Primer coverage again, but `Rite of Broiling`, `Rite of Broken Earth`, `Rite of Earth's Wrath`, `Rite of Icy Expansion`, `Rite of Imprisonment`, `Rite of Moving Earth`, `Rite of Nature's Power`, `Rite of Prior Incarnation`, `Rite of Sanctity`, and `Rite of Glyphic Transformation` still need real guide coverage. This is both a completeness gap and a progression gap because players cannot discover some real systems from the in-game guide alone.
5. Rite of the Forest / Treefyd seed bridge.
   The Primer now teaches the rite itself again, but the full `Treefyd Seed` bridge is still blocked by repo truth: the actual `Treefyd Seed` item/recipe path is not yet surfaced in the port. Finish that item path, then connect the guide to it directly.
6. Spectral Familiar explanation.
   The summon rite can exist without the player understanding what a Spectral Familiar actually is, how it behaves, or how it differs from Cat / Owl / Toad familiars. This needs a practical familiar entry, not just a recipe mention.
7. Mystic Branch explanation.
   The item appears as a ritual/ignition ingredient, but its actual tool-like spellcasting role is still not documented well enough. Add its active-use identity, especially the named spell behavior, so it does not look like a dead crafting stick.
8. Vampire armor coverage.
   Vampirism itself is documented much better than the dedicated vampire armor set. The Chain Coat / Dress Coat / Helmet / Trousers / Oxford Boots / Top Hat family and their vampire-specific bonuses should be covered as a real equipment track, not left to the wiki.
9. Minor completeness gaps.
   Lower-priority omissions still worth tracking: `Bark Belt`, `Biting Belt`, `Beartrap`, `Necromantic Stone` as a standalone explained item, and the explicit `Wicker Man` layout/pattern that Chapter 30 currently references without actually showing.

## Documentation priority order

Use this order when choosing book/completeness work instead of another renderer or rite patch:

1. Missing rite coverage next.
   Fill the omitted Chapter 11 rites, with `Rite of the Forest` now serving as the model for how the rest should be reintroduced and with the remaining Treefyd-seed bridge still waiting on the missing item path.
2. Tool/familiar identity after that.
   `Mystic Branch`, `Spectral Familiar`, and `Necromantic Stone` should be explained once the player can at least enter the systems they belong to.
3. Equipment and minor completeness after the blockers.
   Vampire armor coverage, belts, Beartrap, and the explicit Wicker Man layout are important, but they sit below the earlier progression blockers.

## Highest-priority structural fixes

When architecture work is chosen instead of another parity patch, take these first:

1. Introduce ritual transactions so rites do not consume power or items until all preconditions pass.
2. Split `RitualExecutor` into registry-driven or definition-driven handlers.
3. Audit and document the remaining transient player-state fields after the `SpiritWorldState` deep-copy fix.
4. Add tests around the now-single-source kettle recipe system.
5. Persist or explicitly cancel/refund active rituals across stop/reload.
6. Route ritual cost and biome-changing config through central helpers.
7. Add GameTests for ritual failure/consumption and circle detection.

## Finish-phase focus

The current pass started with steps 1, 5, and 6, and has now pushed step 2 again through the restored Summoning, Moving Earth, Raise Column, Protection, Sanctity, and Imprisonment rite lifecycles, the first supplementary-sacrifice refund-safe Climate Change abort path, the first remembered-chalk-family sustain check for active rites, the first broader direct-route chalk-family cleanup on Broiling / Broken Earth / Moving Earth / Raise Column / Volcano / Flying Ointment, the first paired-ring activation requirements restored on Flying Ointment, `summonwither`, the four player infusion rites, and Hell on Earth's mixed-ring curse, step 7, most of step 9, the altar-adjacent parity work in step 10, and the practical Leech Chest / Taglock / Poppet Shelf / Dream Catcher / Refilling Chest behavior in step 11 forward.
The next highest-value remaining targets are now step 2 first: deeper ritual refund/abort behavior beyond Storm / Summoning / Moving Earth / Raise Column plus the remaining ritual activation mapping gaps around the still-modern ingredient dispatcher, now that the protection-family upkeep ratios, several missing storm/night/day ritual traits, the shared time gate helper, the Rain of Toads dispatcher path, refund-safe taglock target validation, the first restored ritual-vs-infernal chalk-family gates, the first restored otherwhere gates, a direct sustain-time ring-size check for active rites, outer-to-inner retry across all present ring sizes for new casts, an explicit matched-vs-no-match activation result that stops precondition-failed rites from falling through into unrelated ring retries, the first restored dropped-ingredient refund path for broken-circle and lost-upkeep aborts on staged rites, the first restored paired-ring activation gates, the first transaction-style mutation-before-validation fixes on `Bind Familiar`, `Forestation`, the `Breath of the Goddess` cure family, `bindspectral`, and fetish binding plus the first startup-power `already_active` precheck batch on Beastial Call / Infusion Recharge / Sanctity / Imprisonment / Protection / Sky's Wrath / Hell on Earth and the summon-family startup safety batch on Familiar / Witch / Imp / Wither / Demon, at least six recovered dead rite paths (`Manifestation`, `summonwitch`, `summonimp`, `summondemon`, `summonwither`, `summonfamiliar`), the missing utility rites `iceshell` and `glyphictransform`, the spectral-stone pair (`spectralstone`, `bindspectral`), the staged `volcano` terrain rite, the staged `climatechange` biome-shift rite with dropped focus items and glowstone-driven biome selection, several corrected ring-size routes (`Forestation`, animal-calling, familiar rites, fertility, Sky's Wrath, portable Circle Talisman binding, most of the protection family), several corrected infernal rite mechanics (`Blindness`, `Blight`), a cleaner shared familiar `whiff_of_magic` ingredient collision, the major false-positive small/medium/large fallback activations, and the full snow-frame Spirit Portal plus manifestation-duration ghost-travel loop are back closer to original. The big remaining architecture gap is now narrower: the port no longer collapses new casts through the first detected ring size, but it still uses an ordered size retry and representative trigger items instead of the original full multi-circle, multi-sacrifice ritual registry. The fake redstone Charging buff fallback is gone again, the public `TNT` Crater overclaim is gone again, and the whole Charging family is back on closer original footing now that `chargestone` once more expects its original glowstone-dust, redstone, wood-ash, and quicklime support offerings, the infusion charge meter exists again, and the sustained `infusionrecharge` paired-circle upkeep rite is back too. The next parity emphasis therefore shifts away from that family and toward dispatcher accuracy, refund/upkeep lifecycle fidelity, then broader in-game Spirit World verification and sound/graphics QA. Step 12 now narrows to manifestation visual polish, corpse visibility polish, and broader Spirit World verification instead of missing portal mechanics, and step 13 trails behind that.
The last obvious Nightmare item-gap is also closed now: `Charm of Disrupted Dreams` exists again, has its original string-plus-fanciful-thread recipe, and once more blocks Nightmare armor stripping when carried. That means the next Spirit World work is no longer "missing core item coverage" but in-game verification, ghost presentation polish, and any remaining feel mismatches.
The missing `Curse of Corrupt Poppet` rite is also back now, which means the remaining poppet work has shifted away from missing core curse coverage and toward broader in-game verification plus any deeper recipe-shape parity that still sits behind the simplified dispatcher.
The `Banish Demon` effect family is back too, `Transpose Ores` now lives again as a staged large rite, and `Nature's Power` now lives again as a staged medium rite, so the remaining rite sweep is narrowing toward more obscure or architecture-heavy activation gaps rather than plainly absent core infernal rites.
The small otherwhere bound-waystone transposition path is also back closer to original now, since it once again uses the sacrificed waystone and moves nearby entities instead of silently collapsing into a caster-only inventory teleport.
The base small ritual bound-waystone binding path is back closer too, since it once again expects its Ender Dew + glowstone dust support offerings before summoning the fresh bound waystone at the ritual center instead of mutating player inventory.
The small ritual bound-waystone copy route is back closer to original too, since it once again turns one dropped bound waystone plus one extra offered unbound waystone and its original Ender Dew + diamond supports into two bound waystones instead of collapsing into the wrong Banish Demon representative path.
The medium otherwhere taglock transposition path is back too, since it now pulls the bound target into the circle again instead of silently collapsing into the old large-circle caster-pull false positive, and that obsolete large pull fallback is gone.
The altar is visually closer too now that both block and item use a shaped 3D form again, and the joined corner models no longer render Resurrected's hidden internal faces as stray yellow/grey panels. The remaining altar art work is now mostly final in-game orientation/feel verification rather than a flat-vs-3D or obvious internal-face mismatch.

## Finish-line batch order

Use this order for the remaining autonomous work until the port is in "playable parity + known polish gaps" shape:

1. Finish ritual dispatcher fidelity.
   The next wins are recipe-shape accuracy, safer refund/abort paths, and splitting more of the monolithic `RitualExecutor` into traceable helpers.
2. Finish Spirit World presentation and verification.
   Mechanics are back much closer to original now; the main remaining value is manifested-player presentation, corpse polish, and in-game feel verification for nightmares, portals, and dream-state transitions.
3. Finish machine and storage presentation parity.
   Distillery / Witch's Oven / Spinning Wheel GUI cleanup, Refilling Chest / Leech Chest / Poppet Shelf render polish, and any remaining unwrap/facing/model mismatches should be pushed to "recognisable and trustworthy".
4. Finish altar and special-block visual parity.
   Prioritise joined altar orientation verification, Crystal Ball translucency, Coffin item/block presentation, Fume Funnel art/facing, and the remaining wrong-wrap blocks/items listed in `ITEM_REVIEW.md`.
5. Run a targeted sound pass.
   Once the mechanics and visuals are no longer shifting heavily, audit ritual, machine, Spirit World, and special-block sounds against original Witchery and document any intentional placeholders.
6. Close with regression and doc cleanup.
   The final step is a subsystem-by-subsystem verification pass and a docs reconciliation so the remaining gaps are explicit, small, and believable.

## Current 5-point run

Use this as the current unattended run order for the next autonomous/daytime passes. Keep the scope narrow, finish one point before broadening, and prefer shipping one real parity improvement plus docs over opening several half-finished rewrites.

1. Finish the active item/block parity cluster first.
   Best implementation path:
   Stay on the remaining placed-block behavior mismatches that are still easy to prove from original 1.7.10: footprint/collision, support rules, hardness/resistance, light output, comparator/redstone behavior, flammability, lid/open rules, and simple interaction feedback on storage, machine, and ritual-adjacent blocks. Wicker Bundle now has the bloodied variant, Demonic Blood right-click conversion, and Horned Huntsman wickerman summon path restored; next pass should in-game verify the exact structure offsets/model pose and then move back to other ritual-adjacent blocks. Use original assets as source of truth and Witchery Resurrected only where it preserves the same visual result better on NeoForge. Prefer small state/property fixes before touching renderers.
2. Tighten ritual dispatcher fidelity and allow behavior-preserving refactors when they help.
   Best implementation path:
   Keep shrinking `RitualExecutor` only when the refactor directly improves parity work: extract activation-result helpers, sacrifice-resolution helpers, staged lifecycle/refund helpers, or clearer ring/ingredient dispatch helpers if they remove a real mismatch or make the next parity fix safer. Do not do abstract cleanup for its own sake; refactoring is allowed when it measurably improves correctness, traceability, or future parity speed.
3. Finish Spirit World and familiar polish that still affects play feel.
   Best implementation path:
   Prioritise manifested-player presentation, corpse visibility/state, remaining nightmare/portal edge cases, and familiar persistence/recreation details that still make the system feel less like original Witchery even though the core mechanics are already back.
4. Start the first targeted sound parity sweep once a subsystem is stable enough.
   Best implementation path:
   Focus first on ritual fail/sustain/complete cues, then machine active/burning/idle feedback, then Spirit World and special-block interactions. Use original Witchery 1.7.10 as the final judge for cadence and intent, and use modern NeoForge examples only for sound registration/playback patterns.
5. Close each autonomous batch with verification and docs, not just code.
   Best implementation path:
   After every real patch, run `./gradlew.bat compileJava processResources`, update `BUGS.md`, `FEATURE_MATRIX.md`, `ITEM_REVIEW.md`, `QA_PARITY_PLAN.md`, and/or this plan when repo truth changes, then commit. If a task turns out bigger than expected, document the exact remaining gap before moving on instead of leaving hidden drift.
