#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SSF 프레임워크 Java ↔ Python 연동 튜토리얼 스크립트.

호출 규약:
  python tutorial_echo.py --request <req.json> --response <res.json>

요청 JSON 예:
  { "numbers": [1, 2, 3, 4, 5], "message": "hello" }

응답 JSON:
  {
    "result": "OK",
    "msg": "python called",
    "echo_message": "<message 원문>",
    "received_at": "YYYY-MM-DD HH:MM:SS",
    "python_version": "3.x.y",
    "stats": { "count": N, "sum": ..., "avg": ..., "min": ..., "max": ... }
  }

Java 호출부(com.ithows.util.PythonCallUtil)가 요청 JSON 파일을 만들어 전달하고,
이 스크립트는 해당 파일을 읽어 처리한 후 응답 JSON 파일을 만들어 리턴한다.
"""

import argparse
import json
import sys
import traceback
from datetime import datetime


def process(req: dict) -> dict:
    """실제 비즈니스 로직. 요청 dict → 응답 dict."""
    numbers = req.get("numbers") or []
    message = req.get("message", "")

    if numbers:
        stats = {
            "count": len(numbers),
            "sum":   sum(numbers),
            "avg":   sum(numbers) / len(numbers),
            "min":   min(numbers),
            "max":   max(numbers),
        }
    else:
        stats = {"count": 0, "sum": 0, "avg": 0, "min": None, "max": None}

    return {
        "result":         "OK",
        "msg":            "python called",
        "echo_message":   message,
        "received_at":    datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "python_version": sys.version.split()[0],
        "stats":          stats,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--request",  required=True, help="Java 가 기록한 요청 JSON 파일 경로")
    parser.add_argument("--response", required=True, help="Python 이 기록할 응답 JSON 파일 경로")
    args = parser.parse_args()

    with open(args.request, encoding="utf-8") as f:
        req = json.load(f)

    res = process(req)

    with open(args.response, "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    # --response 파일 경로는 에러가 나더라도 복구해서 기록해야 Java 쪽이 읽을 수 있으므로
    # argparse 실패 후에도 가능한 만큼 best-effort 로 응답 파일을 만든다.
    response_path = None
    if "--response" in sys.argv:
        try:
            response_path = sys.argv[sys.argv.index("--response") + 1]
        except Exception:
            response_path = None

    try:
        main()
    except Exception as e:
        err = {
            "result": "ERROR",
            "msg":    str(e),
            "trace":  traceback.format_exc(),
        }
        if response_path:
            try:
                with open(response_path, "w", encoding="utf-8") as f:
                    json.dump(err, f, ensure_ascii=False, indent=2)
            except Exception:
                pass
        sys.exit(1)
