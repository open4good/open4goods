import { computed } from 'vue'

export const usePluralizedTranslation = () => {
  const { t, te, locale } = useI18n()

  const pluralRules = computed(() => new Intl.PluralRules(locale.value))

  const translatePlural = (
    baseKey: string,
    count: number,
    values: Record<string, unknown> = {}
  ): string => {
    const selection = pluralRules.value.select(count)
    const normalized = selection === 'one' && count === 0 ? 'other' : selection
    const candidateKey = `${baseKey}.${normalized}`
    const fallbackKey = `${baseKey}.other`
    const targetKey = te(candidateKey) ? candidateKey : fallbackKey

    return t(targetKey, { count, ...values })
  }

  return { translatePlural }
}
