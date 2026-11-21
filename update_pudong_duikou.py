import os,re,openpyxl,pymysql
from collections import OrderedDict
src=os.environ['SOURCE_XLSX']; wb=openpyxl.load_workbook(src,read_only=True,data_only=True); ws=wb.worksheets[0]
source=OrderedDict()
for r in ws.iter_rows(min_row=3,max_col=5,values_only=True):
    if r[2] and r[3] and r[4]: source.setdefault(str(r[2]).strip(),OrderedDict()).setdefault(str(r[4]).strip(),[]).append(str(r[3]).strip())
def norm(s):
    s=re.sub(r'[（(][^（）()]*[)）]','',s).replace('上海市','').replace('浦东新区','')
    return re.sub(r'\s+','',s)
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
    with conn.cursor() as c:
        c.execute("SELECT id,name FROM junior_high_school WHERE district='浦东新区' AND school_type='公办初中'")
        db=dict(c.fetchall()); dbnorm={norm(n):(i,n) for i,n in db.items()}
        grouped=OrderedDict(); skipped=[]
        for s,towns in source.items():
            hit=dbnorm.get(norm(s))
            if not hit: skipped.append(s); continue
            jid=hit[0]; grouped.setdefault(jid,OrderedDict())
            for town,areas in towns.items(): grouped[jid].setdefault(town,[]).extend(areas)
        c.execute("DELETE FROM junior_high_primary_duikou WHERE district='浦东新区' AND admission_year=2026")
        order=0
        for jid,towns in grouped.items():
            parts=[]
            for town,areas in towns.items():
                seen=[]
                for a in areas:
                    if a not in seen: seen.append(a)
                parts.append(town+'（'+'、'.join(seen)+'）')
            scope='；'.join(parts)
            note='来源文件：2026年浦东新区招生合集.xlsx；工作表：中学对口地段；按学校及所属街镇汇总'
            c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (18,2026,'浦东新区',%s,NULL,'详见中学对口地段','area_based','按地段划片',%s,%s,%s,1)",(jid,scope,note,order))
            order+=1
        conn.commit(); print(f'source_schools={len(source)} matched_source={len(source)-len(skipped)} database_rows={len(grouped)} skipped={skipped}')
except Exception:
    conn.rollback(); raise
finally: conn.close()
