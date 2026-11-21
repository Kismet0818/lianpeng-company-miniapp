"""用小学详情抓包补全云端 primary_school；默认仅预览，--apply 才提交。"""
import argparse
import datetime as dt
import json
import re
from difflib import SequenceMatcher
from pathlib import Path

import pymysql

from sync_cloud_primary_school_tiers import DB

ALLOWED_DISTRICTS = {
    "黄浦": "黄浦区", "徐汇": "徐汇区", "长宁": "长宁区", "静安": "静安区",
    "普陀": "普陀区", "虹口": "虹口区", "杨浦": "杨浦区", "闵行": "闵行区",
    "宝山": "宝山区", "嘉定": "嘉定区", "浦东": "浦东新区", "金山": "金山区",
    "松江": "松江区", "青浦": "青浦区", "奉贤": "奉贤区", "崇明": "崇明区",
}
FIELDS = (
    "short_name", "background_image_url", "address", "description", "official_website",
    "teaching_features", "founded_year", "phone", "fees_standard", "faculty_strength",
    "features_courses", "foreign_situation", "competition_result", "enrollment_year",
    "enrollment_target", "enrollment_planning", "enrollment_way", "is_bus", "boarding_info",
    "latitude", "longitude", "source_update_time", "tuition_min", "tuition_max",
)


def extract_json(path):
    raw = path.read_bytes()
    # 抓包导出的响应可能保留 HTTP chunked framing；不能只删除首尾标记，
    # 因为较大的响应会在 JSON 中间插入多个十六进制分块长度。
    first_end = raw.find(b"\r\n")
    if first_end > 0 and re.fullmatch(rb"[0-9a-fA-F]+", raw[:first_end]):
        chunks, pos = [], 0
        try:
            while True:
                line_end = raw.index(b"\r\n", pos)
                size = int(raw[pos:line_end].split(b";", 1)[0], 16)
                pos = line_end + 2
                if size == 0:
                    break
                chunks.append(raw[pos:pos + size])
                pos += size
                if raw[pos:pos + 2] != b"\r\n":
                    raise ValueError("invalid chunk delimiter")
                pos += 2
            raw = b"".join(chunks)
        except (ValueError, IndexError):
            pass
    text = raw.decode("utf-8-sig")
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        raise ValueError(f"{path.name}: 找不到 JSON 主体")
    root = json.loads(text[start:end + 1])
    if root.get("code") != 0 or not root.get("ok") or not isinstance(root.get("data"), dict):
        raise ValueError(f"{path.name}: 接口响应异常")
    return root["data"]


def norm(value):
    value = str(value or "").strip().removeprefix("上海市")
    value = value.replace("（", "(").replace("）", ")")
    value = value.replace("第一", "一").replace("第二", "二").replace("第三", "三")
    value = value.replace("第四", "四").replace("第五", "五").replace("附属", "附")
    return re.sub(r"[\s·、，,。；;：:\-—()（）]", "", value)


def variants(value, district=""):
    value = str(value or "").strip()
    if not value:
        return set()
    result = {norm(value), norm(re.sub(r"[（(][^）)]*[）)]", "", value))}
    for prefix in ("上海市", district, district.removesuffix("区"), "浦东新区", "浦东"):
        if prefix and value.startswith(prefix):
            result.add(norm(value[len(prefix):]))
    return {x for x in result if x}


def source_aliases(row):
    values = [row.get("name"), row.get("usedName")]
    values += re.split(r"[;；,，、/]", row.get("abbreviation") or "")
    out = set()
    for value in values:
        out |= variants(value, ALLOWED_DISTRICTS.get(row.get("areaName"), ""))
    return out


def db_aliases(row):
    values = [row.get("name")]
    values += re.split(r"[;；,，、/]", row.get("short_name") or "")
    out = set()
    for value in values:
        out |= variants(value, row["district"])
    return out


def similarity(source, school):
    # 完整名称优先于简称；同一集团/学校的不同校区常共享简称。
    source_full = norm(source.get("name"))
    school_full = norm(school.get("name"))
    if source_full and school_full:
        if source_full == school_full:
            return 1.1
        if min(len(source_full), len(school_full)) >= 6 and (source_full in school_full or school_full in source_full):
            return 1.02 + .07 * min(len(source_full), len(school_full)) / max(len(source_full), len(school_full))
    best = 0.0
    for left in source_aliases(source):
        for right in db_aliases(school):
            if left == right:
                return 1.0
            if min(len(left), len(right)) >= 4 and (left in right or right in left):
                best = max(best, .92 + .07 * min(len(left), len(right)) / max(len(left), len(right)))
            best = max(best, SequenceMatcher(None, left, right).ratio())
    return best


def clean(value):
    if value is None:
        return None
    if isinstance(value, str):
        value = value.strip()
        return value or None
    return value


def mapped_values(row):
    enrollment = row.get("enrollmentInfo") or {}
    founded = clean(row.get("foundedTime"))
    total_fee = row.get("totalFee")
    annual_tuition = round(float(total_fee) * 2 / 10000, 2) if total_fee not in (None, "") else None
    aliases = [x.strip() for x in re.split(r"[;；]", row.get("abbreviation") or "") if x.strip()]
    return {
        "short_name": ";".join(aliases) or None,
        "background_image_url": clean(row.get("schoolPic")),
        "address": clean(row.get("address")),
        "description": clean(row.get("characteristic")),
        "official_website": clean(row.get("officialUrl")),
        "teaching_features": clean(row.get("foundedFeatures")),
        "founded_year": founded[:4] if founded else None,
        "phone": clean(row.get("phone")),
        "fees_standard": clean(row.get("feesStandard")),
        "faculty_strength": clean(row.get("facultyStrength")),
        "features_courses": clean(row.get("featuresCourses")),
        "foreign_situation": clean(row.get("foreignSituation")),
        "competition_result": clean(row.get("competitionResult")),
        "enrollment_year": enrollment.get("year"),
        "enrollment_target": clean(enrollment.get("enrollmentTarget")),
        "enrollment_planning": clean(enrollment.get("enrollmentPlanning")),
        "enrollment_way": clean(enrollment.get("enrollmentWay")),
        "is_bus": clean(row.get("isBus")),
        "boarding_info": clean(row.get("isLodging")),
        "latitude": row.get("latitude"),
        "longitude": row.get("longitude"),
        "source_update_time": clean(row.get("updateTime")),
        "tuition_min": annual_tuition,
        "tuition_max": annual_tuition,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("folder", type=Path)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--report", type=Path, default=Path("tmp/primary-detail-match-report.json"))
    parser.add_argument("--exclude-district", action="append", default=[], help="跳过已处理区，例如徐汇区")
    args = parser.parse_args()

    files = sorted(args.folder.glob("*.json"), key=lambda p: int(re.sub(r"\D", "", p.stem)))
    parsed = [(path, extract_json(path)) for path in files]
    invalid = [(p.name, r.get("areaName")) for p, r in parsed if r.get("areaName") not in ALLOWED_DISTRICTS]
    if invalid:
        raise RuntimeError(f"存在非目标区数据: {invalid}")
    excluded = set(args.exclude_district)
    parsed = [(p, r) for p, r in parsed if ALLOWED_DISTRICTS[r["areaName"]] not in excluded]
    unique = {}
    duplicates = []
    for path, row in parsed:
        key = str(row.get("schoolId"))
        if key in unique:
            duplicates.append({"file": path.name, "same_as": unique[key][0].name, "school": row.get("name")})
            if mapped_values(row) != mapped_values(unique[key][1]):
                raise RuntimeError(f"重复来源内容不一致: {unique[key][0].name}, {path.name}")
        else:
            unique[key] = (path, row)

    conn = pymysql.connect(**DB, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor, autocommit=False)
    try:
        with conn.cursor() as cur:
            cur.execute("SHOW COLUMNS FROM primary_school")
            columns = {x["Field"] for x in cur.fetchall()}
            missing = set(FIELDS) - columns
            if missing:
                raise RuntimeError(f"云端表缺少字段: {sorted(missing)}")
            placeholders = ",".join(["%s"] * len(set(ALLOWED_DISTRICTS.values()) - excluded))
            target_districts = sorted(set(ALLOWED_DISTRICTS.values()) - excluded)
            cur.execute(f"SELECT * FROM primary_school WHERE district IN ({placeholders}) ORDER BY district,name,id", target_districts)
            schools = cur.fetchall()
            cur.execute("SELECT COUNT(*) n FROM primary_school")
            count_before = cur.fetchone()["n"]

        matches, problems, used_ids = [], [], set()
        for source_id, (path, source) in unique.items():
            district = ALLOWED_DISTRICTS[source["areaName"]]
            ranked = sorted(((similarity(source, s), s) for s in schools if s["district"] == district), key=lambda x: -x[0])
            top_score, top = ranked[0]
            second_score = ranked[1][0] if len(ranked) > 1 else 0
            item = {"file": path.name, "source_id": source_id, "source_name": source.get("name"), "district": district,
                    "match_id": top["id"], "match_name": top["name"], "score": round(top_score, 4),
                    "second_name": ranked[1][1]["name"] if len(ranked) > 1 else None, "second_score": round(second_score, 4)}
            if top_score < .88 or (top_score < 1 and top_score - second_score < .06) or top["id"] in used_ids:
                problems.append(item)
            else:
                used_ids.add(top["id"])
                item["values"] = mapped_values(source)
                matches.append(item)

        report = {"mode": "apply" if args.apply else "preview", "source_files": len(files), "selected_source_files": len(parsed),
                  "excluded_districts": sorted(excluded), "unique_sources": len(unique),
                  "duplicates": duplicates, "target_db_rows": len(schools), "table_rows_before": count_before,
                  "matches": matches, "problems": problems}
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
        print(json.dumps({k: report[k] for k in ("mode", "source_files", "unique_sources", "target_db_rows", "table_rows_before")}, ensure_ascii=False))
        for m in matches:
            print(f"MATCH {m['file']} {m['source_name']} => #{m['match_id']} {m['match_name']} score={m['score']}")
        for p in problems:
            print(f"REVIEW {p}")
        if problems:
            raise RuntimeError(f"仍有 {len(problems)} 个匹配需要确认，拒绝更新")

        if not args.apply:
            conn.rollback()
            print(f"PREVIEW ONLY report={args.report}")
            return

        stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
        backup = Path("tmp") / f"primary-school-before-detail-update-{stamp}.json"
        backup_rows = [next(s for s in schools if s["id"] == m["match_id"]) for m in matches]
        backup.write_text(json.dumps(backup_rows, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
        # 简称、背景图、官网和学费保留云端已有值，仅在缺失时补齐；其余详情以新数据补全，
        # 但来源为 null/空串时不清空云端原值。
        fill_only_text = {"short_name", "background_image_url", "official_website"}
        fill_only_number = {"tuition_min", "tuition_max"}
        assignments = ",".join(
            f"{f}=COALESCE(NULLIF({f},''),%s)" if f in fill_only_text
            else f"{f}=COALESCE({f},%s)" if f in fill_only_number
            else f"{f}=COALESCE(%s,{f})"
            for f in FIELDS
        )
        sql = f"UPDATE primary_school SET {assignments} WHERE id=%s AND district=%s"
        with conn.cursor() as cur:
            for m in matches:
                vals = m["values"]
                cur.execute(sql, [vals[f] for f in FIELDS] + [m["match_id"], m["district"]])
                if cur.rowcount not in (0, 1):
                    raise RuntimeError(f"更新行数异常: {m['source_name']} rowcount={cur.rowcount}")
            cur.execute("SELECT COUNT(*) n FROM primary_school")
            count_after = cur.fetchone()["n"]
        if count_after != count_before:
            raise RuntimeError(f"总行数发生变化: {count_before} -> {count_after}")
        conn.commit()
        print(f"APPLIED updated={len(matches)} rows={count_after} backup={backup}")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
