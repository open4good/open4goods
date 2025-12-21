import { computed, toValue, type MaybeRef } from 'vue'

import { DEFAULT_EVENT_PACK, type EventPackName } from '~~/config/theme/assets'

const BASE_KEY = 'home.events'

type ResolveOptions = {
  fallbackKeys?: string[]
}

type VariantOptions = ResolveOptions & {
  randomize?: boolean
  stateKey?: string
}

const toFlatPath = (path: string | string[]) =>
  Array.isArray(path) ? path.join('.') : path

const toStringArray = (value: unknown): string[] => {
  if (!Array.isArray(value)) {
    return []
  }

  return value
    .map(entry => (typeof entry === 'string' ? entry.trim() : ''))
    .filter(Boolean)
}

export const useEventPackI18n = (packName: MaybeRef<EventPackName>) => {
  const { tm, te } = useI18n()

  const variantSeeds = useState<Record<string, number>>(
    'event-pack-variant-seeds',
    () => ({})
  )

  const buildKey = (path: string, pack: EventPackName) =>
    `${BASE_KEY}.${pack}.${path}`

  const resolveRaw = (path: string | string[], options?: ResolveOptions) => {
    const normalizedPath = toFlatPath(path)
    const pack = toValue(packName)

    const packKey = buildKey(normalizedPath, pack)

    if (te(packKey)) {
      return tm(packKey)
    }

    const defaultKey = buildKey(normalizedPath, DEFAULT_EVENT_PACK)

    if (te(defaultKey)) {
      return tm(defaultKey)
    }

    for (const key of options?.fallbackKeys ?? []) {
      if (te(key)) {
        return tm(key)
      }
    }

    return undefined
  }

  const pickVariant = (key: string, values: string[]): string | undefined => {
    if (values.length === 0) {
      return undefined
    }

    if (!variantSeeds.value[key]) {
      variantSeeds.value[key] = Math.random()
    }

    const seed = variantSeeds.value[key]
    const index = Math.floor(seed * values.length) % values.length

    return values[index]
  }

  const resolveString = (
    path: string | string[],
    options?: ResolveOptions
  ): string | undefined => {
    const raw = resolveRaw(path, options)

    if (typeof raw === 'string') {
      const trimmed = raw.trim()
      return trimmed.length > 0 ? trimmed : undefined
    }

    return undefined
  }

  const resolveStringVariant = (
    path: string | string[],
    options?: VariantOptions
  ): string | undefined => {
    const raw = resolveRaw(path, options)
    const normalizedPath = toFlatPath(path)

    if (typeof raw === 'string') {
      const trimmed = raw.trim()
      return trimmed.length > 0 ? trimmed : undefined
    }

    const variants = toStringArray(raw)
    const shouldRandomize = options?.randomize ?? true

    if (!shouldRandomize) {
      return variants[0]
    }

    const stateKey = options?.stateKey ?? normalizedPath
    return pickVariant(stateKey, variants)
  }

  const resolveList = <T = unknown>(
    path: string | string[],
    options?: ResolveOptions
  ): T[] => {
    const raw = resolveRaw(path, options)

    if (!Array.isArray(raw)) {
      return []
    }

    return raw as T[]
  }

  return {
    resolveRaw,
    resolveString,
    resolveStringVariant,
    resolveList,
    packKey: computed(() => toValue(packName)),
  }
}
