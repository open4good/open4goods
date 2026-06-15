export {}

declare global {
  interface Window {
    plausible?: (event: string, options?: Record<string, unknown>) => void
  }
}
