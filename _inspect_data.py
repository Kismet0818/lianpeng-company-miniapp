import pymysql
conn = pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com', port=29693, user='lianpeng', password='Datou870263', database='cloud1-d6g28bq6c04919547', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor, connect_timeout=30)
cur = conn.cursor()
cur.execute("SELECT id, name, short_name, district, school_type FROM general_high_school ORDER BY district, id")
rows = cur.fetchall()
print("TOTAL", len(rows))
from collections import OrderedDict
by_dist = OrderedDict()
for r in rows:
    by_dist.setdefault(r['district'], []).append((r['id'], r['name'], r['short_name'], r['school_type']))
for d, items in by_dist.items():
    print("=== "+d+" ("+str(len(items))+") ===")
    for i,n,s,t in items:
        print(f"  {i}|{t}|{n}|{s}")
conn.close()