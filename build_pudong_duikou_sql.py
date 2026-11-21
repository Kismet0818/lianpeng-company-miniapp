import os, openpyxl
from collections import OrderedDict

src = os.environ['SOURCE_XLSX']
wb = openpyxl.load_workbook(src, read_only=True, data_only=True)
ws = wb.worksheets[0]

groups = OrderedDict()
for row in ws.iter_rows(min_row=3, max_col=7, values_only=True):
    school = row[2]
    area = row[3]
    town = row[4]
    if not school or not town or not area:
        continue
    groups.setdefault(str(school).strip(), OrderedDict()).setdefault(str(town).strip(), []).append(str(area).strip())

def esc(v):
    return "'" + str(v).replace('\\', '\\\\').replace("'", "''") + "'"

sql = ["START TRANSACTION;", "DELETE FROM junior_high_primary_duikou WHERE district='浦东新区' AND admission_year=2026;"]
for school, towns in groups.items():
    parts=[]
    for town, areas in towns.items():
        uniq=[]
        for a in areas:
            if a not in uniq: uniq.append(a)
        parts.append(town + '（' + '、'.join(uniq) + '）')
    scope='；'.join(parts)
    note='来源文件：2026年浦东新区招生合集.xlsx；工作表：中学对口地段；按学校及所属街镇汇总'
    sql.append(
        "INSERT INTO junior_high_primary_duikou "
        "(duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) "
        "SELECT 18,2026,'浦东新区',j.id,NULL,'详见中学对口地段','area_based','按地段划片'," + esc(scope) + "," + esc(note) + "," + str(len(sql)-1) + ",1 "
        "FROM junior_high_school j WHERE j.name=" + esc(school) + " AND j.district='浦东新区' AND NOT EXISTS (SELECT 1 FROM junior_high_primary_duikou d WHERE d.admission_year=2026 AND d.district='浦东新区' AND d.junior_high_school_id=j.id);"
    )
sql.append('COMMIT;')
open('pudong_duikou_update.sql','w',encoding='utf-8').write('\n'.join(sql))
open('pudong_duikou_groups.txt','w',encoding='utf-8').write('\n'.join(f'{k}\t{len(v)}' for k,v in groups.items()))
print(f'schools={len(groups)} statements={len(sql)}')
