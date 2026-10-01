#!/usr/bin/env python3
"""Fail-closed gate: refuse a commit made directly on the default branch (GOU-183).

Four completed issues (GOU-175, GOU-177, GOU-178, GOU-179) were committed straight onto the
shared workspace's local `main` instead of an issue branch. None of those commits were ever
pushed -- there was no remote branch, no PR, no CI run -- yet GOU-179 was marked `done`. This is
the upstream half of the branch-contamination family: GOU-159 / check-branch-sync.sh catches a
branch created from a `main` that already carries foreign commits (contamination flowing
downstream, into a new branch); this gate catches an agent's own commit landing on `main` in the
first place (contamination flowing upstream, out of a branch workflow). The host's own
`Paperclip SSH sync merge` commits land on disk before any hook runs and are out of scope -- see
`docs/operations/shared-workspace-branch-sync.md`.

Invoked by `.githooks/pre-commit` as the repository's `pre-commit` hook, so it runs before each
commit is created, while `HEAD` still points at the branch the commit would land on.

Exit status 0 when the current branch is not the default branch; 1 otherwise, with the exact
command to branch off before retrying the commit.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def _git(repo_root: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo_root), *args],
        capture_output=True,
        text=True,
        check=False,
    )
    return result.stdout.strip()


def default_branch(repo_root: Path) -> str:
    import os

    override = os.environ.get("O4G_DEFAULT_BRANCH")
    if override:
        return override

    remote_head = _git(repo_root, "symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD")
    if remote_head and remote_head.startswith("origin/"):
        return remote_head[len("origin/") :]

    return "main"


def current_branch(repo_root: Path) -> str:
    return _git(repo_root, "symbolic-ref", "--quiet", "--short", "HEAD")


def check(repo_root: Path) -> list[str]:
    branch = current_branch(repo_root)
    default = default_branch(repo_root)

    if not branch:
        # Detached HEAD: not on any branch, so not on the default branch either.
        return []

    if branch != default:
        return []

    return [
        f"commit refused: HEAD is on '{default}', the default branch -- commits must land on an "
        "issue branch, never on it directly.",
        f"'{default}' must only ever mirror 'origin/{default}'; branch off before committing:",
        f"  git checkout -b <ISSUE-ID>-<slug> {default}",
        "If work is already staged, it moves with you -- no need to unstage first.",
    ]


def main() -> int:
    repo_root = Path(
        subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=True,
        ).stdout.strip()
    )
    problems = check(repo_root)
    if problems:
        for problem in problems:
            print(problem, file=sys.stderr)
        return 1
    print("OK: not committing to the default branch.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
