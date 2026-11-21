from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = '工作安排及业务问题讨论会议纪要.docx'

def set_cell_shading(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:fill'), fill)
    tcPr.append(shd)

def set_run_font(run, name='宋体', size=11, bold=False, color=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn('w:eastAsia'), name)
    run.font.size = Pt(size)
    run.bold = bold
    if color:
        run.font.color.rgb = RGBColor(*color)

def add_text(p, text, bold=False, size=11):
    r = p.add_run(text)
    set_run_font(r, size=size, bold=bold)
    return r

def set_para(p, before=0, after=6, line=1.35, first=True):
    fmt = p.paragraph_format
    fmt.space_before = Pt(before)
    fmt.space_after = Pt(after)
    fmt.line_spacing = line
    if first:
        fmt.first_line_indent = Cm(0.74)

def add_body(doc, text, lead=None):
    p = doc.add_paragraph()
    set_para(p)
    if lead:
        add_text(p, lead, bold=True)
        add_text(p, text)
    else:
        add_text(p, text)
    return p

def add_bullet(doc, text):
    p = doc.add_paragraph(style='List Bullet')
    set_para(p, after=4, first=False)
    add_text(p, text)
    return p

def add_heading(doc, text, level=1):
    p = doc.add_paragraph()
    p.style = f'Heading {level}'
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.space_before = Pt(12 if level == 1 else 7)
    p.paragraph_format.space_after = Pt(5)
    r = p.add_run(text)
    set_run_font(r, size=14 if level == 1 else 12, bold=True)
    return p

doc = Document()
sec = doc.sections[0]
sec.top_margin = Cm(2.1); sec.bottom_margin = Cm(2.0)
sec.left_margin = Cm(2.3); sec.right_margin = Cm(2.3)

styles = doc.styles
styles['Normal'].font.name = '宋体'
styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
styles['Normal'].font.size = Pt(11)
for sname in ['Heading 1','Heading 2']:
    styles[sname].font.name = '黑体'
    styles[sname]._element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    styles[sname].font.color.rgb = RGBColor(0,0,0)

title = doc.add_paragraph(style='Title')
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
title.paragraph_format.space_after = Pt(16)
r = title.add_run('工作安排及业务问题讨论会议纪要')
set_run_font(r, name='黑体', size=20, bold=True)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(16)
add_text(p, '会议重点：复盘公众号业务表现，明确抖音账号处理方案及国庆后工作安排。', size=10)

add_heading(doc, '一 公众号业务复盘', 1)
add_heading(doc, '1 流量现状', 2)
add_body(doc, '公众号目前未进行投流或加热推广，主要依靠自然流量与平台推荐起量，粉丝基数较小，仅有几十个粉丝。9月24日发布的一篇爆款文章带动整体流量明显上涨，爆款文章可获得平台持续推荐。当前流量主要来自平台推荐，以及家长用户之间的转发分享。')
add_heading(doc, '2 优质内容梳理', 2)
add_bullet(doc, 'SMK系列内容质量较好，转发量较高。SMK不属于公司主营培训业务，但可以此为切入点，持续挖掘优质初中相关内容，为家长提供服务。该系列稿件由水管工耗时两周完成。')
add_bullet(doc, '幼升小相关文章的流量在发布后期出现上涨；幼儿园相关文章浏览量较高，但互动几乎为零，说明内容尚未精准命中用户需求。')
add_bullet(doc, '9月22日文章及幼儿园相关文章由赵迪（小迪）撰写。')
add_heading(doc, '3 内容后续规划', 2)
add_bullet(doc, '沿用SMK文章的写作风格，持续产出政策性升学类内容。')
add_bullet(doc, '结合直播回放素材，从SMK角度撰写优质初中盘点文章。')
add_bullet(doc, '待写稿件包括：2027上海中考人数、上海各区教育资源对比。神秘考推优质初中文章安排在国庆之后创作。')
add_bullet(doc, '幼儿园板块下半年可能存在插班需求，后续根据实际情况跟进。')
add_heading(doc, '4 业务动作', 2)
add_body(doc, '由耿俊航负责完成公众号与小程序的关联打通，承接公众号现有流量。公众号后台已收到部分家长私信咨询，后续内容创作应进一步贴合家长的实际需求。')

add_heading(doc, '二 抖音及视频号账号问题与处理方案', 1)
add_heading(doc, '1 现状', 2)
add_body(doc, '抖音账号已完成充值，但直播仅进行10分钟即被封禁。初步推测原因为营业执照经营范围缺少教育培训类目，涉及教育相关内容时容易触发封禁。视频号暂未进行充值投放。现有老账号具备一定流量，但用户留言咨询较少，直播业务暂时无法正常开展。')
add_heading(doc, '2 处理方案', 2)
add_bullet(doc, '保留现有老账号，不再更新，使账号自然运行；账号名称修改为“莲蓬落户升学”，并调整Logo版式，与新账号形成视觉区分。')
add_bullet(doc, '使用新手机号及更新完成后的营业执照，重新注册企业抖音账号用于直播；相关资质办理需要法人配合。')
add_bullet(doc, '由小纪跟进落实新抖音账号注册及相关全部工作。')

add_heading(doc, '三 人员工作安排', 1)
add_body(doc, '国庆7天假期正常休息，本次会议仅规划节后复工后的工作，重点聚焦节后3个工作日。')
add_heading(doc, '1 小纪', 2)
add_bullet(doc, '跟进新抖音账号注册及资质办理；BOSS渠道合作洽谈延后至国庆结束后开展。')
add_bullet(doc, '完成公众号与小程序打通工作；下午与负责人评审小程序定稿版面，确认无误后上传，开发重点为升学工具功能。')
add_heading(doc, '2 撰稿人员', 2)
add_bullet(doc, '完成《2027上海中考人数》《上海各区教育资源对比》两篇稿件，可参考共享文档内历史旧稿。')
add_bullet(doc, '尽快完成比心老师直播回放扒取，整理为文档稿件后发送至视频组，作为团队内容素材参考；已经扒完的2条回放稿件需及时提交。')
add_bullet(doc, '合理制定排期，不设置过高产出目标，优先保证内容质量。')
add_heading(doc, '3 其他成员', 2)
add_bullet(doc, '节后3个工作日内完成包玉刚学校相关稿件及视频，之后继续排期其他学校内容；国庆期间可提前梳理相关素材。')
add_heading(doc, '4 负责人工作', 2)
add_bullet(doc, '线下实地拜访合作方，拓展业务渠道，解决自媒体获客速度慢的问题；前期对接的小红书合作，对方目前较为抗拒。')

add_heading(doc, '四 其他事项', 1)
add_body(doc, '无其他补充议题，会议结束。')

footer = sec.footer.paragraphs[0]
footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
add_text(footer, '工作安排及业务问题讨论会议纪要', size=9)

doc.core_properties.title = '工作安排及业务问题讨论会议纪要'
doc.core_properties.subject = '公众号、抖音及视频号业务复盘与工作安排'
doc.core_properties.author = '莲蓬团队'
doc.save(OUT)
print(OUT)
