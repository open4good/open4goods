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
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'baseline.json'
            for value in ({}, {'version': 1, 'acceptance': 'owner', 'sourceCommit': 'abc',
                               'accepted': {'git': ['invalid'], 'dir': []}}):
                path.write_text(json.dumps(value))
                with self.assertRaises(ValueError):
                    SCAN.load_baseline(path)

    def test_new_value_fails_even_in_same_location(self):
        original = dict(RuleID='rule', File='config.yml', Secret='fixture',
                        Match='key: fixture', Commit='abc')
        baseline = {'accepted': {'dir': [SCAN.identity(original, 'dir')]}}
        with patch('sys.argv', ['secret_scan.py', 'dir']), \
                patch.object(SCAN, 'load_baseline', return_value=baseline), \
                patch('builtins.print'):
            with patch.object(SCAN, 'scan', return_value=[original]):
                self.assertEqual(0, SCAN.main())
            with patch.object(SCAN, 'scan', return_value=[dict(original, Secret='changed')]):
                self.assertEqual(1, SCAN.main())
            with patch.object(SCAN, 'scan', side_effect=ValueError('failure')):
                self.assertEqual(2, SCAN.main())


if __name__ == '__main__':
    unittest.main()
