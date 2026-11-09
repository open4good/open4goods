/**
 * Single source of the Impact Score method history shown on /impact-score/versions.
 * Texts live in the `impactScoreVersionsPage.entries.<id>` i18n keys; this file only
 * holds the structure (order, status, date, number of changes). To add a version,
 * add an entry here and its texts in both locale files.
 *
 * `date` is an ISO day, left out when the date was not recorded. A `planned` version
 * is announced but not yet in force on any vertical.
 */
export type ImpactScoreVersionStatus = 'current' | 'planned' | 'past'

export interface ImpactScoreVersionEntry {
  id: string
  status: ImpactScoreVersionStatus
  date?: string
  /** Number of change/reason pairs, `changes.<index>.change|reason` in i18n. */
  changeCount: number
}

export const IMPACT_SCORE_VERSIONS: readonly ImpactScoreVersionEntry[] = [
  { id: 'v2', status: 'planned', changeCount: 4 },
  { id: 'v1Fixes', status: 'current', date: '2026-11-09', changeCount: 3 },
  { id: 'v1', status: 'current', changeCount: 1 },
]

/** Latest `date` of the history, used as `dateModified` in JSON-LD. */
export const IMPACT_SCORE_VERSIONS_LAST_MODIFIED: string | undefined =
  IMPACT_SCORE_VERSIONS.map(entry => entry.date)
    .filter((date): date is string => Boolean(date))
    .sort()
    .at(-1)

/** Method version in force today; bump together with the `planned` -> `current` switch. */
export const IMPACT_SCORE_CURRENT_METHOD_VERSION = '1'
