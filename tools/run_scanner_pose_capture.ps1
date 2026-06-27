param(
    [string]$WorldName = "",
    [int]$WaitSeconds = 75,
    [int]$Width = 1920,
    [int]$Height = 1080,
    [switch]$InventoryCapture,
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
$harnessFlags = @(
    "-Dwitchery.scannerPoseHarness=true",
    "-Dwitchery.scannerPoseHarness.capture=true",
    "-Dwitchery.scannerPoseHarness.poseStudio=true",
    "-Dwitchery.scannerPoseHarness.runId=$runId"
)
if ($StopClientAfterCapture) {
    $harnessFlags += "-Dwitchery.scannerPoseHarness.exitAfterCapture=true"
}
if ($InventoryCapture) {
    $harnessFlags += "-Dwitchery.scannerPoseHarness.inventoryCapture=true"
}
foreach ($flag in $harnessFlags) {
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

    $aliasName = "CodexPoseWorld"
    $aliasPath = Join-Path $saveRoot $aliasName
    if (Test-Path $aliasPath) {
        $aliasItem = Get-Item $aliasPath
        if ($aliasItem.LinkType -ne "Junction") {
            $aliasName = "CodexPoseWorld_$runId"
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
$gradle = Join-Path $repoRoot "gradlew.bat"
$gradleArgs = @("runClient")

Write-Host "Starting dev client with scanner pose harness..."
Write-Host "World: $WorldName"
if ($launchWorldName -ne $WorldName) {
    Write-Host "Launch world alias: $launchWorldName"
}
Write-Host "Harness run id: $runId"
if ($InventoryCapture) {
    Write-Host "Capture mode: inventory preview"
}
$launcherLog = Join-Path $repoRoot "run\logs\scanner-pose-launcher-$runId.log"
$launcherErrLog = Join-Path $repoRoot "run\logs\scanner-pose-launcher-$runId.err.log"
$process = Start-Process -FilePath $gradle -ArgumentList $gradleArgs -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $launcherLog -RedirectStandardError $launcherErrLog -PassThru

function Stop-ProcessTree {
    param([int]$ProcessId)

    Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" |
        ForEach-Object { Stop-ProcessTree -ProcessId $_.ProcessId }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

try {
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    $screenshotRoot = Join-Path $repoRoot "run\screenshots"
    New-Item -ItemType Directory -Force -Path $screenshotRoot | Out-Null
    $captures = @()
    $captureFilter = if ($InventoryCapture) { "scanner-inventory-$runId.png" } else { "scanner-pose-$runId-*.png" }
    $expectedCaptureCount = if ($InventoryCapture) { 1 } else { 6 }
    while ((Get-Date) -lt $deadline) {
        $captures = Get-ChildItem -Path $screenshotRoot -Filter $captureFilter -ErrorAction SilentlyContinue |
            Sort-Object Name
        if ($captures.Count -ge $expectedCaptureCount) {
            break
        }
        Start-Sleep -Seconds 1
    }

    if ($captures.Count -lt $expectedCaptureCount) {
        if (Test-Path $launcherLog) {
            Write-Host "Launcher log:"
            Get-Content -Path $launcherLog -Tail 80
        }
        if (Test-Path $launcherErrLog) {
            Write-Host "Launcher error log:"
            Get-Content -Path $launcherErrLog -Tail 80
        }
        $logPath = Join-Path $repoRoot "run\logs\latest.log"
        if (Test-Path $logPath) {
            Write-Host "Last log lines:"
            Get-Content -Path $logPath -Tail 40
        }
        throw "Scanner pose harness captured $($captures.Count) of $expectedCaptureCount expected screenshots within $WaitSeconds seconds."
    }

    Write-Host "Scanner pose screenshots:"
    $captures | ForEach-Object { Write-Host $_.FullName }
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
