from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.text import WD_BREAK


OUT = r"C:\Users\Admin\Desktop\lpsx\outputs\Java后端开发实习简历_联朋升学.docx"
ACCENT = "184A78"
TEXT = "242B33"
MUTED = "606A75"
LIGHT = "EAF1F7"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=70, start=100, bottom=70, end=100):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for m, v in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{m}"))
        if node is None:
            node = OxmlElement(f"w:{m}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(v))
        node.set(qn("w:type"), "dxa")


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def remove_paragraph_borders(paragraph):
    p_pr = paragraph._p.get_or_add_pPr()
    p_bdr = p_pr.find(qn("w:pBdr"))
    if p_bdr is not None:
        p_pr.remove(p_bdr)


def set_font(run, east="Microsoft YaHei", latin="Aptos", size=9.2, color=TEXT, bold=False):
    run.font.name = latin
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), east)
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), latin)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), latin)
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor.from_string(color)
    run.bold = bold


def add_mixed(paragraph, parts, size=9.2, color=TEXT):
    for text, bold in parts:
        set_font(paragraph.add_run(text), size=size, color=color, bold=bold)


def add_section_heading(doc, text):
    p = doc.add_paragraph(style="Heading 1")
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(3.5)
    p.paragraph_format.keep_with_next = True
    set_font(p.add_run(text), size=11.5, color="000000", bold=True)
    return p


def add_bullet(doc, parts):
    p = doc.add_paragraph(style="Resume Bullet")
    add_mixed(p, parts, size=8.8)
    return p


doc = Document()
sec = doc.sections[0]
sec.top_margin = Cm(1.25)
sec.bottom_margin = Cm(1.2)
sec.left_margin = Cm(1.55)
sec.right_margin = Cm(1.55)

styles = doc.styles
normal = styles["Normal"]
normal.font.name = "Aptos"
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
normal.font.size = Pt(9.2)
normal.font.color.rgb = RGBColor.from_string(TEXT)
normal.paragraph_format.line_spacing = 1.16
normal.paragraph_format.space_after = Pt(2.5)

title_style = styles["Title"]
title_style.font.name = "Aptos Display"
title_style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
title_style.font.color.rgb = RGBColor(0, 0, 0)
title_style.font.size = Pt(22)
title_style.font.bold = True
title_ppr = title_style._element.get_or_add_pPr()
title_pbdr = title_ppr.find(qn("w:pBdr"))
if title_pbdr is not None:
    title_ppr.remove(title_pbdr)

h1 = styles["Heading 1"]
h1.font.color.rgb = RGBColor(0, 0, 0)
h1._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")

bullet = styles.add_style("Resume Bullet", WD_STYLE_TYPE.PARAGRAPH)
bullet.base_style = normal
bullet.paragraph_format.left_indent = Cm(0.45)
bullet.paragraph_format.first_line_indent = Cm(-0.28)
bullet.paragraph_format.space_after = Pt(2.8)
bullet.paragraph_format.line_spacing = 1.12
bullet.paragraph_format.keep_together = True
bullet.paragraph_format.widow_control = True
bullet._element.get_or_add_pPr().insert(0, OxmlElement("w:keepNext"))

# Header identity block
p = doc.add_paragraph(style="Title")
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(1)
remove_paragraph_borders(p)
set_font(p.add_run("姓名"), size=22, color="000000", bold=True)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(4)
set_font(p.add_run("Java 后端开发实习生"), size=11.5, color=ACCENT, bold=True)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(6)
set_font(p.add_run("手机：请填写  ·  邮箱：请填写  ·  所在地：上海  ·  求职方向：Java 后端开发"), size=8.8, color=MUTED)

add_section_heading(doc, "个人概述")
p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(3)
add_mixed(p, [
    ("具备基于 ", False), ("Spring Boot", True), (" 的微信小程序后端开发与云端部署经验，参与升学择校平台的数据治理、缓存优化、异步解耦、接口安全及文件服务建设。完成 ", False),
    ("3000+ 条学校数据", True), (" 清洗入库，参与维护 ", False), ("120+ 个业务接口", True), ("，具备 Redis、Redisson、RabbitMQ 在读多写少与异步任务场景中的工程实践。", False)
], size=9.1)

add_section_heading(doc, "核心技能")
table = doc.add_table(rows=3, cols=2)
table.autofit = False
table.columns[0].width = Cm(3.2)
table.columns[1].width = Cm(14.5)
labels = ["后端开发", "数据与安全", "云服务与交付"]
values = [
    "Java · Spring Boot · MyBatis-Plus · RESTful API · 微信小程序后端",
    "MySQL/CynosDB · Redis · Redisson · RabbitMQ · JWT · 组合索引 · 分级限流",
    "阿里云 OSS · Docker · 腾讯云托管 · JMeter · 健康检查 · 回归验证",
]
for i, row in enumerate(table.rows):
    row.cells[0].width = Cm(3.2)
    row.cells[1].width = Cm(14.5)
    for c in row.cells:
        c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        set_cell_margins(c)
    set_cell_shading(row.cells[0], LIGHT)
    p0 = row.cells[0].paragraphs[0]
    p0.paragraph_format.space_after = Pt(0)
    set_font(p0.add_run(labels[i]), size=8.7, color=ACCENT, bold=True)
    p1 = row.cells[1].paragraphs[0]
    p1.paragraph_format.space_after = Pt(0)
    set_font(p1.add_run(values[i]), size=8.7)

add_section_heading(doc, "实习经历")
t = doc.add_table(rows=1, cols=2)
t.autofit = False
t.columns[0].width = Cm(13.2)
t.columns[1].width = Cm(4.5)
for c in t.rows[0].cells:
    set_cell_margins(c, top=0, bottom=0, start=0, end=0)
    c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
p = t.cell(0, 0).paragraphs[0]
p.paragraph_format.space_after = Pt(0)
add_mixed(p, [("上海联朋教育科技有限公司", True), ("  |  Java 后端开发实习生", False)], size=10.2)
p = t.cell(0, 1).paragraphs[0]
p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
p.paragraph_format.space_after = Pt(0)
set_font(p.add_run("2026.07 - 2026.10"), size=9, color=MUTED, bold=True)

p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(2)
p.paragraph_format.space_after = Pt(1.5)
add_mixed(p, [("项目：", True), ("联朋升学  上海升学择校微信小程序", True)], size=9.3, color=ACCENT)

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(3)
set_font(p.add_run("面向上海学生及家长的一站式升学服务平台，覆盖学校检索筛选、对比、招生资讯、查分与志愿填报、收藏留资及资料下载；后端基于 Spring Boot 构建，对接微信小程序、MySQL/CynosDB 与阿里云 OSS，并部署于腾讯云托管。"), size=8.8)

add_bullet(doc, [
    ("•  数据治理：", True), ("整合幼儿园、小学、初中、高中及国际学校等多来源异构数据，设计统一的 ", False),
    ("schoolSource + schoolId", True), (" 资源标识，解决收藏、浏览等业务的跨来源关联问题；编写清洗、批量导入与校验脚本，完成 ", False),
    ("3000+ 条学校数据", True), ("去重、字段标准化和增量入库。", False)
])
add_bullet(doc, [
    ("•  缓存优化：", True), ("采用 ", False), ("Redis Cache-Aside", True), (" 缓存学校详情、地区统计与热门列表，通过随机 TTL 与空值短 TTL 分别降低缓存雪崩、穿透风险；使用 ", False),
    ("Redisson 分布式锁", True), ("控制热点 Key 重建并进行二次检查，避免多实例并发回源。经本地 JMeter 模拟压测，热点查询平均响应时间由约 ", False),
    ("420 ms 降至 70 ms", True), ("，吞吐量提升约 ", False), ("4 倍", True), ("，数据库查询量下降约 ", False), ("75%", True), ("。", False)
])
add_bullet(doc, [
    ("•  异步解耦：", True), ("使用 ", False), ("RabbitMQ", True), ("异步处理浏览行为、资料下载记录与 OSS 图片压缩/水印任务；配置消息持久化、手动 ACK、失败重试与死信队列，并以业务唯一键保障幂等消费。经本地 JMeter 模拟压测，资料下载接口平均响应时间由约 ", False),
    ("800 ms 降至 150 ms", True), ("。", False)
])
add_bullet(doc, [
    ("•  接口与安全：", True), ("参与维护 ", False), ("120+ 个业务接口", True), ("；完成微信登录链路与权限校验，采用 ", False),
    ("JWT + 拦截器", True), ("实现登录态验证，并以 X-Admin-Key 隔离管理端接口；针对登录、留资、搜索和详情接口设计分级滑动窗口限流。", False)
])
add_bullet(doc, [
    ("•  风险防护：", True), ("补充可信代理 IP 解析、分页参数上限与详情枚举防护，减少接口刷取、恶意提交及异常参数造成的服务压力。", False)
])
add_bullet(doc, [
    ("•  文件服务：", True), ("对接 ", False), ("阿里云 OSS", True), ("实现图片与 PDF 资料的签名直传、在线预览和受控访问，减少文件经由业务服务器中转带来的带宽与处理开销。", False)
])
add_bullet(doc, [
    ("•  部署交付：", True), ("使用 ", False), ("Docker", True), ("构建后端服务并部署至腾讯云托管，梳理本地与云端数据库迁移、环境变量配置、健康检查及回归验证流程，提升版本发布与问题定位效率。", False)
])

# Footer with understated page marker
footer = sec.footer
fp = footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
set_font(fp.add_run("Java 后端开发实习简历"), size=7.6, color="8A939D")

doc.core_properties.title = "Java 后端开发实习简历"
doc.core_properties.subject = "联朋升学项目实习经历"
doc.core_properties.author = ""
doc.core_properties.keywords = "Java, Spring Boot, MySQL, OSS, Docker"
doc.save(OUT)
print(OUT)
