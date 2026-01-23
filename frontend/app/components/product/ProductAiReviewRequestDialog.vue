<template>
  <v-dialog v-model="dialogModel" max-width="760" scrollable>
    <v-card class="product-ai-review-request">
      <v-card-text class="product-ai-review-request__body">
        <header class="product-ai-review-request__header">
          <div class="product-ai-review-request__product-info">
            <div
              v-if="productImage"
              class="product-ai-review-request__image-wrapper"
            >
              <img
                :src="productImage"
                :alt="productLabel"
                class="product-ai-review-request__image"
              />
            </div>
            <div>
              <p class="product-ai-review-request__eyebrow">
                {{ t('product.aiReview.request.eyebrow') }}
              </p>
              <h3 class="product-ai-review-request__headline">
                {{ productLabel }}
              </h3>
            </div>
          </div>
          <v-btn
            variant="text"
            icon="mdi-close"
            :aria-label="t('product.aiReview.request.close')"
            @click="dialogModel = false"
          />
        </header>

        <p class="product-ai-review-request__description">
          {{ t('product.aiReview.request.description') }}
        </p>

        <div class="product-ai-review-request__controls">
          <div class="product-ai-review-request__quota">
            <div>
              <p class="product-ai-review-request__quota-label">
                {{ t('siteIdentity.menu.account.privacy.quotas.aiRemaining') }}
              </p>
              <p class="product-ai-review-request__quota-value">
                {{ remainingGenerationsLabel }}
              </p>
            </div>
            <v-icon icon="mdi-sparkles" size="22" />
          </div>

          <ClientOnly>
            <div v-if="showCaptcha" class="product-ai-review-request__captcha">
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
          class="product-ai-review-request__agreement mt-4"
          density="compact"
        >
          {{ t('product.aiReview.request.agreement') }}
        </v-alert>

        <v-alert
          v-if="errorMessage"
          type="error"
          class="product-ai-review-request__alert"
          border="start"
        >
          {{ errorMessage }}
        </v-alert>

        <div v-if="statusMessage" class="product-ai-review-request__status">
          <v-progress-linear
            v-if="isGenerating"
            color="primary"
            :model-value="statusPercent"
            rounded
            height="6"
          />
          <p>{{ statusMessage }}</p>
        </div>
      </v-card-text>

      <v-divider />

      <v-card-actions class="product-ai-review-request__actions">
        <v-btn variant="text" @click="dialogModel = false">
          {{ t('product.aiReview.request.cancel') }}
        </v-btn>
        <v-spacer />
        <v-btn
          v-if="showSubmitButton"
          color="primary"
          size="large"
          variant="flat"
          :loading="requesting"
          :disabled="submitDisabled"
          @click="emit('submit')"
        >
          {{ t('product.aiReview.request.submit') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
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
  productImage: {
    type: String,
    default: null,
  },
})

const emit = defineEmits<{
  (
    event: 'update:modelValue' | 'update:agreementAccepted',
    value: boolean
  ): void
  (event: 'submit' | 'captcha-expired' | 'captcha-error'): void
  (event: 'captcha-verify', token: string): void
}>()

const { t } = useI18n()
const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)
const captchaRef = ref<InstanceType<typeof VueHcaptcha> | null>(null)

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

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

watch(
  () => props.modelValue,
  value => {
    if (value) {
      captchaRef.value?.reset?.()
    }
  }
)

const submitDisabled = computed(() => {
  if (!agreementModel.value || props.requesting) {
    return true
  }

  if (props.requiresCaptcha) {
    // If captcha is required, valid captcha token is needed
    // But if we have a token, we should have auto-submitted.
    // So the button is effectively only for non-captcha or retry?
    // User requested "Removing Validate button".
    // I will hide the button if captcha is required.
    return true
  }

  return false
})

const showSubmitButton = computed(() => {
  if (props.requiresCaptcha) {
    // Show button only if captcha is verified (fallback for auto-submit)
    return !submitDisabled.value
  }
  return true
})

const handleCaptchaVerify = (token: string) => {
  emit('captcha-verify', token)
  emit('submit')
}
</script>

<style scoped>
.product-ai-review-request {
  border-radius: 24px;
}

.product-ai-review-request__body {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.product-ai-review-request__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.product-ai-review-request__product-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.product-ai-review-request__image-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(var(--v-theme-outline), 0.1);
  background-color: rgb(var(--v-theme-surface-primary));
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-ai-review-request__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.product-ai-review-request__eyebrow {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review-request__headline {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
  line-height: 1.3;
}

.product-ai-review-request__description {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  line-height: 1.6;
}

.product-ai-review-request__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  flex-wrap: wrap;
}

.product-ai-review-request__quota {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-ai-review-request__quota-label {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-ai-review-request__quota-value {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  font-weight: 700;
}

.product-ai-review-request__checkbox :deep(.v-selection-control__wrapper) {
  margin-top: 0.1rem;
}

.product-ai-review-request__captcha {
  display: flex;
  justify-content: flex-end;
}

.product-ai-review-request__alert {
  margin-top: 0.4rem;
}

.product-ai-review-request__status {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-ai-review-request__actions {
  padding: 1rem 1.5rem 1.3rem;
}
</style>
