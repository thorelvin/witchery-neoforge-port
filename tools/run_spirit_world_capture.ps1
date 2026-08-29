param(
    [string]$WorldName = "",
    [int]$WaitSeconds = 240,
    [int]$Width = 1920,
    [int]$Height = 1080
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if ([string]::IsNullOrWhiteSpace($WorldName)) {
    $latestSave = Get-ChildItem -Path "run\saves" -Directory -ErrorAction Stop |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $latestSave) {
        throw "No saves were found under run\saves. Create a dev world first or pass -WorldName."
    }
    $WorldName = $latestSave.Name
}

$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$previousWitcheryRunArgs = $env:WITCHERY_RUN_ARGS
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$flags = @(
    "-Dwitchery.spiritWorldCaptureHarness=true",
    "-Dwitchery.spiritWorldCaptureHarness.capture=true",
    "-Dwitchery.spiritWorldCaptureHarness.exitAfterCapture=true",
    "-Dwitchery.spiritWorldCaptureHarness.runId=$runId"
)
foreach ($flag in $flags) {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
        $env:JAVA_TOOL_OPTIONS = $flag
    } elseif ($env:JAVA_TOOL_OPTIONS -notmatch [regex]::Escape($flag)) {
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS $flag"
    }
}

$launchWorldName = $WorldName
$createdWorldAlias = $null
if ($WorldName -match "\s") {
    $saveRoot = Join-Path $repoRoot "run\saves"
    $sourceSave = Join-Path $saveRoot $WorldName
    if (!(Test-Path -LiteralPath $sourceSave)) {
        throw "Save not found: $sourceSave"
    }
    $aliasName = "CodexSpiritWorldCapture"
    $aliasPath = Join-Path $saveRoot $aliasName
    if (Test-Path -LiteralPath $aliasPath) {
        $aliasItem = Get-Item -LiteralPath $aliasPath
        if ($aliasItem.LinkType -ne "Junction") {
            $aliasName = "CodexSpiritWorldCapture_$runId"
            $aliasPath = Join-Path $saveRoot $aliasName
        }
    }
    if (!(Test-Path -LiteralPath $aliasPath)) {
        New-Item -ItemType Junction -Path $aliasPath -Target (Resolve-Path -LiteralPath $sourceSave).Path | Out-Null
        $createdWorldAlias = $aliasPath
    }
    $launchWorldName = $aliasName
}

$env:WITCHERY_RUN_ARGS = "--quickPlaySingleplayer $launchWorldName --width $Width --height $Height"
$launcherLog = Join-Path $repoRoot "run\logs\spirit-world-capture-$runId.log"
$launcherErrLog = Join-Path $repoRoot "run\logs\spirit-world-capture-$runId.err.log"
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
                -Filter "spirit-world-$runId-*.png" -ErrorAction SilentlyContinue)
        if ($captures.Count -ge 4) {
            break
        }
        if ($process.HasExited) {
            break
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    if ($captures.Count -lt 4) {
        if (Test-Path -LiteralPath $launcherLog) { Get-Content -LiteralPath $launcherLog -Tail 100 }
        if (Test-Path -LiteralPath $launcherErrLog) { Get-Content -LiteralPath $launcherErrLog -Tail 100 }
        if (Test-Path -LiteralPath "run\logs\latest.log") {
            Get-Content -LiteralPath "run\logs\latest.log" -Tail 120
        }
        throw "Spirit World harness captured $($captures.Count) of 4 screenshots within $WaitSeconds seconds."
    }

    $destinationRoot = "C:\claude\witchery\captures\spirit-world\port"
    New-Item -ItemType Directory -Force -Path $destinationRoot | Out-Null
    foreach ($capture in $captures) {
        $case = $capture.BaseName.Substring(("spirit-world-$runId-").Length)
        $destination = Join-Path $destinationRoot "port-$case.png"
        Copy-Item -LiteralPath $capture.FullName -Destination $destination -Force
        Write-Host "Spirit World screenshot: $destination"
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    $env:WITCHERY_RUN_ARGS = $previousWitcheryRunArgs
    if ($null -ne $process -and !$process.HasExited) {
        Stop-ProcessTree -ProcessId $process.Id
    }
    if ($null -ne $createdWorldAlias -and (Test-Path -LiteralPath $createdWorldAlias)) {
        [System.IO.Directory]::Delete((Resolve-Path -LiteralPath $createdWorldAlias).Path, $false)
    }
}
