$ErrorActionPreference = "Stop"
$toolRoot = Join-Path $PSScriptRoot "original_capture_agent"
$buildRoot = Join-Path $toolRoot "build"
$agentClasses = Join-Path $buildRoot "agent-classes"
$agentJar = Join-Path $buildRoot "original-witchery-capture-agent.jar"
$agentSource = Join-Path $toolRoot "src\OriginalWitcheryCaptureAgent.java"
$manifest = Join-Path $toolRoot "agent-manifest.mf"

New-Item -ItemType Directory -Force -Path $agentClasses | Out-Null
$javac = & where.exe javac.exe |
    Where-Object { Test-Path (Join-Path (Split-Path $_ -Parent) "jar.exe") } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($javac)) {
    throw "A full JDK with javac.exe and jar.exe is required."
}
$jar = Join-Path (Split-Path $javac -Parent) "jar.exe"

& $javac --release 8 -d $agentClasses $agentSource
if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile the original Witchery capture agent."
}
& $jar cfm $agentJar $manifest -C $agentClasses .
if ($LASTEXITCODE -ne 0) {
    throw "Failed to package the original Witchery capture agent."
}

Write-Output $agentJar
