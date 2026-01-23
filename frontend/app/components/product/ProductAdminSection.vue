<template>
  <ClientOnly>
    <section v-if="showAdmin" :id="panelId" class="product-admin">
      <header class="product-admin__header">
        <h2 class="product-admin__title">{{ $t('product.admin.title') }}</h2>
        <p class="product-admin__subtitle">
          {{ $t('product.admin.subtitle') }}
        </p>
      </header>

      <v-tabs v-model="activeTab" bg-color="transparent" color="primary">
        <v-tab value="local">{{
          $t('product.admin.sections.productJson.tab')
        }}</v-tab>
        <v-tab value="payload">{{
          $t('product.admin.sections.payloadJson.tab')
        }}</v-tab>
        <v-tab value="datasources">{{
          $t('product.admin.sections.datasources.tab')
        }}</v-tab>
      </v-tabs>

      <v-window v-model="activeTab">
        <v-window-item value="local">
          <article
            :id="jsonSectionId"
            class="product-admin__block"
            role="region"
            :aria-label="$t('product.admin.sections.productJson.title')"
          >
            <header class="product-admin__block-header">
              <h3 class="product-admin__block-title">
                {{ $t('product.admin.sections.productJson.title') }}
              </h3>
              <p class="product-admin__block-helper">
                {{ $t('product.admin.sections.productJson.helper') }}
              </p>
            </header>
            <!-- eslint-disable vue/no-v-html -->
            <pre class="product-admin__code" aria-live="polite">
              <code
                v-if="highlightedJson"
                class="hljs language-json"
                v-html="highlightedJson"
              />
              <code v-else class="product-admin__code-fallback">{{ formatted }}</code>
            </pre>
            <!-- eslint-enable vue/no-v-html -->
          </article>
        </v-window-item>

        <v-window-item value="payload">
          <article class="product-admin__block">
            <header class="product-admin__block-header">
              <h3 class="product-admin__block-title">
                {{ $t('product.admin.sections.payloadJson.title') }}
              </h3>
              <p class="product-admin__block-helper">
                {{ $t('product.admin.sections.payloadJson.helper') }}
              </p>
            </header>
            <div class="product-admin__iframe-wrapper">
              <iframe
                :src="apiPayloadUrl"
                class="product-admin__iframe"
                :title="$t('product.admin.sections.payloadJson.iframeTitle')"
              />
            </div>
          </article>
        </v-window-item>

        <v-window-item value="datasources">
          <article
            :id="datasourcesSectionId"
            class="product-admin__block"
            role="region"
            :aria-label="$t('product.admin.sections.datasources.title')"
          >
            <header class="product-admin__block-header">
              <h3 class="product-admin__block-title">
                {{ $t('product.admin.sections.datasources.title') }}
              </h3>
              <p class="product-admin__block-helper">
                {{ $t('product.admin.sections.datasources.helper') }}
              </p>
            </header>

            <v-table density="compact" class="product-admin__table">
              <thead>
                <tr>
                  <th scope="col">
                    {{ $t('product.admin.sections.datasources.table.name') }}
                  </th>
                  <th scope="col">
                    {{ $t('product.admin.sections.datasources.table.code') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in datasourceRows" :key="row.name">
                  <td>{{ row.name }}</td>
                  <td>{{ row.code }}</td>
                </tr>
              </tbody>
            </v-table>
            <p
              v-if="!datasourceRows.length"
              class="product-admin__block-helper"
            >
              {{ $t('product.admin.sections.datasources.empty') }}
            </p>

            <div class="product-admin__yaml">
              <v-tabs v-model="yamlTab" bg-color="transparent" color="primary">
                <v-tab value="product">{{
                  $t('product.admin.sections.datasources.yaml.product')
                }}</v-tab>
                <v-tab value="vertical">{{
                  $t('product.admin.sections.datasources.yaml.vertical')
                }}</v-tab>
              </v-tabs>
              <!-- eslint-disable vue/no-v-html -->
              <v-window v-model="yamlTab">
                <v-window-item value="product">
                  <pre class="product-admin__code" aria-live="polite">
                    <code
                      v-if="highlightedDatasourceYaml"
                      class="hljs language-yaml"
                      v-html="highlightedDatasourceYaml"
                    />
                    <code
                      v-else
                      class="product-admin__code-fallback"
                      >{{ formattedDatasourceYaml }}</code
                    >
                  </pre>
                </v-window-item>
                <v-window-item value="vertical">
                  <pre class="product-admin__code" aria-live="polite">
                    <code
                      v-if="highlightedVerticalYaml"
                      class="hljs language-yaml"
                      v-html="highlightedVerticalYaml"
                    />
                    <code
                      v-else
                      class="product-admin__code-fallback"
                      >{{ formattedVerticalYaml }}</code
                    >
                  </pre>
                </v-window-item>
              </v-window>
              <!-- eslint-enable vue/no-v-html -->
            </div>
          </article>
        </v-window-item>
      </v-window>
    </section>
  </ClientOnly>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PropType } from 'vue'
import type { ProductDto, VerticalConfigFullDto } from '~~/shared/api-client'
import { useAuth } from '~/composables/useAuth'
import { loadHighlightJs } from '~/utils/highlight-loader'

const props = defineProps({
  panelId: {
    type: String,
    default: 'admin',
  },
  jsonSectionId: {
    type: String,
    default: 'admin-json',
  },
  datasourcesSectionId: {
    type: String,
    default: 'admin-datasources',
  },
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  verticalConfig: {
    type: Object as PropType<VerticalConfigFullDto | null>,
    default: null,
  },
})

const { isLoggedIn } = useAuth()

const showAdmin = computed(() => isLoggedIn.value)
const formatAsYaml = (value: unknown): string => {
  const normalizeValue = (input: unknown): unknown => {
    if (input instanceof Set) {
      return Array.from(input)
    }

    if (Array.isArray(input)) {
      return input.map(item => normalizeValue(item))
    }

    if (input && typeof input === 'object') {
      return Object.fromEntries(
        Object.entries(input as Record<string, unknown>).map(([key, val]) => [
          key,
          normalizeValue(val),
        ])
      )
    }

    return input
  }

  const isPlainObject = (input: unknown): input is Record<string, unknown> =>
    Boolean(input) && typeof input === 'object' && !Array.isArray(input)

  const formatScalar = (input: unknown): string => {
    if (input === null || input === undefined) {
      return 'null'
    }

    if (typeof input === 'string') {
      const needsQuotes = /[:\n#]/.test(input) || input.trim() !== input
      return needsQuotes ? JSON.stringify(input) : input
    }

    if (typeof input === 'number' || typeof input === 'boolean') {
      return String(input)
    }

    return JSON.stringify(input)
  }

  const buildYaml = (input: unknown, indent = 0): string => {
    const padding = ' '.repeat(indent)

    if (Array.isArray(input)) {
      if (!input.length) {
        return `${padding}[]`
      }
      return input
        .map(item => {
          if (isPlainObject(item) || Array.isArray(item)) {
            return `${padding}-\n${buildYaml(item, indent + 2)}`
          }
          return `${padding}- ${formatScalar(item)}`
        })
        .join('\n')
    }

    if (isPlainObject(input)) {
      const entries = Object.entries(input)
      if (!entries.length) {
        return `${padding}{}`
      }

      return entries
        .map(([key, val]) => {
          if (isPlainObject(val) || Array.isArray(val)) {
            return `${padding}${key}:\n${buildYaml(val, indent + 2)}`
          }
          return `${padding}${key}: ${formatScalar(val)}`
        })
        .join('\n')
    }

    return `${padding}${formatScalar(input)}`
  }

  return buildYaml(normalizeValue(value))
}

const formatted = computed(() => JSON.stringify(props.product, null, 2))

const activeTab = ref('local')
const yamlTab = ref<'product' | 'vertical'>('product')
const apiPayloadUrl = computed(
  () => `https://api.nudger.fr/product/?gtin=${props.product.gtin}`
)
const datasourceRows = computed(() => {
  const datasources = props.product.datasources?.datasourceCodes ?? {}
  return Object.entries(datasources)
    .map(([name, code]) => ({
      name,
      code,
    }))
    .sort((a, b) => a.name.localeCompare(b.name))
})

const formattedDatasourceYaml = computed(() =>
  formatAsYaml({
    datasources: props.product.datasources ?? null,
  })
)
const formattedVerticalYaml = computed(() =>
  formatAsYaml(props.verticalConfig ?? null)
)

const highlightedJson = ref(formatted.value)
const highlightedDatasourceYaml = ref(formattedDatasourceYaml.value)
const highlightedVerticalYaml = ref(formattedVerticalYaml.value)

const highlightYamlContent = async (
  content: string,
  target: { value: string | null }
) => {
  if (!content) {
    target.value = ''
    return
  }

  target.value = content

  try {
    const hljs = await loadHighlightJs(['yaml'])

    if (hljs) {
      target.value = hljs.highlight(content, {
        language: 'yaml',
      }).value
    }
  } catch (error) {
    console.warn('Unable to highlight YAML in admin section', error)
  }
}

const highlightJson = async () => {
  const content = formatted.value
  if (!content) {
    highlightedJson.value = ''
    return
  }

  highlightedJson.value = content

  try {
    const hljs = await loadHighlightJs(['json'])

    if (hljs) {
      highlightedJson.value = hljs.highlight(content, {
        language: 'json',
      }).value
    }
  } catch (error) {
    console.warn('Unable to highlight product JSON in admin section', error)
  }
}

watch(
  formatted,
  () => {
    highlightJson()
  },
  { immediate: true }
)

watch(
  formattedDatasourceYaml,
  content => {
    highlightYamlContent(content, highlightedDatasourceYaml)
  },
  { immediate: true }
)

watch(
  formattedVerticalYaml,
  content => {
    highlightYamlContent(content, highlightedVerticalYaml)
  },
  { immediate: true }
)
</script>

<style scoped>
.product-admin {
  background: rgba(var(--v-theme-error), 0.08);
  border: 1px solid rgba(var(--v-theme-error), 0.3);
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-admin__title {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-admin__header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-admin__subtitle {
  margin: 0;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-admin__block {
  background: rgba(15, 23, 42, 0.92);
  border-radius: 18px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.28);
}

.product-admin__block-header {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.product-admin__block-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #f8fafc;
}

.product-admin__block-helper {
  margin: 0;
  font-size: 0.9rem;
  color: rgba(226, 232, 240, 0.78);
}

.product-admin__code {
  margin: 0;
  padding: 1rem;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.35);
  overflow-x: auto;
  max-height: 540px;
}

.product-admin__code :deep(.hljs) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.88rem;
  line-height: 1.55;
  color: #e2e8f0;
  background: transparent;
}

.product-admin__code-fallback {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.88rem;
  color: #e2e8f0;
}

.product-admin__table {
  background: rgba(15, 23, 42, 0.92);
  border-radius: 12px;
  color: #e2e8f0;
}

.product-admin__yaml {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (max-width: 960px) {
  .product-admin {
    padding: 1.25rem;
    border-radius: 18px;
  }

  .product-admin__block {
    border-radius: 16px;
  }

  .product-admin__code {
    max-height: 320px;
  }
}

.product-admin__iframe-wrapper {
  width: 100%;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  height: 600px;
}

.product-admin__iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}
</style>
