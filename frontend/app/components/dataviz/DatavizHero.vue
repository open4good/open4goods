<template>
  <div class="dataviz-hero" data-test="dataviz-hero">
    <v-row class="dataviz-hero__grid" align="stretch" justify="center" dense>
      <v-col
        v-for="kpi in kpiCards"
        :key="kpi.key"
        cols="6"
        sm="4"
        md="3"
        lg="2"
      >
        <v-card
          class="dataviz-hero__card"
          elevation="2"
          rounded="xl"
          data-test="hero-kpi-card"
        >
          <div class="dataviz-hero__icon-wrap">
            <v-icon :icon="kpi.icon" size="28" color="primary" />
          </div>
          <div class="dataviz-hero__value">{{ kpi.formattedValue }}</div>
          <div class="dataviz-hero__label">{{ kpi.label }}</div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { DatavizHeroStatsDto } from '~~/shared/api-client'

const props = defineProps<{
  stats: DatavizHeroStatsDto
}>()

const { t, n } = useI18n()

interface KpiCard {
  key: string
  icon: string
  label: string
  formattedValue: string
}

const formatNumber = (value: number | undefined | null): string => {
  if (value == null) return '—'
  return n(value, 'decimal')
}

const formatCurrency = (value: number | undefined | null): string => {
  if (value == null) return '—'
  return n(value, 'currency')
}

const formatPercent = (value: number | undefined | null): string => {
  if (value == null) return '—'
  return `${Math.round(value)} %`
}

const kpiCards = computed<KpiCard[]>(() => {
  const s = props.stats
  const cards: KpiCard[] = []

  if (s.totalProducts != null) {
    cards.push({
      key: 'totalProducts',
      icon: 'mdi-package-variant-closed',
      label: t('dataviz.hero.totalProducts'),
      formattedValue: formatNumber(s.totalProducts),
    })
  }

  if (s.totalOffers != null) {
    cards.push({
      key: 'totalOffers',
      icon: 'mdi-tag-multiple-outline',
      label: t('dataviz.hero.totalOffers'),
      formattedValue: formatNumber(s.totalOffers),
    })
  }

  if (s.averagePrice != null) {
    cards.push({
      key: 'averagePrice',
      icon: 'mdi-currency-eur',
      label: t('dataviz.hero.averagePrice'),
      formattedValue: formatCurrency(s.averagePrice),
    })
  }

  if (s.medianPrice != null) {
    cards.push({
      key: 'medianPrice',
      icon: 'mdi-chart-line',
      label: t('dataviz.hero.medianPrice'),
      formattedValue: formatCurrency(s.medianPrice),
    })
  }

  if (s.averageEcoscore != null) {
    cards.push({
      key: 'averageEcoscore',
      icon: 'mdi-leaf',
      label: t('dataviz.hero.averageEcoscore'),
      formattedValue: `${s.averageEcoscore.toFixed(1)} / 20`,
    })
  }

  if (s.topBrand) {
    cards.push({
      key: 'topBrand',
      icon: 'mdi-trophy-outline',
      label: t('dataviz.hero.topBrand'),
      formattedValue: s.topBrand,
    })
  }

  if (s.newProductsPercent != null) {
    cards.push({
      key: 'newProductsPercent',
      icon: 'mdi-new-box',
      label: t('dataviz.hero.newPercent'),
      formattedValue: formatPercent(s.newProductsPercent),
    })
  }

  if (s.countriesCount != null) {
    cards.push({
      key: 'countriesCount',
      icon: 'mdi-earth',
      label: t('dataviz.hero.countriesCount'),
      formattedValue: formatNumber(s.countriesCount),
    })
  }

  // Dynamic extra KPIs from YAML config
  if (s.extraKpis?.length) {
    for (const kpi of s.extraKpis) {
      cards.push({
        key: `extra-${kpi.id ?? kpi.label}`,
        icon: 'mdi-chart-box-outline',
        label: kpi.label ?? kpi.id ?? '',
        formattedValue:
          kpi.value != null
            ? String(kpi.value) + (kpi.unit ? ` ${kpi.unit}` : '')
            : '—',
      })
    }
  }

  return cards
})
</script>

<style scoped lang="scss">
.dataviz-hero {
  margin-bottom: 32px;

  &__grid {
    gap: 8px;
  }

  &__card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px 12px;
    text-align: center;
    min-height: 140px;
    background: rgba(var(--v-theme-surface-glass), 0.85);
    backdrop-filter: blur(8px);
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(var(--v-theme-shadow-primary-600), 0.12);
    }
  }

  &__icon-wrap {
    margin-bottom: 8px;
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(var(--v-theme-surface-primary-120), 1);
  }

  &__value {
    font-size: 1.35rem;
    font-weight: 700;
    line-height: 1.2;
    color: rgb(var(--v-theme-text-neutral-strong));
    margin-bottom: 4px;
  }

  &__label {
    font-size: 0.78rem;
    font-weight: 500;
    color: rgb(var(--v-theme-text-neutral-secondary));
    line-height: 1.3;
  }
}
</style>
