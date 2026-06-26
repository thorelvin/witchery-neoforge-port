param(
    [string]$Endpoint = "http://127.0.0.1:11434",
    [string]$Model = "witchery-eva-qwen2.5-1.5b-q4km-4k"
)

$ErrorActionPreference = "Stop"

$oldSmoke = $env:WITCHERY_LLM_SMOKE
$oldEndpoint = $env:WITCHERY_LLM_ENDPOINT
$oldModel = $env:WITCHERY_LLM_MODEL

try {
    $env:WITCHERY_LLM_SMOKE = "1"
    $env:WITCHERY_LLM_ENDPOINT = $Endpoint
    $env:WITCHERY_LLM_MODEL = $Model
    & .\gradlew.bat test --rerun-tasks --tests com.emoniph.witchery.ai.NpcOllamaSmokeTest --info
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    $env:WITCHERY_LLM_SMOKE = $oldSmoke
    $env:WITCHERY_LLM_ENDPOINT = $oldEndpoint
    $env:WITCHERY_LLM_MODEL = $oldModel
}
