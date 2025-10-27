import type { RouterConfig } from '@nuxt/schema'

const isCategoryStateHash = (hash: string): boolean => hash.startsWith('#state-')

export default <RouterConfig>{
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    const { hash } = to

    if (hash) {
      if (isCategoryStateHash(hash)) {
        return false
      }

      if (typeof document !== 'undefined') {
        try {
          const target = document.querySelector<HTMLElement>(hash)
          if (target) {
            return { el: target, behavior: 'smooth' }
          }
        } catch (error) {
          if (import.meta.dev) {
            console.warn('Invalid hash selector encountered during navigation.', error)
          }
        }
      }
    }

    return { left: 0, top: 0 }
  },
}

