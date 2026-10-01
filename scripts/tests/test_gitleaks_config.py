#!/usr/bin/env python3
"""Exercise reviewed false positives without suppressing nearby credentials."""

import importlib.util
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

_SPEC = importlib.util.spec_from_file_location(
    'secret_scan', ROOT / 'scripts/verify/secret_scan.py')
_SCAN = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_SCAN)


class BaselineNoteTest(unittest.TestCase):
    def test_credential_shaped_notes_are_not_stale(self):
        """Every accepted credential-shaped identity must still name a live disposition:
        the owner's revocation attestation, or the follow-up rotation issue. A note that
        names neither (e.g. because GOU-107 closed and the text was never updated) must
        fail loudly instead of silently going stale."""
        baseline = _SCAN.load_baseline(_SCAN.BASELINE)
        markers = ('GOU-107', 'attested revoked')
        for mode in ('git', 'dir'):
            for entry in baseline['accepted'][mode]:
                if entry['classification'] != 'credential-shaped':
                    continue
                self.assertTrue(
                    any(marker in entry['note'] for marker in markers),
                    f"stale note for {entry['rule']}/{entry['file']}: {entry['note']!r}")


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
            baseline_text = (
                "storage_key: nudger-ip-quota-v1\nkey: ENERGY_CONSUMPTION_100_CYCLES\n"
                "key: latencyP50Ms\nmodel_key: NV7B4550VAS/U1\n")
            path.write_text(baseline_text)
            code, findings = self.scan(source)
            self.assertEqual(0, code)
            self.assertEqual([], findings)

            # A generated canary is not a provider credential and must still be detected.
            # Gitleaks' built-in generic-api-key rule rejects any match containing one of
            # its internal stopwords (dead, cafe, face, feed, bad, ace, ...). A random hex
            # token hits one of those ~7% of the time, which made this test flaky (GOU-163).
            # Regenerate the canary until the rule actually fires, capped at a handful of
            # attempts so a real regression (rule disabled or broken, not just unlucky
            # stopwords) still fails loudly instead of retrying forever.
            password = secrets.token_hex(20)
            max_attempts = 8
            rule_ids = set()
            for _ in range(max_attempts):
                canary = secrets.token_hex(24)
                path.write_text(
                    baseline_text
                    + "api_key: " + canary + "\n"
                    + "password: " + password + "\n")
                code, findings = self.scan(source)
                rule_ids = {item["RuleID"] for item in findings}
                if "generic-api-key" in rule_ids:
                    break
            else:
                self.fail(
                    f"generic-api-key did not fire on any of {max_attempts} regenerated "
                    "canaries; this looks like the rule being disabled or broken, not bad luck")

            self.assertEqual(1, code)
            self.assertIn("generic-api-key", rule_ids)
            self.assertIn("o4g-spring-inline-credential", rule_ids)

    def test_generic_api_key_rule_fires_on_known_non_stopword_canary(self):
        """Fixed, non-random canary confirmed to contain none of gitleaks' generic-api-key
        stopwords. Unlike test_exact_identifiers_only above, this never retries: if
        generic-api-key is disabled or its pattern regresses, this must fail every run, so
        the retry loop above can never mask a real break in the rule."""
        with tempfile.TemporaryDirectory(prefix="o4g-gitleaks-") as tmp:
            source = Path(tmp) / "source"
            source.mkdir()
            path = source / "config.yml"
            known_canary = "0e00fddf60ae3cf13bd37c75b0c2bf7a33452b05157ffa8c"
            path.write_text("api_key: " + known_canary + "\n")
            code, findings = self.scan(source)
            self.assertEqual(1, code)
            self.assertIn("generic-api-key", {item["RuleID"] for item in findings})

    def test_unquoted_js_identifier_allowlisted_but_quoted_literal_detected(self):
        """GOU-103: `secret: undefined` / `secret: name` are identifiers, not values, only
        when unquoted on the `.js/.mjs/.ts/.vue` extensions the allowlist targets; a real
        quoted literal on the same key and extensions must stay detected."""
        with tempfile.TemporaryDirectory(prefix="o4g-gitleaks-") as tmp:
            source = Path(tmp) / "source"
            source.mkdir()
            (source / "Fixture.vue").write_text(
                "<script setup lang=\"ts\">\n"
                "withDefaults(defineProps<{ secret?: string }>(), {\n"
                "  secret: undefined,\n"
                "})\n"
                "</script>\n"
            )
            (source / "fixture.mjs").write_text(
                "const secretName = 'API_KEY';\n"
                "checks.push({ secret: secretName });\n"
            )
            code, findings = self.scan(source)
            self.assertEqual(0, code, "unquoted JS identifiers must not be flagged")
            self.assertEqual([], findings)

            canary = secrets.token_hex(20)
            (source / "Fixture.vue").write_text(
                "<script setup lang=\"ts\">\n"
                "withDefaults(defineProps<{ secret?: string }>(), {\n"
                f"  secret: \"{canary}\",\n"
                "})\n"
                "</script>\n"
            )
            (source / "fixture.mjs").write_text(
                f"checks.push({{ secret: \"{canary}\" }});\n"
            )
            code, findings = self.scan(source)
            self.assertEqual(1, code, "a quoted literal on the same key must stay detected")
            files = {item["File"] for item in findings}
            self.assertTrue(any(f.endswith("Fixture.vue") for f in files))
            self.assertTrue(any(f.endswith("fixture.mjs") for f in files))

    def test_doc_placeholder_key_allowlisted_but_real_bearer_token_detected(self):
        """GOU-28: the two documented `pdapi_` placeholders carry no secret, so a new facet
        doc page must not need a baseline entry for its curl example. A real-looking bearer
        token in the very same header shape must still be detected - the allowlist is
        anchored on the exact placeholder, not on the surrounding `curl -H` line."""
        with tempfile.TemporaryDirectory(prefix="o4g-gitleaks-") as tmp:
            source = Path(tmp) / "source"
            source.mkdir()
            page = source / "facet.md"
            page.write_text(
                'curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \\\n'
                '  "https://example.test/api/v1/products/0885909950805/price/history"\n'
                'curl -H "Authorization: Bearer pdapi_VOTRE_CLÉ_ICI" \\\n'
                '  "https://example.test/api/v1/products/0885909950805/price/history"\n'
            )
            code, findings = self.scan(source)
            self.assertEqual(0, code, "documented placeholder keys must not be flagged")
            self.assertEqual([], findings)

            canary = "pdapi_" + secrets.token_hex(20)
            with page.open("a") as stream:
                stream.write(f'curl -H "Authorization: Bearer {canary}"\n')
            code, findings = self.scan(source)
            self.assertEqual(1, code, "a real bearer token must stay detected")
            self.assertIn("curl-auth-header", {item["RuleID"] for item in findings})


if __name__ == "__main__":
    if not SCANNER:
        raise SystemExit("Install the pinned gitleaks version or set GITLEAKS_BIN")
    unittest.main()
