# Witchery Claude Docs Pack

This folder is reorganized for Claude Code token efficiency.

## Use

Place `CLAUDE.md` at the root of the repository. Place the `docs/` folder beside it.

## Structure

```text
CLAUDE.md
docs/
  status/
    FEATURE_MATRIX.md
    BUGS.md
    NEXT_TASKS.md
  reference/
    PORTING_GUIDE.md
    API_MAP.md
    PORTING_NOTES.md
    OBSCURE_MECHANICS.md
  roadmap/
    PORTING_BACKLOG.md
    FUTURE_FEATURES.md
  legal/
    LEGAL_NOTES.md
    ATTRIBUTION.md
  archive/
    original uploaded docs
```

## Source-of-truth order

1. Current source code
2. `docs/status/BUGS.md`
3. `docs/status/FEATURE_MATRIX.md`
4. Newest sections of `docs/reference/PORTING_NOTES.md`
5. `docs/reference/PORTING_GUIDE.md`
6. `docs/roadmap/PORTING_BACKLOG.md`
7. `docs/roadmap/FUTURE_FEATURES.md`

## Main changes from old docs

- Root `CLAUDE.md` is short enough to stay always loaded.
- Active status is separated from historical plans.
- Old `PORTING_PLAN.md` is archived instead of used as current status.
- `FEATURE_MATRIX.md` has been reconciled with newer Spirit World/Nightmare/Spirit notes.
- `BUGS.md` has been rewritten as an active sprint board.
- API mapping moved to `docs/reference/API_MAP.md`.
- Future/deity/soulcraft ideas moved out of the port workflow.
