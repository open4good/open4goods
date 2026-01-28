<template>
  <section class="docs-page" :class="`docs-page--${variant}`">
    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="docs-page__loader"
      :aria-label="t('docs.labels.loading')"
    />

    <v-container v-if="!pending" class="docs-page__container">
      <div v-if="error" class="docs-page__error" role="alert">
        <v-alert type="error" variant="tonal">
          {{ t('docs.errors.load') }}
        </v-alert>
      </div>

      <div v-else-if="doc" class="docs-page__layout">
        <header class="docs-page__header">
          <slot name="header">
            <p class="docs-page__eyebrow">
              {{ t('docs.labels.documentation') }}
            </p>
            <h1 class="docs-page__title">{{ doc.title }}</h1>
            <p v-if="doc.description" class="docs-page__description">
              {{ doc.description }}
            </p>

            <div v-if="showMeta" class="docs-page__meta">
              <div v-if="doc.updatedAt" class="docs-page__meta-item">
                <v-icon icon="mdi-calendar" size="18" class="me-1" />
                <span>{{ t('docs.labels.updated') }}</span>
                <span class="ms-1">{{ formatDate(doc.updatedAt) }}</span>
              </div>

              <div v-if="doc.tags?.length" class="docs-page__meta-item">
                <span class="me-2">{{ t('docs.labels.tags') }}</span>
                <v-chip
                  v-for="tag in doc.tags"
                  :key="tag"
                  size="small"
                  class="me-2"
                  color="primary"
                  variant="tonal"
                >
                  {{ tag }}
                </v-chip>
              </div>
            </div>
          </slot>
        </header>

        <div class="docs-page__body">
          <article
            class="docs-page__content"
            :class="{ 'docs-prose': renderProse }"
            role="article"
            :aria-label="t('docs.labels.article')"
          >
            <ContentRenderer :value="doc" :components="resolvedComponents" />
          </article>

          <footer v-if="showMeta" class="docs-page__footer">
            <slot name="footer">
              <div class="docs-page__nav-links">
                <v-btn
                  v-if="previousDoc"
                  variant="text"
                  color="primary"
                  :to="previousDoc.path"
                >
                  {{ t('docs.labels.previous') }}
                  ·
                  {{ previousDoc.title }}
                </v-btn>
                <v-btn
                  v-if="nextDoc"
                  variant="text"
                  color="primary"
                  :to="nextDoc.path"
                >
                  {{ t('docs.labels.next') }}
                  ·
                  {{ nextDoc.title }}
                </v-btn>
              </div>
            </slot>
          </footer>
        </div>

        <aside v-if="showToc && tocItems.length" class="docs-page__toc">
          <slot name="aside">
            <v-card variant="outlined" class="docs-page__toc-card" rounded="xl">
              <v-card-title class="text-subtitle-1 font-weight-semibold">
                {{ t('docs.labels.toc') }}
              </v-card-title>
              <v-divider />
              <v-list density="compact" class="docs-page__toc-list">
                <v-list-item
                  v-for="item in tocItems"
                  :key="item.id"
                  :href="`#${item.id}`"
                  :title="item.title"
                  class="docs-page__toc-item"
                />
              </v-list>
            </v-card>
          </slot>
        </aside>
      </div>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'

import DocsContentLink from '~/components/docs/DocsContentLink.vue'
import {
  normalizeBasePath,
  normalizeDocsLocale,
  resolveDocPath,
  resolveLocaleFromRequest,
  useDocsContent,
  type DocsLocale,
} from '~/composables/useDocsContent'

type TocItem = {
  id: string
  title: string
  depth: number
}

const props = withDefaults(
  defineProps<{
    slugOrPath: string
    locale?: DocsLocale
    basePath?: string
    showMeta?: boolean
    showToc?: boolean
    tocDepth?: number
    renderProse?: boolean
    prefilterTags?: string[]
    mdcComponents?: Record<string, unknown>
    variant?: 'default' | 'compact'
    safeLinks?: boolean
  }>(),
  {
    locale: undefined,
    basePath: '/docs',
    showMeta: true,
    showToc: true,
    tocDepth: 3,
    renderProse: true,
    prefilterTags: () => [],
    mdcComponents: () => ({}),
    variant: 'default',
    safeLinks: false,
  }
)

const { t, locale: i18nLocale } = useI18n()
let requestURL: URL | null = null

try {
  requestURL = useRequestURL()
} catch {
  requestURL = null
}
const { buildCanonicalUrl, buildHreflangLinks, getDocByPath, listDocs } =
  useDocsContent()

const resolvedLocale = computed(() =>
  normalizeDocsLocale(props.locale ?? resolveLocaleFromRequest())
)
const resolvedBasePath = computed(() => normalizeBasePath(props.basePath))
const resolvedPath = computed(() =>
  resolveDocPath({
    locale: resolvedLocale.value,
    slugOrPath: props.slugOrPath,
    basePath: resolvedBasePath.value,
  })
)

const {
  data: doc,
  pending,
  error,
} = await useAsyncData(
  () => `docs-page:${resolvedPath.value}`,
  () => getDocByPath({ path: resolvedPath.value }),
  { watch: [resolvedPath] }
)

if (!doc.value) {
  throw createError({
    statusCode: 404,
    statusMessage: t('docs.errors.notFound'),
  })
}

const docsList = await useAsyncData(
  () => `docs-page-list:${resolvedLocale.value}`,
  () =>
    listDocs({
      locale: resolvedLocale.value,
      basePath: resolvedBasePath.value,
    })
)

const docsByPath = computed(() => {
  const list = docsList.data.value ?? []
  const filtered =
    props.prefilterTags.length > 0
      ? list.filter(entry =>
          entry.tags?.some(tag => props.prefilterTags.includes(tag))
        )
      : list

  return filtered
})

const currentIndex = computed(() =>
  docsByPath.value.findIndex(entry => entry.path === resolvedPath.value)
)

const previousDoc = computed(() => {
  if (currentIndex.value <= 0) {
    return null
  }

  return docsByPath.value[currentIndex.value - 1] ?? null
})

const nextDoc = computed(() => {
  if (currentIndex.value < 0) {
    return null
  }

  return docsByPath.value[currentIndex.value + 1] ?? null
})

const tocItems = computed<TocItem[]>(() => {
  const toc = doc.value?.body as { toc?: { links?: TocItem[] } } | undefined
  const items = toc?.toc?.links ?? []

  return items.filter(item => item.depth <= props.tocDepth)
})

const resolvedComponents = computed(() => ({
  ...(props.mdcComponents ?? {}),
  ...(props.safeLinks
    ? {
        a: (contentProps: { href?: string; target?: string; rel?: string }) =>
          h(DocsContentLink, {
            ...contentProps,
            safeLinks: true,
          }),
      }
    : {}),
}))

const formatDate = (value: string) => {
  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(i18nLocale.value, {
    dateStyle: 'medium',
  }).format(parsed)
}

const canonicalUrl = computed(() =>
  buildCanonicalUrl({
    baseUrl: requestURL?.origin ?? null,
    docPath: resolvedPath.value,
  })
)

const { data: translations } = await useAsyncData(
  () => `docs-page-translations:${resolvedPath.value}`,
  async () => {
    const otherLocales: DocsLocale[] = ['en', 'fr']
    const candidates = await Promise.all(
      otherLocales.map(async locale => {
        const localizedPath = resolveDocPath({
          locale,
          slugOrPath: resolvedPath.value,
          basePath: resolvedBasePath.value,
        })

        const exists = await getDocByPath({
          path: localizedPath,
          fields: ['path'],
        })

        return exists ? locale : null
      })
    )

    return candidates.filter(Boolean) as DocsLocale[]
  }
)

const hreflangLinks = computed(() =>
  buildHreflangLinks({
    docPath: resolvedPath.value,
    baseUrl: requestURL?.origin ?? null,
    availableLocales: translations.value ?? [],
  })
)

useSeoMeta({
  title: () => doc.value?.title ?? t('docs.labels.documentation'),
  description: () => doc.value?.description ?? '',
  ogTitle: () => doc.value?.title ?? '',
  ogDescription: () => doc.value?.description ?? '',
  ogUrl: () => canonicalUrl.value ?? undefined,
  robots: () => (doc.value?.draft ? 'noindex, nofollow' : undefined),
})

useHead(() => ({
  link: [
    ...(canonicalUrl.value
      ? [{ rel: 'canonical', href: canonicalUrl.value }]
      : []),
    ...hreflangLinks.value.map(link => ({
      rel: 'alternate',
      hreflang: link.hreflang,
      href: link.href,
    })),
  ],
}))
</script>

<style scoped>
.docs-page {
  padding-bottom: clamp(2rem, 4vw, 3rem);
}

.docs-page__loader {
  position: sticky;
  top: 0;
  z-index: 10;
}

.docs-page__container {
  display: flex;
  flex-direction: column;
  gap: clamp(1.5rem, 2.5vw, 2rem);
}

.docs-page__layout {
  display: grid;
  gap: clamp(1.5rem, 3vw, 2.5rem);
  grid-template-columns: minmax(0, 1fr);
}

@media (min-width: 960px) {
  .docs-page__layout {
    grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
    align-items: start;
  }
}

.docs-page__header {
  display: grid;
  gap: 0.75rem;
}

.docs-page__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-soft));
  font-size: 0.75rem;
}

.docs-page__title {
  font-size: clamp(2rem, 3vw, 2.8rem);
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.docs-page__description {
  font-size: 1.05rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  max-width: 60ch;
}

.docs-page__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.docs-page__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.docs-page__body {
  display: grid;
  gap: 1.5rem;
}

.docs-page__content {
  background: rgb(var(--v-theme-surface-default));
  border-radius: 24px;
  padding: clamp(1.5rem, 3vw, 2.5rem);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
}

.docs-page__toc {
  position: sticky;
  top: 1.5rem;
}

.docs-page__toc-card {
  background: rgb(var(--v-theme-surface-default));
}

.docs-page__nav-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
