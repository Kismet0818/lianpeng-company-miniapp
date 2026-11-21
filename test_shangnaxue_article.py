import argparse
import json
import sys

import requests


URL = "https://api.shangnaxue.net/cms/article/getArticle"


def main():
    parser = argparse.ArgumentParser(description="模拟微信小程序 getArticle 请求")
    parser.add_argument("--article-id", default="2041437822956646402")
    parser.add_argument("--openid", default="oOQI75G_I4ibpJoPR_pE2vzgDHXI")
    parser.add_argument("--latitude", default="31.18825912475586")
    parser.add_argument("--longitude", default="121.4368667602539")
    parser.add_argument("--signature", required=True, help="抓包中的 x-signature")
    parser.add_argument("--nonce", required=True, help="抓包中的 x-nonce")
    parser.add_argument("--output", default="article_response.json")
    args = parser.parse_args()

    params = {
        "articleId": args.article_id,
        "openid": args.openid,
        "latitude": args.latitude,
        "longitude": args.longitude,
    }
    headers = {
        "x-origin": "021705",
        "x-signature": args.signature,
        "x-nonce": args.nonce,
        "Content-Type": "application/json",
        "Accept": "*/*",
        "Referer": "https://servicewechat.com/wx00be946c856c1077/229/page-frame.html",
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36 "
            "MicroMessenger/7.0.20.1781 MiniProgramEnv/Windows"
        ),
    }

    try:
        response = requests.get(URL, params=params, headers=headers, timeout=20)
    except requests.RequestException as exc:
        print(f"请求失败：{exc}", file=sys.stderr)
        return 1

    print("最终 URL:", response.url)
    print("HTTP 状态:", response.status_code)
    print("Content-Type:", response.headers.get("content-type"))
    print("响应长度:", len(response.content))
    print("响应头:", dict(response.headers))

    try:
        body = response.json()
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(body, f, ensure_ascii=False, indent=2)
        print(f"JSON 已保存到：{args.output}")
        print(json.dumps(body, ensure_ascii=False, indent=2)[:4000])
    except ValueError:
        print("原始响应:", repr(response.text[:4000]))
    return 0 if response.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
