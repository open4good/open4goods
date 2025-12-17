# Git Workflow (Mandatory)

## Branch Management

**BEFORE making any code changes:**

1. Run: `git status` to check current branch
2. If on a protected branch (main, master, develop), you MUST:
   - Create a new feature branch: `git checkout -b feature/<short-descriptive-slug>`
   - Use meaningful branch names that describe the work (e.g., `feature/add-mcp-servers`, `fix/login-validation`)

**Do all edits and commits on the feature branch only.**

## Commit and Push

**AFTER implementing and verifying changes:**

1. Stage changes: `git add -A`
2. Commit with meaningful message: `git commit -m "<clear description of changes>"`
3. Push to origin: `git push -u origin HEAD`

## Pull Request Creation

**After pushing:**

- Prefer using GitHub CLI if available: `gh pr create --fill --draft`
- If `gh` is not available, provide the exact command(s) or URL/steps to open the PR manually
- Always create draft PRs initially for review

## Protected Branches

The following branches are protected and should never receive direct commits:
- `main`
- `master`
- `develop`
