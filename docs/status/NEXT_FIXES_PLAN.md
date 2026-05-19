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
9. Restore special plant/block mechanics that are still plain blocks in the port: blood rose, spanish moss, bramble, void bramble, grassper, and glowglobe.
10. Revisit altar-adjacent placed-item interactions so Arthana, Mystic Branch, chalice, and similar bonuses match original expectations.
11. Restore storage block behavior parity for Leech Chest, Refilling Chest, Dream Catcher handling, and Poppet Shelf edge cases.
12. Finish Spirit World parity around corpse/state transitions, flowing spirit interactions, and demonic nightmare edge cases.
13. Finish machine screen parity so Distillery, Witch's Oven, and Spinning Wheel GUI rendering matches their textures cleanly.
14. Restore deferred systems that still need real implementation, especially Custom Brews, Mirror World work, and remaining rite coverage.
15. Add a repeatable regression checklist so future parity work can be verified subsystem-by-subsystem instead of rediscovered manually.

## Tonight's implementation focus

The current pass started with steps 1, 5, and 6, and has now pushed step 2 again through the restored Summoning, Moving Earth, Raise Column, Crater, Protection, Sanctity, and Imprisonment rite lifecycles, step 7, most of step 9, the altar-adjacent parity work in step 10, and the practical Leech Chest / Taglock / Poppet Shelf / Dream Catcher / Refilling Chest behavior in step 11 forward.
The next highest-value remaining targets are now step 2 first: deeper ritual refund/abort behavior beyond Storm / Summoning / Moving Earth / Raise Column / Crater plus the remaining ritual activation mapping gaps around the still-modern ingredient dispatcher, now that the protection-family upkeep ratios, several missing storm/night/day ritual traits, the shared time gate helper, the Rain of Toads dispatcher path, refund-safe taglock target validation, the first restored ritual-vs-infernal chalk-family gates, the first restored otherwhere gates, a direct sustain-time ring-size check for active rites, at least five recovered dead rite paths (`Manifestation`, `summonwitch`, `summonimp`, `summondemon`, `summonwither`), the missing utility rites `iceshell` and `glyphictransform`, several corrected ring-size routes (`Forestation`, animal-calling, familiar rites), several corrected infernal rite mechanics (`Blindness`, `Blight`), at least one real ingredient collision, and the major false-positive small/medium/large fallback activations are back closer to original; the big remaining architecture gap is still that new casts resolve through the first detected ring size instead of the original multi-circle registry, while the most obvious remaining missing rites are now `volcano`, `climatechange`, `spectralstone`, and `bindspectral`. Step 12 still follows with Spirit Portal / ghost-manifestation cleanup and broader in-game Spirit World verification, and step 13 trails behind that.
