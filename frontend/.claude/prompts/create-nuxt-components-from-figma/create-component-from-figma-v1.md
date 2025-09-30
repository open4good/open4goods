# Creating a Vue Component from Figma

You will create a Vue.js component from a Figma design following a structured methodology and respecting project conventions.

## Project Context

- **Frontend Stack**: Vue.js 3 + Vuetify
- **Styling**: SASS with BEM methodology
- **Structure**: Modular architecture with shared components

## Git Branch Setup

**FIRST STEP - Branch Creation:**
Ask the user: "What name would you like for the new Git branch for this component?"

- Use format: `feature/component-[component-name]` or `feat/[component-name]` as suggestions
- Create and switch to the new branch before starting development

## Workflow Overview

**PREREQUISITES (already done by user):**

- Figma component is already selected
- Component name will be taken from Figma component name
- Component will be created in `components/shared/[category]/[FigmaComponentName].vue`

**ONLY ASK if needed:**

- Which category folder? (cards, ui, form, navigation, layout, media, other...)
- Are there specific integration requirements?

**NO NEED TO ASK:**

- ❌ Component name (use Figma component name)
- ❌ Node ID (component already selected in Figma)
- ❌ Props (determined from Figma design analysis)
- ❌ Responsive behavior (follow Figma breakpoints if any)
- ❌ Icons (use Figma icon names directly)

## Session Configuration

**IMPORTANT**: When prompted by Claude Code for edit permissions, always respond with "Yes" to streamline the development process.

## Workflow to Follow

### 0. Git Branch Setup

```
STEP 0: Branch Creation
- Ask for branch name (suggest: feature/component-[name] or feat/[name])
- Create and checkout new branch: git checkout -b [branch-name]
- Verify branch switch successful
```

### 1. Automatic Figma Design Analysis

```
STEP 1: Direct Design Extraction (NO questions needed)
- Use mcp__figma__get_code to generate code from selected design
- Use mcp__figma__get_image to visualize the component
- Extract component name from Figma (this becomes the Vue component name)
- Automatically analyze design for:
  * Exact dimensions and spacing
  * Color palette (convert to closest existing SASS variables)
  * Typography and font sizes
  * Interactive states ONLY if shown in Figma
  * Responsive behavior ONLY if shown in Figma
- Check existing SASS structure for reusable variables/classes
```

### 2. Vue Component Creation - EXACT FIGMA REPRODUCTION ONLY

```
STEP 2: Conversion and Adaptation - STRICT FIGMA ADHERENCE
- Create .vue file in: components/shared/[category]/ComponentName.vue
- Component structure:
  * <template>: Convert HTML/React structure to Vue.js
  * <script setup>: Use Composition API with MINIMAL props
  * <style lang="sass" scoped>: Component-specific styles only when needed

- CRITICAL RULES FOR FIGMA REPRODUCTION:
  * ❌ DO NOT add slots unless explicitly visible in Figma design
  * ❌ DO NOT add hover effects unless shown in Figma interaction states
  * ❌ DO NOT add variant props unless multiple variants exist in Figma
  * ❌ DO NOT add elevation/shadow props unless specified in design
  * ❌ DO NOT add maxWidth/sizing props unless design shows specific constraints
  * ❌ DO NOT create generic/flexible components - reproduce EXACTLY what's in Figma
  * ❌ DO NOT add computed classes for variants unless Figma shows variants
  * ❌ DO NOT add animation/transition effects unless specified in Figma

- ONLY ADD:
  * Props that correspond to dynamic content visible in the Figma design
  * Styles that match exactly what's shown in Figma
  * Structure that mirrors the Figma component hierarchy

- Required conversions:
  * React/JSX → Vue template syntax
  * Custom CSS → Vuetify responsive utility classes (PRIORITY)
  * React Props → Vue Props with TypeScript validation (ONLY for content shown in Figma)
  * React Events → Vue Events (@click, @input, etc.) - ONLY if interactions shown in Figma
  * Icons: If icons, Use Figma icon names directly in v-icon (e.g., icon="mdi-home")

- CRITICAL: NEVER use custom responsive mixins (@include mobile, @include tablet). Always use Vuetify native responsive system:
  * Grid System: v-container, v-row, v-col with breakpoint props (xs, sm, md, lg, xl)
  * Display: d-{breakpoint}-{value} (e.g., d-lg-none, d-sm-flex)
  * Spacing: {property}{direction}-{breakpoint}-{size} (e.g., ma-lg-8, pa-sm-4)
  * Flex: flex-{breakpoint}-{value} (e.g., flex-lg-row, flex-sm-column)
  * Dynamic classes with $vuetify.display: :class="{ 'ga-4': $vuetify.display.mdAndUp }"
  * Reactive props: :size="$vuetify.display.smAndDown ? 'small' : 'default'"
  * Breakpoints: xs (0-600px), sm (600-960px), md (960-1264px), lg (1264-1904px), xl (1904px+)
```

### 3. Vuetify-First Styling Approach

```
STEP 3: Vuetify Utilities Before Custom Styles
- PRIORITY: Use Vuetify built-in classes and components:
  * v-btn variants (color, size, variant props)
  * v-card, v-sheet for containers
  * v-form components for form elements
  * Built-in spacing and display utilities

- Only create custom SASS when Vuetify utilities are insufficient:
  * Identify elements that CANNOT be achieved with Vuetify classes
  * Create minimal custom styles: assets/sass/components/_[type].sass
  * Recommended SASS structure:
    .component-name
      &__element
        // Element styles (only when Vuetify classes insufficient)
      &--modifier
        // Component variant (prefer Vuetify props when possible)
      &:hover, &:focus
        // Interactive states (use Vuetify state classes first)

- Examples of Vuetify-first approach:
  * Instead of custom .btn-primary → use v-btn color="primary"
  * Instead of custom spacing → use ma-4, pa-2, etc.
  * Instead of custom grid → use v-container, v-row, v-col
```

### 4. Integration and Validation

```
STEP 4: Ecosystem Integration
- Import component in parent page/component
- Check compatibility with:
  * Vue Router (if navigation)
  * Vuetify theme, breakpoints, and responsive behavior
  * Pinia Store (if state management)
- Test different states and props
- Validate responsive design using Vuetify's breakpoint system:
  * Test on different screen sizes (xs, sm, md, lg, xl)
  * Verify Vuetify responsive classes work correctly
  * Use browser DevTools or $vuetify.display for breakpoint testing
```

## Vuetify Resources & SASS Fallback

### PRIORITY: Vuetify Built-in Classes

- **Colors**: Use Vuetify color system (primary, secondary, accent, error, info, success, warning)
  - Apply via props: `color="primary"` or classes: `text-primary`, `bg-secondary`
- **Spacing**: Use Vuetify spacing utilities instead of custom CSS
  - Margins: `ma-{0-16}`, `mx-4`, `my-2`, responsive: `ma-lg-8`
  - Padding: `pa-{0-16}`, `px-4`, `py-2`, responsive: `pa-sm-4`
- **Typography**: Use Vuetify text classes
  - `text-h1` to `text-h6`, `text-body-1`, `text-body-2`, `text-caption`
- **Layout**: Use Vuetify grid and flex utilities
  - Grid: `v-container`, `v-row`, `v-col` with breakpoint props
  - Flex: `d-flex`, `flex-column`, `justify-center`, `align-center`
  - Responsive display: `d-sm-none`, `d-lg-block`

### SASS Fallback (Only when Vuetify insufficient)

**Existing Variables (assets/sass/base/\_variables.sass)**

- Colors: `$primary`, `$secondary`, `$accent` (should match Vuetify theme)
- Typography: `$body-font-family: 'Poppins'`
- Layout: `$border-radius-root: 8px`, `$font-size-root: 16px`

**Available Mixins (assets/sass/base/\_mixins.sass)**

- ❌ **NEVER USE**: `@include mobile`, `@include tablet`, `@include desktop` - Use Vuetify responsive system instead
- Transitions: `@include transition($property, $duration, $easing)` (prefer Vuetify component transitions)
- Shadows: `@include box-shadow($level)` (prefer Vuetify elevation classes)
- Buttons: `@include button-style($bg-color, $text-color)` (prefer v-btn)

**Existing Component Classes (assets/sass/components/)**

- Menu components: `.menu-item-base`, `.btn-nudge-tools`, `.main-menu-container`
- Check other component files: `_buttons.sass`, `_cards.sass`, `_menus.sass`

**IMPORTANT**:

1. FIRST try to achieve the design with Vuetify classes
2. Only create custom SASS when Vuetify utilities cannot achieve the desired result
3. Always reuse existing classes and variables instead of creating duplicates

### Icons Implementation

- Figma icon names match the icon library directly
- Use them as-is in `<v-icon icon="figma-icon-name" />`
- No need to ask for icon mapping - implement directly from Figma

## Conventions to Follow

### File Naming

- Components: `PascalCase.vue` (e.g., `ButtonNudgeTools.vue`)
- SASS Styles: `_kebab-case.sass` (e.g., `_button-variants.sass`)
- Suggested categories: ui, form, navigation, layout, media

### Vue Component Structure

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
}

const props = withDefaults(defineProps<Props>(), {
  // Default values
})

const emit = defineEmits<{
  // Emitted events
}>()
</script>

<!-- ONLY styles that exactly match Figma design - prefer Vuetify classes in template -->
<style lang="sass" scoped>
@use '@/assets/sass/base/variables' as *
@use '@/assets/sass/base/mixins' as *

.component-name
  // ONLY add styles that exactly reproduce the Figma design
  // DO NOT add hover effects, transitions, or animations unless shown in Figma
  // Most styling should be done via Vuetify classes in template

  // ❌ FORBIDDEN unless explicitly in Figma:
  // hover effects, transitions, animations, shadows, borders
  // variant styles, responsive breakpoints not shown in design

  // ❌ NEVER USE custom responsive mixins - use Vuetify responsive system instead:
  // @include mobile - FORBIDDEN, use $vuetify.display.smAndDown in template
  // @include tablet - FORBIDDEN, use $vuetify.display.md in template
  // @include desktop - FORBIDDEN, use $vuetify.display.lgAndUp in template

  // ✅ Only add custom CSS for:
  // Exact colors, fonts, spacing, layout that match Figma pixel-perfect
  // Use standard media queries as last resort, but prefer Vuetify classes
</style>
```

### BEM Methodology

- Block: `.button`
- Element: `.button__icon`, `.button__text`
- Modifier: `.button--primary`, `.button--disabled`

## Final Checklist - EXACT FIGMA REPRODUCTION

- [ ] Figma design correctly analyzed and reproduced EXACTLY
- [ ] Vue component created with proper structure
- [ ] **PRIORITY**: Vuetify responsive classes used instead of custom CSS where possible
- [ ] Minimal custom SASS created only when Vuetify insufficient
- [ ] BEM classes applied correctly (only for custom styles)
- [ ] Component integrated and functional
- [ ] TypeScript code validated
- [ ] Responsive design tested across Vuetify breakpoints (xs, sm, md, lg, xl)
- [ ] Interactive states functional using Vuetify components/utilities
- [ ] MCP Vuetify server consulted for component best practices when available
- [ ] **CRITICAL**: NO additional features beyond what's shown in Figma
- [ ] **CRITICAL**: NO generic slots unless explicitly designed in Figma
- [ ] **CRITICAL**: NO hover effects unless shown in Figma interaction states
- [ ] **CRITICAL**: NO variant props unless multiple variants exist in Figma

## 5. Commit Changes

```
STEP 5: Create Git Commit
- After successful component creation and validation
- Stage all created/modified files: git add .
- Create commit with descriptive message following project conventions
- Use format: "feat(component): add [ComponentName] from Figma design"
- Include component details in commit body if needed
```

## Dynamic Arguments

$ARGUMENTS

---

**IMPORTANT**: Start by analyzing the selected Figma design directly (component name and selection are already done). EXACT design reproduction is the ONLY priority - do not add features, flexibility, or enhancements not explicitly shown in Figma.
