<template>
  <section :aria-labelledby="headingId" class="feedback-issue-list">
    <header class="feedback-issue-list__header">
      <h3 :id="headingId" class="feedback-issue-list__title">{{ title }}</h3>
      <p class="feedback-issue-list__description">
        {{ description }}
      </p>
    </header>

    <v-alert
      v-if="statusMessage"
      type="info"
      variant="tonal"
      border="start"
      class="mb-4"
      role="status"
    >
      {{ statusMessage }}
    </v-alert>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      border="start"
      class="mb-4"
      role="alert"
    >
      {{ errorMessage }}
    </v-alert>

    <div
      class="feedback-issue-list__content"
      :aria-busy="loading && !errorMessage"
      aria-live="polite"
    >
      <div
        v-if="loading && !errorMessage"
        class="feedback-issue-list__loading"
        role="status"
      >
        <v-skeleton-loader
          type="heading, text@2, list-item-three-line@3"
          class="feedback-issue-list__loading-skeleton"
        />
      </div>

      <div
        v-else-if="issues.length === 0"
        class="feedback-issue-list__empty"
        role="status"
      >
        <v-icon
          icon="mdi-emoticon-thought"
          size="36"
          color="primary"
          class="mb-2"
        />
        <p class="mb-0">{{ emptyStateLabel }}</p>
      </div>

      <v-list
        v-else
        class="feedback-issue-list__items"
        bg-color="transparent"
        role="list"
        density="comfortable"
      >
        <v-list-item
          v-for="issue in issues"
          :key="issueKey(issue)"
          class="feedback-issue-list__item"
          :aria-label="issue.title"
        >
          <template #prepend>
            <v-avatar
              size="40"
              class="feedback-issue-list__issue-avatar"
              color="surface-primary-120"
            >
              <v-icon :icon="issueIcon" size="24" color="primary" />
            </v-avatar>
          </template>

          <template #title>
            <div class="feedback-issue-list__issue-header">
              <span class="feedback-issue-list__issue-number"
                >#{{ issue.number ?? '—' }}</span
              >
              <p class="feedback-issue-list__issue-title">{{ issue.title }}</p>
            </div>
          </template>

          <template #subtitle>
            <div class="feedback-issue-list__issue-meta">
              <span class="feedback-issue-list__issue-votes">
                <v-icon icon="mdi-thumb-up-outline" size="18" class="me-1" />
                {{ issue.votes ?? 0 }}
              </span>
              <v-btn
                v-if="issue.url"
                :href="issue.url"
                target="_blank"
                rel="noopener nofollow"
                variant="text"
                size="small"
                :aria-label="`${openIssueLabel} ${issue.number ?? ''}`"
                append-icon="mdi-open-in-new"
              >
                {{ openIssueLabel }}
              </v-btn>
            </div>
          </template>

          <template #append>
            <v-tooltip v-if="voteDisabledMessage" location="top">
              <template #activator="{ props: tooltipProps }">
                <span v-bind="tooltipProps">
                  <v-btn
                    color="primary"
                    variant="flat"
                    size="small"
                    :aria-label="`${voteButtonAriaLabel} ${issue.title}`"
                    :disabled="
                      isVoteDisabled || !issue.id || hasIssueBeenVoted(issue.id)
                    "
                    :loading="votePendingId === issue.id"
                    @click="handleVote(issue.id)"
                  >
                    {{
                      hasIssueBeenVoted(issue.id)
                        ? voteCompletedLabel
                        : voteButtonLabel
                    }}
                  </v-btn>
                </span>
              </template>
              <span>{{ voteDisabledMessage }}</span>
            </v-tooltip>
            <v-btn
              v-else
              color="primary"
              variant="flat"
              size="small"
              :aria-label="`${voteButtonAriaLabel} ${issue.title}`"
              :disabled="
                isVoteDisabled || !issue.id || hasIssueBeenVoted(issue.id)
              "
              :loading="votePendingId === issue.id"
              @click="handleVote(issue.id)"
            >
              {{
                hasIssueBeenVoted(issue.id)
                  ? voteCompletedLabel
                  : voteButtonLabel
              }}
            </v-btn>
          </template>
        </v-list-item>
      </v-list>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface FeedbackIssueDisplay {
  id?: string
  number?: number
  title?: string
  url?: string
  votes?: number
}

const emit = defineEmits<{
  (event: 'vote', issueId: string | undefined): void
}>()

type IssueIcon = string

const props = defineProps<{
  headingId: string
  title: string
  description: string
  issues: FeedbackIssueDisplay[]
  loading: boolean
  errorMessage: string | null
  emptyStateLabel: string
  voteButtonLabel: string
  voteButtonAriaLabel: string
  openIssueLabel: string
  statusMessage?: string | null
  remainingVotes: number | null
  canVote: boolean
  votePendingId?: string | null
  voteDisabledWhenNoVotesMessage: string
  voteDisabledWhenBlockedMessage: string
  issueIcon: IssueIcon
  voteCompletedLabel: string
  votedIssueIds: readonly string[]
}>()

const voteDisabledMessage = computed(() => {
  if (!props.canVote) {
    return props.voteDisabledWhenBlockedMessage
  }

  if (props.remainingVotes !== null && props.remainingVotes <= 0) {
    return props.voteDisabledWhenNoVotesMessage
  }

  return null
})

const isVoteDisabled = computed(
  () =>
    !props.canVote ||
    (props.remainingVotes !== null && props.remainingVotes <= 0)
)

const votedIssueIdsSet = computed(() => new Set(props.votedIssueIds))

const hasIssueBeenVoted = (issueId: string | undefined): boolean => {
  if (!issueId) {
    return false
  }

  return votedIssueIdsSet.value.has(issueId)
}

const issueKey = (issue: FeedbackIssueDisplay) => {
  if (issue.id) {
    return issue.id
  }

  if (typeof issue.number === 'number') {
    return `number-${issue.number}`
  }

  if (issue.title) {
    return `title-${issue.title}`
  }

  return JSON.stringify(issue)
}

const handleVote = (issueId: string | undefined) => {
  if (!issueId || isVoteDisabled.value || hasIssueBeenVoted(issueId)) {
    return
  }

  emit('vote', issueId)
}
</script>

<style scoped lang="scss">
.feedback-issue-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;

  &__header {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__description {
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2rem;
    border-radius: 1rem;
    border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.4);
    background-color: rgba(var(--v-theme-surface-glass), 0.6);
    text-align: center;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__content {
    position: relative;
    min-height: 12rem;
  }

  &__loading {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  &__loading-skeleton {
    border-radius: 1.5rem;
  }

  &__items {
    border-radius: 1.5rem;
    background-color: rgba(var(--v-theme-surface-glass-strong), 0.95);
    box-shadow: 0 18px 45px rgba(var(--v-theme-shadow-primary-600), 0.14);
  }

  &__item {
    border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.15);
    padding-block: 1.25rem;
    align-items: flex-start;

    :deep(.v-list-item__content) {
      min-width: 0;
    }

    :deep(.v-list-item__append) {
      align-self: flex-start;
      margin-left: 1rem;
      margin-top: 0.35rem;
    }

    &:last-of-type {
      border-bottom: none;
    }
  }

  &__issue-avatar {
    backdrop-filter: blur(8px);
    box-shadow: 0 12px 26px rgba(var(--v-theme-shadow-primary-600), 0.12);
  }

  &__issue-header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    column-gap: 0.75rem;
    row-gap: 0.35rem;
  }

  &__issue-number {
    font-weight: 600;
    color: rgb(var(--v-theme-primary));
    flex: 0 0 auto;
  }

  &__issue-title {
    font-weight: 600;
    font-size: 1.1rem;
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-strong));
    white-space: normal;
    word-break: break-word;
    flex: 1 1 100%;
  }

  &__issue-meta {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.75rem;
    margin-top: 0.35rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__issue-votes {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-weight: 600;
  }
}
</style>
