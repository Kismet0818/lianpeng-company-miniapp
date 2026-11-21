import os,re,xlrd,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLS']; ws=xlrd.open_workbook(src).sheet_by_index(0)
pairs=[]
for col in (0,2,4):
 current=None
 for r in range(1,ws.nrows):
  p=str(ws.cell_value(r,col)).replace('\n','').strip(); j=str(ws.cell_value(r,col+1)).replace('\n','').strip()
  if j and j not in ('学校','小学部','二实验'):
   current=j
  if p and p not in ('小学部','学校') and current:
   pairs.append((current,p))
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('普陀区','').replace('（','').replace('）','').replace('(','').replace(')','').replace('附属',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='普陀区'"); js=dict(c.fetchall()); jm={norm(n):(i,n) for i,n in js.items()}
  c.execute("SELECT id,name FROM primary_school WHERE district='普陀区'"); ps=dict(c.fetchall()); pm={norm(n):(i,n) for i,n in ps.items()}
  aliases={'同济二附中':556,'尚阳外国语':554,'上理工附属':518,'华东师大四附中':514,'上师大二实验':520,'区教院附中':538,'曹二实验':522,'区教院附属':538,'梅陇实验中学':539}
  grouped=OrderedDict(); skipped=[]
  for j,p in pairs:
   hit=jm.get(norm(j)) or ((aliases[j],j) if j in aliases else None)
   if not hit: skipped.append(j); continue
   ph=pm.get(norm(p)); key=(hit[0],ph[0] if ph else None,ph[1] if ph else p)
   grouped[key]=1
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='普陀区' AND admission_year=2026")
  note='来源文件：2026年普陀区招生合集.xls；工作表：小学毕业生对口初中方案'
  for order,(jid,pid,pname) in enumerate(grouped,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (13,2026,'普陀区',%s,%s,%s,'direct','对口入学','小学毕业生对口初中方案',%s,%s,1)",(jid,pid,pname,note,order))
 conn.commit(); print(f'parsed_pairs={len(pairs)} relations={len(grouped)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
