import csv
import json
import time
from pathlib import Path
from urllib.parse import urlencode

import requests


BASE_URL = "https://api.shangnaxue.net/cis/api/ma/collectionSchool/pageApi"
OUTPUT_DIR = Path(r"C:\Users\Admin\Desktop\小初高预约")
PAGE_SIZE = 10
REQUEST_TIMEOUT = 20
REQUEST_INTERVAL = 0.3

BASE_PARAMS = {
    "size": PAGE_SIZE,
    "collectionId": "1479328255638360065",
    "areaId": "",
    "latitude": "31.18825912475586",
    "longitude": "121.4368667602539",
    "openid": "oOQI75G_I4ibpJoPR_pE2vzgDHXI",
}


def extract_records(payload):
    """兼容常见分页响应结构，返回当前页学校记录。"""
    if not isinstance(payload, dict):
        return []
    candidates = [payload.get("data"), payload.get("result"), payload]
    for value in candidates:
        if isinstance(value, list):
            return value
        if isinstance(value, dict):
            for key in ("records", "list", "rows", "items", "data"):
                records = value.get(key)
                if isinstance(records, list):
                    return records
    return []


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    session = requests.Session()
    session.headers.update({
        "Accept": "application/json, text/plain, */*",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36",
    })

    all_records = []
    page = 1
    while True:
        params = dict(BASE_PARAMS)
        params["current"] = page
        print(f"正在请求第 {page} 页...")
        try:
            response = session.get(BASE_URL, params=params, timeout=REQUEST_TIMEOUT)
            response.raise_for_status()
            payload = response.json()
        except (requests.RequestException, ValueError) as exc:
            print(f"第 {page} 页请求失败，停止：{exc}")
            break

        records = extract_records(payload)
        if not records:
            print(f"第 {page} 页没有数据，停止。")
            break

        page_file = OUTPUT_DIR / f"page_{page:04d}.json"
        page_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        all_records.extend(records)
        print(f"第 {page} 页获取 {len(records)} 条，累计 {len(all_records)} 条")

        if len(records) < PAGE_SIZE:
            print("当前页少于 page size，已到最后一页。")
            break
        page += 1
        time.sleep(REQUEST_INTERVAL)

    (OUTPUT_DIR / "all_records.json").write_text(
        json.dumps(all_records, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    if all_records:
        fields = sorted({key for item in all_records if isinstance(item, dict) for key in item})
        with (OUTPUT_DIR / "all_records.csv").open("w", encoding="utf-8-sig", newline="") as file:
            writer = csv.DictWriter(file, fieldnames=fields, extrasaction="ignore")
            writer.writeheader()
            writer.writerows(item for item in all_records if isinstance(item, dict))

    summary = {
        "pages": page if all_records else 0,
        "records": len(all_records),
        "output_dir": str(OUTPUT_DIR),
    }
    (OUTPUT_DIR / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
