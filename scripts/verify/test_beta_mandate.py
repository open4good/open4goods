"""Delegation is beta-only and cannot replace delivery qualification."""

import copy
import io
import json
import unittest
import urllib.error
from unittest.mock import patch

from beta_mandate import MandateError, api_get, check_live, main, validate_mandate

# Captured from live Paperclip comment/issue/goal records during GOU-155's
# review (an owner board comment, the GOU-150 milestone issue, and its goal).
# Pins the exact field contract beta_mandate.py may rely on: validate_mandate
# reading any field outside these sets means either a dependency on an
# unverified field or that the fixtures below are stale and need re-capturing
# from the live API before being trusted.
COMMENT_CONTRACT_FIELDS = frozenset({
    "id", "issueId", "companyId", "authorType", "authorUserId", "authorAgentId",
    "onBehalfOfUserId", "createdByRunId", "derivedAuthorAgentId",
    "derivedCreatedByRunId", "derivedAuthorSource", "sourceTrust",
    "deletedAt", "deletedByType", "deletedByUserId", "deletedByAgentId",
    "deletedByRunId", "body",
})
ISSUE_CONTRACT_FIELDS = frozenset({
    "id", "companyId", "projectId", "goalId", "hiddenAt", "reviewPolicy", "status",
})
GOAL_CONTRACT_FIELDS = frozenset({"id", "companyId", "status"})
QUALIFICATION_CONTRACT_FIELDS = frozenset({
    "id", "companyId", "projectId", "status", "hiddenAt",
})


class RealShapeDict(dict):
    """Raises if code reads a field outside the documented live contract."""

    def __init__(self, allowed, data):
        assert set(data) <= allowed, f"fixture has undocumented keys: {set(data) - allowed}"
        super().__init__(data)
        self._allowed = allowed

    def get(self, key, default=None):
        assert key in self._allowed, f"beta_mandate.py reads undocumented field {key!r}"
        return super().get(key, default)


class BetaMandateTest(unittest.TestCase):
    def setUp(self):
        self.ids = dict(company_id="company", project_id="project", milestone_id="milestone",
                        goal_id="goal", comment_id="comment", owner_id="owner",
                        qualification_id="qualification", target="beta")
        self.decision = dict(schema="nudger-beta-mandate/v1", milestoneIssueId="milestone",
                             projectId="project", goalId="goal", target="beta",
                             scopePolicy="evolving-until-review", sensitiveChanges="approved-design-only",
                             qualificationIssueId="qualification", productionAuthorized=False)
        self.records = [
            dict(id="comment", companyId="company", issueId="milestone", authorUserId="owner",
                 authorType="user", sourceTrust=None, derivedAuthorSource=None,
                 body=json.dumps(self.decision)),
            dict(id="milestone", companyId="company", projectId="project", goalId="goal",
                 status="in_progress", reviewPolicy="human_only"),
            dict(id="goal", companyId="company", status="active"),
            dict(id="qualification", companyId="company", projectId="project", status="done"),
        ]

    def test_live_delegation_is_not_deployment_authorization(self):
        with patch("beta_mandate.api_get", side_effect=copy.deepcopy(self.records * 2)):
            report = check_live("https://paperclip.example", "token", **self.ids)
        self.assertTrue(report["mandateValid"])
        self.assertFalse(report["authorized"])
        self.assertFalse(report["scopeFrozen"])

    def test_human_review_freezes_scope_but_allows_qualified_corrections(self):
        self.records[1]["status"] = "in_review"
        self.assertTrue(validate_mandate(*self.records, **self.ids)["scopeFrozen"])

    def test_production_and_other_project_are_refused(self):
        for key, value in [("target", "production"), ("project_id", "foreign"),
                           ("owner_id", "other"), ("comment_id", "forged")]:
            with self.subTest(key=key), self.assertRaises(MandateError):
                validate_mandate(*self.records, **{**self.ids, key: value})

    def test_forged_revoked_or_unqualified_records_are_refused(self):
        cases = [(0, "authorType", "agent"), (0, "authorAgentId", "agent"),
                 (0, "onBehalfOfUserId", "owner"), (0, "derivedAuthorAgentId", "agent"),
                 (0, "derivedCreatedByRunId", "run"), (0, "derivedAuthorSource", "external_chat"),
                 (0, "sourceTrust", "external_chat"), (0, "sourceTrust", "untrusted"),
                 (0, "deletedAt", "now"), (0, "deletedByType", "agent"),
                 (0, "deletedByUserId", "owner"), (0, "deletedByAgentId", "agent"),
                 (0, "deletedByRunId", "run"), (0, "companyId", "foreign"),
                 (0, "body", "CI green"), (0, "body", "[]"),
                 (1, "status", "backlog"), (1, "status", "blocked"),
                 (1, "status", "done"), (1, "status", "cancelled"),
                 (1, "reviewPolicy", "anyone"), (1, "hiddenAt", "now"),
                 (2, "status", "achieved"), (2, "status", "cancelled"),
                 (3, "status", "in_review"), (3, "status", "blocked")]
        for index, key, value in cases:
            with self.subTest(index=index, key=key, value=value):
                records = copy.deepcopy(self.records)
                records[index][key] = value
                with self.assertRaises(MandateError):
                    validate_mandate(*records, **self.ids)

    def test_missing_or_widened_contract_is_refused(self):
        for key in self.decision:
            records = copy.deepcopy(self.records)
            body = dict(self.decision)
            del body[key]
            records[0]["body"] = json.dumps(body)
            with self.subTest(key=key), self.assertRaises(MandateError):
                validate_mandate(*records, **self.ids)
        self.decision["productionAuthorized"] = 0
        self.records[0]["body"] = json.dumps(self.decision)
        with self.assertRaises(MandateError):
            validate_mandate(*self.records, **self.ids)

    def test_validator_only_reads_documented_live_fields(self):
        records = [
            RealShapeDict(COMMENT_CONTRACT_FIELDS, self.records[0]),
            RealShapeDict(ISSUE_CONTRACT_FIELDS, self.records[1]),
            RealShapeDict(GOAL_CONTRACT_FIELDS, self.records[2]),
            RealShapeDict(QUALIFICATION_CONTRACT_FIELDS, self.records[3]),
        ]
        report = validate_mandate(*records, **self.ids)
        self.assertTrue(report["mandateValid"])

    def test_concurrent_revocation_fails_closed(self):
        after = copy.deepcopy(self.records)
        after[2]["status"] = "cancelled"
        with patch("beta_mandate.api_get", side_effect=self.records + after):
            with self.assertRaises(MandateError):
                check_live("https://paperclip.example", "token", **self.ids)

    def test_transport_never_redirects_or_accepts_plain_http(self):
        for base in ["http://paperclip.example", "https://user:secret@paperclip.example",
                     "https://paperclip.example?token=secret"]:
            with self.assertRaises(MandateError):
                api_get(base, "token", "/goals/goal")
        with patch("beta_mandate.urllib.request.build_opener") as build:
            build.return_value.open.return_value = io.StringIO("{}")
            api_get("https://paperclip.example", "token", "/goals/goal")
            handler = build.call_args.args[0]
            self.assertIsNone(handler.redirect_request(None, None, 302, "", {}, "https://other"))

    def test_api_failure_is_nonzero_and_does_not_echo_secrets(self):
        argv = [item for key, value in self.ids.items() for item in ("--" + key.replace("_", "-"), value)]
        with patch("beta_mandate.check_live", side_effect=urllib.error.URLError("secret")):
            with patch("sys.stderr", new_callable=io.StringIO) as err:
                self.assertEqual(2, main(argv))
                self.assertNotIn("secret", err.getvalue())


if __name__ == "__main__":
    unittest.main()
