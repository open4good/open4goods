#!/usr/bin/env python3
"""Create and resume autonomous Nudger recette campaigns."""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import os
import re
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
TERMINAL = {"PASSED", "FAILED", "BLOCKED", "NOT_APPLICABLE"}
FIELDS = [
    "id", "wave", "surface", "mode", "locale", "path", "intent", "status", "attempts",
    "claimed_at", "finished_at", "note", "evidence",
]


def now() -> str:
    """Return a stable UTC timestamp."""
    return dt.datetime.now(dt.UTC).replace(microsecond=0).isoformat()


def validate_run_id(value: str) -> str:
    """Keep run ids path-safe."""
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{2,79}", value):
        raise argparse.ArgumentTypeError("run id must be 3-80 path-safe characters")
    return value


def run_dir(identifier: str) -> Path:
    return ROOT / "artifacts" / "recette" / identifier


def board_path(identifier: str) -> Path:
    return run_dir(identifier) / "board.csv"


def read_rows(identifier: str) -> list[dict[str, str]]:
    path = board_path(identifier)
    if not path.is_file():
        raise ValueError(f"campaign {identifier} does not exist; run resume first")
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def write_rows(identifier: str, rows: list[dict[str, str]]) -> None:
    directory = run_dir(identifier)
    directory.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix="board-", suffix=".csv", dir=directory)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=FIELDS)
            writer.writeheader()
            writer.writerows(rows)
        os.replace(temporary, board_path(identifier))
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def git(*args: str) -> str:
    result = subprocess.run(["git", *args], cwd=ROOT, text=True, capture_output=True, check=True)
    return result.stdout.strip()


def initialize(identifier: str, modes: set[str], locales: set[str]) -> list[dict[str, str]]:
    source = yaml.safe_load((ROOT / "ops" / "recette" / "cases.yml").read_text(encoding="utf-8"))
    defaults = source.get("defaults") or {}
    selected_modes = modes or set(defaults.get("modes") or [])
    selected_locales = locales or set(defaults.get("locales") or [])
    rows = []
    for case in source.get("cases") or []:
        for mode in defaults.get("modes") or []:
            if mode not in selected_modes:
                continue
            for locale in defaults.get("locales") or []:
                if locale not in selected_locales:
                    continue
                rows.append({
                    "id": f"{case['id']}:{mode}:{locale}",
                    "wave": str(case["wave"]),
                    "surface": str(case["surface"]),
                    "mode": mode,
                    "locale": locale,
                    "path": str(case["path"]),
                    "intent": str(case["intent"]),
                    "status": "PENDING",
                    "attempts": "0",
                    "claimed_at": "",
                    "finished_at": "",
                    "note": "",
                    "evidence": "",
                })
    write_rows(identifier, rows)
    status = git("status", "--short")
    metadata = {
        "run": identifier,
        "createdAt": now(),
        "head": git("rev-parse", "HEAD"),
        "branch": git("branch", "--show-current"),
        "dirtyPaths": [line[3:] for line in status.splitlines() if line],
        "modes": sorted(selected_modes),
        "locales": sorted(selected_locales),
    }
    (run_dir(identifier) / "metadata.json").write_text(
        json.dumps(metadata, indent=2) + "\n", encoding="utf-8"
    )
    return rows


def find_row(rows: list[dict[str, str]], case_id: str) -> dict[str, str]:
    for row in rows:
        if row["id"] == case_id:
            return row
    raise ValueError(f"unknown case: {case_id}")


def resume(args: argparse.Namespace) -> int:
    path = board_path(args.run)
    rows = read_rows(args.run) if path.exists() else initialize(
        args.run, set(args.mode or []), set(args.locale or [])
    )
    pending = next((row for row in rows if row["status"] not in TERMINAL), None)
    print(pending["id"] if pending else "COMPLETED")
    return 0


def claim(args: argparse.Namespace) -> int:
    rows = read_rows(args.run)
    row = find_row(rows, args.case)
    if row["status"] in TERMINAL:
        raise ValueError(f"{args.case} is already {row['status']}")
    attempts = int(row["attempts"] or 0) + 1
    if attempts > 3:
        raise ValueError(f"{args.case} already has three attempts")
    row.update(status="IN_PROGRESS", attempts=str(attempts), claimed_at=now())
    write_rows(args.run, rows)
    return 0


def verdict(args: argparse.Namespace) -> int:
    rows = read_rows(args.run)
    row = find_row(rows, args.case)
    if row["status"] != "IN_PROGRESS":
        raise ValueError(f"{args.case} is {row['status']}, expected IN_PROGRESS")
    row.update(status=args.status, finished_at=now(), note=args.note, evidence=args.evidence or "")
    write_rows(args.run, rows)
    return 0


def matrix(args: argparse.Namespace) -> int:
    rows = read_rows(args.run)
    by_status = Counter(row["status"] for row in rows)
    by_wave = Counter((row["wave"], row["status"]) for row in rows)
    print("status,count")
    for status in sorted(by_status):
        print(f"{status},{by_status[status]}")
    print("\nwave,status,count")
    for (wave, status), count in sorted(by_wave.items()):
        print(f"{wave},{status},{count}")
    return 0


def report(args: argparse.Namespace) -> int:
    rows = read_rows(args.run)
    counts = Counter(row["status"] for row in rows)
    metadata = json.loads((run_dir(args.run) / "metadata.json").read_text(encoding="utf-8"))
    lines = [
        f"# Nudger recette {args.run}", "", f"- Revision: `{metadata['head']}`",
        f"- Branch: `{metadata['branch']}`", "", "## Verdicts", "",
        "| Status | Count |", "|---|---:|",
    ]
    lines.extend(f"| {status} | {counts[status]} |" for status in sorted(counts))
    failures = [row for row in rows if row["status"] in {"FAILED", "BLOCKED"}]
    lines += ["", "## Failures and blockers", ""]
    lines.extend(
        f"- `{row['id']}`: {row['note']} ({row['evidence'] or 'no evidence path'})"
        for row in failures
    )
    if not failures:
        lines.append("- None recorded.")
    target = run_dir(args.run) / "report.md"
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(target)
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    resume_parser = commands.add_parser("resume")
    resume_parser.add_argument("--run", required=True, type=validate_run_id)
    resume_parser.add_argument("--mode", action="append", choices=["desktop", "mobile"])
    resume_parser.add_argument("--locale", action="append", choices=["fr", "en"])
    claim_parser = commands.add_parser("claim")
    claim_parser.add_argument("--run", required=True, type=validate_run_id)
    claim_parser.add_argument("case")
    verdict_parser = commands.add_parser("verdict")
    verdict_parser.add_argument("--run", required=True, type=validate_run_id)
    verdict_parser.add_argument("case")
    verdict_parser.add_argument("--status", required=True, choices=sorted(TERMINAL))
    verdict_parser.add_argument("--note", required=True)
    verdict_parser.add_argument("--evidence")
    for name in ("matrix", "report"):
        sub = commands.add_parser(name)
        sub.add_argument("--run", required=True, type=validate_run_id)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        return globals()[args.command](args)
    except (OSError, ValueError, subprocess.CalledProcessError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
