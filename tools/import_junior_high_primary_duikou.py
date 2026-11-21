import os
import re
from pathlib import Path

import pandas as pd
import pymysql

DB = {
    "host": "sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com",
    "port": 29693,
    "user": "lianpeng",
    "password": os.environ.get("MYSQL_PWD", ""),
    "database": "cloud1-d6g28bq6c04919547",
    "charset": "utf8mb4",
    "autocommit": False,
}

DATA_DIR = Path(r"C:\Users\Admin\Desktop\资料合集\2026年招生合集-全(3)\2026年招生合集-全\2026年各区招生合集")

DDL = r'''
CREATE TABLE IF NOT EXISTS `junior_high_primary_duikou` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `duikou_material_id` int DEFAULT NULL COMMENT '对口资料ID，对应duikou_material.id',
  `admission_year` int NOT NULL COMMENT '招生年度',
  `district` varchar(50) NOT NULL COMMENT '所属区',
  `junior_high_school_id` int NOT NULL COMMENT '公办初中ID',
  `primary_school_id` int DEFAULT NULL COMMENT '对口小学ID',
  `primary_school_name` varchar(150) NOT NULL COMMENT '对口小学名称',
  `relation_type` varchar(30) NOT NULL DEFAULT 'direct' COMMENT 'direct明确对口，area_based按地段，lottery电脑派位，overall统筹，unknown待核实',
  `admission_method` varchar(100) DEFAULT NULL COMMENT '入学方式',
  `scope_text` text COMMENT '招生范围原文',
  `note` text COMMENT '备注',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_duikou_material_id` (`duikou_material_id`),
  KEY `idx_junior_year_status` (`junior_high_school_id`,`admission_year`,`status`),
  KEY `idx_primary_year_status` (`primary_school_id`,`admission_year`,`status`),
  KEY `idx_district_year_status` (`district`,`admission_year`,`status`),
  UNIQUE KEY `uk_junior_primary_year` (`admission_year`,`junior_high_school_id`,`primary_school_id`,`primary_school_name`),
  CONSTRAINT `fk_duikou_material` FOREIGN KEY (`duikou_material_id`) REFERENCES `duikou_material` (`id`),
  CONSTRAINT `fk_duikou_junior_high` FOREIGN KEY (`junior_high_school_id`) REFERENCES `junior_high_school` (`id`),
  CONSTRAINT `fk_duikou_primary_school` FOREIGN KEY (`primary_school_id`) REFERENCES `primary_school` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公办初中对口小学关系表';
'''

DISTRICTS = {
    "奉贤": "奉贤区", "嘉定": "嘉定区", "宝山": "宝山区", "崇明": "崇明区",
    "普陀": "普陀区", "浦东": "浦东新区", "虹口": "虹口区", "长宁": "长宁区",
    "闵行": "闵行区", "静安": "静安区", "黄浦": "黄浦区", "徐汇": "徐汇区",
    "杨浦": "杨浦区", "金山": "金山区", "青浦": "青浦区",
}

def norm(s):
    return re.sub(r"\s+", "", str(s or "")).replace("\n", "").replace("\r", "")

def read_book(path):
    engine = "xlrd" if path.suffix.lower() == ".xls" else "openpyxl"
    return pd.ExcelFile(path, engine=engine)

def main():
    if not DB["password"]:
        raise RuntimeError("MYSQL_PWD is required")
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute(DDL)
            cur.execute("SELECT id, name, short_name, district FROM junior_high_school WHERE school_type LIKE %s", ("%公办%",))
            juniors = cur.fetchall()
            cur.execute("SELECT id, name, short_name, district FROM primary_school WHERE school_type LIKE %s", ("%公办%",))
            primaries = cur.fetchall()
            cur.execute("SELECT id, title FROM duikou_material WHERE status=1")
            materials = cur.fetchall()

            # 按区建立学校名称索引，完整名称优先，简称仅作补充匹配。
            def names(rows):
                out = {}
                for sid, name, short, district in rows:
                    for n in (name, short):
                        n = norm(n)
                        if len(n) >= 3:
                            out.setdefault((district, n), (sid, name))
                return out
            jmap, pmap = names(juniors), names(primaries)

            cur.execute("DELETE FROM junior_high_primary_duikou WHERE admission_year=2026")
            insert_sql = """INSERT IGNORE INTO junior_high_primary_duikou
                (duikou_material_id, admission_year, district, junior_high_school_id,
                 primary_school_id, primary_school_name, relation_type, admission_method,
                 scope_text, note, sort_order, status)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1)"""
            total = 0
            by_district = {}
            for path in sorted(DATA_DIR.iterdir()):
                if path.suffix.lower() not in (".xls", ".xlsx"):
                    continue
                district = next((v for k, v in DISTRICTS.items() if k in path.name), None)
                if not district:
                    continue
                material_id = next((mid for mid, title in materials if district.replace("区", "") in str(title) or district in str(title)), None)
                try:
                    xl = read_book(path)
                except Exception as exc:
                    print("SKIP_BOOK", path.name, exc)
                    continue
                for sheet in xl.sheet_names:
                    title = str(sheet)
                    if not re.search(r"初中|中学", title) or not re.search(r"对口|划片|片区|入学|招生", title):
                        continue
                    try:
                        df = pd.read_excel(path, sheet_name=sheet, header=None, dtype=str, engine=xl.engine).fillna("")
                    except Exception as exc:
                        print("SKIP_SHEET", path.name, sheet, exc)
                        continue
                    # 处理 Excel 合并单元格造成的上下行空白。
                    df = df.replace({"nan": ""}).ffill(axis=0)
                    for row_no, row in df.iterrows():
                        cells = [norm(x) for x in row.tolist() if norm(x)]
                        line = " ".join(cells)
                        if not line or re.search(r"合计|总计|说明：|注：", line):
                            continue
                        js = [(sid, nm) for (d, n), (sid, nm) in jmap.items() if d == district and n in line and n == norm(nm)]
                        ps = [(pid, nm) for (d, n), (pid, nm) in pmap.items() if d == district and n in line and n == norm(nm)]
                        if not js:
                            # 允许表中是学校简称，但仅限精确简称匹配。
                            js = [(sid, nm) for (d, n), (sid, nm) in jmap.items() if d == district and n in line]
                        if not js:
                            continue
                        relation = "lottery" if re.search(r"电脑派位|随机派位", line) else ("area_based" if re.search(r"居委会|小区|道路|门牌|地段|片区", line) else "direct")
                        method = "电脑派位" if relation == "lottery" else ("按地段划片" if relation == "area_based" else "对口入学")
                        scope = line if relation == "area_based" else None
                        for jid, _ in js:
                            if ps:
                                for pid, pnm in ps:
                                    cur.execute(insert_sql, (material_id, 2026, district, jid, pid, pnm, relation, method, scope, f"来源文件：{path.name}；工作表：{sheet}；第{row_no+1}行", row_no+1))
                                    total += cur.rowcount
                            elif re.search(r"小学", line):
                                # 保留无法关联到 primary_school 的官方名称，避免漏项。
                                raw = next((x for x in cells if "小学" in x), line[:150])[:150]
                                cur.execute(insert_sql, (material_id, 2026, district, jid, None, raw, "unknown", method, scope, f"来源文件：{path.name}；工作表：{sheet}；第{row_no+1}行", row_no+1))
                                total += cur.rowcount
                        by_district[district] = by_district.get(district, 0) + 1
            conn.commit()
            cur.execute("SELECT COUNT(*) FROM junior_high_primary_duikou WHERE admission_year=2026")
            print("IMPORTED", total, "ROWS", cur.fetchone()[0])
            print("DISTRICT_ROWS", by_district)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    main()
