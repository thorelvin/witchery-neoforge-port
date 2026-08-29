param(
    [string]$OriginalRoot = "C:\Users\thore\OneDrive\Dokumenter\Original Witchery 1.7.10",
    [int]$Width = 1920,
    [int]$Height = 1080,
    [string]$DebugLogDirectory = "",
    [string]$CapturePlan = ""
)

$ErrorActionPreference = "Stop"

$launcherHome = Join-Path $OriginalRoot "launcher-home"
$gameDirectory = Join-Path $OriginalRoot "instance"
$javaExe = Join-Path $OriginalRoot "runtime\jre-legacy\windows-x64\jre-legacy\bin\javaw.exe"
$forgeId = "1.7.10-Forge10.13.4.1614-1.7.10"
$forgeJsonPath = Join-Path $launcherHome "versions\$forgeId\$forgeId.json"
$vanillaJsonPath = Join-Path $launcherHome "versions\1.7.10\1.7.10.json"
$clientJar = Join-Path $launcherHome "versions\$forgeId\$forgeId.jar"
$accountsPath = Join-Path $launcherHome "launcher_accounts.json"
$nativeDirectory = Get-ChildItem (Join-Path $launcherHome "bin") -Directory |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

foreach ($required in @($javaExe, $forgeJsonPath, $vanillaJsonPath, $clientJar, $accountsPath)) {
    if (!(Test-Path $required)) {
        throw "Original Witchery launch dependency is missing: $required"
    }
}
if ($null -eq $nativeDirectory) {
    throw "No extracted Minecraft 1.7.10 native directory was found under $launcherHome\bin"
}

$forge = Get-Content $forgeJsonPath -Raw | ConvertFrom-Json
$vanilla = Get-Content $vanillaJsonPath -Raw | ConvertFrom-Json
$accounts = Get-Content $accountsPath -Raw | ConvertFrom-Json
$account = $accounts.accounts.PSObject.Properties[$accounts.activeAccountLocalId].Value
if ($null -eq $account -or $null -eq $account.minecraftProfile) {
    throw "The isolated launcher profile has no active Minecraft account."
}
$accessToken = if ([string]::IsNullOrWhiteSpace($account.accessToken)) { "0" } else { $account.accessToken }

function Get-LibraryKey {
    param([string]$Name)
    $parts = $Name.Split(":")
    return "$($parts[0]):$($parts[1])"
}

function Get-MavenArtifactPath {
    param([object]$Library)

    if ($null -ne $Library.downloads -and $null -ne $Library.downloads.artifact) {
        return $Library.downloads.artifact.path
    }

    $parts = $Library.name.Split(":")
    if ($parts.Count -lt 3) {
        throw "Unsupported library coordinate: $($Library.name)"
    }
    $groupPath = $parts[0].Replace(".", "\")
    $artifact = $parts[1]
    $version = $parts[2]
    $classifier = if ($parts.Count -gt 3) { "-$($parts[3])" } else { "" }
    return "$groupPath\$artifact\$version\$artifact-$version$classifier.jar"
}

# Version inheritance replaces libraries by group/artifact. Keep Forge's order,
# then append only vanilla libraries that Forge did not override.
$forgeKeys = @{}
$libraries = New-Object System.Collections.Generic.List[object]
foreach ($library in $forge.libraries) {
    $forgeKeys[(Get-LibraryKey $library.name)] = $true
    $libraries.Add($library)
}
foreach ($library in $vanilla.libraries) {
    if (!$forgeKeys.ContainsKey((Get-LibraryKey $library.name))) {
        $libraries.Add($library)
    }
}

$classPath = New-Object System.Collections.Generic.List[string]
foreach ($library in $libraries) {
    if ($null -ne $library.natives -and
            ($null -eq $library.downloads -or $null -eq $library.downloads.artifact)) {
        continue
    }
    $relativePath = Get-MavenArtifactPath $library
    $artifactPath = Join-Path (Join-Path $launcherHome "libraries") $relativePath
    if (!(Test-Path $artifactPath)) {
        throw "Minecraft library is missing: $artifactPath"
    }
    $classPath.Add($artifactPath)
}
$classPath.Add($clientJar)

function Quote-Argument {
    param([string]$Value)
    return '"' + $Value.Replace('"', '\"') + '"'
}

$arguments = @(
    "-Xmx2G",
    "-XX:+UseConcMarkSweepGC",
    "-XX:+CMSIncrementalMode",
    "-XX:-UseAdaptiveSizePolicy",
    "-Xmn128M",
    (Quote-Argument "-Djava.library.path=$($nativeDirectory.FullName)"),
    "-Dminecraft.launcher.brand=minecraft-launcher",
    "-Dminecraft.launcher.version=witchery-parity-capture",
    (Quote-Argument "-Dminecraft.client.jar=$clientJar"),
    "-cp",
    (Quote-Argument ($classPath -join ";")),
    $forge.mainClass,
    "--username", (Quote-Argument $account.minecraftProfile.name),
    "--version", $forgeId,
    "--gameDir", (Quote-Argument $gameDirectory),
    "--assetsDir", (Quote-Argument (Join-Path $launcherHome "assets")),
    "--assetIndex", $vanilla.assetIndex.id,
    "--uuid", $account.minecraftProfile.id,
    "--accessToken", $accessToken,
    "--userProperties", (Quote-Argument "{}"),
    "--userType", "msa",
    "--width", $Width,
    "--height", $Height,
    "--tweakClass", "cpw.mods.fml.common.launcher.FMLTweaker"
)

if (![string]::IsNullOrWhiteSpace($CapturePlan)) {
    $resolvedPlan = (Resolve-Path $CapturePlan).Path
    $buildAgent = Join-Path $PSScriptRoot "build_original_capture_agent.ps1"
    $agentJar = (& $buildAgent | Select-Object -Last 1)
    if (!(Test-Path $agentJar)) {
        throw "The original capture agent was not built: $agentJar"
    }
    $agentArgument = Quote-Argument "-javaagent:$agentJar=$resolvedPlan"
    $arguments = @($arguments[0..4]) + @($agentArgument) + @($arguments[5..($arguments.Count - 1)])
}

$argumentLine = $arguments -join " "
if ([string]::IsNullOrWhiteSpace($DebugLogDirectory)) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $javaExe
    $startInfo.Arguments = $argumentLine
    $startInfo.WorkingDirectory = $gameDirectory
    $startInfo.UseShellExecute = $true
    $process = [System.Diagnostics.Process]::Start($startInfo)
} else {
    New-Item -ItemType Directory -Force -Path $DebugLogDirectory | Out-Null
    $consoleJava = Join-Path (Split-Path $javaExe -Parent) "java.exe"
    $stdout = Join-Path $DebugLogDirectory "original-client.stdout.log"
    $stderr = Join-Path $DebugLogDirectory "original-client.stderr.log"
    $process = Start-Process -FilePath $consoleJava -ArgumentList $argumentLine `
        -WorkingDirectory $gameDirectory -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
}

Write-Output "Started original Witchery 1.7.10 client (PID $($process.Id), ${Width}x${Height})."
