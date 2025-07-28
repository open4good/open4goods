---
name: "♻️ Refactor (EN)"
about: Request a code refactoring to improve maintainability or performance
title: "[Refactor]: "
labels: refactor
assignees: ''
---

### Current Situation  
*Describe the existing code and why refactoring is needed.*  
<!-- Example: "The `ProductComparator` class handles both data fetching and calculation, making it hard to test and maintain." -->

### Refactoring Proposal  
*Describe your plan for refactoring. What will you change, and how will the code improve?*  
<!-- Example: "Separate data fetching into `ProductDataService` and restrict `ProductComparator` to calculations. This will reduce class size and improve testability." -->

### Criteria for Completion  
*Define what success looks like for this refactoring (ensure no functionality changes except improvement in code quality):*  
- [ ] No regressions – existing tests all pass and behavior remains unchanged  
- [ ] Improved code structure – code is easier to understand and maintain (explain the improvements in the pull request)  
<!-- You can add additional specific goals, e.g. performance benchmarks or reduced duplication, if applicable. -->

### Additional Context  
*Add any other context, links to relevant issues, or reasons for the refactoring.*  
<!-- Example: "This refactor will make it easier to add new comparison metrics in the future and is a prerequisite for issue #789." -->  
