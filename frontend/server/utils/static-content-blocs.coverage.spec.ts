import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { getStaticContentBloc } from './static-content-blocs'

/**
 * front-api's /blocs/{blocId} live-XWiki endpoint was removed once every bloc id requested by
 * the frontend was confirmed to have real static content (xwiki-editorial-content-to-nuxt-content
 * AC2). There is no live fallback left, so a `bloc-id` added to application.yml without a
 * matching entry here would silently 404 in production -- this test is the coverage guardrail
 * that replaces the removed backend call.
 */
const APPLICATION_YML_PATH = resolve(
  process.cwd(),
  '../front-api/src/main/resources/application.yml'
)

function configuredBlocIds(): string[] {
  const yaml = readFileSync(APPLICATION_YML_PATH, 'utf-8')
  const ids: string[] = []
  for (const match of yaml.matchAll(/bloc-id:\s*'([^']+)'/g)) {
    ids.push(match[1])
  }
  return ids
}

function teamTitleVariant(blocId: string): string {
  return blocId.endsWith(':')
    ? `${blocId.slice(0, -1)}-title:`
    : `${blocId}-title`
}

describe('static content bloc coverage (front-api application.yml)', () => {
  const configured = configuredBlocIds()

  it('found the team/partner bloc ids declared in application.yml', () => {
    // Guards the guard: fails loudly if the yaml path or bloc-id shape ever changes silently.
    expect(configured.length).toBeGreaterThan(0)
  })

  it.each(configured)('has static content for configured bloc id %s', (blocId) => {
    expect(getStaticContentBloc(blocId, 'fr')).not.toBeNull()
  })

  it.each(configured.filter((id) => id.startsWith('pages:team:')))(
    'has static content for the team -title variant of %s',
    (blocId) => {
      expect(getStaticContentBloc(teamTitleVariant(blocId), 'fr')).not.toBeNull()
    }
  )
})
