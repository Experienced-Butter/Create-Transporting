# Dev-only UI smoke test: drives the running dev client with synthetic input to place blocks,
# open a screen and capture screenshots. Requires an op player named "Dev" on the dev server.
#
# Usage: powershell -File tools/ui_smoke.ps1
# Note: SendKeys treats "~" as Enter, so every command uses absolute coordinates.

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName Microsoft.VisualBasic

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int X, int Y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, IntPtr dwExtraInfo);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $proc) { throw 'Minecraft window not found' }
$hwnd = $proc.MainWindowHandle
[Win32]::SetForegroundWindow($hwnd) | Out-Null
Start-Sleep -Milliseconds 800
$rect = New-Object Win32+RECT
[Win32]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$cx = [int](($rect.Left + $rect.Right) / 2)
$cy = [int](($rect.Top + $rect.Bottom) / 2)
Write-Host "window center=$cx,$cy"

function Type-Slow([string]$text) {
    foreach ($ch in $text.ToCharArray()) {
        if ('+^%~(){}[]'.Contains($ch)) {
            [System.Windows.Forms.SendKeys]::SendWait("{$ch}")
        }
        else {
            [System.Windows.Forms.SendKeys]::SendWait([string]$ch)
        }
        Start-Sleep -Milliseconds 18
    }
}

function Send-Chat([string]$text) {
    [System.Windows.Forms.SendKeys]::SendWait('t')
    Start-Sleep -Milliseconds 500
    Type-Slow $text
    Start-Sleep -Milliseconds 250
    [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
    Start-Sleep -Milliseconds 700
}

function Shot() {
    [System.Windows.Forms.SendKeys]::SendWait('{F2}')
    Start-Sleep -Milliseconds 1500
}

# focus the world, not a GUI
[Win32]::SetCursorPos($cx, $cy) | Out-Null
[Win32]::mouse_event(0x0002, 0, 0, 0, [IntPtr]::Zero)
[Win32]::mouse_event(0x0004, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 600

Send-Chat '/gamemode creative'
Send-Chat '/tp 32 70 48'
Send-Chat '/setblock 32 70 51 create_village_transit:village_station'
Send-Chat '/setblock 34 70 51 create_village_transit:passenger_seat_white'
Send-Chat '/setblock 30 70 51 create_village_transit:trade_station'
Send-Chat '/setblock 32 70 54 create_village_transit:platform_lamp'
Start-Sleep -Milliseconds 900
Shot

# right click the station in front to open its screen
[Win32]::SetCursorPos($cx, $cy) | Out-Null
[Win32]::mouse_event(0x0008, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 150
[Win32]::mouse_event(0x0010, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 1800
Shot

$root = Split-Path -Parent $PSScriptRoot
Get-ChildItem (Join-Path $root 'run/screenshots') -Filter *.png | Sort-Object LastWriteTime -Descending |
    Select-Object -First 3 | ForEach-Object { $_.FullName }
