import { execFileSync } from 'node:child_process'
import { resolve } from 'node:path'
import * as mdi from '@mdi/js'
import { describe, expect, it } from 'vitest'
import { mdiIconPaths } from './mdi-icons'

const toExportName = (iconName: string) =>
  `mdi${iconName
    .slice('mdi-'.length)
    .split('-')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')}`

const mdiExports = mdi as Record<string, string>

describe('mdi icon table', () => {
  it('maps every entry to its @mdi/js path', () => {
    for (const [name, path] of Object.entries(mdiIconPaths)) {
      expect(path, name).toBe(mdiExports[toExportName(name)])
    }
  })

  it('covers every existing mdi-* icon name used in the repository', () => {
    const repoRoot = resolve(__dirname, '../../..')
    const output = execFileSync(
      'git',
      [
        'grep',
        '-hoE',
        'mdi-[a-z0-9]+(-[a-z0-9]+)*',
        '--',
        'frontend/app',
        'frontend/content',
        'frontend/shared/utils',
        'front-api/src/main',
        'verticals/src/main/resources',
        ':!*.spec.ts',
        ':!frontend/app/utils/mdi-icons.ts',
      ],
      { cwd: repoRoot, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 }
    )
    const used = new Set(output.split('\n').filter(Boolean))
    const missing = [...used].filter(
      name => mdiExports[toExportName(name)] && !(name in mdiIconPaths)
    )

    expect(missing).toEqual([])
  })
})
