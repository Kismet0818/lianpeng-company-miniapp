param(
    [string]$InputDir = 'C:\claude',
    [string]$OutputCsv = 'C:\Users\Admin\Desktop\lpsx\tmp\junior-schools-extracted.csv'
)

$rows = @{}
$juniorMarker = ([char]0x521D) + ([char]0x4E2D)
# Windows PowerShell may lose non-ASCII wildcard characters; read all HAR files and
# select records by SchoolType1 instead. This also safely handles renamed HAR files.
$files = Get-ChildItem -LiteralPath $InputDir -File -Filter '*.har' -ErrorAction Stop
foreach ($file in $files) {
    $har = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $har.log.entries) {
        $text = $entry.response.content.text
        if ([string]::IsNullOrWhiteSpace($text)) { continue }
        try { $body = $text | ConvertFrom-Json } catch { continue }
        $items = @($body.result)
        foreach ($item in $items) {
            if ($item -is [System.Array]) { $children = $item } else { $children = @($item) }
            foreach ($candidate in $children) {
                $objects = @($candidate)
                if ($candidate.List) { $objects = @($candidate.List) }
                foreach ($school in $objects) {
                    if (-not $school.ID -or -not ([string]$school.SchoolType1).Contains($juniorMarker)) { continue }
                    $key = "{0}|{1}" -f $school.ID, $school.District
                    if (-not $rows.ContainsKey($key)) {
                        $rows[$key] = [ordered]@{ source_id=$school.ID; name=''; short_name=''; district=''; school_type=''; address=''; boarding_info=''; fees=''; description=''; source_files=@{} }
                    }
                    $row = $rows[$key]
                    foreach ($pair in @(
                        @('name','SchoolFullName'), @('short_name','SchoolShortName'), @('district','District'),
                        @('school_type','PublicOrPrivate'), @('address','Address'), @('boarding_info','Accommodation'),
                        @('fees','Fees'), @('description','Introduction')
                    )) {
                        $target = $pair[0]; $source = $pair[1]
                        if ([string]::IsNullOrWhiteSpace([string]$row[$target]) -and -not [string]::IsNullOrWhiteSpace([string]$school.$source)) { $row[$target] = [string]$school.$source }
                    }
                    $row.source_files[$file.Name] = $true
                }
            }
        }
    }
}

$out = foreach ($row in $rows.Values) {
    [pscustomobject]@{
        source_id=$row.source_id; name=$row.name.Trim(); short_name=$row.short_name.Trim(); district=$row.district.Trim(); school_type=$row.school_type.Trim(); address=$row.address.Trim(); boarding_info=$row.boarding_info.Trim(); fees=$row.fees.Trim(); description=$row.description.Trim(); source_files=($row.source_files.Keys -join ';')
    }
}
$out | Sort-Object district,name | Export-Csv -LiteralPath $OutputCsv -NoTypeInformation -Encoding UTF8
Write-Output ("files={0} schools={1} output={2}" -f $files.Count,$out.Count,$OutputCsv)
