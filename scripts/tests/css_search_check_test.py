"""Tests for scripts/diagnostics/css_search_check.py: BETA_BASIC_AUTH header (GOU-2050)."""

from __future__ import annotations

import importlib.util
import sys
import unittest
import unittest.mock
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "diagnostics" / "css_search_check.py"
spec = importlib.util.spec_from_file_location("css_search_check", SCRIPT)
check = importlib.util.module_from_spec(spec)
assert spec.loader is not None
sys.modules[spec.name] = check
spec.loader.exec_module(check)


class BasicAuthTest(unittest.TestCase):
    def test_header_present_when_variable_is_set(self) -> None:
        with unittest.mock.patch.dict("os.environ", {"BETA_BASIC_AUTH": "qa:secret"}, clear=True):
            opener, _ = check.build_opener()
        self.assertIn(("Authorization", "Basic cWE6c2VjcmV0"), opener.addheaders)

    def test_header_absent_without_variable(self) -> None:
        with unittest.mock.patch.dict("os.environ", {}, clear=True):
            opener, _ = check.build_opener()
        self.assertNotIn("Authorization", [name for name, _ in opener.addheaders])


if __name__ == "__main__":
    unittest.main()
