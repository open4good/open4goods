<template>
  <div class="category-ecoscore" data-test="category-ecoscore">
    <CategoryHero
      v-if="category"
      :title="heroTitle"
      :description="heroDescription"
      :image="heroImage"
      :breadcrumbs="heroBreadcrumbs"
      :eyebrow="category.verticalMetaTitle"
    />

    <div v-if="category" class="category-ecoscore__content">
      <v-container fluid class="py-10">
        <div class="category-ecoscore__layout">
          <aside
            class="category-ecoscore__nav"
            :class="{ 'category-ecoscore__nav--mobile': orientation === 'horizontal' }"
            :aria-label="navAriaLabel"
          >
            <StickySectionNavigation
              data-test="sticky-navigation"
              :sections="navigationSections"
              :active-section="activeSection"
              :orientation="orientation"
              :aria-label="navAriaLabel"
              @navigate="scrollToSection"
            />
          </aside>

          <main class="category-ecoscore__sections" role="main">
            <section
              :id="sectionIds.overview"
              class="category-ecoscore__section"
              role="region"
              :aria-labelledby="`${sectionIds.overview}-title`"
            >
              <v-sheet class="category-ecoscore__surface" elevation="0" rounded="xl">
                <header class="category-ecoscore__header">
                  <span class="category-ecoscore__eyebrow">
                    {{ t('category.ecoscorePage.sections.overview.eyebrow') }}
                  </span>
                  <h2 :id="`${sectionIds.overview}-title`" class="category-ecoscore__title">
                    {{ t('category.ecoscorePage.sections.overview.title', { category: categoryLabel }) }}
                  </h2>
                </header>

                <div class="category-ecoscore__text-block">
                  <TextContent
                    class="category-ecoscore__wiki"
                    bloc-id="pages:impactscore-vertical-jumbotron"
                    :ipsum-length="220"
                  />
                </div>

                <v-card
                  class="category-ecoscore__intro-card"
                  elevation="1"
                  rounded="xl"
                  variant="elevated"
                >
                  <v-row align="stretch" class="ga-4" justify="center">
                    <v-col cols="12" md="7" class="d-flex flex-column justify-center">
                      <div class="category-ecoscore__intro-copy">
                        <p class="category-ecoscore__intro-eyebrow">
                          {{ t('category.ecoscorePage.sections.overview.card.subtitle', { category: categoryLabel }) }}
                        </p>
                        <h3 class="category-ecoscore__intro-title">
                          {{ t('category.ecoscorePage.sections.overview.card.title') }}
                        </h3>
                        <p class="category-ecoscore__intro-description">
                          {{ t('category.ecoscorePage.sections.overview.card.description', { category: categoryLabel }) }}
                        </p>
                        <v-btn
                          class="category-ecoscore__intro-cta"
                          color="primary"
                          size="large"
                          variant="flat"
                          :to="{ path: '/impact-score' }"
                          :aria-label="t('category.ecoscorePage.sections.overview.card.aria')"
                        >
                          <v-icon class="me-2" icon="mdi-compass-outline" size="20" />
                          {{ t('category.ecoscorePage.sections.overview.card.cta') }}
                        </v-btn>
                      </div>
                    </v-col>
                    <v-col cols="12" md="5" class="d-flex justify-center">
                      <v-img
                        v-if="overviewIllustration"
                        :src="overviewIllustration"
                        :alt="t('category.ecoscorePage.sections.overview.card.imageAlt', { category: categoryLabel })"
                        class="category-ecoscore__intro-image"
                        cover
                      />
                    </v-col>
                  </v-row>
                </v-card>
              </v-sheet>
            </section>

            <section
              :id="sectionIds.purpose"
              class="category-ecoscore__section"
              role="region"
              :aria-labelledby="`${sectionIds.purpose}-title`"
            >
              <v-sheet class="category-ecoscore__surface" elevation="0" rounded="xl">
                <header class="category-ecoscore__header">
                  <span class="category-ecoscore__eyebrow">
                    {{ t('category.ecoscorePage.sections.purpose.eyebrow') }}
                  </span>
                  <h2 :id="`${sectionIds.purpose}-title`" class="category-ecoscore__title">
                    {{ t('category.ecoscorePage.sections.purpose.title', { category: categoryLabel }) }}
                  </h2>
                </header>

                <v-row class="category-ecoscore__purpose-grid" align="stretch" justify="center">
                  <v-col cols="12" md="10">
                    <v-card class="category-ecoscore__info-card" elevation="0" rounded="xl">
                      <h3 class="category-ecoscore__info-title">
                        {{ t('category.ecoscorePage.sections.purpose.objectiveTitle') }}
                      </h3>
                      <p class="category-ecoscore__info-body">
                        {{ purposeText ?? t('category.ecoscorePage.sections.purpose.objectiveFallback') }}
                      </p>
                    </v-card>
                  </v-col>

                  <v-col cols="12" md="10">
                    <v-card class="category-ecoscore__info-card" elevation="0" rounded="xl">
                      <h3 class="category-ecoscore__info-title">
                        {{ t('category.ecoscorePage.sections.purpose.dataTitle') }}
                      </h3>
                      <p class="category-ecoscore__info-body">
                        {{ availableDataText ?? t('category.ecoscorePage.sections.purpose.dataFallback') }}
                      </p>

                      <v-divider class="my-4" role="presentation" />

                      <ul class="category-ecoscore__data-list">
                        <li
                          v-for="criterion in availableCriteria"
                          :key="criterion.key"
                          class="category-ecoscore__data-item"
                        >
                          <strong>{{ criterion.label }}</strong>
                          <span class="category-ecoscore__data-description">{{ criterion.description }}</span>
                        </li>
                      </ul>
                    </v-card>
                  </v-col>
                </v-row>
              </v-sheet>
            </section>

            <section
              :id="sectionIds.criteria"
              class="category-ecoscore__section"
              role="region"
              :aria-labelledby="`${sectionIds.criteria}-title`"
            >
              <v-sheet class="category-ecoscore__surface" elevation="0" rounded="xl">
                <header class="category-ecoscore__header">
                  <span class="category-ecoscore__eyebrow">
                    {{ t('category.ecoscorePage.sections.criteria.eyebrow') }}
                  </span>
                  <h2 :id="`${sectionIds.criteria}-title`" class="category-ecoscore__title">
                    {{ t('category.ecoscorePage.sections.criteria.title', { category: categoryLabel }) }}
                  </h2>
                </header>

                <div class="category-ecoscore__text-block">
                  <TextContent
                    class="category-ecoscore__wiki"
                    bloc-id="pages:impactscore-vertical-criterias"
                    :ipsum-length="240"
                  />
                </div>

                <v-row v-if="criteriaCards.length" class="category-ecoscore__criteria-grid" dense>
                  <v-col
                    v-for="criterion in criteriaCards"
                    :key="criterion.key"
                    cols="12"
                    md="6"
                    lg="4"
                  >
                    <article class="category-ecoscore__criteria-card" data-test="impact-criteria-card">
                      <div class="category-ecoscore__criteria-icon" aria-hidden="true">
                        <v-icon v-if="criterion.icon" :icon="criterion.icon" size="26" />
                        <span v-else>{{ criterion.fallback }}</span>
                      </div>
                      <div class="category-ecoscore__criteria-content">
                        <h3 class="category-ecoscore__criteria-title">{{ criterion.label }}</h3>
                        <p v-if="criterion.description" class="category-ecoscore__criteria-description">
                          {{ criterion.description }}
                        </p>
                        <p v-if="criterion.coefficient !== null" class="category-ecoscore__criteria-coefficient">
                          {{ formatPercentage(criterion.coefficient) }}
                        </p>
                      </div>
                    </article>
                  </v-col>
                </v-row>

                <p v-else class="category-ecoscore__empty">
                  {{ t('category.ecoscorePage.sections.criteria.empty') }}
                </p>
              </v-sheet>
            </section>

            <section
              :id="sectionIds.transparency"
              class="category-ecoscore__section"
              role="region"
              :aria-labelledby="`${sectionIds.transparency}-title`"
            >
              <v-sheet class="category-ecoscore__surface" elevation="0" rounded="xl">
                <header class="category-ecoscore__header">
                  <span class="category-ecoscore__eyebrow">
                    {{ t('category.ecoscorePage.sections.transparency.eyebrow') }}
                  </span>
                  <h2 :id="`${sectionIds.transparency}-title`" class="category-ecoscore__title">
                    {{ t('category.ecoscorePage.sections.transparency.title') }}
                  </h2>
                </header>

                <v-row class="category-ecoscore__critical-grid" align="stretch">
                  <v-col cols="12" lg="8">
                    <v-card class="category-ecoscore__critical-card" elevation="0" rounded="xl">
                      <div class="category-ecoscore__critical-media">
                        <v-img
                          :src="transparencyIllustration"
                          alt=""
                          class="category-ecoscore__critical-image"
                          cover
                        />
                      </div>
                      <div class="category-ecoscore__critical-body">
                        <h3 class="category-ecoscore__critical-title">
                          {{ t('category.ecoscorePage.sections.transparency.criticalReviewTitle') }}
                        </h3>
                        <p class="category-ecoscore__critical-text">
                          {{ criticalReviewText ?? t('category.ecoscorePage.sections.transparency.criticalReviewFallback') }}
                        </p>
                      </div>
                    </v-card>
                  </v-col>

                  <v-col cols="12" lg="4">
                    <v-card class="category-ecoscore__community-card" elevation="0" rounded="xl">
                      <h3 class="category-ecoscore__community-title">
                        {{ t('category.ecoscorePage.sections.transparency.communityTitle') }}
                      </h3>
                      <p class="category-ecoscore__community-text">
                        {{ t('category.ecoscorePage.sections.transparency.communityBody', { category: categoryLabel }) }}
                      </p>
                      <div class="category-ecoscore__community-actions">
                        <v-btn
                          v-if="githubConfigUrl"
                          class="mb-2"
                          color="primary"
                          variant="tonal"
                          :href="githubConfigUrl"
                          rel="noopener"
                          target="_blank"
                        >
                          <v-icon class="me-2" icon="mdi-github" size="18" />
                          {{ t('category.ecoscorePage.sections.transparency.communityCta') }}
                        </v-btn>
                        <v-btn
                          color="secondary"
                          variant="text"
                          href="https://github.com/open4good/open4goods/issues/new"
                          rel="noopener"
                          target="_blank"
                        >
                          {{ t('category.ecoscorePage.sections.transparency.communityIssues') }}
                        </v-btn>
                      </div>
                    </v-card>
                  </v-col>
                </v-row>

                <div class="category-ecoscore__transparency-intro">
                  <p class="category-ecoscore__transparency-text">
                    {{ t('category.ecoscorePage.sections.transparency.intro') }}
                    <NuxtLink to="/opensource" class="category-ecoscore__inline-link">
                      {{ t('category.ecoscorePage.sections.transparency.openSourceLink') }}
                    </NuxtLink>
                    {{ t('category.ecoscorePage.sections.transparency.connector') }}
                    <NuxtLink to="/opendata" class="category-ecoscore__inline-link">
                      {{ t('category.ecoscorePage.sections.transparency.openDataLink') }}
                    </NuxtLink>
                    {{ t('category.ecoscorePage.sections.transparency.introSuffix', { category: categoryLabel }) }}
                  </p>
                </div>

                <div class="category-ecoscore__table-wrapper">
                  <h3 class="category-ecoscore__table-title">
                    {{ t('category.ecoscorePage.sections.transparency.tableTitle') }}
                  </h3>
                  <p :id="`${sectionIds.transparency}-table-helper`" class="category-ecoscore__table-helper">
                    {{ t('category.ecoscorePage.sections.transparency.tableHelper') }}
                  </p>

                  <v-table
                    v-if="coefficientComparison.length"
                    class="category-ecoscore__table"
                    density="comfortable"
                    :aria-describedby="`${sectionIds.transparency}-table-helper`"
                    data-test="comparison-table"
                  >
                    <thead>
                      <tr>
                        <th scope="col" class="text-left">
                          {{ t('category.ecoscorePage.sections.transparency.tableHeaders.name') }}
                        </th>
                        <th scope="col" class="text-left">
                          {{ t('category.ecoscorePage.sections.transparency.tableHeaders.proposed') }}
                        </th>
                        <th scope="col" class="text-left">
                          {{ t('category.ecoscorePage.sections.transparency.tableHeaders.applied') }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="row in coefficientComparison" :key="row.key">
                        <th scope="row">{{ row.label }}</th>
                        <td>{{ formatDecimal(row.proposed) }}</td>
                        <td>{{ formatDecimal(row.applied) }}</td>
                      </tr>
                    </tbody>
                  </v-table>

                  <p v-else class="category-ecoscore__empty">
                    {{ t('category.ecoscorePage.sections.transparency.tableFallback') }}
                  </p>
                </div>
              </v-sheet>
            </section>

            <section
              :id="sectionIds.aiAudit"
              class="category-ecoscore__section"
              role="region"
              :aria-labelledby="`${sectionIds.aiAudit}-title`"
            >
              <v-sheet class="category-ecoscore__surface" elevation="0" rounded="xl">
                <header class="category-ecoscore__header">
                  <span class="category-ecoscore__eyebrow">
                    {{ t('category.ecoscorePage.sections.aiAudit.eyebrow') }}
                  </span>
                  <h2 :id="`${sectionIds.aiAudit}-title`" class="category-ecoscore__title">
                    {{ t('category.ecoscorePage.sections.aiAudit.title') }}
                  </h2>
                </header>

                <p class="category-ecoscore__ai-helper">
                  {{ t('category.ecoscorePage.sections.aiAudit.intro', { category: categoryLabel }) }}
                </p>

                <v-row class="category-ecoscore__ai-grid" align="stretch">
                  <v-col cols="12" lg="6">
                    <v-card class="category-ecoscore__code-card" elevation="0" rounded="xl">
                      <h3 class="category-ecoscore__code-title">
                        {{ t('category.ecoscorePage.sections.aiAudit.promptTitle') }}
                      </h3>
                      <p class="category-ecoscore__code-helper">
                        {{ t('category.ecoscorePage.sections.aiAudit.promptHelper') }}
                      </p>
                      <pre
                        class="category-ecoscore__code-block"
                        data-test="ai-yaml"
                        role="region"
                        aria-live="polite"
                      >
{{ formattedYamlPrompt ?? t('category.ecoscorePage.sections.aiAudit.yamlUnavailable') }}
                      </pre>
                    </v-card>
                  </v-col>

                  <v-col cols="12" lg="6">
                    <v-card class="category-ecoscore__code-card" elevation="0" rounded="xl">
                      <h3 class="category-ecoscore__code-title">
                        {{ t('category.ecoscorePage.sections.aiAudit.responseTitle') }}
                      </h3>
                      <p class="category-ecoscore__code-helper">
                        {{ t('category.ecoscorePage.sections.aiAudit.responseHelper') }}
                      </p>
                      <pre
                        class="category-ecoscore__code-block"
                        data-test="ai-json"
                        role="region"
                        aria-live="polite"
                      >
{{ formattedAiJson ?? t('category.ecoscorePage.sections.aiAudit.jsonUnavailable') }}
                      </pre>
                    </v-card>
                  </v-col>
                </v-row>
              </v-sheet>
            </section>
          </main>
        </div>
      </v-container>
    </div>

    <v-container v-else fluid class="py-10">
      <v-skeleton-loader type="image, article" />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import type {
  AttributeConfigDto,
  CategoryBreadcrumbItemDto,
  ImpactScoreCriteriaDto,
  ImpactScoreConfigDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'
import CategoryHero from '~/components/category/CategoryHero.vue'
import TextContent from '~/components/domains/content/TextContent.vue'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'
import { createError, useRequestURL, useRoute, useSeoMeta } from '#imports'
import { useCategories } from '~/composables/categories/useCategories'

const route = useRoute()
const requestURL = useRequestURL()
const { t, locale } = useI18n()
const display = useDisplay()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Category not found' })
}

const { selectCategoryBySlug } = useCategories()
const category = ref<VerticalConfigFullDto | null>(null)

try {
  category.value = await selectCategoryBySlug(categorySlug.value)
} catch (error) {
  if (error instanceof Error && error.name === 'CategoryNotFoundError') {
    throw createError({ statusCode: 404, statusMessage: 'Category not found', cause: error })
  }

  console.error('Failed to resolve category for ecoscore page', error)
  throw createError({ statusCode: 500, statusMessage: 'Failed to load category', cause: error })
}

const siteName = computed(() => String(t('siteIdentity.siteName')))

const heroTitle = computed(() => category.value?.verticalHomeTitle ?? siteName.value)
const heroDescription = computed(() => category.value?.verticalHomeDescription ?? null)
const heroImage = computed(() => {
  if (!category.value) {
    return null
  }

  return (
    category.value.imageMedium ??
    category.value.imageLarge ??
    category.value.imageSmall ??
    null
  )
})

const categoryLabel = computed(
  () => category.value?.verticalHomeTitle ?? category.value?.verticalMetaTitle ?? siteName.value,
)

const heroBreadcrumbs = computed<CategoryBreadcrumbItemDto[]>(() => {
  const base = (category.value?.breadCrumb ?? []).map((item) => ({ ...item }))
  const leafTitle = t('category.ecoscorePage.breadcrumbLeaf')

  return leafTitle ? [...base, { title: leafTitle }] : base
})

const impactScoreConfig = computed<ImpactScoreConfigDto | null>(
  () => category.value?.impactScoreConfig ?? null,
)

const impactScoreTexts = computed(() => impactScoreConfig.value?.texts ?? {})

const purposeText = computed(() => impactScoreTexts.value?.purpose?.trim() || null)
const availableDataText = computed(() => impactScoreTexts.value?.availlableDatas?.trim() || null)
const criticalReviewText = computed(() => impactScoreTexts.value?.criticalReview?.trim() || null)

const availableImpactScoreCriterias = computed<Record<string, ImpactScoreCriteriaDto>>(
  () => category.value?.availableImpactScoreCriterias ?? {},
)

const attributeConfigs = computed<AttributeConfigDto[]>(
  () => category.value?.attributesConfig?.configs ?? [],
)

const attributeMap = computed(() => {
  return attributeConfigs.value.reduce((map, attribute) => {
    if (attribute.key) {
      map.set(attribute.key, attribute)
    }
    return map
  }, new Map<string, AttributeConfigDto>())
})

const availableCriteria = computed(() => {
  return Object.entries(availableImpactScoreCriterias.value).map(([key, criterion]) => {
    const attribute = attributeMap.value.get(key)
    return {
      key,
      label: attribute?.name ?? criterion.title ?? key,
      description: criterion.description ?? '',
    }
  })
})

const criteriaCards = computed(() => {
  const weights = impactScoreConfig.value?.criteriasPonderation ?? {}
  const analysis = impactScoreTexts.value?.criteriasAnalysis ?? {}
  const available = availableImpactScoreCriterias.value
  const keys = new Set<string>([
    ...Object.keys(weights),
    ...Object.keys(analysis),
    ...Object.keys(available ?? {}),
  ])

  return Array.from(keys).map((key) => {
    const attribute = attributeMap.value.get(key)
    const fallbackTitle = available?.[key]?.title ?? key
    const description = analysis?.[key]?.trim() || available?.[key]?.description || ''
    const coefficient = typeof weights?.[key] === 'number' ? Number(weights?.[key]) : null

    return {
      key,
      label: attribute?.name ?? fallbackTitle,
      description,
      coefficient,
      icon: attribute?.icon ?? null,
      fallback: fallbackTitle.charAt(0).toUpperCase(),
    }
  })
})

const overviewIllustration = computed(() => {
  if (!category.value?.id) {
    return heroImage.value
  }
  return `/images/verticals/${category.value.id}.jpg`
})

const transparencyIllustration = computed(() => '/icons/categories/ecoscore/revue_critique.jpg')

const rawYamlPrompt = computed(() => impactScoreConfig.value?.yamlPrompt?.trim() || '')
const formattedYamlPrompt = computed(() => (rawYamlPrompt.value.length ? rawYamlPrompt.value : null))

const rawAiJson = computed(() => impactScoreConfig.value?.aiJsonResponse?.trim() || '')
const parsedAiJson = computed<Record<string, unknown> | null>(() => {
  if (!rawAiJson.value.length) {
    return null
  }

  try {
    return JSON.parse(rawAiJson.value)
  } catch (error) {
    console.warn('Unable to parse AI JSON response for impact score', error)
    return null
  }
})

const formattedAiJson = computed(() => {
  if (parsedAiJson.value) {
    return JSON.stringify(parsedAiJson.value, null, 2)
  }
  return rawAiJson.value.length ? rawAiJson.value : null
})

const aiCoefficients = computed<Record<string, number>>(() => {
  const response = parsedAiJson.value
  if (!response || typeof response !== 'object') {
    return {}
  }

  const coefficients = (response as Record<string, unknown>)?.criteriasPonderation
  if (coefficients && typeof coefficients === 'object') {
    return Object.fromEntries(
      Object.entries(coefficients as Record<string, unknown>).filter(([, value]) => typeof value === 'number'),
    )
  }

  return {}
})

const coefficientComparison = computed(() => {
  const applied = impactScoreConfig.value?.criteriasPonderation ?? {}
  const proposed = aiCoefficients.value ?? {}
  const available = availableImpactScoreCriterias.value
  const keys = new Set<string>([
    ...Object.keys(applied ?? {}),
    ...Object.keys(proposed ?? {}),
  ])

  return Array.from(keys).map((key) => {
    const attribute = attributeMap.value.get(key)
    const fallbackTitle = available?.[key]?.title ?? key
    return {
      key,
      label: attribute?.name ?? fallbackTitle,
      proposed: typeof proposed?.[key] === 'number' ? Number(proposed?.[key]) : null,
      applied: typeof applied?.[key] === 'number' ? Number(applied?.[key]) : null,
    }
  })
})

const navAriaLabel = computed(() => t('category.ecoscorePage.navigation.ariaLabel'))

const sectionIds = {
  overview: 'category-impact-overview',
  purpose: 'category-impact-purpose',
  criteria: 'category-impact-criteria',
  transparency: 'category-impact-transparency',
  aiAudit: 'category-impact-ai-audit',
} as const

type SectionId = (typeof sectionIds)[keyof typeof sectionIds]

const navigationSections = computed(() => [
  {
    id: sectionIds.overview as SectionId,
    label: t('category.ecoscorePage.navigation.overview'),
    icon: 'mdi-information-outline',
  },
  {
    id: sectionIds.purpose as SectionId,
    label: t('category.ecoscorePage.navigation.purpose'),
    icon: 'mdi-bullseye-arrow',
  },
  {
    id: sectionIds.criteria as SectionId,
    label: t('category.ecoscorePage.navigation.criteria'),
    icon: 'mdi-format-list-bulleted',
  },
  {
    id: sectionIds.transparency as SectionId,
    label: t('category.ecoscorePage.navigation.transparency'),
    icon: 'mdi-shield-check-outline',
  },
  {
    id: sectionIds.aiAudit as SectionId,
    label: t('category.ecoscorePage.navigation.aiAudit'),
    icon: 'mdi-robot-outline',
  },
])

const orientation = computed<'vertical' | 'horizontal'>(() => (display.mdAndDown.value ? 'horizontal' : 'vertical'))

const activeSection = ref<SectionId>(sectionIds.overview)
const observer = ref<IntersectionObserver | null>(null)
const visibleSectionRatios = new Map<string, number>()
const MIN_SECTION_RATIO = 0.35

const refreshActiveSection = () => {
  if (!visibleSectionRatios.size) {
    activeSection.value = navigationSections.value[0]?.id ?? sectionIds.overview
    return
  }

  const sorted = [...visibleSectionRatios.entries()].sort((a, b) => b[1] - a[1])
  const [nextActive] = sorted.find(([, ratio]) => ratio >= MIN_SECTION_RATIO) ?? sorted[0] ?? []

  if (nextActive) {
    activeSection.value = nextActive as SectionId
  }
}

const observeSections = () => {
  if (!import.meta.client) {
    return
  }

  observer.value?.disconnect()
  visibleSectionRatios.clear()
  refreshActiveSection()

  observer.value = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        const ratio = entry.intersectionRatio
        if (ratio > 0) {
          visibleSectionRatios.set(entry.target.id, ratio)
        } else {
          visibleSectionRatios.delete(entry.target.id)
        }
      })

      refreshActiveSection()
    },
    {
      rootMargin: '-15% 0px -35% 0px',
      threshold: Array.from({ length: 11 }, (_, index) => index / 10),
    },
  )

  nextTick(() => {
    navigationSections.value.forEach((section) => {
      const element = document.getElementById(section.id)
      if (element) {
        observer.value?.observe(element)
      }
    })
  })
}

onMounted(() => {
  observeSections()
})

onBeforeUnmount(() => {
  observer.value?.disconnect()
  visibleSectionRatios.clear()
})

watch(orientation, () => {
  nextTick(() => {
    observeSections()
  })
})

watch(
  () => navigationSections.value.map((section) => `${section.id}-${section.label}`).join('|'),
  () => {
    nextTick(() => {
      observeSections()
    })
  },
)

const scrollToSection = (sectionId: string) => {
  if (!import.meta.client && typeof window === 'undefined') {
    return
  }

  const element = document.getElementById(sectionId)
  if (!element) {
    return
  }

  activeSection.value = sectionId as SectionId

  const offset = orientation.value === 'horizontal' ? 96 : 120
  const top = element.getBoundingClientRect().top + window.scrollY - offset
  window.scrollTo({ top, behavior: 'smooth' })
}

const githubConfigUrl = computed(() =>
  category.value?.id
    ? `https://github.com/open4good/open4goods/blob/main/verticals/src/main/resources/verticals/${category.value.id}.yml`
    : null,
)

function formatPercentage(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  return `${Math.round(value * 100)}%`
}

function formatDecimal(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  return value.toFixed(2)
}

const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const seoTitle = computed(() => t('category.ecoscorePage.seo.title', { category: categoryLabel.value }))
const seoDescription = computed(() => {
  return (
    purposeText.value ??
    availableDataText.value ??
    category.value?.verticalMetaDescription ??
    category.value?.verticalHomeDescription ??
    t('category.ecoscorePage.seo.description', { category: categoryLabel.value })
  )
})

const ogImage = computed(() => {
  if (!heroImage.value) {
    return undefined
  }

  try {
    return new URL(heroImage.value, requestURL.origin).toString()
  } catch (error) {
    console.error('Invalid hero image URL', error)
    return undefined
  }
})

const ogLocale = computed(() => locale.value.replace('-', '_'))

useSeoMeta({
  title: () => seoTitle.value,
  description: () => seoDescription.value,
  ogTitle: () => seoTitle.value,
  ogDescription: () => seoDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogImage: () => ogImage.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => categoryLabel.value,
})
</script>

<style scoped lang="sass">
.category-ecoscore
  display: flex
  flex-direction: column
  gap: clamp(2rem, 3vw, 3rem)
  background: rgb(var(--v-theme-surface-muted))
  color: rgb(var(--v-theme-text-neutral-strong))

.category-ecoscore__content
  width: 100%

.category-ecoscore__layout
  display: grid
  grid-template-columns: minmax(0, 1fr)
  gap: clamp(2rem, 4vw, 3rem)

.category-ecoscore__nav
  position: relative

.category-ecoscore__sections
  display: flex
  flex-direction: column
  gap: clamp(2.5rem, 4vw, 3.5rem)

.category-ecoscore__section
  scroll-margin-top: 140px

.category-ecoscore__surface
  background: rgb(var(--v-theme-surface-default))
  padding: clamp(1.5rem, 2.5vw, 2.75rem)
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.1)

.category-ecoscore__header
  display: flex
  flex-direction: column
  gap: 0.5rem
  margin-bottom: clamp(1.5rem, 3vw, 2.5rem)

.category-ecoscore__eyebrow
  display: inline-flex
  align-items: center
  gap: 0.5rem
  padding: 0.25rem 0.75rem
  border-radius: 999px
  background: rgba(var(--v-theme-surface-primary-100), 0.9)
  color: rgb(var(--v-theme-accent-primary-highlight))
  font-size: 0.75rem
  letter-spacing: 0.08em
  text-transform: uppercase

.category-ecoscore__title
  margin: 0
  font-size: clamp(1.65rem, 1.5vw + 1.1rem, 2.25rem)
  line-height: 1.2
  font-weight: 700

.category-ecoscore__text-block
  margin-bottom: clamp(1.5rem, 3vw, 2.5rem)

.category-ecoscore__wiki :deep(.xwiki-sandbox)
  font-size: 1.02rem
  line-height: 1.7
  color: rgba(var(--v-theme-text-neutral-strong), 0.92)

.category-ecoscore__intro-card
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-050), 0.75), rgba(var(--v-theme-surface-glass), 0.95))
  padding: clamp(1.5rem, 2vw, 2.5rem)

.category-ecoscore__intro-eyebrow
  font-size: 0.85rem
  text-transform: uppercase
  letter-spacing: 0.08em
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.category-ecoscore__intro-title
  margin: 0.25rem 0
  font-size: clamp(1.4rem, 1.2vw + 1rem, 1.85rem)
  font-weight: 700

.category-ecoscore__intro-description
  margin-bottom: 1.25rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.96)

.category-ecoscore__intro-image
  max-width: min(320px, 100%)
  border-radius: 24px
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12)

.category-ecoscore__purpose-grid
  gap: clamp(1.5rem, 3vw, 2rem)

.category-ecoscore__info-card
  background: rgba(var(--v-theme-surface-muted), 0.85)
  padding: clamp(1.25rem, 2vw, 2rem)
  height: 100%

.category-ecoscore__info-title
  margin: 0 0 0.75rem
  font-size: 1.15rem
  font-weight: 600

.category-ecoscore__info-body
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  line-height: 1.6

.category-ecoscore__data-list
  list-style: none
  display: flex
  flex-direction: column
  gap: 0.85rem
  padding: 0
  margin: 0

.category-ecoscore__data-item
  display: flex
  flex-direction: column
  gap: 0.35rem

.category-ecoscore__data-description
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)
  line-height: 1.5

.category-ecoscore__criteria-grid
  gap: clamp(1.25rem, 2vw, 2rem)

.category-ecoscore__criteria-card
  display: flex
  gap: 1rem
  background: rgba(var(--v-theme-surface-glass), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12)
  border-radius: 18px
  padding: 1.25rem
  height: 100%

.category-ecoscore__criteria-icon
  display: inline-flex
  align-items: center
  justify-content: center
  width: 48px
  height: 48px
  border-radius: 16px
  background: rgba(var(--v-theme-surface-primary-100), 0.9)
  color: rgb(var(--v-theme-accent-primary-highlight))
  font-weight: 600

.category-ecoscore__criteria-title
  margin: 0 0 0.5rem
  font-size: 1.05rem
  font-weight: 600

.category-ecoscore__criteria-description
  margin: 0 0 0.5rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  line-height: 1.5

.category-ecoscore__criteria-coefficient
  margin: 0
  font-weight: 700
  color: rgb(var(--v-theme-accent-supporting))

.category-ecoscore__empty
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)
  font-style: italic

.category-ecoscore__critical-grid
  gap: clamp(1.5rem, 3vw, 2rem)

.category-ecoscore__critical-card
  display: flex
  flex-direction: row
  gap: 1.5rem
  padding: clamp(1.25rem, 2vw, 2rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.1)
  height: 100%

.category-ecoscore__critical-media
  flex: 0 0 clamp(140px, 12vw, 180px)
  border-radius: 18px
  overflow: hidden

.category-ecoscore__critical-image
  width: 100%
  height: 100%

.category-ecoscore__critical-title
  margin: 0 0 0.75rem
  font-size: 1.2rem
  font-weight: 600

.category-ecoscore__critical-text
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  line-height: 1.6

.category-ecoscore__community-card
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.5rem, 2vw, 2rem)
  background: rgba(var(--v-theme-surface-muted), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12)
  height: 100%

.category-ecoscore__community-title
  margin: 0
  font-size: 1.2rem
  font-weight: 600

.category-ecoscore__community-text
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  line-height: 1.6

.category-ecoscore__community-actions
  display: flex
  flex-direction: column
  gap: 0.5rem

.category-ecoscore__transparency-intro
  margin-top: clamp(1.5rem, 2.5vw, 2.5rem)

.category-ecoscore__transparency-text
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.92)
  line-height: 1.6

.category-ecoscore__inline-link
  color: rgb(var(--v-theme-accent-primary-highlight))
  text-decoration: underline

.category-ecoscore__table-wrapper
  margin-top: clamp(1.5rem, 2.5vw, 2.5rem)
  background: rgba(var(--v-theme-surface-glass), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.08)
  border-radius: 18px
  padding: clamp(1.25rem, 2vw, 2rem)

.category-ecoscore__table-title
  margin: 0 0 0.75rem
  font-size: 1.1rem
  font-weight: 600

.category-ecoscore__table-helper
  margin: 0 0 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)

.category-ecoscore__table :deep(table)
  width: 100%
  border-collapse: collapse

.category-ecoscore__table :deep(th),
.category-ecoscore__table :deep(td)
  padding: 0.75rem
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.1)

.category-ecoscore__ai-helper
  margin: 0 0 1.5rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.88)

.category-ecoscore__ai-grid
  gap: clamp(1.5rem, 3vw, 2.25rem)

.category-ecoscore__code-card
  display: flex
  flex-direction: column
  gap: 0.75rem
  padding: clamp(1.25rem, 2vw, 2rem)
  background: rgba(var(--v-theme-surface-muted), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12)
  height: 100%

.category-ecoscore__code-title
  margin: 0
  font-size: 1.1rem
  font-weight: 600

.category-ecoscore__code-helper
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.86)

.category-ecoscore__code-block
  flex: 1 1 auto
  margin: 0
  padding: 1rem
  border-radius: 12px
  background: rgba(var(--v-theme-surface-default), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.14)
  font-family: 'JetBrains Mono', 'Fira Code', 'SFMono-Regular', Consolas, 'Liberation Mono', monospace
  font-size: 0.85rem
  line-height: 1.6
  color: rgba(var(--v-theme-text-neutral-strong), 0.92)
  white-space: pre-wrap
  word-break: break-word
  overflow: auto

@media (min-width: 1280px)
  .category-ecoscore__layout
    grid-template-columns: 280px minmax(0, 1fr)

  .category-ecoscore__nav
    position: sticky
    top: 112px

  .category-ecoscore__section
    scroll-margin-top: 140px

@media (max-width: 960px)
  .category-ecoscore__surface
    padding: clamp(1.25rem, 4vw, 2rem)

  .category-ecoscore__section
    scroll-margin-top: 88px

  .category-ecoscore__critical-card
    flex-direction: column

  .category-ecoscore__critical-media
    width: 100%

  .category-ecoscore__intro-card
    padding: clamp(1rem, 4vw, 2rem)

  .category-ecoscore__intro-image
    max-width: 220px

  .category-ecoscore__code-block
    max-height: 320px
    font-size: 0.82rem

@media (prefers-reduced-motion: reduce)
  .category-ecoscore *
    transition-duration: 0.01ms !important
    animation-duration: 0.01ms !important
</style>
