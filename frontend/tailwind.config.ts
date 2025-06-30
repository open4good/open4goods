import type { Config } from 'tailwindcss'

export default {
  // Paths to all template files in the project
  content: ['./src/**/*.{vue,js,ts}'],

  // Custom theme values. Colors come from `tokens.config.json` if defined,
  // otherwise Tailwind's defaults are used.
  theme: {
    extend: {}
  },

  // Extra Tailwind plugins to load
  plugins: []
} satisfies Config
