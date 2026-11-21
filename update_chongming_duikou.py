import os,re,xlrd,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLS']; ws=xlrd.open_workbook(src).sheet_by_index(5)
source=OrderedDict()
for r in range(1,ws.nrows):
    n=str(ws.cell_value(r,1)).replace('\n','').replace('\xa0','').strip(); scope=str(ws.cell_value(r,4)).replace('\n','').strip()
    if not n or not scope: continue
    source[n]=source.get(n,'')+'；'+scope if n in source else scope
aliases={'三星中学':82,'建设中学':78,'大新中学':73,'三烈中学':81,'合兴中学':76,'向化中学':84,'大公中学':72,'崇东中学':71,'裕安中学':86,'庙镇学校':80,'实验中学':83,'崇中附属':70,'向明附属':92,'长兴中学':89,'长明中学':88,'横沙中学':77,'新海学校':85,'长江学校':87,'上实附属':91}
def norm(s):
    return re.sub(r'\s+','',s.replace('上海市','').replace('崇明区','').replace('（','').replace('）','').replace('(','').replace(')',''))
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
    with conn.cursor() as c:
        c.execute("SELECT id,name FROM junior_high_school WHERE district='崇明区'"); db=dict(c.fetchall()); mp={norm(n):(i,n) for i,n in db.items()}; matched=[]; skipped=[]
        for name,scope in source.items():
            hit=mp.get(norm(name)) or ((aliases[name],name) if name in aliases else None)
            if hit and hit[0] not in [x[0] for x in matched]: matched.append((hit[0],scope))
            elif not hit: skipped.append(name)
        c.execute("DELETE FROM junior_high_primary_duikou WHERE district='崇明区' AND admission_year=2026")
        note='来源文件：2026年崇明区招生合集.xls；工作表：崇明区2026年公办初中招生划区范围'
        for order,(jid,scope) in enumerate(matched,1):
            c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (30,2026,'崇明区',%s,NULL,'详见公办初中招生划区范围','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
    conn.commit(); print(f'source_schools={len(source)} matched={len(matched)} rows={len(matched)} skipped={skipped}')
except:
    conn.rollback(); raise
finally:
    conn.close()
