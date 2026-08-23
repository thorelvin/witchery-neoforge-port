param(
    [string]$WorldName = "",
    [string]$Entity = "witchery:demon",
    [string]$Block = "",
    [float]$Yaw = 0,
    [string]$CaptureCases = "",
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
$caseSpecs = @()
if (![string]::IsNullOrWhiteSpace($Block)) {
    $blockPath = $Block
    if ($blockPath.Contains(':')) { $blockPath = $blockPath.Substring($blockPath.IndexOf(':') + 1) }
    if ($blockPath.Contains('[')) { $blockPath = $blockPath.Substring(0, $blockPath.IndexOf('[')) }
    $caseSpecs = @([pscustomobject]@{ BlockPath = $blockPath })
} elseif ([string]::IsNullOrWhiteSpace($CaptureCases)) {
    $caseSpecs = @([pscustomobject]@{ Entity = $Entity; Yaw = [float]$Yaw })
} else {
    foreach ($value in $CaptureCases.Split(',')) {
        $pieces = $value.Trim().Split('@', 2)
        if ($pieces.Count -ne 2) {
            throw "Invalid capture case '$value'. Expected namespace:path@yaw."
        }
        $caseSpecs += [pscustomobject]@{
            Entity = $pieces[0]
            Yaw = [float]::Parse($pieces[1], [System.Globalization.CultureInfo]::InvariantCulture)
        }
    }
}
$flags = @(
    "-Dwitchery.modelParityHarness=true",
    "-Dwitchery.modelParityHarness.capture=true",
    "-Dwitchery.modelParityHarness.runId=$runId"
)
if (![string]::IsNullOrWhiteSpace($Block)) {
    $flags += "-Dwitchery.modelParityHarness.block=$Block"
} elseif ([string]::IsNullOrWhiteSpace($CaptureCases)) {
    $yawText = ([float]$Yaw).ToString("0.0############", [System.Globalization.CultureInfo]::InvariantCulture)
    $flags += "-Dwitchery.modelParityHarness.entity=$Entity"
    $flags += "-Dwitchery.modelParityHarness.yaw=$yawText"
} else {
    $flags += "-Dwitchery.modelParityHarness.cases=$CaptureCases"
}
if ($StopClientAfterCapture) {
    $flags += "-Dwitchery.modelParityHarness.exitAfterCapture=true"
}
foreach ($flag in $flags) {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
        $env:JAVA_TOOL_OPTIONS = $flag
    } else {
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
    $launchWorldName = "CodexModelParity_$runId"
    $aliasPath = Join-Path $saveRoot $launchWorldName
    New-Item -ItemType Junction -Path $aliasPath -Target (Resolve-Path $sourceSave).Path | Out-Null
    $createdWorldAlias = $aliasPath
}

$env:WITCHERY_RUN_ARGS = "--quickPlaySingleplayer $launchWorldName --width $Width --height $Height"
$gradle = Join-Path $repoRoot "gradlew.bat"
$launcherLog = Join-Path $repoRoot "run\logs\model-parity-launcher-$runId.log"
$launcherErrLog = Join-Path $repoRoot "run\logs\model-parity-launcher-$runId.err.log"
$process = Start-Process -FilePath $gradle -ArgumentList @("runClient") -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $launcherLog -RedirectStandardError $launcherErrLog -PassThru

function Stop-ProcessTree {
    param([int]$ProcessId)
    Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" |
        ForEach-Object { Stop-ProcessTree -ProcessId $_.ProcessId }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Wait-CaptureStable {
    param([Parameter(Mandatory = $true)][string]$Path)

    $deadline = (Get-Date).AddSeconds(10)
    $lastLength = -1L
    $stableSamples = 0
    while ((Get-Date) -lt $deadline -and $stableSamples -lt 3) {
        $length = (Get-Item -LiteralPath $Path -ErrorAction Stop).Length
        if ($length -gt 0 -and $length -eq $lastLength) {
            $stableSamples++
        } else {
            $stableSamples = 0
        }
        $lastLength = $length
        Start-Sleep -Milliseconds 250
    }
    if ($stableSamples -lt 3) {
        throw "Screenshot did not finish writing within 10 seconds: $Path"
    }
}

try {
    $screenshotRoot = Join-Path $repoRoot "run\screenshots"
    $expectedCaptures = if (![string]::IsNullOrWhiteSpace($Block)) {
        [pscustomobject]@{
            BlockPath = $caseSpecs[0].BlockPath
            Name = "model-parity-$runId-block-$($caseSpecs[0].BlockPath).png"
        }
    } else { foreach ($case in $caseSpecs) {
        $entityPath = $case.Entity.Substring($case.Entity.IndexOf(':') + 1)
        $yawText = ([float]$case.Yaw).ToString("0.0############", [System.Globalization.CultureInfo]::InvariantCulture)
        $yawLabel = $yawText.Replace('-', 'm').Replace('.', '_')
        [pscustomobject]@{
            EntityPath = $entityPath
            YawLabel = $yawLabel
            Name = "model-parity-$runId-$entityPath-yaw$yawLabel.png"
        }
    }}
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    while ((Get-Date) -lt $deadline -and
           @($expectedCaptures | Where-Object { !(Test-Path (Join-Path $screenshotRoot $_.Name)) }).Count -gt 0) {
        Start-Sleep -Seconds 1
    }
    $missingCaptures = @($expectedCaptures | Where-Object {
        !(Test-Path (Join-Path $screenshotRoot $_.Name))
    })
    if ($missingCaptures.Count -gt 0) {
        if (Test-Path $launcherLog) { Get-Content $launcherLog -Tail 100 }
        if (Test-Path $launcherErrLog) { Get-Content $launcherErrLog -Tail 100 }
        $latestLog = Join-Path $repoRoot "run\logs\latest.log"
        if (Test-Path $latestLog) { Get-Content $latestLog -Tail 80 }
        throw "Model parity harness did not capture $($missingCaptures.Name -join ', ') within $WaitSeconds seconds."
    }

    $destinationRoot = "C:\claude\witchery\captures\legacy-model-parity\port"
    New-Item -ItemType Directory -Force -Path $destinationRoot | Out-Null
    foreach ($capture in $expectedCaptures) {
        $capturePath = Join-Path $screenshotRoot $capture.Name
        Wait-CaptureStable -Path $capturePath
        $destination = if (![string]::IsNullOrWhiteSpace($Block)) {
            Join-Path $destinationRoot ("block-$($capture.BlockPath).png")
        } else {
            Join-Path $destinationRoot ("entity-$($capture.EntityPath)-yaw$($capture.YawLabel).png")
        }
        Copy-Item -LiteralPath $capturePath -Destination $destination -Force
        Write-Host "Port model parity screenshot: $destination"
    }
    Select-String -Path (Join-Path $repoRoot "run\logs\latest.log") `
        -Pattern "Legacy model parity (player|entity)|Legacy model parity harness captured" |
        Select-Object -Last 10 | ForEach-Object { Write-Host $_.Line }
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
