import { computed, toValue, type MaybeRef } from 'vue'

import {
  DEFAULT_EVENT_PACK,
  EVENT_PACK_I18N_BASE_KEY,
  type EventPackName,
} from '~~/config/theme/event-packs'

type ResolveOptions = {
  /** Cles de fallback additionnelles (apres la racine) */
  fallbackKeys?: string[]
  /** Desactiver le fallback automatique vers la racine */
  noRootFallback?: boolean
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

/**
 * Composable pour resoudre les ressources i18n d'un pack evenementiel.
 *
 * ## Chaine de resolution (fallback automatique)
 *
 * 1. `packs.{packActuel}.{path}` - Valeur specifique au pack actif
 * 2. `packs.default.{path}` - Valeur par defaut des packs
 * 3. `{path}` - Fallback vers la racine du fichier i18n
 * 4. `fallbackKeys` - Cles de fallback additionnelles (optionnel)
 *
 * ## Exemple
 *
 * ```ts
 * const activeEventPack = useSeasonalEventPack()
 * const packI18n = useEventPackI18n(activeEventPack)
 *
 * // Cherche: packs.bastille-day.hero.title
 * //    puis: packs.default.hero.title
 * //    puis: hero.title (racine)
 * const title = packI18n.resolveString('hero.title')
 * ```
 *
 * ## Principe de surcharge
 *
 * - Les packs peuvent surcharger N'IMPORTE QUELLE cle i18n
 * - Si non definie dans le pack, on remonte vers default puis vers la racine
 * - Le fallback vers la racine permet de ne definir que les cles a personnaliser
 */
export const useEventPackI18n = (packName: MaybeRef<EventPackName>) => {
  const { tm, te } = useI18n()

  const variantSeeds = useState<Record<string, number>>(
    'event-pack-variant-seeds',
    () => ({})
  )

  const buildKey = (path: string, pack: EventPackName) =>
    `${EVENT_PACK_I18N_BASE_KEY}.${pack}.${path}`

  /**
   * Resout une valeur brute avec la chaine de fallback complete.
   *
   * Ordre de resolution:
   * 1. packs.{pack}.{path}
   * 2. packs.default.{path}
   * 3. {path} (racine) - sauf si noRootFallback=true
   * 4. fallbackKeys additionnels
   */
  const resolveRaw = (path: string | string[], options?: ResolveOptions) => {
    const normalizedPath = toFlatPath(path)
    const pack = toValue(packName)

    // 1. Chercher dans le pack actif
    const packKey = buildKey(normalizedPath, pack)
    if (te(packKey)) {
      return tm(packKey)
    }

    // 2. Chercher dans le pack default
    if (pack !== DEFAULT_EVENT_PACK) {
      const defaultKey = buildKey(normalizedPath, DEFAULT_EVENT_PACK)
      if (te(defaultKey)) {
        return tm(defaultKey)
      }
    }

    // 3. Fallback vers la racine (comportement i18n standard)
    if (!options?.noRootFallback && te(normalizedPath)) {
      return tm(normalizedPath)
    }

    // 4. Fallback keys additionnels
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
