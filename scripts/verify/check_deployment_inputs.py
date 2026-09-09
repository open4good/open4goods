#!/usr/bin/env python3
"""Gate GitHub Actions deployment inputs against ops/config/deployment-inputs.yml.

AC4 of config-contract-and-environments (docs/adr/0006-github-environments-and-systemd-runtime.md)
requires CI to fail when a deployment input is unclassified, duplicated at the wrong scope or
rendered into logs. This checks the three failure shapes that are actually observable from this
checkout, with no GitHub secret-administration access:

  unclassified   -- a workflow references secrets.NAME or vars.NAME that ops/config/
                     deployment-inputs.yml does not list. Adding an input to CI must be a
                     deliberate, reviewed edit to the manifest, not a silent new reference.
  wrong scope    -- the same name is referenced from both the secrets and vars store (the two
                     stores have different masking guarantees), or a `secrets.NAME || 'literal'`
                     fallback duplicates the value into the tracked workflow file itself -- the
                     same shape as the incident secret-scan.yml exists to catch.
  logged         -- a step whose own env/with block binds a manifest input prints it (echo,
                     printf, cat, env, printenv) or runs with `set -x`/`-o xtrace`/a verbose curl
                     flag, which can defeat GitHub's masking -- known to be unreliable for
                     multi-line secrets such as SSH_PRIVATE_KEY.

What this cannot check from here: whether a beta/prod GitHub Environment holds a duplicate or
stale copy of an input, since none exist yet (AC2 is unattempted) and no secret-administration API
access is available to this lot. That gap is out of scope for this script, not silently passed.

Exit status 0 when every reference is classified, single-store and not logged; 1 otherwise.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
MANIFEST_PATH = ROOT / "ops" / "config" / "deployment-inputs.yml"
WORKFLOWS_DIR = ROOT / ".github" / "workflows"

REFERENCE = re.compile(r"\$\{\{\s*(secrets|vars)\.([A-Za-z0-9_]+)\s*\}\}")
FALLBACK = re.compile(r"\$\{\{\s*(?:secrets|vars)\.[A-Za-z0-9_]+\s*\|\|[^}]+\}\}")
PRINT_COMMANDS = re.compile(r"\b(?:echo|printf|cat|env|printenv)\b")
XTRACE = re.compile(r"\bset\s+(?:-[a-z]*x[a-z]*\b|-o\s+xtrace\b)")
VERBOSE_CURL = re.compile(r"\bcurl\b.*\s(?:-v\b|-vvv\b|--verbose\b|--trace\b|--trace-ascii\b)")


def load_manifest() -> dict[str, dict]:
    data = yaml.safe_load(MANIFEST_PATH.read_text(encoding="utf-8")) or {}
    return data.get("inputs") or {}


def find_references(text: str) -> list[tuple[int, str, str]]:
    """Return (line number, store, name) for every secrets./vars. reference, skipping comments."""
    found = []
    for lineno, line in enumerate(text.splitlines(), start=1):
        if line.strip().startswith("#"):
            continue
        for match in REFERENCE.finditer(line):
            found.append((lineno, match.group(1), match.group(2)))
    return found


def find_fallbacks(text: str) -> list[int]:
    return [lineno for lineno, line in enumerate(text.splitlines(), start=1) if FALLBACK.search(line)]


def sensitive_names_in(value: object, manifest: dict[str, dict]) -> set[str]:
    """Names from the manifest that a raw YAML value (a plain string) resolves to."""
    if not isinstance(value, str):
        return set()
    return {name for _, name in ((m.group(1), m.group(2)) for m in REFERENCE.finditer(value)) if name in manifest}


def check_logging(workflow: dict, manifest: dict[str, dict], relative: str) -> list[str]:
    problems: list[str] = []
    workflow_env = workflow.get("env") or {}
    workflow_sensitive = {name for value in workflow_env.values() for name in sensitive_names_in(value, manifest)}
    for job_name, job in (workflow.get("jobs") or {}).items():
        job_env = job.get("env") or {}
        job_sensitive = workflow_sensitive | {
            name for value in job_env.values() for name in sensitive_names_in(value, manifest)
        }
        for step in job.get("steps") or []:
            step_env = step.get("env") or {}
            with_block = step.get("with") or {}
            step_sensitive = job_sensitive | {
                name
                for value in (*step_env.values(), *with_block.values())
                for name in sensitive_names_in(value, manifest)
            }
            run = step.get("run")
            if not step_sensitive or not isinstance(run, str):
                continue
            step_label = step.get("name", "<unnamed step>")
            for line in run.splitlines():
                stripped = line.strip()
                if not stripped or stripped.startswith("#"):
                    continue
                flagged_names = [
                    name for name in step_sensitive if re.search(r"\$\{?" + re.escape(name) + r"\b", line)
                ]
                if flagged_names and PRINT_COMMANDS.search(line):
                    problems.append(
                        f"{relative}: job {job_name!r} step {step_label!r} may log "
                        f"{', '.join(sorted(flagged_names))}: {stripped}"
                    )
                if XTRACE.search(line):
                    problems.append(
                        f"{relative}: job {job_name!r} step {step_label!r} enables shell tracing while "
                        f"{', '.join(sorted(step_sensitive))} is in scope, which can defeat log masking: {stripped}"
                    )
                if VERBOSE_CURL.search(line):
                    problems.append(
                        f"{relative}: job {job_name!r} step {step_label!r} runs curl verbosely while "
                        f"{', '.join(sorted(step_sensitive))} is in scope: {stripped}"
                    )
    return problems


def check_file(path: Path, manifest: dict[str, dict]) -> list[str]:
    relative = path.relative_to(ROOT).as_posix()
    text = path.read_text(encoding="utf-8")
    problems: list[str] = []

    stores_seen: dict[str, set[str]] = {}
    for lineno, store, name in find_references(text):
        stores_seen.setdefault(name, set()).add(store)
        if name not in manifest:
            problems.append(f"{relative}:{lineno}: unclassified deployment input {store}.{name}")
            continue
        expected_store = manifest[name].get("store")
        if store != expected_store:
            problems.append(
                f"{relative}:{lineno}: {name} is classified as {expected_store!r} but referenced via {store!r}"
            )

    for name, stores in stores_seen.items():
        if len(stores) > 1:
            problems.append(f"{relative}: {name} is referenced from both {' and '.join(sorted(stores))}")

    for lineno in find_fallbacks(text):
        problems.append(f"{relative}:{lineno}: literal fallback duplicates a secrets./vars. reference")

    try:
        workflow = yaml.safe_load(text) or {}
    except yaml.YAMLError as exc:
        problems.append(f"{relative}: invalid YAML: {exc}")
        return problems
    if isinstance(workflow, dict):
        problems.extend(check_logging(workflow, manifest, relative))
    return problems


def main() -> int:
    manifest = load_manifest()
    problems: list[str] = []
    workflow_files = sorted(set(WORKFLOWS_DIR.glob("*.yml")) | set(WORKFLOWS_DIR.glob("*.yaml")))
    for path in workflow_files:
        problems.extend(check_file(path, manifest))

    if problems:
        print("Deployment input check FAILED:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1
    print(f"OK: every deployment input in {WORKFLOWS_DIR.relative_to(ROOT)} is classified, single-store and not logged.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
