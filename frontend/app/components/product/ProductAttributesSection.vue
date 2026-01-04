<template>
  <section :id="sectionId" class="product-attributes">
    <header class="product-attributes__header">
      <h2 class="product-attributes__title">
        {{ sectionTitle }}
      </h2>
      <p class="product-attributes__subtitle">
        {{ $t('product.attributes.subtitle') }}
      </p>
    </header>

    <div class="product-attributes__block">
      <div class="product-attributes__block-header">
        <h3 class="product-attributes__block-title">
          {{ $t('product.attributes.main.title') }}
        </h3>
      </div>

      <div class="product-attributes__main-grid">
        <v-card class="product-attributes__identity-card" variant="flat">
          <div class="product-attributes__identity-heading">
            <v-icon
              icon="mdi-card-account-details-outline"
              class="product-attributes__identity-icon"
            />
            <span>{{ $t('product.attributes.main.identity.title') }}</span>
          </div>

          <div
            v-if="identityRows.length"
            class="product-attributes__identity-table"
          >
            <div
              v-for="row in identityRows"
              :key="row.key"
              class="product-attributes__identity-row"
            >
              <span class="product-attributes__identity-label">{{
                row.label
              }}</span>
              <div class="product-attributes__identity-value">
                <span>{{ row.value }}</span>
                <div
                  v-if="row.details?.length"
                  class="product-attributes__identity-details"
                >
                  <div
                    v-for="detail in row.details"
                    :key="detail.key"
                    class="product-attributes__identity-detail"
                  >
                    <span class="product-attributes__identity-detail-label">
                      {{ detail.label }}
                    </span>
                    <ul class="product-attributes__identity-detail-list">
                      <li v-for="value in detail.values" :key="value">
                        {{ value }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <p
            v-else-if="!gtinImageUrl"
            class="product-attributes__empty product-attributes__identity-empty"
          >
            {{ $t('product.attributes.main.identity.empty') }}
          </p>

          <figure v-if="gtinImageUrl" class="product-attributes__gtin">
            <v-img
              :src="gtinImageUrl"
              :alt="
                $t('product.attributes.main.identity.gtinImageAlt', {
                  gtin: gtinDisplay ?? '—',
                })
              "
              class="product-attributes__gtin-image"
              cover
            />
            <figcaption class="product-attributes__gtin-caption">
              {{ $t('product.attributes.main.identity.gtinLabel') }}
            </figcaption>
          </figure>
        </v-card>

        <v-card class="product-attributes__main-card" variant="flat">
          <v-table
            v-if="mainAttributes.length"
            density="comfortable"
            class="product-attributes__table product-attributes__table--main"
          >
            <tbody>
              <tr v-for="attribute in mainAttributes" :key="attribute.key">
                <th scope="row">{{ attribute.label }}</th>
                <td>
                  <ProductAttributeSourcingLabel
                    class="product-attributes__table-value"
                    :sourcing="attribute.sourcing"
                    :value="attribute.value"
                  />
                </td>
              </tr>
            </tbody>
          </v-table>
          <p v-else class="product-attributes__empty">
            {{ $t('product.attributes.main.attributes.empty') }}
          </p>
        </v-card>
      </div>

      <v-row v-if="timeline" class="product-attributes__timeline-row" dense>
        <v-col cols="12">
          <ProductLifeTimeline :timeline="timeline" />
        </v-col>
      </v-row>
    </div>

    <div
      v-if="showAuditWidget"
      class="product-attributes__block product-attributes__block--audit"
    >
      <div
        class="product-attributes__block-header product-attributes__block-header--audit"
      >
        <div class="product-attributes__audit-heading">
          <h3 class="product-attributes__block-title">
            {{ $t('product.attributes.audit.title') }}
          </h3>
          <p class="product-attributes__audit-subtitle">
            {{ $t('product.attributes.audit.subtitle') }}
          </p>
        </div>
        <div class="product-attributes__audit-controls">
          <v-text-field
            v-model="auditSearchTerm"
            :label="$t('product.attributes.audit.searchPlaceholder')"
            prepend-inner-icon="mdi-magnify"
            hide-details
            clearable
            class="product-attributes__search product-attributes__search--audit"
          />
          <div class="product-attributes__audit-filters">
            <v-checkbox
              v-model="showIndexed"
              :label="$t('product.attributes.audit.filters.indexed')"
              hide-details
              density="compact"
              class="product-attributes__audit-filter"
            />
            <v-checkbox
              v-model="showNotIndexed"
              :label="$t('product.attributes.audit.filters.notIndexed')"
              hide-details
              density="compact"
              class="product-attributes__audit-filter"
            />
          </div>
        </div>
      </div>

      <v-data-table
        v-if="filteredAuditRows.length"
        :headers="auditHeaders"
        :items="filteredAuditRows"
        :items-per-page="auditItemsPerPage"
        :item-class="auditRowClass"
        class="product-attributes__audit-table"
        density="comfortable"
      >
        <template #[`item.attribute`]="{ item }">
          <div class="product-attributes__audit-attribute">
            <span class="product-attributes__audit-name">
              {{ item.name }}
            </span>
            <span class="product-attributes__audit-key">
              {{ item.key }}
            </span>
          </div>
        </template>
        <template #[`item.bestValue`]="{ item }">
          <ProductAttributeSourcingLabel
            :value="item.displayValue"
            :sourcing="item.sourcing"
            class="product-attributes__audit-value"
          />
        </template>
        <template #[`item.sources`]="{ item }">
          <span class="product-attributes__audit-count">
            {{ item.sourceCount }}
          </span>
        </template>
        <template #[`item.indexed`]="{ item }">
          <v-chip
            size="small"
            variant="tonal"
            :color="item.isIndexed ? 'surface-primary-120' : 'surface-muted'"
          >
            {{
              $t(
                item.isIndexed
                  ? 'product.attributes.audit.indexed'
                  : 'product.attributes.audit.notIndexed'
              )
            }}
          </v-chip>
        </template>
      </v-data-table>
      <p
        v-else
        class="product-attributes__empty product-attributes__empty--audit"
      >
        {{
          $t(
            auditHasFilters
              ? 'product.attributes.audit.emptyFiltered'
              : 'product.attributes.audit.empty'
          )
        }}
      </p>
    </div>

    <div class="product-attributes__block product-attributes__block--detailed">
      <div
        class="product-attributes__block-header product-attributes__block-header--detailed"
      >
        <h3 class="product-attributes__block-title">
          {{ $t('product.attributes.detailed.title') }}
        </h3>
        <v-text-field
          v-model="searchTerm"
          :label="$t('product.attributes.searchPlaceholder')"
          prepend-inner-icon="mdi-magnify"
          hide-details
          clearable
          class="product-attributes__search"
        />
      </div>

      <div class="product-attributes__detailed-layout">
        <div class="product-attributes__details-panel">
          <v-row
            v-if="filteredGroups.length"
            class="product-attributes__details-grid"
            dense
          >
            <ProductAttributesDetailCard
              v-for="group in filteredGroups"
              :key="group.id"
              :group="group"
            />
          </v-row>

          <p
            v-else
            class="product-attributes__empty product-attributes__empty--detailed"
          >
            {{
              $t(
                hasSearchTerm
                  ? 'product.attributes.detailed.noResults'
                  : 'product.attributes.detailed.empty'
              )
            }}
          </p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuth } from '~/composables/useAuth'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
import ProductAttributesDetailCard from '~/components/product/attributes/ProductAttributesDetailCard.vue'
import ProductLifeTimeline from '~/components/product/ProductLifeTimeline.vue'
import type {
  AttributeConfigDto,
  ProductAttributeDto,
  ProductAttributeSourceDto,
  ProductAttributesDto,
  ProductDto,
  ProductIndexedAttributeDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'

const props = defineProps({
  sectionId: {
    type: String,
    default: 'caracteristiques',
  },
  attributes: {
    type: Object as PropType<ProductAttributesDto | null>,
    default: null,
  },
  attributeConfigs: {
    type: Array as PropType<AttributeConfigDto[]>,
    default: () => [],
  },
  product: {
    type: Object as PropType<ProductDto | null>,
    default: null,
  },
  productBrand: {
    type: String,
    default: '',
  },
  modelVariation: {
    type: String,
    default: '',
  },
})

const { t, n, locale } = useI18n()
const runtimeConfig = useRuntimeConfig()
const { isLoggedIn } = useAuth()

const sectionTitle = computed(() => {
  const brand = props.productBrand.trim()
  const modelVariation = props.modelVariation.trim()

  if (modelVariation.length) {
    return t('product.attributes.titleWithModel', { modelVariation })
  }

  if (brand.length) {
    return t('product.attributes.titleWithBrand', { brand })
  }

  return t('product.attributes.title')
})

const searchTerm = ref('')
const hasSearchTerm = computed(() => searchTerm.value.trim().length > 0)
const auditSearchTerm = ref('')
const showIndexed = ref(true)
const showNotIndexed = ref(true)

const auditHasFilters = computed(
  () =>
    auditSearchTerm.value.trim().length > 0 ||
    !showIndexed.value ||
    !showNotIndexed.value
)

const showAuditWidget = computed(() => isLoggedIn.value)

const resolvedAttributes = computed<ProductAttributesDto | null>(() => {
  if (props.attributes) {
    return props.attributes
  }

  return props.product?.attributes ?? null
})

const referentialAttributes = computed<Record<string, string>>(
  () => resolvedAttributes.value?.referentialAttributes ?? {}
)

const timeline = computed(() => props.product?.timeline ?? null)

const staticServerBase = computed(() => {
  const fallback = 'https://static.nudger.fr'
  const base = runtimeConfig.public?.staticServer ?? fallback
  return base.endsWith('/') ? base.slice(0, -1) : base
})

const gtinDisplay = computed(() => {
  const candidate = props.product?.gtin ?? props.product?.base?.gtin
  if (candidate == null) {
    return null
  }

  const value = typeof candidate === 'string' ? candidate : String(candidate)
  const trimmed = value.trim()
  return trimmed.length ? trimmed : null
})

const gtinImageUrl = computed(() => {
  const gtin = gtinDisplay.value
  if (!gtin) {
    return null
  }

  return `${staticServerBase.value}/images/${gtin}-gtin.png`
})

const RELATIVE_TIME_UNITS: Array<{
  unit: Intl.RelativeTimeFormatUnit
  ms: number
}> = [
  { unit: 'year', ms: 1000 * 60 * 60 * 24 * 365 },
  { unit: 'month', ms: 1000 * 60 * 60 * 24 * 30 },
  { unit: 'week', ms: 1000 * 60 * 60 * 24 * 7 },
  { unit: 'day', ms: 1000 * 60 * 60 * 24 },
  { unit: 'hour', ms: 1000 * 60 * 60 },
  { unit: 'minute', ms: 1000 * 60 },
  { unit: 'second', ms: 1000 },
]

const formatRelativeTimeFromNow = (
  timestamp?: number | null
): string | null => {
  if (typeof timestamp !== 'number' || !Number.isFinite(timestamp)) {
    return null
  }

  const now = Date.now()
  const diff = timestamp - now
  const formatter = new Intl.RelativeTimeFormat(locale.value, {
    numeric: 'auto',
  })

  for (const { unit, ms } of RELATIVE_TIME_UNITS) {
    const value = diff / ms
    if (Math.abs(value) >= 1 || unit === 'second') {
      return formatter.format(Math.round(value), unit)
    }
  }

  return null
}

const formatDateValue = (timestamp?: number | null): string | null => {
  if (typeof timestamp !== 'number' || !Number.isFinite(timestamp)) {
    return null
  }

  try {
    return new Intl.DateTimeFormat(locale.value, {
      dateStyle: 'medium',
    }).format(new Date(timestamp))
  } catch (error) {
    console.warn('Failed to format product date', error)
    return null
  }
}

const firstNonEmptyString = (...values: Array<unknown>): string | null => {
  for (const value of values) {
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed.length) {
        return trimmed
      }
    }
  }

  return null
}

const toStringList = (input: unknown): string[] => {
  if (!input) {
    return []
  }

  if (input instanceof Set) {
    return Array.from(input)
      .map(entry => (typeof entry === 'string' ? entry.trim() : ''))
      .filter(entry => entry.length > 0)
  }

  if (Array.isArray(input)) {
    return input
      .map(entry => (typeof entry === 'string' ? entry.trim() : ''))
      .filter(entry => entry.length > 0)
  }

  if (typeof input === 'string') {
    const trimmed = input.trim()
    return trimmed.length ? [trimmed] : []
  }

  return []
}

const normalizeAttributeKey = (value: unknown): string | null => {
  if (typeof value !== 'string') {
    return null
  }

  const trimmed = value.trim()
  return trimmed.length ? trimmed.toUpperCase() : null
}

const normalizeSourcingSources = (
  sources: ProductAttributeSourceDto['sources'] | null | undefined
): ProductSourcedAttributeDto[] => {
  if (!sources) {
    return []
  }

  if (Array.isArray(sources)) {
    return sources.filter((entry): entry is ProductSourcedAttributeDto =>
      Boolean(entry)
    )
  }

  if (sources instanceof Set) {
    return Array.from(sources).filter(
      (entry): entry is ProductSourcedAttributeDto => Boolean(entry)
    )
  }

  if (sources instanceof Map) {
    return Array.from(sources.values()).filter(
      (entry): entry is ProductSourcedAttributeDto => Boolean(entry)
    )
  }

  if (typeof sources === 'object') {
    return Object.values(
      sources as Record<string, ProductSourcedAttributeDto | null | undefined>
    ).filter((entry): entry is ProductSourcedAttributeDto => Boolean(entry))
  }

  return []
}

const normalizeSynonyms = (
  synonyms: AttributeConfigDto['synonyms']
): Array<{ sourceName: string; tokens: string[] }> => {
  if (!synonyms) {
    return []
  }

  return Object.entries(synonyms).reduce<
    Array<{ sourceName: string; tokens: string[] }>
  >((accumulator, [sourceName, values]) => {
    const tokens = toStringList(values)
    if (!sourceName.trim() || !tokens.length) {
      return accumulator
    }

    accumulator.push({ sourceName: sourceName.trim(), tokens })
    return accumulator
  }, [])
}

const buildSynonymSourcing = (
  synonyms: AttributeConfigDto['synonyms']
): ProductAttributeSourceDto | null => {
  const entries = normalizeSynonyms(synonyms)
  if (!entries.length) {
    return null
  }

  const sources = entries.map<ProductSourcedAttributeDto>(entry => ({
    datasourceName: entry.sourceName,
    value: entry.tokens.join(', '),
  }))

  return {
    bestValue: undefined,
    conflicts: false,
    sources: new Set(sources),
  }
}

const buildSynonymTokenSet = (
  synonyms: AttributeConfigDto['synonyms']
): string[] => {
  const tokens = normalizeSynonyms(synonyms).flatMap(entry => entry.tokens)
  return tokens.map(token => token.toLowerCase())
}

const matchesSynonyms = (
  tokens: string[],
  values: Array<string | null | undefined>
): boolean => {
  if (!tokens.length) {
    return false
  }

  const normalizedValues = values
    .filter((value): value is string => typeof value === 'string')
    .map(value => value.toLowerCase())

  return tokens.some(token =>
    normalizedValues.some(value => value.includes(token))
  )
}

interface IdentityDetail {
  key: string
  label: string
  values: string[]
}

interface IdentityRow {
  key: string
  label: string
  value: string
  details?: IdentityDetail[]
}

const identityRows = computed<IdentityRow[]>(() => {
  const rows: IdentityRow[] = []
  const identity = props.product?.identity ?? null

  const brand = firstNonEmptyString(
    identity?.brand,
    referentialAttributes.value.brand
  )
  if (brand) {
    rows.push({
      key: 'brand',
      label: t('product.attributes.main.identity.brand'),
      value: brand,
    })
  }

  const alternativeBrands = toStringList(identity?.akaBrands)
  const alternativeModels = toStringList(identity?.akaModels)

  const model = firstNonEmptyString(
    identity?.model,
    referentialAttributes.value.model
  )
  if (model || alternativeBrands.length || alternativeModels.length) {
    const details: IdentityDetail[] = []

    if (alternativeBrands.length) {
      details.push({
        key: 'akaBrands',
        label: t('product.attributes.main.identity.akaBrands'),
        values: alternativeBrands,
      })
    }

    if (alternativeModels.length) {
      details.push({
        key: 'akaModels',
        label: t('product.attributes.main.identity.akaModels'),
        values: alternativeModels,
      })
    }

    rows.push({
      key: 'model',
      label: t('product.attributes.main.identity.model'),
      value: model ?? '—',
      details: details.length ? details : undefined,
    })
  }

  if (gtinDisplay.value) {
    rows.push({
      key: 'gtin',
      label: t('product.attributes.main.identity.gtin'),
      value: gtinDisplay.value,
    })
  }

  const createdAt = formatDateValue(props.product?.base?.creationDate ?? null)
  if (createdAt) {
    rows.push({
      key: 'knownSince',
      label: t('product.attributes.main.identity.knownSince'),
      value: createdAt,
    })
  }

  const lastUpdated = formatRelativeTimeFromNow(
    props.product?.base?.lastChange ?? null
  )
  if (lastUpdated) {
    rows.push({
      key: 'lastUpdated',
      label: t('product.attributes.main.identity.lastUpdated'),
      value: lastUpdated,
    })
  }

  return rows
})

interface MainAttributeRow {
  key: string
  label: string
  value: string
  sourcing?: ProductAttributeSourceDto | null
}

const resolveDisplayValue = (
  attribute: ProductIndexedAttributeDto | null | undefined,
  fallback: string | null
) => {
  const bestValue = attribute?.sourcing?.bestValue
  if (typeof bestValue === 'string') {
    const trimmed = bestValue.trim()
    if (trimmed.length) {
      return trimmed
    }
  }

  return fallback
}

const formatMainAttributeValue = (
  attribute: ProductIndexedAttributeDto | null | undefined
): string | null => {
  if (!attribute) {
    return null
  }

  if (typeof attribute.value === 'string' && attribute.value.trim().length) {
    return attribute.value.trim()
  }

  if (
    typeof attribute.numericValue === 'number' &&
    Number.isFinite(attribute.numericValue)
  ) {
    return n(attribute.numericValue)
  }

  if (typeof attribute.booleanValue === 'boolean') {
    return t(
      attribute.booleanValue ? 'common.boolean.true' : 'common.boolean.false'
    )
  }

  return null
}

const mainAttributes = computed<MainAttributeRow[]>(() => {
  const entries = resolvedAttributes.value?.indexedAttributes ?? {}

  return Object.entries(entries).reduce<MainAttributeRow[]>(
    (accumulator, [key, attribute]) => {
      const fallbackValue = formatMainAttributeValue(attribute)
      const displayValue = resolveDisplayValue(attribute, fallbackValue)

      if (!displayValue) {
        return accumulator
      }

      const label =
        typeof attribute?.name === 'string' && attribute.name.trim().length
          ? attribute.name.trim()
          : key

      accumulator.push({
        key,
        label,
        value: displayValue,
        sourcing: attribute?.sourcing ?? null,
      })
      return accumulator
    },
    []
  )
})

interface AuditAttributeRow {
  key: string
  name: string
  displayValue: string
  bestValue: string | null
  sourceCount: number
  sourcing: ProductAttributeSourceDto | null
  isIndexed: boolean
  isMatched: boolean
  searchText: string
}

const auditHeaders = computed(() => [
  {
    title: t('product.attributes.audit.columns.attribute'),
    key: 'attribute',
    sortable: true,
  },
  {
    title: t('product.attributes.audit.columns.bestValue'),
    key: 'bestValue',
    sortable: false,
  },
  {
    title: t('product.attributes.audit.columns.sources'),
    key: 'sources',
    sortable: true,
  },
  {
    title: t('product.attributes.audit.columns.indexed'),
    key: 'indexed',
    sortable: true,
  },
])

const auditItemsPerPage = 10

const auditRows = computed<AuditAttributeRow[]>(() => {
  const configs = props.attributeConfigs ?? []
  const indexedAttributes = resolvedAttributes.value?.indexedAttributes ?? {}
  const indexedMap = new Map<string, ProductIndexedAttributeDto>()

  Object.entries(indexedAttributes).forEach(([key, attribute]) => {
    const normalized = normalizeAttributeKey(key)
    if (normalized) {
      indexedMap.set(normalized, attribute)
    }
  })

  return configs.reduce<AuditAttributeRow[]>((accumulator, config) => {
    const normalizedKey = normalizeAttributeKey(config.key)
    if (!normalizedKey) {
      return accumulator
    }

    const indexedAttribute = indexedMap.get(normalizedKey)
    const isIndexed = Boolean(indexedAttribute)
    const bestValue = formatMainAttributeValue(indexedAttribute)
    const displayValue =
      resolveDisplayValue(indexedAttribute, bestValue) ??
      t('product.attributes.audit.noBestValue')

    const name =
      config.name?.trim() || indexedAttribute?.name?.trim() || normalizedKey

    const synonyms = config.synonyms
    const synonymSources = buildSynonymSourcing(synonyms)
    const sourcing = indexedAttribute?.sourcing ?? synonymSources ?? null

    const sourceCount = normalizeSourcingSources(sourcing?.sources).length

    const synonymTokens = buildSynonymTokenSet(synonyms)
    const indexedSources = normalizeSourcingSources(
      indexedAttribute?.sourcing?.sources
    )
    const matchValues = [
      indexedAttribute?.name,
      indexedAttribute?.value,
      indexedAttribute?.sourcing?.bestValue,
      ...indexedSources.map(source => source.name),
      ...indexedSources.map(source => source.value),
    ]

    const isMatched = isIndexed && matchesSynonyms(synonymTokens, matchValues)

    const searchText = `${name} ${bestValue ?? ''}`.trim().toLowerCase()

    accumulator.push({
      key: normalizedKey,
      name,
      displayValue,
      bestValue: bestValue ?? null,
      sourceCount,
      sourcing,
      isIndexed,
      isMatched,
      searchText,
    })

    return accumulator
  }, [])
})

const filteredAuditRows = computed(() => {
  const term = auditSearchTerm.value.trim().toLowerCase()
  const showIndexedValue = showIndexed.value
  const showNotIndexedValue = showNotIndexed.value

  return auditRows.value.filter(row => {
    if (row.isIndexed && !showIndexedValue) {
      return false
    }

    if (!row.isIndexed && !showNotIndexedValue) {
      return false
    }

    if (!term) {
      return true
    }

    return row.searchText.includes(term)
  })
})

const auditRowClass = (item: AuditAttributeRow) => {
  if (!item.isIndexed) {
    return 'product-attributes__audit-row product-attributes__audit-row--unindexed'
  }

  if (item.isMatched) {
    return 'product-attributes__audit-row product-attributes__audit-row--matched'
  }

  return 'product-attributes__audit-row product-attributes__audit-row--indexed'
}

export interface DetailAttributeView {
  key: string
  name: string
  value: string
  sourcing?: ProductAttributeSourceDto | null
}

export interface DetailGroupView {
  id: string
  name: string
  attributes: DetailAttributeView[]
  features: DetailAttributeView[]
  unFeatures: DetailAttributeView[]
  totalCount: number
}

const sanitizeAttributeList = (
  items: Array<ProductAttributeDto | null | undefined> | undefined,
  prefix: string
): DetailAttributeView[] => {
  if (!items?.length) {
    return []
  }

  return items.reduce<DetailAttributeView[]>(
    (accumulator, attribute, index) => {
      if (!attribute) {
        return accumulator
      }

      const rawValue =
        typeof attribute.value === 'string' ? attribute.value.trim() : ''
      if (!rawValue.length) {
        return accumulator
      }

      const name =
        typeof attribute.name === 'string' && attribute.name.trim().length
          ? attribute.name.trim()
          : t('product.attributes.detailed.unknownLabel')

      const bestValue = attribute.sourcing?.bestValue
      const displayValue =
        typeof bestValue === 'string' && bestValue.trim().length
          ? bestValue.trim()
          : rawValue

      accumulator.push({
        key: `${prefix}-${index}`,
        name,
        value: displayValue,
        sourcing: attribute.sourcing ?? null,
      })

      return accumulator
    },
    []
  )
}

const classifiedGroups = computed(
  () => resolvedAttributes.value?.classifiedAttributes ?? []
)

const baseGroups = computed<DetailGroupView[]>(() =>
  classifiedGroups.value
    .map((group, index) => {
      if (!group) {
        return null
      }

      const name =
        typeof group.name === 'string' && group.name.trim().length
          ? group.name.trim()
          : t('product.attributes.detailed.untitledGroup')

      const attributes = sanitizeAttributeList(
        group.attributes,
        `group-${index}-attr`
      )
      const features = sanitizeAttributeList(
        group.features,
        `group-${index}-feature`
      )
      const unFeatures = sanitizeAttributeList(
        group.unFeatures,
        `group-${index}-unfeature`
      )

      const totalCount = attributes.length + features.length + unFeatures.length
      if (!totalCount) {
        return null
      }

      return {
        id: `${index}-${name}`,
        name,
        attributes,
        features,
        unFeatures,
        totalCount,
      }
    })
    .filter((group): group is DetailGroupView => Boolean(group))
)

const filteredGroups = computed<DetailGroupView[]>(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (!term) {
    return baseGroups.value
  }

  const filterList = (items: DetailAttributeView[]) =>
    items.filter(item =>
      `${item.name} ${item.value}`.toLowerCase().includes(term)
    )

  return baseGroups.value
    .map(group => {
      const matchesGroupName = group.name.toLowerCase().includes(term)

      if (matchesGroupName) {
        return group
      }

      const attributes = filterList(group.attributes)
      const features = filterList(group.features)
      const unFeatures = filterList(group.unFeatures)

      const totalCount = attributes.length + features.length + unFeatures.length
      if (!totalCount) {
        return null
      }

      return {
        ...group,
        attributes,
        features,
        unFeatures,
        totalCount,
      }
    })
    .filter((group): group is DetailGroupView => Boolean(group))
})
</script>

<style scoped>
.product-attributes {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.product-attributes__header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-attributes__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  max-width: 60ch;
}

.product-attributes__block {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-attributes__block-title {
  font-size: clamp(1.1rem, 1.8vw, 1.4rem);
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__main-grid {
  display: grid;
  gap: 1.25rem;
  grid-template-columns: 1fr;
}

.product-attributes__identity-card,
.product-attributes__main-card {
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6);
  box-shadow: 0 10px 25px -12px rgba(15, 23, 42, 0.15);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-attributes__identity-heading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__identity-icon {
  color: rgb(var(--v-theme-primary));
}

.product-attributes__identity-table {
  display: grid;
  gap: 0.85rem;
}

.product-attributes__identity-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 0.5rem;
  align-items: start;
}

.product-attributes__identity-label {
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-attributes__identity-value {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__identity-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-attributes__identity-detail {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-attributes__identity-detail-label {
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-attributes__identity-detail-list {
  margin: 0;
  padding-inline-start: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  list-style: disc;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__gtin {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-attributes__gtin-image {
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6);
  min-height: 160px;
  background: rgb(var(--v-theme-surface-primary-080));
  width: min(180px, 33%);
  align-self: center;
}

.product-attributes__gtin-caption {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-attributes__table {
  border-radius: 12px;
  overflow: hidden;
}

.product-attributes__table tbody tr:nth-child(odd) {
  background: rgba(var(--v-theme-surface-primary-050), 0.7);
}

.product-attributes__table th {
  text-align: left;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  padding: 0.6rem 0.75rem;
}

.product-attributes__table td {
  padding: 0.6rem 0.75rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__table--main {
  max-height: 420px;
  overflow: auto;
}

.product-attributes__block-header--detailed {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-attributes__block-header--audit {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-attributes__audit-heading {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-attributes__audit-subtitle {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-attributes__audit-controls {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-attributes__audit-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.product-attributes__audit-filter {
  margin: 0;
}

.product-attributes__audit-table {
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
}

.product-attributes__audit-attribute {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.product-attributes__audit-name {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__audit-key {
  font-size: 0.8rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-attributes__audit-value {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.product-attributes__audit-count {
  font-weight: 600;
}

.product-attributes__audit-row {
  transition: background-color 0.2s ease;
}

.product-attributes__audit-row--indexed {
  background: rgba(var(--v-theme-surface-primary-050), 0.5);
}

.product-attributes__audit-row--matched {
  background: rgba(var(--v-theme-accent-supporting), 0.15);
}

.product-attributes__audit-row--unindexed {
  background: rgba(var(--v-theme-surface-muted), 0.4);
}

.product-attributes__search {
  width: 100%;
  max-width: 100%;
}

.product-attributes__search--audit {
  max-width: 420px;
}

.product-attributes__detailed-layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-attributes__details-panel {
  flex: 1;
}

.product-attributes__details-grid {
  margin: 0 -0.75rem;
}

.product-attributes__table-value {
  display: inline-block;
  max-width: 100%;
}

.product-attributes__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-attributes__identity-empty {
  font-style: italic;
}

.product-attributes__empty--detailed {
  padding: 1rem 1.25rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-050), 0.7);
}

.product-attributes__empty--audit {
  padding: 1rem 1.25rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-050), 0.7);
}

.product-attributes__timeline-row {
  margin-top: 1.5rem;
}

@media (min-width: 960px) {
  .product-attributes__main-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
  }

  .product-attributes__block-header--detailed {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .product-attributes__block-header--audit {
    flex-direction: row;
    align-items: flex-start;
    justify-content: space-between;
  }

  .product-attributes__audit-controls {
    align-items: flex-end;
  }

  .product-attributes__search {
    max-width: 320px;
  }

  .product-attributes__search--audit {
    max-width: 360px;
  }
}
</style>
