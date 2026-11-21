import argparse
import json
import os
import re
from difflib import SequenceMatcher

import pymysql
import xlrd


DISTRICTS = {
    "黄浦": "黄浦区", "徐汇": "徐汇区", "长宁": "长宁区", "静安": "静安区",
    "普陀": "普陀区", "虹口": "虹口区", "杨浦": "杨浦区", "闵行": "闵行区",
    "宝山": "宝山区", "嘉定": "嘉定区", "浦东": "浦东新区", "金山": "金山区",
    "松江": "松江区", "青浦": "青浦区", "奉贤": "奉贤区", "崇明": "崇明区",
}

MANUAL_IDS = {
    ("黄浦区", "蓬莱路二小"): [100026],
    ("黄浦区", "徽宁路三小"): [100032],
    ("黄浦区", "复兴东路三小"): [100031],
    ("徐汇区", "高安路一小（宛平路校区、华展东路校区）"): [100007],
    ("徐汇区", "东安路二小"): [100018],
    ("长宁区", "愚园路一小"): [100106],
    ("普陀区", "华东师大附小"): [100125],
    ("普陀区", "中山北路一小"): [100130],
    ("虹口区", "虹口四中心"): [100119],
    ("虹口区", "虹口三中心"): [100118],
    ("杨浦区", "控江二小"): [100085],
    ("杨浦区", "打虎山路一小"): [100094],
    ("杨浦区", "齐齐哈尔路一小"): [100098],
    ("闵行区", "七宝明强小学"): [100001, 100002],
    ("闵行区", "闵行区实验小学"): [100003, 100040, 100046],
    ("浦东新区", "福山外国语小学\n（福山、瑞华、花园）"): [100055],
    ("浦东新区", "浦东第二中心小学"): [100061],
    ("浦东新区", "梧桐小学（樱花校区、金桥校区）"): [100066],
    ("浦东新区", "浦明师范附小"): [100060],
    ("浦东新区", "进才实验小学"): [100058],
    ("浦东新区", "建平实验小学"): [100065],
    ("浦东新区", "张江高科实验小学（藿香校区、香楠校区）"): [100070],
}


def normalized(value, district=None):
    if not value:
        return ""
    value = str(value).strip()
    value = re.sub(r"[（(].*?[）)]", "", value)
    value = value.replace("上海市", "").replace("上海", "")
    if district:
        value = value.replace(district, "").replace(district.removesuffix("区"), "")
    value = value.replace("附属", "附").replace("实验学校小学部", "实验学校")
    value = value.replace("第一", "一").replace("第二", "二")
    value = re.sub(r"[·•、，,；;\-\s]", "", value)
    value = re.sub(r"(?:小学|学校)$", "", value)
    return value


def read_excel(path):
    sheet = xlrd.open_workbook(path).sheet_by_index(0)
    rows = []
    current_district = None
    for row_index in range(sheet.nrows):
        row = sheet.row_values(row_index)
        if row[0] and str(row[0]).endswith("区"):
            current_district = str(row[0]).strip()
            continue
        if row[0] in DISTRICTS:
            current_district = DISTRICTS[row[0]]
        tier = row[2]
        if current_district and tier in (1.0, 2.0) and row[3]:
            rows.append((current_district, int(tier), str(row[3]).strip()))
    return rows


def aliases(school, district):
    values = [school["name"]]
    values.extend(re.split(r"[、，,；;/]", school["short_name"] or ""))
    return {normalized(value, district) for value in values if value}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("xls")
    parser.add_argument("--password", default=os.environ.get("DB_PASSWORD"))
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if not args.password:
        parser.error("请通过 DB_PASSWORD 环境变量或 --password 提供本地数据库密码")
    connection = pymysql.connect(host="127.0.0.1", port=3306, user="root", password=args.password,
                                 database="lpsx", charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor)
    with connection.cursor() as cursor:
        cursor.execute("SELECT id,name,short_name,district,school_type FROM primary_school ORDER BY district,name")
        schools = cursor.fetchall()
    excel_rows = read_excel(args.xls)
    assignments = {}
    unmatched = []
    for district, tier, excel_name in excel_rows:
        candidates = [s for s in schools if s["district"] == district and s["school_type"] == "公办"]
        target = normalized(excel_name, district)
        manual = MANUAL_IDS.get((district, excel_name))
        exact = [s for s in candidates if target in aliases(s, district)]
        ids = manual or ([s["id"] for s in exact] if exact else [])
        if ids:
            for school_id in ids:
                old_tier = assignments.setdefault(school_id, tier)
                if old_tier != tier:
                    raise RuntimeError(f"ID {school_id} matched conflicting tiers")
            continue
        ranked = sorted(candidates, key=lambda s: max(
            SequenceMatcher(None, target, normalized(s["name"], district)).ratio(),
            SequenceMatcher(None, target, normalized(s["short_name"], district)).ratio()), reverse=True)
        unmatched.append((district, tier, excel_name, [(s["id"], s["name"], round(max(
            SequenceMatcher(None, target, normalized(s["name"], district)).ratio(),
            SequenceMatcher(None, target, normalized(s["short_name"], district)).ratio()), 3)) for s in ranked[:3]]))
    print(f"excel={len(excel_rows)} assigned_rows={len(assignments)} unmatched_excel={len(unmatched)}")
    for row in unmatched:
        print(json.dumps(row, ensure_ascii=True))
    if args.apply:
        with connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='primary_school' AND column_name='tier'")
            if cursor.fetchone()["COUNT(*)"] == 0:
                cursor.execute("ALTER TABLE primary_school ADD COLUMN tier varchar(10) DEFAULT NULL COMMENT '学校梯队：一梯队/二梯队；不在名单中为NULL' AFTER school_type, ADD KEY idx_primary_school_tier (tier)")
            cursor.execute("UPDATE primary_school SET tier=NULL")
            cursor.executemany("UPDATE primary_school SET tier=%s WHERE id=%s", [
                ("一梯队" if tier == 1 else "二梯队", school_id) for school_id, tier in assignments.items()
            ])
        connection.commit()
        print(f"APPLIED updated={len(assignments)} null={len(schools) - len(assignments)}")


if __name__ == "__main__":
    main()
