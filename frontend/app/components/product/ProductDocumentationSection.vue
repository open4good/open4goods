<template>
  <section :id="sectionId" class="product-docs">
    <header class="product-docs__header">
      <h2 class="product-docs__title">{{ $t('product.docs.title') }}</h2>
      <p class="product-docs__subtitle">{{ $t('product.docs.subtitle') }}</p>
    </header>

    <div v-if="hasPdfs" class="product-docs__layout">
      <div
        :id="panelId"
        class="product-docs__viewer-pane"
        role="tabpanel"
        :aria-labelledby="activeTabId ?? undefined"
        data-testid="product-docs-viewer"
      >
        <header class="product-docs__viewer-header">
          <h3 class="product-docs__viewer-title">{{ activePdfTitle }}</h3>
          <p v-if="activePdfMeta" class="product-docs__viewer-meta">
            {{ activePdfMeta }}
          </p>
          <p v-if="activePdfLanguage" class="product-docs__viewer-language">
            {{ activePdfLanguage }}
          </p>
          <a
            v-if="activePdf?.url"
            :href="activePdf.url"
            target="_blank"
            rel="noopener"
            class="product-docs__download"
          >
            {{ $t('product.docs.download') }}
          </a>
        </header>

        <ClientOnly>
          <template #default>
            <VuePdfEmbed
              v-if="activePdf?.url"
              :source="activePdf.url"
              text-layer
              annotation-layer
              class="product-docs__viewer-frame"
              :style="{ height: viewerHeight }"
            />
            <p v-else class="product-docs__viewer-empty" data-testid="product-docs-preview-empty">
              {{ $t('product.docs.previewUnavailable') }}
            </p>
          </template>
          <template #fallback>
            <p class="product-docs__viewer-loading">
              {{ $t('product.docs.loading') }}
            </p>
          </template>
        </ClientOnly>
      </div>

      <aside class="product-docs__sidebar" :aria-label="$t('product.docs.navigationAria')">
        <h3 class="product-docs__sidebar-title">{{ $t('product.docs.sidebarTitle') }}</h3>
        <div
          class="product-docs__tabs"
          role="tablist"
          :aria-activedescendant="activeTabId ?? undefined"
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
      </aside>
    </div>

    <p v-else class="product-docs__empty" data-testid="product-docs-empty">
      {{ $t('product.docs.empty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, type PropType, ref, useId, watch } from 'vue'
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

const { locale, n, t } = useI18n()

const viewerHeight = '600px'

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

const activeTabId = computed(() => (activePdfIndex.value >= 0 ? tabId(activePdfIndex.value) : null))

const languageDisplayNames = computed(() => {
  try {
    return new Intl.DisplayNames([locale.value], { type: 'language' })
  } catch {
    return null
  }
})

const selectPdf = (index: number) => {
  if (index < 0 || index >= pdfs.value.length) {
    return
  }

  if (index === activePdfIndex.value) {
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

  const normalized = code.toLowerCase()
  const displayName = languageDisplayNames.value?.of(normalized) ?? null

  if (!displayName || displayName.toLowerCase() === normalized) {
    return normalized.toUpperCase()
  }

  return `${displayName} · ${normalized.toUpperCase()}`
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

.product-docs__viewer-pane {
  --product-docs-viewer-height: 600px;
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

.product-docs__download {
  margin-top: 0.5rem;
  align-self: flex-start;
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

.product-docs__viewer-frame {
  width: 100%;
  height: var(--product-docs-viewer-height);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.18);
  background: rgba(var(--v-theme-surface-default), 0.96);
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

.product-docs__sidebar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-docs__sidebar-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.product-docs__tabs {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-docs__tab {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
  width: 100%;
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

.product-docs__empty {
  padding: 1.5rem;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass), 0.85);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

@media (min-width: 960px) {
  .product-docs__layout {
    flex-direction: row;
    align-items: flex-start;
  }

  .product-docs__viewer-pane {
    flex: 1 1 auto;
    padding: 2rem;
  }

  .product-docs__sidebar {
    flex: 0 0 320px;
    position: sticky;
    top: 96px;
  }
}

@media (max-width: 959px) {
  .product-docs__sidebar-title {
    text-transform: none;
    letter-spacing: normal;
  }
}
</style>
