#!/usr/bin/env python3
"""Tests for the corpus budget margin warning (scripts/verify/check_corpus_budget.py), GOU-179,
and for the merge-based measurement's identity handling and conflict/environment distinction
(GOU-185)."""

from __future__ import annotations

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("check_corpus_budget.py")
SPEC = importlib.util.spec_from_file_location("check_corpus_budget", MODULE_PATH)
assert SPEC and SPEC.loader
BUDGET = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = BUDGET
SPEC.loader.exec_module(BUDGET)


def measured(non_normative_lines: int, rule_shaped: int = 0) -> dict[str, int]:
    return {
        "non_normative_lines": non_normative_lines,
        "rule_shaped_statements_in_non_normative": rule_shaped,
        "normative_documents": 0,
        "adr_lines_max": 0,
    }


def ceilings(non_normative_lines: int, rule_shaped: int = 100) -> dict[str, int]:
    return {
        "non_normative_lines": non_normative_lines,
        "rule_shaped_statements_in_non_normative": rule_shaped,
        "normative_documents": 0,
        "adr_lines_max": 0,
    }


class SuccessMessageMarginWarningTests(unittest.TestCase):
    def test_above_threshold_has_no_warning(self) -> None:
        margin = BUDGET.MARGIN_WARNING_THRESHOLD + 10
        message = BUDGET.success_message(
            measured(1000), ceilings(1000 + margin)
        )
        self.assertIn("OK: corpus within budget", message)
        self.assertIn(f"{margin} lines of margin", message)
        self.assertNotIn("WARN", message)

    def test_below_threshold_warns(self) -> None:
        margin = BUDGET.MARGIN_WARNING_THRESHOLD - 1
        message = BUDGET.success_message(
            measured(1000), ceilings(1000 + margin)
        )
        self.assertIn("OK: corpus within budget", message)
        self.assertIn("WARN", message)
        self.assertIn(f"only {margin} lines of margin remain", message)

    def test_at_threshold_has_no_warning(self) -> None:
        margin = BUDGET.MARGIN_WARNING_THRESHOLD
        message = BUDGET.success_message(
            measured(1000), ceilings(1000 + margin)
        )
        self.assertNotIn("WARN", message)

    def test_warning_never_implies_a_failing_exit_status(self) -> None:
        # The warning is advisory: main() only returns non-zero through the
        # exceeded/problems paths, never through success_message's content.
        message = BUDGET.success_message(measured(1000), ceilings(1000))
        self.assertIn("WARN", message)
        self.assertIn("OK: corpus within budget", message)


class MergeIdentityAndConflictTests(unittest.TestCase):
    """Exercises merge_into_worktree/merge_left_conflicts against real git repos,
    with no identity configured anywhere in the subprocess environment -- the
    exact condition a GitHub runner is in. Before the GOU-185 fix, the "behind,
    no conflict" case below fails with "empty ident name", not a budget result.
    """

    def setUp(self) -> None:
        self._stack: list[Path] = []
        # A deliberately bare environment: a plain git with no wrapper ahead of
        # it on PATH, no HOME-level .gitconfig, system config disabled, and no
        # GIT_*_NAME/EMAIL overrides -- the condition a GitHub runner is
        # actually in. (This sandbox's own PATH puts a credential-brokering
        # git wrapper first, which forces GIT_AUTHOR_NAME/EMAIL to the empty
        # string; that is a sandbox artifact, not the production condition
        # this fixture targets, so it is deliberately excluded here.)
        self.no_identity_env = {
            "PATH": "/usr/bin:/bin",
            "HOME": self._mkdtemp(),
            "GIT_CONFIG_NOSYSTEM": "1",
            "GIT_CONFIG_GLOBAL": "/dev/null",
        }

    def _mkdtemp(self) -> str:
        d = tempfile.mkdtemp(prefix="corpus-budget-test-")
        self._stack.append(Path(d))
        return d

    def tearDown(self) -> None:
        import shutil

        for path in self._stack:
            shutil.rmtree(path, ignore_errors=True)

    def _git(self, repo: Path, *args: str) -> subprocess.CompletedProcess:
        return subprocess.run(
            ["git", *args],
            cwd=repo,
            capture_output=True,
            text=True,
            env=self.no_identity_env,
            check=True,
        )

    def _init_repo(self) -> Path:
        repo = Path(self._mkdtemp())
        self._git(repo, "init", "--quiet", "--initial-branch=main")
        (repo / "file.txt").write_text("line one\n", encoding="utf-8")
        self._git(repo, "add", "file.txt")
        self._git(
            repo,
            "-c", "user.name=seed", "-c", "user.email=seed@invalid",
            "commit", "--quiet", "-m", "initial",
        )
        return repo

    def _worktree_at_head(self, repo: Path) -> Path:
        worktree_dir = Path(self._mkdtemp())
        worktree_dir.rmdir()  # git worktree add requires the path not to exist
        self._git(repo, "worktree", "add", "--detach", "--quiet", str(worktree_dir), "HEAD")
        return worktree_dir

    def test_behind_base_with_no_conflict_passes_with_no_identity_configured(self) -> None:
        repo = self._init_repo()
        # HEAD gains a commit unrelated to the one landing on main: a branch
        # behind its base, with no conflict -- the fixture from the issue.
        (repo / "feature.txt").write_text("feature work\n", encoding="utf-8")
        self._git(repo, "add", "feature.txt")
        self._git(
            repo,
            "-c", "user.name=seed", "-c", "user.email=seed@invalid",
            "commit", "--quiet", "-m", "feature",
        )
        worktree_dir = self._worktree_at_head(repo)

        merged = BUDGET.merge_into_worktree(str(worktree_dir), "main", env=self.no_identity_env)

        self.assertEqual(merged.returncode, 0, msg=f"stdout={merged.stdout!r} stderr={merged.stderr!r}")

    def test_real_conflict_is_detected_as_a_conflict(self) -> None:
        repo = self._init_repo()
        self._git(repo, "checkout", "--quiet", "-b", "feature")
        (repo / "file.txt").write_text("feature line\n", encoding="utf-8")
        self._git(repo, "add", "file.txt")
        self._git(
            repo,
            "-c", "user.name=seed", "-c", "user.email=seed@invalid",
            "commit", "--quiet", "-m", "feature changes the line",
        )
        self._git(repo, "checkout", "--quiet", "main")
        (repo / "file.txt").write_text("main line\n", encoding="utf-8")
        self._git(repo, "add", "file.txt")
        self._git(
            repo,
            "-c", "user.name=seed", "-c", "user.email=seed@invalid",
            "commit", "--quiet", "-m", "main changes the same line",
        )
        self._git(repo, "checkout", "--quiet", "feature")
        worktree_dir = self._worktree_at_head(repo)

        merged = BUDGET.merge_into_worktree(str(worktree_dir), "main", env=self.no_identity_env)

        self.assertNotEqual(merged.returncode, 0)
        self.assertTrue(BUDGET.merge_left_conflicts(str(worktree_dir)))

    def test_unresolvable_base_ref_is_not_mistaken_for_a_conflict(self) -> None:
        repo = self._init_repo()
        worktree_dir = self._worktree_at_head(repo)

        merged = BUDGET.merge_into_worktree(
            str(worktree_dir), "does-not-exist", env=self.no_identity_env
        )

        self.assertNotEqual(merged.returncode, 0)
        self.assertFalse(BUDGET.merge_left_conflicts(str(worktree_dir)))


if __name__ == "__main__":
    unittest.main()
