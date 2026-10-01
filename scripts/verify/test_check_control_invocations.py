#!/usr/bin/env python3
"""Tests for the control-invocation registry gate (GOU-178).

Builds a throwaway fixture tree per case -- a registered-and-wired control, an orphan the
registry does not know about, and a declared wiring that has been removed from its invoker --
and asserts the gate accepts the first and rejects the other two. One case also protects this
gate's own entry point: lint_suite.py must keep calling check_control_invocations.py, since this
is exactly the failure mode this gate exists to catch everywhere else.
"""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def load_module(relative_path: str, name: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / relative_path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


GATE = load_module("scripts/verify/check_control_invocations.py", "check_control_invocations")


def write_registry(root: Path, controls: dict) -> None:
    o4g = root / ".o4g"
    o4g.mkdir(parents=True, exist_ok=True)
    (o4g / "control-invocations.json").write_text(
        json.dumps({"version": 1, "controls": controls}), encoding="utf-8"
    )


def write_control(root: Path, relative_path: str) -> None:
    path = root / relative_path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("#!/usr/bin/env python3\n", encoding="utf-8")


class ControlInvocationRegistryTest(unittest.TestCase):
    def test_registered_and_wired_control_passes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_control(root, "scripts/verify/check_example.py")
            invoker = root / "scripts" / "python" / "lint_suite.py"
            invoker.parent.mkdir(parents=True, exist_ok=True)
            invoker.write_text("run check_example.py here\n", encoding="utf-8")
            write_registry(
                root,
                {"scripts/verify/check_example.py": {"invokedBy": ["scripts/python/lint_suite.py"]}},
            )
            self.assertEqual(GATE.check(root), [])

    def test_exempt_control_requires_written_justification(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_control(root, "scripts/verify/check_example.py")

            write_registry(root, {"scripts/verify/check_example.py": {"exempt": True, "justification": ""}})
            problems = GATE.check(root)
            self.assertTrue(any("no written justification" in problem for problem in problems))

            write_registry(
                root,
                {"scripts/verify/check_example.py": {"exempt": True, "justification": "needs live API access"}},
            )
            self.assertEqual(GATE.check(root), [])

    def test_unregistered_control_fails(self) -> None:
        """AC2: a new scripts/verify/check_*.py absent from the registry fails the gate."""
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_control(root, "scripts/verify/check_new.py")
            write_registry(root, {})
            problems = GATE.check(root)
            self.assertTrue(any("check_new.py" in problem and "not registered" in problem for problem in problems))

    def test_removed_wiring_fails(self) -> None:
        """AC3: removing the call from the declared invoker fails the gate even though the
        registry entry itself is untouched."""
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_control(root, "scripts/verify/check_example.py")
            invoker = root / "scripts" / "python" / "lint_suite.py"
            invoker.parent.mkdir(parents=True, exist_ok=True)
            invoker.write_text("nothing relevant here\n", encoding="utf-8")
            write_registry(
                root,
                {"scripts/verify/check_example.py": {"invokedBy": ["scripts/python/lint_suite.py"]}},
            )
            problems = GATE.check(root)
            self.assertTrue(any("no longer references check_example.py" in problem for problem in problems))

    def test_this_gate_remains_wired_into_lint_suite(self) -> None:
        """AC5: the fixture dies with the control -- if the real lint_suite.py drops the call to
        this gate, this assertion fails."""
        lint_suite = (ROOT / "scripts" / "python" / "lint_suite.py").read_text(encoding="utf-8")
        self.assertIn("check_control_invocations.py", lint_suite)

    def test_real_registry_and_tree_pass(self) -> None:
        self.assertEqual(GATE.check(ROOT), [])


if __name__ == "__main__":
    unittest.main()
