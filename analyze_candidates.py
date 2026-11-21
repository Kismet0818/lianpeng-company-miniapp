import json, re
from pathlib import Path
from openpyxl import load_workbook

BOOK = Path(r"C:\Users\Admin\Downloads\待完善特色班数据_初中635所.xlsx")
SQL = Path(r"C:\Users\Admin\Desktop\lpsx\tmp\release-2.0-backup-20260909-1340\migration\30-new-table-data.sql")
OUT = Path(r"C:\Users\Admin\Desktop\lpsx\.codex_work")
OUT.mkdir(exist_ok=True)

def split_mysql_tuple(s):
    vals, buf, quoted, esc = [], [], False, False
    for ch in s:
        if esc:
            buf.append(ch)
            esc = False
        elif ch == "\\" and quoted:
            esc = True
        elif ch == "'":
            quoted = not quoted
        elif ch == "," and not quoted:
            vals.append("".join(buf).strip())
            buf = []
        else:
            buf.append(ch)
    vals.append("".join(buf).strip())
    return [None if v.upper() == "NULL" else v for v in vals]

db = {}
for line in SQL.read_text(encoding="utf-8").splitlines():
    if not line.startswith("INSERT  IGNORE INTO `junior_high_school`"):
        continue
    m = re.search(r"VALUES\s*\((.*)\);\s*$", line)
    if not m:
        continue
    v = split_mysql_tuple(m.group(1))
    if len(v) >= 17:
        db[int(v[0])] = {"id": int(v[0]), "name": v[1], "description": v[10] or "", "official": v[13] or "", "teaching": v[15] or ""}

wb = load_workbook(BOOK, read_only=True, data_only=False)
ws = wb[wb.sheetnames[0]]
headers = [c.value for c in next(ws.iter_rows(min_row=1, max_row=1))]
schools=[]
for row in ws.iter_rows(min_row=2, values_only=True):
    schools.append(dict(zip(headers,row)))

black = {"教学班","班级","小班","走班","平行班","行政班","初中班","毕业班","课堂","上课","下课","补差班","培优班"}
pat = re.compile(r"([一-鿿A-Za-z0-9·&+—\-]{1,14}班)")
for s in schools:
    d = db.get(int(s["school_id"])) if s.get("school_id") else None
    text = ((d or {}).get("description","") + "\n" + (d or {}).get("teaching", ""))
    c=[]
    contexts=[]
    for bm in re.finditer("班", text):
        contexts.append(text[max(0,bm.start()-35):min(len(text),bm.start()+45)].replace("\n"," "))
    for x in pat.findall(text):
        x=x.strip("，。；：、 ")
        if x not in black and not x.endswith("班级") and 2 <= len(x) <= 15:
            if any(k in x for k in ("特色","实验","创新","双语","精英","菁英","理科","数理","竞赛","融合","英语","小语种","艺术","沪剧","评弹","足球","桥牌","科创","养辉","强基","直升","国际","课程","基础班","拓展班")):
                c.append(x)
    s["db_name"]=(d or {}).get("name")
    s["official"]=(d or {}).get("official","")
    s["candidates"]=list(dict.fromkeys(c))
    s["db_teaching"]=(d or {}).get("teaching","")
    s["db_description"]=(d or {}).get("description","")
    s["class_contexts"]=contexts

(OUT/"schools_candidates.json").write_text(json.dumps(schools,ensure_ascii=False,indent=2),encoding="utf-8")
print(json.dumps({"schools":len(schools),"db_matches":sum(bool(x['db_name']) for x in schools),"candidate_schools":sum(bool(x['candidates']) for x in schools),"candidate_count":sum(len(x['candidates']) for x in schools)},ensure_ascii=False))
for x in schools:
    if x['candidates']:
        print(x['school_id'],x['学校全称'],x['candidates'])
(OUT/"class_contexts.txt").write_text("\n".join(f"{x['school_id']}\t{x['学校全称']}\t"+" || ".join(x['class_contexts']) for x in schools if x['class_contexts']),encoding="utf-8")
