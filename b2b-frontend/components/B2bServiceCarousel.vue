<template>
  <div>
    <B2bPageHeader v-if="title" :title="title" class="mb-4" />
    <v-slide-group show-arrows="desktop" class="b2b-service-carousel">
      <v-slide-group-item
        v-for="service in services"
        :key="service.id"
      >
        <div class="b2b-service-carousel__item ma-2">
          <B2bServiceCard :service="service" />
        </div>
      </v-slide-group-item>
    </v-slide-group>
  </div>
</template>

<script setup lang="ts">
import type { ServiceDescriptor } from '~/domains/b2b/services'

defineProps<{
  services: ServiceDescriptor[]
  title?: string
}>()
</script>

<style scoped>
.b2b-service-carousel__item {
  width: 280px;
}
</style>
