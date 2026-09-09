#!/usr/bin/env python3
"""Inspect new recette log bytes for i18n, Java/server and SSR error signals."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATTERNS = {
    "i18n": re.compile(r"missing translation|missing locale|not found.*locale|\[intlify\]|i18n", re.I),
    "server": re.compile(r"\bERROR\b|Exception(?:\b|:)|ProblemDetail|HTTP/\S+\s+5\d\d|status[=: ]+5\d\d", re.I),
    "ssr": re.compile(r"hydration|nitro.*error|unhandled(?:rejection| promise)|window is not defined|document is not defined|SSR.*(?:fail|error)", re.I),
}
URL = re.compile(r"https?://[^\s\])}>]+")
SECRET = re.compile(r"(?i)(authorization|api[-_]?key|token|password|secret)(\s*[=:]\s*)(\S+)")


def safe_line(value: str) -> str:
    """Remove topology and credential-like values from evidence."""
    value = URL.sub("<url-redacted>", value.strip())
    return SECRET.sub(r"\1\2<redacted>", value)[:500]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", required=True)
    parser.add_argument("--offsets", type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{2,79}", args.run):
        parser.error("invalid run id")
    directory = ROOT / "artifacts" / "recette" / args.run
    offset_path = args.offsets or directory / "offsets.json"
    offsets = json.loads(offset_path.read_text(encoding="utf-8"))
    findings = []
    next_offsets = {}
    for name in ("front-api.log", "frontend-ssr.log"):
        path = directory / name
        start = int(offsets.get(name, 0))
        if not path.exists():
            next_offsets[name] = 0
            continue
        with path.open("rb") as handle:
            handle.seek(start)
            content = handle.read()
            next_offsets[name] = handle.tell()
        for line_number, line in enumerate(content.decode("utf-8", errors="replace").splitlines(), 1):
            categories = [category for category, pattern in PATTERNS.items() if pattern.search(line)]
            if categories:
                findings.append({
                    "log": name,
                    "relativeLine": line_number,
                    "categories": categories,
                    "message": safe_line(line),
                })
    result = {"run": args.run, "fromOffsets": offsets, "toOffsets": next_offsets, "findings": findings}
    target = directory / "log-audit.json"
    target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    (directory / "offsets.json").write_text(json.dumps(next_offsets) + "\n", encoding="utf-8")
    print(json.dumps({"findings": len(findings), "artifact": str(target)}))
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
