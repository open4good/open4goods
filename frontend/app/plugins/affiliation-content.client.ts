import { defineNuxtPlugin } from '#imports'
import {
  isBlogOrGuideArticlePath,
  sanitizeAffiliationContentPath,
} from '~~/shared/utils/affiliation-content-path'

/**
 * Remembers the last article (/blog/<slug>, /guides/…) shown in this tab so
 * offer links can attribute the click to it (GOU-1829). Memory only.
 */
export default defineNuxtPlugin({
  name: 'affiliation-content',
  setup(nuxtApp) {
    const contentPath = useAffiliationContentPath()

    const remember = () => {
      const { path } = nuxtApp.$router.currentRoute.value
      if (isBlogOrGuideArticlePath(path)) {
        contentPath.value = sanitizeAffiliationContentPath(path)
      }
    }

    remember()
    nuxtApp.hook('page:finish', remember)
  },
})
