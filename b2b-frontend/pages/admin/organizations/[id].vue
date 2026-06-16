<template>
  <div>
    <B2bPageHeader
      :title="org?.name ?? t('admin.organizations.detail.title')"
      :subtitle="org ? `${org.slug} · ${org.status}` : ''"
    >
      <template #actions>
        <v-btn prepend-icon="mdi-arrow-left" variant="text" to="/admin/organizations">
          {{ t('admin.overview.back') }}
        </v-btn>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="grantDialog = true">
          {{ t('admin.organizations.detail.manualGrant') }}
        </v-btn>
      </template>
    </B2bPageHeader>

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <div class="d-flex flex-column ga-6">
        <!-- Org metadata -->
        <v-row dense>
          <v-col cols="12" md="6">
            <v-card rounded="lg" variant="outlined" class="pa-4">
              <div class="text-subtitle-2 font-weight-bold mb-3">{{ t('admin.organizations.detail.info') }}</div>
              <v-list density="compact" lines="two">
                <v-list-item :title="t('admin.organizations.name')" :subtitle="org?.name" />
                <v-list-item :title="t('admin.organizations.slug')" :subtitle="org?.slug" />
                <v-list-item :title="t('admin.organizations.detail.billingEmail')" :subtitle="org?.billingEmail ?? '-'" />
                <v-list-item :title="t('admin.organizations.balance')" :subtitle="`${org?.creditBalance ?? 0} credits`" />
                <v-list-item :title="t('admin.organizations.freeGrant')" :subtitle="org?.freeGrantApplied ? t('common.yes') : t('common.no')" />
              </v-list>
            </v-card>
          </v-col>
          <v-col cols="12" md="6">
            <v-card rounded="lg" variant="outlined" class="pa-4 h-100">
              <div class="text-subtitle-2 font-weight-bold mb-3">{{ t('admin.organizations.detail.quickActions') }}</div>
              <div class="d-flex flex-column ga-2">
                <v-btn
                  block
                  variant="outlined"
                  color="primary"
                  prepend-icon="mdi-gift-outline"
                  @click="grantDialog = true"
                >
                  {{ t('admin.organizations.detail.manualGrant') }}
                </v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- Transaction history -->
        <div>
          <div class="text-subtitle-1 font-weight-bold mb-3">{{ t('admin.organizations.detail.transactions') }}</div>
          <v-card rounded="lg" variant="outlined">
            <v-table density="compact">
              <thead>
                <tr>
                  <th>{{ t('dashboard.transactions.type') }}</th>
                  <th>{{ t('dashboard.transactions.credits') }}</th>
                  <th>{{ t('dashboard.transactions.facet') }}</th>
                  <th>{{ t('dashboard.transactions.note') }}</th>
                  <th>{{ t('dashboard.transactions.date') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!transactions.length">
                  <td colspan="5" class="text-center text-medium-emphasis py-4">{{ t('dashboard.transactions.empty') }}</td>
                </tr>
                <tr v-for="tx in transactions" :key="tx.id">
                  <td><B2bStatusChip :status="tx.type" size="x-small" /></td>
                  <td class="text-mono">{{ tx.type === 'DEBIT' ? `-${tx.credits}` : `+${tx.credits}` }}</td>
                  <td class="text-caption">{{ tx.facetId ?? '-' }}</td>
                  <td class="text-caption text-medium-emphasis">{{ tx.note ?? '-' }}</td>
                  <td class="text-caption text-medium-emphasis">{{ formatDate(tx.createdAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </v-card>
        </div>
      </div>
    </B2bAsyncState>

    <!-- Manual grant dialog -->
    <v-dialog v-model="grantDialog" max-width="480">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pa-6 pb-3">{{ t('admin.organizations.detail.grantTitle') }}</v-card-title>
        <v-card-text class="px-6">
          <v-text-field
            v-model.number="grantCredits"
            :label="t('admin.organizations.detail.grantCredits')"
            type="number"
            min="1"
            variant="outlined"
            density="comfortable"
          />
          <v-textarea
            v-model="grantNote"
            :label="t('admin.organizations.detail.grantNote')"
            variant="outlined"
            density="comfortable"
            rows="2"
            class="mt-3"
          />
        </v-card-text>
        <v-card-actions class="px-6 pb-6">
          <v-spacer />
          <v-btn variant="text" @click="grantDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn
            color="primary"
            variant="flat"
            :loading="granting"
            :disabled="!grantCredits || !grantNote"
            @click="doGrant"
          >
            {{ t('admin.organizations.detail.confirmGrant') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="grantSuccess" color="success" timeout="3000">
      {{ t('admin.organizations.detail.grantSuccess', { credits: lastGranted }) }}
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
import type { B2bAdminOrganization } from '~/domains/b2b/admin'
import type { B2bTransaction } from '~/domains/b2b/billing'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['admin'] })

const { t } = useI18n()
const route = useRoute()
const orgId = computed(() => route.params.id as string)

const admin = useAdminRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const org = ref<B2bAdminOrganization | null>(null)
const transactions = ref<B2bTransaction[]>([])

const grantDialog = ref(false)
const grantCredits = ref<number>(1000)
const grantNote = ref('')
const granting = ref(false)
const grantSuccess = ref(false)
const lastGranted = ref(0)

useSeoMeta({ title: computed(() => org.value?.name ? `${org.value.name} - Admin` : 'Organization - Admin') })

async function load() {
  loading.value = true
  error.value = null
  try {
    const [o, txs] = await Promise.all([
      admin.getOrganization(orgId.value),
      admin.getOrganizationTransactions(orgId.value)
    ])
    org.value = o
    transactions.value = txs
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

async function doGrant() {
  granting.value = true
  try {
    const res = await admin.grantManualCredits(orgId.value, {
      credits: grantCredits.value,
      note: grantNote.value
    })
    lastGranted.value = res.creditsGranted
    grantDialog.value = false
    grantCredits.value = 1000
    grantNote.value = ''
    grantSuccess.value = true
    await load()
  } finally {
    granting.value = false
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

onMounted(load)
</script>
