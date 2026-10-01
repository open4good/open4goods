#!/usr/bin/env python3
"""Tests for the local promotion readiness gate (scripts/verify/check_promotion_readiness.py)."""

from __future__ import annotations

import importlib.util
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey


MODULE_PATH = Path(__file__).with_name("check_promotion_readiness.py")
SPEC = importlib.util.spec_from_file_location("check_promotion_readiness", MODULE_PATH)
assert SPEC and SPEC.loader
GATE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GATE
SPEC.loader.exec_module(GATE)

# Test-only Ed25519 keypair (GOU-174/GOU-177): not the real pipeline key, just a disposable
# fixture keypair for these offline tests.
TEST_SIGNING_KEY_HEX = "11" * 32
TEST_SIGNING_KEY = Ed25519PrivateKey.from_private_bytes(bytes.fromhex(TEST_SIGNING_KEY_HEX))
TEST_PUBLIC_KEY_HEX = TEST_SIGNING_KEY.public_key().public_bytes(
    encoding=serialization.Encoding.Raw, format=serialization.PublicFormat.Raw).hex()

CANDIDATE_SHA = "a" * 40
DATASET = "products-backup-2026-09-30"
PROMOTION_TARGET = "production"
PHASE = "beta_validation"
PINNED_AT = "2026-09-30T00:00:00Z"

MANDATE_IDS = dict(
    mandate_milestone_id="GOU-150", mandate_goal_id="goal-1",
    mandate_comment_id="comment-1", mandate_owner_id="owner-1",
    mandate_qualification_id="qual-1",
)


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
        # main() refuses to seal a report without this; build_report() itself is unaffected.
        self.enterContext(mock.patch.dict(
            os.environ, {GATE.gate_proof_seal.SIGNING_KEY_ENV_VAR: TEST_SIGNING_KEY_HEX}))

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
    # Real Paperclip status vocabulary: backlog, todo, in_progress, in_review,
    # blocked, done, cancelled. Only done/cancelled are terminal; everything
    # else -- including a status this script has never seen -- must block.
    NON_TERMINAL_STATUSES = ("backlog", "todo", "in_progress", "in_review", "blocked", "unknown_future_status")

    def test_non_terminal_development_task_is_a_blocker(self) -> None:
        for status in self.NON_TERMINAL_STATUSES:
            with self.subTest(status=status):
                decision = valid_decision(manifestDigest=self.digest())
                issues = [issue("GOU-1", "development", status=status)]
                report = self.build(issues, decision)
                reasons = {b["reason"] for b in report["blockers"]}
                self.assertIn("development_task_open", reasons)
                self.assertFalse(report["ready"])

    def test_cancelled_development_task_is_terminal(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [issue("GOU-1", "development", status="cancelled")]
        report = self.build(issues, decision)
        self.assertEqual(report["blockers"], [])
        self.assertTrue(report["ready"])


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


class RealisticInventoryRegressionTest(TempRepoTestCase):
    """Reproduces the PR #3353 review finding: a realistic-sized inventory where
    every issue carries a valid phase label but one DEVELOPMENT task is not
    terminal must never report ready, regardless of how many other issues are
    done/cancelled."""

    ALL_STATUSES = ("backlog", "todo", "in_progress", "in_review", "blocked", "done", "cancelled")

    def test_large_inventory_with_one_non_terminal_development_task_blocks(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [
            issue(f"GOU-{n}", "development", status="done")
            for n in range(40)
        ]
        issues += [issue(f"GOU-{n}", "beta_validation", status="done") for n in range(40, 44)]
        # The one non-terminal DEVELOPMENT task, buried in an otherwise-clean inventory.
        issues.append(issue("GOU-94", "development", status="blocked"))
        report = self.build(issues, decision)
        self.assertFalse(report["ready"])
        reasons = {b["reason"] for b in report["blockers"]}
        self.assertIn("development_task_open", reasons)

    def test_large_inventory_all_terminal_is_ready(self) -> None:
        decision = valid_decision(manifestDigest=self.digest())
        issues = [
            issue(f"GOU-{n}", "development", status="done" if n % 2 else "cancelled")
            for n in range(50)
        ]
        report = self.build(issues, decision)
        self.assertEqual(report["blockers"], [])
        self.assertTrue(report["ready"])


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


class GateProofSealTest(TempRepoTestCase):
    """GOU-174: the printed report must be sealed, and sealing must itself fail closed."""

    def cli_args(self, issues_path: Path, decision_path: Path) -> list[str]:
        return [
            "--project-id", "proj-1", "--cache-file", str(self.cache_path),
            "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
            "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
            "--issues", str(issues_path), "--decision", str(decision_path),
        ]

    def write_clean_fixtures(self) -> list[str]:
        decision_path = self.root / "decision.json"
        decision_path.write_text(json.dumps(valid_decision(manifestDigest=self.digest())), encoding="utf-8")
        issues_path = self.root / "issues.json"
        issues_path.write_text(json.dumps([issue("GOU-1", "development")]), encoding="utf-8")
        return self.cli_args(issues_path, decision_path)

    def test_clean_pass_is_sealed_and_the_seal_verifies(self) -> None:
        stdout = self.run_and_capture_stdout(self.write_clean_fixtures())
        report = json.loads(stdout)
        self.assertTrue(report["ready"])
        public_key = GATE.gate_proof_seal.load_public_key_from_hex(TEST_PUBLIC_KEY_HEX)
        self.assertTrue(GATE.gate_proof_seal.verify_seal(public_key, report, report.get("seal")))

    def test_hand_written_report_does_not_verify_against_the_real_key(self) -> None:
        """Reproduces the GOU-174 counter-example: five correct-looking fields, hand-typed, with
        no seal at all, must not authenticate -- the defect this issue closes."""
        forged = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": self.digest(),
            "generatedAt": "2026-10-01T00:00:00Z",
        }
        public_key = GATE.gate_proof_seal.load_public_key_from_hex(TEST_PUBLIC_KEY_HEX)
        self.assertFalse(GATE.gate_proof_seal.verify_seal(public_key, forged, forged.get("seal")))
        forged["seal"] = "deadbeef" * 8
        self.assertFalse(GATE.gate_proof_seal.verify_seal(public_key, forged, forged["seal"]))

    def test_missing_signing_key_fails_closed(self) -> None:
        args = self.write_clean_fixtures()
        with mock.patch.dict(os.environ, {}, clear=False):
            del os.environ[GATE.gate_proof_seal.SIGNING_KEY_ENV_VAR]
            rc = GATE.main(args)
        self.assertEqual(rc, 2)

    def test_malformed_signing_key_fails_closed(self) -> None:
        args = self.write_clean_fixtures()
        with mock.patch.dict(os.environ, {GATE.gate_proof_seal.SIGNING_KEY_ENV_VAR: "not-hex"}):
            rc = GATE.main(args)
        self.assertEqual(rc, 2)

    def test_short_signing_key_fails_closed(self) -> None:
        args = self.write_clean_fixtures()
        with mock.patch.dict(os.environ, {GATE.gate_proof_seal.SIGNING_KEY_ENV_VAR: "aa"}):
            rc = GATE.main(args)
        self.assertEqual(rc, 2)

    def test_deploy_host_with_only_the_public_key_cannot_forge_a_seal(self) -> None:
        """GOU-177: unlike the GOU-174 HMAC, the deploy host's public key is not enough to sign a
        new proof -- only scripts/verify/check_promotion_readiness.py's O4G_GATE_PROOF_SIGNING_KEY
        can. This is the property the symmetric scheme could not offer."""
        public_key = GATE.gate_proof_seal.load_public_key_from_hex(TEST_PUBLIC_KEY_HEX)
        forged = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": self.digest(),
            "generatedAt": "2026-10-01T00:00:00Z",
        }
        with self.assertRaises(Exception):
            # A public key object exposes no signing method at all: an attacker who extracted
            # only the public key (e.g. by reading the deploy host's environment or repo file)
            # has no API surface here that produces a signature, unlike an HMAC key which signs
            # and verifies with the same bytes.
            public_key.sign(GATE.gate_proof_seal.canonical_form(forged))

    def run_and_capture_stdout(self, args: list[str]) -> str:
        import contextlib
        import io
        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            rc = GATE.main(args)
        self.assertEqual(rc, 0)
        return buffer.getvalue()


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


class BetaMandateCompositionTest(TempRepoTestCase):
    """GOU-150/GOU-151: beta composes with, never replaces, the live mandate check."""

    def cli_args(self, decision_path: Path | None, issues_path: Path, **mandate_overrides) -> list[str]:
        args = [
            "--project-id", "proj-1", "--company-id", "company-1",
            "--api-base", "https://example.invalid", "--api-token", "token",
            "--cache-file", str(self.cache_path), "--manifest", str(self.manifest_path),
            "--candidate-sha", CANDIDATE_SHA, "--dataset", DATASET,
            "--promotion-target", "beta", "--phase", PHASE,
            "--issues", str(issues_path),
        ]
        if decision_path is not None:
            args += ["--decision", str(decision_path)]
        else:
            args += ["--decision-issue-id", "GOU-150", "--decision-interaction-id", "interaction-1"]
        mandate = dict(MANDATE_IDS)
        mandate.update(mandate_overrides)
        for key, value in mandate.items():
            if value is not None:
                args += ["--" + key.replace("_", "-"), value]
        return args

    def write_issues(self, issues: list[dict]) -> Path:
        path = self.root / "issues.json"
        path.write_text(json.dumps(issues), encoding="utf-8")
        return path

    def test_local_decision_file_is_refused_for_beta(self) -> None:
        decision_path = self.root / "decision.json"
        decision_path.write_text(
            json.dumps(valid_decision(manifestDigest=self.digest(), promotionTarget="beta")),
            encoding="utf-8")
        rc = GATE.main(self.cli_args(decision_path, self.write_issues([issue("GOU-1", "development")])))
        self.assertEqual(rc, 2)

    def test_missing_mandate_identities_fails_closed(self) -> None:
        rc = GATE.main(self.cli_args(
            None, self.write_issues([issue("GOU-1", "development")]), mandate_comment_id=None))
        self.assertEqual(rc, 2)

    def test_mandate_failure_fails_closed(self) -> None:
        original = GATE.beta_mandate.check_live
        GATE.beta_mandate.check_live = lambda *a, **k: (_ for _ in ()).throw(
            GATE.beta_mandate.MandateError("simulated mandate refusal"))
        try:
            rc = GATE.main(self.cli_args(None, self.write_issues([issue("GOU-1", "development")])))
        finally:
            GATE.beta_mandate.check_live = original
        self.assertEqual(rc, 2)

    def test_clean_beta_pass_composes_live_decision_and_mandate(self) -> None:
        decision = valid_decision(manifestDigest=self.digest(), promotionTarget="beta")
        original_fetch_decision = GATE.fetch_decision
        original_check_live = GATE.beta_mandate.check_live
        GATE.fetch_decision = lambda *a, **k: decision
        GATE.beta_mandate.check_live = lambda *a, **k: {
            "mandateValid": True, "authorized": False, "target": "beta",
        }
        try:
            rc = GATE.main(self.cli_args(None, self.write_issues([issue("GOU-1", "development")])))
        finally:
            GATE.fetch_decision = original_fetch_decision
            GATE.beta_mandate.check_live = original_check_live
        self.assertEqual(rc, 0)


class MissingCryptographyDependencyFailsClosedTest(TempRepoTestCase):
    """If `cryptography` is not installed where check_promotion_readiness.py runs, main() must
    refuse with a readable stderr message and a non-zero exit -- never a bare ModuleNotFoundError
    traceback (GOU-177 review on PR #3399: the import itself must stay fail-closed)."""

    def test_main_rejects_cleanly_when_cryptography_is_unavailable(self) -> None:
        decision_path = self.root / "decision.json"
        decision_path.write_text(json.dumps(valid_decision(manifestDigest=self.digest())), encoding="utf-8")
        issues_path = self.root / "issues.json"
        issues_path.write_text(json.dumps([issue("GOU-1", "development")]), encoding="utf-8")
        fake_error = ModuleNotFoundError("No module named 'cryptography'")
        stderr = io.StringIO()
        with mock.patch.object(GATE, "GATE_PROOF_SEAL_IMPORT_ERROR", fake_error):
            with mock.patch("sys.stderr", stderr):
                rc = GATE.main([
                    "--project-id", "proj-1", "--cache-file", str(self.cache_path),
                    "--manifest", str(self.manifest_path), "--candidate-sha", CANDIDATE_SHA,
                    "--dataset", DATASET, "--promotion-target", PROMOTION_TARGET, "--phase", PHASE,
                    "--issues", str(issues_path), "--decision", str(decision_path),
                ])
        self.assertEqual(rc, 2)
        message = stderr.getvalue()
        self.assertIn("cryptography", message)
        self.assertIn("GOU-177", message)
        self.assertNotIn("Traceback", message)


if __name__ == "__main__":
    unittest.main()
