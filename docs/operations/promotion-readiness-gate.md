---
title: "Local promotion readiness gate"
normative: false
audience: PROJECT_SCOPED
---

# Local promotion readiness gate

`scripts/verify/check_promotion_readiness.py` gives a local, read-only
go/no-go on handing a pinned DEVELOPMENT candidate to the separately reviewed
beta/production promotion path. It performs no Paperclip, Git or deployment
write. It is distinct from `scripts/deploy/build-release-bundle.sh` (manifest
assembly only) and from the bundling fix in PR #3351.

It fails closed on: a project issue with zero or several `phase:` labels
(`development`, `beta_validation`, `production`, `post_production`); a
DEVELOPMENT-phase issue that is `open`, `reopened`, or created after the
candidate's pin time; a release-manifest digest or `release=` SHA drifted
from what the decision names; an owner decision that is absent, resolved by a
non-human actor, or silent on the exact candidate SHA, dataset, promotion
target and phase (a `workflow_dispatch` run or a green CI check does not
count, per GOU-94); and a Paperclip fetch that errors, returns an
unvalidatable shape, or comes back shorter than `--cache-file`'s last
known-good count (a possibly truncated page).

Exit codes: `2` when the run itself could not be validated (API failure,
missing decision source); `1` when it validated cleanly but found blockers;
`0` on a clean pass, with a JSON report on stdout.

## Owner decision record

`--decision PATH` (local JSON) or `--decision-issue-id` +
`--decision-interaction-id` (read-only live fetch of a resolved Paperclip
interaction) both resolve to the same shape:

```json
{
  "kind": "request_confirmation",
  "status": "resolved",
  "resolution": "accepted",
  "resolvedBy": {"type": "human", "email": "owner@example.com"},
  "candidateSha": "<git sha>",
  "dataset": "<dataset identifier or digest>",
  "promotionTarget": "beta",
  "authorizedPhase": "beta_validation",
  "manifestDigest": "<sha256 of the release-manifest>",
  "pinnedAt": "<ISO 8601 timestamp>"
}
```

`kind` is `request_confirmation`, `ask_user_questions` or
`signed_decision_record`; anything else, including a bare `workflow_dispatch`
payload or a non-human `resolvedBy`, is treated as no decision at all.

## Invocation

```sh
python3 scripts/verify/check_promotion_readiness.py \
  --project-id "$PAPERCLIP_PROJECT_ID" \
  --cache-file .o4g/promotion-readiness/nudger.json \
  --manifest release/release-manifest \
  --decision-issue-id GOU-94 --decision-interaction-id "$DECISION_INTERACTION_ID" \
  --candidate-sha "$CANDIDATE_SHA" --dataset "$DATASET_DIGEST" \
  --promotion-target beta --phase beta_validation
```

`PAPERCLIP_API_URL`, `PAPERCLIP_API_KEY` and `PAPERCLIP_COMPANY_ID` are read
from the environment. `--issues` swaps in an offline JSON fixture for a dry
run or a test, without touching the live API or authorizing a promotion.
`--cache-file` is local, unshared state.

Not wired into `.github/workflows/testAndPublishBeta.yml` or any live deploy
workflow -- that stays frozen by GOU-91, as a separate follow-up.
