#!/usr/bin/env python3
"""Tests for scripts/deploy/verify_gate_proof.py's Ed25519 seal (GOU-174, GOU-177).

Reproduces the GOU-174 counter-example verbatim: a five-field JSON file, hand-typed by whoever
can run the publish script, naming the real candidate SHA, promotion target and manifest digest
within the freshness window. Before the GOU-174 fix it passed; this file fails if that regresses.

Also proves the GOU-177 property an HMAC seal could not offer: a deploy host holding only the
public key cannot mint a new valid seal, even with full read access to its own environment.
"""

from __future__ import annotations

import importlib.util
import json
import os
import sys
import tempfile
import unittest
from datetime import datetime
from pathlib import Path
from unittest import mock

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

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

REAL_SIGNING_KEY = Ed25519PrivateKey.from_private_bytes(bytes.fromhex("22" * 32))
REAL_PUBLIC_KEY = REAL_SIGNING_KEY.public_key()
OTHER_SIGNING_KEY = Ed25519PrivateKey.from_private_bytes(bytes.fromhex("33" * 32))
OTHER_PUBLIC_KEY = OTHER_SIGNING_KEY.public_key()


def _public_key_hex(public_key) -> str:
    return public_key.public_bytes(
        encoding=serialization.Encoding.Raw, format=serialization.PublicFormat.Raw).hex()


def good_proof() -> dict:
    proof = {
        "ready": True,
        "candidateSha": CANDIDATE_SHA,
        "promotionTarget": PROMOTION_TARGET,
        "manifestDigest": MANIFEST_DIGEST,
        "generatedAt": GENERATED_AT,
    }
    proof["seal"] = SEAL.compute_seal(REAL_SIGNING_KEY, proof)
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
                           public_key=REAL_PUBLIC_KEY)
        self.assertIn("seal", str(ctx.exception))

    def test_forged_json_with_a_guessed_seal_is_rejected(self) -> None:
        forged = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": MANIFEST_DIGEST,
            "generatedAt": GENERATED_AT,
            "seal": "deadbeef" * 16,
        }
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(forged, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           public_key=REAL_PUBLIC_KEY)

    def test_proof_sealed_with_the_wrong_key_is_rejected(self) -> None:
        proof = good_proof()  # signed with REAL_SIGNING_KEY
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(proof, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           public_key=OTHER_PUBLIC_KEY)


class DeployHostCannotForgeASealTest(unittest.TestCase):
    """GOU-177: the property an HMAC seal could not offer. Holding only the public key -- which
    is all a deploy host, or anyone reading the publish script's environment, ever has -- is not
    enough to produce a signature a verifier will accept."""

    def test_public_key_object_exposes_no_signing_capability(self) -> None:
        # An Ed25519PublicKey has a verify() method but no sign() method at all: there is no API
        # call, however the deploy host's environment is read, that turns a public key into a
        # valid seal. This is structurally different from the GOU-174 HMAC key, which was the
        # same bytes on both the signing and verifying side.
        self.assertFalse(hasattr(REAL_PUBLIC_KEY, "sign"))

    def test_proof_forged_with_an_unrelated_key_does_not_verify_against_the_real_public_key(self) -> None:
        # Simulates an attacker who, having only ever seen REAL_PUBLIC_KEY, generates their own
        # keypair and signs a proof with it -- the only forgery attempt possible without the real
        # private key. It is rejected.
        forged_proof = {
            "ready": True,
            "candidateSha": CANDIDATE_SHA,
            "promotionTarget": PROMOTION_TARGET,
            "manifestDigest": MANIFEST_DIGEST,
            "generatedAt": GENERATED_AT,
        }
        forged_proof["seal"] = SEAL.compute_seal(OTHER_SIGNING_KEY, forged_proof)
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(forged_proof, candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           public_key=REAL_PUBLIC_KEY)


class LegitimateProofStillVerifiesTest(unittest.TestCase):
    """No regression: the proof check_promotion_readiness.py actually produces must keep passing."""

    def test_correctly_sealed_proof_is_accepted(self) -> None:
        VERIFY.verify(good_proof(), candidate_sha=CANDIDATE_SHA, promotion_target=PROMOTION_TARGET,
                       manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                       public_key=REAL_PUBLIC_KEY)

    def test_tampering_with_a_sealed_field_after_the_fact_is_rejected(self) -> None:
        proof = good_proof()
        proof["candidateSha"] = "0000000"  # looks internally consistent, but the seal no longer covers it
        with self.assertRaises(VERIFY.GateProofError):
            VERIFY.verify(proof, candidate_sha="0000000", promotion_target=PROMOTION_TARGET,
                           manifest_digest=MANIFEST_DIGEST, max_age_seconds=900, now=NOW,
                           public_key=REAL_PUBLIC_KEY)


class LoadPublicKeyTest(unittest.TestCase):
    def test_missing_env_var_and_missing_file_fails_closed(self) -> None:
        with mock.patch.dict(os.environ, {}, clear=True):
            with mock.patch.object(SEAL, "PUBLIC_KEY_FILE", Path("/nonexistent/gate_proof_public_key.hex")):
                with self.assertRaises(VERIFY.GateProofError):
                    VERIFY.load_public_key()

    def test_malformed_hex_fails_closed(self) -> None:
        with mock.patch.dict(os.environ, {SEAL.PUBLIC_KEY_ENV_VAR: "not-hex"}):
            with self.assertRaises(VERIFY.GateProofError):
                VERIFY.load_public_key()

    def test_wrong_length_key_fails_closed(self) -> None:
        with mock.patch.dict(os.environ, {SEAL.PUBLIC_KEY_ENV_VAR: "aa"}):
            with self.assertRaises(VERIFY.GateProofError):
                VERIFY.load_public_key()

    def test_valid_key_loads_from_env_var(self) -> None:
        with mock.patch.dict(os.environ, {SEAL.PUBLIC_KEY_ENV_VAR: _public_key_hex(REAL_PUBLIC_KEY)}):
            loaded = VERIFY.load_public_key()
        self.assertEqual(_public_key_hex(loaded), _public_key_hex(REAL_PUBLIC_KEY))


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

    def run_main(self, proof_path: str, public_key_hex: str | None) -> int:
        env = {} if public_key_hex is None else {SEAL.PUBLIC_KEY_ENV_VAR: public_key_hex}
        with mock.patch.dict(os.environ, env, clear=public_key_hex is None):
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
        rc = self.run_main(forged_path, _public_key_hex(REAL_PUBLIC_KEY))
        self.assertNotEqual(rc, 0)

    def test_legitimate_proof_accepted_via_cli(self) -> None:
        good_path = self.write(good_proof())
        rc = self.run_main(good_path, _public_key_hex(REAL_PUBLIC_KEY))
        self.assertEqual(rc, 0)

    def test_no_key_configured_on_the_deploy_host_fails_closed(self) -> None:
        good_path = self.write(good_proof())
        with mock.patch.object(SEAL, "PUBLIC_KEY_FILE", Path("/nonexistent/gate_proof_public_key.hex")):
            rc = self.run_main(good_path, None)
        self.assertNotEqual(rc, 0)


if __name__ == "__main__":
    unittest.main()
