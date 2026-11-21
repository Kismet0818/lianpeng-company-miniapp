import os,re,xlrd,pymysql
src=os.environ['SOURCE_XLS']; ws=xlrd.open_workbook(src).sheet_by_index(3)
names=[]
for r in range(2,ws.nrows):
 n=str(ws.cell_value(r,1)).replace('\n','').strip()
 if n and ('中学' in n or '学校' in n): names.append(n)
def norm(s): return re.sub(r'\s+','',s.replace('上海市','').replace('虹口区','').replace('（初中部）','').replace('(初中部)','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM junior_high_school WHERE district='虹口区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}; matched=[]; skipped=[]
  for n in names:
   hit=mp.get(norm(n))
   if hit and hit[0] not in [x[0] for x in matched]: matched.append(hit)
   elif not hit: skipped.append(n)
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='虹口区' AND admission_year=2026")
  note='来源文件：2026年虹口区招生合集.xls；工作表：公办初中招生计划和联系方式；该工作表未提供具体对口小学名单'
  for order,(jid,n) in enumerate(matched,1):
   c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (4,2026,'虹口区',%s,NULL,'详见招生方案','unknown','详见招生方案','公办初中招生计划和联系方式',%s,%s,1)",(jid,note,order))
 conn.commit(); print(f'source_schools={len(names)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except: conn.rollback(); raise
finally: conn.close()
