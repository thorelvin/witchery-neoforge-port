param(
    [string]$ConfigPath = "run\config\witchery-common.toml",
    [string]$SetupDir = "run\witchery-ai",
    [string]$LogPath = "",
    [switch]$AssumeYes,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

$DefaultEndpoint = "http://127.0.0.1:11434"
$DefaultModel = "witchery-eva-qwen2.5-1.5b-q4km-4k"
$EvaSource = "hf.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF:Q4_K_M"

function Write-SetupLog {
    param([string]$Message)

    $line = "[AI setup] $Message"
    Write-Host $line
    if ($LogPath -and $LogPath.Trim().Length -gt 0) {
        Add-Content -Path $LogPath -Value $line
    }
}

function Read-LlmConfig {
    $config = [ordered]@{
        enabled = $true
        provider = "ollama"
        endpoint = $DefaultEndpoint
        model = $DefaultModel
    }

    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        Write-SetupLog "No config found at $ConfigPath; using built-in AI defaults."
        return [pscustomobject]$config
    }

    $section = ""
    foreach ($rawLine in Get-Content -LiteralPath $ConfigPath) {
        $line = $rawLine.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            continue
        }
        if ($line -match '^\[(.+)\]$') {
            $section = $matches[1].Trim().ToLowerInvariant()
            continue
        }
        if ($section -ne "llm" -or $line -notmatch '^([A-Za-z0-9_.-]+)\s*=\s*(.+)$') {
            continue
        }

        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        if ($value -match '^(.*?)\s+#') {
            $value = $matches[1].Trim()
        }
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ($config.Contains($key)) {
            $config[$key] = $value
        }
    }

    [pscustomobject]$config
}

function ConvertTo-ConfigBool {
    param([object]$Value)

    if ($Value -is [bool]) {
        return $Value
    }
    $text = "$Value".Trim().ToLowerInvariant()
    return $text -eq "true" -or $text -eq "1" -or $text -eq "yes" -or $text -eq "on"
}

function Test-LoopbackEndpoint {
    param([string]$Endpoint)

    try {
        $uri = [Uri]$Endpoint
        $hostName = $uri.Host.ToLowerInvariant()
        return $hostName -eq "localhost" -or $hostName -eq "::1" -or $hostName -eq "0:0:0:0:0:0:0:1" -or $hostName.StartsWith("127.")
    } catch {
        return $false
    }
}

function Join-EndpointPath {
    param(
        [string]$Endpoint,
        [string]$Path
    )

    $base = $Endpoint.TrimEnd("/")
    $suffix = if ($Path.StartsWith("/")) { $Path } else { "/$Path" }
    "$base$suffix"
}

function Find-Ollama {
    $command = Get-Command "ollama.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    $command = Get-Command "ollama" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama.exe"),
        (Join-Path $env:ProgramFiles "Ollama\ollama.exe")
    )
    if (${env:ProgramFiles(x86)}) {
        $candidates += (Join-Path ${env:ProgramFiles(x86)} "Ollama\ollama.exe")
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }
    return $null
}

function Test-OllamaEndpoint {
    param([string]$Endpoint)

    try {
        Invoke-RestMethod -Uri (Join-EndpointPath $Endpoint "/api/tags") -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Start-OllamaIfNeeded {
    param(
        [string]$OllamaPath,
        [string]$Endpoint
    )

    if (Test-OllamaEndpoint $Endpoint) {
        return $true
    }

    if (-not (Test-Path -LiteralPath $SetupDir)) {
        New-Item -ItemType Directory -Path $SetupDir -Force | Out-Null
    }

    Write-SetupLog "Starting Ollama service."
    $stdout = Join-Path $SetupDir "ollama-serve.out.log"
    $stderr = Join-Path $SetupDir "ollama-serve.err.log"
    try {
        Start-Process -FilePath $OllamaPath -ArgumentList @("serve") -WindowStyle Hidden `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr | Out-Null
    } catch {
        Write-SetupLog "Could not start Ollama automatically: $($_.Exception.Message)"
    }

    $deadline = (Get-Date).AddSeconds(20)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 500
        if (Test-OllamaEndpoint $Endpoint) {
            return $true
        }
    }
    return $false
}

function Test-OllamaModel {
    param(
        [string]$Endpoint,
        [string]$Model
    )

    try {
        $tags = Invoke-RestMethod -Uri (Join-EndpointPath $Endpoint "/api/tags") -TimeoutSec 5
        foreach ($entry in $tags.models) {
            if ($entry.name -eq $Model -or $entry.name -eq "$Model`:latest" -or $entry.model -eq $Model -or $entry.model -eq "$Model`:latest") {
                return $true
            }
        }
    } catch {
        return $false
    }
    return $false
}

function Invoke-LoggedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$StepName
    )

    if (-not (Test-Path -LiteralPath $SetupDir)) {
        New-Item -ItemType Directory -Path $SetupDir -Force | Out-Null
    }
    $commandLog = Join-Path $SetupDir "setup-command.log"
    Add-Content -Path $commandLog -Value ""
    Add-Content -Path $commandLog -Value "[$(Get-Date -Format o)] $StepName"
    Add-Content -Path $commandLog -Value "$FilePath $($Arguments -join ' ')"

    Write-SetupLog $StepName
    & $FilePath @Arguments 2>&1 | Tee-Object -FilePath $commandLog -Append
    if ($LASTEXITCODE -ne 0) {
        throw "$StepName failed with exit code $LASTEXITCODE. See $commandLog"
    }
}

function Install-OllamaWithWinget {
    $winget = Get-Command "winget.exe" -ErrorAction SilentlyContinue
    if (-not $winget) {
        throw "Ollama is not installed and winget.exe was not found. Install Ollama from https://ollama.com/download and run the launcher again."
    }

    Invoke-LoggedCommand $winget.Source @(
        "install",
        "--id", "Ollama.Ollama",
        "--exact",
        "--source", "winget",
        "--accept-package-agreements",
        "--accept-source-agreements"
    ) "Installing Ollama with winget."
}

function Write-EvaModelfile {
    if (-not (Test-Path -LiteralPath $SetupDir)) {
        New-Item -ItemType Directory -Path $SetupDir -Force | Out-Null
    }
    $modelfile = Join-Path $SetupDir "Modelfile.eva-qwen2.5-1.5b-q4km-4k"
    @"
FROM $EvaSource
PARAMETER num_ctx 4096
PARAMETER temperature 0.35
"@ | Set-Content -LiteralPath $modelfile -Encoding ASCII
    return $modelfile
}

function Install-ConfiguredModel {
    param(
        [string]$OllamaPath,
        [string]$Model
    )

    if ($Model -eq $DefaultModel) {
        $modelfile = Write-EvaModelfile
        Invoke-LoggedCommand $OllamaPath @("create", $Model, "-f", $modelfile) "Creating Witchery EVA model alias."
        return
    }

    Invoke-LoggedCommand $OllamaPath @("pull", $Model) "Pulling configured Ollama model '$Model'."
}

function Ask-InstallConsent {
    param([string]$Model)

    if ($AssumeYes) {
        return $true
    }

    Write-Host ""
    Write-Host "Witchery AI dialogue is enabled, but the local Ollama model is not ready."
    Write-Host "Model: $Model"
    Write-Host "This may install Ollama and download roughly 1-2 GB for the local model."
    Write-Host "No remote gameplay API is used; dialogue runs against Ollama on this PC."
    $answer = Read-Host "Download/install now? (Y/N)"
    return $answer -match '^(y|yes)$'
}

try {
    $llm = Read-LlmConfig
    if (-not (ConvertTo-ConfigBool $llm.enabled)) {
        Write-SetupLog "AI disabled in config; skipping local model setup."
        exit 0
    }

    if ("$($llm.provider)".Trim().ToLowerInvariant() -ne "ollama") {
        Write-SetupLog "AI provider is '$($llm.provider)', not ollama; skipping launcher setup."
        exit 0
    }

    if (-not (Test-LoopbackEndpoint $llm.endpoint)) {
        Write-SetupLog "AI endpoint is not local loopback ($($llm.endpoint)); skipping local install."
        exit 0
    }

    $ollama = Find-Ollama
    if ($ollama -and (Start-OllamaIfNeeded $ollama $llm.endpoint) -and (Test-OllamaModel $llm.endpoint $llm.model)) {
        Write-SetupLog "Local AI model is ready: $($llm.model)"
        exit 0
    }

    if ($CheckOnly) {
        Write-SetupLog "AI setup check only: model is not ready."
        exit 0
    }

    if (-not (Ask-InstallConsent $llm.model)) {
        Write-SetupLog "User skipped AI download/install. Minecraft will still launch; AI dialogue may fall back."
        exit 0
    }

    if (-not $ollama) {
        Install-OllamaWithWinget
        $ollama = Find-Ollama
        if (-not $ollama) {
            throw "Ollama install completed, but ollama.exe was not found. Restart the terminal or install Ollama manually."
        }
    }

    if (-not (Start-OllamaIfNeeded $ollama $llm.endpoint)) {
        throw "Ollama did not answer at $($llm.endpoint)."
    }

    if (-not (Test-OllamaModel $llm.endpoint $llm.model)) {
        Install-ConfiguredModel $ollama $llm.model
    }

    if (Test-OllamaModel $llm.endpoint $llm.model) {
        Write-SetupLog "AI setup complete: $($llm.model)"
    } else {
        Write-SetupLog "Model install command finished, but the model was not visible in Ollama yet."
    }
} catch {
    Write-SetupLog "Setup failed: $($_.Exception.Message)"
    Write-SetupLog "Minecraft will still launch. Disable [llm].enabled or install Ollama/model manually if you do not want this prompt."
}

exit 0
