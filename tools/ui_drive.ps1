# Dev-only UI driver: brings the running dev client to the foreground and types chat commands,
# then captures screenshots. Requires an op player named "Dev" on the dev server.
#
# Usage: powershell -File tools/ui_drive.ps1 -Commands "/tp 0 70 0","/setblock ..."

param(
    [string[]]$Commands = @('/gamemode creative'),
    [switch]$Shot
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Ui {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool fAttach);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int X, int Y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, IntPtr extra);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
if (-not $proc) { throw 'Minecraft window not found' }
$hwnd = $proc.MainWindowHandle

$fg = [Ui]::GetForegroundWindow()
$fgPid = 0
$fgThread = [Ui]::GetWindowThreadProcessId($fg, [ref]$fgPid)
$curThread = [Ui]::GetCurrentThreadId()
[Ui]::AttachThreadInput($fgThread, $curThread, $true) | Out-Null
[Ui]::ShowWindow($hwnd, 9) | Out-Null
[Ui]::BringWindowToTop($hwnd) | Out-Null
[Ui]::SetForegroundWindow($hwnd) | Out-Null
[Ui]::AttachThreadInput($fgThread, $curThread, $false) | Out-Null
Start-Sleep -Milliseconds 900

$now = [Ui]::GetForegroundWindow()
Write-Host "foreground=$now target=$hwnd focused=$($now -eq $hwnd)"
if ($now -ne $hwnd) { throw 'Could not focus the Minecraft window' }

$rect = New-Object Ui+RECT
[Ui]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$cx = [int](($rect.Left + $rect.Right) / 2)
$cy = [int](($rect.Top + $rect.Bottom) / 2)

# Dismiss any open screen, then make sure the world has focus.
[System.Windows.Forms.SendKeys]::SendWait('{ESC}')
Start-Sleep -Milliseconds 400
[Ui]::SetCursorPos($cx, $cy) | Out-Null
[Ui]::mouse_event(0x0002, 0, 0, 0, [IntPtr]::Zero)
[Ui]::mouse_event(0x0004, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 500

function Type-Slow([string]$text) {
    foreach ($ch in $text.ToCharArray()) {
        if ('+^%~(){}[]'.Contains($ch)) { [System.Windows.Forms.SendKeys]::SendWait("{$ch}") }
        else { [System.Windows.Forms.SendKeys]::SendWait([string]$ch) }
        Start-Sleep -Milliseconds 22
    }
}

foreach ($cmd in $Commands) {
    [System.Windows.Forms.SendKeys]::SendWait('t')
    Start-Sleep -Milliseconds 550
    Type-Slow $cmd
    Start-Sleep -Milliseconds 300
    [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
    Start-Sleep -Milliseconds 800
    Write-Host "sent: $cmd"
}

if ($Shot) {
    Start-Sleep -Milliseconds 600
    [System.Windows.Forms.SendKeys]::SendWait('{F2}')
    Start-Sleep -Milliseconds 1500
    $root = Split-Path -Parent $PSScriptRoot
    Get-ChildItem (Join-Path $root 'run/screenshots') -Filter *.png | Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 | ForEach-Object { $_.FullName }
}
