<template>
  <v-card
    v-if="ecoscoreUrl"
    :to="ecoscoreUrl"
    class="category-ecoscore-card"
    variant="tonal"
    elevation="0"
    rounded="xl"
    data-test="category-ecoscore-card"
    :aria-label="t('category.filters.ecoscore.ariaLabel')"
  >
    <div class="category-ecoscore-card__icon">
      <v-icon icon="mdi-star" size="32" />
    </div>

    <div class="category-ecoscore-card__content">
      <p class="category-ecoscore-card__title">
        {{ t('category.filters.ecoscore.title') }}
      </p>
      <p
        v-if="showDescription !== false"
        class="category-ecoscore-card__description"
      >
        {{ description }}
      </p>
      <span class="category-ecoscore-card__cta">
        {{ t('category.filters.ecoscore.cta') }}
        <v-icon icon="mdi-arrow-top-right" size="18" />
      </span>
    </div>
  </v-card>
</template>

<script setup lang="ts">
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

const description = computed(() =>
  t('category.filters.ecoscore.description', {
    category: normalizedCategoryName.value,
  })
)

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
  align-items: flex-start
  gap: 1rem
  padding: 1.5rem
  min-height: 160px
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.95), rgba(var(--v-theme-surface-primary-120), 0.9))
  color: rgb(var(--v-theme-text-neutral-strong))
  text-decoration: none
  transition: transform 0.3s ease, box-shadow 0.3s ease

  &:hover
    transform: translateY(-2px)
    box-shadow: 0 18px 28px -24px rgba(var(--v-theme-shadow-primary-600), 0.45)

  &:focus-visible
    outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.8)
    outline-offset: 3px

  &__icon
    display: inline-flex
    align-items: center
    justify-content: center
    width: 48px
    height: 48px
    min-width: 48px
    border-radius: 999px
    background: rgba(var(--v-theme-primary), 0.12)
    color: rgb(var(--v-theme-primary))

  &__content
    display: flex
    flex-direction: column
    gap: 0.35rem
    flex: 1 1 auto
    min-height: 0

  &__title
    font-size: 1rem
    font-weight: 600
    margin: 0

  &__description
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-size: 0.875rem
    line-height: 1.5

  &__cta
    display: inline-flex
    align-items: center
    gap: 0.35rem
    font-size: 0.875rem
    font-weight: 600
    color: rgb(var(--v-theme-primary))
</style>
