<template>
  <v-col cols="12" sm="6" lg="4" class="product-attributes-detail-card__col">
    <v-card class="product-attributes__detail-card" variant="flat">
      <header class="product-attributes__detail-header">
        <h4 class="product-attributes__detail-title">{{ group.name }}</h4>
        <v-chip size="small" variant="tonal" color="primary">
          {{ group.totalCount }}
        </v-chip>
      </header>

      <div
        v-if="group.features.length"
        class="product-attributes__chip-list product-attributes__chip-list--positive"
      >
        <ul>
          <li v-for="feature in group.features" :key="feature.key">
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
        v-if="group.unFeatures.length"
        class="product-attributes__chip-list product-attributes__chip-list--negative"
      >
        <ul>
          <li v-for="feature in group.unFeatures" :key="feature.key">
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
        v-if="group.attributes.length"
        density="comfortable"
        class="product-attributes__table"
      >
        <tbody>
          <tr v-for="attribute in group.attributes" :key="attribute.key">
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
    </v-card>
  </v-col>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import type { PropType } from 'vue'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
import type { DetailGroupView } from '~/components/product/ProductAttributesSection.vue'

const props = defineProps({
  group: {
    type: Object as PropType<DetailGroupView>,
    required: true,
  },
})

const { group } = toRefs(props)
</script>

<style scoped>
.product-attributes-detail-card__col {
  padding: 0.75rem;
}

.product-attributes__detail-card {
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6);
  box-shadow: 0 10px 25px -12px rgba(15, 23, 42, 0.15);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  height: 100%;
}

.product-attributes__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-attributes__detail-title {
  font-size: clamp(1rem, 1.4vw, 1.2rem);
  font-weight: 600;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attributes__chip-list {
  padding: 0.5rem 0.75rem;
  border-radius: 14px;
  background: rgba(var(--v-theme-surface-primary-080), 0.75);
}

.product-attributes__chip-list--positive {
  border-left: 4px solid rgba(var(--v-theme-success), 0.75);
}

.product-attributes__chip-list--negative {
  border-left: 4px solid rgba(var(--v-theme-error), 0.75);
  background: rgba(var(--v-theme-surface-primary-050), 0.7);
}

.product-attributes__chip-list ul {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0;
  margin: 0;
  list-style: none;
}

.product-attributes__chip-list li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.product-attributes__chip-icon {
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-attributes__chip-icon--positive {
  color: rgb(var(--v-theme-success));
}

.product-attributes__chip-icon--negative {
  color: rgb(var(--v-theme-error));
}

.product-attributes__chip-label {
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
</style>
