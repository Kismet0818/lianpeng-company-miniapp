param(
    [Parameter(Mandatory = $true)][string]$Source,
    [Parameter(Mandatory = $true)][string]$Destination
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression

Copy-Item -LiteralPath $Source -Destination $Destination -Force
$archive = [System.IO.Compression.ZipFile]::Open($Destination, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $entry = $archive.GetEntry('word/document.xml')
    $reader = [System.IO.StreamReader]::new($entry.Open(), [System.Text.Encoding]::UTF8)
    $xmlText = $reader.ReadToEnd()
    $reader.Dispose()

    [xml]$xml = $xmlText
    $ns = [System.Xml.XmlNamespaceManager]::new($xml.NameTable)
    $ns.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')

    $replacements = [ordered]@{
        'AI 应用开发实习生｜大模型应用开发实习生' = 'AI 应用开发工程师｜Java 全栈开发工程师'
        '• 编程与服务开发：熟悉 Java、Spring Boot、MyBatis/MyBatis-Plus 与 RESTful API，具备业务接口设计、联调和问题排查经验。' = '• 全栈与服务开发：熟练使用 Java、Spring Boot、MyBatis/MyBatis-Plus、MySQL 与 RESTful API，能够独立完成需求拆解、数据库设计、接口开发、小程序联调及线上问题排查。'
        '• 大模型应用：具有 Claude、GPT、Gemini 等主流大模型 API 调用、Prompt 设计和 JSON 结构化输出调试经验；了解上下文管理及 Tool Calling 机制。' = '• AI 应用工程：掌握 Prompt Engineering、结构化输出、上下文管理与 Tool Calling；理解 RAG 的文档解析、切块、Embedding、混合检索、引用溯源与离线评测链路。'
        '• 数据与检索基础：熟悉 MySQL、SQL、事务和索引，具备 HAR、JSON、Word、PDF 多源数据清洗、批量导入、幂等更新及一致性校验经验；了解 RAG 的文档解析、切块、Embedding、向量检索、上下文增强、生成与评测流程。' = '• 数据工程：熟悉 SQL、事务、索引和数据建模，具备 HAR、JSON、Word、PDF 等多源数据抽取、字段映射、去重合并、幂等导入及一致性校验经验。'
        '• 工程与部署：熟悉 Maven、Git、Linux 基础命令，具有 Docker、阿里云 OSS、微信云托管、接口测试、限流、日志排查及版本回滚实践。' = '• 工程交付：熟悉 Maven、Git、Docker、阿里云 OSS 与微信云托管，具有生产发布、增量迁移、接口限流、日志排障、真机回归及版本回滚经验。'
        '• 主导教育类微信小程序 2.0 的工程落地，贯通 Spring Boot 服务端、MySQL、前后端联调、云端环境和上线流程；完成 Maven 构建、Docker 打包及微信云托管发布，版本 006 成功接管流量并保留版本 004 回退方案。' = '• 独立承担“联朋升学”微信小程序 2.0 的核心研发与上线交付，贯通需求拆解、Spring Boot 服务端、MySQL、微信小程序联调及云端发布；完成 Docker 构建与微信云托管部署，版本 006 接管生产流量并保留 004 稳定版本用于回退。'
        '• 处理 728 条初中、169 条查分高中、140 条普通高中及 152 条综合/国际学校数据记录；开发 HAR、JSON、Word、PDF 抽取与导入工具，完成字段映射、去重合并、规范化、幂等更新和一致性回查。' = '• 建设覆盖 1,299 所学校及 5,279 条招生记录的数据服务，开发 HAR/JSON/Word/PDF 抽取与导入工具；独立设计跨学段检索与 6 校批量对比，以“来源+ID”隔离多表实体、分组查询消除 N+1，并用 MySQL 窗口函数治理重复数据。'
        '• 设计并实现微信登录、手机号授权、用户状态管理与学校访问行为链路；记录来源、次数和停留时长，过滤少于 3 秒的误触并聚合兴趣数据，为后续兴趣分析和可解释推荐准备数据特征。' = '• 设计用户身份与行为数据闭环：完成微信登录、手机号授权、同手机号主身份归一、用户管理及详情页行为采集；按学校来源、访问次数与有效停留时长聚合兴趣特征，支撑客户画像和可解释择校推荐。'
        '• 对登录、搜索、列表、详情及写操作实施差异化限流，补充分页边界、详情枚举和写入参数校验；覆盖 HTTP 429、分页越界、枚举访问和非法写入等 8 类异常场景。' = '• 建立分级接口安全体系，对登录、搜索、列表、详情及写操作实施差异化限流，并补充分页上限、详情枚举、鉴权与参数校验；完成 8 项自动化测试，异常请求返回标准 HTTP 429。'
        '• 迁移政策 PDF、学校图片和附件至阿里云 OSS，建立文件校验、URL 回查及数据库备份与增量迁移流程；完成云端数据核验、上线回归、回滚文档及公安网安备案材料整理。' = '• 负责生产环境交付与数据安全，完成政策 PDF/学校图片 OSS 迁移、本地—云端增量同步、数据库备份、上线回归与故障回退；小程序 2.0 已通过微信审核并正式发布。'
        'AI 应用实践' = 'AI 应用与产品实践'
        '• 调用 Claude、GPT、Gemini 等模型 API 完成参数配置、Prompt 迭代与结构化输出调试，能够约束输出格式，并定位接口、限流和连通性问题。' = '• 独立实现零模型成本的规则型“智能升学顾问”：以服务端状态机逐轮收集学段、区域、体制、预算和寄宿需求，统一检索 5 类学校数据并输出推荐依据，支持快捷选项、自然语言输入及人工咨询留资闭环。'
        '• 持续学习可溯源 RAG 应用链路，理解文档解析、语义切块、Embedding、Top K 检索、上下文增强、引用溯源、无依据拒答和离线评测的作用与边界。' = '• 熟练运用 AI 编程工具完成需求拆解、代码实现、数据清洗与测试设计，并通过编译、接口、数据库及真机测试闭环验收；具备基于现有政策内容库搭建可溯源 RAG 问答的工程能力。'
    }

    $changed = 0
    foreach ($paragraph in $xml.SelectNodes('//w:p', $ns)) {
        $nodes = @($paragraph.SelectNodes('.//w:t', $ns))
        if ($nodes.Count -eq 0) { continue }
        $current = ($nodes | ForEach-Object { $_.InnerText }) -join ''
        if (-not $replacements.Contains($current)) { continue }
        $nodes[0].InnerText = $replacements[$current]
        for ($i = 1; $i -lt $nodes.Count; $i++) { $nodes[$i].InnerText = '' }
        $changed++
    }

    if ($changed -ne $replacements.Count) {
        throw "Expected $($replacements.Count) replacements but changed $changed."
    }

    function Set-NodeText([System.Xml.XmlNode]$node, [string]$value) {
        $textNodes = @($node.SelectNodes('.//w:t', $ns))
        if ($textNodes.Count -eq 0) { throw 'Template node has no text.' }
        $textNodes[0].InnerText = $value
        for ($j = 1; $j -lt $textNodes.Count; $j++) { $textNodes[$j].InnerText = '' }
    }

    $campusHeading = $null
    $bulletTemplate = $null
    foreach ($paragraph in $xml.SelectNodes('//w:body/w:p', $ns)) {
        $paragraphText = (($paragraph.SelectNodes('.//w:t', $ns) | ForEach-Object { $_.InnerText }) -join '')
        if ($paragraphText -eq '校园经历与证书') { $campusHeading = $paragraph }
        if ($paragraphText.StartsWith('• 独立实现零模型成本')) { $bulletTemplate = $paragraph }
    }
    if ($null -eq $campusHeading -or $null -eq $bulletTemplate) { throw 'Could not find project insertion templates.' }

    $internTable = $null
    foreach ($table in $xml.SelectNodes('//w:body/w:tbl', $ns)) {
        $tableText = (($table.SelectNodes('.//w:t', $ns) | ForEach-Object { $_.InnerText }) -join '')
        if ($tableText.Contains('联朋升学')) { $internTable = $table; break }
    }
    if ($null -eq $internTable) { throw 'Could not find internship title table.' }

    $body = $campusHeading.ParentNode
    $projectHeading = $campusHeading.CloneNode($true)
    Set-NodeText $projectHeading '项目经历'
    [void]$body.InsertBefore($projectHeading, $campusHeading)

    $projectTable = $internTable.CloneNode($true)
    $cells = @($projectTable.SelectNodes('./w:tr/w:tc', $ns))
    if ($cells.Count -lt 2) { throw 'Internship title table does not have two cells.' }
    Set-NodeText $cells[0] 'AI API 加速与统一访问平台｜个人项目'
    Set-NodeText $cells[1] '2025—2026'
    [void]$body.InsertBefore($projectTable, $campusHeading)

    $projectBullets = @(
        '• 面向多模型 API 调用中的网络波动与配置分散问题，独立完成统一访问入口的方案设计、前端页面、服务配置及部署维护，支持 Claude、GPT、Gemini 等模型的调用与调试。',
        '• 在 Linux 与 Cloudflare 环境完成域名解析、HTTPS、请求转发和跨域配置；结合日志与 HTTP 状态定位 DNS、连通性、超时及限流问题，提升接口接入与故障排查效率。',
        '• 设计 Prompt 与 JSON 结构化输出约束，处理异常响应和调用边界；沉淀部署及排障流程，使服务能够重复配置、快速迁移并持续运行。'
    )
    foreach ($bullet in $projectBullets) {
        $newBullet = $bulletTemplate.CloneNode($true)
        Set-NodeText $newBullet $bullet
        [void]$body.InsertBefore($newBullet, $campusHeading)
    }

    $entry.Delete()
    $newEntry = $archive.CreateEntry('word/document.xml', [System.IO.Compression.CompressionLevel]::Optimal)
    $writer = [System.IO.StreamWriter]::new($newEntry.Open(), [System.Text.UTF8Encoding]::new($false))
    $xml.Save($writer)
    $writer.Dispose()
}
finally {
    $archive.Dispose()
}

Write-Output $Destination
