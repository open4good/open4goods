<template>
  <section :id="sectionId" class="product-vigilance">
    <header class="product-vigilance__header">
      <div class="product-vigilance__header-content">
        <h2 class="product-vigilance__title">
          {{ $t('product.vigilance.title') }}
        </h2>
        <p class="product-vigilance__subtitle">
          {{ $t('product.vigilance.subtitle') }}
        </p>
      </div>
    </header>

    <div class="product-vigilance__grid">
      <!-- End of Life Card -->
      <v-card
        v-if="isEndOfLife"
        class="product-vigilance__card product-vigilance__card--eol"
        color="warning"
        variant="tonal"
      >
        <v-card-text class="product-vigilance__card-body">
          <div class="product-vigilance__card-layout">
            <v-icon
              class="product-vigilance__card-icon"
              size="40"
              icon="mdi-alert-decagram-outline"
            />
            <div class="product-vigilance__card-content">
              <p class="product-vigilance__card-title">
                {{ $t('product.impact.endOfLifeTitle') }}
              </p>
              <p class="product-vigilance__card-description">
                {{ endOfLifeDescription }}
              </p>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- Conflicting Attributes Card -->
      <v-card
        v-if="hasConflictingAttributes"
        class="product-vigilance__card product-vigilance__card--conflicts"
        color="error"
        variant="tonal"
      >
        <v-card-text class="product-vigilance__card-body">
          <div class="product-vigilance__card-layout">
            <v-icon
              class="product-vigilance__card-icon"
              size="40"
              icon="mdi-alert-circle-outline"
            />
            <div class="product-vigilance__card-content">
              <p class="product-vigilance__card-title">
                {{ $t('product.vigilance.conflicts.title') }}
              </p>
              <p class="product-vigilance__card-description">
                {{ $t('product.vigilance.conflicts.description') }}
              </p>

              <v-table density="compact" class="mt-4 bg-transparent">
                <thead>
                  <tr>
                    <th>{{ $t('product.vigilance.conflicts.attribute') }}</th>
                    <th>{{ $t('product.vigilance.conflicts.values') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="attr in conflictingAttributes" :key="attr.name">
                    <td>{{ attr.name }}</td>
                    <td>
                      <div class="d-flex flex-wrap gap-2">
                        <!-- Simplified view of conflicting values -->
                        <v-chip
                          v-for="(source, idx) in getUniqueValues(attr)"
                          :key="idx"
                          size="x-small"
                          color="error"
                          variant="outlined"
                        >
                          {{ source }}
                        </v-chip>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </v-table>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- Data Quality Card -->
      <v-card
        v-if="isLowDataQuality"
        class="product-vigilance__card product-vigilance__card--quality"
        color="warning"
        variant="tonal"
      >
        <v-card-text class="product-vigilance__card-body">
          <div class="product-vigilance__card-layout">
            <v-icon
              class="product-vigilance__card-icon"
              size="40"
              icon="mdi-database-alert-outline"
            />
            <div class="product-vigilance__card-content">
              <p class="product-vigilance__card-title">
                {{ $t('product.vigilance.quality.title') }}
              </p>
              <p class="product-vigilance__card-description">
                {{
                  $t('product.vigilance.quality.description', {
                    score: dataQualityScoreValue,
                    avg: dataQualityAvg,
                  })
                }}
              </p>
            </div>
          </div>
        </v-card-text>
      </v-card>
      <!-- Competition Vigilance Card -->
      <v-card
        v-if="isLowCompetition"
        class="product-vigilance__card product-vigilance__card--competition"
        color="warning"
        variant="tonal"
      >
        <v-card-text class="product-vigilance__card-body">
          <div class="product-vigilance__card-layout">
            <v-icon
              class="product-vigilance__card-icon"
              size="40"
              icon="mdi-alert-outline"
            />
            <div class="product-vigilance__card-content">
              <p class="product-vigilance__card-title">
                {{
                  $t('product.price.competition.title', 'Niveau de concurrence')
                }}
              </p>
              <p class="product-vigilance__card-description">
                {{
                  $t(
                    'product.price.competition.lowDescription',
                    'Peu d’offres disponibles, la comparaison est limitée.'
                  )
                }}
              </p>
              <v-chip
                class="mt-2 align-self-start"
                size="small"
                color="warning"
                variant="outlined"
              >
                {{
                  $t('product.price.competition.count', '{count} offres', {
                    count: competitionCount,
                  })
                }}
                <span style="display: none" data-test-count>{{
                  competitionCount
                }}</span>
              </v-chip>
            </div>
          </div>
        </v-card-text>
      </v-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import { normalizeTimestamp } from '~/utils/date-parsing'
import type {
  ProductDto,
  ProductAttributeDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'

const props = defineProps({
  sectionId: {
    type: String,
    default: 'vigilance',
  },
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  onMarketEndDate: {
    type: [String, Number, Date] as PropType<
      string | number | Date | null | undefined
    >,
    default: null,
  },
})

const { t, locale } = useI18n()
const product = toRef(props, 'product')

// --- End of Life Logic ---
const supportStartDate = computed<Date | null>(() => {
  const normalized = normalizeTimestamp(props.onMarketEndDate)
  if (!normalized) return null
  const date = new Date(normalized)
  if (isNaN(date.getTime())) return null
  return date
})

const isEndOfLife = computed(() => {
  if (!supportStartDate.value) return false
  return supportStartDate.value < new Date()
})

const formattedSupportStartDate = computed(() => {
  if (!supportStartDate.value) return ''
  return format(supportStartDate.value, 'dd MMM yyyy', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const productBrand = computed(() => product.value.identity?.brand ?? '')

const endOfLifeDescription = computed(() => {
  if (!formattedSupportStartDate.value) {
    return t('product.impact.endOfLifeDescriptionFallback', {
      brand: productBrand.value,
    })
  }
  return t('product.impact.endOfLifeDescription', {
    brand: productBrand.value,
    onMarketEndDate: formattedSupportStartDate.value,
  })
})

// --- Conflicting Attributes Logic ---
const conflictingAttributes = computed(() => {
  const allAttributes = product.value.attributes?.allAttributes ?? {}
  return Object.values(allAttributes).filter(
    attr => attr.sourcing?.conflicts === true
  )
})

const hasConflictingAttributes = computed(
  () => conflictingAttributes.value.length > 0
)

const getUniqueValues = (attr: ProductAttributeDto) => {
  const sources = attr.sourcing?.sources
  if (!sources) return []

  // Extract values from sources map or array or set
  let values: string[] = []

  // Helper to extract value from a source object
  const extract = (s: ProductSourcedAttributeDto) =>
    s?.value ? String(s.value) : null

  if (Array.isArray(sources)) {
    values = sources.map(extract).filter((v): v is string => v !== null)
  } else if (sources instanceof Set) {
    values = Array.from(sources)
      .map(extract)
      .filter((v): v is string => v !== null)
  } else if (typeof sources === 'object') {
    values = Object.values(sources)
      .map(extract)
      .filter((v): v is string => v !== null)
  }

  return [...new Set(values)]
}

// --- Data Quality Logic ---
const dataQualityScore = computed(
  () => product.value.scores?.scores?.['DATA_QUALITY']
)

const dataQualityScoreValue = computed(() => dataQualityScore.value?.value ?? 0)
const dataQualityAvg = computed(
  () =>
    dataQualityScore.value?.relativ?.avg ??
    dataQualityScore.value?.absolute?.avg ??
    0
)

const isLowDataQuality = computed(() => {
  if (!dataQualityScore.value) return false
  // Alert if score is strictly lower than average
  return dataQualityScoreValue.value < dataQualityAvg.value
})

// --- Competition Vigilance Logic ---
const allOffers = computed(() => {
  const byCondition = product.value.offers?.offersByCondition ?? {}
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const list: any[] = []

  for (const [condition, offers] of Object.entries(byCondition)) {
    if (Array.isArray(offers)) {
      offers.forEach(offer => {
        list.push({ ...offer, condition })
      })
    }
  }

  return list
})

const isLowCompetition = computed(() => {
  const count = allOffers.value.length
  return count > 0 && count <= 2
})

const competitionCount = computed(() => allOffers.value.length)
</script>

<style scoped>
.product-vigilance {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-vigilance__header-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 60ch;
}

.product-vigilance__title {
  font-size: clamp(1.4rem, 2.5vw, 2rem);
  font-weight: 700;
  line-height: 1.1;
}

.product-vigilance__subtitle {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  line-height: 1.5;
}

.product-vigilance__grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-vigilance__card {
  border-radius: 16px;
  overflow: hidden;
}

.product-vigilance__card-body {
  padding: 1.5rem;
}

.product-vigilance__card-layout {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.product-vigilance__card-icon {
  flex-shrink: 0;
}

.product-vigilance__card--eol .product-vigilance__card-icon {
  color: rgba(var(--v-theme-warning));
}

.product-vigilance__card--conflicts .product-vigilance__card-icon {
  color: rgba(var(--v-theme-error));
}

.product-vigilance__card--quality .product-vigilance__card-icon {
  color: rgba(var(--v-theme-warning));
}

.product-vigilance__card-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-grow: 1;
}

.product-vigilance__card-title {
  font-size: 1.1em;
  font-weight: 700;
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-strong), 0.95);
}

.product-vigilance__card-description {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  line-height: 1.5;
}

.gap-2 {
  gap: 0.5rem;
}
</style>
