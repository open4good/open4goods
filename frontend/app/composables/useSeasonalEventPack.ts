import { computed } from 'vue'

import {
  resolveActiveEventPack,
  resolveEventPackName,
} from '~~/config/theme/seasons'

export const useSeasonalEventPack = () => {
  const route = useRoute()

  const forcedPack = computed(() =>
    resolveEventPackName(route.query.event ?? route.query.theme)
  )

  return computed(() => forcedPack.value ?? resolveActiveEventPack())
}
