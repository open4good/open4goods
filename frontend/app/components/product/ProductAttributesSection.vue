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

    <v-text-field
      v-model="searchTerm"
      :label="$t('product.attributes.searchPlaceholder')"
      prepend-inner-icon="mdi-magnify"
      hide-details
      clearable
      class="product-attributes__search"
    />

    <div v-if="referentialEntries.length" class="product-attributes__referential">
      <h3>{{ $t('product.attributes.referentialTitle') }}</h3>
      <v-chip-group column>
        <v-chip
          v-for="entry in referentialEntries"
          :key="entry.key"
          variant="tonal"
          color="primary"
          class="product-attributes__chip"
        >
          <strong>{{ entry.key }}</strong>
          <span>{{ entry.value }}</span>
        </v-chip>
      </v-chip-group>
    </div>

    <v-expansion-panels multiple class="product-attributes__groups">
      <v-expansion-panel v-for="group in filteredGroups" :key="group.name">
        <v-expansion-panel-title>
          {{ group.name }}
          <span class="product-attributes__badge">{{ group.attributes.length }}</span>
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <div v-if="group.features.length" class="product-attributes__feature">
            <h4>{{ $t('product.attributes.features') }}</h4>
            <ul>
              <li v-for="feature in group.features" :key="feature.name">
                <strong>{{ feature.name }}:</strong>
                <span>{{ feature.value }}</span>
              </li>
            </ul>
          </div>
          <div v-if="group.unFeatures.length" class="product-attributes__feature product-attributes__feature--negative">
            <h4>{{ $t('product.attributes.unfeatures') }}</h4>
            <ul>
              <li v-for="feature in group.unFeatures" :key="feature.name">
                <strong>{{ feature.name }}:</strong>
                <span>{{ feature.value }}</span>
              </li>
            </ul>
          </div>
          <v-table density="comfortable" class="product-attributes__table">
            <tbody>
              <tr v-for="attribute in group.attributes" :key="attribute.name">
                <th scope="row">{{ attribute.name }}</th>
                <td>{{ attribute.value }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductAttributesDto, ProductAttributeDto, ProductIndexedAttributeDto } from '~~/shared/api-client'

const props = defineProps({
  sectionId: {
    type: String,
    default: 'caracteristiques',
  },
  attributes: {
    type: Object as PropType<ProductAttributesDto | null>,
    default: null,
  },
})

const { t } = useI18n()

const searchTerm = ref('')

const referentialEntries = computed(() => {
  return Object.entries(props.attributes?.referentialAttributes ?? {}).map(([key, value]) => ({
    key,
    value,
  }))
})

const indexedEntries = computed(() => {
  const entries = props.attributes?.indexedAttributes ?? {}
  return Object.values(entries) as ProductIndexedAttributeDto[]
})

interface GroupView {
  name: string
  attributes: ProductAttributeDto[]
  features: ProductAttributeDto[]
  unFeatures: ProductAttributeDto[]
}

const classifiedGroups = computed(() => props.attributes?.classifiedAttributes ?? [])

const filteredGroups = computed<GroupView[]>(() => {
  const term = searchTerm.value.trim().toLowerCase()

  const baseGroups: GroupView[] = classifiedGroups.value.map((group) => ({
    name: group.name ?? '—',
    attributes: group.attributes ?? [],
    features: group.features ?? [],
    unFeatures: group.unFeatures ?? [],
  }))

  const indexedGroup: GroupView | null = indexedEntries.value.length
    ? {
        name: t('product.attributes.indexedGroup'),
        attributes: indexedEntries.value.map((attribute) => ({
          name: attribute.name ?? attribute.value ?? '—',
          value: attribute.value ?? '—',
        })),
        features: [],
        unFeatures: [],
      }
    : null

  const groups = indexedGroup ? [indexedGroup, ...baseGroups] : baseGroups

  if (!term) {
    return groups
  }

  const matches = (attribute: ProductAttributeDto | ProductIndexedAttributeDto) => {
    const name = (attribute.name ?? '').toString().toLowerCase()
    const value = (attribute.value ?? '').toString().toLowerCase()
    return name.includes(term) || value.includes(term)
  }

  return groups
    .map((group) => ({
      ...group,
      attributes: group.attributes.filter(matches),
      features: group.features.filter(matches),
      unFeatures: group.unFeatures.filter(matches),
    }))
    .filter((group) => group.attributes.length || group.features.length || group.unFeatures.length)
})
</script>

<style scoped>
.product-attributes {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-attributes__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
}

.product-attributes__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-attributes__search {
  max-width: 480px;
}

.product-attributes__referential {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 20px;
  padding: 1.25rem;
}

.product-attributes__chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  text-transform: none;
}

.product-attributes__chip strong {
  font-weight: 700;
  font-size: 0.85rem;
}

.product-attributes__chip span {
  font-size: 1rem;
}

.product-attributes__groups {
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  border-radius: 20px;
  padding: 0.5rem;
}

.product-attributes__badge {
  margin-left: auto;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-attributes__feature {
  margin-bottom: 1rem;
}

.product-attributes__feature h4 {
  margin-bottom: 0.5rem;
}

.product-attributes__feature ul {
  margin: 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.4rem;
}

.product-attributes__feature--negative li {
  color: rgba(var(--v-theme-error), 0.8);
}

.product-attributes__table {
  margin-top: 0.75rem;
}
</style>
