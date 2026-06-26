param(
    [string]$Endpoint = "http://127.0.0.1:11434",
    [string[]]$Models = @(
        "witchery-eva-qwen2.5-1.5b-q4km-4k"
    )
)

$ErrorActionPreference = "Stop"

$oldCompare = $env:WITCHERY_LLM_COMPARE
$oldEndpoint = $env:WITCHERY_LLM_ENDPOINT
$oldModels = $env:WITCHERY_LLM_COMPARE_MODELS
$oldReport = $env:WITCHERY_LLM_COMPARE_REPORT

try {
    $env:WITCHERY_LLM_COMPARE = "1"
    $env:WITCHERY_LLM_ENDPOINT = $Endpoint
    $env:WITCHERY_LLM_COMPARE_MODELS = ($Models -join ",")
    $env:WITCHERY_LLM_COMPARE_REPORT = Join-Path (Get-Location) "build\reports\witchery-ai\model-comparison.md"
    & .\gradlew.bat test --rerun-tasks --tests com.emoniph.witchery.ai.NpcOllamaModelComparisonTest --info
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    $env:WITCHERY_LLM_COMPARE = $oldCompare
    $env:WITCHERY_LLM_ENDPOINT = $oldEndpoint
    $env:WITCHERY_LLM_COMPARE_MODELS = $oldModels
    $env:WITCHERY_LLM_COMPARE_REPORT = $oldReport
}
