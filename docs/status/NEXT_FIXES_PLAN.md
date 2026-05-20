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
9. Restore special plant/block mechanics that are still plain blocks in the port: blood rose, bramble, void bramble, grassper, and glowglobe. Spanish Moss is no longer plain behavior-wise, Wild Bramble has its old toughness back, and Void Bramble now has its owner/unbreakable column-removal plus 32-block ritual-null-source behavior back; remaining plant work is mostly in-game verification and smaller edge-case cleanup.
10. Revisit altar-adjacent placed-item interactions so Arthana, Mystic Branch, chalice, and similar bonuses match original expectations.
11. Restore storage block behavior parity for Leech Chest, Refilling Chest, Dream Catcher handling, and Poppet Shelf edge cases.
12. Finish Spirit World parity around corpse/state transitions, flowing spirit interactions, and demonic nightmare edge cases.
13. Finish machine screen parity so Distillery, Witch's Oven, and Spinning Wheel GUI rendering matches their textures cleanly.
14. Restore deferred systems that still need real implementation, especially Custom Brews, Mirror World work, and remaining rite coverage.
15. Add a repeatable regression checklist so future parity work can be verified subsystem-by-subsystem instead of rediscovered manually.

## Finish-phase focus

The current pass started with steps 1, 5, and 6, and has now pushed step 2 again through the restored Summoning, Moving Earth, Raise Column, Crater, Protection, Sanctity, and Imprisonment rite lifecycles, step 7, most of step 9, the altar-adjacent parity work in step 10, and the practical Leech Chest / Taglock / Poppet Shelf / Dream Catcher / Refilling Chest behavior in step 11 forward.
The next highest-value remaining targets are now step 2 first: deeper ritual refund/abort behavior beyond Storm / Summoning / Moving Earth / Raise Column / Crater plus the remaining ritual activation mapping gaps around the still-modern ingredient dispatcher, now that the protection-family upkeep ratios, several missing storm/night/day ritual traits, the shared time gate helper, the Rain of Toads dispatcher path, refund-safe taglock target validation, the first restored ritual-vs-infernal chalk-family gates, the first restored otherwhere gates, a direct sustain-time ring-size check for active rites, outer-to-inner retry across all present ring sizes for new casts, an explicit matched-vs-no-match activation result that stops precondition-failed rites from falling through into unrelated ring retries, the first restored dropped-ingredient refund path for broken-circle and lost-upkeep aborts on staged rites, at least five recovered dead rite paths (`Manifestation`, `summonwitch`, `summonimp`, `summondemon`, `summonwither`), the missing utility rites `iceshell` and `glyphictransform`, the spectral-stone pair (`spectralstone`, `bindspectral`), the staged `volcano` terrain rite, the staged `climatechange` biome-shift rite with dropped focus items and glowstone-driven biome selection, several corrected ring-size routes (`Forestation`, animal-calling, familiar rites), several corrected infernal rite mechanics (`Blindness`, `Blight`), at least one real ingredient collision, the major false-positive small/medium/large fallback activations, and the full snow-frame Spirit Portal plus manifestation-duration ghost-travel loop are back closer to original. The big remaining architecture gap is now narrower: the port no longer collapses new casts through the first detected ring size, but it still uses an ordered size retry and representative trigger items instead of the original full multi-circle, multi-sacrifice ritual registry. The next parity emphasis therefore shifts from "missing rite coverage" to dispatcher accuracy, refund/upkeep lifecycle fidelity, then broader in-game Spirit World verification and sound/graphics QA. Step 12 now narrows to manifestation visual polish, corpse visibility polish, and broader Spirit World verification instead of missing portal mechanics, and step 13 trails behind that.
The last obvious Nightmare item-gap is also closed now: `Charm of Disrupted Dreams` exists again, has its original string-plus-fanciful-thread recipe, and once more blocks Nightmare armor stripping when carried. That means the next Spirit World work is no longer "missing core item coverage" but in-game verification, ghost presentation polish, and any remaining feel mismatches.
The missing `Curse of Corrupt Poppet` rite is also back now, which means the remaining poppet work has shifted away from missing core curse coverage and toward broader in-game verification plus any deeper recipe-shape parity that still sits behind the simplified dispatcher.
The `Banish Demon` effect family is back too, and both `Transpose Ores` and `Nature's Power` now live again as staged large rites, so the remaining rite sweep is narrowing toward more obscure or architecture-heavy activation gaps rather than plainly absent core infernal rites.
The small otherwhere bound-waystone transposition path is also back closer to original now, since it once again uses the sacrificed waystone and moves nearby entities instead of silently collapsing into a caster-only inventory teleport.
The medium otherwhere taglock transposition path is back too, since it now pulls the bound target into the circle again instead of silently collapsing into the large-circle caster pull mechanic.
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
   Stay on the remaining placed-block behavior mismatches that are still easy to prove from original 1.7.10: footprint/collision, support rules, hardness/resistance, light output, comparator/redstone behavior, lid/open rules, and simple interaction feedback on storage, machine, and ritual-adjacent blocks. Use original assets as source of truth and Witchery Resurrected only where it preserves the same visual result better on NeoForge. Prefer small state/property fixes before touching renderers.
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
