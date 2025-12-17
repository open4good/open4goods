---
description: Create a Pull Request for the current branch
---

# Create Pull Request

This workflow creates a draft Pull Request for the current branch.

## Steps

1. **Verify working tree is clean**
   ```bash
   git status
   ```
   Ensure there are no uncommitted changes.

2. **Push current branch to origin**
   ```bash
   git push -u origin HEAD
   ```

3. **Create draft PR**
   
   If GitHub CLI is available:
   ```bash
   gh pr create --fill --draft
   ```
   
   If `gh` is not available:
   - Provide the GitHub URL to create a PR manually
   - Include: current branch name, suggested title, and summary of changes

## PR Content Guidelines

The PR should include:
- **Title**: Clear, concise description of the change
- **Summary**: What was changed and why
- **Test Evidence**: How the changes were verified
- **Risks**: Any potential breaking changes or concerns
