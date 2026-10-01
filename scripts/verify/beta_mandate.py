#!/usr/bin/env python3
"""Verify a live Paperclip beta mandate; never authorize a deployment by itself.

GOU-94 must combine this check with candidate, scope, CI, backup and rollback
qualification at the write boundary. No offline decision file is accepted.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

from paperclip_readiness import NoRedirect


class MandateError(ValueError):
    """The delegation is absent, stale, out of scope or not qualified."""


# Live records show a native board/API write carries sourceTrust=None and
# derivedAuthorSource=None; anything else means the comment was attributed to
# the owner through a bridged or derived channel and cannot be trusted as a
# directly authenticated owner decision.
ACCEPTED_COMMENT_SOURCE_TRUST = frozenset({None})

DELETION_MARKERS = ("deletedAt", "deletedByType", "deletedByUserId",
                    "deletedByAgentId", "deletedByRunId")


def validate_mandate(comment: dict, milestone: dict, goal: dict, qualification: dict,
                     *, company_id: str, project_id: str, milestone_id: str,
                     goal_id: str, comment_id: str, owner_id: str,
                     qualification_id: str, target: str) -> dict:
    """Check authenticated API records against independently configured identities."""
    if target != "beta":
        raise MandateError("A beta mandate cannot authorize production")
    if not all(isinstance(value, str) and value for value in
               (company_id, project_id, milestone_id, goal_id, comment_id, owner_id, qualification_id)):
        raise MandateError("Explicit mandate identities are required")
    for record in (comment, milestone, goal, qualification):
        if not isinstance(record, dict) or record.get("companyId") != company_id:
            raise MandateError("Malformed or foreign mandate record")
    if (comment.get("id") != comment_id or comment.get("issueId") != milestone_id
            or comment.get("authorUserId") != owner_id or comment.get("authorType") != "user"
            or comment.get("sourceTrust") not in ACCEPTED_COMMENT_SOURCE_TRUST
            or any(comment.get(key) is not None for key in
                   ("authorAgentId", "onBehalfOfUserId", "createdByRunId",
                    "derivedAuthorAgentId", "derivedCreatedByRunId", "derivedAuthorSource",
                    *DELETION_MARKERS))):
        raise MandateError("Mandate must be an undeleted, directly authenticated owner board comment")
    try:
        decision = json.loads(comment.get("body", ""))
    except (ValueError, TypeError) as exc:
        raise MandateError("Malformed mandate body") from exc
    expected = {
        "schema": "nudger-beta-mandate/v1", "milestoneIssueId": milestone_id,
        "projectId": project_id, "goalId": goal_id, "target": "beta",
        "scopePolicy": "evolving-until-review", "sensitiveChanges": "approved-design-only",
        "qualificationIssueId": qualification_id,
    }
    if (not isinstance(decision, dict)
            or any(decision.get(key) != value for key, value in expected.items())
            or decision.get("productionAuthorized") is not False):
        raise MandateError("Mandate does not match the configured delivery contract")
    if (milestone.get("id") != milestone_id or milestone.get("projectId") != project_id
            or milestone.get("goalId") != goal_id or milestone.get("hiddenAt") is not None
            or milestone.get("status") not in ("todo", "in_progress", "in_review")
            or milestone.get("reviewPolicy") != "human_only"):
        raise MandateError("Milestone is inactive, hidden or lacks human acceptance")
    if goal.get("id") != goal_id or goal.get("status") != "active":
        raise MandateError("Goal is no longer active")
    if (qualification.get("id") != qualification_id
            or qualification.get("projectId") != project_id
            or qualification.get("status") != "done"
            or qualification.get("hiddenAt") is not None):
        raise MandateError("Promotion tooling qualification is incomplete")
    return {"mandateValid": True, "authorized": False, "target": "beta",
            "milestoneIssueId": milestone_id, "goalId": goal_id,
            "scopeFrozen": milestone["status"] == "in_review",
            "decisionCommentId": comment_id}


def api_get(base: str, token: str, path: str) -> dict:
    """GET only, HTTPS only, no redirects and no response/credential error logging."""
    parsed = urllib.parse.urlsplit(base)
    if (parsed.scheme != "https" or not parsed.netloc or parsed.username is not None
            or parsed.password is not None or parsed.query or parsed.fragment or not token):
        raise MandateError("Authenticated HTTPS Paperclip configuration is required")
    request = urllib.request.Request(base.rstrip("/") + "/api" + path,
                                     headers={"Authorization": f"Bearer {token}"})
    with urllib.request.build_opener(NoRedirect()).open(request, timeout=30) as response:
        data = json.load(response)
    if not isinstance(data, dict):
        raise MandateError("Malformed Paperclip response")
    return data


def check_live(base: str, token: str, **identities) -> dict:
    """Read twice to detect changes during verification; recheck again at promotion."""
    quote = lambda value: urllib.parse.quote(value, safe="")
    paths = [
        f"/issues/{quote(identities['milestone_id'])}/comments/{quote(identities['comment_id'])}",
        f"/issues/{quote(identities['milestone_id'])}",
        f"/goals/{quote(identities['goal_id'])}",
        f"/issues/{quote(identities['qualification_id'])}",
    ]
    before = [api_get(base, token, path) for path in paths]
    report = validate_mandate(*before, **identities)
    after = [api_get(base, token, path) for path in paths]
    if before != after:
        raise MandateError("Mandate changed during verification; retry after reconciliation")
    validate_mandate(*after, **identities)
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("company-id", "project-id", "milestone-id", "goal-id", "comment-id",
                 "owner-id", "qualification-id"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--target", required=True, choices=("beta", "production"))
    args = vars(parser.parse_args(argv))
    try:
        report = check_live(os.environ.get("PAPERCLIP_API_URL", ""),
                            os.environ.get("PAPERCLIP_API_KEY", ""), **args)
        print(json.dumps(report, indent=2))
        return 0
    except MandateError as exc:
        print(f"Beta mandate refused: {exc}", file=sys.stderr)
        return 1
    except (OSError, ValueError, TypeError, urllib.error.URLError):
        print("Beta mandate unavailable: API or configuration failure.", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
