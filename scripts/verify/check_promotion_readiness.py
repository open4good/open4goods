#!/usr/bin/env python3
"""Local promotion readiness gate: Paperclip phase reconciliation and owner-decision verification.

Fail-closed and read-only. Decides go/no-go for promoting a pinned DEVELOPMENT
candidate; it never writes to Paperclip, Git or a deployment target (see
docs/operations/promotion-readiness-gate.md). It refuses to pass when:

  * an issue in the Paperclip project has zero or more than one `phase:` label;
  * a DEVELOPMENT-phase issue is not `done` or `cancelled` (fail-closed on the
    real Paperclip vocabulary: `backlog`, `todo`, `in_progress`, `in_review`,
    `blocked`, and any unrecognized status all block), or was created after
    the candidate was pinned;
  * the release manifest's digest no longer matches the one the owner decision
    names (rebuilt, edited or otherwise no longer what was reviewed);
  * the owner decision is missing, was not resolved by a human, or does not name
    the exact candidate SHA, dataset, promotion target and phase under review.
    A `workflow_dispatch` run or green CI is never treated as that decision.
  * the Paperclip issue fetch fails, is short relative to the last known-good
    count (a possible hidden page), or returns a shape this script cannot
    fully validate.
  * for a beta promotion target, the live GOU-151 beta mandate
    (`scripts/verify/beta_mandate.py`) does not independently confirm the
    owner-authored, undeleted Paperclip comment, milestone, goal and
    qualification records. A locally supplied `--decision` JSON file (even one
    declaring `resolvedBy.type=human`) is refused for beta: it cannot be
    authenticated as a live Paperclip actor, so it is never accepted as the
    owner decision for that target. The mandate check itself never returns
    `authorized=true`; it is combined here with, not a substitute for, the
    manifest/phase/decision checks above.
  * the `O4G_GATE_PROOF_SIGNING_KEY` environment variable is missing or
    unusable. A clean report is signed with Ed25519 (see
    scripts/deploy/gate_proof_seal.py, GOU-174/GOU-177) before being printed,
    so that `scripts/deploy/verify_gate_proof.py` -- run later, on a deploy
    host with no Paperclip API access and only the matching public key --
    can authenticate it instead of trusting a hand-written file with the
    right field names.

Any of the above exits non-zero. A clean pass exits 0. This script performs no
Paperclip, Git or deployment write of any kind.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import beta_mandate

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "deploy"))
import gate_proof_seal  # noqa: E402  (path must be extended first)

CANONICAL_PHASES = ("development", "beta_validation", "production", "post_production")
# Fail-closed: only these two Paperclip statuses count as finished. Every other
# status (backlog, todo, in_progress, in_review, blocked, or anything unknown)
# blocks a DEVELOPMENT-phase task, per the Lead Tech review on PR #3353 —
# `open`/`reopened` are not part of Paperclip's status vocabulary and were
# silently treating every real status as "done".
TERMINAL_STATUSES = {"done", "cancelled"}
ALLOWED_DECISION_KINDS = {"request_confirmation", "ask_user_questions", "signed_decision_record"}
REQUIRED_DECISION_FIELDS = (
    "kind", "status", "resolution", "resolvedBy",
    "candidateSha", "dataset", "promotionTarget", "authorizedPhase", "manifestDigest", "pinnedAt",
)


class ReadinessError(ValueError):
    """A condition the gate must fail closed on; never treated as 'no blockers found'."""


def normalize_phase(raw: str) -> str:
    return raw.strip().lower().replace(" ", "_").replace("-", "_")


def evaluate_issues(issues: list[dict], pinned_at: str | None) -> list[dict]:
    """Fail closed on phase-labeling defects and unsafe DEVELOPMENT-phase state.

    `pinned_at` may be falsy (an already-invalid decision); the "created after
    pin" comparison is then skipped rather than flagging every issue.
    """
    blockers: list[dict] = []
    for issue in issues:
        if not isinstance(issue, dict):
            blockers.append({"reason": "malformed_issue"})
            continue
        identifier = issue.get("identifier") or issue.get("id") or "<unknown>"
        phase_labels = [
            normalize_phase(label.get("name", "").split(":", 1)[1])
            for label in issue.get("labels") or []
            if isinstance(label, dict) and label.get("name", "").lower().startswith("phase:")
        ]
        valid_phase_labels = [phase for phase in phase_labels if phase in CANONICAL_PHASES]
        if len(phase_labels) != 1 or len(valid_phase_labels) != 1:
            blockers.append({
                "issue": identifier,
                "reason": "missing_or_multiple_phase",
                "detail": f"found {len(phase_labels)} phase label(s): {phase_labels}",
            })
            continue
        if valid_phase_labels[0] != "development":
            continue
        status = issue.get("status")
        if status not in TERMINAL_STATUSES:
            blockers.append({"issue": identifier, "reason": "development_task_open", "status": status})
        created_at = issue.get("createdAt")
        if pinned_at and isinstance(created_at, str) and created_at > pinned_at:
            blockers.append({
                "issue": identifier,
                "reason": "development_task_created_after_pin",
                "createdAt": created_at,
                "pinnedAt": pinned_at,
            })
    return blockers


def reconcile_issue_count(fetched_count: int, cache_path: Path) -> dict:
    """Refuse a fetch smaller than the last known-good count.

    A Paperclip project's issue count does not shrink on its own; a smaller
    count than previously observed is treated as a possibly-truncated page
    ("hidden tasks"), not as a clean, smaller project.
    """
    if fetched_count <= 0:
        raise ReadinessError("Fetched issue inventory is empty")
    previous = None
    if cache_path.is_file():
        try:
            cached = json.loads(cache_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ReadinessError(f"Cannot read issue-count cache {cache_path}: {exc}") from exc
        previous = cached.get("lastKnownIssueCount")
        if not isinstance(previous, int):
            raise ReadinessError(f"Issue-count cache {cache_path} is malformed")
    if previous is not None and fetched_count < previous:
        raise ReadinessError(
            f"Fetched {fetched_count} issues but {previous} were previously observed; "
            "refusing a possibly-truncated page"
        )
    new_count = max(fetched_count, previous or 0)
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(json.dumps({"lastKnownIssueCount": new_count}, indent=2) + "\n", encoding="utf-8")
    return {"previous": previous, "current": fetched_count}


def manifest_digest(manifest_path: Path) -> str:
    if not manifest_path.is_file():
        raise ReadinessError(f"Release manifest not found: {manifest_path}")
    return hashlib.sha256(manifest_path.read_bytes()).hexdigest()


def manifest_release_sha(manifest_path: Path) -> str:
    for line in manifest_path.read_text(encoding="utf-8").splitlines():
        if line.startswith("release="):
            return line.split("=", 1)[1].strip()
    raise ReadinessError(f"Release manifest {manifest_path} has no release= line")


def validate_decision(decision: Any, *, candidate_sha: str, dataset: str,
                       promotion_target: str, phase: str) -> None:
    """Reject anything but an explicit, human-resolved decision naming this exact candidate.

    Deliberately never looks at workflow_dispatch metadata or CI status: those
    are not an owner decision, per GOU-94's constraint.
    """
    if not isinstance(decision, dict):
        raise ReadinessError("Owner decision record is malformed")
    missing = [field for field in REQUIRED_DECISION_FIELDS if field not in decision]
    if missing:
        raise ReadinessError(f"Owner decision record is missing fields: {', '.join(missing)}")
    if decision["kind"] not in ALLOWED_DECISION_KINDS:
        raise ReadinessError(f"Owner decision has an unrecognized or unauthorized kind: {decision['kind']!r}")
    if decision.get("status") != "resolved":
        raise ReadinessError(f"Owner decision is not resolved: status={decision.get('status')!r}")
    if decision.get("resolution") not in ("accepted", "approved", "confirmed"):
        raise ReadinessError(f"Owner decision was not accepted: resolution={decision.get('resolution')!r}")
    resolved_by = decision.get("resolvedBy")
    if not isinstance(resolved_by, dict) or resolved_by.get("type") != "human":
        raise ReadinessError("Owner decision was not resolved by a human actor")
    checks = {
        "candidateSha": candidate_sha,
        "dataset": dataset,
        "promotionTarget": promotion_target,
        "authorizedPhase": phase,
    }
    mismatches = {key: (decision.get(key), expected) for key, expected in checks.items()
                  if decision.get(key) != expected}
    if mismatches:
        raise ReadinessError(f"Owner decision does not name this candidate: {mismatches}")


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    """Do not forward the bearer credential to a redirect target."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def _api_get(base: str, token: str, path: str) -> Any:
    parsed = urllib.parse.urlsplit(base)
    if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.query or parsed.fragment:
        raise ReadinessError("Paperclip API base must be HTTPS without userinfo, query or fragment")
    if not token:
        raise ReadinessError("Paperclip API token is required for a live check")
    opener = urllib.request.build_opener(_NoRedirect())
    request = urllib.request.Request(f"{base.rstrip('/')}{path}", headers={"Authorization": f"Bearer {token}"})
    try:
        with opener.open(request, timeout=30) as response:
            return json.load(response)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
        raise ReadinessError(f"Paperclip API call failed: {exc}") from exc


def fetch_issues(base: str, token: str, company: str, project: str) -> list[dict]:
    """Page deterministically through the whole project; stop on an empty page, not a short one."""
    if not company or not project:
        raise ReadinessError("Paperclip company and project are required")
    issues: list[dict] = []
    seen: set[str] = set()
    for _ in range(1000):
        query = urllib.parse.urlencode({
            "projectId": project, "limit": 100, "offset": len(issues),
            "sortField": "id", "sortDir": "asc",
        })
        page = _api_get(base, token, f"/api/companies/{urllib.parse.quote(company, safe='')}/issues?{query}")
        if not isinstance(page, list):
            raise ReadinessError("Malformed Paperclip issue page")
        if not page:
            return issues
        for issue in page:
            issue_id = issue.get("id") if isinstance(issue, dict) else None
            if not issue_id or issue_id in seen:
                raise ReadinessError("Repeated or malformed issue in page; refusing an unstable inventory")
            seen.add(issue_id)
        issues.extend(page)
    raise ReadinessError("Paperclip pagination limit exceeded")


def fetch_decision(base: str, token: str, issue_id: str, interaction_id: str) -> dict:
    """Read (never write) a resolved interaction that may hold the owner decision."""
    decision = _api_get(
        base, token,
        f"/api/issues/{urllib.parse.quote(issue_id, safe='')}"
        f"/interactions/{urllib.parse.quote(interaction_id, safe='')}",
    )
    if not isinstance(decision, dict):
        raise ReadinessError("Malformed Paperclip interaction response")
    return decision


def check_beta_mandate(*, api_base: str, api_token: str, company_id: str, project_id: str,
                        mandate_milestone_id: str, mandate_goal_id: str, mandate_comment_id: str,
                        mandate_owner_id: str, mandate_qualification_id: str) -> dict:
    """Independently confirm the live GOU-151 beta mandate; never itself a go/no-go.

    Composed alongside (not instead of) the manifest/phase/decision checks: a
    passing mandate only means the owner-authored comment, milestone, goal and
    qualification records were read live and match; it never sets
    ``authorized`` true and is not a substitute for the owner-decision record
    validated separately via ``--decision-issue-id``/``--decision-interaction-id``.
    """
    missing = [name for name, value in (
        ("mandate-milestone-id", mandate_milestone_id), ("mandate-goal-id", mandate_goal_id),
        ("mandate-comment-id", mandate_comment_id), ("mandate-owner-id", mandate_owner_id),
        ("mandate-qualification-id", mandate_qualification_id),
    ) if not value]
    if missing:
        raise ReadinessError(
            "Beta promotion requires live mandate identities: " + ", ".join(missing))
    try:
        return beta_mandate.check_live(
            api_base, api_token, company_id=company_id, project_id=project_id,
            milestone_id=mandate_milestone_id, goal_id=mandate_goal_id,
            comment_id=mandate_comment_id, owner_id=mandate_owner_id,
            qualification_id=mandate_qualification_id, target="beta")
    except beta_mandate.MandateError as exc:
        raise ReadinessError(f"Beta mandate refused: {exc}") from exc


def build_report(*, issues: list[dict], cache_path: Path, manifest_path: Path, decision: Any,
                  candidate_sha: str, dataset: str, promotion_target: str, phase: str,
                  mandate_report: dict | None = None) -> dict:
    blockers: list[dict] = []
    reconciliation = reconcile_issue_count(len(issues), cache_path)

    digest = manifest_digest(manifest_path)
    release_sha = manifest_release_sha(manifest_path)
    if release_sha != candidate_sha:
        blockers.append({
            "reason": "manifest_candidate_mismatch",
            "manifestRelease": release_sha,
            "candidateSha": candidate_sha,
        })

    pinned_digest = decision.get("manifestDigest") if isinstance(decision, dict) else None
    if pinned_digest != digest:
        blockers.append({"reason": "manifest_stale", "pinnedDigest": pinned_digest, "actualDigest": digest})

    try:
        validate_decision(decision, candidate_sha=candidate_sha, dataset=dataset,
                           promotion_target=promotion_target, phase=phase)
    except ReadinessError as exc:
        blockers.append({"reason": "owner_decision_invalid", "detail": str(exc)})

    pinned_at = decision.get("pinnedAt") if isinstance(decision, dict) else None
    blockers.extend(evaluate_issues(issues, pinned_at))

    return {
        "ready": not blockers,
        "issueCount": len(issues),
        "issueCountReconciliation": reconciliation,
        "manifestDigest": digest,
        "betaMandate": mandate_report,
        "blockers": blockers,
        # Echoed back (not just computed) so a publish script can verify this report names the
        # exact candidate/target it is about to mutate state for, without re-deriving them from a
        # trusted-by-assumption source; see scripts/deploy/verify_gate_proof.py (GOU-164).
        "candidateSha": candidate_sha,
        "promotionTarget": promotion_target,
        "dataset": dataset,
        "phase": phase,
        "generatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--project-id", required=True, help="Paperclip Nudger project id")
    parser.add_argument("--company-id", default=os.environ.get("PAPERCLIP_COMPANY_ID", ""))
    parser.add_argument("--api-base", default=os.environ.get("PAPERCLIP_API_URL", ""))
    parser.add_argument("--api-token", default=os.environ.get("PAPERCLIP_API_KEY", ""))
    parser.add_argument("--issues", type=Path,
                         help="Offline JSON array of issues; bypasses the live Paperclip API")
    parser.add_argument("--cache-file", type=Path, required=True,
                         help="Last known-good issue-count cache, used to catch a truncated page")
    parser.add_argument("--manifest", type=Path, required=True,
                         help="release-manifest produced by scripts/deploy/build-release-bundle.sh")
    parser.add_argument("--decision", type=Path,
                         help="Local owner-decision JSON record; refused for --promotion-target beta, "
                              "where only a live-fetched decision plus the beta mandate is accepted")
    parser.add_argument("--decision-issue-id", help="Paperclip issue holding the owner decision interaction")
    parser.add_argument("--decision-interaction-id", help="Resolved interaction id, fetched live (read-only)")
    parser.add_argument("--mandate-milestone-id",
                         help="Beta only: milestone issue id for the live GOU-151 mandate check")
    parser.add_argument("--mandate-goal-id", help="Beta only: goal id for the live mandate check")
    parser.add_argument("--mandate-comment-id",
                         help="Beta only: owner-authored board comment id for the live mandate check")
    parser.add_argument("--mandate-owner-id", help="Beta only: expected owner user id for the mandate check")
    parser.add_argument("--mandate-qualification-id",
                         help="Beta only: qualification issue id for the live mandate check")
    parser.add_argument("--candidate-sha", required=True, help="Git SHA under review for promotion")
    parser.add_argument("--dataset", required=True, help="Dataset identifier or digest under review")
    parser.add_argument("--promotion-target", required=True, choices=("beta", "production"))
    parser.add_argument("--phase", required=True, choices=CANONICAL_PHASES,
                         help="The phase the owner decision is expected to authorize")
    args = parser.parse_args(argv)

    try:
        if not re.fullmatch(r"[0-9a-f]{7,64}", args.candidate_sha):
            raise ReadinessError("candidate SHA must be a lowercase hex Git SHA")

        if args.issues is not None:
            issues = json.loads(args.issues.read_text(encoding="utf-8"))
            if not isinstance(issues, list):
                raise ReadinessError("Offline issue fixture must be a JSON array")
        else:
            issues = fetch_issues(args.api_base, args.api_token, args.company_id, args.project_id)

        if args.promotion_target == "beta" and args.decision is not None:
            raise ReadinessError(
                "A local --decision file cannot authorize a beta promotion; it is not an "
                "authenticated live Paperclip actor. Use --decision-issue-id/"
                "--decision-interaction-id together with the --mandate-* identities")

        if args.decision is not None:
            try:
                decision = json.loads(args.decision.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError) as exc:
                raise ReadinessError(f"Cannot read decision record {args.decision}: {exc}") from exc
        elif args.decision_issue_id and args.decision_interaction_id:
            decision = fetch_decision(args.api_base, args.api_token,
                                       args.decision_issue_id, args.decision_interaction_id)
        else:
            raise ReadinessError(
                "No owner decision source: pass --decision or "
                "--decision-issue-id/--decision-interaction-id")

        mandate_report = None
        if args.promotion_target == "beta":
            mandate_report = check_beta_mandate(
                api_base=args.api_base, api_token=args.api_token,
                company_id=args.company_id, project_id=args.project_id,
                mandate_milestone_id=args.mandate_milestone_id, mandate_goal_id=args.mandate_goal_id,
                mandate_comment_id=args.mandate_comment_id, mandate_owner_id=args.mandate_owner_id,
                mandate_qualification_id=args.mandate_qualification_id,
            )

        report = build_report(
            issues=issues, cache_path=args.cache_file, manifest_path=args.manifest, decision=decision,
            candidate_sha=args.candidate_sha, dataset=args.dataset,
            promotion_target=args.promotion_target, phase=args.phase, mandate_report=mandate_report,
        )
        hex_key = os.environ.get(gate_proof_seal.SIGNING_KEY_ENV_VAR, "")
        if not hex_key:
            raise ReadinessError(
                f"{gate_proof_seal.SIGNING_KEY_ENV_VAR} is not set; cannot sign the gate proof")
        try:
            signing_key = gate_proof_seal.load_signing_key_from_hex(hex_key)
        except ValueError as exc:
            raise ReadinessError(str(exc)) from exc
        report["seal"] = gate_proof_seal.compute_seal(signing_key, report)
        print(json.dumps(report, indent=2))
        return 0 if report["ready"] else 1
    except ReadinessError as exc:
        print(f"Promotion readiness gate failed closed: {exc}", file=sys.stderr)
        return 2
    except (OSError, json.JSONDecodeError, urllib.error.URLError) as exc:
        print(f"Promotion readiness gate failed closed (unexpected error): {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
