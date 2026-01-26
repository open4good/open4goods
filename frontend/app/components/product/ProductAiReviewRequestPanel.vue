<template>
  <div class="product-ai-review-request-panel">
    <div class="product-ai-review-request-panel__body">
      <header class="product-ai-review-request-panel__header">
        <div class="product-ai-review-request-panel__product-info">
          <div>
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
          <v-icon icon="mdi-sparkles" size="22" />
          <div>
            <p class="product-ai-review-request-panel__quota-label">
              {{ t('siteIdentity.menu.account.privacy.quotas.aiRemaining') }}
            </p>
            <p class="product-ai-review-request-panel__quota-value">
              {{ remainingGenerationsLabel }}
            </p>
          </div>
        </div>

        <ClientOnly>
          <div
            v-if="showCaptcha"
            class="product-ai-review-request-panel__captcha"
          >
            <!-- Invisible Captcha -->
            <VueHcaptcha
              ref="captchaRef"
              :sitekey="siteKey"
              :theme="captchaTheme"
              :language="captchaLocale"
              size="invisible"
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

    <v-divider class="my-4" />

    <div class="product-ai-review-request-panel__actions">
      <v-spacer />
      <v-btn
        v-if="showSubmitButton"
        color="primary"
        size="large"
        variant="flat"
        :loading="requesting"
        :disabled="submitDisabled"
        @click="handleSubmit"
      >
        {{ t('product.aiReview.request.submit') }}
      </v-btn>
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
  agreementAccepted: {
    type: Boolean,
    default: true,
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
  requesting: {
    type: Boolean,
    default: false,
  },
  submitDisabled: {
    type: Boolean,
    default: false,
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
  (event: 'update:agreementAccepted', value: boolean): void
  (event: 'submit' | 'captcha-expired' | 'captcha-error'): void
  (event: 'captcha-verify', token: string): void
}>()

const { t } = useI18n()
const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)
const captchaRef = ref<InstanceType<typeof VueHcaptcha> | null>(null)

const agreementModel = computed({
  get: () => props.agreementAccepted,
  set: value => emit('update:agreementAccepted', value),
})

const hasSiteKey = computed(() => props.siteKey.length > 0)
const showCaptcha = computed(() => props.requiresCaptcha && hasSiteKey.value)

const productLabel = computed(() =>
  props.productName.length > 0
    ? props.productName
    : t('product.aiReview.request.productFallback')
)

const submitDisabled = computed(() => {
  if (!agreementModel.value || props.requesting) {
    return true
  }
  // We rely on parent's disabled prop, which should now allow submit even if no token
  return props.submitDisabled
})

const showSubmitButton = computed(() => {
  return !props.isGenerating
})

const handleSubmit = () => {
  if (props.requiresCaptcha && hasSiteKey.value && captchaRef.value) {
    // Invisble captcha: execute to get token
    // If successful, handleCaptchaVerify will be called
    captchaRef.value.execute()
  } else {
    // No captcha required or missing key configuration
    emit('submit')
  }
}

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
  justify-content: center;
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

.product-ai-review-request-panel__actions {
  display: flex;
  justify-content: flex-end;
}
</style>

<style>
/* 
  Global fix for hCaptcha challenge visibility when body is scroll-locked by Vuetify (v-dialog).
  The challenge container is appended to body, which may be shifted off-screen.
  Force it to be fixed to the viewport.
*/
body > div:has(iframe[src*='hcaptcha'][src*='frame=challenge']) {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 999999 !important; /* Ensure it's on top of everything including modal */
}
</style>
