<template>
  <ClientOnly>
    <section v-if="showAdmin" :id="sectionId" class="product-admin">
      <header class="product-admin__header">
        <h2>{{ $t('product.admin.title') }}</h2>
        <p>{{ $t('product.admin.subtitle') }}</p>
      </header>
      <pre class="product-admin__pre">{{ formatted }}</pre>
    </section>
  </ClientOnly>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { useAuth } from '~/composables/useAuth'

const props = defineProps({
  sectionId: {
    type: String,
    default: 'admin',
  },
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { isLoggedIn } = useAuth()

const showAdmin = computed(() => isLoggedIn.value)
const formatted = computed(() => JSON.stringify(props.product, null, 2))
</script>

<style scoped>
.product-admin {
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  border-radius: 24px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-admin__pre {
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.9rem;
  background: rgba(15, 23, 42, 0.85);
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 12px;
  overflow-x: auto;
}
</style>
