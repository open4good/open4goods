import { useNuxtApp } from '#app'

import { useI18nInContextEditorState } from '~/composables/i18n-inctx/useI18nInContextEditorState'

import type { InContextEditDetail } from './types'

const REPO_OWNER = 'open4good'
const REPO_NAME = 'open4goods'
const DEFAULT_BRANCH = 'main'
const LOCALES_DIRECTORY = 'frontend/i18n/locales'

function resolveLocale(detail: InContextEditDetail): string {
  if (detail.locale) {
    return detail.locale
  }

  if (typeof document !== 'undefined') {
    const lang = document.documentElement?.getAttribute('lang')
    if (lang) {
      return lang
    }
  }

  return 'en-US'
}

function buildEditUrl(locale: string, key: string): string {
  const normalizedLocale = locale.trim().replace(/\s+/g, '')
  const fileName = normalizedLocale.endsWith('.json') ? normalizedLocale : `${normalizedLocale}.json`
  const encodedFile = encodeURIComponent(fileName)
  const base = `https://github.com/${REPO_OWNER}/${REPO_NAME}/edit/${DEFAULT_BRANCH}/${LOCALES_DIRECTORY}/${encodedFile}`
  const anchor = `i18n-key=${encodeURIComponent(key)}`
  return `${base}#${anchor}`
}

export async function copyTranslationKey(key: string): Promise<boolean> {
  if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) {
    return false
  }

  try {
    await navigator.clipboard.writeText(key)
    return true
  } catch (error) {
    console.warn('[i18n-inctx]', 'Failed to copy translation key to clipboard', error)
    return false
  }
}

function resolveTranslation(detail: InContextEditDetail, locale: string): string {
  try {
    const nuxtApp = useNuxtApp()
    const composer = nuxtApp.$i18n as { t: (key: string) => string; getLocaleMessage?: (locale: string) => Record<string, unknown>; locale?: { value: string } } | undefined

    if (composer?.getLocaleMessage && typeof composer.getLocaleMessage === 'function' && locale) {
      const messages = composer.getLocaleMessage(locale)
      const segments = detail.key.split('.')
      let current: unknown = messages

      for (const segment of segments) {
        if (current && typeof current === 'object' && segment in (current as Record<string, unknown>)) {
          current = (current as Record<string, unknown>)[segment]
        } else {
          current = undefined
          break
        }
      }

      if (typeof current === 'string') {
        return current
      }
    }

    if (composer?.t) {
      return String(composer.t(detail.key))
    }
  } catch (error) {
    console.warn('[i18n-inctx]', 'Unable to resolve translation value', error)
  }

  return detail.element.textContent?.trim() ?? ''
}

export async function openEditor(detail: InContextEditDetail) {
  if (typeof window === 'undefined') {
    return undefined
  }

  const locale = resolveLocale(detail)
  const url = buildEditUrl(locale, detail.key)
  const translation = resolveTranslation(detail, locale)

  const { open, close, setCopyStatus } = useI18nInContextEditorState()

  open({
    key: detail.key,
    locale,
    translation,
    githubUrl: url,
    element: detail.element,
  })

  const copied = await copyTranslationKey(detail.key)
  setCopyStatus(copied ? 'copied' : 'error')

  return {
    close() {
      close()
    },
  }
}

export async function shutdownEditor() {
  const { close, reset } = useI18nInContextEditorState()
  close()
  reset()
}
