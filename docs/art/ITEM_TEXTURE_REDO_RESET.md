# Item Texture Redo Reset

This document resets the item-texture remaster process.

We should treat the current remaster batches as **useful experiments**, not as the final answer. The project learned a lot from them, but the resulting icons do **not** sit together well enough yet. Too many of them drifted bright, soft, or over-detailed for Witchery, and too many lost readability when reduced to `32x32`.

## Why We Need To Restart

The main failures were process failures, not effort failures:

- We relied too much on attractive high-resolution concept art and then tried to compress it downward.
- Some final sprites kept too much of the concept-image lighting and interior detail, which made them feel lighter and cleaner than Witchery should.
- A number of icons read well when inspected closely, but not well enough at actual inventory scale.
- Several families were internally consistent, but they were not consistent enough with **each other**.
- Too many items were treated as "same base + recolor" or "concept sheet + reduction" instead of "clear object first, texture second."

The result is that some icons are individually decent, but the **set language** is weak.

## Benchmark

Current benchmark for quality and process:

- Source: `src/main/resources/assets/witchery/textures/item/debug_death_ward.png`
- Built copy: `build/resources/main/assets/witchery/textures/item/debug_death_ward.png`

Why this works better:

- The silhouette is clear immediately.
- The object stays readable at inventory size.
- The pixel grid holds together instead of feeling like a soft image that was squeezed down.
- The palette is colorful, but the overall value range still feels grounded and occult.
- The center detail supports the silhouette instead of fighting it.

This should be the **minimum bar** for future redo work.

## Core Correction

The old process leaned too hard on:

1. high-resolution concept image
2. downscale
3. cleanup

That is the wrong order for Witchery item icons.

The new process should be:

1. understand the item
2. define the silhouette
3. decide the value hierarchy
4. place only the few details that help recognition
5. keep the final image readable at `32x32` without needing explanation

High-resolution generation is still allowed, but it must become a **reference step**, not a compression source.

## New Rules

### Visual Rules

- The icon must read on a dark inventory background in under one second.
- The silhouette must still be understandable when viewed at `1x`.
- Witchery items should generally skew **darker, moodier, and more material-heavy** than the current experimental batches.
- Midtones should dominate. Highlights should be controlled and purposeful.
- Avoid bright, airy, pastel-feeling surfaces unless the item is explicitly holy, spectral, or magical enough to justify it.
- Every icon needs one dominant read:
  - shape
  - emblem
  - material contrast
  - glow focus

If it needs all four at once, it is probably too busy.

### Pixel Rules

- Final work happens directly at `32x32`.
- Do not rely on shrinking a painterly or semi-realistic image into the final icon.
- Do not keep sub-pixel forms that only look correct before downscaling.
- Avoid anti-aliased blur edges and overly soft gradients.
- Use transparent padding around the item so the icon breathes in the slot.
- Snap edges and interior accents deliberately to the pixel grid.

### Palette Rules

- Build a darker shared Witchery palette family first.
- Reduce the amount of pure bright highlight used across the set.
- Gold should feel old, ritual, and tarnished more often than polished.
- Wood, leather, bronze, bone, dust, and occult stone should carry the pack mood.
- Strong neon accents should be reserved for real magical focal points, not general fill.

## Corrected Workflow

### Stage 1: Identify The Object

Before drawing, answer:

- What is this item physically?
- What is its primary gameplay fantasy?
- What is the one thing the player should recognize first?

Example:

- `debug_death_ward`: purple occult charm with a clear central jewel and surrounding organic frame.
- Not "many pretty details."
- Not "general magic object."

### Stage 2: Reference Extraction

Allowed references:

- original Witchery art
- local decompiled assets
- Enchanted / Resurrected parallels
- generated concept imagery

But references are only for:

- object identity
- material hints
- motif ideas
- mood

They are **not** final composition or final shading to be reduced.

### Stage 3: High-Res Concept

Use high-resolution generation only to answer:

- what the object is
- what materials belong on it
- which motifs are worth keeping

Then stop.

Do **not** downscale the concept into production.

### Stage 4: Monochrome Silhouette Pass

Before color:

- draw the object as a dark-light shape at `32x32`
- verify that it still reads on a dark background
- verify that its center of mass is clear
- remove detail that does not help identification

If the silhouette is weak here, the icon will fail later.

### Stage 5: Controlled Color Pass

Only after the silhouette works:

- assign a small material palette
- add one main highlight family
- add one magical focal color if needed
- keep the interior shapes larger than instinct suggests

The common mistake is making details too fine. Human eyes need bigger signal at slot scale.

### Stage 6: Inventory Check

Every icon must be checked:

- at `1x`
- at `2x`
- on dark background
- next to related items
- next to unrelated items

Questions:

- Can I identify the item quickly?
- Does it feel too light compared to Witchery?
- Does it look like a rescaled illustration instead of native pixel art?
- Does it collapse into noise next to neighboring items?

If any answer is yes, redo it.

## Batch Rules Going Forward

- Batch size should be smaller until the new process proves itself.
- Prefer `4-8` strongly related items.
- Every batch should include a quick side-by-side family sheet.
- Every batch should be compared against the `debug_death_ward` benchmark before being accepted.

For now, each batch should finish with:

1. concept sheet saved
2. original live sprites backed up
3. final `32x32` redraws
4. family contact sheet review
5. benchmark comparison against `debug_death_ward`

## Acceptance Checklist

An item is not done unless all of these are true:

- It is clearly identifiable by human eye at inventory scale.
- It feels like Witchery, not just generic fantasy craft art.
- It is darker and moodier than the current failed bright remaster trend.
- It looks native to a pixel grid.
- It holds up next to neighboring icons.
- It does not depend on the viewer already knowing what it is.

## Practical Restart Plan

### What To Keep

- Keep all backups already made in `docs/art/item-backups/`.
- Keep concept sheets as research material.
- Keep the current remaster plan as a record of what was attempted.

### What To Change

- Treat the current remastered live textures as provisional.
- Redo them with the corrected workflow instead of iterating small polish on top of weak reductions.
- Use `debug_death_ward` as the first style benchmark for value, readability, and pixel integrity.

### What To Avoid

- Do not approve a texture just because it looks attractive zoomed in.
- Do not reuse a high-res concept image as the main structure of the final icon.
- Do not push too many bright highlights across the full pack.
- Do not let family consistency override item recognizability.

## Recommendation

Restart the redo as a **second-pass production track**:

- build a darker shared palette sheet
- choose a few benchmark icons
- redo the most important/high-traffic items first
- compare every new batch against the benchmark set before continuing

If the icon fails by human-eye recognition at slot size, it fails, even if the zoomed-in art looks nice.
