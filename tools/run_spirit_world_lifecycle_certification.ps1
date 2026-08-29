param(
    [string]$WorldName = "",
    [int]$WaitSecondsPerPhase = 240,
    [int]$Width = 1280,
    [int]$Height = 720
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$saveRoot = (Resolve-Path -LiteralPath "run\saves").Path
if ([string]::IsNullOrWhiteSpace($WorldName)) {
    $latestSave = Get-ChildItem -LiteralPath $saveRoot -Directory |
        Where-Object { $_.Name -notlike "CodexSpiritLifecycle_*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $latestSave) {
        throw "No source save was found under $saveRoot. Create a dev world first or pass -WorldName."
    }
    $WorldName = $latestSave.Name
}

$sourceSave = Join-Path $saveRoot $WorldName
if (!(Test-Path -LiteralPath $sourceSave -PathType Container)) {
    throw "Source save not found: $sourceSave"
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$testWorldName = "CodexSpiritLifecycle_$runId"
$testSave = Join-Path $saveRoot $testWorldName
$reportRoot = Join-Path $repoRoot "build\runtime-test\spirit-world-lifecycle-live\$runId"
$summaryPath = Join-Path $reportRoot "summary.json"
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null
Copy-Item -LiteralPath $sourceSave -Destination $testSave -Recurse

$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$previousWitcheryRunArgs = $env:WITCHERY_RUN_ARGS

function Stop-ProcessTree {
    param([int]$ProcessId)
    Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" |
        ForEach-Object { Stop-ProcessTree -ProcessId $_.ProcessId }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Invoke-LifecyclePhase {
    param([ValidateSet("prepare", "resume")][string]$Phase)

    $flags = @(
        "-Dwitchery.spiritWorldLifecycleHarness=true",
        "-Dwitchery.spiritWorldLifecycleHarness.phase=$Phase",
        "-Dwitchery.spiritWorldLifecycleHarness.runId=$runId",
        "-Dwitchery.spiritWorldLifecycleHarness.reportDir=$reportRoot"
    )
    $env:JAVA_TOOL_OPTIONS = (($previousJavaToolOptions, ($flags -join " ")) |
        Where-Object { ![string]::IsNullOrWhiteSpace($_) }) -join " "
    $env:WITCHERY_RUN_ARGS = "--quickPlaySingleplayer $testWorldName --width $Width --height $Height"

    $stdout = Join-Path $reportRoot "$Phase-client.log"
    $stderr = Join-Path $reportRoot "$Phase-client.err.log"
    $report = Join-Path $reportRoot "spirit-world-lifecycle-$runId-$Phase.json"
    $process = Start-Process -FilePath (Join-Path $repoRoot "gradlew.bat") -ArgumentList @("runClient") `
        -WorkingDirectory $repoRoot -WindowStyle Hidden -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr -PassThru

    try {
        $deadline = (Get-Date).AddSeconds($WaitSecondsPerPhase)
        while ((Get-Date) -lt $deadline -and !$process.HasExited) {
            Start-Sleep -Seconds 1
        }
        if (!$process.HasExited) {
            throw "The $Phase client did not shut down cleanly within $WaitSecondsPerPhase seconds."
        }
        $process.WaitForExit()
        $process.Refresh()
        if ($null -ne $process.ExitCode -and $process.ExitCode -ne 0) {
            throw "The $Phase client exited with code $($process.ExitCode)."
        }
        if (!(Test-Path -LiteralPath $report -PathType Leaf)) {
            throw "The $Phase client exited without writing $report."
        }
        $phaseReport = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($phaseReport.status -ne "passed") {
            throw "The $Phase lifecycle phase failed: $($phaseReport.error)"
        }
        return $phaseReport
    } catch {
        if (Test-Path -LiteralPath $stdout) { Get-Content -LiteralPath $stdout -Tail 120 }
        if (Test-Path -LiteralPath $stderr) { Get-Content -LiteralPath $stderr -Tail 120 }
        if (Test-Path -LiteralPath "run\logs\latest.log") {
            Get-Content -LiteralPath "run\logs\latest.log" -Tail 160
        }
        throw
    } finally {
        if ($null -ne $process -and !$process.HasExited) {
            Stop-ProcessTree -ProcessId $process.Id
        }
    }
}

$summary = [ordered]@{
    status = "failed"
    run_id = $runId
    source_world = $WorldName
    temporary_world = $testWorldName
    stages = @()
}

try {
    $prepare = Invoke-LifecyclePhase -Phase "prepare"
    $summary.stages += "clean_shutdown_while_dreaming"
    $resume = Invoke-LifecyclePhase -Phase "resume"
    $summary.stages += "restart_reconnect_and_icy_needle_wake"
    if ($prepare.prepared_corpse_uuid -ne $resume.restarted_corpse_uuid `
            -or $prepare.prepared_escrow_chars -ne $resume.restarted_escrow_chars `
            -or $prepare.prepared_return_x -ne $resume.restarted_return_x `
            -or $prepare.prepared_return_y -ne $resume.restarted_return_y `
            -or $prepare.prepared_return_z -ne $resume.restarted_return_z `
            -or $prepare.prepared_saved_health -ne $resume.restarted_saved_health `
            -or $prepare.prepared_saved_food -ne $resume.restarted_saved_food) {
        throw "The resumed dream did not exactly match the prepared persisted state."
    }
    $summary.stages += "exact_persisted_state_matched"
    $summary.prepare = $prepare
    $summary.resume = $resume
    $summary.status = "passed"
    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $summaryPath -Encoding utf8
    Write-Host "Spirit World live restart certification passed: $summaryPath"
} catch {
    $summary.error = $_.Exception.Message
    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $summaryPath -Encoding utf8
    throw
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    $env:WITCHERY_RUN_ARGS = $previousWitcheryRunArgs
    if (Test-Path -LiteralPath $testSave -PathType Container) {
        $resolvedTestSave = (Resolve-Path -LiteralPath $testSave).Path
        if ([System.IO.Path]::GetDirectoryName($resolvedTestSave) -ne $saveRoot `
                -or [System.IO.Path]::GetFileName($resolvedTestSave) -ne $testWorldName) {
            throw "Refusing to remove unexpected lifecycle save path: $resolvedTestSave"
        }
        Remove-Item -LiteralPath $resolvedTestSave -Recurse -Force
    }
}
