# Analyze Figma Design

## Purpose

Extract and analyze the Figma component design to understand exact requirements for Vue.js implementation.

## Prerequisites

- Git branch created and checked out
- Figma component already selected by user

## Automatic Analysis Process

### 1. Design Extraction (NO Questions Needed)

Use Figma MCP tools to gather component data:

**Tools to use:**

- `mcp__figma__get_code` - Generate code from selected design
- `mcp__figma__get_image` - Visualize the component

**Extract automatically:**

- Component name from Figma (this becomes the Vue component name)
- Exact dimensions and spacing
- Color palette (convert to closest existing SASS variables)
- Typography and font sizes
<!-- - Interactive states ONLY if shown in Figma -->
- Responsive behavior ONLY if shown in Figma

### 2. Design Analysis Requirements

**CRITICAL RULES - EXACT FIGMA REPRODUCTION ONLY:**

- ❌ DO NOT assume hover effects unless shown in Figma interaction states
- ❌ DO NOT assume variant props unless multiple variants exist in Figma
- ❌ DO NOT assume elevation/shadow props unless specified in design
- ❌ DO NOT assume maxWidth/sizing props unless design shows constraints
- ❌ DO NOT assume slots unless explicitly visible in Figma design
- ❌ DO NOT assume animation/transition effects unless specified in Figma

**ONLY ANALYZE:**

- Dynamic content visible in the Figma design
- Styles that match exactly what's shown in Figma
- Structure that mirrors the Figma component hierarchy
- Interactive elements actually shown in the design

### 3. Category Create

- "Which category folder should this component go in?"
- **Options**: cards, ui, form, navigation, layout, media, other
- **Default final location**: `components/shared/[category]/[FigmaComponentName].vue`

### 4. Integration Requirements Check

**ONLY ASK if needed:**

- "Are there specific integration requirements for this component?"
- Examples: Router integration, store connection, API calls

### 5. Existing SASS Structure Check

Analyze current project structure:

- Check `assets/sass/base/_variables.sass` for reusable variables
- Review `assets/sass/components/` for existing patterns
- Identify Vuetify theme colors that match Figma colors

### 6. Icon Analysis

- Extract icon names directly from Figma
- Map to Material Design Icons (mdi-\*) format
- No need to ask for icon mapping - use Figma names directly

## Analysis Output

Document findings in structured format:

**Component Analysis:**

- Name: [FigmaComponentName]
- Category: [selected-category]
- Dimensions: [width x height]
- Colors used: [list with SASS variable mappings]
- Typography: [font families and sizes]
- Interactive states: [only if shown in Figma]
- Icons needed: [Figma icon names → mdi-* mappings]

**Props Needed:**

- Only props for dynamic content actually shown in Figma
- Type definitions based on content analysis

**Structure:**

- Template hierarchy based on Figma layers
- Required Vuetify components identified

## Next Steps

After analysis completion, proceed with Vue component creation using the dedicated prompt for that phase.
