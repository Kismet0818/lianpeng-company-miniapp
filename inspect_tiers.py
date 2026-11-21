import pymysql
c=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
q=c.cursor()
for t in ['primary_school','junior_high_school','score_query_tool_school']:
 q.execute(f"SELECT tier,HEX(tier),COUNT(*) FROM {t} GROUP BY tier,HEX(tier) ORDER BY HEX(tier)")
 print(t,q.fetchall())
c.close()
