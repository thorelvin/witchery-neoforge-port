# LLM NPC Prompting Research

Date: 2026-06-26

## Sources

- [EVA-Qwen2.5-1.5B-v0.0 model card](https://huggingface.co/EVA-UNIT-01/EVA-Qwen2.5-1.5B-v0.0): small Qwen2.5 fine-tune aimed at roleplay, creative writing, and flavorful dialogue.
- [Ollama structured outputs](https://docs.ollama.com/capabilities/structured-outputs): supports JSON/schema-constrained responses.
- [Ollama API docs](https://github.com/ollama/ollama/blob/main/docs/api.md): `format` can request JSON or a JSON schema; `options` controls generation parameters.
- [Qwen2.5 1.5B Instruct on Ollama](https://ollama.com/library/qwen2.5%3A1.5b-instruct): instruction-tuned baseline with good structured-output behavior.
- [Automating Interactive Drama Management with LLMs](https://aclanthology.org/2024.findings-acl.196.pdf): useful framing for balancing player agency with plot/scene guidance.
- [Role-prompting and scene-contract roleplay paper](https://huggingface.co/papers/2509.00482): reinforces that role, relationship, and scene contracts improve roleplay agents.

## Prompting Direction

The model should not be boxed into sterile one-line utility answers. The better shape is:

- A compact role and scene contract: witch identity, player identity, place, time, weather, biome, memory.
- Concrete world facts from Witchery notes when the player asks for knowledge.
- One vivid spoken line, optionally with one small beat such as `*narrows her eyes* "..."`.
- Memory as history, not a script to repeat.
- Schema only around the response envelope, not around style.

This lets EVA keep its roleplay flavor while the mod still protects game-state truth.

## Implemented

- `OllamaLlmProvider` now sends a JSON schema for `speech`, `intent`, and `memory`.
- `NpcPromptBuilder` now asks for a real in-scene witch response, allows one safe performance beat, and nudges concrete Witchery nouns instead of generic fantasy filler.
- `NpcBrainResponse` now canonicalizes common roleplay formats:
  - `*nods thoughtfully* Witches' salt is...` -> `*nods* "Witches' salt is..."`
  - `*leans closer* 'It is 22:30...'` -> `*leans closer* "It is 22:30..."`
  - Third-person quoted narration can still be reduced to spoken dialogue.
- The validator still rejects fake game actions, clipped lines, prompt leaks, generic payment-memory repeats, "not for sale" trade answers, and unsupported mandrake/mana filler.
- Local dev config now points to `witchery-eva-qwen2.5-1.5b-q4km-4k` for playtesting.

## Latest Two-Model Run

Command:

```powershell
.\tools\compare_llm_npc_models.ps1 -Models "qwen2.5:1.5b-instruct","witchery-eva-qwen2.5-1.5b-q4km-4k"
```

| Model | Raw usable | Fallbacks | Mean ms | Final safe |
| --- | ---: | ---: | ---: | ---: |
| `qwen2.5:1.5b-instruct` | 5/12 | 7/12 | 383 | 12/12 |
| `witchery-eva-qwen2.5-1.5b-q4km-4k` | 2/12 | 10/12 | 475 | 12/12 |

Interpretation: Qwen is still better at obeying the harness. EVA has the more interesting witch voice, but it over-narrates, invents lore, and sometimes ignores the JSON wrapper. The current branch chooses EVA for local playtest because the fallback/polish layer catches the failures while we continue tuning for immersion.
