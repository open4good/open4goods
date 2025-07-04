# Nuxt 3 Frontend Development Guide (Nudger Project)

This guide is a comprehensive overview of the Nudger UI project. It covers the Nuxt 3 / vue 3 application structure, coding conventions and tooling. It is written for new contributors and AI code-generation agents alike, aiming to ensure consistency and clarity in development.

---

## Getting Started: Installation & Setup

1. **Prerequisites**: Node.js 18+, `pnpm` (`npm install -g pnpm`)
2. **Clone the Repository**: `git clone https://github.com/your-org/nudger-nuxt-front.git`
3. **Install Dependencies**: `pnpm install`
4. **Environment Variables**:
   - `STRAPI_URL`: e.g. `http://localhost:1337`
   - `STRAPI_TOKEN`: read-only token
   - `NUXT_PUBLIC_SITE_URL`, etc.
5. **Run Dev Server**: `pnpm dev` → http://localhost:3000
6. **Production Build**: `pnpm build` then `pnpm preview`
7. **Static Generation**: `pnpm generate`
8. **Helper Scripts**:
   - `pnpm lint`
   - `pnpm format`
   - `pnpm test`
   - `pnpm storybook`
   - `pnpm generate:api`
   - `pnpm storybook:build`

---

## Project Structure and Directories

- `src/pages/` → file-based routing
- `src/components/` → reusable components
- `src/layouts/` → page layouts
- `src/composables/` → reusable logic (e.g. `useFeature.ts`)
- `src/stores/` → Pinia stores
- `src/api/` → OpenAPI generated client & API helpers
- `src/assets/` → static assets & global CSS
- `src/middleware/` → route guards
- `src/plugins/` → Nuxt plugins
- `tests/` or `*.spec.ts` → colocated or standalone test files

---

## Vue 3 & Nuxt 3 Coding Conventions

- Use `<script setup lang="ts">`
- Use `defineProps`, `ref`, `computed`, etc.
- Prefer TypeScript everywhere
- SSR-safe code: guard `window`, `document`, etc.
- Auto-imports: `useRouter`, `useHead`, etc.
- Use `@/` or `~/` aliases
- Component structure: separate logic/UI
- Nuxt page features: `definePageMeta`, `useFetch`, `useAsyncData`

---

## Styling with Tailwind CSS

- Use Tailwind utility classes in templates
- Customize via `tailwind.config.js`
- Include `@tailwind base;`, `@tailwind components;`, etc.
- Use `@apply` when necessary
- Responsive: `md:`, `lg:`
- Dark mode support

Example:

```vue
<template>
  <button
    class="px-4 py-2 bg-primary text-white font-semibold rounded hover:bg-primary-dark disabled:opacity-50"
  >
    {{ label }}
  </button>
</template>
```

---

## State Management with Pinia

- Define store with `defineStore`
- State as a function
- Use getters and actions
- Example:

```ts
export const useCartStore = defineStore('cart', {
  state: () => ({ items: [] }),
  getters: {
    itemCount: (state) => state.items.length,
    totalPrice: (state) =>
      state.items.reduce((sum, item) => sum + item.price, 0),
  },
  actions: {
    addItem(product) {
      this.items.push(product)
    },
    removeItem(id) {
      this.items = this.items.filter((i) => i.id !== id)
    },
  },
})
```

---

## OpenAPI Client Generation & Integration

- OpenAPI spec (e.g., `nudger-api.json`)
- Script: `pnpm generate:api` → generates a TypeScript fetch client in `src/api/`
- Use generated API classes for type-safe calls
- Don’t manually edit generated files

Workflow:

1. Update spec
2. Run `generate:api`
3. Use types in code
4. Write tests

---

## Content from Strapi CMS

- Env vars: `STRAPI_URL`, `STRAPI_TOKEN`
- Fetch using `useFetch`

```ts
const { data } = await useFetch(`${config.public.strapiUrl}/api/pages`, {
  headers: { Authorization: `Bearer ${config.strapiToken}` },
})
```

- Handle nested `data[].attributes`
- Consider `useStrapiContent()` composable abstraction

---

## Testing with Vitest

- Colocate test files (`.spec.ts`)
- Run tests: `pnpm test` or `--watch`
- Use `@vue/test-utils`
- Test stores with `setActivePinia`
- Use snapshots carefully

---

## Storybook for UI Components

- Run `pnpm storybook` → http://localhost:6006
- Co-locate stories next to components

```ts
export const Primary: Story = {
  args: { label: 'Primary Button' },
}
```

- Update stories with component changes
- Use Addon Essentials for docs

---

---

## Documentation

- Always document produced code
- Always update existing documentation (for example README.md, AGENTS.md) with features update, architecturals changes or considerations.
- Dependency updates are handled by Renovate using the configs `renovate.json` at
  the repository root and `frontend/renovate.json`. Updates run nightly.

## Linting and Formatting

- ESLint with Nuxt/Vue recommended rules
- Prettier integrated via ESLint
- Run: `pnpm lint`, `pnpm lint --fix`
- Run: `pnpm format` to check formatting
- Husky hooks enforce checks on commits

## Architecture & SSR Best Practices

- Split UI and logic (composables, container/presentational components)
- SSR-safe: avoid global state leakage
- Lazy hydrate non-critical components (e.g., `vue-lazy-hydration`)
- Code-splitting with dynamic imports
- Use `useHead` for SEO

## Pull request

- Use Conventional commits (e.g., `feat:`, `fix:`)
- Generate a clear and complete PR description (**why** and **what**).
- Add a footer indicating this PR is generatEd by AI agent, and the estimatated time an average developper would have spent on this task

Before issueing a PR, systematically validate and check global non regession using

- pnpm lint
- pnpm test run
- pnpm generate
- pnpm storybook, check all components have an associated storybook
- pnpm preview, then tests URLS are HTTP 200 or act as expected
