#!/usr/bin/env python3
"""Fail closed on the governed documentation corpus.

Checks the things a reader cannot check by reading: that every governed document
declares its resolution, that a normative document is written in a language an
English-reading agent can parse, that internal links resolve, and that every
cited ADR and WorkOrder identifier actually exists. Canonical decision 9 is the
reason this is a gate and not a convention -- a stale claim in a guide is a
defect, and the cheapest ones to catch are the mechanical ones.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
FRONT_MATTER = re.compile(r"\A---\n(.*?)\n---\n", re.DOTALL)
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
ADR = re.compile(r"\bADR-(\d{4})\b")

# Published website content is authored against the Nuxt Content schema, not
# ours; generated projections are rewritten by their generator (decision 2, 8).
EXEMPT = ("docs/en/", "docs/fr/", "docs/reference/", "docs/adr/README.md")


def governed_paths(root: Path) -> list[Path]:
    paths = [root / "AGENTS.md"] if (root / "AGENTS.md").is_file() else []
    if (root / "docs").is_dir():
        paths.extend(sorted((root / "docs").rglob("*.md")))
    return [p for p in paths if not p.relative_to(root).as_posix().startswith(EXEMPT)]


def front_matter(path: Path) -> tuple[dict | None, str | None]:
    match = FRONT_MATTER.match(path.read_text(encoding="utf-8"))
    if not match:
        return None, "missing governed front matter"
    try:
        value = yaml.safe_load(match.group(1))
    except yaml.YAMLError as exc:
        return None, f"invalid governed front matter: {exc}"
    if not isinstance(value, dict):
        return None, "governed front matter must be a mapping"
    missing = {"title", "normative", "audience"} - value.keys()
    if missing:
        return None, f"governed front matter missing {', '.join(sorted(missing))}"
    return value, None


def language_problem(fields: dict) -> str | None:
    """Canonical decision 6: normative text is English. A document declares its
    language in `lang`, defaulting to en; a normative document declaring fr is
    refused, because a rule an English-reading agent cannot parse is a rule that
    does not apply."""
    language = str(fields.get("lang", "en")).strip().lower()
    if str(fields.get("normative")).strip().lower() == "true" and language != "en":
        return f"declares normative: true with lang: {language}; normative text is English"
    return None


def local_link_target(source: Path, target: str) -> Path | None:
    """The resolved path a link points at, or None when it is not ours to check:
    an external URL, an anchor, a site-absolute path, or a templating expression
    that only has a value once rendered (docs/templates/**)."""
    target = target.split("#", 1)[0].strip()
    if not target or "://" in target or target.startswith(("mailto:", "#", "/")):
        return None
    if "{{" in target or "{%" in target or "${" in target:
        return None
    return (source.parent / target).resolve()


def active_ids(root: Path) -> tuple[set[str], set[str]]:
    adr_dir = root / "docs" / "adr"
    decisions = {p.name[:4] for p in adr_dir.glob("[0-9][0-9][0-9][0-9]-*.md")} if adr_dir.is_dir() else set()
    work_dir = root / ".o4g" / "work"
    work: set[str] = set()
    if work_dir.is_dir():
        work = {p.stem for p in work_dir.glob("*.yml")}
        ledger = work_dir / "ledger"
        if ledger.is_dir():
            # A closed WorkOrder is compacted into ledger/ (decision 8); a
            # document may still cite it by id.
            work |= {p.stem for p in ledger.glob("*.yml")}
    return decisions, work


def lint(root: Path) -> list[str]:
    root = root.resolve()
    problems: list[str] = []
    decisions, work = active_ids(root)

    for path in governed_paths(root):
        relative = path.relative_to(root).as_posix()
        text = path.read_text(encoding="utf-8")
        fields, error = front_matter(path)
        if error:
            problems.append(f"{relative}: {error}")
        else:
            language = language_problem(fields)
            if language:
                problems.append(f"{relative}: {language}")
        for match in LINK.finditer(text):
            target = local_link_target(path, match.group(1))
            if target is not None and not target.exists():
                problems.append(f"{relative}: broken internal link {match.group(1)!r}")
        for identifier in ADR.findall(text):
            if identifier not in decisions:
                problems.append(f"{relative}: invalid decision reference ADR-{identifier}")

    work_dir = root / ".o4g" / "work"
    if work_dir.is_dir():
        for path in sorted(work_dir.glob("*.yml")):
            try:
                data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
            except yaml.YAMLError as exc:
                problems.append(f"{path.name}: invalid YAML: {exc}")
                continue
            spec = data.get("spec") or {}
            for identifier in spec.get("decisionRefs") or []:
                token = str(identifier).removeprefix("ADR-")
                if token not in decisions:
                    problems.append(f"{path.name}: invalid decision reference {identifier}")
            for identifier in spec.get("dependsOn") or []:
                if identifier not in work:
                    problems.append(f"{path.name}: invalid work-order reference {identifier}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    problems = lint(args.root)
    if problems:
        print("Documentation lint FAILED:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1
    print("OK: documentation front matter, language, links and references are valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
