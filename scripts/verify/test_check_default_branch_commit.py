#!/usr/bin/env python3
"""Tests for the default-branch commit gate (GOU-183).

Builds a throwaway repository fixture with a real `origin`, installs a `pre-commit` hook that
invokes the real, checked-in `scripts/verify/check_default_branch_commit.py` (via
`core.hooksPath`, the same wiring AGENTS.md has an operator enable for `.githooks/pre-commit`),
then runs real `git commit` calls to prove the gate rejects a commit attempted on `main` and
accepts the same commit on an issue branch -- by execution, not by calling the gate function
directly. The fixture's hook script is a thin shim with an absolute path to this repository's
copy of the gate, since the throwaway repo carries no tree of its own to resolve a relative path
against; the gate script it runs, and the real commit/branch outcomes it produces, are not
stubbed.
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def run(cwd: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["git", "-C", str(cwd), *args],
        capture_output=True,
        text=True,
        check=check,
    )


class DefaultBranchCommitGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.fixture = Path(self._tmp.name)

        self.upstream = self.fixture / "upstream.git"
        self.clone = self.fixture / "clone"

        run(self.fixture, "init", "--bare", "--initial-branch=main", str(self.upstream))
        run(
            self.fixture,
            "-c",
            "user.email=test@example.com",
            "-c",
            "user.name=test",
            "clone",
            str(self.upstream),
            str(self.clone),
        )
        run(
            self.clone,
            "-c",
            "user.email=test@example.com",
            "-c",
            "user.name=test",
            "commit",
            "--allow-empty",
            "-m",
            "init",
        )
        run(self.clone, "push", "origin", "main")

        # The fixture repo carries no tree of its own, so its hook cannot resolve the gate via
        # a repo-relative path the way .githooks/pre-commit does for a real checkout. Shim to an
        # absolute path to this repository's real, checked-in gate script instead of a stub.
        gate_script = ROOT / "scripts" / "verify" / "check_default_branch_commit.py"
        hooks_dir = self.fixture / "hooks"
        hooks_dir.mkdir()
        pre_commit = hooks_dir / "pre-commit"
        pre_commit.write_text(
            "#!/usr/bin/env bash\nset -euo pipefail\nexec python3 "
            f'"{gate_script}"\n',
            encoding="utf-8",
        )
        pre_commit.chmod(0o755)
        run(self.clone, "config", "core.hooksPath", str(hooks_dir))

    def tearDown(self) -> None:
        self._tmp.cleanup()

    def _commit(self, message: str) -> subprocess.CompletedProcess:
        return run(
            self.clone,
            "-c",
            "user.email=test@example.com",
            "-c",
            "user.name=test",
            "commit",
            "--allow-empty",
            "-m",
            message,
            check=False,
        )

    def test_commit_on_main_is_rejected(self) -> None:
        result = self._commit("should be rejected")
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("default branch", result.stdout + result.stderr)
        self.assertIn("git checkout -b", result.stdout + result.stderr)

        # The branch must stay exactly at its pushed state: no stray commit landed.
        log = run(self.clone, "log", "--oneline", "main").stdout.strip().splitlines()
        self.assertEqual(len(log), 1)

    def test_commit_on_issue_branch_is_accepted(self) -> None:
        run(self.clone, "checkout", "-b", "GOU-183-example")
        result = self._commit("allowed on an issue branch")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

        log = run(self.clone, "log", "--oneline", "GOU-183-example").stdout.strip().splitlines()
        self.assertEqual(len(log), 2)


if __name__ == "__main__":
    unittest.main()
