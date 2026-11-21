"""分页爬取学校数据并保存为 Excel。

安装依赖：pip install requests pandas openpyxl
运行：python 爬取学校数据.py
"""

from pathlib import Path
import time

import pandas as pd
import requests


API_URL = "https://api.shangnaxue.net/cis/api/ma/school/list"
OUTPUT_FILE = Path("学校数据.xlsx")

# 如果 openid 过期，请替换成浏览器请求中最新的值。
BASE_PARAMS = {
    "size": 20,
    "name": "",
    "cityId": 101,
    "longitude": 121.4368667602539,
    "latitude": 31.18825912475586,
    "openid": "oOQI75G_I4ibpJoPR_pE2vzgDHXI",
    "isSysAppointment": 0,
    "isSysMidway": 0,
    "typeId": "",
    "areaId": "",
    "segmentGradeId": 4,
    "natureId": "",
}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36"
    ),
    "Accept": "application/json, text/plain, */*",
    "Referer": "https://www.shangnaxue.net/",
}


def fetch_all_schools() -> list[dict]:
    session = requests.Session()
    session.headers.update(HEADERS)
    all_rows: list[dict] = []
    current = 1

    while True:
        params = {**BASE_PARAMS, "current": current}
        print(f"正在请求第 {current} 页...")

        try:
            response = session.get(API_URL, params=params, timeout=20)
            response.raise_for_status()
            payload = response.json()
        except requests.RequestException as exc:
            raise RuntimeError(f"第 {current} 页请求失败：{exc}") from exc
        except ValueError as exc:
            raise RuntimeError(f"第 {current} 页返回的不是有效 JSON") from exc

        if payload.get("code") not in (0, "0"):
            raise RuntimeError(
                f"接口返回错误：code={payload.get('code')}, msg={payload.get('msg')}"
            )

        records = ((payload.get("data") or {}).get("records") or [])
        if not records:
            print("没有更多数据，停止分页。")
            break

        all_rows.extend(records)
        print(f"本页 {len(records)} 条，累计 {len(all_rows)} 条")
        current += 1
        time.sleep(0.3)  # 降低连续请求频率

    return all_rows


def save_to_excel(records: list[dict]) -> None:
    rows = [
        {
            "学校名称": item.get("name", ""),
            "图片": item.get("schoolPic", ""),
            "地区": item.get("areaName", ""),
        }
        for item in records
    ]

    df = pd.DataFrame(rows, columns=["学校名称", "图片", "地区"])
    with pd.ExcelWriter(OUTPUT_FILE, engine="openpyxl") as writer:
        df.to_excel(writer, index=False, sheet_name="学校数据")
        worksheet = writer.book["学校数据"]
        worksheet.freeze_panes = "A2"
        worksheet.auto_filter.ref = worksheet.dimensions
        worksheet.column_dimensions["A"].width = 42
        worksheet.column_dimensions["B"].width = 65
        worksheet.column_dimensions["C"].width = 18

    print(f"已保存 {len(df)} 条数据：{OUTPUT_FILE.resolve()}")


if __name__ == "__main__":
    save_to_excel(fetch_all_schools())
