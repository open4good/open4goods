---
title: "Local promotion readiness gate"
normative: false
audience: PROJECT_SCOPED
---

# Local promotion readiness gate

Rules enforced by `scripts/verify/check_promotion_readiness.py` are in
[ADR-0018](../adr/0018-promotion-readiness-gate.md). This note is only how
to invoke it.

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

Exit codes: `2` run could not be validated (API failure, missing decision
source); `1` validated cleanly but found blockers; `0` clean pass, with a
JSON report on stdout.

Not wired into `.github/workflows/testAndPublishBeta.yml` or any live
deploy workflow — see [ADR-0018](../adr/0018-promotion-readiness-gate.md).
