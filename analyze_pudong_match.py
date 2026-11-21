import os,re,openpyxl,pymysql,difflib
src=os.environ['SOURCE_XLSX']
wb=openpyxl.load_workbook(src,read_only=True,data_only=True); ws=wb.worksheets[0]
groups={}
for r in ws.iter_rows(min_row=3,max_col=5,values_only=True):
    if r[2] and r[3] and r[4]: groups.setdefault(str(r[2]).strip(),{})
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
with conn.cursor() as c:
    c.execute("SELECT id,name FROM junior_high_school WHERE district='浦东新区' AND school_type='公办初中'")
    db=dict(c.fetchall())
def norm(s):
    s=re.sub(r'[（(][^（）()]*[)）]','',s)
    s=s.replace('上海市','').replace('浦东新区','')
    return re.sub(r'\s+','',s)
nm={norm(n):(i,n) for i,n in db.items()}
matched=[]; unmatched=[]
for s in groups:
    if norm(s) in nm: matched.append((s,nm[norm(s)]))
    else:
        cand=difflib.get_close_matches(norm(s),list(nm),n=2,cutoff=.75)
        unmatched.append((s,[(nm[x][0],nm[x][1],round(difflib.SequenceMatcher(None,norm(s),x).ratio(),2)) for x in cand]))
print('source',len(groups),'matched',len(matched),'unmatched',len(unmatched))
for x in unmatched: print(x[0].encode('unicode_escape').decode(), x[1])
