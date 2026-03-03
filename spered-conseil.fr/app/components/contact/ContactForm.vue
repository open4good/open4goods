<template>
  <v-card class="pa-6" rounded="lg" elevation="2">
    <h2 class="text-h5 mb-4">{{ t('contact.form.title') }}</h2>

    <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-4">{{ successMessage }}</v-alert>
    <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4">{{ errorMessage }}</v-alert>

    <v-form @submit.prevent="submitForm">
      <v-text-field v-model="form.name" :label="t('contact.form.name')" required class="mb-2" />
      <v-text-field v-model="form.email" :label="t('contact.form.email')" type="email" required class="mb-2" />
      <v-text-field v-model="form.subject" :label="t('contact.form.subject')" required class="mb-2" />
      <v-textarea v-model="form.message" :label="t('contact.form.message')" rows="5" required class="mb-2" />

      <ClientOnly>
        <VueHcaptcha
          v-if="siteKey"
          ref="captchaRef"
          :sitekey="siteKey"
          :language="captchaLanguage"
          @verify="onCaptchaVerify"
          @expired="onCaptchaExpired"
          @error="onCaptchaError"
        />
      </ClientOnly>

      <v-alert v-if="!siteKey" type="warning" variant="tonal" class="my-3">
        {{ t('contact.form.missingSiteKey') }}
      </v-alert>

      <v-btn type="submit" color="primary" :loading="submitting" class="mt-4">
        {{ t('contact.form.send') }}
      </v-btn>
    </v-form>
  </v-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { defineAsyncComponent } from 'vue'

const VueHcaptcha = defineAsyncComponent(() => import('@hcaptcha/vue3-hcaptcha'))

const runtimeConfig = useRuntimeConfig()
const { locale, t } = useI18n()

const siteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey)
const captchaLanguage = computed(() => (locale.value === 'fr-FR' ? 'fr' : 'en'))

const captchaToken = ref('')
const captchaRef = ref<{ resetCaptcha?: () => void } | null>(null)
const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const form = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const onCaptchaVerify = (token: string) => {
  captchaToken.value = token
}

const onCaptchaExpired = () => {
  captchaToken.value = ''
}

const onCaptchaError = () => {
  captchaToken.value = ''
  errorMessage.value = String(t('contact.form.captchaError'))
}

const resetForm = () => {
  form.name = ''
  form.email = ''
  form.subject = ''
  form.message = ''
  captchaToken.value = ''
  captchaRef.value?.resetCaptcha?.()
}

const submitForm = async () => {
  successMessage.value = ''
  errorMessage.value = ''

  if (!captchaToken.value) {
    errorMessage.value = String(t('contact.form.captchaRequired'))
    return
  }

  submitting.value = true

  try {
    await $fetch('/api/contact', {
      method: 'POST',
      body: {
        ...form,
        hCaptchaResponse: captchaToken.value,
      },
    })

    successMessage.value = String(t('contact.form.success'))
    resetForm()
  } catch (error) {
    const message = (error as { data?: { statusMessage?: string } })?.data?.statusMessage
    errorMessage.value = message || String(t('contact.form.error'))
  } finally {
    submitting.value = false
  }
}
</script>
