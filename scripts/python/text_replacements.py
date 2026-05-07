#!/usr/bin/env python3
"""Check or apply canonical text replacements in Markdown and JSON files."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


REPLACEMENTS = {
    "\u2014": "-",
    "\u2013": "-",
    "\u00ab": '"',
    "\u00bb": '"',
    "\u2026": "...",
    "\u2018": "'",
    "\u2019": "'",
    "\u2022": "-",
    "\u25cf": "-",
}

EXCLUDED_DIRS = {
    ".git",
    ".husky",
    ".idea",
    ".mvn",
    ".nuxt",
    ".output",
    ".venv",
    ".vscode/chrome-debug-profile",
    "__pycache__",
    "coverage",
    "dist",
    "node_modules",
    "target",
}

EXCLUDED_FILES = {
    Path("frontend/config/lint-forbidden.json"),
}

TEXT_SUFFIXES = {".json", ".md"}


def is_excluded(path: Path) -> bool:
    parts = path.parts
    for index in range(len(parts)):
        if "/".join(parts[index : index + 2]) in EXCLUDED_DIRS:
            return True
    return any(part in EXCLUDED_DIRS for part in parts)


def iter_files(root: Path) -> list[Path]:
    return sorted(
        path
        for path in root.rglob("*")
        if path.is_file()
        and path.suffix.lower() in TEXT_SUFFIXES
        and not is_excluded(path.relative_to(root))
        and path.relative_to(root) not in EXCLUDED_FILES
    )


def normalize(content: str) -> tuple[str, dict[str, int]]:
    counts: dict[str, int] = {}
    updated = content
    for source, target in REPLACEMENTS.items():
        count = updated.count(source)
        if count:
            counts[source] = count
            updated = updated.replace(source, target)
    return updated, counts


def normalize_json_value(value: object) -> tuple[object, dict[str, int]]:
    if isinstance(value, str):
        return normalize(value)
    if isinstance(value, list):
        normalized_items = []
        merged: dict[str, int] = {}
        for item in value:
            normalized, counts = normalize_json_value(item)
            normalized_items.append(normalized)
            for char, count in counts.items():
                merged[char] = merged.get(char, 0) + count
        return normalized_items, merged
    if isinstance(value, dict):
        normalized_dict = {}
        merged: dict[str, int] = {}
        for key, item in value.items():
            normalized_key, key_counts = normalize(key)
            normalized_item, item_counts = normalize_json_value(item)
            normalized_dict[normalized_key] = normalized_item
            for counts in (key_counts, item_counts):
                for char, count in counts.items():
                    merged[char] = merged.get(char, 0) + count
        return normalized_dict, merged
    return value, {}


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check or apply canonical replacements in Markdown and JSON files."
    )
    parser.add_argument("--root", default=".", help="Repository root to scan.")
    parser.add_argument("--fix", action="store_true", help="Rewrite files in place.")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    failures = 0

    for path in iter_files(root):
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        if path.suffix.lower() == ".json":
            try:
                data = json.loads(content)
                normalized_data, counts = normalize_json_value(data)
                updated = json.dumps(normalized_data, ensure_ascii=False, indent=2) + "\n"
            except json.JSONDecodeError:
                updated, counts = normalize(content)
        else:
            updated, counts = normalize(content)

        if not counts:
            continue

        relative = path.relative_to(root)
        if args.fix:
            if path.suffix.lower() == ".json":
                try:
                    json.loads(updated)
                except json.JSONDecodeError as exc:
                    print(f"ERR: {relative}: replacement would break JSON: {exc}", file=sys.stderr)
                    failures += 1
                    continue
            path.write_text(updated, encoding="utf-8")
            print(f"FIXED: {relative}")
        else:
            detail = ", ".join(f"{repr(char)}={count}" for char, count in counts.items())
            print(f"ERR: {relative}: unsupported characters found ({detail})")
            failures += 1

    if failures:
        print("Run scripts/python/text_replacements.py --fix to rewrite affected files.")
        return 1

    print("OK: Markdown and JSON text replacements are normalized.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
