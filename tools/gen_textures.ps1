# Generates the placeholder 16x16 textures of Create: Village Transit.
#
# The output is deterministic pixel art built from the project palette, so the whole set stays
# visually consistent and can be regenerated after a palette change. Contributors are encouraged
# to replace the PNGs with hand-drawn art - keep the file names and sizes (16x16, RGBA).
#
# Usage:  pwsh -File tools/gen_textures.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$blockDir = Join-Path $root 'src/main/resources/assets/create_village_transit/textures/block'
$itemDir  = Join-Path $root 'src/main/resources/assets/create_village_transit/textures/item'
New-Item -ItemType Directory -Force -Path $blockDir, $itemDir | Out-Null

# --- palette (Create-ish brass / andesite / emerald) -------------------------
$brassDark   = '#8A6B1E'
$brass       = '#C9A227'
$brassLight  = '#F0D264'
$andesiteDk  = '#4F4F4F'
$andesite    = '#6E6E6E'
$andesiteLt  = '#8A8A8A'
$emeraldDark = '#0E7A42'
$emerald     = '#1FBF6A'
$emeraldLt   = '#2FD27A'
$ironDark    = '#343434'
$iron        = '#4A4A4A'
$glow        = '#FFD27F'

function New-Texture {
    param(
        [string]$Path,
        [scriptblock]$Pixel
    )
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $hex = & $Pixel $x $y
            if ($null -eq $hex -or $hex -eq '') {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            }
            else {
                $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($hex))
            }
        }
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $Path"
}

# --- village station: top ----------------------------------------------------
New-Texture (Join-Path $blockDir 'village_station_top.png') {
    param($x, $y)
    $edge = ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15)
    if ($edge) { return $ironDark }
    $frame = ($x -le 1 -or $y -le 1 -or $x -ge 14 -or $y -ge 14)
    if ($frame) { return $brass }
    if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9) {
        if ($x -eq 6 -and $y -eq 6) { return $emeraldLt }
        if ($x -eq 9 -or $y -eq 9) { return $emeraldDark }
        return $emerald
    }
    if ((($x + $y) % 5) -eq 0) { return $andesiteLt }
    return $andesite
}

# --- village station: top (active) ------------------------------------------
New-Texture (Join-Path $blockDir 'village_station_top_active.png') {
    param($x, $y)
    $edge = ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15)
    if ($edge) { return $ironDark }
    $frame = ($x -le 1 -or $y -le 1 -or $x -ge 14 -or $y -ge 14)
    if ($frame) { return $brassLight }
    if ($x -ge 5 -and $x -le 10 -and $y -ge 5 -and $y -le 10) {
        if ($x -ge 7 -and $x -le 8 -and $y -ge 7 -and $y -le 8) { return $glow }
        if ($x -eq 5 -or $y -eq 5) { return $emeraldLt }
        if ($x -eq 10 -or $y -eq 10) { return $emeraldDark }
        return $emerald
    }
    if ((($x + $y) % 5) -eq 0) { return $andesiteLt }
    return $andesite
}

# --- village station: side ---------------------------------------------------
New-Texture (Join-Path $blockDir 'village_station_side.png') {
    param($x, $y)
    if ($y -eq 0 -or $y -eq 15 -or $x -eq 0 -or $x -eq 15) { return $ironDark }
    if ($y -le 2) { return $brass }
    if ($y -eq 3) { return $brassDark }
    if ($y -ge 13) { return $andesiteLt }
    if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9) {
        if ($y -eq 6) { return $emeraldLt }
        if ($y -eq 9) { return $emeraldDark }
        return $emerald
    }
    if ((($x * 3 + $y) % 7) -eq 0) { return $andesiteDk }
    return $andesite
}

# --- village station: side (active) -----------------------------------------
New-Texture (Join-Path $blockDir 'village_station_side_active.png') {
    param($x, $y)
    if ($y -eq 0 -or $y -eq 15 -or $x -eq 0 -or $x -eq 15) { return $ironDark }
    if ($y -le 2) { return $brassLight }
    if ($y -eq 3) { return $brass }
    if ($y -ge 13) { return $andesiteLt }
    if ($x -ge 5 -and $x -le 10 -and $y -ge 5 -and $y -le 10) {
        if ($x -ge 7 -and $x -le 8 -and $y -ge 7 -and $y -le 8) { return $glow }
        if ($y -eq 5) { return $emeraldLt }
        if ($y -eq 10) { return $emeraldDark }
        return $emerald
    }
    if ((($x * 3 + $y) % 7) -eq 0) { return $andesiteDk }
    return $andesite
}

# --- village station: bottom -------------------------------------------------
New-Texture (Join-Path $blockDir 'village_station_bottom.png') {
    param($x, $y)
    if ($y -eq 0 -or $y -eq 15 -or $x -eq 0 -or $x -eq 15) { return $ironDark }
    if (($x -eq 7 -or $x -eq 8) -and ($y -eq 7 -or $y -eq 8)) { return $brassDark }
    if ((($x * 5 + $y * 3) % 11) -eq 0) { return $andesiteLt }
    return $andesiteDk
}

# --- traveller item ----------------------------------------------------------
New-Texture (Join-Path $itemDir 'passenger.png') {
    param($x, $y)
    # suitcase handle
    if ($y -eq 2 -and $x -ge 6 -and $x -le 9) { return $brassDark }
    if ($y -eq 3 -and ($x -eq 5 -or $x -eq 10)) { return $brassDark }
    # suitcase body
    if ($x -ge 3 -and $x -le 12 -and $y -ge 4 -and $y -le 13) {
        if ($x -eq 3 -or $x -eq 12 -or $y -eq 4 -or $y -eq 13) { return $brassDark }
        if ($y -eq 6 -and $x -ge 4 -and $x -le 11) { return $brassLight }
        if ($y -eq 10 -and $x -ge 7 -and $x -le 8) { return $glow }
        return $brass
    }
    # destination tag
    if ($x -ge 11 -and $x -le 14 -and $y -ge 1 -and $y -le 4) {
        if ($x -eq 11 -and $y -eq 4) { return $brassDark }
        return '#4AA3FF'
    }
    return ''
}

# --- platform lamp: base / post / glass / cap --------------------------------
New-Texture (Join-Path $blockDir 'lamp_base.png') {
    param($x, $y)
    if ($y -eq 0 -or $y -eq 15 -or $x -eq 0 -or $x -eq 15) { return $ironDark }
    if (($x -eq 2 -or $x -eq 13) -and ($y -eq 2 -or $y -eq 13)) { return $emeraldLt }
    if ((($x * 7 + $y * 5) % 13) -eq 0) { return $emeraldDark }
    return $emerald
}

New-Texture (Join-Path $blockDir 'lamp_post.png') {
    param($x, $y)
    if ($x -eq 7) { return $iron }
    if ($x -eq 8) { return $ironDark }
    if (($y % 4) -eq 0) { return $ironDark }
    return $ironDark
}

New-Texture (Join-Path $blockDir 'lamp_glass.png') {
    param($x, $y)
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { return $brassDark }
    if ($x -eq 7 -or $x -eq 8) { return $glow }
    if (($x + $y) % 3 -eq 0) { return '#FFE9B0' }
    return '#FFB347'
}

New-Texture (Join-Path $blockDir 'lamp_cap.png') {
    param($x, $y)
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { return $brassDark }
    if (($x + $y) % 4 -eq 0) { return $brassLight }
    return $brass
}

# --- platform lamp item icon -------------------------------------------------
New-Texture (Join-Path $itemDir 'platform_lamp.png') {
    param($x, $y)
    # base
    if ($x -ge 4 -and $x -le 11 -and $y -ge 12 -and $y -le 15) {
        if ($x -eq 4 -or $x -eq 11 -or $y -eq 15) { return $ironDark }
        return $emerald
    }
    # post
    if ($x -ge 7 -and $x -le 8 -and $y -ge 6 -and $y -le 12) { return $ironDark }
    # lantern
    if ($x -ge 5 -and $x -le 10 -and $y -ge 2 -and $y -le 6) {
        if ($x -eq 5 -or $x -eq 10 -or $y -eq 2 -or $y -eq 6) { return $brassDark }
        return $glow
    }
    # handle
    if ($y -eq 1 -and $x -ge 6 -and $x -le 9) { return $brass }
    return ''
}

# --- trade station ------------------------------------------------------------
New-Texture (Join-Path $blockDir 'trade_top.png') {
    param($x, $y)
    if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { return $ironDark }
    if ($x -ge 2 -and $x -le 13 -and $y -ge 2 -and $y -le 13) {
        if ($x -eq 2 -or $y -eq 2 -or $x -eq 13 -or $y -eq 13) { return $brass }
        if ($x -eq 7 -or $x -eq 8) { return $brassDark }
        return $brassLight
    }
    return $andesite
}

New-Texture (Join-Path $blockDir 'trade_side.png') {
    param($x, $y)
    if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { return $ironDark }
    if ($y -le 3) { return $brass }
    if ($y -eq 4) { return $brassDark }
    if ($y -ge 12) { return $andesiteLt }
    if (($y % 4) -eq 0) { return '#6B4B2A' }
    if ($x -eq 7 -or $x -eq 8) { return '#6B4B2A' }
    return '#8A6440'
}

New-Texture (Join-Path $blockDir 'trade_bottom.png') {
    param($x, $y)
    if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { return $ironDark }
    if ((($x * 3 + $y * 7) % 11) -eq 0) { return $andesiteLt }
    return $andesiteDk
}

# --- coins --------------------------------------------------------------------
function New-CoinTexture($path, $base, $dark, $light) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $dx = $x - 7.5
            $dy = $y - 7.5
            $r = [math]::Sqrt($dx * $dx + $dy * $dy)
            if ($r -gt 7.2) {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                continue
            }
            $hex = $base
            if ($r -gt 6.2) { $hex = $dark }
            elseif ($r -gt 5.2) { $hex = $base }
            elseif ($r -gt 1.6) { $hex = $light }
            else { $hex = $dark }
            if ($x -le 5 -and $y -le 5 -and $r -gt 2.4) { $hex = $light }
            $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($hex))
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $path"
}

$coinPalette = [ordered]@{
    copper    = @{ base = '#C77B4A'; dark = '#8A4E2A'; light = '#E7A97A' }
    iron      = @{ base = '#C9CDD1'; dark = '#8A9096'; light = '#EDF1F4' }
    gold      = @{ base = '#F2C641'; dark = '#A8871E'; light = '#FFEB9A' }
    amethyst  = @{ base = '#9C6BE0'; dark = '#6A419F'; light = '#C9A8F2' }
    diamond   = @{ base = '#6FE3D8'; dark = '#3F9E97'; light = '#B4F5EF' }
    emerald   = @{ base = '#35D07F'; dark = '#1B8A51'; light = '#8BF0B8' }
    netherite = @{ base = '#4A3B42'; dark = '#2A2026'; light = '#7A6470' }
}

foreach ($name in $coinPalette.Keys) {
    $p = $coinPalette[$name]
    New-CoinTexture (Join-Path $itemDir "$($name)_coin.png") $p.base $p.dark $p.light
}

Write-Host 'done.'
