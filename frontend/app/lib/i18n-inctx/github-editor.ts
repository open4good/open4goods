import type { InContextEditDetail } from './types'

const REPO_OWNER = 'open4good'
const REPO_NAME = 'open4goods'
const DEFAULT_BRANCH = 'main'
const LOCALES_DIRECTORY = 'frontend/i18n/locales'

let activeEditorWindow: Window | null = null

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

export function buildGithubEditUrl(locale: string, key: string): string {
  const normalizedLocale = locale.trim().replace(/\s+/g, '')
  const fileName = normalizedLocale.endsWith('.json') ? normalizedLocale : `${normalizedLocale}.json`
  const encodedFile = encodeURIComponent(fileName)
  const base = `https://github.com/${REPO_OWNER}/${REPO_NAME}/edit/${DEFAULT_BRANCH}/${LOCALES_DIRECTORY}/${encodedFile}`
  const anchor = `i18n-key=${encodeURIComponent(key)}`
  return `${base}#${anchor}`
}

async function focusEditorWindow(url: string) {
  if (!activeEditorWindow || activeEditorWindow.closed) {
    activeEditorWindow = window.open(url, '_blank', 'noopener')
    return
  }

  try {
    activeEditorWindow.focus()
    activeEditorWindow.location.href = url
  } catch (error) {
    console.warn('[i18n-inctx]', 'Unable to reuse existing editor window', error)
    activeEditorWindow = window.open(url, '_blank', 'noopener')
  }
}

async function copyKeyToClipboard(key: string) {
  if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) {
    return
  }

  try {
    await navigator.clipboard.writeText(key)
  } catch (error) {
    console.warn('[i18n-inctx]', 'Failed to copy translation key to clipboard', error)
  }
}

export async function openEditor(detail: InContextEditDetail) {
  if (typeof window === 'undefined') {
    return undefined
  }

  const locale = resolveLocale(detail)
  const url = buildGithubEditUrl(locale, detail.key)

  await Promise.allSettled([copyKeyToClipboard(detail.key), focusEditorWindow(url)])

  return {
    close() {
      if (activeEditorWindow && !activeEditorWindow.closed) {
        activeEditorWindow.close()
      }

      activeEditorWindow = null
    },
  }
}

export async function shutdownEditor() {
  if (activeEditorWindow && !activeEditorWindow.closed) {
    activeEditorWindow.close()
  }

  activeEditorWindow = null
}
