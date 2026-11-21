import os,re,openpyxl,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLSX']; wb=openpyxl.load_workbook(src,read_only=True,data_only=True); ws=wb.worksheets[7]
source=OrderedDict(); current=None
for r in ws.iter_rows(min_row=3,max_col=4,values_only=True):
    if r[0]: current=str(r[0]).replace('\n','').strip()
    if not current or not r[2]: continue
    primaries=[x.strip() for x in str(r[2]).replace('\r','').split('\n') if x.strip()]
    source.setdefault(current,[]).extend(primaries)
def norm(s):
    s=re.sub(r'[（(][^（）()]*[)）]','',s).replace('上海市','').replace('杨浦区','').replace(' ','').replace('\n','')
    return re.sub(r'\s+','',s)
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='杨浦区'"); js=dict(c.fetchall())
  c.execute("SELECT id,name FROM primary_school WHERE district='杨浦区'"); ps=dict(c.fetchall())
  jmap={norm(n):(i,n) for i,n in js.items()}; pmap={norm(n):(i,n) for i,n in ps.items()}
  grouped=OrderedDict(); skipped=[]
  for j,plist in source.items():
   hit=jmap.get(norm(j))
   if not hit: skipped.append(j); continue
   jid=hit[0]
   for p in plist:
    phit=pmap.get(norm(p)); pid=phit[0] if phit else None; pname=phit[1] if phit else p
    grouped[(jid,pid,pname)]=j
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='杨浦区' AND admission_year=2026")
  note='来源文件：2026年杨浦招生合集.xlsx；工作表：Sheet8；2026年杨浦区小学对口公办初中方案及招生计划'
  for order,((jid,pid,pname),j) in enumerate(grouped.items(),1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (16,2026,'杨浦区',%s,%s,%s,'direct','对口入学','Sheet8',%s,%s,1)",(jid,pid,pname,note,order))
  conn.commit(); print(f'source_junior={len(source)} matched_junior={len(source)-len(skipped)} relations={len(grouped)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
