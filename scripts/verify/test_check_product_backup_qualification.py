#!/usr/bin/env python3
"""Tests for the offline fresh-backup qualification gate.

Pins a synthetic backup fixture with `scripts/migration/pin_product_backup.py` -- the same
tool a local development run uses -- then feeds that real pin output into the qualification
gate. No real backup file, private volume or remote storage is ever touched; everything runs
under a throwaway `tempfile.TemporaryDirectory()`.
"""

from __future__ import annotations

import copy
import gzip
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


PIN = load_module("scripts/migration/pin_product_backup.py", "pin_product_backup")
QUALIFY = load_module("scripts/verify/check_product_backup_qualification.py", "check_product_backup_qualification")


class ProductBackupQualificationTest(unittest.TestCase):
    """Proves qualification accepts a real fresh pin and rejects every broken claim in it."""

    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        source = Path(self.temp.name)
        with gzip.open(source / "products-backup-0.gz", "wt", encoding="utf-8") as handle:
            handle.write('{"gtin":"4006381333931","source":"web","price":4.2}\n')
        (source / PIN.LEGACY_MANIFEST_NAME).write_text(
            json.dumps(
                {
                    "completedAt": "2026-09-12T04:20:50Z",
                    "completedEpochMillis": 1789186850000,
                    "expectedCount": 1,
                    "exportedCount": 1,
                    "pageSize": 1000,
                    "files": ["products-backup-0.gz"],
                }
            ),
            encoding="utf-8",
        )
        self.pin_output = PIN.pin(str(source), 10)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_qualifies_a_freshly_pinned_backup(self) -> None:
        facts = QUALIFY.qualify(self.pin_output)

        self.assertTrue(any("1 line(s) total" in fact for fact in facts))
        self.assertTrue(any("field disposition(s) resolved" in fact for fact in facts))

    def test_rejects_wrong_schema_version(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["schemaVersion"] = "open4goods.product-backup-input/v0"

        with self.assertRaisesRegex(QUALIFY.QualificationError, "schemaVersion"):
            QUALIFY.qualify(broken)

    def test_rejects_digest_mismatch_shaped_corruption(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["files"][0]["sha256"] = "not-a-digest"

        with self.assertRaisesRegex(QUALIFY.QualificationError, "sha256"):
            QUALIFY.qualify(broken)

    def test_rejects_line_count_inconsistency(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["files"][0]["lineCount"] = 2

        with self.assertRaisesRegex(QUALIFY.QualificationError, "lineCount"):
            QUALIFY.qualify(broken)

    def test_rejects_missing_archive_entry(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["files"] = []

        with self.assertRaisesRegex(QUALIFY.QualificationError, "files is empty"):
            QUALIFY.qualify(broken)

    def test_rejects_partial_field_disposition_coverage(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["conversionRules"]["fieldDispositions"].pop("gtin")

        with self.assertRaisesRegex(QUALIFY.QualificationError, "fieldDispositions"):
            QUALIFY.qualify(broken)

    def test_rejects_collapsed_account_billing_preservation_boundary(self) -> None:
        broken = copy.deepcopy(self.pin_output)
        broken["scope"]["separatePreservation"] = ["media bytes"]

        with self.assertRaisesRegex(QUALIFY.QualificationError, "separate"):
            QUALIFY.qualify(broken)

    def test_qualification_never_reads_the_source_volume_or_network(self) -> None:
        # The gate takes only the pin's JSON output; no path under the private
        # source volume, and no URL, is ever referenced by the qualifier module.
        source = inspect_source(QUALIFY)
        self.assertNotIn("PRODUCT_BACKUP_SOURCE_URI", source)
        self.assertNotIn("http://", source)
        self.assertNotIn("https://", source)


def inspect_source(module) -> str:
    return Path(module.__file__).read_text(encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
