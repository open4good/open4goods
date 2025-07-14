<script setup lang="ts">
interface Props {
  data: any
  title?: string
  showStatus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Debug Data',
  showStatus: true,
})

const isExpanded = ref(false)
const copySuccess = ref(false)

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(JSON.stringify(props.data, null, 2))
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
  }
}

const formatData = (data: any): string => {
  try {
    return JSON.stringify(data, null, 2)
  } catch (error) {
    return String(data)
  }
}

const getDataType = (data: any): string => {
  if (data === null) return 'null'
  if (Array.isArray(data)) return 'array'
  if (typeof data === 'object') return 'object'
  return typeof data
}

const dataType = computed(() => getDataType(props.data))
const dataSize = computed(() => {
  if (Array.isArray(props.data)) return `${props.data.length} items`
  if (typeof props.data === 'object' && props.data !== null) {
    return `${Object.keys(props.data).length} properties`
  }
  return `${String(props.data).length} characters`
})
</script>

<template>
  <div class="debug-card">
    <!-- Header -->
    <div class="debug-header">
      <div class="header-left">
        <h3 class="debug-title">{{ title }}</h3>
        <div class="debug-meta">
          <span class="type-badge" :class="dataType">
            {{ dataType }}
          </span>
          <span class="size-info">{{ dataSize }}</span>
        </div>
      </div>

      <div class="header-actions">
        <v-btn
          size="small"
          variant="outlined"
          @click="toggleExpanded"
          :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
        />
        <v-btn
          size="small"
          variant="outlined"
          @click="copyToClipboard"
          :icon="copySuccess ? 'mdi-check' : 'mdi-content-copy'"
          :color="copySuccess ? 'success' : undefined"
        />
      </div>
    </div>

    <!-- Content -->
    <div class="debug-content">
      <div v-if="!isExpanded" class="preview">
        <pre class="preview-text"
          >{{ formatData(data).substring(0, 200)
          }}{{ formatData(data).length > 200 ? '...' : '' }}</pre
        >
      </div>

      <div v-else class="expanded-content">
        <pre class="json-display">{{ formatData(data) }}</pre>
      </div>
    </div>

    <!-- Status indicator (optional) -->
    <div v-if="showStatus" class="status-footer">
      <span class="status-text">
        Last updated: {{ new Date().toLocaleTimeString() }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.debug-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  margin-bottom: 16px;
  overflow: hidden;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.debug-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.debug-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.type-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: bold;
  text-transform: uppercase;
}

.type-badge.object {
  background-color: #2196f3;
  color: white;
}

.type-badge.array {
  background-color: #4caf50;
  color: white;
}

.type-badge.string {
  background-color: #ff9800;
  color: white;
}

.type-badge.number {
  background-color: #9c27b0;
  color: white;
}

.type-badge.boolean {
  background-color: #607d8b;
  color: white;
}

.type-badge.null {
  background-color: #f44336;
  color: white;
}

.size-info {
  font-size: 11px;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.debug-content {
  padding: 16px;
}

.preview {
  max-height: 100px;
  overflow: hidden;
}

.preview-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #333;
  margin: 0;
  white-space: pre-wrap;
}

.expanded-content {
  max-height: 400px;
  overflow-y: auto;
}

.json-display {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #333;
  margin: 0;
  white-space: pre-wrap;
  background: #f8f8f8;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.status-footer {
  padding: 8px 16px;
  background: #f0f0f0;
  border-top: 1px solid #e0e0e0;
  font-size: 11px;
  color: #666;
}

.status-text {
  font-style: italic;
}
</style>
