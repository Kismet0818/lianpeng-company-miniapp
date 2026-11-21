param(
    [Parameter(Mandatory=$true)][string]$HarPath,
    [string]$OutputPath = "tmp/shangnaxue-policy-import.sql"
)

$ErrorActionPreference = 'Stop'
$districtPattern = '\u9EC4\u6D66\u533A|\u5F90\u6C47\u533A|\u957F\u5B81\u533A|\u9759\u5B89\u533A|\u666E\u9640\u533A|\u8679\u53E3\u533A|\u6768\u6D66\u533A|\u95F5\u884C\u533A|\u5B9D\u5C71\u533A|\u5609\u5B9A\u533A|\u6D66\u4E1C\u65B0\u533A|\u91D1\u5C71\u533A|\u677E\u6C5F\u533A|\u9752\u6D66\u533A|\u5949\u8D24\u533A|\u5D07\u660E\u533A'

function Sql-Text($value) {
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) { return 'NULL' }
    return "'" + ([string]$value).Replace('\','\\').Replace("'","''") + "'"
}
function Sql-Number($value) {
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) { return 'NULL' }
    return [string]$value
}

$har = Get-Content -LiteralPath $HarPath -Raw -Encoding UTF8 | ConvertFrom-Json
$articles = [ordered]@{}
$thematicId = $null
$thematicName = $null
$reportedTotal = 0
foreach ($entry in $har.log.entries) {
    if ($entry.request.url -notmatch '/cms/thematic/getThematic') { continue }
    $body = $entry.response.content.text
    if ($entry.response.content.encoding -eq 'base64') {
        $body = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($body))
    }
    if ([string]::IsNullOrWhiteSpace($body)) { continue }
    try { $root = $body | ConvertFrom-Json } catch { continue }
    if ($root.code -ne 200 -or $null -eq $root.data.dateList) { continue }
    $thematicId = $root.data.thematic.thematicId
    $thematicName = $root.data.thematic.name
    $reportedTotal = [Math]::Max($reportedTotal, [int]$root.data.dateList.total)
    foreach ($article in @($root.data.dateList.records)) {
        if ($article.articleId -and $article.title) { $articles[[string]$article.articleId] = $article }
    }
}
if ($articles.Count -eq 0) { throw 'No thematic policy articles found in HAR' }

$lines = [Collections.Generic.List[string]]::new()
$lines.Add('START TRANSACTION;')
$lines.Add((Get-Content -LiteralPath 'tools/create_shangnaxue_policy_article.sql' -Raw -Encoding UTF8))
foreach ($article in $articles.Values) {
    $districtMatch = [regex]::Match([string]$article.title, $districtPattern)
    $district = if ($districtMatch.Success) { $districtMatch.Value } else { $null }
    $rawJson = $article | ConvertTo-Json -Depth 20 -Compress
    $values = @(
        (Sql-Number $article.articleId), (Sql-Number $article.relatedArticleId), (Sql-Number $thematicId),
        (Sql-Text $thematicName), (Sql-Text $article.title), (Sql-Text $article.summary), (Sql-Text $district),
        (Sql-Text $article.picUrl), (Sql-Text $article.articleUrl), (Sql-Text $article.wechatUrl),
        (Sql-Number $article.platformId), (Sql-Text $article.pubTime), (Sql-Text $article.updateTime),
        (Sql-Number $article.browseVolume), (Sql-Number $article.thumbsUpNum), (Sql-Number $article.contentType),
        (Sql-Number $article.type), (Sql-Number $article.status), (Sql-Number $article.isTop),
        (Sql-Number $article.isBig), (Sql-Number $article.isDeleted), (Sql-Text $rawJson)
    ) -join ','
    $lines.Add("INSERT INTO shangnaxue_policy_article (source_article_id,related_article_id,thematic_id,thematic_name,title,summary,district,pic_url,article_url,wechat_url,platform_id,pub_time,source_update_time,browse_volume,thumbs_up_num,content_type,article_type,source_status,is_top,is_big,is_deleted,raw_json) VALUES ($values) ON DUPLICATE KEY UPDATE related_article_id=VALUES(related_article_id),thematic_id=VALUES(thematic_id),thematic_name=VALUES(thematic_name),title=VALUES(title),summary=VALUES(summary),district=VALUES(district),pic_url=VALUES(pic_url),article_url=VALUES(article_url),wechat_url=VALUES(wechat_url),platform_id=VALUES(platform_id),pub_time=VALUES(pub_time),source_update_time=VALUES(source_update_time),browse_volume=VALUES(browse_volume),thumbs_up_num=VALUES(thumbs_up_num),content_type=VALUES(content_type),article_type=VALUES(article_type),source_status=VALUES(source_status),is_top=VALUES(is_top),is_big=VALUES(is_big),is_deleted=VALUES(is_deleted),raw_json=VALUES(raw_json);")
}
$lines.Add('COMMIT;')
$lines | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Output "THEMATIC_ID=$thematicId"
Write-Output "THEMATIC_NAME=$thematicName"
Write-Output "REPORTED_TOTAL=$reportedTotal"
Write-Output "CAPTURED_UNIQUE=$($articles.Count)"
Write-Output "MISSING_FROM_REPORTED=$($reportedTotal-$articles.Count)"
Write-Output "OUTPUT=$([IO.Path]::GetFullPath($OutputPath))"
