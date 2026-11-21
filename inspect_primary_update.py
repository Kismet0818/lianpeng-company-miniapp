import openpyxl, pymysql
p=r'C:\Users\Admin\Desktop\二三梯队学校\二三梯队小学_已完善.xlsx'
w=openpyxl.load_workbook(p,data_only=True)
rows=[]
for r in w['Sheet1'].iter_rows(min_row=2, values_only=True):
    if r[0]: rows.append(r)
print('rows',len(rows),'first',rows[0][:5])
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
cur=conn.cursor(pymysql.cursors.DictCursor)
cur.execute('SELECT id,name,district,tier,school_type FROM primary_school')
db=cur.fetchall(); by={x['name']:x for x in db}
matched=[r[0] for r in rows if r[0] in by]; missing=[r[0] for r in rows if r[0] not in by]
print('matched',len(matched),'missing',len(missing)); print('\n'.join(missing))
print('tier counts', {str(k):sum(1 for x in db if x['tier']==k) for k in set(x['tier'] for x in db)})
conn.close()
