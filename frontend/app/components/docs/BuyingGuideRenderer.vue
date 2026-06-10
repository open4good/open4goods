<template>
  <article class="buying-guide">
    <v-progress-linear
      class="buying-guide__progress"
      color="primary"
      :model-value="readingProgress"
      height="3"
      :aria-label="t('buyingGuide.article.progressAria')"
    />

    <v-fade-transition appear>
      <section class="buying-guide__hero">
        <v-container class="buying-guide__hero-container">
          <v-row align="center" class="buying-guide__hero-row">
            <v-col cols="12" md="7" lg="8">
              <v-breadcrumbs
                :items="breadcrumbItems"
                class="buying-guide__breadcrumbs"
                density="compact"
              />

              <div class="buying-guide__kicker">
                <v-chip
                  color="primary"
                  variant="tonal"
                  size="small"
                  class="buying-guide__eyebrow"
                >
                  {{ t('buyingGuide.article.eyebrow') }}
                </v-chip>
              </div>

              <h1 class="buying-guide__title">{{ doc.title }}</h1>
              <p v-if="doc.description" class="buying-guide__description">
                {{ doc.description }}
              </p>

              <div class="buying-guide__meta">
                <v-chip
                  v-if="doc.updatedAt"
                  prepend-icon="mdi-calendar"
                  variant="flat"
                  color="surface"
                >
                  {{
                    t('buyingGuide.article.updated', { date: formattedDate })
                  }}
                </v-chip>
                <v-chip
                  v-if="readingTimeLabel"
                  prepend-icon="mdi-timer"
                  variant="flat"
                  color="surface"
                >
                  {{ readingTimeLabel }}
                </v-chip>
                <v-chip
                  v-for="tag in visibleTags"
                  :key="tag"
                  variant="tonal"
                  color="secondary"
                >
                  {{ tag }}
                </v-chip>
              </div>
            </v-col>

            <v-col cols="12" md="5" lg="4">
              <v-slide-y-transition appear>
                <v-img
                  v-if="heroImage"
                  :src="heroImage"
                  :alt="categoryTitle"
                  aspect-ratio="16/9"
                  cover
                  eager
                  width="640"
                  height="360"
                  class="buying-guide__hero-image"
                />
              </v-slide-y-transition>
            </v-col>
          </v-row>
        </v-container>
      </section>
    </v-fade-transition>

    <v-container class="buying-guide__container">
      <div class="buying-guide__layout">
        <aside
          v-if="tocItems.length"
          class="buying-guide__toc-rail d-none d-md-block"
        >
          <StickySectionNavigation
            :sections="tocSections"
            :active-section="activeSection"
            :aria-label="t('buyingGuide.article.tocAria')"
            @navigate="navigateToSection"
          />
        </aside>

        <main class="buying-guide__reader">
          <v-expansion-panels
            v-if="tocItems.length"
            variant="accordion"
            class="buying-guide__mobile-toc d-md-none"
          >
            <v-expansion-panel>
              <v-expansion-panel-title expand-icon="mdi-chevron-down">
                {{ t('docs.labels.toc') }}
              </v-expansion-panel-title>
              <v-expansion-panel-text>
                <StickySectionNavigation
                  :sections="tocSections"
                  :active-section="activeSection"
                  :aria-label="t('buyingGuide.article.tocAria')"
                  orientation="horizontal"
                  :sticky="false"
                  @navigate="navigateToSection"
                />
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>

          <v-alert
            v-if="!doc.body"
            type="info"
            variant="tonal"
            class="buying-guide__empty"
          >
            {{ t('docs.errors.load') }}
          </v-alert>

          <SectionReveal v-else transition="slide-y">
            <div
              ref="contentRoot"
              class="buying-guide__content docs-prose"
              role="article"
              :aria-label="t('docs.labels.article')"
            >
              <ContentRenderer :value="doc" :components="resolvedComponents" />
            </div>
          </SectionReveal>
        </main>
      </div>
    </v-container>
  </article>
</template>

<script setup lang="ts">
import {
  computed,
  defineComponent,
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue'

import DocsContentLink from '~/components/docs/DocsContentLink.vue'
import SectionReveal from '~/components/shared/ui/SectionReveal.vue'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'
import ProductCardEmbed from '~/components/product/ProductCardEmbed.vue'
import ProductEmbed from '~/components/product/ProductEmbed.vue'
import BrandShareChart from '~/components/dataviz/BrandShareChart.vue'
import GuideProductGrid from '~/components/product/GuideProductGrid.vue'
import {
  provideGuideContext,
  type GuideContext,
} from '~/composables/useGuideContext'
import type { DocsDoc } from '~/composables/useDocsContent'

type TocItem = {
  id: string
  text?: string
  title?: string
  depth: number
}

const props = defineProps<{
  doc: DocsDoc
  guideContext: GuideContext
  breadcrumbs: Array<{ title?: string; link?: string; to?: string }>
}>()

const { t, locale } = useI18n()

provideGuideContext(props.guideContext)

const activeSection = ref('')
const readingProgress = ref(0)
const contentRoot = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const categoryTitle = computed(() => props.guideContext.categoryTitle)
const heroImage = computed(() => props.guideContext.heroImage)

const breadcrumbItems = computed(() =>
  props.breadcrumbs.map(item => ({
    title: item.title ?? '',
    to: item.to ?? item.link,
    disabled: !item.to && !item.link,
  }))
)

const visibleTags = computed(() =>
  (props.doc.tags ?? []).filter(tag => !tag.startsWith('language:')).slice(0, 4)
)

const formattedDate = computed(() => {
  if (!props.doc.updatedAt) {
    return ''
  }

  const parsed = new Date(props.doc.updatedAt)

  if (Number.isNaN(parsed.getTime())) {
    return props.doc.updatedAt
  }

  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'medium',
  }).format(parsed)
})

const tocItems = computed<TocItem[]>(() => {
  const body = props.doc.body as { toc?: { links?: TocItem[] } } | undefined
  const items = body?.toc?.links ?? []

  return items.filter(item => item.id && item.depth <= 3)
})

const tocSections = computed(() =>
  tocItems.value.map(item => ({
    id: item.id,
    label: item.text ?? item.title ?? item.id,
  }))
)

const plainBodyText = computed(() => collectPlainText(props.doc.body))

const readingTimeMinutes = computed(() => {
  const wordCount = plainBodyText.value.split(/\s+/u).filter(Boolean).length

  if (!wordCount) {
    return 0
  }

  return Math.max(1, Math.ceil(wordCount / 220))
})

const readingTimeLabel = computed(() => {
  if (!readingTimeMinutes.value) {
    return ''
  }

  return t('buyingGuide.article.readingTime', {
    minutes: readingTimeMinutes.value,
  })
})

const navigateToSection = (sectionId: string) => {
  activeSection.value = sectionId

  if (!import.meta.client) {
    return
  }

  const target = document.getElementById(sectionId)

  if (!target) {
    return
  }

  window.scrollTo({
    top: target.getBoundingClientRect().top + window.scrollY - 96,
    behavior: 'smooth',
  })
}

const updateReadingProgress = () => {
  if (!import.meta.client || !contentRoot.value) {
    return
  }

  const rect = contentRoot.value.getBoundingClientRect()
  const start = window.scrollY + rect.top - 96
  const end = start + contentRoot.value.offsetHeight - window.innerHeight
  const scrolled = window.scrollY - start
  const progress = end <= start ? 100 : (scrolled / (end - start)) * 100

  readingProgress.value = Math.min(100, Math.max(0, progress))
}

const updateActiveSectionFromScroll = () => {
  if (!import.meta.client || !tocItems.value.length) {
    return
  }

  const current = tocItems.value
    .map(item => document.getElementById(item.id))
    .filter((element): element is HTMLElement => Boolean(element))
    .reverse()
    .find(element => element.getBoundingClientRect().top <= 120)

  if (current) {
    activeSection.value = current.id
  }
}

const handleScroll = () => {
  updateReadingProgress()
  updateActiveSectionFromScroll()
}

const setupScrollSpy = async () => {
  if (!import.meta.client) {
    return
  }

  await nextTick()
  updateReadingProgress()
  updateActiveSectionFromScroll()

  if (observer) {
    observer.disconnect()
  }

  if ('IntersectionObserver' in window) {
    observer = new IntersectionObserver(
      entries => {
        const visibleEntry = entries
          .filter(entry => entry.isIntersecting)
          .sort(
            (first, second) =>
              first.boundingClientRect.top - second.boundingClientRect.top
          )[0]

        if (visibleEntry?.target.id) {
          activeSection.value = visibleEntry.target.id
        }
      },
      {
        rootMargin: '-96px 0px -65% 0px',
        threshold: 0.01,
      }
    )

    tocItems.value.forEach(item => {
      const heading = document.getElementById(item.id)

      if (heading) {
        observer?.observe(heading)
      }
    })
  }

  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', updateReadingProgress, { passive: true })
}

onMounted(() => {
  void setupScrollSpy()
})

onBeforeUnmount(() => {
  observer?.disconnect()

  if (import.meta.client) {
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('resize', updateReadingProgress)
  }
})

let h1RenderCount = 0

const MarkdownH1 = defineComponent({
  name: 'BuyingGuideMarkdownH1',
  setup(_, { attrs, slots }) {
    const isDuplicateTitle = h1RenderCount === 0
    h1RenderCount += 1

    return () =>
      h(
        'h1',
        {
          ...attrs,
          class: [
            attrs.class,
            isDuplicateTitle
              ? 'buying-guide__markdown-title--duplicate'
              : undefined,
          ],
          'aria-hidden': isDuplicateTitle ? 'true' : undefined,
        },
        slots.default?.()
      )
  },
})

const resolvedComponents = computed(() => ({
  ProductCardEmbed,
  ProductEmbed,
  BrandShareChart,
  GuideProductGrid,
  h1: MarkdownH1,
  a: (contentProps: { href?: string; target?: string; rel?: string }) =>
    h(DocsContentLink, {
      ...contentProps,
      safeLinks: true,
    }),
}))

const collectPlainText = (value: unknown): string => {
  if (!value) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (Array.isArray(value)) {
    return value.map(item => collectPlainText(item)).join(' ')
  }

  if (typeof value === 'object') {
    const record = value as Record<string, unknown>

    return Object.entries(record)
      .filter(
        ([key]) => !['toc', 'props', 'id', 'tag', 'type'].includes(key)
      )
      .map(([, item]) => collectPlainText(item))
      .join(' ')
  }

  return ''
}
</script>

<style scoped>
.buying-guide {
  background: rgb(var(--v-theme-surface-default));
  color: rgb(var(--v-theme-text-neutral-strong));
}

.buying-guide__progress {
  position: sticky;
  top: 64px;
  z-index: 12;
}

.buying-guide__hero {
  background: rgba(var(--v-theme-surface-muted), 0.58);
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.18);
}

.buying-guide__hero-container {
  padding-top: clamp(1rem, 3vw, 2rem);
  padding-bottom: clamp(1.75rem, 4vw, 3.25rem);
}

.buying-guide__hero-row {
  min-height: min(38vh, 360px);
}

.buying-guide__breadcrumbs {
  padding-inline: 0;
  padding-block: 0 1rem;
  color: rgb(var(--v-theme-text-neutral-soft));
}

.buying-guide__kicker {
  display: flex;
  justify-content: flex-start;
}

.buying-guide__eyebrow {
  margin-bottom: 1rem;
}

.buying-guide__title {
  max-width: 34ch;
  margin: 0;
  font-size: clamp(1.9rem, 4vw, 3.45rem);
  font-weight: 700;
  line-height: 1.08;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.buying-guide__description {
  max-width: 64ch;
  margin-top: 1.25rem;
  font-size: 1.1rem;
  line-height: 1.65;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.buying-guide__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.625rem;
  margin-top: 1.25rem;
}

.buying-guide__hero-image {
  width: 100%;
  min-height: 180px;
  max-height: 320px;
  border-radius: 8px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12);
}

.buying-guide__container {
  padding-top: clamp(1.25rem, 3vw, 2.5rem);
  padding-bottom: clamp(2.5rem, 5vw, 4rem);
}

.buying-guide__layout {
  display: grid;
  grid-template-columns: minmax(11rem, 15rem) minmax(0, 74ch);
  gap: clamp(2rem, 4vw, 4rem);
  align-items: start;
  justify-content: center;
}

.buying-guide__toc-rail {
  min-width: 0;
}

.buying-guide__reader {
  min-width: 0;
  width: 100%;
}

.buying-guide__mobile-toc {
  margin-bottom: 1rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  border-radius: 8px;
  overflow: hidden;
}

.buying-guide__content {
  max-width: 74ch;
  text-align: left;
}

.buying-guide__content :deep(.buying-guide__markdown-title--duplicate) {
  display: none;
}

.buying-guide__content :deep(> h1:first-child) {
  display: none;
}

.buying-guide__empty {
  max-width: 76ch;
}

@media (max-width: 959px) {
  .buying-guide__hero-row {
    min-height: auto;
  }

  .buying-guide__title {
    max-width: 100%;
    font-size: clamp(1.7rem, 8vw, 2.6rem);
  }

  .buying-guide__hero-image {
    min-height: 160px;
  }

  .buying-guide__layout {
    display: block;
  }

  .buying-guide__breadcrumbs {
    font-size: 0.82rem;
  }

  .buying-guide__progress {
    top: 56px;
  }
}
</style>
