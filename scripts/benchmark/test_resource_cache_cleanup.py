"""Tests for disk-bounded product-resource cache cleanup safeguards."""

from __future__ import annotations

import importlib.util
import os
import sys
import tempfile
import time
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("resource_cache_cleanup.py")
SPEC = importlib.util.spec_from_file_location("resource_cache_cleanup", MODULE_PATH)
assert SPEC and SPEC.loader
CLEANUP = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = CLEANUP
SPEC.loader.exec_module(CLEANUP)


class ResourceKeyTest(unittest.TestCase):
    """Proves only exact, filename-safe resource keys become live references."""

    def test_extracts_only_cache_keys_from_resource_records(self) -> None:
        keys = CLEANUP.resource_keys({"resources": [
            {"cacheKey": "live-image"}, {"cacheKey": "nested/path"}, {"cacheKey": None}, "not-a-resource",
        ]})

        self.assertEqual({"live-image"}, keys)

    def test_three_shard_walk_excludes_foreign_cache_directories(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            live = root / "A" / "B" / "C" / "resource"
            live.parent.mkdir(parents=True)
            live.write_bytes(b"resource")
            foreign = root / "geocode" / "not-a-resource"
            foreign.parent.mkdir()
            foreign.write_bytes(b"foreign")

            discovered = list(CLEANUP.cache_files(root))

        self.assertEqual([(live, Path("A/B/C/resource"))], discovered)


class QuarantineTest(unittest.TestCase):
    """Proves a recovery copy is complete before the beta cache source is released."""

    def test_copy_then_remove_preserves_the_exact_relative_path(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source" / "A" / "B" / "C" / "resource"
            destination = root / "destination"
            source.parent.mkdir(parents=True)
            destination.mkdir()
            source.write_bytes(b"recoverable")

            CLEANUP.quarantine_file(source, Path("A/B/C/resource"), destination)

            self.assertFalse(source.exists())
            self.assertEqual(b"recoverable", (destination / "A/B/C/resource").read_bytes())

    def test_refuses_to_overwrite_a_recovery_copy(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source"
            destination = root / "destination"
            source.write_bytes(b"source")
            destination.mkdir()
            (destination / "entry").write_bytes(b"existing")

            with self.assertRaisesRegex(CLEANUP.ResourceCleanupError, "already exists"):
                CLEANUP.quarantine_file(source, Path("entry"), destination)
            self.assertTrue(source.exists())


class SweepTest(unittest.TestCase):
    """Keeps recently written files out of an otherwise exact orphan sweep."""

    def test_recent_file_is_preserved_by_the_grace_period(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cache_root = root / "cache"
            recent = cache_root / "A" / "B" / "C" / "recent"
            recent.parent.mkdir(parents=True)
            recent.write_bytes(b"recent")
            database = root / "active.sqlite"
            with CLEANUP.open_database(database) as connection:
                arguments = type("Arguments", (), {
                    "cache_root": cache_root,
                    "cancel_file": None,
                    "file_check_interval": 1,
                    "grace_period_ms": 60_000,
                    "mode": "INVENTORY",
                    "max_move_bytes": None,
                })()

                result = CLEANUP.sweep(arguments, connection)

        self.assertEqual(1, result["withinGracePeriodFileCount"])
        self.assertEqual(0, result["orphanFileCount"])

    def test_move_limit_stops_after_the_requested_recoverable_volume(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cache_root = root / "cache"
            recovery_root = root / "recovery"
            recovery_root.mkdir()
            for name in ("first", "second"):
                cached = cache_root / "A" / "B" / "C" / name
                cached.parent.mkdir(parents=True, exist_ok=True)
                cached.write_bytes(b"12345")
                old = time.time() - 120
                os.utime(cached, (old, old))
            database = root / "active.sqlite"
            with CLEANUP.open_database(database) as connection:
                arguments = type("Arguments", (), {
                    "cache_root": cache_root,
                    "cancel_file": None,
                    "file_check_interval": 1,
                    "grace_period_ms": 60_000,
                    "mode": "MOVE",
                    "quarantine_root": recovery_root,
                    "max_move_bytes": 5,
                })()

                result = CLEANUP.sweep(arguments, connection)

        self.assertEqual(5, result["movedBytes"])
        self.assertTrue(result["moveLimitReached"])

    def test_move_limit_does_not_exceed_the_requested_recoverable_volume(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cache_root = root / "cache"
            candidate = cache_root / "A" / "B" / "C" / "oversized"
            recovery_root = root / "recovery"
            candidate.parent.mkdir(parents=True)
            recovery_root.mkdir()
            candidate.write_bytes(b"123456")
            old = time.time() - 120
            os.utime(candidate, (old, old))
            database = root / "active.sqlite"
            with CLEANUP.open_database(database) as connection:
                arguments = type("Arguments", (), {
                    "cache_root": cache_root,
                    "cancel_file": None,
                    "file_check_interval": 1,
                    "grace_period_ms": 60_000,
                    "mode": "MOVE",
                    "quarantine_root": recovery_root,
                    "max_move_bytes": 5,
                })()

                result = CLEANUP.sweep(arguments, connection)
                self.assertTrue(candidate.exists())

        self.assertEqual(0, result["movedBytes"])
        self.assertTrue(result["moveLimitReached"])


class ValidatedDatabaseTest(unittest.TestCase):
    """Only a finished exact scan for the same source may be reused for MOVE."""

    def test_reuses_a_completed_source_bound_database(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            database = Path(temporary) / "active.sqlite"
            with CLEANUP.open_database(database) as connection:
                CLEANUP.mark_validated_database(connection, "products", 2, 3)

                self.assertEqual((2, 3), CLEANUP.validated_database(connection, "products"))

    def test_refuses_a_database_for_another_source(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            database = Path(temporary) / "active.sqlite"
            with CLEANUP.open_database(database) as connection:
                CLEANUP.mark_validated_database(connection, "products", 2, 3)

                with self.assertRaisesRegex(CLEANUP.ResourceCleanupError, "does not belong"):
                    CLEANUP.validated_database(connection, "another-products")


if __name__ == "__main__":
    unittest.main()
