# Dev-only: captures the Minecraft window to PNG files, so screens can be reviewed without
# synthetic input. Requires the game window to be visible (it is brought to the foreground).
#
# Usage: powershell -File tools/capture_window.ps1 -OutDir run/gui-shots -Count 30 -IntervalMs 1500

param(
    [string]$OutDir = 'run/gui-shots',
    [int]$Count = 30,
    [int]$IntervalMs = 1500
)

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Cap {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint attach, uint attachTo, bool on);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT p);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $proc) { throw 'Minecraft window not found' }
$hwnd = $proc.MainWindowHandle

$fg = [Cap]::GetForegroundWindow()
$fgPid = 0
$fgThread = [Cap]::GetWindowThreadProcessId($fg, [ref]$fgPid)
$curThread = [Cap]::GetCurrentThreadId()
[Cap]::AttachThreadInput($fgThread, $curThread, $true) | Out-Null
[Cap]::ShowWindow($hwnd, 9) | Out-Null
[Cap]::BringWindowToTop($hwnd) | Out-Null
[Cap]::SetForegroundWindow($hwnd) | Out-Null
[Cap]::AttachThreadInput($fgThread, $curThread, $false) | Out-Null
Start-Sleep -Milliseconds 600

$client = New-Object Cap+RECT
[Cap]::GetClientRect($hwnd, [ref]$client) | Out-Null
$origin = New-Object Cap+POINT
[Cap]::ClientToScreen($hwnd, [ref]$origin) | Out-Null
$width = $client.Right - $client.Left
$height = $client.Bottom - $client.Top
Write-Host "capturing ${width}x${height} at $($origin.X),$($origin.Y)"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$resolved = (Resolve-Path $OutDir).Path

for ($i = 0; $i -lt $Count; $i++) {
    $bmp = New-Object System.Drawing.Bitmap $width, $height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($origin.X, $origin.Y, 0, 0, (New-Object System.Drawing.Size $width, $height))
    $g.Dispose()
    $file = Join-Path $resolved ("shot-{0:D3}.png" -f $i)
    $bmp.Save($file, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Start-Sleep -Milliseconds $IntervalMs
}
Get-ChildItem $resolved -Filter *.png | Sort-Object Name | Select-Object -Last 3 | ForEach-Object { $_.FullName }
