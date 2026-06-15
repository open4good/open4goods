export const infraComponentRegistry = {
  text: 'v-text-field',
  number: 'v-text-field',
  switch: 'v-switch',
  json: 'v-textarea'
} as const

export type InfraFieldComponent = keyof typeof infraComponentRegistry
