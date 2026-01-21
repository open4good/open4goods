<template>
  <v-card
    v-if="ecoscoreUrl"
    :to="ecoscoreUrl"
    class="category-ecoscore-card"
    variant="text"
    :ripple="false"
    rounded="lg"
    data-test="category-ecoscore-card"
    :aria-label="t('category.filters.ecoscore.ariaLabel')"
  >
    <div class="category-ecoscore-card__icon-wrapper">
      <v-icon icon="mdi-leaf" size="24" class="category-ecoscore-card__icon" />
    </div>

    <div class="category-ecoscore-card__content">
      <p class="category-ecoscore-card__title">
        {{ t('category.filters.ecoscore.title') }}
      </p>
      <p class="category-ecoscore-card__subtitle">
        {{
          t('category.filters.ecoscore.cta', {
            category: normalizedCategoryName,
          })
        }}
      </p>
    </div>

    <v-icon
      icon="mdi-arrow-right"
      class="category-ecoscore-card__arrow"
      size="20"
    />
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  verticalHomeUrl?: string | null
  categoryName?: string | null
}>()

const { t, locale } = useI18n()

const normalizedCategoryName = computed(() => {
  if (!props.categoryName) {
    return ''
  }

  return props.categoryName.toLocaleLowerCase(locale.value)
})

const ecoscoreUrl = computed(() => {
  if (!props.verticalHomeUrl) {
    return null
  }

  const normalizedBase = props.verticalHomeUrl.endsWith('/')
    ? props.verticalHomeUrl.slice(0, -1)
    : props.verticalHomeUrl

  return `${normalizedBase}/ecoscore`
})
</script>

<style scoped lang="sass">
.category-ecoscore-card
  display: flex
  align-items: center
  gap: 1rem
  padding: 0.75rem 1rem
  width: 100%
  min-height: auto
  background-color: transparent
  border: 1px solid rgba(var(--v-theme-border-primary), 0.15)
  transition: all 0.2s ease
  cursor: pointer
  color: rgb(var(--v-theme-text-neutral-strong))

  &:hover
    background-color: rgba(var(--v-theme-surface-active), 0.4)
    border-color: rgba(var(--v-theme-primary), 0.3)
    transform: translateY(-1px)

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
