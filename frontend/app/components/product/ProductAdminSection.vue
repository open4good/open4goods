<template>
  <ClientOnly>
    <section v-if="showAdmin" :id="panelId" class="product-admin">
      <header class="product-admin__header">
        <h2 class="product-admin__title">{{ $t('product.admin.title') }}</h2>
        <p class="product-admin__subtitle">
          {{ $t('product.admin.subtitle') }}
        </p>
      </header>

      <v-tabs v-model="activeTab" bg-color="transparent" color="primary">
        <v-tab value="local">{{
          $t('product.admin.sections.productJson.title')
        }}</v-tab>
        <v-tab value="payload">Payload JSON du produit</v-tab>
      </v-tabs>

      <v-window v-model="activeTab">
        <v-window-item value="local">
          <article
            :id="jsonSectionId"
            class="product-admin__block"
            role="region"
            :aria-label="$t('product.admin.sections.productJson.title')"
          >
            <header class="product-admin__block-header">
              <h3 class="product-admin__block-title">
                {{ $t('product.admin.sections.productJson.title') }}
              </h3>
              <p class="product-admin__block-helper">
                {{ $t('product.admin.sections.productJson.helper') }}
              </p>
            </header>
            <!-- eslint-disable vue/no-v-html -->
            <pre class="product-admin__code" aria-live="polite">
              <code
                v-if="highlightedJson"
                class="hljs language-json"
                v-html="highlightedJson"
              />
              <code v-else class="product-admin__code-fallback">{{ formatted }}</code>
            </pre>
            <!-- eslint-enable vue/no-v-html -->
          </article>
        </v-window-item>

        <v-window-item value="payload">
          <article class="product-admin__block">
            <header class="product-admin__block-header">
              <h3 class="product-admin__block-title">
                Payload JSON du produit
              </h3>
              <p class="product-admin__block-helper">
                Données JSON brutes via l'API Nudger
              </p>
            </header>
            <div class="product-admin__iframe-wrapper">
              <iframe
                :src="apiPayloadUrl"
                class="product-admin__iframe"
                title="API Payload"
              />
            </div>
          </article>
        </v-window-item>
      </v-window>
    </section>
  </ClientOnly>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PropType } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { useAuth } from '~/composables/useAuth'
import { loadHighlightJs } from '~/utils/highlight-loader'

const props = defineProps({
  panelId: {
    type: String,
    default: 'admin',
  },
  jsonSectionId: {
    type: String,
    default: 'admin-json',
  },
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { isLoggedIn } = useAuth()

const showAdmin = computed(() => isLoggedIn.value)
const formatted = computed(() => JSON.stringify(props.product, null, 2))

const activeTab = ref('local')
const apiPayloadUrl = computed(
  () => `https://api.nudger.fr/product/?gtin=${props.product.gtin}`
)

const highlightedJson = ref(formatted.value)

const highlightJson = async () => {
  const content = formatted.value
  if (!content) {
    highlightedJson.value = ''
    return
  }

  highlightedJson.value = content

  try {
    const hljs = await loadHighlightJs(['json'])

    if (hljs) {
      highlightedJson.value = hljs.highlight(content, {
        language: 'json',
      }).value
    }
  } catch (error) {
    console.warn('Unable to highlight product JSON in admin section', error)
  }
}

watch(
  formatted,
  () => {
    highlightJson()
  },
  { immediate: true }
)
</script>

<style scoped>
.product-admin {
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-admin__title {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-admin__header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-admin__subtitle {
  margin: 0;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-admin__block {
  background: rgba(15, 23, 42, 0.92);
  border-radius: 18px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.28);
}

.product-admin__block-header {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.product-admin__block-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #f8fafc;
}

.product-admin__block-helper {
  margin: 0;
  font-size: 0.9rem;
  color: rgba(226, 232, 240, 0.78);
}

.product-admin__code {
  margin: 0;
  padding: 1rem;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.35);
  overflow-x: auto;
  max-height: 540px;
}

.product-admin__code :deep(.hljs) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.88rem;
  line-height: 1.55;
  color: #e2e8f0;
  background: transparent;
}

.product-admin__code-fallback {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.88rem;
  color: #e2e8f0;
}

@media (max-width: 960px) {
  .product-admin {
    padding: 1.25rem;
    border-radius: 18px;
  }

  .product-admin__block {
    border-radius: 16px;
  }

  .product-admin__code {
    max-height: 320px;
  }
}

.product-admin__iframe-wrapper {
  width: 100%;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  height: 600px;
}

.product-admin__iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}
</style>
