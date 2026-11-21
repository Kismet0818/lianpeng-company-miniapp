import os,re,openpyxl,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLSX']; ws=openpyxl.load_workbook(src,read_only=True,data_only=True).worksheets[4]
source=OrderedDict()
for r in ws.iter_rows(min_row=2,max_col=5,values_only=True):
    if not r[0] or not r[3]: continue
    source.setdefault(str(r[0]).strip(),OrderedDict()).setdefault(str(r[1]).strip(),[]).append(str(r[3]).strip())
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('青浦区','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='青浦区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}
  matched=[]; skipped=[]
  for name,towns in source.items():
   hit=mp.get(norm(name))
   if not hit: skipped.append(name); continue
   parts=[]
   for town,roads in towns.items():
    seen=[]
    for x in roads:
     if x not in seen: seen.append(x)
    parts.append((town+'：' if town else '')+'、'.join(seen))
   matched.append((hit[0],'；'.join(parts)))
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='青浦区' AND admission_year=2026")
  note='来源文件：2026青浦招生合集.xlsx；工作表：初中学区划分一览表；按街道及对口道路汇总'
  for order,(jid,scope) in enumerate(matched,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (2,2026,'青浦区',%s,NULL,'详见初中学区范围','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
 conn.commit(); print(f'source_schools={len(source)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
