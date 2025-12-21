<template>
  <v-container class="py-12">
    <div class="text-center mb-12">
      <h1 class="text-h3 font-weight-bold mb-4 gradient-text">
        AI Agent Workspace
      </h1>
      <p class="text-subtitle-1 text-medium-emphasis">
        Use our specialized agents to submit ideas, report bugs, or ask
        questions.
      </p>
    </div>

    <!-- Phase 1: Selection -->
    <v-fade-transition mode="out-in">
      <div v-if="!selectedTemplate && !submissionResult" key="selection">
        <div class="d-flex justify-space-between align-center mb-6">
          <h2 class="text-h5">Available Agents</h2>
          <v-chip color="secondary" variant="outlined" size="small"
            >{{ templates.length }} Active</v-chip
          >
        </div>

        <v-skeleton-loader
          v-if="loadingTemplates"
          type="card, card, card"
          class="d-flex gap-4"
        ></v-skeleton-loader>

        <AgentTemplateSelector
          v-else
          :templates="templates"
          @select="onSelectTemplate"
        />
      </div>

      <!-- Phase 2: Input -->
      <div v-else-if="selectedTemplate && !submissionResult" key="input">
        <AgentPromptInput
          :template-name="selectedTemplate.name"
          :can-toggle-visibility="!selectedTemplate.publicPromptHistory"
          :default-public="selectedTemplate.publicPromptHistory"
          :fallback-mailto="mailtoLink"
          :loading="submitting"
          @submit="onSubmit"
          @cancel="selectedTemplate = null"
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
        <h2 class="text-h4 mb-2">Request Submitted!</h2>
        <p class="text-body-1 mb-6 max-w-sm mx-auto">
          Your request has been successfully processed by the agent. Issue
          <strong>#{{ submissionResult.issueNumber }}</strong> has been created
          on GitHub.
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
            View on GitHub
          </v-btn>
          <v-btn variant="outlined" size="large" @click="reset"
            >New Request</v-btn
          >
        </div>
      </v-sheet>
    </v-fade-transition>

    <v-divider class="my-12"></v-divider>

    <!-- Activity Stream -->
    <div class="mt-8">
      <h2 class="text-h5 mb-4">Community Activity</h2>
      <v-card variant="flat" border>
        <v-list lines="two" v-if="activity.length > 0">
          <v-list-item
            v-for="(item, i) in activity"
            :key="item.issueId"
            :href="item.url"
            target="_blank"
            :lines="undefined"
          >
            <template v-slot:prepend>
              <v-avatar color="surface-variant" size="40">
                <v-icon icon="mdi-github" size="24"></v-icon>
              </v-avatar>
            </template>

            <v-list-item-title class="font-weight-medium">
              <span class="text-primary mr-2">
                #{{ getIssueNumber(item.url) }}
              </span>
              {{ item.summary || 'Private Request content hidden' }}
            </v-list-item-title>

            <v-list-item-subtitle class="mt-1 d-flex align-center gap-2">
              <v-chip
                size="x-small"
                :color="getStatusColor(item.status)"
                label
                >{{ item.status }}</v-chip
              >
              <v-chip size="x-small" variant="outlined">{{ item.type }}</v-chip>
              <span
                v-if="item.promptVisibility === 'PRIVATE'"
                class="text-caption text-medium-emphasis"
              >
                <v-icon icon="mdi-lock" size="x-small" class="mr-1"></v-icon>
                Private
              </span>
            </v-list-item-subtitle>

            <template v-slot:append>
              <v-icon
                icon="mdi-open-in-new"
                size="small"
                color="medium-emphasis"
              ></v-icon>
            </template>
            <v-divider v-if="i < activity.length - 1" class="mt-2"></v-divider>
          </v-list-item>
        </v-list>

        <div v-else class="pa-8 text-center text-medium-emphasis">
          <v-icon icon="mdi-history" size="large" class="mb-2"></v-icon>
          <div>No recent activity found. Be the first to submit!</div>
        </div>
      </v-card>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAgent } from '@/composables/useAgent'
import type {
  AgentTemplateDto,
  AgentRequestResponseDto,
  AgentActivityDto,
  DomainLanguage,
} from '@/types/agent'
import AgentTemplateSelector from '@/components/agent/AgentTemplateSelector.vue'
import AgentPromptInput from '@/components/agent/AgentPromptInput.vue'

const { t, locale } = useI18n()
const { listTemplates, submitRequest, listActivity, getMailto } = useAgent()

// Reactive state
const templates = ref<AgentTemplateDto[]>([])
const activity = ref<AgentActivityDto[]>([])
const loadingTemplates = ref(true)
const selectedTemplate = ref<AgentTemplateDto | null>(null)
const mailtoLink = ref<string | null>(null)
const submitting = ref(false)
const submissionResult = ref<AgentRequestResponseDto | null>(null)

// Computed locale for API
const currentLang = locale.value.split('-')[0] as DomainLanguage

// Helpers
const getIssueNumber = (url: string) => {
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

// Data loading
async function loadData() {
  try {
    const [tpls, acts] = await Promise.all([
      listTemplates(currentLang),
      listActivity(currentLang),
    ])
    templates.value = tpls
    activity.value = acts
  } catch (e) {
    console.error('Failed to load agents', e)
    // In dev mode, mock data if API fails?
    // templates.value = [ { id: 'mock', name: 'Mock Agent', description: 'API failed', tags: ['bug'], publicPromptHistory: true, allowedRoles: [], icon: '', promptTemplate: '' } ]
  } finally {
    loadingTemplates.value = false
  }
}

onMounted(loadData)

async function onSelectTemplate(template: AgentTemplateDto) {
  selectedTemplate.value = template
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
  isPrivate,
}: {
  prompt: string
  isPrivate: boolean
}) {
  if (!selectedTemplate.value) return
  submitting.value = true
  try {
    submissionResult.value = await submitRequest(
      {
        type: 'FEATURE',
        promptUser: prompt,
        promptTemplateId: selectedTemplate.value.id,
        promptVisibility: isPrivate ? 'PRIVATE' : 'PUBLIC',
      },
      currentLang
    )

    // Refresh activity after short delay to allow GitHub indexing/propagation if needed (optional)
    setTimeout(
      () => listActivity(currentLang).then(acts => (activity.value = acts)),
      1000
    )
  } catch (e) {
    console.error('Submission failed', e)
    alert(
      'Failed to submit request. Please try again or use the email fallback.'
    )
  } finally {
    submitting.value = false
  }
}

function reset() {
  selectedTemplate.value = null
  submissionResult.value = null
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
