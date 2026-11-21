"""抓取 anroader 学校列表并导出 Excel。

安装依赖：
    pip install requests pandas openpyxl

运行：
    python crawl_anroader_schools.py
"""

from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any

import pandas as pd
import requests


API_URL = "https://www.anroader.com/api/mini/list"
OUTPUT_FILE = Path(__file__).with_name("anroader_schools.xlsx")

# 接口字段 -> Excel 中文表头
FIELD_NAMES = {
    "id": "学校ID",
    "name": "学校名称",
    "simplaName": "学校简称",
    "address": "地址",
    "city": "城市",
    "area": "区域",
    "level": "办学层次",
    "nature": "办学性质",
    "phone": "联系电话",
    "principalName": "校长姓名",
    "web": "学校官网",
    "schoolWx": "学校微信",
    "schoolProfile": "学校简介",
    "dailySchedule": "日常作息",
    "hotProject": "特色项目",
    "remark": "备注",
    "established": "建校年份",
    "floorArea": "占地面积（万平方米）",
    "studyNum": "学生人数",
    "teacherNum": "教师人数",
    "tuition": "学费",
    "minScore": "最低分",
    "perScore": "人均分",
    "areaScore": "区域评分",
    "label": "标签",
    "labelValue": "标签值",
    "canteenFlag": "有食堂",
    "dormitoryFlag": "有住宿",
    "schoolPic": "学校图片",
    "schoolPicPrew": "学校图片预览",
    "promotionalImage": "宣传图片",
    "promotionalImagePrew": "宣传图片预览",
    "schoolQr": "学校二维码",
    "schoolQrImagePrew": "二维码图片预览",
    "viewNum": "浏览量",
    "isDeleted": "是否删除",
    "createDate": "创建时间",
    "createId": "创建人ID",
    "updateDate": "更新时间",
    "updateId": "更新人ID",
    "customerId": "客户ID",
    "myCollection": "是否收藏",
    "viewVipFlag": "VIP查看标记",
    "viewVipFreeDownload": "VIP免费下载标记",
    "admissionsPolicy": "招生政策",
    "enrollList": "招生列表",
    "graduateList": "毕业生列表",
    "houseList": "住宿列表",
    "schoolEnrollList": "学校招生列表",
    "teacherList": "教师列表",
    "schoolImgs": "学校图片列表",
    "schoolImgsPrew": "学校图片预览列表",
    "famousStudent": "知名校友",
    "zhiZhuLen": "字段值",
}


def excel_value(value: Any) -> Any:
    """将列表/字典转为可写入 Excel 的文本，其余值原样保留。"""
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False)
    return value


def fetch_all(session: requests.Session, size: int = 10) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    current = 1

    params = {
        "size": size,
        "name": "",
        "area": "",
        "level": "初中,九年一贯制,完全中学",
        "nature": "",
    }

    while True:
        params["current"] = current
        print(f"正在抓取第 {current} 页……")
        response = session.get(API_URL, params=params, timeout=30)
        response.raise_for_status()
        payload = response.json()

        if payload.get("code") != 200:
            raise RuntimeError(f"接口返回错误：{payload}")

        data = payload.get("data") or {}
        page_records = data.get("records") or []
        if not page_records:
            print("本页没有数据，停止抓取。")
            break

        records.extend(page_records)
        print(f"本页 {len(page_records)} 条，累计 {len(records)} 条")
        current += 1
        time.sleep(0.2)  # 降低请求频率，避免给接口造成压力

    return records


def save_excel(records: list[dict[str, Any]], output_file: Path) -> None:
    if not records:
        print("没有可导出的数据。")
        return

    # 先使用预设顺序，再追加接口未来新增的字段
    keys = list(FIELD_NAMES)
    for record in records:
        for key in record:
            if key not in keys:
                keys.append(key)

    rows = []
    for record in records:
        rows.append({
            FIELD_NAMES.get(key, key): excel_value(record.get(key))
            for key in keys
        })

    df = pd.DataFrame(rows)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        df.to_excel(writer, index=False, sheet_name="学校数据")
        worksheet = writer.book["学校数据"]
        worksheet.freeze_panes = "A2"
        worksheet.auto_filter.ref = worksheet.dimensions
        for column_cells in worksheet.columns:
            column_letter = column_cells[0].column_letter
            header = str(column_cells[0].value or "")
            max_length = max(len(header), *(len(str(cell.value or "")) for cell in column_cells[1:]))
            worksheet.column_dimensions[column_letter].width = min(max(max_length + 2, 12), 40)

    print(f"已导出 {len(records)} 条数据：{output_file.resolve()}")


def main() -> None:
    session = requests.Session()
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Referer": "https://www.anroader.com/",
    })
    records = fetch_all(session, size=10)
    save_excel(records, OUTPUT_FILE)


if __name__ == "__main__":
    main()
