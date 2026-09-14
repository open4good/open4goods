"""Tests for provenance-safe Amazon PA-API quarantine classification."""

from __future__ import annotations

import importlib.util
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("quarantine_amazon_paapi.py")
SPEC = importlib.util.spec_from_file_location("quarantine_amazon_paapi", MODULE_PATH)
assert SPEC and SPEC.loader
QUARANTINE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = QUARANTINE
SPEC.loader.exec_module(QUARANTINE)


class ClassificationTest(unittest.TestCase):
    """Proves exact provenance rules and minimal identity preservation."""

    def test_exact_paapi_label_is_proven(self) -> None:
        self.assertEqual("PROVEN_PAAPI", QUARANTINE.classify({"datasourceNames": ["amazon-paapi"]}))

    def test_shared_amazon_label_is_ambiguous_not_proven(self) -> None:
        self.assertEqual("AMBIGUOUS_AMAZON", QUARANTINE.classify({"datasourceNames": ["amazon.fr"]}))

    def test_amazon_url_alone_is_not_a_provenance_claim(self) -> None:
        self.assertEqual("INDEPENDENT_MERCHANT", QUARANTINE.classify({"officialUrl": "https://amazon.fr/dp/B000TEST00"}))

    def test_identity_projection_preserves_only_valid_asins(self) -> None:
        identity = QUARANTINE.identity_document("legacy-1", {"externalIds": {"asin": "B000TEST00", "other": "x"}})

        self.assertEqual({"legacySourceId": "legacy-1", "asins": ["B000TEST00"],
                          "provenance": "AMBIGUOUS_LEGACY_AMAZON"}, identity)

    def test_inventory_content_types_retain_no_values(self) -> None:
        types = QUARANTINE.content_types({"resources": [{"url": "private"}], "price": {"offers": []}})

        self.assertEqual({"IDENTITY", "MEDIA", "OFFER_OR_PRICE"}, types)

    def test_rejects_checkpoint_from_a_different_mode(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            checkpoint = Path(temporary) / "checkpoint.json"
            QUARANTINE.write_json(checkpoint, {"source": "source", "mode": "INVENTORY", "searchAfter": [1]})

            self.assertEqual("INVENTORY", QUARANTINE.load_checkpoint(checkpoint)["mode"])

    def test_inventory_requires_exact_cache_owner_and_checksum(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cached = root / "entry"
            cached.write_bytes(b"approved-paapi-cache-object")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps([{
                "owner": "amazon-paapi", "path": str(cached),
                "sha256": hashlib.sha256(cached.read_bytes()).hexdigest(),
            }]), encoding="utf-8")

            result = QUARANTINE.cache_manifest(manifest, root, False)

            self.assertEqual("INVENTORIED", result["status"])
            self.assertEqual(cached.stat().st_size, result["bytes"])

    def test_delete_requires_a_recovery_reference(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cached = root / "entry"
            cached.write_bytes(b"approved-paapi-cache-object")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps([{
                "owner": "amazon-paapi", "path": str(cached),
                "sha256": hashlib.sha256(cached.read_bytes()).hexdigest(),
            }]), encoding="utf-8")

            with self.assertRaisesRegex(QUARANTINE.MigrationError, "recovery"):
                QUARANTINE.cache_manifest(manifest, root, True)
            self.assertTrue(cached.exists())


if __name__ == "__main__":
    unittest.main()
