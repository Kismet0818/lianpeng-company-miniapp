$ErrorActionPreference='Stop'
$src='C:\claude\职业喵\郭浩远简历_修改稿.docx'; $out='C:\claude\职业喵\郭浩远简历_AI应用开发版.docx'; $pdf='C:\claude\职业喵\郭浩远简历_AI应用开发版.pdf'
if(!(Test-Path -LiteralPath $src)){throw "底稿不存在：$src"}
$w=New-Object -ComObject Word.Application; $w.Visible=$false; $w.DisplayAlerts=0; $d=$null
try{
 $d=$w.Documents.Add(); $s=$d.Sections.Item(1); $s.PageSetup.PaperSize=7; $s.PageSetup.TopMargin=$w.CentimetersToPoints(1.05); $s.PageSetup.BottomMargin=$w.CentimetersToPoints(.95); $s.PageSetup.LeftMargin=$w.CentimetersToPoints(1.4); $s.PageSetup.RightMargin=$w.CentimetersToPoints(1.4)
 $n=$d.Styles.Item(-1); $n.Font.Name='Arial'; $n.Font.NameFarEast='微软雅黑'; $n.Font.Size=9
 function P([string]$t,[double]$z=9,[bool]$b=$false,[int]$a=0,[double]$be=0,[double]$af=0,[double]$li=0,[double]$fi=0,[double]$ls=10.7,[bool]$kn=$false){$p=$d.Paragraphs.Add();$p.Range.Text=$t;$p.Range.Font.Name='Arial';$p.Range.Font.NameFarEast='微软雅黑';$p.Range.Font.Size=$z;$p.Range.Font.Bold=[int]$b;$p.Format.Alignment=$a;$p.Format.SpaceBefore=$be;$p.Format.SpaceAfter=$af;$p.Format.LeftIndent=$w.CentimetersToPoints($li);$p.Format.FirstLineIndent=$w.CentimetersToPoints($fi);$p.Format.LineSpacingRule=4;$p.Format.LineSpacing=$ls;$p.Format.KeepWithNext=[int]$kn;return $p}
 function H([string]$t){$p=P $t 10.3 $true 0 3.5 .8 0 0 12 $true;$p.Borders.Item(-3).LineStyle=1;$p.Borders.Item(-3).LineWidth=6;$p.Borders.Item(-3).Color=7368816;$p.Borders.DistanceFromBottom=1}
 function B([string]$t){P ('• '+$t) 8.55 $false 3 0 .55 .47 -.47 10.35 $false|Out-Null}
 function T([string]$l,[string]$r,[double]$z=9,[bool]$b=$false){$p=P ($l+"`t"+$r) $z $b 0 0 .5 0 0 10.8 $true;$p.TabStops.ClearAll();$u=$s.PageSetup.PageWidth-$s.PageSetup.LeftMargin-$s.PageSetup.RightMargin;$p.TabStops.Add($u,2)|Out-Null}
 P '郭浩远' 18.5 $true 1 0 1 0 0 20|Out-Null; P '150-2139-3462  ｜  3259509003@qq.com' 9.1 $false 1 0 1.4 0 0 10.6|Out-Null; P 'AI 应用开发实习生｜大模型应用开发实习生' 10.1 $true 1 0 .8 0 0 11.8|Out-Null
 H '教育经历'; T '上海电机学院｜人工智能｜本科' '2023.09—2027.06' 9.1 $true; P '主修课程：数据结构、算法设计、数据库原理、机器学习、人工智能' 8.55 $false 0 0 .2 0 0 10.3|Out-Null
 H '专业技能'
 B '编程与服务开发：熟悉 Java、Spring Boot、MyBatis/MyBatis-Plus 与 RESTful API，具备业务接口设计、联调和问题排查经验。'
 B '大模型应用：具有 Claude、GPT、Gemini 等主流大模型 API 调用、Prompt 设计和 JSON 结构化输出调试经验；了解上下文管理及 Tool Calling 机制。'
 B '数据与检索基础：熟悉 MySQL、SQL、事务和索引，具备 HAR、JSON、Word、PDF 多源数据清洗、批量导入、幂等更新及一致性校验经验；了解 RAG 的文档解析、切块、Embedding、向量检索、上下文增强、生成与评测流程。'
 B '工程与部署：熟悉 Maven、Git、Linux 基础命令，具有 Docker、阿里云 OSS、微信云托管、接口测试、限流、日志排查及版本回滚实践。'
 H '实习经历'; T '联朋升学｜Java 后端开发实习生' '2026—至今' 9.1 $true
 B '主导教育类微信小程序 2.0 的工程落地，贯通 Spring Boot 服务端、MySQL、前后端联调、云端环境和上线流程；完成 Maven 构建、Docker 打包及微信云托管发布，版本 006 成功接管流量并保留版本 004 回退方案。'
 B '处理 728 条初中、169 条查分高中、140 条普通高中及 152 条综合/国际学校数据记录；开发 HAR、JSON、Word、PDF 抽取与导入工具，完成字段映射、去重合并、规范化、幂等更新和一致性回查。'
 B '设计并实现微信登录、手机号授权、用户状态管理与学校访问行为链路；记录来源、次数和停留时长，过滤少于 3 秒的误触并聚合兴趣数据，为后续兴趣分析和可解释推荐准备数据特征。'
 B '对登录、搜索、列表、详情及写操作实施差异化限流，补充分页边界、详情枚举和写入参数校验；覆盖 HTTP 429、分页越界、枚举访问和非法写入等 8 类异常场景。'
 B '迁移政策 PDF、学校图片和附件至阿里云 OSS，建立文件校验、URL 回查及数据库备份与增量迁移流程；完成云端数据核验、上线回归、回滚文档及公安网安备案材料整理。'
 H 'AI 应用实践'
 B '调用 Claude、GPT、Gemini 等模型 API 完成参数配置、Prompt 迭代与结构化输出调试，能够约束输出格式，并定位接口、限流和连通性问题。'
 B '持续学习可溯源 RAG 应用链路，理解文档解析、语义切块、Embedding、Top K 检索、上下文增强、引用溯源、无依据拒答和离线评测的作用与边界。'
 H '校园经历与证书'; T '学院宣传部部员｜参与公众号运营与推文撰写' '2023.09—2025.06' 8.7 $true; T '大学英语四级（CET-4）' '2023.12' 8.7 $true
 $d.TrackRevisions=$false; $d.AcceptAllRevisions(); $d.DeleteAllComments(); $d.Repaginate(); if($d.ComputeStatistics(2)-ne 1){throw "当前页数=$($d.ComputeStatistics(2))"}; $d.SaveAs2($out,16); $d.ExportAsFixedFormat($pdf,17); "PAGES=$($d.ComputeStatistics(2))`nDOCX=$out`nPDF=$pdf"
}finally{if($d){$d.Close($false)};$w.Quit();[Runtime.InteropServices.Marshal]::ReleaseComObject($w)|Out-Null;[GC]::Collect();[GC]::WaitForPendingFinalizers()}


