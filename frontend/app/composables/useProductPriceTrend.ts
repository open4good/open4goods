import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'

type ProductTrend = NonNullable<ProductDto['offers']>['newTrend']

export const useProductPriceTrend = () => {
  const { t, n } = useI18n()

  const resolvePriceTrendLabel = (trend: ProductTrend, currency: string) => {
    if (!trend?.trend) {
      return null
    }

    if (trend.trend === 'PRICE_DECREASE') {
      return t('product.price.trend.decrease', {
        amount: n(Math.abs(trend.variation ?? 0), {
          style: 'currency',
          currency,
          maximumFractionDigits: 2,
        }),
      })
    }

    if (trend.trend === 'PRICE_INCREASE') {
      return t('product.price.trend.increase', {
        amount: n(Math.abs(trend.variation ?? 0), {
          style: 'currency',
          currency,
          maximumFractionDigits: 2,
        }),
      })
    }

    return t('product.price.trend.stable')
  }

  const resolvePriceTrendTone = (trend: ProductTrend) => {
    if (trend?.trend === 'PRICE_DECREASE') {
      return 'decrease'
    }

    if (trend?.trend === 'PRICE_INCREASE') {
      return 'increase'
    }

    return 'stable'
  }

  const resolveTrendIcon = (tone: 'decrease' | 'increase' | 'stable') => {
    switch (tone) {
      case 'decrease':
        return 'mdi-trending-down'
      case 'increase':
        return 'mdi-trending-up'
      default:
        return 'mdi-trending-neutral'
    }
  }

  const formatTrendPeriod = (period?: number) => {
    if (!period || period <= 0) {
      return null
    }

    const minutes = Math.max(1, Math.round(period / 60000))
    const hours = Math.round(period / 3600000)
    const days = Math.round(period / 86400000)

    if (days >= 1) {
      return t('product.hero.trendPeriodDays', { count: days })
    }

    if (hours >= 1) {
      return t('product.hero.trendPeriodHours', { count: hours })
    }

    return t('product.hero.trendPeriodMinutes', { count: minutes })
  }

  const formatTrendTooltip = (trend: ProductTrend, currency: string) => {
    if (!trend) {
      return null
    }

    const deviation =
      typeof trend.variation === 'number'
        ? n(Math.abs(trend.variation), {
            style: 'currency',
            currency,
            maximumFractionDigits: 2,
          })
        : null
    const periodLabel = formatTrendPeriod(trend.period)

    if (!deviation || !periodLabel) {
      return null
    }

    return t('product.hero.trendTooltip', {
      deviation,
      period: periodLabel,
    })
  }

  return {
    resolvePriceTrendLabel,
    resolvePriceTrendTone,
    resolveTrendIcon,
    formatTrendTooltip,
  }
}
