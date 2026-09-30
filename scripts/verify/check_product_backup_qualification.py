#!/usr/bin/env python3
"""Qualify a freshly pinned product backup input contract, offline and value-free.

`scripts/migration/pin_product_backup.py` reads the private backup volume once and writes
an import-side manifest (schema `open4goods.product-backup-input/v1`) that never leaves the
archive as data: only its SHA-256, gzip-decoded line counts and a bounded field-shape
inventory. ADR-0014 (docs/adr/0014-local-first-development-and-staged-promotion.md) calls a
fresh backup "qualified" only after that manifest's checksums, gzip integrity, line counts
and internal consistency are proven -- not merely produced.

This script re-checks that proof from the manifest alone. It never opens the archive, the
private backup volume or any remote storage; it is safe to run against a fixture manifest in
CI or on a laptop with no beta access. Pairing this with `pin_product_backup.py` covers the
"fresh backup qualification" leg of GOU-136: pin locally, then qualify the pin's own claims
before anything downstream trusts it.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


SCHEMA_VERSION = "open4goods.product-backup-input/v1"
SHA256 = re.compile(r"^[0-9a-f]{64}$")
BACKUP_FILE_NAME = re.compile(r"products-backup-\d+\.gz")
REQUIRED_PRESERVATION = {"production accounts", "billing", "keys"}


class QualificationError(ValueError):
    """Raised when the pin output does not prove one consistent, complete generation."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise QualificationError(message)


def qualify(pin_output: dict[str, Any]) -> list[str]:
    """Return the qualification facts proven, or raise on the first broken claim."""
    require(pin_output.get("schemaVersion") == SCHEMA_VERSION, "schemaVersion is not the expected pin contract")

    legacy = pin_output.get("legacyManifest")
    require(isinstance(legacy, dict), "legacyManifest is missing")
    require(isinstance(legacy.get("sha256"), str) and SHA256.fullmatch(legacy["sha256"]), "legacyManifest.sha256 is not a SHA-256 digest")
    require(isinstance(legacy.get("completedAt"), str) and legacy["completedAt"], "legacyManifest.completedAt is missing")
    require(isinstance(legacy.get("expectedCount"), int) and isinstance(legacy.get("exportedCount"), int), "legacyManifest counts are missing")
    require(legacy["exportedCount"] >= legacy["expectedCount"], "legacyManifest reports an incomplete export")
    legacy_files = legacy.get("files")
    require(isinstance(legacy_files, list) and bool(legacy_files), "legacyManifest.files is empty")
    require(len(legacy_files) == len(set(legacy_files)), "legacyManifest.files has duplicate archive names")
    require(all(isinstance(name, str) and BACKUP_FILE_NAME.fullmatch(name) for name in legacy_files), "legacyManifest.files contains an unsafe archive name")

    files = pin_output.get("files")
    require(isinstance(files, list) and bool(files), "files is empty")
    file_names = [entry.get("name") for entry in files if isinstance(entry, dict)]
    require(file_names == legacy_files, "files does not list exactly the legacy-manifest archives, in order")

    total_lines = 0
    for entry in files:
        require(isinstance(entry, dict), "a files entry is not an object")
        require(isinstance(entry.get("sha256"), str) and SHA256.fullmatch(entry["sha256"]), f"{entry.get('name')}: sha256 is not a SHA-256 digest")
        require(isinstance(entry.get("bytes"), int) and entry["bytes"] > 0, f"{entry.get('name')}: bytes must be positive")
        require(isinstance(entry.get("lineCount"), int) and entry["lineCount"] >= 0, f"{entry.get('name')}: lineCount is missing or negative")
        total_lines += entry["lineCount"]
    require(total_lines == legacy["exportedCount"], "sum of per-file lineCount does not match legacyManifest.exportedCount")

    sample = pin_output.get("sample")
    require(isinstance(sample, dict), "sample is missing")
    counts = sample.get("counts")
    require(isinstance(counts, dict) and counts.get("lineCount") == total_lines, "sample.counts.lineCount does not match the qualified line count")
    field_shapes = sample.get("fieldShapes")
    require(isinstance(field_shapes, dict), "sample.fieldShapes is missing")

    rules = pin_output.get("conversionRules")
    require(isinstance(rules, dict) and isinstance(rules.get("fieldDispositions"), dict), "conversionRules.fieldDispositions is missing")
    dispositions = rules["fieldDispositions"]
    require(set(dispositions) == set(field_shapes), "conversionRules.fieldDispositions does not cover exactly the observed field names")
    require(all(isinstance(d, dict) and d.get("classification") for d in dispositions.values()), "a field disposition has no classification")

    scope = pin_output.get("scope")
    require(isinstance(scope, dict), "scope is missing")
    preserved = set(scope.get("separatePreservation") or [])
    require(REQUIRED_PRESERVATION <= preserved, "scope.separatePreservation does not keep account, billing and key state separate")

    return [
        f"legacy generation {legacy['completedAt']} ({legacy['sha256'][:12]}...)",
        f"{len(legacy_files)} archive file(s), {total_lines} line(s) total",
        f"{len(dispositions)} field disposition(s) resolved",
    ]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pin_output", type=Path, help="path to a pin_product_backup.py --output file")
    args = parser.parse_args()

    try:
        data = json.loads(args.pin_output.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"product backup qualification rejected: cannot read pin output: {error}", file=sys.stderr)
        return 2
    if not isinstance(data, dict):
        print("product backup qualification rejected: pin output must be a JSON object", file=sys.stderr)
        return 2

    try:
        facts = qualify(data)
    except QualificationError as error:
        print(f"product backup qualification rejected: {error}", file=sys.stderr)
        return 1

    print("OK: product backup qualified")
    for fact in facts:
        print(f"  - {fact}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
