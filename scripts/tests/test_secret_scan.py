#!/usr/bin/env python3
"""Ensure the historical exception cannot suppress a different credential."""

import importlib.util
from pathlib import Path
import unittest
import json
import tempfile
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location(
    'secret_scan', Path(__file__).resolve().parents[1] / 'verify/secret_scan.py')
SCAN = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCAN)


def _entry(scan_id, mode, **overrides):
    entry = {'id': scan_id, 'rule': 'rule', 'file': 'config.yml',
             'classification': 'false-positive-test', 'note': 'fixture'}
    if mode == 'git':
        entry['commit'] = 'abc1234'
    entry.update(overrides)
    return entry


class BaselineTest(unittest.TestCase):
    def test_exception_is_bound_to_content_and_location(self):
        original = dict(RuleID='rule', File='config.yml', Secret='fixture',
                        Match='key: fixture', Commit='abc')
        for mode in ('git', 'dir'):
            for field in ('RuleID', 'File', 'Secret', 'Match'):
                changed = dict(original, **{field: 'different'})
                self.assertNotEqual(SCAN.identity(original, mode), SCAN.identity(changed, mode))
        self.assertNotEqual(SCAN.identity(original, 'git'),
                            SCAN.identity(dict(original, Commit='new'), 'git'))
        self.assertEqual(SCAN.identity(original, 'dir'),
                         SCAN.identity(dict(original, StartLine=200), 'dir'))

    def test_malformed_baseline_rejected(self):
        base = {'version': 1, 'acceptance': 'owner', 'sourceCommit': 'abc',
                'accepted': {'git': [], 'dir': []}}
        digest = 'a' * 64
        cases = [
            {},
            dict(base, accepted={'git': ['not-an-object'], 'dir': []}),
            dict(base, accepted={'git': [_entry(digest, 'git')], 'dir': []},
                 version=2),
            dict(base, accepted={
                'git': [_entry('too-short', 'git')], 'dir': []}),
            dict(base, accepted={
                'git': [_entry(digest, 'git', classification='not-a-real-classification')],
                'dir': []}),
            dict(base, accepted={
                'git': [_entry(digest, 'git', note='')], 'dir': []}),
            dict(base, accepted={
                # dir entries must not carry a git-only 'commit' field.
                'git': [], 'dir': [_entry(digest, 'git')]}),
        ]
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'baseline.json'
            for value in cases:
                path.write_text(json.dumps(value))
                with self.assertRaises(ValueError):
                    SCAN.load_baseline(path)

    def test_valid_baseline_accepted(self):
        digest = 'a' * 64
        base = {'version': 1, 'acceptance': 'owner', 'sourceCommit': 'abc',
                'accepted': {'git': [_entry(digest, 'git')],
                             'dir': [_entry(digest, 'dir')]}}
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'baseline.json'
            path.write_text(json.dumps(base))
            loaded = SCAN.load_baseline(path)
        self.assertEqual(loaded['accepted']['git'][0]['id'], digest)

    def test_new_value_fails_even_in_same_location(self):
        original = dict(RuleID='rule', File='config.yml', Secret='fixture',
                        Match='key: fixture', Commit='abc')
        baseline = {'accepted': {'dir': [_entry(SCAN.identity(original, 'dir'), 'dir')]}}
        with patch('sys.argv', ['secret_scan.py', 'dir']), \
                patch.object(SCAN, 'load_baseline', return_value=baseline), \
                patch('builtins.print'):
            with patch.object(SCAN, 'scan', return_value=[original]):
                self.assertEqual(0, SCAN.main())
            with patch.object(SCAN, 'scan', return_value=[dict(original, Secret='changed')]):
                self.assertEqual(1, SCAN.main())
            with patch.object(SCAN, 'scan', side_effect=ValueError('failure')):
                self.assertEqual(2, SCAN.main())

    def test_unbaselined_finding_reports_new_one_and_fails(self):
        """A finding outside the baseline must surface as `new: 1` and exit 1, not silently pass."""
        baseline = {'accepted': {'dir': []}}
        finding = dict(RuleID='generic-api-key', File='fresh.yml', Secret='fixture',
                       Match='key: fixture', StartLine=3)
        printed = []
        with patch('sys.argv', ['secret_scan.py', 'dir']), \
                patch.object(SCAN, 'load_baseline', return_value=baseline), \
                patch.object(SCAN, 'scan', return_value=[finding]), \
                patch('builtins.print', side_effect=printed.append):
            self.assertEqual(1, SCAN.main())
        summary = json.loads(printed[0])
        self.assertEqual(1, summary['new'])
        self.assertEqual(0, summary['acceptedHistorical'])
        triage = json.loads(printed[1])
        self.assertEqual({'RuleID': 'generic-api-key', 'File': 'fresh.yml', 'StartLine': 3},
                         triage)
        self.assertNotIn('Secret', json.dumps(printed))
        self.assertNotIn('fixture', json.dumps(printed))

    def test_invalid_baseline_fails_closed(self):
        """A structurally invalid baseline must exit 2 and never reach the scanner."""
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'baseline.json'
            path.write_text(json.dumps({'version': 1}))
            with patch('sys.argv', ['secret_scan.py', 'dir']), \
                    patch.object(SCAN, 'BASELINE', path), \
                    patch.object(SCAN, 'scan') as scan_mock, \
                    patch('builtins.print'):
                self.assertEqual(2, SCAN.main())
            scan_mock.assert_not_called()

    def test_emit_identities_never_prints_secret_or_match(self):
        finding = dict(RuleID='generic-api-key', File='fresh.yml', Secret='fixture-secret',
                       Match='key: fixture-secret', StartLine=3, Commit='abc1234')
        printed = []
        with patch('sys.argv', ['secret_scan.py', 'git', '--emit-identities']), \
                patch.object(SCAN, 'scan', return_value=[finding]), \
                patch('builtins.print', side_effect=printed.append):
            self.assertEqual(0, SCAN.main())
        self.assertEqual(1, len(printed))
        record = json.loads(printed[0])
        self.assertEqual({'id', 'RuleID', 'File', 'StartLine', 'Commit'}, set(record.keys()))
        self.assertNotIn('fixture-secret', printed[0])


if __name__ == '__main__':
    unittest.main()
