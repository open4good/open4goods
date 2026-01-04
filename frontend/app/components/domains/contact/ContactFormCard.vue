<template>
  <section
    id="contact-form"
    class="contact-form"
    aria-labelledby="contact-form-heading"
  >
    <v-container class="py-12">
      <v-row justify="center">
        <v-col cols="12" md="10" lg="8">
          <v-card class="contact-form__card" elevation="8" rounded="xl">
            <div class="contact-form__header">
              <p class="contact-form__eyebrow">
                {{ t('contact.form.eyebrow') }}
              </p>
              <h2 id="contact-form-heading" class="contact-form__title">
                {{ t('contact.form.title') }}
              </h2>
              <p class="contact-form__subtitle">
                {{ t('contact.form.subtitle') }}
              </p>
            </div>

            <v-alert
              v-if="success"
              type="success"
              class="mb-4"
              variant="tonal"
              border="start"
              prominent
              role="alert"
            >
              {{ t('contact.form.feedback.success') }}
            </v-alert>

            <v-alert
              v-else-if="errorMessage"
              type="error"
              class="mb-4"
              variant="tonal"
              border="start"
              prominent
              role="alert"
            >
              {{ errorMessage }}
            </v-alert>

            <v-alert
              v-if="!hasSiteKey"
              type="warning"
              class="mb-4"
              variant="tonal"
              border="start"
              prominent
              role="status"
            >
              {{ t('contact.form.feedback.missingSiteKey') }}
            </v-alert>

            <v-form
              ref="formRef"
              class="contact-form__form"
              @submit.prevent="handleSubmit"
            >
              <v-row dense>
                <v-col cols="12">
                  <v-text-field
                    v-model="name"
                    :label="t('contact.form.fields.name.label')"
                    :placeholder="t('contact.form.fields.name.placeholder')"
                    :rules="nameRules"
                    :disabled="submitting"
                    autocomplete="name"
                    required
                    prepend-inner-icon="mdi-account"
                    variant="outlined"
                  />
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="email"
                    :label="t('contact.form.fields.email.label')"
                    :placeholder="t('contact.form.fields.email.placeholder')"
                    :rules="emailRules"
                    :disabled="submitting"
                    autocomplete="email"
                    inputmode="email"
                    required
                    prepend-inner-icon="mdi-email-fast-outline"
                    variant="outlined"
                  />
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="subject"
                    :label="t('contact.form.fields.subject.label')"
                    :placeholder="t('contact.form.fields.subject.placeholder')"
                    :counter="180"
                    :rules="subjectRules"
                    :disabled="submitting"
                    autocomplete="on"
                    required
                    prepend-inner-icon="mdi-form-textbox"
                    variant="outlined"
                  />
                </v-col>

                <v-col cols="12">
                  <v-textarea
                    v-model="message"
                    :label="t('contact.form.fields.message.label')"
                    :placeholder="t('contact.form.fields.message.placeholder')"
                    :counter="1200"
                    :rules="messageRules"
                    :disabled="submitting"
                    auto-grow
                    rows="5"
                    required
                    prepend-inner-icon="mdi-message-text-outline"
                    variant="outlined"
                  />
                </v-col>

                <v-col cols="12">
                  <div class="contact-form__captcha">
                    <ClientOnly v-if="hasSiteKey">
                      <VueHcaptcha
                        ref="captchaRef"
                        :sitekey="siteKey"
                        :language="captchaLocale"
                        :theme="captchaTheme"
                        @verify="handleCaptchaVerify"
                        @expired="handleCaptchaExpired"
                        @error="handleCaptchaError"
                      />
                      <template #fallback>
                        <div
                          class="contact-form__captcha-placeholder"
                          aria-hidden="true"
                        >
                          <v-icon
                            icon="mdi-shield-alert-outline"
                            size="36"
                            color="primary"
                          />
                        </div>
                      </template>
                    </ClientOnly>
                    <div
                      v-else
                      class="contact-form__captcha-placeholder"
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
                      class="contact-form__captcha-error"
                      role="alert"
                    >
                      {{ captchaError }}
                    </p>
                  </div>
                </v-col>

                <v-col cols="12">
                  <div class="contact-form__actions">
                    <v-btn
                      type="submit"
                      color="primary"
                      size="large"
                      :loading="submitting"
                      :disabled="submitting || !hasSiteKey || !captchaToken"
                      prepend-icon="mdi-send"
                    >
                      {{ t('contact.form.actions.submit') }}
                    </v-btn>
                    <v-btn
                      variant="text"
                      color="primary"
                      :disabled="submitting"
                      @click="resetForm"
                    >
                      {{ t('contact.form.actions.reset') }}
                    </v-btn>
                  </div>
                  <p class="contact-form__privacy">
                    {{ t('contact.form.privacy') }}
                  </p>
                </v-col>
              </v-row>
            </v-form>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTheme } from 'vuetify'
import { VForm } from 'vuetify/components'

export interface ContactFormPayload {
  name: string
  email: string
  subject: string
  message: string
  hCaptchaResponse: string
}

const props = defineProps<{
  submitting: boolean
  success: boolean
  errorMessage: string | null
  siteKey: string
  initialSubject?: string
  initialMessage?: string
}>()

const emit = defineEmits<{
  (event: 'submit', payload: ContactFormPayload): void
  (event: 'reset-feedback'): void
}>()

const { t, locale } = useI18n()
const theme = useTheme()
const VueHcaptcha = defineAsyncComponent(
  () => import('@hcaptcha/vue3-hcaptcha')
)
const formRef = ref<InstanceType<typeof VForm> | null>(null)
const captchaRef = ref<InstanceType<typeof VueHcaptcha> | null>(null)

const name = ref('')
const email = ref('')
const subject = ref('')
const message = ref('')
const captchaToken = ref('')
const captchaError = ref<string | null>(null)

const hasSiteKey = computed(() => props.siteKey?.length > 0)
const siteKey = computed(() => props.siteKey)

const captchaTheme = computed(() =>
  theme.global.current.value.dark ? 'dark' : 'light'
)
const captchaLocale = computed(() =>
  locale.value?.startsWith('fr') ? 'fr' : 'en'
)

const nameRules = computed(() => [
  (value: string) => !!value?.trim() || t('contact.form.errors.name.required'),
  (value: string) =>
    (value?.trim().length ?? 0) >= 2 || t('contact.form.errors.name.length'),
])

const emailPattern =
  /^(?:[a-zA-Z0-9_'^&/+-])+(?:\.(?:[a-zA-Z0-9_'^&/+-])+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/u

const emailRules = computed(() => [
  (value: string) => !!value?.trim() || t('contact.form.errors.email.required'),
  (value: string) =>
    emailPattern.test(value?.trim() ?? '') ||
    t('contact.form.errors.email.invalid'),
])

const subjectRules = computed(() => [
  (value: string) =>
    !!value?.trim() || t('contact.form.errors.subject.required'),
  (value: string) =>
    (value?.trim().length ?? 0) >= 3 || t('contact.form.errors.subject.length'),
])

const messageRules = computed(() => [
  (value: string) =>
    !!value?.trim() || t('contact.form.errors.message.required'),
  (value: string) =>
    (value?.trim().length ?? 0) >= 10 ||
    t('contact.form.errors.message.length'),
])

const handleCaptchaVerify = (token: string) => {
  captchaToken.value = token
  captchaError.value = null
}

const handleCaptchaExpired = () => {
  captchaToken.value = ''
  captchaError.value = t('contact.form.errors.captchaExpired')
  captchaRef.value?.reset()
}

const handleCaptchaError = () => {
  captchaToken.value = ''
  captchaError.value = t('contact.form.errors.captchaFailed')
  captchaRef.value?.reset()
}

const resetCaptcha = () => {
  captchaToken.value = ''
  captchaError.value = null
  captchaRef.value?.reset()
}

const clearFormFields = () => {
  name.value = ''
  email.value = ''
  subject.value = ''
  message.value = ''
  formRef.value?.resetValidation()
  resetCaptcha()
}

const resetForm = () => {
  emit('reset-feedback')
  clearFormFields()
}

const handleSubmit = async () => {
  emit('reset-feedback')
  captchaError.value = null

  const validation = await formRef.value?.validate()

  if (!validation?.valid) {
    return
  }

  if (!captchaToken.value) {
    captchaError.value = t('contact.form.errors.missingCaptcha')
    return
  }

  emit('submit', {
    name: name.value.trim(),
    email: email.value.trim(),
    subject: subject.value.trim(),
    message: message.value.trim(),
    hCaptchaResponse: captchaToken.value,
  })
}

watch(
  () => props.initialSubject,
  value => {
    subject.value = value?.trim() ?? ''
  },
  { immediate: true }
)

watch(
  () => props.initialMessage,
  value => {
    message.value = value?.trim() ?? ''
  },
  { immediate: true }
)

watch(
  () => props.success,
  value => {
    if (!value) {
      return
    }

    clearFormFields()
  }
)

watch(
  () => props.errorMessage,
  value => {
    if (value) {
      resetCaptcha()
    }
  }
)
</script>

<style scoped lang="sass">
.contact-form
  background: linear-gradient(180deg, rgba(var(--v-theme-surface-ice-050), 0.35) 0%, rgb(var(--v-theme-surface-default)) 100%)

  &__card
    padding: clamp(2rem, 5vw, 3rem)
    border: 1px solid rgb(var(--v-theme-surface-primary-080))
    background: rgb(var(--v-theme-surface-glass-strong))
    backdrop-filter: blur(10px)

  &__header
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin-bottom: 1.5rem
    text-align: center

  &__eyebrow
    align-self: center
    padding: 0.35rem 1rem
    border-radius: 999px
    background: rgb(var(--v-theme-surface-primary-120))
    color: rgb(var(--v-theme-primary))
    letter-spacing: 0.08em
    text-transform: uppercase
    font-weight: 600
    font-size: 0.82rem

  &__title
    font-size: clamp(2rem, 3vw, 2.4rem)
    font-weight: 700
    margin: 0

  &__subtitle
    font-size: 1.05rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    margin: 0

  &__form
    margin-top: 1.5rem

  &__captcha
    display: flex
    flex-direction: column
    gap: 0.5rem
    align-items: center

  &__captcha-placeholder
    display: flex
    align-items: center
    justify-content: center
    width: 100%
    min-height: 100px
    border-radius: 12px
    border: 1px dashed rgb(var(--v-theme-border-primary-strong))
    background: rgb(var(--v-theme-surface-primary-050))

  &__captcha-error
    color: rgb(var(--v-theme-error))
    font-size: 0.9rem
    margin: 0

  &__actions
    display: flex
    gap: 1rem
    align-items: center
    flex-wrap: wrap
    justify-content: center
    margin-bottom: 0.75rem

  &__privacy
    text-align: center
    font-size: 0.9rem
    color: rgba(var(--v-theme-text-neutral-strong), 0.6)
    margin: 0

@media (max-width: 599px)
  .contact-form
    &__actions
      flex-direction: column
      align-items: stretch

    &__privacy
      font-size: 0.85rem
</style>
