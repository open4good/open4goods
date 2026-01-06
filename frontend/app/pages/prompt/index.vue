<template>
  <div>
    <PageHeader
      :title="$t('agents.page.title')"
      :subtitle="$t('agents.page.subtitle')"
      layout="single-column"
      container="lg"
      background="image"
      background-image-asset-key="promptBackground"
    >
      <template #default>
        <!-- Phase 1: Selection -->
        <v-fade-transition mode="out-in">
          <div v-if="!selectedTemplate && !submissionResult" key="selection">
            <div class="d-flex justify-space-between align-center mb-6">
              <h2 class="text-h5">{{ $t('agents.page.availableAgents') }}</h2>
              <v-chip color="secondary" variant="outlined" size="small">
                {{ accessibleTemplates.length }} / {{ templates.length }}
                {{ $t('agents.page.active') }}
              </v-chip>
            </div>

            <v-alert
              v-if="accessWarning"
              type="warning"
              variant="tonal"
              class="mb-4"
              closable
              @click:close="accessWarning = null"
            >
              {{ accessWarning }}
            </v-alert>

            <v-skeleton-loader
              v-if="loadingTemplates"
              type="card, card, card"
              class="d-flex gap-4"
            ></v-skeleton-loader>

            <AgentTemplateSelector
              v-else
              :templates="templates"
              @select="onSelectTemplate"
              @blocked="onBlockedTemplate"
            />
          </div>

          <!-- Phase 2: Input -->
          <div v-else-if="selectedTemplate && !submissionResult" key="input">
            <AgentPromptInput
              :template-name="selectedTemplate.name"
              :prompt-templates="selectedTemplate.promptTemplates || []"
              :selected-prompt-template-id="selectedPromptTemplateId"
              :allow-template-editing="selectedTemplate.allowTemplateEditing"
              :attributes="selectedTemplate.attributes"
              :can-toggle-visibility="!selectedTemplate.publicPromptHistory"
              :default-public="selectedTemplate.publicPromptHistory"
              :fallback-mailto="mailtoLink"
              :loading="submitting"
              :tags="selectedTemplate.tags"
              :allowed-roles="selectedTemplate.allowedRoles"
              :is-authorized="selectedTemplate.isAuthorized"
              @submit="onSubmit"
              @cancel="onCancel"
              @fallback-contact="onFallbackContact"
            />
          </div>

          <!-- Phase 3: Success -->
          <v-sheet
            v-else-if="submissionResult"
            key="success"
            class="pa-8 d-flex flex-column align-center text-center rounded-xl bg-surface-variant"
          >
            <v-icon
              icon="mdi-check-circle"
              color="success"
              size="80"
              class="mb-6"
            ></v-icon>
            <h2 class="text-h4 mb-2">{{ $t('agents.submission.success') }}</h2>
            <p class="text-body-1 mb-6 max-w-sm mx-auto">
              {{
                $t('agents.submission.message', {
                  id: submissionResult.issueNumber,
                })
              }}
            </p>
            <div class="d-flex gap-4">
              <v-btn
                :href="submissionResult.issueUrl"
                target="_blank"
                color="primary"
                size="large"
                prepend-icon="mdi-github"
                variant="flat"
              >
                {{ $t('agents.submission.viewOnGithub') }}
              </v-btn>
              <v-btn variant="outlined" size="large" @click="reset">{{
                $t('agents.submission.submitAnother')
              }}</v-btn>
            </div>
          </v-sheet>
        </v-fade-transition>
      </template>
    </PageHeader>

    <v-container class="py-12">
      <v-divider class="my-12"></v-divider>

      <!-- Activity Stream -->
      <div v-if="hasAdminAccess(roles, { allowedRoles: [] })" class="mb-12">
        <h2 class="text-h4 mb-6">{{ $t('agents.activity.title') }}</h2>
        <v-card variant="flat" border>
          <v-list v-if="activity.length > 0" lines="two">
            <template v-for="(item, i) in activity" :key="item.id">
              <v-list-item
                :href="item.issueUrl"
                target="_blank"
                :lines="undefined"
              >
                <template #prepend>
                  <v-avatar color="surface-variant" size="40">
                    <v-icon icon="mdi-github" size="24"></v-icon>
                  </v-avatar>
                </template>

                <v-list-item-title class="font-weight-medium">
                  <span class="text-primary mr-2">
                    #{{ getIssueNumber(item.issueUrl) }}
                  </span>
                  {{ item.promptSummary || $t('agents.activity.hidden') }}
                </v-list-item-title>

                <v-list-item-subtitle class="mt-1 d-flex align-center gap-2">
                  <v-chip
                    size="x-small"
                    :color="getStatusColor(item.status)"
                    label
                  >
                    {{ item.status }}
                  </v-chip>
                  <v-chip size="x-small" variant="outlined">{{
                    item.type
                  }}</v-chip>
                  <v-chip
                    v-if="item.commentsCount"
                    size="x-small"
                    color="info"
                    variant="tonal"
                  >
                    <v-icon
                      icon="mdi-comment-text-outline"
                      size="x-small"
                      class="mr-1"
                    />
                    {{ item.commentsCount }}
                  </v-chip>
                  <span
                    v-if="item.promptVisibility === 'PRIVATE'"
                    class="text-caption text-medium-emphasis"
                  >
                    <v-icon
                      icon="mdi-lock"
                      size="x-small"
                      class="mr-1"
                    ></v-icon>
                    {{ $t('agents.activity.private') }}
                  </span>
                </v-list-item-subtitle>

                <template #append>
                  <div class="d-flex align-center">
                    <v-btn
                      icon="mdi-forum"
                      variant="text"
                      size="small"
                      :title="$t('agents.activity.viewDiscussion')"
                      @click.stop="openDiscussion(item)"
                    />
                    <v-icon
                      icon="mdi-open-in-new"
                      size="small"
                      color="medium-emphasis"
                    ></v-icon>
                  </div>
                </template>
              </v-list-item>
              <v-divider
                v-if="i < activity.length - 1"
                class="mt-2"
              ></v-divider>
            </template>
          </v-list>

          <div v-else class="pa-8 text-center text-medium-emphasis">
            <v-icon icon="mdi-history" size="large" class="mb-2"></v-icon>
            <div>{{ $t('agents.activity.empty') }}</div>
          </div>
        </v-card>

        <v-card v-if="currentDiscussion" class="mt-6" variant="text" border>
          <v-card-title class="d-flex align-center justify-space-between">
            <div>
              <div class="text-caption text-medium-emphasis">
                {{ $t('agents.discussion.title') }}
              </div>
              <div class="text-body-2 font-weight-medium">
                #{{ currentDiscussion.number }} ·
                {{ currentDiscussion.title }}
              </div>
            </div>
            <v-btn
              icon="mdi-close"
              variant="text"
              size="small"
              @click="currentDiscussion = null"
            />
          </v-card-title>
          <v-progress-linear
            v-if="loadingDiscussion"
            color="primary"
            indeterminate
            height="3"
          />
          <v-divider></v-divider>
          <v-list lines="three">
            <v-list-item
              v-for="comment in currentDiscussion.comments"
              :key="comment.id"
              :title="comment.author"
            >
              <template #subtitle>
                <div
                  class="d-flex align-center text-caption text-medium-emphasis mb-1"
                >
                  <v-icon icon="mdi-calendar" size="x-small" class="mr-1" />
                  {{ formatDate(comment.createdAt || comment.updatedAt) }}
                </div>
                <div class="text-body-2" style="white-space: pre-line">
                  {{ comment.body }}
                </div>
              </template>
            </v-list-item>
            <v-list-item v-if="currentDiscussion.comments.length === 0">
              <v-list-item-title class="text-medium-emphasis">
                {{ $t('agents.discussion.empty') }}
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card>
      </div>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAgent } from '@/composables/useAgent'
import type {
  AgentTemplateDto,
  AgentRequestResponseDto,
  AgentActivityDto,
  AgentRequestDto,
  AgentRequestDtoTypeEnum,
  AgentRequestDtoPromptVisibilityEnum,
  AgentIssueDto,
} from '~~/shared/api-client/services/agents.services'
import type { DomainLanguage } from '~~/shared/utils/domain-language'
import PageHeader from '~/components/shared/header/PageHeader.vue'
import AgentTemplateSelector from '@/components/agent/AgentTemplateSelector.vue'
import AgentPromptInput from '@/components/agent/AgentPromptInput.vue'
import { useUserRoles } from '@/composables/auth/useUserRoles'
import { hasAdminAccess } from '~~/shared/utils/_roles'

const { t, locale } = useI18n()
const route = useRoute()
useHead({
  title: t('agents.meta.title'),
  meta: [{ name: 'description', content: t('agents.meta.description') }],
})
const { listTemplates, submitRequest, listActivity, getMailto, getIssue } =
  useAgent()
const { roles } = useUserRoles()

type AgentTemplateWithAccess = AgentTemplateDto & { isAuthorized: boolean }

// Reactive state
const templates = ref<AgentTemplateWithAccess[]>([])
const accessibleTemplates = computed(() =>
  templates.value.filter(template => template.isAuthorized)
)
const activity = ref<AgentActivityDto[]>([])
const loadingTemplates = ref(true)
const selectedTemplate = ref<AgentTemplateWithAccess | null>(null)
const selectedPromptTemplateId = ref<string>('')
const mailtoLink = ref<string | null>(null)
const submitting = ref(false)
const submissionResult = ref<AgentRequestResponseDto | null>(null)
const currentDiscussion = ref<AgentIssueDto | null>(null)
const loadingDiscussion = ref(false)
const accessWarning = ref<string | null>(null)

// Computed locale for API
const currentLang = locale.value.split('-')[0] as DomainLanguage

// Helpers
const getIssueNumber = (url?: string) => {
  if (!url) return ''
  const parts = url.split('/')
  return parts[parts.length - 1]
}

const getStatusColor = (status: string) => {
  switch (status.toLowerCase()) {
    case 'open':
      return 'success'
    case 'closed':
      return 'grey'
    case 'issue_created':
      return 'info'
    default:
      return 'default'
  }
}

const mapTemplatesWithAccess = (
  tpls: AgentTemplateDto[]
): AgentTemplateWithAccess[] =>
  tpls.map(template => ({
    ...template,
    isAuthorized:
      !template.allowedRoles?.length ||
      hasAdminAccess(roles.value, { allowedRoles: template.allowedRoles }),
  }))

// Data loading
// Data loading
const {
  data: initialData,
  error: loadError,
  status,
} = await useAsyncData('agents-data', async () => {
  const [tpls, acts] = await Promise.all([
    listTemplates(currentLang),
    listActivity(currentLang),
  ])
  return { tpls, acts }
})

if (initialData.value) {
  templates.value = mapTemplatesWithAccess(initialData.value.tpls)
  activity.value = initialData.value.acts
}

watch(
  initialData,
  newData => {
    if (newData) {
      templates.value = mapTemplatesWithAccess(newData.tpls)
      activity.value = newData.acts
    }
  },
  { immediate: true }
)

watch(
  status,
  newStatus => {
    loadingTemplates.value = newStatus === 'pending'
  },
  { immediate: true }
)

watch(loadError, error => {
  if (error) {
    console.error('Failed to load agents', error)
  }
})

watch(
  roles,
  () => {
    templates.value = mapTemplatesWithAccess(templates.value)
    if (selectedTemplate.value && !selectedTemplate.value.isAuthorized) {
      selectedTemplate.value = null
      selectedPromptTemplateId.value = ''
    }
  },
  { immediate: false }
)

function onBlockedTemplate(template: AgentTemplateWithAccess) {
  const roleSummary = template.allowedRoles?.length
    ? template.allowedRoles.join(', ')
    : t('agents.selector.noRoles')
  accessWarning.value = t('agents.selector.notAuthorized', {
    roles: roleSummary,
  })
}

async function onSelectTemplate(template: AgentTemplateWithAccess) {
  if (!template.isAuthorized) {
    onBlockedTemplate(template)
    return
  }
  accessWarning.value = null
  selectedTemplate.value = template
  selectedPromptTemplateId.value = template.promptTemplates?.[0]?.id ?? ''
  mailtoLink.value = null
  if (template.mailTemplate) {
    try {
      mailtoLink.value = await getMailto(template.id, currentLang)
    } catch (e) {
      console.warn('Failed to get mailto', e)
    }
  }
}

async function onSubmit({
  prompt,
  promptVariantId,
  isPrivate,
  attributeValues,
  captchaToken,
}: {
  prompt: string
  promptVariantId: string
  isPrivate: boolean
  attributeValues: Record<string, unknown>
  captchaToken?: string
}) {
  if (!selectedTemplate.value) return
  submitting.value = true
  try {
    const request: AgentRequestDto = {
      type: 'FEATURE' as unknown as AgentRequestDtoTypeEnum,
      promptUser: prompt,
      promptTemplateId: selectedTemplate.value.id,
      promptVariantId,
      promptVisibility: (isPrivate
        ? 'PRIVATE'
        : 'PUBLIC') as unknown as AgentRequestDtoPromptVisibilityEnum,
      attributeValues,
      captchaToken,
      tags: selectedTemplate.value.tags,
    }

    submissionResult.value = await submitRequest(request, currentLang)

    // Refresh activity after short delay to allow GitHub indexing/propagation if needed (optional)
    setTimeout(
      () => listActivity(currentLang).then(acts => (activity.value = acts)),
      1000
    )
  } catch (e) {
    console.error('Submission failed', e)
    alert(t('agents.submission.error'))
  } finally {
    submitting.value = false
  }
}

async function onFallbackContact({
  prompt,
  attributeValues,
  captchaToken,
}: {
  prompt: string
  attributeValues: Record<string, unknown>
  captchaToken?: string
}) {
  if (!selectedTemplate.value || !mailtoLink.value) return
  if (!captchaToken) {
    alert(t('agents.promptInput.required'))
    return
  }
  submitting.value = true
  try {
    const nameFromAttributes = String(attributeValues.name ?? '').trim()
    const emailFromAttributes = String(attributeValues.email ?? '').trim()
    const contactName =
      nameFromAttributes || t('agents.promptInput.anonymousName')
    const contactEmail = emailFromAttributes || 'contact@nudger.fr'
    const attributeSummary =
      Object.keys(attributeValues).length > 0
        ? JSON.stringify(attributeValues, null, 2)
        : t('agents.promptInput.noAttributes')

    await $fetch('/api/contact', {
      method: 'POST',
      body: {
        name: contactName,
        email: contactEmail,
        message: `${selectedTemplate.value.name}\n\n${prompt}\n\n${attributeSummary}`,
        hCaptchaResponse: captchaToken,
        templateId: selectedTemplate.value.id,
        sourceRoute: route.path,
        sourceComponent: 'AgentPromptInput',
        sourcePage: 'Agents Prompt',
      },
    })
    alert(t('agents.submission.success'))
  } catch (error) {
    console.error('Fallback contact failed', error)
    alert(t('common.error'))
  } finally {
    submitting.value = false
  }
}

function reset() {
  selectedTemplate.value = null
  selectedPromptTemplateId.value = ''
  submissionResult.value = null
}

function onCancel() {
  selectedTemplate.value = null
  selectedPromptTemplateId.value = ''
}

async function openDiscussion(item: AgentActivityDto) {
  if (!item || !item.id) return
  loadingDiscussion.value = true
  try {
    const discussion = await getIssue(item.id, currentLang)
    currentDiscussion.value = {
      ...discussion,
      comments: discussion.comments ?? [],
    }
  } catch (e) {
    console.warn('Failed to load discussion', e)
  } finally {
    loadingDiscussion.value = false
  }
}

const formatDate = (value?: string) => {
  if (!value) return ''
  const date = new Date(value)
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
</script>

<style scoped>
.gradient-text {
  background: linear-gradient(
    45deg,
    rgb(var(--v-theme-primary)),
    rgb(var(--v-theme-secondary))
  );
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.gap-4 {
  gap: 1.5rem;
}
</style>
