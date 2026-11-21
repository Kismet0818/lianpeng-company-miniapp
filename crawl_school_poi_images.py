"""抓取初高中学校的校园图片并导出 Excel。

用法：python crawl_school_poi_images.py
"""
from __future__ import annotations

import json
import re
import time
from pathlib import Path
from typing import Any

import requests
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.worksheet.table import Table, TableStyleInfo


ROOT = Path(r"C:\Users\Admin\Desktop")
SOURCE_DIRS = [ROOT / "初中详细数据", ROOT / "高中详细数据"]
OUTPUT_XLSX = ROOT / "初高中学校图片列表.xlsx"
API_URL = "https://api.shangnaxue.net/cis/api/ma/resource/poiDisplay"
PAGE_SIZE = 100


def read_text(path: Path) -> str:
    raw = path.read_bytes()
    for encoding in ("utf-8", "gb18030"):
        try:
            return raw.decode(encoding)
        except UnicodeDecodeError:
            pass
    return raw.decode("utf-8", errors="ignore")


def json_string(raw: str, key: str) -> str:
    match = re.search(r'"' + re.escape(key) + r'":"(.*?)"', raw, re.S)
    return match.group(1) if match else ""


def json_id(raw: str) -> str:
    match = re.search(r'"schoolId":"?(\d+)"?', raw)
    return match.group(1) if match else ""


def load_schools() -> list[dict[str, str]]:
    schools: dict[str, dict[str, str]] = {}
    for source_dir in SOURCE_DIRS:
        level = "初中" if source_dir.name == "初中详细数据" else "高中"
        for path in source_dir.glob("*/*.json"):
            raw = read_text(path)
            school_id = json_id(raw)
            name = json_string(raw, "name")
            if not school_id or not name:
                continue
            schools.setdefault(school_id, {
                "学校名称": name,
                "学校ID": school_id,
                "学段": level,
            })
    return sorted(schools.values(), key=lambda x: (x["学段"], x["学校名称"]))


def fetch_images(session: requests.Session, school_id: str) -> tuple[list[str], str]:
    images: list[str] = []
    seen: set[str] = set()
    current = 1
    while True:
        response = session.get(API_URL, params={"schoolId": school_id, "current": current, "size": PAGE_SIZE}, timeout=30)
        response.raise_for_status()
        payload: dict[str, Any] = response.json()
        if payload.get("code") != 0:
            return images, f"接口返回 code={payload.get('code')}"
        data = payload.get("data") or {}
        records = data.get("records") or []
        for record in records:
            url = (record.get("imageUrl") or "").strip()
            if url and url not in seen:
                seen.add(url)
                images.append(url)
        pages = int(data.get("pages") or 1)
        if current >= pages or not records:
            break
        current += 1
    return images, "成功"


def export_xlsx(rows: list[dict[str, str]]) -> None:
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "学校图片"
    headers = ["学校名称", "学校ID", "学段", "学校图片列表", "图片数量", "抓取状态"]
    sheet.append(headers)
    for row in rows:
        sheet.append([row.get(header, "") for header in headers])

    header_fill = PatternFill("solid", fgColor="D9EFFF")
    for cell in sheet[1]:
        cell.font = Font(bold=True, color="17324D")
        cell.fill = header_fill
        cell.alignment = Alignment(horizontal="center", vertical="center")
    for row in sheet.iter_rows(min_row=2):
        row[0].alignment = Alignment(vertical="top")
        row[1].alignment = Alignment(vertical="top")
        row[2].alignment = Alignment(horizontal="center", vertical="top")
        row[3].alignment = Alignment(wrap_text=True, vertical="top")
        row[4].alignment = Alignment(horizontal="center", vertical="top")
        row[5].alignment = Alignment(vertical="top")
    sheet.freeze_panes = "A2"
    sheet.auto_filter.ref = f"A1:F{sheet.max_row}"
    sheet.column_dimensions["A"].width = 34
    sheet.column_dimensions["B"].width = 23
    sheet.column_dimensions["C"].width = 10
    sheet.column_dimensions["D"].width = 80
    sheet.column_dimensions["E"].width = 10
    sheet.column_dimensions["F"].width = 16
    table = Table(displayName="SchoolImages", ref=f"A1:F{sheet.max_row}")
    table.tableStyleInfo = TableStyleInfo(name="TableStyleMedium2", showFirstColumn=False, showLastColumn=False, showRowStripes=True, showColumnStripes=False)
    sheet.add_table(table)
    workbook.save(OUTPUT_XLSX)


def main() -> None:
    schools = load_schools()
    session = requests.Session()
    session.headers.update({"Accept": "application/json", "User-Agent": "school-image-export/1.0"})
    rows: list[dict[str, str]] = []
    for index, school in enumerate(schools, start=1):
        try:
            images, status = fetch_images(session, school["学校ID"])
        except Exception as exc:  # keep one failed school from stopping the whole export
            images, status = [], f"失败：{type(exc).__name__}: {exc}"
        row = dict(school)
        row["学校图片列表"] = "\n".join(images)
        row["图片数量"] = str(len(images))
        row["抓取状态"] = status
        rows.append(row)
        print(f"[{index}/{len(schools)}] {school['学校名称']} -> {len(images)} 张，{status}")
        time.sleep(0.05)
    export_xlsx(rows)
    print(f"已导出：{OUTPUT_XLSX}")
    print(f"学校数：{len(rows)}，图片总数：{sum(int(r['图片数量']) for r in rows)}")


if __name__ == "__main__":
    main()
