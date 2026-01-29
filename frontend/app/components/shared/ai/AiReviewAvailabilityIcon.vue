<template>
  <v-tooltip location="bottom">
    <template #activator="{ props: activatorProps }">
      <div
        class="ai-review-icon"
        :class="{
          'ai-review-icon--active': isReviewed,
          'ai-review-icon--available': !isReviewed && hasQuota,
          'ai-review-icon--disabled': !isReviewed && !hasQuota,
        }"
        v-bind="activatorProps"
      >
        <v-icon icon="mdi-robot" :size="size" class="ai-review-icon__icon" />
        <v-icon
          v-if="isReviewed"
          icon="mdi-check-circle"
          size="10"
          class="ai-review-icon__badge"
          color="success"
        />
      </div>
    </template>
    <span>{{ tooltipText }}</span>
  </v-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { IpQuotaCategory } from '~~/shared/api-client'
import { useIpQuota } from '~/composables/useIpQuota'

const props = defineProps<{
  isReviewed: boolean
  size?: string | number
}>()

const { t } = useI18n()
const { getRemaining } = useIpQuota()

// We use the 'ReviewGeneration' quota category
const quotaCategory = IpQuotaCategory.ReviewGeneration

const remaining = computed(() => getRemaining(quotaCategory) ?? 0)
const hasQuota = computed(() => remaining.value > 0)

const tooltipText = computed(() => {
  if (props.isReviewed) {
    return t('product.aiReview.tooltip.available')
  }
  if (hasQuota.value) {
    return t('product.aiReview.tooltip.remaining', { count: remaining.value })
  }
  return t('product.aiReview.tooltip.quotaExceeded')
})
</script>

<style scoped lang="sass">
.ai-review-icon
  position: relative
  display: inline-flex
  align-items: center
  justify-content: center
  width: 32px
  height: 32px
  border-radius: 50%
  background-color: rgb(var(--v-theme-surface-variant))
  color: rgb(var(--v-theme-on-surface-variant))
  transition: all 0.2s ease
  cursor: default

  &--active
    background-color: rgb(var(--v-theme-primary-lighten-1))
    color: rgb(var(--v-theme-on-primary))

    .ai-review-icon__icon
      opacity: 1

  &--available
    cursor: pointer
    background-color: rgb(var(--v-theme-secondary-lighten-1))
    color: rgb(var(--v-theme-on-secondary))

    &:hover
      transform: scale(1.1)

  &--disabled
    opacity: 0.5
    filter: grayscale(100%)

  &__icon
    // Adjust visual center
    margin-top: 1px

  &__badge
    position: absolute
    bottom: -2px
    right: -2px
    background: white
    border-radius: 50%
    border: 1px solid white
</style>
