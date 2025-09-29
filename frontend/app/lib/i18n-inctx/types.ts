export interface InContextEditDetail {
  /** Translation key selected for contextual editing. */
  key: string
  /**
   * Element that triggered the edit request. Consumers may use it to provide
   * additional UX feedback (focus, scroll into view, etc.).
   */
  element: HTMLElement
  /** Active locale associated with the key. */
  locale?: string
}

export type EditorCleanup = (() => void | Promise<void>) | undefined
