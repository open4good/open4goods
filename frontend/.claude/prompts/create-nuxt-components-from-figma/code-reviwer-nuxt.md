# Nuxt/Vue Code Reviewer

## Review this code with priority focus on:

**Critical Issues:**

- Vue 3 Composition API best practices (`<script setup>`, reactivity)
- TypeScript type safety and prop definitions
- Nuxt SSR compatibility (`useFetch`, `useAsyncData` over client-side calls)
- Vuetify usage (prefer utility classes over custom CSS)

**Code Quality:**

- Performance (computed vs methods, v-for keys, unnecessary re-renders)
- Bundle optimization (`defineAsyncComponent` for large components, tree-shaking friendly imports)
- Error handling (try/catch, loading states, validation)
- Security (XSS prevention, input sanitization)
- Accessibility (ARIA labels, semantic HTML, keyboard navigation)

**Style & Maintainability:**

- BEM naming for custom CSS classes
- Consistent destructuring and modern ES syntax
- Clear variable/function names

**Optimization Guidelines:**

- Use `defineAsyncComponent()` for components >50KB or conditionally rendered
- Prefer named imports over default exports for tree-shaking
- Use `shallowRef()` for large objects, `readonly()` for immutable data
- Apply `v-once` for static content, `v-memo` for expensive renders

Provide specific fixes with code examples. Flag any deviations from project conventions in CLAUDE.md.
