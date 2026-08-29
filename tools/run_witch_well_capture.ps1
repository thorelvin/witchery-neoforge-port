param(
    [int]$WaitSeconds = 300,
    [int]$Width = 1920,
    [int]$Height = 1080
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$sourceWorld = Join-Path $repoRoot "build\runtime-test\witch-well-certification\runtime-world"
if (!(Test-Path -LiteralPath $sourceWorld)) {
    throw "Certified Witch Well world not found: $sourceWorld"
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$worldName = "CodexWitchWellCapture_$runId"
$saveRoot = Join-Path $repoRoot "run\saves"
$worldAlias = Join-Path $saveRoot $worldName
New-Item -ItemType Directory -Force -Path $saveRoot | Out-Null
Copy-Item -LiteralPath $sourceWorld -Destination $worldAlias -Recurse

$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$previousWitcheryRunArgs = $env:WITCHERY_RUN_ARGS
$flags = @(
    "-Dwitchery.witchWellCaptureHarness=true",
    "-Dwitchery.witchWellCaptureHarness.capture=true",
    "-Dwitchery.witchWellCaptureHarness.exitAfterCapture=true",
    "-Dwitchery.witchWellCaptureHarness.runId=$runId",
    "-Dwitchery.witchWellCaptureHarness.worldName=$worldName"
)
foreach ($flag in $flags) {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
        $env:JAVA_TOOL_OPTIONS = $flag
    } else {
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS $flag"
    }
}
$env:WITCHERY_RUN_ARGS = "--width $Width --height $Height"

$launcherLog = Join-Path $repoRoot "run\logs\witch-well-capture-$runId.log"
$launcherErrLog = Join-Path $repoRoot "run\logs\witch-well-capture-$runId.err.log"
$process = Start-Process -FilePath (Join-Path $repoRoot "gradlew.bat") -ArgumentList @("runClient") `
    -WorkingDirectory $repoRoot -WindowStyle Hidden -RedirectStandardOutput $launcherLog `
    -RedirectStandardError $launcherErrLog -PassThru

function Stop-ProcessTree {
    param([int]$ProcessId)
    Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" |
        ForEach-Object { Stop-ProcessTree -ProcessId $_.ProcessId }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

try {
    $caseNames = @(
        "regular-exterior", "regular-center", "small-exterior", "small-center",
        "cave-exterior", "cave-shaft", "cave-lower"
    )
    $screenshotRoot = Join-Path $repoRoot "run\screenshots"
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    do {
        $captures = @($caseNames | ForEach-Object {
            Get-Item -LiteralPath (Join-Path $screenshotRoot "witch-well-$runId-$_.png") `
                -ErrorAction SilentlyContinue
        })
        if ($captures.Count -eq $caseNames.Count -or $process.HasExited) {
            break
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    if ($captures.Count -ne $caseNames.Count) {
        if (Test-Path -LiteralPath $launcherLog) { Get-Content -LiteralPath $launcherLog -Tail 120 }
        if (Test-Path -LiteralPath $launcherErrLog) { Get-Content -LiteralPath $launcherErrLog -Tail 120 }
        if (Test-Path -LiteralPath "run\logs\latest.log") {
            Get-Content -LiteralPath "run\logs\latest.log" -Tail 160
        }
        throw "Witch Well harness captured $($captures.Count) of $($caseNames.Count) views."
    }

    $destinationRoot = Join-Path $repoRoot `
        "build\runtime-test\witch-well-certification\captures\visual"
    New-Item -ItemType Directory -Force -Path $destinationRoot | Out-Null
    foreach ($caseName in $caseNames) {
        $source = Join-Path $screenshotRoot "witch-well-$runId-$caseName.png"
        $destination = Join-Path $destinationRoot "$caseName.png"
        Copy-Item -LiteralPath $source -Destination $destination -Force
        Write-Host "Witch Well screenshot: $destination"
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    $env:WITCHERY_RUN_ARGS = $previousWitcheryRunArgs
    if ($null -ne $process -and !$process.HasExited) {
        Stop-ProcessTree -ProcessId $process.Id
    }
    if (Test-Path -LiteralPath $worldAlias) {
        $resolvedSaveRoot = [System.IO.Path]::GetFullPath($saveRoot).TrimEnd('\')
        $resolvedWorldAlias = [System.IO.Path]::GetFullPath(
            (Resolve-Path -LiteralPath $worldAlias).Path)
        if ([System.IO.Path]::GetDirectoryName($resolvedWorldAlias) -ne $resolvedSaveRoot `
                -or [System.IO.Path]::GetFileName($resolvedWorldAlias) -ne $worldName) {
            throw "Refusing to remove unexpected capture save: $resolvedWorldAlias"
        }
        Remove-Item -LiteralPath $resolvedWorldAlias -Recurse -Force
    }
}
