#!/usr/bin/env python3
"""Fail-closed gate: every governance control script must be registered as invoked or exempt.

Five governance checks shipped with a green CI while nothing invoked them (GOU-162, GOU-164,
GOU-153, GOU-165, GOU-174): in each case the check's own test suite ran, so CI was green, while
the check itself never ran. The fault was never removing a wiring -- it was always forgetting
one on something new.

.o4g/control-invocations.json lists every scripts/verify/check_*.py and scripts/deploy/verify_*.py
file with either `invokedBy`, a list of files this script asserts actually mention the control's
basename (verified here by reading them, not trusted), or `exempt: true` with a non-empty written
`justification`. A control script present in the tree but missing from the registry fails. A
registered `invokedBy` file that no longer mentions the script also fails -- a wiring, once
declared, cannot be silently dropped either.

An `invokedBy` entry is either a plain string (just the invoker path), or an object
`{"file": <invoker path>, "requiredArgs": [<flag>, ...]}` for a control that only does its job
with specific flags (GOU-189: check_corpus_budget.py was wired into lint_suite.py, but without
its `--assert-no-ceiling-increase` flag -- the registry was green while the regression it exists
to catch went unchecked). For an object entry, the invoker file must mention both the control's
basename and every string in `requiredArgs`.

Exit status 0 when every discovered control is registered and every declared, non-exempt
invocation point still names the script and all of its required arguments; 1 otherwise.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REGISTRY_PATH = ROOT / ".o4g" / "control-invocations.json"
CONTROL_GLOBS = (
    ("scripts/verify", "check_*.py"),
    ("scripts/deploy", "verify_*.py"),
)


def discover_controls(root: Path) -> list[str]:
    found: list[str] = []
    for directory, pattern in CONTROL_GLOBS:
        for path in sorted((root / directory).glob(pattern)):
            if path.name.startswith("test_"):
                continue
            found.append(str(path.relative_to(root)))
    return sorted(found)


def check(root: Path) -> list[str]:
    registry_path = root / ".o4g" / "control-invocations.json"
    if not registry_path.exists():
        return [f"{registry_path.relative_to(root)} is missing"]

    try:
        data = json.loads(registry_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        return [f"{registry_path.relative_to(root)} is not valid JSON: {error}"]

    controls = data.get("controls")
    if not isinstance(controls, dict):
        return [f"{registry_path.relative_to(root)}: 'controls' must be an object"]

    problems: list[str] = []

    for script in discover_controls(root):
        if script not in controls:
            problems.append(f"{script}: present in the tree but not registered in {registry_path.relative_to(root)}")

    for script, declaration in controls.items():
        if not isinstance(declaration, dict):
            problems.append(f"{script}: registry entry must be an object")
            continue
        if not (root / script).exists():
            problems.append(f"{script}: registered but no longer exists in the tree -- remove the stale entry")
            continue

        if declaration.get("exempt"):
            justification = declaration.get("justification")
            if not isinstance(justification, str) or not justification.strip():
                problems.append(f"{script}: exempt with no written justification")
            continue

        invoked_by = declaration.get("invokedBy")
        if not isinstance(invoked_by, list) or not invoked_by:
            problems.append(f"{script}: neither exempt nor invokedBy is declared")
            continue

        basename = Path(script).name
        for entry in invoked_by:
            if isinstance(entry, str):
                invoker, required_args = entry, []
            elif isinstance(entry, dict):
                invoker = entry.get("file")
                required_args = entry.get("requiredArgs") or []
                if not isinstance(invoker, str) or not isinstance(required_args, list):
                    problems.append(f"{script}: invokedBy entry must have a string 'file' and a list 'requiredArgs'")
                    continue
            else:
                problems.append(f"{script}: invokedBy entries must be a string or an object")
                continue

            invoker_path = root / invoker
            if not invoker_path.exists():
                problems.append(f"{script}: declared invocation point {invoker} does not exist")
                continue
            invoker_text = invoker_path.read_text(encoding="utf-8")
            if basename not in invoker_text:
                problems.append(f"{script}: {invoker} no longer references {basename}")
                continue
            missing_args = [arg for arg in required_args if arg not in invoker_text]
            if missing_args:
                problems.append(
                    f"{script}: {invoker} references {basename} but is missing required argument(s) "
                    f"{', '.join(missing_args)}"
                )

    return problems


def main() -> int:
    problems = check(ROOT)
    if problems:
        for problem in problems:
            print(problem, file=sys.stderr)
        return 1
    print("OK: every control script is registered and every declared invocation point is real.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
