import pymysql
conn = pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com', port=29693, user='lianpeng', password='Datou870263', database='cloud1-d6g28bq6c04919547', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor, connect_timeout=30)
cur = conn.cursor()
cur.execute("SHOW COLUMNS FROM general_high_school")
for row in cur.fetchall():
    print(row['Field'], '|', row['Type'], '|', row['Null'], '|', row['Default'])
conn.close()