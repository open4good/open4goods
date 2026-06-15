import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import openapiTS, { astToString } from 'openapi-typescript'

type UiField = {
  key: string
  label: string
  component: 'text' | 'number' | 'switch' | 'json'
  required: boolean
}

type UiSchemaEntity = {
  entity: string
  endpoint: string
  method: string
  fields: UiField[]
}

const defaultUrl = 'http://localhost:8087/v3/api-docs'
const source = process.env.BACKEND_OPENAPI_URL || defaultUrl
const typesOutput = resolve(process.cwd(), 'types/backend-openapi.d.ts')
const uiSchemaOutput = resolve(process.cwd(), 'ui-schema/health.gen.ts')
const domainOutput = resolve(process.cwd(), 'domains/health/health.gen.ts')
const zodOutput = resolve(process.cwd(), 'domains/health/health.zod.gen.ts')

const args = new Set(process.argv.slice(2))
const shouldGenerateTypes = !args.size || args.has('--types-only') || (!args.has('--ui-schema-only') && !args.has('--domain-only'))
const shouldGenerateUiSchema = !args.size || args.has('--ui-schema-only') || (!args.has('--types-only') && !args.has('--domain-only'))
const shouldGenerateDomain = !args.size || args.has('--domain-only') || (!args.has('--types-only') && !args.has('--ui-schema-only'))

function createHealthUiSchema(): UiSchemaEntity {
  return {
    entity: 'health',
    endpoint: '/actuator/health',
    method: 'get',
    fields: [
      { key: 'status', label: 'health.status', component: 'text', required: true },
      { key: 'components', label: 'health.components', component: 'json', required: false }
    ]
  }
}

function renderUiSchemaModule(entity: UiSchemaEntity) {
  return `// AUTO-GENERATED FILE - DO NOT EDIT\n\nexport type UiField = {\n  key: string\n  label: string\n  component: 'text' | 'number' | 'switch' | 'json'\n  required: boolean\n}\n\nexport type UiSchemaEntity = {\n  entity: string\n  endpoint: string\n  method: string\n  fields: UiField[]\n}\n\nexport const ${entity.entity}UiSchema: UiSchemaEntity = ${JSON.stringify(entity, null, 2)}\n`
}

function renderDomainModule(entity: UiSchemaEntity) {
  return `// AUTO-GENERATED FILE - DO NOT EDIT\n\nexport type ${capitalize(entity.entity)}Entity = {\n  status: string\n  components?: Record<string, unknown>\n}\n\nexport function map${capitalize(entity.entity)}Response(payload: unknown): ${capitalize(entity.entity)}Entity {\n  const status = typeof (payload as { status?: unknown })?.status === 'string' ? (payload as { status: string }).status : 'UNKNOWN'\n  const components = typeof (payload as { components?: unknown })?.components === 'object' && (payload as { components?: unknown }).components !== null\n    ? (payload as { components: Record<string, unknown> }).components\n    : undefined\n\n  return {\n    status,\n    components\n  }\n}\n`
}

function renderZodModule(entity: UiSchemaEntity) {
  const shape = entity.fields
    .map((field) => {
      const base = field.component === 'json'
        ? 'z.record(z.string(), z.unknown())'
        : field.component === 'switch'
          ? 'z.boolean()'
          : field.component === 'number'
            ? 'z.number()'
            : 'z.string()'
      return `  ${field.key}: ${field.required ? base : `${base}.optional()`}`
    })
    .join(',\n')

  return `// AUTO-GENERATED FILE - DO NOT EDIT\n\nimport { z } from 'zod'\n\nexport const ${entity.entity}Schema = z.object({\n${shape}\n})\n\nexport type ${capitalize(entity.entity)}Input = z.infer<typeof ${entity.entity}Schema>\n`
}

function capitalize(value: string) {
  return value.length ? value.charAt(0).toUpperCase() + value.slice(1) : value
}

async function main() {
  await mkdir(resolve(process.cwd(), 'types'), { recursive: true })
  await mkdir(resolve(process.cwd(), 'ui-schema'), { recursive: true })
  await mkdir(resolve(process.cwd(), 'domains/health'), { recursive: true })

  if (shouldGenerateTypes) {
    const ast = await openapiTS(source)
    await writeFile(typesOutput, astToString(ast))
  }

  const healthUiSchema = createHealthUiSchema()

  if (shouldGenerateUiSchema) {
    await writeFile(uiSchemaOutput, renderUiSchemaModule(healthUiSchema))
  }

  if (shouldGenerateDomain) {
    await writeFile(domainOutput, renderDomainModule(healthUiSchema))
    await writeFile(zodOutput, renderZodModule(healthUiSchema))
  }

  console.log(`Generated OpenAPI artifacts from ${source}`)
  if (shouldGenerateTypes) console.log(`- ${typesOutput}`)
  if (shouldGenerateUiSchema) console.log(`- ${uiSchemaOutput}`)
  if (shouldGenerateDomain) {
    console.log(`- ${domainOutput}`)
    console.log(`- ${zodOutput}`)
  }
}

main().catch((error) => {
  console.error('OpenAPI generation failed:', error)
  process.exitCode = 1
})
