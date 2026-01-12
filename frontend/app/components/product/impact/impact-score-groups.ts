import type { ScoreView } from './impact-types'

type Translator = (key: string) => string

export const DIVERS_AGGREGATE_ID = 'DIVERS'

export type ImpactScoreGroup<T extends ScoreView = ScoreView> = {
  id: string
  label: string
  aggregate: T | null
  subscores: T[]
}

const normalizeParticipations = (participations?: string[] | null): string[] =>
  (participations ?? [])
    .map(entry => entry?.toString().trim().toUpperCase())
    .filter((entry): entry is string => Boolean(entry))

const resolveAggregateLabel = <T extends ScoreView>(
  t: Translator,
  aggregateId: string,
  aggregateScore: T | null
) => {
  const translationKey = `product.impact.aggregateScores.${aggregateId}`
  if (t(translationKey) !== translationKey) {
    return t(translationKey)
  }

  return aggregateScore?.label ?? aggregateId
}

const normalizeScoreId = (value: unknown) =>
  typeof value === 'string' ? value.trim().toUpperCase() : ''

export const buildImpactScoreGroups = <T extends ScoreView>(
  scores: T[],
  t: Translator
): { groups: ImpactScoreGroup<T>[]; divers: ImpactScoreGroup<T> | null } => {
  const filteredScores = scores.filter(
    score => normalizeScoreId(score.id) !== 'ECOSCORE'
  )

  const scoreMap = filteredScores.reduce<Map<string, T>>((map, score) => {
    const normalizedId = normalizeScoreId(score.id)
    if (normalizedId) {
      map.set(normalizedId, score)
    }

    return map
  }, new Map())

  const aggregateSet = new Set<string>()
  const participationMap = new Map<string, T[]>()
  const standalone: T[] = []

  filteredScores.forEach(score => {
    const participations = normalizeParticipations(score.participateInScores)

    if (!participations.length) {
      standalone.push(score)
      return
    }

    participations.forEach(aggregateId => {
      aggregateSet.add(aggregateId)

      if (!participationMap.has(aggregateId)) {
        participationMap.set(aggregateId, [])
      }

      participationMap.get(aggregateId)?.push(score)
    })
  })

  const groups = Array.from(participationMap.entries()).map(
    ([id, subscores]) => {
      const aggregate = scoreMap.get(id) ?? null
      return {
        id,
        label: resolveAggregateLabel(t, id, aggregate),
        aggregate,
        subscores,
      }
    }
  )

  const filteredStandalone = standalone.filter(score => {
    const normalizedId = normalizeScoreId(score.id)
    return normalizedId && !aggregateSet.has(normalizedId)
  })

  const divers = filteredStandalone.length
    ? {
        id: DIVERS_AGGREGATE_ID,
        label: resolveAggregateLabel(t, DIVERS_AGGREGATE_ID, null),
        aggregate: null,
        subscores: filteredStandalone,
      }
    : null

  return { groups, divers }
}

export const buildImpactAggregateAnchorId = (aggregateId: string) => {
  const normalized = aggregateId.trim().toLowerCase()
  const safeId = normalized.replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
  return `impact-criteria-${safeId || 'unknown'}`
}
