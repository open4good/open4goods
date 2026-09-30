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

# Every accepted identity must carry one of these. False positives are split by why they
# are not a secret; `credential-shaped` marks a real positive the owner accepted without
# asserting revocation (see GOU-60).
CLASSIFICATIONS = frozenset([
    'false-positive-doc',
    'false-positive-placeholder',
    'false-positive-test',
    'false-positive-identifier',
    'credential-shaped',
])


def identity(finding, mode):
    """Bind an exception to content, file, rule and (for history) original commit."""
    fields = [finding['RuleID'], finding['File'], finding['Secret'], finding['Match']]
    if mode == 'git':
        fields.append(finding['Commit'])
    return hashlib.sha256(json.dumps(fields, ensure_ascii=True).encode()).hexdigest()


def scan(mode):
    """Keep unredacted evidence in a private temporary directory, never CI output."""
    with tempfile.TemporaryDirectory(prefix='o4g-secret-scan-') as tmp:
        report = Path(tmp) / 'report.json'
        command = [os.environ.get('GITLEAKS_BIN', 'gitleaks'), mode, '.',
                   '--config', str(ROOT / '.gitleaks.toml'), '--report-format', 'json',
                   '--report-path', str(report), '--no-banner']
        if mode == 'git':
            command.extend(['--log-opts', 'HEAD'])
        result = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                                check=False, cwd=ROOT)
        if result.returncode not in (0, 1) or not report.exists():
            raise ValueError('Scanner failed; no exception was applied')
        findings = json.loads(report.read_text())
        if not isinstance(findings, list) or (result.returncode == 1 and not findings):
            raise ValueError('Invalid scanner report')
        return findings


def _valid_digest(value):
    return (isinstance(value, str) and len(value) == 64
            and all(char in '0123456789abcdef' for char in value))


def _valid_entry(entry, mode):
    """Every accepted identity is auditable from the baseline alone, no secret value ever."""
    required = {'id', 'rule', 'file', 'classification', 'note'}
    if mode == 'git':
        required = required | {'commit'}
    if not isinstance(entry, dict) or set(entry.keys()) != required:
        return False
    if not _valid_digest(entry['id']):
        return False
    if not isinstance(entry['rule'], str) or not entry['rule']:
        return False
    if not isinstance(entry['file'], str) or not entry['file']:
        return False
    if entry['classification'] not in CLASSIFICATIONS:
        return False
    if not isinstance(entry['note'], str) or not entry['note'].strip():
        return False
    if mode == 'git' and (not isinstance(entry['commit'], str) or not entry['commit']):
        return False
    return True


def load_baseline(path):
    data = json.loads(path.read_text())
    if data.get('version') != 1 or not data.get('acceptance') or not data.get('sourceCommit'):
        raise ValueError('Invalid baseline metadata')
    for mode in ('git', 'dir'):
        entries = data['accepted'][mode]
        if not isinstance(entries, list) or any(not _valid_entry(entry, mode) for entry in entries):
            raise ValueError('Invalid baseline identities')
    return data


def emit_identities(mode):
    """Print an exploitable, value-free identity for every finding: never Secret or Match."""
    findings = scan(mode)
    for item in sorted(findings, key=lambda f: (f['RuleID'], f['File'], f.get('StartLine', 0))):
        record = {'id': identity(item, mode), 'RuleID': item['RuleID'], 'File': item['File'],
                   'StartLine': item.get('StartLine')}
        if mode == 'git':
            record['Commit'] = item.get('Commit')
        print(json.dumps(record, sort_keys=True))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['git', 'dir'])
    parser.add_argument('--emit-identities', action='store_true',
                        help='Print id/RuleID/File/StartLine (and Commit in git mode) per '
                             'finding, to add a baseline exception. Never Secret or Match.')
    args = parser.parse_args()
    try:
        if args.emit_identities:
            emit_identities(args.mode)
            return 0
        baseline = load_baseline(BASELINE)
        findings = scan(args.mode)
        accepted = {entry['id'] for entry in baseline['accepted'][args.mode]}
        new = [item for item in findings if identity(item, args.mode) not in accepted]
        # Counts only: paths and scanner diagnostics can themselves contain sensitive data.
        print(json.dumps({'mode': args.mode, 'findings': len(findings),
                          'acceptedHistorical': len(findings) - len(new), 'new': len(new)}))
        # Triage the new findings only: never the already-accepted ones, never the value.
        for item in sorted(new, key=lambda f: (f['RuleID'], f['File'], f.get('StartLine', 0))):
            print(json.dumps({'RuleID': item['RuleID'], 'File': item['File'],
                              'StartLine': item.get('StartLine')}))
        return 1 if new else 0
    except (OSError, ValueError, KeyError, TypeError):
        print('Secret scan failed closed; inspect privately. No secret values emitted.')
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
