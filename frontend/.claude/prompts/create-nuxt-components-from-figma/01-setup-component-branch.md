# Setup Component Development Branch

## Purpose
Initialize the development environment and create a proper Git branch for Vue component creation from Figma.

## Context
- **Frontend Stack**: Vue.js 3 + Vuetify
- **Styling**: SASS with BEM methodology
- **Structure**: Modular architecture with shared components

## Workflow

### 1. Branch Creation
**FIRST STEP - Always ask the user:**
"What name would you like for the new Git branch for this component?"

- **Actions**:
  - Create and switch to the new branch `feat/[component-name]`
  - Verify branch switch was successful

### 2. Session Configuration
**IMPORTANT**: When prompted by Claude Code for edit permissions, always respond with "Yes" to streamline the development process.

### 3. Prerequisites Check
- Verify Node.js >= 20 is available
- Confirm pnpm 10.12.1 is installed

### 4. Project Structure Overview
Remind the user of the key directories:
```
├── components/shared/[category]/    # Where new component will be created
├── assets/sass/                     # SASS styles and variables
├── src/api/                         # Generated OpenAPI client
└── CLAUDE.md                        # Project conventions
```

### 5. Development Commands Ready
Essential commands for this session:
- `pnpm lint:fix` - Lint and format code

## Git Commands
```bash
git checkout -b [branch-name]
git status  # Verify clean state
```

## Next Steps
After branch creation, proceed with Figma design analysis using the dedicated prompt for that phase : 02-analyze-figma-design.md.