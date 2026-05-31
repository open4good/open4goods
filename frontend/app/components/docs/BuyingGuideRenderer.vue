<template>
  <article class="buying-guide">
    <v-fade-transition appear>
      <section class="buying-guide__hero">
        <v-container class="buying-guide__hero-container">
          <v-row align="center" class="buying-guide__hero-row">
            <v-col cols="12" md="7">
              <v-breadcrumbs
                :items="breadcrumbItems"
                class="buying-guide__breadcrumbs"
              />

              <v-chip
                color="primary"
                variant="tonal"
                size="small"
                class="buying-guide__eyebrow"
              >
                {{ t('buyingGuide.article.eyebrow') }}
              </v-chip>

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
                  v-for="tag in visibleTags"
                  :key="tag"
                  variant="tonal"
                  color="secondary"
                >
                  {{ tag }}
                </v-chip>
              </div>
            </v-col>

            <v-col cols="12" md="5">
              <v-slide-y-transition appear>
                <v-img
                  v-if="heroImage"
                  :src="heroImage"
                  :alt="categoryTitle"
                  cover
                  class="buying-guide__hero-image"
                />
              </v-slide-y-transition>
            </v-col>
          </v-row>
        </v-container>
      </section>
    </v-fade-transition>

    <v-container class="buying-guide__container">
      <v-row class="buying-guide__layout">
        <v-col
          v-if="tocItems.length"
          cols="12"
          md="3"
          class="d-none d-md-block"
        >
          <StickySectionNavigation
            :sections="tocSections"
            :active-section="activeSection"
            :aria-label="t('buyingGuide.article.tocAria')"
            @navigate="navigateToSection"
          />
        </v-col>

        <v-col cols="12" md="9" lg="8">
          <v-expansion-panels
            v-if="tocItems.length"
            variant="accordion"
            class="buying-guide__mobile-toc d-md-none"
          >
            <v-expansion-panel>
              <v-expansion-panel-title>
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
              v-ripple
              class="buying-guide__content docs-prose"
              role="article"
              :aria-label="t('docs.labels.article')"
            >
              <ContentRenderer :value="doc" :components="resolvedComponents" />
            </div>
          </SectionReveal>
        </v-col>
      </v-row>
    </v-container>
  </article>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'

import DocsContentLink from '~/components/docs/DocsContentLink.vue'
import SectionReveal from '~/components/shared/ui/SectionReveal.vue'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'
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

const categoryTitle = computed(() => props.guideContext.categoryTitle)
const heroImage = computed(() => props.guideContext.heroImage)

const breadcrumbItems = computed(() => [
  ...props.breadcrumbs.map(item => ({
    title: item.title ?? '',
    to: item.to ?? item.link,
    disabled: !item.to && !item.link,
  })),
  {
    title: props.doc.title ?? '',
    disabled: true,
  },
])

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

const resolvedComponents = computed(() => ({
  a: (contentProps: { href?: string; target?: string; rel?: string }) =>
    h(DocsContentLink, {
      ...contentProps,
      safeLinks: true,
    }),
}))
</script>

<style scoped>
.buying-guide {
  background: rgb(var(--v-theme-surface-default));
  color: rgb(var(--v-theme-text-neutral-strong));
}

.buying-guide__hero {
  background: linear-gradient(
    180deg,
    rgba(var(--v-theme-surface-primary-080), 0.82),
    rgba(var(--v-theme-surface-default), 1)
  );
}

.buying-guide__hero-container {
  padding-top: clamp(1rem, 3vw, 2rem);
  padding-bottom: clamp(1.5rem, 4vw, 3rem);
}

.buying-guide__hero-row {
  min-height: min(62vh, 620px);
}

.buying-guide__breadcrumbs {
  padding-inline: 0;
}

.buying-guide__eyebrow {
  margin-bottom: 1rem;
}

.buying-guide__title {
  max-width: 12ch;
  margin: 0;
  font-size: clamp(2.1rem, 4vw, 4.5rem);
  font-weight: 800;
  line-height: 1.02;
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
  min-height: 280px;
  max-height: 420px;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.16);
}

.buying-guide__container {
  padding-top: clamp(1.5rem, 4vw, 3rem);
  padding-bottom: clamp(2.5rem, 5vw, 4rem);
}

.buying-guide__layout {
  align-items: start;
}

.buying-guide__mobile-toc {
  margin-bottom: 1.25rem;
}

.buying-guide__content {
  max-width: 76ch;
}

.buying-guide__empty {
  max-width: 76ch;
}

@media (max-width: 959px) {
  .buying-guide__hero-row {
    min-height: auto;
  }

  .buying-guide__title {
    max-width: 14ch;
  }

  .buying-guide__hero-image {
    min-height: 220px;
  }
}
</style>
