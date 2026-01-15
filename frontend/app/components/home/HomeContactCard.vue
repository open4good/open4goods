<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLocalePath } from '#i18n'

const { t } = useI18n()
const localePath = useLocalePath()
const router = useRouter()

const contactRedirectMessage = ref(
  t('home.contactRedirect.prefilledMessage') || ''
)
const contactRedirectSubject = computed(() =>
  t('home.contactRedirect.prefilledSubject').trim()
)
const contactRedirectTitleKey = 'contact.prefill.title.question'

const CONTACT_REDIRECT_MAX_LENGTH = 400

const handleContactRedirect = () => {
  const sanitizedMessage = contactRedirectMessage.value
    ?.trim()
    .slice(0, CONTACT_REDIRECT_MAX_LENGTH)

  const query: Record<string, string> = {}

  if (contactRedirectSubject.value) {
    query.subject = contactRedirectSubject.value
  }

  if (sanitizedMessage) {
    query.message = sanitizedMessage
  }

  query.titleKey = contactRedirectTitleKey

  router.push(
    localePath({
      name: 'contact',
      query: Object.keys(query).length ? query : undefined,
    })
  )
}
</script>

<template>
  <section
    class="home-contact-redirect"
    aria-labelledby="home-contact-redirect-heading"
  >
    <v-card
      class="home-contact-redirect__card"
      elevation="0"
      rounded="xl"
      variant="flat"
      color="surface-primary-050"
    >
      <div class="home-contact-redirect__content">
        <div class="home-contact-redirect__texts">
          <!-- <p class="home-contact-redirect__eyebrow">
            {{ t('home.contactRedirect.eyebrow') }}
          </p> -->
          <!-- <h3
            id="home-contact-redirect-heading"
            class="home-contact-redirect__title"
          >
            {{ t('home.contactRedirect.title') }}
          </h3> -->
          <!-- <p class="home-contact-redirect__subtitle">
            {{ t('home.contactRedirect.subtitle') }}
          </p> -->
        </div>

        <v-form
          class="home-contact-redirect__form"
          @submit.prevent="handleContactRedirect"
        >
          <v-textarea
            v-model="contactRedirectMessage"
            :label="t('home.contactRedirect.inputLabel')"
            :counter="CONTACT_REDIRECT_MAX_LENGTH"
            prepend-inner-icon="mdi-help-circle-outline"
            variant="outlined"
            hide-details="auto"
            clearable
            rows="3"
            color="primary"
          />
          <div class="home-contact-redirect__actions">
            <v-btn
              type="submit"
              color="primary"
              class="nudger_degrade-defaut"
              size="large"
              prepend-icon="mdi-arrow-right"
              block
              elevation="0"
            >
              {{ t('home.contactRedirect.cta') }}
            </v-btn>
            <!-- <v-btn
              :to="localePath({ name: 'contact' })"
              variant="text"
              color="primary"
            >
              {{ t('home.contactRedirect.secondaryCta') }}
            </v-btn> -->
          </div>
          <p class="home-contact-redirect__helper">
            {{ t('home.contactRedirect.helper') }}
          </p>
        </v-form>
      </div>
    </v-card>
  </section>
</template>

<style scoped lang="sass">
.home-contact-redirect__card
  border: 1px solid rgb(var(--v-theme-surface-primary-080))
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-glass), 0.35) 0%, rgb(var(--v-theme-surface-default)) 100%)

.home-contact-redirect__content
  display: flex
  flex-direction: column
  gap: 1.25rem
  padding: 1.5rem

.home-contact-redirect__texts
  display: flex
  flex-direction: column
  gap: 0.5rem

.home-contact-redirect__form
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-contact-redirect__actions
  display: flex
  gap: 0.75rem
  flex-wrap: wrap
  align-items: center

.home-contact-redirect__helper
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 0.72)
  font-size: 0.85rem
  text-align: center
</style>
