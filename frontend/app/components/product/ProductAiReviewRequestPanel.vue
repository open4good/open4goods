<template>
  <div class="product-ai-review-request-panel">
    <div class="product-ai-review-request-panel__body">
      <header class="product-ai-review-request-panel__header">
        <div class="product-ai-review-request-panel__product-info">
          <div>
            <p class="product-ai-review-request-panel__eyebrow">
              {{ t('product.aiReview.request.eyebrow') }}
            </p>
            <h3 class="product-ai-review-request-panel__headline">
              {{ productLabel }}
            </h3>
          </div>
        </div>
      </header>

      <p class="product-ai-review-request-panel__description">
        {{ t('product.aiReview.request.description') }}
      </p>

      <div class="product-ai-review-request-panel__controls">
        <div class="product-ai-review-request-panel__quota">
          <div>
            <p class="product-ai-review-request-panel__quota-label">
              {{ t('siteIdentity.menu.account.privacy.quotas.aiRemaining') }}
            </p>
            <p class="product-ai-review-request-panel__quota-value">
              {{ remainingGenerationsLabel }}
            </p>
          </div>
          <v-icon icon="mdi-sparkles" size="22" />
        </div>

        <ClientOnly>
          <div
            v-if="showCaptcha"
            class="product-ai-review-request-panel__captcha"
          >
            <VueHcaptcha
              ref="captchaRef"
              :sitekey="siteKey"
              :theme="captchaTheme"
              :language="captchaLocale"
              @verify="handleCaptchaVerify"
              @expired="emit('captcha-expired')"
              @error="emit('captcha-error')"
            />
          </div>
        </ClientOnly>
      </div>

      <v-alert
        variant="tonal"
        type="info"
        class="product-ai-review-request-panel__agreement mt-4"
        density="compact"
      >
        {{ t('product.aiReview.request.agreement') }}
      </v-alert>

      <v-alert
        v-if="errorMessage"
        type="error"
        class="product-ai-review-request-panel__alert mt-4"
        border="start"
      >
        {{ errorMessage }}
      </v-alert>

      <div
        v-if="statusMessage"
        class="product-ai-review-request-panel__status mt-4"
      >
        <v-progress-linear
          v-if="isGenerating"
          color="primary"
          :model-value="statusPercent"
          rounded
          height="6"
        />
        <p>{{ statusMessage }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  productName: {
    type: String,
    default: '',
  },
  remainingGenerationsLabel: {
    type: String,
    required: true,
  },
  requiresCaptcha: {
    type: Boolean,
    default: false,
  },
  siteKey: {
    type: String,
    default: '',
  },
  captchaTheme: {
    type: String,
    default: 'light',
  },
  captchaLocale: {
    type: String,
    default: 'en',
  },
  errorMessage: {
    type: String,
    default: null,
  },
  statusMessage: {
    type: String,
    default: null,
  },
  isGenerating: {
    type: Boolean,
    default: false,
  },
  statusPercent: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits<{
  (event: 'submit' | 'captcha-expired' | 'captcha-error'): void
  (event: 'captcha-verify', token: string): void
}>()

const { t } = useI18n()
const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)
const captchaRef = ref<InstanceType<typeof VueHcaptcha> | null>(null)

const hasSiteKey = computed(() => props.siteKey.length > 0)
const showCaptcha = computed(() => props.requiresCaptcha && hasSiteKey.value)

const productLabel = computed(() =>
  props.productName.length > 0
    ? props.productName
    : t('product.aiReview.request.productFallback')
)

const handleCaptchaVerify = (token: string) => {
  emit('captcha-verify', token)
  emit('submit')
}
</script>

<style scoped>
.product-ai-review-request-panel {
  /* No specific styles needed if we rely on parent styling */
}

.product-ai-review-request-panel__body {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.product-ai-review-request-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.product-ai-review-request-panel__product-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.product-ai-review-request-panel__eyebrow {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review-request-panel__headline {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
  line-height: 1.3;
}

.product-ai-review-request-panel__description {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  line-height: 1.6;
}

.product-ai-review-request-panel__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  flex-wrap: wrap;
  width: 100%; /* Ensure full width */
}

.product-ai-review-request-panel__quota {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review-request-panel__quota-label {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review-request-panel__quota-value {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  font-weight: 700;
}

.product-ai-review-request-panel__captcha {
  display: flex;
  justify-content: flex-end;
}

.product-ai-review-request-panel__status {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
</style>
