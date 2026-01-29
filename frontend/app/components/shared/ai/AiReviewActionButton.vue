<template>
  <v-tooltip location="bottom" open-delay="150">
    <template #activator="{ props: tooltipProps }">
      <span
        v-bind="tooltipProps"
        class="ai-review-action-button__wrapper"
      >
        <v-btn
          class="ai-review-action-button"
          :class="[
            `ai-review-action-button--variant-${variant}`,
            `ai-review-action-button--size-${size}`,
            `ai-review-action-button--appearance-${appearance}`,
            {
              'ai-review-action-button--active': isReviewed,
              'ai-review-action-button--disabled': isDisabled,
            },
            buttonClass,
          ]"
          :variant="buttonVariant"
          :color="buttonColor"
          :disabled="isDisabled"
          :aria-label="ariaLabel"
          :title="tooltipText"
          @click.stop.prevent="handleClick"
        >
          <span class="ai-review-action-button__icon-wrapper">
            <v-icon icon="mdi-robot" :size="iconSize" />
            <v-icon
              v-if="isReviewed"
              icon="mdi-check-circle"
              size="10"
              class="ai-review-action-button__badge"
              color="success"
            />
          </span>
          <span
            v-if="variant === 'button'"
            class="ai-review-action-button__label"
          >
            {{ buttonLabel }}
          </span>
        </v-btn>
      </span>
    </template>
    <span>{{ tooltipText }}</span>
  </v-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import { IpQuotaCategory } from '~~/shared/api-client'
import { useIpQuota } from '~/composables/useIpQuota'

const props = withDefaults(
  defineProps<{
    isReviewed: boolean
    reviewCreatedAt?: number
    variant?: 'icon-only' | 'button'
    size?: 'compact' | 'comfortable' | 'large'
    appearance?: 'default' | 'plain'
    label?: string
    buttonClass?: string
  }>(),
  {
    variant: 'icon-only',
    size: 'comfortable',
    appearance: 'default',
    label: undefined,
    buttonClass: undefined,
    reviewCreatedAt: undefined,
  }
)

const emit = defineEmits<{
  (event: 'click'): void
}>()

const { t, locale } = useI18n()
const { getRemaining } = useIpQuota()

const quotaCategory = IpQuotaCategory.ReviewGeneration

const remaining = computed(() => getRemaining(quotaCategory) ?? 0)
const hasQuota = computed(() => remaining.value > 0)
const isDisabled = computed(() => !props.isReviewed && !hasQuota.value)

const iconSize = computed(() => {
  switch (props.size) {
    case 'compact':
      return 18
    case 'large':
      return 26
    default:
      return 22
  }
})

const buttonVariant = computed(() => {
  if (props.appearance === 'plain') {
    return 'plain'
  }

  return props.variant === 'icon-only' ? 'flat' : 'flat'
})

const buttonColor = computed(() => {
  if (props.variant === 'button') {
    return 'primary'
  }

  if (props.isReviewed) {
    return 'primary'
  }

  return props.appearance === 'plain' ? undefined : 'surface'
})

const buttonLabel = computed(() => {
  return props.label ?? t('product.hero.aiReview.label')
})

const createdDate = computed(() => {
  if (!props.reviewCreatedAt) {
    return null
  }

  return format(props.reviewCreatedAt, 'PPP', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const tooltipText = computed(() => {
  if (props.isReviewed) {
    if (createdDate.value) {
      return t('product.aiReview.tooltip.generatedAt', {
        date: createdDate.value,
      })
    }

    return t('product.aiReview.tooltip.available')
  }

  if (hasQuota.value) {
    return t('product.aiReview.tooltip.remaining', {
      count: remaining.value,
    })
  }

  return t('product.aiReview.tooltip.quotaExceeded')
})

const ariaLabel = computed(() => {
  return props.variant === 'button' ? buttonLabel.value : tooltipText.value
})

const handleClick = () => {
  if (isDisabled.value) {
    return
  }

  emit('click')
}
</script>

<style scoped lang="sass">
.ai-review-action-button
  text-transform: none
  letter-spacing: 0
  font-weight: 600
  transition: all 0.2s ease

  &__wrapper
    display: inline-flex
    align-items: center

  &__icon-wrapper
    position: relative
    display: inline-flex
    align-items: center
    justify-content: center

  &__badge
    position: absolute
    bottom: -4px
    right: -4px
    background: white
    border-radius: 50%
    border: 1px solid white

  &__label
    margin-left: 0.5rem

  &--variant-icon-only
    border-radius: 50%
    min-width: 0
    padding: 0
    box-shadow: 0 4px 12px rgba(21, 46, 73, 0.12)
    background-color: rgba(var(--v-theme-surface-default), 0.92)
    color: rgb(var(--v-theme-text-neutral-strong))

    &:hover
      transform: translateY(-2px)
      box-shadow: 0 8px 16px rgba(21, 46, 73, 0.16)

    &.ai-review-action-button--active
      background-color: rgba(var(--v-theme-primary), 0.16)
      color: rgb(var(--v-theme-primary))
      box-shadow: none

    &.ai-review-action-button--size-compact
      width: 2.25rem
      height: 2.25rem

    &.ai-review-action-button--size-comfortable
      width: 2.75rem
      height: 2.75rem

    &.ai-review-action-button--size-large
      width: 3.25rem
      height: 3.25rem

  &--variant-button
    border-radius: 999px
    padding-inline: 1.25rem

  &--appearance-plain
    box-shadow: none
    background-color: transparent !important

    &:hover
      background-color: rgba(var(--v-theme-surface-default), 0.08)

  &--disabled
    opacity: 0.5
    box-shadow: none

    &:hover
      transform: none
</style>
