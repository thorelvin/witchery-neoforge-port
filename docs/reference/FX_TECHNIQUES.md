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

**Why it happens (the mechanism — verified against `MipmapGenerator`):** Minecraft mip-maps the
block/item atlas. When a texture contains any fully-transparent pixel, mip generation uses
`MipmapGenerator.alphaBlend(... transparent=true)`, which sums each texel's gamma-corrected colour
**only `if (col >> 24 != 0)`** — i.e. **fully transparent (alpha 0) texels are skipped; their RGB is
ignored.** A texel with *any* non-zero alpha contributes its **full colour** (the colour term is not
weighted by alpha — only the guard `alpha != 0` matters; alpha magnitude only feeds the blended alpha).
So a coloured pixel with a *small but non-zero* alpha bleeds its colour into the visible edge texels at
lower LODs → a coloured halo that strengthens with distance. There is also a cutoff: in the transparent
branch a blended mip texel with alpha `< 96` is forced to 0, so the halo only survives where the
low-alpha aura mixes with the opaque (alpha-255) silhouette — it **self-concentrates at the edge**.
We first hit this as a **bug**, the "mandrake purple smear" — but note that one is the **opaque** purple
flower's own colour bleeding (alpha-255 pixels), which is harder to kill: alpha-bleeding can't touch it
(it only recolours transparent pixels), and the only lever is muting the flower's colour (see
[BUGS](../status/BUGS.md) 2026-06-11). Glamour Bleed is the same physics, **inverted and controlled** —
we *want* the foreign colour, and we inject it with low-but-non-zero-alpha pixels.

**How to author it (the recipe):**
1. Start from a normal cutout sprite (cross-model plant, item, decal). Keep `"render_type":
   "minecraft:cutout"` so the base render stays crisp (the halo is mip-only).
2. In the transparent margin around the silhouette, paint the aura colour with a **small non-zero
   alpha** — roughly **8–24** (below the render-time cutout alpha test ≈ `0.1×255 = 25`, so it's still
   discarded up close, but non-zero so the mip blend includes it). **alpha 0 does nothing** — it's
   skipped (this is the mistake we made on the first blight-fibre pass). Keep at least one true alpha-0
   pixel elsewhere so the texture stays in the self-feathering "transparent" blend mode.
3. The aura colour is the same regardless of the alpha value; the **alpha magnitude** controls how
   *opaque/far-reaching* the halo is, and **how much** of the margin you colour controls the bloom's
   reach at far LODs. For a tight glow, ring the silhouette; for a wide haze, colour a larger field.

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
| `block/blight_fibre.png` | `(205, 95, 255)` luminous blight-violet, **alpha 12** | 3.5 px ring off strands | 2026-06-10 (first live use), corrected 2026-06-11. The first two passes painted the aura at **alpha 0** — which `MipmapGenerator` skips, so it did nothing (what showed was the strands' own colour). Now at **alpha 12** (included in the mip blend; <25 cutout cutoff so invisible up close). Strands + the few far alpha-0 px untouched. Tune via alpha (opacity/reach) and ring radius. |
- Requires mip-maps enabled (default). Players on Mipmap Levels = 0 won't see it.
- **Does NOT work on particles.** Glamour Bleed is purely a mip artifact, and the particle atlas is
  built with 0 mip levels, so a coloured transparent margin on a particle sprite produces no halo. For an
  eerie *aura on a particle*, either (a) **paint** the soft halo straight into a custom particle sprite's
  alpha gradient (the baked equivalent — works everywhere, no mip dependence), or (b) co-locate a second
  glow particle with the effect. (We tried (b) on the Blight Heart's obsidian-tear "lava drops" — a
  co-located `SCULK_SOUL` wisp per tear — but it didn't read well and was reverted; the painted-sprite
  route (a) is the better bet if revisited.)
