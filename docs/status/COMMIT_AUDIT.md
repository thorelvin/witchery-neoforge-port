# Recent Commit Audit

Last curated: 2026-07-01

Active branch at audit time: `llm-npc-test-qwen25-05b`

Purpose: compact commit-level context for the current AI/persona/protection work.
Use `CURRENT.md` for handoff state and `FEATURE_MATRIX.md` for feature truth.

## Latest Checked Commits

| Commit | Summary | Doc impact |
|---|---|---|
| `5df0331b` | Expanded AI systems and protection facade. | README, current status, feature matrix, LLM NPC notes, and Book of Shadows needed alignment. |
| `045389f4` | Hardened AI card repair routing. | Covered by AI persona/card documentation. |
| `3a88245b` | Routed card-grounded NPC repair through LLM. | Covered by LLM NPC card/fallback notes. |
| `10b11b8b` | Expanded AI persona framework. | Covered by persona framework notes. |
| `aa6551b5` | Added villager AI persona and nearest NPC routing. | Covered by persona framework notes. |
| `eaac2387` | Aligned AI cards with audit architecture. | Covered by knowledge-card notes. |
| `1611cb96` | Expanded AI persona ambient behavior. | Covered by persona framework notes. |
| `d03abd6c` | Refactored NPC persona cards and hardened AI dialogue. | Covered by AI card and fallback notes. |
| `1a1f15f7` | Hardened Leonard AI dialogue. | Covered by Leonard/persona notes. |
| `2c6fdcc7` | Added Leonard AI persona. | Covered by persona framework notes. |
| `e5010a91` | Hardened coven witch lore fallbacks. | Covered by Coven Witch/knowledge-card notes. |
| `d4c8ddb0` | Hardened NPC AI routing and memory. | Covered by memory/router notes. |

## Verification Snapshot

- The working tree was clean before this documentation pass.
- `./gradlew.bat test` was green after the protection facade implementation before the docs update.
- This audit is documentation-only; rerun code tests after any follow-up code change.
