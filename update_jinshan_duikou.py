import os,re,openpyxl,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLSX']; ws=openpyxl.load_workbook(src,read_only=True,data_only=True).worksheets[0]
source=OrderedDict()
for r in ws.iter_rows(min_row=2,max_col=6,values_only=True):
    if not r[1] or not r[4]: continue
    source[str(r[1]).replace('\n','').strip()]=str(r[4]).replace('\n','').strip()
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('金山区','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='金山区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}
  aliases={'上海市金山中学附属山阳中学':250,'上海市新农学校(初中部)':255,'华东师范大学附属枫泾中学(初中部)':229,'上海市金山区钱圩学校(初中部)':241,'上海市金山区吕巷学校(初中部)':240,'上海市干巷学校(初中部)':236}
  matched=[]; skipped=[]
  for name,scope in source.items():
   hit=mp.get(norm(name)) or ((aliases[name],name) if name in aliases else None)
   if hit: matched.append((hit[0],scope))
   else: skipped.append(name)
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='金山区' AND admission_year=2026")
  note='来源文件：2026金山招生合集.xlsx；工作表：公办初中招生入学范围'
  for order,(jid,scope) in enumerate(matched,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (1,2026,'金山区',%s,NULL,'详见对口招生入学范围','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
 conn.commit(); print(f'source_schools={len(source)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
