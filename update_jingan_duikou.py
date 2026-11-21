import os,re,xlrd,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLS']; ws=xlrd.open_workbook(src).sheet_by_index(1)
source=OrderedDict(); current=None; method=None
for r in range(2,ws.nrows):
    a=str(ws.cell_value(r,0)).strip(); b=str(ws.cell_value(r,1)).strip(); p=str(ws.cell_value(r,2)).strip(); m=str(ws.cell_value(r,3)).strip()
    cand=b if b and ('中学' in b or '学校' in b) else (a if a and ('中学' in a or '学校' in a) and a not in ('上海市','上海市市北教育集团','上海市风华初级中学教育集团','上海市彭浦初级中学教育集团') else '')
    if cand: current=cand.replace('\n','').strip()
    if m: method=m
    if current and p: source.setdefault(current,[]).append((p,method or '对口入学'))
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('静安区','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='静安区'"); js=dict(c.fetchall()); jm={norm(n):(i,n) for i,n in js.items()}
  c.execute("SELECT id,name FROM primary_school WHERE district='静安区'"); ps=dict(c.fetchall()); pm={norm(n):(i,n) for i,n in ps.items()}
  grouped=OrderedDict(); skipped=[]
  for j,plist in source.items():
   hit=jm.get(norm(j))
   if not hit: skipped.append(j); continue
   for p,m in plist:
    ph=pm.get(norm(p)); key=(hit[0],ph[0] if ph else None,ph[1] if ph else p)
    grouped[key]=m
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='静安区' AND admission_year=2026")
  note='来源文件：2026年静安区招生合集.xls；工作表：公办初中入学方式'
  for order,((jid,pid,pname),m) in enumerate(grouped.items(),1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (11,2026,'静安区',%s,%s,%s,'direct',%s,'对口小学',%s,%s,1)",(jid,pid,pname,m,note,order))
 conn.commit(); print(f'source_junior={len(source)} relations={len(grouped)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
