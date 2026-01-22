<template>
  <section :id="sectionId" class="product-attributes">
    <header class="product-attributes__header">
      <h2 class="product-attributes__title">
        {{ $t('product.attributes.title', titleParams) }}
      </h2>
      <p
        v-if="technicalShortReviewHtml"
        class="product-attributes__subtitle"
        v-html="technicalShortReviewHtml"
      />
      <p v-else class="product-attributes__subtitle">
        {{ $t('product.attributes.subtitle') }}
      </p>
    </header>

    <div class="product-attributes__block">
      <div class="product-attributes__block-header">
        <h3 id="attributes-main" class="product-attributes__block-title">
          {{ $t('product.attributes.main.title') }}
        </h3>
      </div>

      <div class="product-attributes__main-grid">
        <v-card class="product-attributes__identity-card" variant="flat">
          <div class="product-attributes__identity-heading">
            <v-icon
              icon="mdi-card-account-details-outline"
              size="20"
              class="product-attributes__identity-icon"
            />
            <span>{{ $t('product.attributes.main.identity.title') }}</span>
          </div>

          <div
            v-if="hasIdentityData"
            class="product-attributes__identity-table"
          >
            <div v-if="identityBrand" class="product-attributes__identity-row">
              <span class="product-attributes__identity-label">
                {{ $t('product.attributes.main.identity.brand') }}
              </span>
              <div class="product-attributes__identity-value">
                <span>{{ identityBrand }}</span>
                <div
                  v-if="akaBrands.length"
                  class="product-attributes__identity-details"
                >
                  <span class="product-attributes__identity-detail-label">
                    {{ $t('product.attributes.main.identity.akaBrands') }}
                  </span>
                  <ul class="product-attributes__identity-detail-list">
                    <li v-for="brand in akaBrands" :key="brand">
                      {{ brand }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <div v-if="identityModel" class="product-attributes__identity-row">
              <span class="product-attributes__identity-label">
                {{ $t('product.attributes.main.identity.model') }}
              </span>
              <div class="product-attributes__identity-value">
                <span>{{ identityModel }}</span>
                <div
                  v-if="akaModels.length"
                  class="product-attributes__identity-details"
                >
                  <span class="product-attributes__identity-detail-label">
                    {{ $t('product.attributes.main.identity.akaModels') }}
                  </span>
                  <ul class="product-attributes__identity-detail-list">
                    <li v-for="model in akaModels" :key="model">
                      {{ model }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>

          

            <div v-if="knownSince" class="product-attributes__identity-row">
              <span class="product-attributes__identity-label">
                {{ $t('product.attributes.main.identity.knownSince') }}
              </span>
              <span
                class="product-attributes__identity-value product-attributes__identity-detail--muted"
              >
                {{ knownSince }}
              </span>
            </div>

            <div v-if="lastUpdated" class="product-attributes__identity-row">
              <span class="product-attributes__identity-label">
                {{ $t('product.attributes.main.identity.lastUpdated') }}
              </span>
              <span
                class="product-attributes__identity-value product-attributes__identity-detail--muted"
              >
                {{ lastUpdated }}
              </span>
            </div>


  <div v-if="gtin" class="product-attributes__identity-row">
              <span class="product-attributes__identity-label">
                {{ $t('product.attributes.main.identity.gtin') }}
              </span>
              <div
                class="product-attributes__identity-value product-attributes__gtin"
              >
                <img
                  v-if="gtinBarcodeUrl"
                  :src="gtinBarcodeUrl"
                  :alt="gtin"
                  class="product-attributes__gtin-image"
                />
                <span>{{ gtin }}</span>
                <span class="product-attributes__gtin-caption">
                  {{ $t('product.attributes.main.identity.gtinLabel') }}
                </span>
              </div>
            </div>

          </div>

          <p
            v-else
            class="product-attributes__empty product-attributes__identity-empty"
          >
            {{ $t('product.attributes.main.identity.empty') }}
          </p>
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
          <ProductLifeTimeline id="attributes-timeline" :timeline="timeline" />
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
            :color="item.isIndexed ? 'primary' : 'error'"
            class="product-attributes__audit-chip"
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
        <div
          class="product-attributes__header-section product-attributes__header-section--left"
        >
          <h3 id="attributes-details" class="product-attributes__block-title">
            {{ $t('product.attributes.detailed.title') }}
          </h3>
        </div>

        <div
          class="product-attributes__header-section product-attributes__header-section--center"
        >
          <v-text-field
            v-model="searchTerm"
            :label="$t('product.attributes.searchPlaceholder')"
            prepend-inner-icon="mdi-magnify"
            hide-details
            clearable
            class="product-attributes__search"
          />
        </div>

        <div
          class="product-attributes__header-section product-attributes__header-section--right"
        >
          <v-btn-toggle
            v-model="detailViewMode"
            mandatory
            class="product-attributes__view-toggle"
          >
            <v-btn
              value="table"
              :aria-label="$t('product.attributes.detailed.viewTable')"
            >
              <v-icon icon="mdi-view-list" />
              <v-tooltip
                activator="parent"
                location="bottom"
                :text="$t('product.attributes.detailed.tooltips.viewTable')"
              />
            </v-btn>
            <v-btn
              value="cards"
              :aria-label="$t('product.attributes.detailed.viewCards')"
            >
              <v-icon icon="mdi-view-grid" />
              <v-tooltip
                activator="parent"
                location="bottom"
                :text="$t('product.attributes.detailed.tooltips.viewCards')"
              />
            </v-btn>
          </v-btn-toggle>
        </div>
      </div>

      <div class="product-attributes__detailed-layout">
        <div class="product-attributes__details-panel">
          <template v-if="filteredGroups.length">
            <template v-if="detailViewMode === 'table'">
              <v-row
                v-if="shouldSplitDetailTables"
                class="product-attributes__details-table-grid"
                dense
              >
                <v-col cols="12" md="6">
                  <v-data-table
                    v-model:expanded="expandedDetailGroups"
                    :headers="detailTableHeaders"
                    :items="detailTableItemsLeft"
                    item-value="id"
                    :items-per-page="detailItemsPerPage"
                    class="product-attributes__details-table"
                    density="comfortable"
                    hide-default-footer
                  >
                    <template #[`item.name`]="{ item }">
                      <div class="product-attributes__details-label">
                        <v-btn
                          class="product-attributes__details-toggle"
                          icon
                          density="comfortable"
                          variant="text"
                          :aria-label="
                            isDetailGroupExpanded(item.id)
                              ? $t('product.attributes.detailed.hideDetails')
                              : $t('product.attributes.detailed.showDetails')
                          "
                          @click="toggleDetailGroup(item.id)"
                        >
                          <v-icon
                            :icon="
                              isDetailGroupExpanded(item.id)
                                ? 'mdi-chevron-up'
                                : 'mdi-chevron-down'
                            "
                            size="18"
                          />
                        </v-btn>
                        <span>{{ item.name }}</span>
                      </div>
                    </template>
                    <template #[`item.totalCount`]="{ item }">
                      <v-chip size="small" variant="tonal" color="primary">
                        {{ item.totalCount }}
                      </v-chip>
                    </template>
                    <template #expanded-row="{ columns, item }">
                      <tr class="product-attributes__details-expanded-row">
                        <td :colspan="columns.length">
                          <div
                            class="product-attributes__details-expanded-content"
                          >
                            <div
                              v-if="item.features.length"
                              class="product-attributes__chip-list product-attributes__chip-list--positive"
                            >
                              <ul>
                                <li
                                  v-for="feature in item.features"
                                  :key="feature.key"
                                >
                                  <v-icon
                                    icon="mdi-check-circle"
                                    size="18"
                                    class="product-attributes__chip-icon product-attributes__chip-icon--positive"
                                  />
                                  <span
                                    class="product-attributes__chip-label"
                                    >{{ feature.name }}</span
                                  >
                                </li>
                              </ul>
                            </div>

                            <div
                              v-if="item.unFeatures.length"
                              class="product-attributes__chip-list product-attributes__chip-list--negative"
                            >
                              <ul>
                                <li
                                  v-for="feature in item.unFeatures"
                                  :key="feature.key"
                                >
                                  <v-icon
                                    icon="mdi-close-octagon-outline"
                                    size="18"
                                    class="product-attributes__chip-icon product-attributes__chip-icon--negative"
                                  />
                                  <span
                                    class="product-attributes__chip-label"
                                    >{{ feature.name }}</span
                                  >
                                </li>
                              </ul>
                            </div>

                            <v-table
                              v-if="item.attributes.length"
                              density="comfortable"
                              class="product-attributes__table product-attributes__details-attributes-table"
                            >
                              <tbody>
                                <tr
                                  v-for="attribute in item.attributes"
                                  :key="attribute.key"
                                >
                                  <th scope="row">{{ attribute.name }}</th>
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
                          </div>
                        </td>
                      </tr>
                    </template>
                  </v-data-table>
                </v-col>

                <v-col cols="12" md="6">
                  <v-data-table
                    v-model:expanded="expandedDetailGroups"
                    :headers="detailTableHeaders"
                    :items="detailTableItemsRight"
                    item-value="id"
                    :items-per-page="detailItemsPerPage"
                    class="product-attributes__details-table"
                    density="comfortable"
                    hide-default-footer
                  >
                    <template #[`item.name`]="{ item }">
                      <div class="product-attributes__details-label">
                        <v-btn
                          class="product-attributes__details-toggle"
                          icon
                          density="comfortable"
                          variant="text"
                          :aria-label="
                            isDetailGroupExpanded(item.id)
                              ? $t('product.attributes.detailed.hideDetails')
                              : $t('product.attributes.detailed.showDetails')
                          "
                          @click="toggleDetailGroup(item.id)"
                        >
                          <v-icon
                            :icon="
                              isDetailGroupExpanded(item.id)
                                ? 'mdi-chevron-up'
                                : 'mdi-chevron-down'
                            "
                            size="18"
                          />
                        </v-btn>
                        <span>{{ item.name }}</span>
                      </div>
                    </template>
                    <template #[`item.totalCount`]="{ item }">
                      <v-chip size="small" variant="tonal" color="primary">
                        {{ item.totalCount }}
                      </v-chip>
                    </template>
                    <template #expanded-row="{ columns, item }">
                      <tr class="product-attributes__details-expanded-row">
                        <td :colspan="columns.length">
                          <div
                            class="product-attributes__details-expanded-content"
                          >
                            <div
                              v-if="item.features.length"
                              class="product-attributes__chip-list product-attributes__chip-list--positive"
                            >
                              <ul>
                                <li
                                  v-for="feature in item.features"
                                  :key="feature.key"
                                >
                                  <v-icon
                                    icon="mdi-check-circle"
                                    size="18"
                                    class="product-attributes__chip-icon product-attributes__chip-icon--positive"
                                  />
                                  <span
                                    class="product-attributes__chip-label"
                                    >{{ feature.name }}</span
                                  >
                                </li>
                              </ul>
                            </div>

                            <div
                              v-if="item.unFeatures.length"
                              class="product-attributes__chip-list product-attributes__chip-list--negative"
                            >
                              <ul>
                                <li
                                  v-for="feature in item.unFeatures"
                                  :key="feature.key"
                                >
                                  <v-icon
                                    icon="mdi-close-octagon-outline"
                                    size="18"
                                    class="product-attributes__chip-icon product-attributes__chip-icon--negative"
                                  />
                                  <span
                                    class="product-attributes__chip-label"
                                    >{{ feature.name }}</span
                                  >
                                </li>
                              </ul>
                            </div>

                            <v-table
                              v-if="item.attributes.length"
                              density="comfortable"
                              class="product-attributes__table product-attributes__details-attributes-table"
                            >
                              <tbody>
                                <tr
                                  v-for="attribute in item.attributes"
                                  :key="attribute.key"
                                >
                                  <th scope="row">{{ attribute.name }}</th>
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
                          </div>
                        </td>
                      </tr>
                    </template>
                  </v-data-table>
                </v-col>
              </v-row>

              <v-data-table
                v-else
                v-model:expanded="expandedDetailGroups"
                :headers="detailTableHeaders"
                :items="detailTableItems"
                item-value="id"
                :items-per-page="detailItemsPerPage"
                class="product-attributes__details-table"
                density="comfortable"
                hide-default-footer
              >
                <template #[`item.name`]="{ item }">
                  <div class="product-attributes__details-label">
                    <v-btn
                      class="product-attributes__details-toggle"
                      icon
                      density="comfortable"
                      variant="text"
                      :aria-label="
                        isDetailGroupExpanded(item.id)
                          ? $t('product.attributes.detailed.hideDetails')
                          : $t('product.attributes.detailed.showDetails')
                      "
                      @click="toggleDetailGroup(item.id)"
                    >
                      <v-icon
                        :icon="
                          isDetailGroupExpanded(item.id)
                            ? 'mdi-chevron-up'
                            : 'mdi-chevron-down'
                        "
                        size="18"
                      />
                    </v-btn>
                    <span>{{ item.name }}</span>
                  </div>
                </template>
                <template #[`item.totalCount`]="{ item }">
                  <v-chip size="small" variant="tonal" color="primary">
                    {{ item.totalCount }}
                  </v-chip>
                </template>
                <template #expanded-row="{ columns, item }">
                  <tr class="product-attributes__details-expanded-row">
                    <td :colspan="columns.length">
                      <div class="product-attributes__details-expanded-content">
                        <div
                          v-if="item.features.length"
                          class="product-attributes__chip-list product-attributes__chip-list--positive"
                        >
                          <ul>
                            <li
                              v-for="feature in item.features"
                              :key="feature.key"
                            >
                              <v-icon
                                icon="mdi-check-circle"
                                size="18"
                                class="product-attributes__chip-icon product-attributes__chip-icon--positive"
                              />
                              <span class="product-attributes__chip-label">{{
                                feature.name
                              }}</span>
                            </li>
                          </ul>
                        </div>

                        <div
                          v-if="item.unFeatures.length"
                          class="product-attributes__chip-list product-attributes__chip-list--negative"
                        >
                          <ul>
                            <li
                              v-for="feature in item.unFeatures"
                              :key="feature.key"
                            >
                              <v-icon
                                icon="mdi-close-octagon-outline"
                                size="18"
                                class="product-attributes__chip-icon product-attributes__chip-icon--negative"
                              />
                              <span class="product-attributes__chip-label">{{
                                feature.name
                              }}</span>
                            </li>
                          </ul>
                        </div>

                        <v-table
                          v-if="item.attributes.length"
                          density="comfortable"
                          class="product-attributes__table product-attributes__details-attributes-table"
                        >
                          <tbody>
                            <tr
                              v-for="attribute in item.attributes"
                              :key="attribute.key"
                            >
                              <th scope="row">{{ attribute.name }}</th>
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
                      </div>
                    </td>
                  </tr>
                </template>
              </v-data-table>
            </template>

            <v-row v-else class="product-attributes__details-grid" dense>
              <ProductAttributesDetailCard
                v-for="group in filteredGroups"
                :key="group.id"
                :group="group"
              />
            </v-row>
          </template>

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
import { _sanitizeHtml } from '~~/shared/utils/sanitizer'
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
  titleParams: {
    type: Object as PropType<Record<string, string> | undefined>,
    default: undefined,
  },
})

const { t, n, locale } = useI18n()
const { isLoggedIn } = useAuth()

const searchTerm = ref('')
const hasSearchTerm = computed(() => searchTerm.value.trim().length > 0)
const detailViewMode = ref<'table' | 'cards'>('table')
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

const timeline = computed(() => props.product?.timeline ?? null)
const identity = computed(() => props.product?.identity ?? null)
const gtin = computed(
  () => props.product?.gtin ?? props.product?.base?.gtin ?? null
)
const technicalShortReview = computed(
  () => props.product?.aiReview?.review?.technicalShortReview?.trim() ?? ''
)
const technicalShortReviewHtml = computed(() => {
  if (!technicalShortReview.value) {
    return ''
  }

  const { sanitizedHtml } = _sanitizeHtml(technicalShortReview.value)
  return sanitizedHtml.value || technicalShortReview.value
})

const gtinBarcodeUrl = computed(() => {
  if (!gtin.value) {
    return null
  }

  return `https://bwipjs-api.metafloor.com/?bcid=ean13&text=${gtin.value}&scale=3&height=10&includetext`
})

const normalizeIdentityValue = (
  value: string | null | undefined
): string | null => {
  if (typeof value !== 'string') {
    return null
  }

  const trimmed = value.trim()
  return trimmed.length ? trimmed : null
}

const identityBrand = computed(() =>
  normalizeIdentityValue(identity.value?.brand ?? null)
)
const identityModel = computed(() =>
  normalizeIdentityValue(identity.value?.model ?? null)
)

const akaBrands = computed(() =>
  toStringList(identity.value?.akaBrands).sort((a, b) =>
    a.localeCompare(b, locale.value)
  )
)
const akaModels = computed(() =>
  toStringList(identity.value?.akaModels).sort((a, b) =>
    a.localeCompare(b, locale.value)
  )
)

const formatIdentityDate = (timestamp?: number | null) => {
  if (typeof timestamp !== 'number' || Number.isNaN(timestamp)) {
    return null
  }

  return new Intl.DateTimeFormat(locale.value, { dateStyle: 'medium' }).format(
    new Date(timestamp)
  )
}

const knownSince = computed(() =>
  formatIdentityDate(props.product?.base?.creationDate)
)
const lastUpdated = computed(() =>
  formatIdentityDate(props.product?.base?.lastChange)
)

const hasIdentityData = computed(
  () =>
    Boolean(identityBrand.value) ||
    Boolean(identityModel.value) ||
    akaBrands.value.length > 0 ||
    akaModels.value.length > 0 ||
    Boolean(gtin.value) ||
    Boolean(knownSince.value) ||
    Boolean(lastUpdated.value)
)

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

const buildSynonymTokenSet = (
  synonyms: AttributeConfigDto['synonyms']
): string[] => {
  const tokens = normalizeSynonyms(synonyms).reduce<string[]>(
    (acc, entry) => acc.concat(entry.tokens),
    []
  )
  return tokens.map(token => token.toLowerCase())
}

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
  const entries =
    (resolvedAttributes.value?.indexedAttributes as Record<
      string,
      ProductIndexedAttributeDto
    >) ?? {}

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

const checkAttributeMatchesConfig = (
  attribute: ProductAttributeDto,
  config: AttributeConfigDto
): boolean => {
  // Check exact name match
  const rawName = attribute.name?.trim().toLowerCase()
  if (!rawName) return false

  if (config.key?.trim().toLowerCase() === rawName) return true
  if (config.name?.trim().toLowerCase() === rawName) return true

  // Check synonyms
  const synonymTokens = buildSynonymTokenSet(config.synonyms)
  if (synonymTokens.indexOf(rawName) !== -1) return true

  // Check icecat IDs
  if (attribute.icecatTaxonomyIds?.size && config.icecatFeaturesIds?.size) {
    for (const id of attribute.icecatTaxonomyIds) {
      if (config.icecatFeaturesIds.has(String(id))) {
        // Double check name to be safe? Or trust ID?
        // Trusting ID is better for specific feature matches
        return true
      }
    }
  }

  return false
}

const getImplementationAims = (
  config: AttributeConfigDto,
  isIndexed: boolean
): string[] => {
  if (isIndexed) return []

  const aims: string[] = []
  if (config.participateInScores?.size) {
    aims.push(t('product.attributes.audit.aims.score'))
  }
  if (config.participateInACV?.size) {
    aims.push(t('product.attributes.audit.aims.acv'))
  }
  return aims
}

const auditRows = computed<AuditAttributeRow[]>(() => {
  const configs = props.attributeConfigs ?? []
  const indexedAttributes =
    (resolvedAttributes.value?.indexedAttributes as Record<
      string,
      ProductIndexedAttributeDto
    >) ?? {}
  const rows: AuditAttributeRow[] = []
  const usedConfigKeys = new Set<string>()

  // 1. Add all Indexed Attributes
  Object.entries(indexedAttributes).forEach(([key, attribute]) => {
    const normalizedKey = normalizeAttributeKey(key)
    if (normalizedKey) {
      usedConfigKeys.add(normalizedKey)
    }

    const config = configs.filter(
      c => normalizeAttributeKey(c.key) === normalizedKey
    )[0]
    const name =
      config?.name?.trim() ||
      (typeof attribute?.name === 'string' ? attribute.name.trim() : '') ||
      normalizedKey ||
      t('product.attributes.audit.unknown')

    const bestValue = formatMainAttributeValue(attribute)
    const displayValue =
      resolveDisplayValue(attribute, bestValue) ??
      t('product.attributes.audit.noBestValue')

    // For indexed attributes, we don't usually show aims, but we could.
    // User focus is on MISSING attributes having aims.

    const sources = normalizeSourcingSources(attribute?.sourcing?.sources)
    const sourceNames = sources
      .map(s => s.datasourceName)
      .filter(Boolean)
      .join(' ')
    const searchText = `${name} ${bestValue ?? ''} ${sourceNames}`
      .trim()
      .toLowerCase()

    rows.push({
      key: `indexed-${normalizedKey}`,
      name,
      displayValue,
      bestValue: bestValue ?? null,
      sourceCount: sources.length,
      sourcing: attribute?.sourcing ?? null,
      isIndexed: true,
      isMatched: true,
      searchText,
    })
  })

  // 2. Add Unmapped Raw Attributes
  const rawAttributesMap = new Map<string, ProductAttributeDto>()

  // 2a. Include 'allAttributes' (new source of truth for full dump)
  Object.values(
    (resolvedAttributes.value?.allAttributes as Record<
      string,
      ProductAttributeDto
    >) ?? {}
  ).forEach(attr => {
    if (attr.name) {
      rawAttributesMap.set(attr.name, attr)
    }
  })

  // 2b. Include 'classifiedAttributes' (legacy fallback / grouped view source)
  ;(resolvedAttributes.value?.classifiedAttributes ?? [])
    .flatMap(group => [
      ...(group.attributes ?? []),
      ...(group.features ?? []),
      ...(group.unFeatures ?? []),
    ])
    .filter((attr): attr is ProductAttributeDto => Boolean(attr))
    .forEach(attr => {
      // Prioritize existing if name matches, or add if missing
      if (attr.name && !rawAttributesMap.has(attr.name)) {
        rawAttributesMap.set(attr.name, attr)
      }
    })

  const allRawAttributes = Array.from(rawAttributesMap.values())

  allRawAttributes.forEach((attr, index) => {
    // Find matching config
    const matchedConfig = configs.filter(config =>
      checkAttributeMatchesConfig(attr, config)
    )[0]

    // If matched config matches an ALREADY indexed attribute, skip it (it's absorbed)
    if (matchedConfig) {
      const configKey = normalizeAttributeKey(matchedConfig.key)
      if (configKey && usedConfigKeys.has(configKey)) {
        return
      }
    }

    // It is NOT indexed (or matched to a generic config that produced no index)
    const name = attr.name?.trim() || t('product.attributes.audit.unknown')
    const rawValue = attr.value?.trim() || ''

    const aims = matchedConfig
      ? getImplementationAims(matchedConfig, false)
      : []
    const aimsText = aims.length ? `(${aims.join(', ')})` : ''

    const sources = normalizeSourcingSources(attr.sourcing?.sources)
    const sourceNames = sources
      .map(s => s.datasourceName)
      .filter(Boolean)
      .join(' ')

    const displayValue = aimsText ? `${rawValue} ${aimsText}` : rawValue
    const searchText = `${name} ${rawValue} ${aimsText} ${sourceNames}`
      .trim()
      .toLowerCase()

    rows.push({
      key: `raw-${index}-${name}`,
      name,
      displayValue,
      bestValue: null,
      sourceCount: 1, // It is its own source
      sourcing: attr.sourcing ?? null,
      isIndexed: false,
      isMatched: Boolean(matchedConfig),
      searchText,
    })
  })

  return rows
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
  if (item.isIndexed) {
    return 'product-attributes__audit-row product-attributes__audit-row--indexed'
  }

  if (item.isMatched) {
    return 'product-attributes__audit-row product-attributes__audit-row--matched'
  }

  return 'product-attributes__audit-row product-attributes__audit-row--unindexed'
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

const detailTableHeaders = computed(() => [
  {
    title: t('product.attributes.detailed.columns.group'),
    key: 'name',
    sortable: true,
  },
  {
    title: t('product.attributes.detailed.columns.count'),
    key: 'totalCount',
    sortable: true,
  },
])

const detailItemsPerPage = -1

const expandedDetailGroups = ref<string[]>([])

const detailTableItems = computed(() => filteredGroups.value)

const shouldSplitDetailTables = computed(
  () => detailViewMode.value === 'table' && filteredGroups.value.length > 10
)
const detailTableItemsLeft = computed(() => {
  if (!shouldSplitDetailTables.value) {
    return detailTableItems.value
  }

  const midpoint = Math.ceil(filteredGroups.value.length / 2)
  return filteredGroups.value.slice(0, midpoint)
})
const detailTableItemsRight = computed(() => {
  if (!shouldSplitDetailTables.value) {
    return []
  }

  const midpoint = Math.ceil(filteredGroups.value.length / 2)
  return filteredGroups.value.slice(midpoint)
})

const isDetailGroupExpanded = (id: string) =>
  expandedDetailGroups.value.includes(id)

const toggleDetailGroup = (id: string) => {
  if (isDetailGroupExpanded(id)) {
    expandedDetailGroups.value = expandedDetailGroups.value.filter(
      entry => entry !== id
    )
    return
  }

  expandedDetailGroups.value = [...expandedDetailGroups.value, id]
}
</script>

<style scoped>
.product-attributes {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  overflow: hidden;
  max-width: 100%;
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

.product-attributes__identity-detail--muted {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  font-size: 0.9rem;
}

.product-attributes__identity-detail--muted
  .product-attributes__identity-detail-list {
  color: inherit;
}

.product-attributes__gtin {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-attributes__gtin-image {
  width: min(200px, 100%);
  max-width: 200px;
  max-height: 80px;
  height: auto;
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

.product-attributes__detail-controls {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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

.product-attributes__view-toggle {
  align-self: flex-start;
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

.product-attributes__details-table-grid {
  margin: 0 -0.75rem;
  row-gap: 1.5rem;
}

.product-attributes__details-table {
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
}

.product-attributes__details-label {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__details-toggle {
  margin-left: -0.35rem;
}

.product-attributes__details-expanded-row td {
  padding: 0;
}

.product-attributes__details-expanded-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem 1.5rem 1.5rem;
  background: rgba(var(--v-theme-surface-primary-050), 0.6);
}

.product-attributes__details-attributes-table {
  background: transparent;
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

.product-attributes__header-section {
  display: flex;
  align-items: center;
  width: 100%;
}

.product-attributes__header-section--left {
  justify-content: flex-start;
  width: auto;
}

.product-attributes__header-section--center {
  justify-content: center;
}

.product-attributes__header-section--right {
  justify-content: flex-end;
  width: auto;
}

@media (min-width: 960px) {
  .product-attributes__main-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .product-attributes__block-header--detailed {
    display: grid;
    grid-template-columns: auto minmax(260px, 1fr) auto;
    align-items: center;
    gap: 1.5rem;
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
    max-width: 420px;
  }

  .product-attributes__search--audit {
    max-width: 360px;
  }
}
</style>
