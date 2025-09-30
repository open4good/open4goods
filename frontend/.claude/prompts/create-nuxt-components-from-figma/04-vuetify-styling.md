# Vuetify-First Styling Approach

## Purpose

Apply styling to the Vue component using Vuetify utilities as priority, with minimal custom SASS as fallback.

## Prerequisites

- Vue component structure created
- Figma design requirements documented
- Component template implemented

## Vuetify-First Methodology

### 1. PRIORITY: Vuetify Built-in Classes

**Colors** (Use Vuetify color system first):

- Primary colors: `color="primary"`, `text-primary`, `bg-secondary`
- Semantic colors: `error`, `info`, `success`, `warning`
- Surface colors: `surface`, `surface-variant`

**Spacing** (Replace custom margins/padding):

- Margins: `ma-{0-16}`, `mx-4`, `my-2`
- Padding: `pa-{0-16}`, `px-4`, `py-2`
- Responsive spacing: `ma-lg-8`, `pa-sm-4`

**Typography** (Use Vuetify text classes):

- Headers: `text-h1` to `text-h6`
- Body text: `text-body-1`, `text-body-2`
- Display: `text-caption`, `text-overline`

**Layout** (Grid and flex utilities):

- Grid: `v-container`, `v-row`, `v-col` with breakpoint props
- Flex: `d-flex`, `flex-column`, `justify-center`, `align-center`
- Responsive display: `d-sm-none`, `d-lg-block`

### 2. Vuetify Component Selection

**Replace custom elements with Vuetify components:**

- Buttons: `v-btn` with `color`, `size`, `variant` props
- Cards: `v-card`, `v-sheet` for containers
- Form elements: `v-text-field`, `v-select`, `v-checkbox`
- Navigation: `v-tabs`, `v-breadcrumbs`, `v-pagination`
- Data display: `v-table`, `v-list`, `v-chip`

**Component props vs custom styles:**

```vue
<!-- ✅ PREFERRED: Use component props -->
<v-btn color="primary" size="large" variant="elevated">
  Click me
</v-btn>

<!-- ❌ AVOID: Custom styling -->
<button class="custom-primary-button">
  Click me
</button>
```

### 3. Responsive Implementation

**CRITICAL**: Use Vuetify responsive system, NOT custom mixins:

**Breakpoint Display:**

```vue
<!-- Show/hide elements by breakpoint -->
<div class="d-none d-lg-block">Desktop only</div>
<div class="d-block d-lg-none">Mobile/tablet only</div>
```

**Responsive Props:**

```vue
<!-- Responsive component sizing -->
<v-btn :size="$vuetify.display.smAndDown ? 'small' : 'default'">
  Responsive Button
</v-btn>
```

**Dynamic Classes:**

```vue
<!-- Conditional classes based on breakpoint -->
<div
  :class="{
    'ga-4': $vuetify.display.mdAndUp,
    'ga-2': $vuetify.display.smAndDown,
  }"
>
  Responsive grid gap
</div>
```

**Grid Responsiveness:**

```vue
<v-row>
  <v-col cols="12" sm="6" md="4" lg="3">
    Responsive column
  </v-col>
</v-row>
```

### 4. When to Use Custom SASS

**ONLY create custom SASS when Vuetify utilities are insufficient:**

**Identify elements that CANNOT be achieved with Vuetify:**

- Unique color combinations not in theme
- Complex pseudo-element styles
- Specific hover effects shown in Figma
- Pixel-perfect spacing not available in Vuetify scale

**Minimal Custom SASS Structure:**

```sass
@use '@/assets/sass/base/variables' as *
@use '@/assets/sass/base/mixins' as *

.component-name
  // Only styles that exactly match Figma design
  // that cannot be achieved with Vuetify classes

  &__element
    // Element-specific styles (minimal)

  &--modifier
    // Variant styles (prefer Vuetify props when possible)
```

### 5. SASS Fallback Resources

**Existing Variables** (`assets/sass/base/_variables.sass`):

- `$primary`, `$secondary`, `$accent` (should match Vuetify theme)
- `$body-font-family: 'Poppins'`
- `$border-radius-root: 8px`

**Available Mixins** (`assets/sass/base/_mixins.sass`):

- ❌ **FORBIDDEN**: `@include mobile`, `@include tablet`, `@include desktop`
- ✅ **Allowed**: `@include transition($property, $duration, $easing)`
- ✅ **Allowed**: `@include box-shadow($level)` (prefer Vuetify elevation)

**Existing Component Classes** (`assets/sass/components/`):

- Check for reusable patterns: `_buttons.sass`, `_cards.sass`, `_menus.sass`
- Reuse existing classes instead of creating duplicates

### 6. Styling Priority Checklist

**Before writing custom CSS:**

- [ ] Can this be achieved with Vuetify component props?
- [ ] Can this be achieved with Vuetify utility classes?
- [ ] Can this be achieved with Vuetify responsive classes?
- [ ] Is there an existing SASS component class that fits?
- [ ] Is this style exactly as shown in Figma design?

**Custom SASS is acceptable only when:**

- [ ] Figma design requires exact colors not in Vuetify theme
- [ ] Specific spacing not available in Vuetify scale (0-16)
- [ ] Unique hover/focus effects shown in Figma
- [ ] Complex layout that grid system cannot achieve

### 7. Forbidden Styling Practices

**❌ NEVER DO:**

- Use custom responsive mixins (@include mobile, @include tablet)
- Add hover effects not shown in Figma
- Create variant props without Figma variants
- Add transitions/animations not specified in Figma
- Override Vuetify component internals with deep selectors
- Use !important declarations

**✅ ALWAYS DO:**

- Try Vuetify solution first
- Use existing project variables/mixins
- Match Figma design exactly
- Keep custom CSS minimal
- Use BEM methodology for custom classes

### 8. Testing Responsive Design

**Vuetify Breakpoint Testing:**

- xs (0-600px): Mobile phones
- sm (600-960px): Tablets
- md (960-1264px): Small laptops
- lg (1264-1904px): Large laptops/desktops
- xl (1904px+): Large monitors

**Use browser DevTools or $vuetify.display:**

```javascript
// In component for debugging
console.log('Current breakpoint:', $vuetify.display)
```

## Next Steps

After styling completion, proceed with integration testing and commit using the finalization prompt.
