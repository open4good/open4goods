#!/usr/bin/env python3
"""Tests for the corpus budget margin warning (scripts/verify/check_corpus_budget.py), GOU-179."""

from __future__ import annotations

import importlib.util
import sys
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


if __name__ == "__main__":
    unittest.main()
