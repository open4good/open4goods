import { useState } from '#app'

export type TranslationCopyStatus = 'idle' | 'copied' | 'error'

interface InContextEditorState {
  isOpen: boolean
  key: string
  locale?: string
  translation: string
  githubUrl: string
  element: HTMLElement | null
  copyStatus: TranslationCopyStatus
}

const DEFAULT_STATE: InContextEditorState = {
  isOpen: false,
  key: '',
  locale: undefined,
  translation: '',
  githubUrl: '',
  element: null,
  copyStatus: 'idle',
}

interface OpenPayload {
  key: string
  locale?: string
  translation: string
  githubUrl: string
  element: HTMLElement | null
}

export function useI18nInContextEditorState() {
  const state = useState<InContextEditorState>('i18n-inctx-editor', () => ({ ...DEFAULT_STATE }))

  function open(payload: OpenPayload) {
    state.value = {
      ...DEFAULT_STATE,
      ...payload,
      isOpen: true,
      copyStatus: 'idle',
    }
  }

  function close() {
    state.value = {
      ...state.value,
      isOpen: false,
      element: null,
      copyStatus: 'idle',
    }
  }

  function reset() {
    state.value = { ...DEFAULT_STATE }
  }

  function setCopyStatus(status: TranslationCopyStatus) {
    state.value.copyStatus = status
  }

  return {
    state,
    open,
    close,
    reset,
    setCopyStatus,
  }
}
