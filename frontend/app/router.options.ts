import type { RouterConfig } from '@nuxt/schema'

const HASH_STATE_PREFIX = '#state-'

export default <RouterConfig>{
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (
      typeof window !== 'undefined' &&
      to.hash?.startsWith(HASH_STATE_PREFIX)
    ) {
      return false
    }

    if (to.hash) {
      return { el: to.hash }
    }

    return { left: 0, top: 0 }
  },
}
