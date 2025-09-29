import type { EditorCleanup, InContextEditDetail } from './types'

export const EDIT_EVENT_NAME = 'i18n-inctx:edit'

const DATA_KEY_ATTRIBUTE = 'data-i18n-key'
const DATA_LOCALE_ATTRIBUTE = 'data-i18n-locale'
const DEFAULT_HIGHLIGHT_CLASS = 'i18n-inctx__highlight'
const HIGHLIGHT_STYLE_ID = 'i18n-inctx-style'

interface CreateEventHandlersOptions {
  key: string
  element: HTMLElement
  locale?: string
  highlightClass: string
}

interface EventHandlers {
  pointerenter: (event: PointerEvent) => void
  pointerleave: (event: PointerEvent) => void
  click: (event: MouseEvent) => void
  cleanup: () => void
}

const highlightDisposers: Array<() => void> = []
let highlightStyleElement: HTMLStyleElement | null = null
let editListener: ((event: CustomEvent<InContextEditDetail>) => void) | null = null
let editorCleanup: EditorCleanup
let editorModuleLoaded = false
const attachedElements = new WeakSet<HTMLElement>()

const isClient = typeof window !== 'undefined' && typeof document !== 'undefined'

function ensureHighlightStyle(className: string) {
  if (!isClient) {
    return
  }

  const styleId = `${HIGHLIGHT_STYLE_ID}-${className}`
  const existing = document.getElementById(styleId) as HTMLStyleElement | null

  if (existing) {
    highlightStyleElement = existing
    return
  }

  if (highlightStyleElement) {
    highlightStyleElement.remove()
    highlightStyleElement = null
  }

  const style = document.createElement('style')
  style.id = styleId
  style.textContent = `.${className} { outline: 2px dashed rgba(59, 130, 246, 0.85); outline-offset: 2px; cursor: pointer; }`
  document.head.appendChild(style)
  highlightStyleElement = style
}

function defaultResolveLocale(element: HTMLElement): string | undefined {
  const explicitLocale = element.getAttribute(DATA_LOCALE_ATTRIBUTE) ?? element.dataset.i18nLocale
  if (explicitLocale) {
    return explicitLocale
  }

  if (isClient) {
    const lang = document.documentElement?.getAttribute('lang')
    if (lang) {
      return lang
    }
  }

  return undefined
}

function dispatchEditEvent(detail: InContextEditDetail) {
  const { element } = detail
  const editEvent = new CustomEvent<InContextEditDetail>(EDIT_EVENT_NAME, {
    bubbles: true,
    composed: true,
    detail,
  })

  element.dispatchEvent(editEvent)
}

export function createEventHandlers({ key, element, locale, highlightClass }: CreateEventHandlersOptions): EventHandlers {
  const applyHighlight = () => element.classList.add(highlightClass)
  const clearHighlight = () => element.classList.remove(highlightClass)

  const pointerenter = () => {
    applyHighlight()
  }

  const pointerleave = () => {
    clearHighlight()
  }

  const click = (event: MouseEvent) => {
    if (!event.altKey) {
      return
    }

    event.preventDefault()
    event.stopPropagation()

    dispatchEditEvent({
      key,
      element,
      locale,
    })
  }

  const cleanup = () => {
    clearHighlight()
  }

  return { pointerenter, pointerleave, click, cleanup }
}

interface InstallOptions {
  root?: ParentNode
  highlightClass?: string
  filter?: (element: HTMLElement, key: string) => boolean
  resolveLocale?: (element: HTMLElement) => string | undefined
}

function ensureEditListener() {
  if (!isClient || editListener) {
    return
  }

  editListener = async (rawEvent: CustomEvent<InContextEditDetail>) => {
    const { detail } = rawEvent
    if (!detail) {
      return
    }

    try {
      const editor = await import('./editor')
      editorModuleLoaded = true

      if (editorCleanup) {
        await Promise.resolve(editorCleanup())
        editorCleanup = undefined
      }

      editorCleanup = await editor.openEditor(detail)
    } catch (error) {
      console.error('[i18n-inctx]', 'Unable to launch translation editor', error)
    }
  }

  document.addEventListener(EDIT_EVENT_NAME, editListener as EventListener)
}

function cleanupEditListener() {
  if (!isClient || !editListener) {
    return
  }

  document.removeEventListener(EDIT_EVENT_NAME, editListener as EventListener)
  editListener = null
}

function cleanupHighlights() {
  while (highlightDisposers.length > 0) {
    const dispose = highlightDisposers.pop()
    try {
      dispose?.()
    } catch (error) {
      console.error('[i18n-inctx]', 'Failed to dispose highlight listener', error)
    }
  }
}

function cleanupStyle() {
  if (!isClient || !highlightStyleElement) {
    return
  }

  highlightStyleElement.remove()
  highlightStyleElement = null
}

export function install({
  root = isClient ? document : undefined,
  highlightClass = DEFAULT_HIGHLIGHT_CLASS,
  filter,
  resolveLocale = defaultResolveLocale,
}: InstallOptions = {}) {
  if (!isClient || !root) {
    return
  }

  ensureHighlightStyle(highlightClass)
  ensureEditListener()

  const scope = root instanceof Document ? root.body ?? document.body : root
  if (!scope) {
    return
  }

  const nodes = scope.querySelectorAll<HTMLElement>(`[${DATA_KEY_ATTRIBUTE}]`)
  nodes.forEach((element) => {
    if (attachedElements.has(element)) {
      return
    }

    const key = element.getAttribute(DATA_KEY_ATTRIBUTE) ?? element.dataset.i18nKey
    if (!key) {
      return
    }

    if (filter && !filter(element, key)) {
      return
    }

    const locale = resolveLocale(element)
    const handlers = createEventHandlers({ key, element, locale, highlightClass })

    element.addEventListener('pointerenter', handlers.pointerenter)
    element.addEventListener('pointerleave', handlers.pointerleave)
    element.addEventListener('click', handlers.click)

    highlightDisposers.push(() => {
      element.removeEventListener('pointerenter', handlers.pointerenter)
      element.removeEventListener('pointerleave', handlers.pointerleave)
      element.removeEventListener('click', handlers.click)
      handlers.cleanup()
      attachedElements.delete(element)
    })

    attachedElements.add(element)
  })
}

export function teardown() {
  if (!isClient) {
    return
  }

  cleanupHighlights()
  cleanupEditListener()
  cleanupStyle()
  attachedElements.clear()

  if (editorCleanup) {
    const cleanup = editorCleanup
    editorCleanup = undefined
    void Promise.resolve(cleanup())
  }

  if (editorModuleLoaded) {
    editorModuleLoaded = false
    void import('./editor')
      .then((module) => module.shutdownEditor())
      .catch((error) => {
        console.error('[i18n-inctx]', 'Failed to shutdown editor', error)
      })
  }
}
