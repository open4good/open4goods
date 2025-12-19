import { computed } from 'vue'

import {
  resolveActiveParallaxPack,
  resolveParallaxPackName,
} from '~~/config/theme/seasons'

export const useSeasonalParallaxPack = () => {
  const route = useRoute()

  const forcedPack = computed(() => resolveParallaxPackName(route.query.theme))

  return computed(() => forcedPack.value ?? resolveActiveParallaxPack())
}
