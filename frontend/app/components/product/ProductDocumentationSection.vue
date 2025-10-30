<template>
  <section :id="sectionId" class="product-docs">
    <header class="product-docs__header">
      <h2 class="product-docs__title">{{ $t('product.docs.title') }}</h2>
      <p class="product-docs__subtitle">{{ $t('product.docs.subtitle') }}</p>
    </header>

    <div v-if="hasPdfs" class="product-docs__layout">
      <nav
        class="product-docs__nav"
        role="tablist"
        :aria-label="tWithFallback('product.docs.navigationAria', 'Product documentation list')"
        :aria-activedescendant="activeTabId ?? undefined"
        data-testid="product-docs-nav"
      >
        <button
          type="button"
          class="product-docs__nav-arrow"
          :aria-label="tWithFallback('product.docs.controls.scrollLeft', 'Scroll left')"
          :disabled="!canScrollTabsBackward"
          @click="scrollTabs(-1)"
        >
          ‹
        </button>

        <div ref="tabsTrackRef" class="product-docs__nav-track">
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

        <button
          type="button"
          class="product-docs__nav-arrow"
          :aria-label="tWithFallback('product.docs.controls.scrollRight', 'Scroll right')"
          :disabled="!canScrollTabsForward"
          @click="scrollTabs(1)"
        >
          ›
        </button>
      </nav>

      <div
        :id="panelId"
        class="product-docs__viewer-pane"
        role="tabpanel"
        :aria-labelledby="activeTabId ?? undefined"
        data-testid="product-docs-viewer"
      >
        <header class="product-docs__viewer-header">
          <div class="product-docs__viewer-header-text">
            <h3 class="product-docs__viewer-title">{{ activePdfTitle }}</h3>
            <p v-if="activePdfMeta" class="product-docs__viewer-meta">
              {{ activePdfMeta }}
            </p>
            <p v-if="activePdfLanguage" class="product-docs__viewer-language">
              {{ activePdfLanguage }}
            </p>
          </div>
          <div class="product-docs__viewer-actions">
            <a
              v-if="activePdf?.url"
              :href="activePdf.url"
              target="_blank"
              rel="noopener"
              class="product-docs__download"
            >
              {{ $t('product.docs.download') }}
            </a>
            <div class="product-docs__viewer-toolbar" role="group">
              <button
                type="button"
                class="product-docs__control"
                :aria-label="tWithFallback('product.docs.controls.zoomOut', 'Zoom out')"
                @click="zoomOut"
              >
                −
              </button>
              <button
                type="button"
                class="product-docs__control"
                :aria-label="tWithFallback('product.docs.controls.zoomIn', 'Zoom in')"
                @click="zoomIn"
              >
                +
              </button>
              <button
                type="button"
                class="product-docs__control"
                :aria-label="tWithFallback('product.docs.controls.resetView', 'Reset view')"
                @click="resetView"
              >
                {{ tWithFallback('product.docs.controls.reset', 'Reset') }}
              </button>
              <button
                type="button"
                class="product-docs__control"
                :aria-pressed="isFitWidth"
                :aria-label="tWithFallback('product.docs.controls.fitWidth', 'Fit to width')"
                @click="applyFitWidth"
              >
                {{ tWithFallback('product.docs.controls.fit', 'Fit width') }}
              </button>
              <button
                type="button"
                class="product-docs__control"
                :aria-label="tWithFallback('product.docs.controls.rotate', 'Rotate document')"
                @click="rotateClockwise"
              >
                {{ tWithFallback('product.docs.controls.rotateLabel', 'Rotate') }}
              </button>
              <button
                type="button"
                class="product-docs__control"
                :aria-label="
                  showAllPages
                    ? tWithFallback('product.docs.controls.switchSinglePage', 'Switch to single page view')
                    : tWithFallback('product.docs.controls.switchAllPages', 'Show all pages')
                "
                @click="togglePageView"
              >
                {{
                  showAllPages
                    ? tWithFallback('product.docs.controls.allPages', 'All pages')
                    : tWithFallback('product.docs.controls.singlePage', 'Single page')
                }}
              </button>
              <div
                v-if="!showAllPages && totalPages > 1"
                class="product-docs__page-controls"
                role="group"
                :aria-label="tWithFallback('product.docs.controls.pageNavigation', 'Page navigation')"
              >
                <button
                  type="button"
                  class="product-docs__control"
                  :disabled="currentPage <= 1"
                  :aria-label="tWithFallback('product.docs.controls.previousPage', 'Previous page')"
                  @click="previousPage"
                >
                  ‹
                </button>
                <span class="product-docs__page-indicator">
                  {{ pageIndicatorLabel }}
                </span>
                <button
                  type="button"
                  class="product-docs__control"
                  :disabled="currentPage >= totalPages"
                  :aria-label="tWithFallback('product.docs.controls.nextPage', 'Next page')"
                  @click="nextPage"
                >
                  ›
                </button>
              </div>
            </div>
          </div>
        </header>

        <ClientOnly>
          <template #default>
              <div ref="viewerSurfaceRef" class="product-docs__viewer-surface">
              <div class="product-docs__viewer-scroll">
                <VuePdfEmbed
                  v-if="activePdf?.url"
                  ref="pdfViewerRef"
                  :source="activePdf.url"
                  text-layer
                  annotation-layer
                  class="product-docs__viewer-frame"
                  :page="viewerPage"
                  :scale="viewerScale"
                  :rotation="viewerRotation"
                  :width="viewerWidth"
                  @loaded="onPdfLoaded"
                />
                <p v-else class="product-docs__viewer-empty" data-testid="product-docs-preview-empty">
                  {{ $t('product.docs.previewUnavailable') }}
                </p>
              </div>
            </div>
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
import { useEventListener, useResizeObserver } from '@vueuse/core'
import {
  computed,
  defineAsyncComponent,
  nextTick,
  onBeforeUnmount,
  onMounted,
  type PropType,
  ref,
  useId,
  watch,
} from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductPdfDto } from '~~/shared/api-client'

import 'vue-pdf-embed/dist/styles/annotationLayer.css'
import 'vue-pdf-embed/dist/styles/textLayer.css'

type VuePdfEmbedComponent = (typeof import('vue-pdf-embed'))['default']

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

const { locale, n, t, te } = useI18n()

const pdfs = computed(() => props.pdfs ?? [])

const activePdfIndex = ref(-1)
const tabsTrackRef = ref<HTMLElement | null>(null)
const viewerSurfaceRef = ref<HTMLElement | null>(null)
const pdfViewerRef = ref<InstanceType<VuePdfEmbedComponent> | null>(null)

const measuredViewerWidth = ref(0)
const canScrollTabsBackward = ref(false)
const canScrollTabsForward = ref(false)

const isFitWidth = ref(true)
const zoomLevel = ref(1)
const rotation = ref(0)
const showAllPages = ref(true)
const currentPage = ref(1)
const pdfLoadedPageCount = ref<number | null>(null)

const MIN_ZOOM = 0.5
const MAX_ZOOM = 3
const ZOOM_STEP = 0.25

const activePdf = computed(() => (activePdfIndex.value >= 0 ? pdfs.value[activePdfIndex.value] ?? null : null))

const hasPdfs = computed(() => pdfs.value.length > 0)

const sectionUid = useId()
const panelId = `${sectionUid}-panel`
const tabId = (index: number) => `${sectionUid}-tab-${index}`

const activeTabId = computed(() => (activePdfIndex.value >= 0 ? tabId(activePdfIndex.value) : null))

const totalPages = computed(() => activePdf.value?.numberOfPages ?? pdfLoadedPageCount.value ?? 0)

const viewerWidth = computed(() => (isFitWidth.value && measuredViewerWidth.value > 0 ? measuredViewerWidth.value : undefined))

const viewerScale = computed(() => (!isFitWidth.value ? zoomLevel.value : undefined))

const viewerRotation = computed(() => ((rotation.value % 360) + 360) % 360)

const viewerPage = computed(() => (showAllPages.value ? undefined : currentPage.value))

const pageIndicatorLabel = computed(() => {
  if (!totalPages.value) {
    return ''
  }

  if (te('product.docs.controls.pageOf')) {
    return t('product.docs.controls.pageOf', { page: currentPage.value, total: totalPages.value })
  }

  return `Page ${currentPage.value} / ${totalPages.value}`
})

const languageDisplayNames = computed(() => {
  try {
    return new Intl.DisplayNames([locale.value], { type: 'language' })
  } catch {
    return null
  }
})

const tWithFallback = (key: string, fallback: string) => (te(key) ? t(key) : fallback)

const selectPdf = (index: number) => {
  if (index < 0 || index >= pdfs.value.length) {
    return
  }

  if (index === activePdfIndex.value) {
    return
  }

  activePdfIndex.value = index
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

const updateTabScrollState = () => {
  const track = tabsTrackRef.value

  if (!track) {
    canScrollTabsBackward.value = false
    canScrollTabsForward.value = false
    return
  }

  const maxScrollLeft = Math.max(track.scrollWidth - track.clientWidth, 0)
  canScrollTabsBackward.value = track.scrollLeft > 1
  canScrollTabsForward.value = track.scrollLeft + 1 < maxScrollLeft
}

const scrollTabs = (direction: number) => {
  const track = tabsTrackRef.value
  if (!track) {
    return
  }

  const amount = track.clientWidth || 320
  const delta = direction * amount

  if (typeof track.scrollBy === 'function') {
    track.scrollBy({ left: delta, behavior: 'smooth' })
    return
  }

  track.scrollLeft += delta
}

watch(
  pdfs,
  (nextPdfs) => {
    if (!nextPdfs.length) {
      activePdfIndex.value = -1
      return
    }

    const clampedIndex = Math.min(Math.max(activePdfIndex.value, 0), nextPdfs.length - 1)
    activePdfIndex.value = clampedIndex

    if (import.meta.client) {
      nextTick(updateTabScrollState)
    }
  },
  { immediate: true }
)

const updateViewerWidth = (entries: ResizeObserverEntry[] | ResizeObserverEntry) => {
  const entry = Array.isArray(entries) ? entries[0] : entries
  const width = entry?.contentRect?.width ?? 0
  measuredViewerWidth.value = width
}

let stopTabScrollListeners: Array<() => void> = []
let stopViewerResize: (() => void) | undefined

if (import.meta.client) {
  onMounted(() => {
    stopTabScrollListeners = [
      useEventListener(tabsTrackRef, 'scroll', updateTabScrollState),
      useEventListener(window, 'resize', () => requestAnimationFrame(updateTabScrollState)),
      useResizeObserver(tabsTrackRef, updateTabScrollState),
    ]

    stopViewerResize = useResizeObserver(viewerSurfaceRef, updateViewerWidth)

    nextTick(() => {
      updateTabScrollState()
    })
  })

  onBeforeUnmount(() => {
    stopTabScrollListeners.forEach((stop) => stop())
    stopTabScrollListeners = []

    if (stopViewerResize) {
      stopViewerResize()
      stopViewerResize = undefined
    }
  })
}

const onTabKeydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    selectPdf(index)
  }
}

const pdfTitle = (pdf: ProductPdfDto) => pdf.extractedTitle ?? pdf.metadataTitle ?? pdf.fileName ?? t('product.docs.untitled')

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
  const code = pdf.language?.trim()
  if (!code) {
    return null
  }

  let canonical: string | null = null

  try {
    ;[canonical] = Intl.getCanonicalLocales(code)
  } catch {
    canonical = null
  }

  if (!canonical) {
    return code
  }

  const displayName = languageDisplayNames.value?.of(canonical) ?? null

  if (!displayName || displayName.toLowerCase() === canonical.toLowerCase()) {
    return canonical.toUpperCase()
  }

  return `${displayName} · ${canonical.toUpperCase()}`
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

const activePdfTitle = computed(() => (activePdf.value ? pdfTitle(activePdf.value) : t('product.docs.title')))
const activePdfMeta = computed(() => (activePdf.value ? formatMeta(activePdf.value) : null))
const activePdfLanguage = computed(() => (activePdf.value ? formatLanguage(activePdf.value) : null))

watch(
  activePdf,
  () => {
    zoomLevel.value = 1
    rotation.value = 0
    isFitWidth.value = true
    showAllPages.value = true
    currentPage.value = 1
    pdfLoadedPageCount.value = null
  },
  { immediate: true }
)

watch(totalPages, (nextTotal) => {
  if (!showAllPages.value && nextTotal > 0) {
    currentPage.value = clamp(currentPage.value, 1, nextTotal)
  }
})

watch(showAllPages, (allPages) => {
  if (allPages) {
    currentPage.value = 1
  } else if (totalPages.value > 0) {
    currentPage.value = clamp(currentPage.value, 1, totalPages.value)
  }
})

const zoomIn = () => {
  isFitWidth.value = false
  zoomLevel.value = Math.min(zoomLevel.value + ZOOM_STEP, MAX_ZOOM)
}

const zoomOut = () => {
  isFitWidth.value = false
  zoomLevel.value = Math.max(zoomLevel.value - ZOOM_STEP, MIN_ZOOM)
}

const resetView = () => {
  zoomLevel.value = 1
  rotation.value = 0
  isFitWidth.value = true
  showAllPages.value = true
  currentPage.value = 1
}

const applyFitWidth = () => {
  isFitWidth.value = true
}

const rotateClockwise = () => {
  rotation.value = (rotation.value + 90) % 360
}

const togglePageView = () => {
  showAllPages.value = !showAllPages.value
}

const previousPage = () => {
  if (showAllPages.value || currentPage.value <= 1) {
    return
  }

  currentPage.value = clamp(currentPage.value - 1, 1, totalPages.value || 1)
}

const nextPage = () => {
  if (showAllPages.value || !totalPages.value || currentPage.value >= totalPages.value) {
    return
  }

  currentPage.value = clamp(currentPage.value + 1, 1, totalPages.value)
}

const onPdfLoaded = (doc: { numPages?: number } | null) => {
  const pages = doc?.numPages
  if (Number.isFinite(pages)) {
    pdfLoadedPageCount.value = Number(pages)
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

.product-docs__nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 18px;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-docs__nav-arrow {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
  background: rgba(var(--v-theme-surface-default), 0.85);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-size: 1.2rem;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.product-docs__nav-arrow:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}

.product-docs__nav-arrow:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.product-docs__nav-arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.product-docs__nav-track {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  padding: 0.25rem 0;
  scroll-snap-type: x proximity;
  scrollbar-width: none;
}

.product-docs__nav-track::-webkit-scrollbar {
  display: none;
}

.product-docs__tab {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
  flex: 0 0 auto;
  min-width: min(260px, 80vw);
  padding: 1rem 1.25rem;
  border-radius: 20px;
  border: 2px solid transparent;
  background: rgba(var(--v-theme-surface-glass), 0.82);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  scroll-snap-align: start;
}

.product-docs__tab:hover {
  transform: translateY(-2px);
}

.product-docs__tab:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.product-docs__tab--active {
  border-color: rgba(var(--v-theme-accent-primary-highlight), 0.9);
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
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
  --product-docs-viewer-height: min(70vh, 720px);
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

.product-docs__viewer-header-text {
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
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-start;
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
  gap: 0.5rem;
  align-items: center;
}

.product-docs__control {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.45rem 0.9rem;
  border-radius: 12px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  background: rgba(var(--v-theme-surface-default), 0.92);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.product-docs__control:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.12);
}

.product-docs__control:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.32);
}

.product-docs__control:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.product-docs__control[aria-pressed='true'] {
  background: rgba(var(--v-theme-surface-primary-080), 0.95);
  border-color: rgba(var(--v-theme-accent-primary-highlight), 0.7);
  color: rgba(var(--v-theme-text-neutral-strong), 0.95);
}

.product-docs__page-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: 0.5rem;
}

.product-docs__page-indicator {
  min-width: 80px;
  text-align: center;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-docs__viewer-surface {
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-default), 0.96);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
}

.product-docs__viewer-scroll {
  max-height: var(--product-docs-viewer-height);
  overflow-y: auto;
  padding: 1rem 1.25rem 1.5rem;
}

.product-docs__viewer-scroll::-webkit-scrollbar {
  width: 10px;
}

.product-docs__viewer-scroll::-webkit-scrollbar-track {
  background: rgba(var(--v-theme-surface-glass), 0.85);
  border-radius: 999px;
}

.product-docs__viewer-scroll::-webkit-scrollbar-thumb {
  background: rgba(var(--v-theme-border-primary-strong), 0.5);
  border-radius: 999px;
}

.product-docs__viewer-frame {
  width: 100%;
  display: block;
}

.product-docs__viewer-frame :deep(canvas) {
  width: 100% !important;
  height: auto !important;
  margin: 0 auto 1.5rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  border-radius: 12px;
}

.product-docs__viewer-frame :deep(canvas:last-child) {
  margin-bottom: 0;
}

.product-docs__viewer-empty,
.product-docs__viewer-loading {
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

@media (min-width: 960px) {
  .product-docs__nav {
    padding: 1rem 1.25rem;
    gap: 1rem;
  }

  .product-docs__tab {
    min-width: 240px;
  }

  .product-docs__viewer-pane {
    padding: 2rem;
  }

  .product-docs__viewer-header {
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-start;
  }

  .product-docs__viewer-actions {
    flex-direction: row;
    align-items: center;
    gap: 0.75rem 1rem;
  }

  .product-docs__viewer-toolbar {
    justify-content: flex-end;
  }
}

@media (max-width: 599px) {
  .product-docs__nav {
    grid-template-columns: auto 1fr auto;
    padding: 0.75rem;
  }

  .product-docs__tab {
    min-width: 210px;
    padding: 0.85rem 1rem;
  }

  .product-docs__viewer-pane {
    padding: 1.25rem;
  }

  .product-docs__viewer-scroll {
    padding: 0.75rem 1rem 1.25rem;
  }
}
</style>
