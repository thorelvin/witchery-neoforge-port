# Legacy Model Coordinate Parity

This is the source-of-truth contract for placing ported Witchery models. A
model is not parity-complete because it looks plausible: its cubes, pivots,
hierarchy, render method, animation values, and outer renderer transform must
all reproduce the 1.7.10 transform stack.

## Authoritative implementations

- Minecraft 1.7.10 `ModelRenderer` (`bix` in the client jar), mapped through
  `fml/conf/joined.srg` and `fields.csv`.
- Modern Minecraft `ModelPart.translateAndRotate` from the transformed
  NeoForge sources in the Gradle cache.
- Original Witchery model and renderer sources under
  `decompiled/witchery-1.7.10-0.24.1/com/emoniph/witchery/client`.
- Original Forge item pipeline in
  `MinecraftForge-1.7.10/src/main/java/net/minecraftforge/client/ForgeHooksClient.java`.

The reusable conversion code lives in
`client/model/LegacyModelTransforms.java`. Matrix tests cover both legacy
render methods with all three rotations nonzero.

## Coordinate layers

Work from the cube outward. A later layer cannot repair a wrong earlier one.

1. **Cube-local geometry**: `addBox(x, y, z, dx, dy, dz, deformation)` uses
   model pixels. The display list converts cube vertices using the render
   scale, normally `1 / 16` block per model pixel.
2. **Part pivot**: `rotationPointX/Y/Z` is also in model pixels. Both old and
   modern renderers divide it by 16 at render time. Copy these values directly
   into `PartPose` or `ModelPart.setPos`; never pre-divide them.
3. **Part rotation**: angles are radians. The correct modern values depend on
   which legacy render method the source calls; see below.
4. **Parent hierarchy**: a normal child is transformed inside every ancestor's
   matrix. Moving a source root part under a new convenience parent changes its
   physical location and rotation center even when every visible cube value is
   copied.
5. **Model renderer transform**: entity renderers and TESRs can translate,
   rotate, mirror, or scale the complete model before any part renders.
6. **Item context transform**: 1.7.10 Forge applies different inventory,
   entity, equipped-block, and equipped-item matrices before invoking a custom
   item renderer. Modern item JSON display transforms are another matrix layer,
   not a substitute with automatically equivalent values.

For a normal nested part, a cube vertex reaches its renderer space as:

```text
context * parent... * offset * T(pivot / 16) * Rz * Ry * Rx * (cube / 16)
```

Legacy `offsetX/Y/Z` is exceptional: it is a raw world-unit translation, not a
model-pixel pivot. Modern `ModelPart` has no direct equivalent, so preserve it
with a dedicated parent node or an explicit `PoseStack.translate` at the same
position in the matrix stack.

## Legacy render methods

### `ModelRenderer.render(scale)`

The old implementation composes:

```text
T(offset) * T(rotationPoint * scale) * Rz * Ry * Rx
```

It then renders every child under that matrix. Modern
`ModelPart.translateAndRotate` uses `T(pivot / 16)` and
`Quaternionf.rotationZYX(z, y, x)`, so ordinary source pivots and Euler angles
copy directly. Use `LegacyModelTransforms.render(...)` to make that contract
explicit.

### `ModelRenderer.renderWithRotation(scale)`

The old implementation composes:

```text
T(rotationPoint * scale) * Ry * Rx * Rz
```

It does **not** apply `offsetX/Y/Z` and does **not** recurse into children.
Modern `ModelPart` cannot switch Euler order. Use
`LegacyModelTransforms.renderWithRotation(...)` for baked poses and
`setRenderWithRotation(...)` for animated poses; they preserve the legacy
quaternion and convert it to equivalent modern Z-Y-X Euler values.

Original Witchery calls this method only in `ModelWitchHand` and for the
Hellhound head and tail. Death's Hand therefore requires conversion on every
part. Hellhound requires conversion whenever multiple head/tail axes are
active, especially the source shake animation's Z rotation.

### `ModelRenderer.postRender(scale)`

This applies the same pivot and Z-Y-X rotation as ordinary `render`, without
drawing. Modern `ModelPart.translateAndRotate` is its direct equivalent. It is
used by original held-item/head attachment renderers and must be included when
auditing equipment placement.

## Block and item origins

Original Witchery TESRs commonly use this block-space adapter before rendering
models authored around Y=24:

```text
T(0.5, 0.5, 0.5) * Rz(180 degrees) * T(0, -1, 0)
```

The current port's `startBlockEntityPose` and Beartrap world renderer preserve
that exact matrix. Direction rotations occur after it and must match the
source metadata mapping.

Original `RenderBlockItem` invokes the same TESR at the origin and adds
`T(-0.5, 0, -0.5)` only in the dropped-entity context. Forge supplies the
remaining inventory/equipped transforms. Each modern special block item must
therefore be checked as the product of its JSON display transform and special
renderer matrix, not by comparing either layer alone.

## Physical anchor verification

For every source-mapped model, parity evidence must cover:

1. Part names/count, parent-child graph, and which parts are actually drawn.
2. Texture dimensions and each cube's UV, origin, dimensions, mirror flag, and
   deformation.
3. Every part pivot and initial rotation, including the source render method.
4. Dynamic pose values and state semantics at meaningful animation frames.
5. The outer renderer matrix, scale, shadow size, and attachment transforms.
6. World-space anchor points or full matrices for representative cube corners.
7. Runtime A/B captures in the original and port at controlled camera, pose,
   handedness, entity yaw, and item state.

Tests should compare numeric matrices or transformed anchors. Screenshots are
the final rasterization check, not the only coordinate evidence.

## Audit inventory

| Port family | Original source | Current evidence |
| --- | --- | --- |
| `DeathHandModel` | `ModelWitchHand` | Geometry/pivots and Y-X-Z conversion tested; first/third-person renderer matrices tested; runtime hand position calibrated. |
| Held Brew Bag, Cane Sword, Hand Bow, Huntsman's Spear, and Mystic Branch | Matching source `Model*` plus `Render*` | Exact source part graphs, pivots, mirror timing, textures, renderer matrices, Cane Sword pivot scaling, Hand Bow bolt meshes, and draw stages are wired for held contexts through the shared legacy equipped-item adapter. Compilation passes; numeric tests, resource-load checks, glint parity, dedicated-server check, and runtime A/B remain the next continuation point. |
| `BeartrapModel` | `ModelBeartrap` | Rewritten to the exact 20-part source graph, cube bounds, pivots, plate heights, and 1.2-radian sprung jaws; numeric tests added. Runtime A/B pending. |
| `HellhoundModel` | `ModelHellhound` | Source geometry, gait, shake, interested/converting poses, and multi-axis Y-X-Z conversion restored and tested. Controlled yaw-0/yaw-180 A/B captured. |
| `LeechChestModel` | `ModelLeechChest` | Source geometry, cubic lid easing, independent quadrant rotations, and stored-player sac visibility restored and tested. Runtime A/B pending. |
| `BloodCrucibleModel` | `ModelBloodCrucible` | Source nine-part body, renderer matrix, texture size, and continuous liquid plane height restored and tested. Runtime A/B pending. |
| `BrazierModel` | `ModelBrazier` | Source 14-part structure, ash plane, renderer matrix, and 64x64 texture restored and tested. Runtime A/B pending. |
| `CandelabraModel` | `ModelCandelabra` | All 14 source cubes, pivots, effective mirror timing, exact texture, and TESR matrix tested. Runtime A/B pending. |
| `ChaliceModel` | `ModelChalice` | Restored the source one-part mirrored seven-cube body, separate liquid plane, exact texture, renderer matrix, and world anchors. Runtime A/B pending. |
| `KettleModel` | `ModelKettle` | All 24 source cubes, pivots, rotations, effective mirror timing, four reachable liquid frames, three fill heights, tint normalization, texture, and directional matrices tested. Runtime A/B pending. |
| `DistilleryModel` | `ModelDistillery` | Restored the four mirrored source bottles and jar-count visibility; all 21 cubes, pivots, bottle tilt, texture, directional matrices, and transition-only client sync tested. Runtime A/B pending. |
| `SpinningWheelModel` | `ModelSpinningWheel` | Restored the single mirrored five-cube wheel, source wall-clock wheel/thread animation, idle item reset, transition-only client sync, all 14 cubes, and directional matrices. Runtime A/B pending. |
| `SilverVatModel` | `ModelSilverVat` | All 21 body/spout/deposit cubes, effective mirror state, source spout-neighbor mapping, texture, TESR matrix, liquid tint construction, and exact world-space liquid footprint tested. Runtime A/B pending. |
| `FumeFunnelModel` | `ModelFumeFunnel` | Corrected the right tube's one-pixel Y error, restored simultaneous chimney/side-pipe visibility, and restricted both item variants to the source idle subset; all 22 cubes, effective mirror timing, conditional groups, and directional matrices tested. Runtime A/B pending. |
| `PoppetShelfModel` | `ModelPoppetChest` | Source one-cube shelf and metadata-independent matrix tested; displayed slots proven algebraically equivalent to the source grid, poppet-only visibility and wall-clock spin restored, and modern upright-sprite compensation documented. Runtime A/B pending. |
| `WitchesOvenModel` | `ModelWitchesOven` | All nine source parts and 13 cubes tested; restored mirror winding on the eight named leg cubes and verified all four directional TESR matrices. Runtime A/B pending. |
| `CrystalBallModel` | `ModelCrystalBall` | Replaced the static JSON approximation with the exact six-cube block-entity and special-item renderer; source matrix, effective mirror timing, 160-tick inner-globe pulse, translucent shell tint, texture hash, and world-space bounds tested. Runtime A/B pending. |
| `DreamCatcherModel` | `ModelDreamCatcher` | Restored the Java block-entity renderer and all five weave-specific net faces; all ten source cubes, effective mirror timing, metadata-derived offsets/rotations, texture, item no-weave state, and exact wall-plane centers tested. Runtime A/B pending. |
| `CoffinModel` | `ModelCoffin` | Replaced the invented split shell/four-texture renderer with the exact eight-cube source model rendered once per half; mirror winding, seam offsets, directional matrices, 90-degree cubic lid motion, 10-tick client animation, source collision walls, item renderer, texture, and exact two-block stitch tested. Runtime A/B pending. |
| `FetishModel` / `FetishTrentModel` | `ModelFetishScarecrow` / `ModelFetishTrent` | All 20 source cubes, pivots, rotations, and effective mirror timing tested; restored metadata-equivalent facing, two-pass dye tinting, spectral alpha, source collision footprint, and special-item models. Scarecrow ground contact is verified numerically. Runtime A/B pending. |
| `AlluringSkullModel` | vanilla `ModelSkeletonHead` plus `RenderAlluringSkull` | Source head cube and 64x32 textures restored; floor rotation and all four exact wall translations (`0.26` / `0.74`), yaw matrices, and attachment-plane centers tested. Runtime A/B pending. |
| `DemonHeartModel` | `ModelDemonHeart` | All ten source cubes, pivots, rotations, effective unmirrored winding, static/pulsing groups, exact 25-tick local pulse, directional matrices, block footprint, and source texture restored and tested. Runtime A/B pending. |
| `WolfHeadModel` | `ModelWolfHead` plus `RenderWolfHead` | Source four-cube head, 64x32 texture mapping, exact Hellhound texture, 2D inventory form, floor rotation, and all four `0.26` / `0.74` wall matrices and head centers tested. Runtime A/B pending. |
| `WolfAltarModel` | `ModelWolfAltar` plus `RenderStatueWerewolf` | All 37 source cubes, hierarchy, rotations, four mirrored limbs, three pivot-scaled figures, source render order, world facings, two-block bounds, exact texture, and custom inventory TESR pose restored and tested. Runtime A/B pending. |
| `GrassperModel` | `ModelGrassper` plus `RenderGrassper` | All ten source parts, rotations, effective mirror timing, directional matrices, exact texture, special item, held-item transform, graphics-mode behavior, and wall-clock spin rate tested. Runtime A/B pending. |
| `StatueGoddessModel` | `ModelGoddess` plus `RenderGoddess` | All nine source cubes, effective mirror timing, fixed arm pose, world facings, exact texture, two-block bounds, and custom inventory TESR pose restored and tested. Runtime A/B pending. |
| `MandrakeStatueModel` / `WolfStatueModel` | `ModelMandrake` / `ModelStatueWolf` plus their statue TESRs | Restored the source six- and twelve-part graphs and effective mirror timing; exact `-0.255` / `-0.26` vertical offsets, half scale, all four world facings, inventory TESR matrices, textures, and renderer routes tested. Runtime A/B pending. |
| `DarkMarkModel` | `ModelDarkMark` plus `RenderDarkMark` | Both mirrored source skull cubes, yaw/pitch look tracking, model-local 2x scale and translation, exact texture, and source 0.5 shadow radius restored and tested. Controlled yaw-0/yaw-180 A/B captured; source smoke obscures most of the mesh, so numeric geometry remains the deciding evidence. |
| `BroomModel` | `ModelBroom` plus `RenderBroom` | All ten source cubes, pivots, rotations, effective mirror timing, exact texture, brush tint, hit wobble, `90 - yaw` rotation, source shadow, and custom-renderer vertical anchor restored and tested; removed invented bristle sway. Runtime A/B pending. |
| `DeathEntityModel` | `ModelDeath` plus `RenderDeath` | Restored the eight-part source graph, mirrored two-cube scythe with zero-depth blade, robe-only 1.05 X/Z scale, complete attack/idle pose, leg offsets, exact texture, and source shadow. Numeric tests and controlled yaw-0/yaw-180 A/B are complete. |
| `DemonEntityModel` | `ModelDemon` plus `RenderDemon` | All eight source parts and 14 cubes, wing poses, effective mirror timing, attack/gait poses, exact texture, source brightness, and renderer-layer walking roll restored and tested. Controlled A/B agrees within one raster pixel. |
| `FlameImpModel` / `FlameImpWingModel` | `ModelImp` plus `RenderImp` | Restored the ten-cube body graph, separate source `128x32` wing UV coordinate space, riding pose, attack-state resets, leg offsets, powered X/Z scale, exact texture, and source shadow; UV normalization and controlled yaw-0/yaw-180 A/B are complete. |
| `LeonardModel` | `ModelLeonard` plus `RenderLeonard` | All 16 source cubes, nested neck/head and gown hierarchy, effective unmirrored winding, riding and generic attack poses, partial-tick special attack, exact texture, full brightness, and source shadow restored and tested. Controlled yaw-0/yaw-180 A/B captured. |
| `LordOfTormentModel` | `ModelLordOfTorment` plus `RenderLordOfTorment` | All 12 source parts and 20 cubes, nine mirrored head cubes, unmirrored horns/body, attack and wing poses, exact texture, full brightness, source shadow, and the original positive-X/Z movement gate restored and tested. Controlled yaw-0/yaw-180 A/B captured. |
| `BabaYagaModel` | `ModelBabaYaga` plus `RenderBabaYaga` | Rebuilt the inherited source villager rig and all 14 parts/19 cubes, including crossed arms, expanded robe, nested nose detail, exact hat, mirrored standalone mortar/pestle, cloak, entity-ID wobble, enchanted-broom item anchor, 0.9375 scale, exact legacy vertical anchor, source texture, and 0.5 shadow. Controlled yaw-0/yaw-180 A/B captured. |
| `CatFamiliarModel` | vanilla `ModelOcelot` plus `RenderWitchCat` | Exact eleven-cube ocelot geometry restored, including the 13.8-pixel front-leg pivots; source walking, crouching, sprinting, sitting, tail modes, split kitten head/body transforms, tamed 0.8 scale, legacy vertical anchor, 0.5 shadow, and exact black-cat texture are tested. Controlled yaw-0/yaw-180 A/B captured. |
| `OwlModel` | `ModelOwl` plus `RenderOwl` | Restored all six source parts and nine mirrored cubes, body-child legs, exact landed/flying/sitting poses, split owlet head/body transforms, synchronized persistent feather dyes and source tint math, exact rest/surface gate, legacy vertical anchor, 0.5 shadow, and source texture. Controlled yaw-0/yaw-180 A/B captured with expected landed-state variance. |
| `ToadModel` | `ModelToad` plus `RenderToad` | Restored all six source parts and ten mirrored cubes, exact half-pixel eye positions and body/arm angles, movement leg bias, opposing arm gait, sitting legs, split child transforms, synchronized persistent skin dyes and source tint math, legacy vertical anchor, 0.3 shadow, and exact texture. Controlled yaw-0/yaw-180 A/B captured. |
| `EntModel` | `ModelEnt` plus `RenderEnt` | Verified all 16 mirrored source cubes and hierarchy, including the two inner leaf cubes baked in their source-local 64x32 UV space, source leg gait, triangular walking roll, legacy vertical anchor, 0.5 shadow, and exact 256x256 texture. Controlled yaw-0/yaw-180 A/B captured. |
| `FlyingMonkeyModel` | `ModelMonkey` plus `RenderWingedMonkey` | Verified all 14 source cubes and hierarchy, exact mirror timing, landed/flying/sitting/tail/look branches, source attack-timer arm wave (including its net-head-yaw parameter quirk), exact zero-velocity and solid/liquid landing gate, source feather tint, legacy vertical anchor, 0.5 shadow, and exact texture. Controlled yaw-0/yaw-180 A/B captured with expected landed-state variance. |
| `GhostModel` | `ModelSpectre`, `RenderBanshee`, and `RenderSpectre` | Verified the seven-cube source geometry, nested mouth plane, reaching and screaming pose branches, fixed body anchor, exact legacy vertical anchor, zero shadows, source translucent alpha (including obscured Spectre), and exact textures. Controlled Spectre/Banshee yaw-0/yaw-180 A/B captured. |
| `PoltergeistModel` | `ModelPoltergeist` plus `RenderPoltergeist` | Verified all eight source cubes, duplicate arm pairs, walking and age-driven arm motion, source render-time leg anchors, restored 15-tick attack pose, exact legacy vertical anchor, zero shadow, 0.4 alpha, and exact texture. Controlled yaw-0/yaw-180 A/B captured. |
| `SpiritModel` / `LostSoulModel` | `ModelSpirit` plus `RenderSpirit` | Restored the fixed five-cube mirrored body with no invented bob, source texture and RGB/0.6-alpha tinting, and the exact rebased matrix for the legacy model-local `translate(0,0.65,0)` then half scale. The modern pre-base transform is `translateY(-0.1073125)` then `scale(0.5)`. Controlled yaw-0/yaw-180 A/B captured; particles, alpha, and Lost Soul's randomized source color make numeric geometry the deciding evidence. |
| `GoblinGulgModel` | `ModelGoblinGulg` plus `RenderGoblinGulg` | Rebuilt the seven-part/11-cube source graph from Gulg's own face geometry, including exact tusks, nose, lip, left-arm origin, and all-cube mirror timing. Restored walking, held-item, swing, crouch, bow, idle bob, and attack branches, runtime arm pivots, standing/crouching legacy Y anchors, 0.5 shadow, and exact texture. Controlled base-model yaw-0/yaw-180 A/B is complete; armor/item attachment and source glint remain separate follow-ups. |
| `GoblinMogModel` | `ModelGoblinMog` plus `RenderGoblinMog` | Rebuilt the eight-part/12-cube source graph with Mog's exact five-cube face, mirrored geometry, 45-degree chest plate, and skirt. Restored walking, held-item, swing, crouch, bow, idle bob, runtime pivots, standing/crouching legacy Y anchors, 0.5 shadow, and exact texture. Controlled base-model yaw-0/yaw-180 A/B is complete; armor/item attachment and source glint remain separate follow-ups. |
| `HobgoblinModel` | `ModelGoblin` plus `RenderGoblin` | Restored all 13 visible mirrored source cubes, exact nose and ear sequence, source short-body anchors, working/Koboldite mining, worshipping, crouching, held-item, swing, bow, and idle branches, standing/crouching legacy Y anchors, 0.5 shadow, and exact four-texture profession routing. A permanently hidden head-child compatibility cube satisfies modern `HumanoidModel` without rendering. Controlled base-model yaw-0/yaw-180 A/B is complete; armor/item attachments remain a separate follow-up. |
| `GrotesqueModel` | `ModelGrotesque` plus `ModelOverlayRenderer` and the fixed player aura route | Verified the single mirrored seven-cube demon head, look tracking, source model-local `translateY(0.735)` and `scale(1.3)`, potion-only `scale(1.01)`, legacy living-base correction, invisible-viewer alpha behavior, and exact Demon texture. Runtime A/B pending. |
| `HornedHuntsmanModel` | `ModelHornedAvatar` plus `RenderHornedAvatar` | Replaced the unrelated port geometry with all 16 source parts and 18 cubes, including the nested shins, feet, forearms, mirrored three-cube spear, and zero-depth horn and spear planes. Restored source gait, attack timer/event, walking body roll, full brightness, legacy Y anchor, 0.5 shadow, and exact `avatar.png`. Controlled base-model yaw-0/yaw-180 A/B is complete; the optional source green glint remains a separate follow-up. |
| `LilithModel` | `ModelLilith` plus `RenderLilith` | Verified all 24 source parts/cubes and seven effective mirrors; restored the four zero-depth horn/wing planes, gait-driven double skirt, riding branch, idle arm motion, source yaw-coupled attack-wave quirk, full brightness, 0.5 shadow, legacy Y anchor, and exact texture. Controlled yaw-0/yaw-180 A/B captured. |
| `MandrakeEntityModel` | `ModelMandrake` plus `RenderMandrake` | Verified the eight source cubes, fixed head/legs, triangle-wave arms, four effective mirrors, and zero-depth leaf plane. Moved the 6.5-degree gait roll from the model root to the source renderer-space pivot, restored the legacy Y anchor and 0.5 shadow, and retained the exact texture. Controlled yaw-0/yaw-180 A/B captured. |
| `MindrakeModel` | `ModelMindrake` plus `RenderMindrake` | Verified the rigid five-cube source graph, zero-depth leaves, unmirrored winding, and exact texture; removed invented leaf sway and leg gait. Restored the source whole-entity 6.5-degree gait roll at renderer pivot, 0.25 shadow, and legacy Y anchor. Controlled yaw-0/yaw-180 A/B captured after making harness cleanup non-destructive. |
| `MirrorFaceModel` | `ModelMirrorFace` plus `RenderMirrorFace` | Verified the single unmirrored head cube, 24-pixel pivot, look tracking, 0.8 alpha, zero shadow, and exact texture. Rebased source post-base `scale(0.75)` then `translateY(0.4)` into the exact modern pre-base transform `translateY(-0.0820625)` then `scale(0.75)`. Controlled yaw-0/yaw-180 A/B captured. |
| `NightmareModel` | `ModelNightmare` plus `RenderNightmare` | Verified all 17 source cubes, nested six-finger hierarchy, exact finger angles, seven tendril pivots/moduli, attack pose, and source unmirrored winding. Tendrils now use integer entity ticks, with exact 0.9/0.6 alpha, 0.5 shadow, legacy Y anchor, and texture. Controlled yaw-0/yaw-180 A/B captured. |
| `ParasyticLouseModel` | `ModelLouse` plus `RenderParasyticLouse` | Verified all seven body segments and three fins, accumulated Z pivots, age-driven yaw/X-pivot wave and fin copying, unmirrored winding, 180-degree death flip, 0.3 shadow, legacy Y anchor, and exact texture. Controlled yaw-0/yaw-180 A/B captured. |
| `TreefydModel` | `ModelTreefyd` plus `RenderTreefyd` | Verified all 11 source cubes, three mirrored head cubes, four zero-depth petal/tongue/leaf planes, 45-degree crossed leaves, base and four-leg pivots, look/gait poses, exact texture, 0.5 shadow, legacy Y anchor, and whole-entity 5-degree gait roll. Controlled yaw-0/yaw-180 A/B captured. |
| `WolfmanModel` / `WerewolfWolfModel` | `ModelWolfman`, `RenderWolfman`, `TransformWolfman`, `TransformWolf`, and Minecraft 1.7.10 `ModelWolf` | Replaced the invented player/reflection wolfman with the same ten-part/13-cube source model used by the mob, including its root tail, two effective mirrors, exact attack/gait/sitting/crouch/held-item/bow/idle branches, 0.5 shadow, legacy anchors, and exact source texture. Rebuilt player WOLF from the exact asymmetric 1.7.10 11-cube wolf geometry and standing/sitting animation, bundled its legacy texture, and preserved source local/remote crouch differences. Controlled base-model yaw-0/yaw-180 A/B is complete; armor and held-item attachments remain separate follow-ups. |
| `WitchHunterModel` | `ModelWitchHunter` plus `RenderWitchHunter` and Minecraft 1.7.10 `ModelBiped` | Restored the exact standard biped plus source hat brim, hat top, and body-child coat geometry; corrected all three custom cubes to source-unmirrored winding and moved the coat to its source Y=11 pivot. Restored riding, crouching, held-item, swing, bow, and idle poses, headwear copying, legacy Y anchors, 0.5 shadow, source skin routing, standard armor, and held-item layers. Controlled unequipped yaw-0/yaw-180 A/B is complete; runtime attachments remain a separate follow-up. |
| `LegacyBipedModel` / Elle / human Reflection | Minecraft 1.7.10 `ModelBiped`, `RenderFollower`, and `RenderReflection` | Added one exact seven-cube legacy biped pipeline with its two effective mirrors and full riding, crouching, held-item, swing, bow, and idle pose logic. Elle no longer applies her exact 64x32 follower skin to an unrelated Witch model; human Reflections no longer bake a modern player mesh. Both routes now use source textures, 0.5 shadows, armor/held-item support, and legacy standing/crouching anchors. Runtime attachment A/B pending. |
| `EarmuffsModel` / `EarmuffsLayer` | `ModelEarmuffs` plus `ItemEarmuffs` | Restored all five unmirrored source cubes and their exact head-local coordinates, replacing the port's two-swatch placeholder with the original 64x64 wearable texture. The layer's parent-head transform is matrix-equivalent to the source child attachment. Runtime wearer A/B pending. |
| `DeathArmorModel` | `ModelClothesDeath` plus `ItemDeathsClothes` | Restored the inherited zero-deformation seven-cube biped and headwear beneath all ten source clothing children instead of replacing those parents. The exact hierarchy, deformations, rendered-vertex bounds, effective unmirrored custom winding, slot visibility, and 128x64 texture are tested. Runtime wearer A/B pending. |
| `HunterArmorModel` | `ModelHunterClothes` plus `ItemHunterClothes` | Restored the inherited 0.4-deformation head/headwear/body/arms, three unmirrored hat cubes, unmirrored body-child coat, and the separately selected 0.01-deformation leg geometry. Removed invented duplicate leg/boot child groups; rendered bounds, winding, slot visibility, and source sheets are tested. Runtime wearer A/B pending. |
| Witch clothing armor models | `ModelWitchesClothes` plus `ItemWitchesClothes` | Restored the source model selection: 0.61 for hats, robes, and footwear; 0.45 for armor-type-2 belts. Witches' and Baba's hats retain the inherited inflated biped, Baba's stack and necromancer shoulders use source-unmirrored winding, and the missing biting/bark belt model and exact two-pass sheets are wired. The charge-driven bark growth overlay remains a dedicated runtime follow-up; base clothing A/B pending. |
| Goblin clothing armor models | `ModelGoblinClothes` plus `ItemGoblinClothes` | Verified the 0.61 Twisting Band and Mog's Quiver models, zero-deformation Gulg girdle model, four unmirrored body-child quiver parts, source slot subsets, and exact 64x32 sheets. Runtime wearer A/B pending. |
| `VampireArmorModel` / `VampireArmorChainLayer` | `ModelVampireArmor` plus `ItemVampireClothes` | Replaced the later Resurrected-style approximation with the 0.24.1 model: source 0.3 outer body/boots, 0.02 leggings/body, two-part top hat, five skirt layers, three cloak parts, female chest, crouch/riding/gait poses, metal/female selection, exact 64x96 sheets, and the source cumulative 1.06 chain pass with -0.05 arm pivot. Removed invented slim and veil/pauldron geometry. Runtime wearer A/B pending. |
| `GarlicGarlandModel` | `ModelGarlicGarland` plus `TileEntityGarlicGarlandRenderer` | Replaced the flat block approximation with the exact 29-part source graph, texture, upper/lower block routing, facing matrix, collision shape, block entity, and special-item renderer. Controlled block capture confirms the source silhouette and anchor. |
| `MirrorModel` / `MirrorBlockEntityRenderer` | `ModelMirror` plus `TileEntityMirrorRenderer` | Restored the exact 19-part foot/head graph, both source textures, directional matrix, active overlay, two-block collision shapes, block entities, and item route. Controlled original/port captures agree to one vertical raster row. |
| `SpectralFamiliarModel` | `ModelFamiliarPig` plus `RenderFamiliar` | Replaced the placeholder with the exact six-part 1.7 pig graph, sitting/walking poses, source texture, translucent tint, zero shadow, 0.8 tamed scale, and rebased legacy anchor. Two-angle captures have the same horizontal center and height; the remaining one-row vertical and two-pixel back-edge differences are raster/lighting variance. |
| `VillageGuardModel` / `VampireRenderer` | `ModelVillageGuard` plus `RenderVillageGuard` / `RenderVampire` | Restored the shared source biped geometry, custom overlays, pose branches, textures, and exact 0.6x1.8 adult dimensions. Controlled unequipped yaw-0/yaw-180 captures certify both routes without armor-state noise. |
| Witch Cauldron JSON | `ModelCauldron` plus `RenderCauldron` | Replaced the full-block approximation with the exact 17 source body cubes after algebraically composing the TESR and shared Y=16 part pivot. All three liquid planes use the source 12-pixel footprint and source fill heights. Approved exception: the port keeps 12 custom splayed-foot cubes, grounded at Y=0 and reaching all four block corners. Collision bounds and every fill-state resource are tested numerically. |
| Special block models/items | Matching `Model*` plus TESR and `RenderBlockItem` | Common outer matrices identified; model-by-model geometry and modern JSON product remain under audit. |
| Entity models | Matching original `Model*` and entity renderer | Source inventory, part graphs, hierarchy, animation, renderer anchors, registry dimensions, and two-angle controlled captures are complete for the ported 0.24.1 entity set. Particle-heavy and state-dependent captures use numeric source tests as the deciding evidence. |
| Armor and attachment models | Clothes models plus `postRender` call sites | Source mapping inventoried; wearer pivots and held/head attachment matrices remain under audit. |
| New port-only models | No direct 1.7.10 model | Excluded from numeric source parity; inspect only for intended placement and visible raster gaps. |

## Runtime A/B capture ledger

The reproducible capture tooling is under `tools/original_capture_agent` and
`tools/run_model_parity_capture.ps1`. Original and port images are written to
the sibling workspace directory `captures/legacy-model-parity`. Both clients
use 1920x1080, FOV 70, gamma 1.0, a nine-block camera distance, fixed noon
unless the entity requires night, fixed entity coordinates, and yaw 0/180.

Runtime coverage now includes:

- Demon, Flame Imp, Death, Lord of Torment, Hellhound, Leonard, Horned
  Huntsman, and Wolfman as the high-risk entity batch.
- Village Guard and Vampire in controlled unequipped states.
- Mandrake, Treefyd, Ent, Owl, Toad, Cat Familiar, Parasytic Louse, Baba Yaga,
  Nightmare, Spectre, Poltergeist, Banshee, Spirit, Witch Hunter, Dark Mark,
  Mindrake, Hobgoblin, Goblin Gulg, Goblin Mog, Lost Soul, Lilith, Flying
  Monkey, and Mirror Face in one 46-image front/rear batch.
- Spectral Familiar, Garlic Garland, and the two-block Mirror in their own
  controlled captures.
- Witch Cauldron in a controlled empty-state block capture after the exact
  body rewrite. The source and port body share the same horizontal center and
  bottom raster row; their top differs by one raster row. The port's wider
  lower silhouette is the approved custom splayed-foot exception.

The capture harness rebuilds the studio floor for every entity and moves the
previous target to Y=-1024 before killing it. This prevents death explosions,
drops, and other cleanup side effects from contaminating the next image.
`DarkMarkEntity` remains `noSummon` in gameplay; the property-gated client
harness creates only that target locally for its two screenshots.

Runtime images are supporting evidence, not a replacement for matrices. Owl
and Flying Monkey poses vary with landed state and animation phase. Spirit,
Lost Soul, and Dark Mark meshes are partly obscured by source particles and
alpha blending; Lost Soul also randomizes its source soul color at creation.
For those cases the exact part graph and outer-transform tests are the deciding
coordinate evidence. The Demon pair agrees within one raster pixel. Spectral
Familiar has an exact horizontal center and height with only one vertical
raster row and two back-edge pixels of lighting variance. Mirror agrees within
one raster row. Witch Cauldron has the same body center and bottom row, with a
one-row top-edge variance between the TESR and baked-model pipelines.

## Entity dimensions

Entity renderer anchoring starts at the entity origin, so a correct model on a
wrong bounding box still produces bad ground contact and camera-relative
placement. The registry now follows the source constructors:

| Source family | Width x height |
| --- | --- |
| Default 1.7 living entity, including Witch Hunter, Poltergeist, Baba Yaga, Were Villager, Egassy, Elle, Death, Leonard, Village Guard, and Vampire | `0.6 x 1.8` |
| Owl and vanilla-ocelot Cat Familiar | `0.6 x 0.8` |
| Toad | `0.8 x 0.8` |
| Hobgoblin | `0.6 x 0.95` |
| Spirit and Lost Soul | `0.25 x 0.25` |
| Active Spell Effect | `1.0 x 1.0` |
| Flame Imp | `0.4 x 1.3`, or `0.6 x 1.3` while powered |

Coven Witch remains `0.6 x 1.95` because the source explicitly sets that size.
Port-only entities retain their intentional modern dimensions. The source
guard's unused larger guard-type path is not applied: the port's guard type is
a cosmetic variant, while the active source adult guard and vampire paths are
both `0.6 x 1.8`.

## Source inventory closure

Every `Model*.java` class in Witchery 0.24.1 now has a port mapping or an
explicit modern-native disposition. `ModelPoppetChest` maps to
`PoppetShelfModel`; source clothing models map to their armor models; and
`ModelVillagerBed` is intentionally obsolete because modern villagers and beds
natively provide the sleeping renderer path that the 1.7 event workaround
implemented. Aggregate tests deliberately cover shared models such as Spirit
and Lost Soul, so test filenames are not always one-to-one with model files.
No unresolved placeholder, simplified, or approximate source-model geometry
remains in the client model/renderer packages.

Do not mark a family complete until all applicable evidence above exists.
