# -*- coding: utf-8 -*-
import pymysql
try:
    c=pymysql.connect(host='192.168.3.5',port=3306,user='root',password='Datou123456!',database='lpsx',charset='utf8mb4',connect_timeout=8)
    cur=c.cursor()
    for t in ['score_query_tool_school','junior_high_school']:
        cur.execute(f"SHOW COLUMNS FROM {t} LIKE 'tier'")
        print(t, '->', cur.fetchone())
    cur.execute("SELECT COUNT(*) FROM score_query_tool_school")
    print('sqs total:', cur.fetchone()[0])
    c.close()
except Exception as e:
    print('ERR', e)