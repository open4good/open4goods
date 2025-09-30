# Create Vue.js Component

## Purpose

Convert Figma design analysis into a functional Vue.js component with exact reproduction of the design.

## Prerequisites

- Figma design analyzed and documented
- Component category selected
- Git branch ready for changes

## Component Creation Process

### 1. File Creation

**Location**: `components/shared/[category]/[FigmaComponentName].vue`

**Required conversions:**

- React/JSX → Vue template syntax
- Custom CSS → Vuetify responsive utility classes (PRIORITY)
- React Props → Vue Props with TypeScript validation
- React Events → Vue Events (@click, @input, etc.)
- Icons: Use Figma icon names directly in v-icon (e.g., `icon="mdi-home"`)

### 2. Vue Component Structure Template

```vue
<template>
  <!-- Use Vuetify components and utility classes -->
  <v-card class="component-name" elevation="1">
    <v-container>
      <v-row>
        <v-col cols="12" sm="6" md="4">
          <!-- Responsive grid using Vuetify -->
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>

<script setup lang="ts">
interface Props {
  // Props with TypeScript types
  // ONLY for dynamic content visible in Figma design
}

const props = withDefaults(defineProps<Props>(), {
  // Default values
})

const emit = defineEmits<{
  // Emitted events - ONLY if interactions shown in Figma
}>()
</script>

<style lang="sass" scoped>
@use '@/assets/sass/base/variables' as *
@use '@/assets/sass/base/mixins' as *

.component-name
  // ONLY add styles that exactly reproduce the Figma design
  // Most styling should be done via Vuetify classes in template
</style>
```

### 3. Critical Implementation Rules

**STRICT FIGMA ADHERENCE:**

- ❌ DO NOT add slots unless explicitly visible in Figma design
- ❌ DO NOT add hover effects unless shown in Figma interaction states
- ❌ DO NOT add variant props unless multiple variants exist in Figma
- ❌ DO NOT add elevation/shadow props unless specified in design
- ❌ DO NOT add maxWidth/sizing props unless design shows constraints
- ❌ DO NOT create generic/flexible components
- ❌ DO NOT add computed classes for variants unless Figma shows variants
- ❌ DO NOT add animation/transition effects unless specified in Figma

**ONLY ADD:**

- Props that correspond to dynamic content visible in the Figma design
- Styles that match exactly what's shown in Figma
- Structure that mirrors the Figma component hierarchy
- Events only if interactions are shown in Figma

### 4. Vuetify Responsive System (NO Custom Breakpoints)

**CRITICAL**: NEVER use custom responsive mixins (@include mobile, @include tablet). Always use Vuetify native responsive system:

**Grid System:**

- `v-container`, `v-row`, `v-col` with breakpoint props (xs, sm, md, lg, xl)

**Display Classes:**

- `d-{breakpoint}-{value}` (e.g., `d-lg-none`, `d-sm-flex`)

**Spacing Classes:**

- `{property}{direction}-{breakpoint}-{size}` (e.g., `ma-lg-8`, `pa-sm-4`)

**Flex Classes:**

- `flex-{breakpoint}-{value}` (e.g., `flex-lg-row`, `flex-sm-column`)

**Dynamic Classes:**

- `:class="{ 'ga-4': $vuetify.display.mdAndUp }"`

**Reactive Props:**

- `:size="$vuetify.display.smAndDown ? 'small' : 'default'"`

**Breakpoints:**

- xs (0-600px), sm (600-960px), md (960-1264px), lg (1264-1904px), xl (1904px+)

### 5. TypeScript Props Definition

**Props Guidelines:**

- Only define props for content that appears dynamic in Figma
- Use appropriate TypeScript types
- Provide sensible defaults
- Add JSDoc comments for complex props

```typescript
interface Props {
  /** Text content from Figma design */
  title?: string
  /** Icon name matching Figma design */
  iconName?: string
  /** Color variant if multiple shown in Figma */
  variant?: 'primary' | 'secondary'
  /** Click handler if button shown in Figma */
  disabled?: boolean
}
```

### 6. Component Validation

**Before completion, verify:**

- [ ] Template structure matches Figma layer hierarchy
- [ ] All dynamic content has corresponding props
- [ ] Vuetify components used where appropriate
- [ ] Responsive behavior matches Figma (if specified)
- [ ] Icons render correctly with mdi-\* names
- [ ] TypeScript compilation passes
- [ ] No custom responsive mixins used

### 7. File Naming Conventions

- Components: `PascalCase.vue` (e.g., `ButtonNudgeTools.vue`)
- Use exact Figma component name when possible
- Keep names descriptive and specific

## Next Steps

After component creation, proceed with Vuetify-first styling approach using the dedicated styling prompt.
