#!/usr/bin/env python3
"""Guard the Git-authored source-usage policy inventory against two silent gaps (GOU-165).

`services/data-reference/src/main/resources/policy/source-usage-policies.json` is
the only place `SourceUsagePolicyRegistry.allows`/`allowsUse` ever looks up a
persisted `(policyId, version)` reference; a reference missing from it is denied
with no error, no log and no metric (before GOU-165's logging fix) and, more
durably, forever unresolvable for any data already stamped with it. This script
fails closed on two distinct gaps that already recurred twice on this milestone:

  * AC1 - a reference the production code can stamp onto a record (a literal
    `new SourceUsagePolicyRef("id", "version")`, or a `SourceUsagePolicy.denyAll(
    "id", ..., "version", ...)` policy) but that is not a row in the JSON
    inventory. This is a pending reference: nothing has reviewed it yet, so any
    record already carrying it resolves to nothing.
  * AC2 - a `(policyId, version)` row present in the compared revision (default
    `origin/main`) that is no longer present at all in the current file. Once a
    reviewed version is persisted onto real data, retiring it must keep the row
    in the file (e.g. with `effectiveUntil`/`revokedAt` set) rather than delete
    it, so the reference stays resolvable and the retirement is readable in the
    registry itself, not reconstructed from `git log`.

Read-only; this script never writes to the working tree or to Git.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

POLICY_RESOURCE = "services/data-reference/src/main/resources/policy/source-usage-policies.json"

# A literal `new SourceUsagePolicyRef("policyId", "version")` reference, the
# shape every production USAGE_POLICY constant and stamped head reference uses.
_REF_LITERAL = re.compile(
    r'new\s+SourceUsagePolicyRef\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)')

# `SourceUsagePolicy.denyAll("policyId", <sourceIdExpr>, "version", ...)`: the
# second argument is a SourceId expression, not a literal, so it is skipped.
_DENY_ALL_LITERAL = re.compile(
    r'SourceUsagePolicy\s*\.\s*denyAll\s*\(\s*"([^"]+)"\s*,\s*[^,]+,\s*"([^"]+)"',
    re.DOTALL)


def is_test_source(path: Path) -> bool:
    return "src/test/" in path.as_posix() or "src/it/" in path.as_posix()


def find_cited_references(repo_root: Path) -> dict[tuple[str, str], list[str]]:
    """Maps each literal-cited (policyId, version) to the production files citing it."""
    cited: dict[tuple[str, str], list[str]] = {}
    for java_file in sorted(repo_root.rglob("*.java")):
        if is_test_source(java_file):
            continue
        text = java_file.read_text(encoding="utf-8")
        rel = java_file.relative_to(repo_root).as_posix()
        for pattern in (_REF_LITERAL, _DENY_ALL_LITERAL):
            for match in pattern.finditer(text):
                cited.setdefault((match.group(1), match.group(2)), []).append(rel)
    return cited


def load_registry_pairs(document_text: str) -> set[tuple[str, str]]:
    document = json.loads(document_text)
    return {(policy["policyId"], policy["version"]) for policy in document["policies"]}


def check_no_pending_references(repo_root: Path) -> list[str]:
    """AC1: every production-cited reference must be a row in the registry."""
    registry_pairs = load_registry_pairs((repo_root / POLICY_RESOURCE).read_text(encoding="utf-8"))
    cited = find_cited_references(repo_root)
    problems = []
    for (policy_id, version), files in sorted(cited.items()):
        if (policy_id, version) not in registry_pairs:
            problems.append(
                f"pending policy reference {policy_id}/{version} cited in production code "
                f"but absent from {POLICY_RESOURCE}: {', '.join(files)}")
    return problems


def git_show(repo_root: Path, revision: str, relative_path: str) -> str | None:
    result = subprocess.run(
        ["git", "-C", str(repo_root), "show", f"{revision}:{relative_path}"],
        capture_output=True, text=True, check=False)
    if result.returncode != 0:
        return None
    return result.stdout


def check_no_silent_removal(old_text: str, new_text: str) -> list[str]:
    """AC2: a (policyId, version) row must never disappear between two revisions.

    Retiring a version must keep its row (e.g. with `effectiveUntil`/`revokedAt`
    set) rather than delete it, so a reference already persisted onto data stays
    resolvable and the retirement is legible in the registry itself.
    """
    old_pairs = load_registry_pairs(old_text)
    new_pairs = load_registry_pairs(new_text)
    removed = sorted(old_pairs - new_pairs)
    return [
        f"policy reference {policy_id}/{version} was removed from {POLICY_RESOURCE}; "
        "keep the row and record its retirement (effectiveUntil/revokedAt) instead of deleting it"
        for policy_id, version in removed
    ]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--against", default="origin/main",
                         help="Git revision the registry must not have silently dropped a row from")
    parser.add_argument("--skip-removal-check", action="store_true",
                         help="Skip AC2 (useful when --against is unreachable, e.g. a shallow clone)")
    args = parser.parse_args(argv)

    problems = check_no_pending_references(args.repo_root)

    if not args.skip_removal_check:
        old_text = git_show(args.repo_root, args.against, POLICY_RESOURCE)
        if old_text is None:
            problems.append(
                f"could not read {POLICY_RESOURCE} at {args.against!r}; "
                "pass --skip-removal-check only for a known-unreachable revision")
        else:
            new_text = (args.repo_root / POLICY_RESOURCE).read_text(encoding="utf-8")
            problems.extend(check_no_silent_removal(old_text, new_text))

    if problems:
        print("source usage policy registry guard rejected:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1

    print("source usage policy registry guard accepted: no pending reference, no silent removal")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
