param(
    [Parameter(Mandatory = $true)]
    [string]$HarPath,
    [string]$OutputPath = "tmp/general-high-schools.csv"
)

$ErrorActionPreference = "Stop"

function First-Value($Object, [string[]]$Names) {
    foreach ($name in $Names) {
        $property = $Object.PSObject.Properties[$name]
        if ($null -ne $property -and $null -ne $property.Value -and "$($property.Value)".Trim()) {
            return "$($property.Value)".Trim()
        }
    }
    return $null
}

function Visit-Json($Node, [System.Collections.Generic.List[object]]$Results) {
    if ($null -eq $Node) { return }
    if ($Node -is [System.Collections.IEnumerable] -and $Node -isnot [string] -and
        $Node -isnot [System.Management.Automation.PSCustomObject]) {
        foreach ($item in $Node) { Visit-Json $item $Results }
        return
    }
    if ($Node -isnot [System.Management.Automation.PSCustomObject]) { return }

    $name = First-Value $Node @('name','schoolName','school_name','highSchoolName','high_school_name','SchoolFullName')
    $district = First-Value $Node @('district','schoolDistrict','school_district','District','area','region')
    if ($name -and $district) {
        $Results.Add([pscustomobject]@{
            name = $name
            source_school_id = First-Value $Node @('ID','id','SchoolID','schoolId','school_id')
            source_school_number = First-Value $Node @('SchoolNumber','schoolNumber','school_number')
            short_name = First-Value $Node @('shortName','short_name','SchoolShortName','abbr')
            background_image_url = First-Value $Node @('backgroundImageUrl','background_image_url','ImageLogo','background','cover','coverUrl','cover_url','image','imageUrl','image_url')
            district = $district
            tuition_min = First-Value $Node @('tuitionMin','tuition_min','minTuition','Fees')
            tuition_max = First-Value $Node @('tuitionMax','tuition_max','maxTuition','tuition','Fees')
            boarding_info = First-Value $Node @('boardingInfo','boarding_info','Accommodation','boarding','accommodation')
            school_type = First-Value $Node @('schoolType','school_type','PublicOrPrivate','SchoolType2Short','SchoolType1','nature','property')
            source_category = First-Value $Node @('SchoolType2Short','SchoolType2','category','level')
            address = First-Value $Node @('address','schoolAddress','school_address','Address')
            description = First-Value $Node @('description','intro','introduction','schoolIntro','school_intro','Introduction')
            source_url = $script:CurrentUrl
        })
    }
    foreach ($property in $Node.PSObject.Properties) { Visit-Json $property.Value $Results }
}

$har = Get-Content -LiteralPath $HarPath -Raw -Encoding UTF8 | ConvertFrom-Json
$results = [System.Collections.Generic.List[object]]::new()
foreach ($entry in $har.log.entries) {
    $script:CurrentUrl = $entry.request.url
    $text = $entry.response.content.text
    if (-not $text) { continue }
    if ($entry.response.content.encoding -eq 'base64') {
        try { $text = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($text)) } catch { continue }
    }
    try { $json = $text | ConvertFrom-Json } catch { continue }
    Visit-Json $json $results
}

$unique = $results | Group-Object name,district | ForEach-Object {
    $group = $_.Group
    $merged = [ordered]@{}
    foreach ($prop in $group[0].PSObject.Properties.Name) {
        $value = $null
        foreach ($row in $group) {
            $candidate = $row.$prop
            if ($null -ne $candidate -and "$candidate".Trim()) { $value = "$candidate"; break }
        }
        $merged[$prop] = $value
    }
    [pscustomobject]$merged
} | Sort-Object district,name
$parent = Split-Path -Parent $OutputPath
if ($parent) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
$unique | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding UTF8
Write-Output "Extracted $($unique.Count) unique schools to $OutputPath"
