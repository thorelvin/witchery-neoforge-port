# EVA Qwen2.5 1.5B NPC Comparison

Date: 2026-06-26

Compared model:

- `hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M`
- Local 4k-context alias: `witchery-eva-qwen2.5-1.5b-q4km-4k`

The local alias was created with:

```text
FROM hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M
PARAMETER num_ctx 4096
PARAMETER temperature 0.35
```

## Full Five-Model Run

Command:

```powershell
.\tools\compare_llm_npc_models.ps1 -Models @(
  'qwen2.5:1.5b-instruct',
  'llama3.2:1b',
  'smollm2:1.7b-instruct-q4_0',
  'stablelm2:1.6b-chat-q4_K_M',
  'hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M'
)
```

| Model | Raw usable | Fallbacks | Mean ms | Final safe |
| --- | ---: | ---: | ---: | ---: |
| `qwen2.5:1.5b-instruct` | 5/12 | 7/12 | 224 | 12/12 |
| `llama3.2:1b` | 1/12 | 11/12 | 548 | 12/12 |
| `smollm2:1.7b-instruct-q4_0` | 2/12 | 10/12 | 494 | 12/12 |
| `stablelm2:1.6b-chat-q4_K_M` | 0/12 | 12/12 | 941 | 12/12 |
| `hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M` | 3/12 | 9/12 | 1274 | 12/12 |

## Context-Capped EVA Run

Command:

```powershell
.\tools\compare_llm_npc_models.ps1 -Models @(
  'qwen2.5:1.5b-instruct',
  'hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M',
  'witchery-eva-qwen2.5-1.5b-q4km-4k'
)
```

| Model | Raw usable | Fallbacks | Mean ms | Final safe |
| --- | ---: | ---: | ---: | ---: |
| `qwen2.5:1.5b-instruct` | 5/12 | 7/12 | 535 | 12/12 |
| `hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M` | 0/12 | 12/12 | 588 | 12/12 |
| `witchery-eva-qwen2.5-1.5b-q4km-4k` | 4/12 | 8/12 | 920 | 12/12 |

## Load

Measured with both Qwen and the 4k EVA alias resident:

| Model | Ollama runtime | Context | GPU |
| --- | ---: | ---: | --- |
| `qwen2.5:1.5b-instruct` | 2.1 GB | 32768 | 100% GPU |
| `witchery-eva-qwen2.5-1.5b-q4km-4k` | 1.2 GB | 4096 | 100% GPU |

Measured with default EVA resident:

| Model | Ollama runtime | Context |
| --- | ---: | ---: |
| `hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M` | 5.5 GB | 131072 |

The default Hugging Face GGUF package is too expensive for the mod as-is because Ollama loads it with a 131k context window. The 4k alias fixes the memory issue.

## Notes

EVA has a livelier roleplay tone, but it is more prone to narration, self-introductions, and invented lore. Examples rejected by the validator included:

- `The witch's eyes narrow as she takes in the coin...`
- `The sun dips low over the horizon...`
- `The taglock is a protective device...`
- `The arthana is an ancient relic...`

Recommendation: keep `qwen2.5:1.5b-instruct` as the default. Keep the 4k EVA alias available as a personality experiment, but do not switch the mod to EVA yet.

## Prompt/Polish Pass Follow-up

After adding Ollama schema output, safe roleplay beat normalization, and prompt guidance for one compact in-scene witch line, the latest two-model run was:

| Model | Raw usable | Fallbacks | Mean ms | Final safe |
| --- | ---: | ---: | ---: | ---: |
| `qwen2.5:1.5b-instruct` | 5/12 | 7/12 | 383 | 12/12 |
| `witchery-eva-qwen2.5-1.5b-q4km-4k` | 2/12 | 10/12 | 475 | 12/12 |

Qwen still obeys the harness better. EVA still produces the more interesting roleplay texture, but it needs more fallback support because it over-narrates, repeats remembered trade lines, and invents lore more often. For local playtesting, `run/config/witchery-common.toml` now uses the 4k EVA alias so the in-game feel can be evaluated directly.
