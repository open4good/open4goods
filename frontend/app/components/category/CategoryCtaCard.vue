<template>
  <v-card
    v-bind="mergedProps"
    :class="['category-cta-card', attrs.class]"
    variant="text"
    :ripple="false"
    rounded="lg"
    :aria-label="ariaLabel"
    :role="clickableRole"
    :tabindex="clickableTabIndex"
    @click="onActivate"
    @keydown="onKeydown"
  >
    <div v-if="icon" class="category-cta-card__icon-wrapper">
      <v-icon :icon="icon" size="24" class="category-cta-card__icon" />
    </div>

    <div class="category-cta-card__content">
      <p class="category-cta-card__title">
        {{ title }}
      </p>
      <p v-if="subtitle" class="category-cta-card__subtitle">
        {{ subtitle }}
      </p>
    </div>

    <v-icon
      v-if="showArrow"
      icon="mdi-arrow-right"
      class="category-cta-card__arrow"
      size="20"
    />
  </v-card>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string | null
    icon?: string | null
    to?: string | null
    ariaLabel: string
    showArrow?: boolean
    clickable?: boolean
  }>(),
  {
    subtitle: null,
    icon: null,
    to: null,
    showArrow: true,
    clickable: false,
  }
)

const emit = defineEmits<{
  (event: 'click', payload: Event): void
}>()

const attrs = useAttrs()

const isClickable = computed(() => Boolean(props.to) || props.clickable)

const cardProps = computed(() => (props.to ? { to: props.to } : {}))
const mergedProps = computed(() => ({
  ...attrs,
  ...cardProps.value,
}))

const clickableRole = computed(() =>
  isClickable.value && !props.to ? 'button' : undefined
)
const clickableTabIndex = computed(() =>
  isClickable.value && !props.to ? 0 : undefined
)

const onActivate = (event: Event) => {
  if (props.to || !isClickable.value) {
    return
  }

  emit('click', event)
}

const onKeydown = (event: KeyboardEvent) => {
  if (!isClickable.value || props.to) {
    return
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    emit('click', event)
  }
}
</script>

<style scoped lang="sass">
.category-cta-card
  display: flex
  align-items: center
  gap: 1rem
  padding: 1rem
  width: 100%
  min-height: auto
  background-color: rgb(var(--v-theme-surface))
  border: 1px solid rgba(var(--v-theme-border-primary), 0.08)
  border-radius: 16px
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05)
  transition: all 0.3s cubic-bezier(0.3, 0, 0.2, 1)
  cursor: pointer
  color: rgb(var(--v-theme-text-neutral-strong))

  &:hover
    background-color: rgb(var(--v-theme-surface))
    border-color: rgba(var(--v-theme-primary), 0.2)
    box-shadow: 0 12px 28px -4px rgba(0, 0, 0, 0.12)
    transform: translateY(-2px)

  &:focus-visible
    outline: 2px solid rgb(var(--v-theme-accent-primary-highlight))
    outline-offset: 2px

  &__icon-wrapper
    display: flex
    align-items: center
    justify-content: center
    width: 40px
    height: 40px
    border-radius: 8px
    background: rgba(var(--v-theme-primary), 0.1)
    color: rgb(var(--v-theme-primary))
    flex-shrink: 0

  &__content
    display: flex
    flex-direction: column
    gap: 0.125rem
    flex: 1
    min-width: 0

  &__title
    font-size: 0.95rem
    font-weight: 600
    line-height: 1.2
    margin: 0
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    font-size: 0.8rem
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
    margin: 0
    font-weight: 500
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis

  &__arrow
    color: rgba(var(--v-theme-text-neutral-disabled), 0.8)
    transition: transform 0.2s ease, color 0.2s ease

  &:hover &__arrow
    transform: translateX(2px)
    color: rgb(var(--v-theme-primary))
</style>
