#!/usr/bin/env python3
"""Verify a promotion-readiness gate proof immediately before a publish script mutates state.

Called by publish-java-release.sh / publish-nuxt-release.sh under the same deploy.lock as the
artifact-digest re-verification, right before the `current` symlink is swapped -- never as a
throwaway pre-check that a racing or delayed invocation could outlive. The proof is the JSON
report `scripts/verify/check_promotion_readiness.py` prints on a clean pass (redirected by the
operator to a file); it is rejected unless it is all of:

  * `ready: true`;
  * for this exact candidate SHA, promotion target and release-manifest digest (recomputed here
    from the manifest actually about to be published, never trusted from the proof itself) --
    a proof for a different candidate or target, or a stale manifest digest, is refused, so a
    proof cannot be replayed across SHAs, targets or an edited-then-rebuilt manifest;
  * generated within a bounded freshness window of "now", so a once-valid proof cannot authorize
    an unbounded number of later, unrelated publishes;
  * sealed with an Ed25519 signature (see gate_proof_seal.py, GOU-174/GOU-177) over exactly those
    fields. This process verifies the signature against the pipeline's public key only -- it never
    reads, derives or holds the private signing key, so nothing the publish-script operator's
    environment exposes lets them mint a new valid seal. Without this, the proof is just five
    lines of JSON anyone who can run this script could hand-write; the seal is what makes it
    authenticate instead of merely describe.

Fail-closed: any missing field, mismatch, malformed timestamp, stale proof, missing/invalid seal,
or unprovisioned public key exits non-zero.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import gate_proof_seal
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey


class GateProofError(ValueError):
    """A condition the gate-proof check must fail closed on."""


def parse_generated_at(value: object) -> float:
    if not isinstance(value, str) or not value.strip():
        raise GateProofError(f"gate proof generatedAt is missing or not a string: {value!r}")
    text = value.strip()
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    try:
        parsed = datetime.fromisoformat(text)
    except ValueError as exc:
        raise GateProofError(f"gate proof generatedAt is not a valid ISO 8601 timestamp: {value!r}") from exc
    if parsed.tzinfo is None:
        raise GateProofError(f"gate proof generatedAt must be timezone-aware: {value!r}")
    return parsed.astimezone(timezone.utc).timestamp()


def verify(proof: object, *, candidate_sha: str, promotion_target: str, manifest_digest: str,
           max_age_seconds: int, now: float, public_key: Ed25519PublicKey) -> None:
    if not isinstance(proof, dict):
        raise GateProofError("gate proof must be a JSON object")
    # Checked before any field is trusted: a mismatched field on a forged proof must fail on the
    # seal, not on the field check, so an attacker never learns which hand-written field was wrong.
    if not gate_proof_seal.verify_seal(public_key, proof, proof.get("seal")):
        raise GateProofError("gate proof seal is missing or invalid")
    if proof.get("ready") is not True:
        raise GateProofError(f"gate proof is not ready: ready={proof.get('ready')!r}")
    checks = {
        "candidateSha": candidate_sha,
        "promotionTarget": promotion_target,
        "manifestDigest": manifest_digest,
    }
    mismatches = {key: (proof.get(key), expected) for key, expected in checks.items()
                  if proof.get(key) != expected}
    if mismatches:
        raise GateProofError(f"gate proof does not name this exact candidate: {mismatches}")
    generated_at = parse_generated_at(proof.get("generatedAt"))
    age = now - generated_at
    if age < 0:
        raise GateProofError(f"gate proof generatedAt is in the future (clock skew?): age={age:.0f}s")
    if age > max_age_seconds:
        raise GateProofError(f"gate proof is stale: age={age:.0f}s exceeds max {max_age_seconds}s")


def load_public_key() -> Ed25519PublicKey:
    """Load the pipeline's Ed25519 public key: from PUBLIC_KEY_ENV_VAR for tests and local
    fixtures, otherwise from the repo-committed PUBLIC_KEY_FILE. Never reads, derives or needs the
    private signing key -- this process cannot mint a seal, only check one."""
    hex_key = os.environ.get(gate_proof_seal.PUBLIC_KEY_ENV_VAR, "")
    if not hex_key:
        if not gate_proof_seal.PUBLIC_KEY_FILE.is_file():
            raise GateProofError(
                "no gate proof public key is provisioned: "
                f"{gate_proof_seal.PUBLIC_KEY_FILE} is missing and "
                f"{gate_proof_seal.PUBLIC_KEY_ENV_VAR} is not set (GOU-177); "
                "promotion is inoperative until the pipeline's Ed25519 keypair is provisioned")
        hex_key = gate_proof_seal.PUBLIC_KEY_FILE.read_text(encoding="utf-8").strip()
    try:
        return gate_proof_seal.load_public_key_from_hex(hex_key)
    except ValueError as exc:
        raise GateProofError(str(exc)) from exc


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--proof", type=Path, required=True,
                         help="Path to the JSON report written by check_promotion_readiness.py")
    parser.add_argument("--candidate-sha", required=True)
    parser.add_argument("--promotion-target", required=True, choices=("beta", "production"))
    parser.add_argument("--manifest-digest", required=True,
                         help="sha256 of the release-manifest actually about to be published")
    parser.add_argument("--max-age-seconds", type=int, default=900,
                         help="Bounded freshness window for generatedAt (default: 900)")
    parser.add_argument("--now", type=float,
                         help="Override 'now' as a Unix timestamp; for tests only")
    args = parser.parse_args(argv)

    try:
        if args.max_age_seconds <= 0:
            raise GateProofError(f"--max-age-seconds must be positive: {args.max_age_seconds}")
        public_key = load_public_key()
        if not args.proof.is_file():
            raise GateProofError(f"gate proof not found: {args.proof}")
        try:
            proof = json.loads(args.proof.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise GateProofError(f"gate proof is unreadable or malformed: {exc}") from exc

        now = args.now if args.now is not None else time.time()
        verify(proof, candidate_sha=args.candidate_sha, promotion_target=args.promotion_target,
               manifest_digest=args.manifest_digest, max_age_seconds=args.max_age_seconds, now=now,
               public_key=public_key)
        return 0
    except GateProofError as exc:
        print(f"Promotion gate proof rejected: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
