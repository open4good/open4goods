#!/usr/bin/env python3
"""Tests for the product backup input contract pinning tool."""

from __future__ import annotations

import gzip
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("pin_product_backup.py")
SPEC = importlib.util.spec_from_file_location("pin_product_backup", MODULE_PATH)
assert SPEC and SPEC.loader
PIN = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = PIN
SPEC.loader.exec_module(PIN)


class ProductBackupPinTest(unittest.TestCase):
    """Proves that pinning remains bounded by one complete legacy generation."""

    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.source = Path(self.temp.name)
        self.write_gzip("products-backup-0.gz", ['{"gtin":"4006381333931","source":"web","price":4.2}'])
        self.write_manifest(["products-backup-0.gz"], 1)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def write_gzip(self, name: str, records: list[str]) -> None:
        with gzip.open(self.source / name, "wt", encoding="utf-8") as handle:
            handle.write("\n".join(records) + "\n")

    def write_manifest(self, files: list[str], count: int) -> None:
        (self.source / PIN.LEGACY_MANIFEST_NAME).write_text(json.dumps({
            "completedAt": "2026-09-12T04:20:50Z",
            "completedEpochMillis": 1789186850000,
            "expectedCount": count,
            "exportedCount": count,
            "pageSize": 1000,
            "files": files,
        }), encoding="utf-8")

    def test_pins_manifest_listed_file_with_checksum_and_safe_inventory(self) -> None:
        result = PIN.pin(str(self.source), 10)

        self.assertEqual(PIN.SCHEMA_VERSION, result["schemaVersion"])
        self.assertEqual(1, result["files"][0]["lineCount"])
        self.assertEqual(["string"], result["sample"]["fieldShapes"]["gtin"])
        self.assertNotIn("4006381333931", json.dumps(result))
        self.assertEqual(94, result["conversionRules"]["attributeDispositionCount"])
        self.assertEqual("UNATTRIBUTED_IDENTITY", result["conversionRules"]["fieldDispositions"]["gtin"]["classification"])

    def test_rejects_missing_manifest_listed_file(self) -> None:
        self.write_manifest(["products-backup-9.gz"], 1)

        with self.assertRaisesRegex(PIN.InputContractError, "missing"):
            PIN.pin(str(self.source), 10)

    def test_rejects_relative_or_remote_source_locations(self) -> None:
        with self.assertRaisesRegex(PIN.InputContractError, "absolute"):
            PIN.source_path("relative-backup")
        with self.assertRaisesRegex(PIN.InputContractError, "absolute"):
            PIN.source_path("file:relative-backup")
        with self.assertRaisesRegex(PIN.InputContractError, "absolute"):
            PIN.source_path("https://example.test/product-backup")

    def test_rejects_truncated_archive(self) -> None:
        (self.source / "products-backup-0.gz").write_bytes(b"not gzip")

        with self.assertRaises((PIN.InputContractError, gzip.BadGzipFile, EOFError)):
            PIN.pin(str(self.source), 10)

    def test_rejects_mixed_line_count_generation(self) -> None:
        self.write_gzip("products-backup-0.gz", ["{}", "{}"])

        with self.assertRaisesRegex(PIN.InputContractError, "line count"):
            PIN.pin(str(self.source), 10)

    def test_reads_the_nested_legacy_normalized_gtin(self) -> None:
        self.write_gzip("products-backup-0.gz", ['{"gtinInfos":{"normalizedGtin14":"4006381333931"}}'])

        result = PIN.pin(str(self.source), 10)

        self.assertEqual(1, result["sample"]["counts"]["validGtins"])


if __name__ == "__main__":
    unittest.main()
