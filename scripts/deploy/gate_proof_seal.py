"""Canonical form and HMAC seal for a promotion-readiness gate proof (GOU-174).

Shared by the proof producer (scripts/verify/check_promotion_readiness.py, which has live
Paperclip API access) and the proof verifier (scripts/deploy/verify_gate_proof.py, which runs on
the deploy host under deploy.lock without that access). Without a seal, the file handoff between
those two trust boundaries only *describes* readiness -- anyone who can invoke the publish script
can hand-write the same five fields. The seal is what the operator cannot forge: producing one
requires the HMAC key named by ``ENV_VAR``, which is provisioned to the pipeline, not to the
publish-script operator.
"""

from __future__ import annotations

import hashlib
import hmac
import json

# The exact fields GOU-174 requires the seal to cover; order is irrelevant, canonical_form fixes it.
SEALED_FIELDS = ("ready", "candidateSha", "promotionTarget", "manifestDigest", "generatedAt")
ENV_VAR = "O4G_GATE_PROOF_HMAC_KEY"
MIN_KEY_BYTES = 32


def canonical_form(proof: dict) -> bytes:
    """Deterministic byte form of the sealed fields, independent of field order, whitespace or
    any extra (unsealed) field present in the proof -- so the seal cannot be defeated by adding
    or reordering fields the verifier does not read for this purpose."""
    payload = {field: proof.get(field) for field in SEALED_FIELDS}
    return json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")


def compute_seal(key: bytes, proof: dict) -> str:
    return hmac.new(key, canonical_form(proof), hashlib.sha256).hexdigest()


def verify_seal(key: bytes, proof: dict, seal: object) -> bool:
    if not isinstance(seal, str) or not seal:
        return False
    return hmac.compare_digest(compute_seal(key, proof), seal)


def load_key_from_hex(hex_key: str) -> bytes:
    """Parse and size-check the shared key; raises ValueError on anything unusable."""
    try:
        key = bytes.fromhex(hex_key)
    except ValueError as exc:
        raise ValueError(f"{ENV_VAR} is not valid hex: {exc}") from exc
    if len(key) < MIN_KEY_BYTES:
        raise ValueError(f"{ENV_VAR} is too short to be a usable HMAC key "
                          f"({len(key)} bytes, need at least {MIN_KEY_BYTES})")
    return key
