from docx import Document
import importlib.util
print('whisper', importlib.util.find_spec('whisper'))
print('faster_whisper', importlib.util.find_spec('faster_whisper'))
p=r'C:\Users\Admin\Desktop\新建文件夹\9.20会议记录(2)(1).docx'
d=Document(p)
print('paras',len(d.paragraphs),'tables',len(d.tables))
for i,p in enumerate(d.paragraphs):
 print(i,repr(p.text),p.style.name, p.alignment, p.paragraph_format.left_indent, p.paragraph_format.first_line_indent)
print('sections',len(d.sections))
for s in d.sections: print(s.page_width,s.page_height,s.top_margin,s.bottom_margin,s.left_margin,s.right_margin)
