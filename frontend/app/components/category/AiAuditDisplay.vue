<template>
  <div class="ai-audit-display">
    <!-- Positioning -->
    <v-card
      v-if="aiResult.positioning"
      class="mb-4"
      variant="tonal"
      color="info"
    >
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-target" class="me-2" />
        {{ t('aiAudit.positioning.title') }}
      </v-card-title>
      <v-card-text>
        <p class="text-subtitle-1 font-weight-bold">
          {{ aiResult.positioning.claim }}
        </p>
        <p>{{ aiResult.positioning.what_it_means_for_users }}</p>
        <v-alert
          v-if="aiResult.positioning.not_an_absolute_footprint"
          type="warning"
          variant="outlined"
          density="compact"
          class="mt-2"
        >
          {{ t('aiAudit.positioning.notAbsoluteWarning') }}
        </v-alert>
      </v-card-text>
    </v-card>

    <!-- Gap Analysis -->
    <v-expansion-panels
      v-if="aiResult.gap_analysis_absolute_vs_relative"
      class="mb-4"
    >
      <v-expansion-panel>
        <v-expansion-panel-title>
          <v-icon icon="mdi-scale-balance" class="me-2" />
          {{ t('aiAudit.gapAnalysis.title') }}
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-row>
            <v-col cols="12" md="6">
              <h4 class="text-subtitle-2 mb-2">
                {{ t('aiAudit.gapAnalysis.missing') }}
              </h4>
              <p>
                {{
                  aiResult.gap_analysis_absolute_vs_relative
                    .what_is_missing_vs_full_lca
                }}
              </p>
            </v-col>
            <v-col cols="12" md="6">
              <h4 class="text-subtitle-2 mb-2">
                {{ t('aiAudit.gapAnalysis.whyUseful') }}
              </h4>
              <p>
                {{
                  aiResult.gap_analysis_absolute_vs_relative
                    .why_relative_is_still_useful
                }}
              </p>
            </v-col>
          </v-row>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- Criteria Weights -->
    <v-card v-if="aiResult.criteria_weights" class="mb-4">
      <v-card-title>
        <v-icon icon="mdi-weight" class="me-2" />
        {{ t('aiAudit.weights.title') }}
      </v-card-title>
      <v-table density="compact">
        <thead>
          <tr>
            <th>{{ t('aiAudit.weights.criterion') }}</th>
            <th>{{ t('aiAudit.weights.weight') }}</th>
            <th>{{ t('aiAudit.weights.rationale') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="criterion in aiResult.criteria_weights"
            :key="criterion.criterion"
          >
            <td class="font-weight-medium">{{ criterion.criterion }}</td>
            <td>{{ formatPercent(criterion.weight) }}</td>
            <td class="text-caption">{{ criterion.rationale }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card>

    <!-- Sources -->
    <v-card v-if="aiResult.sources" class="mb-4">
      <v-card-title>
        <v-icon icon="mdi-book-open-variant" class="me-2" />
        {{ t('aiAudit.sources.title') }}
      </v-card-title>
      <v-list density="compact" lines="two">
        <v-list-item
          v-for="source in aiResult.sources"
          :key="source.id"
          :href="source.url"
          target="_blank"
          rel="noopener"
          prepend-icon="mdi-link"
        >
          <v-list-item-title>
            [{{ source.id }}] {{ source.title }}
            <span class="text-caption text-medium-emphasis"
              >({{ source.year }})</span
            >
          </v-list-item-title>
          <v-list-item-subtitle>{{
            source.publisher || source.authors_or_org
          }}</v-list-item-subtitle>
        </v-list-item>
      </v-list>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

interface Source {
  id: string
  url?: string
  title: string
  year?: string | number
  publisher?: string
  authors_or_org?: string
}

interface AiResult {
  positioning?: {
    claim: string
    what_it_means_for_users: string
    not_an_absolute_footprint?: boolean
  }
  gap_analysis_absolute_vs_relative?: {
    what_is_missing_vs_full_lca: string
    why_relative_is_still_useful: string
  }
  criteria_weights?: Array<{
    criterion: string
    weight: number
    rationale: string
  }>
  sources?: Source[]
}

defineProps<{
  aiResult: AiResult
}>()

const { t } = useI18n()

function formatPercent(value: number) {
  return new Intl.NumberFormat('default', {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value)
}
</script>

<style scoped>
.ai-audit-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
