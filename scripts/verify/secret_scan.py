#!/usr/bin/env python3
"""Scan secrets with an exact, owner-accepted historical baseline; emit no values."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / '.gitleaks-baseline.json'


def identity(finding, mode):
    """Bind an exception to content, file, rule and (for history) original commit."""
    fields = [finding['RuleID'], finding['File'], finding['Secret'], finding['Match']]
    if mode == 'git':
        fields.append(finding['Commit'])
    return hashlib.sha256(json.dumps(fields, ensure_ascii=True).encode()).hexdigest()


def scan(mode, source, revision=None):
    """Keep unredacted evidence in a private temporary directory, never CI output."""
    with tempfile.TemporaryDirectory(prefix='o4g-secret-scan-') as tmp:
        report = Path(tmp) / 'report.json'
        command = [os.environ.get('GITLEAKS_BIN', 'gitleaks'), mode, '.',
                   '--config', str(ROOT / '.gitleaks.toml'), '--report-format', 'json',
                   '--report-path', str(report), '--no-banner']
        if mode == 'git':
            command.extend(['--log-opts', revision or 'HEAD'])
        result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                                check=False, cwd=source)
        if result.returncode not in (0, 1) or not report.exists():
            raise ValueError('Scanner failed; no exception was applied')
        findings = json.loads(report.read_text())
        if not isinstance(findings, list) or (result.returncode == 1 and not findings):
            raise ValueError('Invalid scanner report')
        return findings


def load_baseline(path):
    data = json.loads(path.read_text())
    if data.get('version') != 1 or not data.get('acceptance') or not data.get('sourceCommit'):
        raise ValueError('Invalid baseline metadata')
    for mode in ('git', 'dir'):
        entries = data['accepted'][mode]
        if not isinstance(entries, list) or any(
                not isinstance(entry, str) or len(entry) != 64
                or any(char not in '0123456789abcdef' for char in entry) for entry in entries):
            raise ValueError('Invalid baseline identities')
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['git', 'dir'])
    args = parser.parse_args()
    try:
        baseline = load_baseline(BASELINE)
        findings = scan(args.mode, '.')
        accepted = set(baseline['accepted'][args.mode])
        new = [item for item in findings if identity(item, args.mode) not in accepted]
        # Counts only: paths and scanner diagnostics can themselves contain sensitive data.
        print(json.dumps({'mode': args.mode, 'findings': len(findings),
                          'acceptedHistorical': len(findings) - len(new), 'new': len(new)}))
        return 1 if new else 0
    except (OSError, ValueError, KeyError, TypeError):
        print('Secret scan failed closed; inspect privately. No secret values emitted.')
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
