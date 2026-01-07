<template>
  <article class="product-alternatives">
    <header class="product-alternatives__header">
      <h3 class="product-alternatives__title">
        {{ t('product.impact.alternatives.title') }}
      </h3>
      <p class="product-alternatives__subtitle">
        {{ t('product.impact.alternatives.subtitle', subtitleParams) }}
      </p>
    </header>

    <div v-if="filterDefinitions.length" class="product-alternatives__filters">
      <div class="product-alternatives__chips">
        <v-tooltip
          v-for="filter in filterDefinitions"
          :key="filter.key"
          :text="filter.tooltip"
          location="top"
        >
          <template #activator="{ props: tooltipProps }">
            <button
              type="button"
              class="product-alternatives__chip"
              :class="{
                'product-alternatives__chip--active': isFilterActive(
                  filter.key
                ),
                'product-alternatives__chip--disabled': filter.disabled,
              }"
              v-bind="tooltipProps"
              :aria-pressed="isFilterActive(filter.key)"
              :disabled="filter.disabled"
              @click="toggleFilter(filter.key)"
            >
              <span>{{ filter.label }}</span>
            </button>
          </template>
        </v-tooltip>
      </div>
    </div>

    <div class="product-alternatives__content">
      <v-skeleton-loader
        v-if="loading && !alternatives.length"
        type="image, article"
        class="product-alternatives__skeleton"
      />
      <template v-else>
        <div v-if="alternatives.length" class="product-alternatives__scroller">
          <v-slide-group show-arrows class="product-alternatives__slide-group">
            <v-slide-group-item
              v-for="alternative in alternatives"
              :key="
                alternative.gtin ??
                alternative.fullSlug ??
                alternative.slug ??
                JSON.stringify(alternative.identity)
              "
              v-slot="{ toggle, selectedClass }"
            >
              <ProductTileCard
                :product="alternative"
                :product-link="productLink(alternative)"
                :image-src="resolveImage(alternative)"
                :attributes="popularAttributesByProduct(alternative)"
                :impact-score="impactScoreValue(alternative)"
                :offer-badges="offerBadges(alternative)"
                :offers-count-label="offersCountLabel(alternative)"
                :untitled-label="t('product.impact.alternatives.untitled')"
                :not-rated-label="t('category.products.notRated')"
                layout="horizontal"
                class="product-alternatives__slide-item product-alternatives__card"
                :class="selectedClass"
                @click="toggle"
              />
            </v-slide-group-item>
          </v-slide-group>
        </div>
        <div
          v-else-if="errorMessage"
          class="product-alternatives__empty product-alternatives__empty--error"
        >
          <p>{{ errorMessage }}</p>
          <v-btn color="primary" variant="text" @click="retryFetch">
            {{ t('product.impact.alternatives.retry') }}
          </v-btn>
        </div>
        <div v-else class="product-alternatives__empty-card">
          <v-icon
            icon="mdi-emoticon-happy-outline"
            size="56"
            class="product-alternatives__empty-icon"
            aria-hidden="true"
          />
          <p class="product-alternatives__empty-message">
            {{ t('product.impact.alternatives.bestProduct') }}
          </p>
        </div>
      </template>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AttributeConfigDto,
  Filter,
  ProductDto,
  ProductSearchResponseDto,
} from '~~/shared/api-client'
import { ProductsIncludeEnum } from '~~/shared/api-client'
import ProductTileCard from '~/components/category/products/ProductTileCard.vue'
import {
  formatAttributeValue,
  resolveAttributeRawValueByKey,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto | null>,
    default: null,
  },
  verticalId: {
    type: String,
    default: '',
  },
  popularAttributes: {
    type: Array as PropType<AttributeConfigDto[]>,
    default: () => [],
  },
  maxResults: {
    type: Number,
    default: 5,
  },
  subtitleParams: {
    type: Object as PropType<Record<string, string> | undefined>,
    default: undefined,
  },
})

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const alternatives = ref<ProductDto[]>([])
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const hasInteracted = ref(false)
const activeFilterKeys = ref<Set<string>>(new Set())
let requestToken = 0

const normalizedPopularAttributes = computed(
  () => props.popularAttributes ?? []
)

const currencySymbolCache = new Map<string, string>()
const NBSP = '\u00A0'

const resolveCurrencySymbol = (currency?: string | null): string | null => {
  if (!currency) {
    return null
  }

  const upperCaseCurrency = currency.toUpperCase()

  if (currencySymbolCache.has(upperCaseCurrency)) {
    return currencySymbolCache.get(upperCaseCurrency) ?? null
  }

  try {
    const formatter = new Intl.NumberFormat('en', {
      style: 'currency',
      currency: upperCaseCurrency,
    })
    const symbol =
      formatter.formatToParts(0).find(part => part.type === 'currency')
        ?.value ?? upperCaseCurrency

    currencySymbolCache.set(upperCaseCurrency, symbol)

    return symbol
  } catch {
    currencySymbolCache.set(upperCaseCurrency, upperCaseCurrency)

    return upperCaseCurrency
  }
}

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const productLink = (product: ProductDto) =>
  product.fullSlug ?? product.slug ?? undefined

const impactScoreValue = (product: ProductDto) =>
  resolvePrimaryImpactScore(product)

const offersCountLabel = (product: ProductDto) =>
  formatOffersCount(product, translatePlural)

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = (
  product: ProductDto
): DisplayedAttribute[] => {
  const attributes = resolvePopularAttributes(
    product,
    normalizedPopularAttributes.value
  )
  const entries: DisplayedAttribute[] = []

  attributes.forEach(attribute => {
    const value = formatAttributeValue(attribute, t, n)
    if (!value) {
      return
    }

    entries.push({
      key: attribute.key,
      label: attribute.label,
      value,
      icon: attribute.icon ?? null,
    })
  })

  return entries
}

const formatOfferPrice = (
  offer:
    | { price?: number | null; shortPrice?: string | null }
    | null
    | undefined,
  product: ProductDto
): string | null => {
  if (!offer) {
    return null
  }

  const currency = product.offers?.bestPrice?.currency ?? null
  const shortPrice = offer.shortPrice?.trim()

  if (shortPrice) {
    const symbol = resolveCurrencySymbol(currency)

    if (!symbol) {
      return shortPrice
    }

    const normalisedShortPrice = shortPrice.replace(/\s+/g, ' ').trim()
    const containsSymbol =
      normalisedShortPrice.includes(symbol) ||
      normalisedShortPrice.toUpperCase().includes(currency?.toUpperCase() ?? '')

    return containsSymbol
      ? normalisedShortPrice
      : `${normalisedShortPrice}${NBSP}${symbol}`
  }

  const price = offer.price

  if (price == null) {
    return null
  }

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

type OfferBadge = {
  key: string
  label: string
  price: string
  appearance: 'new' | 'occasion' | 'default'
}

const offerBadges = (product: ProductDto): OfferBadge[] => {
  const entries: OfferBadge[] = []
  const newOffer = product.offers?.bestNewOffer
  const occasionOffer = product.offers?.bestOccasionOffer

  if (newOffer) {
    const formatted = formatOfferPrice(newOffer, product)

    if (formatted) {
      entries.push({
        key: 'new',
        label: t('category.products.pricing.newOfferLabel'),
        price: formatted,
        appearance: 'new',
      })
    }
  }

  if (occasionOffer) {
    const formatted = formatOfferPrice(occasionOffer, product)

    if (formatted) {
      entries.push({
        key: 'occasion',
        label: t('category.products.pricing.occasionOfferLabel'),
        price: formatted,
        appearance: 'occasion',
      })
    }
  }

  if (!entries.length) {
    const fallbackOffer = product.offers?.bestPrice
    const formatted =
      formatOfferPrice(fallbackOffer, product) ?? formatBestPrice(product, t, n)

    entries.push({
      key: 'best',
      label: t('category.products.pricing.bestOfferLabel'),
      price: formatted,
      appearance: 'default',
    })
  }

  return entries
}

const formatCurrency = (value: number, currency?: string | null) => {
  if (!Number.isFinite(value)) {
    return null
  }

  if (!currency) {
    return n(value, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  try {
    return n(value, { style: 'currency', currency })
  } catch {
    return `${n(value, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
  }
}

const resolveScoreNumericValue = (
  score:
    | { relativ?: { value?: number | null } | null; value?: number | null }
    | null
    | undefined
): number | null => {
  const relative = score?.relativ?.value
  if (typeof relative === 'number' && Number.isFinite(relative)) {
    return relative
  }

  const absolute = score?.value
  if (typeof absolute === 'number' && Number.isFinite(absolute)) {
    return absolute
  }

  return null
}

const ecoscoreValue = computed<number | null>(() => {
  const ecoscoreScore = resolveScoreNumericValue(
    props.product?.scores?.ecoscore ?? null
  )
  if (ecoscoreScore != null) {
    return ecoscoreScore
  }

  const mapValue = resolveScoreNumericValue(
    props.product?.scores?.scores?.ECOSCORE
  )
  if (mapValue != null) {
    return mapValue
  }

  const base = props.product?.base?.ecoscoreValue
  if (typeof base === 'number' && Number.isFinite(base)) {
    return base
  }

  return null
})

const priceValue = computed<number | null>(() => {
  const price = props.product?.offers?.bestPrice?.price
  return typeof price === 'number' && Number.isFinite(price) ? price : null
})

const priceCurrency = computed(
  () => props.product?.offers?.bestPrice?.currency ?? null
)

const toNumeric = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string') {
    const parsed = Number.parseFloat(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

const toTerm = (value: unknown): string | null => {
  if (value == null) {
    return null
  }

  if (Array.isArray(value)) {
    const firstMeaningful = value.find(entry => entry != null)
    return toTerm(firstMeaningful)
  }

  if (typeof value === 'boolean') {
    return value ? 'true' : 'false'
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    return `${value}`
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed.length ? trimmed : null
  }

  return null
}

const resolveAttributeMapping = (
  key: string,
  config: AttributeConfigDto | undefined
): string | null => {
  if (!props.product) {
    return null
  }

  const indexed = props.product.attributes?.indexedAttributes?.[key]
  if (indexed) {
    if (
      config?.filteringType === 'NUMERIC' ||
      typeof indexed.numericValue === 'number'
    ) {
      return `attributes.indexed.${key}.numericValue`
    }

    if (
      config?.filteringType === 'BOOLEAN' ||
      typeof indexed.booleanValue === 'boolean'
    ) {
      return `attributes.indexed.${key}.booleanValue`
    }

    return `attributes.indexed.${key}.value`
  }

  if (props.product.attributes?.referentialAttributes?.[key] != null) {
    return `attributes.referentialAttributes.${key}`
  }

  if (config?.filteringType === 'NUMERIC') {
    return `attributes.indexed.${key}.numericValue`
  }

  if (config?.filteringType === 'BOOLEAN') {
    return `attributes.indexed.${key}.booleanValue`
  }

  return `attributes.indexed.${key}.value`
}

type AlternativeFilterDefinition = {
  key: string
  label: string
  tooltip: string
  defaultSelected: boolean
  disabled: boolean
  resolveClause: () => Filter | null
}

const ecoscoreFilterDefinition = computed<AlternativeFilterDefinition | null>(
  () => {
    if (ecoscoreValue.value == null) {
      return null
    }

    const value = ecoscoreValue.value
    return {
      key: 'ecoscore',
      label: t('product.impact.alternatives.filters.ecoscore'),
      tooltip: t('product.impact.alternatives.tooltips.ecoscore', {
        value: n(value, { maximumFractionDigits: 1, minimumFractionDigits: 0 }),
      }),
      defaultSelected: true,
      disabled: false,
      resolveClause: () => ({
        field: ECOSCORE_RELATIVE_FIELD,
        operator: 'range',
        min: value,
      }),
    }
  }
)

const priceFilterDefinition = computed<AlternativeFilterDefinition | null>(
  () => {
    if (priceValue.value == null) {
      return null
    }

    const value = priceValue.value
    return {
      key: 'price',
      label: t('product.impact.alternatives.filters.price'),
      tooltip:
        t('product.impact.alternatives.tooltips.price', {
          value:
            formatCurrency(value, priceCurrency.value) ??
            n(value, { maximumFractionDigits: 2 }),
        }) ?? '',
      defaultSelected: true,
      disabled: false,
      resolveClause: () => ({
        field: 'price.minPrice.price',
        operator: 'range',
        max: value,
      }),
    }
  }
)

const attributeFilterDefinitions = computed<AlternativeFilterDefinition[]>(
  () => {
    if (!props.product) {
      return []
    }

    return normalizedPopularAttributes.value.reduce<
      AlternativeFilterDefinition[]
    >((accumulator, config) => {
      const key = config.key?.trim()
      if (!key) {
        return accumulator
      }

      const mapping = resolveAttributeMapping(key, config)
      if (!mapping) {
        return accumulator
      }

      const rawValue = resolveAttributeRawValueByKey(
        props.product as ProductDto,
        key
      )
      if (rawValue == null) {
        return accumulator
      }

      const betterRule = config.betterIs ?? null
      const readableLabel = config.name ?? key

      if (betterRule) {
        const numericValue = toNumeric(rawValue)
        if (numericValue == null) {
          return accumulator
        }

        const prefersGreater = betterRule === 'GREATER'
        const symbol = prefersGreater ? '↑' : '↓'

        accumulator.push({
          key: `attribute:${key}`,
          label: `${readableLabel} ${symbol}`,
          tooltip: prefersGreater
            ? t('product.impact.alternatives.tooltips.attributeBetter', {
                label: readableLabel,
              })
            : t('product.impact.alternatives.tooltips.attributeLower', {
                label: readableLabel,
              }),
          defaultSelected: true,
          disabled: false,
          resolveClause: () => ({
            field: mapping,
            operator: 'range',
            ...(prefersGreater ? { min: numericValue } : { max: numericValue }),
          }),
        })

        return accumulator
      }

      const term = toTerm(rawValue)
      if (!term) {
        return accumulator
      }

      accumulator.push({
        key: `attribute:${key}`,
        label: `${readableLabel} =`,
        tooltip: t('product.impact.alternatives.tooltips.attributeEqual', {
          label: readableLabel,
        }),
        defaultSelected: false,
        disabled: false,
        resolveClause: () => ({
          field: mapping,
          operator: 'term',
          terms: [term],
        }),
      })

      return accumulator
    }, [])
  }
)

const filterDefinitions = computed<AlternativeFilterDefinition[]>(() => {
  const definitions: AlternativeFilterDefinition[] = []

  if (priceFilterDefinition.value) {
    definitions.push(priceFilterDefinition.value)
  }

  if (ecoscoreFilterDefinition.value) {
    definitions.push(ecoscoreFilterDefinition.value)
  }

  definitions.push(...attributeFilterDefinitions.value)

  return definitions
})

const filterDefinitionMap = computed(
  () =>
    new Map(
      filterDefinitions.value.map(definition => [definition.key, definition])
    )
)

const isFilterActive = (key: string) => activeFilterKeys.value.has(key)

const toggleFilter = (key: string) => {
  const definition = filterDefinitionMap.value.get(key)
  if (!definition || definition.disabled) {
    return
  }

  hasInteracted.value = true
  const next = new Set(activeFilterKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  activeFilterKeys.value = next
}

const activeClauses = computed<Filter[]>(() => {
  return Array.from(activeFilterKeys.value)
    .map(key => filterDefinitionMap.value.get(key))
    .filter(
      (definition): definition is AlternativeFilterDefinition =>
        Boolean(definition) && !definition.disabled
    )
    .map(definition => definition.resolveClause())
    .filter((clause): clause is Filter => clause != null)
})

const canSearch = computed(
  () => Boolean(props.product?.gtin) && props.verticalId.trim().length > 0
)

const syncActiveFilters = (preserveSelection: boolean) => {
  const previous = new Set(activeFilterKeys.value)
  const next = new Set<string>()

  filterDefinitions.value.forEach(definition => {
    if (definition.disabled) {
      return
    }

    if (preserveSelection) {
      if (previous.has(definition.key)) {
        next.add(definition.key)
      }
      return
    }

    if (definition.defaultSelected) {
      next.add(definition.key)
    }
  })

  activeFilterKeys.value = next
}

watch(
  () => props.product?.gtin ?? null,
  () => {
    hasInteracted.value = false
    alternatives.value = []
    errorMessage.value = null
    syncActiveFilters(false)
  },
  { immediate: true }
)

watch(
  filterDefinitions,
  () => {
    syncActiveFilters(hasInteracted.value)
  },
  { immediate: true }
)

const fetchAlternatives = async () => {
  if (!canSearch.value) {
    alternatives.value = []
    return
  }

  const body: Record<string, unknown> = {
    verticalId: props.verticalId,
    pageSize: props.maxResults,
    pageNumber: 0,
    include: [
      ProductsIncludeEnum.Base,
      ProductsIncludeEnum.Identity,
      ProductsIncludeEnum.Offers,
      ProductsIncludeEnum.Scores,
      ProductsIncludeEnum.Resources,
    ],
    sort: {
      field: ECOSCORE_RELATIVE_FIELD,
      order: 'DESC',
    },
  }

  if (activeClauses.value.length) {
    body.filters = { filters: activeClauses.value }
  }

  const currentToken = ++requestToken
  loading.value = true
  errorMessage.value = null

  try {
    const response = await $fetch<ProductSearchResponseDto>(
      '/api/products/search',
      {
        method: 'POST',
        body,
      }
    )

    if (currentToken !== requestToken) {
      return
    }

    const pageData = Array.isArray(response.products?.data)
      ? response.products.data
      : []
    const products = pageData.filter(
      candidate => candidate.gtin !== props.product?.gtin
    )
    alternatives.value = products.slice(0, props.maxResults)
  } catch (error) {
    if (currentToken !== requestToken) {
      return
    }

    console.error('Failed to fetch product alternatives', error)
    errorMessage.value = t('product.impact.alternatives.error')
    alternatives.value = []
  } finally {
    if (currentToken === requestToken) {
      loading.value = false
    }
  }
}

watch(
  [() => props.verticalId, () => props.product?.gtin ?? null, activeClauses],
  () => {
    if (canSearch.value) {
      fetchAlternatives()
    }
  },
  { immediate: true }
)

const retryFetch = () => {
  fetchAlternatives()
}
</script>

<style scoped>
.product-alternatives {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  border-radius: 24px;
  min-height: 100%;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-glass), 0.95),
    rgba(var(--v-theme-surface-primary-080), 0.9)
  );
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-alternatives__header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-alternatives__title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-alternatives__subtitle {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-alternatives__filters {
  display: flex;
  flex-wrap: wrap;
}

.product-alternatives__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.product-alternatives__chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.18);
  background-color: rgba(var(--v-theme-surface-default), 0.95);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-size: 0.875rem;
  font-weight: 600;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.product-alternatives__chip:hover:not(:disabled),
.product-alternatives__chip:focus-visible:not(:disabled) {
  border-color: rgba(var(--v-theme-primary), 0.6);
  box-shadow: 0 4px 14px rgba(21, 46, 73, 0.12);
}

.product-alternatives__chip--active {
  background-color: rgba(var(--v-theme-primary), 0.12);
  border-color: rgba(var(--v-theme-primary), 0.4);
  color: rgb(var(--v-theme-primary));
}

.product-alternatives__chip--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.product-alternatives__content {
  min-height: 180px;
  display: flex;
  flex-direction: column;
}

.product-alternatives__scroller {
  position: relative;
  display: flex;
  justify-content: center;
  max-width: 100%;
  overflow: hidden;
}

.product-alternatives__slide-group {
  width: 100%;
  padding: 0.5rem 1rem;
}

.product-alternatives__slide-group :deep(.v-slide-group__content) {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  padding: 0.25rem;
}

.product-alternatives__slide-item {
  padding: 0;
}

.product-alternatives__card {
  width: 240px;
  max-width: 100%;
}

.product-alternatives__skeleton {
  border-radius: 18px;
  overflow: hidden;
}

.product-alternatives__empty {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-start;
  justify-content: center;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  font-size: 0.95rem;
}

.product-alternatives__empty--error {
  color: rgb(var(--v-theme-error));
}

.product-alternatives__empty-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-primary-080), 0.7);
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
  font-size: 1.05rem;
  justify-content: center;
  align-self: center;
  max-width: 520px;
}

.product-alternatives__empty-icon {
  color: rgba(var(--v-theme-primary), 0.85);
}

.product-alternatives__empty-message {
  margin: 0;
  color: rgb(var(--v-theme-primary));
}

@media (max-width: 960px) {
  .product-alternatives {
    padding: 1.25rem;
  }

  .product-alternatives__card {
    width: 220px;
  }
}
</style>
