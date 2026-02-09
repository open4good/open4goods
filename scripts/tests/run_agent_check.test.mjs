import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, writeFileSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';

function buildArgs(baseDir, outFile, provider = '') {
  const args = [
    'scripts/ai/run_agent.mjs',
    '--check',
    '--config',
    '.github/ai/agent.config.json',
    '--template',
    join(baseDir, 'template.md'),
    '--schema',
    join(baseDir, 'schema.json'),
    '--context',
    join(baseDir, 'context.json'),
    '--out',
    outFile
  ];
  if (provider) {
    args.push('--provider', provider);
  }
  return args;
}

function setupFiles() {
  const dir = mkdtempSync(join(tmpdir(), 'run-agent-check-'));
  writeFileSync(join(dir, 'template.md'), '# Test template');
  writeFileSync(join(dir, 'schema.json'), '{}');
  writeFileSync(join(dir, 'context.json'), '{}');
  return dir;
}

test('check mode fails when provider secret is missing', () => {
  const dir = setupFiles();
  const outFile = join(dir, 'out.json');

  const env = { ...process.env };
  delete env.OPENAI_API_KEY;

  const result = spawnSync('node', buildArgs(dir, outFile, 'codex'), {
    encoding: 'utf8',
    env
  });

  assert.equal(result.status, 1);
  const report = JSON.parse(readFileSync(outFile, 'utf8'));
  assert.equal(report.ok, false);
  assert.deepEqual(report.missing_secrets, ['OPENAI_API_KEY']);
});

test('check mode succeeds when provider secret is present', () => {
  const dir = setupFiles();
  const outFile = join(dir, 'out.json');

  const env = {
    ...process.env,
    OPENAI_API_KEY: 'test-key'
  };

  const result = spawnSync('node', buildArgs(dir, outFile, 'codex'), {
    encoding: 'utf8',
    env
  });

  assert.equal(result.status, 0);
  const report = JSON.parse(readFileSync(outFile, 'utf8'));
  assert.equal(report.ok, true);
  assert.deepEqual(report.missing_secrets, []);
  assert.equal(report.providers[0].provider, 'codex');
});
