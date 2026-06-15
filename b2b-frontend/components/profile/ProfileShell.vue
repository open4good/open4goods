<template>
  <div class="profile-shell py-4 py-md-8">
    <v-row align="start" class="profile-shell__grid">
      <v-col cols="12" lg="3" xl="2">
        <v-sheet class="profile-shell__nav pa-2" rounded="lg" border>
          <div class="profile-shell__orbital-header">
            <ClientOnly>
              <LandingHeroNetworkVisual
                class="profile-shell__orbital-canvas"
                theme="dark"
                :paused="prefersReducedMotion"
                :config="networkConfig"
              />
            </ClientOnly>
            <div class="profile-shell__orbital-overlay">
              <div class="text-overline text-primary font-weight-bold" style="font-size: 0.65rem; letter-spacing: 0.12em;">
                {{ t('profile.nav.badge') }}
              </div>
              <div class="text-subtitle-1 font-weight-bold text-white mt-1 text-truncate">
                {{ displayName }}
              </div>
            </div>
          </div>

          <v-divider class="my-2" />

          <v-list density="comfortable" nav>
            <v-list-item
              v-for="item in navigation"
              :key="item.to"
              :to="localePath(item.to)"
              :prepend-icon="item.icon"
              :title="t(item.titleKey)"
              :subtitle="t(item.subtitleKey)"
              color="primary"
              lines="two"
              rounded="lg"
            />
          </v-list>
        </v-sheet>
      </v-col>

      <v-col cols="12" lg="9" xl="10">
        <slot />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import LandingHeroNetworkVisual from '~/components/landing/HeroNetworkVisual.vue'
import type { DeepPartial, HeroNetworkConfig } from '~/components/landing/HeroNetworkVisual.vue'

const { t } = useI18n()
const localePath = useLocalePath()
const { session } = useAuthSession()
const { prefersReducedMotion } = useReducedMotion()

const displayName = computed(() => session.value?.name || session.value?.email || t('profile.fallback_name'))

const networkConfig = computed<DeepPartial<HeroNetworkConfig>>(() => ({
  seed: 42,
  density: 'low',
  fps: 18,
  routers: { count: 3, layout: 'ellipse', size: [8, 14], linkChance: 0.4, communicationIntensity: 0.5, minDistance: 80 },
  nodes: { total: 14, size: [1.8, 3.6], pulseChance: 0.08, switchRouterChance: 0.03, minLifetime: 28000, maxLifetime: 48000, spawnJitter: 0.9, maxVisibleOnSmall: 14 },
  communication: { packetCount: 5, packetSpeed: 0.45, beamChance: 0.03, subtlety: 0.92, linkSampleSize: 12, dottedPaths: false },
  rings: { count: 2, opacity: 0.04, speed: 0.04, scale: 0.9 },
  layout: { anchorX: 'center', anchorY: 'center', spread: 0.85, fit: 'cover' },
  visual: {
    background: 'transparent',
    glow: 0.7,
    blur: 0.28,
    lineOpacity: 0.1,
    colorRouter: '#00f5ff',
    colorRouterDeep: '#053b73',
    colorNode: '#b15cff',
    colorNodeDeep: '#35136e',
    colorPacket: '#00ffc6',
    colorAccent: '#ff3df2',
    colorGrid: 'rgba(125, 211, 252, 0.06)'
  }
}))

const navigation = [
  {
    to: '/profile',
    icon: 'mdi-view-dashboard-outline',
    titleKey: 'profile.nav.items.dashboard.title',
    subtitleKey: 'profile.nav.items.dashboard.subtitle'
  },
  {
    to: '/profile/informations',
    icon: 'mdi-card-account-details-outline',
    titleKey: 'profile.nav.items.informations.title',
    subtitleKey: 'profile.nav.items.informations.subtitle'
  },
  {
    to: '/profile/nodes',
    icon: 'mdi-server-network',
    titleKey: 'profile.nav.items.nodes.title',
    subtitleKey: 'profile.nav.items.nodes.subtitle'
  },
  {
    to: '/profile/keys',
    icon: 'mdi-key-variant',
    titleKey: 'profile.nav.items.keys.title',
    subtitleKey: 'profile.nav.items.keys.subtitle'
  },
  {
    to: '/profile/billing',
    icon: 'mdi-credit-card-outline',
    titleKey: 'profile.nav.items.billing.title',
    subtitleKey: 'profile.nav.items.billing.subtitle'
  },
  {
    to: '/profile/organisations',
    icon: 'mdi-domain',
    titleKey: 'profile.nav.items.organisations.title',
    subtitleKey: 'profile.nav.items.organisations.subtitle'
  },
  {
    to: '/profile/security',
    icon: 'mdi-shield-lock-outline',
    titleKey: 'profile.nav.items.security.title',
    subtitleKey: 'profile.nav.items.security.subtitle'
  }
]
</script>

<style scoped>
.profile-shell__nav {
  position: sticky;
  top: 88px;
  background: rgb(var(--v-theme-surface));
  overflow: hidden;
}

.profile-shell__orbital-header {
  position: relative;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  background: #06131f;
  margin: 4px 4px 0;
}

.profile-shell__orbital-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.9;
}

.profile-shell__orbital-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 10px 14px;
  background: linear-gradient(to top, rgba(6, 19, 31, 0.72) 0%, transparent 60%);
}

@media (max-width: 1279px) {
  .profile-shell__nav {
    position: static;
  }
}
</style>
