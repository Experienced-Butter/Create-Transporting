# Generates the 16 passenger seats: blockstates, models, item models, recipes and placeholder
# textures, plus the create:seats block tag that makes Create treat them as seats.
#
# Usage:  powershell -File tools/gen_seat_assets.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src/main/resources/assets/create_village_transit'
$data   = Join-Path $root 'src/main/resources/data'
New-Item -ItemType Directory -Force -Path (Join-Path $assets 'blockstates'), (Join-Path $assets 'models/block'),
 (Join-Path $assets 'models/item'), (Join-Path $assets 'textures/block'), (Join-Path $data 'create_village_transit/recipe'),
 (Join-Path $data 'create/tags/block') | Out-Null

$colors = [ordered]@{
    white      = @{ hex = '#F9FFFE'; en = 'White';  zh = '白色' }
    orange     = @{ hex = '#F9801D'; en = 'Orange'; zh = '橙色' }
    magenta    = @{ hex = '#C74EBD'; en = 'Magenta'; zh = '品红色' }
    light_blue = @{ hex = '#3AB3DA'; en = 'Light Blue'; zh = '淡蓝色' }
    yellow     = @{ hex = '#FED83D'; en = 'Yellow'; zh = '黄色' }
    lime       = @{ hex = '#80C71F'; en = 'Lime'; zh = '黄绿色' }
    pink       = @{ hex = '#F38BAA'; en = 'Pink'; zh = '粉红色' }
    gray       = @{ hex = '#474F52'; en = 'Gray'; zh = '灰色' }
    light_gray = @{ hex = '#9D9D97'; en = 'Light Gray'; zh = '淡灰色' }
    cyan       = @{ hex = '#169C9C'; en = 'Cyan'; zh = '青色' }
    purple     = @{ hex = '#8932B8'; en = 'Purple'; zh = '紫色' }
    blue       = @{ hex = '#3C44AA'; en = 'Blue'; zh = '蓝色' }
    brown      = @{ hex = '#835432'; en = 'Brown'; zh = '棕色' }
    green      = @{ hex = '#5E7C16'; en = 'Green'; zh = '绿色' }
    red        = @{ hex = '#B02E26'; en = 'Red'; zh = '红色' }
    black      = @{ hex = '#1D1D21'; en = 'Black'; zh = '黑色' }
}

$emerald = '#1FBF6A'
$emeraldDark = '#0E7A42'
$brass = '#C9A227'

function Write-Json($path, $object) {
    $json = $object | ConvertTo-Json -Depth 8
    Set-Content -Path $path -Value $json -Encoding UTF8
}

function New-SeatTexture($path, $base, $dark, $light) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $hex = $base
            if ($y -eq 0 -or $x -eq 0 -or $x -eq 15 -or $y -eq 15) { $hex = $dark }
            elseif ($y -eq 1) { $hex = $light }
            elseif (($x -eq 7 -or $x -eq 8) -and $y -ge 4 -and $y -le 11) { $hex = $dark }
            elseif ($y -ge 13 -and $y -le 14) { $hex = $emerald }
            elseif ($y -eq 15) { $hex = $emeraldDark }
            elseif (($x -eq 3 -or $x -eq 12) -and ($y -eq 3 -or $y -eq 12)) { $hex = $brass }
            $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($hex))
        }
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Adjust($hex, $delta) {
    $c = [System.Drawing.ColorTranslator]::FromHtml($hex)
    $r = [math]::Max(0, [math]::Min(255, $c.R + $delta))
    $g = [math]::Max(0, [math]::Min(255, $c.G + $delta))
    $b = [math]::Max(0, [math]::Min(255, $c.B + $delta))
    return ('#{0:X2}{1:X2}{2:X2}' -f $r, $g, $b)
}

$tagValues = @()
$langEn = @{}
$langZh = @{}

foreach ($name in $colors.Keys) {
    $info = $colors[$name]
    $id = "passenger_seat_$name"
    $base = $info.hex
    $dark = Adjust $base -40
    $light = Adjust $base 35

    New-SeatTexture (Join-Path $assets "textures/block/$id.png") $base $dark $light

    Write-Json (Join-Path $assets "blockstates/$id.json") @{ variants = @{ '' = @{ model = "create_village_transit:block/$id" } } }

    Write-Json (Join-Path $assets "models/block/$id.json") @{
        textures = @{ particle = "create_village_transit:block/$id"; seat = "create_village_transit:block/$id" }
        elements = @(
            @{ from = @(1, 0, 1); to = @(15, 4, 15); faces = @{
                down = @{ texture = '#seat' }; up = @{ texture = '#seat' }
                north = @{ texture = '#seat' }; south = @{ texture = '#seat' }
                west = @{ texture = '#seat' }; east = @{ texture = '#seat' } } },
            @{ from = @(1, 4, 1); to = @(15, 11, 3); faces = @{
                down = @{ texture = '#seat' }; up = @{ texture = '#seat' }
                north = @{ texture = '#seat' }; south = @{ texture = '#seat' }
                west = @{ texture = '#seat' }; east = @{ texture = '#seat' } } }
        )
    }

    Write-Json (Join-Path $assets "models/item/$id.json") @{ parent = "create_village_transit:block/$id" }

    Write-Json (Join-Path $data "create_village_transit/recipe/$id.json") @{
        type = 'minecraft:crafting_shapeless'
        category = 'misc'
        ingredients = @(
            @{ item = "create:${name}_seat" },
            @{ item = 'minecraft:emerald' }
        )
        result = @{ id = "create_village_transit:$id"; count = 1 }
    }

    $tagValues += "create_village_transit:$id"
    $langEn["block.create_village_transit.$id"] = "$($info.en) Passenger Seat"
    $langZh["block.create_village_transit.$id"] = "旅客座垫（$($info.zh)）"
}

Write-Json (Join-Path $data 'create/tags/block/seats.json') @{ replace = $false; values = $tagValues }

# Merge the seat names into the existing lang files (keeping every other key).
function Merge-Lang($path, $additions) {
    $existing = [ordered]@{}
    if (Test-Path $path) {
        $raw = Get-Content $path -Raw -Encoding UTF8
        if ($raw.Trim().Length -gt 0) {
            $obj = $raw | ConvertFrom-Json
            foreach ($prop in $obj.PSObject.Properties) { $existing[$prop.Name] = $prop.Value }
        }
    }
    foreach ($key in $additions.Keys) { $existing[$key] = $additions[$key] }
    $json = $existing | ConvertTo-Json -Depth 5
    [System.IO.File]::WriteAllText((Resolve-Path $path), $json, (New-Object System.Text.UTF8Encoding($false)))
}

Merge-Lang (Join-Path $assets 'lang/en_us.json') $langEn
Merge-Lang (Join-Path $assets 'lang/zh_cn.json') $langZh

Write-Host "generated $($colors.Count) passenger seats."
