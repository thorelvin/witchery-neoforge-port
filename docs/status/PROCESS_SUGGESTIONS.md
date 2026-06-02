# Process Suggestions (Claude-logged, awaiting owner decision)

These are workflow/quality suggestions surfaced during autonomous port work. They are
**logged, not acted on** unless the project owner approves. Keep this list short; remove
items once decided.

## 2026-06-02

1. ~~**`NEXT_TASKS.md` has become an append-only changelog (791 lines).**~~
   **DONE 2026-06-02 (owner-approved).** The ~400 lines of dated "local delta" entries were
   rotated into `docs/archive/CHANGELOG.md`; `NEXT_TASKS.md` is back to a header + the
   genuine upcoming-work sections (now ~399 lines). New convention: add a session delta
   briefly at the top of `NEXT_TASKS.md`, then sweep older ones into the changelog.

2. **1.7.10 `setHardness(x)` auto-raises resistance to `x*5`.**
   Several ported blocks use single-arg `.strength(hardness)` (resistance == hardness),
   which silently lowers blast resistance vs source for any block that only called
   `func_149711_c` in 1.7.10. Not worth a sweep on its own, but worth matching
   (`.strength(h, h*5)`) opportunistically when touching a block for other parity reasons.
   Documented in `docs/reference/PORTING_NOTES.md`.
