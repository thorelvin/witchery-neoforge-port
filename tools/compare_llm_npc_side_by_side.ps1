param(
    [string]$Endpoint = "http://127.0.0.1:11434",
    [string[]]$Models = @(
        "witchery-eva-qwen2.5-1.5b-q4km-4k"
    )
)

$ErrorActionPreference = "Stop"

$oldEnabled = $env:WITCHERY_LLM_SIDE_BY_SIDE
$oldEndpoint = $env:WITCHERY_LLM_ENDPOINT
$oldModels = $env:WITCHERY_LLM_SIDE_BY_SIDE_MODELS
$oldReport = $env:WITCHERY_LLM_SIDE_BY_SIDE_REPORT

try {
    $env:WITCHERY_LLM_SIDE_BY_SIDE = "1"
    $env:WITCHERY_LLM_ENDPOINT = $Endpoint
    $env:WITCHERY_LLM_SIDE_BY_SIDE_MODELS = ($Models -join ",")
    $env:WITCHERY_LLM_SIDE_BY_SIDE_REPORT = Join-Path (Get-Location) "build\reports\witchery-ai\chat-side-by-side.md"
    & .\gradlew.bat test --rerun-tasks --tests com.emoniph.witchery.ai.NpcOllamaSideBySideComparisonTest --info
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    $env:WITCHERY_LLM_SIDE_BY_SIDE = $oldEnabled
    $env:WITCHERY_LLM_ENDPOINT = $oldEndpoint
    $env:WITCHERY_LLM_SIDE_BY_SIDE_MODELS = $oldModels
    $env:WITCHERY_LLM_SIDE_BY_SIDE_REPORT = $oldReport
}
