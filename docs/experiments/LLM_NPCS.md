# Local LLM NPC Experiment

This branch adds a local NPC dialogue layer for Witchery entities. AI dialogue is enabled by default, but the launcher asks before installing Ollama or downloading model files.

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
prewarmEnabled = true
provider = "ollama"
endpoint = "http://127.0.0.1:11434"
model = "witchery-eva-qwen2.5-1.5b-q4km-4k"
timeoutMillis = 3000
prewarmTimeoutMillis = 30000
maxConcurrentRequests = 1
maxTokens = 80
keepAliveMinutes = 30
cooldownTicks = 100
cancelHandledNpcChat = true
allowLanEndpoint = false
apiKeyEnv = ""
extraHeaders = ""
responseFormat = "json_object"
```

The branch keeps `enabled = true` and `prewarmEnabled = true` for local
playtesting. Release packs or servers without a managed model sidecar should set
both to `false` so startup never tries to warm a missing model. Set
`cancelHandledNpcChat = false` if explicit/private NPC-addressed chat should
also remain visible as normal multiplayer chat. Ambient look-at routing still
broadcasts player chat even when cancellation is enabled. Set
`allowLanEndpoint = true` only when the model is running on a trusted LAN sidecar machine. For
`provider = "openai_compatible"`, set `apiKeyEnv` to the name of an environment
variable containing a bearer token when the endpoint needs one, for example
`OPENAI_API_KEY`. `extraHeaders` accepts semicolon- or newline-separated
`Header: value` entries for compatible servers that need non-auth metadata.
`responseFormat = "json_schema"` sends the strict NPC response schema to
endpoints/models that support structured outputs; keep `json_object` or use
`none` for looser local-compatible servers.

## Commands

- `/witchery ai status`
- `/witchery ai chat <message>`
- `/witchery ai memory`
- `/witchery ai knowledge <query>`
- `/witchery ai probe [prompt]`
- `/witchery ai reload`
- `/witchery ai forget [nearest|all|<npc-uuid>]`

## Current Scope

The current persona framework covers Coven Witches, Leonard, villagers, hobgoblins, Baba Yaga, demons/flame imps, boss/atmosphere voices, and reflections. Prompt context can include entity name, player name, biome, time of day, day count, weather, nearby threats, relationship state, nearby altar/coven facts, carried gear, trade stock, and compact entity/player memory where relevant.

Existing trading, recruitment, quests, dismissal, combat, item grants, curses, and command execution remain authoritative server code. The model may produce flavor text, help classify route/social/command intent, and extract durable player memory, but it cannot grant items, change trades, bypass quest gates, run commands by itself, or control combat logic.

Small social reactions are deterministic after validation: praise can earn a short blessing, insults can earn a short curse, and an explicit herb request can receive one free herb per witch/player memory. If a fallback has already been used for a repeated nearby question, the dialogue layer may allow more raw model voice through when it is safe enough.

Nearby chat replies trigger when the player is close to a Coven Witch and addresses her by name/`witch`, asks a question, says a greeting, or looks at her while sending a short conversational message.

Hobgoblins also understand guarded chat commands through `NpcCommandKind`: mine, worship/pray, stay, stop, come here, go away, follow, and guard. The obvious command cases are caught cheaply first; ambiguous cases can ask the local model for a tiny yes/no command-intent check. Command execution is still entity-specific and permission-checked.

## Knowledge Cards

Practical witch knowledge is split into compact markdown cards under `src/main/resources/data/witchery/ai/cards/` and packaged as `data/witchery/ai/cards/`. `NpcKnowledgeBase` loads the files listed in `cards/index.txt`, then retrieves only the matching facts for the player's words. This keeps the prompt small while letting the witch teach real recipes, brews, machine use, starter reagents, and rites.

Card lines use this shape:

```text
- [triggers: whiff|whif|whiff of magic] Targeted fact: Whiff of Magic is crafted with ...
```

The cards are guidance, not a hard script. The prompt presents them as targeted facts, the model phrases the answer in character, and deterministic fallback uses them only when the raw answer is unsafe, too vague, or leaks internal prompt/card language. Tests also compare key recipe cards against the local recipe JSON so NPCs do not drift away from the mod's actual content.

## Safety And Authority

- AI dialogue is local by default. The launcher prepares Ollama/EVA only after user approval.
- `allowLanEndpoint = false` blocks non-loopback endpoints unless a trusted LAN sidecar is explicitly configured.
- `WitcheryProtectionService` guards NPC commands and terrain-changing magic.
- Optional reflective adapters check Open Parties and Claims, FTB Chunks, and Common Protection API when those mods are installed.
- Model output is treated as speech or classifier help, never as direct game-state authority.

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
