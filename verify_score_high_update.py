import pymysql
c=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
q=c.cursor(pymysql.cursors.DictCursor)
q.execute("SELECT COUNT(*) n,COUNT(founded_year) founded,COUNT(phone) phone,COUNT(fees_standard) fees,COUNT(source_update_time) marked FROM score_query_tool_school WHERE source_update_time>=NOW()-INTERVAL 5 MINUTE")
print(q.fetchone())
q.execute("SELECT name,founded_year,phone,fees_standard,enrollment_year FROM score_query_tool_school WHERE source_update_time>=NOW()-INTERVAL 5 MINUTE ORDER BY id LIMIT 2")
for x in q.fetchall(): print(x)
c.close()
