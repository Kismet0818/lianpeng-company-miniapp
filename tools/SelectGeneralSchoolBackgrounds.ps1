param(
    [string[]]$HarPaths = @(
        'C:\claude\2026-09-03-095817.har',
        'C:\claude\shengxiadexuexiao.har',
        'C:\claude\tupian.har'
    ),
    [string]$SchoolCsv = 'tmp\all-schools-with-id.csv',
    [string]$OutputCsv = 'tmp\general-school-background-selection.csv',
    [string]$DownloadDir = 'tmp\general-school-background-candidates'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Path $DownloadDir -Force | Out-Null

$byCode = @{}
foreach ($harPath in $HarPaths) {
    if (-not (Test-Path -LiteralPath $harPath)) { continue }
    $raw = Get-Content -LiteralPath $harPath -Raw -Encoding UTF8
    $pattern = 'https://miaguo-1253256735\.file\.myqcloud\.com/web/_School/(\d{4})_(\d+)(?!_s)\.(?:jpeg|jpg|png)'
    foreach ($match in [regex]::Matches($raw, $pattern)) {
        $code = $match.Groups[1].Value
        $number = [int]$match.Groups[2].Value
        if (-not $byCode.ContainsKey($code)) { $byCode[$code] = @{} }
        $byCode[$code][$match.Value] = $number
    }
}

$generalCategory = -join ([char[]](0x4E00, 0x822C, 0x9AD8, 0x4E2D))
$schools = Import-Csv -LiteralPath $SchoolCsv |
    Where-Object { $_.source_category -eq $generalCategory -and $_.source_school_id }
$selected = foreach ($school in $schools) {
    $code = '{0:D4}' -f [int]$school.source_school_id
    $chosen = $null
    if ($byCode.ContainsKey($code)) {
        $candidates = $byCode[$code].GetEnumerator() | Sort-Object Value -Descending
        foreach ($candidate in $candidates) {
            $extension = [IO.Path]::GetExtension(([uri]$candidate.Key).AbsolutePath)
            $file = Join-Path $DownloadDir ($code + '_' + $candidate.Value + $extension)
            try {
                curl.exe --ssl-no-revoke -L -sS -o $file $candidate.Key
                if ((Get-Item -LiteralPath $file).Length -lt 2000) { continue }
                $image = [System.Drawing.Image]::FromFile($file)
                $width = $image.Width
                $height = $image.Height
                $image.Dispose()
                if ($width -ge 300 -and $height -ge 180 -and ($width / $height) -ge 1.25) {
                    $chosen = [pscustomobject]@{ url=$candidate.Key; width=$width; height=$height; number=$candidate.Value }
                    break
                }
            } catch { continue }
        }
    }
    [pscustomobject]@{
        name = $school.name
        district = $school.district
        source_school_id = $school.source_school_id
        image_url = if ($chosen) { $chosen.url } else { $null }
        width = if ($chosen) { $chosen.width } else { $null }
        height = if ($chosen) { $chosen.height } else { $null }
        image_number = if ($chosen) { $chosen.number } else { $null }
    }
}

$selected | Export-Csv -LiteralPath $OutputCsv -NoTypeInformation -Encoding UTF8
Write-Output "schools=$($selected.Count) selected=$(@($selected | Where-Object image_url).Count) empty=$(@($selected | Where-Object { -not $_.image_url }).Count) output=$OutputCsv"
