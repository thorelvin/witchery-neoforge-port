param(
    [string]$SourceWorld = "",
    [int]$WaitSeconds = 240,
    [int]$Width = 1920,
    [int]$Height = 1080
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$saveRoot = Join-Path $repoRoot "run\saves"
if ([string]::IsNullOrWhiteSpace($SourceWorld)) {
    $latestSave = Get-ChildItem -LiteralPath $saveRoot -Directory |
        Where-Object { $_.Name -notlike "CodexSpiritPresentation_*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $latestSave) {
        throw "No source save was found under $saveRoot."
    }
    $SourceWorld = $latestSave.Name
}

$sourceSave = Join-Path $saveRoot $SourceWorld
if (!(Test-Path -LiteralPath $sourceSave -PathType Container)) {
    throw "Source save not found: $sourceSave"
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$disposableName = "CodexSpiritPresentation_$runId"
$disposableSave = Join-Path $saveRoot $disposableName
$resolvedSaveRoot = (Resolve-Path -LiteralPath $saveRoot).Path.TrimEnd('\')
$resolvedDisposableParent = [System.IO.Path]::GetFullPath((Split-Path -Parent $disposableSave)).TrimEnd('\')
if ($resolvedDisposableParent -ne $resolvedSaveRoot) {
    throw "Refusing to create disposable save outside $resolvedSaveRoot"
}
Copy-Item -LiteralPath $sourceSave -Destination $disposableSave -Recurse

$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$previousWitcheryRunArgs = $env:WITCHERY_RUN_ARGS
$flags = @(
    "-Dwitchery.spiritPresentationHarness=true",
    "-Dwitchery.spiritPresentationHarness.capture=true",
    "-Dwitchery.spiritPresentationHarness.exitAfterCapture=true",
    "-Dwitchery.spiritPresentationHarness.runId=$runId"
)
foreach ($flag in $flags) {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
        $env:JAVA_TOOL_OPTIONS = $flag
    } elseif ($env:JAVA_TOOL_OPTIONS -notmatch [regex]::Escape($flag)) {
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS $flag"
    }
}
$env:WITCHERY_RUN_ARGS = "--quickPlaySingleplayer $disposableName --width $Width --height $Height"

$runtimeRoot = Join-Path $repoRoot "build\runtime-test\spirit-presentation"
New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
$launcherLog = Join-Path $runtimeRoot "port-client-$runId.log"
$launcherErrLog = Join-Path $runtimeRoot "port-client-$runId.err.log"
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
    $screenshotRoot = Join-Path $repoRoot "run\screenshots"
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    $captures = @()
    do {
        $captures = @(Get-ChildItem -LiteralPath $screenshotRoot `
                -Filter "spirit-presentation-$runId-*.png" -ErrorAction SilentlyContinue)
        if ($captures.Count -ge 6 -or $process.HasExited) {
            break
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    if ($captures.Count -lt 6) {
        if (Test-Path -LiteralPath $launcherLog) { Get-Content -LiteralPath $launcherLog -Tail 100 }
        if (Test-Path -LiteralPath $launcherErrLog) { Get-Content -LiteralPath $launcherErrLog -Tail 100 }
        if (Test-Path -LiteralPath "run\logs\latest.log") {
            Get-Content -LiteralPath "run\logs\latest.log" -Tail 160
        }
        throw "Spirit presentation harness captured $($captures.Count) of 6 screenshots within $WaitSeconds seconds."
    }

    $destinationRoot = "C:\claude\witchery\captures\spirit-presentation\port"
    New-Item -ItemType Directory -Force -Path $destinationRoot | Out-Null
    foreach ($capture in $captures) {
        $case = $capture.BaseName.Substring(("spirit-presentation-$runId-").Length)
        $destination = Join-Path $destinationRoot "port-$case.png"
        Copy-Item -LiteralPath $capture.FullName -Destination $destination -Force
        Write-Host "Spirit presentation screenshot: $destination"
    }
    $evidenceLog = Join-Path $destinationRoot "port-$runId.log"
    Copy-Item -LiteralPath "run\logs\latest.log" -Destination $evidenceLog -Force
    Write-Host "Spirit presentation evidence log: $evidenceLog"
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    $env:WITCHERY_RUN_ARGS = $previousWitcheryRunArgs
    if ($null -ne $process -and !$process.HasExited) {
        Stop-ProcessTree -ProcessId $process.Id
    }
    if (Test-Path -LiteralPath $disposableSave) {
        $resolvedDisposable = (Resolve-Path -LiteralPath $disposableSave).Path
        if ((Split-Path -Parent $resolvedDisposable).TrimEnd('\') -ne $resolvedSaveRoot -or
                (Split-Path -Leaf $resolvedDisposable) -ne $disposableName) {
            throw "Refusing to remove unexpected disposable path $resolvedDisposable"
        }
        [System.IO.Directory]::Delete($resolvedDisposable, $true)
    }
}
