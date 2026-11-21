import os,re,openpyxl,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLSX']; ws=openpyxl.load_workbook(src,read_only=True,data_only=True).worksheets[0]
source=OrderedDict()
for r in ws.iter_rows(min_row=2,max_col=4,values_only=True):
    if not r[1] or not r[2]: continue
    name=str(r[1]).replace('\n','').strip(); scope=str(r[2]).replace('\n','').strip()
    source[name]=scope
def norm(s):
    s=re.sub(r'[（(][^（）()]*[)）]','',s).replace('上海市','').replace('嘉定区','')
    return re.sub(r'\s+','',s)
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='嘉定区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}
  matched_map={}; skipped=[]
  for name,scope in source.items():
   hit=mp.get(norm(name))
   if hit: matched_map[hit[0]]=scope
   else: skipped.append(name)
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='嘉定区' AND admission_year=2026")
  note='来源文件：2026年嘉定区招生合集.xlsx；工作表：公办初中划片范围、招生方式与招生计划'
  matched=list(matched_map.items())
  for order,(jid,scope) in enumerate(matched,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (22,2026,'嘉定区',%s,NULL,'详见户籍对口范围','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
 conn.commit(); print(f'source_schools={len(source)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
