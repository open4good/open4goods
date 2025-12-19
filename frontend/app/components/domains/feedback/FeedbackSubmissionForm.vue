<template>
  <section
    :id="sectionId"
    class="feedback-form"
    :aria-labelledby="`${sectionId}-heading`"
  >
    <v-card class="feedback-form__card" elevation="0" rounded="xl">
      <div class="feedback-form__header">
        <v-avatar
          size="42"
          class="feedback-form__header-icon"
          color="surface-primary-120"
        >
          <v-icon :icon="categoryIcon" size="26" color="primary" />
        </v-avatar>
        <div>
          <p class="feedback-form__eyebrow">{{ eyebrow }}</p>
          <h3 :id="`${sectionId}-heading`" class="feedback-form__title">
            {{ title }}
          </h3>
          <p class="feedback-form__subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <p class="feedback-form__intro">
        {{ intro }}
      </p>

      <v-alert
        v-if="success"
        type="success"
        variant="tonal"
        prominent
        border="start"
        class="mb-4"
        role="status"
      >
        {{ successMessage }}
      </v-alert>

      <v-alert
        v-else-if="errorMessage"
        type="error"
        variant="tonal"
        prominent
        border="start"
        class="mb-4"
        role="alert"
      >
        {{ errorMessage }}
      </v-alert>

      <v-alert
        v-if="!hasSiteKey"
        type="warning"
        variant="tonal"
        border="start"
        class="mb-4"
        role="status"
      >
        {{ missingCaptchaMessage }}
      </v-alert>

      <v-form
        ref="formRef"
        class="feedback-form__form"
        @submit.prevent="onSubmit"
      >
        <v-row dense>
          <v-col cols="12">
            <v-text-field
              v-model="author"
              :label="authorLabel"
              :placeholder="authorPlaceholder"
              :disabled="submitting"
              autocomplete="nickname"
              variant="outlined"
              prepend-inner-icon="mdi-account"
            />
          </v-col>

          <v-col cols="12">
            <v-text-field
              v-model="titleInput"
              :label="titleLabel"
              :placeholder="titlePlaceholder"
              :rules="titleRules"
              :disabled="submitting"
              required
              variant="outlined"
              prepend-inner-icon="mdi-format-title"
              maxlength="140"
            />
          </v-col>

          <v-col cols="12">
            <v-textarea
              v-model="message"
              :label="messageLabel"
              :placeholder="messagePlaceholder"
              :rules="messageRules"
              :counter="2000"
              :disabled="submitting"
              auto-grow
              required
              rows="6"
              variant="outlined"
              prepend-inner-icon="mdi-text-long"
            />
          </v-col>

          <v-col cols="12">
            <div class="feedback-form__captcha">
              <VueHcaptcha
                v-if="hasSiteKey"
                ref="captchaRef"
                :sitekey="siteKey"
                :language="captchaLocale"
                :theme="captchaTheme"
                @verify="handleCaptchaVerify"
                @expired="handleCaptchaExpired"
                @error="handleCaptchaError"
              />
              <div
                v-else
                class="feedback-form__captcha-placeholder"
                aria-hidden="true"
              >
                <v-icon
                  icon="mdi-shield-alert-outline"
                  size="36"
                  color="primary"
                />
              </div>
              <p
                v-if="captchaError"
                class="feedback-form__captcha-error"
                role="alert"
              >
                {{ captchaError }}
              </p>
            </div>
          </v-col>

          <v-col cols="12">
            <div class="feedback-form__actions">
              <v-btn
                type="submit"
                color="primary"
                size="large"
                :loading="submitting"
                :disabled="submitting || !captchaToken || !hasSiteKey"
                prepend-icon="mdi-send"
              >
                {{ submitLabel }}
              </v-btn>
              <v-btn
                variant="text"
                color="primary"
                :disabled="submitting"
                @click="resetForm"
              >
                {{ resetLabel }}
              </v-btn>
            </div>
            <p class="feedback-form__privacy">{{ privacyNotice }}</p>
          </v-col>
        </v-row>
      </v-form>
    </v-card>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useTheme } from 'vuetify'
import { VForm } from 'vuetify/components'

const VueHcaptcha = defineAsyncComponent(async () => {
  const module = await import('@hcaptcha/vue3-hcaptcha')
  return module.default
})

type VueHcaptchaComponent = (typeof import('@hcaptcha/vue3-hcaptcha'))['default']

export interface FeedbackFormSubmitPayload {
  type: string
  title: string
  message: string
  author: string
  hCaptchaResponse: string
  url: string
}

const props = defineProps<{
  sectionId: string
  eyebrow: string
  title: string
  subtitle: string
  intro: string
  categoryIcon: string
  categoryType: string
  submitting: boolean
  success: boolean
  errorMessage: string | null
  siteKey: string
  authorLabel: string
  authorPlaceholder: string
  defaultAuthor: string
  titleLabel: string
  titlePlaceholder: string
  messageLabel: string
  messagePlaceholder: string
  submitLabel: string
  resetLabel: string
  successMessage: string
  missingCaptchaMessage: string
  privacyNotice: string
  captchaMissingMessage: string
  captchaExpiredMessage: string
  captchaFailedMessage: string
  titleTooShortMessage: string
  messageTooShortMessage: string
  currentLocale: string
  currentUrl: string
}>()

const emit = defineEmits<{
  (event: 'submit', payload: FeedbackFormSubmitPayload): void
  (event: 'reset-feedback'): void
}>()

const theme = useTheme()
const formRef = ref<InstanceType<typeof VForm> | null>(null)
const captchaRef = ref<InstanceType<VueHcaptchaComponent> | null>(null)

const author = ref(props.defaultAuthor)
const titleInput = ref('')
const message = ref('')
const captchaToken = ref('')
const captchaError = ref<string | null>(null)

const hasSiteKey = computed(() => props.siteKey?.length > 0)
const siteKey = computed(() => props.siteKey)

const captchaLocale = computed(() =>
  props.currentLocale.startsWith('fr') ? 'fr' : 'en'
)
const captchaTheme = computed(() =>
  theme.current.value.dark ? 'dark' : 'light'
)

watch(
  () => props.defaultAuthor,
  nextAuthor => {
    if (!author.value) {
      author.value = nextAuthor
    }
  }
)

watch(
  () => props.success,
  isSuccess => {
    if (isSuccess) {
      resetForm()
    }
  }
)

watch([author, titleInput, message], () => {
  emit('reset-feedback')
})

const titleRules = [
  (value: string) =>
    (!!value && value.trim().length >= 4) || props.titleTooShortMessage,
]

const messageRules = [
  (value: string) =>
    (!!value && value.trim().length >= 20) || props.messageTooShortMessage,
]

const handleCaptchaVerify = (token: string) => {
  captchaToken.value = token
  captchaError.value = null
}

const handleCaptchaExpired = () => {
  captchaToken.value = ''
  captchaError.value = props.captchaExpiredMessage
  captchaRef.value?.reset?.()
}

const handleCaptchaError = () => {
  captchaToken.value = ''
  captchaError.value = props.captchaFailedMessage
  captchaRef.value?.reset?.()
}

const validateForm = async () => {
  const validation = await formRef.value?.validate()

  if (validation?.valid === false) {
    return false
  }

  if (!captchaToken.value) {
    captchaError.value = props.captchaMissingMessage
    return false
  }

  return true
}

const onSubmit = async () => {
  const isValid = await validateForm()

  if (!isValid) {
    return
  }

  emit('submit', {
    type: props.categoryType,
    title: titleInput.value.trim(),
    message: message.value.trim(),
    author: author.value.trim(),
    hCaptchaResponse: captchaToken.value,
    url: props.currentUrl,
  })
}

const resetForm = () => {
  author.value = props.defaultAuthor
  titleInput.value = ''
  message.value = ''
  captchaToken.value = ''
  captchaError.value = null
  formRef.value?.resetValidation()
  captchaRef.value?.reset?.()
  emit('reset-feedback')
}
</script>

<style scoped lang="scss">
.feedback-form {
  &__card {
    padding: clamp(1.5rem, 3vw, 2.25rem);
    background: rgb(var(--v-theme-surface-glass-strong));
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
    box-shadow: none;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.5rem;
  }

  &__header-icon {
    backdrop-filter: blur(8px);
  }

  &__eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.12em;
    font-size: 0.85rem;
    font-weight: 600;
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin-bottom: 0.25rem;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__subtitle {
    margin: 0.25rem 0 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__intro {
    margin-bottom: 1.5rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__form {
    margin-top: 1rem;
  }

  &__captcha {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-start;
  }

  &__captcha-placeholder {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 302px;
    height: 78px;
    border-radius: 0.75rem;
    border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.4);
    background-color: rgba(var(--v-theme-surface-glass), 0.6);
  }

  &__captcha-error {
    margin: 0;
    color: rgb(var(--v-theme-error));
    font-size: 0.9rem;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    align-items: center;
    margin-top: 0.5rem;
  }

  &__privacy {
    margin-top: 0.75rem;
    color: rgb(var(--v-theme-text-neutral-soft));
    font-size: 0.85rem;
  }
}
</style>
