<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import TextContent from '~/components/domains/content/TextContent.vue'
import AgentPromptInput from '@/components/agent/AgentPromptInput.vue'
import { useAgent } from '@/composables/useAgent'
import type {
  AgentActivityDto,
  AgentRequestDto,
  AgentRequestDtoPromptVisibilityEnum,
  AgentRequestDtoTypeEnum,
  AgentRequestResponseDto,
  AgentTemplateDto,
} from '~~/shared/api-client/services/agents.services'
import type { DomainLanguage } from '~~/app/types/agent'

type FaqItem = {
  question: string
  answer: string
  blocId: string
}

type AgentFaqAppendPayload = Pick<FaqItem, 'question' | 'answer'>

type LastAgentResult = {
  question: string
  issueUrl?: string
  previewUrl?: string
  status?: string
  visibility?: string
}

const props = defineProps<{
  items: FaqItem[]
}>()

const emit = defineEmits<{
  (event: 'append-faq', item: AgentFaqAppendPayload): void
}>()

const { t, locale } = useI18n()
const { listTemplates, submitRequest, listActivity } = useAgent()

const template = ref<AgentTemplateDto | null>(null)
const submitting = ref(false)
const lastResult = ref<LastAgentResult | null>(null)

const localeCode = computed(() => locale.value.split('-')[0] as DomainLanguage)

const hasPanels = computed(() => props.items.length > 0)

const lastResultLink = computed(
  () => lastResult.value?.previewUrl || lastResult.value?.issueUrl || ''
)

const decorateAnswer = (
  prompt: string,
  response: AgentRequestResponseDto
): string => {
  const link = response.previewUrl || response.issueUrl
  if (link) {
    return t('home.faq.agent.dynamicAnswerWithLink', { link })
  }

  if (response.workflowState) {
    return t('home.faq.agent.dynamicAnswerWithState', {
      state: response.workflowState,
    })
  }

  return t('home.faq.agent.dynamicAnswerPending', { question: prompt })
}

const appendAgentFaq = (payload: AgentFaqAppendPayload) => {
  emit('append-faq', payload)
}

const pickLatestPublicQuestion = (activities: AgentActivityDto[]) =>
  activities.find(
    activity =>
      activity.type === 'QUESTION' &&
      activity.promptVisibility === 'PUBLIC' &&
      Boolean(activity.promptSummary)
  )

const loadAgentContext = async () => {
  try {
    const [templates, activities] = await Promise.all([
      listTemplates(localeCode.value),
      listActivity(localeCode.value),
    ])

    template.value =
      templates.find(item => item.id === 'question') ?? templates.at(0) ?? null

    const lastQuestion = pickLatestPublicQuestion(activities)
    if (lastQuestion) {
      lastResult.value = {
        question:
          lastQuestion.promptSummary ||
          t('home.faq.agent.preview.questionFallback'),
        issueUrl: lastQuestion.issueUrl,
        status: lastQuestion.status,
        visibility: lastQuestion.promptVisibility,
      }
    }
  } catch (error) {
    console.error('Failed to load agent context', error)
  }
}

onMounted(loadAgentContext)

async function handleSubmit({
  prompt,
  isPrivate,
  attributeValues,
  captchaToken,
}: {
  prompt: string
  isPrivate: boolean
  attributeValues: Record<string, unknown>
  captchaToken?: string
}) {
  if (!template.value) {
    return
  }

  const trimmedPrompt = prompt.trim()

  if (!trimmedPrompt) {
    return
  }

  submitting.value = true
  try {
    const request: AgentRequestDto = {
      type: 'QUESTION' as unknown as AgentRequestDtoTypeEnum,
      promptUser: trimmedPrompt,
      promptTemplateId: template.value.id,
      promptVisibility: isPrivate
        ? AgentRequestDtoPromptVisibilityEnum.Private
        : AgentRequestDtoPromptVisibilityEnum.Public,
      attributeValues,
      captchaToken,
    }

    const response = await submitRequest(request, localeCode.value)

    lastResult.value = {
      question: trimmedPrompt,
      issueUrl: response.issueUrl,
      previewUrl: response.previewUrl,
      status: response.workflowState,
      visibility: response.promptVisibility,
    }

    appendAgentFaq({
      question: trimmedPrompt,
      answer: decorateAnswer(trimmedPrompt, response),
    })
  } catch (error) {
    console.error('Failed to submit question', error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="home-section home-faq" aria-labelledby="home-faq-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <h2 id="home-faq-title" class="home-hero__subtitle">
          {{ t('home.faq.title') }}
        </h2>
        <p class="home-section__subtitle text-center">
          {{ t('home.faq.subtitle') }}
        </p>
        <v-expansion-panels
          v-if="hasPanels"
          class="home-faq__panels"
          multiple
          variant="accordion"
        >
          <v-expansion-panel v-for="panel in props.items" :key="panel.blocId">
            <v-expansion-panel-title>
              <h3 class="home-faq__panel-title">{{ panel.question }}</h3>
            </v-expansion-panel-title>
            <v-expansion-panel-text class="home-faq__panel-text">
              <TextContent
                class="home-faq__text-content"
                :bloc-id="panel.blocId"
                :fallback-text="panel.answer"
                :ipsum-length="panel.answer.length"
              />
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-card class="home-faq__agent-card" elevation="0">
          <div class="home-faq__agent-header">
            <p class="home-faq__agent-eyebrow">
              {{ t('home.faq.agent.eyebrow') }}
            </p>
            <h3 class="home-faq__agent-title">
              {{ t('home.faq.agent.title') }}
            </h3>
            <p class="home-faq__agent-subtitle">
              {{ t('home.faq.agent.subtitle') }}
            </p>
          </div>

          <div class="home-faq__agent-form">
            <AgentPromptInput
              v-if="template"
              :template-name="template.name"
              :attributes="template.attributes"
              :can-toggle-visibility="!template.publicPromptHistory"
              :default-public="template.publicPromptHistory"
              :loading="submitting"
              @submit="handleSubmit"
            />
            <v-skeleton-loader
              v-else
              type="article, actions"
              class="home-faq__agent-skeleton"
            />
          </div>

          <v-divider class="my-6" />

          <div class="home-faq__agent-preview">
            <div class="home-faq__agent-preview-header">
              <span class="home-faq__agent-preview-eyebrow">
                {{ t('home.faq.agent.preview.eyebrow') }}
              </span>
              <p class="home-faq__agent-preview-title">
                {{ t('home.faq.agent.preview.title') }}
              </p>
              <p class="home-faq__agent-preview-subtitle">
                {{ t('home.faq.agent.preview.subtitle') }}
              </p>
            </div>

            <div class="home-faq__agent-preview-body">
              <div class="home-faq__agent-preview-question">
                <v-icon
                  icon="mdi-robot-happy-outline"
                  color="primary"
                  class="mr-2"
                />
                <div>
                  <p class="text-caption text-medium-emphasis mb-1">
                    {{ t('home.faq.agent.preview.label') }}
                  </p>
                  <p class="text-body-1 font-weight-medium mb-1">
                    {{
                      lastResult?.question ||
                      t('home.faq.agent.preview.questionFallback')
                    }}
                  </p>
                  <p class="text-caption text-medium-emphasis">
                    {{
                      lastResult?.status
                        ? t('home.faq.agent.preview.status', {
                            status: lastResult?.status,
                          })
                        : t('home.faq.agent.preview.statusPending')
                    }}
                  </p>
                </div>
              </div>

              <div class="home-faq__agent-preview-actions">
                <v-btn
                  :disabled="!lastResultLink"
                  color="primary"
                  variant="tonal"
                  :href="lastResultLink || undefined"
                  :aria-disabled="!lastResultLink"
                  target="_blank"
                >
                  {{ t('home.faq.agent.preview.cta') }}
                </v-btn>
                <p class="text-caption text-medium-emphasis">
                  {{ t('home.faq.agent.preview.helper') }}
                </p>
              </div>
            </div>
          </div>
        </v-card>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-section__container
  padding-inline: 0

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.5rem)

.home-section__header
  max-width: 760px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__panels
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  overflow: hidden
  border: 1px solid rgb(var(--v-theme-primary))

.home-faq__panel-title
  font-weight: 600
  font-size: 1.05rem

.home-faq__panel-text
  background: rgb(var(--v-theme-surface-default))

.home-faq__text-content
  padding-block: 0.5rem

.home-faq__agent-card
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  padding: clamp(1.25rem, 2.5vw, 2rem)
  border: 1px solid rgba(var(--v-theme-primary), 0.2)
  background: linear-gradient(145deg, rgba(var(--v-theme-surface-primary-080), 0.4), rgba(var(--v-theme-surface-default), 0.9))
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.05)
  display: flex
  flex-direction: column
  gap: clamp(1rem, 2vw, 1.5rem)

.home-faq__agent-header
  display: flex
  flex-direction: column
  gap: 0.35rem
  text-align: left

.home-faq__agent-eyebrow
  display: inline-flex
  align-items: center
  gap: 0.5rem
  padding: 0.35rem 0.75rem
  background: rgba(var(--v-theme-primary), 0.08)
  color: rgb(var(--v-theme-primary))
  border-radius: 999px
  font-weight: 600
  font-size: 0.9rem
  width: fit-content

.home-faq__agent-title
  font-size: clamp(1.2rem, 2.8vw, 1.5rem)
  margin: 0

.home-faq__agent-subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  max-width: 720px

.home-faq__agent-form
  width: 100%

.home-faq__agent-skeleton
  width: 100%

.home-faq__agent-preview
  display: grid
  gap: clamp(0.5rem, 1vw, 0.75rem)

.home-faq__agent-preview-header
  display: flex
  flex-direction: column
  gap: 0.25rem

.home-faq__agent-preview-eyebrow
  font-size: 0.85rem
  font-weight: 600
  color: rgb(var(--v-theme-primary))

.home-faq__agent-preview-title
  margin: 0
  font-weight: 600
  font-size: 1.05rem

.home-faq__agent-preview-subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-faq__agent-preview-body
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-faq__agent-preview-question
  display: flex
  gap: 0.75rem
  align-items: flex-start
  padding: 0.75rem 1rem
  border-radius: 0.75rem
  background: rgba(var(--v-theme-surface-primary-050), 0.7)

.home-faq__agent-preview-actions
  display: flex
  align-items: center
  gap: 0.75rem
  flex-wrap: wrap

@media (min-width: 960px)
  .home-faq__agent-preview-body
    flex-direction: row
    justify-content: space-between
    align-items: center

  .home-faq__agent-preview-actions
    justify-content: flex-end
</style>
