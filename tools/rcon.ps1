param(
    [Parameter(Mandatory = $true)][string]$Command,
    [string]$HostName = '127.0.0.1',
    [int]$Port = 25575,
    [string]$Password = 'cvtest'
)

$ErrorActionPreference = 'Stop'
$client = New-Object System.Net.Sockets.TcpClient
$client.Connect($HostName, $Port)
$stream = $client.GetStream()

function Send-Packet([System.IO.Stream]$stream, [int]$id, [int]$type, [string]$body) {
    $bytes = [Text.Encoding]::UTF8.GetBytes($body)
    $length = 4 + 4 + $bytes.Length + 2
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([int]$length)
    $bw.Write([int]$id)
    $bw.Write([int]$type)
    $bw.Write($bytes)
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $out = $ms.ToArray()
    $stream.Write($out, 0, $out.Length)
    $stream.Flush()
}

function Read-Packet([System.IO.Stream]$stream) {
    $header = New-Object byte[] 4
    $read = 0
    while ($read -lt 4) { $read += $stream.Read($header, $read, 4 - $read) }
    $length = [BitConverter]::ToInt32($header, 0)
    $buffer = New-Object byte[] $length
    $read = 0
    while ($read -lt $length) { $read += $stream.Read($buffer, $read, $length - $read) }
    $id = [BitConverter]::ToInt32($buffer, 0)
    $payload = [Text.Encoding]::UTF8.GetString($buffer, 8, $length - 10)
    return @($id, $payload)
}

Send-Packet $stream 1 3 $Password
$login = Read-Packet $stream
if ($login[0] -eq -1) { throw 'RCON authentication failed' }

Send-Packet $stream 2 2 $Command
$response = Read-Packet $stream
Write-Output $response[1]
$client.Close()
