param(
    [string]$WorldName = "",
    [int]$WaitSeconds = 120,
    [int]$Width = 1920,
    [int]$Height = 1080,
    [switch]$StopClientAfterCapture
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
    "-Dwitchery.machinePresentationHarness=true",
    "-Dwitchery.machinePresentationHarness.capture=true",
    "-Dwitchery.machinePresentationHarness.runId=$runId"
)
if ($StopClientAfterCapture) {
    $flags += "-Dwitchery.machinePresentationHarness.exitAfterCapture=true"
}
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
    if (!(Test-Path $sourceSave)) {
        throw "Save not found: $sourceSave"
    }
    $aliasName = "CodexMachineGuiWorld"
    $aliasPath = Join-Path $saveRoot $aliasName
    if (Test-Path $aliasPath) {
        $aliasItem = Get-Item $aliasPath
        if ($aliasItem.LinkType -ne "Junction") {
            $aliasName = "CodexMachineGuiWorld_$runId"
            $aliasPath = Join-Path $saveRoot $aliasName
        }
    }
    if (!(Test-Path $aliasPath)) {
        New-Item -ItemType Junction -Path $aliasPath -Target (Resolve-Path $sourceSave).Path | Out-Null
        $createdWorldAlias = $aliasPath
    }
    $launchWorldName = $aliasName
}

$env:WITCHERY_RUN_ARGS = "--quickPlaySingleplayer $launchWorldName --width $Width --height $Height"
$launcherLog = Join-Path $repoRoot "run\logs\machine-gui-launcher-$runId.log"
$launcherErrLog = Join-Path $repoRoot "run\logs\machine-gui-launcher-$runId.err.log"
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
    do {
        $captures = @(Get-ChildItem $screenshotRoot -Filter "machine-gui-$runId-*.png" -ErrorAction SilentlyContinue)
        if ($captures.Count -ge 4) {
            break
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    if ($captures.Count -lt 4) {
        if (Test-Path $launcherLog) { Get-Content $launcherLog -Tail 80 }
        if (Test-Path $launcherErrLog) { Get-Content $launcherErrLog -Tail 80 }
        if (Test-Path "run\logs\latest.log") { Get-Content "run\logs\latest.log" -Tail 80 }
        throw "Machine presentation harness captured $($captures.Count) of 4 screenshots within $WaitSeconds seconds."
    }

    $destinationRoot = "C:\claude\witchery\captures\machine-presentation\port"
    New-Item -ItemType Directory -Force -Path $destinationRoot | Out-Null
    foreach ($capture in $captures) {
        $case = $capture.BaseName.Substring(("machine-gui-$runId-").Length)
        $destination = Join-Path $destinationRoot "gui-$case.png"
        Copy-Item -LiteralPath $capture.FullName -Destination $destination -Force
        Write-Host "Machine GUI screenshot: $destination"
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    $env:WITCHERY_RUN_ARGS = $previousWitcheryRunArgs
    if ($StopClientAfterCapture -and $null -ne $process) {
        Stop-ProcessTree -ProcessId $process.Id
    }
    if ($null -ne $createdWorldAlias -and (Test-Path $createdWorldAlias)) {
        [System.IO.Directory]::Delete((Resolve-Path $createdWorldAlias).Path, $false)
    }
}

Write-Host "Dev client process id: $($process.Id)"
