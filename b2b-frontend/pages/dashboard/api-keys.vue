<template>
  <div>
    <B2bPageHeader
      :title="t('dashboard.keys.title')"
      :subtitle="t('dashboard.keys.subtitle')"
    >
      <template #actions>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="createDialog = true">
          {{ t('dashboard.keys.create') }}
        </v-btn>
      </template>
    </B2bPageHeader>

    <B2bAsyncState :loading="loading" :error="error" @retry="load">
      <v-card rounded="lg" variant="outlined">
        <v-table>
          <thead>
            <tr>
              <th>{{ t('dashboard.keys.name') }}</th>
              <th>{{ t('dashboard.keys.prefix') }}</th>
              <th>{{ t('dashboard.keys.status') }}</th>
              <th>{{ t('dashboard.keys.lastUsed') }}</th>
              <th>{{ t('dashboard.keys.created') }}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-if="!keys.length">
              <td colspan="6" class="text-center text-medium-emphasis py-6">
                {{ t('dashboard.keys.empty') }}
              </td>
            </tr>
            <tr v-for="key in keys" :key="key.id">
              <td class="font-weight-medium">{{ key.name }}</td>
              <td><code class="text-caption">{{ key.keyPrefix }}…</code></td>
              <td><B2bStatusChip :status="key.status" size="x-small" /></td>
              <td class="text-caption text-medium-emphasis">
                {{ key.lastUsedAt ? formatDate(key.lastUsedAt) : t('common.not_available') }}
              </td>
              <td class="text-caption text-medium-emphasis">{{ formatDate(key.createdAt) }}</td>
              <td>
                <div class="d-flex ga-1 justify-end">
                  <v-btn
                    v-if="key.status === 'ACTIVE'"
                    icon="mdi-refresh"
                    size="x-small"
                    variant="text"
                    :title="t('dashboard.keys.rotate')"
                    :loading="rotatingId === key.id"
                    @click="confirmRotate(key)"
                  />
                  <v-btn
                    v-if="key.status === 'ACTIVE'"
                    icon="mdi-delete-outline"
                    size="x-small"
                    variant="text"
                    color="error"
                    :title="t('dashboard.keys.revoke')"
                    :loading="revokingId === key.id"
                    @click="confirmRevoke(key)"
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
    </B2bAsyncState>

    <!-- Create key dialog -->
    <v-dialog v-model="createDialog" max-width="440">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pa-6 pb-3">{{ t('dashboard.keys.createTitle') }}</v-card-title>
        <v-card-text class="px-6">
          <v-text-field
            v-model="newKeyName"
            :label="t('dashboard.keys.nameLabel')"
            :rules="[v => !!v || t('dashboard.keys.nameRequired')]"
            variant="outlined"
            density="comfortable"
            autofocus
          />
        </v-card-text>
        <v-card-actions class="px-6 pb-6">
          <v-spacer />
          <v-btn variant="text" @click="createDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" variant="flat" :loading="creating" :disabled="!newKeyName" @click="doCreate">
            {{ t('dashboard.keys.create') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Reveal secret dialog (shown once after create/rotate) -->
    <v-dialog v-model="revealDialog" max-width="540" persistent>
      <v-card rounded="lg">
        <v-card-title class="text-h6 pa-6 pb-3">
          <v-icon icon="mdi-key-variant" color="primary" class="mr-2" />
          {{ t('dashboard.keys.revealTitle') }}
        </v-card-title>
        <v-card-text class="px-6">
          <v-alert type="warning" variant="tonal" class="mb-4">
            {{ t('dashboard.keys.revealWarning') }}
          </v-alert>
          <B2bCodeBlock :code="revealedSecret" language="text" />
        </v-card-text>
        <v-card-actions class="px-6 pb-6">
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="closeReveal">
            {{ t('dashboard.keys.savedKey') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Rotate confirmation dialog -->
    <v-dialog v-model="rotateDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pa-6 pb-3">{{ t('dashboard.keys.rotateConfirm') }}</v-card-title>
        <v-card-text class="px-6 text-body-2 text-medium-emphasis">
          {{ t('dashboard.keys.rotateWarning', { name: actionTarget?.name }) }}
        </v-card-text>
        <v-card-actions class="px-6 pb-6">
          <v-spacer />
          <v-btn variant="text" @click="rotateDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="warning" variant="flat" :loading="rotatingId === actionTarget?.id" @click="doRotate">
            {{ t('dashboard.keys.rotate') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Revoke confirmation dialog -->
    <v-dialog v-model="revokeDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pa-6 pb-3">{{ t('dashboard.keys.revokeConfirm') }}</v-card-title>
        <v-card-text class="px-6 text-body-2 text-medium-emphasis">
          {{ t('dashboard.keys.revokeWarning', { name: actionTarget?.name }) }}
        </v-card-text>
        <v-card-actions class="px-6 pb-6">
          <v-spacer />
          <v-btn variant="text" @click="revokeDialog = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" variant="flat" :loading="revokingId === actionTarget?.id" @click="doRevoke">
            {{ t('dashboard.keys.revoke') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import type { B2bApiKey } from '~/domains/b2b/keys'
import type { AppApiError } from '~/composables/useApiClient'

definePageMeta({ layout: 'default', middleware: ['authenticated'] })

const { t } = useI18n()
useSeoMeta({ title: t('dashboard.keys.seo.title') })

const repo = useB2bApiKeysRepository()

const loading = ref(true)
const error = ref<AppApiError | null>(null)
const keys = ref<B2bApiKey[]>([])

const createDialog = ref(false)
const newKeyName = ref('')
const creating = ref(false)

const revealDialog = ref(false)
const revealedSecret = ref('')

const rotateDialog = ref(false)
const revokeDialog = ref(false)
const actionTarget = ref<B2bApiKey | null>(null)
const rotatingId = ref<string | null>(null)
const revokingId = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    keys.value = await repo.list()
  } catch (err) {
    error.value = err as AppApiError
  } finally {
    loading.value = false
  }
}

async function doCreate() {
  creating.value = true
  try {
    const res = await repo.create({ name: newKeyName.value })
    createDialog.value = false
    newKeyName.value = ''
    revealedSecret.value = res.clearKey
    revealDialog.value = true
    await load()
  } finally {
    creating.value = false
  }
}

function confirmRotate(key: B2bApiKey) {
  actionTarget.value = key
  rotateDialog.value = true
}

function confirmRevoke(key: B2bApiKey) {
  actionTarget.value = key
  revokeDialog.value = true
}

async function doRotate() {
  if (!actionTarget.value) return
  rotatingId.value = actionTarget.value.id
  try {
    const res = await repo.rotate(actionTarget.value.id)
    rotateDialog.value = false
    revealedSecret.value = res.clearKey
    revealDialog.value = true
    await load()
  } finally {
    rotatingId.value = null
  }
}

async function doRevoke() {
  if (!actionTarget.value) return
  revokingId.value = actionTarget.value.id
  try {
    await repo.revoke(actionTarget.value.id)
    revokeDialog.value = false
    await load()
  } finally {
    revokingId.value = null
  }
}

function closeReveal() {
  revealDialog.value = false
  revealedSecret.value = ''
  actionTarget.value = null
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

onMounted(load)
</script>
