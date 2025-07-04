import type { StorybookConfig } from '@storybook-vue/nuxt'

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(js|ts|mdx)'],
  addons: ['@storybook/addon-essentials'],
  framework: {
    name: '@storybook-vue/nuxt',
    options: {},
  },
}

export default config
