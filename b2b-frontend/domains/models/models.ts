export type TokenRateView = {
  input?: number
  output?: number
  currency?: string
  unit?: string
}

export type ModelBillingView = {
  payPerToken?: TokenRateView
  buyPerToken?: TokenRateView
}

export type ModelSmallView = {
  id: string
  hardwareTier: string
  modelSize?: string
}

export type ModelMediumView = ModelSmallView & {
  family: string
  contextSize?: number
  tags: string[]
}

export type ModelLargeView = ModelMediumView & {
  healthyAgents?: number
  degradedAgents?: number
  totalAgents?: number
  installedAgents?: number
   availabilityLevel?: string
  popularity?: number
  backend?: string
  description?: string
  descriptions?: Record<string, string>
  documentationUrl?: string
  urls: string[]
  license?: string
  icon?: string
  billing?: ModelBillingView
  runtimeProfile?: ModelRuntimeProfileView
  runtimePlacement?: ModelRuntimePlacementView
  inferaMetadata?: {
    summary?: string
    usages?: string[]
    detailedDescriptions?: Record<string, unknown>
    imageUrl?: string
    licenseUrl?: string
    maintainer?: string
    parameterCount?: string
    modalities?: string[]
    quantization?: string[]
    recommendedHardware?: string[]
    privacy?: string
    securityNotes?: string[]
    compatibility?: string[]
    raw?: InferaModelMetadataView
  }
  parameters: Record<string, unknown>
}

export type ModelRuntimeProfileView = {
  capabilities: string[]
  inputModalities: string[]
  outputModalities: string[]
  stopSequences: string[]
  templateFormat: string
  supportsImages: boolean
  supportsAudio: boolean
  supportsFunctionCalling: boolean
  externalProvider: boolean
  preferredEndpoint: string
}

export type ModelRuntimePlacementView = {
  slotKinds: string[]
  preferredSlotKind?: string
  cpuFallbackPolicy: 'not_applicable' | 'opt_in_degraded' | 'available'
}

export type InferaModelMetadataView = {
  schema?: string
  billing?: ModelBillingView
  catalog?: {
    tags?: string[]
    usages?: string[]
    downloadPriority?: number
  }
  model?: {
    family?: string
    parameterCount?: string
    architecture?: string
    modalities?: {
      input?: string[]
      output?: string[]
    }
    contextWindow?: number
  }
  source?: {
    provider?: SourcePartyView
    publisher?: SourcePartyView
    repository?: {
      originalUrl?: string
      artifactUrl?: string
      docUrl?: string
    }
  }
  quantization?: Record<string, unknown>
  hardware?: {
    minimal?: MinimalHardwareRequirementView
  }
  runtime?: {
    slotKinds?: string[]
    preferredSlotKind?: string
  }
  knowledge?: {
    releaseDate?: string | null
    trainingCutoff?: string | null
    trainingDataType?: string
    motivations?: string[]
    intendedUse?: string[]
    strengths?: string[]
    limitations?: string[]
    evaluation?: {
      status?: string
      notes?: string[]
    }
    facts?: Array<{ statement?: string, link?: string }>
  }
  i18n?: Record<string, unknown>
  reference?: {
    sources?: Array<{
      id?: string
      title?: string
      url?: string
      sourceType?: string
      usedFor?: string[]
    }>
  }
}

export type SourcePartyView = {
  name?: string
  type?: string
  website?: string | null
  logoUrl?: string | null
}

export type MinimalHardwareRequirementView = {
  schema?: string
  gpu: {
    required: boolean
    vramGb: number
    runtimes: string[]
  }
  cpu: {
    cores: number
    frequencyGhz: number
  }
  ramGb: number
  diskGb: number
}

const FALLBACK_MINIMAL_HARDWARE_REQUIREMENT: MinimalHardwareRequirementView = {
  gpu: { required: true, vramGb: 4, runtimes: ['nvidia', 'amd', 'intel', 'metal'] },
  cpu: { cores: 4, frequencyGhz: 2.2 },
  ramGb: 8,
  diskGb: 0.64
}

export function resolveModelDescription(model: ModelLargeView, locale?: string): string {
  const normalizedLocale = (locale || '').toLowerCase()
  const localized = model.descriptions ?? {}

  if (normalizedLocale && localized[normalizedLocale]) {
    return localized[normalizedLocale]
  }

  const localePrefix = normalizedLocale.split('-')[0]
  if (localePrefix && localized[localePrefix]) {
    return localized[localePrefix]
  }

  return localized.en || model.description || '-'
}

export function resolveMinimalHardwareRequirement(model?: ModelLargeView): MinimalHardwareRequirementView {
  const candidate = model?.inferaMetadata?.raw?.hardware?.minimal as Partial<MinimalHardwareRequirementView> | undefined
  if (!candidate) return FALLBACK_MINIMAL_HARDWARE_REQUIREMENT

  return {
    schema: candidate.schema,
    gpu: {
      required: candidate.gpu?.required ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.gpu.required,
      vramGb: candidate.gpu?.vramGb ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.gpu.vramGb,
      runtimes: candidate.gpu?.runtimes ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.gpu.runtimes
    },
    cpu: {
      cores: candidate.cpu?.cores ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.cpu.cores,
      frequencyGhz: candidate.cpu?.frequencyGhz ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.cpu.frequencyGhz
    },
    ramGb: candidate.ramGb ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.ramGb,
    diskGb: candidate.diskGb ?? FALLBACK_MINIMAL_HARDWARE_REQUIREMENT.diskGb
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function toModelLargeView(payload: Record<string, any>): ModelLargeView {
  return {
    id: payload.id ?? 'unknown-model',
    hardwareTier: payload.hardwareTier ?? 'unknown',
    modelSize: payload.modelSize,
    family: payload.family ?? 'unknown',
    contextSize: payload.contextSize,
    tags: payload.tags ?? [],
    backend: payload.backend,
    description: payload.description,
    descriptions: payload.descriptions ?? undefined,
    documentationUrl: payload.documentationUrl,
    urls: payload.urls ?? [],
    license: payload.license,
    icon: payload.icon,
    billing: payload.billing,
    runtimeProfile: toRuntimeProfileView(payload.runtimeProfile),
    runtimePlacement: toRuntimePlacementView(payload),
    inferaMetadata: payload.inferaMetadata ?? undefined,
    healthyAgents: typeof payload.healthyAgents === 'number' ? payload.healthyAgents : undefined,
    degradedAgents: typeof payload.degradedAgents === 'number' ? payload.degradedAgents : undefined,
    totalAgents: typeof payload.totalAgents === 'number' ? payload.totalAgents : undefined,
    installedAgents: typeof payload.installedAgents === 'number' ? payload.installedAgents : undefined,
    availabilityLevel: typeof payload.availabilityLevel === 'string' ? payload.availabilityLevel : undefined,
    popularity: typeof payload.popularity === 'number' ? payload.popularity : undefined,
    parameters: payload.parameters ?? {}
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function toRuntimePlacementView(payload: Record<string, any>): ModelRuntimePlacementView | undefined {
  const rawRuntime = payload.inferaMetadata?.raw?.runtime
  if (!rawRuntime || typeof rawRuntime !== 'object') return undefined
  const runtime = rawRuntime as Record<string, unknown>
  const slotKinds = stringArray(runtime.slotKinds)
    .map(value => value.toUpperCase())
    .filter((value): value is 'CPU' | 'GPU' => value === 'CPU' || value === 'GPU')
  if (!slotKinds.length) return undefined

  const preferred = typeof runtime.preferredSlotKind === 'string' ? runtime.preferredSlotKind.toUpperCase() : undefined
  const normalizedPreferred = preferred === 'CPU' || preferred === 'GPU' ? preferred : undefined
  const preferredSlotKind = normalizedPreferred && slotKinds.includes(normalizedPreferred) ? normalizedPreferred : undefined
  const dualGpuPreferred = slotKinds.includes('CPU') && slotKinds.includes('GPU') && preferredSlotKind === 'GPU'
  const cpuFallbackPolicy = dualGpuPreferred
    ? 'opt_in_degraded'
    : slotKinds.includes('CPU') && slotKinds.includes('GPU')
      ? 'available'
      : 'not_applicable'

  return {
    slotKinds,
    preferredSlotKind,
    cpuFallbackPolicy
  }
}

function toRuntimeProfileView(payload: unknown): ModelRuntimeProfileView | undefined {
  if (!payload || typeof payload !== 'object') return undefined
  const profile = payload as Record<string, unknown>
  return {
    capabilities: stringArray(profile.capabilities),
    inputModalities: stringArray(profile.inputModalities),
    outputModalities: stringArray(profile.outputModalities),
    stopSequences: stringArray(profile.stopSequences),
    templateFormat: typeof profile.templateFormat === 'string' ? profile.templateFormat : 'provider-native',
    supportsImages: profile.supportsImages === true,
    supportsAudio: profile.supportsAudio === true,
    supportsFunctionCalling: profile.supportsFunctionCalling === true,
    externalProvider: profile.externalProvider === true,
    preferredEndpoint: typeof profile.preferredEndpoint === 'string' ? profile.preferredEndpoint : 'unknown'
  }
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string' && item.length > 0) : []
}
