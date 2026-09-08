#!/usr/bin/env python3
"""Enforce the open4goods corpus budget and the two-resolution rule.

The problem this addresses: this repository accumulated five dialects of agent
instructions (AGENTS.md, CLAUDE.md, GEMINI.md, copilot-instructions.md, README.md)
that contradict each other on verifiable facts, and 53% of docs/ was unreachable
from the file that calls itself the canonical entry point. Prose is cheap for a
language model to produce, so authority accrued to whatever grew fastest. Both
effects are now bounded mechanically.

Two-resolution rule. Every concept exists exactly twice: a machine contract under
.o4g/, and one short human explanation. The third form -- the long narrative
document restating both -- is what this rejects. Every governed Markdown document
therefore declares its resolution in front matter:

    normative: true     states rules; permitted only where NORMATIVE_PATHS allows
    normative: false    derived and explanatory; rule-shaped language is counted

The governed set is AGENTS.md plus docs/**. README.md files are deliberately
outside it: this is a public open-source repository whose front page GitHub
renders as a visible table, and a front-matter block there is noise for the
audience that reads it.

Ratchet. .o4g/corpus-budget.json records the current measurements as ceilings.
Exceeding a ceiling fails. Lowering one is an ordinary commit and is the only way
the budget moves down; raising one is a deliberate, reviewable act. Pass --update
to rewrite the ceilings from the current tree.

Direction. Pass --assert-no-ceiling-increase REF to compare the ceilings against
those at REF and fail if any rose.

Exit status 0 when the corpus is within budget, 1 otherwise.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUDGET_FILE = ROOT / ".o4g" / "corpus-budget.json"

# Documents permitted to state rules. Everything else is derived.
NORMATIVE_PATHS = (
    "AGENTS.md",
    "docs/00-canonical-decisions.md",
    "docs/adr/",
)

SEARCH_ROOTS = ("docs",)

# README.md is intentionally absent -- see the module docstring.
ROOT_DOCS = ("AGENTS.md",)

# docs/en and docs/fr are website content, not project corpus: they are authored
# as data for the Nuxt Content collection (frontend/content.config.ts) and carry
# its schema's front matter, not ours. Generated projections carry no
# hand-written rule-shaped prose; counting them would make regeneration itself
# look like corpus growth.
EXEMPT_PATHS = (
    "docs/en/",
    "docs/fr/",
    "docs/reference/",
    "docs/adr/README.md",
)

# Language that asserts a rule, in both corpus languages. Counted only in
# non-normative documents, where it signals a rule that belongs in an ADR or in
# the canonical decisions instead.
RULE_SHAPED = re.compile(
    r"\b(must not|must|shall|may never|never|is forbidden|are forbidden|"
    r"is required|are required|requires|cannot|forbidden|mandatory|"
    r"doit|doivent|ne doit|ne doivent|il faut|obligatoire|interdit|"
    r"jamais|toujours)\b",
    re.IGNORECASE,
)

FRONT_MATTER = re.compile(r"\A---\n(.*?)\n---\n", re.DOTALL)

CANONICAL_DECISIONS = ROOT / "docs" / "00-canonical-decisions.md"
DECISION_LINE = re.compile(r"^(\d+)([a-z]?)\.\s")

MEASURE_RESOLUTION = {
    "non_normative_lines": "non-normative",
    "rule_shaped_statements_in_non_normative": "non-normative",
    "normative_documents": "normative",
    "adr_lines_max": "normative",
    "work_open_lines": "machine",
    "work_ledger_lines": "machine",
}

# Measures recorded but not ratcheted, and why. Both grow by construction: an ADR
# is how a rule is meant to be added, and a ledger record is how a WorkOrder is
# meant to close, so gating either on a ceiling would make the intended act the
# thing that blocks a merge.
UNRATCHETED = {
    "normative_documents": "bounded by NORMATIVE_PATHS, not by a line count",
    "work_ledger_lines": "append-only closure records",
}


def check_decision_numbering() -> list[str]:
    """Canonical decisions are the corpus's most-cited coordinate system: an ADR or
    another decision names one by number, never by heading. A duplicate or
    out-of-order number silently makes two different rules answer to the same
    citation, and nothing else in this file would catch that."""
    if not CANONICAL_DECISIONS.exists():
        return []
    problems: list[str] = []
    seen: set[str] = set()
    last_base = 0
    rel = str(CANONICAL_DECISIONS.relative_to(ROOT))
    for line in CANONICAL_DECISIONS.read_text(encoding="utf-8").splitlines():
        match = DECISION_LINE.match(line)
        if not match:
            continue
        base, letter = int(match.group(1)), match.group(2)
        token = match.group(0).split(".")[0]
        if token in seen:
            problems.append(f"{rel}: decision {token} is declared more than once")
            continue
        seen.add(token)
        if letter:
            if base != last_base:
                problems.append(
                    f"{rel}: decision {token} does not immediately follow decision {base}"
                )
        else:
            if base <= last_base:
                problems.append(
                    f"{rel}: decision {token} is out of order (follows decision {last_base})"
                )
            last_base = base
    return problems


def parse_front_matter(text: str) -> dict[str, str]:
    match = FRONT_MATTER.match(text)
    if not match:
        return {}
    fields = {}
    for line in match.group(1).splitlines():
        if ":" in line and not line.lstrip().startswith("#"):
            key, _, value = line.partition(":")
            fields[key.strip()] = value.strip().strip("\"'")
    return fields


def body_of(text: str) -> str:
    match = FRONT_MATTER.match(text)
    return text[match.end():] if match else text


def is_normative_path(rel: str) -> bool:
    return any(rel == p or rel.startswith(p) for p in NORMATIVE_PATHS)


def yaml_lines(path: Path) -> int:
    return len([ln for ln in path.read_text(encoding="utf-8").splitlines() if ln.strip()])


def measure_work_lines() -> tuple[int, int]:
    """(work_open_lines, work_ledger_lines): the live open-order corpus an agent
    must load, and the compacted closure records under ledger/."""
    root = ROOT / ".o4g" / "work"
    if not root.is_dir():
        return 0, 0
    open_lines = sum(yaml_lines(path) for path in root.glob("*.yml"))
    ledger = root / "ledger"
    ledger_lines = sum(yaml_lines(p) for p in ledger.glob("*.yml")) if ledger.is_dir() else 0
    return open_lines, ledger_lines


def measure_adr_lines_max() -> int:
    adr_dir = ROOT / "docs" / "adr"
    if not adr_dir.is_dir():
        return 0
    counts = [
        len(path.read_text(encoding="utf-8").splitlines())
        for path in adr_dir.glob("[0-9][0-9][0-9][0-9]-*.md")
    ]
    return max(counts) if counts else 0


def discover() -> list[Path]:
    documents = []
    for name in ROOT_DOCS:
        candidate = ROOT / name
        if candidate.exists():
            documents.append(candidate)
    for directory in SEARCH_ROOTS:
        root = ROOT / directory
        if root.is_dir():
            documents.extend(sorted(root.rglob("*.md")))
    return documents


def measure() -> tuple[dict[str, int], list[str]]:
    problems: list[str] = check_decision_numbering()
    non_normative_lines = 0
    rule_shaped = 0
    normative_docs = 0

    for path in discover():
        rel = str(path.relative_to(ROOT))
        if rel.startswith(EXEMPT_PATHS):
            continue
        text = path.read_text(encoding="utf-8")
        fields = parse_front_matter(text)

        declared = fields.get("normative")
        if declared is None:
            problems.append(
                f"{rel}: front matter must declare 'normative: true' or 'normative: false'"
            )
            continue
        if declared not in ("true", "false"):
            problems.append(f"{rel}: 'normative' must be true or false, got '{declared}'")
            continue

        if declared == "true":
            if not is_normative_path(rel):
                problems.append(
                    f"{rel}: declares normative: true, but only {', '.join(NORMATIVE_PATHS)} "
                    "may state rules. Move the rule into an ADR or into "
                    "docs/00-canonical-decisions.md, and leave the explanation here."
                )
                continue
            normative_docs += 1
            continue

        body = body_of(text)
        lines = [ln for ln in body.splitlines() if ln.strip()]
        non_normative_lines += len(lines)
        rule_shaped += sum(1 for ln in lines if RULE_SHAPED.search(ln))

    work_open_lines, work_ledger_lines = measure_work_lines()
    return (
        {
            "non_normative_lines": non_normative_lines,
            "rule_shaped_statements_in_non_normative": rule_shaped,
            "normative_documents": normative_docs,
            "work_open_lines": work_open_lines,
            "work_ledger_lines": work_ledger_lines,
            "adr_lines_max": measure_adr_lines_max(),
        },
        problems,
    )


def ceilings_at(ref: str) -> dict[str, int] | None:
    """The recorded ceilings as of a Git ref, or None when they cannot be read."""
    result = subprocess.run(
        ["git", "show", f"{ref}:.o4g/corpus-budget.json"],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return None
    try:
        return json.loads(result.stdout)["ceilings"]
    except (json.JSONDecodeError, KeyError, TypeError):
        return None


def assert_no_ceiling_increase(ref: str) -> int:
    baseline = ceilings_at(ref)
    if baseline is None:
        print(f"Cannot read .o4g/corpus-budget.json at '{ref}'.", file=sys.stderr)
        return 2
    current = json.loads(BUDGET_FILE.read_text(encoding="utf-8"))["ceilings"]
    raised = [
        f"{key}: {current[key]} exceeds {value} at {ref} (+{current[key] - value})"
        for key, value in baseline.items()
        if key not in UNRATCHETED and key in current and current[key] > value
    ]
    if raised:
        print(f"Corpus ceiling raised against {ref}:\n", file=sys.stderr)
        for item in raised:
            print(f"  {item}", file=sys.stderr)
        print(
            "\nA change that needs more room is a change that needs a person.",
            file=sys.stderr,
        )
        return 1
    print(f"OK: no corpus ceiling rose against {ref}.")
    return 0


BUDGET_COMMENT = (
    "Ceilings for the open4goods corpus. Exceeding one fails CI. Lowering one is "
    "the intended direction and needs no ceremony; raising one is a deliberate, "
    "reviewable act. Regenerate with scripts/verify/check_corpus_budget.py --update."
)


def main() -> int:
    argv = sys.argv[1:]
    if "--assert-no-ceiling-increase" in argv:
        index = argv.index("--assert-no-ceiling-increase")
        if index + 1 >= len(argv):
            print("usage: --assert-no-ceiling-increase REF", file=sys.stderr)
            return 2
        return assert_no_ceiling_increase(argv[index + 1])

    measured, problems = measure()

    if "--update" in argv:
        BUDGET_FILE.parent.mkdir(parents=True, exist_ok=True)
        BUDGET_FILE.write_text(
            json.dumps(
                {
                    "_comment": BUDGET_COMMENT,
                    "measures": {
                        key: {
                            "resolution": MEASURE_RESOLUTION.get(key, "machine"),
                            **({"informational": UNRATCHETED[key]} if key in UNRATCHETED else {}),
                        }
                        for key in measured
                    },
                    "ceilings": measured,
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        print(f"Budget ceilings written to {BUDGET_FILE.relative_to(ROOT)}:")
        for key, value in measured.items():
            print(f"  {key}: {value}")
        if problems:
            print("\nUnresolved two-resolution problems (not budget-related):", file=sys.stderr)
            for problem in problems:
                print(f"  {problem}", file=sys.stderr)
            return 1
        return 0

    if not BUDGET_FILE.exists():
        print(
            f"No budget file at {BUDGET_FILE.relative_to(ROOT)}. Create it with --update.",
            file=sys.stderr,
        )
        return 2

    ceilings = json.loads(BUDGET_FILE.read_text(encoding="utf-8"))["ceilings"]
    exceeded = []
    for key, value in measured.items():
        ceiling = ceilings.get(key)
        if ceiling is None or key in UNRATCHETED:
            continue
        if value > ceiling:
            exceeded.append(f"{key}: {value} exceeds ceiling {ceiling} (+{value - ceiling})")

    if problems or exceeded:
        if problems:
            print("Two-resolution rule violated:\n", file=sys.stderr)
            for problem in problems:
                print(f"  {problem}", file=sys.stderr)
            print("", file=sys.stderr)
        if exceeded:
            print("Corpus budget exceeded:\n", file=sys.stderr)
            for item in exceeded:
                print(f"  {item}", file=sys.stderr)
            print(
                "\nAdding prose is not the way through this. State the rule as an ADR "
                "or a canonical decision, or delete something. If the growth is genuinely "
                "warranted, raise the ceiling in .o4g/corpus-budget.json in the same "
                "commit, so it is reviewed.",
                file=sys.stderr,
            )
        return 1

    print(
        "OK: corpus within budget "
        f"({measured['non_normative_lines']}/{ceilings['non_normative_lines']} non-normative lines, "
        f"{measured['rule_shaped_statements_in_non_normative']}/"
        f"{ceilings['rule_shaped_statements_in_non_normative']} rule-shaped statements "
        "outside normative docs)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
