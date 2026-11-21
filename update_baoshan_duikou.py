import os,re,xlrd,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLS']; ws=xlrd.open_workbook(src).sheet_by_index(1)
source=OrderedDict()
for r in range(2,ws.nrows):
    name=str(ws.cell_value(r,1)).strip(); addr=str(ws.cell_value(r,2)).strip(); scope=str(ws.cell_value(r,3)).strip()
    if not name or not scope: continue
    source.setdefault(name,[]).append((addr,scope))
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('宝山区','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='宝山区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}
  matched=[]; skipped=[]
  for name,items in source.items():
   hit=mp.get(norm(name))
   if not hit: skipped.append(name); continue
   jid=hit[0]; parts=[]
   for addr,scope in items: parts.append((addr+'：' if addr else '')+scope)
   matched.append((jid,'；'.join(parts)))
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='宝山区' AND admission_year=2026")
  note='来源文件：2026年宝山区招生合集.xls；工作表：校区范围与招生计划（初中）'
  for order,(jid,scope) in enumerate(matched,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (20,2026,'宝山区',%s,NULL,'详见户籍对口范围','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
 conn.commit(); print(f'source_schools={len(source)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
