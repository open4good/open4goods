<template>
  <div class="feedback-page">
    <FeedbackHero
      :eyebrow="t('feedback.hero.eyebrow')"
      :title="t('feedback.hero.title')"
      :subtitle="t('feedback.hero.subtitle')"
      :description="t('feedback.hero.description')"
      :primary-cta="heroPrimaryCta"
      :secondary-cta="heroSecondaryCta"
      :cta-group-label="t('feedback.hero.ctaGroupLabel')"
      :stats="heroStats"
    />

    <section
      id="feedback-tabs"
      class="feedback-tabs"
      aria-labelledby="feedback-tabs-heading"
    >
      <v-container class="py-16">
        <header class="feedback-tabs__header">
          <p class="feedback-tabs__eyebrow">{{ t('feedback.tabs.eyebrow') }}</p>
          <h2 id="feedback-tabs-heading" class="feedback-tabs__title">
            {{ t('feedback.tabs.title') }}
          </h2>
          <p class="feedback-tabs__description">
            {{ t('feedback.tabs.description') }}
          </p>
        </header>

        <v-tabs
          v-model="selectedTab"
          class="feedback-tabs__tabs"
          align-tabs="center"
          grow
          density="comfortable"
          :aria-label="t('feedback.tabs.ariaLabel')"
        >
          <v-tab
            v-for="tab in tabs"
            :key="tab.value"
            :value="tab.value"
            class="feedback-tabs__tab"
            :prepend-icon="tab.icon"
            :aria-controls="`${tab.value.toLowerCase()}-panel`"
          >
            <span class="feedback-tabs__tab-label">{{ tab.label }}</span>
          </v-tab>
        </v-tabs>

        <v-window v-model="selectedTab" class="feedback-tabs__window">
          <v-window-item
            v-for="tab in tabs"
            :key="tab.value"
            :value="tab.value"
          >
            <v-row class="g-8 mt-8" align="stretch">
              <v-col cols="12" md="6">
                <ClientOnly>
                  <FeedbackIssueList
                    :heading-id="`${tab.value.toLowerCase()}-issues-heading`"
                    :title="tab.issueTitle"
                    :description="tab.description"
                    :issues="issuesByType[tab.value]"
                    :loading="issueLoadingStates[tab.value]"
                    :error-message="issueErrorMessages[tab.value]"
                    :empty-state-label="tab.emptyMessage"
                    :vote-button-label="t('feedback.voting.voteButton')"
                    :vote-button-aria-label="
                      t('feedback.voting.voteButtonAria')
                    "
                    :open-issue-label="t('feedback.voting.openIssue')"
                    :status-message="voteStatusMessage"
                    :remaining-votes="remainingVotes"
                    :can-vote="canVote"
                    :vote-pending-id="votingIssueId"
                    :vote-disabled-when-no-votes-message="
                      t('feedback.voting.noVotes')
                    "
                    :vote-disabled-when-blocked-message="
                      t('feedback.voting.limitReached')
                    "
                    :issue-icon="tab.icon"
                    :vote-completed-label="t('feedback.voting.voteCompleted')"
                    :voted-issue-ids="votedIssueIds"
                    @vote="issueId => handleVote(issueId, tab.value)"
                  />

                  <template #fallback>
                    <v-skeleton-loader
                      class="feedback-tabs__issues-fallback"
                      max-width="420"
                      type="heading, paragraph, list-item-two-line@3"
                    />
                  </template>
                </ClientOnly>
              </v-col>

              <v-col cols="12" md="6">
                <FeedbackSubmissionForm
                  :section-id="`${tab.value.toLowerCase()}-form`"
                  :eyebrow="tab.formEyebrow"
                  :title="tab.formTitle"
                  :subtitle="tab.formSubtitle"
                  :intro="tab.formIntro"
                  :category-icon="tab.icon"
                  :category-type="tab.value"
                  :submitting="submissionState.submitting"
                  :success="
                    submissionState.success && submittedCategory === tab.value
                  "
                  :error-message="submissionState.errorMessage"
                  :site-key="siteKey"
                  :author-label="t('feedback.form.fields.author.label')"
                  :author-placeholder="
                    t('feedback.form.fields.author.placeholder')
                  "
                  :default-author="t('feedback.form.fields.author.default')"
                  :title-label="t('feedback.form.fields.title.label')"
                  :title-placeholder="tab.formTitlePlaceholder"
                  :message-label="t('feedback.form.fields.message.label')"
                  :message-placeholder="tab.formMessagePlaceholder"
                  :submit-label="t('feedback.form.actions.submit')"
                  :reset-label="t('feedback.form.actions.reset')"
                  :success-message="t('feedback.form.feedback.success')"
                  :missing-captcha-message="
                    t('feedback.form.feedback.missingSiteKey')
                  "
                  :privacy-notice="t('feedback.form.privacy')"
                  :captcha-missing-message="
                    t('feedback.form.errors.missingCaptcha')
                  "
                  :captcha-expired-message="
                    t('feedback.form.errors.captchaExpired')
                  "
                  :captcha-failed-message="
                    t('feedback.form.errors.captchaFailed')
                  "
                  :title-too-short-message="
                    t('feedback.form.errors.title.length')
                  "
                  :message-too-short-message="
                    t('feedback.form.errors.message.length')
                  "
                  :current-locale="currentLocale"
                  :current-url="currentUrl"
                  @submit="handleSubmitFeedback"
                  @reset-feedback="clearSubmissionFeedback"
                />
              </v-col>
            </v-row>
          </v-window-item>
        </v-window>
      </v-container>
    </section>

    <FeedbackOpenSourceSection
      :eyebrow="t('feedback.openSource.eyebrow')"
      :title="t('feedback.openSource.title')"
      :description="t('feedback.openSource.description')"
      :cards="openSourceCards"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { FetchError } from 'ofetch'
import FeedbackHero from '~/components/domains/feedback/FeedbackHero.vue'
import FeedbackIssueList, {
  type FeedbackIssueDisplay,
} from '~/components/domains/feedback/FeedbackIssueList.vue'
import FeedbackSubmissionForm, {
  type FeedbackFormSubmitPayload,
} from '~/components/domains/feedback/FeedbackSubmissionForm.vue'
import FeedbackOpenSourceSection from '~/components/domains/feedback/FeedbackOpenSourceSection.vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type {
  FeedbackIssueDto,
  FeedbackRemainingVotesDto,
  FeedbackVoteEligibilityDto,
  FeedbackSubmissionResponseDto,
} from '~~/shared/api-client'

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const requestURL = useRequestURL()
const runtimeConfig = useRuntimeConfig()
const localePath = useLocalePath()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const siteKey = computed(() => runtimeConfig.public.hcaptchaSiteKey ?? '')
const currentUrl = computed(() => requestURL.href)
const currentLocale = computed(() => locale.value)

const tabs = computed(() => [
  {
    value: 'IDEA' as const,
    label: String(t('feedback.tabs.idea.label')),
    icon: 'mdi-lightbulb-on-outline',
    issueTitle: String(t('feedback.tabs.idea.issueTitle')),
    description: String(t('feedback.tabs.idea.description')),
    emptyMessage: String(t('feedback.issues.empty.idea')),
    formEyebrow: String(t('feedback.form.sections.idea.eyebrow')),
    formTitle: String(t('feedback.form.sections.idea.title')),
    formSubtitle: String(t('feedback.form.sections.idea.subtitle')),
    formIntro: String(t('feedback.form.sections.idea.intro')),
    formTitlePlaceholder: String(
      t('feedback.form.sections.idea.titlePlaceholder')
    ),
    formMessagePlaceholder: String(
      t('feedback.form.sections.idea.messagePlaceholder')
    ),
  },
  {
    value: 'BUG' as const,
    label: String(t('feedback.tabs.bug.label')),
    icon: 'mdi-bug-check-outline',
    issueTitle: String(t('feedback.tabs.bug.issueTitle')),
    description: String(t('feedback.tabs.bug.description')),
    emptyMessage: String(t('feedback.issues.empty.bug')),
    formEyebrow: String(t('feedback.form.sections.bug.eyebrow')),
    formTitle: String(t('feedback.form.sections.bug.title')),
    formSubtitle: String(t('feedback.form.sections.bug.subtitle')),
    formIntro: String(t('feedback.form.sections.bug.intro')),
    formTitlePlaceholder: String(
      t('feedback.form.sections.bug.titlePlaceholder')
    ),
    formMessagePlaceholder: String(
      t('feedback.form.sections.bug.messagePlaceholder')
    ),
  },
])

type FeedbackCategory = 'IDEA' | 'BUG'

const selectedTab = ref<FeedbackCategory>('IDEA')

const loadErrorLabel = computed(() => String(t('feedback.issues.loadError')))

const issueErrorMessages = reactive<Record<FeedbackCategory, string | null>>({
  IDEA: null,
  BUG: null,
})

const issueLoadFailed = reactive<Record<FeedbackCategory, boolean>>({
  IDEA: false,
  BUG: false,
})

const fetchIssues = async (category: FeedbackCategory) => {
  try {
    const response = await $fetch<FeedbackIssueDto[]>('/api/feedback/issues', {
      headers: requestHeaders,
      query: { type: category },
    })

    issueErrorMessages[category] = null
    issueLoadFailed[category] = false
    return response
  } catch (error) {
    console.warn('Falling back to empty feedback issues list', {
      category,
      error,
    })
    issueErrorMessages[category] = loadErrorLabel.value
    issueLoadFailed[category] = true
    return []
  }
}

const votesRequestNonce = ref(0)
interface VotesQuery {
  cacheBuster?: string
}

const buildVotesQuery = (): VotesQuery => {
  const query: VotesQuery = {}

  if (votesRequestNonce.value > 0) {
    query.cacheBuster = String(votesRequestNonce.value)
  }

  return query
}

const ideaIssues = ref<FeedbackIssueDisplay[]>([])
const bugIssues = ref<FeedbackIssueDisplay[]>([])
const ideaIssuesLoading = ref(true)
const bugIssuesLoading = ref(true)

const loadIssuesForCategory = async (category: FeedbackCategory) => {
  if (!import.meta.client) {
    return
  }

  const targetIssuesRef = category === 'IDEA' ? ideaIssues : bugIssues
  const loadingRef = category === 'IDEA' ? ideaIssuesLoading : bugIssuesLoading

  loadingRef.value = true

  try {
    const response = await fetchIssues(category)
    targetIssuesRef.value = response as FeedbackIssueDisplay[]
  } finally {
    loadingRef.value = false
  }
}

const remainingVotesState = ref<FeedbackRemainingVotesDto | null>(null)
const canVoteState = ref<FeedbackVoteEligibilityDto | null>(null)
const votesRemainingEndpoint: string = '/api/feedback/votes/remaining'
const votesEligibilityEndpoint: string = '/api/feedback/votes/can'

const loadRemainingVotes = async () => {
  if (!import.meta.client) {
    return
  }

  try {
    remainingVotesState.value = await $fetch<FeedbackRemainingVotesDto, string>(
      votesRemainingEndpoint,
      {
        query: buildVotesQuery(),
        headers: {
          ...requestHeaders,
          'cache-control': 'no-cache',
        },
      }
    )
  } catch (error) {
    console.warn(
      'Unable to load remaining votes, falling back to unknown state',
      error
    )
    remainingVotesState.value = {}
  }
}

const loadVoteEligibility = async () => {
  if (!import.meta.client) {
    return
  }

  try {
    canVoteState.value = await $fetch<FeedbackVoteEligibilityDto, string>(
      votesEligibilityEndpoint,
      {
        query: buildVotesQuery(),
        headers: {
          ...requestHeaders,
          'cache-control': 'no-cache',
        },
      }
    )
  } catch (error) {
    console.warn(
      'Unable to determine vote eligibility, assuming voting is allowed',
      error
    )
    canVoteState.value = { canVote: true }
  }
}

const issuesByType = computed<Record<FeedbackCategory, FeedbackIssueDisplay[]>>(
  () => ({
    IDEA: ideaIssues.value,
    BUG: bugIssues.value,
  })
)

const issueLoadingStates = computed<Record<FeedbackCategory, boolean>>(() => ({
  IDEA: ideaIssuesLoading.value,
  BUG: bugIssuesLoading.value,
}))

const remainingVotes = computed(
  () => remainingVotesState.value?.remainingVotes ?? null
)
const canVote = computed(() => canVoteState.value?.canVote ?? true)

const voteStatusMessage = computed(() => {
  if (!canVote.value) {
    return String(t('feedback.voting.limitReached'))
  }

  if (remainingVotes.value !== null) {
    if (remainingVotes.value <= 0) {
      return String(t('feedback.voting.noVotes'))
    }

    return String(
      t('feedback.voting.remaining', { count: remainingVotes.value })
    )
  }

  return null
})

const votingIssueId = ref<string | null>(null)
const votedIssueIds = ref<string[]>([])

const submissionState = reactive({
  submitting: false,
  success: false,
  errorMessage: null as string | null,
})

const submittedCategory = ref<FeedbackCategory>('IDEA')

const voteErrors = reactive<Record<FeedbackCategory, string | null>>({
  IDEA: null,
  BUG: null,
})

const extractErrorMessage = (error: unknown): string => {
  if (error instanceof FetchError) {
    const data = error.data as {
      statusMessage?: string
      message?: string
    } | null

    if (data?.statusMessage) {
      return data.statusMessage
    }

    if (data?.message) {
      return data.message
    }

    if (error.message) {
      return error.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  if (typeof error === 'string' && error.length > 0) {
    return error
  }

  return String(t('feedback.common.genericError'))
}

const refreshVotesState = async () => {
  votesRequestNonce.value = Date.now()
  await Promise.allSettled([loadRemainingVotes(), loadVoteEligibility()])
}

const handleVote = async (
  issueId: string | undefined,
  category: FeedbackCategory
) => {
  if (!issueId) {
    return
  }

  votingIssueId.value = issueId
  voteErrors[category] = null

  try {
    await $fetch('/api/feedback/vote', {
      method: 'POST',
      headers: requestHeaders,
      body: { issueId },
    })

    if (!votedIssueIds.value.includes(issueId)) {
      votedIssueIds.value = [...votedIssueIds.value, issueId]
    }

    const currentRemainingVotes = remainingVotesState.value?.remainingVotes

    if (typeof currentRemainingVotes === 'number') {
      const nextRemainingVotes = Math.max(currentRemainingVotes - 1, 0)

      if (!remainingVotesState.value) {
        remainingVotesState.value = { remainingVotes: nextRemainingVotes }
      } else {
        remainingVotesState.value = {
          ...remainingVotesState.value,
          remainingVotes: nextRemainingVotes,
        }
      }

      if (nextRemainingVotes <= 0) {
        if (!canVoteState.value) {
          canVoteState.value = { canVote: false }
        } else {
          canVoteState.value = {
            ...canVoteState.value,
            canVote: false,
          }
        }
      }
    }

    await loadIssuesForCategory(category)

    await refreshVotesState()
  } catch (error) {
    console.error('Failed to vote on feedback issue', error)
    voteErrors[category] = extractErrorMessage(error)
  } finally {
    votingIssueId.value = null
  }
}

watch(
  () => voteErrors.IDEA,
  error => {
    if (error) {
      issueErrorMessages.IDEA = error
    } else if (!issueLoadFailed.IDEA) {
      issueErrorMessages.IDEA = null
    }
  }
)

watch(
  () => voteErrors.BUG,
  error => {
    if (error) {
      issueErrorMessages.BUG = error
    } else if (!issueLoadFailed.BUG) {
      issueErrorMessages.BUG = null
    }
  }
)

const handleSubmitFeedback = async (payload: FeedbackFormSubmitPayload) => {
  submissionState.submitting = true
  submissionState.success = false
  submissionState.errorMessage = null
  submittedCategory.value = payload.type as FeedbackCategory

  try {
    const response = await $fetch<FeedbackSubmissionResponseDto>(
      '/api/feedback',
      {
        method: 'POST',
        headers: requestHeaders,
        body: payload,
      }
    )

    if (!response.success) {
      submissionState.success = false
      submissionState.errorMessage =
        response.message ?? String(t('feedback.common.genericError'))
      return
    }

    submissionState.success = true

    appendSubmittedIssue(payload.type as FeedbackCategory, payload, response)
  } catch (error) {
    console.error('Failed to submit feedback', error)
    submissionState.success = false
    submissionState.errorMessage = extractErrorMessage(error)
  } finally {
    submissionState.submitting = false
  }
}

const clearSubmissionFeedback = () => {
  if (submissionState.success || submissionState.errorMessage) {
    submissionState.success = false
    submissionState.errorMessage = null
  }
}

const heroPrimaryCta = computed(() => ({
  label: String(t('feedback.hero.primaryCta.label')),
  ariaLabel: String(t('feedback.hero.primaryCta.ariaLabel')),
  href: '#feedback-tabs',
  icon: 'mdi-arrow-down-bold',
  onClick: handleHeroPrimaryCtaClick,
}))

const heroSecondaryCta = computed(() => ({
  label: String(t('feedback.hero.secondaryCta.label')),
  ariaLabel: String(t('feedback.hero.secondaryCta.ariaLabel')),
  href: 'https://github.com/open4good/open4goods/issues',
  target: '_blank',
  rel: 'noopener nofollow',
  icon: 'mdi-open-in-new',
}))

const heroStats = computed(() => ({
  eyebrow: String(t('feedback.hero.stats.eyebrow')),
  title: String(t('feedback.hero.stats.title')),
  description: String(t('feedback.hero.stats.description')),
  items: [
    {
      icon: 'mdi-progress-check',
      label: String(t('feedback.hero.stats.items.iterations')),
    },
    {
      icon: 'mdi-vote-outline',
      label: String(t('feedback.hero.stats.items.votes')),
    },
    {
      icon: 'mdi-database-eye-outline',
      label: String(t('feedback.hero.stats.items.transparency')),
    },
  ],
}))

const openSourceCards = computed(() => [
  {
    icon: 'mdi-github',
    title: String(t('feedback.openSource.cards.opensource.title')),
    description: String(t('feedback.openSource.cards.opensource.description')),
    cta: {
      label: String(t('feedback.openSource.cards.opensource.cta')),
      ariaLabel: String(t('feedback.openSource.cards.opensource.ariaLabel')),
      href: localePath('opensource'),
    },
  },
  {
    icon: 'mdi-database-search',
    title: String(t('feedback.openSource.cards.opendata.title')),
    description: String(t('feedback.openSource.cards.opendata.description')),
    cta: {
      label: String(t('feedback.openSource.cards.opendata.cta')),
      ariaLabel: String(t('feedback.openSource.cards.opendata.ariaLabel')),
      href: localePath('opendata'),
    },
  },
])

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('feedback', locale.value),
    requestURL.origin
  ).toString()
)

const appendSubmittedIssue = (
  category: FeedbackCategory,
  payload: FeedbackFormSubmitPayload,
  response: FeedbackSubmissionResponseDto
) => {
  const issuesRef = category === 'IDEA' ? ideaIssues : bugIssues
  const existingIssues = issuesRef.value ?? []
  const trimmedTitle = payload.title.trim()

  const newIssue: FeedbackIssueDisplay = {
    number: response.issueNumber ?? undefined,
    title: trimmedTitle,
    url: response.issueUrl ?? undefined,
    votes: 0,
  }

  const deduplicatedIssues = existingIssues.filter(issue => {
    if (response.issueNumber && issue.number === response.issueNumber) {
      return false
    }

    if (response.issueUrl && issue.url === response.issueUrl) {
      return false
    }

    if (
      !response.issueNumber &&
      issue.title?.trim().toLowerCase() === trimmedTitle.toLowerCase()
    ) {
      return false
    }

    return true
  })

  issuesRef.value = [newIssue, ...deduplicatedIssues]
}

const handleHeroPrimaryCtaClick = (event: MouseEvent) => {
  if (!import.meta.client) {
    return
  }

  const target = document.getElementById('feedback-tabs')

  if (!target) {
    return
  }

  event.preventDefault()
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const ogImageAlt = computed(() => String(t('feedback.seo.imageAlt')))

const alternateLinks = computed(() =>
  availableLocales.map(availableLocale => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      resolveLocalizedRoutePath('feedback', availableLocale),
      requestURL.origin
    ).toString(),
  }))
)

useSeoMeta({
  title: () => String(t('feedback.seo.title')),
  description: () => String(t('feedback.seo.description')),
  ogTitle: () => String(t('feedback.seo.title')),
  ogDescription: () => String(t('feedback.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
}))

onMounted(async () => {
  if (!import.meta.client) {
    return
  }

  await Promise.allSettled([
    loadIssuesForCategory('IDEA'),
    loadIssuesForCategory('BUG'),
    loadRemainingVotes(),
    loadVoteEligibility(),
  ])
})
</script>

<style scoped lang="scss">
.feedback-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.feedback-tabs {
  background-color: rgb(var(--v-theme-surface-default));

  &__header {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    max-width: 54rem;
  }

  &__eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.14em;
    font-weight: 600;
    color: rgb(var(--v-theme-primary));
  }

  &__title {
    font-size: clamp(2rem, 3vw, 2.8rem);
    font-weight: 700;
    margin: 0;
  }

  &__description {
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__tabs {
    margin-top: 2rem;
    border-radius: 999px;
    background-color: rgba(var(--v-theme-surface-glass), 0.8);
    box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.25);
  }

  &__tab {
    flex-direction: column;
    text-transform: none;
    font-weight: 600;
    gap: 0.25rem;
  }

  &__tab-label {
    font-size: 1.05rem;
  }

  &__tab-caption {
    font-size: 0.85rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__window {
    margin-top: 2.5rem;
  }

  &__issues-fallback {
    width: 100%;
    border-radius: 1rem;
  }
}
</style>
