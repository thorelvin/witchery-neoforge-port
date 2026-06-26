# Local LLM NPC Experiment

This branch adds a local NPC dialogue test for Coven Witches. AI dialogue is enabled by default, but the launcher asks before installing Ollama or downloading model files.

## Runtime

Primary test target:

```powershell
ollama create witchery-eva-qwen2.5-1.5b-q4km-4k -f run\witchery-ai\Modelfile.eva-qwen2.5-1.5b-q4km-4k
ollama serve
```

`RUN_MINECRAFT.bat` reads `run/config/witchery-common.toml`. When `[llm].enabled = true` and the configured Ollama model is missing, it prompts `Y/N` before installing Ollama with `winget` or downloading the model. The mod talks to Ollama at `http://127.0.0.1:11434` by default. It does not call remote gameplay APIs.

## Config

`config/witchery-common.toml`:

```toml
[llm]
enabled = true
provider = "ollama"
endpoint = "http://127.0.0.1:11434"
model = "witchery-eva-qwen2.5-1.5b-q4km-4k"
timeoutMillis = 15000
maxConcurrentRequests = 1
maxTokens = 80
cooldownTicks = 100
allowLanEndpoint = false
```

Set `allowLanEndpoint = true` only when the model is running on a trusted LAN sidecar machine.

## Commands

- `/witchery ai status`
- `/witchery ai chat <message>`
- `/witchery ai memory`
- `/witchery ai knowledge <query>`
- `/witchery ai probe [prompt]`
- `/witchery ai reload`
- `/witchery ai forget [nearest|all|<npc-uuid>]`

## Current Scope

Coven Witch trade greetings and nearby player chat replies are wired. Prompt context includes the witch name, player name, current biome, time of day, day count, weather, nearby threat, relationship state, and compact memory. Existing trading, recruitment, quests, dismissal, and combat remain authoritative. The model may produce flavor text and a validated intent, but it cannot grant items, change trades, bypass quest gates, run commands, or control combat logic.

Nearby chat replies trigger when the player is close to a Coven Witch and addresses her by name/`witch`, asks a question, says a greeting, or looks at her while sending a short conversational message.

## Model Comparison

Run the local comparison harness with:

```powershell
.\tools\compare_llm_npc_models.ps1
```

The original four-model set was chosen from local Ollama-friendly small models:

- [`qwen2.5:1.5b-instruct`](https://ollama.com/library/qwen2.5): Qwen2.5 has instruction-tuned models from 0.5B upward.
- [`llama3.2:1b`](https://ollama.com/library/llama3.2:1b): Llama 3.2 1B is a small instruction-tuned text model aimed at local/edge use.
- [`smollm2:1.7b-instruct-q4_0`](https://ollama.com/library/smollm2): SmolLM2 is a compact on-device family with 135M, 360M, and 1.7B sizes.
- [`stablelm2:1.6b-chat-q4_K_M`](https://ollama.com/library/stablelm2:1.6b-chat-q4_K_M): StableLM2 1.6B Chat is an explicit chat/instruct small model, but under a Stability AI non-commercial research license in Ollama.

Latest local result, 10 prompt cases, rerun after the stricter lore/memory filters:

| Model | Raw usable | Fallbacks | Mean ms | Final safe |
| --- | ---: | ---: | ---: | ---: |
| qwen2.5:1.5b-instruct | 5/10 | 5/10 | 394 | 10/10 |
| llama3.2:1b | 0/10 | 10/10 | 484 | 10/10 |
| smollm2:1.7b-instruct-q4_0 | 3/10 | 7/10 | 368 | 10/10 |
| stablelm2:1.6b-chat-q4_K_M | 0/10 | 10/10 | 425 | 10/10 |

Current playtest default: `witchery-eva-qwen2.5-1.5b-q4km-4k`. Qwen remains a useful obedience baseline, but EVA has the stronger witch voice and is now the model the launcher prepares by default.
