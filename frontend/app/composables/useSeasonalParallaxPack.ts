import { computed } from 'vue'

import { resolveActiveParallaxPack } from '~~/config/theme/seasons'

export const useSeasonalParallaxPack = () =>
  computed(() => resolveActiveParallaxPack())
