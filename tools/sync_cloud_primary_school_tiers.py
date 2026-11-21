import argparse
import json
import os
import re
from difflib import SequenceMatcher
from pathlib import Path

import openpyxl
import pymysql


cloud_config = Path(__file__).parents[1] / "src" / "main" / "resources" / "application-cloud.yml"
cloud_config_text = cloud_config.read_text(encoding="utf-8")
password_match = re.search(r"password:\s*\$\{DB_PASSWORD:([^}]+)}", cloud_config_text)

DB = {
    "host": "sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com",
    "port": 29693,
    "user": "lianpeng",
    "password": os.environ.get("DB_PASSWORD") or (password_match.group(1) if password_match else ""),
    "database": "cloud1-d6g28bq6c04919547",
}

DISTRICTS = {
    "黄浦": "黄浦区", "徐汇": "徐汇区", "长宁": "长宁区", "静安": "静安区",
    "普陀": "普陀区", "虹口": "虹口区", "杨浦": "杨浦区", "闵行": "闵行区",
    "宝山": "宝山区", "嘉定": "嘉定区", "浦东": "浦东新区", "金山": "金山区",
    "松江": "松江区", "青浦": "青浦区", "奉贤": "奉贤区", "崇明": "崇明区",
}


def norm(value, district=""):
    value = str(value or "").strip()
    value = re.sub(r"（[^）]*）|\([^)]*\)", "", value)
    value = value.removeprefix("上海市")
    value = value.replace("附属", "附").replace("第一", "一").replace("第二", "二")
    value = value.replace("第三", "三").replace("第四", "四").replace("第五", "五")
    value = re.sub(r"[·、，,。；;：:\-—\s]", "", value)
    value = value.replace("小学部", "小学")
    return value


def aliases(school):
    district = school["district"]
    name = school["name"] or ""
    without_city = name.removeprefix("上海市")
    values = [name]
    if without_city.startswith(district):
        values.append(without_city[len(district):])
    values += re.split(r"[、，,；;/]", school.get("short_name") or "")
    return {norm(v, district) for v in values if norm(v, district)}


def parse_entries(path, school_type):
    ws = openpyxl.load_workbook(path, read_only=True, data_only=True)["小学"]
    entries = []
    current_district = None
    for row in ws.iter_rows(min_row=2, values_only=True):
        district_key = str(row[0] or "").strip()
        if district_key in DISTRICTS:
            current_district = DISTRICTS[district_key]
        if not current_district:
            continue
        for tier, cell in enumerate(row[2:5], 1):
            text = str(cell or "").strip()
            if not text:
                continue
            prefix = "公办" if school_type == "公办" else "民办"
            if not re.match(rf"^{prefix}\s*[：:]", text):
                continue
            # Public entries are on the district heading row; private entries are
            # on the following dedicated row.
            if school_type == "公办" and district_key not in DISTRICTS:
                continue
            text = re.sub(rf"^{prefix}\s*[：:]\s*", "", text)
            # Cells in the district row are the public-school row. Parenthetical notes
            # and trailing score/ranking numbers are descriptive rather than names.
            text = re.sub(r"（[^）]*）|\([^)]*\)", "", text)
            text = re.sub(r"(?<=\D)\d+(?:\s*[-—]\s*\d+)?", "", text)
            parts = [p.strip() for p in re.split(r"[、，,；;\n]+", text) if p.strip()]
            for part in parts:
                part = re.sub(r"^(?:公办|民办)\s*[：:]\s*", "", part).strip()
                part = re.sub(r"等$", "", part).strip()
                if part:
                    entries.append({"district": current_district, "tier": tier, "excel_name": part})
    return entries


def score(entry, school):
    target = norm(entry["excel_name"], entry["district"])
    best = 0.0
    for alias in aliases(school):
        if not target or not alias:
            continue
        if target == alias:
            return 1.0
        if target in alias or alias in target:
            best = max(best, 0.91 + 0.08 * min(len(target), len(alias)) / max(len(target), len(alias)))
        best = max(best, SequenceMatcher(None, target, alias).ratio())
    return best


def build_matches(entries, schools):
    matches, review = {}, []
    for school in schools:
        ranked = sorted(
            ((score(entry, school), entry) for entry in entries if entry["district"] == school["district"]),
            key=lambda item: -item[0],
        )
        top = ranked[0] if ranked else (0.0, None)
        second = ranked[1][0] if len(ranked) > 1 else 0.0
        if top[1] and top[0] >= 0.91 and (top[0] - second >= 0.025 or top[0] == 1.0):
            matches[school["id"]] = {**top[1], "school": school, "score": top[0]}
        elif top[0] >= 0.70:
            review.append({
                "reason": "cloud_school_needs_review",
                "school": school,
                "candidates": [{**entry, "score": round(sc, 3)} for sc, entry in ranked[:3]],
            })
    return matches, review


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("xlsx", type=Path)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--school-type", choices=("public", "private"), default="public")
    parser.add_argument("--manual", type=Path, help="JSON map of Excel name to primary_school id")
    args = parser.parse_args()

    conn = pymysql.connect(**DB, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor, autocommit=False)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id,name,short_name,district,school_type,tier FROM primary_school ORDER BY district,name,id")
            all_schools = cur.fetchall()
        school_type = "公办" if args.school_type == "public" else "民办"
        selected = [s for s in all_schools if s["school_type"] == school_type]
        entries = parse_entries(args.xlsx, school_type)
        matches, review = build_matches(entries, selected)

        if args.manual:
            manual = json.loads(args.manual.read_text(encoding="utf-8"))
            by_id = {s["id"]: s for s in selected}
            for school_id_text, tier in manual.items():
                school_id = int(school_id_text)
                school = by_id.get(school_id)
                if not school or tier not in (1, 2, 3, None):
                    raise RuntimeError(f"Invalid manual mapping {school_id} -> {tier}")
                review = [item for item in review if item["school"]["id"] != school_id]
                if tier is None:
                    matches.pop(school_id, None)
                else:
                    matches[school_id] = {"district": school["district"], "tier": tier, "excel_name": "人工确认", "school": school, "score": "manual"}

        summary = {
            "excel_entries": len(entries),
            "cloud_selected_schools": len(selected),
            "matched_cloud_schools": len(matches),
            "cloud_schools_needing_review": len(review),
            "result_tiers": {str(t): sum(1 for m in matches.values() if m["tier"] == t) for t in (1, 2, 3)},
            "cloud_selected_set_null": len(selected) - len(matches),
            "other_type_untouched": len(all_schools) - len(selected),
        }
        print(json.dumps(summary, ensure_ascii=True))
        for item in review:
            print(json.dumps(item, ensure_ascii=True, default=str))

        if args.apply:
            if review:
                raise RuntimeError("Cloud-school review items remain; refusing to update cloud database")
            with conn.cursor() as cur:
                cur.execute("UPDATE primary_school SET tier=NULL WHERE school_type=%s", (school_type,))
                cur.executemany(
                    "UPDATE primary_school SET tier=%s WHERE id=%s AND school_type=%s",
                    [(f'{m["tier"]}梯队'.replace("1", "一").replace("2", "二").replace("3", "三"), sid, school_type) for sid, m in matches.items()],
                )
                cur.execute("SELECT tier,COUNT(*) n FROM primary_school WHERE school_type=%s GROUP BY tier ORDER BY tier", (school_type,))
                result = cur.fetchall()
                if sum(row["n"] for row in result if row["tier"] is not None) != len(matches):
                    raise RuntimeError("Post-update count mismatch")
            conn.commit()
            print(json.dumps({"applied": True, "selected_counts": result}, ensure_ascii=True))
        else:
            conn.rollback()
    finally:
        conn.close()


if __name__ == "__main__":
    main()
