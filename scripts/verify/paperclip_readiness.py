#!/usr/bin/env python3
"""Read-only phase readiness assessment; never a promotion authorization.

Fetch the whole project on each invocation so newly added or reopened work cannot
be hidden by the imported dependency list. Credentials come only from the
environment. Output contains identifiers and status, never descriptions or secrets.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

PHASES = ("development", "beta validation", "production", "post production")
REQUIRED = {"beta": ("development",), "production": ("development", "beta validation")}
GATES = {"development": "GOU-45", "beta validation": "GOU-31"}


def assess(issues: list[dict], project_id: str, target: str) -> dict:
    """Reject incomplete, unclassified, cancelled or inconsistent project work."""
    if not isinstance(issues, list) or not issues:
        raise ValueError("Empty or malformed issue inventory")
    blockers: list[dict] = []
    seen: set[str] = set()
    found_gates: set[str] = set()
    for issue in issues:
        if not isinstance(issue, dict) or issue.get("projectId") != project_id:
            raise ValueError("Issue inventory contains a foreign or malformed issue")
        identifier = issue.get("identifier")
        if not isinstance(identifier, str) or identifier in seen:
            raise ValueError("Missing or duplicate issue identifier")
        seen.add(identifier)
        labels = [label.get("name", "") for label in issue.get("labels", [])]
        phases = [name.removeprefix("phase: ") for name in labels if name.startswith("phase:")]
        # GOU-63 coordinates the campaign; it is not an executable phase task.
        if identifier == "GOU-63" and issue.get("workMode") == "planning" and not phases:
            continue
        if len(phases) != 1 or phases[0] not in PHASES:
            blockers.append({"issue": identifier, "reason": "exactly one valid phase label required"})
            continue
        phase = phases[0]
        if identifier in GATES.values() and identifier != GATES.get(phase):
            blockers.append({"issue": identifier, "reason": "readiness gate has wrong phase"})
        if phase not in REQUIRED[target]:
            continue
        if identifier == GATES.get(phase):
            found_gates.add(phase)
        if issue.get("status") != "done":
            blockers.append({"issue": identifier, "reason": "not done", "status": issue.get("status")})
    for phase in REQUIRED[target]:
        if phase not in found_gates:
            blockers.append({"issue": GATES[phase], "reason": "readiness gate missing from inventory"})
    return {"target": target, "ready": not blockers, "authorized": False,
            "issueCount": len(issues), "blockers": blockers}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    """Do not forward the bearer credential to a redirect target."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def fetch_issues(base: str, token: str, company: str, project: str) -> list[dict]:
    """Page deterministically through the installed Paperclip issue-list API."""
    parsed = urllib.parse.urlsplit(base)
    if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.query or parsed.fragment:
        raise ValueError("Paperclip API base must be HTTPS without userinfo, query or fragment")
    if not token or not company or not project:
        raise ValueError("Paperclip credentials, company and project are required")
    opener = urllib.request.build_opener(NoRedirect())
    issues: list[dict] = []
    seen: set[str] = set()
    # Stop on an empty page, not a short page: the server can clamp the page size.
    for _ in range(1000):
        query = urllib.parse.urlencode({"projectId": project, "limit": 100,
                                       "offset": len(issues), "sortField": "id", "sortDir": "asc",
                                       "includeRoutineExecutions": "true"})
        url = f"{base.rstrip('/')}/api/companies/{urllib.parse.quote(company, safe='')}/issues?{query}"
        request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
        with opener.open(request, timeout=30) as response:
            page = json.load(response)
        if not isinstance(page, list):
            raise ValueError("Malformed Paperclip issue page")
        if not page:
            return issues
        for issue in page:
            if not isinstance(issue, dict) or not issue.get("id") or issue["id"] in seen:
                raise ValueError("Repeated or malformed issue page; retry against a stable inventory")
            seen.add(issue["id"])
        issues.extend(page)
    raise ValueError("Paperclip pagination limit exceeded")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=REQUIRED, required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--issues", type=Path, help="Offline JSON inventory; never authorizes promotion")
    args = parser.parse_args()
    try:
        if args.issues:
            issues = json.loads(args.issues.read_text(encoding="utf-8"))
        else:
            issues = fetch_issues(os.environ.get("PAPERCLIP_API_URL", ""),
                                  os.environ.get("PAPERCLIP_API_KEY", ""),
                                  os.environ.get("PAPERCLIP_COMPANY_ID", ""), args.project)
        report = assess(issues, args.project, args.target)
        report["source"] = "offline" if args.issues else "live"
        print(json.dumps(report, indent=2))
        return 0 if report["ready"] else 1
    except (ValueError, KeyError, TypeError, AttributeError, OSError, urllib.error.URLError):
        # Network errors may include the URL; never echo responses or credentials.
        print("Readiness unavailable: incomplete inventory, invalid configuration or API failure.", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
