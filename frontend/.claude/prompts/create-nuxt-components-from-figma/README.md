# Vue Component from Figma - Prompt Collection

## Overview

This collection of specialized prompts optimizes the process of creating Vue.js components from Figma designs. Each prompt focuses on a specific phase of development to maximize efficiency and ensure exact design reproduction.

## Prompt Sequence

### 1. Setup Component Branch
**File**: `01-setup-component-branch.md`
**Purpose**: Initialize Git branch and development environment
**Key Actions**:
- Create and checkout new feature branch
- Verify development prerequisites
- Set up session permissions

### 2. Analyze Figma Design
**File**: `02-analyze-figma-design.md`
**Purpose**: Extract and analyze Figma component requirements
**Key Actions**:
- Use Figma MCP tools for design extraction
- Document component structure and styling needs
- Identify category placement and integration requirements

### 3. Create Vue Component
**File**: `03-create-vue-component.md`
**Purpose**: Build the Vue.js component structure and logic
**Key Actions**:
- Create component file with proper TypeScript structure
- Implement template matching Figma hierarchy
- Define props only for dynamic content shown in design

### 4. Vuetify Styling
**File**: `04-vuetify-styling.md`
**Purpose**: Apply Vuetify-first styling approach
**Key Actions**:
- Use Vuetify components and utility classes as priority
- Implement responsive design with Vuetify breakpoint system
- Add minimal custom SASS only when necessary

### 5. Finalize and Commit
**File**: `05-finalize-and-commit.md`
**Purpose**: Complete integration, validation, and Git commit
**Key Actions**:
- Run code quality checks and tests
- Validate exact Figma reproduction
- Create proper Git commit with descriptive message

## Key Principles

### Exact Figma Reproduction
- ❌ NO additional features beyond Figma design
- ❌ NO hover effects unless shown in Figma
- ❌ NO variant props unless multiple variants exist
- ❌ NO generic slots unless explicitly designed
- ✅ ONLY implement what's visible in the design

### Vuetify-First Approach
- **Priority 1**: Vuetify components and utility classes
- **Priority 2**: Existing project SASS variables/mixins
- **Priority 3**: Minimal custom SASS for exact design match
- **Forbidden**: Custom responsive mixins (@include mobile, @include tablet)

### Project Integration
- Follow existing TypeScript and Vue conventions
- Use project's SASS structure and variables
- Integrate with Vuetify theme and breakpoint system
- Maintain code quality standards

## Usage Instructions

1. **Sequential Execution**: Use prompts in order (01 → 02 → 03 → 04 → 05)
2. **Phase Focus**: Each prompt handles one specific development phase
3. **Context Carry-over**: Information from each phase informs the next
4. **Validation**: Each prompt includes quality checks and validation steps

## Original Monolithic Prompt

The original comprehensive prompt (`create-component-from-figma.md`) remains available for reference but is no longer recommended for active use due to its length and complexity.

## Benefits of Prompt Decomposition

- **Focused Execution**: Each prompt has a clear, specific purpose
- **Better Error Recovery**: Issues can be isolated to specific phases
- **Improved Maintainability**: Updates can target specific aspects
- **Enhanced Clarity**: Reduced cognitive load per prompt
- **Optimal Context Usage**: More efficient token usage per phase