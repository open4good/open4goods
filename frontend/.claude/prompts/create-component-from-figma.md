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
  * <style lang="sass" scoped>: Component-specific styles
- Required conversions:
  * React/JSX → Vue template syntax
  * Tailwind classes → Vuetify + custom BEM classes
  * React Props → Vue Props with TypeScript validation
  * React Events → Vue Events (@click, @input, etc.)
  * Icons: Use Figma icon names directly in v-icon (e.g., icon="mdi-home")
```


### 3. Reusable Styles Extraction
```
STEP 3: Optimization and Reusability
- Identify reusable elements in the design:
  * Buttons with variants (.btn-nudge-tools, .btn-menu-link)
  * Cards and containers
  * Form elements
  * Icons and badges
- Create/update: assets/sass/components/_[type].sass
- Recommended SASS structure:
  .component-name
    &__element
      // Element styles
    &--modifier
      // Component variant
    &:hover, &:focus
      // Interactive states
```


### 4. Integration and Validation
```
STEP 4: Ecosystem Integration
- Import component in parent page/component
- Check compatibility with:
  * Vue Router (if navigation)
  * Vuetify theme and breakpoints
  * Pinia Store (if state management)
- Test different states and props
- Validate responsive design
```


## Available SASS Resources


### Existing Variables (assets/sass/base/_variables.sass)
- Colors: `$primary`, `$secondary`, `$accent`, `$error`, `$info`, `$success`, `$warning`
- Typography: `$body-font-family: 'Poppins'`
- Layout: `$border-radius-root: 8px`, `$font-size-root: 16px`


### Available Mixins (assets/sass/base/_mixins.sass)
- Responsive: `@include mobile`, `@include tablet`, `@include desktop`
- Transitions: `@include transition($property, $duration, $easing)`
- Shadows: `@include box-shadow($level)`
- Buttons: `@include button-style($bg-color, $text-color)`


### Existing Component Classes (assets/sass/components/)
- Menu components: `.menu-item-base`, `.btn-nudge-tools`, `.main-menu-container`
- Check other component files: `_buttons.sass`, `_cards.sass`, `_menus.sass`


**IMPORTANT**: Always reuse existing classes and variables instead of creating duplicates!


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
  <div class="component-name">
    <!-- Optimized HTML structure -->
  </div>
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


<style lang="sass" scoped>
@use '@/assets/sass/base/variables' as *
@use '@/assets/sass/base/mixins' as *


.component-name
  // Use existing variables and mixins
  color: $primary
  border-radius: $border-radius-root
  @include transition()
  @include box-shadow(1)
 
  // Responsive design with existing mixins
  @include mobile
    // Mobile styles
   
  @include tablet
    // Tablet styles
   
  @include desktop
    // Desktop styles
</style>
```


### BEM Methodology
- Block: `.button`
- Element: `.button__icon`, `.button__text`
- Modifier: `.button--primary`, `.button--disabled`


## Final Checklist
- [ ] Figma design correctly analyzed and reproduced
- [ ] Vue component created with proper structure
- [ ] Styles extracted to appropriate SASS files
- [ ] BEM classes applied correctly
- [ ] Component integrated and functional
- [ ] TypeScript code validated
- [ ] Responsive design tested
- [ ] Interactive states functional


## Dynamic Arguments
$ARGUMENTS


---


**IMPORTANT**: Always start by asking the preliminary questions above, then analyze the Figma design before coding. Design fidelity and code reusability are priorities.
