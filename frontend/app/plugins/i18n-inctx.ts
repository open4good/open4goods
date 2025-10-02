import type { Ref } from 'vue'
import { computed, isRef, watch } from 'vue'
import type { NuxtApp } from '#app'
import { useRuntimeConfig } from '#app'
import { useAuth } from '~/composables/useAuth'
import { buildGithubEditUrl } from '~/lib/i18n-inctx/github-editor'

const MARK_START = '\u0002'
const MARK_END = '\u0003'
const ROOT_CLASS = 'i18n-inctx--editable'
const STYLE_ELEMENT_ID = 'i18n-inctx-overlay-style'
const OVERLAY_CLASS = 'i18n-inctx__overlay'
const EDIT_LINK_CLASS = 'i18n-inctx__edit-link'

interface NuxtI18nComposer {
  locale?: Ref<string>
  getPostTranslationHandler?: () => ((message: string, key: string) => string) | null
  setPostTranslationHandler?: (handler: (message: string, key: string) => string) => void
}

interface OverlayEntry {
  key: string
  textNode: Text
  overlay: HTMLDivElement
  link: HTMLAnchorElement
}

const SKIP_TAGS = new Set(['SCRIPT', 'STYLE', 'NOSCRIPT', 'TEMPLATE'])

function withMarker(value: string, key: string): string {
  if (!key || typeof value !== 'string') {
    return value
  }

  if (value.startsWith(MARK_START)) {
    return value
  }

  return `${MARK_START}${key}${MARK_END}${value}`
}

function extractMarker(value: string | null): { key: string; content: string } | null {
  if (!value || !value.startsWith(MARK_START)) {
    return null
  }

  const endIndex = value.indexOf(MARK_END, MARK_START.length)
  if (endIndex === -1) {
    return null
  }

  const key = value.slice(MARK_START.length, endIndex)
  const content = value.slice(endIndex + MARK_END.length)

  return { key, content }
}

function ensureStyles() {
  if (typeof document === 'undefined') {
    return
  }

  if (document.getElementById(STYLE_ELEMENT_ID)) {
    return
  }

  const style = document.createElement('style')
  style.id = STYLE_ELEMENT_ID
  style.textContent = `
.${OVERLAY_CLASS} {
  position: absolute;
  box-sizing: border-box;
  border: 1px dashed rgba(59, 130, 246, 0.8);
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.08);
  pointer-events: none;
  z-index: 2147483644;
  display: none;
}

.${EDIT_LINK_CLASS} {
  position: absolute;
  z-index: 2147483645;
  display: none;
  font-size: 0.75rem;
  font-weight: 500;
  font-family: inherit;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(59, 130, 246, 0.4);
  border-radius: 4px;
  padding: 0.15rem 0.4rem;
  text-decoration: none;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.18);
}

.${EDIT_LINK_CLASS}:hover,
.${EDIT_LINK_CLASS}:focus-visible {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.65);
}

.${EDIT_LINK_CLASS}:focus-visible {
  outline: 2px solid rgba(59, 130, 246, 0.65);
  outline-offset: 2px;
}

.${ROOT_CLASS} .${OVERLAY_CLASS} {
  display: block;
}

.${ROOT_CLASS} .${EDIT_LINK_CLASS} {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
`
  document.head.appendChild(style)
}

function isEligibleTextNode(node: Text): boolean {
  const parent = node.parentElement
  if (!parent) {
    return false
  }

  if (SKIP_TAGS.has(parent.tagName)) {
    return false
  }

  return true
}

function traverseTextNodes(root: Node, visitor: (node: Text) => void) {
  if (root.nodeType === Node.TEXT_NODE) {
    visitor(root as Text)
    return
  }

  if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) {
    return
  }

  const element = root as Element
  if (element.classList.contains(OVERLAY_CLASS) || element.classList.contains(EDIT_LINK_CLASS)) {
    return
  }

  element.childNodes.forEach(node => traverseTextNodes(node, visitor))
}

function createOverlayEntry(key: string, textNode: Text): OverlayEntry {
  const overlay = document.createElement('div')
  overlay.className = OVERLAY_CLASS
  overlay.dataset.i18nKey = key

  const link = document.createElement('a')
  link.className = EDIT_LINK_CLASS
  link.textContent = 'Edit'
  link.target = '_blank'
  link.rel = 'noopener'
  link.setAttribute('aria-label', `Edit translation ${key}`)
  link.dataset.i18nKey = key

  document.body.appendChild(overlay)
  document.body.appendChild(link)

  return {
    key,
    textNode,
    overlay,
    link,
  }
}

function removeOverlayEntry(entry: OverlayEntry) {
  entry.overlay.remove()
  entry.link.remove()
}

function updateOverlayPosition(entry: OverlayEntry) {
  const range = document.createRange()
  try {
    range.selectNodeContents(entry.textNode)
  } catch {
    entry.overlay.style.display = 'none'
    entry.link.style.display = 'none'
    return
  }

  const rect = range.getBoundingClientRect()
  if (!rect || rect.width === 0 || rect.height === 0) {
    entry.overlay.style.display = 'none'
    entry.link.style.display = 'none'
    return
  }

  const scrollX = window.scrollX || document.documentElement.scrollLeft || 0
  const scrollY = window.scrollY || document.documentElement.scrollTop || 0

  entry.overlay.style.display = 'block'
  entry.overlay.style.left = `${rect.left + scrollX}px`
  entry.overlay.style.top = `${rect.top + scrollY}px`
  entry.overlay.style.width = `${rect.width}px`
  entry.overlay.style.height = `${rect.height}px`

  const linkWidth = entry.link.offsetWidth || 0
  const linkHeight = entry.link.offsetHeight || 0
  let linkTop = rect.top + scrollY - linkHeight - 6
  if (linkTop < scrollY) {
    linkTop = rect.top + scrollY + rect.height + 6
  }
  const linkLeft = rect.left + scrollX + rect.width - linkWidth

  entry.link.style.display = 'inline-flex'
  entry.link.style.top = `${linkTop}px`
  entry.link.style.left = `${Math.max(scrollX, linkLeft)}px`
}

function createOverlayManager(nuxtApp: NuxtApp) {
  const composer = nuxtApp.$i18n as NuxtI18nComposer | undefined
  const localeRef = composer && isRef(composer.locale) ? composer.locale : null

  const overlays = new Map<Text, OverlayEntry>()
  const scheduled = new Set<Text>()
  let enabled = false
  let observer: MutationObserver | null = null
  let rafHandle: number | null = null
  let resizeListenerAttached = false
  const viewportHandler = () => {
    overlays.forEach(entry => {
      scheduleUpdate(entry.textNode)
    })
  }

  function getLocale(): string {
    if (localeRef && localeRef.value) {
      return localeRef.value
    }

    if (composer?.locale && !isRef(composer.locale) && typeof composer.locale === 'string') {
      return composer.locale
    }

    return 'en-US'
  }

  function scheduleUpdate(node: Text) {
    if (!enabled) {
      return
    }

    scheduled.add(node)

    if (rafHandle != null) {
      return
    }

    rafHandle = window.requestAnimationFrame(() => {
      rafHandle = null
      scheduled.forEach(textNode => {
        const entry = overlays.get(textNode)
        if (entry) {
          updateOverlayPosition(entry)
        }
      })
      scheduled.clear()
    })
  }

  function updateEntryMetadata(entry: OverlayEntry) {
    const locale = getLocale()
    entry.overlay.dataset.i18nKey = entry.key
    entry.overlay.dataset.i18nLocale = locale
    entry.link.dataset.i18nKey = entry.key
    entry.link.dataset.i18nLocale = locale
    entry.link.href = buildGithubEditUrl(locale, entry.key)
  }

  function deactivateAll() {
    overlays.forEach(entry => {
      removeOverlayEntry(entry)
    })
    overlays.clear()
    scheduled.clear()
    if (rafHandle != null) {
      window.cancelAnimationFrame(rafHandle)
      rafHandle = null
    }
  }

  function processTextNode(node: Text) {
    if (!isEligibleTextNode(node)) {
      return
    }

    const marker = extractMarker(node.nodeValue ?? '')

    if (!marker) {
      const existing = overlays.get(node)
      if (existing) {
        removeOverlayEntry(existing)
        overlays.delete(node)
      }
      return
    }

    if (node.nodeValue !== marker.content) {
      node.nodeValue = marker.content
    }

    const existing = overlays.get(node)

    if (!enabled) {
      if (existing) {
        removeOverlayEntry(existing)
        overlays.delete(node)
      }
      return
    }

    const entry = existing ?? createOverlayEntry(marker.key, node)
    entry.key = marker.key
    overlays.set(node, entry)
    updateEntryMetadata(entry)
    scheduleUpdate(node)
  }

  function processAttribute(target: Element, attributeName: string | null) {
    if (!attributeName) {
      return
    }

    const rawValue = target.getAttribute(attributeName)
    if (!rawValue) {
      return
    }

    const marker = extractMarker(rawValue)
    if (!marker) {
      return
    }

    target.setAttribute(attributeName, marker.content)
  }

  function handleMutations(mutations: MutationRecord[]) {
    for (const mutation of mutations) {
      if (mutation.type === 'characterData') {
        const node = mutation.target as Text
        processTextNode(node)
        continue
      }

      if (mutation.type === 'attributes') {
        processAttribute(mutation.target as Element, mutation.attributeName)
        continue
      }

      mutation.addedNodes.forEach(node => {
        traverseTextNodes(node, textNode => {
          processTextNode(textNode)
        })
      })

      mutation.removedNodes.forEach(node => {
        traverseTextNodes(node, textNode => {
          const entry = overlays.get(textNode)
          if (entry) {
            removeOverlayEntry(entry)
            overlays.delete(textNode)
          }
        })
      })
    }
  }

  function ensureObserver() {
    if (observer || !document.body) {
      return
    }

    observer = new MutationObserver(handleMutations)
    observer.observe(document.body, {
      subtree: true,
      childList: true,
      characterData: true,
      attributes: true,
      attributeOldValue: false,
    })
  }

  function attachViewportListeners() {
    if (resizeListenerAttached) {
      return
    }

    window.addEventListener('resize', viewportHandler, { passive: true })
    window.addEventListener('scroll', viewportHandler, { passive: true, capture: true })
    resizeListenerAttached = true
  }

  function detachViewportListeners() {
    if (!resizeListenerAttached) {
      return
    }

    window.removeEventListener('resize', viewportHandler)
    window.removeEventListener('scroll', viewportHandler, true)
    resizeListenerAttached = false
  }

  function scan() {
    if (!document.body) {
      return
    }

    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
    const nodes: Text[] = []
    while (walker.nextNode()) {
      const current = walker.currentNode as Text
      nodes.push(current)
    }

    nodes.forEach(node => processTextNode(node))
  }

  function toggle(state: boolean) {
    if (enabled === state) {
      return
    }

    enabled = state

    if (enabled) {
      ensureStyles()
      document.documentElement.classList.add(ROOT_CLASS)
      ensureObserver()
      attachViewportListeners()
      scan()
    } else {
      document.documentElement.classList.remove(ROOT_CLASS)
      detachViewportListeners()
      deactivateAll()
    }
  }

  ensureObserver()
  scan()

  if (localeRef) {
    watch(localeRef, () => {
      overlays.forEach(entry => {
        updateEntryMetadata(entry)
        scheduleUpdate(entry.textNode)
      })
    })
  }

  return {
    toggle,
  }
}

export default defineNuxtPlugin((nuxtApp) => {
  const composer = nuxtApp.$i18n as NuxtI18nComposer | undefined

  const existingPostTranslation = composer?.getPostTranslationHandler?.()
  composer?.setPostTranslationHandler?.((message: string, key: string) => {
    const base = existingPostTranslation ? existingPostTranslation(message, key) : message
    if (typeof base !== 'string') {
      return base
    }
    return withMarker(base, key)
  })

  if (import.meta.server) {
    return
  }

  const overlayManager = createOverlayManager(nuxtApp)

  nuxtApp.hook('app:mounted', () => {
    nuxtApp.runWithContext(() => {
      const { isLoggedIn, hasRole } = useAuth()
      const config = useRuntimeConfig()
      const rawRoles = (config.public.editRoles as string[]) || []
      const roles = computed(() => rawRoles)
      const canEdit = computed(() => isLoggedIn.value && roles.value.some(role => hasRole(role)))

      watch(canEdit, (value) => {
        overlayManager.toggle(value)
      }, { immediate: true })
    })
  })
})

