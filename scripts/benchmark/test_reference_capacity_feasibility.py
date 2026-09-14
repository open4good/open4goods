"""Tests for the deterministic reference-capacity disk gate."""

from __future__ import annotations

import importlib.util
import sys
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


if __name__ == "__main__":
    unittest.main()
