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


## Preliminary Questions to Ask


**AFTER branch creation, ask me:**


1. **Component Location**:
   - Which category? (ui, form, navigation, layout, media, other...)
   - What name for the component? (PascalCase)
   - Suggested final path: `components/shared/[category]/[ComponentName].vue`


2. **Component Configuration**:
   - Which props are needed?
   - Are there events to emit?
   - Should the component be responsive?
   - Icons: Use Figma icon names directly (they match the icon library)


3. **Integration**:
   - In which page/component will it be used?
   - Are there specific Vuetify dependencies?
   <!-- - Should we create variants (primary button, secondary, etc.)? -->


4. **Styles**:
   - Which SASS file for global styles? (`_buttons.sass`, `_cards.sass`, etc.)
   - Are there existing utility classes to reuse?


**Wait for my answers before starting development!**


## Session Configuration


**IMPORTANT**: When prompted by Claude Code for edit permissions, always respond with "Yes, allow all edits during this session" to streamline the development process.


## Workflow to Follow


### 0. Git Branch Setup
```
STEP 0: Branch Creation
- Ask for branch name (suggest: feature/component-[name] or feat/[name])
- Create and checkout new branch: git checkout -b [branch-name]
- Verify branch switch successful
```


### 1. Figma Design Retrieval and Analysis
```
STEP 1: Design Extraction
- Use mcp__figma__get_code to generate code from design
- Use mcp__figma__get_image to visualize the component
- Check project SASS structure (verify imports in assets/sass/main.sass)
- Review existing SASS classes to avoid duplicates:
  * Variables: Check assets/sass/base/_variables.sass for colors, fonts, etc.
  * Mixins: Check assets/sass/base/_mixins.sass for reusable patterns
  * Components: Review existing component files in assets/sass/components/
- Thoroughly analyze:
  * Dimensions and spacing
  * Color palette used (match with existing variables)
  * Typography and font sizes
  * Interactive states (hover, focus, disabled)
  * Responsive design if applicable
```


### 2. Vue Component Creation
```
STEP 2: Conversion and Adaptation
- Create .vue file in: components/shared/[category]/ComponentName.vue
- Component structure:
  * <template>: Convert HTML/React structure to Vue.js
  * <script setup>: Use Composition API
  * <style lang="sass" scoped>: Component-specific styles only when needed
- Required conversions:
  * React/JSX → Vue template syntax
  * Custom CSS → Vuetify responsive utility classes (PRIORITY)
  * React Props → Vue Props with TypeScript validation
  * React Events → Vue Events (@click, @input, etc.)
  * Icons: Use Figma icon names directly in v-icon (e.g., icon="mdi-home")

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
  * Apply via props: `color="primary"` or classes: `text-primary`, `bg-secondary`
- **Spacing**: Use Vuetify spacing utilities instead of custom CSS
  * Margins: `ma-{0-16}`, `mx-4`, `my-2`, responsive: `ma-lg-8`
  * Padding: `pa-{0-16}`, `px-4`, `py-2`, responsive: `pa-sm-4`
- **Typography**: Use Vuetify text classes
  * `text-h1` to `text-h6`, `text-body-1`, `text-body-2`, `text-caption`
- **Layout**: Use Vuetify grid and flex utilities
  * Grid: `v-container`, `v-row`, `v-col` with breakpoint props
  * Flex: `d-flex`, `flex-column`, `justify-center`, `align-center`
  * Responsive display: `d-sm-none`, `d-lg-block`


### SASS Fallback (Only when Vuetify insufficient)

**Existing Variables (assets/sass/base/_variables.sass)**
- Colors: `$primary`, `$secondary`, `$accent` (should match Vuetify theme)
- Typography: `$body-font-family: 'Poppins'`
- Layout: `$border-radius-root: 8px`, `$font-size-root: 16px`

**Available Mixins (assets/sass/base/_mixins.sass)**
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
});


const emit = defineEmits<{
  // Emitted events
}>();
</script>


<!-- Minimal custom styles - prefer Vuetify classes in template -->
<style lang="sass" scoped>
@use '@/assets/sass/base/variables' as *
@use '@/assets/sass/base/mixins' as *

.component-name
  // Only add custom styles when Vuetify utilities are insufficient
  // Most styling should be done via Vuetify classes in template

  // Example: Custom styling only when needed
  // color: $primary  // Prefer text-primary class instead
  // @include transition()  // Vuetify components have built-in transitions
  
  // ❌ NEVER USE custom responsive mixins - use Vuetify responsive system instead:
  // @include mobile - FORBIDDEN, use $vuetify.display.smAndDown in template
  // @include tablet - FORBIDDEN, use $vuetify.display.md in template  
  // @include desktop - FORBIDDEN, use $vuetify.display.lgAndUp in template
  
  // ✅ If absolute necessity for custom responsive CSS (extremely rare):
  // Use standard media queries as last resort, but prefer Vuetify classes
</style>
```


### BEM Methodology
- Block: `.button`
- Element: `.button__icon`, `.button__text`
- Modifier: `.button--primary`, `.button--disabled`


## Final Checklist
- [ ] Figma design correctly analyzed and reproduced
- [ ] Vue component created with proper structure
- [ ] **PRIORITY**: Vuetify responsive classes used instead of custom CSS where possible
- [ ] Minimal custom SASS created only when Vuetify insufficient
- [ ] BEM classes applied correctly (only for custom styles)
- [ ] Component integrated and functional
- [ ] TypeScript code validated
- [ ] Responsive design tested across Vuetify breakpoints (xs, sm, md, lg, xl)
- [ ] Interactive states functional using Vuetify components/utilities
- [ ] MCP Vuetify server consulted for component best practices when available


## Dynamic Arguments
$ARGUMENTS


---


**IMPORTANT**: Always start by asking the preliminary questions above, then analyze the Figma design before coding. Design fidelity and code reusability are priorities.
