<template>
  <div class="score-reading-guide">
    <v-btn
      variant="text"
      color="primary"
      prepend-icon="mdi-information-outline"
      class="score-reading-guide__trigger"
      @click="isOpen = true"
    >
      {{ t('technicalFieldGuide.cta') }}
    </v-btn>

    <v-dialog v-model="isOpen" max-width="980" scrollable>
      <v-card>
        <v-card-title class="text-h6">
          {{ t('technicalFieldGuide.title') }}
        </v-card-title>
        <v-card-text>
          <p class="mb-4 text-body-2">
            {{ t('technicalFieldGuide.intro') }}
          </p>

          <v-table density="comfortable">
            <thead>
              <tr>
                <th>{{ t('technicalFieldGuide.table.technicalField') }}</th>
                <th>{{ t('technicalFieldGuide.table.readableLabel') }}</th>
                <th>{{ t('technicalFieldGuide.table.tooltip') }}</th>
                <th>{{ t('technicalFieldGuide.table.source') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in visibleEntries" :key="entry.mapping">
                <td><code>{{ entry.mapping }}</code></td>
                <td>{{ t(entry.labelKey) }}</td>
                <td>{{ t(entry.tooltipKey) }}</td>
                <td>{{ t(entry.sourceKey) }}</td>
              </tr>
            </tbody>
          </v-table>

          <v-expand-transition>
            <div v-if="showAdvanced && hiddenEntries.length" class="mt-4">
              <v-alert variant="tonal" type="info" density="compact">
                {{ t('technicalFieldGuide.advancedVisible') }}
              </v-alert>
            </div>
          </v-expand-transition>
        </v-card-text>
        <v-card-actions class="justify-space-between">
          <v-btn
            v-if="hiddenEntries.length"
            variant="text"
            @click="showAdvanced = !showAdvanced"
          >
            {{
              showAdvanced
                ? t('technicalFieldGuide.hideAdvanced')
                : t('technicalFieldGuide.showAdvanced', {
                    count: hiddenEntries.length,
                  })
            }}
          </v-btn>
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="isOpen = false">
            {{ t('common.actions.close') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  buildTechnicalFieldGuideEntries,
  type TechnicalFieldGuideEntry,
} from '~/utils/technical-field-guide'

const props = defineProps<{
  mappings: Array<string | null | undefined>
}>()

const { t } = useI18n()
const isOpen = ref(false)
const showAdvanced = ref(false)

const entries = computed<TechnicalFieldGuideEntry[]>(() =>
  buildTechnicalFieldGuideEntries(props.mappings)
)

const essentialEntries = computed(() => entries.value.filter(entry => entry.essential))
const hiddenEntries = computed(() => entries.value.filter(entry => !entry.essential))

const visibleEntries = computed(() =>
  showAdvanced.value
    ? entries.value
    : [...essentialEntries.value]
)
</script>

<style scoped lang="sass">
.score-reading-guide__trigger
  text-transform: none
  font-weight: 600
</style>
