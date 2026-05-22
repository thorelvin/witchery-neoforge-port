## Item Texture Remaster Plan

> Process note: before continuing new remaster production, read [ITEM_TEXTURE_REDO_RESET.md](C:/claude/witchery/witchery-port/docs/art/ITEM_TEXTURE_REDO_RESET.md). The current remaster batches are now treated as experimental output, and the redo process has been reset around darker values, stronger slot-scale readability, and stricter native `32x32` pixel construction.

This file tracks the staged migration of Witchery item textures from mixed legacy assets into a consistent remastered set.

### Goals

- Keep Witchery silhouettes and motifs recognizable.
- Promote selected item families to `32x32` masters where extra detail helps.
- Preserve sharp pixel readability and avoid soft upscale blur.
- Remaster items in cohesive families so neighboring icons feel related in inventory.
- Create new thematic pixel art rather than copying or directly tracing original mod assets.
- Keep the original runtime sprites backed up outside the live asset path before each batch.

### Style Rules

- Preserve source-backed silhouettes unless there is a strong gameplay readability reason to change them.
- Use original Witchery, Resurrected, Enchanted, and local decompiled assets as thematic references only, not as final runtime art.
- A remaster batch is only considered fully done once the live textures are uniquely redrawn, not merely promoted or upscaled.
- Use top-left lighting and bright but controlled specular highlights.
- Avoid soft antialiasing, blurred edges, and painterly shading.
- Keep transparent padding around each item so icons do not crowd slot edges.
- Reuse palette ramps per material family:
  - silver / ironlike metals
  - koboldite blue metal
  - spectral green dust
  - necromantic purple-red stone
  - attuned / charged ritual stones
  - waystones / bound waystones

### Batch Order

1. Occult materials and stones
2. Metals and forged tools
3. Herbs, seeds, and powders
4. Bottles, brews, buckets, and chalks
5. Talismans, charms, and ritual curios
6. Wearables and armor-like items
7. Hero items and one-off artifacts

### Completed Batch

Batch 01: occult materials and stones

- `silver_dust.png`
- `ingredient.spectral_dust.png`
- `ingredient.necro_stone.png`
- `ingredient.attuned_stone.png`
- `ingredient.attuned_stone_charged.png`
- `ingredient.seer_stone.png`
- `ingredient.waystone.png`
- `ingredient.waystone_bound.png`

Batch 02: forged and ritual tools

- `arthana.png`
- `boline.png`
- `silver_sword.png`
- `silver_bolt.png`
- `handbow.png`
- `huntsmanspear.png`

Batch 03: herbology and seeds

- `ingredient.belladonna.png`
- `ingredient.mandrake_root.png`
- `ingredient.artichoke.png`
- `ingredient.wormwood.png`
- `ingredient.seeds_belladonna.png`
- `ingredient.seeds_mandrake.png`
- `ingredient.seeds_artichoke.png`
- `ingredient.seedswormwood.png`

Batch 04: fibers and frost reagents

- `disturbed_cotton.png`
- `wispy_cotton.png`
- `ingredient.disturbed_cotton.png`
- `ingredient.fanciful_thread.png`
- `icy_needle.png`
- `ingredient.icy_needle.png`
- `ingredient.seeds_snowbell.png`

Batch 05: powders and minor plant reagents

- `ingredient.ash_wood.png`
- `ingredient.berries_rowan.png`
- `ingredient.bat_wool.png`
- `ingredient.dog_tongue.png`
- `ingredient.owlets_wing.png`
- `ingredient.toe_of_frog.png`

Batch 06: buckets and chalks

- `bucket_hollowtears.png`
- `bucket_spirit.png`
- `chalk_ritual.png`
- `chalk_heart.png`
- `chalk_otherwhere.png`
- `chalk_infernal.png`

Batch 07: bottles and high-traffic brew icons

- `ingredient.brew_flowing_spirit.png`
- `ingredient.brew_hollow_tears.png`
- `ingredient.brew_hitchcock.png`
- `ingredient.brew_frogs_tongue.png`
- `ingredient.brew_sleep.png`
- `ingredient.brew_love.png`

Batch 08: additional brew bottles and related vessel follow-up

- `ingredient.brew_ink.png`
- `ingredient.brew_ice.png`
- `ingredient.brew_raising.png`
- `ingredient.brew_wasting.png`
- `ingredient.brew_revealing.png`
- `ingredient.brew_infection.png`

Batch 09: talismans, charms, and ritual curios

- `circle_talisman.png`
- `circle_talisman.1.png` through `.9.png`
- `devils_tongue_charm.png`
- `alluring_skull_off.png`
- `alluring_skull_on.png`

Batch 10: remaining ritual curios and utility icons

- `apple_of_sleeping.png`
- `brewbag.png`
- `congealed_spirit.png`
- `demonic_contract.png`
- `deathscowl.png`

Batch 11: wearables and visible character gear

- `babas_hat.png`
- `bark_belt.png`
- `biting_belt.png`
- `ice_slippers.png`
- `hunterboots.png`
- `hunterhat.png`

Batch 12: additional wearable gear and silvered variants

- `huntercoat.png`
- `hunterlegs.png`
- `hunter_boots_silvered.png`
- `hunter_chestplate_silvered.png`
- `hunter_helmet_silvered.png`
- `hunter_leggings_silvered.png`

Batch 13: robes, hoods, and death-aligned wearables

- `deaths_hood.png`
- `deaths_robes.png`
- `deaths_footwear.png`
- `deathsrobe.png`
- `deathsfeet.png`
- `deathshand.png`

Batch 14: lingering witch-attire wearables and footwear outliers

- `witches_hat.png`
- `witches_robes.png`
- `necromancer_robes.png`
- `ruby_slippers.png`
- `seeping_shoes.png`

Batch 15: occult handhelds and tethered ritual curios

- `mysticbranch.png`
- `polynesia_charm.png`
- `wandpott.png`
- `witch_hand.png`
- `taglock_kit.png`
- `taglock_kit.full.png`

Batch 16: Primer and specialist grimoire family

- `witches_primer.png`
- `ingredient.book_biomes.png`
- `ingredient.book_burning.png`
- `ingredient.book_circle_magic.png`
- `ingredient.book_distilling.png`
- `ingredient.book_herbology.png`
- `ingredient.book_infusions.png`
- `ingredient.book_oven.png`
- `ingredient.book_wands.png`

Batch 17: ritual vessels and workshop inserts

- `ingredient.chalice.png`
- `ingredient.chalice_full.png`
- `ingredient.clay_jar.png`
- `ingredient.clay_jar_soft.png`
- `ingredient.candelabra.png`
- `ingredient.fume_filter.png`

Batch 18: spectral stone family

- `witchery.spectralstone.png`
- `witchery.spectralstone.spectre.png`
- `witchery.spectralstone.banshee.png`
- `witchery.spectralstone.poltergeist.png`

Batch 19: poppet family

- `poppet.png`
- `poppet.protect_armor.png`
- `poppet.protect_death.png`
- `poppet.protect_earth.png`
- `poppet.protect_fire.png`
- `poppet.protect_poppet.png`
- `poppet.protect_starvation.png`
- `poppet.protect_tool.png`
- `poppet.protect_voodoo.png`
- `poppet.protect_water.png`
- `poppet.vampiric.png`
- `poppet.voodoo.png`

Batch 20: ritual bolt family

- `ingredient.bolt_stake.png`
- `ingredient.bolt_anti_magic.png`
- `ingredient.bolt_holy.png`
- `ingredient.bolt_splitting.png`

Batch 21: infernal contract family

- `ingredient.contract.png`
- `ingredient.contract_blaze.png`
- `ingredient.contract_evaporate.png`
- `ingredient.contract_fiery_touch.png`
- `ingredient.contract_resist_fire.png`
- `ingredient.contract_smelting.png`
- `ingredient.contract_torment.png`

Batch 22: Dream Weave family

- `ingredient.weave_dig_fast.png`
- `ingredient.weave_intensity.png`
- `ingredient.weave_move_fast.png`
- `ingredient.weave_nightmares.png`
- `ingredient.weave_saturation.png`
- `weave_fasting.png`
- `weave_fleet_foot.png`
- `weave_intensity.png`
- `weave_iron_arm.png`
- `weave_nightmares.png`

Batch 23: solid-brew payloads and helper overlay

- `ingredient.brew_web.png`
- `ingredient.brew_solid_dirt.png`
- `ingredient.brew_solid_erosion.png`
- `ingredient.brew_solid_overlay.png`
- `ingredient.brew_solid_sand.png`
- `ingredient.brew_solid_sandstone.png`
- `ingredient.brew_solid_stone.png`

Batch 24: reagents and bottled essences

- `ingredient.quicklime.png`
- `ingredient.gypsum.png`
- `ingredient.graveyard_dust.png`
- `ingredient.reek_of_misfortune.png`
- `ingredient.foul_fume.png`
- `ingredient.odour_of_purity.png`
- `ingredient.whiff_of_magic.png`
- `ingredient.breath_of_the_goddess.png`
- `ingredient.happenstance_oil.png`

Batch 25: spirit essences and bound-spirit token

- `ingredient.ghost_of_the_light.png`
- `ingredient.soul_of_the_world.png`
- `ingredient.spirit_of_otherwhere.png`
- `ingredient.infernal_animus.png`
- `ingredient.condensed_fear.png`
- `ingredient.focused_will.png`
- `ingredient.subdued_spirit.png`
- `soul_of_torment.png`

Batch 26: alchemical catalysts and potent distillates

- `ingredient.drop_of_luck.png`
- `ingredient.ender_dew.png`
- `ingredient.diamond_vapour.png`
- `ingredient.redstone_soup.png`
- `ingredient.oil_of_vitriol.png`
- `ingredient.tear_of_the_goddess.png`
- `ingredient.refined_evil.png`
- `ingredient.infernal_blood.png`

Batch 27: transformative media, ointments, and infusion prep

- `ingredient.anointing_paste.png`
- `ingredient.mutandis.png`
- `ingredient.mutandis_extremis.png`
- `ingredient.mysticunguent.png`
- `ingredient.flying_ointment.png`
- `ingredient.purified_milk.png`
- `ingredient.nullcatalyst.png`
- `ingredient.infusion_base.png`

Batch 28: broom, dreamflight, and woven night-magic supports

- `ingredient.broom.png`
- `ingredient.broom_enchanted.png`
- `ingredient.broom_overlay.png`
- `ingredient.exhale_of_the_horned_one.png`
- `ingredient.golden_thread.png`
- `ingredient.tormented_twine.png`
- `ingredient.charm_disrupted_dreams.png`
- `ingredient.mellifluous_hunger.png`

Focused readability correction: forged-tool silhouette cleanup

- `handbow.png` refreshed to read as a compact hand crossbow rather than a generic tool silhouette
- `arthana.png` refreshed to read as a short ritual dagger / mini sword

### Current Batch

 Batch 29: cleanup and remaining visible outliers

Likely candidates for the next bounded unique-redraw pass:

- audit remaining clearly non-unique survivors
- `debug_death_ward.png`
- remaining occult tools and one-off utilities still locked at legacy `16x16`
- any leftover one-off icons that still look like promoted placeholders

### Originality Status

- Batch 01 now serves as a uniquely redrawn baseline instead of a promoted placeholder family.
- Batch 02 now serves as a uniquely redrawn baseline instead of a promoted placeholder family.
- Batch 03 through Batch 28 now serve as the first uniquely redrawn remaster-family baselines.

### Concept Reference Notes

- Batch 15 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-15-occult-handhelds-concept.png` before the final pixel redraw pass.
- Batch 16 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-16-books-concept.png` before the final pixel redraw pass.
- Batch 17 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-17-vessels-and-filters-concept.png` before the final pixel redraw pass.
- Batch 18 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-18-spectral-stones-concept.png` before the final pixel redraw pass.
- Batch 19 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-19-poppets-concept.png` before the final pixel redraw pass.
- Batch 20 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-20-bolts-concept.png` before the final pixel redraw pass.
- Batch 21 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-21-contracts-concept.png` before the final pixel redraw pass.
- Batch 22 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-22-dream-weaves-concept.png` before the final pixel redraw pass.
- Batch 23 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-23-solid-brews-concept.png` before the final pixel redraw pass.
- Batch 24 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-24-reagents-and-essences-concept.png` before the final pixel redraw pass.
- Batch 25 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-25-spirit-essences-concept.png` before the final pixel redraw pass.
- Batch 26 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-26-alchemical-catalysts-concept.png` before the final pixel redraw pass.
- Batch 27 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-27-transformative-media-concept.png` before the final pixel redraw pass.
- Batch 28 used a saved high-resolution concept reference sheet at `docs/art/concepts/2026-05-22-batch-28-broom-and-dreamflight-concept.png` before the final pixel redraw pass.


### Backup Policy

For each remaster batch, copy the live pre-remaster textures into:

`docs/art/item-backups/<date>-<batch-name>/`

This keeps the originals close to the migration notes without leaving dead backup files inside the runtime asset tree.
