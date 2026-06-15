# Porting Notes — Chronological API Gotchas

This is intentionally chronological. Do not load the whole file unless needed; search it for the API/entity/system you are working on.

Current target:
- Minecraft: 1.21.11
- NeoForge: 21.11.42
- Java: 21

---

## 2026-06-14 Custom brew guide references must be generated from the live registry

- The Primer `Brew Reagents` entry is generated from `Brews.ingredientReferences()`, which exposes the
  ordered `LinkedHashMap` brew registry as display metadata: item, ingredient kind, capacity/power,
  modifier ceilings, capacity increments, effect ids, action name keys, and explicit-stir status.
- Keep future book/help/JEI integrations on that API instead of copying a static ingredient table. The
  custom brew roster changes often, and a duplicate guide table will drift from the actual cauldron logic.
- Use `witchery:item_reference` pages when the guide needs an item-icon list. The page serializes real item
  ids, renders hoverable item stacks in the normal Primer, and has a placed-book renderer branch so the same
  generated page data remains readable on the block book.
- Some old source ids are not good display names (`mindrakebulb`, `attuned_stone_charged`,
  `tear_of_goddess`, etc.), so the Primer provider keeps a tiny item-name override list for historical
  id oddities and falls back to registry-id title casing for the rest.

## 2026-06-14 Dog Tongue ritualized brews must be state-safe on failure

- The live-cauldron `Tongue of Dog` release is a ritual trigger, not a normal stir. Failed releases must
  not consume the Tongue of Dog, empty the cauldron, clear the brew contents, or spend altar power.
- Keep ritual delivery validation two-phase: first reject structural brew failures (no effects/actions,
  drink-only dispersal), then run world checks for triggered item targets or gas/liquid placement. This
  avoids probing placement or item targets for brews that are not releasable rituals in the first place.
- The cauldron reserves altar power before final delivery because the delivery effect can mutate world
  state. If final delivery still returns a failure, immediately refund the reserved power through
  `AltarBlockEntity.refundPower(...)` before returning. Only shrink Dog Tongue and empty/clear the
  cauldron after `SUCCESS`.

## 2026-06-12 Placed guide book altar focus; custom brew liquid must be finite and non-displacing

- **Placed guide book altar bonus:** a `PLACED_MODONOMICON` block with a stored book directly on one of the
  six altar footprint blocks counts as a small "knowledge/focus" upgrade: +1 recharge scale, counted once,
  added beside skull/candle recharge bonuses, then multiplied by Pentacle and Infinity Egg. It deliberately
  does **not** add raw altar power/capacity. Because the book stack lives in `PlacedModonomiconBlockEntity`,
  changing empty/non-empty state must call `AltarBlockEntity.invalidate(level)` on the block below so the
  altar readout updates immediately instead of waiting for the periodic rescan.
- **Custom brew liquid flow:** source-style liquid brew is still a custom block, but its world behaviour must
  read as vanilla flowing liquid: never displace non-empty vanilla fluids, only the placed cell is a source,
  flowing cells carry decaying `DISTANCE`, require support/continuity, and evaporate when cut off. Rendering
  should use lower/tinted distance-state models so non-source cells do not look like full cubes. Keep item,
  cauldron, liquid, and particle colour derived from the same `BrewContents.color()` value.
- **Placed Modonomicon page rendering:** the block-book must use the same page list as the item Primer, not a
  duplicated static page table. Render page items through `ItemStackRenderState` in GUI display context and
  submit those flat layers to the page collector; falling back to block particle sprites makes logs,
  machines, stone blocks, and Distillery/Oven art diverge from the Primer. Counts are a separate text overlay.
- **Placed book model axes:** when viewing the open book from the player side, the debug move
  `x=+1, y=+2, z=+3` mapped visually to: x moved the right cover left, y moved it upward, and z pushed it
  deeper into the book/back cover. For the final right-cover alignment, reset bad x/y experiments and use
  z+ nudges. Do not rename visual left/right without checking the renderer's handedness first.
- **Apothecary goto crash breadcrumbs:** world-load crashes around `/witchery goto apothecary` may not leave a
  crash report if the integrated server dies during structure placement. `WitcheryVillageElement` should catch
  `Throwable` around individual piece placement and log the element id/box; `ApothecaryPiece` now logs guarded
  shell/decor/foundation/chest/sign/resident phases so the last log line identifies the failing phase.

## 2026-06-12 Modern MobEffects need icon textures (1.7.10 potions had none); Snow Trail continuity

- **Effect icons:** 1.7.10 `PotionBase` potions left `statusIconIndex = -1`, so the HUD drew no icon at
  all. Modern MC always resolves `textures/mob_effect/<id>.png` and shows the magenta/black checker when
  missing. Every custom `MobEffect` therefore needs an 18×18-ish PNG. The canonical Witchery icon set is
  in `decompiled/witcherycompanion-0.36.1-beta/assets/witchery/textures/mob_effect/` — copy from there
  (filenames mostly match our registry ids; `swim_boost.png` → our `swimming`). `sun_allergy` has no
  companion equivalent and is still iconless.
- **Snow Trail trail continuity (deliberate deviation):** source `PotionSnowTrail` stamps 4 footprints at
  the feet every 10 ticks and only on **air** foot blocks — so it skips tall-grass/fern columns and dots
  ~2 blocks apart at walk speed. The port keeps the temperature gate (`temp < 1.6`, hot biomes stay bare)
  but now stores the last foot pos in `entity.getPersistentData()` (`witchery_snowtrail_last`) and draws a
  continuous line between ticks, covering replaceable plants and following ±1–2 terrain steps. Use
  `getPersistentData().getLongOr(key, default)` / `putLong` for per-entity transient effect state.

## 2026-06-11 A block's `useItemOn` is SKIPPED when the player sneaks with a non-empty hand

Confirmed in `ServerPlayerGameMode#useItemOn` (MC 1.21.11):
```java
boolean flag = !mainHand.isEmpty() || !offHand.isEmpty();
boolean flag1 = (player.isSecondaryUseActive() && flag)
        && !(mainHand.doesSneakBypassUse(...) && offHand.doesSneakBypassUse(...));
if (event.getUseBlock().isTrue() || (event.getUseBlock().isDefault() && !flag1)) {
    blockstate.useItemOn(...);   // ← not reached when flag1 is true
}
```
So any block interaction designed as **"sneak + right-click with an item"** in `BlockBehaviour.useItemOn`
is unreachable for normal items (a plain `Item` returns `false` from `doesSneakBypassUse`). This bit the
Witch's Cauldron explicit-stir gesture (capacity ingredients like Mandrake Root could never be stirred).

**Fix pattern:** handle the sneak+item gesture in a `PlayerInteractEvent.RightClickBlock` listener — it
fires via `CommonHooks.onRightClickBlock` *before* that `flag1` gate (`if (event.isCanceled()) return
event.getCancellationResult();`). Gate the listener on `player.isSecondaryUseActive()`, do the work, then
`event.setCanceled(true)` + `event.setCancellationResult(...)`. Leave the non-sneak case uncancelled so the
block's own `useItemOn` still handles normal clicks. See `events/WitchCauldronEvents.onSneakStirBrew` →
`WitchCauldronBlock.stirExplicitIngredient`.

## 2026-06-10 Sky's Wrath focus = sword tiers (decode obfuscated 1.7.10 item ids; cross-check Resurrected)

The port triggered the three Sky's Wrath storm variants on Snowball / Water Bucket / Gunpowder. Source
`WitcheryRecipes` registers them as `SacrificeItem(Items.field_151041_m, itemAshWood)` (base),
`Items.field_151052_q + ash` (large), `Items.field_151040_l + ash + chargedStone` (portable) — those
obfuscated ids are **wooden / stone / iron sword**, not snowball/water/gunpowder. Confirmed two ways:
`field_151040_l` appears in `EntityGoblin`'s blacksmith sell list + watch-tower chest loot (= iron_sword),
and Witchery Resurrected's data-driven `data/witchery/rites/nature/{sky_wrath,large_sky_wrath,
portable_sky_wrath}.json` spell it out literally (`minecraft:wooden_sword` / `stone_sword` / `iron_sword`,
`data: 32767` = any damage, + `witchery:wood_ash`, optional `bound_waystone`, portable adds
`charged_attuned_stone`). The port's radii/bolts/power already matched; only the focus item was wrong.

**Lesson:** when a 1.7.10 sacrifice uses a bare `Items.field_######` id, *decode it* — don't infer the
modern item from the rite's flavour. The fastest confirmation is Resurrected's JSON rites (readable item
ids) and/or a second source usage of the same field. Here the project's own `Book_of_Shadows.html` had the
swords right all along; the code was the outlier. (`item.is(Items.WOODEN_SWORD)` matches any durability,
covering the source `data:32767`.) Also added the focus swords to `RitualEvents.selectIngredientSource`'s
small-ritual preference so the sword is picked as the trigger over the Wood Ash offering — same
offering-vs-trigger rule as the Creaking/Blight fixes.

## 2026-06-10 Staged ritual-sacrifice consumption (one item per 20 ticks, source SacrificeItem)

QA: rite offerings all vanished at once on activation; source pops them one at a time. Source
`SacrificeItem.addSteps` adds **one `StepSacrificeItem` per required item**, run sequentially; each step's
`process` only acts on `ticks % 20 == 0`, so it consumes **one sacrifice per 20 ticks**, emits an
`EXPLODE` particle + `RANDOM_POP` sound at the item, copies the stack into `sacrificedItems` (for refund),
decrements/removes the `EntityItem`, returns `COMPLETED` → next item; the rite's effect steps run only
after all sacrifices are gone.

The port consumed every offering instantly at the choke points `RitualExecutor.consumeOfferedItem`,
`RitualOffering.consumeOne()`, and the dropped trigger in `RitualEvents`. Added a transient,
per-dimension stager **`SacrificeConsumer`** those all route through: `stage(RitualOffering)` schedules
each item to pop at `max(queue) + 20` ticks (sequential, one per second); `tick(level)` (wired into
`RitualEvents.onLevelTick` beside `tickActiveRituals`) pops the due ones — `ParticleTypes.EXPLOSION` +
`SoundEvents.ITEM_PICKUP` (modern `random.pop`) then the real decrement
(`RitualExecutor.removeOfferedItemNow` / grassper shrink). The effect still applies immediately (the port's
rites are atomic functions, not source's step machine) — only the *removal* is staggered.

Two guards keep the still-present items from being reused: while pending, each item is non-pickable
(`setPickUpDelay` covering its pop) and its entity id is in a pending set that **both** offering finders
(`RitualOfferings.findNearbyOffering` ×2) and the `RitualEvents` trigger scan skip — so a rite cannot
re-detect its own un-popped offerings to double-fire. `cancelAllWithRefund` (server stop) flushes pending
pops so nothing is silently spared. **No refund dup:** ongoing-rite refunds store a *copy* at queue time
and the original is still consumed via its pop (or the flush), so net consumption is unchanged from the
instant path. Transient by design — a crash within the ~1–2 s pop window just leaves the items in world
(cosmetic). Headless fallback: `stage` with no `ServerLevel` consumes immediately.

## 2026-06-10 Rite of Imprisonment — every-tick inward pull (Attractive, not Barrier) — cadence fix

QA: Imprisonment activated ("hold creatures fast") but zombies slowly inched out. **First attempt
mis-identified the rite** (briefly ported it as a barrier cylinder); corrected after re-reading the
source registration. The `setUnlocalizedName` is chained onto each `RiteRegistry.addRecipe(...)`, so the
mapping in `WitcheryRecipes.java` (≈3042–3091) is:
- `RiteProtectionCircleRepulsive(4, 0.8F, 0)` → **`witchery.rite.protection`** (pushes mobs away — the
  port's Sanctity).
- `RiteProtectionCircleAttractive(4, 0.8F, 0)` → **`witchery.rite.imprisonment`** (pulls mobs inward).
- `RiteProtectionCircleBarrier(4, 5, 1.2F, false, 0)` → **`witchery.rite.barrier`** (a *separate* rite —
  the solid barrier cylinder; already ported as the port's Protection Barrier rites via
  `drawProtectionBarrier`).

So **Imprisonment is the Attractive pull, not the Barrier.** `RiteProtectionCircleAttractive.update` →
`attract()`: each tick, every `EntityCreature` whose **next** position (`pos + motion`) reaches the
boundary (`distance >= radius - 1`) gets its motion set ~0.22/axis **toward the centre**, Y zeroed;
players and the Ender Dragon are exempt. The base `RiteProtectionCircle.ProtectionCircleStep.process`
returns `UPKEEP` and is ticked **every game tick**, so the pull is continuous.

The port's pull logic (`pullProtectionMob` — same boundary gate + 0.22 toward-centre clamp) was already
faithful; the **only** bug was cadence: the rite was queued at `pulseInterval=20`, so the pull fired once
a second and mobs walked out in the other 19 ticks. Fix: `pulseInterval=1` (queue args `4,0,0,1,0,0.8f,0`
— radius 4, upkeep 0.8 to match source), and dropped the non-source Slowness the approximation had added.
**Lesson: protection-circle parity is mostly about the per-tick upkeep cadence — these `RiteProtectionCircle`
subclasses all run `update()` every tick.** The sibling Sanctity (Repulsive) rite still queues at
`pulseInterval=20` and has the same latent drift bug (left pending QA).

## 2026-06-10 Ritual concurrency + re-click toggle-cancel (source parity)

**Question raised in QA:** can multiple rites run at once? And why does an ongoing rite (Imprisonment)
never stop with a strong altar?

**Source model** (`BlockCircle.TileEntityCircle`, 1.7.10-0.24.1): each circle's tile entity holds two
lists — `activeRituals` (running their step sequence, processed one-at-a-time at index 0) and
`upkeepRituals` (ongoing maintained rites, *all* ticked each tick). `isRitualActive()` =
`!activeRituals.isEmpty() || !upkeepRituals.isEmpty()`. Activation (`func_149727_a` → `activateBlock`):
**if `isRitualActive()` → `deactivate()`** (toggle OFF — aborts the active rite via `abortNext`, clears
ALL upkeep rituals, and `ABORTED_REFUND` drops the consumed sacrifices back). So source enforces **one
ongoing rite per circle**, and **re-clicking an active circle cancels it**. Concurrency is achieved with
**separate circles** — each tile entity is independent.

**Port before this change:** active rites live in `RitualSavedData` keyed by `center`;
`hasActiveRitualAt(center)` already scoped the block per-circle, so concurrency across separate circles
already worked (matching source). The gap was the **re-click toggle** — the port *refused* re-activation
("already at its work") and the only way to stop an ongoing rite was to break a chalk glyph (fail
`hasSustainingCircle`). With a strong altar that out-recharges upkeep drain, that left ongoing rites
effectively unstoppable.

**Fix:** restored source's toggle in `RitualEvents.onRightClickBlock`. Added
`RitualExecutor.cancelActiveRitualsAt(level, center)` — removes every active rite at that centre,
refunds its `sacrificedIngredient` + `supplementalSacrifices` (mirrors source `ABORTED_REFUND`), clears
any climate plan, returns whether anything was cancelled. The toggle is **gated on `NO_MATCH`**: re-click
runs `execute(...)` first, and only if no new rite matched AND a rite is active at the centre does it
cancel (lang `witchery.ritual.cancelled`). This is deliberately *better* than source's blanket
deactivate-on-click: a valid new rite still runs instead of cancelling, so an **instant** rite (e.g.
`riteOfSummonImp` — it spawns immediately, never registers active state or calls
`ensureActiveRitualCenterAvailable`) can be cast on a concentric/second ring **while** an ongoing rite
keeps running. Two *ongoing* rites on one centre are still refused (the ongoing rite's own
`ensureActiveRitualCenterAvailable` returns `MATCHED_FAILED`, not `NO_MATCH`) — use separate circles,
as in source. `already_active` lang updated to point at the empty-hand toggle and second-circle option.

## 2026-06-10 Creaking familiar — Rite of the Creakening + trigger-selection gotcha

New **additive** feature (a Pale Garden / 1.21 Creaking tie-in): a `bind_creaking_familiar`
RitualDefinition (MEDIUM ring, `VARIANT_RITUAL`) binds a wild vanilla `Creaking` as a familiar.
Because `Creaking` is **not** a `TamableAnimal`, it gets a parallel implementation rather than the
`FamiliarEntity` path: per-entity attachment `WitcheryAttachments.CREAKING_FAMILIAR`
(`CreakingFamiliarData` record), `CreakingFamiliarHelper` (bind/restore/dismiss/follow/defend/
sit + the "freeze when a non-owner looks at it" tick), and `CreakingFamiliarEvents`
(`EntityTickEvent.Post` driver, death cancel while `isTearingDown`, shift-interact sit/dismiss).
The familiar API in `FamiliarHelper` was widened from `TamableAnimal` to `LivingEntity`
(`findBoundFamiliarEntity`/`describeFamiliarEntity`; `restoreBoundFamiliar` now returns
`LivingEntity`) so both paths flow through one set of callers (`FamiliarEvents`, `SeerStoneItem`,
`RitualExecutor.call_familiar`). Familiar type id = 5; `/witchery familiar creaking`.

**The activation gotcha (this is the reusable lesson).** A rite is triggered by `RitualEvents`
picking ONE dropped item near the circle centre as the "ingredient", then matching it against the
definitions. For a MEDIUM ritual ring with no special-cased trigger, `selectIngredientSource`
falls through to **"nearest dropped item of any type"**. The existing `bind_familiar` rite is robust
because its trigger items (Tear of the Goddess / Odour of Purity) are **also in its own offering
list** — so whichever offering lands nearest centre still satisfies the trigger predicate.
`bind_creaking_familiar`'s trigger is **Creaking Heart, which is NOT in its offering list**
(resin clump, pale oak sapling, Whiff of Magic, Tear of the Goddess, Mutandis Extremis), so when any
offering landed closer to centre, the wrong ingredient was selected, `matches()` failed, and the rite
silently fell through to "unknown ritual" — it appeared dead.

**Rule**: any rite whose trigger item is not one of its own required offerings MUST be preferred
explicitly in `RitualEvents.selectIngredientSource` (like the obsidian protection branch), or it
will only fire by luck of drop positions. Fixed by adding a Creaking-Heart-preferring
`nearestOfferingSource` branch to the MEDIUM ritual-ring case. (All 6 items still must be within the
1.5-block offering radius of centre — that constraint is shared by every rite.)

Also note: `riteOfBindCreakingFamiliar`'s in-effect `.support` "missing offerings" hint is currently
**unreachable** — the dispatch `.requiringOffering(...)` gate filters the definition out before the
effect runs, so missing offerings show the generic "unknown ritual" instead. Left as-is for parity
with the other offering-gated rites; revisit if per-rite "you're missing X" feedback is wanted.

## 2026-06-07 RitualExecutor → declarative dispatch (monolith split)

Replaced the `RitualExecutor` dispatch monolith (the docs' "biggest technical-debt hotspot")
with a **data-driven `RitualDefinition` registry**, in 11 behaviour-preserving slices, each
verified by `./gradlew test`. `RitualExecutor` went 7,423 → ~6,084 lines (−18%); the test suite
grew 107 → 130.

Architecture / gotchas:
- **`RitualDefinition`** is a rite as data: name, ring size, trigger `Predicate<ItemStack>`,
  required circle variant (`ANY_VARIANT` sentinel), `Effect` (`(Player, Level, BlockPos, ItemStack)
  -> boolean`), plus fluent `requiringOffering(...)`, `vetoedWhen(...)`, and `CompanionRing`
  varargs. The model is immutable: `requiringOffering`/`vetoedWhen` return copies.
- **`RITUAL_DEFINITIONS`** (74 entries) lives in `RitualExecutor`; `tryRitualDefinitions(size,
  item, variant)` checks, in order, `matches()` (size+variant+trigger) && companion rings present
  && offerings present && not vetoed, then runs the first match. `execute` calls it directly — the
  old `dispatch{Small,Medium,Large}` if-ladders are gone.
- **Ordered disambiguation**: one trigger → several rites is expressed by listing the offering-gated
  definition before its fallback (e.g. `bone_meal`: portable_fertility | fertility). Order in the
  list is the precedence.
- **Behaviour parity rule** used throughout: each migrated trigger is unique within its ring (or
  disambiguated by variant/offering), so consulting the table first yields exactly what the old
  chain would have; an absent companion/offering or a held veto skips the definition and falls
  through to the same NO_MATCH.
- **Extractions** (each its own file, same package): `RitualTransaction` + `RitualOffering`
  (cost/sacrifice), `RitualSavedData` (active-ritual state + codecs), `RitualOfferingSets` (~50
  `*Offerings` DTOs), `RitualOfferings` (the `findNearbyOffering` scan primitives + ~50
  `find*Offerings` matchers). `RitualOfferings` was reached from `RitualExecutor` via
  `import static …RitualOfferings.*` so the ~270 call sites (incl. `findNearbyOffering` ×213)
  needed no edits — note that **`import static` of package-private members works within the same
  package**, which kept the moved methods package-private.
- **Tests** (plain-JVM `unitTest`): `RitualDefinitionTest` (dispatch mappings/gating across rings,
  cross-ring disambiguation), `RitualDefinitionModelTest` (the data model directly), `AltarPowerTest`
  (Appendix-A power-source values via package-private `AltarBlockEntity.powerContribution`/
  `powerSourceCap` hooks), and a roster-integrity check (70 defs, unique names).
- **Debug**: `/witchery rite list` / `/witchery rite <name>` (GAMEMASTERS) force-run a definition's
  effect at the player, bypassing dispatch gating (held item = trigger); the rite's own internal
  power/offering checks still apply, so run near a powered altar.

Full slice-by-slice log is in `docs/status/NEXT_FIXES_PLAN.md` (architecture item #1).

## 2026-06-05 Guidebook progression — book-aligned unlock chain

Turned the finished 51-entry Primer into **one growing book** unlocked by crafting topic books.
Final shape: **12 categories** (Getting Started always open + 11 gated), 51 content entries +
11 "What Comes Next" recipe entries. Linear chain: Primer → Herbology → Brews & Infusions →
Collecting Fumes → Distilling → Circle Magic → Conjuration → Symbology → Poppets → Spirit World →
Creatures → Transformations.

Gotchas / decisions (Modonomicon 1.121.0):
- **Category locking = `BookCategoryModel.withCondition(BookAdvancementConditionModel.create()
  .withAdvancementId(id))`.** There is **no `hideWhileLocked` on categories** (only on entries).
- **To HIDE a locked category, the BOOK must be in INDEX display mode**
  (`BookModel.withDisplayMode(BookDisplayMode.INDEX)`). Confirmed by decompiling
  `BookParentIndexScreen` — its `init` filters categories through a `isUnlockedFor(player, category)`
  predicate (`lambda$init$3`), so an unmet condition removes the category button entirely. The NODE
  parent screen does not filter the same way.
- **Do NOT use `withShowCategoryButton(false)` for conditional hiding** — it is a *static* flag that
  hides the button permanently, even after unlock. It is unrelated to conditions.
- **Chain via advancements** under `data/witchery/advancement/guide/<key>.json`: criterion
  `minecraft:inventory_changed` on the held book → `rewards.recipes` unlocks the *next* book's recipe
  in the player's recipe book. Same advancement id is the category's unlock condition. `parent`:
  `minecraft:recipes/root` (so it behaves like a recipe-unlock advancement, no toast spam). The last
  link (transformations) has no reward.
- **First-join gift**: dedicated boolean attachment `WitcheryAttachments.RECEIVED_PRIMER`
  (`AttachmentType.builder(() -> FALSE).serialize(Codec.BOOL.fieldOf("value")).copyOnDeath()`) — a
  standalone attachment avoids the `WitcheryPlayerData` RecordCodecBuilder **16-field cap**.
  `serialize(...)` needs a `MapCodec`, so wrap a primitive codec with `.fieldOf(...)`.
- **Debug**: `/witchery guide unlock [all|<book>]` / `/witchery guide reset` grant/revoke the
  `witchery:guide/*` advancements via `player.getAdvancements().award/revoke(holder, "has_book")`,
  `holder` from `src.getServer().getAdvancements().get(id)`.
- Primer recipe is now cheap/recoverable: shapeless Book + black dye + feather (was witch_hand+Book).
- Cauldron Book + Observations stay **standalone** Modonomicon books, outside the chain.

Polish pass (same day):
- **Gating-order fix (the real one).** The reagents `odour_of_purity` / `breath_of_goddess` /
  `whiff_of_magic` are **Witches' Oven products** (sapling → fume: hawthorn→odour, birch→breath,
  rowan→whiff), *not* cauldron brews (corrected after the first pass got this wrong). `book_infusions`
  needs Odour of Purity, so it depends on the **Oven** — but Collecting Fumes (the oven book) sat at
  chain #3, *after* Brews & Infusions at #2: a deadlock. Fix: **reordered Collecting Fumes → #2,
  before Brews & Infusions (#3)** (swap `sort_number`s + the three affected advancement `rewards`:
  herbology→book_oven, oven→book_infusions, infusions→book_distilling). New chain: Herbology →
  Collecting Fumes → Brews & Infusions → Distilling → Circle Magic → … Audited end-to-end: every
  book's reagents are obtainable from strictly-earlier knowledge or vanilla.
- (Kept the cauldron/`anointing_paste` entry in Herbology — thematic, and Herbology's prose already
  used the cauldron — but it is *not* the gating fix; nothing in the chain needs cauldron products.)
- "What Comes Next" captions now correctly name the **Witches' Oven** sapling source for each brew.
- **Station recipe images.** Modonomicon's crafting page only renders vanilla 3×3 recipes, so custom
  station recipes (`witchery:oven`, `witchery:distillery`, kettle, cauldron) can't use it. Built
  `tools/recipe_image.py`: it composites the real station GUI (`textures/gui/witches_oven.png` /
  `distillery.png`) with item sprites placed at the **menu's own slot coordinates** (read from
  `WitchesOvenMenu`/`DistilleryMenu` `addSlot` calls), crops the panel, scales 2×, and centres it in a
  256×256 PNG (Modonomicon `BookImagePageRenderer` draws the top-left ~200×200 region). Witchery
  sprites resolve via each item model's `layer0`; vanilla sprites are pulled straight from the
  `*client-extra*.jar`. Output → `assets/witchery/textures/gui/book/<name>.png`, shown via
  `BookImagePageModel.create().withImages(id("textures/gui/book/<name>.png"))` (full texture-path
  identifier, **not** an item id). `withImages(Identifier...)` accepts multiple frames that cycle —
  room to animate alternates (e.g. Diamond Vapour vs Diamond) later, like the FTB wiki templates.
  Wired oven brews into Collecting Fumes and the distillery run into Distilling.
- **Kettle / Witch's Cauldron cards.** Those vessels have no slot GUI (you right-click items into the
  brew), so `vessel_card()` composites onto the original cauldron panel
  (`tools/recipe_assets/witch_cauldron_panel.png`): ingredient sprites in a row up top, result in the
  output slot, count badge drawn for stacks. The brew surface is **tinted to the brew's own colour**
  (the `color` field on `witchery:kettle_brewing` recipes; e.g. Brew of Sleeping → purple). Kettle
  cards build straight from the recipe JSON (`kettle_from_recipe`); the Witch's Cauldron *workings*
  (Mutandis / Mutandis Extremis / Drop of Luck) are **hardcoded in `WitchCauldronEvents`, not recipe
  JSON**, so their ingredient lists are specified by hand (the right-click trigger is just shown as the
  last item in the row, matching the FTB `Cg/Witches Cauldron` template). The panel is 2× scale, so it
  is scaled to fit Modonomicon's ~200px display width. Wired cauldron workings into Herbology's
  cauldron entry, Redstone Soup into the Kettle entry, Brew of Sleeping into Spirit World.
- **Custom page for live recipes + tooltips.** Baked recipe PNGs can't show item tooltips. Modonomicon
  is extensible, so the station recipes now use a custom page `witchery:station_recipe`
  (`book/page/StationRecipePage` + `StationRecipePageModel` datagen + client
  `StationRecipePageRenderer`). The page stores a background texture + a list of `(ItemStack, x, y)`
  slots; the renderer blits the empty GUI panel (`textures/gui/book/bg_{oven,distillery,cauldron}.png`,
  emitted by `tools/recipe_image.py::emit_backgrounds`) then calls
  `parentScreen.renderItemStack(g, x, y, mouseX, mouseY, stack)` per slot — a default method on
  Modonomicon's `ContentRenderingScreen` that draws the item **with its stack count and a deferred
  hover tooltip**. So every recipe item now has a real tooltip and count (Mutandis ×6, etc.).
  Registration: `LoaderRegistry.registerPageLoader(TYPE, ::fromJson, ::fromNetwork)` in
  `WitcheryMod.commonSetup` (via `enqueueWork`), and
  `PageRendererRegistry.registerPageRenderer(TYPE, page -> new StationRecipePageRenderer(...))` in
  `WitcheryClient.registerBookPageRenderers` (an `FMLClientSetupEvent` listener, mod bus, Dist.CLIENT).
  Network: write own fields then `super.toNetwork` (base writes anchor+condition last); `fromNetwork`
  reads fields, then anchor, then condition — matching `BookPage.toNetwork` order exactly (mismatch
  breaks book sync). **Why a page needs network code at all:** Modonomicon books are datapack-driven
  and loaded **server-side** (like recipes/advancements); the client renders them, so the server
  serializes every parsed book and ships it to clients via `SyncBooksMessage` on join / datapack
  reload — `toNetwork`/`fromNetwork` are that wire format. This runs in **singleplayer too** (the
  integrated server syncs over the loopback), so there is no client-only shortcut and a byte-order
  mismatch fails the same way in single- and multiplayer. Slot coords come from the station menus'
  `addSlot` calls (oven/distillery);
  cauldron/kettle ingredients are laid out in a row over the pot. This replaced the baked
  `recipeImage(...)` pages (PNGs deleted; the bg panels remain).
- **Image-page sizing gotcha (from in-game QA):** `BookImagePageRenderer` draws the **full 256×256
  texture** into the page frame (Patchouli-style), *not* a top-left 200×200 sub-region — the `200`s in
  its `blit` are the on-screen quad size, with `texWidth/texHeight = 256`. So content centred in a
  200×200 box lands **top-left with empty space to the right and below**. Fix: `_fit_center()` scales
  the panel to ~236px and centres it in the whole 256×256 canvas, so the recipe reads large and
  centred and the frame isn't mostly empty. Keep recipe-image captions short (1–2 sentences) — the
  image dominates the page and long text gets cramped underneath.
- **Art pass**: the 4 new books got dedicated 16×16 textures on the shared purple guide-book base
  (`ingredient.book_oven` as clean base) with thematic emblems — poppets = cream voodoo doll + red
  pin, spirit_world = pale-cyan wisp, creatures = amber familiar-eye, transformations = blood-moon
  (pale crescent + red drop). Matches the existing 7-book family (purple book + colored emblem).

## 2026-06-04 Modonomicon guidebook — Patchouli → Modonomicon migration (Phase 1)

Began replacing the (dead) Patchouli "Witches' Primer" with a native **Modonomicon** book.
Patchouli has no 1.21.11 build, so the old `assets/witchery/patchouli_books/witches_primer`
book never opened in-game; `WitchesPrimerItem` only printed a "patchouli_missing" message.

Decisions: Cloudsmith maven dependency; keep the legacy Patchouli JSON in parallel as the
conversion source (delete in a later phase); Phase 1 = minimal one-category/one-entry book.

Key facts + gotchas (NeoForge 21.11.42 / MDG 2.0.141 / Modonomicon 1.121.0):
- **Dependency**: repo `https://dl.cloudsmith.io/public/klikli-dev/mods/maven/`, coord
  `com.klikli_dev:modonomicon-${minecraft_version}-neoforge:${modonomicon_version}` with
  `{ transitive = false }`. Use plain `implementation` (compile API + runtime mod). MIT-licensed.
- **GatherDataEvent is split** in 21.11: `GatherDataEvent.Client` (assets) and `.Server` (data).
  The old combined `data()` run type is GONE — MDG run types are now
  `[client, clientData, serverData, gameTestServer, server, junit]`. The `data {}` run block in
  `build.gradle` must call `clientData()` (not `data()`), else `prepareDataRun` fails with
  "Trying to prepare unknown run: data". Task name stays `runData` (matches the block name).
- **Modonomicon registers books on `GatherDataEvent.Client`** (not Server) even though book JSON
  lands in `data/<modid>/modonomicon/books/...` — confirmed by decompiling its own
  `DataGenerators`. So our `WitcheryDataGenerators.onGatherData(GatherDataEvent.Client)` runs under
  `runData`/`clientData`.
- **Datagen API** lives in `com.klikli_dev.modonomicon.api.datagen` (providers) and
  `...api.datagen.book[.page]` (models). Low-level path: implement `BookSubProvider` (functional:
  `generate(BiConsumer<Identifier,BookModel>, HolderLookup.Provider)`), build with
  `BookModel.create(id, nameKey)` / `BookCategoryModel.create` / `BookEntryModel.create` / page
  models (`BookTextPageModel`, `BookSpotlightPageModel`, etc.), register via
  `NeoBookProvider.of(event, subProviders...)` → `generator.addProvider(true, provider)`.
- **Page types map 1:1** from the old book: patchouli text/spotlight/crafting/image/entity →
  Modonomicon `BookTextPageModel`/`BookSpotlightPageModel`/`BookCraftingRecipePageModel`/
  `BookImagePageModel`/`BookEntityPageModel`. No custom page types needed for current content.
- **Text format is Markdown**, not Patchouli `$()`. A converter ($()→Markdown) is Phase 2.
- **Lang collision avoidance**: Modonomicon's high-level `SingleBookSubProvider`/`EntryProvider`
  auto-generate lang into a `LanguageProviderCache` that an `AbstractModonomiconLanguageProvider`
  then writes to `assets/<modid>/lang/en_us.json` — which would COLLIDE with the hand-maintained
  `en_us.json`. Phase 1 sidesteps this entirely: page text is authored as literal `Component`s
  (`withText(Component.literal(...))`, rendered verbatim, no key), and book/category/entry names
  reuse EXISTING `witchery.book.*` keys (names are resolved as translation keys). Establishing the
  real lang strategy is a Phase-3 task.
- **Opening the book**: client-only `BookGuiManager.get().openBook(BookAddress.defaultFor(bookId))`.
  Book data is datapack-synced, so no server round-trip — open directly on the logical client behind
  a `level.isClientSide()` guard, from a client-only class (`WitchesPrimerScreenOpener`) so the GUI
  types never load on a dedicated server.

Verified: `compileJava`, `runData` (writes book/category/entry JSON), and `build` (incl. tests) all
green. In-game open is QA-pending (needs a client launch).

## 2026-06-03 Headless unit tests (ModDevGradle unitTest)

You can verify game-registry-dependent logic WITHOUT launching the client/server. The project
uses `net.neoforged.moddev` (MDG), which has built-in JUnit support that bootstraps the game in
a plain JVM:

```gradle
neoForge { ... unitTest { enable(); testedMod = mods."${mod_id}" } }
dependencies {
    testImplementation platform("org.junit:junit-bom:5.11.3")
    testImplementation "org.junit.jupiter:junit-jupiter"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}
test { useJUnitPlatform() }
```

Put tests in `src/test/java`; `Items`/`MobEffects`/etc. holders resolve after bootstrap. Run
`./gradlew test` (NOT a Minecraft launch — no VRAM, Ollama-safe). `BrewLogicTest` is the first
example (5 brew-engine assertions). This is the right tool for the NEXT_FIXES_PLAN #11 test
backlog (ritual dispatch, circle detection, no-consume-on-failure, etc.). The GameTest framework
in 21.11.42 is the heavier data-driven `GameTestInstance`/`TestData`/`TestEnvironmentDefinition`
path (needs a TEST_FUNCTION registry entry + environment + structure NBT) — prefer plain unit
tests for pure logic.

---

## 2026-06-03 Custom brew composition system + NeoForge 21.11 gotchas

Built `com.emoniph.witchery.brewing` (`Brews`, `BrewContents`) + `WitchCauldronBlockEntity`,
re-added `brew_bottle`/`splash_brew_bottle`/`lingering_brew_bottle` as composable items.
Faithful to Resurrected `BrewActionList`/`ModifiersEffect`: brew = ordered ingredient list;
modifier ingredients accumulate strength/duration/invert/dispersal, each effect ingredient
then applies its potion at the accumulated values and resets.

API gotchas hit this pass (1.21.11 / NeoForge 21.11):
- **Item colours are data-driven now** — `RegisterColorHandlersEvent.Item` no longer exists
  (only `.Block`). Put a `tints` array in the item-model definition (`items/<id>.json` under
  `model`) using a tint source. To tint from a custom value, stash the colour in the
  `CUSTOM_MODEL_DATA` component's `colors` list and read it with
  `{"type":"minecraft:custom_model_data","index":0,"default":<int>}`. layer0 = tintIndex 0.
- **`BaseEntityBlock.getTicker` must be `public`** (overriding a public `EntityBlock` method);
  `protected` fails with "weaker access privileges".
- **`BaseEntityBlock` default `getRenderShape` is `INVISIBLE`** — override to
  `RenderShape.MODEL` when converting a normally-modelled block (e.g. the Witch's Cauldron) to
  an EntityBlock, or it vanishes in-world.
- **`MobEffects` use plain names**: `JUMP_BOOST` (not `JUMP`), `SPEED`, `SLOWNESS`, `STRENGTH`,
  `WEAKNESS`, `HEALTH_BOOST`, `SLOW_FALLING`, etc.
- **Codec-backed entity/BE NBT**: `ValueOutput.store(key, Codec, value)` /
  `ValueInput.read(key, Codec) -> Optional<T>` are the clean way to persist a record via its
  Codec (used for `BrewContents` on the thrown entity and cauldron BE).
- A `DataComponentType` holding a record value needs both `.persistent(CODEC)` and
  `.networkSynchronized(STREAM_CODEC)`; build the StreamCodec via
  `ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()).map(...)`.

---

## 2026-06-02 Mystic Branch air-glyph symbol magic (port of the symbols system)

Ported `ItemMysticBranch` + `infusion.infusions.symbols.*` into a new
`com.emoniph.witchery.symbol` package (`StrokeSet`, `SymbolEffect`,
`SymbolEffectProjectile`, `EffectRegistry`) plus `entity.SpellEffectEntity`,
`network.SpellPreparedPayload`, and client `SymbolDrawState`.

Key mechanism + gotchas:
- **Stroke detection is client-only** (it watches local look deltas), so it lives in a
  client class driven from `WitcheryClientEvents.onClientTick` (`ClientTickEvent.Post`), NOT
  in the common `Item`. The recognised symbol is sent to the server via a C2S payload; the
  server stores it in transient `WitcheryPlayerData` fields (`preparedSpellId/Level`, not in
  the CODEC) and casts it on `releaseUsing`.
- **Stroke bytes** match source exactly: pitchDiff>=7 -> 0 (up), pitchDiff<=-7 -> 1 (down),
  yawDiff<=-7 -> 2, yawDiff>=7 -> 3. `Mth.wrapDegrees` the yaw delta. The first int of each
  source `StrokeSet(level, ...)` is the spell level; copy sequences verbatim. Once a symbol
  matches (`contains`), stop appending — the shortest matching prefix wins (faithful).
- **Cost curve**: `floor(2^(level-1) * baseCost)` infused energy, spent on release.
- **Static-init registry**: `EffectRegistry`'s `static final SymbolEffect` fields call
  `instance().add(...)` during class init; `INSTANCE` must be declared first. Call
  `EffectRegistry.bootstrap()` in `commonSetup` so it is populated before first use.
- **Deliberate port simplifications**: no Worship-potion level cap (drawn length picks the
  level), no Beast-infusion-gated unforgivable curses (not registered yet), no persistent
  spellbook/cooldown (only knowledge-free spells registered this slice).
- **Sound API**: `SoundEvents.AMETHYST_BLOCK_CHIME` is a plain `SoundEvent` (no `.value()`),
  while e.g. `SoundEvents.NOTE_BLOCK_*` are `Holder<SoundEvent>` (need `.value()` for
  `player.playSound(SoundEvent,...)`). `level.playSound(...)` accepts the Holder overload, so
  passing a `Holder<SoundEvent>` there works without `.value()`.

---

## 2026-06-02 1.7.10 setHardness auto-raises resistance (block strength parity)

1.7.10 `Block.func_149711_c(float h)` (setHardness) is not just hardness — it also does
`if (resistance < h*5) resistance = h*5`. So any source block that called only
`setHardness(h)` (no `setResistance`) effectively has blast resistance `h*5`.

Modern `.strength(float)` sets destroyTime AND explosionResistance to the *same* value, so
a single-arg `.strength(h)` port silently lowers blast resistance from `h*5` to `h`. To match
source exactly for such blocks, use `.strength(h, h*5)`. Example fixed this pass:
`BlockSilverVat` was `setHardness(8.0F)` → resistance 40; port had `strength(3.0f)`, now
`strength(8.0f, 40.0f)`. When a source block *did* call `setResistance(r)`, use that explicit
`r` instead (e.g. Statue Goddess `2.5/1000`, Leech Chest `25/1000`).

Also re-confirmed source `Material.iron` blocks should use `SoundType.METAL` (+ metal map
color): `BlockFumeFunnel` was ported with `STONE` sound and is now `METAL`.

---

## 2026-05-30 VillageWallGenerator / goto command bugs

**`/witchery goto` underground**: `Level.getHeight(MOTION_BLOCKING, x, z)` returns `0` (not `-64`) for cold/unloaded chunks. Old threshold `<= -59` missed it, sending the player to Y=0. Fixed: threshold is now `< 10`.

**No fence gate in wall openings**: `placeGatePillar` cleared terrain in middle cells but never placed a gate block. Fixed: places `OAK_FENCE_GATE` at `sy` (first air block above ground), facing set from whether the gate run is E-W or N-S.

**Gate detection missing roads**: old code only scanned the one INTERIOR cell directly adjacent to each WALL cell, ±4 height. Roads are often 1-3 cells inward from the wall. Fixed: scan from WALL cells inward up to depth 3, ±8 height, checking `DIRT_PATH` and `GRAVEL`. Also reduced `GAP_FILL` from 7 → 4 to reduce stub wall protrusions.

---

## 2026-05-28 WitcheryPlayerData — 16-field DFU codec limit

`RecordCodecBuilder.mapCodec` hard-caps at 16 fields. Adding vampire progression fields (`level`, `ultimate`, `ultimateCharges`) without wrapping them would push past this limit.

**Pattern used**: replace a simple `Codec.BOOL.optionalFieldOf("vampire")` with a single `VampireState.CODEC.optionalFieldOf("vampire_state")` that embeds all 4 vampire sub-fields. Total top-level fields stays at 16.

Any future expansion of player state fields must follow the same pattern — add a new nested state class (like `FamiliarState`, `InfusionChargeState`, `SpiritWorldState`, `VampireState`) instead of adding flat codec fields.

## 2026-05-28 BloodCrucibleBlock — BESR / model coordinate system

Block entity renderers in this project use the standard entity-model flip:
```java
ps.translate(0.5, 0.5, 0.5);
ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
ps.translate(0.0F, -1.0F, 0.0F);
```
After this transform, model coords map to block coords as:
- `block_x = 0.5 - model_px / 16`
- `block_y = 1.5 - model_py / 16`  ← model Y=24 → block y=0 (floor)
- `block_z = 0.5 + model_pz / 16`

`ps.translate(0, +N/16, 0)` in the rotated pose moves the part **down** in world space (positive is toward block floor). Use this for liquid fill offset.

---

## 2026-05-26 NeoForge 21.11.42 API surface vs decompiled source

The `ng_execute` Gradle cache contains **decompiled NeoForge sources that may be newer than 21.11.42**. Do not trust those source files as the ground truth for which APIs are available on the compile classpath. Always verify against the actual compiled binary (e.g. `javap -p <class-from-compiledWithNeoForge_*.jar>`).

Known methods that appear in the decompiled ShearsItem source but DO NOT exist in NeoForge 21.11.42's compile classpath:
- `LivingEntity.getSlotForHand(InteractionHand)` → use `hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND`
- `InteractionResult.sidedSuccess(boolean)` → use `isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER`

`IShearable.spawnShearedDrop` takes `ServerLevel`, NOT `Level` — cast when you have an `!isClientSide()` guard.

`AbstractGolem` does NOT exist in NeoForge 21.11.42 — use `net.minecraft.world.entity.animal.golem.IronGolem` and `SnowGolem` explicitly.

---

## Current target
- Minecraft: 1.21.11
- NeoForge: 21.11.42
- Java: 21

## Completed milestones

### 2026-05-24 parity-scan correction

Some early milestones below are historical implementation slices, not current full-parity claims. In particular, M22 Vampirism has grown beyond the original boolean/blood/sun/bite foundation: the Glass Goblet fill route now uses the source level-9 Blood Power gate, the Creatures of the Night Glass Goblet -> Elle -> Lilith chain has a practical port, drained villager/guard goblet conversion now spawns the Vampire mob, Sun Collector/Grenade counterplay is live, and Blood Crucible/ultimate-selection has a restored pass. Remaining vampirism backlog still includes broader Blood Power parity, Resist Sun, Zombie Respect, old cure flow polish, complete late-level gates, and exact guard passive blood-regeneration/village-side-effect polish. M14/M15/M11 machine notes are likewise historical foundations; Spinning Wheel, Distillery, and Brazier recipe/automation exactness now have first restored passes but still need hopper QA, while Kettle/Oven sided automation, Witches' Oven allowlist/byproducts/funnels, and Silver Vat smelt-byproduct storage remain open parity work. Crystal Ball now has source-style `LastUsedTime` recharge with the old strict `elapsed > 100` gate plus nearby-player subject selection for ordinary predictions, complete-bound-Waystone same-dimension/150-block scrying with the old power-only item-use path, filled Taglock/Blooded Waystone held-use scrying, source-style remote camera payload/view support, the 1200-tick held-use cadence, and old bound-stack max of 1. The merged action list now lives in `docs/status/NEXT_TASKS.md`.

- [x] **M1** — NeoForge project shell (mod loads, logger, registries skeleton)
- [x] **M2** — Registries and creative tab (all blocks/items registered, lang, textures)
- [x] **M2b** — All simple block JSON models and item models (flat icons)
- [x] **M2c** — BESR renderers for 12 complex blocks (Kettle, Oven, Distillery, Candelabra, Brazier, Chalice, Crystal Ball, Dream Catcher, Fume Funnel, Spinning Wheel, Poppet Shelf, Leech Chest)
- [x] **M2d** — Plant transparency (render_type cutout for cross/leaves blocks)
- [x] **M3** — Plants MVP (Belladonna, Mandrake, Artichoke — CropBlock with 5 stages, blockstates, models, loot tables)
- [x] **M4** — Ingredients (belladonna, mandrake_root, artichoke as plantable+ingredient items; wood_ash, rowan_berries, wormwood, exhale_of_horned_one, hint_of_rebirth)
- [x] **M5** — Kettle MVP (BlockEntity with tick, player interactions, item-throw-in, brew extraction)
- [x] **M6** — Custom kettle recipe type (RecipeType + RecipeSerializer + 10 JSON recipe files)
- [x] **M7** — First brew effects: sleeping (blindness+slow), frost (freeze water+slow), love (animal breeding), revealing (night vision+glowing)
- [x] **M17** — Tier 1 Ritual Rites (LARGE 7x7 ring added; ingredient-based dispatch — dropped ItemEntity at center determines rite; 7 new rites: TotalEclipse/coal, BeastialCall/wheat, Fertility/bone_meal, Charging/redstone, TheForest/sapling, Broiling/raw_food, Protection/default-large)
- [x] **M16** — Altar power system (AltarBlock+AltarBlockEntity scan 5-block radius every 100 ticks; power 0-1000 from witch wood/leaves/candelabra/brazier/chalice/statue/skull/demon heart; KettleBlockEntity.tick() replaced isPowered placeholder with findNearbyAltar scan + consumePower; right-click altar shows power level)
- [x] **M15** — Distillery (DistilleryRecipe with 2 ItemStack inputs; fuel-based; DistilleryMenu+Screen; REDSTONE_SOUP/DIAMOND_VAPOUR/BAT_WOOL items; 4 recipes)
- [x] **M14** — Spinning Wheel (SpinningRecipe extends AbstractCookingRecipe; no fuel — 100-tick auto-tick; SpinningWheelMenu + SpinningWheelScreen; FANCIFUL_THREAD item; recipes: cotton→2 thread, rowan_berries→1 thread; crafting: 4 thread→white_wool placeholder)
- [x] **M13** — Cotton/Wormwood/Snowbell crop batch (cotton AGE_3 with somnian+disturbed textures; wormwood+snowbell AGE_4 with own textures; the old Witch Crop / Rowan Seeds shortcut from this milestone was later removed again after decompiled/source comparison showed it was a false port-only path)
- [x] **M12** — All 13 brew effects implemented as an early slice (Frost now has source connected-water freezing, ice-shield, and entity ice-cage direct hits in practical item form; Raising originally spawned only two placeholder zombies and now has source-style top-block grave raising in practical item form with weighted zombie/skeleton/zombified-piglin spawns plus old enslaver metadata; Wasting originally shipped as an AoE Wither+Slow placeholder and now has source-style hunger/weakness/wither falloff plus fragile plant decay in practical drink form; Erosion originally degraded stone/dirt and now has source-style no-drop block erosion, Obsidian return, 8-damage acid hit, and equipment corrosion in practical item form; Foulness: AoE nausea+poison; Infection originally shipped as an AoE poison placeholder and now has source-style direct hit behavior for infested stone-family blocks, Villager zombification, and brief Slowness IX plus small magic damage; Love now has the source animal, Villager, and player-enslaved zombie birth branches in practical drink form; Bats now has the source no-drop bat burst plus short Slowness VI falloff in practical item form; Ink now has source compact Blindness falloff plus attack-target clearing in practical item form; Hitchcock now has source 3-4 temporary target ravens in practical item form; Sprouting now has source face-directed branch growth in practical item form; Substitution now has source dropped-block replacement in practical item form; Cursed Leaping now has source Jump Boost IV plus vertical shove falloff in practical item form; Frog's Tongue now has source compact thrower pull in practical item form; Webs now has the source compact cobweb cross in practical item form; Thorns now has source cactus-column block/entity hits in practical item form; Vines now has source horizontal side-hit vine strips in practical item form; Grotesque: Strength IV+Resistance+Slow; FlowingSpirit: Invisibility+SlowFall+NightVision; Foaming: places cobwebs; Poison: Poison III on drinker)
- [x] **M11** — Witches Oven (OvenRecipe extends AbstractCookingRecipe + SimpleCookingSerializer; WitchesOvenBlockEntity with furnace-style tick; WitchesOvenMenu + WitchesOvenScreen; 3 recipes: logs→ash, nether_wart→exhale_of_horned_one, quartz→lime_dye)
- [x] **Broom** — BroomEntity extends Mob (not Entity — MobRenderer needs LivingEntity; Entity-based vehicles need custom EntityRenderer); mobInteract() not interact() for Mob subclasses; empty readAdditionalSaveData/addAdditionalSaveData cause override errors when base class uses HolderLookup.Provider variant — just omit them; flight: setNoGravity(true)+manual deltaMovement in tick() using rider.zza and getLookAngle(); positionRider() places passenger on handle; BroomItem.use() spawns entity + startRiding(); /witchery spawn broom works
- [x] **M29** — Polish pass (K1 sounds: Banshee→PHANTOM, Spectre→VEX, Mandrake→SILVERFISH; K5 config: WitcheryConfig.java with ModConfigSpec registered via modContainer.registerConfig — BoolValue→ConfigValue<Boolean> in NeoForge 1.21.11; 13 crafting recipes: bone_needle/clay_jar_soft/quicklime/frozen_heart/graveyard_dust/attuned_stone/null_catalyst/anointing_paste/wormy_apple/wicker_bundle + 3 shaped (mutandis/null_catalyst_initial/quartz_sphere); 2 new items: anointing_paste/wormy_apple)
- [x] **M27** — Horror + Summoned Entities (Banshee/Spectre/Poltergeist: Monster+FlyingMoveControl+FlyingPathNavigation — FlyingMob base class rejects PathfinderMob goals; Treefyd/Ent: ground Monsters, passive Ent retaliates only; Mandrake: fast tiny mob with poison on hit; Demon: Monster+RangedAttackMob throws SmallFireball(level,entity,vec3.scale); GhostModel shared by 3 flying types with LAYER_BANSHEE/LAYER_SPECTRE/LAYER_POLTERGEIST; RangedBowAttackGoal needs explicit generic type <EntityClass> to avoid inference error; SmallFireball constructor is (Level, LivingEntity, Vec3) not (Level, LivingEntity, double, double, double))
- [x] **M24** — Witch NPCs (CovenWitchEntity: AbstractVillager+Mob.createMobAttributes, 5 random skins, 10 trades for witchery ingredients, WitchModel renderer; WitchHunterEntity: Monster+RangedAttackMob, targets vampire/werewolf players+Witch via NearestAttackableTargetGoal, bow ranged attack+melee, 3 random skins; WitchHunterModel: biped+hat brim+hat top+coat; /witchery spawn command; API notes: ItemCost in trading package not world.item, AbstractVillager needs rewardTradeXp override, SpawnGroupData needs explicit import)
- [x] **M23** — Lycanthropy + 3D werewolf render layer (WerewolfModel: HierarchicalModel from ModelWerewolf 64×64, body hunched PI/9, digitigrade legs, ears/snout/mane/tail; WerewolfLayer: RenderLayer on PlayerRenderer via EntityRenderersEvent.AddLayers; only renders when isWerewolf+fullMoon+isNight; texture=wolfman.png copied from resurrected werewolf.png; API notes: AddLayers.getSkins() returns Set<PlayerSkin.Model> not Set<String> in 1.21.11; renderToBuffer(PoseStack, VertexConsumer, light, overlay) order — PoseStack first) (WerewolfEvents.java: full moon=getMoonPhase()==0; wolf form on full-moon night: Speed III+Strength II+Jump II+Hunger II+NightVision refreshed every 100t; transformationLevel set to 1f on first activation; Curse of the Wolf ritual=LARGE+BONE/BONE_BLOCK; TODO: silver weakness, wolfsbane debuff, howling)
- [x] **M22** — Vampirism (VampireEvents.java: tick drain 1/100t, igniteForTicks(40) sunburn in daylight+sky+no helmet, blood thresholds 0-50 frenzy/51-150 weak/151-349 night vision/350-500 speed+strength; EntityInteract bite=empty hand+non-player LivingEntity→2 damage+bloodValue; Creatures of the Night ritual=MEDIUM+DEMON_HEART→setVampire+250 blood; API note: setSecondsOnFire→igniteForTicks in 1.21.11)
- [x] **M21** — Environment & Transport Rituals (7 new rites; SMALL: Transposition=ender_pearl→respawn point, PriorIncarnation=rotten_flesh→recall items from last death within 16b; MEDIUM: SkysWrath=gunpowder→12 real lightning strikes, Sanctity=gold_ingot→repel mobs 25b + Resistance II, Imprisonment=obsidian→Slowness V+Weakness on mobs in 10b; LARGE: MovingEarth=dirt/gravel/sand→raise 5×5 column 5b, BrokenEarth=flint→dig 3-wide 5-deep trench in look direction; WitcheryPlayerData gains lastDeathPos+lastDeathDimension; PoppetEvents LOWEST-priority handler records death pos when not cancelled)
- [x] **M20** — Familiar Entities (Owl/Cat/Toad custom entities extending TamableAnimal; WitcheryEntities DeferredRegister; OwlModel+ToadModel ported from original 64x32/32x32 UV layouts; CatFamiliarModel now restores the old RenderOcelot-style body/leg/tail layout with Witchery blackcat texture and sit state; three MobRenderer subclasses; RiteBindFamiliar via feather/fish/slimeball in SMALL circle; Owl warns of nearby mobs, Toad collects dropped items, Cat avoids Phantoms/Creepers)
- [x] **M19** — Infusion Base System (InfusionType enum; WitcheryPlayerData + SyncPayload updated; 10 new items: 4 brew bases, mystic unguent, flying ointment, happenstance oil, infused brew base + 2 infused brews; 4 kettle recipes; Rite of Infusion dispatches per brew base; InfusionEvents passive tick effects; all item/block textures audited and filled from original assets)
- [x] **M18** — Taglock Kit + remaining poppets (TaglockKitItem collects UUID+name via right-click; ProtectionPoppetItem with 6 ProtectionTypes; VoodooPoppetItem; VampiricPoppetItem; PoppetEvents handles death prevention across all types + vampiric damage redirect)
- [x] **M10** — Poppet MVP (right-click player to bind UUID; tooltip shows bound player; death prevention consumes poppet + restores 2 hearts + totem particles)
- [x] **M8** — Ritual circle MVP (ChalkItem places CIRCLE blocks; arthana right-click triggers circle scan; two rites: Infusion 3×3 → Regen+Speed, Summoning 5×5 → Strength+NightVision+lightning)
- [x] **M9** — Player data foundation (AttachmentType with CODEC; fields: vampire, werewolf, bloodLevel, transformationLevel, familiarUUID; server→client sync via SyncPlayerDataPayload; persists through death/respawn)

## Notes

### 2026-05-18 — MC 1.21.11 / NeoForge 21.11.42 migration

Full compile-clean migration from 1.21.1 (NeoForge 21.1.229) → 1.21.11 (NeoForge 21.11.42). Every item below caused a real compile error.

**Entity combat**
- `LivingEntity.hurt(DamageSource, float)` is now `final` in Entity. Override point is `hurtServer(ServerLevel, DamageSource, float) → boolean`. Return `super.hurtServer(...)` to chain.
- `Entity.causeFallDamage(float, float, DamageSource)` → first param is now `double`.
- `Entity.doHurtTarget(Entity)` → `doHurtTarget(ServerLevel, Entity)`.

**Goal AI**
- `NearestAttackableTargetGoal` 4th-arg `Predicate<LivingEntity>` replaced by `TargetingConditions.Selector` (two-arg FI): `(e, sl) -> condition`. Method refs that only take one arg must become lambdas.

**Entity spawn**
- `EntityType.create(ServerLevel)` removed. Use `create(Level, EntitySpawnReason)` or the 6-arg overload with `Consumer<E>`.
- `TamableAnimal.setOwnerUUID(UUID)` removed. Use `setOwner(LivingEntity)`.

**Entity render states**
- `AvatarRenderState` has `public int id` but `HumanoidRenderState` does not. For non-player mob renderers that need entity ID (e.g. skin variation), create a custom `RenderState extends HumanoidRenderState` with `entityId` field and override `extractRenderState` to populate it.
- `EntityRenderersEvent.AddLayers.getSkins()` now returns `Set<PlayerModelType>`, not `Set<String>`. `.getSkin(String)` removed; use `.getPlayerRenderer(PlayerModelType)` (returns `AvatarRenderer<AbstractClientPlayer>`).

**Item tooltip**
- `Item.appendHoverText` signature changed completely:
  - Old: `(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)`
  - New: `(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`
  - Change `List<Component> tooltip` param to `Consumer<Component> tooltip`.
  - Change `tooltip.add(x)` to `tooltip.accept(x)`.
  - Add `net.minecraft.world.item.component.TooltipDisplay display` as 3rd param.
  - Import `net.minecraft.world.item.Item` if using `Item.TooltipContext`.
  - `super.appendHoverText(...)` calls must include the `_display` arg.

**Item rendering (placed block entity)**
- `ItemModelResolver.updateForTopItem` signature:
  - Old: `(ItemStackRenderState, ItemStack, ItemDisplayContext, boolean, Entity, Entity, int)`
  - New: `(ItemStackRenderState, ItemStack, ItemDisplayContext, Level, ItemOwner, int)`
  - `ItemOwner` is an interface with `level()`, `position()`, `getVisualRotationYInDegrees()`. Implement inline with an anonymous class for BESRs.

**Tools**
- `PickaxeItem` class removed. Use `ToolMaterial.IRON.applyToolProperties(new Item.Properties(), BlockTags.INCORRECT_FOR_IRON_TOOL, miningSpeed, attackDamage, attackSpeed)`.
- `ToolMaterial.applySwordProperties(Properties, attackDamage, attackSpeed)` for swords.

**NBT / CompoundTag**
- `CompoundTag.getList(String, byte)` → `getListOrEmpty(String)` (or `getList(String)` which returns `Optional<ListTag>`).
- `CompoundTag.getCompound(String)` → returns `Optional<CompoundTag>`. Use `getCompoundOrEmpty(String)` for non-null.
- `CompoundTag.getString(String)` → returns `Optional<String>`. Use `getStringOr(String, "")`.
- `CompoundTag.getInt(String)` → returns `Optional<Integer>`. Use `getIntOr(String, 0)`.
- `CompoundTag.getByte(String)` → returns `Optional<Byte>`. Use `.orElse((byte)0) & 0xFF`.
- All of the above apply to **both** string-literal keys and constant-variable keys. The regex-based fix script only patches string literals — find constant usages manually (e.g. `tag.getString(TAG_NAME)` → `tag.getStringOr(TAG_NAME, "")`).
- `TagParser.parseTag(String)` → `TagParser.parseCompoundFully(String)` (throws `CommandSyntaxException`).
- `ListTag.getString(int)` → `ListTag.getStringOr(int, "")`.
- `CompoundTag.getLong(String)` → returns `Optional<Long>`. Use `.orElse(0L)` or store as string. The `putLong/getLong` roundtrip requires `cd.getLong("key").orElse(0L)` on read.

**ValueInput / ValueOutput (block entity NBT)**
- `ValueOutput.putUUID` and `ValueInput.getUUID/hasUUID` **do not exist** in this API. Store UUIDs as strings: `tag.putString("key", uuid.toString())` / `UUID.fromString(tag.getStringOr("key", ""))`.
- `ValueOutput.put(String, CompoundTag/ListTag)` does not exist. Store lists as comma-separated strings, or use `childrenList(key)` / `childrenListOrEmpty(key)`.
- **No `getList` on ValueInput** — ValueInput is not CompoundTag.

**Block API**
- `ScheduledTickAccess` package: `net.minecraft.world.ticks.ScheduledTickAccess` → **`net.minecraft.world.level.ScheduledTickAccess`** (both import and FQ usage in method signatures).
- `ServerLevel.blockUpdated(BlockPos, Block)` removed. Use `Level.sendBlockUpdated(BlockPos, BlockState, BlockState, int)`.
- `ChunkAccess.setBlockState(BlockPos, BlockState, boolean)` → second boolean replaced by `int` flags. Use `0` instead of `false`.
- `BlockBehaviour.entityInside` extra params added: `(BlockState, Level, BlockPos, Entity, InsideBlockEffectApplier, boolean)`. `InsideBlockEffectApplier` is in `net.minecraft.world.entity` (not `world.level.block`).

**Level utilities**
- `Level.isDay()` / `isNight()` removed. Use `getDayTime() % 24000 < 12000` (day) or `>= 12000` (night).
- `Level.getMoonPhase()` removed. Use `(getDayTime() / 24000L) % 8L == 0` for full moon.
- `ServerLevel.getMoonBrightness()` → `getMoonBrightness(BlockPos)`. Or use the moon-phase calculation above.
- `LevelHeightAccessor.getMinBuildHeight()` → `getMinY()`. `getMaxBuildHeight()` → `getMaxY()`. Both old names are absent from bytecode entirely.

**Recipe system**
- `RecipeManager.getAllRecipesFor(RecipeType)` removed. Use `level.recipeAccess().recipeMap().byType(type)` returns `Collection<RecipeHolder<T>>`.
- `SingleItemRecipe.result()` is `protected`. Use `assemble(SingleRecipeInput, registryAccess)` to get the output `ItemStack`.
- `AbstractCookingRecipe` no longer takes `RecipeType` in its constructor.

**Spawn/sleep**
- `ServerPlayer.getRespawnPosition()` / `getRespawnDimension()` removed. Use `sp.getRespawnConfig()` which returns `ServerPlayer.RespawnConfig` (nullable). Pos is `respawnConfig.respawnData().pos()`, dimension is `.dimension()`.
- `ServerPlayer.RespawnPosAngle.of(Vec3, BlockPos)` → now requires a third `float yaw` arg.
- `Player.BedSleepingProblem.NOT_POSSIBLE_NOW` removed. `BedSleepingProblem` is now a record with only `TOO_FAR_AWAY`, `OBSTRUCTED`, `OTHER_PROBLEM`, `NOT_SAFE`.
- `BedSleepingProblem.getMessage()` → `.message()`.

**Permissions / commands**
- `CommandSourceStack.hasPermissions(int)` removed. Use `src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))` for op-level 2. Enum values: `ALL(0)`, `MODERATORS(1)`, `GAMEMASTERS(2)`, `ADMINS(3)`, `OWNERS(4)`.

**Networking**
- `PacketDistributor.sendToServer(payload)` removed from the common class. Client-to-server is now `net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload)`.

**Fuel values**
- `ItemStack.getBurnTime(RecipeType, FuelValues)` removed. Use `level.fuelValues().isFuel(stack)` or `level.fuelValues().burnDuration(stack)`.

**Mod loading**
- `FMLEnvironment.dist` (field) removed. Use `FMLEnvironment.getDist()` (method).

**Sound events**
- `SoundEvents.THUNDER_HIT` removed. Equivalent is `SoundEvents.LIGHTNING_BOLT_THUNDER`.

**ChunkGenerator**
- `ChunkGenerator.applyCarvers` no longer has a `GenerationStep` 7th parameter. Signature is now `(WorldGenRegion, long, RandomState, BiomeManager, StructureManager, ChunkAccess)`.
- `fillFromNoise(Blender, RandomState, StructureManager, ChunkAccess) → CompletableFuture<ChunkAccess>` is now abstract and must be implemented (return `CompletableFuture.completedFuture(chunk)` for flat/maze generators).
- `NoiseColumn` moved to `net.minecraft.world.level.NoiseColumn` (was `world.level.chunk.NoiseColumn`).

**SavedData (level-persistent data)**
- `SavedData.Factory<T>` is **gone**. The new pattern uses `SavedDataType<T extends SavedData>`:
  ```java
  public static final Codec<MyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.INT.fieldOf("key").forGetter(d -> d.field)
  ).apply(inst, MyData::fromCodec));
  public static final SavedDataType<MyData> TYPE = new SavedDataType<>(
      "save_id", MyData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  // Retrieve: overworld.getDataStorage().computeIfAbsent(TYPE)
  ```
- `SavedData` only has `setDirty()` / `isDirty()`. No `save()`/`load()` — serialization is driven by the Codec in `SavedDataType`.

**Entity position / rotation**
- `Entity.moveTo(double, double, double, float, float)` does **not** exist in bytecode. The correct methods are:
  - `snapTo(double, double, double)` — position only
  - `snapTo(double, double, double, float, float)` — position + yaw + pitch  ← **use this**
  - `snapTo(BlockPos, float, float)`, `snapTo(Vec3)`, `snapTo(Vec3, float, float)`
- Do NOT call `moveTo(double,double,double,float,float)` or `moveTo(double,double,double)` — neither exists in the compiled class.

**Entity renderers (simple)**
- All `MobRenderer` subclasses must implement `createRenderState()`. If using `LivingEntityRenderState`, override: `public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }`.

### 2026-05-18 — Item review fixes (LeavesBlock, BESR facing, block registration)

**LeavesBlock is abstract in NeoForge 21.11.42**
- The decompiled source shows `LeavesBlock(BlockBehaviour.Properties)` as concrete, but NeoForge patches it to be abstract with two abstract methods:
  - `codec()` — must return `MapCodec<? extends LeavesBlock>`
  - `spawnFallingLeavesParticle(Level, BlockPos, RandomSource)` — particle spawn hook
- Also the constructor takes `(float leafParticleChance, BlockBehaviour.Properties)`, not just `Properties`.
- Use the concrete subclasses: `UntintedParticleLeavesBlock(float chance, ParticleOptions particle, Properties)` or `TintedParticleLeavesBlock`. For modded leaves, create a class extending `UntintedParticleLeavesBlock`:
  ```java
  public class RowanLeavesBlock extends UntintedParticleLeavesBlock {
      public RowanLeavesBlock(BlockBehaviour.Properties p) {
          super(0.02f, ParticleTypes.PALE_OAK_LEAVES, p);
      }
  }
  ```
- Do NOT try to override `codec()` with a tighter generic type — it will fail with incompatible return type.

**DirectionProperty removed — use EnumProperty<Direction>**
- `net.minecraft.world.level.block.state.properties.DirectionProperty` no longer exists.
- Replace with `EnumProperty<Direction>` from the same package.
- `BlockStateProperties.HORIZONTAL_FACING` is typed as `EnumProperty<Direction>`.

**BESR custom render state with facing**
- To pass block-entity data to a BESR's `submit()`, create a custom render state:
  ```java
  public static class State extends BlockEntityRenderState {
      public Direction facing = Direction.NORTH;
  }
  ```
- Override `createRenderState()` to return the custom type.
- Override `extractRenderState` (5-arg: `T be, S state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay`). Call the default with `BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay)` — use the interface-default syntax, not `super.`, because the interface method is a default method, not a superclass method.
- For blocks where facing is stored in block state (not BE): read via `be.getBlockState().getValue(MyBlock.FACING)` inside `extractRenderState`.

**BESR Y-rotation for HORIZONTAL_FACING**
- `Direction.toYRot()` returns the yaw angle matching Minecraft's block-facing convention (SOUTH=0, WEST=90, NORTH=180, EAST=270).
- Use `Axis.YP.rotationDegrees(-facing.toYRot())` to make a model face the direction of the `FACING` property.

**Leaf block decay requires #minecraft:logs tag**
- `LeavesBlock` computes decay distance by checking `BlockTags.LOGS` on adjacent blocks. If your modded log block isn't in `#minecraft:logs`, leaves will always decay (distance=7). Add your logs to `data/minecraft/tags/block/logs.json`.

**Fence connection requires #minecraft:fences tag**
- Non-vanilla `FenceBlock` instances need to be added to `data/minecraft/tags/block/fences.json` so the connection logic recognises them.

**Plank tags for vanilla recipes**
- Modded planks are not automatically usable in vanilla recipes (crafting table, sticks, etc.). Add them to `data/minecraft/tags/block/planks.json` and `data/minecraft/tags/item/planks.json`.

**Block and item registration (CRITICAL)**
- `BlockBehaviour.Properties` requires `.setId(ResourceKey<Block>)` to be called before the Block constructor runs. In 1.21.11, `Block.<init>` calls `properties.effectiveDrops()` which asserts the ID is non-null.
  - `BLOCKS.register(name, () -> new MyBlock(BlockBehaviour.Properties.of()...))` **breaks** — ID isn't set in time.
  - Use `BLOCKS.registerBlock(name, props -> new MyBlock(props.mapColor(...).strength(...)), BlockBehaviour.Properties.of())` instead. `props` has the ID pre-set by NeoForge; chain builder methods on it.
  - For blocks with extra constructor args: `BLOCKS.registerBlock(name, props -> new DoorBlock(BlockSetType.OAK, props.strength(...)), BlockBehaviour.Properties.of())`.
  - For custom Properties helpers (e.g. cropProps()): `BLOCKS.registerBlock(name, MyBlock::new, () -> cropProps())` (Supplier form).
  - `BLOCKS.registerSimpleBlock` already handles this correctly and does not need changes.
- Same applies to items: `Item.Properties` requires `.setId(ResourceKey<Item>)` before `Item.<init>`.
  - `ITEMS.register(name, () -> new MyItem(new Item.Properties()...))` **breaks**.
  - Options:
    1. `ITEMS.registerItem(name, MyItem::new)` — factory gets empty Properties with ID pre-set.
    2. `ITEMS.registerItem(name, MyItem::new, new Item.Properties().stacksTo(1))` — factory gets base Properties (with chain) with ID pre-set.
    3. Helper: `ITEMS.register(name, id -> new MyItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).chain()))` — pass `id` directly from the `Function<Identifier, Item>` form.
  - `ITEMS.registerSimpleItem` and `ITEMS.registerSimpleBlockItem` already handle this correctly.
- **Item model format**: In 1.21.11, `ModelManager` looks for item models at `assets/<mod>/items/<name>.json` (new format), not the old `models/item/<name>.json` path by default. The old path may still serve as a fallback for some model types. Expect "No model loaded for default item model ID" WARNs for every item without a new-format JSON — non-fatal, items render as missing-texture model.

**Biome JSON**
- `"carvers": {}` (empty object) → `"carvers": []` (empty array). The carvers field is now a flat list in 1.21.11.
- `"temperature_modifier": "none"` field removed entirely in 1.21.11 biomes.

**Recipe JSON**
- Ingredient format completely changed in 1.21.11:
  - Item: `{"item": "namespace:id"}` → `"namespace:id"` (plain string)
  - Tag: `{"tag": "namespace:tag"}` → `"#namespace:tag"` (# prefix string)
  - In shaped `key` entries and shapeless/custom `ingredients` arrays
  - Smelting `ingredient` field: same change
- Result format: `{"item": "x", "count": N}` → `{"id": "x", "count": N}` (`item` → `id`)
- Custom recipe types (witchery:kettle_brewing, witchery:distillery, etc.) that return `PlacementInfo.NOT_PLACEABLE` will log `"can't be placed due to empty ingredients"` — this is expected and harmless.

**Structure worldgen**
- `BlockEntity.level` is null when accessed inside `StructurePiece.postProcess()` during worldgen. Call `blockEntity.setLevel(worldGenLevel.getLevel())` before using any methods that internally invoke `this.level.sendBlockUpdated(...)` (e.g. `SignBlockEntity.setText()`).
- `StructurePiece.getMirror()` returns null if `setOrientation(Direction)` was never called on the piece — `placeBlock()` will crash calling `BlockState.mirror(null)`. Always call `setOrientation(Direction.NORTH)` (or any valid direction) in custom `StructurePiece` constructors.
- `postProcess` receives a `WorldGenLevel`, NOT a `ServerLevel`. Never cast `level` to `ServerLevel` directly — it will `ClassCastException`. Use `level.getLevel()` to obtain the `ServerLevel` when needed (e.g. for `getCurrentDifficultyAt`, `addFreshEntityWithPassengers`).

**Loot tables**
- `minecraft:looting_enchant` function renamed to `minecraft:enchanted_count_increase` in 1.21.11. New format requires `"enchantment": "minecraft:looting"` field alongside the existing `count` field.

**Misc**
- `Player.sendSystemMessage(Component)` removed. Use `displayClientMessage(Component, false)`.
- `Level.canSeeSky(BlockPos)` still exists (via `BlockAndTintGetter`).
- `Inventory.items` (field) is now private. Iterate via `getContainerSize()` + `getItem(i)`.

### 2026-05-16 (M25b — Nightmare + Spirit entities)

**New entity files:**
- NightmareEntity.java: 100 HP, 0.35 speed, 4 dmg, MeleeAttackGoal; defenseTimer=40 ticks invincibility after each hit (damage capped 15/hit); doHurtTarget strips random armor slot (HEAD/CHEST/LEGS/FEET) from player; drops Mellifluous Hunger only in Spirit World (dropCustomDeathLoot override)
- SpiritEntity.java: extends Monster (needed for PathfinderMob/AvoidEntityGoal); 20 HP, flying (FlyingMoveControl + FlyingPathNavigation), passive — AvoidEntityGoal<Player> 8f radius; drops 1 Subdued Spirit always

**New model files:**
- NightmareModel.java: HierarchicalModel 64x64; head/body/left_arm/right_arm with 3 fingers each; 7 leg-tendrils (leg1-leg7) animated with sine waves keyed on entity ID modulus; attack animation swings arms via triangleWave()
- SpiritModel.java: HierarchicalModel 32x32; orb of 5 sub-boxes (shape1-5) as children of body pivot; gently bobs via sin(ageInTicks*0.05); rendered at 0.5x scale via SpiritRenderer.scale() override

**New renderer files:** NightmareRenderer (shadow 0.6f), SpiritRenderer (shadow 0.25f, scale 0.5x)

**Spawn eggs:** NIGHTMARE_SPAWN_EGG (0x1A0033/0x9900CC), SPIRIT_SPAWN_EGG (0xAADDFF/0xFFFFFF)
**Commands:** /witchery spawn nightmare, /witchery spawn spirit
**Spirit World biome:** Spirit added as creature spawner (weight 10, 1-3)
**API note:** AvoidEntityGoal requires explicit generic type <Player> and PathfinderMob — use extends Monster not Mob for passive flying entities that need goal AI


### 2026-05-16 (M25 — Spirit World entry/exit mechanics)

**New files:**
- `spiritworld/SpiritWorldHelper.java` — core entry/exit logic: saves inventory (keeps Icy Needle + Mutandis + Mutandis Extremis), 30/70 dream/nightmare roll, XZ-matched spawn in Spirit World, restores inventory on exit
- `item/AppleOfSleepingItem.java` — food item, on eat server-side calls SpiritWorldHelper.enter()
- `item/IcyNeedleItem.java` — right-click in Spirit World calls SpiritWorldHelper.exit()

**Modified:**
- `BrewOfSleepingItem` — calls SpiritWorldHelper.enter() instead of sleep effects
- `WitcheryPlayerData` — 3 new codec-persisted fields: inSpiritWorld, spiritWorldIsDream, savedInventorySnbt; all carried in copyFrom()
- `SyncPlayerDataPayload` — inSpiritWorld synced to client
- `WitcheryPlayerEvents` — LivingDeathEvent in Spirit World: cancel death, restore inventory, exit to Overworld at 1 HP
- `WitcheryCommands.findSafeEntry` — Spirit World uses overworld scan (Y 200→−60) not End-style
- `WitcheryItems` — registered APPLE_OF_SLEEPING (AppleOfSleepingItem), ICY_NEEDLE now IcyNeedleItem

**Resources:** apple_of_sleeping model + texture (ingredient.sleeping_apple.png), recipe (apple + belladonna), lang key

**NeoForge API note:** `TagParser.parseTag(String)` (net.minecraft.nbt) is available and returns Tag; cast to ListTag for inventory restore. `player.getInventory().save(ListTag)` + `.load(ListTag)` is the correct inventory serialization pattern.

**Up next (M25b):** NightmareEntity + SpiritEntity — these are the first Spirit World mobs. Nightmare: 100 HP, spawns during nightmare state (spiritWorldIsDream == false), teleports to player, strips armor. Spirit: 20 HP, passive/flees, drops Subdued Spirit. Both need entity class + model + renderer + registration.


### 2026-05-16 (Wiki corrections — M27 entity stats + dimension data)

**Entity HP corrections (FTB Wiki):**
- Demon: 60 → 100 HP (was placeholder; wiki: 100 HP, fires Fire Charges, fire-immune)
- Treefyd: 30 → 50 HP (wiki: 50 HP, blinds on attack, hostile to non-whitelist)
- Ent: 80 → 200 HP (wiki: 200 HP, flings player, spawns when Alder/Hawthorn/Rowan chopped)
- Mandrake: 10 → 20 HP (wiki: 20 HP, scream causes Nausea, spawns via ritual)
- Coven Witch: 20 → 30 HP (wiki: 30 HP, neutral, throws vanilla potions, spawns in Overworld)
- Banshee: already 20 HP ✓ (was 40 in a previous placeholder — corrected last session)

**Ritual-only mobs (do NOT naturally spawn):**
- Banshee: Summon Banshee ritual + Brazier (drops Spectral Dust)
- Spectre: Summon Spectre ritual + Brazier
- Demon: Rite of Summoning (drops Demon Heart, Gunpowder, Magma Cream)
- Mandrake mob: spawned from Mandrake plant pull
- Treefyd: conjured by witch player ability

**Spirit World dimension (confirmed by wiki):**
- Mirror of Overworld topography — using minecraft:overworld noise ✓
- Renamed dream_world → spirit_world; orphaned dream_world JSON files removed
- Entry: Apple of Sleeping, Brew of Sleeping, Spirit Portal (needs Rite of Manifestation)
- Exit: die, or throw Icy Needle
- Inventory stripped on entry (Icy Needle + Mutandis always transfer; Baubles transfer if mod installed)
- Circle Magic does not work in Spirit World
- Sources: Wispy Cotton, Disturbed Cotton, Mellifluous Hunger (from Nightmares)
- Wispy Cotton and Brew of Flowing Spirit can pass back to Overworld; other items cannot
- Dream state: Dream (perpetual day) or Nightmare (perpetual night, more common); Dream Weaver blocks affect chance

**Spirit World mobs (not yet implemented — deferred):**
- Nightmare: 100 HP, spawns during nightmares, teleports to player, respawns, strips armor, drops Mellifluous Hunger
- Spirit: 20 HP, passive/flees, flying, drops 1 Subdued Spirit

**Torment dimension (confirmed by wiki / old source):**
- Horizontally aligned mazes. Old 1.7.10 `GenerateMaze.display` uses invisible `Force` for walls/ceilings and vanilla Bedrock for floors/portal frames; Resurrected has a TormentStone naming/art path, but the baseline port should preserve old mechanics/layout unless intentionally deviating.
- No vanilla mobs (blaze/magma_cube removed from torment biome spawner)
- Remaining spawners: witchery:demon, witchery:mandrake_mob
- Entry: summon Lord of Torment via Torment Scroll (final gift from tamed Flame Imp), fight to 50% HP (250 HP), gets teleported in with player
- Exit: die → respawn in Overworld, or complete maze → Torment Portal
- Legacy maze generator restored in the port; existing generated chunks must be deleted before visual/layout retest.

**Lord of Torment (not yet implemented):**
- 500 HP boss, fires Slowness fireballs, at 50% HP teleports self+player to Torment
- Drops: Soul of Torment Demon, Demon Hearts, Miniature Yellow Hearts, enchanted books
- Eating Soul of Torment Demon grants ability to send others to Torment

**Flame Imp (not yet implemented):**
- 50 HP, 4 damage, hostile; summoned via Rite of Summoning
- Tameable with Demonic Contract (crafted, signed in blood) + 25 XP levels
- Bound imp stays in area, attacks those hostile to witch
- Demon Heart → temporarily doubles size and damage (30 min cooldown; Icy Needle cancels)
- Happy tamed Flame Imp eventually gives Torment Scroll (Lord of Torment summon item)


### 2026-05-15 (M20 — Familiar Entities)
- TamableAnimal is at net.minecraft.world.entity.TamableAnimal (NOT world.entity.passive) in 1.21.11
- Mob.createMobAttributes() replaces Animal.createAnimalAttributes() which was removed in 1.21.11
- Inventory.add(ItemStack) returns boolean in 1.21.11 (not ItemStack remainder)
- AvoidEntityGoal<T> requires explicit type parameter when Java can't infer: new AvoidEntityGoal<Phantom>(...)
- EntityAttributeCreationEvent handler goes in @EventBusSubscriber(bus=MOD) class alongside DeferredRegister
- Entity textures go in textures/entity/ (new folder); model LayerLocations in WitcheryClient.LAYER_DEFS list
- riteOfBindFamiliar: spawns entity at circle center, calls tame(player), stores UUID in WitcheryPlayerData, syncs payload
- Owl warning: scans 20-block radius every 100 ticks via tick() override, sends chat message if monsters found
- Toad collection: checks 6-block radius every 200 ticks, calls Inventory.add() and discards ItemEntity on success

### 2026-05-15 (M19 — Infusion Base System)
- InfusionType enum stored as String in WitcheryPlayerData to avoid codec complexity; fromString() parses at call sites
- SyncPlayerDataPayload updated to carry infusionType — add one writeUtf/readUtf pair to both encode/decode and the from() factory
- Rite of Infusion now dispatches before coal/food rites in dispatchSmall — brew base item determines infusion type
- RiteOfBlessing (Regen II + Speed I) replaces old riteOfInfusion fallback for empty small circle
- InfusionEvents uses PlayerTickEvent.Post every 100 ticks; re-applies effects for 140 ticks so they stay active without requiring always-on duration
- Infernal Animus recipe consumes a Voodoo Poppet as ingredient (unbound poppets work since recipe matching ignores CustomData)
- PacketDistributor.sendToPlayer() syncs infusion immediately after Rite of Infusion
- All 15 missing item-model block textures copied from original decompiled assets (camelCase→snake_case rename); 12 poppet textures + taglock textures also added from original

### 2026-05-15 (M18 — Taglock Kit + remaining poppets)
- TaglockKitItem.getBoundName() avoids collision with Item.getName(ItemStack) — never use "getName" as a static method name in Item subclasses
- ProtectionPoppetItem uses ProtectionType enum (EARTH/WATER/FIRE/HUNGER/VOODOO/POPPET_SHIELD) dispatched in PoppetEvents.damageMatchesProtection()
- Death poppets use LivingDeathEvent (cancellable) on the forge bus; POPPET_SHIELD returns false (protects other poppets, not death itself)
- Vampiric poppet uses LivingDamageEvent.Pre (not LivingDamageEvent) to intercept damage before armor reduction; redirects 50% as magic damage to bound player
- DamageTypes.* for specific types (FALL, DROWN, STARVE, MAGIC); DamageTypeTags.IS_FIRE for fire tag; DamageTypeTags.BYPASSES_ARMOR added to VOODOO coverage
- VoodooPoppetItem right-click applies Poison II + Weakness II to bound player; consumes on use
- All poppets stackTo(1) — each is uniquely bound to one player UUID
- Item models point to specific textures (poppet.protect_earth.png etc.) registered alongside poppet base texture



### 2026-05-15 (M11 — Witches Oven)
- OvenRecipe extends AbstractCookingRecipe; constructor takes (group, category, ingredient, result, exp, cookTime) matching SimpleCookingSerializer.Factory interface. No recipeBookCategory() override needed.
- Burn time: use stack.getBurnTime(recipeType) (NeoForge IItemStackExtension), not CommonHooks.getBurnTime()
- AbstractCookingRecipe stores RecipeType in this.type field; getType() returns it. Pass WitcheryRecipeTypes.OVEN.get() in super() call.
- Screen registered via RegisterMenuScreensEvent on the MOD bus (in WitcheryClient @EventBusSubscriber MOD)
- Menu type registered via WitcheryMenuTypes.MENU_TYPES.register(modEventBus) in WitcheryMod constructor
- openMenu(be, pos) pattern: ServerPlayer.openMenu(MenuProvider, Consumer<FriendlyByteBuf>) or the overload ServerPlayer.openMenu(menuProvider, blockPos)
- Official Witchery site scraped: full rite names, complete item list, dream catcher types, additional brews confirmed — PORTING_PLAN.md updated

### 2026-05-15 (Crop block fix)
- CropBlock.createBlockStateDefinition uses the STATIC field CropBlock.AGE (AGE_7), not this.getAgeProperty(). Overriding only getAgeProperty() is not enough — must also override createBlockStateDefinition to register the correct property (AGE_4). The Java constant name (BELLADONNA_AGE etc.) is irrelevant; the property's actual name is "age" (from BlockStateProperties.AGE_4 = IntegerProperty.create("age", 0, 4)). Blockstate JSON variants must use "age=0" through "age=4".

### 2026-05-15 (Polishing pass)
- WitcheryBlockItemRenderer refactored: if-else chain + 14 fields → IdentityHashMap<Item, Supplier<BlockEntity>> factory map + computeIfAbsent cache. Adding a BESR block item now requires one line here only.
- WitcheryClient.registerClientExtensions now derives item list from WitcheryBlockItemRenderer.registeredItems() — no more duplicate list to maintain.
- WitcheryClient.registerLayerDefinitions collapsed from 14 calls to a List<Map.Entry<...>> + forEach.
- KettleBlockEntity.color made private, getColor() added; KettleRenderer updated.
- ChalkItem.hurtAndBreak guarded against null player (ctx.getPlayer() can be null in automation).
- Removed orphaned lang key "block.witchery.demon_heart_block".

### 2026-05-15 (M10)
- PoppetItem uses DataComponents.CUSTOM_DATA + CustomData.update() for UUID binding (not legacy NBT tag)
- LivingDeathEvent is cancellable on the NeoForge event bus (@EventBusSubscriber, not mod bus)
- Poppet stacks to 1 (each is uniquely bound); death restores to 4f HP (2 hearts) before re-die loop
- Binding: interactLivingEntity() intercepts right-click on a Player entity

### 2026-05-15 (M8+M9)
- M9: AttachmentType registered via DeferredRegister<AttachmentType<?>> + NeoForgeRegistries.Keys.ATTACHMENT_TYPES
- M9: SyncPlayerDataPayload uses StreamCodec<RegistryFriendlyByteBuf, T> (not ByteBuf) for playToClient
- M9: RegisterPayloadHandlersEvent (not RegisterPayloadsEvent) + event.registrar("1").playToClient(...)
- M9: WitcheryPlayerEvents uses @EventBusSubscriber (forge bus, not mod bus) for player lifecycle events
- M8: ChalkItem.hurtAndBreak takes EquipmentSlot not InteractionHand in 1.21.11
- M8: LightningBolt spawned via new LightningBolt(EntityType.LIGHTNING_BOLT, level) + addFreshEntity() + setVisualOnly(true)
- M8: CIRCLE block uses carpet model (16×1×16 thin flat block) for chalk mark appearance
- M8: Ritual trigger: right-click any floor block with arthana; scans ring one Y level above clicked block

### 2026-05-15
- M3+M4 done: BelladonnaBlock/MandrakeBlock/ArtichokeBlock extend CropBlock with AGE_4 property (5 stages)
- BlockStateProperties.AGE_4 used; getAgeProperty() + getMaxAge() + getBaseSeedId() overridden
- Crop items (belladonna, mandrake_root, artichoke) are ItemNameBlockItem — same item is both seed and ingredient
- Loot tables: always drop 1 seed, mature (age=4) drops 0-2 more
- WITCH_CROP and COTTON remain as simpleBlock placeholders (growth logic deferred)
- Fetish + Statue of Goddess converted to full BESR (FetishBlock/StatueGoddessBlock + BlockEntity + Renderer)
- Refilling chest inventory icon fixed via BEWLR reusing LeechChest renderer
- Dream catcher GUI rotation fixed: [0,180,0] → [0,90,0]

### 2026-05-14
- Decompiled all three jars (witchery-1.7.2-0.19.0, companion, WitcheryResurrected 1.12.2)
- Renamed all camelCase texture files to snake_case (required by 1.21.11 resource location rules)
- BESR pattern established: ModelPart divides by /16 internally — do NOT add explicit scale(0.0625)
- Standard BESR transform: translate(0.5,0.5,0.5) → rotateZ(180) → translate(0,-1,0) → rotateY(-90)
- noOcclusion() required on all BESR blocks so block below is visible through legs/frame gaps
- Blocks placed before noOcclusion was set need to be broken and replaced to refresh light
- Zero-dimension boxes (W=0/H=0/D=0) port correctly to CubeListBuilder; negative UV offsets clamp to 0 and give correct texture sampling (e.g. chalice cup sides)
- LeechChest uses different transform: translate(0,1,1) + scale(1,-1,-1) instead of standard rotateZ pattern

### 2026-05-15 (M25/M28 — Dimensions)
- Dimensions are fully data-driven: `data/witchery/dimension_type/`, `data/witchery/dimension/`, `data/witchery/worldgen/biome/`
- Biome `features` array must be 10 sub-arrays `[[], [], ...]`, not bare `[]`
- `server.getLevel(ResourceKey<Level>)` returns null if dimension not loaded yet — safe to call after the first teleport
- Force chunk generation before scanning for a safe Y: `level.getChunk(0, 0, ChunkStatus.FULL, true)`
- Dream World uses End-style noise (`"settings": "minecraft:end"`) — central island always at ~(0, 64, 0)
- Torment now uses a custom legacy maze generator, not Nether-style noise; regenerate existing Torment chunks before retesting maze block/layout changes.
- `WitcheryPlayerData` has `returnPos`/`returnDimension` — set before teleport, cleared on exit; copied via `PlayerEvent.Clone`

### 2026-05-15 (Entity models)
- Always compare against decompiled `ModelXxx.java` before writing a modern model — textures were painted for the original UV
- **GhostModel** (Banshee/Spectre/Poltergeist): original `ModelSpectre` UV: head=texOffs(0,16), body=texOffs(16,0), arms=texOffs(0,0), robeUpper=texOffs(38,9), robeLower=texOffs(32,20). 64×32 texture.
- **CatFamiliarModel**: `blackcat.png` was used by `RenderWitchCat extends RenderOcelot`, so it is an ocelot UV texture. The port renderer now uses the dedicated CatFamiliarModel instead of modern vanilla CatModel/all_black.png, with old ocelot anchors: head(0,0), body(20,0) at the horizontal ModelOcelot pose, back legs(8,13), front legs(40,0), and two root tail segments at tail(0,15)/(4,15).
- **UV overflow**: `texOffs(0,30)` with a 4×9×4 box in 64×32 texture needs y=43 — beyond texture height. Move to free UV area.
- **EntModel**: actual `ent.png` is 256×256 — must declare `LayerDefinition.create(mesh, 256, 256)` to avoid UV mismatch.
- `EntityRenderDispatcher.getRenderer(Entity)` returns raw type — cast via `Object` then `instanceof PlayerRenderer`

### 2026-05-15 (Poppet shelf + coven witch)
- Poppet shelf switched from custom 4-slot menu/screen to vanilla `ChestMenu(MenuType.GENERIC_9x1, id, playerInv, this, 1)` — original had 9 slots and used `player.displayGUIChest()`
- `ChestMenu.oneRow(id, playerInv)` in 1.21.11 takes only 2 args (no container) — use the full constructor instead
- Coven Witch: `AbstractVillager`; override `showProgressBar()→false`, `getVillagerXp()→0`, `canRestock()→true` for no-prerequisites trading
- Coven Witch random names: generate from first/surname arrays in `finalizeSpawn()`; set via `setCustomName(Component.literal(...))`
- `Component.translatable(key).getString()` on server returns raw key — send `Component.translatable(key)` directly and let client translate

### 2026-05-15 (WerewolfLayer)
- `getParentModel().setAllVisible(false)` hides player body; called each frame with one-frame delay before effect takes hold
- Werewolf buffs now always apply when `isWerewolf()=true` — removed full-moon gate to match the always-on visual
- `WitcheryClientEvents` (game bus, `Dist.CLIENT`) resets `setAllVisible(true)` on respawn/dimension change for non-werewolf players

### 2026-05-23 — PlayerSkin API and humanoid NPC renderer (NeoForge 21.11.42)

**PlayerSkin package moved**
- In 21.11.42 `PlayerSkin` is at `net.minecraft.world.entity.player.PlayerSkin`, NOT `net.minecraft.client.resources.PlayerSkin` (the old 21.1.x location). Imports from the client package will fail to resolve.

**PlayerSkin texture accessor changed**
- Old (21.1.x): `PlayerSkin.texture()` → `ResourceLocation`
- New (21.11.42): `PlayerSkin.body()` → `ClientAsset$Texture`; then `.texturePath()` → `Identifier`
- Do NOT call `.texture()` — it does not exist in the compiled 21.11.42 jar.

**SkinManager lookup changed**
- Old: `SkinManager.lookupInsecure(GameProfile)` — does not exist in 21.11.42
- New: `SkinManager.createLookup(GameProfile, boolean)` → `Supplier<PlayerSkin>`; call `.get()` then `.body().texturePath()`
- The boolean controls whether to allow insecure (non-HTTPS) lookups; pass `false` for the cached/default behavior.

**Full skin resolution pattern for an NPC that mirrors a player's skin:**
```java
private static Identifier resolveSkin(ReflectionEntity entity) {
    UUID ownerUUID = entity.getOwnerUUID();
    if (ownerUUID == null) return DefaultPlayerSkin.getDefaultTexture();
    Minecraft mc = Minecraft.getInstance();
    if (mc.level != null) {
        Player player = mc.level.getPlayerByUUID(ownerUUID);
        if (player instanceof AbstractClientPlayer acp) {
            return acp.getSkin().body().texturePath();
        }
    }
    String name = entity.getOwnerName();
    GameProfile profile = new GameProfile(ownerUUID, name.isEmpty() ? null : name);
    return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
}
```

**HumanoidMobRenderer for humanoid NPCs**
- Use `HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>>` as the base class for any NPC that should render with standard humanoid animation and accept a player skin texture.
- Use `MobRenderer` + `LivingEntityRenderState` only for non-humanoid mobs; it will not correctly animate arms/legs for player-skin UV layouts.

**ModelLayers.PLAYER for player-skin-compatible models**
- `ctx.bakeLayer(ModelLayers.PLAYER)` produces a `ModelPart` built to the exact 64×64 player skin UV layout.
- Using any other model (e.g. `WitchHunterModel`, `ZombieModel`) with a player skin texture will produce UV mismatches and visible head/body artifacts even if the mesh geometry looks roughly correct.

**Verifying compiled API with javap**
- When unsure which API variant is live in the current NeoForge jar, run:
  `javap -cp ./build/moddev/artifacts/neoforge-21.11.42-merged.jar net.minecraft.client.resources.SkinManager`
- Use `neoforge-21.11.42-merged.jar`, not `neoforge-21.1.229.jar` — method signatures differ between these two jars.

**Block model JSON: `cauldron_inner` wraps wrong on flat faces**
- `minecraft:block/cauldron_inner` in 1.21 is a bowl-shaped circular texture (dark centre, lighter rim). Assigning it as a `#shadow` or inner-surface texture on flat rectangular faces produces obvious visual distortion — the circular gradient maps onto a rectangle and looks wrong.
- **Fix:** Use `iron_block` (or the same seamless tile as the exterior) for any flat inner/shadow face. Ambient occlusion will naturally darken recessed interior faces without a special texture.

**Block model JSON: spout/protrusion UV dimensions must match face physical dimensions**
- If a box element is 3 units wide in X but its north/south UV spans 4 units (or Z=3 but UV height is 4), the texture appears squished even on seamless tiles — grid lines become rectangular instead of square.
- **Rule:** UV size = physical face size. For each face:
  - north / south: UV width = `to.x − from.x`, UV height = `to.y − from.y`
  - east / west: UV width = `to.z − from.z`, UV height = `to.y − from.y`
  - up / down: UV width = `to.x − from.x`, UV height = `to.z − from.z`
- **Silver vat example (2026-05-25):** all four upper spouts had north/south/up/down UVs that were 1 px too wide because the spout caps are X=3 or Z=3 but the UVs used 4-unit ranges copied from the X=4/Z=4 side faces. Fixed by trimming `u2` or `v2` down by 1.

**Stale dimension chunks are not regenerated**
- If a custom chunk generator changes its structure (e.g. Mirror World room layout), previously generated and saved chunks will NOT be regenerated on the next launch. Old saved chunk data is loaded from disk as-is.
- To force regeneration: delete `run/saves/<world>/dimensions/<modid>/<dimension>/` before launching, then trigger any block/BE state that was tied to the old chunk (e.g. break and replace the mirror block to clear its room assignment).
- This applies to all custom dimensions (Mirror World, Torment, Spirit World). Always delete the dimension folder and reset relevant block entities when testing chunk-structure changes.

### 2026-05-25 — Mirror World, block rendering, coffin, placement direction

**Custom dimension height must match chunk generator WALL_Y pattern length**
- `dimension_type` JSON `"height"` must equal `ChunkGenerator.getGenDepth()` and the length of your Y-axis wall pattern. Mirror World uses a 16-element `WALL_Y` array → height=16, logical_height=16, min_y=0.
- If `height` is left at the overworld default (384), the chunk generator loops 24 times through the 16-element pattern, stacking 24 rooms vertically. Each room in the stack is rendered as geometry → GPU OOM crash within seconds of entering the dimension.
- Also set `"has_ceiling": true` so the sky renderer doesn't run, and `"has_skylight": false` to prevent sky-light propagation in a fully enclosed space.

**`chunk.getMaxY()` is INCLUSIVE — use `chunk.getMinY() + chunk.getHeight()` for exclusive loop bounds**
- `ChunkAccess.getMaxY()` returns the highest valid Y coordinate (inclusive), NOT an exclusive upper bound.
- For 16-tall Mirror World (minY=0, height=16): `getMaxY()` = 15. A loop `for (y = 0; y < 15; y++)` runs y=0..14, skipping the ceiling at y=15.
- Always compute the exclusive upper bound as `chunk.getMinY() + chunk.getHeight()` in custom ChunkGenerators:
  ```java
  int minY = chunk.getMinY();
  int maxY = chunk.getMinY() + chunk.getHeight(); // exclusive
  for (int y = minY; y < maxY; y++) { ... }
  ```

**Block `render_type` and `.noOcclusion()` must both be managed for solid/transparent blocks**
- `"render_type": "minecraft:translucent"` (or `"minecraft:cutout"`) in the block model JSON tells the render engine to use the translucency pipeline — neighbors are NOT culled. Remove it to restore default solid (opaque) rendering.
- `.noOcclusion()` in `BlockBehaviour.Properties` tells the occlusion engine that the block does not fill its full voxel cube → neighbor faces are not culled regardless of translucency. Remove it when a block should be opaque and cull neighbors.
- Both must be absent for a fully solid, neighbor-culling block. Either one alone can cause light/occlusion artifacts.

**Coffin BESR: always render two passes — exterior texture + inside texture**
- The original Witchery coffin has two complementary texture sheets per half: `coffin_head.png` / `coffin_head_inside.png` and `coffin_bottom.png` / `coffin_bottom_inside.png`. Each sheet is opaque where the other is transparent (cutout). The exterior sheet draws the lid, sides, and base; the inside sheet fills the transparent interior-face regions the exterior leaves empty.
- A single-pass render using only the exterior texture leaves interior faces invisible — visible as transparency when the lid is open.
- Fix: call `collector.submitModelPart(...)` twice per half, once for each texture, both using `RenderTypes.entityCutout(...)`. The textures are 128×128 RGBA PNG.
- The model `LayerDefinition` declared texture size must match the actual PNG dimensions — a mismatch halves all V coordinates and causes the texture to read the wrong rows.

**`item/generated` display transform: center low-sitting textures with a `gui` translation**
- If an item's texture artwork sits in the lower portion of its 16×16 PNG frame (blank space at top), the item icon will appear lower than center in the inventory slot.
- Fix: add a `"display"` → `"gui"` override in `models/item/<name>.json` with a positive Y translation. Units are model-space pixels (1 unit ≈ 1 px in the slot).
  ```json
  "display": {
    "gui": {
      "rotation": [0, 0, 0],
      "translation": [0, 4, 0],
      "scale": [1, 1, 1]
    }
  }
  ```
- Tune the Y value: `4` centers an icon occupying roughly the bottom half of the frame. Increase for icons sitting even lower, decrease if the result overshoots.
- No recompile needed — resource-only change; use F3+T in-game to reload and check.
- **Redstone Soup example (2026-05-25):** artwork sat in the bottom ~8 px of a 16×16 PNG → `translation: [0, 4, 0]` centers it.

**`HorizontalDirectionalBlock`: FACING should be the direction the decorative face looks toward, NOT the wall direction**
- `BlockPlaceContext.getHorizontalDirection()` returns the direction the player is looking (toward the block being placed).
- When FACING means "the direction the glass/face looks toward" (i.e. toward the player), use `ctx.getHorizontalDirection()` directly — **do NOT call `.getOpposite()`**.
- `.getOpposite()` flips FACING to point into the wall, which reverses the collision shape, the entry zone detection, and the blockstate model y-rotation (the glass face ends up behind the block).
- Rule of thumb: if your block model's "front" face (e.g. south face = Z=14-16 slab) should face the player who placed it, store `ctx.getHorizontalDirection()` as-is.

**Mirror World debug command entry was landing on top of ceiling, not at floor height (2026-05-25)**
- `/witchery mirror` calls `findSafeEntry` which scanned from y=200 downward. In a 16-block-tall dimension (y=0 to y=15), y=16+ positions are out-of-range and return AIR. The scan found the CEILING block at y=15 as the first solid with two "air" blocks above it (y=16 and y=17 are out of range = air). So the player was placed at y=16, on top of all the mirror blocks.
- The scan also used the raw overworld X/Z, which in the mirror world can land on a wall block (local x/z 0-3 or 13-15 are solid mirror; only local 4-12 are open).
- Fix: for Mirror World, skip the scan entirely. Snap to the chunk's guaranteed-open interior centre (`cx*16+8, cz*16+8`) and set Y to `MirrorHelper.ROOM_FLOOR_Y` (the first open block above the floor).
- Rule: dimension-specific entry logic should use the known geometry constants from the chunk generator, not a generic scan that assumes standard Minecraft height ranges.

**Anointing Paste: use `useOn()` in the item class, not a `RightClickBlock` event handler (2026-05-25)**
- The first pass used a `@EventBusSubscriber` handler on `PlayerInteractEvent.RightClickBlock`. It worked server-side but had no client-side particle feedback, and forced callers to know about the event class for what is purely an item interaction.
- Pattern: move the logic to `AnointingPasteItem.useOn(UseOnContext)`. On client side: spawn particles + play sound; on server side: set block + shrink item + award stat. Return `InteractionResult.SUCCESS` client / `CONSUME` server (i.e. `sidedSuccess`-style split).
- For witch particles: `ParticleTypes.WITCH` with a small upward velocity (`0.0, 0.06, 0.0`) and 24 random positions in a 2×1.5×2 box centered on the cauldron. Only add particles in the `level.isClientSide()` branch.
- Preserving vanilla water-cauldron level: check `state.is(Blocks.WATER_CAULDRON)` → `state.getValue(LayeredCauldronBlock.LEVEL)`; empty cauldron `Blocks.CAULDRON` → 0; lava/powder-snow cauldrons → PASS.
- Rule: item interactions that only involve the held item and a specific block belong in `useOn()`, not in an event handler.

**`CoffinModel` spurious `inner_floor` parts caused bottom texture wrapping bug (2026-05-25)**
- The ported `CoffinModel.java` had extra `inner_floor` children added to both `createHeadLayer()` and `createBottomLayer()` that are NOT present in the original `ModelCoffin.java` from WitcheryResurrected.
- Both used `texOffs(4, 1)` — the same UV region as the head lid — so they sampled and rendered lid texture pixels on the floor of the coffin interior, causing visible wrong texture wrapping on the bottom exterior.
- Fix: removed both `base.addOrReplaceChild("inner_floor", ...)` lines. The constructor only accesses `lid`, so no other changes were needed.
- Rule: when porting a `LayerDefinition`, compare part-by-part against the original `ModelPart` source. Any extra `addOrReplaceChild` not in the original is a port artifact that will corrupt UV sampling.

**`MirrorBlock` VoxelShape naming convention: slab must be on the WALL side (2026-05-25)**
- A `HorizontalDirectionalBlock` named NORTH means the block is placed against the north wall. The thin slab must occupy the WALL side (low-Z, z=0–4 for NORTH) not the player-facing side (z=12–16).
- Original port had all four shapes inverted: NORTH was at z=12–16 (player/south side), SOUTH at z=0–4 (north side), WEST at x=12–16 (east side), EAST at x=0–4 (west side).
- Correct shapes:
  ```java
  NORTH = Block.box(0,  0, 0, 16, 16,  4);  // z=0-4   = north/low-Z wall
  SOUTH = Block.box(0,  0, 12, 16, 16, 16); // z=12-16 = south/high-Z wall
  WEST  = Block.box(0,  0, 0,  4, 16, 16);  // x=0-4   = west/low-X wall
  EAST  = Block.box(12, 0, 0, 16, 16, 16);  // x=12-16 = east/high-X wall
  ```
- The perception issue of "glass facing wall" for N/S was likely caused by the inverted VoxelShape: with the hitbox on the player side, the player was stopped far from the glass and the glass appeared to be against the wall, even though the blockstate rotation (NORTH: y=180, SOUTH: y=0) was theoretically correct.
- Blockstate y-rotations (CW when viewed from above): NORTH→y=180, SOUTH→y=0, WEST→y=270, EAST→y=90. These put the model's decorative "north" face toward the player for all four directions.

- `AbstractGolem` does **not** exist in NeoForge 21.11.42 — use `net.minecraft.world.entity.animal.golem.IronGolem` and `net.minecraft.world.entity.animal.golem.SnowGolem` explicitly for the 1.7.10 `EntityGolem` equivalent.

**`GlintWeedBlock` particle type: use `END_ROD`, not `FLAME` (2026-05-29)**
- The original Glint Weed emits soft magical light (level 14–15). Using `ParticleTypes.FLAME` in `animateTick` made it look like a burning fire plant — identical in appearance to Ember Moss, which is the plant that *should* look fiery.
- Fix: `ParticleTypes.END_ROD` with a slow upward velocity `(0, 0.01, 0)` gives the correct drifting magical-glow look.
- Rule: do not share particle types between plants with different themes. FLAME = fire/heat; END_ROD/GLOW = cold magical light.
- Known gap: original Glint Weed can spread on walls and ceilings. The port's `BushBlock` base class `canSurvive()` requires a solid floor face, so spread is currently ground-only. Fixing wall/ceiling placement requires overriding `canSurvive` to accept any adjacent solid face and providing appropriate model/placement code.

**`setNoGravity(true)` breaks ground pathfinding for Monster-based entities (2026-05-29)**
- `LeonardEntity` was given `setNoGravity(true)` to make it feel like a floating shade. This silently broke all movement: `WaterAvoidingRandomStrollGoal` and `MeleeAttackGoal` both stalled because the vanilla ground path planner cannot find floor waypoints for a mid-air entity. The entity would retaliate (HurtByTargetGoal fired correctly) but never move toward the target.
- Fix: remove `setNoGravity(true)`. For the visual hover effect, use a renderer-level `scale()` override that applies `PoseStack.translate(0, Mth.sin(ageInTicks * 0.05F) * 0.06F, 0)`. This moves the whole rendered model without touching physics.
- Rule: entities that extend `Monster` use ground navigation by default. Only add `setNoGravity` if you also override `createNavigation()` to return a `FlyingPathNavigation` (or equivalent) and swap `MoveControl` for `FlyingMoveControl`.

**`BloodCrucibleModel` front wall Z offset and blood UV/tint bugs (2026-05-29)**
- **Front wall 1 pixel outside AABB**: `PartPose.offset(-4f, 19f, -5f)` placed the front wall at world Z=0.1875–0.25, but the block AABB starts at z=4/16=0.25. Correct value is `(-4f, 19f, -4f)` → world Z=0.25–0.3125 (outer face flush with AABB, inner face at z=5/16). The general formula for a BESR with `translate(0.5,0.5,0.5) → rotateZ(180°) → translate(0,-1,0)` is: `world_z = 0.5 + model_z/16`. Verify each wall offset satisfies the AABB face coordinates before committing.
- **Blood UV region**: `texOffs(0,44)` on the blood flat quad samples rows 44–49 of the 32×64 block texture. If those rows are dark/transitional material instead of the intended blood colour, blood renders near-black. Always ensure the target UV region in the texture matches the intended colour — check with a visualiser or generate the texture programmatically.
- **Blood tint multiplier**: `ARGB.colorFromFloat(1.0F, R, G, B)` multiplies the texture colour channel-by-channel. Using a dark red (R=0.55, G=0.05, B=0.05) on an already-crimson texture produces near-black (110, 1, 1). Use white `(1.0F, 1.0F, 1.0F, 1.0F)` or near-white for translucency when the texture already has the right colour baked in.

**`StructurePiece.postProcess()`: never call `level.getLevel().getCurrentDifficultyAt()` (2026-05-30)**
- `ServerLevel.getCurrentDifficultyAt(pos)` calls `getChunk()` internally. When invoked from inside a chunk generation task (which `postProcess` always runs on), this blocks waiting for the chunk — deadlocking the server and hanging the game at "Preparing for world creation".
- Fix: construct `DifficultyInstance` directly: `new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F)`.
- Affected pattern: any `entity.finalizeSpawn(level, level.getLevel().getCurrentDifficultyAt(spawnPos), ...)` inside `postProcess`. Replace with `entity.finalizeSpawn(level, new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F), ...)`.
- Rule: inside `postProcess`, only use `WorldGenLevel` methods (not `level.getLevel()`) for anything that might touch chunk data.

**`spawn_overrides` is REQUIRED in structure JSON in NeoForge 21.11.42 (2026-05-30)**
- `StructureSettings` codec in this build treats `spawn_overrides` as a required field, not optional. Omitting it throws `IllegalStateException: No key spawn_overrides` and leaves the structure "unbound" in the registry, hanging world creation.
- Fix: always include `"spawn_overrides": {}` (or with actual spawns) in every `worldgen/structure/*.json`.

**`ResourceKey.location()` renamed to `ResourceKey.identifier()` in 1.21.11 (2026-05-30)**
- `ResourceKey<T>.location()` no longer exists. Use `.identifier()` instead. Returns the same `ResourceLocation`/`Identifier` value.
- Dynamic registries (like `Registries.STRUCTURE`) don't have `registryOrThrow()` on `RegistryAccess`. Use `lookupOrThrow(key)` → `HolderLookup.RegistryLookup<T>`. To get a `ResourceKey` from a value object, iterate `lookup.listElements()` and match by identity (`h.value() == object`), then call `h.key()`.

**Wolf voice sounds are no longer static `SoundEvents` fields in 1.21.11 (2026-06-03)**
- There is no `SoundEvents.WOLF_AMBIENT` / `WOLF_GROWL` / `WOLF_HURT` / `WOLF_DEATH`. Wolf vocalisations are now data-driven per variant via `WolfSoundVariants` (the `SoundEvents.WOLF_SOUNDS` map). Only `WOLF_STEP`, `WOLF_SHAKE`, and `WOLF_ARMOR_*` survive as plain static `SoundEvent`s.
- For a wolf-like modded mob, either pull from `WOLF_SOUNDS` or substitute another beast's static sounds. `HellhoundEntity` uses `HOGLIN_ANGRY/HURT/DEATH` (deep Nether-beast voice) for ambient/hurt/death and keeps `WOLF_STEP` for footfalls. All `HOGLIN_*`, `RAVAGER_*`, `FOX_*` ambient/hurt/death are plain `SoundEvent`s (no `.value()` needed); confirm a field's type with `javap net.minecraft.sounds.SoundEvents` since some (e.g. note-block) are `Holder<SoundEvent>`.

**Symbol-spell `StrokeSet` is level-first; source has a no-level overload (2026-06-03)**
- When porting a Mystic Branch symbol spell from `infusion/infusions/symbols/EffectRegistry`, note the source `StrokeSet` has TWO constructors: `(int level, byte... strokes)` and `(byte... strokes)`. Spells like Accio pass an explicit level (`new StrokeSet(1, 3,0,2,2,1)` = level 1, strokes `[3,0,2,2,1]`), but some (e.g. Morsmordre) use the no-level form (`new StrokeSet(0, 0,3,2,2)` = full strokes `[0,0,3,2,2]`).
- The port's `StrokeSet` is ALWAYS `(int level, int... strokes)`. Copying a no-level source set verbatim eats the first stroke as the level (→ wrong glyph + bogus level 0). Always prepend an explicit level (`1` for a basic spell). Symptom: the glyph draws as a different, shorter spell.
- Client recognition (`SymbolDrawState`) freezes on the FIRST exact stroke-sequence match, so if spell A's full strokes are a prefix of spell B's, B can't be drawn. Keep the roster prefix-free (a quick script over `EffectRegistry` can verify no sequence is a prefix/duplicate of another). Port stroke bytes: `0=up, 1=down, 2=yaw→stroke when turning right, 3=yaw→left`.
- **Detection needs a live HUD to be usable (2026-06-03).** Both the decompiled original (`ItemMysticBranch.onUsingTick`) and Witchery Resurrected use the same anchor-relative model: each tick, if the look's offset from a moving anchor passes ~7-8°, record a stroke and re-anchor. This over/under-counts blind; Resurrected ships a shader (`mystic_branch_lines.fsh`, `MysticBranchRenderer.renderBranchLines`) that draws the committed strokes + the pending pen so the count is visible — THAT is what makes it reliable, not threshold tuning. Don't try timing-based cooldown/settle debouncing (it caused over-count, then no-cast). The port now mirrors this: `SymbolDrawState` (dominant-axis anchor-reset) + `SymbolHud` drawn via `RenderGuiEvent.Post` (`event.getGuiGraphics()` + `event.getPartialTick().getGameTimeDeltaPartialTick(false)`; `GuiGraphics.fill` for the axis-aligned path).
- In-game QA on 2026-06-03 reports no-cross/no-overlap patterns are stable, while symbols that retrace/cross previous motion still feel unreliable. Source does not treat crossing as special; it is still just the stroke byte stream above. Future fixes should compare the affected spell's source `StrokeSet`s first, then improve recognizer/HUD usability if the transcription is already correct.

**`NearestAttackableTargetGoal` predicate + item hatch events (MindrakeEntity, 2026-06-03)**
- The 6-arg `NearestAttackableTargetGoal<>(mob, Class, randomInterval, mustSee, mustReach, predicate)` predicate is a `TargetingConditions.Selector` = `BiPredicate<LivingEntity, ServerLevel>` in 1.21.11 — write `(living, level) -> ...`, not a single-arg `Predicate`. Used here so an untamed Mindrake hunts players but a tamed one doesn't.
- Item "hatch on the ground" pattern (Mindrake Bulb): `ItemTossEvent` / `ItemExpireEvent` both live in `net.neoforged.neoforge.event.entity.item`; `ItemEvent.getEntity()` returns the `ItemEntity`. `ItemTossEvent.getPlayer()` is the thrower — call `itemEntity.setThrower(player)` so `itemEntity.getOwner()` resolves the thrower at expire time. Shorten the fuse with `itemEntity.lifespan = ticks`. `SoundEvents.GENERIC_EXPLODE` is a `Holder<SoundEvent>` → use `.value()`.
- A tameable mob created in code: `TamableAnimal` + `setTame(false,false)` in ctor, goals `SitWhenOrderedToGoal`/`FollowOwnerGoal`/`OwnerHurtByTargetGoal`/`OwnerHurtTargetGoal`, and `mob.tame(player)` to bind an owner. Suppress breeding with `getBreedOffspring → null` + `isFood → false`. `Level.ExplosionInteraction.MOB` respects the mobGriefing rule; `discard()` removes without death drops (the source's setDead-on-attack), while overriding `die()` runs the killed-path explosion + loot.

**Quadruped entity render pattern (HellhoundEntity, 2026-06-03)**
- Reusing vanilla `WolfModel` requires a `WolfRenderState`, which is fiddly to populate. The established port pattern is simpler: hand-port the geometry into a `EntityModel<LivingEntityRenderState>` (see `HellhoundModel`/`WolfHeadModel`/`WolfmanModel`), register the `ModelLayerLocation` in `WitcheryClient.LAYER_DEFS`, and use a `MobRenderer<E, LivingEntityRenderState, M>` (no custom render-state subclass needed when head-look + walk anim suffice — read `state.yRot/xRot/walkAnimationPos/walkAnimationSpeed/ageInTicks`). Diagonal quadruped gait: `cos(swing*0.6662)*1.4*amount` on one diagonal pair, `+PI` phase on the other. Make the mob fire-immune with `EntityType.Builder.fireImmune()`, not an override.

**Cursed-block family / triggered dispersal — track curse as data, don't replace the block (2026-06-13)**
- Source (`brewing/BlockButtonBase`/`…`/`TileEntityCursedBlock` + `DispersalTriggered`) **replaces** the vanilla button/lever/plate/door with a cursed twin block. That works in 1.7.10 because there was one wood type, but in 1.21 a single cursed block forces every wood to one look and one revert target (a birch door comes back as oak; door window panes render solid unless the new block is registered cutout, etc.). The first port attempt did this (subclass each vanilla block as `EntityBlock` + a shared BE) and hit exactly those problems.
- Better 1.21 approach: **don't touch the block.** Store the curse as data keyed by `BlockPos` in a per-level `SavedData` (`CursedBlockData`: brew + charges + thrower UUID, persisted via Codec like `RitualSavedData`). The block stays the exact original (right wood/state/appearance, any vanilla or modded type), "revert" is just removing the record, and there is zero new block/asset/registry. The cursed state is intentionally invisible (a trap) — which is automatic when you don't replace anything.
- Activation interception (`CursedBlockEvents`, `@EventBusSubscriber`): buttons/levers/doors fire on `PlayerInteractEvent.RightClickBlock` (MAIN_HAND; skip when sneaking-with-an-item = placement; normalise doors to the lower half; don't cancel, so the block still activates). Pressure plates have **no** interaction event → poll on `LevelTickEvent.Post`: for each cursed pos in a loaded chunk, read `BlockStateProperties.POWERED` and fire on the false→true rising edge (transient per-`ResourceKey<Level>` powered-set), applying to a `LivingEntity` in the plate's touch AABB; prune records whose block is no longer curseable in the same loop.
- Impact mapping (`CursedBlocks.tryCurse`, from `DispersalTriggered.onImpactSplashPotion`): a thrown brew clips on COLLIDER shapes, and buttons/levers/plates have **no collision**, so the bottle passes through them and hits the block they're mounted on — check both `hit.getBlockPos()` and `hitPos.relative(hit.getDirection())`. Doors are solid, so a direct hit works.

**Placed Modonomicon pages - page-space first, not hitbox-space (2026-06-13)**
- Treat the placed book as a live projection of the generated Modonomicon data, not as a separate book. The block page resolver now reads the same entry/page order, gating checks, recipe pages, text, and item render state as the item Primer path, so future book edits flow into the placed book.
- Page clicks should be ray-mapped to the rendered page planes and accepted only when the mapped point lands on a known button/text/entry region. The old broad block targeting felt good for simple page turns but failed once the page mesh tilted and the interaction plane drifted behind the book.
- Hide page text/items during the opening shuffle and during page-flip animation. Render the final page only after the flip settles; otherwise the eye catches readable text sliding across the paper.
- Block-book item art should use the same GUI item render-state as the normal Modonomicon page. Sprite items stay sprite-flat; block/station items get the normal GUI isometric look, then are projected close to the paper plane. Do not flatten every item by particle texture or give all item sprites fake depth.
- Debug axis note for the placed tome model: on the right-side brown cover/paper group, the final "into the book" correction was local `z + 1` after resetting the earlier exploratory moves. During the axis probe, local X moved the target left on screen, local Y moved it upward on screen, and local Z moved it deeper into the book.

**Crystal Ball and Waystone scrying - camera entity, not possession (2026-06-13)**
- Source-style scrying should move the client camera to coordinates while keeping the player entity/server state stable. The port now sends a small client camera payload and drives the view either from a live target marker or from a hidden no-render `ScryingEyeEntity` for location/self scrying.
- For living taglock/Blooded Waystone targets, third-person camera offset is safer than head possession: stay behind/above the target, terrain-clamp the camera so it does not clip below ground, hide the caster's held item while scrying, and restore the normal camera on release, full duration, invalid target, or timeout.
- Waystone scrying accepts only complete same-dimension location bindings inside the old range. Blooded Waystones and filled Taglock Kits use the old 500-power Crystal Ball route and the 1200-tick held lifecycle.
- Apothecary villagers must keep their Witchery identity separate from vanilla profession churn. Bind their jobsite to the building cauldron using the vanilla leatherworker POI only as the job anchor; on join/tick/right-click, repair name, trades, villager data, and remembered cauldron site if vanilla has stripped the custom appearance/trades.

**Dropped Waystone transposition and Player Compass frames (2026-06-13)**
- Source `EntityItemWaystone` has two passive dropped-item branches. Blank Waystones on a small Otherwhere circle bind themselves and clear the chalk; already-bound/Blooded Waystones on a small Otherwhere circle consume one stone, collect nearby living entities plus loose item entities in the old four-block radius, respect Ender Inhibition, and transpose those targets to the stored coordinate or live Blooded Waystone target. The bound branch does not clear the circle in source.
- Player Compass uses the old `ItemEntityLocator` 32-frame formula: `frame = floor(((angle / TAU) + 1) * 32) % 32`, where `angle = -((visualYaw - 90) - atan2(dz, dx))`. The port exposes that math on `PlayerCompassItem`, registers a range-dispatch property, and uses the original `playercompass1..32` frame sprites for bound compasses while keeping the base sprite for unbound.

**Brew fluids - vanilla-shaped flow with Witchery payloads (2026-06-13)**
- The current practical model is still a Witchery brew payload in a custom block/entity, but the liquid behavior should mimic vanilla flow: source stays a source, flowing cells carry distance, slope models show direction/height, optimal-flow search prefers nearby drops, and non-empty vanilla fluids win contact instead of flickering with brew liquid.
- The first render tick must have enough color data to avoid a white/purple flash: bottle, bucket, cauldron, particle, and placed-fluid tint all derive from `BrewContents.color()`.
- Custom splash bottle impact now plays glass break. Avoid a constant water-droplet trail for non-splash visual noise; impact particles should carry the brew color.

**Player infusions confer NO passive buffs in source — the port's are a deliberate divergence (2026-06-14)**
- Swept the decompiled `infusion/` tree: the four `InfusionLight/Overworld/Otherwhere/Infernal` classes do **not** override `onUpdate`, base `Infusion.onUpdate` is empty, and the only per-tick infusion hook calls `PlayerEffects.onUpdate` (which is *Imp-familiar* effects, not infusion buffs). Just 4 `getInfusionID(` usages exist in the whole codebase, none applying potion effects. So an infusion in 1.7.10 is purely an **energy pool + active powers** (dispatched through `ItemWitchHand` → `Infusion.onLeftClickEntity/onUsingItemTick/onPlayerStoppedUsing`) + creature powers; it grants no sustained buffs.
- The port's `InfusionEvents` buffs (Glowing/Speed/Fire-Res/etc.) were therefore invented. By user decision they are **kept but pared to one small thematic identity effect each**, documented as a divergence: Light = Night Vision, Overworld = Regeneration only while standing on natural ground (`BlockTags.DIRT`/`LEAVES`/moss/farmland), Otherwhere = Slow Falling, Infernal = Fire Resistance — plus a per-infusion grant flash (`onInfusionGranted`, called from both `riteOfInfusion` and `/witchery infuse`) and an ambient aura particle on a 12-tick cadence (`ServerLevel.sendParticles`, no `serverLevel()` accessor in this mapping — cast `(ServerLevel) player.level()`).
- Rule: when "porting passives" for a system, first confirm the source actually has them. Infusion completion is otherwise about the energy bar HUD (`RenderInfusionEnergyBar`), the Witch-Hand active powers, creature-power hooks, Infernal `PotionEnslaved`, and the Otherwhere `InfusedSpirit*Effect` family — the phased roadmap, not buffs.

**Lycanthropy questline — Phase 1: data model + Wolf Altar gates (2026-06-15)**
- Replaced the port-invented flat `wolfMastery` 0-3 with a nested `WitcheryPlayerData.WerewolfState` mirroring `VampireState`: `level` (0-10), `form` (`WolfForm` NONE/WOLF/WOLFMAN ordinal), `questState` (`WolfQuestState`), `questCounter`, persisted `howledChunks` (`List<Long>`, same codec shape as the vampire `visited_villages`), `lastHowl`/`lastBoneFind`. **Codec note:** the main CODEC was already at the 16-field DFU limit, so the old `wolf_mastery` field *slot* was swapped for `werewolf_state` (not added) — old saves silently reset werewolf progress to 0 (documented divergence; mastery was a placeholder).
- `setWerewolf(true)` seeds `level=1` (source infection sets `werewolfLevel=1`); `isWerewolf()` stays the boolean but is kept in lockstep by `setWerewolfLevel` (`werewolf = level>0`). `getMaxBloodPower` hybrid check now reads `wws.level>=2`. Call sites migrated: `CreativeBatWolfTokenItem` (cycles 0-10), `/witchery wolfmastery` (0-10; private method still named setWolfMastery), Moon Charm tick, debug/status readouts.
- `WolfAltarBlock` rewritten from a flat boon into the source `BlockStatueWerewolf` questline state machine. Offering gates use vanilla `Items.GOLD_INGOT` (3 — source uses ingots, **not** the port's earlier nuggets), `Items.MUTTON` (30), `WitcheryItems.TONGUE_OF_DOG` (10); L4 hands out `HORN_OF_THE_HUNT` and sets quest STARTED; L5→9 are counter-quests reading `getWolfQuestCounter()` against `COUNTER_TARGET` {10,16,6,30,1}; an established werewolf can always re-craft a Moon Charm with 3 gold. Both `useItemOn` and `useWithoutItem` route to one `serve(...)`.
- Wolf-form buffs (`WerewolfEvents.applyWolfForm`) now scale Speed/Strength/Jump amplifiers by level — a **simplified** port of source `Shapeshift.getStatBoost` (the exact per-level/per-form StatBoost attribute arrays, with health/damage/resistance/leap, are Phase 3).
- **Phase 2 (2026-06-15):** wired the quest-counter events. `WerewolfEvents.onWerewolfQuestKill` (LivingDeathEvent, `!isCanceled` so were-villager conversions don't count): L4 `HornedHuntsmanEntity` defeat → quest COMPLETE; L5 `Enemy` pounce-kill in beast form (source-faithful: requires `!player.onGround()` — the killing blow lands airborne; the altar text explains the mechanic); L8 `ZombifiedPiglin` (import `…monster.zombie.ZombifiedPiglin`); L9 `Villager`/`Player`. `onTameQuestWolf` (EntityInteract): L7 tame wild adult `Wolf` 1-in-3, cancels the interaction. `HowlPayload`: L6 records `new ChunkPos(pos).toLong()` via `addHowledChunk` at night (`getDayTime()%24000 >= 13000` — `Level.isDay()/isNight()` aren't in this mapping). All gated on `getWolfQuestState()==STARTED`; counters complete on the next altar visit. **Simplification:** form requirement reduced to `WerewolfShiftUtil.isTransformed` until Phase 3 form selection (source gates L8 on WOLFMAN, L5/L9 on wolf-animal form). `WerewolfStateTest` locks the data model.

**Apothecary worldgen freeze + Wolf Altar statue model (2026-06-15)**
- **Apothecary freeze (root cause + fix):** `ApothecaryVillager.configure` ran during structure generation and called `ensureJobSite`, which hits `PoiManager.exists/take`. Force-loading a POI section mid-gen waits on the very chunk being generated → deadlock (log signature: "Apothecary foundation finished" then no "resident phase finished"). Fix: `configure` only `rememberJobSite` now; the POI binding happens later on a live level via `refreshIfApothecary` (fired by the villager's join/tick events). `WorldGenRegion` stores gen-spawned entities in the proto-chunk and fires `EntityJoinLevelEvent` only when the chunk loads live, so no POI work runs during gen. Apothecary was the only village piece doing POI access during generation (others only `finalizeSpawn`). **Rule:** never touch the POI manager / villager job-site brain memory from a `StructurePiece` — defer to a live-tick path.
- **Wolf Altar statue:** replaced the placeholder stone-cube model with the source `ModelWolfAltar` (38-box wolf-god statue, `wolfaltar.png` 128×128). `WolfAltarModel` builds the hierarchy 1:1; `WolfAltarRenderer` (BER, mirrors `StatueGoddessRenderer`) reproduces the source's per-subtree GL scaling — the upright wolfman ×0.7 and each flanking wolf ×0.5 about their pivots, plinth ×1 — by submitting the top-level parts separately (`Model.Simple`'s single-root submit can't scale subtrees). `WolfAltarBlock` is now a `BaseEntityBlock` (FACING, `RenderShape.INVISIBLE`, `WolfAltarBlockEntity` marker); BE + layer + renderer registered. Inventory icon is still the placeholder cube (no special item renderer). Visual geometry is unverifiable without a client — needs in-game QA.

**Lycanthropy questline — Phase 4: Wolftrap curse-spread + Book (2026-06-15)**
- Completed the entity-backed Wolftrap (source `BlockBeartrap` silvered branch). `BeartrapBlockEntity.trySummonBaitWerewolf` (was a stub) spawns a persistent `WolfmanEntity` 16-32 blocks from a baited trap (existing `hasWolftrapBait`: leashed sheep ≤8 + adjacent Wolf Altar + full-moon night, with the start-time delay), via heightmap surface + `noCollision` placement, and stores its UUID. `tryCaptureWolf(LivingEntity)` (source `tryTrapWolf`) makes that exact lured Wolfman infectious when it steps on the trap; the existing `WerewolfEvents.onInfectiousWolfmanDamage` then converts the next bitten villager → Were-Villager. `BeartrapBlock.entityInside` springs a silvered trap for the lured Wolfman (capture) or a werewolf player; removed the now-unused `canTrap`. API note: `Entity.moveTo` is `snapTo` in 1.21.11.
- Book of Shadows → Version 10 / Ninth Edition: Chapter 17 ladder corrected to the implemented questline (gold **ingots**, 30 mutton, 10 Tongue of Dog, Horn of the Hunt, then airborne-kill/howl-16/tame-6/pigmen-30/take-a-life; abilities aligned to dig-bones/feast/paralysing+pack howls/armour-rend/L10 bite), Wolftrap callout rewritten for the live capture, cover changelog + version bumped.
- **Lycanthropy roadmap (Phases 1-4) complete.** Deferred (visual/QA only): four-legged wolf client model (form drives the wolfman render layer), hunter-armour wolf-protection gate, Primer entry (datagen).

**Lycanthropy questline — Phase 3a: form selection + L10 bite (2026-06-15)**
- `WerewolfShiftUtil` form layer: `inWolfForm` (strict WOLF), `inWolfmanForm`, `inWolfShape` (source `isWolfAnimalForm` = either), `canControlTransform` (L≥2), `isWolfmanAllowed` (L≥5), `cycleForm`. `setTransformed` now seeds `WolfForm.WOLF` on transform and clears to NONE on revert, so both the Moon Charm and the auto-moon shift produce a real form.
- Moon Charm (`MoonCharmItem.onUseTick`): voluntary transform gated on `canControlTransform`; **sneak**-use while transformed cycles WOLF↔WOLFMAN (fails below L5). Lang `item.witchery.moon_charm.form.{wolf,wolfman}` + `.no_wolfman`.
- Quest counters refined from the Phase-2 "transformed" placeholder to source form-gates: L5/L9 `inWolfShape`, L8 `inWolfmanForm`, L6 howl + L7 tame `inWolfForm` (strict).
- L10 infectious bite (`WerewolfEvents.onWerewolfPlayerBite`, `LivingDamageEvent.Pre`, source `Shapeshift.processWolfInfection`): L10 + wolf shape + victim post-damage health `< 0.25*max` + 1-in-4 → villager converts to Were-Villager (reusing `spawnWereVillager`, damage zeroed + discard) or player becomes a werewolf (`setWerewolf(true)`). **Divergences:** vampires & existing werewolves are immune (no config hybrid toggle); the source hunter-armour wolf-protection gate is not yet ported (Phase 3b/4 follow-up).
- **Phase 3b (2026-06-15):** ported the source `Shapeshift` per-form abilities. `HowlPayload` is form-gated — wolfman L≥7 → `PARALYSED` III AoE (4+rand(level-6)s), wolf L≥8 → summon 2+rand(level-7) tamed wolves (shared `summonWolves`), else the port's baseline frighten + 2 wolves. `WerewolfEvents.onWolfFeed` (L≥4 wolf shape, non-undead kill → `foodData.eat(8, 0.8f)`), `onWolfDig` (`BlockDropsEvent` on `Blocks.DIRT`, L≥3 strict wolf, 60s cooldown via `lastBoneFind` repurposed as game-time ticks, 1/20 → 1-2 `Items.BONE` added to drops), `onWolfmanRendArmor` (`LivingDamageEvent.Pre`, L9 wolfman: random armour slot `hurtAndBreak(25%·maxDamage)`, or tear off + `spawnAtLocation` for unbreakable). **Still deferred:** the four-legged wolf client model (form only drives the wolfman render layer) and the hunter-armour wolf-protection gate on the bite/rend.

**Vampire Resist Sun + late-level gates (2026-06-14)**
- Source player-vampire sun logic lives in `GenericEvents.onPlayerTick` (not a `Potion`): in sunlight a level-5+ vampire drains 60 blood power/interval and takes Weakness IV + Slowness I + Mining Fatigue I (`Potion.field_76437_t`/`field_76421_d`/`field_76419_f`), a level-<5 vampire loses all power, and any vampire at 0 power past `tickCount>400` calls `EntityUtil.instantDeath`; `setOnFire(5)` fires whenever power hits 0. Ported into `VampireEvents.tickBloodPower`; the port's extra `checkSunburn` flat-damage tick now early-outs on `resistsSun` (level≥5 && bloodPower>0) so Resist Sun is coherent. **Divergence:** instant-death uses `damageSources().onFire()` with `Float.MAX_VALUE` (no custom "sun" death message ported).
- Level gates (source): 1→2 blood fill, 2→3 bite villager 250-280 ×5, 3→4 night-counter, 4→5 sun-grenade, 5→6 Blaze kill, 6→7 Lilith, **7→8 visit 3 distinct villages**, **8→9 bite a caged villager 250-280 ×5**, 9→10 Glass Goblet own-blood. The port had 1-5/9 via auto-fill shortcuts + 6→7/9→10 done; this pass adds 7→8 and 8→9. 7→8 uses `ServerLevel.isVillage(pos)` with a **transient** per-session 64-block de-dupe cell (`VISITED_VILLAGES`) — divergence: not persisted across relog (the questCounter-equivalent count is the set size, also transient). 8→9 reuses the existing `checkVillagerBloodQuest` + new `util/VampireCage` (ports `villagerIsInCage`/`isCaged`: ≥15 of 16 iron-bar ring positions over two body layers + a fully solid 3×3 roof) behind a `BlockQuery` so the geometry is unit-tested in `VampireCageTest`.
- **Follow-ups closed (2026-06-14):** (1) `getMaxBloodPower` now applies the source hybrid halving `500 + (wolfMastery>=2 ? floor(level*0.5) : level)*250` — the port has no werewolf *level*, so `wolfMastery>=2` (0-3 scale) stands in for source `werewolfLevel>=2`. (2) The 7→8 visited-village set moved from a transient `Map<UUID,Set<Long>>` to a persisted `VampireState.visitedVillages` (codec `visited_villages`), so the quest survives relog. (3) Vampire **cure rite** ported from `RiteRemoveVampirism`: `remove_vampirism` dispatch (Medium Ritual ring, filled player taglock trigger, `.requiringOffering(findCureVampirismOfferings)` = breath of goddess + silver sword + garlic + diamond + brew of love), gated on `requireCoven(6)` + `hasActiveCurseMasteryFamiliar` (cat), 10000 altar power via `RitualTransaction`, source 3-in-4 cure (`setVampireLevel(0)` + clear visited villages) else a harmless fizzle. Reuses `resolveOfferedTaglockTarget`; 64-block range check mirrors source. Roster lock bumped 74→75.

**Overworld infusion earth-metal powers (EarthItems, 2026-06-14)**
- Ported source `InfusionOverworld`'s metal/earth powers onto the Witch Hand. New `util/EarthItems`: `isMetal` (iron/golden/chainmail tools+armour + iron/gold ingots & gold nugget — source-faithful set), `oreToIngot`, `isThrowableRock`. **Modern divergence:** 1.7.10 used the OreDictionary for `oreToIngot`; with none in 1.21 the port uses an explicit `Block→ingot` map (vanilla iron/gold/copper ores) — extend that map for modded ores.
- Powers: left-click flings a metal-bearing target (gated on `EarthItems.isMetal` over hand+armour); sneak-channel magnetises loose metal items + smelts ores in a 13×7×13 box (spend a local energy budget, sync once — avoid per-ore packet spam); release disarms a metal-wielding mob, smelts an aimed ore (sneak), hurls a loose earthen block as `EarthRockEntity` (block-side + air in front), or raises a pillar (block-top).
- `EarthRockEntity`: a `ThrowableProjectile implements ItemSupplier` rendered with the vanilla `ThrownItemRenderer` (cobblestone) — register the entity (`EntityType.Builder…MISC`) and the renderer in `WitcheryClient`. Simplest faithful rock without porting the multi-purpose source `EntityWitchProjectile`.
- Shockwave (`events/EarthShockwaveEvents`): a transient per-server task list ticked from `LevelTickEvent.Post` (mirrors source `ServerTickEvents.TASKS`), each stage an expanding 8-damage + radial-knockback ring with a dust ring. **Simplified divergence:** dropped the source block-moving ripple (cosmetic + heavy); the damage/knockback ring carries the feel.

**Infernal infusion enslave AI (PotionEnslaved, Phase 4, 2026-06-14)**
- Ported source `brewing/potions/PotionEnslaved` static helpers into `util/Enslaved`. **Divergence:** the source binds via an infinite `ENSLAVED` `MobEffect` (which also drives the AI through `onLivingUpdate` and persistence). The port instead records enslavement on persistent-data tags — `WITCEnslaverName` (source name tag) + port-only `witchery_enslaver_uuid` — so it interoperates with the already-ported `BrewOfLoveItem`/`BrewOfRaisingItem` tags and survives a player rename. No new `MobEffect` registered.
- AI is event-driven (`events/EnslavedMobEvents`) rather than an `EntityAI*` goal: `onLivingChangeTarget` clears a thrall's target when it would be its enslaver (source `onLivingSetAttackTarget`); `onEnslaverHurt` (`LivingIncomingDamageEvent` on the enslaver) sends nearby thralls after the attacker (source `EntityAIEnslaverHurtByTarget`, which polled the enslaver's revenge target). Multiple subscribers to `LivingChangeTargetEvent` coexist (this one + `WitcheryPlayerEvents`).
- `canBeEnslaved` mirrors source `canCreatureBeEnslaved`: any `Mob` except boss (`EnderDragon`/`WitherBoss` ← `IBossDisplayData`), `AbstractGolem`, `Witch`, and the mod natives `DemonEntity`/`FlameImpEntity`/`EntEntity`. Note golems moved to `net.minecraft.world.entity.animal.golem` in 1.21.11.
- `WitchHandItem.infernalCommandOrEnslave` now routes through `Enslaved` (sneak = bind for 5 energy; non-sneak = command all thralls in a 50-block box to the look target for 1), replacing the ad-hoc `WITCHERY_INFUSION_ENSLAVED_OWNER` UUID-only tag. **This completes the infusion-completion roadmap (Phases 1-4).** The Phase-4 "Otherwhere `InfusedSpirit*Effect` family" was a misnomer — it is the Fetish conjuration system, already ported as `FetishBlockEntity`.

**Creature-power close-out (onDamage variants + in-field sacrifice, 2026-06-14)**
- Ported the remaining source `CreaturePower.onDamage` reactions into `InfusionEvents.onIncomingDamage` (only active for an Infernal infusion holding that power): Pigman fire-ward (3 energy → Fire Res **1200t**, reapplies even if active — matches source `CreaturePowerPigMan`); Blaze/Ghast fire-ward (3 energy → Fire Res **200t**, no-op if already lit — source `CreaturePowerBlaze`/`Ghast`); Skeleton drown-ward (5 energy → Water Breathing 600t + air 30); Squid drown-ward (1 energy → Water Breathing II 1200t + air 300); Creeper lightning-absorb (cancel the hit, +25 charges). Drowning is matched via `DamageTypes.DROWN`, lightning via `DamageTypeTags.IS_LIGHTNING` — **divergence:** the source Creeper detected lightning with a `Thread.currentThread().getStackTrace()` scan for `onStruckByLightning`; the modern damage-type tag is the clean equivalent.
- In-field sacrifice (`InfusionInfernal.trySacrificeCreature`): `WitchHandItem` sneak-left-click on a mob **already enslaved by the player** slays it and grants/recharges that creature's power, no altar required. It reuses the altar-kill charge tables (`WitcheryPlayerEvents.creaturePowerFor`/`creaturePowerChargesFor`, promoted to `public static`) so both acquisition paths award identical charges. **Divergence:** the sacrifice is free — source consumes 1 creature-power charge, but you cannot pay that before owning any power, and the preceding enslave already costs 5 energy.
- **Creature-power subsystem is now feature-complete** apart from an optional charge/selection HUD (the energy/charge bar already exists in `InfusionEnergyHud`).

**Coven system — Phase 3: ally combat AI (2026-06-15)**
- The port's Coven Witch is an `AbstractVillager` merchant with no combat AI; source recruited witches fight for their coven leader. `CovenWitchEntity` now `implements RangedAttackMob` with `performRangedAttack` throwing a witch-style harmful splash potion (`ThrownSplashPotion` + `PotionContents` data component, mirroring `BabaYagaEntity.throwSplashPotion`) from a deliberately weaker roster — Slowness/Poison/Weakness/Blindness, no Wither/boss tier.
- Two inner goals, both gated on `isRecruited()`: `CovenDefendTargetGoal` (a `TargetGoal`) picks the witch's own attacker first (`getLastHurtByMob`), else whatever her **leader** is fighting (`Player.getLastHurtMob`) or has been hurt by, all within ~16 blocks (256 sq); `CovenAttackGoal` (a plain `Goal` like Baba Yaga's `potionAttackGoal`) closes to ~8 blocks and lobs a potion every 60 ticks on line of sight. A shared `isHostileToCoven` guard **never** targets the leader herself or a sister witch of the same `covenOwner`. `covenLeader()` resolves the owner UUID via `level().getPlayerByUUID`.
- Goal indices shifted (PanicGoal stays 1, the attack goal inserts at 2, the rest bump down); targetSelector goal 1 is the new defend goal.

**Coven system — Phase 3 close-out: fight quests + persisted roster (2026-06-15)**
- **FightPet/FightZombie quests** (source `EntityCovenWitch.FightPetQuest`/`FightZombiePetQuest`): the recruit quest roster (`CovenWitchEntity.quests()`, built at call time so the deferred `WitcheryItems` holders are populated) now mixes two fight quests + five fetch quests. A fight quest's `offerQuest` conjures a buffed Spider/Zombie (`MAX_HEALTH` 100, `ATTACK_DAMAGE` 5, named `witchery.witch.pet`, `setTarget(player)`) stamped with the witch's UUID in `getPersistentData()` (`WITCQuestWitchUUID`). `WitcheryMobDropEvents.onQuestPetDrops` (a second `LivingDropsEvent` subscriber) sees that stamp on death and injects the witch's tagged remains — a Spider Eye / Rotten Flesh carrying the same UUID in `DataComponents.CUSTOM_DATA`. Completion (`holdingQuestItem`) requires the held item to be that item **and** carry the matching UUID stamp (source `isQuestItemForEntity`: `Quest.isQuestItem` is false for fight quests, so only the UUID match counts; fetch quests match the plain item). `questKind` (ordinal) is persisted (`WITCQuestKind`). The port keeps its one-step offer→deliver flow (Phase 2), so the monster spawns at offer rather than source's separate accept step.
- **QA fix (2026-06-15):** the conjured Zombie killed its own quest-giver — `CovenWitchEntity` is an `AbstractVillager`, which a vanilla Zombie hunts by default, so it retargeted the witch (glaring in creative, where the player is not a valid melee target). Fixed with `events/CovenQuestEvents.onQuestPetChangeTarget` (a `LivingChangeTargetEvent` subscriber mirroring `EnslavedMobEvents`): a pet carrying `WITCQuestWitchUUID` has any target-change onto a `CovenWitchEntity` cancelled (`setNewAboutToBeSetTarget(null)`). Also offset the spawn ~2.5 blocks toward the player so it no longer materialises embedded in the witch.
- **Persisted player-roster coven cap** (source player `WITCCoven` NBT list, `MAX_COVEN_SIZE = 6`): new `world/CovenData extends SavedData` (`Map<UUID owner, List<UUID> witches>`, overworld `getDataStorage().computeIfAbsent`, codec mirrors `PredictionData`). The cap (`CovenData.isFull`/`size`, and `CovenWitchEntity.covenSize`) now reads this persisted roster instead of scanning loaded entities, so it is stable when witches unload. Recruit-complete `add`s, dismiss + `die(DamageSource)` `remove`, and a one-shot `tick()` reconcile adds any witch whose `covenOwner` is set but is missing from `CovenData` (heals witches recruited before the roster existed). `SeerStoneItem` already routes through `covenSize`, so its readout follows. **Why not the source model exactly:** source dematerialises a recruited witch into the player NBT and re-summons her via rites/Seer Stone; the port keeps her as a live world entity so the Phase-3 ally AI has something to fight, so `CovenData` is a parallel roster record rather than the witch's storage. The 16-field DFU limit on `WitcheryPlayerData`'s main codec also made a SavedData the cleaner home than a new player-data field. Tested by `CovenRecruitTest` (cap enforcement, removal, quest-kind ordinal).
- **QA polish (2026-06-15):** (1) **Two-step dismiss guard rail** — sneak-interacting an owned witch no longer removes her instantly; the first arms a transient 5-second window (`dismissArmedUntil`, game-time based) and asks for confirmation (`witchery.witch.say.confirmleave`), a second within it casts her out. (2) **Readouts** — `CovenWitchEntity.findOfferedQuest(player)` scans loaded witches within 64 blocks for one whose quest is keyed to the player, and `describeQuest` renders a one-line objective; `/witchery status` shows `coven=N/6` + the offered trial, and `SeerStoneItem` adds a quest line. The Seer Stone use order was reworked so a **plain use reads self** (was: only on sneak, so a normal click fell through to the coven-call and appeared to do nothing), **sneak calls a witch**, aim reads another; the coven-size lang reworded from "nearby" to the persisted roster. (3) **No-mechanic-jargon** — player-facing strings avoid terms like "sneak-use" per user feedback (the Book of Shadows phrases it "come to her crouched").

**Custom brews — deferred effect-action roster finished (2026-06-15)**
- Three source `brewing/action/effect/*` actions added to `Brews` (enum `TRANSPOSE`/`RAISE_DEAD`/`SPREAD_DEBUFFS`, registered + dispatched alongside the existing nature actions):
  - **Transpose** (`BrewActionTranspose` splash path, Ender Pearl, 1000 power, lvl 4): `transposeEntity` random-teleports the contacted entity to a safe surface spot up to `10*(strength+1)` blocks away (`Heightmap.MOTION_BLOCKING_NO_LEAVES`, 3-air check), skipped under `ENDER_INHIBITION`. The ritual block-transpose-between-two-circles path is *not* this action (that's the rite system).
  - **Raising / summon undead** (`BrewActionRaising`, Bone, 2000 power, lvl 4): `raiseDead` spawns one undead at the impact + `sourceRaiseDeadExtraCount` (RNG, strength-scaled) more on nearby ground; each is Zombie(60%)/Skeleton(37%)/Zombified-Piglin(3%), `setPersistenceRequired`, enslaved to the caster via `Enslaved.enslave`, and **loot-suppressed** — tagged `witchery_no_drops` (`Brews.SUMMONED_NO_DROPS_TAG`), which `WitcheryMobDropEvents.onLivingDrops` now clears drops for (source `EntityUtil.setNoDrops`, prevents bone-farming).
  - **Spread Affliction** (`spreaddebuffs`, Creeper Head, 2000 power, lvl 8): `spreadDebuffs` copies the target's curable HARMFUL debuffs (amplifier ≤ `max(strength-1,1)`) onto up to `ceil((strength+1)*powerFactor)` creatures within `(strength+1)*4`, then strips them off the target.
- Pure helpers locked by `BrewNatureActionTest`. The other items I'd grouped under the deferred roster were already present: **Raise Land** already scales by strength (`raiseLandHeight`), **Biome Change** is the Shifting Seasons rite (`RitualExecutor`/`BiomeNoteItem`), and **Summon Leonard** is the Witch Cauldron Witch-Hat summon (`WitchCauldronBlock`).

**Egassy polish (2026-06-15, QA):** dropped the floating "Egassy" overhead name (removed the `setCustomName`/`setCustomNameVisible` from the constructor) and recoloured his quips green (`ChatFormatting.YELLOW`→`GREEN`). His lines already broadcast via the action bar to nearby players (`displayClientMessage(msg, true)`), the same path Village Guards use — so this matches the requested "speak as the guards, in green" with no chat spam.
