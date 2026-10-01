#!/usr/bin/env python3
"""Fail-closed gate: the legacy real-time aggregation path must not grow new Icecat coupling.

GOU-187 arbitrated option (b): `TaxonomyRealTimeAggregationService`,
`AttributeRealtimeAggregationService`, `MediaAggregationService` and `TaxonomyMappingService`
(wired at `AggregationFacadeService.java:324-348`) stay in service for the transition instead of
being migrated now, and their retirement is GOU-50's scope. GOU-39 AC2 only permits this because
the legacy path is frozen: "No new Icecat logic is added to that legacy path: any evolution goes
through the source-record/projection pipeline, and a CI check must fail if the set of Icecat
symbols referenced by those four classes grows."

A written decision does not hold on its own -- GOU-192 exists because this exact failure mode
(a documented rule nothing enforces) recurred eight times on this milestone. This script is that
enforcement: it parses the `import` statements of the four legacy files, keeps the ones naming an
Icecat-specific type (package segment `icecat`, or a simple name starting with `Icecat`), and
compares that set against the frozen baseline below. A symbol outside the baseline fails the
check and is named in the message. Shrinking the set (an import removed) is always accepted --
only growth is a violation. docs/architecture/icecat-reference-data.md enumerates the same four
files and must stay in sync with this list when the baseline legitimately changes.

Read-only; this script never writes to the working tree.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# Frozen baseline (GOU-192): the Icecat symbols each legacy file is allowed to import today.
# Growing one of these sets is the exact drift AC2 forbids -- see the module docstring.
BASELINE: dict[str, frozenset[str]] = {
    "api/src/main/java/org/open4goods/api/services/aggregation/services/realtime/"
    "TaxonomyRealTimeAggregationService.java": frozenset({"IcecatCategoryVerticalResolver"}),
    "api/src/main/java/org/open4goods/api/services/aggregation/services/realtime/"
    "AttributeRealtimeAggregationService.java": frozenset({"IcecatConstants", "IcecatFeatureResolver"}),
    "api/src/main/java/org/open4goods/api/services/aggregation/services/realtime/"
    "MediaAggregationService.java": frozenset(),
    "api/src/main/java/org/open4goods/api/services/TaxonomyMappingService.java": frozenset({"IcecatReferential"}),
}

_IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;", re.MULTILINE)


def extract_icecat_symbols(java_source: str) -> set[str]:
    """Simple names imported from an `icecat` package, or whose own name starts with `Icecat`."""
    symbols: set[str] = set()
    for match in _IMPORT.finditer(java_source):
        fqcn = match.group(1)
        segments = fqcn.split(".")
        simple_name = segments[-1]
        package_segments = {segment.lower() for segment in segments[:-1]}
        if "icecat" in package_segments or simple_name.startswith("Icecat"):
            symbols.add(simple_name)
    return symbols


def check(root: Path) -> list[str]:
    problems: list[str] = []
    for relative_path, allowed in BASELINE.items():
        path = root / relative_path
        if not path.exists():
            problems.append(
                f"{relative_path}: registered legacy file no longer exists -- "
                "update docs/architecture/icecat-reference-data.md and this script's BASELINE together")
            continue
        actual = extract_icecat_symbols(path.read_text(encoding="utf-8"))
        grown = sorted(actual - allowed)
        if grown:
            problems.append(
                f"{relative_path}: new Icecat symbol(s) {', '.join(grown)} -- "
                "GOU-39 AC2 forbids new Icecat logic on the legacy real-time aggregation path; "
                "route this evolution through the source-record/projection pipeline instead")
    return problems


def main() -> int:
    problems = check(ROOT)
    if problems:
        print("Icecat legacy-path symbol guard rejected:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print("OK: the legacy real-time aggregation path references no Icecat symbol beyond the frozen baseline.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
