import openpyxl, pymysql, re

XLSX=r'C:\Users\Admin\Desktop\二三梯队学校\二三梯队小学_已完善.xlsx'
CFG=dict(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)

w=openpyxl.load_workbook(XLSX,data_only=True)
rows=[r for r in w['Sheet1'].iter_rows(min_row=2,values_only=True) if r[0]]
conn=pymysql.connect(**CFG); cur=conn.cursor(pymysql.cursors.DictCursor)
cur.execute('SELECT id,name FROM primary_school')
ids={r['name']:r['id'] for r in cur.fetchall()}
if any(r[0] not in ids for r in rows): raise RuntimeError('存在未匹配学校，已停止写入')

mapping={3:'founded_year',4:'phone',5:'fees_standard',6:'faculty_strength',7:'features_courses',8:'foreign_situation',9:'competition_result',10:'enrollment_year',11:'enrollment_target',12:'enrollment_planning',13:'enrollment_way',14:'is_bus'}
changed=0; field_counts={}; examples=[]
try:
    for r in rows:
        sets=[]; vals=[]
        for idx,col in mapping.items():
            v=r[idx]
            if v is not None and str(v).strip()!='':
                if col=='founded_year': v=str(v).strip()[:20]
                sets.append(f'`{col}`=%s'); vals.append(str(v).strip() if not isinstance(v,(int,float)) else v)
                field_counts[col]=field_counts.get(col,0)+1
        coord=str(r[15]).strip() if r[15] is not None else ''
        m=re.match(r'^\s*([-+]?\d+(?:\.\d+)?)\s*,\s*([-+]?\d+(?:\.\d+)?)\s*$',coord)
        if m:
            sets += ['latitude=%s','longitude=%s']; vals += [float(m.group(1)),float(m.group(2))]
            field_counts['latitude/longitude']=field_counts.get('latitude/longitude',0)+1
        if not sets: continue
        sets.append('source_update_time=NOW()')
        vals.append(ids[r[0]])
        cur.execute(f"UPDATE primary_school SET {', '.join(sets)} WHERE id=%s", vals)
        changed += cur.rowcount
        if len(examples)<5: examples.append((r[0],cur.rowcount))
    conn.commit()
    print({'source_rows':len(rows),'updated_rows':changed,'field_nonempty_counts':field_counts,'examples':examples})
finally:
    conn.close()
