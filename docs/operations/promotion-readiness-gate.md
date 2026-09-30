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
DEVELOPMENT-phase issue whose status is anything other than `done` or
`cancelled` (Paperclip's real vocabulary is `backlog`, `todo`, `in_progress`,
`in_review`, `blocked`, `done`, `cancelled` — every non-terminal status,
including one this script has never seen, blocks) or that was created after
the candidate's pin time; a release-manifest digest or `release=` SHA drifted
from what the decision names; an owner decision that is absent, resolved by a
non-human actor, or silent on the exact candidate SHA, dataset, promotion
target and phase (a `workflow_dispatch` run or a green CI check does not
count, per GOU-94); a Paperclip fetch that errors, returns an unvalidatable
shape, or comes back shorter than `--cache-file`'s last known-good count (a
possibly truncated page); and, for `--promotion-target beta`, a live
GOU-151 beta mandate (`scripts/verify/beta_mandate.py`) that does not
independently confirm the owner-authored, undeleted board comment, milestone,
goal and qualification records over the API.

Exit codes: `2` when the run itself could not be validated (API failure,
missing decision source); `1` when it validated cleanly but found blockers;
`0` on a clean pass, with a JSON report on stdout.

## Owner decision record

`--decision PATH` (local JSON) or `--decision-issue-id` +
`--decision-interaction-id` (read-only live fetch of a resolved Paperclip
interaction) both resolve to the same shape. `--decision` is refused for
`--promotion-target beta`: a locally supplied JSON file, even one declaring
`resolvedBy.type=human`, cannot be authenticated as a live Paperclip actor, so
beta only accepts the live-fetched decision together with the mandate check
below.

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

## Beta mandate composition (GOU-150/GOU-151)

For `--promotion-target beta`, this gate also calls
`scripts/verify/beta_mandate.py`'s live check, given
`--mandate-milestone-id`, `--mandate-goal-id`, `--mandate-comment-id`,
`--mandate-owner-id` and `--mandate-qualification-id`. That check reads the
owner-authored board comment, milestone, goal and qualification issue twice
over the authenticated Paperclip API and refuses on any mismatch, deletion,
non-human authorship, or change between the two reads. It never returns
`authorized=true` by itself (see its own docstring): it is one input among
the manifest, phase and owner-decision checks above, not a replacement for
any of them, and production keeps its own dedicated decision.

## Invocation

```sh
python3 scripts/verify/check_promotion_readiness.py \
  --project-id "$PAPERCLIP_PROJECT_ID" \
  --cache-file .o4g/promotion-readiness/nudger.json \
  --manifest release/release-manifest \
  --decision-issue-id GOU-94 --decision-interaction-id "$DECISION_INTERACTION_ID" \
  --mandate-milestone-id GOU-150 --mandate-goal-id "$GOAL_ID" \
  --mandate-comment-id "$MANDATE_COMMENT_ID" --mandate-owner-id "$OWNER_USER_ID" \
  --mandate-qualification-id "$QUALIFICATION_ISSUE_ID" \
  --candidate-sha "$CANDIDATE_SHA" --dataset "$DATASET_DIGEST" \
  --promotion-target beta --phase beta_validation
```

`PAPERCLIP_API_URL`, `PAPERCLIP_API_KEY` and `PAPERCLIP_COMPANY_ID` are read
from the environment. `--issues` swaps in an offline JSON fixture for a dry
run or a test, without touching the live API or authorizing a promotion.
`--cache-file` is local, unshared state.

Not wired into `.github/workflows/testAndPublishBeta.yml` or any live deploy
workflow -- that stays frozen by GOU-91, as a separate follow-up.
