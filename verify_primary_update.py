import pymysql
c=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4')
q=c.cursor(pymysql.cursors.DictCursor)
q.execute("SELECT COUNT(*) n, COUNT(founded_year) founded, COUNT(phone) phone, COUNT(latitude) lat, COUNT(longitude) lon FROM primary_school WHERE source_update_time >= NOW()-INTERVAL 5 MINUTE")
print(q.fetchone())
q.execute("SELECT name,founded_year,phone,latitude,longitude FROM primary_school WHERE name IN ('上海市黄浦区巨鹿路第一小学','上海市静安区第二中心小学')")
for x in q.fetchall(): print(x)
c.close()
