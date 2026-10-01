#!/usr/bin/env python3
"""Tests for scripts/deploy/verify_gate_proof.py's HMAC seal (GOU-174).

Reproduces the GOU-174 counter-example verbatim: a five-field JSON file, hand-typed by whoever
can run the publish script, naming the real candidate SHA, promotion target and manifest digest
within the freshness window. Before this fix it passed; this file fails if that regresses.
"""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from datetime import datetime
from pathlib import Path

DEPLOY_DIR = Path(__file__).resolve().parent.parent / "deploy"
sys.path.insert(0, str(DEPLOY_DIR))  # verify_gate_proof.py does a bare `import gate_proof_seal`


def _load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


SEAL = _load("gate_proof_seal", DEPLOY_DIR / "gate_proof_seal.py")
VERIFY = _load("verify_gate_proof", DEPLOY_DIR / "verify_gate_proof.py")

CANDIDATE_SHA = "deadbeef"
PROMOTION_TARGET = "beta"
MANIFEST_DIGEST = "a" * 64
GENERATED_AT = "2026-10-01T12:00:00Z"
NOW = datetime.fromisoformat(GENERATED_AT.replace("Z", "+00:00")).timestamp()  # proof is fresh at "now"
REAL_KEY = bytes.fromhex("22" * 32)
OTHER_KEY = bytes.fromhex("33" * 32)


def good_proof() -> dict:
    proof = {
        "ready": True,
        "candidateSha": CANDIDATE_SHA,
        "promotionTarget": PROMOTION_TARGET,
        "manifestDigest": MANIFEST_DIGEST,
        "generatedAt": GENERATED_AT,
    }
    proof["seal"] = SEAL.compute_seal(REAL_KEY, proof)
    return proof


class HandWrittenProofIsRejectedTest(unittest.TestCase):
    """The exact counter-example from the GOU-174 issue body."""

    def test_forged_json_with_no_seal_field_is_rejected(self) -> None:
        forged = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": MANIFEST_DIGEST,
            "generatedAt": GENERATED_AT,
        }
        with self.assertRaises(VERIFY.GateProofError) as ctx:
            VERIFY.verify(forged, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           hmac_key=REAL_KEY)
        self.assertIn("seal", str(ctx.exception))

    def test_forged_json_with_a_guessed_seal_is_rejected(self) -> None:
        forged = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": MANIFEST_DIGEST,
            "generatedAt": GENERATED_AT,
            "seal": "deadbeef" * 8,
        }
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(forged, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           hmac_key=REAL_KEY)

    def test_proof_sealed_with_the_wrong_key_is_rejected(self) -> None:
        proof = good_proof()  # sealed with REAL_KEY
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(proof, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           hmac_key=OTHER_KEY)


class LegitimateProofStillVerifiesTest(unittest.TestCase):
    """No regression: the proof check_promotion_readiness.py actually produces must keep passing."""

    def test_correctly_sealed_proof_is_accepted(self) -> None:
        VERIFY.verify(good_proof(), candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                       manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW, hmac_key=REAL_KEY)

    def test_tampering_with_a_sealed_field_after_the_fact_is_rejected(self) -> None:
        proof = good_proof()
        proof["candidateSha"] = "0000000"  # looks internally consistent, but the seal no longer covers it
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(proof, candidate_sha="0000000", promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           hmac_key=REAL_KEY)


class LoadHmacKeyTest(unittest.TestCase):
    def test_missing_env_var_fails_closed(self) -> None:
        import os
        from unittest import mock
        with mock.patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(VERIFY.GateProofError):
                VERIFY.load_hmac_key()

    def test_malformed_hex_fails_closed(self) -> None:
        import os
        from unittest import mock
        with mock.patch.dict(os.environ, {SEAL.ENV_VAR: "not-hex"}):
            with self.assertRaises(VERIFY.GateProofError):
                VERIFY.load_hmac_key()

    def test_short_key_fails_closed(self) -> None:
        import os
        from unittest import mock
        with mock.patch.dict(os.environ, {SEAL.ENV_VAR: "aa"}):
            with self.assertRaises(VERIFY.GateProofError):
                VERIFY.load_hmac_key()

    def test_valid_key_loads(self) -> None:
        import os
        from unittest import mock
        with mock.patch.dict(os.environ, {SEAL.ENV_VAR: REAL_KEY.hex()}):
            self.assertEqual(VERIFY.load_hmac_key(), REAL_KEY)


class MainCliEndToEndTest(unittest.TestCase):
    """Drives main() exactly as publish-java-release.sh / publish-nuxt-release.sh do."""

    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def write(self, proof: dict) -> str:
        path = self.root / "proof.json"
        path.write_text(json.dumps(proof), encoding="utf-8")
        return str(path)

    def run_main(self, proof_path: str, hmac_key_hex: str | None) -> int:
        import os
        from unittest import mock
        env = {} if hmac_key_hex is None else {SEAL.ENV_VAR: hmac_key_hex}
        with mock.patch.dict(os.environ, env, clear=hmac_key_hex is None):
            return VERIFY.main([
                "--proof", proof_path, "--candidate-sha", CANDIDATE_SHA,
                "--promotion-target", PROMOTION_TARGET, "--manifest-digest", MANIFEST_DIGEST,
                "--now", str(NOW),
            ])

    def test_hand_written_proof_rejected_via_cli(self) -> None:
        forged_path = self.write({
            "ready": True, "candidateSha": CANDIDATE_SHA, "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": MANIFEST_DIGEST, "generatedAt": GENERATED_AT,
        })
        rc = self.run_main(forged_path, REAL_KEY.hex())
        self.assertNotEqual(rc, 0)

    def test_legitimate_proof_accepted_via_cli(self) -> None:
        good_path = self.write(good_proof())
        rc = self.run_main(good_path, REAL_KEY.hex())
        self.assertEqual(rc, 0)

    def test_no_key_configured_on_the_deploy_host_fails_closed(self) -> None:
        good_path = self.write(good_proof())
        rc = self.run_main(good_path, None)
        self.assertNotEqual(rc, 0)


if __name__ == "__main__":
    unittest.main()
