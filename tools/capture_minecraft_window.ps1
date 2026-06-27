param(
    [int]$DelaySeconds = 2,
    [double]$HoldUseSeconds = 0,
    [string]$OutputDirectory = "run\screenshots"
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

$nativeSource = @"
using System;
using System.Runtime.InteropServices;
public static class WitcheryCaptureNative {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);

    public const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    public const uint MOUSEEVENTF_RIGHTUP = 0x0010;

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }
}
"@
Add-Type -TypeDefinition $nativeSource

function Get-MinecraftWindow {
    Get-Process |
        Where-Object {
            $_.MainWindowHandle -ne 0 -and
            ($_.MainWindowTitle -match "Minecraft" -or $_.ProcessName -match "java|minecraft")
        } |
        Sort-Object StartTime -Descending |
        Select-Object -First 1
}

$windowProcess = Get-MinecraftWindow
if ($null -eq $windowProcess) {
    throw "No visible Minecraft/java window was found. Start the dev client first, then run this script."
}

[WitcheryCaptureNative]::SetForegroundWindow($windowProcess.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 500

if ($HoldUseSeconds -gt 0) {
    [WitcheryCaptureNative]::mouse_event([WitcheryCaptureNative]::MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds ([int]($HoldUseSeconds * 1000))
    [WitcheryCaptureNative]::mouse_event([WitcheryCaptureNative]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
}

Start-Sleep -Seconds $DelaySeconds

$rect = New-Object WitcheryCaptureNative+RECT
[WitcheryCaptureNative]::GetWindowRect($windowProcess.MainWindowHandle, [ref]$rect) | Out-Null
$width = [Math]::Max(1, $rect.Right - $rect.Left)
$height = [Math]::Max(1, $rect.Bottom - $rect.Top)

$bitmap = New-Object System.Drawing.Bitmap($width, $height)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)

$outputRoot = if ([System.IO.Path]::IsPathRooted($OutputDirectory)) {
    $OutputDirectory
} else {
    Join-Path (Get-Location) $OutputDirectory
}
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputPath = Join-Path $outputRoot "codex-capture-$stamp.png"
$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$graphics.Dispose()
$bitmap.Dispose()

Write-Output $outputPath
