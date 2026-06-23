# Witchery Docs Index

This file is the map for the project docs. Read it first when deciding where to
write new notes.

## Docs Use Contract

- Start with [status/CURRENT.md](status/CURRENT.md) for handoff context and
  [status/NEXT_TASKS.md](status/NEXT_TASKS.md) for the next slice candidate.
- Do not put rolling session diaries in `NEXT_TASKS.md` or
  `FEATURE_MATRIX.md`; move long history to [archive/CHANGELOG.md](archive/CHANGELOG.md).
- Keep [status/NEXT_TASKS.md](status/NEXT_TASKS.md) for future work only:
  current plan, candidate slices, QA prompts, and explicit deferred items.
- Keep [status/FEATURE_MATRIX.md](status/FEATURE_MATRIX.md) for feature-level
  truth: Done/Partial/Deferred state and compact parity caveats.
- Put active in-game retests in [status/QA_QUEUE.md](status/QA_QUEUE.md), bugs
  in [status/BUGS.md](status/BUGS.md), and item/equipment audit notes in
  [status/ITEM_REVIEW.md](status/ITEM_REVIEW.md).
- Player-facing docs are for behavior a player can act on: setup, commands,
  recipes, mechanics, and discovery. Do not copy dev-only history into them.
- Ordinary code slices should update at most three docs. If more seem needed,
  update the source-of-truth doc first and link instead of repeating details.

## Active Sources Of Truth

- [status/CURRENT.md](status/CURRENT.md) - short living handoff: current state,
  next work, and recent high-signal changes.
- [status/QA_QUEUE.md](status/QA_QUEUE.md) - active in-game QA checklist and
  tester-facing verification notes.
- [status/BUGS.md](status/BUGS.md) - active bug board and fixed bug record.
- [status/ITEM_REVIEW.md](status/ITEM_REVIEW.md) - item/equipment audit,
  exact-ID decisions, and unresolved item follow-ups.
- [status/FEATURE_MATRIX.md](status/FEATURE_MATRIX.md) - compact feature-level
  parity status. Use it for feature state changes, not session journaling.
- [status/NEXT_TASKS.md](status/NEXT_TASKS.md) - backlog and slice candidates.
  Keep it focused on future work.

## Player-Facing Docs

- [../README.md](../README.md) - setup, build, high-level state, and navigation.
- [../Book_of_Shadows.html](../Book_of_Shadows.html) - broad player-facing guide.
- In-game Modonomicon/Primer data under `src/main/resources/data/witchery`
  remains the concrete in-game book source.

Update player-facing docs only when behavior, recipes, commands, or discovery
for players changed. Do not copy every dev-note into them.

## Reference Docs

- [reference/REMAKE_GUIDE.md](reference/REMAKE_GUIDE.md) - remake workflow.
- [reference/API_MAP.md](reference/API_MAP.md) - useful source/API mapping.
- [reference/DECOMPILED_SOURCES.md](reference/DECOMPILED_SOURCES.md) - source
  research locations.
- [reference/FX_TECHNIQUES.md](reference/FX_TECHNIQUES.md) - effect/rendering
  implementation notes.
- [reference/OBSCURE_MECHANICS.md](reference/OBSCURE_MECHANICS.md) - source
  mechanics worth preserving.
- [reference/SPIRIT_WORLD_ATMOSPHERE.md](reference/SPIRIT_WORLD_ATMOSPHERE.md)
  - Spirit World atmosphere notes.

Reference docs should be stable. If a note is only useful for "what happened
today", put it in `status/CURRENT.md` or the changelog instead.

## Historical Or Frozen Docs

- [archive/](archive/) - old snapshots and moved history.
- [reference/REMAKE_NOTES.md](reference/REMAKE_NOTES.md) - large chronological
  implementation log; useful for archaeology, not day-to-day updates.
- [archive/status/QA_PARITY_PLAN.md](archive/status/QA_PARITY_PLAN.md) - superseded by
  `status/QA_QUEUE.md` for new QA notes.
- [archive/status/NEXT_FIXES_PLAN.md](archive/status/NEXT_FIXES_PLAN.md) - superseded by
  `status/CURRENT.md` and `status/NEXT_TASKS.md` for new planning.
- [archive/status/RITUAL_QA_PROGRESS.md](archive/status/RITUAL_QA_PROGRESS.md) - superseded by
  `status/QA_QUEUE.md` for new ritual QA.
- [archive/status/STORY_BOOK_LOOT.md](archive/status/STORY_BOOK_LOOT.md) - small historical note;
  fold future story-book QA into `status/QA_QUEUE.md`.
- [archive/status/PARITY_MATRIX_1710.md](archive/status/PARITY_MATRIX_1710.md)
  - frozen 1.7.10 parity snapshot.
- [archive/status/PROCESS_SUGGESTIONS.md](archive/status/PROCESS_SUGGESTIONS.md)
  - old process notes; current routing rules live in this index.
- [wiki_raw/](wiki_raw/) and `wiki_*.md` - scraped/source-reference material.
  Treat as evidence, not as current remake truth.

## Update Rules

When a code slice lands:

1. Add one short note to [status/CURRENT.md](status/CURRENT.md).
2. If player behavior changed, update `README.md`, `Book_of_Shadows.html`, or
   the in-game Primer data.
3. If a tester needs to verify something, add it to
   [status/QA_QUEUE.md](status/QA_QUEUE.md).
4. If a feature status changed, update
   [status/FEATURE_MATRIX.md](status/FEATURE_MATRIX.md).
5. If an item moved from placeholder to practical/parity, update
   [status/ITEM_REVIEW.md](status/ITEM_REVIEW.md).
6. If it fixed a real bug, update [status/BUGS.md](status/BUGS.md).

Avoid updating more than three docs for an ordinary slice. If a change seems to
need five or six docs, update the source-of-truth doc first and link from the
others instead of repeating the whole explanation.
