<template>
  <div class="b2b-billing-catalog">
    <v-row>
      <v-col v-for="item in items" :key="String(item.id)" cols="12" md="4">
        <v-card class="h-100 b2b-billing-catalog__card" variant="flat">
          <v-card-text class="pa-5">
            <div class="d-flex align-start justify-space-between ga-3 mb-4">
              <div>
                <h3 class="text-h6 font-weight-bold">{{ item.name }}</h3>
                <p class="text-body-2 text-medium-emphasis mb-0">{{ item.description }}</p>
              </div>
              <v-chip v-if="item.badge" color="primary" variant="tonal" size="small">{{ item.badge }}</v-chip>
            </div>
            <p class="text-h4 font-weight-bold mb-1">{{ item.price }}</p>
            <p class="text-caption text-medium-emphasis mb-4">{{ item.credits }} credits</p>
            <v-btn block color="primary" @click="emit('select', String(item.id))">{{ ctaLabel }}</v-btn>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  items: Array<{ id: string | number; name: string; description?: string; price: string; credits: string | number; badge?: string }>
  ctaLabel?: string
}>(), {
  ctaLabel: 'Choose'
})

const emit = defineEmits<{
  select: [string]
}>()
</script>

<style scoped>
.b2b-billing-catalog__card {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
}
</style>
