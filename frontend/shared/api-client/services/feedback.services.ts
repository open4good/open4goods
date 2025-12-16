import {
  CanVoteDomainLanguageEnum,
  FeedbackApi,
  FeedbackSubmissionRequestDtoTypeEnum,
  ListIssuesDomainLanguageEnum,
  ListIssuesTypeEnum,
  RemainingVotesDomainLanguageEnum,
  SubmitFeedbackDomainLanguageEnum,
  VoteDomainLanguageEnum,
} from '..'
import type {
  FeedbackSubmissionRequestDto,
  FeedbackVoteResponseDto,
  FeedbackIssueDto,
  FeedbackRemainingVotesDto,
  FeedbackSubmissionResponseDto,
  FeedbackVoteEligibilityDto,
} from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

type FeedbackType = FeedbackSubmissionRequestDtoTypeEnum

const DOMAIN_LANGUAGE_TO_CAN_VOTE_MAP: Record<
  DomainLanguage,
  CanVoteDomainLanguageEnum
> = {
  fr: CanVoteDomainLanguageEnum.Fr,
  en: CanVoteDomainLanguageEnum.En,
}

const DOMAIN_LANGUAGE_TO_LIST_MAP: Record<
  DomainLanguage,
  ListIssuesDomainLanguageEnum
> = {
  fr: ListIssuesDomainLanguageEnum.Fr,
  en: ListIssuesDomainLanguageEnum.En,
}

const DOMAIN_LANGUAGE_TO_REMAINING_MAP: Record<
  DomainLanguage,
  RemainingVotesDomainLanguageEnum
> = {
  fr: RemainingVotesDomainLanguageEnum.Fr,
  en: RemainingVotesDomainLanguageEnum.En,
}

const DOMAIN_LANGUAGE_TO_SUBMIT_MAP: Record<
  DomainLanguage,
  SubmitFeedbackDomainLanguageEnum
> = {
  fr: SubmitFeedbackDomainLanguageEnum.Fr,
  en: SubmitFeedbackDomainLanguageEnum.En,
}

const DOMAIN_LANGUAGE_TO_VOTE_MAP: Record<
  DomainLanguage,
  VoteDomainLanguageEnum
> = {
  fr: VoteDomainLanguageEnum.Fr,
  en: VoteDomainLanguageEnum.En,
}

export const FEEDBACK_TYPES: FeedbackType[] = [
  FeedbackSubmissionRequestDtoTypeEnum.Idea,
  FeedbackSubmissionRequestDtoTypeEnum.Bug,
]

/**
 * Service wrapper around the Feedback API. Only usable from the server runtime.
 */
export const useFeedbackService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: FeedbackApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useFeedbackService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new FeedbackApi(createBackendApiConfig())
    }

    return api
  }

  const listIssues = async (
    type?: ListIssuesTypeEnum
  ): Promise<FeedbackIssueDto[]> => {
    return resolveApi().listIssues({
      domainLanguage: DOMAIN_LANGUAGE_TO_LIST_MAP[domainLanguage],
      ...(type ? { type } : {}),
    })
  }

  const canVote = async (): Promise<FeedbackVoteEligibilityDto> => {
    return resolveApi().canVote({
      domainLanguage: DOMAIN_LANGUAGE_TO_CAN_VOTE_MAP[domainLanguage],
    })
  }

  const remainingVotes = async (): Promise<FeedbackRemainingVotesDto> => {
    return resolveApi().remainingVotes({
      domainLanguage: DOMAIN_LANGUAGE_TO_REMAINING_MAP[domainLanguage],
    })
  }

  const submitFeedback = async (
    payload: FeedbackSubmissionRequestDto
  ): Promise<FeedbackSubmissionResponseDto> => {
    return resolveApi().submitFeedback({
      domainLanguage: DOMAIN_LANGUAGE_TO_SUBMIT_MAP[domainLanguage],
      feedbackSubmissionRequestDto: payload,
    })
  }

  const voteOnIssue = async (
    issueId: string
  ): Promise<FeedbackVoteResponseDto> => {
    return resolveApi().vote({
      issueId,
      domainLanguage: DOMAIN_LANGUAGE_TO_VOTE_MAP[domainLanguage],
    })
  }

  return {
    listIssues,
    canVote,
    remainingVotes,
    submitFeedback,
    voteOnIssue,
  }
}

export type { FeedbackType }
