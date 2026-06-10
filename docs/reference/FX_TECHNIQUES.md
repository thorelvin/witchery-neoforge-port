# FX Techniques — deliberate visual effects for Witchery

Reusable rendering tricks discovered during the port, written up so we can use them *on purpose*
later. Each entry: what it looks like, why it happens, how to author it, how to tune it, and where it
fits the mod's mood.

---

## "Glamour Bleed" — chromatic halo via colored-alpha mipmap bleed

*(a.k.a. colored-alpha mipmap halo; "glamour" = a witch's illusory aura)*

**What it looks like:** a soft, ghostly halo of colour that seems to seep out of a block/sprite's
silhouette. It is **faint up close and blooms with distance** (and as the camera moves the bloom
shimmers/“smears”), because it lives in the texture's mip levels, not the base pixels. On a cross-model
plant or a cutout sprite it reads like a coloured aura or heat-haze clinging to the shape.

**Why it happens (the mechanism):** Minecraft mip-maps the block/item atlas. Mip generation averages
the **RGB of every texel in a 2×2 block, including fully-transparent ones** (alpha is averaged
separately). So if the *transparent* pixels around a sprite carry a colour (e.g. RGB = purple, alpha =
0), that colour bleeds into the visible edge texels at lower LODs → a coloured halo that strengthens
with distance. Normally this is a **bug** (we hit it as the "mandrake purple smear" — see
[BUGS](../status/BUGS.md) 2026-06-10 and [PORTING_NOTES](./PORTING_NOTES.md): the fix is *alpha
bleeding*, i.e. recolouring transparent pixels to match their opaque neighbours so nothing foreign
bleeds). Glamour Bleed is that same physics, **inverted and controlled** — we *want* the foreign colour.

**How to author it (the recipe):**
1. Start from a normal cutout sprite (cross-model plant, item, decal). Keep `"render_type":
   "minecraft:cutout"` so the base render stays crisp (the halo is mip-only).
2. In the **fully-transparent margin** around the silhouette, paint the aura colour with **alpha = 0**
   (colour present, fully invisible at LOD0). A 1–3 px feathered ring around the shape is plenty.
3. The farther a transparent pixel's colour is from the real edge colour, the more obvious the bloom.
   For a tight glow, ring the silhouette; for a wide haze, colour a larger transparent field.

**Tuning knobs:**
- **Colour** of the transparent ring = aura hue. **Distance** of coloured pixels from the silhouette =
  how far the bloom reaches at range.
- **Saturation/brightness** of the ghost colour = bloom intensity (mip averaging dilutes it, so push it
  brighter than the target).
- Mip LOD (player distance + GUI vs world) controls when it appears — strongest at mid/far range, near-
  invisible in the hotbar. Good for "you notice it from across the room."
- It is **subtle and not frame-controllable** — for a strong, animated, or pulsing aura use a real
  translucent overlay quad / shader / emissive layer instead. Glamour Bleed is the cheap, no-code,
  texture-only option.

**Where it fits Witchery:**
- Cursed / hexed blocks (Blight Heart, cursed huts, corrupt poppets) — an unwholesome coloured seep.
- Spectral / Spirit-World flora and the spectral-stone family — pale wispy auras.
- Enchanted items (Mystic Branch, charged Attuned Stone, infused brews) — a faint magical fringe.
- Mandrake / mindrake and other "alive and watching" plants — the very smear we removed, used on purpose.

**Caveats / interactions:**
- Our crop textures were just **alpha-bled to remove** accidental Glamour Bleed (44 crop stage PNGs,
  2026-06-10). If we ever want it on a specific plant, re-introduce a coloured transparent ring on *that*
  texture only — and **do not run the alpha-bleed pass over it** or it will be "fixed" away. Mark any
  intentional-bleed texture in a comment / this doc so a future cleanup doesn't strip it.

### Intentional-bleed textures (DO NOT alpha-bleed these)

| Texture | Aura RGB | Halo radius | Notes |
|---|---|---|---|
| `block/blight_fibre.png` | `(150, 50, 210)` cold blight-violet | 2.2 px off strands | 2026-06-10, first live use. Cutout vine-style decal; aura painted into the transparent ring around the strands, opaque strands + alpha untouched. Tune via radius (tighter = subtler) / colour. |
- Requires mip-maps enabled (default). Players on Mipmap Levels = 0 won't see it.
- **Does NOT work on particles.** Glamour Bleed is purely a mip artifact, and the particle atlas is
  built with 0 mip levels, so a coloured transparent margin on a particle sprite produces no halo. For an
  eerie *aura on a particle*, either (a) **paint** the soft halo straight into a custom particle sprite's
  alpha gradient (the baked equivalent — works everywhere, no mip dependence), or (b) co-locate a second
  glow particle with the effect. (We tried (b) on the Blight Heart's obsidian-tear "lava drops" — a
  co-located `SCULK_SOUL` wisp per tear — but it didn't read well and was reverted; the painted-sprite
  route (a) is the better bet if revisited.)
