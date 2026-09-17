#!/usr/bin/env python3
"""Regression tests for deterministic local product backup sampling."""

from __future__ import annotations

import gzip
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("product_backup_sample.py")
SPEC = importlib.util.spec_from_file_location("product_backup_sample", MODULE_PATH)
assert SPEC and SPEC.loader
SAMPLE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = SAMPLE
SPEC.loader.exec_module(SAMPLE)


class ProductBackupSampleTest(unittest.TestCase):
    """Prove coverage failure and byte-identical repeated generation."""

    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        records = []
        for index, vertical in enumerate(SAMPLE.VERTICALS):
            records.append({
                "gtin": "4006381333931",
                "vertical": vertical,
                "sources": [f"source-{index}"],
                "attributes": {"weight": index},
                "offers": [{"price": index + 1}],
                "resources": [{"url": f"https://example.invalid/{index}"}],
            })
        records.extend([
            {"gtin": "4006381333931", "name": "legacy only"},
            {"gtin": "4006381333931", "datasource": "amazon.fr"},
            {"gtin": "invalid", "error": "known shape"},
        ])
        with gzip.open(self.root / "products-0.gz", "wt", encoding="utf-8") as handle:
            for record in records:
                handle.write(json.dumps(record, sort_keys=True) + "\n")
        (self.root / SAMPLE.MANIFEST).write_text(json.dumps({
            "files": ["products-0.gz"],
            "expectedCount": len(records),
            "exportedCount": len(records),
        }), encoding="utf-8")

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_repeated_generation_is_byte_identical_and_covers_every_bucket(self) -> None:
        first = SAMPLE.generate(self.root, self.root / "first", 2)
        second = SAMPLE.generate(self.root, self.root / "second", 2)

        self.assertEqual(first["sampleSha256"], second["sampleSha256"])
        self.assertEqual(set((*SAMPLE.VERTICALS, *SAMPLE.SPECIAL_BUCKETS)), set(first["buckets"]))
        self.assertEqual(
            (self.root / "first/products-sample.jsonl.gz").read_bytes(),
            (self.root / "second/products-sample.jsonl.gz").read_bytes(),
        )

    def test_rejects_incomplete_manifest(self) -> None:
        manifest = json.loads((self.root / SAMPLE.MANIFEST).read_text(encoding="utf-8"))
        manifest["expectedCount"] += 1
        (self.root / SAMPLE.MANIFEST).write_text(json.dumps(manifest), encoding="utf-8")

        with self.assertRaisesRegex(SAMPLE.SampleError, "incomplete"):
            SAMPLE.generate(self.root, self.root / "sample", 1)


if __name__ == "__main__":
    unittest.main()
