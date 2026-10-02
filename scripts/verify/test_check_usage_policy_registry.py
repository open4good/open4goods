#!/usr/bin/env python3
"""Tests for the source usage policy registry guard (scripts/verify/check_usage_policy_registry.py)."""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("check_usage_policy_registry.py")
SPEC = importlib.util.spec_from_file_location("check_usage_policy_registry", MODULE_PATH)
assert SPEC and SPEC.loader
GUARD = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GUARD
SPEC.loader.exec_module(GUARD)


def registry_document(pairs: list[tuple[str, str]]) -> str:
    return json.dumps({
        "schemaVersion": "https://open4goods.org/schema/source-usage-policy-2.json",
        "policies": [{"policyId": policy_id, "version": version} for policy_id, version in pairs],
    })


class FindCitedReferencesTest(unittest.TestCase):
    def test_finds_a_literal_reference_constant(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            java = root / "src/main/java/Adapter.java"
            java.parent.mkdir(parents=True)
            java.write_text(
                'public static final SourceUsagePolicyRef USAGE_POLICY = '
                'new SourceUsagePolicyRef("icecat-open-content", "2");')

            cited = GUARD.find_cited_references(root)

            self.assertIn(("icecat-open-content", "2"), cited)
            self.assertIn("src/main/java/Adapter.java", cited[("icecat-open-content", "2")])

    def test_finds_a_denyall_policy_construction(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            java = root / "src/main/java/LegacyBackupUsagePolicy.java"
            java.parent.mkdir(parents=True)
            java.write_text(
                'public static final SourceUsagePolicy POLICY = SourceUsagePolicy.denyAll(\n'
                '        "legacy-backup-import", SOURCE_ID, "1", Instant.parse("2026-09-30T00:00:00Z"),\n'
                '        LocalDate.of(2026, 9, 30));')

            cited = GUARD.find_cited_references(root)

            self.assertIn(("legacy-backup-import", "1"), cited)

    def test_ignores_test_sources(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            java = root / "src/test/java/FixtureTest.java"
            java.parent.mkdir(parents=True)
            java.write_text('new SourceUsagePolicyRef("fixture", "1");')

            cited = GUARD.find_cited_references(root)

            self.assertEqual(cited, {})


class CheckNoPendingReferencesTest(unittest.TestCase):
    def test_fails_on_a_reference_missing_from_the_registry(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            policy_dir = root / "services/data-reference/src/main/resources/policy"
            policy_dir.mkdir(parents=True)
            (policy_dir / "source-usage-policies.json").write_text(
                registry_document([("icecat-open-content", "2")]))
            java = root / "src/main/java/LegacyBackupUsagePolicy.java"
            java.parent.mkdir(parents=True)
            java.write_text('SourceUsagePolicy.denyAll("legacy-backup-import", SOURCE_ID, "1", x, y);')

            problems = GUARD.check_no_pending_references(root)

            self.assertEqual(len(problems), 1)
            self.assertIn("legacy-backup-import/1", problems[0])

    def test_passes_once_the_reference_is_registered(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            policy_dir = root / "services/data-reference/src/main/resources/policy"
            policy_dir.mkdir(parents=True)
            (policy_dir / "source-usage-policies.json").write_text(
                registry_document([("icecat-open-content", "2"), ("legacy-backup-import", "1")]))
            java = root / "src/main/java/LegacyBackupUsagePolicy.java"
            java.parent.mkdir(parents=True)
            java.write_text('SourceUsagePolicy.denyAll("legacy-backup-import", SOURCE_ID, "1", x, y);')

            self.assertEqual(GUARD.check_no_pending_references(root), [])


class CheckNoSilentRemovalTest(unittest.TestCase):
    def test_fails_when_a_version_disappears(self):
        old_text = registry_document([("icecat-open-content", "1"), ("eprel-public-api", "2")])
        new_text = registry_document([("icecat-open-content", "2"), ("eprel-public-api", "2")])

        problems = GUARD.check_no_silent_removal(old_text, new_text)

        self.assertEqual(len(problems), 1)
        self.assertIn("icecat-open-content/1", problems[0])

    def test_passes_when_every_old_version_is_still_present(self):
        old_text = registry_document([("icecat-open-content", "1"), ("eprel-public-api", "2")])
        new_text = registry_document(
            [("icecat-open-content", "1"), ("icecat-open-content", "2"), ("eprel-public-api", "2")])

        self.assertEqual(GUARD.check_no_silent_removal(old_text, new_text), [])

    def test_passes_when_nothing_changed(self):
        text = registry_document([("eprel-public-api", "2")])

        self.assertEqual(GUARD.check_no_silent_removal(text, text), [])


class RepositoryStateTest(unittest.TestCase):
    """Runs the guard against this actual repository's current registry and sources."""

    def test_the_real_repository_has_no_pending_reference(self):
        repo_root = Path(__file__).resolve().parents[2]
        problems = GUARD.check_no_pending_references(repo_root)
        self.assertEqual(problems, [], "\n".join(problems))


if __name__ == "__main__":
    unittest.main()
