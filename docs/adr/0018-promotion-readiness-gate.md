---
title: "ADR 0018: Local promotion readiness gate"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [8, 14]
---

# ADR 0018: Local promotion readiness gate

## Context

GOU-94 requires a local, read-only go/no-go before a pinned DEVELOPMENT
candidate reaches the separately reviewed beta/production promotion path.
Readiness alone never authorizes promotion (decision 14); this gate only
decides go/no-go and performs no Paperclip, Git or deployment write. It is
distinct from `scripts/deploy/build-release-bundle.sh` (manifest assembly
only) and from the bundling fix in PR #3351.

## Decision

`scripts/verify/check_promotion_readiness.py` fails closed when any of the
following holds:

- a project issue carries zero or several `phase:` labels (`development`,
  `beta_validation`, `production`, `post_production`);
- a DEVELOPMENT-phase issue's status is anything other than `done` or
  `cancelled` - Paperclip's real vocabulary is `backlog`, `todo`,
  `in_progress`, `in_review`, `blocked`, `done`, `cancelled`, and every
  non-terminal status, including one this gate has never seen, blocks - or
  the issue was created after the candidate's pin time;
- a release-manifest digest or `release=` SHA drifted from what the owner
  decision names;
- an owner decision is absent, resolved by a non-human actor, or silent on
  the exact candidate SHA, dataset, promotion target and phase (a
  `workflow_dispatch` run or a green CI check does not count, per GOU-94);
- the Paperclip fetch errors, returns an unvalidatable shape, or comes back
  shorter than `--cache-file`'s last known-good count (a possibly truncated
  page);
- for `--promotion-target beta`, the live GOU-151 beta mandate
  (`scripts/verify/beta_mandate.py`) does not independently confirm the
  owner-authored, undeleted board comment, milestone, goal and
  qualification records over the API.

Exit codes: `2` when the run itself could not be validated (API failure,
missing decision source); `1` when it validated cleanly but found blockers;
`0` on a clean pass.

### Owner decision record

`--decision PATH` (local JSON) or `--decision-issue-id` +
`--decision-interaction-id` (read-only live fetch of a resolved Paperclip
interaction) resolve to the same shape. `--decision` is refused for
`--promotion-target beta`: a locally supplied JSON file, even one declaring
`resolvedBy.type=human`, cannot be authenticated as a live Paperclip actor
(decision 14's delivery-mandate delegation is per ADR-0015, not a local
fixture). Beta only accepts the live-fetched decision together with the
mandate check below. `kind` must be `request_confirmation`,
`ask_user_questions` or `signed_decision_record`; a bare `workflow_dispatch`
payload or a non-human `resolvedBy` is no decision at all.

### Beta mandate composition (GOU-150/GOU-151)

For `--promotion-target beta`, this gate also calls
`scripts/verify/beta_mandate.py`'s live check. That check reads the
owner-authored board comment, milestone, goal and qualification issue twice
over the authenticated Paperclip API and refuses on any mismatch, deletion,
non-human authorship, or change between the two reads. It never returns
`authorized=true` by itself: it is one input among the manifest, phase and
owner-decision checks above, not a replacement for any of them, and
production keeps its own dedicated decision (decision 14).

Not wired into `.github/workflows/testAndPublishBeta.yml` or any live
deploy workflow - that stays frozen by GOU-91, as a separate follow-up.

## Gate proof seal (GOU-174, GOU-177)

The clean-pass report is sealed so `scripts/deploy/verify_gate_proof.py`,
run later on the deploy host with no Paperclip access, can authenticate it
instead of trusting a hand-written file (GOU-174). GOU-174's HMAC made the
verifier a second holder of the signing secret: it ran inside the publish
script's own process, so whoever could read that process's environment
could forge a new proof, not just check one.

GOU-177 replaces the HMAC with Ed25519 (`scripts/deploy/gate_proof_seal.py`).
The producer (`check_promotion_readiness.py`) holds the private signing key
(`O4G_GATE_PROOF_SIGNING_KEY`, a secret, provisioned only where it runs with
live Paperclip access). The verifier holds only the matching public key
(`scripts/deploy/gate_proof_public_key.hex`, not a secret, or
`O4G_GATE_PROOF_PUBLIC_KEY` for tests) - it authenticates a seal but cannot
produce one. The publish-script operator's account therefore never holds
anything that mints a valid seal, however fully its environment is read.

Rejected alternative: symmetric HMAC isolated by host file permissions
(`install-systemd-runtime.sh`'s 0600 `exposed-docs.env`/`geocode.env`
pattern). That isolates a human operator from a runtime secret a started
process already holds - a different boundary than isolating the verifying
process itself from a key it must read synchronously, inline, to do its
job; doing that would need a privilege-separation boundary (a second
account, reachable only through a narrow audited `sudo` rule) this repo
has no tooling for. Ed25519 closes the hole structurally instead: there is
no secret on the verifying side to isolate.

Dependency cost: `cryptography` (`...asymmetric.ed25519`) is the only
third-party Python import under `scripts/deploy/` or `scripts/verify/`;
elsewhere there both are stdlib-only. It is already present in this
project's reference build host's system Python, as an existing
OS/toolchain dependency, so the marginal cost is confirming its presence
per host, not adding and maintaining a new package.

No keypair is provisioned yet, by design: GOU-94's beta freeze means no
promotion should run. The private key is a new secret, not derived from
one already granted. Generating a keypair is within any agent's rights;
deciding this is the moment to mint the one real key, naming its
custodian, and provisioning it to wherever `check_promotion_readiness.py`
runs outside this repository are not - they are infrastructure and
secret-custody decisions reserved to Goulven. Until
`scripts/deploy/gate_proof_public_key.hex` is committed, `verify_gate_proof.py`
fails closed with "no gate proof public key is provisioned", which is the
correct state during the freeze.
