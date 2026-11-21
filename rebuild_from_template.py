from docx import Document
from docx.shared import Pt
from docx.oxml.ns import qn

template = r'C:\Users\Admin\Desktop\新建文件夹\9.20会议记录(2)(1).docx'
out = r'C:\Users\Admin\Desktop\新建文件夹\9.20会议纪要_按模板整理.docx'
doc = Document(template)
body = doc._element.body
for child in list(body):
    if child.tag != qn('w:sectPr'):
        body.remove(child)

sections = [
('会议时间：2026-09-20', '会议时间'),
('参会人员：5人', '参会人员'),
('会议记录：赵迪（小迪）', '记录人'),
('会议结论', 'heading'),
('本次会议围绕公众号业务复盘、抖音及视频号账号问题、节后内容生产与业务承接进行讨论。会议明确：公众号继续围绕家长升学需求生产政策及学校信息内容；抖音保留老账号并重新注册企业账号用于直播；节后3个工作日优先完成账号资质、小程序打通、重点稿件及视频素材整理。', 'body'),
('一、公众号业务复盘', 'heading'),
('1. 流量现状', 'sub'),
('公众号目前未进行投流或加热推广，主要依靠自然流量和平台推荐起量，粉丝基数较小，仅有几十个粉丝。9月24日发布的一篇爆款文章带动整体流量明显上涨，爆款文章可获得平台持续推荐。当前流量主要来自平台推荐，以及家长用户之间的转发分享。', 'body'),
('2. 优质内容梳理', 'sub'),
('SMK系列内容质量较好，转发量较高。SMK不属于公司主营培训业务，但可作为切入点，持续挖掘优质初中相关内容，为家长提供服务；该系列稿件由水管工耗时两周完成。', 'body'),
('幼升小相关文章的流量在发布后期出现上涨；幼儿园相关文章浏览量较高，但互动几乎为零，说明内容尚未精准命中用户需求。9月22日文章及幼儿园相关文章由赵迪（小迪）撰写。', 'body'),
('3. 内容后续规划', 'sub'),
('沿用SMK文章的写作风格，持续产出政策性升学类内容；结合直播回放素材，从SMK角度撰写优质初中盘点文章。', 'body'),
('待写稿件包括：2027上海中考人数、上海各区教育资源对比。神秘考推优质初中文章安排在国庆之后创作。幼儿园板块下半年可能存在插班需求，后续根据实际情况跟进。', 'body'),
('4. 业务动作', 'sub'),
('由耿俊航负责完成公众号与小程序的关联打通，承接公众号现有流量。公众号后台已收到部分家长私信咨询，后续内容创作应进一步贴合家长实际需求。', 'body'),
('二、抖音及视频号账号问题与处理方案', 'heading'),
('1. 现状', 'sub'),
('抖音账号已完成充值，但直播仅进行10分钟即被封禁。初步推测原因为营业执照经营范围缺少教育培训类目，涉及教育相关内容时容易触发封禁。视频号暂未进行充值投放。现有老账号具备一定流量，但用户留言咨询较少，直播业务暂时无法正常开展。', 'body'),
('2. 处理方案', 'sub'),
('保留现有老账号，不再更新，使账号自然运行；账号名称修改为“莲蓬落户升学”，并调整Logo版式，与新账号形成视觉区分。', 'body'),
('使用新手机号及更新完成后的营业执照，重新注册企业抖音账号用于直播；相关资质办理需要法人配合。由小纪跟进落实新抖音账号注册及相关全部工作。', 'body'),
('三、人员工作安排', 'heading'),
('国庆7天假期正常休息，本次会议仅规划节后复工工作，重点聚焦节后3个工作日。', 'body'),
('1. 小纪', 'sub'),
('跟进新抖音账号注册及资质办理；BOSS渠道合作洽谈延后至国庆结束后开展。完成公众号与小程序打通工作；下午与负责人评审小程序定稿版面，确认无误后上传，开发重点为升学工具功能。', 'body'),
('2. 撰稿人员', 'sub'),
('完成《2027上海中考人数》《上海各区教育资源对比》两篇稿件，可参考共享文档内历史旧稿。尽快完成比心老师直播回放扒取，整理为文档稿件后发送至视频组；已经扒完的2条回放稿件需及时提交。合理制定排期，不设置过高产出目标，优先保证内容质量。', 'body'),
('3. 其他成员', 'sub'),
('节后3个工作日内完成包玉刚学校相关稿件及视频，之后继续排期其他学校内容；国庆期间可提前梳理相关素材。', 'body'),
('4. 负责人工作', 'sub'),
('线下实地拜访合作方，拓展业务渠道，解决自媒体获客速度慢的问题；前期对接的小红书合作，对方目前较为抗拒。', 'body'),
('四、其他事项', 'heading'),
('无其他补充议题，会议结束。', 'body'),
]

def add_para(text, kind):
    p = doc.add_paragraph()
    if kind == 'heading':
        p.style = 'Heading 4'
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(3)
    elif kind == 'sub':
        p.paragraph_format.space_before = Pt(4)
        p.paragraph_format.space_after = Pt(2)
    else:
        p.paragraph_format.space_after = Pt(5)
        p.paragraph_format.line_spacing = 1.15
    run = p.add_run(text)
    run.font.name = '宋体'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    run.font.size = Pt(10.5)
    if kind in ('heading','sub'):
        run.bold = True
    return p

for text, kind in sections:
    add_para(text, kind)

doc.core_properties.title = '9.20会议纪要'
doc.core_properties.author = '莲蓬团队'
doc.save(out)
print(out)
