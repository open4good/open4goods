<template>
  <section :id="sectionId" class="product-vigilance">
    <header class="product-vigilance__header">
      <div class="product-vigilance__header-content">
        <h2 class="product-vigilance__title">
          {{ t('product.vigilance.title') }}
        </h2>
        <p
          class="product-vigilance__subtitle"
          v-html="sanitize(t('product.vigilance.subtitle'))"
        />
      </div>
    </header>

    <v-row justify="center">
      <!-- End of Life Card -->
      <v-col v-if="isEndOfLife" cols="12" :md="cardColumnSize">
        <v-card
          class="product-vigilance__card product-vigilance__card--eol h-100"
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
                  {{ t('product.impact.endOfLifeTitle') }}
                </p>
                <p
                  class="product-vigilance__card-description"
                  v-html="sanitize(endOfLifeDescription)"
                />

                <div
                  v-if="minAttributes.length"
                  class="product-vigilance__min-attributes"
                >
                  <div
                    v-for="attr in minAttributes"
                    :key="attr.name"
                    class="product-vigilance__min-attribute-item"
                  >
                    <span class="product-vigilance__min-attribute-label">
                      {{ attr.name }}
                    </span>
                    <ProductAttributeSourcingLabel
                      :sourcing="attr.sourcing"
                      :enable-tooltip="true"
                    />
                  </div>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Conflicting Attributes Card -->
      <v-col v-if="hasConflictingAttributes" cols="12" :md="cardColumnSize">
        <v-card
          class="product-vigilance__card product-vigilance__card--conflicts h-100"
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
                  {{ t('product.vigilance.conflicts.title') }}
                </p>
                <p
                  class="product-vigilance__card-description"
                  v-html="
                    sanitize(t('product.vigilance.conflicts.description'))
                  "
                />

                <div class="product-vigilance__conflicts-list">
                  <div
                    v-for="attr in displayedConflictingAttributes"
                    :key="attr.name"
                    class="product-vigilance__conflict-item"
                  >
                    <ProductAttributeSourcingLabel
                      :sourcing="attr.sourcing"
                      :enable-tooltip="true"
                    >
                      {{ attr.name }}
                    </ProductAttributeSourcingLabel>
                  </div>
                </div>

                <v-btn
                  v-if="conflictingAttributes.length > 10"
                  variant="text"
                  density="compact"
                  class="mt-2 px-0 text-lowercase ml-n2"
                  :prepend-icon="
                    showAllConflicts ? 'mdi-chevron-up' : 'mdi-chevron-down'
                  "
                  @click="showAllConflicts = !showAllConflicts"
                >
                  {{
                    showAllConflicts
                      ? t('product.vigilance.conflicts.showLess')
                      : t('product.vigilance.conflicts.showMore')
                  }}
                </v-btn>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Data Quality Card -->
      <v-col v-if="isLowDataQuality" cols="12" :md="cardColumnSize">
        <v-card
          class="product-vigilance__card product-vigilance__card--quality h-100"
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
                  {{ t('product.vigilance.quality.title') }}
                </p>
                <p
                  class="product-vigilance__card-description"
                  v-html="
                    sanitize(
                      t('product.vigilance.quality.description', {
                        score: dataQualityScoreValue,
                        avg: dataQualityAvg,
                      })
                    )
                  "
                />
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Obsolescence Warning Card -->
      <v-col v-if="hasObsolescenceWarning" cols="12" :md="cardColumnSize">
        <v-card
          class="product-vigilance__card product-vigilance__card--obsolescence h-100"
          color="warning"
          variant="tonal"
        >
          <v-card-text class="product-vigilance__card-body">
            <div class="product-vigilance__card-layout">
              <v-icon
                class="product-vigilance__card-icon"
                size="40"
                icon="mdi-timer-alert-outline"
              />
              <div class="product-vigilance__card-content">
                <p class="product-vigilance__card-title">
                  {{ t('product.vigilance.obsolescence.title') }}
                </p>
                <p
                  class="product-vigilance__card-description"
                  v-html="sanitize(obsolescenceWarning)"
                />
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Competition Vigilance Card -->
      <v-col v-if="isLowCompetition" cols="12" :md="cardColumnSize">
        <v-card
          class="product-vigilance__card product-vigilance__card--competition h-100"
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
                  {{ t('product.price.competition.title') }}
                </p>
                <p
                  class="product-vigilance__card-description"
                  v-html="
                    sanitize(t('product.price.competition.lowDescription'))
                  "
                />
                <v-btn
                  class="mt-2 align-self-start"
                  size="small"
                  color="warning"
                  variant="outlined"
                  prepend-icon="mdi-eye-outline"
                  @click="emit('click:offers')"
                >
                  {{
                    t('product.price.competition.cta', {
                      count: competitionCount + 0,
                    })
                  }}
                </v-btn>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
import { computed, toRef, ref } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import DOMPurify from 'isomorphic-dompurify'
import { normalizeTimestamp } from '~/utils/date-parsing'
import type { ProductDto } from '~~/shared/api-client'

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
    legalSupportDuration: legalSupportDuration.value,
  })
})

const minAttributes = computed(() => {
  const allAttributes = product.value.attributes?.allAttributes ?? {}
  return Object.entries(allAttributes)
    .filter(([key]) => key.toUpperCase().startsWith('MIN_'))
    .map(([, attr]) => attr)
    .filter(attr => !!attr && attr.name)
})

const legalSupportDuration = computed(() => {
  const allAttributes = product.value.attributes?.allAttributes ?? {}
  // Heuristic: search for MIN_GUARANTEED_...SPARE_PARTS or similar that implies support duration
  // We prefer attributes that mention spare parts or support
  const attrEntry = Object.entries(allAttributes).find(
    ([key]) =>
      key.toUpperCase().startsWith('MIN_') &&
      (key.toUpperCase().includes('SPARE') ||
        key.toUpperCase().includes('SUPPORT'))
  )

  if (attrEntry) {
    const val = attrEntry[1].sourcing?.bestValue
    if (val) {
      // If purely numeric, append "ans" (years) as a guess for EPREL durations
      const numeric = Number(val)
      if (!isNaN(numeric)) {
        return locale.value.startsWith('fr')
          ? `${numeric} ans`
          : `${numeric} years`
      }
      return val
    }
  }

  // Fallback: check timeline duration between onMarketEndDate and EPREL_SUPPORT_END
  // Not implemented to avoid complexity, defaulting to generic fallback if attribute not found
  return locale.value.startsWith('fr') ? 'Non spécifié' : 'Unspecified'
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

const showAllConflicts = ref(false)

const emit = defineEmits<{
  (e: 'click:offers'): void
}>()

const displayedConflictingAttributes = computed(() => {
  if (showAllConflicts.value) {
    return conflictingAttributes.value
  }
  return conflictingAttributes.value.slice(0, 10)
})

// --- Data Quality Logic ---
const dataQualityScore = computed(
  () => product.value.scores?.scores?.['DATA_QUALITY']
)

const dataQualityScoreValueNum = computed(() => {
  return (dataQualityScore.value?.value ?? 0) * 4
})

const dataQualityScoreValue = computed(() => {
  // Convert 0-5 scale to 0-20
  return dataQualityScoreValueNum.value.toFixed(1)
})

const dataQualityAvgNum = computed(() => {
  return (
    (dataQualityScore.value?.relativ?.avg ??
      dataQualityScore.value?.absolute?.avg ??
      0) * 4
  )
})

const dataQualityAvg = computed(() => {
  // Convert 0-5 scale to 0-20
  return dataQualityAvgNum.value.toFixed(1)
})

const isLowDataQuality = computed(() => {
  if (!dataQualityScore.value) return false
  // Alert if score is strictly lower than average
  return dataQualityScoreValueNum.value < dataQualityAvgNum.value
})

// --- Obsolescence Vigilance Logic ---
const obsolescenceWarning = computed(
  () => product.value.aiReview?.review?.obsolescenceWarning
)

const hasObsolescenceWarning = computed(
  () => !!obsolescenceWarning.value && obsolescenceWarning.value.length > 0
)

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

// --- Active Cards & Layout Logic ---
const activeCardCount = computed(() => {
  let count = 0
  if (isEndOfLife.value) count++
  if (hasConflictingAttributes.value) count++
  if (isLowDataQuality.value) count++
  if (hasObsolescenceWarning.value) count++
  if (isLowCompetition.value) count++
  return count
})

const cardColumnSize = computed(() => {
  const count = activeCardCount.value
  if (count <= 2) return 6
  if (count === 3) return 4
  return 3
})
console.log('DEBUG COUNT:', competitionCount.value)

function sanitize(content: string | null | undefined): string {
  if (!content) return ''
  return DOMPurify.sanitize(content, {
    ADD_ATTR: ['target', 'rel', 'class'],
  })
}
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

.product-vigilance__card--obsolescence .product-vigilance__card-icon {
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

.product-vigilance__conflicts-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.product-vigilance__conflict-item {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-strong), 0.95);
}

.product-vigilance__min-attributes {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(var(--v-theme-on-warning), 0.15);
}

.product-vigilance__min-attribute-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
}

.product-vigilance__min-attribute-label {
  color: rgba(var(--v-theme-text-neutral-strong), 0.9);
  font-weight: 500;
}
</style>
