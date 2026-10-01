"""Canonical form and Ed25519 seal for a promotion-readiness gate proof (GOU-174, GOU-177).

Shared by the proof producer (scripts/verify/check_promotion_readiness.py, which has live
Paperclip API access) and the proof verifier (scripts/deploy/verify_gate_proof.py, which runs on
the deploy host under deploy.lock without that access). Without a seal, the file handoff between
those two trust boundaries only *describes* readiness -- anyone who can invoke the publish script
can hand-write the same five fields.

GOU-174 sealed the proof with a symmetric HMAC, which made the verifier a second holder of the
exact secret that forges a proof: whoever could read the publish script's environment to verify
could also sign. GOU-177 replaces it with Ed25519: the producer holds the private key
(``SIGNING_KEY_ENV_VAR``, provisioned only to whatever runs check_promotion_readiness.py with live
Paperclip access); the verifier needs only the matching public key, which is not a secret and can
be committed to the repo (``PUBLIC_KEY_FILE``) or, for tests, supplied via
``PUBLIC_KEY_ENV_VAR``. A deploy host configured this way holds nothing that lets it mint a new
valid seal, however completely its environment is read.
"""

from __future__ import annotations

import json
from pathlib import Path

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey

# The exact fields GOU-174 requires the seal to cover; order is irrelevant, canonical_form fixes it.
SEALED_FIELDS = ("ready", "candidateSha", "promotionTarget", "manifestDigest", "generatedAt")

# Producer-only secret: the Ed25519 private key seed, hex-encoded. Never read by
# scripts/deploy/verify_gate_proof.py or by anything running on the deploy host.
SIGNING_KEY_ENV_VAR = "O4G_GATE_PROOF_SIGNING_KEY"

# Not a secret: overrides the repo-committed public key for tests and local fixtures. The real
# publish scripts never set this; they rely on PUBLIC_KEY_FILE.
PUBLIC_KEY_ENV_VAR = "O4G_GATE_PROOF_PUBLIC_KEY"

# Committed to the repo once the pipeline's keypair is provisioned (see
# docs/operations/promotion-readiness-gate.md). Absent until then, by design: the gate fails
# closed rather than trust an unprovisioned mechanism.
PUBLIC_KEY_FILE = Path(__file__).with_name("gate_proof_public_key.hex")

KEY_BYTES = 32  # Ed25519 private seeds and public keys are both exactly 32 raw bytes.


def canonical_form(proof: dict) -> bytes:
    """Deterministic byte form of the sealed fields, independent of field order, whitespace or
    any extra (unsealed) field present in the proof -- so the seal cannot be defeated by adding
    or reordering fields the verifier does not read for this purpose."""
    payload = {field: proof.get(field) for field in SEALED_FIELDS}
    return json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")


def compute_seal(signing_key: Ed25519PrivateKey, proof: dict) -> str:
    return signing_key.sign(canonical_form(proof)).hex()


def verify_seal(public_key: Ed25519PublicKey, proof: dict, seal: object) -> bool:
    if not isinstance(seal, str) or not seal:
        return False
    try:
        signature = bytes.fromhex(seal)
    except ValueError:
        return False
    try:
        public_key.verify(signature, canonical_form(proof))
        return True
    except InvalidSignature:
        return False


def _load_raw_key(hex_key: str, *, what: str) -> bytes:
    try:
        raw = bytes.fromhex(hex_key)
    except ValueError as exc:
        raise ValueError(f"{what} is not valid hex: {exc}") from exc
    if len(raw) != KEY_BYTES:
        raise ValueError(f"{what} must be exactly {KEY_BYTES} bytes ({len(raw)} given)")
    return raw


def load_signing_key_from_hex(hex_key: str) -> Ed25519PrivateKey:
    """Parse and size-check the producer's private key; raises ValueError on anything unusable."""
    raw = _load_raw_key(hex_key, what=SIGNING_KEY_ENV_VAR)
    return Ed25519PrivateKey.from_private_bytes(raw)


def load_public_key_from_hex(hex_key: str) -> Ed25519PublicKey:
    """Parse and size-check the verifier's public key; raises ValueError on anything unusable."""
    raw = _load_raw_key(hex_key, what="gate proof public key")
    return Ed25519PublicKey.from_public_bytes(raw)
