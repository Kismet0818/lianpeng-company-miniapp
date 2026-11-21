import argparse
import json
import os
import re
from difflib import SequenceMatcher
from pathlib import Path

import openpyxl
import pymysql


ROOT = Path(__file__).parents[1]
CLOUD_CONFIG = (ROOT / "src/main/resources/application-cloud.yml").read_text(encoding="utf-8")
PASSWORD_MATCH = re.search(r"password:\s*\$\{DB_PASSWORD:([^}]+)}", CLOUD_CONFIG)
DB = {
    "host": "sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com",
    "port": 29693,
    "user": "lianpeng",
    "password": os.environ.get("DB_PASSWORD") or (PASSWORD_MATCH.group(1) if PASSWORD_MATCH else ""),
    "database": "cloud1-d6g28bq6c04919547",
}

DISTRICTS = {
    "黄浦": "黄浦区", "徐汇": "徐汇区", "长宁": "长宁区", "静安": "静安区",
    "普陀": "普陀区", "虹口": "虹口区", "杨浦": "杨浦区", "闵行": "闵行区",
    "宝山": "宝山区", "嘉定": "嘉定区", "浦东": "浦东新区", "金山": "金山区",
    "松江": "松江区", "青浦": "青浦区", "奉贤": "奉贤区", "崇明": "崇明区",
}
TIER_COLUMNS = ((1, 1), (2, 2), (3, 3), (9, 4))


def normalize(value):
    value = str(value or "").strip()
    value = re.sub(r"（[^）]*）|\([^)]*\)", "", value)
    value = value.removeprefix("上海市").removeprefix("上海")
    value = value.replace("附属", "附").replace("第一", "一").replace("第二", "二")
    value = value.replace("第三", "三").replace("第四", "四").replace("实验性", "实验")
    value = re.sub(r"[·、，,。；;：:\-—\s]", "", value)
    return value


def school_aliases(school):
    name = school["name"] or ""
    district = school["district"]
    no_city = name.removeprefix("上海市").removeprefix("上海")
    values = [name]
    if no_city.startswith(district):
        values.append(no_city[len(district):])
    values.extend(re.split(r"[、，,；;/]", school.get("short_name") or ""))
    return {normalize(v) for v in values if normalize(v)}


def parse_entries(path):
    ws = openpyxl.load_workbook(path, read_only=True, data_only=True).worksheets[1]
    entries = []
    current_district = None
    for row_number, row in enumerate(ws.iter_rows(min_row=2, values_only=True), 2):
        district_key = str(row[0] or "").strip()
        if district_key in DISTRICTS:
            current_district = DISTRICTS[district_key]
        if not current_district:
            continue
        for column, tier in TIER_COLUMNS:
            text = str(row[column] or "").strip()
            kind_match = re.match(r"^(公办|民办)\s*[：:]\s*", text)
            if not kind_match:
                continue
            school_type = kind_match.group(1) + "初中"
            text = text[kind_match.end():]
            text = re.sub(r"（[^）]*%[^）]*）|\([^)]*%[^)]*\)", "", text)
            text = re.sub(r"(?:等|等学校)\s*$", "", text.strip())
            for part in re.split(r"[、，,；;\n]+", text):
                part = part.strip(" 、，,；;。")
                if not part:
                    continue
                entries.append({
                    "district": current_district,
                    "school_type": school_type,
                    "tier": tier,
                    "excel_name": part,
                    "row": row_number,
                })
    return entries


def similarity(entry, school):
    target = normalize(entry["excel_name"])
    best = 0.0
    for alias in school_aliases(school):
        if target == alias:
            return 1.0
        if len(target) >= 3 and len(alias) >= 3 and (target in alias or alias in target):
            best = max(best, 0.91 + 0.08 * min(len(target), len(alias)) / max(len(target), len(alias)))
        best = max(best, SequenceMatcher(None, target, alias).ratio())
    return best


def propose(entries, schools):
    matches = {}
    review = []
    for school in schools:
        candidates = [e for e in entries if e["district"] == school["district"] and e["school_type"] == school["school_type"]]
        ranked = sorted(((similarity(e, school), e) for e in candidates), key=lambda x: -x[0])
        top = ranked[0] if ranked else (0.0, None)
        second = ranked[1][0] if len(ranked) > 1 else 0.0
        competing_tiers = {e["tier"] for sc, e in ranked if sc >= max(0.91, top[0] - 0.025)}
        if top[1] and top[0] >= 0.91 and len(competing_tiers) == 1 and (top[0] - second >= 0.02 or top[0] == 1.0):
            matches[school["id"]] = {**top[1], "school": school, "score": top[0]}
        elif top[0] >= 0.65:
            review.append({
                "school": school,
                "candidates": [{**e, "score": round(sc, 3)} for sc, e in ranked[:5]],
            })
    return matches, review


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("xlsx", type=Path)
    parser.add_argument("--manual", type=Path)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    conn = pymysql.connect(**DB, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor, autocommit=False)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) n FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='junior_high_school' AND column_name='tier'")
            has_tier = cur.fetchone()["n"] == 1
            fields = "id,name,short_name,district,school_type" + (",tier" if has_tier else "")
            cur.execute(f"SELECT {fields} FROM junior_high_school ORDER BY district,name,id")
            schools = cur.fetchall()
        entries = parse_entries(args.xlsx)
        matches, review = propose(entries, schools)

        if args.manual:
            manual = json.loads(args.manual.read_text(encoding="utf-8"))
            by_id = {s["id"]: s for s in schools}
            for school_id_text, tier in manual.items():
                school_id = int(school_id_text)
                if school_id not in by_id or tier not in (1, 2, 3, 4, None):
                    raise RuntimeError(f"Invalid manual mapping {school_id} -> {tier}")
                review = [x for x in review if x["school"]["id"] != school_id]
                if tier is None:
                    matches.pop(school_id, None)
                else:
                    school = by_id[school_id]
                    matches[school_id] = {"district": school["district"], "school_type": school["school_type"], "tier": tier, "excel_name": "人工确认", "row": 0, "school": school, "score": "manual"}

        print(json.dumps({
            "excel_entries": len(entries), "cloud_schools": len(schools), "matched": len(matches),
            "review": len(review), "set_null": len(schools) - len(matches),
            "tiers": {str(t): sum(m["tier"] == t for m in matches.values()) for t in (1, 2, 3, 4)},
            "public_matched": sum(m["school"]["school_type"] == "公办初中" for m in matches.values()),
            "private_matched": sum(m["school"]["school_type"] == "民办初中" for m in matches.values()),
        }, ensure_ascii=True))
        for item in review:
            print(json.dumps(item, ensure_ascii=True, default=str))

        if args.apply:
            if review:
                raise RuntimeError("Review items remain; refusing cloud update")
            with conn.cursor() as cur:
                if not has_tier:
                    cur.execute("ALTER TABLE junior_high_school ADD COLUMN tier varchar(10) DEFAULT NULL COMMENT '学校梯队：一梯队/二梯队/三梯队/四梯队；不在名单中为NULL' AFTER school_type, ADD KEY idx_junior_school_tier (tier)")
                cur.execute("UPDATE junior_high_school SET tier=NULL")
                labels = {1: "一梯队", 2: "二梯队", 3: "三梯队", 4: "四梯队"}
                cur.executemany("UPDATE junior_high_school SET tier=%s WHERE id=%s", [(labels[m["tier"]], sid) for sid, m in matches.items()])
                cur.execute("SELECT tier,COUNT(*) n FROM junior_high_school GROUP BY tier ORDER BY tier")
                counts = cur.fetchall()
                if sum(x["n"] for x in counts if x["tier"] is not None) != len(matches):
                    raise RuntimeError("Post-update count mismatch")
            conn.commit()
            print(json.dumps({"applied": True, "counts": counts}, ensure_ascii=True))
        else:
            conn.rollback()
    finally:
        conn.close()


if __name__ == "__main__":
    main()
