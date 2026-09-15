"""Tests for the deterministic reference-capacity disk gate."""

from __future__ import annotations

import importlib.util
import gzip
import json
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("reference_capacity_feasibility.py")
SPEC = importlib.util.spec_from_file_location("reference_capacity_feasibility", MODULE_PATH)
assert SPEC and SPEC.loader
CAPACITY = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = CAPACITY
SPEC.loader.exec_module(CAPACITY)


class CapacityPlanTest(unittest.TestCase):
    """Keeps full-volume estimates explicit and conservative."""

    def setUp(self) -> None:
        self.cluster = {"storeBytes": 1_000}

    def test_preserves_unknowns_without_replay_measurements(self) -> None:
        result = CAPACITY.capacity_plan(self.cluster, 10_000, None, None, None, None, 1, 1_000, 16_000_000)

        self.assertIn("UNKNOWN", result["coexistence"])
        self.assertIn("BLOCKED", result["fullVolumeGate"])

    def test_requires_every_candidate_size_when_any_is_known(self) -> None:
        with self.assertRaisesRegex(CAPACITY.FeasibilityError, "all candidate"):
            CAPACITY.capacity_plan(self.cluster, 10_000, 100, None, 100, 100, 1, 1_000, 16_000_000)

    def test_includes_replica_snapshot_and_headroom_in_required_free_space(self) -> None:
        result = CAPACITY.capacity_plan(self.cluster, 2_000, 100, 200, 300, 400, 1, 1_000, 16_000_000)

        self.assertEqual(1_000, result["candidatePrimaryBytes"]["sourceHead"] + result["candidatePrimaryBytes"]["projection"]
                         + result["candidatePrimaryBytes"]["priceStore"] + result["snapshotBytes"])
        self.assertEqual(2_600, result["projectedUsedBytesDuringCoexistence"])
        self.assertEqual(2_380, result["requiredFreeBytes"])
        self.assertEqual("BLOCKED", result["fullVolumeGate"])


class BoundedArchiveSampleTest(unittest.TestCase):
    """Proves the private sample remains bounded and never emits category values."""

    def test_collects_seven_opaque_category_cohorts_and_an_unclassified_gtin(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive = root / "archive"
            archive.mkdir()
            names = ["products-backup-0.gz", "products-backup-1.gz"]
            records = [
                {"gtinInfos": {"normalizedGtin14": "00000000000000"}, "sourceUrls": {}, "offersCount": 1},
                *[{"gtinInfos": {"normalizedGtin14": f"0000000000000{value}"}, "category": f"category-{value}",
                   "sourceUrls": {}, "offersCount": value} for value in range(1, 8)],
            ]
            for name in names:
                with gzip.open(archive / name, "wt", encoding="utf-8") as handle:
                    for record in records:
                        handle.write(json.dumps(record) + "\n")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"legacyManifest": {"files": names}}), encoding="utf-8")

            result = CAPACITY.bounded_archive_sample(archive, manifest, "test-seed", 10, 64 * 1024)

        self.assertEqual(7, result["verticalCohortCount"])
        self.assertEqual(2, result["unclassifiedGtinRecords"])
        self.assertEqual(7, len(result["selectedVerticalCohortHashes"]))
        self.assertNotIn("category-1", str(result))


if __name__ == "__main__":
    unittest.main()
