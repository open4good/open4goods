"""Regression scenarios for dynamic phase readiness and the deployment freeze."""

import copy
import io
import json
import unittest
from pathlib import Path
from unittest.mock import patch

import yaml

from paperclip_readiness import assess, fetch_issues

ROOT = Path(__file__).resolve().parents[2]


def issue(identifier, phase="development", status="done"):
    return {"id": identifier, "identifier": identifier, "projectId": "project",
            "status": status, "labels": [{"name": f"phase: {phase}"}]}


class ReadinessTest(unittest.TestCase):
    def setUp(self):
        self.issues = [issue("GOU-45"), issue("GOU-31", "beta validation")]

    def test_readiness_is_never_authorization(self):
        result = assess(self.issues, "project", "production")
        self.assertTrue(result["ready"])
        self.assertFalse(result["authorized"])

    def test_new_or_reopened_work_invalidates_closed_gate(self):
        for status in ["backlog", "todo", "in_progress", "in_review", "blocked", "cancelled"]:
            with self.subTest(status=status):
                self.assertFalse(assess(self.issues + [issue("NEW", status=status)], "project", "beta")["ready"])

    def test_beta_work_blocks_production_but_not_beta_entry(self):
        items = self.issues + [issue("BETA", "beta validation", "todo")]
        self.assertTrue(assess(items, "project", "beta")["ready"])
        self.assertFalse(assess(items, "project", "production")["ready"])

    def test_unknown_or_multiple_labels_block(self):
        for labels in [[], [{"name": "phase: unknown"}],
                       [{"name": "phase: development"}, {"name": "phase: production"}]]:
            task = issue("NEW")
            task["labels"] = labels
            self.assertFalse(assess(self.issues + [task], "project", "beta")["ready"])

    def test_coordination_exception_cannot_exempt_an_executable_task(self):
        task = issue("GOU-63")
        task.update(labels=[], workMode="planning", status="in_progress")
        self.assertTrue(assess(self.issues + [task], "project", "beta")["ready"])
        task["workMode"] = "standard"
        self.assertFalse(assess(self.issues + [task], "project", "beta")["ready"])

    def test_missing_or_relabelled_gate_blocks(self):
        self.assertFalse(assess([issue("GOU-31", "beta validation")], "project", "beta")["ready"])
        items = copy.deepcopy(self.issues)
        items[0]["labels"] = [{"name": "phase: production"}]
        self.assertFalse(assess(items, "project", "beta")["ready"])

    def test_empty_foreign_and_duplicate_inventories_fail(self):
        foreign = issue("FOREIGN")
        foreign["projectId"] = "other"
        for items in [[], [foreign], self.issues * 2]:
            with self.assertRaises(ValueError):
                assess(items, "project", "beta")

    @patch("paperclip_readiness.urllib.request.build_opener")
    def test_short_pages_are_not_treated_as_complete(self, build):
        build.return_value.open.side_effect = [io.StringIO(json.dumps([x])) for x in self.issues] + [io.StringIO("[]")]
        self.assertEqual(self.issues, fetch_issues("https://paperclip.example", "token", "company", "project"))
        urls = [call.args[0].full_url for call in build.return_value.open.call_args_list]
        self.assertIn("offset=1", urls[1])
        self.assertIn("offset=2", urls[2])

    @patch("paperclip_readiness.urllib.request.build_opener")
    def test_repeated_pages_fail_closed(self, build):
        build.return_value.open.side_effect = [io.StringIO(json.dumps(self.issues)) for _ in range(2)]
        with self.assertRaises(ValueError):
            fetch_issues("https://paperclip.example", "token", "company", "project")

    def test_credentials_cannot_go_to_plain_http(self):
        with self.assertRaises(ValueError):
            fetch_issues("http://paperclip.example", "token", "company", "project")


class WorkflowSafetyTest(unittest.TestCase):
    def test_ci_has_no_deployment_job_or_ssh_step(self):
        for name in ["testAndPublishBeta.yml", "frontend-ci.yml", "b2b-frontend-ci.yml", "ci-pr.yml"]:
            data = yaml.safe_load((ROOT / ".github/workflows" / name).read_text())
            for job in data["jobs"].values():
                self.assertNotIn("environment", job)
                for step in job["steps"]:
                    self.assertNotIn("ssh-", step.get("uses", ""))
                    self.assertNotIn("ssh ", step.get("run", ""))

    def test_legacy_release_is_unconditionally_disabled(self):
        data = yaml.safe_load((ROOT / ".github/workflows/releaseDeployProd.yml").read_text())
        self.assertEqual({"frozen"}, set(data["jobs"]))
        self.assertIn("exit 1", data["jobs"]["frozen"]["steps"][0]["run"])


if __name__ == "__main__":
    unittest.main()
