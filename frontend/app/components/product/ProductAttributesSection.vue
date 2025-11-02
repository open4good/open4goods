<template>
  <section :id="sectionId" class="product-attributes">
    <header class="product-attributes__header">
      <h2 class="product-attributes__title">
        {{ $t('product.attributes.title') }}
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
            <v-icon icon="mdi-card-account-details-outline" class="product-attributes__identity-icon" />
            <span>{{ $t('product.attributes.main.identity.title') }}</span>
          </div>

          <div v-if="identityRows.length" class="product-attributes__identity-table">
            <div
              v-for="row in identityRows"
              :key="row.key"
              class="product-attributes__identity-row"
            >
              <span class="product-attributes__identity-label">{{ row.label }}</span>
              <span class="product-attributes__identity-value">{{ row.value }}</span>
            </div>
          </div>
          <p v-else-if="!gtinImageUrl" class="product-attributes__empty product-attributes__identity-empty">
            {{ $t('product.attributes.main.identity.empty') }}
          </p>

          <figure v-if="gtinImageUrl" class="product-attributes__gtin">
            <v-img
              :src="gtinImageUrl"
              :alt="$t('product.attributes.main.identity.gtinImageAlt', { gtin: gtinDisplay ?? '—' })"
              class="product-attributes__gtin-image"
              cover
            />
            <figcaption class="product-attributes__gtin-caption">
              {{ $t('product.attributes.main.identity.gtinLabel') }}
            </figcaption>
          </figure>
        </v-card>

        <v-card class="product-attributes__main-card" variant="flat">
          <div class="product-attributes__card-header">
            <h4>{{ $t('product.attributes.main.attributes.title') }}</h4>
          </div>
          <v-table
            v-if="mainAttributes.length"
            density="comfortable"
            class="product-attributes__table product-attributes__table--main"
          >
            <tbody>
              <tr v-for="attribute in mainAttributes" :key="attribute.key">
                <th scope="row">{{ attribute.label }}</th>
                <td>{{ attribute.value }}</td>
              </tr>
            </tbody>
          </v-table>
          <p v-else class="product-attributes__empty">
            {{ $t('product.attributes.main.attributes.empty') }}
          </p>
        </v-card>
      </div>
    </div>

    <div class="product-attributes__block product-attributes__block--detailed">
      <div class="product-attributes__block-header product-attributes__block-header--detailed">
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

      <div v-if="filteredGroups.length" class="product-attributes__details-grid">
        <v-card
          v-for="group in filteredGroups"
          :key="group.id"
          class="product-attributes__detail-card"
          variant="flat"
        >
          <header class="product-attributes__detail-header">
            <h4>{{ group.name }}</h4>
            <v-chip size="small" variant="tonal" color="primary">
              {{ group.totalCount }}
            </v-chip>
          </header>

          <div
            v-if="group.features.length"
            class="product-attributes__feature-list product-attributes__feature-list--positive"
          >
            <p class="product-attributes__feature-title">
              <v-icon icon="mdi-check-circle-outline" class="product-attributes__feature-icon" />
              <span>{{ $t('product.attributes.features') }}</span>
            </p>
            <ul>
              <li v-for="feature in group.features" :key="feature.key">
                <span class="product-attributes__feature-name">{{ feature.name }}</span>
                <span class="product-attributes__feature-value">{{ feature.value }}</span>
              </li>
            </ul>
          </div>

          <div
            v-if="group.unFeatures.length"
            class="product-attributes__feature-list product-attributes__feature-list--negative"
          >
            <p class="product-attributes__feature-title">
              <v-icon icon="mdi-alert-circle-outline" class="product-attributes__feature-icon" />
              <span>{{ $t('product.attributes.unfeatures') }}</span>
            </p>
            <ul>
              <li v-for="feature in group.unFeatures" :key="feature.key">
                <span class="product-attributes__feature-name">{{ feature.name }}</span>
                <span class="product-attributes__feature-value">{{ feature.value }}</span>
              </li>
            </ul>
          </div>

          <v-table
            v-if="group.attributes.length"
            density="comfortable"
            class="product-attributes__table"
          >
            <tbody>
              <tr v-for="attribute in group.attributes" :key="attribute.key">
                <th scope="row">{{ attribute.name }}</th>
                <td>{{ attribute.value }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </div>

      <p v-else class="product-attributes__empty product-attributes__empty--detailed">
        {{
          $t(
            hasSearchTerm
              ? 'product.attributes.detailed.noResults'
              : 'product.attributes.detailed.empty',
          )
        }}
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  ProductAttributeDto,
  ProductAttributesDto,
  ProductDto,
  ProductIndexedAttributeDto,
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
  product: {
    type: Object as PropType<ProductDto | null>,
    default: null,
  },
})

const { t, n, locale } = useI18n()
const runtimeConfig = useRuntimeConfig()

const searchTerm = ref('')
const hasSearchTerm = computed(() => searchTerm.value.trim().length > 0)

const resolvedAttributes = computed<ProductAttributesDto | null>(() => {
  if (props.attributes) {
    return props.attributes
  }

  return props.product?.attributes ?? null
})

const referentialAttributes = computed<Record<string, string>>(
  () => resolvedAttributes.value?.referentialAttributes ?? {},
)

const staticServerBase = computed(() => {
  const fallback = 'https://nudger.fr'
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

const RELATIVE_TIME_UNITS: Array<{ unit: Intl.RelativeTimeFormatUnit; ms: number }> = [
  { unit: 'year', ms: 1000 * 60 * 60 * 24 * 365 },
  { unit: 'month', ms: 1000 * 60 * 60 * 24 * 30 },
  { unit: 'week', ms: 1000 * 60 * 60 * 24 * 7 },
  { unit: 'day', ms: 1000 * 60 * 60 * 24 },
  { unit: 'hour', ms: 1000 * 60 * 60 },
  { unit: 'minute', ms: 1000 * 60 },
  { unit: 'second', ms: 1000 },
]

const formatRelativeTimeFromNow = (timestamp?: number | null): string | null => {
  if (typeof timestamp !== 'number' || !Number.isFinite(timestamp)) {
    return null
  }

  const now = Date.now()
  const diff = timestamp - now
  const formatter = new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' })

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
    return new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }).format(
      new Date(timestamp),
    )
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
      .map((entry) => (typeof entry === 'string' ? entry.trim() : ''))
      .filter((entry) => entry.length > 0)
  }

  if (Array.isArray(input)) {
    return input
      .map((entry) => (typeof entry === 'string' ? entry.trim() : ''))
      .filter((entry) => entry.length > 0)
  }

  if (typeof input === 'string') {
    const trimmed = input.trim()
    return trimmed.length ? [trimmed] : []
  }

  return []
}

interface IdentityRow {
  key: string
  label: string
  value: string
}

const identityRows = computed<IdentityRow[]>(() => {
  const rows: IdentityRow[] = []
  const identity = props.product?.identity ?? null

  const brand = firstNonEmptyString(identity?.brand, referentialAttributes.value.brand)
  if (brand) {
    rows.push({
      key: 'brand',
      label: t('product.attributes.main.identity.brand'),
      value: brand,
    })
  }

  const otherBrands = toStringList(identity?.akaBrands)
  if (otherBrands.length) {
    rows.push({
      key: 'akaBrands',
      label: t('product.attributes.main.identity.akaBrands'),
      value: otherBrands.join(', '),
    })
  }

  const model = firstNonEmptyString(identity?.model, referentialAttributes.value.model)
  if (model) {
    rows.push({
      key: 'model',
      label: t('product.attributes.main.identity.model'),
      value: model,
    })
  }

  const otherNames = toStringList(identity?.akaModels)
  if (otherNames.length) {
    rows.push({
      key: 'akaModels',
      label: t('product.attributes.main.identity.akaModels'),
      value: otherNames.join(', '),
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

  const lastUpdated = formatRelativeTimeFromNow(props.product?.base?.lastChange ?? null)
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
}

const formatMainAttributeValue = (
  attribute: ProductIndexedAttributeDto | null | undefined,
): string | null => {
  if (!attribute) {
    return null
  }

  if (typeof attribute.value === 'string' && attribute.value.trim().length) {
    return attribute.value.trim()
  }

  if (typeof attribute.numericValue === 'number' && Number.isFinite(attribute.numericValue)) {
    return n(attribute.numericValue)
  }

  if (typeof attribute.booleanValue === 'boolean') {
    return t(attribute.booleanValue ? 'common.boolean.true' : 'common.boolean.false')
  }

  return null
}

const mainAttributes = computed<MainAttributeRow[]>(() => {
  const entries = resolvedAttributes.value?.indexedAttributes ?? {}

  return Object.entries(entries).reduce<MainAttributeRow[]>((accumulator, [key, attribute]) => {
    const value = formatMainAttributeValue(attribute)
    if (!value) {
      return accumulator
    }

    const label =
      typeof attribute?.name === 'string' && attribute.name.trim().length
        ? attribute.name.trim()
        : key

    accumulator.push({ key, label, value })
    return accumulator
  }, [])
})

interface DetailAttributeView {
  key: string
  name: string
  value: string
}

interface DetailGroupView {
  id: string
  name: string
  attributes: DetailAttributeView[]
  features: DetailAttributeView[]
  unFeatures: DetailAttributeView[]
  totalCount: number
}

const sanitizeAttributeList = (
  items: Array<ProductAttributeDto | null | undefined> | undefined,
  prefix: string,
): DetailAttributeView[] => {
  if (!items?.length) {
    return []
  }

  return items.reduce<DetailAttributeView[]>((accumulator, attribute, index) => {
    if (!attribute) {
      return accumulator
    }

    const rawValue = typeof attribute.value === 'string' ? attribute.value.trim() : ''
    if (!rawValue.length) {
      return accumulator
    }

    const name =
      typeof attribute.name === 'string' && attribute.name.trim().length
        ? attribute.name.trim()
        : t('product.attributes.detailed.unknownLabel')

    accumulator.push({
      key: `${prefix}-${index}`,
      name,
      value: rawValue,
    })

    return accumulator
  }, [])
}

const classifiedGroups = computed(() => resolvedAttributes.value?.classifiedAttributes ?? [])

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

      const attributes = sanitizeAttributeList(group.attributes, `group-${index}-attr`)
      const features = sanitizeAttributeList(group.features, `group-${index}-feature`)
      const unFeatures = sanitizeAttributeList(group.unFeatures, `group-${index}-unfeature`)

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
    .filter((group): group is DetailGroupView => Boolean(group)),
)

const filteredGroups = computed<DetailGroupView[]>(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (!term) {
    return baseGroups.value
  }

  const filterList = (items: DetailAttributeView[]) =>
    items.filter((item) => `${item.name} ${item.value}`.toLowerCase().includes(term))

  return baseGroups.value
    .map((group) => {
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
.product-attributes__main-card,
.product-attributes__detail-card {
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
}

.product-attributes__gtin-caption {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-attributes__card-header h4 {
  margin: 0;
  font-size: clamp(1rem, 1.6vw, 1.25rem);
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
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

.product-attributes__search {
  width: 100%;
  max-width: 100%;
}

.product-attributes__details-grid {
  display: grid;
  gap: 1.25rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.product-attributes__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-attributes__detail-header h4 {
  font-size: clamp(1rem, 1.4vw, 1.2rem);
  font-weight: 600;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__feature-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-primary-080), 0.8);
}

.product-attributes__feature-list--positive {
  border-left: 4px solid rgba(var(--v-theme-success), 0.65);
}

.product-attributes__feature-list--negative {
  border-left: 4px solid rgba(var(--v-theme-error), 0.65);
  background: rgba(var(--v-theme-surface-primary-050), 0.6);
}

.product-attributes__feature-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__feature-icon {
  font-size: 1.1rem;
}

.product-attributes__feature-list ul {
  margin: 0;
  padding-left: 1rem;
  display: grid;
  gap: 0.35rem;
}

.product-attributes__feature-name {
  font-weight: 600;
}

.product-attributes__feature-value {
  display: block;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  margin-top: 0.15rem;
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

@media (min-width: 960px) {
  .product-attributes__main-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
  }

  .product-attributes__block-header--detailed {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .product-attributes__search {
    max-width: 320px;
  }
}
</style>
