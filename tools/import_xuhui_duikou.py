import os
import re
from pathlib import Path

import pandas as pd
import pymysql

DB = {
    "host": "sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com",
    "port": 29693,
    "user": "lianpeng",
    "password": os.environ["MYSQL_PWD"],
    "database": "cloud1-d6g28bq6c04919547",
    "charset": "utf8mb4",
    "autocommit": False,
}
DATA_DIR = Path(r"C:\Users\Admin\Desktop\资料合集\2026年招生合集-全(3)\2026年招生合集-全\2026年各区招生合集")
FILE = next(DATA_DIR.glob("*徐汇区招生合集.xlsx"))
SHEET = "徐汇区公办初中入学方式"

def clean(v):
    return re.sub(r"\s+", "", str(v or "")).replace("\n", "").replace("\r", "")

def main():
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id,name,short_name FROM junior_high_school WHERE district=%s AND school_type LIKE %s", ("徐汇区", "%公办%"))
            juniors = cur.fetchall()
            cur.execute("SELECT id,name,short_name FROM primary_school WHERE district=%s AND school_type LIKE %s", ("徐汇区", "%公办%"))
            primaries = cur.fetchall()
            cur.execute("SELECT id,title FROM duikou_material WHERE status=1")
            materials = cur.fetchall()
            material_id = next((mid for mid,title in materials if "徐汇" in str(title)), None)

            # 只替换徐汇区2026数据，其他区不受影响。
            cur.execute("DELETE FROM junior_high_primary_duikou WHERE district=%s AND admission_year=%s", ("徐汇区", 2026))
            insert = """INSERT IGNORE INTO junior_high_primary_duikou
              (duikou_material_id,admission_year,district,junior_high_school_id,
               primary_school_id,primary_school_name,relation_type,admission_method,
               scope_text,note,sort_order,status)
              VALUES (%s,2026,'徐汇区',%s,%s,%s,%s,%s,%s,%s,%s,1)"""

            df = pd.read_excel(FILE, sheet_name=SHEET, header=None, dtype=str).fillna("")
            inserted = 0
            unmatched_junior = []
            for ridx, row in df.iterrows():
                no = clean(row.iloc[0]) if len(row) else ""
                if not no.isdigit() or not (1 <= int(no) <= 24):
                    continue
                line = [clean(x) for x in row.tolist()]
                junior_cell = line[1] if len(line) > 1 else ""
                junior_hits = []
                for sid, name, short in juniors:
                    full, abbr = clean(name), clean(short)
                    if (full and full in junior_cell) or (abbr and len(abbr) >= 3 and abbr in junior_cell):
                        junior_hits.append((sid, name))
                # 当前行只有一个初中；若简称命中，保留数据库中的标准名称作为关联对象。
                if not junior_hits:
                    unmatched_junior.append((int(no), junior_cell))
                    continue
                junior_id, junior_name = junior_hits[0]
                primary_text = " | ".join(x for x in line[2:] if x)
                primary_hits = []
                for pid, name, short in primaries:
                    full, abbr = clean(name), clean(short)
                    if (full and full in primary_text) or (abbr and len(abbr) >= 3 and abbr in primary_text):
                        if pid not in [x[0] for x in primary_hits]:
                            primary_hits.append((pid, name))
                relation = "lottery" if int(no) <= 9 else "direct"
                method = "电脑派位" if relation == "lottery" else "对口入学"
                note = f"来源文件：{FILE.name}；工作表：{SHEET}；第{ridx+1}行；公办初中项目{no}"
                if primary_hits:
                    for order, (pid, pname) in enumerate(primary_hits):
                        cur.execute(insert, (material_id, junior_id, pid, pname, relation, method, primary_text or None, note, int(no)*100+order))
                        inserted += cur.rowcount
                else:
                    # 没有小学ID的派位/范围项目也保留原文，不伪造小学名称。
                    raw_name = "电脑派位/招生范围见原文" if relation == "lottery" else "招生范围见原文"
                    cur.execute(insert, (material_id, junior_id, None, raw_name, relation, method, primary_text or junior_cell, note, int(no)*100))
                    inserted += cur.rowcount
            conn.commit()
            cur.execute("SELECT COUNT(*),COUNT(DISTINCT junior_high_school_id),COUNT(DISTINCT primary_school_id),SUM(primary_school_id IS NULL) FROM junior_high_primary_duikou WHERE district=%s AND admission_year=2026", ("徐汇区",))
            print("FILE", FILE.name)
            print("MATERIAL_ID", material_id)
            print("INSERTED_AND_TOTAL", inserted, cur.fetchone())
            print("UNMATCHED_JUNIOR", unmatched_junior)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    main()
