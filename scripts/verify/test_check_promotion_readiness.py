#!/usr/bin/env python3
"""Tests for the local promotion readiness gate (scripts/verify/check_promotion_readiness.py)."""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("check_promotion_readiness.py")
SPEC = importlib.util.spec_from_file_location("check_promotion_readiness", MODULE_PATH)
assert SPEC and SPEC.loader
GATE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GATE
SPEC.loader.exec_module(GATE)

CANDIDATE_SHA = "a" * 40
DATASET = "products-backup-2026-09-30"
PROMOTION_TARGET = "beta"
PHASE = "beta_validation"
PINNED_AT = "2026-09-30T00:00:00Z"


def phase_label(phase: str) -> dict:
    return {"name": f"phase: {phase}"}


def issue(identifier: str, phase: str, status: str = "done", created_at: str = "2026-09-01T00:00:00Z",
          extra_labels: list[dict] | None = None) -> dict:
    labels = [phase_label(phase)] if phase is not None else []
    labels.extend(extra_labels or [])
    return {
        "id": identifier,
        "identifier": identifier,
        "status": status,
        "createdAt": created_at,
        "labels": labels,
    }


def valid_decision(**overrides) -> dict:
    decision = {
        "kind": "request_confirmation",
        "status": "resolved",
        "resolution": "accepted",
        "resolvedBy": {"type": "human", "email": "goulven.furet@gmail.com"},
        "candidateSha": CANDIDATE_SHA,
        "dataset": DATASET,
        "promotionTarget": PROMOTION_TARGET,
        "authorizedPhase": PHASE,
        "manifestDigest": None,  # filled in per-test once the manifest digest is known
        "pinnedAt": PINNED_AT,
    }
    decision.update(overrides)
    return decision


class TempRepoTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.cache_path = self.root / "cache.json"
        self.manifest_path = self.root / "release-manifest"
        self.write_manifest()

    def tearDown(self) -> None:
        self.temp.cleanup()

    def write_manifest(self, release: str = CANDIDATE_SHA, contract: str = "v1") -> None:
        self.manifest_path.write_text(
            f"format=1\nrelease={release}\ncontract={contract}\n"
            "artifact api deadbeef\n",
            encoding="utf-8",
        )

    def digest(self) -> str:
        return GATE.manifest_digest(self.manifest_path)

    def build(self, issues: list[dict], decision: dict) -> dict:
        return GATE.build_report(
            issues=issues, cache_path=self.cache_path, manifest_path=self.manifest_path, decision=decision,
            candidate_sha=CANDIDATE_SHA, dataset=DATASET, promotion_target=PROMOTION_TARGET, phase=PHASE,
        )


class CleanPassTest(TempRepoTestCase):
    def test_clean_project_is_ready(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [
            issue("GOU-1", "development"),
            issue("GOU-2", "beta_validation"),
        ]
        report = self.build(issues, decision)
        self.assertEqual(report["blockers"], [])
        self.assertTrue(report["ready"])


class MissingPhaseLabelTest(TempRepoTestCase):
    def test_issue_without_a_phase_label_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", None)]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("missing_or_multiple_phase", reasons)
        self.assertFalse(report["ready"])


class MultiplePhaseLabelsTest(TempRepoTestCase):
    def test_issue_with_two_phase_labels_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development", extra_labels=[phase_label("production")])]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("missing_or_multiple_phase", reasons)
        self.assertFalse(report["ready"])


class OpenDevelopmentTaskTest(TempRepoTestCase):
    def test_open_development_task_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development", status="open")]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("development_task_open", reasons)
        self.assertFalse(report["ready"])

    def test_reopened_development_task_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development", status="reopened")]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("development_task_open", reasons)


class NewDevelopmentTaskAfterPinTest(TempRepoTestCase):
    def test_development_task_created_after_pin_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development", created_at="2026-10-01T00:00:00Z")]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("development_task_created_after_pin", reasons)
        self.assertFalse(report["ready"])

    def test_non_development_task_created_after_pin_is_not_flagged(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "production", created_at="2026-10-01T00:00:00Z")]
        report = self.build(issues, decision)
        self.assertEqual(report["blockers"], [])


class StaleManifestTest(TempRepoTestCase):
    def test_manifest_digest_drift_since_pin_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest="0" * 64)
        issues = [issue("GOU-1", "development")]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("manifest_stale", reasons)
        self.assertFalse(report["ready"])

    def test_manifest_release_not_matching_candidate_is_a_blocker(self) -> None:
        self.write_manifest(release="b" * 40)
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development")]
        report = self.build(issues, decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("manifest_candidate_mismatch", reasons)

    def test_missing_manifest_file_fails_closed(self) -> None:
        self.manifest_path.unlink()
        decision = valid_decision(manifestDigest="0" * 64)
        with self.assertRaises(GATE.ReadinessError):
            self.build([issue("GOU-1", "development")], decision)


class OwnerDecisionInvalidTest(TempRepoTestCase):
    def test_missing_decision_source_fails_closed(self) -> None:
        rc = GATE.main([
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(self.write_offline_issues([])),
        ])
        self.assertEqual(rc, 2)

    def test_decision_missing_required_field_is_a_blocker(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        del decision["resolvedBy"]
        report = self.build([issue("GOU-1", "development")], decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("owner_decision_invalid", reasons)
        self.assertFalse(report["ready"])

    def test_workflow_dispatch_style_decision_is_rejected(self) -> None:
        """Not an ask_user_questions/request_confirmation/signed record: never treated as authorization."""
        decision = valid_decision(manifestDigest=self.digest(), kind="workflow_dispatch")
        report = self.build([issue("GOU-1", "development")], decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("owner_decision_invalid", reasons)

    def test_ci_green_is_not_a_decision(self) -> None:
        """A CI-resolved (non-human) actor never counts as owner authorization."""
        decision = valid_decision(manifestDigest=self.digest(),
                                   resolvedBy={"type": "ci", "system": "github-actions"})
        report = self.build([issue("GOU-1", "development")], decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("owner_decision_invalid", reasons)

    def test_decision_naming_a_different_candidate_is_rejected(self) -> None:
        decision = valid_decision(manifestDigest=self.digest(), candidateSha="c" * 40)
        report = self.build([issue("GOU-1", "development")], decision)
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("owner_decision_invalid", reasons)

    def test_forged_decision_missing_from_disk_fails_closed(self) -> None:
        rc = GATE.main([
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(self.write_offline_issues([])),
            "--decision", str(self.root / "does-not-exist.json"),
        ])
        self.assertEqual(rc, 2)

    def write_offline_issues(self, issues: list[dict]) -> Path:
        path = self.root / "issues.json"
        path.write_text(json.dumps(issues), encoding="utf-8")
        return path


class IssueCountReconciliationTest(TempRepoTestCase):
    def test_short_page_relative_to_cache_fails_closed(self) -> None:
        self.cache_path.write_text(json.dumps({"lastKnownIssueCount": 5}), encoding="utf-8")
        decision = valid_decision(manifestDigest=self.digest())
        with self.assertRaises(GATE.ReadinessError):
            self.build([issue("GOU-1", "development")], decision)

    def test_growing_count_updates_the_cache(self) -> None:
        self.cache_path.write_text(json.dumps({"lastKnownIssueCount": 1}), encoding="utf-8")
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development"), issue("GOU-2", "beta_validation")]
        report = self.build(issues, decision)
        self.assertTrue(report["ready"])
        cached = json.loads(self.cache_path.read_text(encoding="utf-8"))
        self.assertEqual(cached["lastKnownIssueCount"], 2)

    def test_empty_inventory_fails_closed(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        with self.assertRaises(GATE.ReadinessError):
            self.build([], decision)

    def test_malformed_cache_fails_closed(self) -> None:
        self.cache_path.write_text("not json", encoding="utf-8")
        decision = valid_decision(manifestDigest=self.digest())
        with self.assertRaises(GATE.ReadinessError):
            self.build([issue("GOU-1", "development")], decision)


class _RaisingOpener:
    """Stands in for urllib.request's opener to simulate a network failure deterministically."""

    def open(self, request, timeout=None):
        raise OSError("simulated connection failure")


class ApiFailureTest(TempRepoTestCase):
    def test_non_https_base_fails_closed(self) -> None:
        with self.assertRaises(GATE.ReadinessError):
            GATE.fetch_issues("http://example.invalid", "token", "company-1", "proj-1")

    def test_missing_token_fails_closed(self) -> None:
        with self.assertRaises(GATE.ReadinessError):
            GATE.fetch_issues("https://example.invalid", "", "company-1", "proj-1")

    def test_network_failure_fails_closed_not_no_blockers(self) -> None:
        # A network failure must raise, never silently resolve to an empty,
        # "clean" inventory.
        original_build_opener = GATE.urllib.request.build_opener
        GATE.urllib.request.build_opener = lambda *args, **kwargs: _RaisingOpener()
        try:
            with self.assertRaises(GATE.ReadinessError):
                GATE.fetch_issues("https://example.invalid", "token", "company-1", "proj-1")
        finally:
            GATE.urllib.request.build_opener = original_build_opener

    def test_main_never_exits_zero_on_api_failure(self) -> None:
        original_build_opener = GATE.urllib.request.build_opener
        GATE.urllib.request.build_opener = lambda *args, **kwargs: _RaisingOpener()
        try:
            rc = GATE.main([
                "--project-id", "proj-1", "--company-id", "company-1",
                "--api-base", "https://example.invalid", "--api-token", "token",
                "--cache-file", str(self.cache_path), "--manifest", str(self.manifest_path),
                "--candidate-sha", CANDIDATE_SHA, "--dataset", DATASET,
                "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
                "--decision", str(self.write_decision(valid_decision(manifestDigest=self.digest()))),
            ])
        finally:
            GATE.urllib.request.build_opener = original_build_opener
        self.assertNotEqual(rc, 0)

    def write_decision(self, decision: dict) -> Path:
        path = self.root / "decision.json"
        path.write_text(json.dumps(decision), encoding="utf-8")
        return path


class MainEndToEndTest(TempRepoTestCase):
    def test_clean_run_via_cli_exits_zero(self) -> None:
        decision_path = self.root / "decision.json"
        decision_path.write_text(json.dumps(valid_decision(manifestDigest=self.digest())), encoding="utf-8")
        issues_path = self.root / "issues.json"
        issues_path.write_text(json.dumps([issue("GOU-1", "development")]), encoding="utf-8")
        rc = GATE.main([
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(issues_path), "--decision", str(decision_path),
        ])
        self.assertEqual(rc, 0)

    def test_blockers_exit_one_not_zero(self) -> None:
        decision_path = self.root / "decision.json"
        decision_path.write_text(json.dumps(valid_decision(manifestDigest=self.digest())), encoding="utf-8")
        issues_path = self.root / "issues.json"
        issues_path.write_text(json.dumps([issue("GOU-1", "development", status="open")]), encoding="utf-8")
        rc = GATE.main([
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(issues_path), "--decision", str(decision_path),
        ])
        self.assertEqual(rc, 1)

    def test_malformed_candidate_sha_fails_closed(self) -> None:
        rc = GATE.main([
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", "not-a-sha",
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(self.root / "missing.json"),
        ])
        self.assertEqual(rc, 2)


if __name__ == "__main__":
    unittest.main()
