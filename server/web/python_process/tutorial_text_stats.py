#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SSF 프레임워크 Java ↔ Python 연동 튜토리얼 — 텍스트 통계 분석 샘플.

호출 규약 (tutorial_echo.py 와 동일):
  python tutorial_text_stats.py --request <req.json> --response <res.json>

요청 JSON 예:
  {
    "text":          "분석할 원문 텍스트 ...",
    "topN":          5,       // 선택, 기본 5 — 가장 자주 쓰인 단어 N개
    "minWordLength": 2        // 선택, 기본 1 — N자 이상 단어만 집계
  }

응답 JSON:
  {
    "result":            "OK",
    "msg":               "text analyzed",
    "counts": {
      "characters":         전체 문자 수,
      "charactersNoSpaces": 공백 제외 문자 수,
      "words":              단어 수,
      "uniqueWords":        중복 제거 단어 수,
      "sentences":          문장 수,
      "lines":              줄 수
    },
    "averageWordLength": 평균 단어 길이 (소수 둘째 자리),
    "topWords":          [ { "word": "...", "count": N }, ... ]
  }

파이썬 표준 라이브러리(re, collections)만 사용하므로 별도 설치 불필요.
한글/영문/숫자를 모두 단어로 인식한다.
"""

import argparse
import json
import re
import sys
import traceback
from collections import Counter


# 단어: 영문/숫자/한글(가-힣)의 연속 — 구두점과 공백을 구분자로 사용
WORD_PATTERN = re.compile(r"[A-Za-z0-9가-힣]+", re.UNICODE)

# 문장 종결: 영문(. ! ?) + 한문/한국 문장부호(。！？)
SENTENCE_PATTERN = re.compile(r"[.!?。！？]+")


def analyze(text: str, top_n: int = 5, min_word_length: int = 1) -> dict:
    """문자열을 받아 통계 dict 로 반환."""

    characters           = len(text)
    characters_no_spaces = len(re.sub(r"\s", "", text))
    lines                = (text.count("\n") + 1) if text else 0

    # 단어 추출 — 영문은 대소문자 구분 없이 집계
    words = [w.lower() for w in WORD_PATTERN.findall(text)]
    words = [w for w in words if len(w) >= min_word_length]

    unique_words = set(words)

    # 문장 — 종결부호로 분리 후 비어있지 않은 것만
    sentences = len([s for s in SENTENCE_PATTERN.split(text) if s.strip()])

    avg_word_length = (sum(len(w) for w in words) / len(words)) if words else 0.0

    top_words = [
        {"word": w, "count": c}
        for w, c in Counter(words).most_common(max(0, top_n))
    ]

    return {
        "result": "OK",
        "msg":    "text analyzed",
        "counts": {
            "characters":         characters,
            "charactersNoSpaces": characters_no_spaces,
            "words":              len(words),
            "uniqueWords":        len(unique_words),
            "sentences":          sentences,
            "lines":              lines,
        },
        "averageWordLength": round(avg_word_length, 2),
        "topWords":          top_words,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--request",  required=True, help="Java 가 기록한 요청 JSON 파일 경로")
    parser.add_argument("--response", required=True, help="Python 이 기록할 응답 JSON 파일 경로")
    args = parser.parse_args()

    with open(args.request, encoding="utf-8") as f:
        req = json.load(f)

    text            = req.get("text", "") or ""
    top_n           = int(req.get("topN", 5))
    min_word_length = int(req.get("minWordLength", 1))

    res = analyze(text, top_n, min_word_length)

    with open(args.response, "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    # 예외 발생 시에도 Java 쪽이 읽을 수 있도록 응답 파일에 에러 JSON 을 남긴다.
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
