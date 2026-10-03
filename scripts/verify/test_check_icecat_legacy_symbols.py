#!/usr/bin/env python3
"""Tests for the Icecat legacy-path symbol guard (scripts/verify/check_icecat_legacy_symbols.py)."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("check_icecat_legacy_symbols.py")
SPEC = importlib.util.spec_from_file_location("check_icecat_legacy_symbols", MODULE_PATH)
assert SPEC and SPEC.loader
GUARD = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GUARD
SPEC.loader.exec_module(GUARD)


class ExtractIcecatSymbolsTest(unittest.TestCase):
    def test_finds_a_type_imported_from_an_icecat_package(self):
        source = "import org.open4goods.icecat.services.IcecatFeatureResolver;\nclass X {}"

        self.assertEqual(GUARD.extract_icecat_symbols(source), {"IcecatFeatureResolver"})

    def test_finds_an_icecat_named_type_outside_the_icecat_package(self):
        source = "import org.open4goods.model.vertical.referential.IcecatReferential;\nclass X {}"

        self.assertEqual(GUARD.extract_icecat_symbols(source), {"IcecatReferential"})

    def test_ignores_unrelated_imports(self):
        source = "import java.util.List;\nimport org.open4goods.model.product.Product;\nclass X {}"

        self.assertEqual(GUARD.extract_icecat_symbols(source), set())

    def test_ignores_comment_mentions_of_icecat(self):
        source = (
            "import java.util.List;\n"
            "// Removes protected Icecat URLs that cannot be fetched anonymously.\n"
            "class X {}")

        self.assertEqual(GUARD.extract_icecat_symbols(source), set())

    def test_collects_multiple_symbols(self):
        source = (
            "import org.open4goods.icecat.services.IcecatFeatureResolver;\n"
            "import org.open4goods.icecat.util.IcecatConstants;\n"
            "class X {}")

        self.assertEqual(
            GUARD.extract_icecat_symbols(source), {"IcecatFeatureResolver", "IcecatConstants"})


class CheckTest(unittest.TestCase):
    def write_baseline_files(self, root: Path) -> None:
        for relative_path, allowed in GUARD.BASELINE.items():
            path = root / relative_path
            path.parent.mkdir(parents=True, exist_ok=True)
            imports = "\n".join(f"import org.open4goods.icecat.services.{name};" for name in sorted(allowed))
            path.write_text(f"{imports}\nclass X {{}}\n", encoding="utf-8")

    def test_accepts_the_frozen_baseline(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write_baseline_files(root)

            self.assertEqual(GUARD.check(root), [])

    def test_rejects_a_new_symbol_on_a_legacy_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write_baseline_files(root)
            grown_file = root / next(iter(GUARD.BASELINE))
            grown_file.write_text(
                grown_file.read_text(encoding="utf-8").replace(
                    "class X {}", "import org.open4goods.icecat.services.IcecatSupplierService;\nclass X {}"),
                encoding="utf-8")

            problems = GUARD.check(root)

            self.assertEqual(len(problems), 1)
            self.assertIn("IcecatSupplierService", problems[0])

    def test_accepts_a_symbol_removed_from_the_baseline(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write_baseline_files(root)
            shrunk_file = next(
                root / relative_path for relative_path, allowed in GUARD.BASELINE.items() if allowed)
            shrunk_file.write_text("class X {}\n", encoding="utf-8")

            self.assertEqual(GUARD.check(root), [])

    def test_fails_when_a_registered_legacy_file_is_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write_baseline_files(root)
            missing_file = root / next(iter(GUARD.BASELINE))
            missing_file.unlink()

            problems = GUARD.check(root)

            self.assertEqual(len(problems), 1)
            self.assertIn("no longer exists", problems[0])


if __name__ == "__main__":
    unittest.main()
