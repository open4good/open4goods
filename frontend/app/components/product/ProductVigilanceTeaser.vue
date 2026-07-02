<template>
  <div v-if="hasAlerts" class="product-vigilance-teaser">
    <div class="product-vigilance-teaser__container">
      <div class="product-vigilance-teaser__left">
        <v-icon
          :icon="mdiAlertCircleOutline"
          class="product-vigilance-teaser__icon mr-3"
          size="22"
        />
        <div class="product-vigilance-teaser__info">
          <div class="product-vigilance-teaser__title">
            {{
              t('product.vigilance.teaser.alertCount', alerts.length, {
                count: alerts.length,
              })
            }}
          </div>
          <div class="product-vigilance-teaser__chips d-none d-md-flex mt-1">
            <v-chip
              v-for="alert in alerts"
              :key="alert.key"
              :color="alert.color"
              size="x-small"
              variant="tonal"
              class="mr-2 font-weight-bold"
              label
            >
              <v-icon :icon="alert.icon" start size="12" />
              {{ t(alert.titleKey) }}
            </v-chip>
          </div>
        </div>
      </div>
      <v-btn
        variant="text"
        color="warning"
        size="small"
        class="font-weight-bold product-vigilance-teaser__cta"
        :append-icon="mdiChevronRight"
        @click="emit('click:alerts')"
      >
        {{ t('product.vigilance.teaser.cta') }}
      </v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { mdiAlertCircleOutline, mdiChevronRight } from '@mdi/js'
import type { ProductDto } from '~~/shared/api-client'
import { useProductVigilance } from '~/composables/useProductVigilance'

const props = defineProps<{
  product: ProductDto
}>()

const emit = defineEmits<{
  (e: 'click:alerts'): void
}>()

const { t } = useI18n()
const productRef = toRef(props, 'product')
const { alerts, hasAlerts } = useProductVigilance(productRef)
</script>

<style scoped>
.product-vigilance-teaser {
  background: rgba(var(--v-theme-warning), 0.05);
  border: 1px solid rgba(var(--v-theme-warning), 0.2);
  border-radius: 12px;
  padding: 0.75rem 1.25rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.product-vigilance-teaser:hover {
  background: rgba(var(--v-theme-warning), 0.08);
  border-color: rgba(var(--v-theme-warning), 0.35);
  transform: translateY(-1px);
}

.product-vigilance-teaser__container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.product-vigilance-teaser__left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.product-vigilance-teaser__icon {
  color: rgb(var(--v-theme-warning));
}

.product-vigilance-teaser__info {
  display: flex;
  flex-direction: column;
}

.product-vigilance-teaser__title {
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-vigilance-teaser__cta {
  text-transform: none;
  letter-spacing: normal;
}
</style>
