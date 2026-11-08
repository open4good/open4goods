---
title: "ADR 0016: Secret-scan baseline as a reviewed exception ledger"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [1]
---

# ADR 0016: Secret-scan baseline as a reviewed exception ledger

## Context

Canonical decision 1 forbids committing a secret, but the history this public
repository inherited already contains credential-shaped content, and that history
is not being rewritten. So the guardrail has to distinguish a finding that is new
from one already known, without becoming an opaque hash list: from a bare list of
digests a reviewer cannot tell an accepted false positive from an accepted real
credential, and cannot tell whether a stale entry still means anything.

## Decision

`scripts/verify/secret_scan.py` gates in two modes, `git` over history and `dir`
over the working tree. Both fail on a finding outside the accepted set and on a
scanner failure -- an unreadable report applies no exception. CI publishes counts
only; unredacted evidence stays in a private temporary directory and is deleted.
A red scan prints just the newly found identities (`RuleID`, `File`, `StartLine`),
leaving the accepted set and every value out.

`.gitleaks-baseline.json` is the exception ledger and the authority on what was
accepted, by whom and when: its `acceptance` and `sourceCommit` metadata carry
that record, so no prose copy of it is kept elsewhere. An identity is a digest
over rule, file, secret and match, plus the original commit in `git` mode, so an
exception binds where it was found and does not silently cover a reintroduction
somewhere else.

Each accepted identity is an object, never a bare digest: `id`, `rule`, `file`,
`classification`, `note`, and `commit` in `git` mode. Every field is value-free,
so the baseline is auditable on its own. A `classification` is one of
`false-positive-doc`, `false-positive-placeholder`, `false-positive-test`,
`false-positive-identifier`, or `credential-shaped` -- a real positive the owner
accepted without asserting revocation. `load_baseline()` validates the shape and
exits 2 on anything else, including an unexpected key.

A `credential-shaped` entry carries one of two fixed `note` texts, and
`scripts/tests/test_gitleaks_config.py` rejects any other, so a note cannot drift
away from what it asserts: either an owner attestation that the credential is
already revoked, or acceptance without a revocation assertion, with rotation
tracked on a named issue that blocks promotion until it closes. An attestation is
the owner's word, not cryptographic proof, and the value stays in history either
way -- which is why an attested entry still stays in the ledger.

Adding an exception: confirm by hand that the finding is not a live credential,
keeping its value out of commits and comments; run
`secret_scan.py <mode> --emit-identities` to read its value-free `id`, `RuleID`,
`File`, `StartLine` and `Commit`; append the object to `accepted.<mode>`; re-run
until the scan reports `new: 0`. `CODEOWNERS` covers the baseline, `.gitleaks.toml`,
the scanner and its workflow, so every such change is reviewed. A non-secret shape
that recurs across many files is allowlisted narrowly in `.gitleaks.toml` rather
than accumulating one baseline entry per occurrence.

## Consequences

The ledger is reviewable by reading it, and reviewing it is the same act as
reviewing code, because `CODEOWNERS` routes it. Accepting an identity is cheap,
which is the point: the alternative is a permanently red scan that everyone learns
to ignore. Residual rotation is tracked as an issue with a promotion blocker rather
than as a scanner exemption, so the ledger cannot become the place a rotation
decision goes to die.
