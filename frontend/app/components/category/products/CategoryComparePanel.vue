<template>
  <ClientOnly>
    <VExpandTransition>
      <aside
        v-if="hasItems"
        class="category-compare-panel"
        role="complementary"
        :aria-label="t('category.products.compare.regionLabel')"
      >
        <v-card
          class="category-compare-panel__card"
          elevation="12"
          rounded="xl"
        >
          <header class="category-compare-panel__header">
            <div class="category-compare-panel__title-block">
              <h2 class="category-compare-panel__title">
                {{ t('category.products.compare.title') }}
              </h2>
              <p class="category-compare-panel__count">{{ itemsCountLabel }}</p>
            </div>

            <v-btn
              class="category-compare-panel__toggle"
              variant="text"
              color="primary"
              size="small"
              @click="toggleCollapse"
            >
              {{
                isCollapsed
                  ? t('category.products.compare.expandPanel')
                  : t('category.products.compare.collapsePanel')
              }}
            </v-btn>
          </header>

          <VExpandTransition>
            <div v-show="!isCollapsed" class="category-compare-panel__body">
              <ul class="category-compare-panel__items" role="list">
                <li
                  v-for="item in items"
                  :key="item.id"
                  class="category-compare-panel__item"
                >
                  <v-avatar
                    class="category-compare-panel__item-avatar"
                    size="52"
                    rounded="lg"
                  >
                    <v-img
                      v-if="item.image"
                      :src="item.image"
                      :alt="item.name"
                      cover
                    />
                    <div
                      v-else
                      class="category-compare-panel__item-placeholder"
                      aria-hidden="true"
                    >
                      {{ itemInitials(item.name) }}
                    </div>
                  </v-avatar>

                  <div class="category-compare-panel__item-content">
                    <NuxtLink
                      v-if="itemLink(item)"
                      :to="itemLink(item)"
                      class="category-compare-panel__item-name category-compare-panel__item-name--link"
                    >
                      {{ item.name }}
                    </NuxtLink>
                    <span v-else class="category-compare-panel__item-name">{{
                      item.name
                    }}</span>
                    <v-btn
                      class="category-compare-panel__item-remove"
                      variant="text"
                      size="small"
                      color="error"
                      :aria-label="
                        t('category.products.compare.removeSingle', {
                          name: item.name,
                        })
                      "
                      @click="remove(item.id)"
                    >
                      {{ t('category.products.compare.removeFromList') }}
                    </v-btn>
                  </div>
                </li>
              </ul>

              <p v-if="items.length < 2" class="category-compare-panel__hint">
                {{ t('category.products.compare.addMoreHint') }}
              </p>

              <v-btn
                class="category-compare-panel__cta"
                color="primary"
                variant="flat"
                block
                :disabled="!canLaunch"
                @click="launchComparison"
              >
                {{ t('category.products.compare.launchComparison') }}
              </v-btn>
            </div>
          </VExpandTransition>
        </v-card>
      </aside>
    </VExpandTransition>
  </ClientOnly>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import { buildCompareHash } from '~/utils/_compare-url'
import {
  useProductCompareStore,
  type CompareListItem,
} from '~/stores/useProductCompareStore'

const emit = defineEmits<{
  (event: 'launch-comparison', items: CompareListItem[]): void
}>()

const { t, locale } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const router = useRouter()
const route = useRoute()

const compareStore = useProductCompareStore()
const { items, isCollapsed } = storeToRefs(compareStore)

const comparePath = computed(() =>
  resolveLocalizedRoutePath('compare', locale.value)
)

const hasItems = computed(() => items.value.length > 0)

const itemsCountLabel = computed(() =>
  translatePlural('category.products.compare.itemsCount', items.value.length, {
    count: items.value.length,
  })
)

const canLaunch = computed(() => items.value.length >= 2)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const remove = (id: string) => {
  compareStore.removeById(id)

  if (route.path === comparePath.value) {
    const hash = buildCompareHash(items.value.map(item => item.gtin))
    router.replace(hash ? `${comparePath.value}${hash}` : comparePath.value)
  }
}

const itemLink = (item: CompareListItem) => {
  return item.fullSlug ?? item.slug ?? undefined
}

const launchComparison = () => {
  if (!canLaunch.value) {
    return
  }

  const gtins = items.value.map(item => item.gtin)
  const hash = buildCompareHash(gtins)

  if (!hash) {
    return
  }

  router.push(`${comparePath.value}${hash}`)
  emit('launch-comparison', [...items.value])
}

const itemInitials = (name: string) => {
  return name
    .split(' ')
    .filter(Boolean)
    .map(part => part[0]?.toUpperCase())
    .slice(0, 2)
    .join('')
}
</script>

<style scoped lang="sass">
.category-compare-panel
  width: 100%
  pointer-events: auto

  &__card
    background: rgb(var(--v-theme-surface-glass))
    box-shadow: 0 24px 40px rgba(15, 35, 65, 0.18)
    border-radius: 1.25rem
    padding: 1.25rem

  &__header
    display: flex
    align-items: flex-start
    justify-content: space-between
    gap: 0.5rem

  &__title
    margin: 0
    font-size: 1.1rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__count
    margin: 0.25rem 0 0
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__toggle
    margin-inline-start: auto
    text-transform: none

  &__body
    margin-top: 1rem
    display: flex
    flex-direction: column
    gap: 1rem

  &__items
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin: 0
    padding: 0
    list-style: none

  &__item
    display: flex
    align-items: center
    gap: 0.75rem

  &__item-avatar
    flex-shrink: 0
    background: rgb(var(--v-theme-surface-default))

  &__item-placeholder
    display: flex
    align-items: center
    justify-content: center
    width: 100%
    height: 100%
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))
    background: rgba(var(--v-theme-surface-primary-080), 0.6)
    border-radius: inherit

  &__item-content
    display: flex
    flex-direction: column
    gap: 0.25rem
    min-width: 0

  &__item-name
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis

    &--link
      display: inline-block
      color: rgb(var(--v-theme-primary))
      text-decoration: none
      transition: text-decoration-color 0.2s ease

      &:hover,
      &:focus-visible
        text-decoration: underline
        text-decoration-color: currentColor

  &__item-remove
    align-self: flex-start
    padding: 0
    text-transform: none

  &__hint
    margin: 0
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__cta
    text-transform: none
    font-weight: 600

@media (max-width: 600px)
  .category-compare-panel
    // width handled by container

    &__card
      border-radius: 1.25rem 1.25rem 0 0
      box-shadow: 0 -18px 32px rgba(15, 35, 65, 0.16)
      padding: 1rem 1.25rem calc(1rem + env(safe-area-inset-bottom, 0px))

    &__body
      gap: 0.75rem

    &__items
      gap: 0.5rem
</style>
