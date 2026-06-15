<template>
  <v-menu location="bottom end">
    <template #activator="{ props }">
      <v-btn
        v-bind="props"
        :aria-label="t('nav.language')"
        variant="text"
        class="px-2"
      >
        <span class="text-h6">{{ activeFlag }}</span>
      </v-btn>
    </template>

    <v-list density="compact">
      <v-list-subheader>{{ t('nav.language') }}</v-list-subheader>
      <v-list-item
        v-for="item in localeItems"
        :key="item.code"
        :active="item.code === locale"
        @click="onLocaleChange(item.code)"
      >
        <template #prepend>
          <span class="mr-2 text-h6">{{ item.flag }}</span>
        </template>
        <v-list-item-title>{{ item.name }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
const { t, locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()

const flags: Record<string, string> = {
  en: '🇬🇧',
  fr: '🇫🇷'
}

const localeItems = computed(() => locales.value.map((item) => {
  const localeItem = item as { code: string, name?: string }

  return {
    code: localeItem.code,
    name: localeItem.name || localeItem.code.toUpperCase(),
    flag: flags[localeItem.code] || '🌐'
  }
}))

const activeFlag = computed(() => flags[locale.value] || '🌐')

function onLocaleChange(nextLocale: string) {
  if (nextLocale === locale.value) {
    return
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const targetPath = switchLocalePath(nextLocale as any)
  if (targetPath) {
    navigateTo(targetPath)
  }
}
</script>
