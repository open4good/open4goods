import { computed, toValue, type MaybeRef } from 'vue'

import {
  EVENT_PACK_I18N_BASE_KEY,
  type EventPackName,
} from '~~/config/theme/event-packs'

type ResolveOptions = {
  /** Cles de fallback additionnelles (apres la racine) */
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

/**
 * Composable pour resoudre les ressources i18n d'un pack evenementiel.
 *
 * ## Chaine de resolution
 *
 * 1. `packs.{packActif}.{path}` - Surcharge evenementielle
 * 2. `{path}` - Valeur a la racine (par defaut)
 *
 * ## Exemple
 *
 * ```ts
 * const activeEventPack = useSeasonalEventPack()
 * const packI18n = useEventPackI18n(activeEventPack)
 *
 * // Cherche: packs.bastille-day.hero.title
 * //    puis: hero.title (racine)
 * const title = packI18n.resolveString('hero.title')
 * ```
 *
 * ## Principe de surcharge
 *
 * - Les packs surchargent uniquement les cles qu'ils redefinissent
 * - Toutes les autres cles viennent de la racine du fichier i18n
 * - Pas de niveau intermediaire "default" : simple et previsible
 */
export const useEventPackI18n = (packName: MaybeRef<EventPackName>) => {
  const { tm, te } = useI18n()

  const variantSeeds = useState<Record<string, number>>(
    'event-pack-variant-seeds',
    () => ({})
  )

  const buildPackKey = (path: string, pack: EventPackName) =>
    `${EVENT_PACK_I18N_BASE_KEY}.${pack}.${path}`

  /**
   * Resout une valeur brute avec la chaine de fallback.
   *
   * Ordre de resolution:
   * 1. packs.{pack}.{path} - surcharge evenementielle
   * 2. {path} - valeur racine (defaut)
   * 3. fallbackKeys - cles additionnelles (optionnel)
   */
  const resolveRaw = (path: string | string[], options?: ResolveOptions) => {
    const normalizedPath = toFlatPath(path)
    const pack = toValue(packName)

    // 1. Chercher dans le pack actif
    const packKey = buildPackKey(normalizedPath, pack)
    if (te(packKey)) {
      return tm(packKey)
    }

    // 2. Fallback vers la racine
    if (te(normalizedPath)) {
      return tm(normalizedPath)
    }

    // 3. Fallback keys additionnels (compatibilite)
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
