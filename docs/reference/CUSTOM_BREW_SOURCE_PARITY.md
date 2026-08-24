# Custom Brew Source Parity

This ledger records the completed mechanical port of the original Witchery
`1.7.10-0.24.1` custom-brew registry and Witch's Cauldron bottling behavior.
It is the developer source of truth; player instructions belong in the
Witches' Brews Modonomicon book and `Book_of_Shadows.html`.

## Authoritative Registry

- Original registry: `WitcheryBrewRegistry.java`
- Source SHA-256:
  `17db77abb73bab8b4d3cc5bf827e1d545c5d1a6ba095a79a9229be0891b7abe3`
- Registration sites: `158`, retained in original source order
- Extractor: `tools/extract_brew_registry.py`
- Fixture:
  `src/test/resources/witchery/brewing/source_brew_registrations.json`
- Contract: `SourceBrewRegistryAuditTest`

The fixture records source line, action class, ingredient expression,
translation keys, altar power, effect level, duration, curability, and
nullifiers. Its largest groups are 48 `BrewActionEffect`, 40
`BrewPotionEffect`, 22 `BrewActionModifier`, 14 `BrewActionRitualRecipe`, and
six `BrewCurseEffect` registrations. All 21 source action classes have a
documented modern owner; no source registration remains unclassified.

## Modern Ownership

- `Brews` owns composition, capacity, modifiers, dispersal, potion effects,
  world actions, curses, ritual targets, colours, and source costs.
- `WitchCauldronBlock` and `WitchCauldronBlockEntity` own ordered stirring,
  station recipes, exact fluid volume, bottling, and persisted brew contents.
- `BrewingYield` owns the persisted 0-100 brewing skill and deterministic
  bottling yield calculation.
- `BrewMobEffectEvents` and the dedicated effect classes own ongoing entity
  behavior such as Reflect Damage, Volatility, Resizing, Hellish Aura,
  Darkness Allergy, Love, Spiked, and Waking Nightmare.
- Gas, liquid, thrown-bottle, bucket, triggered-curse, ritual, and renderer
  paths own delivery-specific behavior. Temporary water and lava holders
  restore displaced fluid instead of leaving permanent world edits.

## Bottling Contract

A full cauldron contains 3,000 internal fluid units. One bottle drains one of
the exact source tiers: `3000`, `1500`, `1000`, `750`, `600`, `500`, `375`,
`300`, `200`, or `100`, yielding `1`, `2`, `3`, `4`, `5`, `6`, `8`, `10`,
`15`, or `30` bottles per full cauldron. The visible three-level water state is
separate from the persisted exact amount, so partial bottling does not lose
fluid.

Yield is determined by brewing skill, Brewing Expertise, Witch's Hat or Baba
Yaga's Hat, Witches' Robes or Necromancer Robes, and an active Owl familiar.
Diamond capacity applies the original two-tier yield penalty. Skill training is
awarded only when the brewed capacity is demanding enough for the current
0-29, 30-59, 60-89, or 90-100 mastery band.

## Source Station Recipes

The cauldron supports the remaining original non-composed recipes:

- Mandrake Root + Exhale of the Horned One, then Egg -> six Mutandis
- Nether Wart, then Mutandis -> Mutandis Extremis
- Mandrake Root + Tear of the Goddess + Diamond Vapour + Ender Pearl + Wheat,
  then Mutandis -> Nether Wart
- Mandrake Root + Stone + End Stone, then Mutandis Extremis -> two End Stone
  for 1,800 altar power
- Nether Wart plus successive Glowstone Dust, Blaze Rod, and Charged Attuned
  Stone supports, then Snow Block -> Endless Water with 5, 20, 50, or 100 uses
  for 3,000 altar power
- Mandrake Root plus successive Glowstone Dust, Blaze Rod, and Charged Attuned
  Stone supports, then Charcoal -> Combustion Brew level 0-3
- raw pork, chicken, beef, odd pork, or mutton -> its cooked form
- Witch's Hand -> six Rotten Flesh
- Nether Wart + Tear of the Goddess + Vine + Red Mushroom, then a vanilla
  Compass -> blank Player Compass

Normal use stages source station ingredients. Sneak-use remains the explicit
custom-brew stir route when an ingredient has both meanings.

## Verification

- JVM: `BrewingYieldTest`, `SourceBrewRegistryAuditTest`,
  `CustomBrewMobEffectMathTest`, `BrewMobEffectEventsTest`,
  `DoubleJumpPayloadTest`, and the cauldron block/block-entity suites
- Book contract: `CauldronBookParityTest`
- Dedicated server: `witchery:custom_brew_environmental_actions`, covering
  timed water/lava restoration, web placement, and poisonous-toad expiry
- Release gate: `gradlew test build` and
  `python tools/runtime_test_harness.py all`

Mechanical source parity is closed. Remaining work is ordinary live-client
presentation, balance, multiplayer, and controller/input QA, not missing
registry ownership.
