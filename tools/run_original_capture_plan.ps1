param(
    [Parameter(Mandatory = $true)]
    [string]$Plan,
    [int]$ProcessId = 0
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $PSScriptRoot "original_capture_agent"
$buildRoot = Join-Path $toolRoot "build"
$agentClasses = Join-Path $buildRoot "agent-classes"
$attachClasses = Join-Path $buildRoot "attach-classes"
$agentJar = Join-Path $buildRoot "original-witchery-capture-agent.jar"
$agentSource = Join-Path $toolRoot "src\OriginalWitcheryCaptureAgent.java"
$attachSource = Join-Path $toolRoot "src\OriginalWitcheryAttach.java"
$manifest = Join-Path $toolRoot "agent-manifest.mf"

$resolvedPlan = (Resolve-Path $Plan).Path
New-Item -ItemType Directory -Force -Path $agentClasses, $attachClasses | Out-Null

$javac = & where.exe javac.exe |
    Where-Object { Test-Path (Join-Path (Split-Path $_ -Parent) "jar.exe") } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($javac)) {
    throw "A full JDK with javac.exe and jar.exe is required."
}
$java = Join-Path (Split-Path $javac -Parent) "java.exe"
$jar = Join-Path (Split-Path $javac -Parent) "jar.exe"

& $javac --release 8 -d $agentClasses $agentSource
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile the original Witchery capture agent."
}
& $jar cfm $agentJar $manifest -C $agentClasses .
if ($LASTEXITCODE -ne 0) {
    throw "Failed to package the original Witchery capture agent."
}
& $javac --add-modules jdk.attach -d $attachClasses $attachSource
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile the original Witchery attach helper."
}

if ($ProcessId -eq 0) {
    $client = Get-Process java, javaw -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq "Minecraft 1.7.10" } |
        Sort-Object StartTime -Descending |
        Select-Object -First 1
    if ($null -eq $client) {
        throw "No running original Minecraft 1.7.10 client was found."
    }
    $ProcessId = $client.Id
}

& $java --add-modules jdk.attach -cp $attachClasses OriginalWitcheryAttach `
    $ProcessId $agentJar $resolvedPlan
if ($LASTEXITCODE -ne 0) {
    throw "Could not attach the capture plan to original Witchery PID $ProcessId."
}

Write-Output "Attached capture plan to original Witchery PID $ProcessId."
