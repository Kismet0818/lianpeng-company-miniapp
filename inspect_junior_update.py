import openpyxl,pymysql
p=r'C:\Users\Admin\Desktop\二三梯队学校\二三梯队初中.xlsx'; w=openpyxl.load_workbook(p,data_only=True)
rows=[r for r in w['Sheet1'].iter_rows(min_row=2,values_only=True) if r[0]]
c=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
q=c.cursor(pymysql.cursors.DictCursor); q.execute('SELECT id,name,district,tier,school_type FROM junior_high_school'); db=q.fetchall(); by={x['name']:x for x in db}
missing=[r[0] for r in rows if r[0] not in by]
print('rows',len(rows),'db',len(db),'matched',len(rows)-len(missing),'missing',len(missing)); print('\n'.join(missing))
print('first',rows[0]); print('tiers', {str(k):sum(1 for x in db if x['tier']==k) for k in set(x['tier'] for x in db)})
c.close()
