import type { HLJSApi, LanguageFn } from 'highlight.js'

export type HighlightLanguage = 'json' | 'yaml'

const languageLoaders: Record<HighlightLanguage, () => Promise<{ default: LanguageFn }>> = {
  json: () => import('highlight.js/lib/languages/json'),
  yaml: () => import('highlight.js/lib/languages/yaml'),
}

let highlighterPromise: Promise<HLJSApi> | null = null
const registeredLanguages = new Set<HighlightLanguage>()
let cssLoaded = false

export const loadHighlightJs = async (
  languages: HighlightLanguage[] = []
): Promise<HLJSApi | null> => {
  if (!import.meta.client) {
    return null
  }

  if (!highlighterPromise) {
    highlighterPromise = import('highlight.js/lib/core').then(async (module) => {
      const hljs = module.default

      if (!cssLoaded) {
        await import('highlight.js/styles/github-dark.css')
        cssLoaded = true
      }

      return hljs
    })
  }

  const hljs = await highlighterPromise

  await Promise.all(
    languages.map(async (language) => {
      if (registeredLanguages.has(language)) {
        return
      }

      if (!hljs.getLanguage(language)) {
        const { default: loader } = await languageLoaders[language]()
        hljs.registerLanguage(language, loader)
      }

      registeredLanguages.add(language)
    })
  )

  return hljs
}
