# Repository Agent Guidance

## Local LLM offloading

- Keep cloud Codex responsible for planning, architecture, final review, and verification. Before loading large source batches into cloud context, consider a bounded local pass with `C:\Users\thore\Documents\Codex\2026-08-18\i-2\outputs\ollama-witchery-bench\local_worker.py` and model `ornith:9b`.
- First verify that Ollama and `ornith:9b` are available. On a new workflow, prove the route with one read-only inventory or review before delegating edits.
- Delegate API inventories, file classification, summaries, first-pass reviews, test scaffolding, documentation, and isolated mechanical Java ports. Require exact repository-relative file-and-line evidence for inventories and reviews.
- Do not delegate architecture, networking, persistence, registry lifecycle, client/server decisions, build-system changes, mixins, or broad cross-module refactors.
- Use temperature `0`, thinking off, and a `16384` context by default. Use `65536` only for a genuinely multi-file inventory.
- Have the local command load source files by path and send only the bounded result to cloud context; do not paste large source batches into the delegation prompt manually.
- Treat local output and generated code as candidates. Cloud Codex must inspect the result or diff and run compilation, tests, formatting, and relevant dedicated-server checks before accepting it.
- Escalate to cloud Codex after two failed Ornith attempts, or immediately when the task is ambiguous or architectural. Preserve all existing user changes throughout.
- Report the delegated scope, Ornith prompt/output token counts, verification performed, and estimated cloud tokens avoided.
