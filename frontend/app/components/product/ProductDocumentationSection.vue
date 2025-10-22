<template>
  <section :id="sectionId" class="product-docs">
    <header class="product-docs__header">
      <h2 class="product-docs__title">{{ $t('product.docs.title') }}</h2>
      <p class="product-docs__subtitle">{{ $t('product.docs.subtitle') }}</p>
    </header>

    <div class="product-docs__list">
      <article v-for="pdf in pdfs" :key="pdf.cacheKey" class="product-docs__item">
        <header class="product-docs__item-header">
          <h3>{{ pdfTitle(pdf) }}</h3>
          <p class="product-docs__meta">
            {{ formatMeta(pdf) }}
          </p>
          <a :href="pdf.url" target="_blank" rel="noopener" class="product-docs__download">
            {{ $t('product.docs.download') }}
          </a>
        </header>
        <ClientOnly>
          <PdfViewer
            v-if="pdf.url"
            class="product-docs__viewer"
            :src="pdf.url"
            :style="{ height: viewerHeight }"
          />
        </ClientOnly>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { defineAsyncComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductPdfDto } from '~~/shared/api-client'

type PdfViewerComponent = (typeof import('@vue-pdf-viewer/viewer'))['VPdfViewer']

const PdfViewer = defineAsyncComponent<PdfViewerComponent>(async () => {
  const module = await import('@vue-pdf-viewer/viewer')
  return module.VPdfViewer
})

defineProps({
  sectionId: {
    type: String,
    default: 'documentation',
  },
  pdfs: {
    type: Array as PropType<ProductPdfDto[]>,
    default: () => [],
  },
})

const { n, t } = useI18n()

const viewerHeight = '480px'

const pdfTitle = (pdf: ProductPdfDto) => {
  return pdf.extractedTitle ?? pdf.metadataTitle ?? pdf.fileName ?? t('product.docs.untitled')
}

const formatMeta = (pdf: ProductPdfDto) => {
  const pages = pdf.numberOfPages ?? 0
  const size = pdf.fileSize ? formatBytes(pdf.fileSize) : null
  const author = pdf.author ?? null

  const parts = [
    pages ? t('product.docs.pageCount', { count: pages }) : null,
    size,
    author,
  ].filter(Boolean)

  return parts.join(' · ')
}

const formatBytes = (bytes: number) => {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return null
  }

  const kb = bytes / 1024
  if (kb < 1024) {
    return `${n(kb, { maximumFractionDigits: 1 })} KB`
  }

  const mb = kb / 1024
  return `${n(mb, { maximumFractionDigits: 1 })} MB`
}
</script>

<style scoped>
.product-docs {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-docs__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
}

.product-docs__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-docs__list {
  display: grid;
  gap: 1.5rem;
}

.product-docs__item {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.06);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-docs__item-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-docs__download {
  align-self: flex-start;
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: none;
  font-weight: 600;
}

.product-docs__viewer {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
}
</style>
