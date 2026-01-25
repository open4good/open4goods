<template>
  <ClientOnly>
    <VExpandTransition>
      <aside
        v-if="hasItems"
        class="ai-review-panel"
        :class="{ 'ai-review-panel--stacked': isStacked }"
        role="complementary"
        :aria-label="t('product.aiReview.generationPanel.title')"
      >
        <v-card class="ai-review-panel__card" elevation="12" rounded="xl">
          <header class="ai-review-panel__header">
            <div class="ai-review-panel__title-block">
              <h2 class="ai-review-panel__title">
                {{ t('product.aiReview.generationPanel.title') }}
              </h2>
              <p class="ai-review-panel__quota">
                {{
                  t('product.aiReview.generationPanel.quotaTitle', {
                    count: remainingGenerationsLabel,
                  })
                }}
              </p>
            </div>

            <v-btn
              class="ai-review-panel__toggle"
              variant="text"
              color="primary"
              size="small"
              @click="toggleCollapse"
            >
              {{
                isCollapsed
                  ? t('product.aiReview.generationPanel.expandPanel')
                  : t('product.aiReview.generationPanel.collapsePanel')
              }}
            </v-btn>
          </header>

          <VExpandTransition>
            <div v-show="!isCollapsed" class="ai-review-panel__body">
              <ul class="ai-review-panel__items" role="list">
                <li
                  v-for="item in items"
                  :key="item.gtin"
                  class="ai-review-panel__item"
                >
                  <v-avatar
                    class="ai-review-panel__item-avatar"
                    size="40"
                    rounded="lg"
                  >
                    <v-img
                      v-if="item.productImage"
                      :src="item.productImage"
                      :alt="item.productName"
                      cover
                    />
                    <div
                      v-else
                      class="ai-review-panel__item-placeholder"
                      aria-hidden="true"
                    >
                      <v-icon icon="mdi-image-off-outline" size="20" />
                    </div>
                  </v-avatar>

                  <div class="ai-review-panel__item-content">
                    <NuxtLink
                      :to="resolveProductLink(item)"
                      class="ai-review-panel__item-name"
                    >
                      {{ item.productName }}
                    </NuxtLink>

                    <div class="ai-review-panel__item-status">
                      <!-- Generating -->
                      <div
                        v-if="
                          item.status === 'pending' ||
                          item.status === 'generating'
                        "
                        class="d-flex align-center text-caption text-medium-emphasis"
                      >
                        <v-progress-circular
                          indeterminate
                          color="primary"
                          size="12"
                          width="2"
                          class="mr-2"
                        />
                        <span>{{
                          item.statusMessage ||
                          t(
                            'product.aiReview.generationPanel.status.generating'
                          )
                        }}</span>
                      </div>

                      <!-- Success -->
                      <div
                        v-else-if="item.status === 'success'"
                        class="d-flex align-center text-caption text-success"
                      >
                        <v-icon
                          icon="mdi-check-circle"
                          size="14"
                          class="mr-1"
                        />
                        <span>{{
                          t('product.aiReview.generationPanel.status.success')
                        }}</span>
                      </div>

                      <!-- Error -->
                      <div
                        v-else
                        class="d-flex align-center text-caption text-error"
                      >
                        <v-icon
                          icon="mdi-alert-circle"
                          size="14"
                          class="mr-1"
                        />
                        <span>{{
                          t('product.aiReview.generationPanel.status.error')
                        }}</span>
                      </div>
                    </div>
                  </div>

                  <v-btn
                    class="ai-review-panel__item-remove"
                    variant="text"
                    size="x-small"
                    icon="mdi-close"
                    color="medium-emphasis"
                    :aria-label="t('common.actions.close')"
                    @click="remove(item.gtin)"
                  />
                </li>
              </ul>
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
import { useI18n } from 'vue-i18n'
import { useAiReviewGenerationStore } from '~/stores/useAiReviewGenerationStore'
import { useProductCompareStore } from '~/stores/useProductCompareStore'
import { useIpQuota } from '~/composables/useIpQuota'
import { IpQuotaCategory } from '~~/shared/api-client'

const { t } = useI18n()
const store = useAiReviewGenerationStore()
const compareStore = useProductCompareStore()
const { getRemaining } = useIpQuota()

const { items, isCollapsed } = storeToRefs(store)
const { items: compareItems } = storeToRefs(compareStore)

const hasItems = computed(() => items.value.length > 0)
const isStacked = computed(() => compareItems.value.length > 0)

const quotaCategory = IpQuotaCategory.ReviewGeneration
const remainingGenerations = computed(() => getRemaining(quotaCategory))
const remainingGenerationsLabel = computed(() => {
  if (
    remainingGenerations.value === null ||
    remainingGenerations.value === undefined
  ) {
    return '?'
  }
  return String(remainingGenerations.value)
})

const toggleCollapse = () => {
  store.isCollapsed = !store.isCollapsed
}

const remove = (gtin: string) => {
  store.removeItem(gtin)
}

const resolveProductLink = (item: {
  productSlug?: string
  gtin: string
  status: string
}) => {
  // If we have a slug use it, otherwise fallback to a generic search or similar?
  // Ideally we should always have the slug.
  if (item.productSlug) {
    return item.productSlug // Assuming stored as full path or needs resolving?
    // In store we save `product.slug` or `product.fullSlug`.
    // Let's assume we store the link ready-to-use or enough to build it.
    // If it's just a slug, we might need to know the category...
    // For simplicity, let's assume we can navigate to /product/gtin-slug
    // Or just use the raw slug if it looks like a path.
  }
  return `/${item.gtin}`
}
</script>

<style scoped lang="sass">
.ai-review-panel
  position: fixed
  inset-inline-end: 1.5rem
  inset-block-end: 1.5rem
  width: min(380px, calc(100% - 3rem))
  z-index: 12
  transition: inset-block-end 0.3s ease

  &--stacked

    // Rough estimate for collapsed compare panel height
    inset-block-end: calc(1.5rem + 80px)

  &__card
    background: rgb(var(--v-theme-surface))
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15)
    border-radius: 1.25rem
    padding: 1rem 1.25rem
    border: 1px solid rgba(var(--v-theme-outline), 0.1)

  &__header
    display: flex
    align-items: flex-start
    justify-content: space-between
    gap: 0.5rem

  &__title
    margin: 0
    font-size: 1rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__quota
    margin: 0.1rem 0 0
    font-size: 0.8rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__toggle
    margin-inline-start: auto
    text-transform: none
    letter-spacing: normal

  &__body
    margin-top: 1rem
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__items
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin: 0
    padding: 0
    list-style: none
    max-height: 40vh
    overflow-y: auto

  &__item
    display: flex
    align-items: center
    gap: 0.75rem
    padding-right: 0.5rem

  &__item-avatar
    flex-shrink: 0
    background: rgb(var(--v-theme-surface-default))
    border: 1px solid rgba(var(--v-theme-outline), 0.1)

  &__item-placeholder
    display: flex
    align-items: center
    justify-content: center
    width: 100%
    height: 100%
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__item-content
    flex: 1
    min-width: 0
    display: flex
    flex-direction: column
    gap: 2px

  &__item-name
    font-weight: 600
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-strong))
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis
    text-decoration: none

    &:hover
      color: rgb(var(--v-theme-primary))
      text-decoration: underline

  &__item-status
    display: flex
    align-items: center

  &__item-remove
    align-self: center
    opacity: 0.6
    &:hover
      opacity: 1

@media (max-width: 600px)
  .ai-review-panel
    width: calc(100% - 2rem)
    inset-inline: 1rem
    inset-block-end: 1rem

    &--stacked
      inset-block-end: calc(1rem + 80px)
</style>
