param([switch]$NoBrowser)

$ErrorActionPreference = 'Stop'
$port = 3000
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$prefix = "http://+:$port/"
$browserUrl = "http://localhost:$port/"
$lanIp = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' -and $_.InterfaceAlias -notmatch 'Loopback|vEthernet|WSL|Bluetooth' } |
    Select-Object -First 1 -ExpandProperty IPAddress)
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($prefix)

try {
    $listener.Start()
} catch {
    Write-Host 'Startup failed. Port 3000 may already be in use.' -ForegroundColor Red
    Write-Host 'Close the previous admin window and try again.' -ForegroundColor Yellow
    Read-Host 'Press Enter to exit'
    exit 1
}

Write-Host 'LPSX Admin is running.' -ForegroundColor Green
Write-Host "Open on this computer: $browserUrl"
if ($lanIp) { Write-Host "Open from LAN devices: http://${lanIp}:$port/" -ForegroundColor Cyan }
Write-Host 'Keep this window open. Close it to stop the admin page.'
if (-not $NoBrowser) { Start-Process $prefix }

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $requestPath = [Uri]::UnescapeDataString($context.Request.Url.AbsolutePath.TrimStart('/'))
        if ([string]::IsNullOrWhiteSpace($requestPath)) { $requestPath = 'admin.html' }
        $candidate = [IO.Path]::GetFullPath((Join-Path $root $requestPath))
        $rootPath = [IO.Path]::GetFullPath($root)
        if (-not $candidate.StartsWith($rootPath, [StringComparison]::OrdinalIgnoreCase)) {
            $context.Response.StatusCode = 403
            $context.Response.Close()
            continue
        }
        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            $context.Response.StatusCode = 404
            $context.Response.Close()
            continue
        }
        $bytes = [IO.File]::ReadAllBytes($candidate)
        $context.Response.ContentType = if ([IO.Path]::GetExtension($candidate) -eq '.html') { 'text/html; charset=utf-8' } else { 'application/octet-stream' }
        $context.Response.ContentLength64 = $bytes.Length
        $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
        $context.Response.Close()
    }
} finally {
    $listener.Stop()
    $listener.Close()
}
