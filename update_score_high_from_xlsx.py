import openpyxl,pymysql,re
XLSX=r'C:\Users\Admin\Desktop\二三梯队学校\二三梯队高中_信息补充.xlsx'
CFG=dict(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
w=openpyxl.load_workbook(XLSX,data_only=True); rows=[r for r in w['Sheet1'].iter_rows(min_row=2,values_only=True) if r[0]]
c=pymysql.connect(**CFG); q=c.cursor(pymysql.cursors.DictCursor); q.execute('SELECT id,name FROM score_query_tool_school'); ids={x['name']:x['id'] for x in q.fetchall()}
missing=[r[0] for r in rows if r[0] not in ids]
if missing: raise RuntimeError('存在未匹配学校: '+','.join(missing[:10]))
mapping={1:'founded_year',2:'phone',3:'fees_standard',4:'faculty_strength',5:'features_courses',6:'foreign_situation',7:'competition_result',8:'enrollment_year',9:'enrollment_target',10:'enrollment_planning',11:'enrollment_way',12:'is_bus'}
limits={'founded_year':20,'fees_standard':100,'enrollment_way':255,'is_bus':50,'phone':255}
field_counts={}; changed=0
try:
  for r in rows:
    sets=[]; vals=[]
    for idx,col in mapping.items():
      v=r[idx]
      if v is not None and str(v).strip()!='':
        v=str(v).strip() if not isinstance(v,(int,float)) else v
        if col in limits: v=str(v)[:limits[col]]
        sets.append(f'`{col}`=%s'); vals.append(v); field_counts[col]=field_counts.get(col,0)+1
    coord=str(r[13]).strip() if r[13] is not None else ''
    m=re.match(r'^\s*([-+]?\d+(?:\.\d+)?)\s*,\s*([-+]?\d+(?:\.\d+)?)\s*$',coord)
    if m:
      sets += ['latitude=%s','longitude=%s']; vals += [float(m.group(1)),float(m.group(2))]; field_counts['latitude/longitude']=field_counts.get('latitude/longitude',0)+1
    if sets:
      sets.append('source_update_time=NOW()'); vals.append(ids[r[0]])
      q.execute(f"UPDATE score_query_tool_school SET {', '.join(sets)} WHERE id=%s",vals); changed += q.rowcount
  c.commit(); print({'source_rows':len(rows),'updated_rows':changed,'field_nonempty_counts':field_counts})
finally: c.close()
