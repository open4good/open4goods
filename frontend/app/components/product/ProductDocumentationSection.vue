<template>
  <section :id="sectionId" class="product-docs">
    <header class="product-docs__header">
      <h2 class="product-docs__title">{{ $t('product.docs.title') }}</h2>
      <p class="product-docs__subtitle">{{ $t('product.docs.subtitle') }}</p>
    </header>

    <div v-if="hasPdfs" class="product-docs__layout">
      <div class="product-docs__navigation" :aria-label="$t('product.docs.navigationAria')" role="region">
        <div class="product-docs__navigation-header">
          <h3 class="product-docs__navigation-title">{{ $t('product.docs.sidebarTitle') }}</h3>
          <div class="product-docs__navigation-controls">
            <button
              type="button"
              class="product-docs__nav-control"
              :class="{ 'product-docs__nav-control--disabled': !canScrollLeft }"
              :disabled="!canScrollLeft"
              :aria-label="$t('product.docs.toolbar.scrollPrevious')"
              data-testid="product-docs-nav-prev"
              @click="scrollNavigation('previous')"
            >
              ‹
            </button>
            <div class="product-docs__tabs-viewport">
              <div
                ref="navigationListRef"
                class="product-docs__tabs"
                role="tablist"
                :aria-activedescendant="activeTabId ?? undefined"
                @scroll="updateNavigationScrollState"
              >
                <button
                  v-for="(pdf, index) in pdfs"
                  :id="tabId(index)"
                  :key="pdf.cacheKey ?? index"
                  type="button"
                  role="tab"
                  class="product-docs__tab"
                  :class="{ 'product-docs__tab--active': index === activePdfIndex }"
                  :aria-selected="index === activePdfIndex"
                  :aria-controls="panelId"
                  data-testid="product-docs-tab"
                  @click="selectPdf(index)"
                  @keydown="onTabKeydown(index, $event)"
                >
                  <span class="product-docs__tab-title">{{ pdfTitle(pdf) }}</span>
                  <span v-if="formatMeta(pdf)" class="product-docs__tab-meta">
                    {{ formatMeta(pdf) }}
                  </span>
                  <span v-if="formatLanguage(pdf)" class="product-docs__tab-language">
                    {{ formatLanguage(pdf) }}
                  </span>
                </button>
              </div>
            </div>
            <button
              type="button"
              class="product-docs__nav-control"
              :class="{ 'product-docs__nav-control--disabled': !canScrollRight }"
              :disabled="!canScrollRight"
              :aria-label="$t('product.docs.toolbar.scrollNext')"
              data-testid="product-docs-nav-next"
              @click="scrollNavigation('next')"
            >
              ›
            </button>
          </div>
        </div>
      </div>

      <div
        :id="panelId"
        class="product-docs__viewer-pane"
        role="tabpanel"
        :aria-labelledby="activeTabId ?? undefined"
        data-testid="product-docs-viewer"
      >
        <header class="product-docs__viewer-header">
          <div class="product-docs__viewer-header-texts">
            <h3 class="product-docs__viewer-title">{{ activePdfTitle }}</h3>
            <p v-if="activePdfMeta" class="product-docs__viewer-meta">
              {{ activePdfMeta }}
            </p>
            <p v-if="activePdfLanguage" class="product-docs__viewer-language">
              {{ activePdfLanguage }}
            </p>
          </div>
          <div class="product-docs__viewer-actions">
            <button
              v-if="activePdf?.url"
              type="button"
              class="product-docs__viewer-action"
              data-testid="product-docs-print"
              @click="printDocument"
            >
              {{ $t('product.docs.toolbar.print') }}
            </button>
            <a
              v-if="activePdf?.url"
              :href="activePdf.url"
              target="_blank"
              rel="noopener"
              class="product-docs__download"
            >
              {{ $t('product.docs.download') }}
            </a>
          </div>
        </header>

        <div class="product-docs__viewer-toolbar" role="toolbar" :aria-label="$t('product.docs.toolbar.ariaLabel')">
          <div class="product-docs__toolbar-group">
            <button
              type="button"
              class="product-docs__toolbar-button"
              :disabled="zoomMode === 'manual' && zoomLevel <= MIN_ZOOM"
              data-testid="product-docs-zoom-out"
              @click="decreaseZoom"
            >
              {{ $t('product.docs.toolbar.zoomOut') }}
            </button>
            <button
              type="button"
              class="product-docs__toolbar-button"
              :class="{ 'product-docs__toolbar-button--active': zoomMode === 'auto' }"
              data-testid="product-docs-zoom-auto"
              @click="setAutoZoom"
            >
              {{ $t('product.docs.toolbar.fitToWidth') }}
            </button>
            <button
              type="button"
              class="product-docs__toolbar-button"
              :disabled="zoomMode === 'manual' && zoomLevel >= MAX_ZOOM"
              data-testid="product-docs-zoom-in"
              @click="increaseZoom"
            >
              {{ $t('product.docs.toolbar.zoomIn') }}
            </button>
            <span class="product-docs__zoom-indicator" data-testid="product-docs-zoom-indicator">
              {{ zoomDisplayLabel }}
            </span>
          </div>

          <div v-if="hasMultiplePages" class="product-docs__toolbar-group">
            <label class="product-docs__toolbar-label" :for="pageSelectId">
              {{ $t('product.docs.toolbar.pageLabel') }}
            </label>
            <select
              :id="pageSelectId"
              v-model="selectedPage"
              class="product-docs__toolbar-select"
              data-testid="product-docs-page-select"
            >
              <option :value="null">{{ $t('product.docs.toolbar.allPages') }}</option>
              <option v-for="pageNumber in availablePages" :key="pageNumber" :value="pageNumber">
                {{ $t('product.docs.toolbar.pageOption', { page: pageNumber }) }}
              </option>
            </select>
          </div>

          <div class="product-docs__toolbar-group">
            <label class="product-docs__toolbar-label" :for="rotationSelectId">
              {{ $t('product.docs.toolbar.rotationLabel') }}
            </label>
            <select
              :id="rotationSelectId"
              v-model="rotation"
              class="product-docs__toolbar-select"
              data-testid="product-docs-rotation-select"
            >
              <option v-for="angle in rotationOptions" :key="angle" :value="angle">
                {{ $t('product.docs.toolbar.rotationOption', { angle }) }}
              </option>
            </select>
          </div>

          <div class="product-docs__toolbar-group product-docs__toolbar-group--toggles">
            <label class="product-docs__toolbar-toggle">
              <input
                v-model="showTextLayer"
                type="checkbox"
                class="product-docs__toolbar-checkbox"
                data-testid="product-docs-text-layer"
              />
              <span>{{ $t('product.docs.toolbar.textLayer') }}</span>
            </label>
            <label class="product-docs__toolbar-toggle">
              <input
                v-model="showAnnotationLayer"
                type="checkbox"
                class="product-docs__toolbar-checkbox"
                data-testid="product-docs-annotation-layer"
              />
              <span>{{ $t('product.docs.toolbar.annotationLayer') }}</span>
            </label>
          </div>
        </div>

        <ClientOnly>
          <template #default>
            <div v-if="activePdf?.url" ref="viewerContainerRef" class="product-docs__viewer-frame">
              <VuePdfEmbed
                ref="pdfComponentRef"
                :source="activePdf.url"
                :width="pdfWidth ?? undefined"
                :scale="pdfScale ?? undefined"
                :rotation="rotation"
                :page="selectedPage ?? undefined"
                :text-layer="showTextLayer"
                :annotation-layer="showAnnotationLayer"
                class="product-docs__viewer-canvas"
                @loaded="onDocumentLoaded"
                @loading-failed="onDocumentError"
              />
            </div>
            <p v-else class="product-docs__viewer-empty" data-testid="product-docs-preview-empty">
              {{ $t('product.docs.previewUnavailable') }}
            </p>
            <p v-if="viewerError" class="product-docs__viewer-error" data-testid="product-docs-preview-error">
              {{ viewerError }}
            </p>
          </template>
          <template #fallback>
            <p class="product-docs__viewer-loading">
              {{ $t('product.docs.loading') }}
            </p>
          </template>
        </ClientOnly>
      </div>
    </div>

    <p v-else class="product-docs__empty" data-testid="product-docs-empty">
      {{ $t('product.docs.empty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { useElementSize, useEventListener, useResizeObserver } from '@vueuse/core'
import {
  computed,
  defineAsyncComponent,
  nextTick,
  type ComponentPublicInstance,
  type PropType,
  ref,
  type Ref,
  useId,
  watch,
} from 'vue'
import type { PDFDocumentProxy } from 'pdfjs-dist/types/src/display/api'
import { useI18n } from 'vue-i18n'
import type { ProductPdfDto } from '~~/shared/api-client'

import 'vue-pdf-embed/dist/styles/annotationLayer.css'
import 'vue-pdf-embed/dist/styles/textLayer.css'

type VuePdfEmbedComponent = (typeof import('vue-pdf-embed'))['default']

type VuePdfEmbedInstance = ComponentPublicInstance<{
  download: (filename: string) => Promise<void>
  print: (dpi?: number, filename?: string, allPages?: boolean) => Promise<void>
}>

const MIN_ZOOM = 0.5
const MAX_ZOOM = 2
const ZOOM_STEP = 0.25
const NAV_SCROLL_RATIO = 0.8
const ROTATION_OPTIONS = [0, 90, 180, 270] as const

const VuePdfEmbed = defineAsyncComponent<VuePdfEmbedComponent>(async () => {
  const module = await import('vue-pdf-embed')
  return module.default
})

const props = defineProps({
  sectionId: {
    type: String,
    default: 'documentation',
  },
  pdfs: {
    type: Array as PropType<ProductPdfDto[]>,
    default: () => [],
  },
})

const { locale, n, t } = useI18n()

const pdfs = computed(() => props.pdfs ?? [])

const activePdfIndex = ref(-1)

watch(
  pdfs,
  (nextPdfs) => {
    if (!nextPdfs.length) {
      activePdfIndex.value = -1
      return
    }

    const clampedIndex = Math.min(Math.max(activePdfIndex.value, 0), nextPdfs.length - 1)
    activePdfIndex.value = clampedIndex
  },
  { immediate: true }
)

const activePdf = computed(() => (activePdfIndex.value >= 0 ? pdfs.value[activePdfIndex.value] ?? null : null))

const hasPdfs = computed(() => pdfs.value.length > 0)

const sectionUid = useId()
const panelId = `${sectionUid}-panel`
const tabId = (index: number) => `${sectionUid}-tab-${index}`
const pageSelectId = `${sectionUid}-page-select`
const rotationSelectId = `${sectionUid}-rotation-select`

const activeTabId = computed(() => (activePdfIndex.value >= 0 ? tabId(activePdfIndex.value) : null))

const languageDisplayNames = computed(() => {
  try {
    return new Intl.DisplayNames([locale.value], { type: 'language' })
  } catch {
    return null
  }
})

const navigationListRef = ref<HTMLDivElement | null>(null)
const viewerContainerRef = ref<HTMLElement | null>(null)
const pdfComponentRef: Ref<VuePdfEmbedInstance | null> = ref(null)

const { width: viewerWidth } = useElementSize(viewerContainerRef)

const zoomMode = ref<'auto' | 'manual'>('auto')
const zoomLevel = ref(1)
const rotation = ref<number>(0)
const showTextLayer = ref(true)
const showAnnotationLayer = ref(true)
const selectedPage = ref<number | null>(null)
const totalPages = ref<number | null>(null)
const viewerError = ref<string | null>(null)

const pdfWidth = computed(() => {
  if (zoomMode.value !== 'auto') {
    return null
  }

  const width = Math.floor(viewerWidth.value)
  return width > 0 ? width : null
})

const pdfScale = computed(() => (zoomMode.value === 'manual' ? zoomLevel.value : null))

const availablePages = computed(() => {
  const count = totalPages.value ?? activePdf.value?.numberOfPages ?? 0
  if (!count || count <= 0) {
    return [] as number[]
  }

  return Array.from({ length: count }, (_, index) => index + 1)
})

const hasMultiplePages = computed(() => availablePages.value.length > 1)

const rotationOptions = ROTATION_OPTIONS

const zoomDisplayLabel = computed(() =>
  zoomMode.value === 'auto'
    ? t('product.docs.toolbar.zoomAuto')
    : t('product.docs.toolbar.zoomIndicator', { value: Math.round(zoomLevel.value * 100) })
)

const formatMeta = (pdf: ProductPdfDto) => {
  const parts: string[] = []

  const size = formatBytes(pdf.fileSize)
  if (size) {
    parts.push(size)
  }

  if (pdf.numberOfPages) {
    parts.push(t('product.docs.pageCount', { count: pdf.numberOfPages }))
  }

  const dateLabel = formatDate(pdf)
  if (dateLabel) {
    parts.push(dateLabel)
  }

  if (pdf.author) {
    parts.push(pdf.author)
  }

  return parts.join(' · ')
}

const formatDate = (pdf: ProductPdfDto) => {
  const timestamp = pdf.modificationDate ?? pdf.creationDate ?? pdf.timeStamp
  if (!timestamp) {
    return null
  }

  try {
    return new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }).format(new Date(timestamp))
  } catch {
    return null
  }
}

const formatLanguage = (pdf: ProductPdfDto) => {
  const raw = pdf.language?.trim()
  if (!raw) {
    return null
  }

  const normalized = raw.toLowerCase()

  let displayName: string | null = null

  if (languageDisplayNames.value) {
    try {
      displayName = languageDisplayNames.value.of(normalized) ?? null
    } catch {
      displayName = null
    }
  }

  if (displayName) {
    const upper = normalized.toUpperCase()
    if (displayName.toLowerCase() === normalized) {
      return upper
    }

    return `${displayName} · ${upper}`
  }

  if (/^[a-z]{2,3}(?:-[a-z0-9]{2,8})*$/i.test(raw)) {
    return raw.toUpperCase()
  }

  return raw
}

const formatBytes = (bytes?: number) => {
  if (!Number.isFinite(bytes) || !bytes || bytes <= 0) {
    return null
  }

  const kb = bytes / 1024
  if (kb < 1024) {
    return `${n(kb, { maximumFractionDigits: 1 })} KB`
  }

  const mb = kb / 1024
  return `${n(mb, { maximumFractionDigits: 1 })} MB`
}

const pdfTitle = (pdf: ProductPdfDto) => pdf.extractedTitle ?? pdf.metadataTitle ?? pdf.fileName ?? t('product.docs.untitled')

const activePdfTitle = computed(() => (activePdf.value ? pdfTitle(activePdf.value) : t('product.docs.title')))
const activePdfMeta = computed(() => (activePdf.value ? formatMeta(activePdf.value) : null))
const activePdfLanguage = computed(() => (activePdf.value ? formatLanguage(activePdf.value) : null))

const clampZoom = (value: number) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, Number.isFinite(value) ? value : 1))

const setAutoZoom = () => {
  zoomMode.value = 'auto'
}

const increaseZoom = () => {
  zoomMode.value = 'manual'
  zoomLevel.value = clampZoom(zoomLevel.value + ZOOM_STEP)
}

const decreaseZoom = () => {
  zoomMode.value = 'manual'
  zoomLevel.value = clampZoom(zoomLevel.value - ZOOM_STEP)
}

const selectPdf = (index: number) => {
  if (index < 0 || index >= pdfs.value.length || index === activePdfIndex.value) {
    return
  }

  activePdfIndex.value = index
}

const onTabKeydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    selectPdf(index)
  }
}

const scrollNavigation = (direction: 'previous' | 'next') => {
  const element = navigationListRef.value
  if (!element) {
    return
  }

  const offset = element.clientWidth * NAV_SCROLL_RATIO
  const delta = direction === 'next' ? offset : -offset
  element.scrollBy({ left: delta, behavior: 'smooth' })

  if (typeof window !== 'undefined') {
    window.requestAnimationFrame(() => updateNavigationScrollState())
  }
}

const canScrollLeft = ref(false)
const canScrollRight = ref(false)

const updateNavigationScrollState = () => {
  const element = navigationListRef.value
  if (!element) {
    canScrollLeft.value = false
    canScrollRight.value = false
    return
  }

  const maxScrollLeft = element.scrollWidth - element.clientWidth
  canScrollLeft.value = element.scrollLeft > 0
  canScrollRight.value = element.scrollLeft < maxScrollLeft - 1
}

useResizeObserver(navigationListRef, updateNavigationScrollState)
useEventListener(navigationListRef, 'scroll', updateNavigationScrollState)

watch([pdfs, activePdfIndex], async () => {
  await nextTick()
  updateNavigationScrollState()
})

watch(activePdf, async () => {
  zoomMode.value = 'auto'
  zoomLevel.value = 1
  rotation.value = 0
  showTextLayer.value = true
  showAnnotationLayer.value = true
  selectedPage.value = null
  viewerError.value = null
  totalPages.value = activePdf.value?.numberOfPages ?? null

  await nextTick()
  updateNavigationScrollState()
})

const onDocumentLoaded = (documentProxy: PDFDocumentProxy) => {
  totalPages.value = documentProxy.numPages
  viewerError.value = null
}

const onDocumentError = () => {
  viewerError.value = t('product.docs.loadError')
}

const printDocument = async () => {
  const instance = pdfComponentRef.value
  if (!instance) {
    return
  }

  try {
    await instance.print(undefined, activePdfTitle.value, selectedPage.value === null)
  } catch {
    viewerError.value = t('product.docs.printError')
  }
}
</script>

<style scoped>
.product-docs {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-docs__header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-docs__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
}

.product-docs__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-docs__layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-docs__navigation {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass), 0.82);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-docs__navigation-header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-docs__navigation-title {
  font-size: 0.95rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-docs__navigation-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.product-docs__tabs-viewport {
  flex: 1 1 auto;
  overflow: hidden;
}

.product-docs__tabs {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
  scrollbar-width: thin;
}

.product-docs__tabs::-webkit-scrollbar {
  height: 6px;
}

.product-docs__tabs::-webkit-scrollbar-thumb {
  background: rgba(var(--v-theme-border-primary-strong), 0.6);
  border-radius: 999px;
}

.product-docs__nav-control {
  flex: 0 0 auto;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  background: rgba(var(--v-theme-surface-default), 0.9);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  font-size: 1.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-docs__nav-control:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(15, 23, 42, 0.12);
}

.product-docs__nav-control:disabled,
.product-docs__nav-control--disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.product-docs__tab {
  flex: 0 0 auto;
  min-width: clamp(180px, 28vw, 320px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
  padding: 1rem 1.25rem;
  border-radius: 20px;
  border: 2px solid transparent;
  background: rgba(var(--v-theme-surface-glass), 0.82);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.product-docs__tab:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.12);
}

.product-docs__tab:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.product-docs__tab--active {
  border-color: rgba(var(--v-theme-accent-primary-highlight), 0.9);
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.18);
}

.product-docs__tab-title {
  font-weight: 600;
  font-size: 1.05rem;
}

.product-docs__tab-meta {
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-docs__tab-language {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.95);
}

.product-docs__viewer-pane {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-docs__viewer-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-docs__viewer-header-texts {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-docs__viewer-title {
  font-size: clamp(1.3rem, 2vw, 1.85rem);
  font-weight: 700;
}

.product-docs__viewer-meta {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-docs__viewer-language {
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.95);
}

.product-docs__viewer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.product-docs__viewer-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  border-radius: 999px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
  background: rgba(var(--v-theme-surface-default), 0.92);
  cursor: pointer;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-docs__viewer-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.product-docs__download {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  border-radius: 999px;
  font-weight: 600;
  text-decoration: none;
  color: rgb(var(--v-theme-text-on-accent));
  background: rgb(var(--v-theme-accent-primary-highlight));
  box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-docs__download:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px rgba(var(--v-theme-shadow-primary-600), 0.28);
}

.product-docs__download:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.product-docs__viewer-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem 1rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-default), 0.92);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.product-docs__toolbar-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.product-docs__toolbar-group--toggles {
  gap: 0.75rem;
}

.product-docs__toolbar-button {
  padding: 0.45rem 0.85rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  background: rgba(var(--v-theme-surface-glass), 0.9);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.product-docs__toolbar-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(15, 23, 42, 0.12);
}

.product-docs__toolbar-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.product-docs__toolbar-button--active {
  background: rgba(var(--v-theme-accent-primary-highlight), 0.15);
  border-color: rgba(var(--v-theme-accent-primary-highlight), 0.65);
  color: rgba(var(--v-theme-text-on-accent), 0.9);
}

.product-docs__zoom-indicator {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-weight: 600;
  min-width: 4.5rem;
}

.product-docs__toolbar-label {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-weight: 600;
}

.product-docs__toolbar-select {
  min-width: 120px;
  padding: 0.35rem 0.65rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  background: rgba(var(--v-theme-surface-default), 0.92);
  color: inherit;
  font-size: 0.9rem;
}

.product-docs__toolbar-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-docs__toolbar-checkbox {
  width: 1rem;
  height: 1rem;
  accent-color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-docs__viewer-frame {
  width: 100%;
  min-height: 24rem;
  max-height: clamp(28rem, 70vh, 45rem);
  border-radius: 18px;
  overflow: auto;
  background: rgba(var(--v-theme-surface-default), 0.96);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
}

.product-docs__viewer-canvas {
  width: 100%;
  min-height: inherit;
}

.product-docs__viewer-empty,
.product-docs__viewer-loading,
.product-docs__viewer-error {
  padding: 2.5rem 1.5rem;
  text-align: center;
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  background: rgba(var(--v-theme-surface-glass), 0.85);
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.5);
  border-radius: 18px;
}

.product-docs__empty {
  padding: 1.5rem;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass), 0.85);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

@media (min-width: 768px) {
  .product-docs__navigation-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .product-docs__viewer-header {
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-start;
  }

  .product-docs__viewer-actions {
    justify-content: flex-end;
  }
}

@media (min-width: 1200px) {
  .product-docs__viewer-pane {
    padding: 2rem;
  }

  .product-docs__viewer-frame {
    max-height: clamp(32rem, 75vh, 52rem);
  }
}
</style>
