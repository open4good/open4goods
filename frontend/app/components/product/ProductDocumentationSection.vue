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
        :aria-label="
          tWithFallback(
            'product.docs.navigationAria',
            'Product documentation list'
          )
        "
        :aria-activedescendant="activeTabId ?? undefined"
        data-testid="product-docs-nav"
      >
        <button
          v-if="tabsOverflowing"
          type="button"
          class="product-docs__nav-arrow"
          :aria-label="
            tWithFallback('product.docs.controls.scrollLeft', 'Scroll left')
          "
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
            :title="pdfTitle(pdf)"
            @click="selectPdf(index)"
            @keydown="onTabKeydown(index, $event)"
          >
            <span class="product-docs__tab-title">{{
              truncatedTabTitle(pdf)
            }}</span>
            <div class="product-docs__tab-columns">
              <div
                class="product-docs__tab-column product-docs__tab-column--left"
              >
                <span
                  v-for="(item, leftIndex) in getTabLeftMeta(pdf)"
                  :key="`left-${pdf.cacheKey ?? index}-${leftIndex}`"
                  class="product-docs__tab-meta"
                >
                  {{ item }}
                </span>
              </div>
              <div
                class="product-docs__tab-column product-docs__tab-column--right"
              >
                <span
                  v-for="(item, rightIndex) in getTabRightMeta(pdf)"
                  :key="`right-${pdf.cacheKey ?? index}-${rightIndex}`"
                  class="product-docs__tab-meta"
                >
                  {{ item }}
                </span>
              </div>
            </div>
          </button>
        </div>

        <button
          v-if="tabsOverflowing"
          type="button"
          class="product-docs__nav-arrow"
          :aria-label="
            tWithFallback('product.docs.controls.scrollRight', 'Scroll right')
          "
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
          <div class="product-docs__viewer-heading">
            <h3 class="product-docs__viewer-title" :title="activePdfTitle">
              {{ activePdfDisplayTitle }}
            </h3>
            <p v-if="activePdfMetaLeft" class="product-docs__viewer-meta">
              {{ activePdfMetaLeft }}
            </p>
            <p v-if="activePdfMetaRight" class="product-docs__viewer-meta">
              {{ activePdfMetaRight }}
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
              <v-icon
                icon="mdi-file-pdf-box"
                size="20"
                class="product-docs__download-icon"
              />
              <span>{{ $t('product.docs.download') }}</span>
            </a>
            <div class="product-docs__viewer-toolbar" role="group">
              <button
                type="button"
                class="product-docs__control"
                :aria-label="
                  tWithFallback(
                    'product.docs.controls.rotate',
                    'Rotate document'
                  )
                "
                @click="rotateClockwise"
              >
                {{
                  tWithFallback('product.docs.controls.rotateLabel', 'Rotate')
                }}
              </button>
            </div>
          </div>
        </header>

        <ClientOnly>
          <template #default>
            <div ref="viewerSurfaceRef" class="product-docs__viewer-surface">
              <div class="product-docs__viewer-scroll">
                <VuePdfEmbed
                  v-if="activePdf?.url && !pdfError"
                  :source="activePdf.url"
                  text-layer
                  annotation-layer
                  class="product-docs__viewer-frame product-docs__viewer-frame--fit"
                  :rotation="viewerRotation"
                  :width="viewerWidth"
                  @loaded="onPdfLoaded"
                  @loading-failed="onPdfError"
                />
                <p
                  v-else
                  class="product-docs__viewer-empty"
                  data-testid="product-docs-preview-empty"
                >
                  {{
                    pdfError
                      ? tWithFallback(
                          'product.docs.previewError',
                          'Preview unavailable'
                        )
                      : $t('product.docs.previewUnavailable')
                  }}
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

type VuePdfEmbedComponent = (typeof import('vue-pdf-embed'))['default']

const VuePdfEmbed = defineAsyncComponent<VuePdfEmbedComponent>(async () => {
  if (!import.meta.client) {
    return () => null
  }

  const [module] = await Promise.all([
    import('vue-pdf-embed'),
    import('vue-pdf-embed/dist/styles/annotationLayer.css'),
    import('vue-pdf-embed/dist/styles/textLayer.css'),
  ])

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

const measuredViewerWidth = ref(0)
const canScrollTabsBackward = ref(false)
const canScrollTabsForward = ref(false)
const tabsOverflowing = ref(false)

const rotation = ref(0)
const pdfLoadedPageCount = ref<number | null>(null)
const pdfError = ref(false)

const MAX_TAB_TITLE_LENGTH = 35

const activePdf = computed(() =>
  activePdfIndex.value >= 0 ? (pdfs.value[activePdfIndex.value] ?? null) : null
)

const hasPdfs = computed(() => pdfs.value.length > 0)

const sectionUid = useId()
const panelId = `${sectionUid}-panel`
const tabId = (index: number) => `${sectionUid}-tab-${index}`

const activeTabId = computed(() =>
  activePdfIndex.value >= 0 ? tabId(activePdfIndex.value) : null
)

const totalPages = computed(
  () => activePdf.value?.numberOfPages ?? pdfLoadedPageCount.value ?? 0
)

const viewerWidth = computed(() =>
  measuredViewerWidth.value > 0 ? measuredViewerWidth.value : undefined
)

const viewerRotation = computed(() => ((rotation.value % 360) + 360) % 360)

const languageDisplayNames = computed(() => {
  try {
    return new Intl.DisplayNames([locale.value], { type: 'language' })
  } catch {
    return null
  }
})

const tWithFallback = (key: string, fallback: string) =>
  te(key) ? t(key) : fallback

const selectPdf = (index: number) => {
  if (index < 0 || index >= pdfs.value.length) {
    return
  }

  if (index === activePdfIndex.value) {
    return
  }

  activePdfIndex.value = index
}

const updateTabScrollState = () => {
  const track = tabsTrackRef.value

  if (!track) {
    canScrollTabsBackward.value = false
    canScrollTabsForward.value = false
    tabsOverflowing.value = false
    return
  }

  const maxScrollLeft = Math.max(track.scrollWidth - track.clientWidth, 0)
  canScrollTabsBackward.value = track.scrollLeft > 1
  canScrollTabsForward.value = track.scrollLeft + 1 < maxScrollLeft
  tabsOverflowing.value = track.scrollWidth - track.clientWidth > 1
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
  nextPdfs => {
    if (!nextPdfs.length) {
      activePdfIndex.value = -1
      return
    }

    const clampedIndex = Math.min(
      Math.max(activePdfIndex.value, 0),
      nextPdfs.length - 1
    )
    activePdfIndex.value = clampedIndex

    if (import.meta.client) {
      nextTick(updateTabScrollState)
    }
  },
  { immediate: true }
)

const updateViewerWidth = (
  entries: ResizeObserverEntry[] | ResizeObserverEntry
) => {
  const entry = Array.isArray(entries) ? entries[0] : entries
  const width = entry?.contentRect?.width ?? 0
  measuredViewerWidth.value = width
}

let stopTabScrollListeners: Array<() => void> = []
let stopViewerResize: (() => void) | undefined

if (import.meta.client) {
  onMounted(() => {
    const tabResizeObserver = useResizeObserver(
      tabsTrackRef,
      updateTabScrollState
    )

    stopTabScrollListeners = [
      useEventListener(tabsTrackRef, 'scroll', updateTabScrollState),
      useEventListener(window, 'resize', () =>
        requestAnimationFrame(updateTabScrollState)
      ),
      tabResizeObserver.stop,
    ].filter((stop): stop is () => void => typeof stop === 'function')

    const viewerResizeObserver = useResizeObserver(
      viewerSurfaceRef,
      updateViewerWidth
    )
    stopViewerResize =
      typeof viewerResizeObserver.stop === 'function'
        ? viewerResizeObserver.stop
        : undefined

    nextTick(() => {
      updateTabScrollState()
    })
  })

  onBeforeUnmount(() => {
    stopTabScrollListeners.forEach(stop => stop())
    stopTabScrollListeners = []

    stopViewerResize?.()
    stopViewerResize = undefined
  })
}

const onTabKeydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    selectPdf(index)
  }
}

const fallbackDocumentTitle = computed(() =>
  tWithFallback('product.docs.documentPdf', 'Document PDF')
)

const resolvePdfTitle = (pdf: ProductPdfDto) => {
  const candidates = [pdf.extractedTitle, pdf.metadataTitle, pdf.fileName]
  const resolved = candidates.find(
    (value): value is string => typeof value === 'string' && value.trim().length
  )
  return resolved?.trim() ?? fallbackDocumentTitle.value
}

const truncateTitle = (title: string) => {
  if (title.length <= MAX_TAB_TITLE_LENGTH) {
    return title
  }

  const sliceLength = Math.max(0, MAX_TAB_TITLE_LENGTH - 1)
  return `${title.slice(0, sliceLength).trimEnd()}…`
}

const pdfTitle = (pdf: ProductPdfDto) => resolvePdfTitle(pdf)

const truncatedTabTitle = (pdf: ProductPdfDto) => truncateTitle(pdfTitle(pdf))

const formatPageCount = (count?: number | null) => {
  if (!Number.isFinite(count)) {
    return null
  }

  const total = Math.max(0, Math.round(Number(count)))
  if (!total) {
    return null
  }

  if (te('product.docs.pageCount')) {
    return t('product.docs.pageCount', total, { count: total })
  }

  return `${total} page${total === 1 ? '' : 's'}`
}

const formatDate = (pdf: ProductPdfDto) => {
  const timestamp = pdf.modificationDate ?? pdf.creationDate ?? pdf.timeStamp
  if (!timestamp) {
    return null
  }

  try {
    return new Intl.DateTimeFormat(locale.value, {
      dateStyle: 'medium',
    }).format(new Date(timestamp))
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

const formatProducer = (pdf: ProductPdfDto) => {
  const producer = pdf.author?.trim()
  return producer?.length ? producer : null
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

const getTabLeftMeta = (pdf: ProductPdfDto) => {
  const inferredPageCount =
    pdf === activePdf.value ? totalPages.value : pdf.numberOfPages

  return [
    formatPageCount(inferredPageCount),
    formatDate(pdf),
    formatLanguage(pdf),
  ].filter((value): value is string => Boolean(value))
}

const getTabRightMeta = (pdf: ProductPdfDto) =>
  [formatBytes(pdf.fileSize), formatProducer(pdf)].filter(
    (value): value is string => Boolean(value)
  )

const joinMetaParts = (parts: string[]) =>
  parts.filter(value => value?.trim().length).join(' · ')

const activePdfTitle = computed(() =>
  activePdf.value ? pdfTitle(activePdf.value) : t('product.docs.title')
)
const activePdfDisplayTitle = computed(() =>
  truncateTitle(activePdfTitle.value)
)
const activePdfMetaLeft = computed(() =>
  activePdf.value ? joinMetaParts(getTabLeftMeta(activePdf.value)) : ''
)
const activePdfMetaRight = computed(() =>
  activePdf.value ? joinMetaParts(getTabRightMeta(activePdf.value)) : ''
)

watch(
  activePdf,
  () => {
    rotation.value = 0
    pdfLoadedPageCount.value = null
    pdfError.value = false
  },
  { immediate: true }
)

const rotateClockwise = () => {
  rotation.value = (rotation.value + 90) % 360
}

const onPdfLoaded = (doc: { numPages?: number } | null) => {
  const pages = doc?.numPages
  if (Number.isFinite(pages)) {
    pdfLoadedPageCount.value = Number(pages)
  }
}

const onPdfError = () => {
  pdfError.value = true
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
  display: flex;
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
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    opacity 0.2s ease;
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
  flex: 1 1 auto;
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
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
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

.product-docs__tab-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.35rem 0.75rem;
  width: 100%;
}

.product-docs__tab-column {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.product-docs__tab-column--right {
  text-align: right;
  align-items: flex-end;
}

.product-docs__tab-meta {
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-docs__viewer-pane {
  --product-docs-viewer-height: min(140vh, 1440px);
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

.product-docs__viewer-heading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
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
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-docs__download-icon {
  color: currentColor;
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
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
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
  display: block;
}

.product-docs__viewer-frame :deep(canvas) {
  display: block;
  margin: 0 auto 1.5rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  border-radius: 12px;
}

.product-docs__viewer-frame--fit :deep(canvas) {
  width: 100% !important;
  height: auto !important;
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
    padding: 0.75rem;
  }

  .product-docs__tab {
    min-width: 210px;
    padding: 0.85rem 1rem;
  }

  .product-docs__tab-columns {
    grid-template-columns: 1fr;
  }

  .product-docs__tab-column--right {
    align-items: flex-start;
    text-align: left;
  }

  .product-docs__viewer-pane {
    padding: 1.25rem;
  }

  .product-docs__viewer-scroll {
    padding: 0.75rem 1rem 1.25rem;
  }
}
</style>
