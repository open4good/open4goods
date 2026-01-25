<template>
  <div class="eprel-details">
    <h3 class="eprel-details__title">{{ $t('product.eprel.detailsTitle') }}</h3>
    <v-table density="compact" class="eprel-details__table">
      <tbody>
        <tr v-if="formattedOnMarketEndDate">
          <td class="text-medium-emphasis">
            {{ $t('product.eprel.marketEndDate') }}
          </td>
          <td>{{ formattedOnMarketEndDate }}</td>
        </tr>
        <tr v-if="outOfMarketDuration">
          <td class="text-medium-emphasis">
            {{ $t('product.eprel.outOfMarketDuration') }}
          </td>
          <td>
            <v-chip size="small" color="warning" variant="tonal">
              {{ outOfMarketDuration }}
            </v-chip>
          </td>
        </tr>
        <tr v-if="supportUntilYear">
          <td class="text-medium-emphasis">
            {{ $t('product.eprel.supportGuaranteedUntil') }}
          </td>
          <td>
            <v-chip size="small" color="success" variant="tonal">
              {{ supportUntilYear }}
            </v-chip>
          </td>
        </tr>
        <tr v-if="minSoftwareUpdates">
          <td class="text-medium-emphasis">
            {{ $t('product.eprel.minSoftwareUpdates') }}
          </td>
          <td>{{ minSoftwareUpdates }} {{ $t('common.years') }}</td>
        </tr>
        <tr v-if="minSpareParts">
          <td class="text-medium-emphasis">
            {{ $t('product.eprel.minSpareParts') }}
          </td>
          <td>{{ minSpareParts }} {{ $t('common.years') }}</td>
        </tr>
      </tbody>
    </v-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { format, differenceInMonths, differenceInYears } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import { normalizeTimestamp } from '~/utils/date-parsing'
import type { ProductEprelDto } from '~~/shared/api-client'

interface EprelDataWrapper {
  eprelDatas?: ProductEprelDto
}

const props = defineProps<{
  eprelData: EprelDataWrapper
}>()

const { t, locale } = useI18n()

// Helper to access nested properties safely
const getAttribute = (key: string) => {
  return props.eprelData?.eprelDatas?.categorySpecificAttributes?.[key]
}

const onMarketStartDate = computed(() => {
  const val = props.eprelData?.eprelDatas?.onMarketStartDate
  return normalizeTimestamp(val)
})

const onMarketEndDate = computed(() => {
  const val = props.eprelData?.eprelDatas?.onMarketEndDate
  return normalizeTimestamp(val)
})

const formattedOnMarketEndDate = computed(() => {
  if (!onMarketEndDate.value) return null
  return format(new Date(onMarketEndDate.value), 'dd MMM yyyy', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const outOfMarketDuration = computed(() => {
  if (!onMarketEndDate.value) return null
  const end = new Date(onMarketEndDate.value)
  const now = new Date()

  // If still on market (end date in future), don't show duration
  if (end > now) return null

  const years = differenceInYears(now, end)
  const months = differenceInMonths(now, end) % 12

  const parts: string[] = []
  if (years > 0) parts.push(t('common.count.years', { count: years }, years))
  if (months > 0)
    parts.push(t('common.count.months', { count: months }, months))

  return parts.join(' ')
})

const minGuaranteedSupport = computed(() => {
  return Number(getAttribute('minGuaranteedSupportYears')) || 0
})

const supportUntilYear = computed(() => {
  if (!onMarketStartDate.value || !minGuaranteedSupport.value) return null
  const start = new Date(onMarketStartDate.value)
  const supportEnd = new Date(start)
  supportEnd.setFullYear(start.getFullYear() + minGuaranteedSupport.value)
  return supportEnd.getFullYear()
})

const minSoftwareUpdates = computed(() =>
  getAttribute('minAvailabilitySoftwareUpdatesYears')
)
const minSpareParts = computed(() =>
  getAttribute('minAvailabilitySparePartsYears')
)
</script>

<style scoped>
.eprel-details {
  border-radius: 12px;
  background: rgba(var(--v-theme-surface-variant), 0.3);
  padding: 1rem;
}

.eprel-details__title {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  opacity: 0.9;
}

.eprel-details__table {
  background: transparent !important;
}

:deep(.v-table__wrapper) {
  overflow: visible;
}
</style>
