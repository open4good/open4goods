import type { NuxtApp } from '#app'
import type { ComponentInternalInstance } from 'vue'
import type { Composer } from 'vue-i18n'

interface TranslationContext {
  instance: ComponentInternalInstance | null
}

interface HighlightState {
  element: Element | null
}

const HIGHLIGHT_CLASS = 'i18n-inctx__highlight'
const DATA_KEY = 'data-i18n-key'
const DATA_KEY_LABEL = 'data-i18n-key-label'

const createStyleElement = (): HTMLStyleElement => {
  const style = document.createElement('style')
  style.id = 'i18n-inctx-style'
  style.textContent = `
[${DATA_KEY}] {
  cursor: inherit;
}
[${DATA_KEY}].${HIGHLIGHT_CLASS} {
  outline: 2px dashed #1976D2;
  outline-offset: 2px;
  position: relative !important;
  cursor: pointer;
}
[${DATA_KEY}].${HIGHLIGHT_CLASS}::after {
  content: attr(${DATA_KEY_LABEL});
  position: absolute;
  top: -1.75rem;
  left: 0;
  background: rgba(25, 118, 210, 0.92);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.3);
  white-space: nowrap;
  pointer-events: none;
  z-index: 2147483647;
}
`
  return style
}

const resolveElement = (instance: ComponentInternalInstance | null | undefined): Element | null => {
  if (!instance) {
    return null
  }

  const candidate = (instance.subTree?.el ?? instance.vnode?.el) as Node | null

  if (!candidate) {
    return null
  }

  if (candidate instanceof Text) {
    return candidate.parentElement
  }

  if (candidate instanceof Comment) {
    return null
  }

  if (candidate instanceof Element) {
    return candidate
  }

  return null
}

const annotateTranslation = (context: TranslationContext | undefined, key: string, value: string) => {
  if (!context) {
    return
  }

  const element = resolveElement(context.instance)

  if (!element) {
    return
  }

  element.setAttribute(DATA_KEY, key)
  element.setAttribute(DATA_KEY_LABEL, key)
  element.dispatchEvent(
    new CustomEvent('i18n-inctx:translated', {
      detail: { key, value, element },
      bubbles: true,
    })
  )
}

const clearDocumentAnnotations = () => {
  document.querySelectorAll(`[${DATA_KEY}]`).forEach((element) => {
    element.removeAttribute(DATA_KEY)
    element.removeAttribute(DATA_KEY_LABEL)
    element.classList.remove(HIGHLIGHT_CLASS)
  })
}

const createEventHandlers = (highlightState: HighlightState) => {
  let altPressed = false

  const removeHighlight = () => {
    if (!highlightState.element) {
      return
    }

    highlightState.element.classList.remove(HIGHLIGHT_CLASS)
    highlightState.element = null
  }

  const highlight = (element: Element | null) => {
    if (!element) {
      removeHighlight()
      return
    }

    if (highlightState.element === element) {
      return
    }

    removeHighlight()

    highlightState.element = element
    element.classList.add(HIGHLIGHT_CLASS)
  }

  const handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Alt') {
      altPressed = true
    }
  }

  const handleKeyUp = (event: KeyboardEvent) => {
    if (event.key === 'Alt') {
      altPressed = false
      removeHighlight()
    }
  }

  const handlePointerMove = (event: PointerEvent) => {
    if (!altPressed) {
      removeHighlight()
      return
    }

    const target = (event.target as Element | null)?.closest(`[${DATA_KEY}]`)
    highlight(target)
  }

  const handleClick = (event: MouseEvent) => {
    if (!altPressed) {
      return
    }

    const target = (event.target as Element | null)?.closest(`[${DATA_KEY}]`)

    if (!target) {
      return
    }

    event.preventDefault()
    event.stopPropagation()

    const key = target.getAttribute(DATA_KEY)
    if (key) {
      console.info(`[i18n-inctx] Edit key: ${key}`)
    }
  }

  return {
    removeHighlight,
    handleKeyDown,
    handleKeyUp,
    handlePointerMove,
    handleClick,
  }
}

export interface I18nInContextOverlay {
  teardown: () => void
}

export const install = (nuxtApp: NuxtApp): I18nInContextOverlay => {
  const i18nInstance = nuxtApp.$i18n as { global?: Composer } | Composer | undefined

  if (!i18nInstance) {
    console.warn('[i18n-inctx] vue-i18n instance not found on Nuxt app. Overlay will not be installed.')
    return {
      teardown: () => {},
    }
  }

  const composer: Composer = 'global' in (i18nInstance as { global?: Composer }) && (i18nInstance as { global?: Composer }).global
    ? (i18nInstance as { global: Composer }).global
    : (i18nInstance as Composer)

  const highlightState: HighlightState = { element: null }
  const handlers = createEventHandlers(highlightState)
  const styleElement = createStyleElement()
  document.head.appendChild(styleElement)

  window.addEventListener('keydown', handlers.handleKeyDown)
  window.addEventListener('keyup', handlers.handleKeyUp)
  window.addEventListener('pointermove', handlers.handlePointerMove)
  window.addEventListener('click', handlers.handleClick, true)

  const originalPostTranslation = composer.postTranslation

  composer.postTranslation = (translation, key, context) => {
    const resolved = originalPostTranslation ? originalPostTranslation(translation, key, context) : translation

    if (typeof window !== 'undefined') {
      queueMicrotask(() => annotateTranslation(context as TranslationContext | undefined, key, resolved))
    }

    return resolved
  }

  return {
    teardown: () => {
      composer.postTranslation = originalPostTranslation ?? null

      handlers.removeHighlight()
      window.removeEventListener('keydown', handlers.handleKeyDown)
      window.removeEventListener('keyup', handlers.handleKeyUp)
      window.removeEventListener('pointermove', handlers.handlePointerMove)
      window.removeEventListener('click', handlers.handleClick, true)

      if (styleElement.parentNode) {
        styleElement.parentNode.removeChild(styleElement)
      }

      clearDocumentAnnotations()
    },
  }
}
