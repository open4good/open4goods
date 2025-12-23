import { computed } from 'vue'

import {
  resolveActiveEventPack,
  resolveEventPackName,
} from '~~/config/theme/event-packs'

/**
 * Composable pour déterminer le pack événementiel actif.
 *
 * Ordre de priorité :
 * 1. Paramètre URL `?event=xxx` (nouveau)
 * 2. Paramètre URL `?theme=xxx` (legacy, rétrocompatibilité)
 * 3. Résolution automatique basée sur la date courante
 *
 * @example
 * ```ts
 * const activeEventPack = useSeasonalEventPack()
 * // activeEventPack.value === 'bastille-day' (si entre 10-16 juillet)
 * ```
 *
 * @example Test via URL
 * ```
 * https://nudger.fr?event=bastille-day
 * https://nudger.fr?theme=sdg (legacy)
 * ```
 */
export const useSeasonalEventPack = () => {
  const route = useRoute()

  const forcedPack = computed(() =>
    resolveEventPackName(route.query.event ?? route.query.theme)
  )

  return computed(() => forcedPack.value ?? resolveActiveEventPack())
}
