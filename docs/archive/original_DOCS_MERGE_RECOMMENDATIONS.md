# Docs Merge Recommendations

## Summary

The docs folder has valuable information, but several files overlap and some old status sections conflict with newer progress. Keep `CLAUDE.md` small and use the docs folder as on-demand reference.

## Main problems

1. `CLAUDE.md` is too large for always-on memory.
2. `PORTING_PLAN.md` is both a roadmap, archive, feature spec, and obscure-mechanics reference.
3. `PORTING_GUIDE.md` and `PORTING_NOTES.md` overlap but serve different purposes.
4. `LEGAL_NOTES.md` and older rewrite wording may conflict unless one policy is declared authoritative.
5. `FEATURE_MATRIX.md`, `PORTING_PLAN.md`, and `PORTING_NOTES.md` contain different progress ages.

## Recommended source of truth order

1. Actual source code
2. `docs/bugs.md`
3. `docs/FEATURE_MATRIX.md`
4. newest entries in `docs/PORTING_NOTES.md`
5. `docs/PORTING_GUIDE.md`
6. `docs/PORTING_PLAN.md`
7. `docs/FUTURE_FEATURES.md`

## Recommended file layout

```text
CLAUDE.md

docs/
  status/
    FEATURE_MATRIX.md
    BUGS.md
    NEXT_TASKS.md

  reference/
    PORTING_GUIDE.md
    PORTING_NOTES.md
    OLD_TO_NEW_API_MAP.md
    ORIGINAL_BEHAVIOR_NOTES.md
    OBSCURE_MECHANICS.md

  roadmap/
    PORTING_BACKLOG.md
    FUTURE_FEATURES.md

  legal/
    LEGAL_NOTES.md
    ATTRIBUTION.md
    RELEASE_ASSET_AUDIT.md

  archive/
    PORTING_PLAN_2026-05-15.md
```

## What to merge into CLAUDE.md

Keep only:
- project stance
- current target
- docs map
- source-of-truth order
- token-saving behavior
- build/test command
- core coding rules
- short current progress summary
- short current missing work list

Do not merge:
- full API tables
- full milestone history
- future feature specs
- bug details
- full legal text
- full ritual lists
- obsolete progress snapshots

## What to split out of PORTING_PLAN.md

Move or extract:
- current active backlog -> `docs/status/NEXT_TASKS.md`
- obscure mechanics -> `docs/reference/OBSCURE_MECHANICS.md`
- old implementation phases -> `docs/archive/PORTING_PLAN_2026-05-15.md`
- detailed ritual roster -> `docs/reference/RITUAL_ROSTER.md`
- Creaking milestone -> `docs/roadmap/M30_CREAKING.md`

After extraction, keep `PORTING_PLAN.md` as either:
- a compact active backlog, or
- delete/rename it to the archive path.

## What to do with PORTING_GUIDE.md

Keep it as the fast implementation manual:
- How to add a brew
- How to add a rite
- How to add a poppet
- How to add an entity renderer
- How to add a dimension
- API map

Trim stale current-progress tables from it and point to `FEATURE_MATRIX.md`.

## What to do with PORTING_NOTES.md

Keep it as chronological API/session notes. Do not load it every session. Claude should search it when an API issue appears.

Add a top-level index at the top:

```md
## Index
- Dimensions
- Entity models
- Poppets
- Networking
- Recipes
- Crops
- Rendering
```

## What to do with FEATURE_MATRIX.md

Make it the single status source of truth. Update it after every milestone.

Add:
- "Last verified against source code"
- "Next priority"
- "Blocked by"
- "Bug link"

## What to do with bugs.md

Keep as active sprint board. Rename to `BUGS.md` for consistency.

Add:
- status column: open / fixed / verified
- affected milestone
- test command
- screenshot/video reference if visual

## What to do with LEGAL_NOTES.md

Keep the project owner's fan-port policy here. Add release checklist:
- placeholder assets replaced or explicitly approved
- copied/decompiled-derived files reviewed
- README disclaimer present
- attribution file present
- mod ID/name release decision made

## Suggested next Claude prompt

```text
Read CLAUDE.md, docs/FEATURE_MATRIX.md, docs/bugs.md, and docs/PORTING_GUIDE.md.

Task: reorganize the docs folder without changing code.
Goals:
1. Move active status into docs/status/
2. Move reference/API docs into docs/reference/
3. Move future ideas into docs/roadmap/
4. Move legal notes into docs/legal/
5. Archive old PORTING_PLAN.md instead of deleting it
6. Update CLAUDE.md with the docs map and source-of-truth order

Do not rewrite large docs yet. Only move/rename files and add short index headings where useful.
Run no build; this is docs-only.
Output changed file paths and short reasons.
```
