#!/usr/bin/env python3
"""Enforce the open4goods corpus budget and the two-resolution rule.

The problem this addresses: this repository accumulated five dialects of agent
instructions (AGENTS.md, CLAUDE.md, GEMINI.md, copilot-instructions.md, README.md)
that contradict each other on verifiable facts, and 53% of docs/ was unreachable
from the file that calls itself the canonical entry point. Prose is cheap for a
language model to produce, so authority accrued to whatever grew fastest. Both
effects are now bounded mechanically.

Two-resolution rule. Project and corpus contracts live under .o4g/; active
task contracts live in Paperclip issues. A short human explanation points
to the authoritative contract. Long narrative documents restating both
are rejected. Every governed Markdown document declares its resolution:

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

Margin warning. A pass with little room left is indistinguishable from a
comfortable pass until the next documentation PR fails on it, so a successful
run also reports how many non-normative lines remain before the ceiling, and
warns -- without failing -- once that margin drops under
MARGIN_WARNING_THRESHOLD. The warning is for the next writer, not this run: a
tight margin is not this run's fault and not this run's problem to fix.

Exit status 0 when the corpus is within budget, 1 otherwise. The margin
warning never changes this: it is printed on the success path and the exit
status stays 0.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
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

# Below this many lines of non-normative margin left under the ceiling, a
# successful run warns: the next documentation PR will likely need to shrink
# something before it can land, and it is cheaper to know that before writing
# it than after CI fails.
MARGIN_WARNING_THRESHOLD = 40

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
}

# Measures recorded but not ratcheted, and why. Both grow by construction: an ADR
# is how a rule is meant to be added, so gating it on a ceiling would make the
# intended act the thing that blocks a merge.
UNRATCHETED = {
    "normative_documents": "bounded by NORMATIVE_PATHS, not by a line count",
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

    return (
        {
            "non_normative_lines": non_normative_lines,
            "rule_shaped_statements_in_non_normative": rule_shaped,
            "normative_documents": normative_docs,
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


def merge_into_worktree(
    worktree_dir: str, base_ref: str, env: dict[str, str] | None = None
) -> subprocess.CompletedProcess:
    """Attempt the measurement merge inside worktree_dir, under a throwaway
    committer identity scoped to this one command.

    `git merge --no-ff` validates the committer identity before it looks at
    content, even with --no-commit: a GitHub runner has none configured, so an
    unscoped call fails identically whether the branches conflict or not. The
    `-c` flags set that identity for this invocation alone -- no global config
    is touched and no trace is left in the repository, since --no-commit means
    the identity is never actually used to create anything.

    env defaults to None, which inherits the caller's environment (the
    production path); tests pass a stripped environment to prove the merge
    still succeeds with no git identity configured anywhere.
    """
    return subprocess.run(
        [
            "git",
            "-c", "user.name=corpus-budget",
            "-c", "user.email=corpus-budget@invalid",
            "merge", "--no-commit", "--no-ff", base_ref,
        ],
        cwd=worktree_dir,
        capture_output=True,
        text=True,
        env=env,
    )


def merge_left_conflicts(worktree_dir: str) -> bool:
    """Whether a failed merge in worktree_dir left real conflicted paths.

    A merge can fail before it ever touches a path -- an unknown ref, a
    corrupt object, a missing identity -- and that failure is not "a merge
    conflict with the base branch". Only unmerged entries in the index mean
    the content actually conflicted.
    """
    unmerged = subprocess.run(
        ["git", "diff", "--name-only", "--diff-filter=U"],
        cwd=worktree_dir,
        capture_output=True,
        text=True,
    )
    return bool(unmerged.stdout.strip())


def run_merge_check(base_ref: str) -> int:
    """Measure the corpus budget on the tree a merge of base_ref into HEAD would
    produce, not on HEAD alone.

    The repository does not require PR branches to be kept up to date, so a
    branch that is individually within budget can still push the corpus over
    its ceiling once combined with commits that landed on the base branch
    after the branch was forked -- a combination plain HEAD measurement never
    sees. A real merge conflict here is reported as a failure, not swallowed
    into a pass; a merge that cannot even be attempted (environment, tooling)
    is reported as that, not misreported as a conflict.
    """
    worktree_dir = tempfile.mkdtemp(prefix="corpus-budget-merge-")
    try:
        added = subprocess.run(
            ["git", "worktree", "add", "--detach", "--quiet", worktree_dir, "HEAD"],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if added.returncode != 0:
            print(
                f"Cannot create a worktree to measure the merge with {base_ref}:\n"
                f"{added.stderr}",
                file=sys.stderr,
            )
            return 2

        merged = merge_into_worktree(worktree_dir, base_ref)
        if merged.returncode != 0:
            if merge_left_conflicts(worktree_dir):
                print(
                    f"Merge conflict between HEAD and {base_ref}:\n",
                    file=sys.stderr,
                )
                print(merged.stdout, file=sys.stderr)
                print(merged.stderr, file=sys.stderr)
                print(
                    "\nThis is a merge conflict with the base branch: resolve it "
                    "on the branch. This is not a budget failure.",
                    file=sys.stderr,
                )
                return 1
            print(
                f"Cannot measure the corpus budget the merge with {base_ref} would "
                "produce: the merge command itself failed for a reason other than "
                "a content conflict (see output below). This is an environment or "
                "tooling problem, not a conflict with the base branch -- do not "
                "look for conflicting files.\n",
                file=sys.stderr,
            )
            print(merged.stdout, file=sys.stderr)
            print(merged.stderr, file=sys.stderr)
            return 2

        env = os.environ.copy()
        env.pop("GITHUB_BASE_REF", None)
        result = subprocess.run(
            [sys.executable, "scripts/verify/check_corpus_budget.py"],
            cwd=worktree_dir,
            env=env,
        )
        return result.returncode
    finally:
        subprocess.run(
            ["git", "worktree", "remove", "--force", worktree_dir],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        shutil.rmtree(worktree_dir, ignore_errors=True)


BUDGET_COMMENT = (
    "Ceilings for the open4goods corpus. Exceeding one fails CI. Lowering one is "
    "the intended direction and needs no ceremony; raising one is a deliberate, "
    "reviewable act. Regenerate with scripts/verify/check_corpus_budget.py --update. "
    f"A passing run also warns in its own output, without failing, once the "
    f"non_normative_lines margin drops under {MARGIN_WARNING_THRESHOLD} lines "
    "(MARGIN_WARNING_THRESHOLD in check_corpus_budget.py) -- so the next writer "
    "knows the margin is tight before a documentation PR hits the ceiling, not after."
)


def main() -> int:
    argv = sys.argv[1:]
    if "--assert-no-ceiling-increase" in argv:
        index = argv.index("--assert-no-ceiling-increase")
        if index + 1 >= len(argv):
            print("usage: --assert-no-ceiling-increase REF", file=sys.stderr)
            return 2
        return assert_no_ceiling_increase(argv[index + 1])

    base_ref = os.environ.get("GITHUB_BASE_REF")
    if base_ref and "--update" not in argv:
        return run_merge_check(f"origin/{base_ref}")

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

    print(success_message(measured, ceilings))
    return 0


def success_message(measured: dict[str, int], ceilings: dict[str, int]) -> str:
    margin = ceilings["non_normative_lines"] - measured["non_normative_lines"]
    message = (
        "OK: corpus within budget "
        f"({measured['non_normative_lines']}/{ceilings['non_normative_lines']} non-normative lines, "
        f"{margin} lines of margin, "
        f"{measured['rule_shaped_statements_in_non_normative']}/"
        f"{ceilings['rule_shaped_statements_in_non_normative']} rule-shaped statements "
        "outside normative docs)."
    )
    if margin < MARGIN_WARNING_THRESHOLD:
        message += (
            f"\nWARN: only {margin} lines of margin remain; the next "
            "documentation PR will likely fail."
        )
    return message


if __name__ == "__main__":
    raise SystemExit(main())
