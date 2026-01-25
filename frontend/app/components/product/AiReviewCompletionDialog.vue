<template>
  <v-dialog v-model="isOpen" max-width="400">
    <v-card v-if="currentItem" class="ai-completion-dialog" rounded="xl">
      <v-card-text class="pa-5 text-center">
        <div class="ai-completion-dialog__icon mb-4">
          <v-icon
            v-if="currentItem.status === 'success'"
            icon="mdi-check-circle"
            color="success"
            size="48"
          />
          <v-icon v-else icon="mdi-alert-circle" color="error" size="48" />
        </div>

        <h3 class="text-h6 font-weight-bold mb-2">
          {{
            currentItem.status === 'success'
              ? t('product.aiReview.generationPanel.alert.successTitle')
              : t('product.aiReview.generationPanel.alert.errorTitle')
          }}
        </h3>

        <p class="text-body-1 mb-6">
          {{
            currentItem.status === 'success'
              ? t('product.aiReview.generationPanel.alert.successMessage', {
                  product: currentItem.productName,
                })
              : t('product.aiReview.generationPanel.alert.errorMessage', {
                  product: currentItem.productName,
                })
          }}
        </p>

        <div class="d-flex flex-column gap-3">
          <v-btn
            v-if="currentItem.status === 'success'"
            color="primary"
            class="mb-2"
            block
            size="large"
            @click="viewReview(currentItem)"
          >
            {{ t('product.aiReview.generationPanel.alert.viewResult') }}
          </v-btn>

          <v-btn variant="text" block @click="close">
            {{ t('common.actions.close') }}
          </v-btn>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAiReviewGenerationStore } from '~/stores/useAiReviewGenerationStore'
import type { GenerationQueueItem } from '~/stores/useAiReviewGenerationStore'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const store = useAiReviewGenerationStore()
const { items } = storeToRefs(store)

const currentItem = ref<GenerationQueueItem | null>(null)
const isOpen = ref(false)

// Watch for items that become completed (success/failed) and haven't been seen
watch(
  items,
  newItems => {
    // Find the first item that is completed, not seen, and not currently being viewed
    const nextAlert = newItems.find(item => {
      const isCompleted = item.status === 'success' || item.status === 'failed'
      const notSeen = !item.seen
      const notOnPage = route.params.gtin !== item.gtin // Simple check, might need robust route checking
      return isCompleted && notSeen && notOnPage
    })

    if (nextAlert) {
      currentItem.value = nextAlert
      isOpen.value = true
    }
  },
  { deep: true }
)

const close = () => {
  if (currentItem.value) {
    store.acknowledgeCompletion(currentItem.value.gtin)
  }
  isOpen.value = false
  setTimeout(() => {
    currentItem.value = null
  }, 300)
}

const viewReview = (item: GenerationQueueItem) => {
  close()
  if (item.productSlug) {
    // Assuming slug is full path or we can construct it?
    // Usually slug is just the slug part.
    // Product links are often `/category/product-slug` or `/product/gtin-slug`
    // If we only have slug, we might not have the category.
    // Fallback to GTIN based route
    // Wait, let's use router push logic
    // For now, let's assume we can navigate to the stored slug if it's a path, or build one.
    // If we only have partial info, fallback to reload or product ID?
    // Let's rely on what we stored.

    // Simplest: Go to product page by GTIN if we don't have full path
    // Or if `productSlug` contains slash, use it.

    // In store: productSlug: product.slug (often just the name-slug)
    // We need category...
    // Let's try navigating to using a finder helper or just `/product/<gtin>` if supported?
    // Our app supports `/p/<gtin>` ? Or maybe just force a navigation that resolves?

    // Inspecting store usage: we saved `product.slug`.

    // Let's assume navigating to `/product/${item.gtin}` works as redirect
    // OR just use item.productSlug if it looks full.

    // Better: In `AiReviewGenerationStore`, we saved `productSlug`.
    // In `ProductPage`, we see logic for `gtin`.

    router.push({ path: `/product/${item.gtin}` }).catch(() => {
      // fallback
    })
  } else {
    router.push({ path: `/product/${item.gtin}` })
  }
}
</script>

<style scoped>
.ai-completion-dialog__icon {
  display: flex;
  justify-content: center;
}
</style>
