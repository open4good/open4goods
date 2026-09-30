#!/usr/bin/env python3
"""Exercise reviewed false positives without suppressing nearby credentials."""

import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCANNER = os.environ.get("GITLEAKS_BIN") or shutil.which("gitleaks")


class GitleaksConfigTest(unittest.TestCase):
    def scan(self, directory: Path):
        report = directory.parent / "report.json"
        result = subprocess.run(
            [SCANNER, "dir", str(directory), "--config", str(ROOT / ".gitleaks.toml"),
             "--redact", "--report-format", "json", "--report-path", str(report), "--no-banner"],
            capture_output=True, check=False,
        )
        # Do not emit scanner stdout/stderr: fixture values are deliberately secret-shaped.
        self.assertIn(result.returncode, [0, 1], "Scanner configuration or invocation failed")
        return result.returncode, json.loads(report.read_text()) if report.exists() else []

    def test_exact_identifiers_only(self):
        with tempfile.TemporaryDirectory(prefix="o4g-gitleaks-") as tmp:
            source = Path(tmp) / "source"
            source.mkdir()
            path = source / "config.yml"
            path.write_text("storage_key: nudger-ip-quota-v1\nkey: ENERGY_CONSUMPTION_100_CYCLES\n"
                            "key: latencyP50Ms\nmodel_key: NV7B4550VAS/U1\n")
            code, findings = self.scan(source)
            self.assertEqual(0, code)
            self.assertEqual([], findings)
            # A generated canary is not a provider credential and must still be detected.
            canary = secrets.token_hex(24)
            with path.open("a") as stream:
                stream.write("api_key: " + canary + "\n")
                stream.write("password: " + secrets.token_hex(20) + "\n")
            code, findings = self.scan(source)
            self.assertEqual(1, code)
            self.assertIn("generic-api-key", {item["RuleID"] for item in findings})
            self.assertIn("o4g-spring-inline-credential", {item["RuleID"] for item in findings})


if __name__ == "__main__":
    if not SCANNER:
        raise SystemExit("Install the pinned gitleaks version or set GITLEAKS_BIN")
    unittest.main()
